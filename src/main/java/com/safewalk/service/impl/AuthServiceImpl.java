package com.safewalk.service.impl;

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
import com.safewalk.exception.ResourceNotFoundException;
import com.safewalk.mapper.UserMapper;
import com.safewalk.repository.UserRepository;
import com.safewalk.security.JwtTokenProvider;
import com.safewalk.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Autowired
    public AuthServiceImpl(
            UserRepository userRepository,
            UserMapper userMapper,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String cleanMobileNumber = request.getMobileNumber().trim();
        log.info("Processing registration request for mobile number: {}", cleanMobileNumber);

        if (userRepository.existsByMobileNumber(cleanMobileNumber)) {
            log.warn("Registration failed. Mobile number already exists: {}", cleanMobileNumber);
            throw new DuplicateResourceException("Mobile number '" + cleanMobileNumber + "' is already registered with a SafeWalk account.");
        }

        String encodedPassword = passwordEncoder.encode(request.getPassword());
        String encodedEmergencyPassword = request.getEmergencyPassword() != null && !request.getEmergencyPassword().trim().isEmpty()
                ? passwordEncoder.encode(request.getEmergencyPassword().trim())
                : null;

        if (request.getRole() == Role.ADMIN) {
            log.warn("Registration rejected. Attempt to register as ADMIN via public registration.");
            throw new IllegalArgumentException("Registration with role ADMIN is not permitted.");
        }

        Role userRole = request.getRole() != null ? request.getRole() : Role.USER;
        com.safewalk.entity.VerificationStatus verificationStatus = (userRole == Role.POLICE || userRole == Role.HOSPITAL)
                ? com.safewalk.entity.VerificationStatus.PENDING
                : com.safewalk.entity.VerificationStatus.NONE;

        String dept = request.getDepartment() != null && !request.getDepartment().trim().isEmpty()
                ? request.getDepartment().trim()
                : (request.getPoliceStation() != null && !request.getPoliceStation().trim().isEmpty() ? request.getPoliceStation().trim() : null);
        String badge = request.getBadgeNumber() != null && !request.getBadgeNumber().trim().isEmpty()
                ? request.getBadgeNumber().trim()
                : null;

        User user = User.builder()
                .name(request.getName().trim())
                .mobileNumber(cleanMobileNumber)
                .password(encodedPassword)
                .emergencyPassword(encodedEmergencyPassword)
                .role(userRole)
                .verificationStatus(verificationStatus)
                .policeVerified(false)
                .accountStatus(AccountStatus.ACTIVE)
                .badgeNumber(badge)
                .department(dept)
                .build();

        User savedUser = userRepository.save(user);
        log.info("Successfully registered user with ID: {} and mobile number: {}", savedUser.getId(), savedUser.getMobileNumber());

        String token = jwtTokenProvider.generateToken(savedUser);
        UserResponse userResponse = userMapper.toResponse(savedUser);

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .user(userResponse)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String cleanIdentifier;
        boolean isEmail = false;

        if (org.springframework.util.StringUtils.hasText(request.getEmail())) {
            cleanIdentifier = request.getEmail().trim().toLowerCase();
            isEmail = true;
        } else if (org.springframework.util.StringUtils.hasText(request.getMobileNumber())) {
            String raw = request.getMobileNumber().trim();
            if (raw.contains("@")) {
                cleanIdentifier = raw.toLowerCase();
                isEmail = true;
            } else {
                cleanIdentifier = raw;
                isEmail = false;
            }
        } else {
            throw new InvalidCredentialsException("Invalid mobile number or password.");
        }

        log.info("Processing login request for identifier: {}", isEmail ? cleanIdentifier : maskMobile(cleanIdentifier));

        User user = isEmail
                ? userRepository.findByEmail(cleanIdentifier)
                        .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password."))
                : userRepository.findByMobileNumber(cleanIdentifier)
                        .orElseThrow(() -> new InvalidCredentialsException("Invalid mobile number or password."));

        if (user.getAccountStatus() == AccountStatus.DEACTIVATED || user.getAccountStatus() == AccountStatus.SUSPENDED) {
            log.warn("Login blocked for user ID: {} with account status: {}", user.getId(), user.getAccountStatus());
            throw new AccountStatusException("Your account is currently " + user.getAccountStatus().name() + ". Access denied.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Invalid password attempt for identifier: {}", isEmail ? cleanIdentifier : maskMobile(cleanIdentifier));
            throw new InvalidCredentialsException(isEmail ? "Invalid email or password." : "Invalid mobile number or password.");
        }

        log.info("Successful login for user ID: {}", user.getId());
        String token = jwtTokenProvider.generateToken(user);
        UserResponse userResponse = userMapper.toResponse(user);

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .user(userResponse)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUserProfile(String identifier) {
        log.debug("Fetching current user profile for identifier: {}", identifier);
        String clean = identifier.trim();
        User user = clean.contains("@")
                ? userRepository.findByEmail(clean.toLowerCase())
                        .orElseThrow(() -> new ResourceNotFoundException("User profile not found for identifier: " + clean))
                : userRepository.findByMobileNumber(clean)
                        .orElseThrow(() -> new ResourceNotFoundException("User profile not found for identifier: " + clean));
        return userMapper.toResponse(user);
    }

    private String maskMobile(String mobile) {
        if (mobile == null || mobile.length() < 4) return "***";
        return mobile.substring(0, 2) + "****" + mobile.substring(mobile.length() - 2);
    }
}
