package com.socialworld.app.language;

import com.socialworld.app.auth.AuthenticatedUser;
import com.socialworld.app.language.dto.AddLanguageRequest;
import com.socialworld.app.language.dto.UserLanguageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users/me/languages")
@RequiredArgsConstructor
@Tag(name = "Languages")
public class UserLanguageController {

    private final UserLanguageService userLanguageService;

    @GetMapping
    @Operation(summary = "List my languages")
    public List<UserLanguageResponse> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return userLanguageService.listForUser(user.id());
    }

    @PostMapping
    @Operation(summary = "Add a language (NATIVE, LEARNING or SPEAKING)")
    public ResponseEntity<UserLanguageResponse> add(@AuthenticationPrincipal AuthenticatedUser user,
                                                    @Valid @RequestBody AddLanguageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userLanguageService.add(user.id(), request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove one of my languages")
    public ResponseEntity<Void> remove(@AuthenticationPrincipal AuthenticatedUser user,
                                       @PathVariable UUID id) {
        userLanguageService.remove(user.id(), id);
        return ResponseEntity.noContent().build();
    }
}
