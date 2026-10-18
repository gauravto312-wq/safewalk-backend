package com.safewalk.service;

import com.safewalk.dto.request.PoliceVerificationRequest;
import com.safewalk.dto.response.UserResponse;
import com.safewalk.entity.AccountStatus;
import com.safewalk.entity.AuditLog;
import com.safewalk.entity.Role;
import com.safewalk.entity.User;
import com.safewalk.mapper.EmergencyMapper;
import com.safewalk.mapper.UserMapper;
import com.safewalk.repository.AuditLogRepository;
import com.safewalk.repository.EmergencyRepository;
import com.safewalk.repository.UserRepository;
import com.safewalk.service.impl.AdminServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminPoliceVerificationTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmergencyRepository emergencyRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    private UserMapper userMapper;
    private EmergencyMapper emergencyMapper;
    private AdminServiceImpl adminService;

    private User policeOfficer;
    private User normalUser;

    @BeforeEach
    void setUp() {
        userMapper = new UserMapper();
        emergencyMapper = new EmergencyMapper();
        adminService = new AdminServiceImpl(
                userRepository, emergencyRepository, auditLogRepository, userMapper, emergencyMapper, null);

        policeOfficer = User.builder()
                .id(100L)
                .name("Inspector Vijay")
                .mobileNumber("+919999911111")
                .role(Role.POLICE)
                .policeVerified(false)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        normalUser = User.builder()
                .id(200L)
                .name("Rohit Civilian")
                .mobileNumber("+919999922222")
                .role(Role.USER)
                .policeVerified(false)
                .accountStatus(AccountStatus.ACTIVE)
                .build();
    }

    @Test
    void updatePoliceVerification_VerifyPolice_Success() {
        PoliceVerificationRequest request = PoliceVerificationRequest.builder()
                .verified(true)
                .reason("Credentials and badge number verified by HQ")
                .build();

        when(userRepository.findById(100L)).thenReturn(Optional.of(policeOfficer));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        UserResponse response = adminService.updatePoliceVerification(100L, request, "+919999900000");

        assertNotNull(response);
        assertTrue(response.isPoliceVerified());

        ArgumentCaptor<AuditLog> logCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(logCaptor.capture());
        AuditLog logged = logCaptor.getValue();
        assertEquals("POLICE_VERIFICATION_UPDATE", logged.getAction());
        assertEquals(100L, logged.getTargetId());
        assertTrue(logged.getDetails().contains("Updated police verification from false to true"));
    }

    @Test
    void updatePoliceVerification_UnverifyPolice_Success() {
        policeOfficer.setPoliceVerified(true);

        PoliceVerificationRequest request = PoliceVerificationRequest.builder()
                .verified(false)
                .reason("Suspended pending inquiry")
                .build();

        when(userRepository.findById(100L)).thenReturn(Optional.of(policeOfficer));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        UserResponse response = adminService.updatePoliceVerification(100L, request, "+919999900000");

        assertNotNull(response);
        assertFalse(response.isPoliceVerified());
    }

    @Test
    void updatePoliceVerification_NonPoliceUser_ThrowsIllegalArgumentException() {
        PoliceVerificationRequest request = PoliceVerificationRequest.builder()
                .verified(true)
                .build();

        when(userRepository.findById(200L)).thenReturn(Optional.of(normalUser));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                adminService.updatePoliceVerification(200L, request, "+919999900000"));

        assertTrue(ex.getMessage().contains("Only users with POLICE role can be verified as police"));
    }
}
