package com.safewalk.service;

import com.safewalk.dto.response.EmergencyLocationResponse;
import com.safewalk.dto.response.EmergencyResponse;
import com.safewalk.dto.response.NearbyUserResponse;
import com.safewalk.entity.AccountStatus;
import com.safewalk.entity.DispatchStatus;
import com.safewalk.entity.Emergency;
import com.safewalk.entity.EmergencyDispatch;
import com.safewalk.entity.EmergencyStatus;
import com.safewalk.entity.EmergencyType;
import com.safewalk.entity.Role;
import com.safewalk.entity.User;
import com.safewalk.mapper.EmergencyMapper;
import com.safewalk.repository.EmergencyContactRepository;
import com.safewalk.repository.EmergencyDispatchRepository;
import com.safewalk.repository.EmergencyRepository;
import com.safewalk.repository.UserRepository;
import com.safewalk.service.impl.EmergencyServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.geo.Point;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Feature 3: Movement-Aware Dynamic Responder Discovery and Lifecycle Alignment Tests.
 * Verifies all 13 Phase C scenarios covering requester movement, continuous discovery,
 * deduplication, acceptance, arrival, resolution, fallback, expiry, and participant retention.
 */
@ExtendWith(MockitoExtension.class)
class EmergencyMovementDiscoveryTest {

    @Mock
    private EmergencyRepository emergencyRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private com.safewalk.event.producer.SosEventProducer sosEventProducer;

    @Mock
    private WebSocketService webSocketService;

    @Mock
    private FcmService fcmService;

    @Mock
    private LocationService locationService;

    @Mock
    private EmergencyContactRepository emergencyContactRepository;

    @Mock
    private EmergencyDispatchRepository emergencyDispatchRepository;

    private EmergencyMapper emergencyMapper;
    private PasswordEncoder passwordEncoder;
    private EmergencyServiceImpl emergencyService;

    private User requester;
    private User responder1;
    private User responder2;
    private Emergency activeEmergency;

    // Location A (Creation location): Bangalore MG Road
    private static final double LOC_A_LAT = 12.9716;
    private static final double LOC_A_LNG = 77.5946;

    // Location B (Moved location ~1.2 km North): Bangalore Cubbon Park
    private static final double LOC_B_LAT = 12.9824;
    private static final double LOC_B_LNG = 77.5946;

    @BeforeEach
    void setUp() {
        emergencyMapper = new EmergencyMapper();
        passwordEncoder = new BCryptPasswordEncoder();
        emergencyService = new EmergencyServiceImpl(
                emergencyRepository, userRepository, emergencyMapper, passwordEncoder, sosEventProducer,
                webSocketService, fcmService, null, locationService, emergencyContactRepository,
                emergencyDispatchRepository);

        requester = User.builder()
                .id(1L)
                .name("Aman Requester")
                .mobileNumber("+919876543210")
                .password(passwordEncoder.encode("password123"))
                .role(Role.USER)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        responder1 = User.builder()
                .id(2L)
                .name("Priya Responder 1")
                .mobileNumber("+919876543211")
                .password(passwordEncoder.encode("password123"))
                .role(Role.USER)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        responder2 = User.builder()
                .id(3L)
                .name("Rahul Responder 2")
                .mobileNumber("+919876543212")
                .password(passwordEncoder.encode("password123"))
                .role(Role.USER)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        activeEmergency = Emergency.builder()
                .id(100L)
                .requester(requester)
                .type(EmergencyType.GENERAL)
                .status(EmergencyStatus.ACTIVE)
                .latitude(LOC_A_LAT)
                .longitude(LOC_A_LNG)
                .createdAt(LocalDateTime.now().minusMinutes(10))
                .responders(new HashSet<>())
                .build();
    }

