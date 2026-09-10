package com.socialworld.app.chat;

import com.socialworld.app.avatar.AvatarService;
import com.socialworld.app.chat.dto.MessageResponse;
import com.socialworld.app.chat.ws.ChatSessionRegistry;
import com.socialworld.app.common.exception.ApiException;
import com.socialworld.app.common.exception.ErrorCode;
import com.socialworld.app.user.User;
import com.socialworld.app.user.UserRepository;
import com.socialworld.app.user.UserStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private MessageRepository messageRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AvatarService avatarService;
    @Mock
    private ChatSessionRegistry sessionRegistry;
    @Mock
    private com.socialworld.app.moderation.BlockService blockService;

    @InjectMocks
    private ChatService chatService;

    private final UUID senderId = UUID.randomUUID();

    private User activeReceiver() {
        return User.builder()
                .username("bob")
                .email("bob@example.com")
                .passwordHash("hash")
                .birthDate(LocalDate.now().minusYears(25))
                .status(UserStatus.ACTIVE)
                .build();
    }

    @Test
    void send_persistsAndPushesToBothParticipants() {
        User receiver = activeReceiver();
        when(userRepository.findById(receiver.getId())).thenReturn(Optional.of(receiver));
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> inv.getArgument(0));

        MessageResponse response = chatService.sendMessage(
                senderId, receiver.getId(), "  hey there  ", MessageType.TEXT);

        assertThat(response.content()).isEqualTo("hey there");
        assertThat(response.senderId()).isEqualTo(senderId);
        verify(sessionRegistry).sendToUser(eq(receiver.getId()), any());
        verify(sessionRegistry).sendToUser(eq(senderId), any());
    }

    @Test
    void send_rejectsSelfMessage() {
        assertThatThrownBy(() -> chatService.sendMessage(senderId, senderId, "hi", MessageType.TEXT))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getCode())
                .isEqualTo(ErrorCode.SELF_ACTION_NOT_ALLOWED);
        verify(messageRepository, never()).save(any());
    }

    @Test
    void send_rejectsEmptyAndWhitespaceOnly() {
        UUID receiverId = UUID.randomUUID();
        for (String content : new String[]{"", "   ", "\n\n", "\u0000\u0007"}) {
            assertThatThrownBy(() -> chatService.sendMessage(senderId, receiverId, content, MessageType.TEXT))
                    .isInstanceOf(ApiException.class)
                    .extracting(e -> ((ApiException) e).getCode())
                    .isEqualTo(ErrorCode.VALIDATION_ERROR);
        }
        verify(messageRepository, never()).save(any());
    }

    @Test
    void send_rejectsTooLongMessage() {
        assertThatThrownBy(() -> chatService.sendMessage(
                senderId, UUID.randomUUID(), "x".repeat(1001), MessageType.TEXT))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getCode())
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void send_rejectsInactiveReceiver() {
        User receiver = activeReceiver();
        receiver.setStatus(UserStatus.SUSPENDED);
        when(userRepository.findById(receiver.getId())).thenReturn(Optional.of(receiver));

        assertThatThrownBy(() -> chatService.sendMessage(senderId, receiver.getId(), "hi", MessageType.TEXT))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getCode())
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }

    @Test
    void sanitize_stripsControlCharsButKeepsNewlines() {
        assertThat(ChatService.sanitize("ab\u0007c\nd\r")).isEqualTo("abc\nd");
        assertThat(ChatService.sanitize("  hello  ")).isEqualTo("hello");
    }

    @Test
    void getConversationMessages_marksIncomingAsRead() {
        UUID partnerId = UUID.randomUUID();
        when(messageRepository.findConversation(eq(senderId), eq(partnerId), any()))
                .thenReturn(java.util.List.of());

        chatService.getConversationMessages(senderId, partnerId, null);

        verify(messageRepository, times(1)).markConversationRead(eq(senderId), eq(partnerId), any());
    }
}
