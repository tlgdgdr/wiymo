package com.socialworld.app.moderation.dto;

import com.socialworld.app.moderation.Report;
import com.socialworld.app.moderation.ReportReason;
import com.socialworld.app.moderation.ReportStatus;

import java.time.Instant;
import java.util.UUID;

public record AdminReportResponse(
        UUID id,
        UUID reporterId,
        String reporterUsername,
        UUID reportedUserId,
        String reportedUsername,
        ReportReason reason,
        String description,
        ReportStatus status,
        Instant createdAt
) {
    public static AdminReportResponse from(Report report, String reporterUsername, String reportedUsername) {
        return new AdminReportResponse(
                report.getId(),
                report.getReporterId(),
                reporterUsername,
                report.getReportedUserId(),
                reportedUsername,
                report.getReason(),
                report.getDescription(),
                report.getStatus(),
                report.getCreatedAt());
    }
}
