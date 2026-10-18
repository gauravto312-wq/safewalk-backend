package com.safewalk.controller;

import com.safewalk.dto.request.LocationUpdateRequest;
import com.safewalk.dto.response.ApiResponse;
import com.safewalk.dto.response.GeocodingResponse;
import com.safewalk.dto.response.NearbyUserResponse;
import com.safewalk.dto.response.RouteDistanceResponse;
import com.safewalk.dto.response.UserLocationResponse;
import com.safewalk.service.LocationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/location")
public class LocationController {

    private final LocationService locationService;

    @Autowired
    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<UserLocationResponse>> getCurrentLocation(Authentication authentication) {
        UserLocationResponse response = locationService.getUserCurrentLocation(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Current location retrieved successfully", response));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<String>> updateLocation(
            @Valid @RequestBody LocationUpdateRequest request,
            Authentication authentication) {
        locationService.updateUserLocation(authentication.getName(), request);
        return ResponseEntity.ok(ApiResponse.success("Location updated successfully"));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<String>> removeLocation(Authentication authentication) {
        locationService.removeUserLocation(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Location removed successfully"));
    }

    @GetMapping("/nearby")
    public ResponseEntity<ApiResponse<List<NearbyUserResponse>>> getNearbyResponders(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(required = false) Double radius) {
        List<NearbyUserResponse> responders = locationService.findNearbyResponders(latitude, longitude, radius);
        return ResponseEntity.ok(ApiResponse.success("Nearby responders retrieved successfully", responders));
    }

    @GetMapping("/geocode")
    public ResponseEntity<ApiResponse<GeocodingResponse>> reverseGeocode(
            @RequestParam Double latitude,
            @RequestParam Double longitude) {
        GeocodingResponse response = locationService.reverseGeocode(latitude, longitude);
        return ResponseEntity.ok(ApiResponse.success("Reverse geocoding retrieved successfully", response));
    }

    @GetMapping("/distance")
    public ResponseEntity<ApiResponse<RouteDistanceResponse>> calculateRouteDistance(
            @RequestParam Double originLat,
            @RequestParam Double originLng,
            @RequestParam Double destLat,
            @RequestParam Double destLng) {
        RouteDistanceResponse response = locationService.calculateRouteDistance(originLat, originLng, destLat, destLng);
        return ResponseEntity.ok(ApiResponse.success("Route distance and ETA calculated successfully", response));
    }
}
