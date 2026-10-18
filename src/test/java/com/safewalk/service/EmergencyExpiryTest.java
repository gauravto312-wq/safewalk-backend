package com.safewalk.service;

import com.safewalk.dto.request.CancelEmergencyRequest;
import com.safewalk.dto.response.EmergencyResponse;
import com.safewalk.entity.AccountStatus;
import com.safewalk.entity.Emergency;
import com.safewalk.entity.EmergencyStatus;
import com.safewalk.entity.EmergencyType;
import com.safewalk.entity.Role;
import com.safewalk.entity.User;
import com.safewalk.mapper.EmergencyMapper;
import com.safewalk.repository.EmergencyContactRepository;
import com.safewalk.repository.EmergencyRepository;
import com.safewalk.repository.UserRepository;
import com.safewalk.scheduler.EmergencyExpiryScheduler;
import com.safewalk.service.impl.EmergencyServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmergencyExpiryTest {

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

    private EmergencyMapper emergencyMapper;
    private PasswordEncoder passwordEncoder;
    private EmergencyServiceImpl emergencyService;

    private User requester;
    private User responder;

    @BeforeEach
    void setUp() {
        emergencyMapper = new EmergencyMapper();
        passwordEncoder = new BCryptPasswordEncoder();
        emergencyService = new EmergencyServiceImpl(
                emergencyRepository, userRepository, emergencyMapper, passwordEncoder, sosEventProducer,
                webSocketService, fcmService, null, locationService, emergencyContactRepository);

        requester = User.builder()
                .id(1L)
                .name("Aman Sharma")
                .mobileNumber("+919876543210")
                .password(passwordEncoder.encode("password123"))
                .emergencyPassword(passwordEncoder.encode("sos12345"))
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
    }

    @Test
    void expireOverdueEmergencies_ActiveEmergencyExpiredSuccessfully() {
        LocalDateTime createdTime = LocalDateTime.now().minusHours(6);
        Emergency overdueEmergency = Emergency.builder()
                .id(101L)
                .requester(requester)
                .type(EmergencyType.GENERAL)
                .status(EmergencyStatus.ACTIVE)
                .latitude(26.8467)
                .longitude(80.9462)
                .createdAt(createdTime)
                .updatedAt(createdTime)
                .responders(new HashSet<>())
                .build();

        when(emergencyRepository.findOverdueEmergencies(any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(overdueEmergency));
        when(emergencyRepository.save(any(Emergency.class))).thenAnswer(invocation -> invocation.getArgument(0));

        int expiredCount = emergencyService.expireOverdueEmergencies();

        assertEquals(1, expiredCount);
        assertEquals(EmergencyStatus.EXPIRED, overdueEmergency.getStatus());
        assertNotNull(overdueEmergency.getExpiredAt());
        assertEquals(createdTime.plusHours(5), overdueEmergency.getExpiredAt());

        verify(webSocketService, times(3)).broadcastEvent(any(), any());
    }

    @Test
    void expireOverdueEmergencies_AcceptedEmergencyAlsoExpired() {
        LocalDateTime createdTime = LocalDateTime.now().minusHours(7);
        Emergency acceptedEmergency = Emergency.builder()
                .id(102L)
                .requester(requester)
                .responder(responder)
                .type(EmergencyType.GENERAL)
                .status(EmergencyStatus.ACCEPTED)
                .latitude(26.8467)
                .longitude(80.9462)
                .createdAt(createdTime)
                .acceptedAt(createdTime.plusMinutes(10))
                .responders(new HashSet<>(Collections.singletonList(responder)))
                .build();

        when(emergencyRepository.findOverdueEmergencies(any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(acceptedEmergency));
        when(emergencyRepository.save(any(Emergency.class))).thenAnswer(invocation -> invocation.getArgument(0));

        int expiredCount = emergencyService.expireOverdueEmergencies();

        assertEquals(1, expiredCount);
        assertEquals(EmergencyStatus.EXPIRED, acceptedEmergency.getStatus());
        assertEquals(createdTime.plusHours(5), acceptedEmergency.getExpiredAt());
    }

    @Test
    void expireOverdueEmergencies_NoOverdueEmergenciesReturnsZero() {
        when(emergencyRepository.findOverdueEmergencies(any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        int expiredCount = emergencyService.expireOverdueEmergencies();

        assertEquals(0, expiredCount);
        verify(emergencyRepository, never()).save(any());
    }

    @Test
    void cannotCancelExpiredEmergency() {
        Emergency expiredEmergency = Emergency.builder()
                .id(103L)
                .requester(requester)
                .status(EmergencyStatus.EXPIRED)
                .createdAt(LocalDateTime.now().minusHours(6))
                .expiredAt(LocalDateTime.now().minusHours(1))
                .build();

        when(emergencyRepository.findById(103L)).thenReturn(Optional.of(expiredEmergency));

        CancelEmergencyRequest cancelReq = new CancelEmergencyRequest();
        cancelReq.setEmergencyPassword("sos12345");

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                emergencyService.cancelEmergency(103L, cancelReq, requester.getMobileNumber()));

        assertTrue(ex.getMessage().contains("EXPIRED"));
        verify(emergencyRepository, never()).save(any());
    }

    @Test
    void cannotResolveExpiredEmergency() {
        Emergency expiredEmergency = Emergency.builder()
                .id(104L)
                .requester(requester)
                .status(EmergencyStatus.EXPIRED)
                .createdAt(LocalDateTime.now().minusHours(6))
                .expiredAt(LocalDateTime.now().minusHours(1))
                .build();

        when(emergencyRepository.findByIdWithPessimisticLock(104L)).thenReturn(Optional.of(expiredEmergency));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                emergencyService.resolveEmergency(104L, requester.getMobileNumber()));

        assertTrue(ex.getMessage().contains("EXPIRED"));
        verify(emergencyRepository, never()).save(any());
    }

    @Test
    void cannotAcceptExpiredEmergency() {
        Emergency expiredEmergency = Emergency.builder()
                .id(105L)
                .requester(requester)
                .status(EmergencyStatus.EXPIRED)
                .createdAt(LocalDateTime.now().minusHours(6))
                .expiredAt(LocalDateTime.now().minusHours(1))
                .build();

        when(emergencyRepository.findById(105L)).thenReturn(Optional.of(expiredEmergency));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                emergencyService.acceptEmergency(105L, responder.getMobileNumber()));

        assertTrue(ex.getMessage().contains("EXPIRED"));
        verify(emergencyRepository, never()).save(any());
    }

    @Test
    void cannotArriveAtExpiredEmergency() {
        Emergency expiredEmergency = Emergency.builder()
                .id(106L)
                .requester(requester)
                .status(EmergencyStatus.EXPIRED)
                .createdAt(LocalDateTime.now().minusHours(6))
                .expiredAt(LocalDateTime.now().minusHours(1))
                .build();

        when(emergencyRepository.findByIdWithPessimisticLock(106L)).thenReturn(Optional.of(expiredEmergency));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                emergencyService.arriveEmergency(106L, responder.getMobileNumber()));

        assertTrue(ex.getMessage().contains("EXPIRED"));
        verify(emergencyRepository, never()).save(any());
    }

    @Test
    void cannotGetLocationOfExpiredEmergency() {
        Emergency expiredEmergency = Emergency.builder()
                .id(107L)
                .requester(requester)
                .status(EmergencyStatus.EXPIRED)
                .createdAt(LocalDateTime.now().minusHours(6))
                .expiredAt(LocalDateTime.now().minusHours(1))
                .build();

        when(emergencyRepository.findById(107L)).thenReturn(Optional.of(expiredEmergency));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                emergencyService.getEmergencyLocations(107L, requester.getMobileNumber(), false));

        assertTrue(ex.getMessage().contains("EXPIRED"));
    }

    @Test
    void scheduler_TriggersStartupAndPeriodicReconciliation() {
        EmergencyExpiryScheduler scheduler = new EmergencyExpiryScheduler(emergencyService);

        when(emergencyRepository.findOverdueEmergencies(any(LocalDateTime.class))).thenReturn(Collections.emptyList());

        scheduler.onStartupReconciliation();
        verify(emergencyRepository, times(1)).findOverdueEmergencies(any());

        scheduler.scheduleEmergencyExpiryCheck();
        verify(emergencyRepository, times(2)).findOverdueEmergencies(any());
    }

    @Test
    void expireOverdueEmergencies_ResolvedEmergencyNotOverwritten() {
        LocalDateTime createdTime = LocalDateTime.now().minusHours(6);
        Emergency candidate = Emergency.builder()
                .id(108L)
                .requester(requester)
                .status(EmergencyStatus.ACTIVE)
                .createdAt(createdTime)
                .build();

        Emergency resolvedState = Emergency.builder()
                .id(108L)
                .requester(requester)
                .status(EmergencyStatus.RESOLVED)
                .createdAt(createdTime)
                .resolvedAt(LocalDateTime.now().minusMinutes(10))
                .build();

        when(emergencyRepository.findOverdueEmergencies(any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(candidate));
        when(emergencyRepository.findByIdWithPessimisticLock(108L))
                .thenReturn(Optional.of(resolvedState));

        int expiredCount = emergencyService.expireOverdueEmergencies();

        assertEquals(0, expiredCount);
        assertEquals(EmergencyStatus.RESOLVED, resolvedState.getStatus());
        verify(emergencyRepository, never()).save(resolvedState);
    }

    @Test
    void expireOverdueEmergencies_CancelledEmergencyNotOverwritten() {
        LocalDateTime createdTime = LocalDateTime.now().minusHours(6);
        Emergency candidate = Emergency.builder()
                .id(109L)
                .requester(requester)
                .status(EmergencyStatus.ACTIVE)
                .createdAt(createdTime)
                .build();

        Emergency cancelledState = Emergency.builder()
                .id(109L)
                .requester(requester)
                .status(EmergencyStatus.CANCELLED)
                .createdAt(createdTime)
                .cancelledAt(LocalDateTime.now().minusMinutes(5))
                .build();

        when(emergencyRepository.findOverdueEmergencies(any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(candidate));
        when(emergencyRepository.findByIdWithPessimisticLock(109L))
                .thenReturn(Optional.of(cancelledState));

        int expiredCount = emergencyService.expireOverdueEmergencies();

        assertEquals(0, expiredCount);
        assertEquals(EmergencyStatus.CANCELLED, cancelledState.getStatus());
        verify(emergencyRepository, never()).save(cancelledState);
    }

    @Test
    void cancelEmergency_UsesPessimisticLock_RejectsExpiredEmergency() {
        Emergency expiredEmergency = Emergency.builder()
                .id(110L)
                .requester(requester)
                .status(EmergencyStatus.EXPIRED)
                .createdAt(LocalDateTime.now().minusHours(6))
                .expiredAt(LocalDateTime.now().minusHours(1))
                .build();

        when(emergencyRepository.findByIdWithPessimisticLock(110L)).thenReturn(Optional.of(expiredEmergency));

        CancelEmergencyRequest cancelReq = new CancelEmergencyRequest();
        cancelReq.setEmergencyPassword("sos12345");

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                emergencyService.cancelEmergency(110L, cancelReq, requester.getMobileNumber()));

        assertTrue(ex.getMessage().contains("EXPIRED"));
        verify(emergencyRepository, never()).save(any());
    }

    @Test
    void acceptEmergency_UsesPessimisticLock_RejectsExpiredEmergency() {
        Emergency expiredEmergency = Emergency.builder()
                .id(111L)
                .requester(requester)
                .status(EmergencyStatus.EXPIRED)
                .createdAt(LocalDateTime.now().minusHours(6))
                .expiredAt(LocalDateTime.now().minusHours(1))
                .build();

        when(emergencyRepository.findByIdWithPessimisticLock(111L)).thenReturn(Optional.of(expiredEmergency));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                emergencyService.acceptEmergency(111L, responder.getMobileNumber()));

        assertTrue(ex.getMessage().contains("EXPIRED"));
        verify(emergencyRepository, never()).save(any());
    }

    @Test
    void concurrentLifecycleOperations_ExpiryVsResolve_ProducesValidFinalStatus() throws Exception {
        LocalDateTime createdTime = LocalDateTime.now().minusHours(6);
        final Emergency emergency = Emergency.builder()
                .id(112L)
                .requester(requester)
                .status(EmergencyStatus.ACTIVE)
                .createdAt(createdTime)
                .build();

        java.util.concurrent.atomic.AtomicReference<EmergencyStatus> currentStatus =
                new java.util.concurrent.atomic.AtomicReference<>(EmergencyStatus.ACTIVE);

        when(emergencyRepository.findOverdueEmergencies(any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(emergency));

        when(emergencyRepository.findByIdWithPessimisticLock(112L)).thenAnswer(inv -> {
            Emergency current = Emergency.builder()
                    .id(112L)
                    .requester(requester)
                    .status(currentStatus.get())
                    .createdAt(createdTime)
                    .build();
            return Optional.of(current);
        });

        when(emergencyRepository.save(any(Emergency.class))).thenAnswer(inv -> {
            Emergency saved = inv.getArgument(0);
            currentStatus.set(saved.getStatus());
            return saved;
        });

        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(2);
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);

        java.util.concurrent.Future<Integer> expiryFuture = executor.submit(() -> {
            latch.await();
            return emergencyService.expireOverdueEmergencies();
        });

        java.util.concurrent.Future<?> resolveFuture = executor.submit(() -> {
            latch.await();
            try {
                return emergencyService.resolveEmergency(112L, requester.getMobileNumber());
            } catch (IllegalStateException e) {
                return e.getMessage();
            }
        });

        latch.countDown();
        expiryFuture.get(5, java.util.concurrent.TimeUnit.SECONDS);
        resolveFuture.get(5, java.util.concurrent.TimeUnit.SECONDS);
        executor.shutdown();

        EmergencyStatus finalStatus = currentStatus.get();
        assertTrue(finalStatus == EmergencyStatus.EXPIRED || finalStatus == EmergencyStatus.RESOLVED);
    }
}
