package com.safewalk.service;

import com.safewalk.dto.request.LoginRequest;
import com.safewalk.dto.request.RegisterRequest;
import com.safewalk.dto.response.AuthResponse;
import com.safewalk.dto.response.UserResponse;
import com.safewalk.entity.AccountStatus;
import com.safewalk.entity.Role;
import com.safewalk.entity.User;
import com.safewalk.exception.AccountStatusException;
import com.safewalk.exception.DuplicateResourceException;
import com.safewalk.exception.InvalidCredentialsException;
import com.safewalk.mapper.UserMapper;
import com.safewalk.repository.UserRepository;
import com.safewalk.security.JwtTokenProvider;
import com.safewalk.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    private PasswordEncoder passwordEncoder;
    private UserMapper userMapper;
    private AuthServiceImpl authService;

    private RegisterRequest registerRequest;
    private User registeredUser;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        userMapper = new UserMapper();
        authService = new AuthServiceImpl(userRepository, userMapper, passwordEncoder, jwtTokenProvider);

        registerRequest = RegisterRequest.builder()
                .name("Rohan Mehta")
                .mobileNumber("+919876543210")
                .password("plainPassword123")
                .emergencyPassword("sosPassword123")
                .build();

        registeredUser = User.builder()
                .id(100L)
                .name("Rohan Mehta")
                .mobileNumber("+919876543210")
                .password(passwordEncoder.encode("plainPassword123"))
                .emergencyPassword(passwordEncoder.encode("sosPassword123"))
                .role(Role.USER)
                .accountStatus(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void register_Success_HashesPasswordsAndReturnsToken() {
        when(userRepository.existsByMobileNumber(registerRequest.getMobileNumber())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(registeredUser);
        when(jwtTokenProvider.generateToken(any(User.class))).thenReturn("mocked.jwt.token");

        AuthResponse response = authService.register(registerRequest);

        assertNotNull(response);
        assertEquals("mocked.jwt.token", response.getToken());
        assertEquals("Bearer", response.getTokenType());
        assertNotNull(response.getUser());
        assertEquals("+919876543210", response.getUser().getMobileNumber());
        assertEquals(Role.USER, response.getUser().getRole());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedEntity = userCaptor.getValue();

        // Verify password hashing - plain text is never stored!
        assertTrue(passwordEncoder.matches("plainPassword123", savedEntity.getPassword()));
        assertTrue(passwordEncoder.matches("sosPassword123", savedEntity.getEmergencyPassword()));
    }

    @Test
    void register_DuplicateMobileNumber_ThrowsException() {
        when(userRepository.existsByMobileNumber(registerRequest.getMobileNumber())).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> authService.register(registerRequest));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_WithValidEmail_PersistsNormalizedEmail() {
        RegisterRequest requestWithEmail = RegisterRequest.builder()
                .name("Rohan Mehta")
                .mobileNumber("+919876543210")
                .email("  Rohan.Mehta@Example.Com  ")
                .password("plainPassword123")
                .emergencyPassword("sosPassword123")
                .build();

        User userWithEmail = User.builder()
                .id(101L)
                .name("Rohan Mehta")
                .mobileNumber("+919876543210")
                .email("rohan.mehta@example.com")
                .password(passwordEncoder.encode("plainPassword123"))
                .role(Role.USER)
                .accountStatus(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(userRepository.existsByMobileNumber(requestWithEmail.getMobileNumber())).thenReturn(false);
        when(userRepository.existsByEmail("rohan.mehta@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(userWithEmail);
        when(jwtTokenProvider.generateToken(any(User.class))).thenReturn("mocked.jwt.token");

        AuthResponse response = authService.register(requestWithEmail);

        assertNotNull(response);
        assertNotNull(response.getUser());
        assertEquals("rohan.mehta@example.com", response.getUser().getEmail());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedEntity = userCaptor.getValue();
        assertEquals("rohan.mehta@example.com", savedEntity.getEmail());
    }

    @Test
    void register_DuplicateEmail_ThrowsException() {
        RegisterRequest requestWithEmail = RegisterRequest.builder()
                .name("Rohan Mehta")
                .mobileNumber("+919876543210")
                .email("Duplicate@Example.Com")
                .password("plainPassword123")
                .build();

        when(userRepository.existsByMobileNumber(requestWithEmail.getMobileNumber())).thenReturn(false);
        when(userRepository.existsByEmail("duplicate@example.com")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> authService.register(requestWithEmail));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void login_Success_ReturnsJwtToken() {
        LoginRequest loginRequest = LoginRequest.builder()
                .mobileNumber("+919876543210")
                .password("plainPassword123")
                .build();

        when(userRepository.findByMobileNumber("+919876543210")).thenReturn(Optional.of(registeredUser));
        when(jwtTokenProvider.generateToken(registeredUser)).thenReturn("login.jwt.token");

        AuthResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals("login.jwt.token", response.getToken());
        assertEquals("+919876543210", response.getUser().getMobileNumber());
    }

    @Test
    void login_WithEmail_ValidCredentials_ReturnsToken() {
        registeredUser.setEmail("gaurav@safewalk.in");
        LoginRequest loginRequest = LoginRequest.builder()
                .email("gaurav@safewalk.in")
                .password("plainPassword123")
                .build();

        when(userRepository.findByEmail("gaurav@safewalk.in")).thenReturn(Optional.of(registeredUser));
        when(jwtTokenProvider.generateToken(registeredUser)).thenReturn("login.jwt.token");

        AuthResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals("login.jwt.token", response.getToken());
        assertEquals("gaurav@safewalk.in", response.getUser().getEmail());
    }

    @Test
    void login_WrongPassword_ThrowsInvalidCredentialsException() {
        LoginRequest loginRequest = LoginRequest.builder()
                .mobileNumber("+919876543210")
                .password("wrongPassword")
                .build();

        when(userRepository.findByMobileNumber("+919876543210")).thenReturn(Optional.of(registeredUser));

        assertThrows(InvalidCredentialsException.class, () -> authService.login(loginRequest));
    }

    @Test
    void login_DeactivatedAccount_ThrowsAccountStatusException() {
        registeredUser.setAccountStatus(AccountStatus.DEACTIVATED);
        LoginRequest loginRequest = LoginRequest.builder()
                .mobileNumber("+919876543210")
                .password("plainPassword123")
                .build();

        when(userRepository.findByMobileNumber("+919876543210")).thenReturn(Optional.of(registeredUser));

        assertThrows(AccountStatusException.class, () -> authService.login(loginRequest));
    }

    @Test
    void getCurrentUserProfile_Success() {
        when(userRepository.findByMobileNumber("+919876543210")).thenReturn(Optional.of(registeredUser));

        UserResponse response = authService.getCurrentUserProfile("+919876543210");

        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals("Rohan Mehta", response.getName());
    }
}
