package com.safewalk.service;

import com.safewalk.dto.request.CancelEmergencyRequest;
import com.safewalk.dto.request.CreateEmergencyRequest;
import com.safewalk.dto.response.EmergencyLocationResponse;
import com.safewalk.dto.response.EmergencyResponse;
import com.safewalk.entity.AccountStatus;
import com.safewalk.entity.Emergency;
import com.safewalk.entity.EmergencyStatus;
import com.safewalk.entity.EmergencyType;
import com.safewalk.entity.Role;
import com.safewalk.entity.User;
import com.safewalk.entity.VerificationStatus;
import com.safewalk.exception.AccountStatusException;
import com.safewalk.exception.DuplicateResourceException;
import com.safewalk.exception.InvalidCredentialsException;
import com.safewalk.exception.ResourceNotFoundException;
import com.safewalk.mapper.EmergencyMapper;
import com.safewalk.repository.EmergencyRepository;
import com.safewalk.repository.UserRepository;
import com.safewalk.service.impl.EmergencyServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.geo.Point;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmergencyServiceTest {

    @Mock
    private EmergencyRepository emergencyRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private com.safewalk.event.producer.SosEventProducer sosEventProducer;

    @Mock
    private LocationService locationService;

    @Mock
    private com.safewalk.repository.EmergencyContactRepository emergencyContactRepository;

    private EmergencyMapper emergencyMapper;
    private PasswordEncoder passwordEncoder;
    private EmergencyServiceImpl emergencyService;

    private User requester;
    private Emergency activeEmergency;

    @BeforeEach
    void setUp() {
        emergencyMapper = new EmergencyMapper();
        passwordEncoder = new BCryptPasswordEncoder();
        emergencyService = new EmergencyServiceImpl(
                emergencyRepository, userRepository, emergencyMapper, passwordEncoder, sosEventProducer,
                null, null, null, locationService, emergencyContactRepository);

        requester = User.builder()
                .id(1L)
                .name("Aman Sharma")
                .mobileNumber("+919876543210")
                .password(passwordEncoder.encode("password123"))
                .emergencyPassword(passwordEncoder.encode("sosPassword123"))
                .role(Role.USER)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        activeEmergency = Emergency.builder()
                .id(100L)
                .requester(requester)
                .type(EmergencyType.GENERAL)
                .status(EmergencyStatus.ACTIVE)
                .latitude(26.8467)
                .longitude(80.9462)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .responders(new HashSet<>())
                .build();
    }

    @Test
    void createEmergency_Success() {
        CreateEmergencyRequest request = CreateEmergencyRequest.builder()
                .type(EmergencyType.GENERAL)
                .latitude(26.8467)
                .longitude(80.9462)
                .build();

        when(userRepository.findByMobileNumber("+919876543210")).thenReturn(Optional.of(requester));
        when(emergencyRepository.existsByRequesterIdAndStatusIn(any(), any())).thenReturn(false);
        when(emergencyRepository.save(any(Emergency.class))).thenReturn(activeEmergency);

        EmergencyResponse response = emergencyService.createEmergency(request, "+919876543210");

        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals(EmergencyStatus.ACTIVE, response.getStatus());
        assertEquals(EmergencyType.GENERAL, response.getType());
        assertEquals(1L, response.getRequesterId());
    }

    @Test
    void createEmergency_ActiveEmergencyAlreadyExists_ThrowsException() {
        CreateEmergencyRequest request = CreateEmergencyRequest.builder().build();

        when(userRepository.findByMobileNumber("+919876543210")).thenReturn(Optional.of(requester));
        when(emergencyRepository.existsByRequesterIdAndStatusIn(any(), any())).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> emergencyService.createEmergency(request, "+919876543210"));
        verify(emergencyRepository, never()).save(any(Emergency.class));
    }

    @Test
    void getEmergencyById_RequesterAccess_Success() {
        when(emergencyRepository.findById(100L)).thenReturn(Optional.of(activeEmergency));

        EmergencyResponse response = emergencyService.getEmergencyById(100L, "+919876543210", false);

        assertNotNull(response);
        assertEquals(100L, response.getId());
    }

    @Test
    void getEmergencyById_OtherUserAccess_ThrowsAccessDenied() {
        when(emergencyRepository.findById(100L)).thenReturn(Optional.of(activeEmergency));

        assertThrows(AccessDeniedException.class, () -> emergencyService.getEmergencyById(100L, "+919999999999", false));
    }

    @Test
    void acceptEmergency_FirstResponder_Success() {
        User responder = User.builder()
                .id(2L)
                .name("Priya Responder")
                .mobileNumber("+919876543211")
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        when(emergencyRepository.findById(100L)).thenReturn(Optional.of(activeEmergency));
        when(userRepository.findByMobileNumber("+919876543211")).thenReturn(Optional.of(responder));
        when(locationService.getUserCoordinates(2L)).thenReturn(new Point(80.9462, 26.8476));
        when(locationService.isLocationFresh(2L)).thenReturn(true);
        when(emergencyRepository.save(any(Emergency.class))).thenAnswer(i -> i.getArgument(0));

        EmergencyResponse response = emergencyService.acceptEmergency(100L, "+919876543211");

        assertNotNull(response);
        assertEquals(EmergencyStatus.ACCEPTED, response.getStatus());
        assertEquals(2L, response.getResponderId());
        assertEquals("Priya Responder", response.getResponderName());
        assertNotNull(response.getAcceptedAt());
        assertEquals(1, response.getResponders().size());
        assertEquals(2L, response.getResponders().get(0).getId());
    }

    @Test
    void acceptEmergency_MultipleResponders_Success() {
        User responder1 = User.builder()
                .id(2L)
                .name("Priya Responder 1")
                .mobileNumber("+919876543211")
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        User responder2 = User.builder()
                .id(3L)
                .name("Rahul Responder 2")
                .mobileNumber("+919876543212")
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        activeEmergency.setStatus(EmergencyStatus.ACCEPTED);
        activeEmergency.setResponder(responder1);
        activeEmergency.addResponder(responder1);

        when(emergencyRepository.findById(100L)).thenReturn(Optional.of(activeEmergency));
        when(userRepository.findByMobileNumber("+919876543212")).thenReturn(Optional.of(responder2));
        when(locationService.getUserCoordinates(3L)).thenReturn(new Point(80.9462, 26.8476));
        when(locationService.isLocationFresh(3L)).thenReturn(true);
        when(emergencyRepository.save(any(Emergency.class))).thenAnswer(i -> i.getArgument(0));

        EmergencyResponse response = emergencyService.acceptEmergency(100L, "+919876543212");

        assertNotNull(response);
        assertEquals(EmergencyStatus.ACCEPTED, response.getStatus());
        assertEquals(2, response.getResponders().size());
    }

    @Test
    void acceptEmergency_DuplicateAcceptance_ThrowsDuplicateResourceException() {
        User responder = User.builder()
                .id(2L)
                .name("Priya Responder")
                .mobileNumber("+919876543211")
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        activeEmergency.setStatus(EmergencyStatus.ACCEPTED);
        activeEmergency.addResponder(responder);

        when(emergencyRepository.findById(100L)).thenReturn(Optional.of(activeEmergency));
        when(userRepository.findByMobileNumber("+919876543211")).thenReturn(Optional.of(responder));

        assertThrows(DuplicateResourceException.class, () -> emergencyService.acceptEmergency(100L, "+919876543211"));
    }

    @Test
    void acceptEmergency_ResolvedStatus_ThrowsIllegalStateException() {
        activeEmergency.setStatus(EmergencyStatus.RESOLVED);
        when(emergencyRepository.findById(100L)).thenReturn(Optional.of(activeEmergency));

        assertThrows(IllegalStateException.class, () -> emergencyService.acceptEmergency(100L, "+919876543211"));
    }

    @Test
    void acceptEmergency_SelfAcceptance_ThrowsException() {
        when(emergencyRepository.findById(100L)).thenReturn(Optional.of(activeEmergency));
        when(userRepository.findByMobileNumber("+919876543210")).thenReturn(Optional.of(requester));

        assertThrows(IllegalArgumentException.class, () -> emergencyService.acceptEmergency(100L, "+919876543210"));
    }

    @Test
    void acceptEmergency_UnverifiedPolice_ThrowsAccountStatusException() {
        User unverifiedPolice = User.builder()
                .id(20L)
                .name("Officer Pending")
                .mobileNumber("+919876543220")
                .role(Role.POLICE)
                .accountStatus(AccountStatus.ACTIVE)
                .verificationStatus(VerificationStatus.PENDING)
                .build();

        when(emergencyRepository.findById(100L)).thenReturn(Optional.of(activeEmergency));
        when(userRepository.findByMobileNumber("+919876543220")).thenReturn(Optional.of(unverifiedPolice));

        AccountStatusException ex = assertThrows(AccountStatusException.class,
                () -> emergencyService.acceptEmergency(100L, "+919876543220"));
        assertTrue(ex.getMessage().contains("Account verification pending"));
    }

    @Test
    void acceptEmergency_RejectedPolice_ThrowsAccountStatusException() {
        User rejectedPolice = User.builder()
                .id(21L)
                .name("Officer Rejected")
                .mobileNumber("+919876543221")
                .role(Role.POLICE)
                .accountStatus(AccountStatus.ACTIVE)
                .verificationStatus(VerificationStatus.REJECTED)
                .build();

        when(emergencyRepository.findById(100L)).thenReturn(Optional.of(activeEmergency));
        when(userRepository.findByMobileNumber("+919876543221")).thenReturn(Optional.of(rejectedPolice));

        AccountStatusException ex = assertThrows(AccountStatusException.class,
                () -> emergencyService.acceptEmergency(100L, "+919876543221"));
        assertTrue(ex.getMessage().contains("Account verification pending"));
    }

    @Test
    void acceptEmergency_VerifiedPolice_Succeeds() {
        User verifiedPolice = User.builder()
                .id(22L)
                .name("Officer Verified")
                .mobileNumber("+919876543222")
                .role(Role.POLICE)
                .accountStatus(AccountStatus.ACTIVE)
                .policeVerified(true)
                .verificationStatus(VerificationStatus.VERIFIED)
                .build();

        when(emergencyRepository.findById(100L)).thenReturn(Optional.of(activeEmergency));
        when(userRepository.findByMobileNumber("+919876543222")).thenReturn(Optional.of(verifiedPolice));
        when(locationService.getUserCoordinates(22L)).thenReturn(new Point(80.9462, 26.8476));
        when(locationService.isLocationFresh(22L)).thenReturn(true);
        when(emergencyRepository.save(any(Emergency.class))).thenAnswer(i -> i.getArgument(0));

        EmergencyResponse response = emergencyService.acceptEmergency(100L, "+919876543222");

        assertNotNull(response);
        assertEquals(EmergencyStatus.ACCEPTED, response.getStatus());
        assertTrue(response.getResponders().stream().anyMatch(r -> r.getId().equals(22L)));
    }

    @Test
    void getEmergencyLocations_RequesterAccess_Success() {
        User responder = User.builder()
                .id(2L)
                .name("Priya Responder")
                .mobileNumber("+919876543211")
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        activeEmergency.setStatus(EmergencyStatus.ACCEPTED);
        activeEmergency.addResponder(responder);

        when(emergencyRepository.findById(100L)).thenReturn(Optional.of(activeEmergency));
        when(locationService.getUserCoordinates(1L)).thenReturn(new Point(80.9462, 26.8467));
        when(locationService.getUserCoordinates(2L)).thenReturn(new Point(80.9470, 26.8475));

        EmergencyLocationResponse response = emergencyService.getEmergencyLocations(100L, "+919876543210", false);

        assertNotNull(response);
        assertEquals(100L, response.getEmergencyId());
        assertEquals(EmergencyStatus.ACCEPTED, response.getStatus());
        assertNotNull(response.getRequester());
        assertEquals(1L, response.getRequester().getUserId());
        assertEquals(1, response.getResponders().size());
        assertEquals(2L, response.getResponders().get(0).getUserId());
        assertNotNull(response.getResponders().get(0).getDistanceKm());
    }

    @Test
    void getEmergencyLocations_ResponderAccess_Success() {
        User responder = User.builder()
                .id(2L)
                .name("Priya Responder")
                .mobileNumber("+919876543211")
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        activeEmergency.setStatus(EmergencyStatus.ACCEPTED);
        activeEmergency.addResponder(responder);

        when(emergencyRepository.findById(100L)).thenReturn(Optional.of(activeEmergency));

        EmergencyLocationResponse response = emergencyService.getEmergencyLocations(100L, "+919876543211", false);

        assertNotNull(response);
        assertEquals(100L, response.getEmergencyId());
    }

    @Test
    void getEmergencyLocations_UnauthorizedUser_ThrowsAccessDenied() {
        when(emergencyRepository.findById(100L)).thenReturn(Optional.of(activeEmergency));

        assertThrows(AccessDeniedException.class, () ->
                emergencyService.getEmergencyLocations(100L, "+919999999999", false));
    }

    @Test
    void getEmergencyLocations_ResolvedEmergency_ThrowsIllegalState() {
        activeEmergency.setStatus(EmergencyStatus.RESOLVED);
        when(emergencyRepository.findById(100L)).thenReturn(Optional.of(activeEmergency));

        assertThrows(IllegalStateException.class, () ->
                emergencyService.getEmergencyLocations(100L, "+919876543210", false));
    }

    @Test
    void rejectEmergency_Success() {
        User responder = User.builder()
                .id(2L)
                .name("Priya Responder")
                .mobileNumber("+919876543211")
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        when(emergencyRepository.findById(100L)).thenReturn(Optional.of(activeEmergency));
        when(userRepository.findByMobileNumber("+919876543211")).thenReturn(Optional.of(responder));

        EmergencyResponse response = emergencyService.rejectEmergency(100L, "+919876543211");

        assertNotNull(response);
        assertEquals(100L, response.getId());
    }

    @Test
    void cancelEmergency_ValidEmergencyPassword_Success() {
        CancelEmergencyRequest cancelRequest = CancelEmergencyRequest.builder()
                .emergencyPassword("sosPassword123")
                .build();

        when(emergencyRepository.findById(100L)).thenReturn(Optional.of(activeEmergency));
        when(emergencyRepository.save(any(Emergency.class))).thenAnswer(i -> i.getArgument(0));

        EmergencyResponse response = emergencyService.cancelEmergency(100L, cancelRequest, "+919876543210");

        assertNotNull(response);
        assertEquals(EmergencyStatus.CANCELLED, response.getStatus());
        assertNotNull(response.getCancelledAt());
    }

    @Test
    void cancelEmergency_InvalidEmergencyPassword_ThrowsException() {
        CancelEmergencyRequest cancelRequest = CancelEmergencyRequest.builder()
                .emergencyPassword("wrongSosPassword")
                .build();

        when(emergencyRepository.findById(100L)).thenReturn(Optional.of(activeEmergency));

        assertThrows(InvalidCredentialsException.class, () -> emergencyService.cancelEmergency(100L, cancelRequest, "+919876543210"));
    }

    @Test
    void resolveEmergency_Success() {
        when(emergencyRepository.findById(100L)).thenReturn(Optional.of(activeEmergency));
        when(emergencyRepository.save(any(Emergency.class))).thenAnswer(i -> i.getArgument(0));

        EmergencyResponse response = emergencyService.resolveEmergency(100L, "+919876543210");

        assertNotNull(response);
        assertEquals(EmergencyStatus.RESOLVED, response.getStatus());
        assertNotNull(response.getResolvedAt());
    }

    @Test
    void resolveEmergency_AlreadyCancelled_ThrowsIllegalState() {
        activeEmergency.setStatus(EmergencyStatus.CANCELLED);
        when(emergencyRepository.findById(100L)).thenReturn(Optional.of(activeEmergency));

        assertThrows(IllegalStateException.class, () -> emergencyService.resolveEmergency(100L, "+919876543210"));
    }

    @Test
    void getActiveEmergencies_Citizen_ReturnsList() {
        when(userRepository.findByMobileNumber("+919876543210")).thenReturn(Optional.of(requester));
        when(emergencyRepository.findByStatusInOrderByCreatedAtDesc(any()))
                .thenReturn(java.util.Collections.singletonList(activeEmergency));

        java.util.List<EmergencyResponse> list = emergencyService.getActiveEmergencies("+919876543210");

        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals(100L, list.get(0).getId());
    }

    @Test
    void getActiveEmergencies_UnverifiedPolice_ReturnsEmpty() {
        User unverifiedPolice = User.builder()
                .id(20L)
                .name("Officer Pending")
                .mobileNumber("+919876543220")
                .role(Role.POLICE)
                .accountStatus(AccountStatus.ACTIVE)
                .verificationStatus(VerificationStatus.PENDING)
                .build();

        when(userRepository.findByMobileNumber("+919876543220")).thenReturn(Optional.of(unverifiedPolice));

        java.util.List<EmergencyResponse> list = emergencyService.getActiveEmergencies("+919876543220");

        assertNotNull(list);
        assertTrue(list.isEmpty());
    }

    @Test
    void getEmergencyById_TrustedContactAccess_Success() {
        User contactUser = User.builder()
                .id(10L)
                .name("Trusted Mom")
                .mobileNumber("+919876543299")
                .role(Role.USER)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        when(emergencyRepository.findById(100L)).thenReturn(Optional.of(activeEmergency));
        when(userRepository.findByMobileNumber("+919876543299")).thenReturn(Optional.of(contactUser));
        when(emergencyContactRepository.existsByUserIdAndContactUserId(1L, 10L)).thenReturn(true);

        EmergencyResponse response = emergencyService.getEmergencyById(100L, "+919876543299", false);

        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertTrue(response.isTrustedContactViewer());
    }

    @Test
    void getEmergencyLocations_TrustedContactAccess_Success() {
        User contactUser = User.builder()
                .id(10L)
                .name("Trusted Mom")
                .mobileNumber("+919876543299")
                .role(Role.USER)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        when(emergencyRepository.findById(100L)).thenReturn(Optional.of(activeEmergency));
        when(userRepository.findByMobileNumber("+919876543299")).thenReturn(Optional.of(contactUser));
        when(emergencyContactRepository.existsByUserIdAndContactUserId(1L, 10L)).thenReturn(true);

        EmergencyLocationResponse response = emergencyService.getEmergencyLocations(100L, "+919876543299", false);

        assertNotNull(response);
        assertEquals(100L, response.getEmergencyId());
    }
}
