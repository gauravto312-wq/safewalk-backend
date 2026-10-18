package com.safewalk.event.consumer;

import com.safewalk.dto.response.NearbyUserResponse;
import com.safewalk.entity.AccountStatus;
import com.safewalk.entity.Role;
import com.safewalk.event.SosCreatedEvent;
import com.safewalk.service.FcmService;
import com.safewalk.service.LocationService;
import com.safewalk.service.WebSocketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SosEventConsumerTest {

    @Mock
    private WebSocketService webSocketService;

    @Mock
    private LocationService locationService;

    @Mock
    private FcmService fcmService;

    private SosEventConsumer sosEventConsumer;

    @BeforeEach
    void setUp() {
        sosEventConsumer = new SosEventConsumer(webSocketService, locationService, fcmService);
    }

    @Test
    void consumeSosCreatedEvent_ValidEvent_DiscoversNearbyRespondersAndDispatchesAlerts() {
        SosCreatedEvent event = SosCreatedEvent.builder()
                .emergencyId(100L)
                .requesterId(1L)
                .requesterMobile("+919876543210")
                .type("GENERAL")
                .status("ACTIVE")
                .latitude(28.6139)
                .longitude(77.2090)
                .build();

        NearbyUserResponse responder = NearbyUserResponse.builder()
                .userId(2L)
                .name("Priya Responder")
                .mobileNumber("+919876543211")
                .latitude(28.6140)
                .longitude(77.2095)
                .distanceKm(0.3)
                .role(Role.USER)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        when(locationService.findNearbyResponders(28.6139, 77.2090, 2.0))
                .thenReturn(Collections.singletonList(responder));

        assertDoesNotThrow(() -> sosEventConsumer.consumeSosCreatedEvent(event));

        verify(locationService).findNearbyResponders(28.6139, 77.2090, 2.0);
        verify(fcmService).sendPushNotificationToUser(eq(2L), eq("🚨 Emergency Alert"), any(), any());
        verify(webSocketService).broadcastEvent(eq("/topic/emergencies"), any());
    }

    @Test
    void consumeSosCreatedEvent_NullEvent_HandledGracefully() {
        assertDoesNotThrow(() -> sosEventConsumer.consumeSosCreatedEvent(null));
    }
}
