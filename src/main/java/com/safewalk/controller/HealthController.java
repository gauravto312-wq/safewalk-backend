package com.safewalk.controller;

import com.safewalk.dto.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, String>>> checkHealth() {
        Map<String, String> healthDetails = new HashMap<>();
        healthDetails.put("status", "UP");
        healthDetails.put("service", "SafeWalk Backend Baseline");
        healthDetails.put("phase", "Phase 1A - Foundation");

        return ResponseEntity.ok(ApiResponse.success("SafeWalk system operational", healthDetails));
    }
}
