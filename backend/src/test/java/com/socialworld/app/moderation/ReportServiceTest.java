package com.socialworld.app.moderation;

import com.socialworld.app.common.exception.ApiException;
import com.socialworld.app.common.exception.ErrorCode;
import com.socialworld.app.moderation.dto.CreateReportRequest;
import com.socialworld.app.moderation.dto.ReportResponse;
import com.socialworld.app.user.User;
import com.socialworld.app.user.UserRepository;
import com.socialworld.app.user.UserStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private ReportRepository reportRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ReportService reportService;

    private final UUID reporterId = UUID.randomUUID();
    private final UUID reportedId = UUID.randomUUID();

    private void reportedExists() {
        when(userRepository.findById(reportedId)).thenReturn(Optional.of(User.builder()
                .username("bob").email("b@example.com").passwordHash("h")
                .birthDate(LocalDate.now().minusYears(25)).status(UserStatus.ACTIVE).build()));
    }

    @Test
    void create_savesOpenReportWithSanitizedDescription() {
        reportedExists();
        when(reportRepository.existsByReporterIdAndReportedUserIdAndStatus(
                reporterId, reportedId, ReportStatus.OPEN)).thenReturn(false);
        when(reportRepository.save(any(Report.class))).thenAnswer(inv -> inv.getArgument(0));

        ReportResponse response = reportService.create(reporterId,
                new CreateReportRequest(reportedId, ReportReason.HARASSMENT, "  rude messages  "));

        assertThat(response.status()).isEqualTo(ReportStatus.OPEN);
        assertThat(response.reason()).isEqualTo(ReportReason.HARASSMENT);
    }

    @Test
    void create_rejectsSelfReport() {
        assertThatThrownBy(() -> reportService.create(reporterId,
                new CreateReportRequest(reporterId, ReportReason.SPAM, null)))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getCode())
                .isEqualTo(ErrorCode.SELF_ACTION_NOT_ALLOWED);
        verify(reportRepository, never()).save(any());
    }

    @Test
    void create_rejectsSecondOpenReportForSamePair() {
        reportedExists();
        when(reportRepository.existsByReporterIdAndReportedUserIdAndStatus(
                reporterId, reportedId, ReportStatus.OPEN)).thenReturn(true);

        assertThatThrownBy(() -> reportService.create(reporterId,
                new CreateReportRequest(reportedId, ReportReason.SPAM, null)))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getCode())
                .isEqualTo(ErrorCode.REPORT_ALREADY_OPEN);
    }

    @Test
    void create_rejectsUnknownUser() {
        when(userRepository.findById(reportedId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reportService.create(reporterId,
                new CreateReportRequest(reportedId, ReportReason.SPAM, null)))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getCode())
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }
}
