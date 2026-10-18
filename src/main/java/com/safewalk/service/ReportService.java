package com.safewalk.service;

import com.safewalk.dto.response.DashboardReportResponse;
import com.safewalk.dto.response.EmergencyResponse;
import com.safewalk.dto.response.EmergencyStatusReportResponse;
import com.safewalk.dto.response.EmergencySummaryReportResponse;
import com.safewalk.dto.response.EmergencyTypeReportResponse;
import com.safewalk.dto.response.UserStatsReportResponse;
import com.safewalk.entity.EmergencyStatus;
import com.safewalk.entity.EmergencyType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface ReportService {

    EmergencySummaryReportResponse getEmergencySummaryReport(LocalDateTime from, LocalDateTime to);

    EmergencyTypeReportResponse getEmergencyTypeReport(LocalDateTime from, LocalDateTime to);

    EmergencyStatusReportResponse getEmergencyStatusReport(LocalDateTime from, LocalDateTime to);

    UserStatsReportResponse getUserStatsReport();

    DashboardReportResponse getDashboardReport(LocalDateTime from, LocalDateTime to);

    Page<EmergencyResponse> getEmergenciesReport(EmergencyStatus status, EmergencyType type, LocalDateTime from, LocalDateTime to, Pageable pageable);
}
