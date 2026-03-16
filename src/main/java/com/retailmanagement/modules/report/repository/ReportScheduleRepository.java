package com.retailmanagement.modules.report.repository;

import com.retailmanagement.modules.report.model.ReportSchedule;
import com.retailmanagement.modules.report.enums.ReportType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReportScheduleRepository extends JpaRepository<ReportSchedule, Long> {

    Optional<ReportSchedule> findByScheduleId(String scheduleId);

    List<ReportSchedule> findByReportType(ReportType reportType);

    List<ReportSchedule> findByIsActiveTrue();

    @Query("SELECT rs FROM ReportSchedule rs WHERE rs.isActive = true AND rs.nextRunDate <= :date")
    List<ReportSchedule> findSchedulesDueForExecution(@Param("date") LocalDateTime date);

    List<ReportSchedule> findByCreatedById(Long userId);

    boolean existsByScheduleId(String scheduleId);
}