package com.socialworld.app.chat;

import com.socialworld.app.avatar.AvatarService;
import com.socialworld.app.chat.dto.ConversationResponse;
import com.socialworld.app.chat.dto.MessageResponse;
import com.socialworld.app.chat.ws.ChatSessionRegistry;
import com.socialworld.app.common.exception.ApiException;
import com.socialworld.app.common.exception.ErrorCode;
import com.socialworld.app.moderation.BlockService;
import com.socialworld.app.notification.PushNotificationService;
import com.socialworld.app.user.User;
import com.socialworld.app.user.UserRepository;
import com.socialworld.app.user.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    static final int MAX_CONTENT_LENGTH = 1000;
    static final int PAGE_SIZE = 50;

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final AvatarService avatarService;
    private final ChatSessionRegistry sessionRegistry;
    private final BlockService blockService;
    private final PushNotificationService pushNotificationService;

    /**
     * Persists a message and pushes it to both participants' live sockets.
     * Used by the WebSocket handler, the REST fallback, and (later) gifts.
     */
    @Transactional
    public MessageResponse sendMessage(UUID senderId, UUID receiverId, String rawContent, MessageType type) {
        if (senderId.equals(receiverId)) {
            throw new ApiException(ErrorCode.SELF_ACTION_NOT_ALLOWED, "You cannot message yourself.");
        }
        blockService.assertInteractionAllowed(senderId, receiverId);
        String content = sanitize(rawContent);
        if (content.isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Message cannot be empty.");
        }
        if (content.length() > MAX_CONTENT_LENGTH) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Message is too long (max 1000 characters).");
        }

        userRepository.findById(receiverId)
                .filter(u -> u.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "User not found."));

        Message message = messageRepository.save(Message.builder()
                .senderId(senderId)
                .receiverId(receiverId)
                .content(content)
                .messageType(type)
                .build());

        MessageResponse response = MessageResponse.from(message);
        sessionRegistry.sendToUser(receiverId, new WsEvent<>("message", response));
        sessionRegistry.sendToUser(senderId, new WsEvent<>("message", response));

        // No live socket: reach the recipient's phone instead.
        if (!sessionRegistry.isOnline(receiverId)) {
            String senderName = userRepository.findById(senderId)
                    .map(User::getUsername).orElse("Someone");
            String preview = type == MessageType.GIFT
                    ? "\uD83C\uDF81 sent you a gift: " + content
                    : content.length() > 100 ? content.substring(0, 100) + "…" : content;
            pushNotificationService.notifyUser(receiverId, senderName, preview);
        }
        return response;
    }

    @Transactional
    public List<MessageResponse> getConversationMessages(UUID userId, UUID partnerId, Instant before) {
        List<Message> messages = before == null
                ? messageRepository.findConversation(userId, partnerId, PageRequest.of(0, PAGE_SIZE))
                : messageRepository.findConversationBefore(userId, partnerId, before, PageRequest.of(0, PAGE_SIZE));

        // Opening a conversation marks the partner's messages as read.
        messageRepository.markConversationRead(userId, partnerId, Instant.now());

        return messages.stream().map(MessageResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<ConversationResponse> getConversations(UUID userId) {
        List<Message> latest = messageRepository.findLatestMessagePerPartner(userId);
        if (latest.isEmpty()) {
            return List.of();
        }

        java.util.Set<UUID> blocked = blockService.blockedPartnerIds(userId);

        Map<UUID, Long> unread = messageRepository.countUnreadBySender(userId).stream()
                .collect(Collectors.toMap(row -> (UUID) row[0], row -> (Long) row[1]));

        List<UUID> partnerIds = latest.stream()
                .map(m -> partnerOf(m, userId))
                .toList();
        Map<UUID, User> partners = userRepository.findAllById(partnerIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        Map<UUID, ConversationResponse> byPartner = new HashMap<>();
        for (Message message : latest) {
            UUID partnerId = partnerOf(message, userId);
            User partner = partners.get(partnerId);
            if (partner == null || partner.getStatus() != UserStatus.ACTIVE
                    || blocked.contains(partnerId)) {
                continue;
            }
            byPartner.put(partnerId, new ConversationResponse(
                    partnerId,
                    partner.getUsername(),
                    partner.isOnline() || sessionRegistry.isOnline(partnerId),
                    avatarService.getForUser(partnerId),
                    MessageResponse.from(message),
                    unread.getOrDefault(partnerId, 0L)));
        }

        return byPartner.values().stream()
                .sorted((a, b) -> b.lastMessage().createdAt().compareTo(a.lastMessage().createdAt()))
                .toList();
    }

    private UUID partnerOf(Message message, UUID userId) {
        return message.getSenderId().equals(userId) ? message.getReceiverId() : message.getSenderId();
    }

    public static String sanitize(String content) {
        if (content == null) {
            return "";
        }
        // Keep newlines, strip all other control characters.
        return content.replaceAll("[\\p{Cntrl}&&[^\n]]", "").trim();
    }

    /** Envelope for everything pushed over the WebSocket. */
    public record WsEvent<T>(String type, T payload) {
    }
}
