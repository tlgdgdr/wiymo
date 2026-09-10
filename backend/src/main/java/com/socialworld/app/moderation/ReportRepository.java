package com.socialworld.app.moderation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReportRepository extends JpaRepository<Report, UUID> {

    boolean existsByReporterIdAndReportedUserIdAndStatus(UUID reporterId, UUID reportedUserId,
                                                         ReportStatus status);

    List<Report> findByStatusOrderByCreatedAtDesc(ReportStatus status);
}
