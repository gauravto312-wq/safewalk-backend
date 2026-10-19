package com.safewalk.config;

import com.safewalk.entity.AccountStatus;
import com.safewalk.entity.AuditLog;
import com.safewalk.entity.Role;
import com.safewalk.entity.User;
import com.safewalk.entity.VerificationStatus;
import com.safewalk.repository.AuditLogRepository;
import com.safewalk.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Secure, operator-triggered bootstrap runner for provisioning the initial administrator account.
 *
 * Requirements satisfied:
 * - Public registration remains blocked for ADMIN role.
 * - No hardcoded default administrator credentials.
 * - Password hashed using established BCryptPasswordEncoder.
 * - No duplicate administrator created if already present.
 * - Bootstrap credentials or secrets are never logged.
 * - Fails safely if disabled or if credentials are not provided.
 */
@Component
public class AdminBootstrapRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogRepository auditLogRepository;

    private final boolean enabled;
    private final String adminMobile;
    private final String adminPassword;
    private final String adminName;
    private final String adminEmail;

    public AdminBootstrapRunner(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuditLogRepository auditLogRepository,
            boolean enabled,
            String adminMobile,
            String adminPassword,
            String adminName) {
        this(userRepository, passwordEncoder, auditLogRepository, enabled, adminMobile, adminPassword, adminName, null);
    }

    @Autowired
    public AdminBootstrapRunner(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuditLogRepository auditLogRepository,
            @Value("${safewalk.admin.bootstrap.enabled:${ADMIN_BOOTSTRAP_ENABLED:false}}") boolean enabled,
            @Value("${safewalk.admin.bootstrap.mobile-number:${ADMIN_BOOTSTRAP_MOBILE:+919999999999}}") String adminMobile,
            @Value("${safewalk.admin.bootstrap.password:${ADMIN_BOOTSTRAP_PASSWORD:}}") String adminPassword,
            @Value("${safewalk.admin.bootstrap.name:${ADMIN_BOOTSTRAP_NAME:System Administrator}}") String adminName,
            @Value("${safewalk.admin.bootstrap.email:${ADMIN_BOOTSTRAP_EMAIL:admin@safewalk.in}}") String adminEmail) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogRepository = auditLogRepository;
        this.enabled = enabled;
        this.adminMobile = adminMobile;
        this.adminPassword = adminPassword;
        this.adminName = adminName;
        this.adminEmail = adminEmail;
    }

    @Override
    public void run(String... args) {
        if (!enabled) {
            log.debug("Secure admin bootstrap is disabled (safewalk.admin.bootstrap.enabled=false).");
            return;
        }

        if (!StringUtils.hasText(adminPassword)) {
            log.warn("Secure admin bootstrap is enabled, but admin password configuration is missing. Bootstrap aborted safely.");
            return;
        }

        String cleanMobile = StringUtils.hasText(adminMobile) ? adminMobile.trim() : "+919999999999";
        String cleanEmail = StringUtils.hasText(adminEmail) ? adminEmail.trim().toLowerCase() : null;

        if (cleanMobile.length() < 8) {
            log.warn("Invalid admin mobile number format for bootstrap. Minimum 8 characters required.");
            return;
        }

        if (adminPassword.length() < 8) {
            log.warn("Insecure admin bootstrap password: minimum 8 characters required. Bootstrap aborted.");
            return;
        }

        // Idempotency Check 1: Do not create duplicate administrator if email is already registered
        if (cleanEmail != null && userRepository.existsByEmail(cleanEmail)) {
            log.info("Administrator account already exists for email [{}]. Bootstrap creation skipped (idempotent).", cleanEmail);
            return;
        }

        // Idempotency Check 2: Do not create duplicate administrator if mobile is already registered
        if (userRepository.existsByMobileNumber(cleanMobile)) {
            log.info("Administrator account already exists for mobile [{}]. Updating email mapping if provided.", maskMobile(cleanMobile));
            if (cleanEmail != null) {
                userRepository.findByMobileNumber(cleanMobile).ifPresent(user -> {
                    if (user.getEmail() == null || !user.getEmail().equalsIgnoreCase(cleanEmail)) {
                        user.setEmail(cleanEmail);
                        userRepository.save(user);
                        log.info("Updated administrator email mapping to [{}]", cleanEmail);
                    }
                });
            }
            return;
        }

        User adminUser = User.builder()
                .name(StringUtils.hasText(adminName) ? adminName.trim() : "System Administrator")
                .mobileNumber(cleanMobile)
                .email(cleanEmail)
                .password(passwordEncoder.encode(adminPassword))
                .role(Role.ADMIN)
                .accountStatus(AccountStatus.ACTIVE)
                .verificationStatus(VerificationStatus.VERIFIED)
                .policeVerified(true)
                .build();

        User savedAdmin = userRepository.save(adminUser);

        AuditLog auditLog = AuditLog.builder()
                .adminMobile(cleanMobile)
                .action("ADMIN_BOOTSTRAP")
                .targetType("USER")
                .targetId(savedAdmin.getId())
                .details("Initial administrator account provisioned via secure operator bootstrap.")
                .build();
        auditLogRepository.save(auditLog);

        log.info("Initial administrator account successfully provisioned (ID: {}). Bootstrap completed.", savedAdmin.getId());
    }

    private String maskMobile(String mobile) {
        if (mobile == null || mobile.length() < 4) return "***";
        return mobile.substring(0, 2) + "****" + mobile.substring(mobile.length() - 2);
    }
}
