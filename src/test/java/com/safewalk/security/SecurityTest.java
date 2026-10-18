package com.safewalk.security;

import com.safewalk.entity.AccountStatus;
import com.safewalk.entity.Role;
import com.safewalk.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SecurityTest {

    private static final String TEST_JWT_SECRET = "test_only_secret_key_that_is_at_least_256_bits_long_for_testing_purposes_only_123456";
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(
                TEST_JWT_SECRET,
                3600000L
        );
    }

    @Test
    void validateToken_ValidToken_ReturnsTrue() {
        User user = User.builder()
                .id(1L)
                .name("Aman Test")
                .mobileNumber("+919876543210")
                .role(Role.USER)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        String token = jwtTokenProvider.generateToken(user);
        assertNotNull(token);
        assertTrue(jwtTokenProvider.validateToken(token));
        assertEquals("+919876543210", jwtTokenProvider.getMobileNumberFromToken(token));
        assertEquals(1L, jwtTokenProvider.getUserIdFromToken(token));
        assertEquals("USER", jwtTokenProvider.getRoleFromToken(token));
    }

    @Test
    void validateToken_InvalidToken_ReturnsFalse() {
        assertFalse(jwtTokenProvider.validateToken("invalid.jwt.token"));
        assertFalse(jwtTokenProvider.validateToken(""));
        assertFalse(jwtTokenProvider.validateToken(null));
    }

    @Test
    void validateToken_ExpiredToken_ReturnsFalse() {
        // Create token provider with 1ms expiration
        JwtTokenProvider shortLivedProvider = new JwtTokenProvider(
                TEST_JWT_SECRET,
                1L
        );

        User user = User.builder()
                .id(1L)
                .name("Aman Test")
                .mobileNumber("+919876543210")
                .role(Role.USER)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        String token = shortLivedProvider.generateToken(user);
        try {
            Thread.sleep(10);
        } catch (InterruptedException ignored) {}

        assertFalse(shortLivedProvider.validateToken(token));
    }

    @Test
    void userPrincipal_AccountStatusRestrictions() {
        User activeUser = User.builder().id(1L).accountStatus(AccountStatus.ACTIVE).role(Role.USER).build();
        UserPrincipal activePrincipal = UserPrincipal.create(activeUser);
        assertTrue(activePrincipal.isEnabled());
        assertTrue(activePrincipal.isAccountNonLocked());

        User warningUser = User.builder().id(2L).accountStatus(AccountStatus.WARNING).role(Role.USER).build();
        UserPrincipal warningPrincipal = UserPrincipal.create(warningUser);
        assertTrue(warningPrincipal.isEnabled());
        assertTrue(warningPrincipal.isAccountNonLocked());

        User restrictedUser = User.builder().id(3L).accountStatus(AccountStatus.RESTRICTED).role(Role.USER).build();
        UserPrincipal restrictedPrincipal = UserPrincipal.create(restrictedUser);
        assertTrue(restrictedPrincipal.isEnabled());
        assertTrue(restrictedPrincipal.isAccountNonLocked());

        User suspendedUser = User.builder().id(4L).accountStatus(AccountStatus.SUSPENDED).role(Role.USER).build();
        UserPrincipal suspendedPrincipal = UserPrincipal.create(suspendedUser);
        assertTrue(suspendedPrincipal.isEnabled());
        assertFalse(suspendedPrincipal.isAccountNonLocked());

        User deactivatedUser = User.builder().id(5L).accountStatus(AccountStatus.DEACTIVATED).role(Role.USER).build();
        UserPrincipal deactivatedPrincipal = UserPrincipal.create(deactivatedUser);
        assertFalse(deactivatedPrincipal.isEnabled());
        assertFalse(deactivatedPrincipal.isAccountNonLocked());
    }

    @Test
    void userPrincipal_RoleAuthoritiesMapping() {
        User user = User.builder().id(1L).role(Role.USER).accountStatus(AccountStatus.ACTIVE).build();
        UserPrincipal userPrincipal = UserPrincipal.create(user);
        assertTrue(userPrincipal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_USER")));

        User admin = User.builder().id(2L).role(Role.ADMIN).accountStatus(AccountStatus.ACTIVE).build();
        UserPrincipal adminPrincipal = UserPrincipal.create(admin);
        assertTrue(adminPrincipal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));

        User police = User.builder().id(3L).role(Role.POLICE).accountStatus(AccountStatus.ACTIVE).build();
        UserPrincipal policePrincipal = UserPrincipal.create(police);
        assertTrue(policePrincipal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_POLICE")));

        User hospital = User.builder().id(4L).role(Role.HOSPITAL).accountStatus(AccountStatus.ACTIVE).build();
        UserPrincipal hospitalPrincipal = UserPrincipal.create(hospital);
        assertTrue(hospitalPrincipal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_HOSPITAL")));
    }

    @Test
    void jwtTokenProvider_RejectsInvalidSecretConfiguration() {
        assertThrows(IllegalArgumentException.class, () -> new JwtTokenProvider(null, 3600000L));
        assertThrows(IllegalArgumentException.class, () -> new JwtTokenProvider("", 3600000L));
        assertThrows(IllegalArgumentException.class, () -> new JwtTokenProvider("   ", 3600000L));
        assertThrows(IllegalArgumentException.class, () -> new JwtTokenProvider("short_secret_under_32_bytes", 3600000L));
    }
}
