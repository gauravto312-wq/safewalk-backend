package com.safewalk.controller;

import com.safewalk.config.SecurityConfig;
import com.safewalk.security.CustomAccessDeniedHandler;
import com.safewalk.security.CustomUserDetailsService;
import com.safewalk.security.JwtAuthenticationEntryPoint;
import com.safewalk.security.JwtAuthenticationFilter;
import com.safewalk.security.JwtTokenProvider;
import com.safewalk.service.RedisService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RedisHealthController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class, CustomAccessDeniedHandler.class})
class RedisHealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RedisService redisService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void checkRedisHealth_ReturnsStatusUp() throws Exception {
        when(redisService.get(anyString())).thenReturn("SafeWalk Redis Active Verification");
        when(redisService.getExpire(anyString(), eq(TimeUnit.SECONDS))).thenReturn(60L);
        when(redisService.hasKey(anyString())).thenReturn(true).thenReturn(false);
        when(redisService.delete(anyString())).thenReturn(true);

        mockMvc.perform(get("/api/v1/redis/health")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.data.service").value("Redis Connection & Utility Engine"))
                .andExpect(jsonPath("$.data.writeReadSuccess").value(true));
    }
}
