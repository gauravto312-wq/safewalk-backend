package com.safewalk.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.safewalk.config.SecurityConfig;
import com.safewalk.dto.request.CancelEmergencyRequest;
import com.safewalk.dto.request.CreateEmergencyRequest;
import com.safewalk.dto.response.EmergencyLocationResponse;
import com.safewalk.dto.response.EmergencyResponse;
import com.safewalk.entity.EmergencyStatus;
import com.safewalk.entity.EmergencyType;
import com.safewalk.security.CustomAccessDeniedHandler;
import com.safewalk.security.CustomUserDetailsService;
import com.safewalk.security.JwtAuthenticationEntryPoint;
import com.safewalk.security.JwtAuthenticationFilter;
import com.safewalk.security.JwtTokenProvider;
import com.safewalk.service.EmergencyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EmergencyController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class, CustomAccessDeniedHandler.class})
class EmergencyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EmergencyService emergencyService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void createEmergency_Unauthenticated_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/emergencies"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(username = "+919876543210", roles = "USER")
    void createEmergency_Authenticated_ReturnsCreated() throws Exception {
        CreateEmergencyRequest request = CreateEmergencyRequest.builder()
                .type(EmergencyType.GENERAL)
                .latitude(26.8467)
                .longitude(80.9462)
                .build();

        EmergencyResponse mockResponse = EmergencyResponse.builder()
                .id(1L)
                .requesterId(10L)
                .requesterName("Aman Sharma")
                .requesterMobile("+919876543210")
                .type(EmergencyType.GENERAL)
                .status(EmergencyStatus.ACTIVE)
                .latitude(26.8467)
                .longitude(80.9462)
                .createdAt(LocalDateTime.now())
                .build();

        when(emergencyService.createEmergency(any(CreateEmergencyRequest.class), eq("+919876543210")))
                .thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/emergencies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.type").value("GENERAL"));
    }

    @Test
    @WithMockUser(username = "+919876543210", roles = "USER")
    void getMyActiveEmergency_ReturnsActiveEmergency() throws Exception {
        EmergencyResponse mockResponse = EmergencyResponse.builder()
                .id(1L)
                .requesterId(10L)
                .type(EmergencyType.GENERAL)
                .status(EmergencyStatus.ACTIVE)
                .build();

        when(emergencyService.getMyActiveEmergency(eq("+919876543210"))).thenReturn(mockResponse);

        mockMvc.perform(get("/api/v1/emergencies/my-active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    @WithMockUser(username = "+919876543210", roles = "USER")
    void getActiveEmergencies_AuthenticatedUser_ReturnsActiveEmergencies() throws Exception {
        EmergencyResponse mockResponse = EmergencyResponse.builder()
                .id(1L)
                .requesterId(10L)
                .type(EmergencyType.GENERAL)
                .status(EmergencyStatus.ACTIVE)
                .build();

        when(emergencyService.getActiveEmergencies(eq("+919876543210")))
                .thenReturn(Collections.singletonList(mockResponse));

        mockMvc.perform(get("/api/v1/emergencies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].status").value("ACTIVE"));
    }

    @Test
    @WithMockUser(username = "+919876543210", roles = "USER")
    void cancelEmergency_ValidPassword_ReturnsOk() throws Exception {
        CancelEmergencyRequest request = CancelEmergencyRequest.builder()
                .emergencyPassword("sosPassword123")
                .build();

        EmergencyResponse mockResponse = EmergencyResponse.builder()
                .id(1L)
                .status(EmergencyStatus.CANCELLED)
                .cancelledAt(LocalDateTime.now())
                .build();

        when(emergencyService.cancelEmergency(eq(1L), any(CancelEmergencyRequest.class), eq("+919876543210")))
                .thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/emergencies/1/cancel")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    @Test
    @WithMockUser(username = "+919876543211", roles = "USER")
    void acceptEmergency_AuthenticatedResponder_ReturnsOk() throws Exception {
        EmergencyResponse mockResponse = EmergencyResponse.builder()
                .id(1L)
                .status(EmergencyStatus.ACCEPTED)
                .responderId(20L)
                .responderName("Priya Responder")
                .responderMobile("+919876543211")
                .acceptedAt(LocalDateTime.now())
                .build();

        when(emergencyService.acceptEmergency(eq(1L), eq("+919876543211"))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/emergencies/1/accept"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.data.responderId").value(20));
    }

    @Test
    @WithMockUser(username = "+919876543211", roles = "USER")
    void rejectEmergency_AuthenticatedResponder_ReturnsOk() throws Exception {
        EmergencyResponse mockResponse = EmergencyResponse.builder()
                .id(1L)
                .status(EmergencyStatus.ACTIVE)
                .build();

        when(emergencyService.rejectEmergency(eq(1L), eq("+919876543211"))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/emergencies/1/reject"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @WithMockUser(username = "+919876543210", roles = "USER")
    void resolveEmergency_ReturnsOk() throws Exception {
        EmergencyResponse mockResponse = EmergencyResponse.builder()
                .id(1L)
                .status(EmergencyStatus.RESOLVED)
                .resolvedAt(LocalDateTime.now())
                .build();

        when(emergencyService.resolveEmergency(eq(1L), eq("+919876543210"))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/emergencies/1/resolve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("RESOLVED"));
    }

    @Test
    @WithMockUser(username = "+919876543210", roles = "USER")
    void getEmergencyLocations_AuthenticatedParticipant_ReturnsOk() throws Exception {
        EmergencyLocationResponse mockResponse = EmergencyLocationResponse.builder()
                .emergencyId(1L)
                .status(EmergencyStatus.ACCEPTED)
                .requester(EmergencyLocationResponse.ParticipantLocation.builder()
                        .userId(10L)
                        .name("Aman Sharma")
                        .mobileNumber("+919876543210")
                        .latitude(26.8467)
                        .longitude(80.9462)
                        .build())
                .responders(java.util.Collections.singletonList(
                        EmergencyLocationResponse.ParticipantLocation.builder()
                                .userId(20L)
                                .name("Priya Responder")
                                .mobileNumber("+919876543211")
                                .latitude(26.8470)
                                .longitude(80.9465)
                                .distanceKm(0.05)
                                .build()
                ))
                .build();

        when(emergencyService.getEmergencyLocations(eq(1L), eq("+919876543210"), eq(false)))
                .thenReturn(mockResponse);

        mockMvc.perform(get("/api/v1/emergencies/1/locations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.emergencyId").value(1))
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.data.requester.userId").value(10))
                .andExpect(jsonPath("$.data.responders[0].userId").value(20));
    }
}
