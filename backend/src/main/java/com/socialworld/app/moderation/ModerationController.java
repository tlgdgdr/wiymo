package com.socialworld.app.moderation;

import com.socialworld.app.auth.AuthenticatedUser;
import com.socialworld.app.moderation.dto.CreateReportRequest;
import com.socialworld.app.moderation.dto.ReportResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Moderation")
public class ModerationController {

    private final BlockService blockService;
    private final ReportService reportService;

    @PostMapping("/api/users/{id}/block")
    @Operation(summary = "Block a user (idempotent); cuts chat, gifts, profiles, discovery, rooms")
    public ResponseEntity<Void> block(@AuthenticationPrincipal AuthenticatedUser user,
                                      @PathVariable UUID id) {
        blockService.block(user.id(), id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/api/users/{id}/block")
    @Operation(summary = "Remove my block on a user")
    public ResponseEntity<Void> unblock(@AuthenticationPrincipal AuthenticatedUser user,
                                        @PathVariable UUID id) {
        blockService.unblock(user.id(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/reports")
    @Operation(summary = "Report a user for moderation review")
    public ResponseEntity<ReportResponse> report(@AuthenticationPrincipal AuthenticatedUser user,
                                                 @Valid @RequestBody CreateReportRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reportService.create(user.id(), request));
    }
}
