package com.safewalk.service;

import com.safewalk.dto.request.FcmTokenRequest;

import java.util.Map;

public interface FcmService {

    void registerDeviceToken(String currentUserMobile, FcmTokenRequest request);

    void removeDeviceToken(String currentUserMobile, String token);

    boolean sendNotification(String token, String title, String body, Map<String, String> data);

    boolean sendNotificationWithTtl(String token, String title, String body, Map<String, String> data, Long ttlMillis, String collapseKey);

    int sendPushNotificationToUser(Long userId, String title, String body, Map<String, String> data);

    int sendPushNotificationToUserWithTtl(Long userId, String title, String body, Map<String, String> data, Long ttlMillis, String collapseKey);
}
