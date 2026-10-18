package com.safewalk.controller;

import com.safewalk.dto.request.FcmNotificationRequest;
import com.safewalk.dto.request.FcmTokenRequest;
import com.safewalk.dto.response.ApiResponse;
import com.safewalk.service.FcmService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final FcmService fcmService;

    @Autowired
    public NotificationController(FcmService fcmService) {
        this.fcmService = fcmService;
    }

    @PostMapping("/device-token")
    public ResponseEntity<ApiResponse<String>> registerDeviceToken(
            @Valid @RequestBody FcmTokenRequest request,
            Authentication authentication) {
        fcmService.registerDeviceToken(authentication.getName(), request);
        return ResponseEntity.ok(ApiResponse.success("FCM device token registered successfully"));
    }

    @DeleteMapping("/device-token")
    public ResponseEntity<ApiResponse<String>> removeDeviceToken(
            @RequestParam String token,
            Authentication authentication) {
        fcmService.removeDeviceToken(authentication.getName(), token);
        return ResponseEntity.ok(ApiResponse.success("FCM device token deactivated successfully"));
    }

    @PostMapping("/test")
    public ResponseEntity<ApiResponse<String>> sendTestNotification(
            @Valid @RequestBody FcmNotificationRequest request) {
        boolean sent = fcmService.sendNotification(
                request.getTargetToken() != null ? request.getTargetToken() : "test-device-token",
                request.getTitle(),
                request.getBody(),
                request.getData());

        if (sent) {
            return ResponseEntity.ok(ApiResponse.success("Test FCM push notification processed successfully"));
        } else {
            return ResponseEntity.ok(ApiResponse.success("FCM push notification delivery attempted (device unregistered or operating in dev mode)"));
        }
    }
}
