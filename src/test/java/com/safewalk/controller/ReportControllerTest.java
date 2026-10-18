package com.safewalk.controller;

import com.safewalk.config.SecurityConfig;
import com.safewalk.dto.response.DashboardReportResponse;
import com.safewalk.dto.response.EmergencyStatusReportResponse;
import com.safewalk.dto.response.EmergencySummaryReportResponse;
import com.safewalk.dto.response.EmergencyTypeReportResponse;
import com.safewalk.dto.response.UserStatsReportResponse;
import com.safewalk.security.CustomAccessDeniedHandler;
import com.safewalk.security.CustomUserDetailsService;
import com.safewalk.security.JwtAuthenticationEntryPoint;
import com.safewalk.security.JwtAuthenticationFilter;
import com.safewalk.security.JwtTokenProvider;
import com.safewalk.service.ReportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReportController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class, CustomAccessDeniedHandler.class})
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReportService reportService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void getEmergencySummaryReport_Unauthenticated_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/reports/emergencies/summary"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "+919876543210", roles = "USER")
    void getEmergencySummaryReport_UserRole_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/reports/emergencies/summary"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "+919876543210", roles = "ADMIN")
    void getEmergencySummaryReport_AdminRole_ReturnsOk() throws Exception {
        EmergencySummaryReportResponse summary = EmergencySummaryReportResponse.builder()
                .totalEmergencies(10L)
                .activeEmergencies(2L)
                .acceptedEmergencies(1L)
                .cancelledEmergencies(1L)
                .resolvedEmergencies(6L)
                .build();

        when(reportService.getEmergencySummaryReport(any(), any())).thenReturn(summary);

        mockMvc.perform(get("/api/v1/reports/emergencies/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalEmergencies").value(10))
                .andExpect(jsonPath("$.data.resolvedEmergencies").value(6));
    }

    @Test
    @WithMockUser(username = "+919876543210", roles = "USER")
    void getEmergencyTypeReport_UserRole_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/reports/emergencies/types"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "+919876543210", roles = "POLICE")
    void getEmergencyTypeReport_PoliceRole_ReturnsOk() throws Exception {
        EmergencyTypeReportResponse typeStats = EmergencyTypeReportResponse.builder()
                .generalCount(5L)
                .hospitalCount(3L)
                .accidentCount(2L)
                .build();

        when(reportService.getEmergencyTypeReport(any(), any())).thenReturn(typeStats);

        mockMvc.perform(get("/api/v1/reports/emergencies/types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.generalCount").value(5));
    }

    @Test
    @WithMockUser(username = "+919876543210", roles = "USER")
    void getUserStatsReport_UserRole_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/reports/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "+919876543210", roles = "ADMIN")
    void getUserStatsReport_AdminRole_ReturnsOk() throws Exception {
        UserStatsReportResponse userStats = UserStatsReportResponse.builder()
                .totalUsers(100L)
                .activeUsers(90L)
                .build();

        when(reportService.getUserStatsReport()).thenReturn(userStats);

        mockMvc.perform(get("/api/v1/reports/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalUsers").value(100));
    }
}
