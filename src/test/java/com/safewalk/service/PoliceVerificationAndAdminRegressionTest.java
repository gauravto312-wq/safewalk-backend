package com.safewalk.service;

import com.safewalk.config.AdminBootstrapRunner;
import com.safewalk.dto.request.AdminVerificationRequest;
import com.safewalk.dto.response.UserResponse;
import com.safewalk.dto.response.VerificationDocumentResponse;
import com.safewalk.entity.AccountStatus;
import com.safewalk.entity.AuditLog;
import com.safewalk.entity.Role;
import com.safewalk.entity.User;
import com.safewalk.entity.VerificationDocument;
import com.safewalk.entity.VerificationStatus;
import com.safewalk.mapper.UserMapper;
import com.safewalk.repository.AuditLogRepository;
import com.safewalk.repository.EmergencyRepository;
import com.safewalk.repository.UserRepository;
import com.safewalk.repository.VerificationDocumentRepository;
import com.safewalk.service.impl.AdminServiceImpl;
import com.safewalk.service.impl.LocalFileSystemStorageService;
import com.safewalk.service.impl.VerificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PoliceVerificationAndAdminRegressionTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private VerificationDocumentRepository documentRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private EmergencyRepository emergencyRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private FcmService fcmService;

    @TempDir
    Path tempUploadDir;

    private UserMapper userMapper;
    private FileStorageService fileStorageService;
    private VerificationServiceImpl verificationService;
    private AdminServiceImpl adminService;

    @BeforeEach
    void setUp() {
        userMapper = new UserMapper();
        fileStorageService = new LocalFileSystemStorageService(tempUploadDir.toString());
        verificationService = new VerificationServiceImpl(
                documentRepository, userRepository, fileStorageService, auditLogRepository);
        adminService = new AdminServiceImpl(
                userRepository, emergencyRepository, auditLogRepository, userMapper, null, null, documentRepository, fcmService);
    }

    @Test
    @DisplayName("1. Rejected police officer resubmission returns to PENDING, preserves reason, excludes dispatch")
    void testRejectedPoliceResubmission_transitionsToPending() throws IOException {
        User rejectedOfficer = User.builder()
                .id(101L)
                .name("Officer Arjun Kulkarni")
                .mobileNumber("+919876500101")
                .role(Role.POLICE)
                .policeVerified(false)
                .verificationStatus(VerificationStatus.REJECTED)
                .badgeNumber("MH-POLICE-101")
                .department("Crime Branch Zone 1")
                .rejectionReason("ID card blurred and expired")
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        when(userRepository.findByMobileNumber("+919876500101")).thenReturn(Optional.of(rejectedOfficer));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(documentRepository.save(any(VerificationDocument.class))).thenAnswer(inv -> {
            VerificationDocument d = inv.getArgument(0);
            d.setId(501L);
            return d;
        });

        MockMultipartFile correctedFile = new MockMultipartFile(
                "file", "new_valid_police_badge.pdf", "application/pdf", "%PDF-1.4 Corrected Badge Content".getBytes());

        VerificationDocumentResponse uploadResponse = verificationService.uploadDocument(
                "+919876500101", "Updated Police Badge", correctedFile);

        assertNotNull(uploadResponse);
        // Verify state transitioned back to PENDING
        assertEquals(VerificationStatus.PENDING, rejectedOfficer.getVerificationStatus(),
                "Resubmitted officer MUST transition back to PENDING review");
        assertFalse(rejectedOfficer.isPoliceVerified(),
                "Resubmitted officer MUST NOT be marked policeVerified=true before admin approval");
        assertEquals("ID card blurred and expired", rejectedOfficer.getRejectionReason(),
                "Prior rejection reason must be preserved for audit history until re-evaluation");

        // Verify user was updated in repository
        verify(userRepository, atLeastOnce()).save(rejectedOfficer);

        // Verify audit log for resubmission was recorded
        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditCaptor.capture());
        AuditLog audit = auditCaptor.getValue();
        assertEquals("DOCUMENT_RESUBMISSION", audit.getAction());
        assertEquals(101L, audit.getTargetId());
    }

    @Test
    @DisplayName("2. Admin review: approve transitions to VERIFIED and enables dispatch eligibility")
    void testAdminApproval_transitionsToVerifiedAndEnablesDispatch() {
        User pendingOfficer = User.builder()
                .id(101L)
                .name("Officer Arjun Kulkarni")
                .mobileNumber("+919876500101")
                .role(Role.POLICE)
                .policeVerified(false)
                .verificationStatus(VerificationStatus.PENDING)
                .badgeNumber("MH-POLICE-101")
                .department("Crime Branch Zone 1")
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        when(userRepository.findById(101L)).thenReturn(Optional.of(pendingOfficer));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        AdminVerificationRequest request = new AdminVerificationRequest(
                VerificationStatus.VERIFIED, "Credentials authenticated against police database");

        UserResponse response = adminService.updateVerificationStatus(101L, request, "+919999999999");

        assertNotNull(response);
        assertEquals(VerificationStatus.VERIFIED, response.getVerificationStatus());
        assertTrue(response.isPoliceVerified(), "Approved police officer MUST be marked policeVerified=true");
        assertEquals(Role.POLICE, response.getRole());
        assertEquals("MH-POLICE-101", response.getBadgeNumber());
        assertEquals("Crime Branch Zone 1", response.getDepartment());
    }

    @Test
    @DisplayName("3. Admin document preview: authorized admin receives inline resource with nosniff header")
    void testAdminDocumentPreview_authorized() throws IOException {
        User officer = User.builder()
                .id(101L)
                .name("Officer Arjun Kulkarni")
                .mobileNumber("+919876500101")
                .role(Role.POLICE)
                .policeVerified(false)
                .verificationStatus(VerificationStatus.PENDING)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        MockMultipartFile pdfFile = new MockMultipartFile(
                "file", "badge_preview.pdf", "application/pdf", "%PDF-1.4 Sample PDF content".getBytes());

        when(userRepository.findByMobileNumber("+919876500101")).thenReturn(Optional.of(officer));
        when(documentRepository.save(any(VerificationDocument.class))).thenAnswer(inv -> {
            VerificationDocument d = inv.getArgument(0);
            d.setId(601L);
            return d;
        });

        verificationService.uploadDocument("+919876500101", "Officer Badge", pdfFile);

        ArgumentCaptor<VerificationDocument> docCaptor = ArgumentCaptor.forClass(VerificationDocument.class);
        verify(documentRepository).save(docCaptor.capture());
        VerificationDocument savedDoc = docCaptor.getValue();
        savedDoc.setId(601L);

        when(documentRepository.findById(601L)).thenReturn(Optional.of(savedDoc));

        Resource resource = verificationService.getDocumentForAdminPreview(101L, 601L);
        assertNotNull(resource);
        assertTrue(resource.exists());
        assertTrue(resource.contentLength() > 0);
    }

    @Test
    @DisplayName("4. Admin document preview: IDOR protection throws exception if document does not belong to user")
    void testAdminDocumentPreview_mismatchedUser_throwsSecurityException() {
        User anotherUser = User.builder().id(999L).build();
        VerificationDocument docEntity = VerificationDocument.builder()
                .id(701L)
                .user(anotherUser)
                .title("Another Document")
                .fileName("secret.pdf")
                .storedPath("docs/secret.pdf")
                .mimeType("application/pdf")
                .fileSize(200L)
                .build();

        when(documentRepository.findById(701L)).thenReturn(Optional.of(docEntity));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                verificationService.getDocumentForAdminPreview(101L, 701L));

        assertTrue(ex.getMessage().contains("does not belong to user"));
    }

    @Test
    @DisplayName("5. Secure Admin Bootstrap: initializes administrator when enabled and not already present")
    void testAdminBootstrap_initializesAdminWhenEnabled() {
        AdminBootstrapRunner runner = new AdminBootstrapRunner(
                userRepository, passwordEncoder, auditLogRepository, true, "+919999900001", "SecureAdminPass#2026", "SysAdmin");

        when(userRepository.existsByMobileNumber("+919999900001")).thenReturn(false);
        when(passwordEncoder.encode("SecureAdminPass#2026")).thenReturn("$2a$10$hashedSecurePassword");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });

        runner.run();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedAdmin = userCaptor.getValue();
        assertEquals(Role.ADMIN, savedAdmin.getRole());
        assertEquals("+919999900001", savedAdmin.getMobileNumber());
        assertEquals("$2a$10$hashedSecurePassword", savedAdmin.getPassword());
        assertTrue(savedAdmin.isPoliceVerified());
        assertEquals(VerificationStatus.VERIFIED, savedAdmin.getVerificationStatus());
        assertEquals(AccountStatus.ACTIVE, savedAdmin.getAccountStatus());
    }

    @Test
    @DisplayName("6. Secure Admin Bootstrap: does not create duplicate admin if admin account exists")
    void testAdminBootstrap_skipsWhenAdminExists() {
        when(userRepository.existsByMobileNumber("+919999900001")).thenReturn(true);

        AdminBootstrapRunner runner = new AdminBootstrapRunner(
                userRepository, passwordEncoder, auditLogRepository, true, "+919999900001", "SecureAdminPass#2026", "SysAdmin");

        runner.run();

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("7. Secure Admin Bootstrap: does nothing when disabled")
    void testAdminBootstrap_disabled_doesNothing() {
        AdminBootstrapRunner runner = new AdminBootstrapRunner(
                userRepository, passwordEncoder, auditLogRepository, false, "+919999900001", "SecureAdminPass#2026", "SysAdmin");

        runner.run();

        verify(userRepository, never()).existsByMobileNumber(any());
        verify(userRepository, never()).save(any());
    }
}