    @Test
    @DisplayName("Scenario 1: Requester movement - discovery cycle uses moved coordinates")
    void testRequesterMovement_NextDiscoveryCycleUsesNewLocation() {
        when(emergencyRepository.findActiveEmergenciesForDiscovery(any(LocalDateTime.class)))
                .thenReturn(List.of(activeEmergency));
        when(emergencyRepository.findByIdWithPessimisticLock(100L))
                .thenReturn(Optional.of(activeEmergency));
        when(emergencyRepository.findById(100L))
                .thenReturn(Optional.of(activeEmergency));

        // Requester live coordinates in Redis: Location B
        when(locationService.getUserCoordinates(1L)).thenReturn(new Point(LOC_B_LNG, LOC_B_LAT));
        when(locationService.getUserLocationTimestamp(1L)).thenReturn(System.currentTimeMillis() - 5000L);
        when(locationService.isLocationFresh(1L)).thenReturn(true);

        NearbyUserResponse nearbyAtLocB = NearbyUserResponse.builder()
                .userId(2L)
                .mobileNumber("+919876543211")
                .distanceKm(0.5)
                .build();

        // Must search around Location B, NOT Location A
        when(locationService.findNearbyResponders(eq(LOC_B_LAT), eq(LOC_B_LNG), eq(2.0)))
                .thenReturn(List.of(nearbyAtLocB));
        when(locationService.isLocationFresh(2L)).thenReturn(true);
        when(emergencyDispatchRepository.findByEmergencyIdAndResponderId(100L, 2L)).thenReturn(Optional.empty());
        when(userRepository.getReferenceById(2L)).thenReturn(responder1);

        EmergencyDispatch mockPending = EmergencyDispatch.builder()
                .id(501L)
                .emergency(activeEmergency)
                .responder(responder1)
                .status(DispatchStatus.PENDING)
                .build();
        when(emergencyDispatchRepository.saveAndFlush(any(EmergencyDispatch.class))).thenReturn(mockPending);

        int count = emergencyService.discoverAndDispatchNewResponders();

        assertEquals(1, count);
        verify(locationService).findNearbyResponders(eq(LOC_B_LAT), eq(LOC_B_LNG), eq(2.0));
        verify(locationService, never()).findNearbyResponders(eq(LOC_A_LAT), eq(LOC_A_LNG), anyDouble());
    }

    @Test
    @DisplayName("Scenario 2: Old location - responder near only original location is not discovered after requester moves away")
    void testOldLocation_ResponderNearOldLocationNotDiscoveredAfterMovement() {
        when(emergencyRepository.findActiveEmergenciesForDiscovery(any(LocalDateTime.class)))
                .thenReturn(List.of(activeEmergency));
        when(emergencyRepository.findByIdWithPessimisticLock(100L))
                .thenReturn(Optional.of(activeEmergency));

        // Requester has moved to Location B (now ~1.2 km away)
        when(locationService.getUserCoordinates(1L)).thenReturn(new Point(LOC_B_LNG, LOC_B_LAT));
        when(locationService.getUserLocationTimestamp(1L)).thenReturn(System.currentTimeMillis() - 10000L);
        when(locationService.isLocationFresh(1L)).thenReturn(true);

        // At Location B, no nearby responders are found (responder at Location A is out of radius)
        when(locationService.findNearbyResponders(eq(LOC_B_LAT), eq(LOC_B_LNG), eq(2.0)))
                .thenReturn(Collections.emptyList());

        int count = emergencyService.discoverAndDispatchNewResponders();

        assertEquals(0, count);
        verify(fcmService, never()).sendPushNotificationToUserWithTtl(anyLong(), anyString(), anyString(), anyMap(), anyLong(), anyString());
    }

