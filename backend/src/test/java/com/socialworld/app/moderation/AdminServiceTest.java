package com.socialworld.app.moderation;

import com.socialworld.app.common.exception.ApiException;
import com.socialworld.app.common.exception.ErrorCode;
import com.socialworld.app.moderation.dto.AdminReportResponse;
import com.socialworld.app.user.User;
import com.socialworld.app.user.UserRepository;
import com.socialworld.app.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private ReportRepository reportRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AdminService adminService;

    private User admin;
    private User regular;

    private User user(String name, boolean isAdmin) {
        return User.builder()
                .username(name).email(name + "@example.com").passwordHash("h")
                .birthDate(LocalDate.now().minusYears(30))
                .status(UserStatus.ACTIVE).admin(isAdmin).build();
    }

    @BeforeEach
    void setUp() {
        admin = user("admin", true);
        regular = user("bob", false);
        lenient().when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        lenient().when(userRepository.findById(regular.getId())).thenReturn(Optional.of(regular));
    }

    @Test
    void nonAdminGetsNotFoundNotForbidden() {
        assertThatThrownBy(() -> adminService.listReports(regular.getId(), null))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getCode())
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }

    @Test
    void updateReportStatus_closesReport() {
        Report report = Report.builder()
                .reporterId(regular.getId()).reportedUserId(admin.getId())
                .reason(ReportReason.SPAM).status(ReportStatus.OPEN).build();
        when(reportRepository.findById(report.getId())).thenReturn(Optional.of(report));

        AdminReportResponse response = adminService.updateReportStatus(
                admin.getId(), report.getId(), ReportStatus.CLOSED);

        assertThat(response.status()).isEqualTo(ReportStatus.CLOSED);
        assertThat(report.getStatus()).isEqualTo(ReportStatus.CLOSED);
    }

    @Test
    void updateReportStatus_rejectsReopening() {
        assertThatThrownBy(() -> adminService.updateReportStatus(
                admin.getId(), UUID.randomUUID(), ReportStatus.OPEN))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getCode())
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void suspend_setsStatusAndKnocksOffline() {
        regular.setOnline(true);

        adminService.setUserStatus(admin.getId(), regular.getId(), UserStatus.SUSPENDED);

        assertThat(regular.getStatus()).isEqualTo(UserStatus.SUSPENDED);
        assertThat(regular.isOnline()).isFalse();
    }

    @Test
    void suspend_cannotTargetAdminsOrUseDeleted() {
        assertThatThrownBy(() -> adminService.setUserStatus(admin.getId(), admin.getId(), UserStatus.SUSPENDED))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> adminService.setUserStatus(admin.getId(), regular.getId(), UserStatus.DELETED))
                .isInstanceOf(ApiException.class);
    }
}
