package com.safewalk.service.impl;

import com.safewalk.dto.request.AdminUserStatusUpdateRequest;
import com.safewalk.dto.response.AuditLogResponse;
import com.safewalk.dto.response.DashboardReportResponse;
import com.safewalk.dto.response.EmergencyResponse;
import com.safewalk.dto.response.UserResponse;
import com.safewalk.entity.AccountStatus;
import com.safewalk.entity.AuditLog;
import com.safewalk.entity.Emergency;
import com.safewalk.entity.EmergencyStatus;
import com.safewalk.entity.EmergencyType;
import com.safewalk.entity.Role;
import com.safewalk.entity.User;
import com.safewalk.exception.ResourceNotFoundException;
import com.safewalk.mapper.EmergencyMapper;
import com.safewalk.mapper.UserMapper;
import com.safewalk.repository.AuditLogRepository;
import com.safewalk.repository.EmergencyRepository;
import com.safewalk.repository.UserRepository;
import com.safewalk.service.AdminService;
import com.safewalk.service.ReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class AdminServiceImpl implements AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminServiceImpl.class);

    private final UserRepository userRepository;
    private final EmergencyRepository emergencyRepository;
    private final AuditLogRepository auditLogRepository;
    private final UserMapper userMapper;
    private final EmergencyMapper emergencyMapper;
    private final ReportService reportService;
    private final com.safewalk.repository.VerificationDocumentRepository verificationDocumentRepository;
    private final com.safewalk.service.FcmService fcmService;

    public AdminServiceImpl(
            UserRepository userRepository,
            EmergencyRepository emergencyRepository,
            AuditLogRepository auditLogRepository,
            UserMapper userMapper,
            EmergencyMapper emergencyMapper,
            ReportService reportService) {
        this(userRepository, emergencyRepository, auditLogRepository, userMapper, emergencyMapper, reportService, null, null);
    }

    public AdminServiceImpl(
            UserRepository userRepository,
            EmergencyRepository emergencyRepository,
            AuditLogRepository auditLogRepository,
            UserMapper userMapper,
            EmergencyMapper emergencyMapper,
            ReportService reportService,
            com.safewalk.repository.VerificationDocumentRepository verificationDocumentRepository) {
        this(userRepository, emergencyRepository, auditLogRepository, userMapper, emergencyMapper, reportService, verificationDocumentRepository, null);
    }

    @Autowired
    public AdminServiceImpl(
            UserRepository userRepository,
            EmergencyRepository emergencyRepository,
            AuditLogRepository auditLogRepository,
            UserMapper userMapper,
            EmergencyMapper emergencyMapper,
            ReportService reportService,
            @Autowired(required = false) com.safewalk.repository.VerificationDocumentRepository verificationDocumentRepository,
            @Autowired(required = false) com.safewalk.service.FcmService fcmService) {
        this.userRepository = userRepository;
        this.emergencyRepository = emergencyRepository;
        this.auditLogRepository = auditLogRepository;
        this.userMapper = userMapper;
        this.emergencyMapper = emergencyMapper;
        this.reportService = reportService;
        this.verificationDocumentRepository = verificationDocumentRepository;
        this.fcmService = fcmService;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(String search, AccountStatus status, Pageable pageable) {
        log.debug("Admin fetching users list with search='{}', status={}", search, status);

        if (StringUtils.hasText(search)) {
            return userRepository.findByNameContainingIgnoreCaseOrMobileNumberContaining(search, search, pageable)
                    .map(userMapper::toResponse);
        } else if (status != null) {
            return userRepository.findByAccountStatus(status, pageable)
                    .map(userMapper::toResponse);
        } else {
            return userRepository.findAllByOrderByCreatedAtDesc(pageable)
                    .map(userMapper::toResponse);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long userId) {
        log.debug("Admin fetching user details for ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        return userMapper.toResponse(user);
    }

    @Override
    @Transactional
    public UserResponse updateUserStatus(Long userId, AdminUserStatusUpdateRequest request, String adminMobile) {
        log.info("Admin [{}] updating account status for user ID: {} to {}", adminMobile, userId, request.getStatus());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        AccountStatus oldStatus = user.getAccountStatus();
        user.setAccountStatus(request.getStatus());
        User updatedUser = userRepository.save(user);

        // Record Audit Log
        String auditDetails = String.format("Updated account status from %s to %s. Reason: %s",
                oldStatus, request.getStatus(), request.getReason() != null ? request.getReason() : "N/A");

        AuditLog auditLog = AuditLog.builder()
                .adminMobile(adminMobile)
                .action("USER_STATUS_UPDATE")
                .targetType("USER")
                .targetId(userId)
                .details(auditDetails)
                .build();
        auditLogRepository.save(auditLog);

        log.info("Successfully updated user ID {} status to {} and logged audit trace", userId, request.getStatus());
        return userMapper.toResponse(updatedUser);
    }

    @Override
    @Transactional
    public UserResponse updatePoliceVerification(Long userId, com.safewalk.dto.request.PoliceVerificationRequest request, String adminMobile) {
        log.info("Admin [{}] updating police verification for user ID: {} to {}", adminMobile, userId, request.isVerified());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        if (user.getRole() != com.safewalk.entity.Role.POLICE) {
            log.warn("Police verification rejected. User ID: {} has role: {}", userId, user.getRole());
            throw new IllegalArgumentException("Only users with POLICE role can be verified as police.");
        }

        boolean oldVerified = user.isPoliceVerified();
        user.setPoliceVerified(request.isVerified());
        User updatedUser = userRepository.save(user);

        // Record Audit Log
        String auditDetails = String.format("Updated police verification from %s to %s. Reason: %s",
                oldVerified, request.isVerified(), request.getReason() != null ? request.getReason() : "N/A");

        AuditLog auditLog = AuditLog.builder()
                .adminMobile(adminMobile)
                .action("POLICE_VERIFICATION_UPDATE")
                .targetType("USER")
                .targetId(userId)
                .details(auditDetails)
                .build();
        auditLogRepository.save(auditLog);

        log.info("Successfully updated user ID {} police verification to {} and logged audit trace", userId, request.isVerified());
        return userMapper.toResponse(updatedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<com.safewalk.dto.response.PendingVerificationResponse> getPendingVerifications() {
        log.info("Admin fetching pending verifications for POLICE and HOSPITAL roles");
        java.util.List<User> pendingUsers = userRepository.findByVerificationStatus(com.safewalk.entity.VerificationStatus.PENDING);
        java.util.List<com.safewalk.dto.response.PendingVerificationResponse> responseList = new java.util.ArrayList<>();

        for (User u : pendingUsers) {
            if (u.getRole() == Role.POLICE || u.getRole() == Role.HOSPITAL) {
                java.util.List<com.safewalk.dto.response.VerificationDocumentResponse> docs = new java.util.ArrayList<>();
                if (verificationDocumentRepository != null) {
                    java.util.List<com.safewalk.entity.VerificationDocument> docEntities =
                            verificationDocumentRepository.findByUserIdOrderByUploadedAtDesc(u.getId());
                    for (com.safewalk.entity.VerificationDocument doc : docEntities) {
                        docs.add(com.safewalk.dto.response.VerificationDocumentResponse.builder()
                                .id(doc.getId())
                                .userId(u.getId())
                                .title(doc.getTitle())
                                .fileName(doc.getFileName())
                                .mimeType(doc.getMimeType())
                                .fileSize(doc.getFileSize())
                                .uploadedAt(doc.getUploadedAt())
                                .build());
                    }
                }

                responseList.add(com.safewalk.dto.response.PendingVerificationResponse.builder()
                        .userId(u.getId())
                        .name(u.getName())
                        .mobileNumber(u.getMobileNumber())
                        .role(u.getRole())
                        .verificationStatus(u.getVerificationStatus())
                        .registeredAt(u.getCreatedAt())
                        .documents(docs)
                        .build());
            }
        }

        return responseList;
    }

    @Override
    @Transactional
    public UserResponse updateVerificationStatus(Long userId, com.safewalk.dto.request.AdminVerificationRequest request, String adminMobile) {
        log.info("Admin [{}] updating verification status for user ID: {} to {}", adminMobile, userId, request.getStatus());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        if (user.getRole() != Role.POLICE && user.getRole() != Role.HOSPITAL) {
            log.warn("Verification update rejected. User ID: {} has role: {}", userId, user.getRole());
            throw new IllegalArgumentException("Only users with POLICE or HOSPITAL role can undergo authority verification.");
        }

        com.safewalk.entity.VerificationStatus oldStatus = user.getVerificationStatus();
        user.setVerificationStatus(request.getStatus());

        if (user.getRole() == Role.POLICE) {
            user.setPoliceVerified(request.getStatus() == com.safewalk.entity.VerificationStatus.VERIFIED);
        }

        if (request.getStatus() == com.safewalk.entity.VerificationStatus.REJECTED && request.getReason() != null) {
            user.setRejectionReason(request.getReason().trim());
        } else if (request.getStatus() == com.safewalk.entity.VerificationStatus.VERIFIED) {
            user.setRejectionReason(null);
        }

        User updatedUser = userRepository.save(user);

        // Record Audit Log
        String auditDetails = String.format("Updated %s verification status from %s to %s. Reason: %s",
                user.getRole(), oldStatus, request.getStatus(), request.getReason() != null ? request.getReason() : "N/A");

        AuditLog auditLog = AuditLog.builder()
                .adminMobile(adminMobile)
                .action("USER_VERIFICATION_UPDATE")
                .targetType("USER")
                .targetId(userId)
                .details(auditDetails)
                .build();
        auditLogRepository.save(auditLog);

        // Safe FCM notification delivery (non-fatal if Firebase is not configured)
        if (fcmService != null) {
            try {
                java.util.Map<String, String> notifData = new java.util.HashMap<>();
                notifData.put("type", "VERIFICATION_UPDATED");
                notifData.put("verificationStatus", request.getStatus().name());
                notifData.put("status", request.getStatus().name());
                if (request.getReason() != null) {
                    notifData.put("rejectionReason", request.getReason());
                }
                String notifTitle = "SafeWalk Verification Update";
                String notifBody = "Your " + user.getRole() + " verification status is now " + request.getStatus();
                fcmService.sendPushNotificationToUser(user.getId(), notifTitle, notifBody, notifData);
            } catch (Exception e) {
                log.warn("Non-fatal: Failed to deliver FCM push notification for verification update to user ID {}: {}", user.getId(), e.getMessage());
            }
        }

        log.info("Successfully updated user ID {} verification status to {} and logged audit trace", userId, request.getStatus());
        return userMapper.toResponse(updatedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EmergencyResponse> getAllEmergencies(EmergencyStatus status, EmergencyType type, Pageable pageable) {
        log.debug("Admin fetching emergencies list with status={}, type={}", status, type);

        if (status != null && type != null) {
            return emergencyRepository.findByStatusAndType(status, type, pageable)
                    .map(emergencyMapper::toResponse);
        } else if (status != null) {
            return emergencyRepository.findByStatus(status, pageable)
                    .map(emergencyMapper::toResponse);
        } else if (type != null) {
            return emergencyRepository.findByType(type, pageable)
                    .map(emergencyMapper::toResponse);
        } else {
            return emergencyRepository.findAll(pageable)
                    .map(emergencyMapper::toResponse);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public EmergencyResponse getEmergencyById(Long emergencyId) {
        log.debug("Admin fetching emergency details for ID: {}", emergencyId);

        Emergency emergency = emergencyRepository.findById(emergencyId)
                .orElseThrow(() -> new ResourceNotFoundException("Emergency request not found with ID: " + emergencyId));

        return emergencyMapper.toResponse(emergency);
    }

    @Override
    @Transactional
    public EmergencyResponse cancelEmergencyByAdmin(Long emergencyId, String reason, String adminMobile) {
        log.info("Admin [{}] cancelling emergency ID: {}", adminMobile, emergencyId);

        Emergency emergency = emergencyRepository.findById(emergencyId)
                .orElseThrow(() -> new ResourceNotFoundException("Emergency request not found with ID: " + emergencyId));

        if (emergency.getStatus() == EmergencyStatus.CANCELLED || emergency.getStatus() == EmergencyStatus.RESOLVED) {
            throw new IllegalStateException("Cannot cancel an emergency request that is already " + emergency.getStatus());
        }

        emergency.setStatus(EmergencyStatus.CANCELLED);
        emergency.setCancelledAt(LocalDateTime.now());
        Emergency updatedEmergency = emergencyRepository.save(emergency);

        AuditLog auditLog = AuditLog.builder()
                .adminMobile(adminMobile)
                .action("EMERGENCY_CANCEL")
                .targetType("EMERGENCY")
                .targetId(emergencyId)
                .details("Emergency cancelled by admin. Reason: " + (reason != null ? reason : "N/A"))
                .build();
        auditLogRepository.save(auditLog);

        return emergencyMapper.toResponse(updatedEmergency);
    }

    @Override
    @Transactional
    public EmergencyResponse resolveEmergencyByAdmin(Long emergencyId, String reason, String adminMobile) {
        log.info("Admin [{}] resolving emergency ID: {}", adminMobile, emergencyId);

        Emergency emergency = emergencyRepository.findById(emergencyId)
                .orElseThrow(() -> new ResourceNotFoundException("Emergency request not found with ID: " + emergencyId));

        if (emergency.getStatus() == EmergencyStatus.CANCELLED || emergency.getStatus() == EmergencyStatus.RESOLVED) {
            throw new IllegalStateException("Cannot resolve an emergency request that is already " + emergency.getStatus());
        }

        emergency.setStatus(EmergencyStatus.RESOLVED);
        emergency.setResolvedAt(LocalDateTime.now());
        Emergency updatedEmergency = emergencyRepository.save(emergency);

        AuditLog auditLog = AuditLog.builder()
                .adminMobile(adminMobile)
                .action("EMERGENCY_RESOLVE")
                .targetType("EMERGENCY")
                .targetId(emergencyId)
                .details("Emergency resolved by admin. Reason: " + (reason != null ? reason : "N/A"))
                .build();
        auditLogRepository.save(auditLog);

        return emergencyMapper.toResponse(updatedEmergency);
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardReportResponse getAdminDashboard() {
        log.debug("Admin fetching system dashboard");
        return reportService.getDashboardReport(null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getAuditLogs(Pageable pageable) {
        log.debug("Admin fetching audit logs");
        return auditLogRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(logEntity -> AuditLogResponse.builder()
                        .id(logEntity.getId())
                        .adminMobile(logEntity.getAdminMobile())
                        .action(logEntity.getAction())
                        .targetType(logEntity.getTargetType())
                        .targetId(logEntity.getTargetId())
                        .details(logEntity.getDetails())
                        .createdAt(logEntity.getCreatedAt())
                        .build());
    }
}