    @Test
    @DisplayName("Scenario 3: New responder enters radius - discovered on subsequent cycle")
    void testNewResponderEntersRadius_DiscoveredOnSubsequentCycle() {
        when(emergencyRepository.findActiveEmergenciesForDiscovery(any(LocalDateTime.class)))
                .thenReturn(List.of(activeEmergency));
        when(emergencyRepository.findByIdWithPessimisticLock(100L))
                .thenReturn(Optional.of(activeEmergency));
        when(emergencyRepository.findById(100L))
                .thenReturn(Optional.of(activeEmergency));

        // Requester at Location B
        when(locationService.getUserCoordinates(1L)).thenReturn(new Point(LOC_B_LNG, LOC_B_LAT));
        when(locationService.getUserLocationTimestamp(1L)).thenReturn(System.currentTimeMillis());
        when(locationService.isLocationFresh(1L)).thenReturn(true);

        // Responder 2 entered the 2 km radius around Location B
        NearbyUserResponse newlyEnteredResponder = NearbyUserResponse.builder()
                .userId(3L)
                .mobileNumber("+919876543212")
                .distanceKm(1.1)
                .build();
        when(locationService.findNearbyResponders(eq(LOC_B_LAT), eq(LOC_B_LNG), eq(2.0)))
                .thenReturn(List.of(newlyEnteredResponder));
        when(locationService.isLocationFresh(3L)).thenReturn(true);
        when(emergencyDispatchRepository.findByEmergencyIdAndResponderId(100L, 3L)).thenReturn(Optional.empty());
        when(userRepository.getReferenceById(3L)).thenReturn(responder2);

        EmergencyDispatch mockDispatch = EmergencyDispatch.builder()
                .id(502L)
                .emergency(activeEmergency)
                .responder(responder2)
                .status(DispatchStatus.PENDING)
                .build();
        when(emergencyDispatchRepository.saveAndFlush(any(EmergencyDispatch.class))).thenReturn(mockDispatch);
        when(fcmService.sendPushNotificationToUserWithTtl(eq(3L), anyString(), anyString(), anyMap(), anyLong(), anyString()))
                .thenReturn(1);

        int count = emergencyService.discoverAndDispatchNewResponders();

        assertEquals(1, count);
        verify(fcmService).sendPushNotificationToUserWithTtl(eq(3L), anyString(), anyString(), anyMap(), anyLong(), eq("safewalk_emergency_alert"));
    }

