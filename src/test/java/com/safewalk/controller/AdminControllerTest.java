package com.safewalk.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.safewalk.config.SecurityConfig;
import com.safewalk.dto.request.AdminUserStatusUpdateRequest;
import com.safewalk.dto.response.DashboardReportResponse;
import com.safewalk.dto.response.EmergencyResponse;
import com.safewalk.dto.response.UserResponse;
import com.safewalk.entity.AccountStatus;
import com.safewalk.entity.EmergencyStatus;
import com.safewalk.entity.Role;
import com.safewalk.security.CustomAccessDeniedHandler;
import com.safewalk.security.CustomUserDetailsService;
import com.safewalk.security.JwtAuthenticationEntryPoint;
import com.safewalk.security.JwtAuthenticationFilter;
import com.safewalk.security.JwtTokenProvider;
import com.safewalk.service.AdminService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class, CustomAccessDeniedHandler.class})
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AdminService adminService;

    @MockBean
    private com.safewalk.service.VerificationService verificationService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void getAllUsers_UserRole_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "+919876543210", roles = "USER")
    void getAllUsers_NonAdminRole_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "+919999999999", roles = "ADMIN")
    void getAllUsers_AdminRole_ReturnsUsersPage() throws Exception {
        UserResponse userResponse = UserResponse.builder()
                .id(1L)
                .name("Regular User")
                .mobileNumber("+919876543210")
                .role(Role.USER)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        PageImpl<UserResponse> page = new PageImpl<>(Collections.singletonList(userResponse));
        when(adminService.getAllUsers(any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(1))
                .andExpect(jsonPath("$.data.content[0].name").value("Regular User"));
    }

    @Test
    @WithMockUser(username = "+919999999999", roles = "ADMIN")
    void updateUserStatus_AdminRole_ReturnsUpdatedUser() throws Exception {
        AdminUserStatusUpdateRequest request = new AdminUserStatusUpdateRequest(AccountStatus.SUSPENDED, "Violation");

        UserResponse userResponse = UserResponse.builder()
                .id(1L)
                .name("Regular User")
                .mobileNumber("+919876543210")
                .role(Role.USER)
                .accountStatus(AccountStatus.SUSPENDED)
                .build();

        when(adminService.updateUserStatus(eq(1L), any(AdminUserStatusUpdateRequest.class), eq("+919999999999")))
                .thenReturn(userResponse);

        mockMvc.perform(put("/api/v1/admin/users/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accountStatus").value("SUSPENDED"));
    }

    @Test
    @WithMockUser(username = "+919999999999", roles = "ADMIN")
    void getAdminDashboard_AdminRole_ReturnsDashboard() throws Exception {
        DashboardReportResponse dashboard = DashboardReportResponse.builder().build();

        when(adminService.getAdminDashboard()).thenReturn(dashboard);

        mockMvc.perform(get("/api/v1/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(username = "+919876543210", roles = "USER")
    void previewDocument_NonAdminRole_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/verifications/1/documents/10/preview"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "+919999999999", roles = "ADMIN")
    void previewDocument_AdminRole_ReturnsInlineResource() throws Exception {
        byte[] pdfBytes = "%PDF-1.4 test document".getBytes();
        org.springframework.core.io.Resource resource = new org.springframework.core.io.ByteArrayResource(pdfBytes);
        com.safewalk.entity.VerificationDocument doc = com.safewalk.entity.VerificationDocument.builder()
                .id(10L)
                .fileName("badge_proof.pdf")
                .mimeType("application/pdf")
                .fileSize((long) pdfBytes.length)
                .build();

        when(verificationService.getDocumentForAdminPreview(1L, 10L)).thenReturn(resource);
        when(verificationService.getDocumentEntity(10L)).thenReturn(doc);

        mockMvc.perform(get("/api/v1/admin/verifications/1/documents/10/preview"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("Content-Disposition", org.hamcrest.Matchers.containsString("inline")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("Cache-Control", "no-cache, no-store, must-revalidate"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_PDF));
    }
}
