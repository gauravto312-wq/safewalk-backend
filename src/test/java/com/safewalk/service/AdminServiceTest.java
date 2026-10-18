package com.safewalk.service;

import com.safewalk.dto.request.AdminUserStatusUpdateRequest;
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
import com.safewalk.service.impl.AdminServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmergencyRepository emergencyRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private ReportService reportService;

    private UserMapper userMapper;
    private EmergencyMapper emergencyMapper;
    private AdminServiceImpl adminService;

    private User targetUser;
    private Emergency targetEmergency;

    @BeforeEach
    void setUp() {
        userMapper = new UserMapper();
        emergencyMapper = new EmergencyMapper();
        adminService = new AdminServiceImpl(userRepository, emergencyRepository, auditLogRepository, userMapper, emergencyMapper, reportService);

        targetUser = User.builder()
                .id(10L)
                .name("Test User")
                .mobileNumber("+919876543210")
                .role(Role.USER)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        targetEmergency = Emergency.builder()
                .id(100L)
                .requester(targetUser)
                .type(EmergencyType.GENERAL)
                .status(EmergencyStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void getUserById_Success() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(targetUser));

        UserResponse response = adminService.getUserById(10L);

        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals("Test User", response.getName());
    }

    @Test
    void getUserById_NotFound_ThrowsException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> adminService.getUserById(99L));
    }

    @Test
    void updateUserStatus_Success_AndLogsAudit() {
        AdminUserStatusUpdateRequest request = new AdminUserStatusUpdateRequest(AccountStatus.SUSPENDED, "Repeated violations");

        when(userRepository.findById(10L)).thenReturn(Optional.of(targetUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        UserResponse response = adminService.updateUserStatus(10L, request, "+919999999999");

        assertNotNull(response);
        assertEquals(AccountStatus.SUSPENDED, response.getAccountStatus());
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void cancelEmergencyByAdmin_Success_AndLogsAudit() {
        when(emergencyRepository.findById(100L)).thenReturn(Optional.of(targetEmergency));
        when(emergencyRepository.save(any(Emergency.class))).thenAnswer(i -> i.getArgument(0));

        EmergencyResponse response = adminService.cancelEmergencyByAdmin(100L, "Safety concern resolved", "+919999999999");

        assertNotNull(response);
        assertEquals(EmergencyStatus.CANCELLED, response.getStatus());
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void resolveEmergencyByAdmin_Success_AndLogsAudit() {
        when(emergencyRepository.findById(100L)).thenReturn(Optional.of(targetEmergency));
        when(emergencyRepository.save(any(Emergency.class))).thenAnswer(i -> i.getArgument(0));

        EmergencyResponse response = adminService.resolveEmergencyByAdmin(100L, "Resolved by admin team", "+919999999999");

        assertNotNull(response);
        assertEquals(EmergencyStatus.RESOLVED, response.getStatus());
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void cancelEmergencyByAdmin_AlreadyResolved_ThrowsIllegalState() {
        targetEmergency.setStatus(EmergencyStatus.RESOLVED);
        when(emergencyRepository.findById(100L)).thenReturn(Optional.of(targetEmergency));

        assertThrows(IllegalStateException.class, () -> adminService.cancelEmergencyByAdmin(100L, "Cancelled", "+919999999999"));
    }
}
