package com.socialworld.app.wallet.dto;

import com.socialworld.app.wallet.Wallet;

public record WalletResponse(long coinBalance) {

    public static WalletResponse from(Wallet wallet) {
        return new WalletResponse(wallet.getCoinBalance());
    }
}
