package com.socialworld.app.wallet;

import com.socialworld.app.common.exception.ApiException;
import com.socialworld.app.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @InjectMocks
    private WalletService walletService;

    private final UUID userId = UUID.randomUUID();

    @Test
    void getOrCreate_newWalletGetsWelcomeCoins() {
        when(walletRepository.findById(userId)).thenReturn(Optional.empty());
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

        Wallet wallet = walletService.getOrCreate(userId);

        assertThat(wallet.getCoinBalance()).isEqualTo(WalletService.WELCOME_COINS);
    }

    @Test
    void deduct_lowersBalance() {
        Wallet wallet = Wallet.builder().userId(userId).coinBalance(100).build();
        when(walletRepository.findById(userId)).thenReturn(Optional.of(wallet));
        when(walletRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.of(wallet));

        Wallet result = walletService.deduct(userId, 30);

        assertThat(result.getCoinBalance()).isEqualTo(70);
    }

    @Test
    void deduct_neverGoesNegative() {
        Wallet wallet = Wallet.builder().userId(userId).coinBalance(20).build();
        when(walletRepository.findById(userId)).thenReturn(Optional.of(wallet));
        when(walletRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.of(wallet));

        assertThatThrownBy(() -> walletService.deduct(userId, 30))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getCode())
                .isEqualTo(ErrorCode.INSUFFICIENT_BALANCE);
        assertThat(wallet.getCoinBalance()).isEqualTo(20);
    }

    @Test
    void deduct_rejectsNonPositiveAmounts() {
        assertThatThrownBy(() -> walletService.deduct(userId, 0))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getCode())
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }
}
