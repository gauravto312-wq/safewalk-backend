package com.safewalk.controller;

import com.safewalk.dto.response.ApiResponse;
import com.safewalk.dto.response.DashboardReportResponse;
import com.safewalk.dto.response.EmergencyResponse;
import com.safewalk.dto.response.EmergencyStatusReportResponse;
import com.safewalk.dto.response.EmergencySummaryReportResponse;
import com.safewalk.dto.response.EmergencyTypeReportResponse;
import com.safewalk.dto.response.UserStatsReportResponse;
import com.safewalk.entity.EmergencyStatus;
import com.safewalk.entity.EmergencyType;
import com.safewalk.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private final ReportService reportService;

    @Autowired
    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/emergencies/summary")
    public ResponseEntity<ApiResponse<EmergencySummaryReportResponse>> getEmergencySummaryReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        EmergencySummaryReportResponse response = reportService.getEmergencySummaryReport(from, to);
        return ResponseEntity.ok(ApiResponse.success("Emergency summary report generated successfully", response));
    }

    @GetMapping("/emergencies/types")
    public ResponseEntity<ApiResponse<EmergencyTypeReportResponse>> getEmergencyTypeReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        EmergencyTypeReportResponse response = reportService.getEmergencyTypeReport(from, to);
        return ResponseEntity.ok(ApiResponse.success("Emergency type report generated successfully", response));
    }

    @GetMapping("/emergencies/status")
    public ResponseEntity<ApiResponse<EmergencyStatusReportResponse>> getEmergencyStatusReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        EmergencyStatusReportResponse response = reportService.getEmergencyStatusReport(from, to);
        return ResponseEntity.ok(ApiResponse.success("Emergency status report generated successfully", response));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<UserStatsReportResponse>> getUserStatsReport() {
        UserStatsReportResponse response = reportService.getUserStatsReport();
        return ResponseEntity.ok(ApiResponse.success("User statistics report generated successfully", response));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<DashboardReportResponse>> getDashboardReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        DashboardReportResponse response = reportService.getDashboardReport(from, to);
        return ResponseEntity.ok(ApiResponse.success("Dashboard report generated successfully", response));
    }

    @GetMapping("/emergencies")
    public ResponseEntity<ApiResponse<Page<EmergencyResponse>>> getEmergenciesReport(
            @RequestParam(required = false) EmergencyStatus status,
            @RequestParam(required = false) EmergencyType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<EmergencyResponse> response = reportService.getEmergenciesReport(status, type, from, to, pageable);
        return ResponseEntity.ok(ApiResponse.success("Emergencies report retrieved successfully", response));
    }
}
