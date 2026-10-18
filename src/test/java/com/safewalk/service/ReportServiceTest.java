package com.safewalk.service;

import com.safewalk.dto.response.DashboardReportResponse;
import com.safewalk.dto.response.EmergencyStatusReportResponse;
import com.safewalk.dto.response.EmergencySummaryReportResponse;
import com.safewalk.dto.response.EmergencyTypeReportResponse;
import com.safewalk.dto.response.UserStatsReportResponse;
import com.safewalk.entity.AccountStatus;
import com.safewalk.entity.EmergencyStatus;
import com.safewalk.entity.EmergencyType;
import com.safewalk.entity.Role;
import com.safewalk.mapper.EmergencyMapper;
import com.safewalk.repository.EmergencyRepository;
import com.safewalk.repository.UserRepository;
import com.safewalk.service.impl.ReportServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private EmergencyRepository emergencyRepository;

    @Mock
    private UserRepository userRepository;

    private EmergencyMapper emergencyMapper;
    private ReportServiceImpl reportService;

    @BeforeEach
    void setUp() {
        emergencyMapper = new EmergencyMapper();
        reportService = new ReportServiceImpl(emergencyRepository, userRepository, emergencyMapper);
    }

    @Test
    void getEmergencySummaryReport_ReturnsValidSummary() {
        when(emergencyRepository.count()).thenReturn(20L);
        when(emergencyRepository.countByStatus(EmergencyStatus.ACTIVE)).thenReturn(5L);
        when(emergencyRepository.countByStatus(EmergencyStatus.ACCEPTED)).thenReturn(3L);
        when(emergencyRepository.countByStatus(EmergencyStatus.CANCELLED)).thenReturn(2L);
        when(emergencyRepository.countByStatus(EmergencyStatus.RESOLVED)).thenReturn(10L);

        EmergencySummaryReportResponse summary = reportService.getEmergencySummaryReport(null, null);

        assertNotNull(summary);
        assertEquals(20L, summary.getTotalEmergencies());
        assertEquals(5L, summary.getActiveEmergencies());
        assertEquals(3L, summary.getAcceptedEmergencies());
        assertEquals(2L, summary.getCancelledEmergencies());
        assertEquals(10L, summary.getResolvedEmergencies());
    }

    @Test
    void getEmergencyTypeReport_ReturnsValidTypeDistribution() {
        when(emergencyRepository.countByType(EmergencyType.GENERAL)).thenReturn(10L);
        when(emergencyRepository.countByType(EmergencyType.HOSPITAL)).thenReturn(6L);
        when(emergencyRepository.countByType(EmergencyType.ACCIDENT)).thenReturn(4L);

        EmergencyTypeReportResponse response = reportService.getEmergencyTypeReport(null, null);

        assertNotNull(response);
        assertEquals(10L, response.getGeneralCount());
        assertEquals(6L, response.getHospitalCount());
        assertEquals(4L, response.getAccidentCount());
        assertEquals(10L, response.getTypeDistribution().get("GENERAL"));
    }

    @Test
    void getEmergencyStatusReport_ReturnsValidStatusDistribution() {
        when(emergencyRepository.countByStatus(EmergencyStatus.ACTIVE)).thenReturn(5L);
        when(emergencyRepository.countByStatus(EmergencyStatus.ACCEPTED)).thenReturn(3L);
        when(emergencyRepository.countByStatus(EmergencyStatus.RESOLVED)).thenReturn(10L);
        when(emergencyRepository.countByStatus(EmergencyStatus.CANCELLED)).thenReturn(2L);

        EmergencyStatusReportResponse response = reportService.getEmergencyStatusReport(null, null);

        assertNotNull(response);
        assertEquals(5L, response.getActiveCount());
        assertEquals(3L, response.getAcceptedCount());
        assertEquals(10L, response.getResolvedCount());
        assertEquals(2L, response.getCancelledCount());
    }

    @Test
    void getUserStatsReport_ReturnsValidUserStats() {
        when(userRepository.count()).thenReturn(50L);
        when(userRepository.countByAccountStatus(AccountStatus.ACTIVE)).thenReturn(40L);
        when(userRepository.countByAccountStatus(AccountStatus.WARNING)).thenReturn(3L);
        when(userRepository.countByAccountStatus(AccountStatus.RESTRICTED)).thenReturn(2L);
        when(userRepository.countByAccountStatus(AccountStatus.SUSPENDED)).thenReturn(3L);
        when(userRepository.countByAccountStatus(AccountStatus.DEACTIVATED)).thenReturn(2L);
        when(userRepository.countByRole(Role.POLICE)).thenReturn(5L);
        when(userRepository.countByRole(Role.HOSPITAL)).thenReturn(4L);

        UserStatsReportResponse response = reportService.getUserStatsReport();

        assertNotNull(response);
        assertEquals(50L, response.getTotalUsers());
        assertEquals(40L, response.getActiveUsers());
        assertEquals(5L, response.getPoliceUsers());
        assertEquals(4L, response.getHospitalUsers());
    }

    @Test
    void getDashboardReport_ReturnsAggregatedDashboard() {
        when(emergencyRepository.count()).thenReturn(20L);
        when(emergencyRepository.countByStatus(EmergencyStatus.ACTIVE)).thenReturn(5L);
        when(emergencyRepository.countByStatus(EmergencyStatus.ACCEPTED)).thenReturn(3L);
        when(emergencyRepository.countByStatus(EmergencyStatus.CANCELLED)).thenReturn(2L);
        when(emergencyRepository.countByStatus(EmergencyStatus.RESOLVED)).thenReturn(10L);

        when(emergencyRepository.countByType(EmergencyType.GENERAL)).thenReturn(10L);
        when(emergencyRepository.countByType(EmergencyType.HOSPITAL)).thenReturn(6L);
        when(emergencyRepository.countByType(EmergencyType.ACCIDENT)).thenReturn(4L);

        when(userRepository.count()).thenReturn(50L);
        when(userRepository.countByAccountStatus(AccountStatus.ACTIVE)).thenReturn(40L);
        when(userRepository.countByAccountStatus(AccountStatus.WARNING)).thenReturn(3L);
        when(userRepository.countByAccountStatus(AccountStatus.RESTRICTED)).thenReturn(2L);
        when(userRepository.countByAccountStatus(AccountStatus.SUSPENDED)).thenReturn(3L);
        when(userRepository.countByAccountStatus(AccountStatus.DEACTIVATED)).thenReturn(2L);
        when(userRepository.countByRole(Role.POLICE)).thenReturn(5L);
        when(userRepository.countByRole(Role.HOSPITAL)).thenReturn(4L);

        when(emergencyRepository.findTop5ByOrderByCreatedAtDesc()).thenReturn(Collections.emptyList());

        DashboardReportResponse dashboard = reportService.getDashboardReport(null, null);

        assertNotNull(dashboard);
        assertNotNull(dashboard.getEmergencySummary());
        assertNotNull(dashboard.getTypeStats());
        assertNotNull(dashboard.getStatusStats());
        assertNotNull(dashboard.getUserStats());
        assertNotNull(dashboard.getRecentActivity());
    }
}
