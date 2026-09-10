package com.socialworld.app.moderation.dto;

import com.socialworld.app.moderation.Report;
import com.socialworld.app.moderation.ReportReason;
import com.socialworld.app.moderation.ReportStatus;

import java.time.Instant;
import java.util.UUID;

public record ReportResponse(
        UUID id,
        UUID reportedUserId,
        ReportReason reason,
        ReportStatus status,
        Instant createdAt
) {
    public static ReportResponse from(Report report) {
        return new ReportResponse(
                report.getId(),
                report.getReportedUserId(),
                report.getReason(),
                report.getStatus(),
                report.getCreatedAt());
    }
}
