package com.safewalk.service;

import com.safewalk.dto.request.AdminUserStatusUpdateRequest;
import com.safewalk.dto.response.AuditLogResponse;
import com.safewalk.dto.response.DashboardReportResponse;
import com.safewalk.dto.response.EmergencyResponse;
import com.safewalk.dto.response.UserResponse;
import com.safewalk.entity.AccountStatus;
import com.safewalk.entity.EmergencyStatus;
import com.safewalk.entity.EmergencyType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminService {

    Page<UserResponse> getAllUsers(String search, AccountStatus status, Pageable pageable);

    UserResponse getUserById(Long userId);

    UserResponse updateUserStatus(Long userId, AdminUserStatusUpdateRequest request, String adminMobile);

    UserResponse updatePoliceVerification(Long userId, com.safewalk.dto.request.PoliceVerificationRequest request, String adminMobile);

    java.util.List<com.safewalk.dto.response.PendingVerificationResponse> getPendingVerifications();

    UserResponse updateVerificationStatus(Long userId, com.safewalk.dto.request.AdminVerificationRequest request, String adminMobile);

    Page<EmergencyResponse> getAllEmergencies(EmergencyStatus status, EmergencyType type, Pageable pageable);

    EmergencyResponse getEmergencyById(Long emergencyId);

    EmergencyResponse cancelEmergencyByAdmin(Long emergencyId, String reason, String adminMobile);

    EmergencyResponse resolveEmergencyByAdmin(Long emergencyId, String reason, String adminMobile);

    DashboardReportResponse getAdminDashboard();

    Page<AuditLogResponse> getAuditLogs(Pageable pageable);
}
