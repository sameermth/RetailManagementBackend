package com.retailmanagement.modules.report.repository;

import com.retailmanagement.modules.report.model.Report;
import com.retailmanagement.modules.report.enums.ReportType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    Optional<Report> findByReportId(String reportId);

    List<Report> findByReportType(ReportType reportType);

    Page<Report> findByReportType(ReportType reportType, Pageable pageable);

    List<Report> findByGeneratedByUserId(Long userId);

    Page<Report> findByGeneratedByUserId(Long userId, Pageable pageable);

    List<Report> findByGeneratedDateBetween(LocalDateTime startDate, LocalDateTime endDate);

    @Query("SELECT r FROM Report r WHERE r.isScheduled = true AND r.nextScheduledDate <= :date")
    List<Report> findScheduledReportsDue(@Param("date") LocalDateTime date);

    @Query("SELECT COUNT(r) FROM Report r WHERE r.generatedDate BETWEEN :startDate AND :endDate")
    Long countReportsGenerated(@Param("startDate") LocalDateTime startDate,
                               @Param("endDate") LocalDateTime endDate);

    boolean existsByReportId(String reportId);
}