package com.socialworld.app.wallet;

import com.socialworld.app.auth.AuthenticatedUser;
import com.socialworld.app.wallet.dto.WalletResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wallets")
@RequiredArgsConstructor
@Tag(name = "Wallet")
public class WalletController {

    private final WalletService walletService;

    @GetMapping("/me")
    @Operation(summary = "My coin balance (wallet auto-created with the welcome credit)")
    public WalletResponse myWallet(@AuthenticationPrincipal AuthenticatedUser user) {
        return WalletResponse.from(walletService.getOrCreate(user.id()));
    }
}
