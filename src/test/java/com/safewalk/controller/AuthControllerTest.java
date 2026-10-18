package com.safewalk.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.safewalk.config.SecurityConfig;
import com.safewalk.dto.request.LoginRequest;
import com.safewalk.dto.request.RegisterRequest;
import com.safewalk.dto.response.AuthResponse;
import com.safewalk.dto.response.UserResponse;
import com.safewalk.entity.AccountStatus;
import com.safewalk.entity.Role;
import com.safewalk.exception.DuplicateResourceException;
import com.safewalk.exception.InvalidCredentialsException;
import com.safewalk.security.CustomAccessDeniedHandler;
import com.safewalk.security.CustomUserDetailsService;
import com.safewalk.security.JwtAuthenticationEntryPoint;
import com.safewalk.security.JwtAuthenticationFilter;
import com.safewalk.security.JwtTokenProvider;
import com.safewalk.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class, CustomAccessDeniedHandler.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void register_ValidPayload_ReturnsCreated() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .name("Ananya Roy")
                .mobileNumber("+919876543210")
                .password("password123")
                .emergencyPassword("emerg123")
                .build();

        UserResponse userResponse = UserResponse.builder()
                .id(1L)
                .name("Ananya Roy")
                .mobileNumber("+919876543210")
                .role(Role.USER)
                .accountStatus(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        AuthResponse authResponse = AuthResponse.builder()
                .token("valid.jwt.token")
                .tokenType("Bearer")
                .user(userResponse)
                .build();

        when(authService.register(any(RegisterRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").value("valid.jwt.token"))
                .andExpect(jsonPath("$.data.user.mobileNumber").value("+919876543210"));
    }

    @Test
    void register_DuplicateMobile_ReturnsConflict() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .name("Ananya Roy")
                .mobileNumber("+919876543210")
                .password("password123")
                .build();

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new DuplicateResourceException("Mobile number '+919876543210' is already registered with a SafeWalk account."));

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void login_ValidCredentials_ReturnsToken() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .mobileNumber("+919876543210")
                .password("password123")
                .build();

        UserResponse userResponse = UserResponse.builder()
                .id(1L)
                .name("Ananya Roy")
                .mobileNumber("+919876543210")
                .role(Role.USER)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        AuthResponse authResponse = AuthResponse.builder()
                .token("valid.jwt.token")
                .tokenType("Bearer")
                .user(userResponse)
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").value("valid.jwt.token"));
    }

    @Test
    void login_InvalidCredentials_ReturnsUnauthorized() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .mobileNumber("+919876543210")
                .password("wrongPassword")
                .build();

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new InvalidCredentialsException("Invalid mobile number or password."));

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }
}
