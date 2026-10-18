package com.safewalk.service.impl;

import com.safewalk.dto.request.LocationUpdateRequest;
import com.safewalk.dto.response.NearbyUserResponse;
import com.safewalk.entity.AccountStatus;
import com.safewalk.entity.Emergency;
import com.safewalk.entity.EmergencyStatus;
import com.safewalk.entity.User;
import com.safewalk.exception.ResourceNotFoundException;
import com.safewalk.repository.EmergencyRepository;
import com.safewalk.repository.UserRepository;
import com.safewalk.service.LocationService;
import com.safewalk.service.MapsService;
import com.safewalk.service.WebSocketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class LocationServiceImpl implements LocationService {

    private static final Logger log = LoggerFactory.getLogger(LocationServiceImpl.class);
    private static final String LOCATION_KEY = "safewalk:users:locations";
    private static final String TIMESTAMP_KEY_PREFIX = "safewalk:users:location_timestamp:";

    private final StringRedisTemplate stringRedisTemplate;
    private final UserRepository userRepository;
    private final MapsService mapsService;
    private final EmergencyRepository emergencyRepository;
    private final WebSocketService webSocketService;
    private final double defaultRadiusKm;
    private final long freshnessSeconds;

    public LocationServiceImpl(
            StringRedisTemplate stringRedisTemplate,
            UserRepository userRepository,
            MapsService mapsService,
            double defaultRadiusKm) {
        this(stringRedisTemplate, userRepository, mapsService, null, null, defaultRadiusKm, 300L);
    }

    public LocationServiceImpl(
            StringRedisTemplate stringRedisTemplate,
            UserRepository userRepository,
            MapsService mapsService,
            double defaultRadiusKm,
            long freshnessSeconds) {
        this(stringRedisTemplate, userRepository, mapsService, null, null, defaultRadiusKm, freshnessSeconds);
    }

    @Autowired
    public LocationServiceImpl(
            StringRedisTemplate stringRedisTemplate,
            UserRepository userRepository,
            MapsService mapsService,
            @Autowired(required = false) EmergencyRepository emergencyRepository,
            @Autowired(required = false) WebSocketService webSocketService,
            @Value("${safewalk.location.radius-km:2.0}") double defaultRadiusKm,
            @Value("${safewalk.location.freshness-seconds:300}") long freshnessSeconds) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.userRepository = userRepository;
        this.mapsService = mapsService;
        this.emergencyRepository = emergencyRepository;
        this.webSocketService = webSocketService;
        this.defaultRadiusKm = defaultRadiusKm;
        this.freshnessSeconds = freshnessSeconds;
    }

    @Override
    public void updateUserLocation(String mobileNumber, LocationUpdateRequest request) {
        log.info("Updating location for user with mobile number: {}", mobileNumber);

        if (request.getLatitude() < -90.0 || request.getLatitude() > 90.0 ||
                request.getLongitude() < -180.0 || request.getLongitude() > 180.0) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90 and longitude between -180 and 180");
        }

        User user = userRepository.findByMobileNumber(mobileNumber)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with mobile number: " + mobileNumber));

        Point point = new Point(request.getLongitude(), request.getLatitude());
        stringRedisTemplate.opsForGeo().add(LOCATION_KEY, point, user.getId().toString());

        long ts = (request.getTimestamp() != null && request.getTimestamp() > 0)
                ? request.getTimestamp()
                : System.currentTimeMillis();
        try {
            if (stringRedisTemplate.opsForValue() != null) {
                stringRedisTemplate.opsForValue().set(TIMESTAMP_KEY_PREFIX + user.getId(), String.valueOf(ts));
            }
        } catch (Exception e) {
            log.warn("Could not save location timestamp to Redis for user ID {}: {}", user.getId(), e.getMessage());
        }

        log.info("Successfully updated Redis GEO location for user ID: {} at ({}, {}) with timestamp {}",
                user.getId(), request.getLatitude(), request.getLongitude(), ts);

        broadcastParticipantLocationIfActive(user, request.getLatitude(), request.getLongitude());
    }

    @Override
    public void removeUserLocation(String mobileNumber) {
        log.info("Removing location from Redis GEO for user with mobile number: {}", mobileNumber);

        User user = userRepository.findByMobileNumber(mobileNumber)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with mobile number: " + mobileNumber));

        stringRedisTemplate.opsForGeo().remove(LOCATION_KEY, user.getId().toString());
        try {
            stringRedisTemplate.delete(TIMESTAMP_KEY_PREFIX + user.getId());
        } catch (Exception e) {
            log.warn("Could not remove location timestamp from Redis for user ID {}: {}", user.getId(), e.getMessage());
        }

        log.info("Successfully removed Redis GEO location for user ID: {}", user.getId());
    }

    @Override
    public Point getUserCoordinates(Long userId) {
        if (userId == null) {
            return null;
        }
        try {
            List<Point> positions = stringRedisTemplate.opsForGeo().position(LOCATION_KEY, userId.toString());
            if (positions != null && !positions.isEmpty()) {
                return positions.get(0);
            }
        } catch (Exception e) {
            log.warn("Error fetching coordinates from Redis GEO for user ID {}: {}", userId, e.getMessage());
        }
        return null;
    }

    @Override
    public List<NearbyUserResponse> findNearbyResponders(Double latitude, Double longitude, Double radiusKm) {
        if (latitude == null || latitude < -90.0 || latitude > 90.0) {
            throw new IllegalArgumentException("Latitude must be between -90.0 and 90.0");
        }
        if (longitude == null || longitude < -180.0 || longitude > 180.0) {
            throw new IllegalArgumentException("Longitude must be between -180.0 and 180.0");
        }

        if (radiusKm != null && radiusKm <= 0) {
            throw new IllegalArgumentException("Radius must be greater than 0");
        }

        double searchRadius = (radiusKm != null && radiusKm > 0) ? radiusKm : defaultRadiusKm;

        log.info("Searching nearby responders around ({}, {}) within {} km", latitude, longitude, searchRadius);

        GeoReference<String> reference = GeoReference.fromCoordinate(longitude, latitude);
        Distance distance = new Distance(searchRadius, Metrics.KILOMETERS);
        RedisGeoCommands.GeoSearchCommandArgs args = RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs()
                .includeDistance()
                .includeCoordinates()
                .sortAscending();

        GeoResults<RedisGeoCommands.GeoLocation<String>> results = stringRedisTemplate.opsForGeo()
                .search(LOCATION_KEY, reference, distance, args);

        if (results == null || results.getContent().isEmpty()) {
            log.info("No nearby users found within {} km", searchRadius);
            return Collections.emptyList();
        }

        List<NearbyUserResponse> responders = new ArrayList<>();

        for (GeoResult<RedisGeoCommands.GeoLocation<String>> result : results) {
            RedisGeoCommands.GeoLocation<String> geoLocation = result.getContent();
            String memberUserId = geoLocation.getName();

            try {
                Long userId = Long.parseLong(memberUserId);
                Optional<User> userOpt = userRepository.findById(userId);

                if (userOpt.isPresent()) {
                    User user = userOpt.get();

                    // Filter out non-active users (DEACTIVATED or SUSPENDED)
                    if (user.getAccountStatus() == AccountStatus.ACTIVE) {
                        Point pt = geoLocation.getPoint();
                        Double resLat = pt != null ? pt.getY() : null;
                        Double resLng = pt != null ? pt.getX() : null;
                        double distVal = result.getDistance() != null ? result.getDistance().getValue() : 0.0;
                        double roundedDistance = Math.round(distVal * 100.0) / 100.0;

                        boolean eligible = false;
                        if (user.getRole() == com.safewalk.entity.Role.POLICE) {
                            // Verified Police: eligible from 0m to searchRadius (50m lower bound exclusion does NOT apply)
                            // Unverified Police: strictly excluded
                            boolean isVerified = user.getVerificationStatus() == com.safewalk.entity.VerificationStatus.VERIFIED ||
                                    (user.getVerificationStatus() == null && user.isPoliceVerified());
                            if (isVerified && distVal <= searchRadius) {
                                eligible = true;
                            } else {
                                log.debug("Skipping police user ID: {} - verified: {}, distance: {} km",
                                        userId, isVerified, distVal);
                            }
                        } else if (user.getRole() == com.safewalk.entity.Role.HOSPITAL) {
                            // Verified Hospital: eligible from 0m to searchRadius
                            // Unverified Hospital: strictly excluded
                            boolean isVerified = user.getVerificationStatus() == com.safewalk.entity.VerificationStatus.VERIFIED;
                            if (isVerified && distVal <= searchRadius) {
                                eligible = true;
                            } else {
                                log.debug("Skipping hospital user ID: {} - verified: {}, distance: {} km",
                                        userId, isVerified, distVal);
                            }
                        } else if (user.getRole() == com.safewalk.entity.Role.USER) {
                            // Normal Citizen bystanders: must be strictly > 50m (0.05 km) AND <= searchRadius (2.0 km)
                            if (distVal > 0.05 && distVal <= searchRadius) {
                                eligible = true;
                            } else {
                                log.debug("Skipping user ID: {} with role {} - distance {} km is outside (> 0.05 km and <= {} km)",
                                        userId, user.getRole(), distVal, searchRadius);
                            }
                        } else {
                            log.debug("Skipping user ID: {} with non-responder role {}", userId, user.getRole());
                        }

                        if (eligible) {
                            responders.add(NearbyUserResponse.builder()
                                    .userId(user.getId())
                                    .name(user.getName())
                                    .mobileNumber(user.getMobileNumber())
                                    .latitude(resLat)
                                    .longitude(resLng)
                                    .distanceKm(roundedDistance)
                                    .role(user.getRole())
                                    .accountStatus(user.getAccountStatus())
                                    .policeVerified(user.isPoliceVerified())
                                    .build());
                        }
                    } else {
                        log.debug("Skipping user ID: {} because account status is: {}", userId, user.getAccountStatus());
                    }
                }
            } catch (NumberFormatException e) {
                log.warn("Failed to parse user ID member from Redis GEO result: {}", memberUserId);
            }
        }

        log.info("Found {} active nearby responders within {} km", responders.size(), searchRadius);
        return responders;
    }

    @Override
    public com.safewalk.dto.response.GeocodingResponse reverseGeocode(Double latitude, Double longitude) {
        return mapsService.reverseGeocode(latitude, longitude);
    }

    @Override
    public com.safewalk.dto.response.RouteDistanceResponse calculateRouteDistance(
            Double originLat, Double originLng, Double destLat, Double destLng) {
        return mapsService.calculateDistanceAndEta(originLat, originLng, destLat, destLng);
    }

    private void broadcastParticipantLocationIfActive(User user, Double latitude, Double longitude) {
        if (webSocketService == null || emergencyRepository == null) {
            return;
        }

        try {
            List<Emergency> activeEmergencies = emergencyRepository.findByParticipantUserIdAndStatusIn(
                    user.getId(), Arrays.asList(EmergencyStatus.ACTIVE, EmergencyStatus.ACCEPTED));

            for (Emergency emergency : activeEmergencies) {
                boolean isRequester = emergency.getRequester().getId().equals(user.getId());

                Map<String, Object> locationPayload = new HashMap<>();
                locationPayload.put("emergencyId", emergency.getId());
                locationPayload.put("userId", user.getId());
                locationPayload.put("name", user.getName());
                locationPayload.put("mobileNumber", user.getMobileNumber());
                locationPayload.put("isRequester", isRequester);
                locationPayload.put("latitude", latitude);
                locationPayload.put("longitude", longitude);
                locationPayload.put("timestamp", LocalDateTime.now().toString());

                com.safewalk.dto.event.RealTimeEvent event = com.safewalk.dto.event.RealTimeEvent.builder()
                        .eventType("LOCATION_UPDATE")
                        .message("Live location update for " + (isRequester ? "requester" : "responder") + " " + user.getName())
                        .data(locationPayload)
                        .build();

                // 1. Broadcast to emergency-specific location channel
                webSocketService.broadcastEvent("/topic/sos/" + emergency.getId() + "/location", event);

                // 2. Broadcast to emergency-specific general channel
                webSocketService.broadcastEvent("/topic/sos/" + emergency.getId(), event);

                // 3. Send targeted updates to participants' private queues
                if (!isRequester) {
                    webSocketService.sendToUser(emergency.getRequester().getMobileNumber(), "/queue/messages", event);
                } else {
                    if (emergency.getResponders() != null) {
                        for (User responder : emergency.getResponders()) {
                            webSocketService.sendToUser(responder.getMobileNumber(), "/queue/messages", event);
                        }
                    }
                    if (emergency.getResponder() != null) {
                        webSocketService.sendToUser(emergency.getResponder().getMobileNumber(), "/queue/messages", event);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to broadcast real-time location update for user ID {}: {}", user.getId(), e.getMessage());
        }
    }

    @Override
    public Long getUserLocationTimestamp(Long userId) {
        if (userId == null) {
            return null;
        }
        try {
            if (stringRedisTemplate.opsForValue() != null) {
                String val = stringRedisTemplate.opsForValue().get(TIMESTAMP_KEY_PREFIX + userId);
                if (val != null) {
                    return Long.parseLong(val.trim());
                }
            }
        } catch (Exception e) {
            log.warn("Error reading location timestamp from Redis for user ID {}: {}", userId, e.getMessage());
        }
        return null;
    }

    @Override
    public boolean isLocationFresh(Long userId) {
        Long ts = getUserLocationTimestamp(userId);
        if (ts == null) {
            return false;
        }
        long elapsedSeconds = (System.currentTimeMillis() - ts) / 1000L;
        return elapsedSeconds >= 0 && elapsedSeconds <= freshnessSeconds;
    }

    @Override
    public com.safewalk.dto.response.UserLocationResponse getUserCurrentLocation(String mobileNumber) {
        User user = userRepository.findByMobileNumber(mobileNumber)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with mobile number: " + mobileNumber));

        Point pt = getUserCoordinates(user.getId());
        Long ts = getUserLocationTimestamp(user.getId());
        boolean fresh = isLocationFresh(user.getId());

        return com.safewalk.dto.response.UserLocationResponse.builder()
                .userId(user.getId())
                .latitude(pt != null ? pt.getY() : null)
                .longitude(pt != null ? pt.getX() : null)
                .timestamp(ts)
                .fresh(fresh)
                .build();
    }
}
