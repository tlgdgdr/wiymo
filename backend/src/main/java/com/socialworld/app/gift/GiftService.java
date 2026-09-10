package com.socialworld.app.gift;

import com.socialworld.app.chat.ChatService;
import com.socialworld.app.chat.MessageType;
import com.socialworld.app.chat.dto.MessageResponse;
import com.socialworld.app.chat.ws.ChatSessionRegistry;
import com.socialworld.app.common.exception.ApiException;
import com.socialworld.app.common.exception.ErrorCode;
import com.socialworld.app.moderation.BlockService;
import com.socialworld.app.gift.dto.GiftResponse;
import com.socialworld.app.gift.dto.SendGiftRequest;
import com.socialworld.app.gift.dto.SendGiftResponse;
import com.socialworld.app.user.UserRepository;
import com.socialworld.app.user.UserStatus;
import com.socialworld.app.wallet.Wallet;
import com.socialworld.app.wallet.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GiftService {

    private final GiftRepository giftRepository;
    private final GiftTransactionRepository giftTransactionRepository;
    private final WalletService walletService;
    private final UserRepository userRepository;
    private final ChatService chatService;
    private final ChatSessionRegistry sessionRegistry;
    private final BlockService blockService;

    @Transactional(readOnly = true)
    public List<GiftResponse> listGifts() {
        return giftRepository.findByEnabledTrueOrderBySortOrder().stream()
                .map(GiftResponse::from)
                .toList();
    }

    /**
     * One database transaction: lock wallet, deduct, record, drop a GIFT
     * message into the chat, then notify the recipient's live socket.
     */
    @Transactional
    public SendGiftResponse sendGift(UUID senderId, SendGiftRequest request) {
        if (senderId.equals(request.receiverId())) {
            throw new ApiException(ErrorCode.SELF_ACTION_NOT_ALLOWED, "You cannot send a gift to yourself.");
        }
        blockService.assertInteractionAllowed(senderId, request.receiverId());
        userRepository.findById(request.receiverId())
                .filter(u -> u.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "User not found."));

        Gift gift = giftRepository.findByIdAndEnabledTrue(request.giftId())
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Gift not found."));

        Wallet wallet = walletService.deduct(senderId, gift.getCoinPrice());

        GiftTransaction transaction = giftTransactionRepository.save(GiftTransaction.builder()
                .senderId(senderId)
                .receiverId(request.receiverId())
                .giftId(gift.getId())
                .coinAmount(gift.getCoinPrice())
                .build());

        MessageResponse message = chatService.sendMessage(
                senderId, request.receiverId(), gift.getName(), MessageType.GIFT);

        sessionRegistry.sendToUser(request.receiverId(), new ChatService.WsEvent<>("gift", Map.of(
                "transactionId", transaction.getId(),
                "giftId", gift.getId(),
                "giftName", gift.getName(),
                "iconUrl", gift.getIconUrl(),
                "senderId", senderId)));

        return new SendGiftResponse(
                transaction.getId(),
                gift.getId(),
                gift.getName(),
                gift.getCoinPrice(),
                wallet.getCoinBalance(),
                message);
    }
}
