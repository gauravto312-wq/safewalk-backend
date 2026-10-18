package com.safewalk.event.consumer;

import com.safewalk.dto.response.NearbyUserResponse;
import com.safewalk.entity.AccountStatus;
import com.safewalk.entity.EmergencyContact;
import com.safewalk.entity.Role;
import com.safewalk.entity.User;
import com.safewalk.event.SosCreatedEvent;
import com.safewalk.repository.EmergencyContactRepository;
import com.safewalk.service.FcmService;
import com.safewalk.service.LocationService;
import com.safewalk.service.WebSocketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SosEventConsumerFeatureTest {

    @Mock
    private WebSocketService webSocketService;

    @Mock
    private LocationService locationService;

    @Mock
    private FcmService fcmService;

    @Mock
    private EmergencyContactRepository emergencyContactRepository;

    private SosEventConsumer sosEventConsumer;

    private User requester;
    private User contact1;
    private User contact2;

    @BeforeEach
    void setUp() {
        sosEventConsumer = new SosEventConsumer(webSocketService, locationService, fcmService, emergencyContactRepository);

        requester = User.builder().id(1L).mobileNumber("+919876543210").role(Role.USER).accountStatus(AccountStatus.ACTIVE).build();
        contact1 = User.builder().id(10L).name("Contact Ten").mobileNumber("+911010101010").role(Role.USER).accountStatus(AccountStatus.ACTIVE).build();
        contact2 = User.builder().id(20L).name("Contact Twenty").mobileNumber("+912020202020").role(Role.USER).accountStatus(AccountStatus.ACTIVE).build();
    }

    @Test
    void consumeSosCreatedEvent_EmergencyContactsNotifiedRegardlessOfDistance_AndDeduplicated() {
        SosCreatedEvent event = SosCreatedEvent.builder()
                .emergencyId(999L)
                .requesterId(1L)
                .requesterMobile("+919876543210")
                .type("GENERAL")
                .status("ACTIVE")
                .latitude(28.6139)
                .longitude(77.2090)
                .build();

        // Requester has Contact 10 (who is also nearby!) and Contact 20 (who is 50km away / not in GEO)
        EmergencyContact ec1 = EmergencyContact.builder().id(100L).user(requester).contactUser(contact1).build();
        EmergencyContact ec2 = EmergencyContact.builder().id(101L).user(requester).contactUser(contact2).build();
        when(emergencyContactRepository.findByUserId(1L)).thenReturn(Arrays.asList(ec1, ec2));

        // Redis GEO discovers: Contact 10 (at 0.2km), Responder 30 (at 0.5km), and Requester 1 (at 0.0km)
        NearbyUserResponse nearbyContact10 = NearbyUserResponse.builder().userId(10L).distanceKm(0.2).build();
        NearbyUserResponse nearbyStranger30 = NearbyUserResponse.builder().userId(30L).distanceKm(0.5).build();
        NearbyUserResponse nearbyRequester1 = NearbyUserResponse.builder().userId(1L).distanceKm(0.0).build();

        when(locationService.findNearbyResponders(28.6139, 77.2090, 2.0))
                .thenReturn(Arrays.asList(nearbyContact10, nearbyStranger30, nearbyRequester1));

        assertDoesNotThrow(() -> sosEventConsumer.consumeSosCreatedEvent(event));

        // 1. Verify Contact 10 received Trusted Contact Alert
        verify(fcmService).sendPushNotificationToUser(
                eq(10L),
                contains("TRUSTED CONTACT"),
                any(),
                any()
        );

        // 2. Verify Contact 20 (distant contact) received Trusted Contact Alert
        verify(fcmService).sendPushNotificationToUser(
                eq(20L),
                contains("TRUSTED CONTACT"),
                any(),
                any()
        );

        // 3. Verify Contact 10 did NOT receive a second (duplicate) standard alert!
        verify(fcmService, times(1)).sendPushNotificationToUser(eq(10L), any(), any(), any());

        // 4. Verify nearby stranger 30 received standard emergency alert
        verify(fcmService).sendPushNotificationToUser(
                eq(30L),
                eq("🚨 Emergency Alert"),
                any(),
                any()
        );

        // 5. Verify Requester themselves NEVER received any alert
        verify(fcmService, never()).sendPushNotificationToUser(eq(1L), any(), any(), any());
    }
}
