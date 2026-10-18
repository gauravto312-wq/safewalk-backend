package com.safewalk.service;

import com.safewalk.dto.request.FcmTokenRequest;
import com.safewalk.entity.AccountStatus;
import com.safewalk.entity.Role;
import com.safewalk.entity.User;
import com.safewalk.entity.UserFcmToken;
import com.safewalk.exception.ResourceNotFoundException;
import com.safewalk.repository.UserFcmTokenRepository;
import com.safewalk.repository.UserRepository;
import com.safewalk.service.impl.FcmServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
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
class FcmServiceImplTest {

    @Mock
    private UserFcmTokenRepository userFcmTokenRepository;

    @Mock
    private UserRepository userRepository;

    private FcmServiceImpl fcmService;

    private User testUser;

    @BeforeEach
    void setUp() {
        fcmService = new FcmServiceImpl(userFcmTokenRepository, userRepository);

        testUser = User.builder()
                .id(1L)
                .name("Aman Sharma")
                .mobileNumber("+919876543210")
                .role(Role.USER)
                .accountStatus(AccountStatus.ACTIVE)
                .build();
    }

    @Test
    void registerDeviceToken_NewToken_Success() {
        when(userRepository.findByMobileNumber("+919876543210")).thenReturn(Optional.of(testUser));
        when(userFcmTokenRepository.findByToken("mock.fcm.token")).thenReturn(Optional.empty());

        FcmTokenRequest request = new FcmTokenRequest("mock.fcm.token", "ANDROID");
        fcmService.registerDeviceToken("+919876543210", request);

        ArgumentCaptor<UserFcmToken> captor = ArgumentCaptor.forClass(UserFcmToken.class);
        verify(userFcmTokenRepository).save(captor.capture());

        UserFcmToken savedToken = captor.getValue();
        assertNotNull(savedToken);
        assertEquals("mock.fcm.token", savedToken.getToken());
        assertEquals("ANDROID", savedToken.getPlatform());
        assertTrue(savedToken.isActive());
        assertEquals(1L, savedToken.getUser().getId());
    }

    @Test
    void registerDeviceToken_ExistingToken_UpdatesUserAndActivates() {
        UserFcmToken existingToken = UserFcmToken.builder()
                .id(10L)
                .user(testUser)
                .token("mock.fcm.token")
                .active(false)
                .build();

        when(userRepository.findByMobileNumber("+919876543210")).thenReturn(Optional.of(testUser));
        when(userFcmTokenRepository.findByToken("mock.fcm.token")).thenReturn(Optional.of(existingToken));

        FcmTokenRequest request = new FcmTokenRequest("mock.fcm.token", "ANDROID");
        fcmService.registerDeviceToken("+919876543210", request);

        verify(userFcmTokenRepository).save(existingToken);
        assertTrue(existingToken.isActive());
    }

    @Test
    void registerDeviceToken_UserNotFound_ThrowsException() {
        when(userRepository.findByMobileNumber("+919876543210")).thenReturn(Optional.empty());

        FcmTokenRequest request = new FcmTokenRequest("mock.fcm.token", "ANDROID");
        assertThrows(ResourceNotFoundException.class, () ->
                fcmService.registerDeviceToken("+919876543210", request));
    }

    @Test
    void removeDeviceToken_Success() {
        UserFcmToken existingToken = UserFcmToken.builder()
                .id(10L)
                .user(testUser)
                .token("mock.fcm.token")
                .active(true)
                .build();

        when(userRepository.findByMobileNumber("+919876543210")).thenReturn(Optional.of(testUser));
        when(userFcmTokenRepository.findByTokenAndUserId("mock.fcm.token", 1L)).thenReturn(Optional.of(existingToken));

        fcmService.removeDeviceToken("+919876543210", "mock.fcm.token");

        verify(userFcmTokenRepository).save(existingToken);
        assertFalse(existingToken.isActive());
    }

    @Test
    void sendNotification_EmptyToken_ReturnsFalse() {
        boolean result = fcmService.sendNotification("", "Title", "Body", null);
        assertFalse(result);
        verify(userFcmTokenRepository, never()).save(any());
    }

    @Test
    void sendPushNotificationToUser_NoTokens_ReturnsZero() {
        when(userFcmTokenRepository.findByUserIdAndActiveTrue(1L)).thenReturn(Collections.emptyList());

        int count = fcmService.sendPushNotificationToUser(1L, "Title", "Body", null);
        assertEquals(0, count);
    }
}
