package com.socialworld.app.friendship;

import com.socialworld.app.auth.AuthenticatedUser;
import com.socialworld.app.friendship.dto.ConnectionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/connections")
@RequiredArgsConstructor
@Tag(name = "Connections")
public class ConnectionController {

    private final ConnectionService connectionService;

    @PostMapping("/{userId}")
    @Operation(summary = "Request a connection with a user")
    public ResponseEntity<ConnectionResponse> request(@AuthenticationPrincipal AuthenticatedUser user,
                                                      @PathVariable UUID userId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(connectionService.request(user.id(), userId));
    }

    @PostMapping("/{id}/accept")
    @Operation(summary = "Accept an incoming pending request")
    public ConnectionResponse accept(@AuthenticationPrincipal AuthenticatedUser user,
                                     @PathVariable UUID id) {
        return connectionService.accept(user.id(), id);
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject an incoming pending request")
    public ConnectionResponse reject(@AuthenticationPrincipal AuthenticatedUser user,
                                     @PathVariable UUID id) {
        return connectionService.reject(user.id(), id);
    }

    @GetMapping
    @Operation(summary = "My pending and accepted connections")
    public List<ConnectionResponse> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return connectionService.list(user.id());
    }
}
