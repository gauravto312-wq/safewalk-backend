package com.safewalk.service.impl;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.safewalk.dto.request.FcmTokenRequest;
import com.safewalk.entity.User;
import com.safewalk.entity.UserFcmToken;
import com.safewalk.exception.ResourceNotFoundException;
import com.safewalk.repository.UserFcmTokenRepository;
import com.safewalk.repository.UserRepository;
import com.safewalk.service.FcmService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class FcmServiceImpl implements FcmService {

    private static final Logger log = LoggerFactory.getLogger(FcmServiceImpl.class);

    private final UserFcmTokenRepository userFcmTokenRepository;
    private final UserRepository userRepository;

    @Autowired
    public FcmServiceImpl(UserFcmTokenRepository userFcmTokenRepository, UserRepository userRepository) {
        this.userFcmTokenRepository = userFcmTokenRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public void registerDeviceToken(String currentUserMobile, FcmTokenRequest request) {
        log.info("Registering FCM device token for authenticated user");

        User user = userRepository.findByMobileNumber(currentUserMobile.trim())
                .orElseThrow(() -> new ResourceNotFoundException("User not found for mobile number: " + currentUserMobile));

        String tokenStr = request.getToken() != null ? request.getToken().trim() : "";
        if (tokenStr.isEmpty()) {
            log.warn("Cannot register empty FCM device token for user ID: {}", user.getId());
            return;
        }

        List<UserFcmToken> existingTokens = null;
        try {
            existingTokens = userFcmTokenRepository.findAllByToken(tokenStr);
        } catch (Exception ignored) {}

        if (existingTokens == null || existingTokens.isEmpty()) {
            try {
                java.util.Optional<UserFcmToken> opt = userFcmTokenRepository.findByToken(tokenStr);
                if (opt != null && opt.isPresent()) {
                    existingTokens = java.util.Collections.singletonList(opt.get());
                }
            } catch (Exception ignored) {}
        }
        if (existingTokens == null) {
            existingTokens = java.util.Collections.emptyList();
        }

        UserFcmToken fcmToken;
        if (!existingTokens.isEmpty()) {
            fcmToken = existingTokens.get(0);
            fcmToken.setUser(user);
            fcmToken.setActive(true);
            if (request.getPlatform() != null) {
                fcmToken.setPlatform(request.getPlatform());
            }
            if (existingTokens.size() > 1) {
                for (int i = 1; i < existingTokens.size(); i++) {
                    userFcmTokenRepository.delete(existingTokens.get(i));
                }
            }
            log.info("Reconciled and updated existing FCM token ID: {} for user ID: {}", fcmToken.getId(), user.getId());
        } else {
            fcmToken = UserFcmToken.builder()
                    .user(user)
                    .token(tokenStr)
                    .platform(request.getPlatform() != null ? request.getPlatform() : "ANDROID")
                    .active(true)
                    .build();
            log.info("Created new FCM device token for user ID: {}", user.getId());
        }

        try {
            userFcmTokenRepository.save(fcmToken);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // Concurrent race condition: another request inserted this token simultaneously.
            // Re-fetch the saved token and ensure ownership belongs to the current user.
            List<UserFcmToken> racedTokens = userFcmTokenRepository.findAllByToken(tokenStr);
            if (!racedTokens.isEmpty()) {
                UserFcmToken racedToken = racedTokens.get(0);
                racedToken.setUser(user);
                racedToken.setActive(true);
                if (request.getPlatform() != null) {
                    racedToken.setPlatform(request.getPlatform());
                }
                userFcmTokenRepository.save(racedToken);
                log.info("Reconciled raced FCM token ID: {} for user ID: {}", racedToken.getId(), user.getId());
            } else {
                throw e;
            }
        }
    }

    @Override
    @Transactional
    public void removeDeviceToken(String currentUserMobile, String token) {
        log.info("Deactivating FCM device token for authenticated user");

        User user = userRepository.findByMobileNumber(currentUserMobile.trim())
                .orElseThrow(() -> new ResourceNotFoundException("User not found for mobile number: " + currentUserMobile));

        if (token == null || token.trim().isEmpty()) {
            return;
        }

        Optional<UserFcmToken> tokenOpt = userFcmTokenRepository.findByTokenAndUserId(token.trim(), user.getId());
        if (tokenOpt.isPresent()) {
            UserFcmToken fcmToken = tokenOpt.get();
            fcmToken.setActive(false);
            userFcmTokenRepository.save(fcmToken);
            log.info("Successfully deactivated FCM token ID: {} for user ID: {}", fcmToken.getId(), user.getId());
        } else {
            log.warn("FCM token not found for user ID: {} during deactivation request", user.getId());
        }
    }

    @Override
    @Transactional
    public boolean sendNotification(String token, String title, String body, Map<String, String> data) {
        return sendNotificationWithTtl(token, title, body, data, null, null);
    }

    @Override
    @Transactional
    public boolean sendNotificationWithTtl(String token, String title, String body, Map<String, String> data, Long ttlMillis, String collapseKey) {
        if (token == null || token.trim().isEmpty()) {
            log.warn("Cannot send FCM notification: device token is empty");
            return false;
        }

        if (FirebaseApp.getApps().isEmpty()) {
            log.info("[DEV MODE] Firebase App not initialized. Simulated push notification [Title: '{}', Body: '{}', TTL: {} ms, CollapseKey: '{}']",
                    title, body, ttlMillis, collapseKey);
            return true;
        }

        try {
            com.google.firebase.messaging.AndroidConfig.Builder androidConfigBuilder =
                    com.google.firebase.messaging.AndroidConfig.builder()
                            .setPriority(com.google.firebase.messaging.AndroidConfig.Priority.HIGH);

            if (ttlMillis != null && ttlMillis > 0) {
                androidConfigBuilder.setTtl(ttlMillis);
            }
            if (collapseKey != null && !collapseKey.trim().isEmpty()) {
                androidConfigBuilder.setCollapseKey(collapseKey.trim());
            }

            Message.Builder messageBuilder = Message.builder()
                    .setToken(token)
                    .setAndroidConfig(androidConfigBuilder.build());

            java.util.Map<String, String> dataPayload = new java.util.HashMap<>();
            if (data != null && !data.isEmpty()) {
                dataPayload.putAll(data);
            }
            if (title != null && !dataPayload.containsKey("title")) {
                dataPayload.put("title", title);
            }
            if (body != null && !dataPayload.containsKey("body")) {
                dataPayload.put("body", body);
            }
            messageBuilder.putAllData(dataPayload);

            String messageId = FirebaseMessaging.getInstance().send(messageBuilder.build());
            String maskedToken = token.length() > 10 ? token.substring(0, 6) + "..." + token.substring(token.length() - 4) : "***";
            log.info("Successfully sent FCM push notification [messageId={}, token={}, ttl={}ms]", messageId, maskedToken, ttlMillis);
            return true;
        } catch (FirebaseMessagingException e) {
            log.warn("FCM messaging exception delivering push notification: {}", e.getMessage());
            String errorCode = e.getMessagingErrorCode() != null ? e.getMessagingErrorCode().name() : "";
            if ("UNREGISTERED".equals(errorCode) || "INVALID_ARGUMENT".equals(errorCode)) {
                userFcmTokenRepository.findByToken(token).ifPresent(t -> {
                    t.setActive(false);
                    userFcmTokenRepository.save(t);
                    log.info("Deactivated invalid/unregistered FCM token in database");
                });
            }
            return false;
        } catch (Exception e) {
            log.error("Unexpected error delivering FCM push notification: {}", e.getMessage());
            return false;
        }
    }

    @Override
    @Transactional
    public int sendPushNotificationToUser(Long userId, String title, String body, Map<String, String> data) {
        Long ttlMillis = null;
        String collapseKey = null;
        if (data != null && data.containsKey("emergencyId")) {
            collapseKey = "safewalk_emergency_alert";
            if ("SOS_CREATED".equals(data.get("type"))) {
                ttlMillis = 5 * 3600 * 1000L;
            }
        }
        return sendPushNotificationToUserWithTtl(userId, title, body, data, ttlMillis, collapseKey);
    }

    @Override
    @Transactional
    public int sendPushNotificationToUserWithTtl(Long userId, String title, String body, Map<String, String> data, Long ttlMillis, String collapseKey) {
        List<UserFcmToken> activeTokens = userFcmTokenRepository.findByUserIdAndActiveTrue(userId);
        if (activeTokens.isEmpty()) {
            log.info("No active FCM tokens found for user ID: {}", userId);
            return 0;
        }

        int successCount = 0;
        for (UserFcmToken tokenEntity : activeTokens) {
            boolean sent = sendNotificationWithTtl(tokenEntity.getToken(), title, body, data, ttlMillis, collapseKey);
            if (sent) {
                successCount++;
            }
        }
        return successCount;
    }
}
