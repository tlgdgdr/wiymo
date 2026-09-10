package com.socialworld.app.moderation;

import com.socialworld.app.auth.AuthenticatedUser;
import com.socialworld.app.moderation.dto.AdminReportResponse;
import com.socialworld.app.user.UserStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Requires users.is_admin (set directly in the database). */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/reports")
    @Operation(summary = "Reports by status (default OPEN)")
    public List<AdminReportResponse> reports(@AuthenticationPrincipal AuthenticatedUser user,
                                             @RequestParam(required = false) ReportStatus status) {
        return adminService.listReports(user.id(), status);
    }

    @PostMapping("/reports/{id}/status")
    @Operation(summary = "Mark a report REVIEWED or CLOSED")
    public AdminReportResponse updateReport(@AuthenticationPrincipal AuthenticatedUser user,
                                            @PathVariable UUID id,
                                            @RequestParam ReportStatus status) {
        return adminService.updateReportStatus(user.id(), id, status);
    }

    @PostMapping("/users/{id}/status")
    @Operation(summary = "Suspend or reactivate a user")
    public ResponseEntity<Void> setUserStatus(@AuthenticationPrincipal AuthenticatedUser user,
                                              @PathVariable UUID id,
                                              @RequestParam UserStatus status) {
        adminService.setUserStatus(user.id(), id, status);
        return ResponseEntity.noContent().build();
    }
}
