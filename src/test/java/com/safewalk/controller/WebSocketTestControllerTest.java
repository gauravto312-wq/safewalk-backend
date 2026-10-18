package com.safewalk.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.safewalk.config.SecurityConfig;
import com.safewalk.dto.event.RealTimeEvent;
import com.safewalk.exception.GlobalExceptionHandler;
import com.safewalk.security.CustomAccessDeniedHandler;
import com.safewalk.security.CustomUserDetailsService;
import com.safewalk.security.JwtAuthenticationEntryPoint;
import com.safewalk.security.JwtAuthenticationFilter;
import com.safewalk.security.JwtTokenProvider;
import com.safewalk.service.WebSocketService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WebSocketTestController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class, CustomAccessDeniedHandler.class, GlobalExceptionHandler.class})
class WebSocketTestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WebSocketService webSocketService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void sendTestEvent_Unauthenticated_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/realtime/test"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(username = "+919876543210", roles = "USER")
    void sendTestEvent_Authenticated_ReturnsOkAndPublishesEvent() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("testKey", "testValue");

        mockMvc.perform(post("/api/v1/realtime/test")
                .param("message", "Custom test connection message")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.eventType").value("TEST"))
                .andExpect(jsonPath("$.data.message").value("Custom test connection message"));

        verify(webSocketService).broadcastEvent(eq("/topic/test"), any(RealTimeEvent.class));
    }
}
