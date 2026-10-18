package com.safewalk.service;

import com.safewalk.dto.request.CancelEmergencyRequest;
import com.safewalk.dto.response.EmergencyResponse;
import com.safewalk.entity.AccountStatus;
import com.safewalk.entity.Emergency;
import com.safewalk.entity.EmergencyStatus;
import com.safewalk.entity.EmergencyType;
import com.safewalk.entity.Role;
import com.safewalk.entity.User;
import com.safewalk.exception.InvalidCredentialsException;
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

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmergencyServiceFeatureTest {

    @Mock
    private EmergencyRepository emergencyRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private LocationService locationService;

    @Mock
    private WebSocketService webSocketService;

    @Mock
    private FcmService fcmService;

    private EmergencyMapper emergencyMapper;
    private PasswordEncoder passwordEncoder;
    private EmergencyServiceImpl emergencyService;

    private User requester;
    private User responder1;
    private User responder2;

    @BeforeEach
    void setUp() {
        emergencyMapper = new EmergencyMapper();
        passwordEncoder = new BCryptPasswordEncoder();
        emergencyService = new EmergencyServiceImpl(
                emergencyRepository, userRepository, emergencyMapper, passwordEncoder, null,
                webSocketService, fcmService, null, locationService);

        requester = User.builder()
                .id(1L)
                .name("Aman Requester")
                .mobileNumber("+919876543210")
                .password(passwordEncoder.encode("password123"))
                .emergencyPassword(passwordEncoder.encode("sos123"))
                .role(Role.USER)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        responder1 = User.builder()
                .id(2L)
                .name("Ravi Responder 1")
                .mobileNumber("+919876543211")
                .role(Role.USER)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        responder2 = User.builder()
                .id(3L)
                .name("Sneha Responder 2")
                .mobileNumber("+919876543212")
                .role(Role.USER)
                .accountStatus(AccountStatus.ACTIVE)
                .build();
    }

    // ==========================================
    // 1. 5-Second Cancellation Window Tests
    // ==========================================

    @Test
    void cancelEmergency_Within5Seconds_Success() {
        Emergency emergency = Emergency.builder()
                .id(100L)
                .requester(requester)
                .status(EmergencyStatus.ACTIVE)
                .createdAt(LocalDateTime.now().minusSeconds(2)) // 2 seconds ago (< 5.0s)
                .build();

        CancelEmergencyRequest request = CancelEmergencyRequest.builder()
                .emergencyPassword("sos123")
                .build();

        when(emergencyRepository.findById(100L)).thenReturn(Optional.of(emergency));
        when(emergencyRepository.save(any(Emergency.class))).thenAnswer(i -> i.getArgument(0));

        EmergencyResponse response = emergencyService.cancelEmergency(100L, request, "+919876543210");

        assertNotNull(response);
        assertEquals(EmergencyStatus.CANCELLED, response.getStatus());
        assertNotNull(response.getCancelledAt());
    }

    @Test
    void cancelEmergency_After5Seconds_ThrowsIllegalStateException() {
        Emergency emergency = Emergency.builder()
                .id(100L)
                .requester(requester)
                .status(EmergencyStatus.ACTIVE)
                .createdAt(LocalDateTime.now().minus(Duration.ofMillis(5100))) // 5.1 seconds ago (> 5.0s)
                .build();

        CancelEmergencyRequest request = CancelEmergencyRequest.builder()
                .emergencyPassword("sos123")
                .build();

        when(emergencyRepository.findById(100L)).thenReturn(Optional.of(emergency));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                emergencyService.cancelEmergency(100L, request, "+919876543210"));

        assertTrue(ex.getMessage().contains("Cancellation window has expired"));
    }

    @Test
    void cancelEmergency_ExactlyAtBoundary_Over5000ms_Rejected() {
        Emergency emergency = Emergency.builder()
                .id(100L)
                .requester(requester)
                .status(EmergencyStatus.ACTIVE)
                .createdAt(LocalDateTime.now().minus(Duration.ofMillis(5005))) // 5005 ms (> 5000 ms)
                .build();

        CancelEmergencyRequest request = CancelEmergencyRequest.builder()
                .emergencyPassword("sos123")
                .build();

        when(emergencyRepository.findById(100L)).thenReturn(Optional.of(emergency));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                emergencyService.cancelEmergency(100L, request, "+919876543210"));

        assertTrue(ex.getMessage().contains("Cancellation window has expired"));
    }

    // ==========================================
    // 2. 25-Meter Responder Arrival Tests
    // ==========================================

    @Test
    void arriveEmergency_Within25Meters_Success() {
        // Requester at New Delhi (28.6139, 77.2090)
        Emergency emergency = Emergency.builder()
                .id(100L)
                .requester(requester)
                .responder(responder1)
                .status(EmergencyStatus.ACCEPTED)
                .latitude(28.613900)
                .longitude(77.209000)
                .build();
        emergency.addResponder(responder1);

        // Responder very close (approx ~10 meters away: 0.0001 deg lat is ~11 meters)
        Point responderPoint = new Point(77.209000, 28.613990);

        when(emergencyRepository.findById(100L)).thenReturn(Optional.of(emergency));
        when(userRepository.findByMobileNumber("+919876543211")).thenReturn(Optional.of(responder1));
        when(locationService.getUserCoordinates(2L)).thenReturn(responderPoint);
        when(locationService.isLocationFresh(2L)).thenReturn(true);
        when(emergencyRepository.save(any(Emergency.class))).thenAnswer(i -> i.getArgument(0));

        EmergencyResponse response = emergencyService.arriveEmergency(100L, "+919876543211");

        assertNotNull(response);
        assertEquals(2L, response.getArrivedResponderId());
        assertEquals("Ravi Responder 1", response.getArrivedResponderName());
        assertNotNull(response.getArrivedAt());
    }

    @Test
    void arriveEmergency_Over25Meters_ThrowsIllegalStateException() {
        // Requester at (28.613900, 77.209000)
        Emergency emergency = Emergency.builder()
                .id(100L)
                .requester(requester)
                .responder(responder1)
                .status(EmergencyStatus.ACCEPTED)
                .latitude(28.613900)
                .longitude(77.209000)
                .build();
        emergency.addResponder(responder1);

        // Responder is ~100 meters away (0.0009 deg lat difference)
        Point responderPoint = new Point(77.209000, 28.614800);

        when(emergencyRepository.findById(100L)).thenReturn(Optional.of(emergency));
        when(userRepository.findByMobileNumber("+919876543211")).thenReturn(Optional.of(responder1));
        when(locationService.getUserCoordinates(2L)).thenReturn(responderPoint);
        when(locationService.isLocationFresh(2L)).thenReturn(true);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                emergencyService.arriveEmergency(100L, "+919876543211"));

        assertTrue(ex.getMessage().contains("Must be within 25 meters"));
    }

    @Test
    void arriveEmergency_UnacceptedResponder_ThrowsAccessDenied() {
        Emergency emergency = Emergency.builder()
                .id(100L)
                .requester(requester)
                .status(EmergencyStatus.ACTIVE)
                .latitude(28.613900)
                .longitude(77.209000)
                .build();

        when(emergencyRepository.findById(100L)).thenReturn(Optional.of(emergency));
        when(userRepository.findByMobileNumber("+919876543211")).thenReturn(Optional.of(responder1));

        assertThrows(AccessDeniedException.class, () ->
                emergencyService.arriveEmergency(100L, "+919876543211"));
    }

    @Test
    void arriveEmergency_ConcurrencySafety_OnlyOneArrivedResponderAssigned() {
        Emergency emergency = Emergency.builder()
                .id(100L)
                .requester(requester)
                .responder(responder1)
                .arrivedResponder(responder1) // Responder 1 already arrived!
                .arrivedAt(LocalDateTime.now().minusMinutes(1))
                .status(EmergencyStatus.ACCEPTED)
                .latitude(28.613900)
                .longitude(77.209000)
                .build();
        emergency.addResponder(responder1);
        emergency.addResponder(responder2);

        // Responder 2 is also within 25m
        Point responder2Point = new Point(77.209000, 28.613950);

        when(emergencyRepository.findById(100L)).thenReturn(Optional.of(emergency));
        when(userRepository.findByMobileNumber("+919876543212")).thenReturn(Optional.of(responder2));
        when(locationService.getUserCoordinates(3L)).thenReturn(responder2Point);
        when(locationService.isLocationFresh(3L)).thenReturn(true);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                emergencyService.arriveEmergency(100L, "+919876543212"));

        assertTrue(ex.getMessage().contains("Another responder (Ravi Responder 1) has already arrived"));
    }

    // ==========================================
    // 3. Two-Party Resolution Tests
    // ==========================================

    @Test
    void resolveEmergency_RequesterConfirmsFirst_AwaitingResponder() {
        Emergency emergency = Emergency.builder()
                .id(100L)
                .requester(requester)
                .responder(responder1)
                .arrivedResponder(responder1)
                .arrivedAt(LocalDateTime.now())
                .status(EmergencyStatus.ACCEPTED)
                .build();
        emergency.addResponder(responder1);

        when(emergencyRepository.findById(100L)).thenReturn(Optional.of(emergency));
        when(emergencyRepository.save(any(Emergency.class))).thenAnswer(i -> i.getArgument(0));

        // Requester confirms first
        EmergencyResponse response = emergencyService.resolveEmergency(100L, "+919876543210");

        assertNotNull(response);
        assertEquals(EmergencyStatus.ACCEPTED, response.getStatus()); // Still ACCEPTED, not RESOLVED yet
        assertTrue(response.isRequesterResolveConfirmed());
        assertFalse(response.isResponderResolveConfirmed());
    }

    @Test
    void resolveEmergency_TwoPartyConfirmation_BothConfirm_TransitionsToResolved() {
        Emergency emergency = Emergency.builder()
                .id(100L)
                .requester(requester)
                .responder(responder1)
                .arrivedResponder(responder1)
                .arrivedAt(LocalDateTime.now())
                .status(EmergencyStatus.ACCEPTED)
                .requesterResolveConfirmed(true) // Requester already confirmed!
                .build();
        emergency.addResponder(responder1);

        when(emergencyRepository.findById(100L)).thenReturn(Optional.of(emergency));
        when(emergencyRepository.save(any(Emergency.class))).thenAnswer(i -> i.getArgument(0));

        // Arrived responder confirms second
        EmergencyResponse response = emergencyService.resolveEmergency(100L, "+919876543211");

        assertNotNull(response);
        assertEquals(EmergencyStatus.RESOLVED, response.getStatus()); // NOW RESOLVED!
        assertTrue(response.isRequesterResolveConfirmed());
        assertTrue(response.isResponderResolveConfirmed());
        assertNotNull(response.getResolvedAt());
    }

    @Test
    void resolveEmergency_ReverseOrder_ResponderConfirmsFirst_ThenRequesterResolves() {
        Emergency emergency = Emergency.builder()
                .id(100L)
                .requester(requester)
                .responder(responder1)
                .arrivedResponder(responder1)
                .arrivedAt(LocalDateTime.now())
                .status(EmergencyStatus.ACCEPTED)
                .responderResolveConfirmed(true) // Responder confirmed first!
                .build();
        emergency.addResponder(responder1);

        when(emergencyRepository.findById(100L)).thenReturn(Optional.of(emergency));
        when(emergencyRepository.save(any(Emergency.class))).thenAnswer(i -> i.getArgument(0));

        // Requester confirms second
        EmergencyResponse response = emergencyService.resolveEmergency(100L, "+919876543210");

        assertNotNull(response);
        assertEquals(EmergencyStatus.RESOLVED, response.getStatus());
        assertTrue(response.isRequesterResolveConfirmed());
        assertTrue(response.isResponderResolveConfirmed());
        assertNotNull(response.getResolvedAt());
    }

    @Test
    void resolveEmergency_NonArrivedResponder_ThrowsException() {
        Emergency emergency = Emergency.builder()
                .id(100L)
                .requester(requester)
                .responder(responder1)
                .arrivedResponder(null) // No arrival yet!
                .status(EmergencyStatus.ACCEPTED)
                .latitude(28.613900)
                .longitude(77.209000)
                .build();
        emergency.addResponder(responder1);

        // Responder is 500m away
        Point distantPoint = new Point(77.209000, 28.618400);
        when(emergencyRepository.findById(100L)).thenReturn(Optional.of(emergency));
        when(locationService.getUserCoordinates(2L)).thenReturn(distantPoint);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                emergencyService.resolveEmergency(100L, "+919876543211"));

        assertTrue(ex.getMessage().contains("Only the arrived responder"));
    }

    @Test
    void resolveEmergency_AnotherResponderWhenArrivedResponderExists_ThrowsAccessDenied() {
        Emergency emergency = Emergency.builder()
                .id(100L)
                .requester(requester)
                .responder(responder1)
                .arrivedResponder(responder1) // Responder 1 is arrived!
                .arrivedAt(LocalDateTime.now())
                .status(EmergencyStatus.ACCEPTED)
                .build();
        emergency.addResponder(responder1);
        emergency.addResponder(responder2);

        when(emergencyRepository.findById(100L)).thenReturn(Optional.of(emergency));

        // Responder 2 tries to confirm resolution instead of responder 1
        assertThrows(AccessDeniedException.class, () ->
                emergencyService.resolveEmergency(100L, "+919876543212"));
    }
}
