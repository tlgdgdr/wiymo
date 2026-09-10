package com.socialworld.app.gift;

import com.socialworld.app.auth.AuthenticatedUser;
import com.socialworld.app.gift.dto.GiftResponse;
import com.socialworld.app.gift.dto.SendGiftRequest;
import com.socialworld.app.gift.dto.SendGiftResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/gifts")
@RequiredArgsConstructor
@Tag(name = "Gifts")
public class GiftController {

    private final GiftService giftService;

    @GetMapping
    @Operation(summary = "Gift catalog")
    public List<GiftResponse> listGifts() {
        return giftService.listGifts();
    }

    @PostMapping("/send")
    @Operation(summary = "Send a gift: deducts coins, records the transaction, drops a GIFT chat message")
    public ResponseEntity<SendGiftResponse> send(@AuthenticationPrincipal AuthenticatedUser user,
                                                 @Valid @RequestBody SendGiftRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(giftService.sendGift(user.id(), request));
    }
}
