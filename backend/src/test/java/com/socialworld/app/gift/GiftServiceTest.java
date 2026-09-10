package com.socialworld.app.gift;

import com.socialworld.app.chat.ChatService;
import com.socialworld.app.chat.MessageType;
import com.socialworld.app.chat.ws.ChatSessionRegistry;
import com.socialworld.app.common.exception.ApiException;
import com.socialworld.app.common.exception.ErrorCode;
import com.socialworld.app.gift.dto.SendGiftRequest;
import com.socialworld.app.gift.dto.SendGiftResponse;
import com.socialworld.app.user.User;
import com.socialworld.app.user.UserRepository;
import com.socialworld.app.user.UserStatus;
import com.socialworld.app.wallet.Wallet;
import com.socialworld.app.wallet.WalletService;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GiftServiceTest {

    @Mock
    private GiftRepository giftRepository;
    @Mock
    private GiftTransactionRepository giftTransactionRepository;
    @Mock
    private WalletService walletService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ChatService chatService;
    @Mock
    private ChatSessionRegistry sessionRegistry;

    @InjectMocks
    private GiftService giftService;

    private final UUID senderId = UUID.randomUUID();
    private User receiver;
    private Gift rose;

    @BeforeEach
    void setUp() {
        receiver = User.builder()
                .username("bob")
                .email("bob@example.com")
                .passwordHash("hash")
                .birthDate(LocalDate.now().minusYears(25))
                .status(UserStatus.ACTIVE)
                .build();
        rose = Gift.builder().name("Rose").iconUrl("https://example.com/rose.png")
                .coinPrice(30).category(GiftCategory.ROMANTIC).build();
        lenient().when(userRepository.findById(receiver.getId())).thenReturn(Optional.of(receiver));
        lenient().when(giftRepository.findByIdAndEnabledTrue(rose.getId())).thenReturn(Optional.of(rose));
    }

    @Test
    void send_deductsRecordsMessagesAndNotifies() {
        Wallet wallet = Wallet.builder().userId(senderId).coinBalance(70).build();
        when(walletService.deduct(senderId, 30)).thenReturn(wallet);
        when(giftTransactionRepository.save(any(GiftTransaction.class))).thenAnswer(inv -> inv.getArgument(0));

        SendGiftResponse response = giftService.sendGift(senderId,
                new SendGiftRequest(receiver.getId(), rose.getId()));

        assertThat(response.giftName()).isEqualTo("Rose");
        assertThat(response.coinAmount()).isEqualTo(30);
        assertThat(response.newBalance()).isEqualTo(70);
        verify(chatService).sendMessage(senderId, receiver.getId(), "Rose", MessageType.GIFT);
        verify(sessionRegistry).sendToUser(eq(receiver.getId()), any());
        verify(giftTransactionRepository).save(any(GiftTransaction.class));
    }

    @Test
    void send_insufficientBalanceAbortsBeforeRecording() {
        when(walletService.deduct(senderId, 30))
                .thenThrow(new ApiException(ErrorCode.INSUFFICIENT_BALANCE));

        assertThatThrownBy(() -> giftService.sendGift(senderId,
                new SendGiftRequest(receiver.getId(), rose.getId())))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getCode())
                .isEqualTo(ErrorCode.INSUFFICIENT_BALANCE);
        verify(giftTransactionRepository, never()).save(any());
        verify(chatService, never()).sendMessage(any(), any(), any(), any());
    }

    @Test
    void send_rejectsSelfGift() {
        assertThatThrownBy(() -> giftService.sendGift(senderId,
                new SendGiftRequest(senderId, rose.getId())))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getCode())
                .isEqualTo(ErrorCode.SELF_ACTION_NOT_ALLOWED);
        verify(walletService, never()).deduct(any(), org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void send_rejectsUnknownOrDisabledGift() {
        UUID unknownGift = UUID.randomUUID();
        when(giftRepository.findByIdAndEnabledTrue(unknownGift)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> giftService.sendGift(senderId,
                new SendGiftRequest(receiver.getId(), unknownGift)))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getCode())
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }

    @Test
    void send_rejectsInactiveReceiver() {
        receiver.setStatus(UserStatus.SUSPENDED);

        assertThatThrownBy(() -> giftService.sendGift(senderId,
                new SendGiftRequest(receiver.getId(), rose.getId())))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getCode())
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }
}
