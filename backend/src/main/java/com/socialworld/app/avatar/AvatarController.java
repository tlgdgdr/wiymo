package com.socialworld.app.avatar;

import com.socialworld.app.auth.AuthenticatedUser;
import com.socialworld.app.avatar.dto.AvatarAssetResponse;
import com.socialworld.app.avatar.dto.AvatarResponse;
import com.socialworld.app.avatar.dto.UpdateAvatarRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Avatar")
public class AvatarController {

    private final AvatarService avatarService;

    @GetMapping("/api/avatar/assets")
    @Operation(summary = "All enabled avatar assets, grouped by category and sort order")
    public List<AvatarAssetResponse> listAssets() {
        return avatarService.listAssets();
    }

    @GetMapping("/api/users/me/avatar")
    @Operation(summary = "My avatar (defaults until customized)")
    public AvatarResponse myAvatar(@AuthenticationPrincipal AuthenticatedUser user) {
        return avatarService.getForUser(user.id());
    }

    @PutMapping("/api/users/me/avatar")
    @Operation(summary = "Replace my avatar; body layer is required")
    public AvatarResponse updateAvatar(@AuthenticationPrincipal AuthenticatedUser user,
                                       @Valid @RequestBody UpdateAvatarRequest request) {
        return avatarService.update(user.id(), request);
    }
}
