package com.safewalk.controller;

import com.safewalk.dto.request.CancelEmergencyRequest;
import com.safewalk.dto.request.CreateEmergencyRequest;
import com.safewalk.dto.response.ApiResponse;
import com.safewalk.dto.response.EmergencyLocationResponse;
import com.safewalk.dto.response.EmergencyResponse;
import com.safewalk.service.EmergencyService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/emergencies")
public class EmergencyController {

    private final EmergencyService emergencyService;

    @Autowired
    public EmergencyController(EmergencyService emergencyService) {
        this.emergencyService = emergencyService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<EmergencyResponse>> createEmergency(
            @Valid @RequestBody(required = false) CreateEmergencyRequest request,
            Authentication authentication) {
        if (request == null) {
            request = new CreateEmergencyRequest();
        }
        EmergencyResponse response = emergencyService.createEmergency(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Emergency request created successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<java.util.List<EmergencyResponse>>> getActiveEmergencies(Authentication authentication) {
        java.util.List<EmergencyResponse> response = emergencyService.getActiveEmergencies(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Active emergencies retrieved successfully", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EmergencyResponse>> getEmergencyById(
            @PathVariable Long id,
            Authentication authentication) {
        boolean isAuthorizedRole = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") ||
                               a.getAuthority().equals("ROLE_POLICE") ||
                               a.getAuthority().equals("ROLE_HOSPITAL"));
        EmergencyResponse response = emergencyService.getEmergencyById(id, authentication.getName(), isAuthorizedRole);
        return ResponseEntity.ok(ApiResponse.success("Emergency request retrieved successfully", response));
    }

    @GetMapping("/{id}/locations")
    public ResponseEntity<ApiResponse<EmergencyLocationResponse>> getEmergencyLocations(
            @PathVariable Long id,
            Authentication authentication) {
        boolean isAuthorizedRole = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") ||
                               a.getAuthority().equals("ROLE_POLICE") ||
                               a.getAuthority().equals("ROLE_HOSPITAL"));
        EmergencyLocationResponse response = emergencyService.getEmergencyLocations(id, authentication.getName(), isAuthorizedRole);
        return ResponseEntity.ok(ApiResponse.success("Emergency mutual locations retrieved successfully", response));
    }

    @GetMapping("/my-active")
    public ResponseEntity<ApiResponse<EmergencyResponse>> getMyActiveEmergency(Authentication authentication) {
        EmergencyResponse response = emergencyService.getMyActiveEmergency(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Active emergency request retrieved successfully", response));
    }

    @GetMapping("/my-history")
    public ResponseEntity<ApiResponse<Page<EmergencyResponse>>> getMyEmergencyHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        Pageable pageable = PageRequest.of(page, size);
        Page<EmergencyResponse> response = emergencyService.getMyEmergencyHistory(authentication.getName(), pageable);
        return ResponseEntity.ok(ApiResponse.success("Emergency history retrieved successfully", response));
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<ApiResponse<EmergencyResponse>> acceptEmergency(
            @PathVariable Long id,
            Authentication authentication) {
        EmergencyResponse response = emergencyService.acceptEmergency(id, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Emergency request accepted successfully", response));
    }

    @PostMapping("/{id}/arrive")
    public ResponseEntity<ApiResponse<EmergencyResponse>> arriveEmergency(
            @PathVariable Long id,
            Authentication authentication) {
        EmergencyResponse response = emergencyService.arriveEmergency(id, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Responder arrival marked successfully", response));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<EmergencyResponse>> rejectEmergency(
            @PathVariable Long id,
            Authentication authentication) {
        EmergencyResponse response = emergencyService.rejectEmergency(id, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Emergency request rejected successfully", response));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<EmergencyResponse>> cancelEmergency(
            @PathVariable Long id,
            @Valid @RequestBody CancelEmergencyRequest request,
            Authentication authentication) {
        EmergencyResponse response = emergencyService.cancelEmergency(id, request, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Emergency request cancelled successfully", response));
    }

    @PostMapping("/{id}/resolve")
    public ResponseEntity<ApiResponse<EmergencyResponse>> resolveEmergency(
            @PathVariable Long id,
            Authentication authentication) {
        EmergencyResponse response = emergencyService.resolveEmergency(id, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Emergency request resolved successfully", response));
    }
}
