package com.safewalk.service;

import com.safewalk.dto.request.AdminVerificationRequest;
import com.safewalk.dto.request.RegisterRequest;
import com.safewalk.dto.response.AuthResponse;
import com.safewalk.dto.response.NearbyUserResponse;
import com.safewalk.dto.response.PendingVerificationResponse;
import com.safewalk.dto.response.UserResponse;
import com.safewalk.dto.response.VerificationDocumentResponse;
import com.safewalk.entity.AccountStatus;
import com.safewalk.entity.AuditLog;
import com.safewalk.entity.Role;
import com.safewalk.entity.User;
import com.safewalk.entity.VerificationDocument;
import com.safewalk.entity.VerificationStatus;
import com.safewalk.exception.DuplicateResourceException;
import com.safewalk.mapper.UserMapper;
import com.safewalk.repository.AuditLogRepository;
import com.safewalk.repository.EmergencyRepository;
import com.safewalk.repository.UserRepository;
import com.safewalk.repository.VerificationDocumentRepository;
import com.safewalk.security.JwtTokenProvider;
import com.safewalk.service.impl.AdminServiceImpl;
import com.safewalk.service.impl.AuthServiceImpl;
import com.safewalk.service.impl.LocationServiceImpl;
import com.safewalk.service.impl.VerificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PoliceHospitalVerificationTest {

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
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private GeoOperations<String, String> geoOperations;

    @TempDir
    Path tempUploadDir;

    private UserMapper userMapper;
    private AuthServiceImpl authService;
    private AdminServiceImpl adminService;
    private VerificationServiceImpl verificationService;
    private LocationServiceImpl locationService;

    @BeforeEach
    void setUp() {
        userMapper = new UserMapper();
        authService = new AuthServiceImpl(userRepository, userMapper, passwordEncoder, jwtTokenProvider);
        adminService = new AdminServiceImpl(userRepository, emergencyRepository, auditLogRepository, userMapper, null, null, documentRepository);
        verificationService = new VerificationServiceImpl(documentRepository, userRepository, tempUploadDir.toString());
        locationService = new LocationServiceImpl(stringRedisTemplate, userRepository, null, 2.0);
    }

    // 1. REGISTRATION TESTS

    @Test
    void registerCitizen_defaultsToUserRoleAndNoneVerification() {
        RegisterRequest request = RegisterRequest.builder()
                .name("Citizen John")
                .mobileNumber("+919876543210")
                .password("Password123!")
                .build();

        when(userRepository.existsByMobileNumber("+919876543210")).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn("encodedPwd");
        when(jwtTokenProvider.generateToken(any(User.class))).thenReturn("jwt-token");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(1L);
            return u;
        });

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals(Role.USER, response.getUser().getRole());
        assertEquals(VerificationStatus.NONE, response.getUser().getVerificationStatus());
        assertFalse(response.getUser().isPoliceVerified());
    }

    @Test
    void registerPolice_createsPendingAccount() {
        RegisterRequest request = RegisterRequest.builder()
                .name("Officer Singham")
                .mobileNumber("+919876500001")
                .password("Password123!")
                .role(Role.POLICE)
                .build();

        when(userRepository.existsByMobileNumber("+919876500001")).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn("encodedPwd");
        when(jwtTokenProvider.generateToken(any(User.class))).thenReturn("jwt-token");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(2L);
            return u;
        });

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals(Role.POLICE, response.getUser().getRole());
        assertEquals(VerificationStatus.PENDING, response.getUser().getVerificationStatus());
        assertFalse(response.getUser().isPoliceVerified());
    }

    @Test
    void registerHospital_createsPendingAccount() {
        RegisterRequest request = RegisterRequest.builder()
                .name("City Hospital")
                .mobileNumber("+919876500002")
                .password("Password123!")
                .role(Role.HOSPITAL)
                .build();

        when(userRepository.existsByMobileNumber("+919876500002")).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn("encodedPwd");
        when(jwtTokenProvider.generateToken(any(User.class))).thenReturn("jwt-token");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(3L);
            return u;
        });

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals(Role.HOSPITAL, response.getUser().getRole());
        assertEquals(VerificationStatus.PENDING, response.getUser().getVerificationStatus());
    }

    @Test
    void registerAdmin_isRejected() {
        RegisterRequest request = RegisterRequest.builder()
                .name("Fake Admin")
                .mobileNumber("+919876500003")
                .password("Password123!")
                .role(Role.ADMIN)
                .build();

        when(userRepository.existsByMobileNumber("+919876500003")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");

        assertThrows(IllegalArgumentException.class, () -> authService.register(request));
    }

    @Test
    void registerDuplicateMobile_throwsConflictException() {
        RegisterRequest request = RegisterRequest.builder()
                .name("Duplicate User")
                .mobileNumber("+919876543210")
                .password("Password123!")
                .role(Role.POLICE)
                .build();

        when(userRepository.existsByMobileNumber("+919876543210")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> authService.register(request));
    }

    // 2. DOCUMENT UPLOAD TESTS

    @Test
    void uploadDocument_validPoliceDocument_succeeds() {
        User police = User.builder()
                .id(10L)
                .name("Officer")
                .mobileNumber("+919876500010")
                .role(Role.POLICE)
                .verificationStatus(VerificationStatus.PENDING)
                .build();

        when(userRepository.findByMobileNumber("+919876500010")).thenReturn(Optional.of(police));
        when(documentRepository.save(any(VerificationDocument.class))).thenAnswer(invocation -> {
            VerificationDocument doc = invocation.getArgument(0);
            doc.setId(101L);
            doc.setUploadedAt(LocalDateTime.now());
            return doc;
        });

        MockMultipartFile file = new MockMultipartFile(
                "file", "badge.pdf", "application/pdf", "Dummy PDF content".getBytes());

        VerificationDocumentResponse response = verificationService.uploadDocument(
                "+919876500010", "Badge Proof", file);

        assertNotNull(response);
        assertEquals(101L, response.getId());
        assertEquals("badge.pdf", response.getFileName());
        assertEquals("Badge Proof", response.getTitle());
    }

    @Test
    void uploadDocument_invalidExtension_throwsException() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "malware.exe", "application/octet-stream", "bad".getBytes());

        assertThrows(IllegalArgumentException.class, () ->
                verificationService.uploadDocument("+919876500010", "Bad", file));
    }

    // 3. ADMIN VERIFICATION WORKFLOW TESTS

    @Test
    void getPendingVerifications_returnsPendingAuthoritiesWithDocs() {
        User pendingPolice = User.builder()
                .id(20L)
                .name("Officer Singham")
                .mobileNumber("+919876500020")
                .role(Role.POLICE)
                .verificationStatus(VerificationStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.findByVerificationStatus(VerificationStatus.PENDING))
                .thenReturn(Collections.singletonList(pendingPolice));
        when(documentRepository.findByUserIdOrderByUploadedAtDesc(20L))
                .thenReturn(Collections.emptyList());

        List<PendingVerificationResponse> list = adminService.getPendingVerifications();

        assertEquals(1, list.size());
        assertEquals(20L, list.get(0).getUserId());
        assertEquals(Role.POLICE, list.get(0).getRole());
        assertEquals(VerificationStatus.PENDING, list.get(0).getVerificationStatus());
    }

    @Test
    void updateVerificationStatus_approvePolice_setsVerifiedAndAudits() {
        User police = User.builder()
                .id(30L)
                .name("Officer Vijay")
                .mobileNumber("+919876500030")
                .role(Role.POLICE)
                .verificationStatus(VerificationStatus.PENDING)
                .policeVerified(false)
                .build();

        when(userRepository.findById(30L)).thenReturn(Optional.of(police));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminVerificationRequest request = new AdminVerificationRequest(VerificationStatus.VERIFIED, "Badge verified");
        UserResponse response = adminService.updateVerificationStatus(30L, request, "+919999933333");

        assertEquals(VerificationStatus.VERIFIED, response.getVerificationStatus());
        assertTrue(response.isPoliceVerified());

        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditCaptor.capture());
        assertEquals("USER_VERIFICATION_UPDATE", auditCaptor.getValue().getAction());
        assertEquals(30L, auditCaptor.getValue().getTargetId());
    }

    @Test
    void updateVerificationStatus_approveHospital_setsVerified() {
        User hospital = User.builder()
                .id(40L)
                .name("Apex Hospital")
                .mobileNumber("+919876500040")
                .role(Role.HOSPITAL)
                .verificationStatus(VerificationStatus.PENDING)
                .build();

        when(userRepository.findById(40L)).thenReturn(Optional.of(hospital));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminVerificationRequest request = new AdminVerificationRequest(VerificationStatus.VERIFIED, "License valid");
        UserResponse response = adminService.updateVerificationStatus(40L, request, "+919999933333");

        assertEquals(VerificationStatus.VERIFIED, response.getVerificationStatus());
    }

    @Test
    void updateVerificationStatus_fcmFailureDoesNotRollbackOrThrow() {
        FcmService mockFcm = mock(FcmService.class);
        AdminServiceImpl serviceWithFcm = new AdminServiceImpl(
                userRepository, emergencyRepository, auditLogRepository, userMapper, null, null, documentRepository, mockFcm);

        User police = User.builder()
                .id(45L)
                .name("Officer Resilient")
                .mobileNumber("+919876500045")
                .role(Role.POLICE)
                .verificationStatus(VerificationStatus.PENDING)
                .policeVerified(false)
                .build();

        when(userRepository.findById(45L)).thenReturn(Optional.of(police));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        org.mockito.Mockito.doThrow(new RuntimeException("FCM Service Unavailable"))
                .when(mockFcm).sendPushNotificationToUser(any(), any(), any(), any());

        AdminVerificationRequest request = new AdminVerificationRequest(VerificationStatus.VERIFIED, "Approved");
        UserResponse response = serviceWithFcm.updateVerificationStatus(45L, request, "+919999933333");

        assertNotNull(response);
        assertEquals(VerificationStatus.VERIFIED, response.getVerificationStatus());
        assertTrue(response.isPoliceVerified());
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    // 4. EMERGENCY DISPATCH GATING TESTS

    @Test
    void findNearbyResponders_unverifiedPoliceAndHospital_areStrictlyExcluded() {
        User unverifiedPolice = User.builder()
                .id(50L)
                .name("Unverified Cop")
                .mobileNumber("+919876500050")
                .role(Role.POLICE)
                .accountStatus(AccountStatus.ACTIVE)
                .policeVerified(false)
                .verificationStatus(VerificationStatus.PENDING)
                .build();

        User unverifiedHospital = User.builder()
                .id(51L)
                .name("Unverified Clinic")
                .mobileNumber("+919876500051")
                .role(Role.HOSPITAL)
                .accountStatus(AccountStatus.ACTIVE)
                .verificationStatus(VerificationStatus.PENDING)
                .build();

        User verifiedPolice = User.builder()
                .id(52L)
                .name("Verified Cop")
                .mobileNumber("+919876500052")
                .role(Role.POLICE)
                .accountStatus(AccountStatus.ACTIVE)
                .policeVerified(true)
                .verificationStatus(VerificationStatus.VERIFIED)
                .build();

        User verifiedHospital = User.builder()
                .id(53L)
                .name("Verified Hospital")
                .mobileNumber("+919876500053")
                .role(Role.HOSPITAL)
                .accountStatus(AccountStatus.ACTIVE)
                .verificationStatus(VerificationStatus.VERIFIED)
                .build();

        User citizenClose = User.builder()
                .id(54L)
                .name("Citizen 30m away")
                .mobileNumber("+919876500054")
                .role(Role.USER)
                .accountStatus(AccountStatus.ACTIVE)
                .verificationStatus(VerificationStatus.NONE)
                .build();

        User citizenEligible = User.builder()
                .id(55L)
                .name("Citizen 500m away")
                .mobileNumber("+919876500055")
                .role(Role.USER)
                .accountStatus(AccountStatus.ACTIVE)
                .verificationStatus(VerificationStatus.NONE)
                .build();

        // Setup Redis GeoResults
        GeoResult<RedisGeoCommands.GeoLocation<String>> res50 = new GeoResult<>(
                new RedisGeoCommands.GeoLocation<>("50", new Point(77.2, 28.6)), new Distance(0.2, Metrics.KILOMETERS));
        GeoResult<RedisGeoCommands.GeoLocation<String>> res51 = new GeoResult<>(
                new RedisGeoCommands.GeoLocation<>("51", new Point(77.2, 28.6)), new Distance(0.3, Metrics.KILOMETERS));
        GeoResult<RedisGeoCommands.GeoLocation<String>> res52 = new GeoResult<>(
                new RedisGeoCommands.GeoLocation<>("52", new Point(77.2, 28.6)), new Distance(0.02, Metrics.KILOMETERS)); // 20m away
        GeoResult<RedisGeoCommands.GeoLocation<String>> res53 = new GeoResult<>(
                new RedisGeoCommands.GeoLocation<>("53", new Point(77.2, 28.6)), new Distance(0.04, Metrics.KILOMETERS)); // 40m away
        GeoResult<RedisGeoCommands.GeoLocation<String>> res54 = new GeoResult<>(
                new RedisGeoCommands.GeoLocation<>("54", new Point(77.2, 28.6)), new Distance(0.03, Metrics.KILOMETERS)); // 30m away (bystander floor < 50m)
        GeoResult<RedisGeoCommands.GeoLocation<String>> res55 = new GeoResult<>(
                new RedisGeoCommands.GeoLocation<>("55", new Point(77.2, 28.6)), new Distance(0.5, Metrics.KILOMETERS)); // 500m away

        GeoResults<RedisGeoCommands.GeoLocation<String>> geoResults = new GeoResults<>(
                Arrays.asList(res50, res51, res52, res53, res54, res55));

        when(stringRedisTemplate.opsForGeo()).thenReturn(geoOperations);
        when(geoOperations.search(
                eq("safewalk:users:locations"),
                any(org.springframework.data.redis.domain.geo.GeoReference.class),
                any(Distance.class),
                any(RedisGeoCommands.GeoSearchCommandArgs.class)))
                .thenReturn(geoResults);

        when(userRepository.findById(50L)).thenReturn(Optional.of(unverifiedPolice));
        when(userRepository.findById(51L)).thenReturn(Optional.of(unverifiedHospital));
        when(userRepository.findById(52L)).thenReturn(Optional.of(verifiedPolice));
        when(userRepository.findById(53L)).thenReturn(Optional.of(verifiedHospital));
        when(userRepository.findById(54L)).thenReturn(Optional.of(citizenClose));
        when(userRepository.findById(55L)).thenReturn(Optional.of(citizenEligible));

        List<NearbyUserResponse> responders = locationService.findNearbyResponders(28.6, 77.2, 2.0);

        // Expected:
        // 50 (Unverified Police) -> EXCLUDED
        // 51 (Unverified Hospital) -> EXCLUDED
        // 52 (Verified Police at 20m) -> ELIGIBLE (No 50m floor for verified authority)
        // 53 (Verified Hospital at 40m) -> ELIGIBLE (No 50m floor for verified authority)
        // 54 (Citizen at 30m) -> EXCLUDED (< 50m bystander floor)
        // 55 (Citizen at 500m) -> ELIGIBLE (> 50m and <= 2km)
        assertEquals(3, responders.size());
        assertTrue(responders.stream().anyMatch(r -> r.getUserId().equals(52L)));
        assertTrue(responders.stream().anyMatch(r -> r.getUserId().equals(53L)));
        assertTrue(responders.stream().anyMatch(r -> r.getUserId().equals(55L)));
        assertFalse(responders.stream().anyMatch(r -> r.getUserId().equals(50L)));
        assertFalse(responders.stream().anyMatch(r -> r.getUserId().equals(51L)));
        assertFalse(responders.stream().anyMatch(r -> r.getUserId().equals(54L)));
    }
}
