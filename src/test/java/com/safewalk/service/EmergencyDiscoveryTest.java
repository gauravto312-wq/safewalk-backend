package com.safewalk.service;

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
import com.safewalk.scheduler.EmergencyDiscoveryScheduler;
import com.safewalk.service.impl.EmergencyServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

@ExtendWith(MockitoExtension.class)
class EmergencyDiscoveryTest {

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
    private EmergencyDiscoveryScheduler scheduler;

    private User requester;
    private User responder;
    private Emergency activeEmergency;

    @BeforeEach
    void setUp() {
        emergencyMapper = new EmergencyMapper();
        passwordEncoder = new BCryptPasswordEncoder();
        emergencyService = new EmergencyServiceImpl(
                emergencyRepository, userRepository, emergencyMapper, passwordEncoder, sosEventProducer,
                webSocketService, fcmService, null, locationService, emergencyContactRepository,
                emergencyDispatchRepository);

        scheduler = new EmergencyDiscoveryScheduler(emergencyService);

        requester = User.builder()
                .id(1L)
                .name("Aman Sharma")
                .mobileNumber("+919876543210")
                .password(passwordEncoder.encode("password123"))
                .role(Role.USER)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        responder = User.builder()
                .id(2L)
                .name("Priya Verma")
                .mobileNumber("+919876543211")
                .password(passwordEncoder.encode("password123"))
                .role(Role.USER)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        activeEmergency = Emergency.builder()
                .id(100L)
                .requester(requester)
                .type(EmergencyType.GENERAL)
                .status(EmergencyStatus.ACTIVE)
                .latitude(28.6139)
                .longitude(77.2090)
                .createdAt(LocalDateTime.now().minusMinutes(10))
                .build();
    }

    @Test
    @DisplayName("Continuous Discovery: Successfully discovers and dispatches FCM to new nearby responder")
    void testDiscoverAndDispatchNewResponders_Success() {
        when(emergencyRepository.findActiveEmergenciesForDiscovery(any(LocalDateTime.class)))
                .thenReturn(List.of(activeEmergency));
        when(emergencyRepository.findByIdWithPessimisticLock(100L))
                .thenReturn(Optional.of(activeEmergency));
        when(emergencyRepository.findById(100L))
                .thenReturn(Optional.of(activeEmergency));

        NearbyUserResponse nearby = NearbyUserResponse.builder()
                .userId(2L)
                .mobileNumber("+919876543211")
                .distanceKm(0.8)
                .build();

        when(locationService.findNearbyResponders(eq(28.6139), eq(77.2090), eq(2.0)))
                .thenReturn(List.of(nearby));
        when(locationService.isLocationFresh(2L)).thenReturn(true);
        when(emergencyDispatchRepository.findByEmergencyIdAndResponderId(100L, 2L)).thenReturn(Optional.empty());
        when(userRepository.getReferenceById(2L)).thenReturn(responder);

        EmergencyDispatch mockPendingDispatch = EmergencyDispatch.builder()
                .id(501L)
                .emergency(activeEmergency)
                .responder(responder)
                .status(DispatchStatus.PENDING)
                .distanceMeters(800.0)
                .build();
        when(emergencyDispatchRepository.saveAndFlush(any(EmergencyDispatch.class)))
                .thenReturn(mockPendingDispatch);

        when(fcmService.sendPushNotificationToUserWithTtl(
                eq(2L), anyString(), anyString(), anyMap(), anyLong(), eq("safewalk_emergency_alert")))
                .thenReturn(1);

        int count = emergencyService.discoverAndDispatchNewResponders();

        assertEquals(1, count);
        verify(emergencyDispatchRepository).saveAndFlush(any(EmergencyDispatch.class));
        verify(fcmService).sendPushNotificationToUserWithTtl(
                eq(2L), anyString(), anyString(), anyMap(), anyLong(), eq("safewalk_emergency_alert"));
        verify(webSocketService).sendToUser(eq("+919876543211"), eq("/queue/messages"), any());
    }

