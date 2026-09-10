package com.socialworld.app.moderation;

import com.socialworld.app.chat.ChatService;
import com.socialworld.app.common.exception.ApiException;
import com.socialworld.app.common.exception.ErrorCode;
import com.socialworld.app.moderation.dto.CreateReportRequest;
import com.socialworld.app.moderation.dto.ReportResponse;
import com.socialworld.app.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;

    @Transactional
    public ReportResponse create(UUID reporterId, CreateReportRequest request) {
        if (reporterId.equals(request.reportedUserId())) {
            throw new ApiException(ErrorCode.SELF_ACTION_NOT_ALLOWED, "You cannot report yourself.");
        }
        userRepository.findById(request.reportedUserId())
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "User not found."));

        // One open report per pair keeps the queue clean; the reporter still
        // gets a success shape so they know it's filed.
        if (reportRepository.existsByReporterIdAndReportedUserIdAndStatus(
                reporterId, request.reportedUserId(), ReportStatus.OPEN)) {
            throw new ApiException(ErrorCode.REPORT_ALREADY_OPEN);
        }

        Report report = reportRepository.save(Report.builder()
                .reporterId(reporterId)
                .reportedUserId(request.reportedUserId())
                .reason(request.reason())
                .description(request.description() == null || request.description().isBlank()
                        ? null : ChatService.sanitize(request.description()))
                .status(ReportStatus.OPEN)
                .build());
        return ReportResponse.from(report);
    }
}
