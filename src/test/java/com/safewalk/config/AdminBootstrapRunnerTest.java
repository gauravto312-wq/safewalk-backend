package com.safewalk.config;

import com.safewalk.entity.AccountStatus;
import com.safewalk.entity.AuditLog;
import com.safewalk.entity.Role;
import com.safewalk.entity.User;
import com.safewalk.entity.VerificationStatus;
import com.safewalk.repository.AuditLogRepository;
import com.safewalk.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapRunnerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuditLogRepository auditLogRepository;

    private static final String ADMIN_EMAIL = "admin@safewalk.in";
    private static final String ADMIN_MOBILE = "+919999999999";
    private static final String ADMIN_PASSWORD = "safewalkadmin";
    private static final String ADMIN_NAME = "System Administrator";

    @BeforeEach
    void setUp() {
        lenient().when(passwordEncoder.encode(ADMIN_PASSWORD)).thenReturn("$2a$10$hashedSafeWalkAdminPasswordMock");
    }

    @Test
    @DisplayName("Admin bootstrap provisions administrator with admin@safewalk.in and Role.ADMIN when enabled")
    void testAdminBootstrap_createsAdminAccount_whenEnabledAndNotPresent() {
        when(userRepository.existsByEmail(ADMIN_EMAIL)).thenReturn(false);
        when(userRepository.existsByMobileNumber(ADMIN_MOBILE)).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(100L);
            return u;
        });

        AdminBootstrapRunner runner = new AdminBootstrapRunner(
                userRepository, passwordEncoder, auditLogRepository,
                true, ADMIN_MOBILE, ADMIN_PASSWORD, ADMIN_NAME, ADMIN_EMAIL
        );

        runner.run();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedAdmin = userCaptor.getValue();

        assertEquals(ADMIN_EMAIL, savedAdmin.getEmail());
        assertEquals(ADMIN_MOBILE, savedAdmin.getMobileNumber());
        assertEquals(Role.ADMIN, savedAdmin.getRole());
        assertEquals(AccountStatus.ACTIVE, savedAdmin.getAccountStatus());
        assertEquals(VerificationStatus.VERIFIED, savedAdmin.getVerificationStatus());
        assertTrue(savedAdmin.isPoliceVerified());
        assertEquals("$2a$10$hashedSafeWalkAdminPasswordMock", savedAdmin.getPassword());
        assertEquals(ADMIN_NAME, savedAdmin.getName());

        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("Admin bootstrap skips creation idempotently if email admin@safewalk.in already exists")
    void testAdminBootstrap_skipsWhenEmailAlreadyExists_idempotency() {
        when(userRepository.existsByEmail(ADMIN_EMAIL)).thenReturn(true);

        AdminBootstrapRunner runner = new AdminBootstrapRunner(
                userRepository, passwordEncoder, auditLogRepository,
                true, ADMIN_MOBILE, ADMIN_PASSWORD, ADMIN_NAME, ADMIN_EMAIL
        );

        runner.run();

        verify(userRepository, never()).save(any(User.class));
        verify(auditLogRepository, never()).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("Admin bootstrap skips creation idempotently if mobile number already exists")
    void testAdminBootstrap_skipsWhenMobileAlreadyExists_idempotency() {
        when(userRepository.existsByEmail(ADMIN_EMAIL)).thenReturn(false);
        when(userRepository.existsByMobileNumber(ADMIN_MOBILE)).thenReturn(true);

        AdminBootstrapRunner runner = new AdminBootstrapRunner(
                userRepository, passwordEncoder, auditLogRepository,
                true, ADMIN_MOBILE, ADMIN_PASSWORD, ADMIN_NAME, ADMIN_EMAIL
        );

        runner.run();

        // Should not create new admin
        verify(userRepository, never()).save(argThat(user -> user.getId() == null));
    }

    @Test
    @DisplayName("Admin bootstrap does nothing when disabled")
    void testAdminBootstrap_disabled_doesNothing() {
        AdminBootstrapRunner runner = new AdminBootstrapRunner(
                userRepository, passwordEncoder, auditLogRepository,
                false, ADMIN_MOBILE, ADMIN_PASSWORD, ADMIN_NAME, ADMIN_EMAIL
        );

        runner.run();

        verify(userRepository, never()).existsByEmail(any());
        verify(userRepository, never()).existsByMobileNumber(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Admin bootstrap aborts safely when password is missing")
    void testAdminBootstrap_missingPassword_abortsSafely() {
        AdminBootstrapRunner runner = new AdminBootstrapRunner(
                userRepository, passwordEncoder, auditLogRepository,
                true, ADMIN_MOBILE, "", ADMIN_NAME, ADMIN_EMAIL
        );

        runner.run();

        verify(userRepository, never()).existsByEmail(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Admin bootstrap aborts safely when password is less than 8 characters")
    void testAdminBootstrap_shortPassword_abortsSafely() {
        AdminBootstrapRunner runner = new AdminBootstrapRunner(
                userRepository, passwordEncoder, auditLogRepository,
                true, ADMIN_MOBILE, "short", ADMIN_NAME, ADMIN_EMAIL
        );

        runner.run();

        verify(userRepository, never()).existsByEmail(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Admin bootstrap uses default administrative mobile if mobile is not provided")
    void testAdminBootstrap_defaultMobileApplied_whenMobileBlank() {
        when(userRepository.existsByEmail(ADMIN_EMAIL)).thenReturn(false);
        when(userRepository.existsByMobileNumber("+919999999999")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        AdminBootstrapRunner runner = new AdminBootstrapRunner(
                userRepository, passwordEncoder, auditLogRepository,
                true, "", ADMIN_PASSWORD, ADMIN_NAME, ADMIN_EMAIL
        );

        runner.run();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals("+919999999999", userCaptor.getValue().getMobileNumber());
        assertEquals(ADMIN_EMAIL, userCaptor.getValue().getEmail());
        assertEquals(Role.ADMIN, userCaptor.getValue().getRole());
    }
}