    @Test
    @DisplayName("Continuous Discovery: Skips responder who already received dispatch (SENT)")
    void testDiscoverAndDispatch_SkipsAlreadyDispatched() {
        when(emergencyRepository.findActiveEmergenciesForDiscovery(any(LocalDateTime.class)))
                .thenReturn(List.of(activeEmergency));
        when(emergencyRepository.findByIdWithPessimisticLock(100L))
                .thenReturn(Optional.of(activeEmergency));
        when(emergencyRepository.findById(100L))
                .thenReturn(Optional.of(activeEmergency));

        NearbyUserResponse nearby = NearbyUserResponse.builder()
                .userId(2L)
                .mobileNumber("+919876543211")
                .distanceKm(0.8)
                .build();

        when(locationService.findNearbyResponders(eq(28.6139), eq(77.2090), eq(2.0)))
                .thenReturn(List.of(nearby));
        when(locationService.isLocationFresh(2L)).thenReturn(true);

        EmergencyDispatch sentDispatch = EmergencyDispatch.builder()
                .id(501L)
                .emergency(activeEmergency)
                .responder(responder)
                .status(DispatchStatus.SENT)
                .build();
        when(emergencyDispatchRepository.findByEmergencyIdAndResponderId(100L, 2L)).thenReturn(Optional.of(sentDispatch));

        int count = emergencyService.discoverAndDispatchNewResponders();

        assertEquals(0, count);
        verify(fcmService, never()).sendPushNotificationToUserWithTtl(anyLong(), anyString(), anyString(), anyMap(), anyLong(), anyString());
        verify(emergencyDispatchRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Continuous Discovery: Skips responder with stale location coordinates")
    void testDiscoverAndDispatch_SkipsStaleLocation() {
        when(emergencyRepository.findActiveEmergenciesForDiscovery(any(LocalDateTime.class)))
                .thenReturn(List.of(activeEmergency));
        when(emergencyRepository.findByIdWithPessimisticLock(100L))
                .thenReturn(Optional.of(activeEmergency));
        when(emergencyRepository.findById(100L))
                .thenReturn(Optional.of(activeEmergency));

        NearbyUserResponse nearby = NearbyUserResponse.builder()
                .userId(2L)
                .mobileNumber("+919876543211")
                .distanceKm(0.8)
                .build();

        when(locationService.findNearbyResponders(eq(28.6139), eq(77.2090), eq(2.0)))
                .thenReturn(List.of(nearby));
        // Stale location coordinates (> 300s)
        when(locationService.isLocationFresh(2L)).thenReturn(false);

        int count = emergencyService.discoverAndDispatchNewResponders();

        assertEquals(0, count);
        verify(emergencyDispatchRepository, never()).findByEmergencyIdAndResponderId(anyLong(), anyLong());
        verify(fcmService, never()).sendPushNotificationToUserWithTtl(anyLong(), anyString(), anyString(), anyMap(), anyLong(), anyString());
    }

    @Test
    @DisplayName("Continuous Discovery: Skips requester themselves if returned by location service")
    void testDiscoverAndDispatch_SkipsRequester() {
        when(emergencyRepository.findActiveEmergenciesForDiscovery(any(LocalDateTime.class)))
                .thenReturn(List.of(activeEmergency));
        when(emergencyRepository.findByIdWithPessimisticLock(100L))
                .thenReturn(Optional.of(activeEmergency));
        when(emergencyRepository.findById(100L))
                .thenReturn(Optional.of(activeEmergency));

        NearbyUserResponse nearbyRequester = NearbyUserResponse.builder()
                .userId(1L) // Requester ID
                .mobileNumber("+919876543210")
                .distanceKm(0.0)
                .build();

        when(locationService.findNearbyResponders(eq(28.6139), eq(77.2090), eq(2.0)))
                .thenReturn(List.of(nearbyRequester));

        int count = emergencyService.discoverAndDispatchNewResponders();

        assertEquals(0, count);
        verify(emergencyDispatchRepository, never()).findByEmergencyIdAndResponderId(anyLong(), anyLong());
        verify(fcmService, never()).sendPushNotificationToUserWithTtl(anyLong(), anyString(), anyString(), anyMap(), anyLong(), anyString());
    }

    @Test
    @DisplayName("Continuous Discovery: Skips emergency if status transitioned to ACCEPTED or EXPIRED")
    void testDiscoverAndDispatch_SkipsNonActiveEmergency() {
        Emergency acceptedEmergency = Emergency.builder()
                .id(101L)
                .requester(requester)
                .status(EmergencyStatus.ACCEPTED)
                .latitude(28.6139)
                .longitude(77.2090)
                .createdAt(LocalDateTime.now().minusMinutes(5))
                .build();

        when(emergencyRepository.findActiveEmergenciesForDiscovery(any(LocalDateTime.class)))
                .thenReturn(List.of(acceptedEmergency));
        when(emergencyRepository.findByIdWithPessimisticLock(101L))
                .thenReturn(Optional.of(acceptedEmergency));

        int count = emergencyService.discoverAndDispatchNewResponders();

        assertEquals(0, count);
        verify(locationService, never()).findNearbyResponders(anyDouble(), anyDouble(), anyDouble());
    }

    @Test
    @DisplayName("Continuous Discovery: Skips emergency older than 5 hours (TTL <= 0)")
    void testDiscoverAndDispatch_SkipsOverdueEmergency() {
        Emergency oldEmergency = Emergency.builder()
                .id(102L)
                .requester(requester)
                .status(EmergencyStatus.ACTIVE)
                .latitude(28.6139)
                .longitude(77.2090)
                .createdAt(LocalDateTime.now().minusHours(5).minusMinutes(1))
                .build();

        when(emergencyRepository.findActiveEmergenciesForDiscovery(any(LocalDateTime.class)))
                .thenReturn(List.of(oldEmergency));
        when(emergencyRepository.findByIdWithPessimisticLock(102L))
                .thenReturn(Optional.of(oldEmergency));

        int count = emergencyService.discoverAndDispatchNewResponders();

        assertEquals(0, count);
        verify(locationService, never()).findNearbyResponders(anyDouble(), anyDouble(), anyDouble());
    }

    @Test
    @DisplayName("Continuous Discovery: Calculates dynamic TTL accurately against 5-hour lifetime")
    void testDiscoverAndDispatch_CalculatesDynamicTtl() {
        // Created 2 hours ago: remaining TTL should be ~3 hours (between 2h59m and 3h01m)
        LocalDateTime twoHoursAgo = LocalDateTime.now().minusHours(2);
        Emergency twoHourOldEmergency = Emergency.builder()
                .id(103L)
                .requester(requester)
                .status(EmergencyStatus.ACTIVE)
                .latitude(28.6139)
                .longitude(77.2090)
                .createdAt(twoHoursAgo)
                .build();

        when(emergencyRepository.findActiveEmergenciesForDiscovery(any(LocalDateTime.class)))
                .thenReturn(List.of(twoHourOldEmergency));
        when(emergencyRepository.findByIdWithPessimisticLock(103L))
                .thenReturn(Optional.of(twoHourOldEmergency));
        when(emergencyRepository.findById(103L))
                .thenReturn(Optional.of(twoHourOldEmergency));

        NearbyUserResponse nearby = NearbyUserResponse.builder()
                .userId(2L)
                .mobileNumber("+919876543211")
                .distanceKm(1.2)
                .build();

        when(locationService.findNearbyResponders(eq(28.6139), eq(77.2090), eq(2.0)))
                .thenReturn(List.of(nearby));
        when(locationService.isLocationFresh(2L)).thenReturn(true);
        when(emergencyDispatchRepository.findByEmergencyIdAndResponderId(103L, 2L)).thenReturn(Optional.empty());
        when(userRepository.getReferenceById(2L)).thenReturn(responder);

        ArgumentCaptor<Long> ttlCaptor = ArgumentCaptor.forClass(Long.class);
        when(fcmService.sendPushNotificationToUserWithTtl(
                eq(2L), anyString(), anyString(), anyMap(), ttlCaptor.capture(), eq("safewalk_emergency_alert")))
                .thenReturn(1);

        emergencyService.discoverAndDispatchNewResponders();

        Long capturedTtl = ttlCaptor.getValue();
        assertNotNull(capturedTtl);
        long threeHoursMillis = 3 * 3600 * 1000L;
        // Verify TTL is within 5 seconds of 3 hours
        assertTrue(Math.abs(capturedTtl - threeHoursMillis) < 5000L,
                "TTL should be ~3 hours (10,800,000 ms), but was: " + capturedTtl);
    }

    @Test
    @DisplayName("Continuous Discovery: Gracefully handles concurrent duplicate dispatch insert")
    void testDiscoverAndDispatch_HandlesConcurrentDuplicate() {
        when(emergencyRepository.findActiveEmergenciesForDiscovery(any(LocalDateTime.class)))
                .thenReturn(List.of(activeEmergency));
        when(emergencyRepository.findByIdWithPessimisticLock(100L))
                .thenReturn(Optional.of(activeEmergency));
        when(emergencyRepository.findById(100L))
                .thenReturn(Optional.of(activeEmergency));

        NearbyUserResponse nearby = NearbyUserResponse.builder()
                .userId(2L)
                .mobileNumber("+919876543211")
                .distanceKm(0.8)
                .build();

        when(locationService.findNearbyResponders(eq(28.6139), eq(77.2090), eq(2.0)))
                .thenReturn(List.of(nearby));
        when(locationService.isLocationFresh(2L)).thenReturn(true);
        when(emergencyDispatchRepository.findByEmergencyIdAndResponderId(100L, 2L)).thenReturn(Optional.empty());
        when(userRepository.getReferenceById(2L)).thenReturn(responder);

        // Simulate database unique constraint violation from concurrent process
        when(emergencyDispatchRepository.saveAndFlush(any(EmergencyDispatch.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate entry for uk_emergency_responder"));

        int count = emergencyService.discoverAndDispatchNewResponders();

        assertEquals(0, count);
        verify(fcmService, never()).sendPushNotificationToUserWithTtl(anyLong(), anyString(), anyString(), anyMap(), anyLong(), anyString());
    }

    @Test
    @DisplayName("Discovery Scheduler: Successfully triggers emergency discovery cycle")
    void testDiscoveryScheduler_ExecutesCycle() {
        when(emergencyRepository.findActiveEmergenciesForDiscovery(any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        scheduler.scheduleContinuousDiscovery();

        verify(emergencyRepository).findActiveEmergenciesForDiscovery(any(LocalDateTime.class));
    }

    @Test
    @DisplayName("Risk C: Aborts continuous discovery loop immediately when emergency transitions to ACCEPTED mid-loop")
    void testDiscoverAndDispatch_AbortsWhenEmergencyTransitionsToAcceptedMidLoop() {
        when(emergencyRepository.findActiveEmergenciesForDiscovery(any(LocalDateTime.class)))
                .thenReturn(List.of(activeEmergency));
        when(emergencyRepository.findByIdWithPessimisticLock(100L))
                .thenReturn(Optional.of(activeEmergency));

        NearbyUserResponse responder1 = NearbyUserResponse.builder()
                .userId(2L)
                .mobileNumber("+919876543211")
                .distanceKm(0.5)
                .build();
        NearbyUserResponse responder2 = NearbyUserResponse.builder()
                .userId(3L)
                .mobileNumber("+919876543212")
                .distanceKm(0.9)
                .build();

        when(locationService.findNearbyResponders(eq(28.6139), eq(77.2090), eq(2.0)))
                .thenReturn(List.of(responder1, responder2));

        Emergency acceptedEmergency = Emergency.builder()
                .id(100L)
                .requester(requester)
                .type(EmergencyType.GENERAL)
                .status(EmergencyStatus.ACCEPTED)
                .latitude(28.6139)
                .longitude(77.2090)
                .createdAt(activeEmergency.getCreatedAt())
                .build();

        // 1st iteration for responder 1: activeEmergency. 2nd iteration for responder 2: acceptedEmergency
        when(emergencyRepository.findById(100L))
                .thenReturn(Optional.of(activeEmergency))
                .thenReturn(Optional.of(acceptedEmergency));

        when(locationService.isLocationFresh(2L)).thenReturn(true);
        when(emergencyDispatchRepository.findByEmergencyIdAndResponderId(100L, 2L)).thenReturn(Optional.empty());
        when(userRepository.getReferenceById(2L)).thenReturn(responder);

        when(fcmService.sendPushNotificationToUserWithTtl(
                eq(2L), anyString(), anyString(), anyMap(), anyLong(), eq("safewalk_emergency_alert")))
                .thenReturn(1);

        int count = emergencyService.discoverAndDispatchNewResponders();

        // Only responder 1 should be dispatched; loop aborted before responder 2
        assertEquals(1, count);
        verify(fcmService, times(1)).sendPushNotificationToUserWithTtl(
                eq(2L), anyString(), anyString(), anyMap(), anyLong(), eq("safewalk_emergency_alert"));
        verify(fcmService, never()).sendPushNotificationToUserWithTtl(
                eq(3L), anyString(), anyString(), anyMap(), anyLong(), anyString());
    }

    @Test
    @DisplayName("Risk B: Retries previously FAILED dispatch when within bounded retry limit")
    void testDiscoverAndDispatch_RetriesFailedDispatchWithinBoundedLimit() {
        when(emergencyRepository.findActiveEmergenciesForDiscovery(any(LocalDateTime.class)))
                .thenReturn(List.of(activeEmergency));
        when(emergencyRepository.findByIdWithPessimisticLock(100L))
                .thenReturn(Optional.of(activeEmergency));
        when(emergencyRepository.findById(100L))
                .thenReturn(Optional.of(activeEmergency));

        NearbyUserResponse nearby = NearbyUserResponse.builder()
                .userId(2L)
                .mobileNumber("+919876543211")
                .distanceKm(0.8)
                .build();

        when(locationService.findNearbyResponders(eq(28.6139), eq(77.2090), eq(2.0)))
                .thenReturn(List.of(nearby));
        when(locationService.isLocationFresh(2L)).thenReturn(true);

        // Previous failed attempt was 2 minutes ago, retryCount = 1 (< 2 bounded limit)
        EmergencyDispatch failedDispatch = EmergencyDispatch.builder()
                .id(601L)
                .emergency(activeEmergency)
                .responder(responder)
                .status(DispatchStatus.FAILED)
                .retryCount(1)
                .lastAttemptAt(LocalDateTime.now().minusMinutes(2))
                .build();
        when(emergencyDispatchRepository.findByEmergencyIdAndResponderId(100L, 2L))
                .thenReturn(Optional.of(failedDispatch));
        when(emergencyDispatchRepository.saveAndFlush(any(EmergencyDispatch.class)))
                .thenReturn(failedDispatch);

        when(fcmService.sendPushNotificationToUserWithTtl(
                eq(2L), anyString(), anyString(), anyMap(), anyLong(), eq("safewalk_emergency_alert")))
                .thenReturn(1);

        int count = emergencyService.discoverAndDispatchNewResponders();

        assertEquals(1, count);
        assertEquals(2, failedDispatch.getRetryCount());
        assertEquals(DispatchStatus.SENT, failedDispatch.getStatus());
        verify(fcmService).sendPushNotificationToUserWithTtl(
                eq(2L), anyString(), anyString(), anyMap(), anyLong(), eq("safewalk_emergency_alert"));
    }

    @Test
    @DisplayName("Risk B: Suppresses retrying FAILED dispatch when bounded retry limit (2) is reached")
    void testDiscoverAndDispatch_SuppressesFailedDispatchExceedingRetryLimit() {
        when(emergencyRepository.findActiveEmergenciesForDiscovery(any(LocalDateTime.class)))
                .thenReturn(List.of(activeEmergency));
        when(emergencyRepository.findByIdWithPessimisticLock(100L))
                .thenReturn(Optional.of(activeEmergency));
        when(emergencyRepository.findById(100L))
                .thenReturn(Optional.of(activeEmergency));

        NearbyUserResponse nearby = NearbyUserResponse.builder()
                .userId(2L)
                .mobileNumber("+919876543211")
                .distanceKm(0.8)
                .build();

        when(locationService.findNearbyResponders(eq(28.6139), eq(77.2090), eq(2.0)))
                .thenReturn(List.of(nearby));
        when(locationService.isLocationFresh(2L)).thenReturn(true);

        // Max retries reached (retryCount = 2)
        EmergencyDispatch exhaustedDispatch = EmergencyDispatch.builder()
                .id(602L)
                .emergency(activeEmergency)
                .responder(responder)
                .status(DispatchStatus.FAILED)
                .retryCount(2)
                .lastAttemptAt(LocalDateTime.now().minusMinutes(10))
                .build();
        when(emergencyDispatchRepository.findByEmergencyIdAndResponderId(100L, 2L))
                .thenReturn(Optional.of(exhaustedDispatch));

        int count = emergencyService.discoverAndDispatchNewResponders();

        assertEquals(0, count);
        verify(fcmService, never()).sendPushNotificationToUserWithTtl(anyLong(), anyString(), anyString(), anyMap(), anyLong(), anyString());
    }

    @Test
    @DisplayName("Risk A: Recovers stale PENDING dispatch record from previous server crash")
    void testDiscoverAndDispatch_RecoversStalePendingDispatch() {
        when(emergencyRepository.findActiveEmergenciesForDiscovery(any(LocalDateTime.class)))
                .thenReturn(List.of(activeEmergency));
        when(emergencyRepository.findByIdWithPessimisticLock(100L))
                .thenReturn(Optional.of(activeEmergency));
        when(emergencyRepository.findById(100L))
                .thenReturn(Optional.of(activeEmergency));

        NearbyUserResponse nearby = NearbyUserResponse.builder()
                .userId(2L)
                .mobileNumber("+919876543211")
                .distanceKm(0.8)
                .build();

        when(locationService.findNearbyResponders(eq(28.6139), eq(77.2090), eq(2.0)))
                .thenReturn(List.of(nearby));
        when(locationService.isLocationFresh(2L)).thenReturn(true);

        // Dispatch was stuck in PENDING for 5 minutes (crash recovery)
        EmergencyDispatch staleDispatch = EmergencyDispatch.builder()
                .id(603L)
                .emergency(activeEmergency)
                .responder(responder)
                .status(DispatchStatus.PENDING)
                .createdAt(LocalDateTime.now().minusMinutes(5))
                .build();
        when(emergencyDispatchRepository.findByEmergencyIdAndResponderId(100L, 2L))
                .thenReturn(Optional.of(staleDispatch));
        when(emergencyDispatchRepository.saveAndFlush(any(EmergencyDispatch.class)))
                .thenReturn(staleDispatch);

        when(fcmService.sendPushNotificationToUserWithTtl(
                eq(2L), anyString(), anyString(), anyMap(), anyLong(), eq("safewalk_emergency_alert")))
                .thenReturn(1);

        int count = emergencyService.discoverAndDispatchNewResponders();

        assertEquals(1, count);
        assertEquals(DispatchStatus.SENT, staleDispatch.getStatus());
        verify(fcmService).sendPushNotificationToUserWithTtl(
                eq(2L), anyString(), anyString(), anyMap(), anyLong(), eq("safewalk_emergency_alert"));
    }
}
