package com.safewalk.controller;

import com.safewalk.dto.request.AdminUserStatusUpdateRequest;
import com.safewalk.dto.response.ApiResponse;
import com.safewalk.dto.response.AuditLogResponse;
import com.safewalk.dto.response.DashboardReportResponse;
import com.safewalk.dto.response.EmergencyResponse;
import com.safewalk.dto.response.UserResponse;
import com.safewalk.entity.AccountStatus;
import com.safewalk.entity.EmergencyStatus;
import com.safewalk.entity.EmergencyType;
import com.safewalk.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.safewalk.entity.VerificationDocument;
import com.safewalk.service.VerificationService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final VerificationService verificationService;

    public AdminController(AdminService adminService) {
        this(adminService, null);
    }

    @Autowired
    public AdminController(
            AdminService adminService,
            @Autowired(required = false) VerificationService verificationService) {
        this.adminService = adminService;
        this.verificationService = verificationService;
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> getAllUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) AccountStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<UserResponse> response = adminService.getAllUsers(search, status, pageable);
        return ResponseEntity.ok(ApiResponse.success("Users retrieved successfully", response));
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
        UserResponse response = adminService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success("User details retrieved successfully", response));
    }

    @PutMapping("/users/{id}/status")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserStatus(
            @PathVariable Long id,
            @Valid @RequestBody AdminUserStatusUpdateRequest request,
            Authentication authentication) {
        UserResponse response = adminService.updateUserStatus(id, request, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("User account status updated successfully", response));
    }

    @PutMapping("/users/{id}/police-verification")
    public ResponseEntity<ApiResponse<UserResponse>> updatePoliceVerification(
            @PathVariable Long id,
            @Valid @RequestBody com.safewalk.dto.request.PoliceVerificationRequest request,
            Authentication authentication) {
        UserResponse response = adminService.updatePoliceVerification(id, request, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Police verification status updated successfully", response));
    }

    @GetMapping("/verifications/pending")
    public ResponseEntity<ApiResponse<java.util.List<com.safewalk.dto.response.PendingVerificationResponse>>> getPendingVerifications() {
        java.util.List<com.safewalk.dto.response.PendingVerificationResponse> response = adminService.getPendingVerifications();
        return ResponseEntity.ok(ApiResponse.success("Pending verifications retrieved successfully", response));
    }

    @PutMapping({"/users/{id}/verification", "/verifications/{id}/status"})
    public ResponseEntity<ApiResponse<UserResponse>> updateVerificationStatus(
            @PathVariable Long id,
            @Valid @RequestBody com.safewalk.dto.request.AdminVerificationRequest request,
            Authentication authentication) {
        UserResponse response = adminService.updateVerificationStatus(id, request, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Verification status updated successfully", response));
    }

    @GetMapping("/verifications/{userId}/documents/{documentId}/preview")
    public ResponseEntity<Resource> previewVerificationDocument(
            @PathVariable Long userId,
            @PathVariable Long documentId) {
        if (verificationService == null) {
            throw new IllegalStateException("Verification service is not available.");
        }
        VerificationDocument doc = verificationService.getDocumentEntity(documentId);
        Resource resource = verificationService.getDocumentForAdminPreview(userId, documentId);

        String mimeType = doc.getMimeType();
        if (mimeType == null || (!mimeType.equalsIgnoreCase("application/pdf") &&
                !mimeType.equalsIgnoreCase("image/jpeg") &&
                !mimeType.equalsIgnoreCase("image/png"))) {
            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).build();
        }

        String safeFileName = StringUtils.hasText(doc.getFileName()) ? doc.getFileName() : "preview_document";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, mimeType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"preview_" + safeFileName + "\"")
                .header("X-Content-Type-Options", "nosniff")
                .header("Cache-Control", "no-cache, no-store, must-revalidate")
                .header("Pragma", "no-cache")
                .header("Expires", "0")
                .body(resource);
    }

    @GetMapping("/emergencies")
    public ResponseEntity<ApiResponse<Page<EmergencyResponse>>> getAllEmergencies(
            @RequestParam(required = false) EmergencyStatus status,
            @RequestParam(required = false) EmergencyType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<EmergencyResponse> response = adminService.getAllEmergencies(status, type, pageable);
        return ResponseEntity.ok(ApiResponse.success("Emergencies retrieved successfully", response));
    }

    @GetMapping("/emergencies/{id}")
    public ResponseEntity<ApiResponse<EmergencyResponse>> getEmergencyById(@PathVariable Long id) {
        EmergencyResponse response = adminService.getEmergencyById(id);
        return ResponseEntity.ok(ApiResponse.success("Emergency details retrieved successfully", response));
    }

    @PostMapping("/emergencies/{id}/cancel")
    public ResponseEntity<ApiResponse<EmergencyResponse>> cancelEmergencyByAdmin(
            @PathVariable Long id,
            @RequestParam(required = false) String reason,
            Authentication authentication) {
        EmergencyResponse response = adminService.cancelEmergencyByAdmin(id, reason, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Emergency cancelled by admin successfully", response));
    }

    @PostMapping("/emergencies/{id}/resolve")
    public ResponseEntity<ApiResponse<EmergencyResponse>> resolveEmergencyByAdmin(
            @PathVariable Long id,
            @RequestParam(required = false) String reason,
            Authentication authentication) {
        EmergencyResponse response = adminService.resolveEmergencyByAdmin(id, reason, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Emergency resolved by admin successfully", response));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<DashboardReportResponse>> getAdminDashboard() {
        DashboardReportResponse response = adminService.getAdminDashboard();
        return ResponseEntity.ok(ApiResponse.success("Admin dashboard data retrieved successfully", response));
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<ApiResponse<Page<AuditLogResponse>>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<AuditLogResponse> response = adminService.getAuditLogs(pageable);
        return ResponseEntity.ok(ApiResponse.success("Audit logs retrieved successfully", response));
    }
}
