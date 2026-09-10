package com.socialworld.app.moderation;

import com.socialworld.app.common.exception.ApiException;
import com.socialworld.app.common.exception.ErrorCode;
import com.socialworld.app.moderation.dto.AdminReportResponse;
import com.socialworld.app.user.User;
import com.socialworld.app.user.UserRepository;
import com.socialworld.app.user.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Just enough moderation tooling for store review requirements: list
 * reports, resolve them, suspend/reactivate users. Admins are flagged
 * directly in the database (users.is_admin), never via API.
 */
@Service
@RequiredArgsConstructor
public class AdminService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<AdminReportResponse> listReports(UUID adminId, ReportStatus status) {
        requireAdmin(adminId);
        List<Report> reports = reportRepository.findByStatusOrderByCreatedAtDesc(
                status == null ? ReportStatus.OPEN : status);

        Map<UUID, String> usernames = userRepository.findAllById(
                        reports.stream()
                                .flatMap(r -> Stream.of(r.getReporterId(), r.getReportedUserId()))
                                .distinct()
                                .toList()).stream()
                .collect(Collectors.toMap(User::getId, User::getUsername, (a, b) -> a));

        return reports.stream()
                .map(r -> AdminReportResponse.from(r,
                        usernames.getOrDefault(r.getReporterId(), "?"),
                        usernames.getOrDefault(r.getReportedUserId(), "?")))
                .toList();
    }

    @Transactional
    public AdminReportResponse updateReportStatus(UUID adminId, UUID reportId, ReportStatus status) {
        requireAdmin(adminId);
        if (status == null || status == ReportStatus.OPEN) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "status: must be REVIEWED or CLOSED");
        }
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Report not found."));
        report.setStatus(status);
        return AdminReportResponse.from(report,
                username(report.getReporterId()), username(report.getReportedUserId()));
    }

    @Transactional
    public void setUserStatus(UUID adminId, UUID targetId, UserStatus status) {
        requireAdmin(adminId);
        if (status != UserStatus.ACTIVE && status != UserStatus.SUSPENDED) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "status: must be ACTIVE or SUSPENDED");
        }
        User target = userRepository.findById(targetId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "User not found."));
        if (target.isAdmin()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Admins cannot be suspended via API.");
        }
        if (target.getStatus() == UserStatus.DELETED) {
            throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "User not found.");
        }
        target.setStatus(status);
        if (status == UserStatus.SUSPENDED) {
            target.setOnline(false);
        }
    }

    private void requireAdmin(UUID userId) {
        boolean isAdmin = userRepository.findById(userId)
                .map(User::isAdmin)
                .orElse(false);
        if (!isAdmin) {
            // 404, not 403: non-admins should not learn these endpoints exist.
            throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Not found.");
        }
    }

    private String username(UUID id) {
        return userRepository.findById(id).map(User::getUsername).orElse("?");
    }
}