    @Test
    @DisplayName("Scenario 4: Repeated cycles - zero duplicate dispatch records or notifications")
    void testRepeatedCycles_NoDuplicateDispatch() {
        when(emergencyRepository.findActiveEmergenciesForDiscovery(any(LocalDateTime.class)))
                .thenReturn(List.of(activeEmergency));
        when(emergencyRepository.findByIdWithPessimisticLock(100L))
                .thenReturn(Optional.of(activeEmergency));
        when(emergencyRepository.findById(100L))
                .thenReturn(Optional.of(activeEmergency));

        when(locationService.getUserCoordinates(1L)).thenReturn(new Point(LOC_B_LNG, LOC_B_LAT));
        when(locationService.getUserLocationTimestamp(1L)).thenReturn(System.currentTimeMillis());
        when(locationService.isLocationFresh(1L)).thenReturn(true);

        NearbyUserResponse nearby = NearbyUserResponse.builder()
                .userId(2L)
                .mobileNumber("+919876543211")
                .distanceKm(0.8)
                .build();
        when(locationService.findNearbyResponders(eq(LOC_B_LAT), eq(LOC_B_LNG), eq(2.0)))
                .thenReturn(List.of(nearby));
        when(locationService.isLocationFresh(2L)).thenReturn(true);

        // Already dispatched in previous cycle (status SENT)
        EmergencyDispatch sentDispatch = EmergencyDispatch.builder()
                .id(501L)
                .emergency(activeEmergency)
                .responder(responder1)
                .status(DispatchStatus.SENT)
                .build();
        when(emergencyDispatchRepository.findByEmergencyIdAndResponderId(100L, 2L))
                .thenReturn(Optional.of(sentDispatch));

        int count = emergencyService.discoverAndDispatchNewResponders();

        assertEquals(0, count);
        verify(fcmService, never()).sendPushNotificationToUserWithTtl(anyLong(), anyString(), anyString(), anyMap(), anyLong(), anyString());
        verify(emergencyDispatchRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Scenario 5: Movement and acceptance - responder near moved requester accepts successfully")
    void testMovementAndAcceptance_ResponderNearMovedRequesterCanAccept() {
        // Requester live location is Location B
        when(emergencyRepository.findById(100L)).thenReturn(Optional.of(activeEmergency));
        when(userRepository.findByMobileNumber("+919876543211")).thenReturn(Optional.of(responder1));

        when(locationService.getUserCoordinates(1L)).thenReturn(new Point(LOC_B_LNG, LOC_B_LAT));
        when(locationService.getUserLocationTimestamp(1L)).thenReturn(System.currentTimeMillis());
        when(locationService.isLocationFresh(1L)).thenReturn(true);

        // Responder is ~200 meters from Location B (Cubbon Park)
        // 0.0018 deg latitude difference is ~200 m
        double responderLat = LOC_B_LAT + 0.0018;
        when(locationService.getUserCoordinates(2L)).thenReturn(new Point(LOC_B_LNG, responderLat));
        when(locationService.isLocationFresh(2L)).thenReturn(true);
        when(emergencyRepository.save(any(Emergency.class))).thenAnswer(i -> i.getArgument(0));

        EmergencyResponse response = emergencyService.acceptEmergency(100L, "+919876543211");

        assertNotNull(response);
        assertEquals(EmergencyStatus.ACCEPTED, response.getStatus());
        // Distance is ~200 m, well within 50m - 2000m rule
        assertTrue(response.getDistanceMeters() > 50.0 && response.getDistanceMeters() <= 2000.0);
    }

    @Test
    @DisplayName("Scenario 6: Movement and arrival - arrival distance verified against live requester position")
    void testMovementAndArrival_ArrivalThresholdVerifiedAgainstLiveRequester() {
        activeEmergency.setStatus(EmergencyStatus.ACCEPTED);
        activeEmergency.addResponder(responder1);

        when(emergencyRepository.findById(100L)).thenReturn(Optional.of(activeEmergency));
        when(userRepository.findByMobileNumber("+919876543211")).thenReturn(Optional.of(responder1));

        // Requester moved to Location B
        when(locationService.getUserCoordinates(1L)).thenReturn(new Point(LOC_B_LNG, LOC_B_LAT));
        when(locationService.getUserLocationTimestamp(1L)).thenReturn(System.currentTimeMillis());
        when(locationService.isLocationFresh(1L)).thenReturn(true);

        // Responder is 15 meters from Location B (approx 0.00013 deg latitude)
        double responderLat = LOC_B_LAT + 0.00013;
        when(locationService.getUserCoordinates(2L)).thenReturn(new Point(LOC_B_LNG, responderLat));
        when(locationService.isLocationFresh(2L)).thenReturn(true);
        when(emergencyRepository.save(any(Emergency.class))).thenAnswer(i -> i.getArgument(0));

        EmergencyResponse response = emergencyService.arriveEmergency(100L, "+919876543211");

        assertNotNull(response);
        assertEquals(2L, response.getArrivedResponderId());
        assertNotNull(response.getArrivedAt());
    }

    @Test
    @DisplayName("Scenario 7: Resolution - two-party resolution with moved requester")
    void testResolution_TwoPartyResolutionWithMovedRequester() {
        activeEmergency.setStatus(EmergencyStatus.ACCEPTED);
        activeEmergency.addResponder(responder1);
        activeEmergency.setArrivedResponder(responder1);
        activeEmergency.setArrivedAt(LocalDateTime.now().minusMinutes(2));

        when(emergencyRepository.findById(100L)).thenReturn(Optional.of(activeEmergency));
        when(emergencyRepository.save(any(Emergency.class))).thenAnswer(i -> i.getArgument(0));

        // Step 1: Requester confirms resolution
        EmergencyResponse r1 = emergencyService.resolveEmergency(100L, "+919876543210");
        assertEquals(EmergencyStatus.ACCEPTED, r1.getStatus());
        assertTrue(r1.isRequesterResolveConfirmed());
        assertFalse(r1.isResponderResolveConfirmed());

        // Step 2: Responder confirms resolution -> resolves fully
        EmergencyResponse r2 = emergencyService.resolveEmergency(100L, "+919876543211");
        assertEquals(EmergencyStatus.RESOLVED, r2.getStatus());
        assertTrue(r2.isRequesterResolveConfirmed());
        assertTrue(r2.isResponderResolveConfirmed());
    }

    @Test
    @DisplayName("Scenario 8: Stale or missing location - falls back safely to original creation coordinates")
    void testStaleOrMissingLocation_SafelyFallsBackToCreationCoordinates() {
        when(emergencyRepository.findActiveEmergenciesForDiscovery(any(LocalDateTime.class)))
                .thenReturn(List.of(activeEmergency));
        when(emergencyRepository.findByIdWithPessimisticLock(100L))
                .thenReturn(Optional.of(activeEmergency));

        // Stale coordinates in Redis (> 300s)
        when(locationService.getUserCoordinates(1L)).thenReturn(new Point(LOC_B_LNG, LOC_B_LAT));
        when(locationService.getUserLocationTimestamp(1L)).thenReturn(System.currentTimeMillis() - 400_000L);
        when(locationService.isLocationFresh(1L)).thenReturn(false);

        // Because Redis coordinates are stale, scheduler must fall back to creation location A
        when(locationService.findNearbyResponders(eq(LOC_A_LAT), eq(LOC_A_LNG), eq(2.0)))
                .thenReturn(Collections.emptyList());

        int count = emergencyService.discoverAndDispatchNewResponders();

        assertEquals(0, count);
        verify(locationService).findNearbyResponders(eq(LOC_A_LAT), eq(LOC_A_LNG), eq(2.0));
        verify(locationService, never()).findNearbyResponders(eq(LOC_B_LAT), eq(LOC_B_LNG), anyDouble());
    }

    @Test
    @DisplayName("Scenario 9: Expiry - emergency older than 5 hours is not redispatched, movement does not extend life")
    void testExpiry_OverdueEmergencyNotRedispatchedAndMovementDoesNotExtendLifetime() {
        Emergency overdueEmergency = Emergency.builder()
                .id(101L)
                .requester(requester)
                .status(EmergencyStatus.ACTIVE)
                .latitude(LOC_A_LAT)
                .longitude(LOC_A_LNG)
                // Created 5 hours and 5 minutes ago
                .createdAt(LocalDateTime.now().minusHours(5).minusMinutes(5))
                .build();

        when(emergencyRepository.findActiveEmergenciesForDiscovery(any(LocalDateTime.class)))
                .thenReturn(List.of(overdueEmergency));
        when(emergencyRepository.findByIdWithPessimisticLock(101L))
                .thenReturn(Optional.of(overdueEmergency));

        // Even if requester is moving with fresh coordinates
        when(locationService.getUserCoordinates(1L)).thenReturn(new Point(LOC_B_LNG, LOC_B_LAT));
        when(locationService.getUserLocationTimestamp(1L)).thenReturn(System.currentTimeMillis());
        when(locationService.isLocationFresh(1L)).thenReturn(true);

        int count = emergencyService.discoverAndDispatchNewResponders();

        assertEquals(0, count);
        verify(locationService, never()).findNearbyResponders(anyDouble(), anyDouble(), anyDouble());
    }

    @Test
    @DisplayName("Scenario 10: Terminal states and races - non-ACTIVE emergencies cannot receive new dispatches")
    void testTerminalStatesAndRaces_CancelledOrResolvedCannotReceiveDispatches() {
        Emergency cancelledEmergency = Emergency.builder()
                .id(102L)
                .requester(requester)
                .status(EmergencyStatus.CANCELLED)
                .latitude(LOC_A_LAT)
                .longitude(LOC_A_LNG)
                .createdAt(LocalDateTime.now().minusMinutes(5))
                .build();

        when(emergencyRepository.findActiveEmergenciesForDiscovery(any(LocalDateTime.class)))
                .thenReturn(List.of(cancelledEmergency));
        when(emergencyRepository.findByIdWithPessimisticLock(102L))
                .thenReturn(Optional.of(cancelledEmergency));

        int count = emergencyService.discoverAndDispatchNewResponders();

        assertEquals(0, count);
        verify(locationService, never()).findNearbyResponders(anyDouble(), anyDouble(), anyDouble());
    }

    @Test
    @DisplayName("Scenario 11: Retry behavior - bounded retry for FAILED dispatch with backoff")
    void testRetryBehavior_FailedDispatchEligibleForBoundedRetry() {
        when(emergencyRepository.findActiveEmergenciesForDiscovery(any(LocalDateTime.class)))
                .thenReturn(List.of(activeEmergency));
        when(emergencyRepository.findByIdWithPessimisticLock(100L))
                .thenReturn(Optional.of(activeEmergency));
        when(emergencyRepository.findById(100L))
                .thenReturn(Optional.of(activeEmergency));

        when(locationService.getUserCoordinates(1L)).thenReturn(new Point(LOC_B_LNG, LOC_B_LAT));
        when(locationService.getUserLocationTimestamp(1L)).thenReturn(System.currentTimeMillis());
        when(locationService.isLocationFresh(1L)).thenReturn(true);

        NearbyUserResponse nearby = NearbyUserResponse.builder()
                .userId(2L)
                .mobileNumber("+919876543211")
                .distanceKm(0.8)
                .build();
        when(locationService.findNearbyResponders(eq(LOC_B_LAT), eq(LOC_B_LNG), eq(2.0)))
                .thenReturn(List.of(nearby));
        when(locationService.isLocationFresh(2L)).thenReturn(true);

        // Previous attempt failed 90 seconds ago (retryCount=1, past 60s backoff)
        EmergencyDispatch failedDispatch = EmergencyDispatch.builder()
                .id(501L)
                .emergency(activeEmergency)
                .responder(responder1)
                .status(DispatchStatus.FAILED)
                .retryCount(1)
                .lastAttemptAt(LocalDateTime.now().minusSeconds(90))
                .build();
        when(emergencyDispatchRepository.findByEmergencyIdAndResponderId(100L, 2L))
                .thenReturn(Optional.of(failedDispatch));
        when(emergencyDispatchRepository.saveAndFlush(any(EmergencyDispatch.class)))
                .thenReturn(failedDispatch);
        when(fcmService.sendPushNotificationToUserWithTtl(eq(2L), anyString(), anyString(), anyMap(), anyLong(), anyString()))
                .thenReturn(1);

        int count = emergencyService.discoverAndDispatchNewResponders();

        assertEquals(1, count);
        assertEquals(2, failedDispatch.getRetryCount());
        verify(fcmService).sendPushNotificationToUserWithTtl(eq(2L), anyString(), anyString(), anyMap(), anyLong(), anyString());
    }

    @Test
    @DisplayName("Scenario 12: FCM configuration - TTL is bounded by original SOS 5-hour lifetime and collapse key is fixed")
    void testFcmConfiguration_TtlBoundedByOriginalSosExpiryAndCollapseKeyFixed() {
        LocalDateTime createdAt = LocalDateTime.now().minusHours(2);
        activeEmergency.setCreatedAt(createdAt);

        when(emergencyRepository.findActiveEmergenciesForDiscovery(any(LocalDateTime.class)))
                .thenReturn(List.of(activeEmergency));
        when(emergencyRepository.findByIdWithPessimisticLock(100L))
                .thenReturn(Optional.of(activeEmergency));
        when(emergencyRepository.findById(100L))
                .thenReturn(Optional.of(activeEmergency));

        when(locationService.getUserCoordinates(1L)).thenReturn(new Point(LOC_B_LNG, LOC_B_LAT));
        when(locationService.getUserLocationTimestamp(1L)).thenReturn(System.currentTimeMillis());
        when(locationService.isLocationFresh(1L)).thenReturn(true);

        NearbyUserResponse nearby = NearbyUserResponse.builder()
                .userId(2L)
                .mobileNumber("+919876543211")
                .distanceKm(0.7)
                .build();
        when(locationService.findNearbyResponders(eq(LOC_B_LAT), eq(LOC_B_LNG), eq(2.0)))
                .thenReturn(List.of(nearby));
        when(locationService.isLocationFresh(2L)).thenReturn(true);
        when(emergencyDispatchRepository.findByEmergencyIdAndResponderId(100L, 2L)).thenReturn(Optional.empty());
        when(userRepository.getReferenceById(2L)).thenReturn(responder1);

        EmergencyDispatch mockDispatch = EmergencyDispatch.builder()
                .id(501L)
                .emergency(activeEmergency)
                .responder(responder1)
                .status(DispatchStatus.PENDING)
                .build();
        when(emergencyDispatchRepository.saveAndFlush(any(EmergencyDispatch.class))).thenReturn(mockDispatch);

        emergencyService.discoverAndDispatchNewResponders();

        ArgumentCaptor<Long> ttlCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<String> collapseKeyCaptor = ArgumentCaptor.forClass(String.class);
        verify(fcmService).sendPushNotificationToUserWithTtl(
                eq(2L), anyString(), anyString(), anyMap(), ttlCaptor.capture(), collapseKeyCaptor.capture());

        assertEquals("safewalk_emergency_alert", collapseKeyCaptor.getValue());
        // Remaining time should be approximately 3 hours (3 * 3600 * 1000 ms) +/- 5 seconds
        long remainingTtl = ttlCaptor.getValue();
        long expectedTtl = java.time.Duration.between(LocalDateTime.now(), createdAt.plusHours(5)).toMillis();
        assertTrue(Math.abs(remainingTtl - expectedTtl) < 5000);
    }

    @Test
    @DisplayName("Scenario 13: Existing participants - accepted responder beyond 2 km retains access")
    void testExistingParticipants_AcceptedResponderBeyond2kmRetainsAccess() {
        activeEmergency.setStatus(EmergencyStatus.ACCEPTED);
        activeEmergency.setResponder(responder1);
        activeEmergency.addResponder(responder1);

        // Caller is responder1
        when(userRepository.findByMobileNumber("+919876543211")).thenReturn(Optional.of(responder1));
        when(emergencyRepository.findByStatusInOrderByCreatedAtDesc(any()))
                .thenReturn(List.of(activeEmergency));

        // Requester at Location B
        when(locationService.getUserCoordinates(1L)).thenReturn(new Point(LOC_B_LNG, LOC_B_LAT));
        when(locationService.getUserLocationTimestamp(1L)).thenReturn(System.currentTimeMillis());
        when(locationService.isLocationFresh(1L)).thenReturn(true);

        // Responder is 4.5 km away from requester (Location B)
        // 0.04 deg lat is ~4.44 km
        double distantResponderLat = LOC_B_LAT + 0.04;
        when(locationService.getUserCoordinates(2L)).thenReturn(new Point(LOC_B_LNG, distantResponderLat));
        when(locationService.isLocationFresh(2L)).thenReturn(true);

        List<EmergencyResponse> activeList = emergencyService.getActiveEmergencies("+919876543211");

        // Accepted responder MUST retain access and see emergency, despite being > 2 km away
        assertEquals(1, activeList.size());
        assertEquals(100L, activeList.get(0).getId());
        assertTrue(activeList.get(0).getDistanceMeters() > 2000.0);
    }
}
