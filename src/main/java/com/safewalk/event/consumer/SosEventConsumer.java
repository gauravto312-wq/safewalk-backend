package com.safewalk.event.consumer;

import com.safewalk.dto.response.NearbyUserResponse;
import com.safewalk.entity.AccountStatus;
import com.safewalk.entity.EmergencyContact;
import com.safewalk.entity.User;
import com.safewalk.event.SosCreatedEvent;
import com.safewalk.repository.EmergencyContactRepository;
import com.safewalk.service.FcmService;
import com.safewalk.service.LocationService;
import com.safewalk.service.WebSocketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class SosEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(SosEventConsumer.class);

    private final WebSocketService webSocketService;
    private final LocationService locationService;
    private final FcmService fcmService;
    private final EmergencyContactRepository emergencyContactRepository;
    private final com.safewalk.repository.EmergencyDispatchRepository emergencyDispatchRepository;
    private final com.safewalk.repository.EmergencyRepository emergencyRepository;
    private final com.safewalk.repository.UserRepository userRepository;

    public SosEventConsumer(WebSocketService webSocketService) {
        this(webSocketService, null, null, null, null, null, null);
    }

    public SosEventConsumer(WebSocketService webSocketService, LocationService locationService, FcmService fcmService) {
        this(webSocketService, locationService, fcmService, null, null, null, null);
    }

    public SosEventConsumer(
            WebSocketService webSocketService,
            LocationService locationService,
            FcmService fcmService,
            EmergencyContactRepository emergencyContactRepository) {
        this(webSocketService, locationService, fcmService, emergencyContactRepository, null, null, null);
    }

    @Autowired
    public SosEventConsumer(
            @Autowired(required = false) WebSocketService webSocketService,
            @Autowired(required = false) LocationService locationService,
            @Autowired(required = false) FcmService fcmService,
            @Autowired(required = false) EmergencyContactRepository emergencyContactRepository,
            @Autowired(required = false) com.safewalk.repository.EmergencyDispatchRepository emergencyDispatchRepository,
            @Autowired(required = false) com.safewalk.repository.EmergencyRepository emergencyRepository,
            @Autowired(required = false) com.safewalk.repository.UserRepository userRepository) {
        this.webSocketService = webSocketService;
        this.locationService = locationService;
        this.fcmService = fcmService;
        this.emergencyContactRepository = emergencyContactRepository;
        this.emergencyDispatchRepository = emergencyDispatchRepository;
        this.emergencyRepository = emergencyRepository;
        this.userRepository = userRepository;
    }

    @KafkaListener(
            topics = "${safewalk.kafka.topics.sos-events:sos.events}",
            groupId = "${spring.kafka.consumer.group-id:sos-processing-group}"
    )
    @Transactional
    public void consumeSosCreatedEvent(SosCreatedEvent event) {
        if (event == null) {
            log.warn("Received null SOS event payload from Kafka topic");
            return;
        }

        log.info("Kafka Consumer received SOS event [eventType={}, eventId={}, emergencyId={}, requesterId={}, mobile={}, location=({}, {})]",
                event.getEventType(),
                event.getEventId(),
                event.getEmergencyId(),
                event.getRequesterId(),
                event.getRequesterMobile(),
                event.getLatitude(),
                event.getLongitude());

        Set<Long> notifiedUserIds = new HashSet<>();
        Long requesterId = event.getRequesterId();

        // Calculate remaining TTL against 5-hour emergency lifetime
        long remainingTtlMillis = 5 * 3600 * 1000L;
        com.safewalk.entity.Emergency currentEmergency = null;
        if (emergencyRepository != null && event.getEmergencyId() != null) {
            try {
                currentEmergency = emergencyRepository.findById(event.getEmergencyId()).orElse(null);
                if (currentEmergency != null && currentEmergency.getStatus() != com.safewalk.entity.EmergencyStatus.ACTIVE) {
                    log.info("Emergency ID {} is not ACTIVE (status: {}); skipping SOS event notification dispatch",
                            event.getEmergencyId(), currentEmergency.getStatus());
                    return;
                }
                if (currentEmergency != null && currentEmergency.getCreatedAt() != null) {
                    long calculated = java.time.Duration.between(java.time.LocalDateTime.now(), currentEmergency.getCreatedAt().plusHours(5)).toMillis();
                    if (calculated > 0) {
                        remainingTtlMillis = calculated;
                    }
                }
            } catch (Exception ignored) {}
        }

        // 1. Notify Requester's Emergency / Trusted Contacts (Notified REGARDLESS of distance)
        if (emergencyContactRepository != null && requesterId != null) {
            try {
                List<EmergencyContact> contacts = null;
                try {
                    contacts = emergencyContactRepository.findByUserIdWithContactUser(requesterId);
                } catch (Exception e) {
                    log.debug("findByUserIdWithContactUser exception, falling back: {}", e.getMessage());
                }
                if (contacts == null || contacts.isEmpty()) {
                    try {
                        List<EmergencyContact> fallback = emergencyContactRepository.findByUserId(requesterId);
                        if (fallback != null && !fallback.isEmpty()) {
                            contacts = fallback;
                        }
                    } catch (Exception ignored) {}
                }
                if (contacts == null) {
                    contacts = java.util.Collections.emptyList();
                }
                log.info("Found {} emergency contacts for requester ID: {}", contacts.size(), requesterId);

                for (EmergencyContact contact : contacts) {
                    User contactUser = contact.getContactUser();
                    if (contactUser == null || contactUser.getId() == null) {
                        continue;
                    }
                    Long contactUserId = contactUser.getId();

                    // Never notify requester themselves
                    if (contactUserId.equals(requesterId)) {
                        continue;
                    }

                    // Only notify active accounts
                    if (contactUser.getAccountStatus() != AccountStatus.ACTIVE) {
                        log.debug("Skipping emergency contact ID: {} because status is: {}", contactUserId, contactUser.getAccountStatus());
                        continue;
                    }

                    if (fcmService != null) {
                        Map<String, String> data = new HashMap<>();
                        data.put("emergencyId", String.valueOf(event.getEmergencyId()));
                        data.put("type", "SOS_CREATED");
                        data.put("isEmergencyContact", "true");
                        data.put("requesterId", requesterId != null ? String.valueOf(requesterId) : "");
                        data.put("requesterName", event.getRequesterName() != null ? event.getRequesterName() : "Trusted Contact");
                        data.put("requesterMobile", event.getRequesterMobile() != null ? event.getRequesterMobile() : "");
                        data.put("latitude", String.valueOf(event.getLatitude()));
                        data.put("longitude", String.valueOf(event.getLongitude()));
                        data.put("distance", "Trusted Contact");
                        if (event.getRequesterAvatarUrl() != null && !event.getRequesterAvatarUrl().trim().isEmpty()) {
                            data.put("avatarUrl", event.getRequesterAvatarUrl());
                            data.put("requesterAvatarUrl", event.getRequesterAvatarUrl());
                        }

                        String contactName = (event.getRequesterName() != null && !event.getRequesterName().trim().isEmpty())
                                ? event.getRequesterName()
                                : (event.getRequesterMobile() != null ? event.getRequesterMobile() : "SafeWalk User");

                        fcmService.sendPushNotificationToUser(
                                contactUserId,
                                "🚨 EMERGENCY SOS - TRUSTED CONTACT ALERT!",
                                "Your trusted contact (" + contactName + ") has triggered an emergency SOS! Tap to assist immediately.",
                                data
                        );
                    }

                    notifiedUserIds.add(contactUserId);
                }
            } catch (Exception e) {
                log.warn("Error querying emergency contacts or sending notifications for event ID {}: {}",
                        event.getEventId(), e.getMessage());
            }
        }

        // 2. Perform Redis GEO discovery for eligible nearby responders within 2.0 KM
        // (Includes normal users > 50m and <= 2km, and verified police <= 2km; excludes unverified police and <= 50m normal users)
        if (locationService != null && event.getLatitude() != null && event.getLongitude() != null) {
            try {
                List<NearbyUserResponse> nearbyResponders = locationService.findNearbyResponders(
                        event.getLatitude(), event.getLongitude(), 2.0);

                log.info("Kafka Consumer discovered {} nearby eligible responders within 2 KM for emergency ID {}",
                        nearbyResponders.size(), event.getEmergencyId());

                if (fcmService != null && !nearbyResponders.isEmpty()) {
                    for (NearbyUserResponse responder : nearbyResponders) {
                        Long responderId = responder.getUserId();

                        // Exclude the requester themselves
                        if (requesterId != null && requesterId.equals(responderId)) {
                            continue;
                        }

                        // Deduplicate: If already notified as emergency contact, skip to prevent double notification
                        if (notifiedUserIds.contains(responderId)) {
                            log.debug("Responder ID: {} was already notified as an emergency contact; skipping duplicate FCM alert.", responderId);
                            continue;
                        }

                        // Persistent deduplication check
                        if (emergencyDispatchRepository != null && event.getEmergencyId() != null &&
                                emergencyDispatchRepository.existsByEmergencyIdAndResponderId(event.getEmergencyId(), responderId)) {
                            log.debug("Responder ID: {} already has dispatch record for emergency ID {}; skipping duplicate.", responderId, event.getEmergencyId());
                            continue;
                        }

                        // Atomic insert with PENDING status
                        com.safewalk.entity.EmergencyDispatch dispatch = null;
                        if (emergencyDispatchRepository != null && currentEmergency != null && userRepository != null) {
                            dispatch = com.safewalk.entity.EmergencyDispatch.builder()
                                    .emergency(currentEmergency)
                                    .responder(userRepository.getReferenceById(responderId))
                                    .status(com.safewalk.entity.DispatchStatus.PENDING)
                                    .distanceMeters(responder.getDistanceKm() != null ? responder.getDistanceKm() * 1000.0 : null)
                                    .build();
                            try {
                                dispatch = emergencyDispatchRepository.saveAndFlush(dispatch);
                            } catch (org.springframework.dao.DataIntegrityViolationException ex) {
                                continue;
                            }
                        }

                        Map<String, String> data = new HashMap<>();
                        data.put("emergencyId", String.valueOf(event.getEmergencyId()));
                        data.put("type", "SOS_CREATED");
                        data.put("isEmergencyContact", "false");
                        data.put("requesterId", requesterId != null ? String.valueOf(requesterId) : "");
                        data.put("requesterName", event.getRequesterName() != null ? event.getRequesterName() : "Someone in need");
                        data.put("requesterMobile", event.getRequesterMobile() != null ? event.getRequesterMobile() : "");
                        data.put("latitude", String.valueOf(event.getLatitude()));
                        data.put("longitude", String.valueOf(event.getLongitude()));
                        if (event.getRequesterAvatarUrl() != null && !event.getRequesterAvatarUrl().trim().isEmpty()) {
                            data.put("avatarUrl", event.getRequesterAvatarUrl());
                            data.put("requesterAvatarUrl", event.getRequesterAvatarUrl());
                        }

                        String distanceStr = "Nearby";
                        if (responder.getDistanceKm() != null) {
                            double distKm = responder.getDistanceKm();
                            if (distKm < 1.0) {
                                long meters = Math.round(distKm * 1000.0);
                                distanceStr = meters + " m";
                                data.put("distanceMeters", String.valueOf(meters));
                            } else {
                                distanceStr = String.format(java.util.Locale.US, "%.1f km", distKm);
                            }
                            data.put("distanceKm", String.valueOf(distKm));
                        }
                        data.put("distance", distanceStr);

                        String requesterDisplayName = (event.getRequesterName() != null && !event.getRequesterName().trim().isEmpty())
                                ? event.getRequesterName()
                                : "Someone in need";

                        String notifTitle = "🚨 Emergency Alert";
                        String notifBody = requesterDisplayName + " needs help! Approx. " + distanceStr + " away.";

                        boolean sent = false;
                        int sentCount = fcmService.sendPushNotificationToUser(
                                responderId,
                                notifTitle,
                                notifBody,
                                data
                        );
                        sent = sentCount > 0;

                        if (dispatch != null && emergencyDispatchRepository != null) {
                            dispatch.setStatus(sent ? com.safewalk.entity.DispatchStatus.SENT : com.safewalk.entity.DispatchStatus.FAILED);
                            dispatch.setSentAt(java.time.LocalDateTime.now());
                            emergencyDispatchRepository.save(dispatch);
                        }

                        notifiedUserIds.add(responderId);
                    }
                }
            } catch (Exception e) {
                log.warn("Error processing nearby responder discovery / FCM notification for event ID {}: {}",
                        event.getEventId(), e.getMessage());
            }
        }

        // 3. Broadcast real-time WebSocket event
        if (webSocketService != null) {
            com.safewalk.dto.event.RealTimeEvent realTimeEvent = com.safewalk.dto.event.RealTimeEvent.builder()
                    .eventType("SOS_CREATED")
                    .message("New emergency request created")
                    .data(event)
                    .build();
            webSocketService.broadcastEvent("/topic/emergencies", realTimeEvent);
        }
    }
}
