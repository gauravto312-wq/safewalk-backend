package com.safewalk.service.impl;

import com.safewalk.dto.request.CancelEmergencyRequest;
import com.safewalk.dto.request.CreateEmergencyRequest;
import com.safewalk.dto.response.EmergencyLocationResponse;
import com.safewalk.dto.response.EmergencyResponse;
import com.safewalk.entity.Emergency;
import com.safewalk.entity.EmergencyStatus;
import com.safewalk.entity.Role;
import com.safewalk.entity.User;
import com.safewalk.exception.AccountStatusException;
import com.safewalk.exception.DuplicateResourceException;
import com.safewalk.exception.InvalidCredentialsException;
import com.safewalk.exception.ResourceNotFoundException;
import com.safewalk.mapper.EmergencyMapper;
import com.safewalk.repository.EmergencyContactRepository;
import com.safewalk.repository.EmergencyRepository;
import com.safewalk.repository.UserRepository;
import com.safewalk.service.EmergencyService;
import com.safewalk.service.FcmService;
import com.safewalk.service.LocationService;
import com.safewalk.service.MapsService;
import com.safewalk.service.WebSocketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.geo.Point;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class EmergencyServiceImpl implements EmergencyService {

    private static final Logger log = LoggerFactory.getLogger(EmergencyServiceImpl.class);

    private final EmergencyRepository emergencyRepository;
    private final UserRepository userRepository;
    private final EmergencyMapper emergencyMapper;
    private final PasswordEncoder passwordEncoder;
    private final com.safewalk.event.producer.SosEventProducer sosEventProducer;
    private final WebSocketService webSocketService;
    private final FcmService fcmService;
    private final MapsService mapsService;
    private final LocationService locationService;
    private final EmergencyContactRepository emergencyContactRepository;
    private final com.safewalk.repository.EmergencyDispatchRepository emergencyDispatchRepository;

    public EmergencyServiceImpl(
            EmergencyRepository emergencyRepository,
            UserRepository userRepository,
            EmergencyMapper emergencyMapper,
            PasswordEncoder passwordEncoder,
            com.safewalk.event.producer.SosEventProducer sosEventProducer) {
        this(emergencyRepository, userRepository, emergencyMapper, passwordEncoder, sosEventProducer, null, null, null, null, null, null);
    }

    public EmergencyServiceImpl(
            EmergencyRepository emergencyRepository,
            UserRepository userRepository,
            EmergencyMapper emergencyMapper,
            PasswordEncoder passwordEncoder,
            com.safewalk.event.producer.SosEventProducer sosEventProducer,
            WebSocketService webSocketService,
            FcmService fcmService,
            MapsService mapsService,
            LocationService locationService) {
        this(emergencyRepository, userRepository, emergencyMapper, passwordEncoder, sosEventProducer, webSocketService, fcmService, mapsService, locationService, null, null);
    }

    public EmergencyServiceImpl(
            EmergencyRepository emergencyRepository,
            UserRepository userRepository,
            EmergencyMapper emergencyMapper,
            PasswordEncoder passwordEncoder,
            com.safewalk.event.producer.SosEventProducer sosEventProducer,
            WebSocketService webSocketService,
            FcmService fcmService,
            MapsService mapsService,
            LocationService locationService,
            EmergencyContactRepository emergencyContactRepository) {
        this(emergencyRepository, userRepository, emergencyMapper, passwordEncoder, sosEventProducer, webSocketService, fcmService, mapsService, locationService, emergencyContactRepository, null);
    }

    @Autowired
    public EmergencyServiceImpl(
            EmergencyRepository emergencyRepository,
            UserRepository userRepository,
            EmergencyMapper emergencyMapper,
            PasswordEncoder passwordEncoder,
            com.safewalk.event.producer.SosEventProducer sosEventProducer,
            @Autowired(required = false) WebSocketService webSocketService,
            @Autowired(required = false) FcmService fcmService,
            @Autowired(required = false) MapsService mapsService,
            @Autowired(required = false) LocationService locationService,
            @Autowired(required = false) EmergencyContactRepository emergencyContactRepository,
            @Autowired(required = false) com.safewalk.repository.EmergencyDispatchRepository emergencyDispatchRepository) {
        this.emergencyRepository = emergencyRepository;
        this.userRepository = userRepository;
        this.emergencyMapper = emergencyMapper;
        this.passwordEncoder = passwordEncoder;
        this.sosEventProducer = sosEventProducer;
        this.webSocketService = webSocketService;
        this.fcmService = fcmService;
        this.mapsService = mapsService;
        this.locationService = locationService;
        this.emergencyContactRepository = emergencyContactRepository;
        this.emergencyDispatchRepository = emergencyDispatchRepository;
    }

    @Override
    @Transactional
    public EmergencyResponse createEmergency(CreateEmergencyRequest request, String currentUserMobile) {
        log.info("Processing emergency creation for user mobile: {}", currentUserMobile);

        User requester = userRepository.findByMobileNumber(currentUserMobile.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Requester user account not found for mobile: " + currentUserMobile));

        if (requester.getAccountStatus() == com.safewalk.entity.AccountStatus.RESTRICTED ||
                requester.getAccountStatus() == com.safewalk.entity.AccountStatus.SUSPENDED ||
                requester.getAccountStatus() == com.safewalk.entity.AccountStatus.DEACTIVATED) {
            log.warn("Emergency creation rejected. User account status is {}", requester.getAccountStatus());
            throw new AccountStatusException("Your account is " + requester.getAccountStatus().name() + ". SOS emergency creation is restricted.");
        }

        if (emergencyRepository.existsByRequesterIdAndStatusIn(requester.getId(), Arrays.asList(EmergencyStatus.ACTIVE, EmergencyStatus.ACCEPTED))) {
            log.warn("Emergency creation rejected. Active or accepted emergency already exists for user ID: {}", requester.getId());
            throw new DuplicateResourceException("You already have an active emergency request in progress. Resolve or cancel it before creating a new one.");
        }

        Emergency emergency = emergencyMapper.toEntity(request, requester);
        if ((emergency.getLatitude() == null || emergency.getLongitude() == null) && locationService != null) {
            org.springframework.data.geo.Point reqPoint = locationService.getUserCoordinates(requester.getId());
            if (reqPoint != null) {
                emergency.setLatitude(reqPoint.getY());
                emergency.setLongitude(reqPoint.getX());
            }
        }
        Emergency savedEmergency = emergencyRepository.save(emergency);

        log.info("Successfully created emergency request ID: {} for user ID: {} with type: {}", savedEmergency.getId(), requester.getId(), savedEmergency.getType());

        if (sosEventProducer != null) {
            com.safewalk.event.SosCreatedEvent event = com.safewalk.event.SosCreatedEvent.builder()
                    .emergencyId(savedEmergency.getId())
                    .requesterId(requester.getId())
                    .requesterName(requester.getName())
                    .requesterMobile(requester.getMobileNumber())
                    .requesterAvatarUrl(requester.getAvatarUrl())
                    .type(savedEmergency.getType() != null ? savedEmergency.getType().name() : "GENERAL")
                    .status(savedEmergency.getStatus() != null ? savedEmergency.getStatus().name() : "ACTIVE")
                    .latitude(savedEmergency.getLatitude())
                    .longitude(savedEmergency.getLongitude())
                    .build();
            sosEventProducer.publishSosCreatedEvent(event);
        }

        EmergencyResponse createdResponse = emergencyMapper.toResponse(savedEmergency);

        if (webSocketService != null) {
            try {
                com.safewalk.dto.event.RealTimeEvent wsEvent = com.safewalk.dto.event.RealTimeEvent.builder()
                        .eventType("SOS_CREATED")
                        .message("New emergency request created")
                        .data(createdResponse)
                        .build();
                webSocketService.broadcastEvent("/topic/emergencies", wsEvent);
                webSocketService.broadcastEvent("/topic/sos/" + savedEmergency.getId(), wsEvent);

                if (locationService != null && savedEmergency.getLatitude() != null && savedEmergency.getLongitude() != null) {
                    List<com.safewalk.dto.response.NearbyUserResponse> eligibleResponders =
                            locationService.findNearbyResponders(savedEmergency.getLatitude(), savedEmergency.getLongitude(), 2.0);
                    for (com.safewalk.dto.response.NearbyUserResponse resp : eligibleResponders) {
                        if (!resp.getUserId().equals(requester.getId()) && resp.getMobileNumber() != null) {
                            webSocketService.sendToUser(resp.getMobileNumber(), "/queue/messages", wsEvent);
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Notice: WebSocket broadcast for emergency ID {}: {}", savedEmergency.getId(), e.getMessage());
            }
        }

        return createdResponse;
    }

    @Override
    @Transactional(readOnly = true)
    public EmergencyResponse getEmergencyById(Long emergencyId, String currentUserMobile, boolean isAdmin) {
        log.debug("Fetching emergency request ID: {} for user: {}", emergencyId, currentUserMobile);

        Emergency emergency = emergencyRepository.findById(emergencyId)
                .orElseThrow(() -> new ResourceNotFoundException("Emergency request not found with ID: " + emergencyId));

        boolean isRequester = emergency.getRequester().getMobileNumber().equalsIgnoreCase(currentUserMobile.trim());
        boolean isResponder = (emergency.getResponder() != null && emergency.getResponder().getMobileNumber().equalsIgnoreCase(currentUserMobile.trim()))
                || (emergency.getResponders() != null && emergency.getResponders().stream().anyMatch(r -> r.getMobileNumber().equalsIgnoreCase(currentUserMobile.trim())));

        User caller = userRepository.findByMobileNumber(currentUserMobile.trim()).orElse(null);
        boolean isTrustedContact = caller != null && emergencyContactRepository != null &&
                emergencyContactRepository.existsByUserIdAndContactUserId(emergency.getRequester().getId(), caller.getId());

        if (!isAdmin && !isRequester && !isResponder && !isTrustedContact) {
            log.warn("Unauthorized access attempt to emergency ID: {} by user: {}", emergencyId, currentUserMobile);
            throw new AccessDeniedException("Access denied. You can only view your own emergency requests, requests you respond to, or requests where you are a trusted contact.");
        }

        EmergencyResponse response = emergencyMapper.toResponse(emergency);
        response.setTrustedContactViewer(isTrustedContact);

        if (mapsService != null && emergency.getLatitude() != null && emergency.getLongitude() != null) {
            try {
                response.setAddressDetails(mapsService.reverseGeocode(emergency.getLatitude(), emergency.getLongitude()));
            } catch (Exception e) {
                log.debug("Could not fetch address details for emergency ID {}: {}", emergencyId, e.getMessage());
            }
        }

        Point reqCoords = getRequesterCoordinates(emergency);
        Double reqLat = reqCoords != null ? reqCoords.getY() : emergency.getLatitude();
        Double reqLng = reqCoords != null ? reqCoords.getX() : emergency.getLongitude();

        if (locationService != null && reqLat != null && reqLng != null) {
            try {
                if (caller != null) {
                    Point callerPt = locationService.getUserCoordinates(caller.getId());
                    if (callerPt != null) {
                        double d = calculateHaversineMetersExact(reqLat, reqLng, callerPt.getY(), callerPt.getX());
                        response.setDistanceMeters(d);
                    }
                }
            } catch (Exception e) {
                log.debug("Could not calculate caller distance for emergency ID {}: {}", emergencyId, e.getMessage());
            }
        }

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public EmergencyLocationResponse getEmergencyLocations(Long emergencyId, String currentUserMobile, boolean isAdmin) {
        log.debug("Fetching mutual emergency locations for emergency ID: {}, user: {}", emergencyId, currentUserMobile);

        Emergency emergency = emergencyRepository.findById(emergencyId)
                .orElseThrow(() -> new ResourceNotFoundException("Emergency request not found with ID: " + emergencyId));

        boolean isRequester = emergency.getRequester().getMobileNumber().equalsIgnoreCase(currentUserMobile.trim());
        boolean isResponder = (emergency.getResponder() != null && emergency.getResponder().getMobileNumber().equalsIgnoreCase(currentUserMobile.trim()))
                || (emergency.getResponders() != null && emergency.getResponders().stream().anyMatch(r -> r.getMobileNumber().equalsIgnoreCase(currentUserMobile.trim())));

        User caller = userRepository.findByMobileNumber(currentUserMobile.trim()).orElse(null);
        boolean isTrustedContact = caller != null && emergencyContactRepository != null &&
                emergencyContactRepository.existsByUserIdAndContactUserId(emergency.getRequester().getId(), caller.getId());

        if (!isAdmin && !isRequester && !isResponder && !isTrustedContact) {
            log.warn("Unauthorized location access attempt to emergency ID: {} by user: {}", emergencyId, currentUserMobile);
            throw new AccessDeniedException("Access denied. You are not an active participant or trusted contact in this emergency.");
        }

        if (emergency.getStatus() == EmergencyStatus.RESOLVED || emergency.getStatus() == EmergencyStatus.CANCELLED || emergency.getStatus() == EmergencyStatus.EXPIRED) {
            log.warn("Emergency ID: {} is {}, live location sharing has ended.", emergencyId, emergency.getStatus());
            throw new IllegalStateException("Live location sharing has ended because the emergency is " + emergency.getStatus().name() + ".");
        }

        // Get requester live location
        Point reqCoords = getRequesterCoordinates(emergency);
        Double reqLat = reqCoords != null ? reqCoords.getY() : emergency.getLatitude();
        Double reqLng = reqCoords != null ? reqCoords.getX() : emergency.getLongitude();

        EmergencyLocationResponse.ParticipantLocation requesterLoc = EmergencyLocationResponse.ParticipantLocation.builder()
                .userId(emergency.getRequester().getId())
                .name(emergency.getRequester().getName())
                .mobileNumber(emergency.getRequester().getMobileNumber())
                .latitude(reqLat)
                .longitude(reqLng)
                .distanceKm(0.0)
                .updatedAt(LocalDateTime.now())
                .build();

        // Get all accepted responders live locations
        Set<User> allResponders = new LinkedHashSet<>();
        if (emergency.getResponders() != null) {
            allResponders.addAll(emergency.getResponders());
        }
        if (emergency.getResponder() != null) {
            allResponders.add(emergency.getResponder());
        }

        List<EmergencyLocationResponse.ParticipantLocation> responderLocations = new ArrayList<>();
        for (User r : allResponders) {
            Double respLat = null;
            Double respLng = null;
            Double distance = null;

            if (locationService != null) {
                org.springframework.data.geo.Point respPt = locationService.getUserCoordinates(r.getId());
                if (respPt != null) {
                    respLat = respPt.getY();
                    respLng = respPt.getX();
                }
            }

            if (reqLat != null && reqLng != null && respLat != null && respLng != null) {
                distance = calculateHaversineKm(reqLat, reqLng, respLat, respLng);
            }

            responderLocations.add(EmergencyLocationResponse.ParticipantLocation.builder()
                    .userId(r.getId())
                    .name(r.getName())
                    .mobileNumber(r.getMobileNumber())
                    .latitude(respLat)
                    .longitude(respLng)
                    .distanceKm(distance)
                    .updatedAt(LocalDateTime.now())
                    .build());
        }

        return EmergencyLocationResponse.builder()
                .emergencyId(emergency.getId())
                .status(emergency.getStatus())
                .requester(requesterLoc)
                .responders(responderLocations)
                .build();
    }

    /**
     * Resolves the latest valid coordinates for an emergency's requester.
     * Prefers fresh live location from Redis GEO (<= 300s).
     * Safely falls back to original creation coordinates if live location is missing or stale.
     * Note: org.springframework.data.geo.Point convention is X = Longitude, Y = Latitude.
     */
    private Point getRequesterCoordinates(Emergency emergency) {
        if (emergency == null) {
            return null;
        }
        if (locationService != null && emergency.getRequester() != null && emergency.getRequester().getId() != null) {
            Long requesterId = emergency.getRequester().getId();
            try {
                Point livePt = locationService.getUserCoordinates(requesterId);
                if (livePt != null) {
                    Long ts = locationService.getUserLocationTimestamp(requesterId);
                    if (ts == null || locationService.isLocationFresh(requesterId)) {
                        return livePt;
                    } else {
                        log.debug("Requester ID {} coordinates in Redis are stale (> 300s); falling back to creation coordinates", requesterId);
                    }
                }
            } catch (Exception e) {
                log.debug("Error checking live coordinates for requester ID {}: {}", requesterId, e.getMessage());
            }
        }
        if (emergency.getLatitude() != null && emergency.getLongitude() != null) {
            return new Point(emergency.getLongitude(), emergency.getLatitude());
        }
        return null;
    }

    private double calculateHaversineKm(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Earth radius in km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c;
        return Math.round(distance * 100.0) / 100.0;
    }

    private double calculateHaversineMetersExact(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371000.0; // Earth radius in meters
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c;
        return Math.round(distance * 100.0) / 100.0;
    }

    @Override
    @Transactional(readOnly = true)
    public EmergencyResponse getMyActiveEmergency(String currentUserMobile) {
        log.debug("Fetching active emergency for user mobile: {}", currentUserMobile);

        User requester = userRepository.findByMobileNumber(currentUserMobile.trim())
                .orElseThrow(() -> new ResourceNotFoundException("User account not found for mobile: " + currentUserMobile));

        Emergency emergency = emergencyRepository.findFirstByRequesterIdAndStatusInOrderByCreatedAtDesc(
                requester.getId(), Arrays.asList(EmergencyStatus.ACTIVE, EmergencyStatus.ACCEPTED))
                .orElseThrow(() -> new ResourceNotFoundException("No active emergency request found for user."));

        return emergencyMapper.toResponse(emergency);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmergencyResponse> getActiveEmergencies(String currentUserMobile) {
        log.debug("Fetching active emergencies for user mobile: {}", currentUserMobile);

        User caller = userRepository.findByMobileNumber(currentUserMobile.trim())
                .orElseThrow(() -> new ResourceNotFoundException("User account not found for mobile: " + currentUserMobile));

        // Bug #1 Protection: Unverified Police / Hospital users must not receive or see dispatchable emergencies
        boolean isCallerVerifiedAuthority = caller.getVerificationStatus() == com.safewalk.entity.VerificationStatus.VERIFIED
                || (caller.getVerificationStatus() == null && caller.isPoliceVerified());
        if ((caller.getRole() == Role.POLICE || caller.getRole() == Role.HOSPITAL) && !isCallerVerifiedAuthority) {
            log.debug("User ID: {} is unverified authority ({}), returning empty active emergencies",
                    caller.getId(), caller.getVerificationStatus());
            return new ArrayList<>();
        }

        List<Emergency> emergencies = emergencyRepository.findByStatusInOrderByCreatedAtDesc(
                Arrays.asList(EmergencyStatus.ACTIVE, EmergencyStatus.ACCEPTED));

        Point callerPt = null;
        boolean isCallerLocationFresh = false;
        if (locationService != null) {
            try {
                callerPt = locationService.getUserCoordinates(caller.getId());
                isCallerLocationFresh = locationService.isLocationFresh(caller.getId());
            } catch (Exception e) {
                log.debug("Error checking caller location: {}", e.getMessage());
            }
        }

        List<EmergencyResponse> responses = new ArrayList<>();
        for (Emergency e : emergencies) {
            Point reqCoords = getRequesterCoordinates(e);
            Double reqLat = reqCoords != null ? reqCoords.getY() : e.getLatitude();
            Double reqLng = reqCoords != null ? reqCoords.getX() : e.getLongitude();

            Double distanceMeters = null;
            if (callerPt != null && reqLat != null && reqLng != null) {
                distanceMeters = calculateHaversineMetersExact(reqLat, reqLng, callerPt.getY(), callerPt.getX());
            }

            boolean isRequester = e.getRequester() != null && caller.getId().equals(e.getRequester().getId());
            boolean isAcceptedResponder = (e.getResponder() != null && caller.getId().equals(e.getResponder().getId()))
                    || (e.getResponders() != null && e.getResponders().stream().anyMatch(r -> caller.getId().equals(r.getId())));
            boolean isParticipant = isRequester || isAcceptedResponder;

            if (!isParticipant) {
                // Prospective candidate responders discovering emergencies must have a fresh location in Redis
                if (callerPt == null || !isCallerLocationFresh || distanceMeters == null) {
                    log.debug("Emergency ID: {} filtered out for prospective caller ID: {} (location missing, stale, or distance unknown)",
                            e.getId(), caller.getId());
                    continue;
                }

                boolean eligible;
                long roundedDistance = Math.round(distanceMeters);
                if (caller.getRole() == Role.POLICE || caller.getRole() == Role.HOSPITAL) {
                    eligible = (roundedDistance <= 2000);
                } else {
                    eligible = (roundedDistance > 50 && roundedDistance <= 2000);
                }
                if (!eligible) {
                    log.debug("Emergency ID: {} filtered out for prospective caller ID: {} (distance: {} meters)",
                            e.getId(), caller.getId(), distanceMeters);
                    continue;
                }
            }

            EmergencyResponse resp = emergencyMapper.toResponse(e);
            if (distanceMeters != null) {
                resp.setDistanceMeters(distanceMeters);
            }
            responses.add(resp);
        }
        return responses;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EmergencyResponse> getMyEmergencyHistory(String currentUserMobile, Pageable pageable) {
        log.debug("Fetching emergency history for user mobile: {}", currentUserMobile);

        User caller = userRepository.findByMobileNumber(currentUserMobile.trim())
                .orElseThrow(() -> new ResourceNotFoundException("User account not found for mobile: " + currentUserMobile));

        return emergencyRepository.findByParticipantUserIdOrderByCreatedAtDesc(caller.getId(), pageable)
                .map(emergencyMapper::toResponse);
    }

    @Override
    @Transactional
    public EmergencyResponse acceptEmergency(Long emergencyId, String currentUserMobile) {
        log.info("Processing emergency acceptance for ID: {} by responder mobile: {}", emergencyId, currentUserMobile);

        Emergency emergency = emergencyRepository.findByIdWithPessimisticLock(emergencyId)
                .or(() -> emergencyRepository.findById(emergencyId))
                .orElseThrow(() -> new ResourceNotFoundException("Emergency request not found with ID: " + emergencyId));

        if (emergency.getStatus() != EmergencyStatus.ACTIVE && emergency.getStatus() != EmergencyStatus.ACCEPTED) {
            log.warn("Acceptance failed. Emergency ID: {} status is {}", emergencyId, emergency.getStatus());
            throw new IllegalStateException("Cannot accept emergency request because it is currently " + emergency.getStatus().name());
        }

        User responder = userRepository.findByMobileNumber(currentUserMobile.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Responder user account not found for mobile: " + currentUserMobile));

        if (responder.getAccountStatus() == com.safewalk.entity.AccountStatus.RESTRICTED ||
                responder.getAccountStatus() == com.safewalk.entity.AccountStatus.SUSPENDED ||
                responder.getAccountStatus() == com.safewalk.entity.AccountStatus.DEACTIVATED) {
            log.warn("Acceptance failed. Responder account status is {}", responder.getAccountStatus());
            throw new AccountStatusException("Your account is " + responder.getAccountStatus().name() + ". Accepting emergency requests is restricted.");
        }

        boolean isResponderVerifiedAuthority = responder.getVerificationStatus() == com.safewalk.entity.VerificationStatus.VERIFIED
                || (responder.getVerificationStatus() == null && responder.isPoliceVerified());
        if ((responder.getRole() == Role.POLICE || responder.getRole() == Role.HOSPITAL) && !isResponderVerifiedAuthority) {
            log.warn("Acceptance failed. Responder ID: {} (role: {}) is not verified. Status: {}",
                    responder.getId(), responder.getRole(), responder.getVerificationStatus());
            throw new AccountStatusException("Account verification pending. Unverified authorities cannot accept emergency requests.");
        }

        if (emergency.getRequester().getId().equals(responder.getId())) {
            log.warn("Acceptance failed. Requester ID: {} tried to accept their own emergency", responder.getId());
            throw new IllegalArgumentException("You cannot accept your own emergency request.");
        }

        // Check if responder already accepted
        boolean alreadyAccepted = (emergency.getResponder() != null && emergency.getResponder().getId().equals(responder.getId()))
                || (emergency.getResponders() != null && emergency.getResponders().stream().anyMatch(r -> r.getId().equals(responder.getId())));

        if (alreadyAccepted) {
            log.warn("Acceptance duplicate. Responder ID: {} has already accepted emergency ID: {}", responder.getId(), emergencyId);
            throw new DuplicateResourceException("You have already accepted this emergency request.");
        }

        // Location rules validation
        Point reqCoords = getRequesterCoordinates(emergency);
        if (reqCoords == null) {
            log.warn("Acceptance failed. Emergency ID: {} location coordinates missing", emergencyId);
            throw new IllegalStateException("Emergency location coordinates are missing.");
        }
        Double reqLat = reqCoords.getY();
        Double reqLng = reqCoords.getX();

        if (locationService == null) {
            log.warn("Acceptance failed. Location service unavailable for responder ID: {}", responder.getId());
            throw new IllegalStateException("Live location coordinates not found for responder. Update your location first.");
        }

        Point responderCoords = locationService.getUserCoordinates(responder.getId());
        if (responderCoords == null) {
            log.warn("Acceptance failed. Responder ID: {} has no live coordinates in Redis", responder.getId());
            throw new IllegalStateException("Live location coordinates not found for responder. Update your location first.");
        }

        if (!locationService.isLocationFresh(responder.getId())) {
            log.warn("Acceptance failed. Responder ID: {} location is stale in Redis", responder.getId());
            throw new IllegalStateException("Responder location is stale. Please update your live location.");
        }

        double distanceMeters = calculateHaversineMetersExact(
                reqLat, reqLng,
                responderCoords.getY(), responderCoords.getX());
        log.info("Acceptance distance for responder ID: {} to emergency ID: {}: {} meters",
                responder.getId(), emergencyId, distanceMeters);

        long roundedDistance = Math.round(distanceMeters);
        if (responder.getRole() == Role.POLICE || responder.getRole() == Role.HOSPITAL) {
            // Verified authority: distance <= 2000m (NO 50m exclusion)
            if (roundedDistance > 2000) {
                log.warn("Acceptance rejected. Verified authority responder ID: {} is {} meters away (> 2000m)",
                        responder.getId(), distanceMeters);
                throw new IllegalStateException(String.format("Emergency is too far (%.1f meters). Authorities must be within 2 kilometers.", distanceMeters));
            }
        } else {
            // Normal citizen responder: distance > 50.0m AND <= 2000.0m
            if (roundedDistance <= 50) {
                log.warn("Acceptance rejected. Citizen responder ID: {} is {} meters away (<= 50m)",
                        responder.getId(), distanceMeters);
                throw new IllegalStateException(String.format("You are too close to this emergency (%.1f meters). Citizens must be beyond 50 meters.", distanceMeters));
            }
            if (roundedDistance > 2000) {
                log.warn("Acceptance rejected. Citizen responder ID: {} is {} meters away (> 2000m)",
                        responder.getId(), distanceMeters);
                throw new IllegalStateException(String.format("Emergency is too far (%.1f meters). Responders must be within 2 kilometers.", distanceMeters));
            }
        }

        emergency.addResponder(responder);
        if (emergency.getResponder() == null) {
            emergency.setResponder(responder);
        }
        if (emergency.getStatus() == EmergencyStatus.ACTIVE) {
            emergency.setStatus(EmergencyStatus.ACCEPTED);
        }
        if (emergency.getAcceptedAt() == null) {
            emergency.setAcceptedAt(LocalDateTime.now());
        }

        Emergency savedEmergency = emergencyRepository.save(emergency);

        log.info("Successfully accepted emergency ID: {} by responder ID: {}. Total responders: {}",
                emergencyId, responder.getId(), savedEmergency.getResponders().size());

        EmergencyResponse response = emergencyMapper.toResponse(savedEmergency);
        response.setDistanceMeters(distanceMeters);

        if (webSocketService != null) {
            com.safewalk.dto.event.RealTimeEvent event = com.safewalk.dto.event.RealTimeEvent.builder()
                    .eventType("SOS_ACCEPTED")
                    .message("Responder " + responder.getName() + " has accepted your emergency request.")
                    .data(response)
                    .build();
            webSocketService.broadcastEvent("/topic/sos/" + emergencyId, event);
            webSocketService.sendToUser(emergency.getRequester().getMobileNumber(), "/queue/messages", event);
        }

        if (fcmService != null) {
            Map<String, String> data = new HashMap<>();
            data.put("emergencyId", emergencyId.toString());
            data.put("type", "SOS_ACCEPTED");
            data.put("responderId", responder.getId().toString());
            fcmService.sendPushNotificationToUser(
                    emergency.getRequester().getId(),
                    "✅ Responder En Route!",
                    responder.getName() + " has accepted your SOS request and is on their way.",
                    data
            );
        }

        return response;
    }

    @Override
    @Transactional
    public EmergencyResponse arriveEmergency(Long emergencyId, String currentUserMobile) {
        log.info("Processing responder arrival for emergency ID: {} by responder mobile: {}", emergencyId, currentUserMobile);

        Emergency emergency = emergencyRepository.findByIdWithPessimisticLock(emergencyId)
                .or(() -> emergencyRepository.findById(emergencyId))
                .orElseThrow(() -> new ResourceNotFoundException("Emergency request not found with ID: " + emergencyId));

        if (emergency.getStatus() == EmergencyStatus.CANCELLED || emergency.getStatus() == EmergencyStatus.RESOLVED || emergency.getStatus() == EmergencyStatus.EXPIRED) {
            log.warn("Arrival failed. Emergency ID: {} status is {}", emergencyId, emergency.getStatus());
            throw new IllegalStateException("Cannot mark arrival for an emergency that is already " + emergency.getStatus().name());
        }

        User responder = userRepository.findByMobileNumber(currentUserMobile.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Responder user account not found for mobile: " + currentUserMobile));

        boolean isAccepted = (emergency.getResponder() != null && emergency.getResponder().getId().equals(responder.getId()))
                || (emergency.getResponders() != null && emergency.getResponders().stream().anyMatch(r -> r.getId().equals(responder.getId())));

        if (!isAccepted) {
            log.warn("Arrival unauthorized. User ID: {} is not an accepted responder for emergency ID: {}", responder.getId(), emergencyId);
            throw new AccessDeniedException("Access denied. Only accepted responders can mark arrival at the emergency scene.");
        }

        Point reqCoords = getRequesterCoordinates(emergency);
        if (reqCoords == null) {
            log.warn("Arrival failed. Emergency ID: {} location coordinates missing", emergencyId);
            throw new IllegalStateException("Emergency location coordinates are missing.");
        }
        Double reqLat = reqCoords.getY();
        Double reqLng = reqCoords.getX();

        if (locationService == null) {
            log.warn("Arrival failed. Location service unavailable for responder ID: {}", responder.getId());
            throw new IllegalStateException("Live location coordinates not found for responder. Update your location first.");
        }

        Point responderCoords = locationService.getUserCoordinates(responder.getId());
        if (responderCoords == null) {
            log.warn("Arrival failed. Responder ID: {} has no live coordinates in Redis", responder.getId());
            throw new IllegalStateException("Live location coordinates not found for responder. Update your location first.");
        }

        if (!locationService.isLocationFresh(responder.getId())) {
            log.warn("Arrival failed. Responder ID: {} location is stale in Redis", responder.getId());
            throw new IllegalStateException("Responder location is stale. Please update your live location.");
        }

        double distanceMeters = calculateHaversineMetersExact(
                reqLat, reqLng,
                responderCoords.getY(), responderCoords.getX());

        log.info("Calculated responder distance to emergency ID {}: {} meters", emergencyId, distanceMeters);

        if (Math.round(distanceMeters) > 25) {
            log.warn("Arrival rejected. Responder ID {} is {} meters away (must be <= 25m)", responder.getId(), distanceMeters);
            throw new IllegalStateException(String.format("Responder is too far to mark arrival. Must be within 25 meters (current distance: %.1f meters).", distanceMeters));
        }

        synchronized (emergency) {
            if (emergency.getArrivedResponder() != null) {
                if (emergency.getArrivedResponder().getId().equals(responder.getId())) {
                    log.info("Responder ID {} already marked as arrived responder for emergency ID {}", responder.getId(), emergencyId);
                    return emergencyMapper.toResponse(emergency);
                } else {
                    log.warn("Another responder (ID: {}) has already arrived for emergency ID {}", emergency.getArrivedResponder().getId(), emergencyId);
                    throw new IllegalStateException("Another responder (" + emergency.getArrivedResponder().getName() + ") has already arrived at the scene.");
                }
            }
            emergency.setArrivedResponder(responder);
            emergency.setArrivedAt(LocalDateTime.now());
        }

        Emergency savedEmergency = emergencyRepository.save(emergency);
        log.info("Successfully marked responder ID {} arrival for emergency ID {}", responder.getId(), emergencyId);

        EmergencyResponse response = emergencyMapper.toResponse(savedEmergency);
        response.setDistanceMeters(distanceMeters);

        if (webSocketService != null) {
            com.safewalk.dto.event.RealTimeEvent event = com.safewalk.dto.event.RealTimeEvent.builder()
                    .eventType("RESPONDER_ARRIVED")
                    .message("Responder " + responder.getName() + " has arrived at the scene.")
                    .data(response)
                    .build();
            webSocketService.broadcastEvent("/topic/sos/" + emergencyId, event);
            webSocketService.sendToUser(emergency.getRequester().getMobileNumber(), "/queue/messages", event);
        }

        if (fcmService != null) {
            Map<String, String> data = new HashMap<>();
            data.put("emergencyId", emergencyId.toString());
            data.put("type", "RESPONDER_ARRIVED");
            data.put("responderId", responder.getId().toString());
            fcmService.sendPushNotificationToUser(
                    emergency.getRequester().getId(),
                    "🚨 Responder Arrived!",
                    responder.getName() + " has arrived at your emergency location.",
                    data
            );
        }

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public EmergencyResponse rejectEmergency(Long emergencyId, String currentUserMobile) {
        log.info("Processing emergency rejection for ID: {} by responder mobile: {}", emergencyId, currentUserMobile);

        Emergency emergency = emergencyRepository.findById(emergencyId)
                .orElseThrow(() -> new ResourceNotFoundException("Emergency request not found with ID: " + emergencyId));

        User responder = userRepository.findByMobileNumber(currentUserMobile.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Responder user account not found for mobile: " + currentUserMobile));

        log.info("Responder ID: {} rejected emergency notification for emergency ID: {}", responder.getId(), emergencyId);

        EmergencyResponse response = emergencyMapper.toResponse(emergency);

        if (webSocketService != null) {
            com.safewalk.dto.event.RealTimeEvent event = com.safewalk.dto.event.RealTimeEvent.builder()
                    .eventType("SOS_REJECTED")
                    .message("Emergency notification declined")
                    .data(response)
                    .build();
            webSocketService.sendToUser(currentUserMobile.trim(), "/queue/messages", event);
        }

        return response;
    }

    @Override
    @Transactional
    public EmergencyResponse cancelEmergency(Long emergencyId, CancelEmergencyRequest request, String currentUserMobile) {
        log.info("Processing emergency cancellation for ID: {} by user: {}", emergencyId, currentUserMobile);

        Emergency emergency = emergencyRepository.findByIdWithPessimisticLock(emergencyId)
                .or(() -> emergencyRepository.findById(emergencyId))
                .orElseThrow(() -> new ResourceNotFoundException("Emergency request not found with ID: " + emergencyId));

        User requester = emergency.getRequester();
        if (!requester.getMobileNumber().equalsIgnoreCase(currentUserMobile.trim())) {
            log.warn("Cancellation unauthorized. User {} does not own emergency ID: {}", currentUserMobile, emergencyId);
            throw new AccessDeniedException("Access denied. You can only cancel your own emergency request.");
        }

        if (emergency.getStatus() == EmergencyStatus.CANCELLED || emergency.getStatus() == EmergencyStatus.RESOLVED || emergency.getStatus() == EmergencyStatus.EXPIRED) {
            log.warn("Invalid cancellation transition. Emergency ID: {} status is already {}", emergencyId, emergency.getStatus());
            throw new IllegalStateException("Cannot cancel an emergency request that is already " + emergency.getStatus().name());
        }

        // Strict 5-second SOS cancellation window check (server-side exact timestamp check)
        LocalDateTime createdAt = emergency.getCreatedAt();
        if (createdAt != null) {
            long elapsedMillis = java.time.Duration.between(createdAt, LocalDateTime.now()).toMillis();
            if (elapsedMillis > 5000L) {
                log.warn("Cancellation rejected for emergency ID: {}. 5-second cancellation window expired (elapsed: {} ms)",
                        emergencyId, elapsedMillis);
                throw new IllegalStateException("Cancellation window has expired. SOS can only be cancelled within 5 seconds of creation.");
            }
        }

        boolean passwordMatches = false;
        if (requester.getEmergencyPassword() != null && passwordEncoder.matches(request.getEmergencyPassword().trim(), requester.getEmergencyPassword())) {
            passwordMatches = true;
        }
        if (!passwordMatches && requester.getPassword() != null && passwordEncoder.matches(request.getEmergencyPassword().trim(), requester.getPassword())) {
            passwordMatches = true;
        }

        if (!passwordMatches) {
            log.warn("Cancellation failed for emergency ID: {}. Invalid emergency password.", emergencyId);
            throw new InvalidCredentialsException("Invalid emergency password. Cancellation rejected.");
        }

        emergency.setStatus(EmergencyStatus.CANCELLED);
        emergency.setCancelledAt(LocalDateTime.now());
        Emergency updatedEmergency = emergencyRepository.save(emergency);

        log.info("Successfully cancelled emergency ID: {}", emergencyId);

        EmergencyResponse response = emergencyMapper.toResponse(updatedEmergency);

        if (webSocketService != null) {
            com.safewalk.dto.event.RealTimeEvent event = com.safewalk.dto.event.RealTimeEvent.builder()
                    .eventType("SOS_CANCELLED")
                    .message("Emergency request ID " + emergencyId + " was cancelled by requester.")
                    .data(response)
                    .build();
            webSocketService.broadcastEvent("/topic/sos/" + emergencyId, event);
            webSocketService.broadcastEvent("/topic/sos/" + emergencyId + "/location",
                    com.safewalk.dto.event.RealTimeEvent.builder()
                            .eventType("LOCATION_SHARING_STOPPED")
                            .message("Location sharing ended: emergency cancelled")
                            .data(response)
                            .build());

            if (updatedEmergency.getResponders() != null) {
                for (User r : updatedEmergency.getResponders()) {
                    webSocketService.sendToUser(r.getMobileNumber(), "/queue/messages", event);
                }
            }
            if (updatedEmergency.getResponder() != null && (updatedEmergency.getResponders() == null || !updatedEmergency.getResponders().contains(updatedEmergency.getResponder()))) {
                webSocketService.sendToUser(updatedEmergency.getResponder().getMobileNumber(), "/queue/messages", event);
            }
        }

        if (fcmService != null) {
            Map<String, String> data = new HashMap<>();
            data.put("emergencyId", emergencyId.toString());
            data.put("type", "SOS_CANCELLED");
            if (updatedEmergency.getResponders() != null) {
                for (User r : updatedEmergency.getResponders()) {
                    fcmService.sendPushNotificationToUser(
                            r.getId(),
                            "⚠️ SOS Cancelled",
                            "The emergency request you were responding to has been cancelled by the user.",
                            data
                    );
                }
            }
            if (updatedEmergency.getResponder() != null && (updatedEmergency.getResponders() == null || !updatedEmergency.getResponders().contains(updatedEmergency.getResponder()))) {
                fcmService.sendPushNotificationToUser(
                        updatedEmergency.getResponder().getId(),
                        "⚠️ SOS Cancelled",
                        "The emergency request you were responding to has been cancelled by the user.",
                        data
                );
            }
        }

        return response;
    }

    private User resolveCaller(Emergency emergency, String currentUserMobile) {
        if (emergency.getRequester().getMobileNumber().equalsIgnoreCase(currentUserMobile.trim())) {
            return emergency.getRequester();
        }
        if (emergency.getResponder() != null && emergency.getResponder().getMobileNumber().equalsIgnoreCase(currentUserMobile.trim())) {
            return emergency.getResponder();
        }
        if (emergency.getResponders() != null) {
            for (User r : emergency.getResponders()) {
                if (r.getMobileNumber().equalsIgnoreCase(currentUserMobile.trim())) {
                    return r;
                }
            }
        }
        return userRepository.findByMobileNumber(currentUserMobile.trim())
                .orElseThrow(() -> new ResourceNotFoundException("User account not found for mobile: " + currentUserMobile));
    }

    @Override
    @Transactional
    public EmergencyResponse resolveEmergency(Long emergencyId, String currentUserMobile) {
        log.info("Processing emergency resolution for ID: {} by user: {}", emergencyId, currentUserMobile);

        Emergency emergency = emergencyRepository.findByIdWithPessimisticLock(emergencyId)
                .or(() -> emergencyRepository.findById(emergencyId))
                .orElseThrow(() -> new ResourceNotFoundException("Emergency request not found with ID: " + emergencyId));

        final User caller = resolveCaller(emergency, currentUserMobile);
        boolean isRequester = emergency.getRequester().getId().equals(caller.getId());

        boolean isAcceptedResponder = (emergency.getResponder() != null && emergency.getResponder().getId().equals(caller.getId()))
                || (emergency.getResponders() != null && emergency.getResponders().stream().anyMatch(r -> r.getId().equals(caller.getId())));

        if (!isRequester && !isAcceptedResponder) {
            log.warn("Resolution unauthorized. User {} is neither requester nor assigned responder for emergency ID: {}", currentUserMobile, emergencyId);
            throw new AccessDeniedException("Access denied. You can only resolve emergency requests that you created or accepted.");
        }

        if (emergency.getStatus() == EmergencyStatus.CANCELLED || emergency.getStatus() == EmergencyStatus.RESOLVED || emergency.getStatus() == EmergencyStatus.EXPIRED) {
            log.warn("Invalid resolution transition. Emergency ID: {} status is already {}", emergencyId, emergency.getStatus());
            throw new IllegalStateException("Cannot resolve an emergency request that is already " + emergency.getStatus().name());
        }

        boolean hasResponders = (emergency.getResponder() != null)
                || (emergency.getResponders() != null && !emergency.getResponders().isEmpty())
                || (emergency.getArrivedResponder() != null);

        if (isRequester) {
            emergency.setRequesterResolveConfirmed(true);
            log.info("Requester ID {} confirmed resolution for emergency ID {}", caller.getId(), emergencyId);
        }

        if (isAcceptedResponder) {
            // Only the arrived responder can confirm resolution.
            // If arrivedResponder is not yet set, attempt to verify <= 25m arrival now:
            if (emergency.getArrivedResponder() == null) {
                Point reqCoords = getRequesterCoordinates(emergency);
                if (locationService != null && reqCoords != null) {
                    org.springframework.data.geo.Point responderCoords = locationService.getUserCoordinates(caller.getId());
                    if (responderCoords != null) {
                        double distanceMeters = calculateHaversineMetersExact(
                                reqCoords.getY(), reqCoords.getX(),
                                responderCoords.getY(), responderCoords.getX());
                        if (distanceMeters <= 25.0) {
                            emergency.setArrivedResponder(caller);
                            emergency.setArrivedAt(LocalDateTime.now());
                            log.info("Responder ID {} verified as arrived responder at scene (distance: {} m)", caller.getId(), distanceMeters);
                        }
                    }
                }
            }

            if (emergency.getArrivedResponder() == null) {
                log.warn("Responder ID {} tried to confirm resolution but is not marked as arrived (within 25m)", caller.getId());
                throw new IllegalStateException("Only the arrived responder (within 25 meters) can confirm emergency resolution.");
            }

            if (!emergency.getArrivedResponder().getId().equals(caller.getId())) {
                log.warn("Responder ID {} tried to confirm resolution but arrived responder is ID {}",
                        caller.getId(), emergency.getArrivedResponder().getId());
                throw new AccessDeniedException("Only the designated arrived responder (" + emergency.getArrivedResponder().getName() + ") can confirm emergency resolution.");
            }

            emergency.setResponderResolveConfirmed(true);
            log.info("Arrived responder ID {} confirmed resolution for emergency ID {}", caller.getId(), emergencyId);
        }

        // Two-Party Resolution evaluation:
        // If responders are attached: both requester and arrived responder must confirm!
        // If no responders were ever attached: requester confirmation alone is sufficient.
        boolean fullyResolved = (!hasResponders && emergency.isRequesterResolveConfirmed())
                || (emergency.isRequesterResolveConfirmed() && emergency.isResponderResolveConfirmed());

        if (fullyResolved) {
            emergency.setStatus(EmergencyStatus.RESOLVED);
            emergency.setResolvedAt(LocalDateTime.now());
            Emergency updatedEmergency = emergencyRepository.save(emergency);

            log.info("Successfully fully resolved emergency ID: {}", emergencyId);

            EmergencyResponse response = emergencyMapper.toResponse(updatedEmergency);

            if (webSocketService != null) {
                com.safewalk.dto.event.RealTimeEvent event = com.safewalk.dto.event.RealTimeEvent.builder()
                        .eventType("SOS_RESOLVED")
                        .message("Emergency request ID " + emergencyId + " has been resolved.")
                        .data(response)
                        .build();
                webSocketService.broadcastEvent("/topic/sos/" + emergencyId, event);
                webSocketService.broadcastEvent("/topic/sos/" + emergencyId + "/location",
                        com.safewalk.dto.event.RealTimeEvent.builder()
                                .eventType("LOCATION_SHARING_STOPPED")
                                .message("Location sharing ended: emergency resolved")
                                .data(response)
                                .build());

                // Notify requester and all responders
                webSocketService.sendToUser(updatedEmergency.getRequester().getMobileNumber(), "/queue/messages", event);
                if (updatedEmergency.getResponders() != null) {
                    for (User r : updatedEmergency.getResponders()) {
                        webSocketService.sendToUser(r.getMobileNumber(), "/queue/messages", event);
                    }
                }
                if (updatedEmergency.getResponder() != null) {
                    webSocketService.sendToUser(updatedEmergency.getResponder().getMobileNumber(), "/queue/messages", event);
                }
            }

            if (fcmService != null) {
                Map<String, String> data = new HashMap<>();
                data.put("emergencyId", emergencyId.toString());
                data.put("type", "SOS_RESOLVED");

                // Send FCM to requester
                fcmService.sendPushNotificationToUser(
                        updatedEmergency.getRequester().getId(),
                        "✅ Emergency Resolved",
                        "Your emergency request has been fully resolved.",
                        data
                );

                // Send FCM to all responders
                if (updatedEmergency.getResponders() != null) {
                    for (User r : updatedEmergency.getResponders()) {
                        fcmService.sendPushNotificationToUser(
                                r.getId(),
                                "✅ Emergency Resolved",
                                "The emergency request has been marked as fully resolved.",
                                data
                        );
                    }
                }
                if (updatedEmergency.getResponder() != null && (updatedEmergency.getResponders() == null || !updatedEmergency.getResponders().contains(updatedEmergency.getResponder()))) {
                    fcmService.sendPushNotificationToUser(
                            updatedEmergency.getResponder().getId(),
                            "✅ Emergency Resolved",
                            "The emergency request has been marked as fully resolved.",
                            data
                    );
                }
            }

            return response;
        } else {
            // One party confirmed; awaiting the second confirmation
            Emergency savedEmergency = emergencyRepository.save(emergency);
            log.info("Emergency ID: {} resolution confirmation recorded: requesterConfirmed={}, responderConfirmed={}. Awaiting second party confirmation.",
                    emergencyId, emergency.isRequesterResolveConfirmed(), emergency.isResponderResolveConfirmed());

            EmergencyResponse response = emergencyMapper.toResponse(savedEmergency);

            if (webSocketService != null) {
                String pendingMsg = emergency.isRequesterResolveConfirmed()
                        ? "Requester has confirmed resolution. Awaiting arrived responder confirmation."
                        : "Arrived responder has confirmed resolution. Awaiting requester confirmation.";

                com.safewalk.dto.event.RealTimeEvent pendingEvent = com.safewalk.dto.event.RealTimeEvent.builder()
                        .eventType("RESOLUTION_CONFIRMATION_PENDING")
                        .message(pendingMsg)
                        .data(response)
                        .build();

                webSocketService.broadcastEvent("/topic/sos/" + emergencyId, pendingEvent);

                if (emergency.isRequesterResolveConfirmed()) {
                    if (emergency.getArrivedResponder() != null) {
                        webSocketService.sendToUser(emergency.getArrivedResponder().getMobileNumber(), "/queue/messages", pendingEvent);
                    }
                } else {
                    webSocketService.sendToUser(emergency.getRequester().getMobileNumber(), "/queue/messages", pendingEvent);
                }
            }

            return response;
        }
    }

    @Override
    @Transactional
    public int expireOverdueEmergencies() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(5);
        List<Emergency> overdueEmergencies = emergencyRepository.findOverdueEmergencies(cutoff);
        if (overdueEmergencies.isEmpty()) {
            return 0;
        }

        log.info("Found {} overdue active emergencies older than 5 hours (created on or before {})", overdueEmergencies.size(), cutoff);
        int expiredCount = 0;

        for (Emergency candidate : overdueEmergencies) {
            try {
                Emergency emergency = emergencyRepository.findByIdWithPessimisticLock(candidate.getId())
                        .or(() -> emergencyRepository.findById(candidate.getId()))
                        .orElse(candidate);

                if (emergency.getStatus() == EmergencyStatus.ACTIVE || emergency.getStatus() == EmergencyStatus.ACCEPTED) {
                    emergency.setStatus(EmergencyStatus.EXPIRED);
                    LocalDateTime calculatedExpiry = emergency.getCreatedAt() != null
                            ? emergency.getCreatedAt().plusHours(5)
                            : LocalDateTime.now();
                    emergency.setExpiredAt(calculatedExpiry);

                    Emergency savedEmergency = emergencyRepository.save(emergency);
                    expiredCount++;
                    log.info("Successfully expired emergency ID: {} (created: {}, expiredAt: {})",
                            savedEmergency.getId(), savedEmergency.getCreatedAt(), savedEmergency.getExpiredAt());

                    EmergencyResponse response = emergencyMapper.toResponse(savedEmergency);

                    if (webSocketService != null) {
                        try {
                            com.safewalk.dto.event.RealTimeEvent event = com.safewalk.dto.event.RealTimeEvent.builder()
                                    .eventType("SOS_EXPIRED")
                                    .message("Emergency request ID " + savedEmergency.getId() + " has expired after 5 hours.")
                                    .data(response)
                                    .build();
                            webSocketService.broadcastEvent("/topic/sos/" + savedEmergency.getId(), event);
                            webSocketService.broadcastEvent("/topic/sos", event);
                            webSocketService.broadcastEvent("/topic/sos/" + savedEmergency.getId() + "/location",
                                    com.safewalk.dto.event.RealTimeEvent.builder()
                                            .eventType("LOCATION_SHARING_STOPPED")
                                            .message("Location sharing ended: emergency expired")
                                            .data(response)
                                            .build());

                            if (savedEmergency.getRequester() != null && savedEmergency.getRequester().getMobileNumber() != null) {
                                webSocketService.sendToUser(savedEmergency.getRequester().getMobileNumber(), "/queue/messages", event);
                            }
                            if (savedEmergency.getResponders() != null) {
                                for (User r : savedEmergency.getResponders()) {
                                    if (r.getMobileNumber() != null) {
                                        webSocketService.sendToUser(r.getMobileNumber(), "/queue/messages", event);
                                    }
                                }
                            }
                            if (savedEmergency.getResponder() != null && savedEmergency.getResponder().getMobileNumber() != null) {
                                webSocketService.sendToUser(savedEmergency.getResponder().getMobileNumber(), "/queue/messages", event);
                            }
                        } catch (Exception wsEx) {
                            log.warn("WebSocket notification failed for expired emergency ID: {}", savedEmergency.getId(), wsEx);
                        }
                    }

                    if (fcmService != null) {
                        try {
                            Map<String, String> data = new HashMap<>();
                            data.put("emergencyId", savedEmergency.getId().toString());
                            data.put("type", "SOS_EXPIRED");
                            if (savedEmergency.getRequester() != null && savedEmergency.getRequester().getId() != null) {
                                fcmService.sendPushNotificationToUser(
                                        savedEmergency.getRequester().getId(),
                                        "⌛ Emergency Expired",
                                        "Your emergency request has expired after 5 hours.",
                                        data
                                );
                            }
                            if (savedEmergency.getResponders() != null) {
                                for (User r : savedEmergency.getResponders()) {
                                    if (r.getId() != null) {
                                        fcmService.sendPushNotificationToUser(
                                                r.getId(),
                                                "⌛ Emergency Expired",
                                                "The emergency request has expired after 5 hours.",
                                                data
                                        );
                                    }
                                }
                            }
                        } catch (Exception fcmEx) {
                            log.warn("FCM push failed for expired emergency ID: {}", savedEmergency.getId(), fcmEx);
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Failed to expire overdue emergency ID: {}", candidate.getId(), e);
            }
        }

        log.info("Emergency expiry scan finished. Expired {}/{} overdue emergencies.", expiredCount, overdueEmergencies.size());
        return expiredCount;
    }

    @Override
    @Transactional
    public int discoverAndDispatchNewResponders() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(5);
        List<Emergency> activeEmergencies = emergencyRepository.findActiveEmergenciesForDiscovery(cutoff);
        if (activeEmergencies.isEmpty()) {
            return 0;
        }

        int totalDispatchedCount = 0;

        for (Emergency candidate : activeEmergencies) {
            try {
                Emergency emergency = emergencyRepository.findByIdWithPessimisticLock(candidate.getId())
                        .or(() -> emergencyRepository.findById(candidate.getId()))
                        .orElse(candidate);

                // Strictly consider ONLY ACTIVE emergencies (skip ACCEPTED, RESOLVED, CANCELLED, EXPIRED)
                if (emergency.getStatus() != EmergencyStatus.ACTIVE) {
                    continue;
                }

                Point reqCoords = getRequesterCoordinates(emergency);
                if (reqCoords == null) {
                    continue;
                }

                // Verify 5-hour lifetime window from original creation time
                LocalDateTime createdAt = emergency.getCreatedAt();
                if (createdAt == null) {
                    continue;
                }
                long remainingTtlMillis = java.time.Duration.between(LocalDateTime.now(), createdAt.plusHours(5)).toMillis();
                if (remainingTtlMillis <= 0) {
                    log.info("Emergency ID {} has reached or exceeded its 5-hour lifetime; skipping discovery dispatch", emergency.getId());
                    continue;
                }

                if (locationService == null) {
                    continue;
                }

                List<com.safewalk.dto.response.NearbyUserResponse> nearbyResponders =
                        locationService.findNearbyResponders(reqCoords.getY(), reqCoords.getX(), 2.0);

                if (nearbyResponders == null || nearbyResponders.isEmpty()) {
                    continue;
                }

                for (com.safewalk.dto.response.NearbyUserResponse responder : nearbyResponders) {
                    // Revalidate emergency status immediately before processing responder dispatch
                    Emergency freshEmergency = emergencyRepository.findById(emergency.getId()).orElse(null);
                    if (freshEmergency == null || freshEmergency.getStatus() != EmergencyStatus.ACTIVE) {
                        log.info("Emergency ID {} is no longer ACTIVE (status: {}); aborting remaining continuous discovery dispatches",
                                emergency.getId(), freshEmergency != null ? freshEmergency.getStatus() : "null");
                        break;
                    }
                    if (freshEmergency.getCreatedAt() != null &&
                            LocalDateTime.now().isAfter(freshEmergency.getCreatedAt().plusHours(5))) {
                        log.info("Emergency ID {} has reached or exceeded its 5-hour lifetime mid-loop; aborting", emergency.getId());
                        break;
                    }

                    Long responderId = responder.getUserId();
                    if (responderId == null) {
                        continue;
                    }

                    // Never dispatch to the requester themselves
                    if (emergency.getRequester() != null && responderId.equals(emergency.getRequester().getId())) {
                        continue;
                    }

                    // Strict location freshness requirement (must be <= 300s)
                    if (!locationService.isLocationFresh(responderId)) {
                        log.debug("Skipping responder ID {} for emergency ID {}: location coordinates are stale in Redis", responderId, emergency.getId());
                        continue;
                    }

                    // Persistent deduplication & bounded retry check
                    com.safewalk.entity.EmergencyDispatch existingDispatch = null;
                    if (emergencyDispatchRepository != null) {
                        existingDispatch = emergencyDispatchRepository.findByEmergencyIdAndResponderId(emergency.getId(), responderId).orElse(null);
                    }

                    if (existingDispatch != null) {
                        // 1. If already successfully sent, NEVER dispatch again (Zero duplicate delivery)
                        if (existingDispatch.getStatus() == com.safewalk.entity.DispatchStatus.SENT) {
                            continue;
                        }

                        // 2. If PENDING, check if it's stale (crash recovery) or actively in flight
                        if (existingDispatch.getStatus() == com.safewalk.entity.DispatchStatus.PENDING) {
                            LocalDateTime created = existingDispatch.getCreatedAt();
                            boolean isStalePending = created != null && created.isBefore(LocalDateTime.now().minusMinutes(2));
                            if (!isStalePending) {
                                // Currently being dispatched by another thread or node; skip
                                continue;
                            }
                            log.info("Recovering stale PENDING dispatch record ID {} for emergency ID {} and responder ID {}",
                                    existingDispatch.getId(), emergency.getId(), responderId);
                        }

                        // 3. If FAILED, check if eligible for bounded safe retry (max 2 retries, 60s backoff)
                        if (existingDispatch.getStatus() == com.safewalk.entity.DispatchStatus.FAILED) {
                            if (existingDispatch.getRetryCount() >= 2) {
                                // Bounded limit reached; do not retry further
                                continue;
                            }
                            LocalDateTime lastAttempt = existingDispatch.getLastAttemptAt();
                            if (lastAttempt != null && lastAttempt.isAfter(LocalDateTime.now().minusMinutes(1))) {
                                // Within 1-minute backoff window; wait before retrying
                                continue;
                            }
                            log.info("Scheduling bounded retry (attempt #{}) for failed dispatch ID {} to responder ID {}",
                                    existingDispatch.getRetryCount() + 1, existingDispatch.getId(), responderId);
                        }
                    }

                    // Atomic reservation / lock
                    com.safewalk.entity.EmergencyDispatch dispatch = existingDispatch;
                    if (emergencyDispatchRepository != null) {
                        if (dispatch == null) {
                            dispatch = com.safewalk.entity.EmergencyDispatch.builder()
                                    .emergency(emergency)
                                    .responder(userRepository.getReferenceById(responderId))
                                    .status(com.safewalk.entity.DispatchStatus.PENDING)
                                    .distanceMeters(responder.getDistanceKm() != null ? responder.getDistanceKm() * 1000.0 : null)
                                    .retryCount(0)
                                    .lastAttemptAt(LocalDateTime.now())
                                    .build();
                            try {
                                dispatch = emergencyDispatchRepository.saveAndFlush(dispatch);
                            } catch (org.springframework.dao.DataIntegrityViolationException e) {
                                // Concurrent scan/instance already created dispatch for this pair
                                continue;
                            }
                        } else {
                            dispatch.setStatus(com.safewalk.entity.DispatchStatus.PENDING);
                            dispatch.setLastAttemptAt(LocalDateTime.now());
                            dispatch.setRetryCount(dispatch.getRetryCount() + 1);
                            dispatch = emergencyDispatchRepository.saveAndFlush(dispatch);
                        }
                    }

                    // Prepare FCM Notification payload
                    Map<String, String> data = new HashMap<>();
                    data.put("emergencyId", String.valueOf(emergency.getId()));
                    data.put("type", "SOS_CREATED");
                    data.put("isEmergencyContact", "false");
                    data.put("requesterId", emergency.getRequester() != null ? String.valueOf(emergency.getRequester().getId()) : "");
                    data.put("requesterName", (emergency.getRequester() != null && emergency.getRequester().getName() != null)
                            ? emergency.getRequester().getName() : "Someone in need");
                    data.put("requesterMobile", (emergency.getRequester() != null && emergency.getRequester().getMobileNumber() != null)
                            ? emergency.getRequester().getMobileNumber() : "");
                    data.put("latitude", String.valueOf(reqCoords.getY()));
                    data.put("longitude", String.valueOf(reqCoords.getX()));
                    if (emergency.getRequester() != null && emergency.getRequester().getAvatarUrl() != null && !emergency.getRequester().getAvatarUrl().trim().isEmpty()) {
                        data.put("avatarUrl", emergency.getRequester().getAvatarUrl());
                        data.put("requesterAvatarUrl", emergency.getRequester().getAvatarUrl());
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

                    String requesterDisplayName = (emergency.getRequester() != null && emergency.getRequester().getName() != null && !emergency.getRequester().getName().trim().isEmpty())
                            ? emergency.getRequester().getName()
                            : "Someone in need";
                    String notifTitle = "🚨 Emergency Alert";
                    String notifBody = requesterDisplayName + " needs help! Approx. " + distanceStr + " away.";

                    // Dispatch FCM push notification with dynamic remaining TTL and fixed emergency collapse key
                    boolean sent = false;
                    if (fcmService != null) {
                        int sentCount = fcmService.sendPushNotificationToUserWithTtl(
                                responderId,
                                notifTitle,
                                notifBody,
                                data,
                                remainingTtlMillis,
                                "safewalk_emergency_alert"
                        );
                        sent = sentCount > 0;
                    }

                    // Update dispatch record
                    if (dispatch != null && emergencyDispatchRepository != null) {
                        dispatch.setStatus(sent ? com.safewalk.entity.DispatchStatus.SENT : com.safewalk.entity.DispatchStatus.FAILED);
                        if (sent) {
                            dispatch.setSentAt(LocalDateTime.now());
                        }
                        emergencyDispatchRepository.save(dispatch);
                    }

                    // Dispatch WebSocket notification to responder private queue
                    if (webSocketService != null && responder.getMobileNumber() != null) {
                        try {
                            com.safewalk.dto.event.RealTimeEvent wsEvent = com.safewalk.dto.event.RealTimeEvent.builder()
                                    .eventType("SOS_CREATED")
                                    .message("Emergency request nearby")
                                    .data(emergencyMapper.toResponse(emergency))
                                    .build();
                            webSocketService.sendToUser(responder.getMobileNumber(), "/queue/messages", wsEvent);
                        } catch (Exception wsEx) {
                            log.debug("WebSocket notice failed for responder {}: {}", responderId, wsEx.getMessage());
                        }
                    }

                    totalDispatchedCount++;
                    log.info("Continuous discovery dispatched alert for emergency ID {} to newly eligible responder ID {} ({})",
                            emergency.getId(), responderId, distanceStr);
                }
            } catch (Exception e) {
                log.error("Failed to run continuous discovery for emergency ID: {}", candidate.getId(), e);
            }
        }

        return totalDispatchedCount;
    }
}
