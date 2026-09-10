package com.socialworld.app.wallet;

import com.socialworld.app.common.exception.ApiException;
import com.socialworld.app.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WalletService {

    /** Demo credit for the MVP; real coin purchases come much later. */
    public static final long WELCOME_COINS = 100;

    private final WalletRepository walletRepository;

    /** Read-only view; creates the wallet with the welcome credit on first touch. */
    @Transactional
    public Wallet getOrCreate(UUID userId) {
        return walletRepository.findById(userId)
                .orElseGet(() -> walletRepository.save(Wallet.builder()
                        .userId(userId)
                        .coinBalance(WELCOME_COINS)
                        .build()));
    }

    /**
     * Deducts under a row lock; must run inside the caller's transaction.
     * Throws INSUFFICIENT_BALANCE rather than ever going negative.
     */
    @Transactional
    public Wallet deduct(UUID userId, long amount) {
        if (amount <= 0) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Amount must be positive.");
        }
        // Ensure the row exists before locking it.
        getOrCreate(userId);
        Wallet wallet = walletRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Wallet not found."));
        if (wallet.getCoinBalance() < amount) {
            throw new ApiException(ErrorCode.INSUFFICIENT_BALANCE);
        }
        wallet.setCoinBalance(wallet.getCoinBalance() - amount);
        return wallet;
    }
}
