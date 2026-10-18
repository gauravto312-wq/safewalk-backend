package com.safewalk.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.safewalk.config.SecurityConfig;
import com.safewalk.dto.request.FcmNotificationRequest;
import com.safewalk.dto.request.FcmTokenRequest;
import com.safewalk.exception.GlobalExceptionHandler;
import com.safewalk.security.CustomAccessDeniedHandler;
import com.safewalk.security.CustomUserDetailsService;
import com.safewalk.security.JwtAuthenticationEntryPoint;
import com.safewalk.security.JwtAuthenticationFilter;
import com.safewalk.security.JwtTokenProvider;
import com.safewalk.service.FcmService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class, CustomAccessDeniedHandler.class, GlobalExceptionHandler.class})
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FcmService fcmService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void registerDeviceToken_Unauthenticated_ReturnsUnauthorized() throws Exception {
        FcmTokenRequest request = new FcmTokenRequest("sample.fcm.token", "ANDROID");

        mockMvc.perform(post("/api/v1/notifications/device-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(username = "+919876543210", roles = "USER")
    void registerDeviceToken_Authenticated_ReturnsOk() throws Exception {
        FcmTokenRequest request = new FcmTokenRequest("sample.fcm.token", "ANDROID");

        mockMvc.perform(post("/api/v1/notifications/device-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("FCM device token registered successfully"));

        verify(fcmService).registerDeviceToken(eq("+919876543210"), any(FcmTokenRequest.class));
    }

    @Test
    @WithMockUser(username = "+919876543210", roles = "USER")
    void registerDeviceToken_BlankToken_ReturnsBadRequest() throws Exception {
        FcmTokenRequest request = new FcmTokenRequest("", "ANDROID");

        mockMvc.perform(post("/api/v1/notifications/device-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(username = "+919876543210", roles = "USER")
    void removeDeviceToken_Authenticated_ReturnsOk() throws Exception {
        mockMvc.perform(delete("/api/v1/notifications/device-token")
                .param("token", "sample.fcm.token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("FCM device token deactivated successfully"));

        verify(fcmService).removeDeviceToken("+919876543210", "sample.fcm.token");
    }

    @Test
    @WithMockUser(username = "+919876543210", roles = "USER")
    void sendTestNotification_UserRole_ReturnsForbidden() throws Exception {
        FcmNotificationRequest request = new FcmNotificationRequest("sample.fcm.token", "Emergency Test", "This is a test notification", null);

        mockMvc.perform(post("/api/v1/notifications/test")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "+919876543210", roles = "ADMIN")
    void sendTestNotification_AdminRole_ReturnsOk() throws Exception {
        FcmNotificationRequest request = new FcmNotificationRequest("sample.fcm.token", "Emergency Test", "This is a test notification", null);

        when(fcmService.sendNotification(eq("sample.fcm.token"), eq("Emergency Test"), eq("This is a test notification"), any()))
                .thenReturn(true);

        mockMvc.perform(post("/api/v1/notifications/test")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
