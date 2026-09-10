package com.socialworld.app.user;

import com.socialworld.app.auth.AuthenticatedUser;
import com.socialworld.app.user.dto.MeResponse;
import com.socialworld.app.user.dto.PublicProfileResponse;
import com.socialworld.app.user.dto.UpdateIntentionRequest;
import com.socialworld.app.user.dto.UpdateProfileRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "My account")
    public MeResponse me(@AuthenticationPrincipal AuthenticatedUser user) {
        return userService.getMe(user.id());
    }

    @PutMapping("/me")
    @Operation(summary = "Update my profile (bio, country, gender)")
    public MeResponse updateProfile(@AuthenticationPrincipal AuthenticatedUser user,
                                    @Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(user.id(), request);
    }

    @PutMapping("/me/intention")
    @Operation(summary = "Change what I'm in the mood for")
    public MeResponse updateIntention(@AuthenticationPrincipal AuthenticatedUser user,
                                      @Valid @RequestBody UpdateIntentionRequest request) {
        return userService.updateIntention(user.id(), request.intention());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Another user's public profile (age, never birth date)")
    public PublicProfileResponse publicProfile(@PathVariable UUID id) {
        return userService.getPublicProfile(id);
    }
}
