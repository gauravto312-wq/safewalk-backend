package com.safewalk.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.junit.jupiter.api.Assertions.*;

class CorsConfigTest {

    private CorsConfig corsConfig;

    @BeforeEach
    void setUp() {
        corsConfig = new CorsConfig();
        ReflectionTestUtils.setField(
                corsConfig,
                "allowedOriginsConfig",
                "https://safewalk-frontend-beta.vercel.app,http://localhost:5173,http://127.0.0.1:5173,http://localhost:3000,http://localhost:8080"
        );
    }

    @Test
    void corsConfigurationSource_ContainsProductionAndLocalOrigins() {
        CorsConfigurationSource source = corsConfig.corsConfigurationSource();
        assertNotNull(source);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/auth/login");

        CorsConfiguration config = source.getCorsConfiguration(request);
        assertNotNull(config);

        // Production Vercel Origin
        assertTrue(config.getAllowedOrigins().contains("https://safewalk-frontend-beta.vercel.app"));

        // Local development origins
        assertTrue(config.getAllowedOrigins().contains("http://localhost:5173"));
        assertTrue(config.getAllowedOrigins().contains("http://127.0.0.1:5173"));
        assertTrue(config.getAllowedOrigins().contains("http://localhost:3000"));
        assertTrue(config.getAllowedOrigins().contains("http://localhost:8080"));

        // Local origin patterns
        assertTrue(config.getAllowedOriginPatterns().contains("http://localhost:[*]"));
        assertTrue(config.getAllowedOriginPatterns().contains("http://127.0.0.1:[*]"));
        assertTrue(config.getAllowedOriginPatterns().contains("http://10.0.2.2:[*]"));
        assertTrue(config.getAllowedOriginPatterns().contains("http://192.168.*:[*]"));
        assertTrue(config.getAllowedOriginPatterns().contains("http://10.*:[*]"));
    }

    @Test
    void corsConfigurationSource_MethodsAndHeadersConfiguredCorrectly() {
        CorsConfigurationSource source = corsConfig.corsConfigurationSource();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/auth/login");

        CorsConfiguration config = source.getCorsConfiguration(request);
        assertNotNull(config);

        assertTrue(config.getAllowedMethods().containsAll(
                java.util.List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")));
        assertTrue(config.getAllowedHeaders().containsAll(
                java.util.List.of("Authorization", "Content-Type", "Accept", "X-Requested-With", "Origin")));
        assertTrue(config.getExposedHeaders().containsAll(
                java.util.List.of("Authorization", "Content-Disposition")));
        assertEquals(Boolean.TRUE, config.getAllowCredentials());
        assertEquals(3600L, config.getMaxAge());
    }

    @Test
    void corsConfigurationSource_CheckOrigin_ValidatesProperly() {
        CorsConfigurationSource source = corsConfig.corsConfigurationSource();
        MockHttpServletRequest request = new MockHttpServletRequest();
        CorsConfiguration config = source.getCorsConfiguration(request);
        assertNotNull(config);

        // Allowed
        assertEquals("https://safewalk-frontend-beta.vercel.app", config.checkOrigin("https://safewalk-frontend-beta.vercel.app"));
        assertEquals("http://localhost:5173", config.checkOrigin("http://localhost:5173"));
        assertEquals("http://localhost:4173", config.checkOrigin("http://localhost:4173"));
        assertEquals("http://10.0.2.2:8080", config.checkOrigin("http://10.0.2.2:8080"));

        // Rejected
        assertNull(config.checkOrigin("https://unauthorized-domain.com"));
        assertNull(config.checkOrigin("http://evil-localhost.com"));
    }
}
