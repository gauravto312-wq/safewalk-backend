package com.safewalk.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.safewalk.config.SecurityConfig;
import com.safewalk.dto.request.LocationUpdateRequest;
import com.safewalk.dto.response.GeocodingResponse;
import com.safewalk.dto.response.NearbyUserResponse;
import com.safewalk.dto.response.RouteDistanceResponse;
import com.safewalk.dto.response.UserLocationResponse;
import com.safewalk.entity.AccountStatus;
import com.safewalk.entity.Role;
import com.safewalk.exception.GlobalExceptionHandler;
import com.safewalk.security.CustomAccessDeniedHandler;
import com.safewalk.security.CustomUserDetailsService;
import com.safewalk.security.JwtAuthenticationEntryPoint;
import com.safewalk.security.JwtAuthenticationFilter;
import com.safewalk.security.JwtTokenProvider;
import com.safewalk.service.LocationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LocationController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class, CustomAccessDeniedHandler.class, GlobalExceptionHandler.class})
class LocationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LocationService locationService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void getCurrentLocation_Unauthenticated_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/location"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(username = "+919876543210", roles = "USER")
    void getCurrentLocation_Authenticated_ReturnsOk() throws Exception {
        UserLocationResponse locationResponse = UserLocationResponse.builder()
                .userId(1L)
                .latitude(28.6139)
                .longitude(77.2090)
                .timestamp(1696000000000L)
                .fresh(true)
                .build();

        when(locationService.getUserCurrentLocation("+919876543210")).thenReturn(locationResponse);

        mockMvc.perform(get("/api/v1/location"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(1))
                .andExpect(jsonPath("$.data.latitude").value(28.6139))
                .andExpect(jsonPath("$.data.longitude").value(77.2090))
                .andExpect(jsonPath("$.data.fresh").value(true));
    }

    @Test
    void updateLocation_Unauthenticated_ReturnsUnauthorized() throws Exception {
        LocationUpdateRequest request = new LocationUpdateRequest(28.6139, 77.2090);

        mockMvc.perform(put("/api/v1/location")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(username = "+919876543210", roles = "USER")
    void updateLocation_Authenticated_ValidRequest_ReturnsOk() throws Exception {
        LocationUpdateRequest request = new LocationUpdateRequest(28.6139, 77.2090);

        mockMvc.perform(put("/api/v1/location")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Location updated successfully"));

        verify(locationService).updateUserLocation(eq("+919876543210"), any(LocationUpdateRequest.class));
    }

    @Test
    @WithMockUser(username = "+919876543210", roles = "USER")
    void updateLocation_InvalidCoordinates_ReturnsBadRequest() throws Exception {
        LocationUpdateRequest request = new LocationUpdateRequest(95.0, 77.2090);

        mockMvc.perform(put("/api/v1/location")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void removeLocation_Unauthenticated_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(delete("/api/v1/location"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(username = "+919876543210", roles = "USER")
    void removeLocation_Authenticated_ReturnsOk() throws Exception {
        mockMvc.perform(delete("/api/v1/location"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Location removed successfully"));

        verify(locationService).removeUserLocation("+919876543210");
    }

    @Test
    void getNearbyResponders_Unauthenticated_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/location/nearby")
                .param("latitude", "28.6139")
                .param("longitude", "77.2090"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(username = "+919876543210", roles = "USER")
    void getNearbyResponders_Authenticated_ValidCoordinates_ReturnsOk() throws Exception {
        NearbyUserResponse responder = NearbyUserResponse.builder()
                .userId(1L)
                .name("Alice Responder")
                .mobileNumber("+919876543211")
                .latitude(28.6140)
                .longitude(77.2095)
                .distanceKm(0.5)
                .role(Role.USER)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        List<NearbyUserResponse> responders = Collections.singletonList(responder);

        when(locationService.findNearbyResponders(28.6139, 77.2090, 2.0))
                .thenReturn(responders);

        mockMvc.perform(get("/api/v1/location/nearby")
                .param("latitude", "28.6139")
                .param("longitude", "77.2090")
                .param("radius", "2.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Nearby responders retrieved successfully"))
                .andExpect(jsonPath("$.data[0].userId").value(1))
                .andExpect(jsonPath("$.data[0].name").value("Alice Responder"))
                .andExpect(jsonPath("$.data[0].distanceKm").value(0.5));
    }

    @Test
    @WithMockUser(username = "+919876543210", roles = "USER")
    void getNearbyResponders_InvalidCoordinates_ReturnsBadRequest() throws Exception {
        when(locationService.findNearbyResponders(100.0, 77.2090, 2.0))
                .thenThrow(new IllegalArgumentException("Latitude must be between -90.0 and 90.0"));

        mockMvc.perform(get("/api/v1/location/nearby")
                .param("latitude", "100.0")
                .param("longitude", "77.2090")
                .param("radius", "2.0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Latitude must be between -90.0 and 90.0"));
    }

    @Test
    @WithMockUser(username = "+919876543210", roles = "USER")
    void reverseGeocode_Authenticated_ReturnsOk() throws Exception {
        GeocodingResponse geocodeResponse = GeocodingResponse.builder()
                .formattedAddress("Location Coordinates (28.6139, 77.2090)")
                .latitude(28.6139)
                .longitude(77.2090)
                .provider("google (Fallback)")
                .build();

        when(locationService.reverseGeocode(28.6139, 77.2090)).thenReturn(geocodeResponse);

        mockMvc.perform(get("/api/v1/location/geocode")
                .param("latitude", "28.6139")
                .param("longitude", "77.2090"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Reverse geocoding retrieved successfully"))
                .andExpect(jsonPath("$.data.formattedAddress").value("Location Coordinates (28.6139, 77.2090)"));
    }

    @Test
    @WithMockUser(username = "+919876543210", roles = "USER")
    void calculateRouteDistance_Authenticated_ReturnsOk() throws Exception {
        RouteDistanceResponse distanceResponse = RouteDistanceResponse.builder()
                .distanceKm(12.5)
                .distanceText("12.50 km")
                .durationMinutes(25.0)
                .durationText("25 mins")
                .originLatitude(28.6139)
                .originLongitude(77.2090)
                .destinationLatitude(28.5355)
                .destinationLongitude(77.3910)
                .provider("google (Haversine Fallback)")
                .build();

        when(locationService.calculateRouteDistance(28.6139, 77.2090, 28.5355, 77.3910)).thenReturn(distanceResponse);

        mockMvc.perform(get("/api/v1/location/distance")
                .param("originLat", "28.6139")
                .param("originLng", "77.2090")
                .param("destLat", "28.5355")
                .param("destLng", "77.3910"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Route distance and ETA calculated successfully"))
                .andExpect(jsonPath("$.data.distanceKm").value(12.5))
                .andExpect(jsonPath("$.data.durationMinutes").value(25.0));
    }
}
