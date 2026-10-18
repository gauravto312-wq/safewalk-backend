package com.safewalk.service;

import com.safewalk.dto.response.EmergencyResponse;
import com.safewalk.entity.AccountStatus;
import com.safewalk.entity.Emergency;
import com.safewalk.entity.EmergencyStatus;
import com.safewalk.entity.EmergencyType;
import com.safewalk.entity.Role;
import com.safewalk.entity.User;
import com.safewalk.entity.VerificationStatus;
import com.safewalk.exception.AccountStatusException;
import com.safewalk.mapper.EmergencyMapper;
import com.safewalk.repository.EmergencyRepository;
import com.safewalk.repository.UserRepository;
import com.safewalk.service.impl.EmergencyServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.geo.Point;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmergencyLocationRulesTest {

    @Mock
    private EmergencyRepository emergencyRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private LocationService locationService;

    private EmergencyMapper emergencyMapper;
    private EmergencyServiceImpl emergencyService;

    private User requester;
    private Emergency activeEmergency;

    private static final double REQ_LAT = 28.6139;
    private static final double REQ_LNG = 77.2090;

    @BeforeEach
    void setUp() {
        emergencyMapper = new EmergencyMapper();
        emergencyService = new EmergencyServiceImpl(
                emergencyRepository, userRepository, emergencyMapper, new BCryptPasswordEncoder(),
                null, null, null, null, locationService);

        requester = User.builder()
                .id(1L)
                .name("Aman Requester")
                .mobileNumber("+919876543210")
                .role(Role.USER)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        activeEmergency = Emergency.builder()
                .id(100L)
                .requester(requester)
                .type(EmergencyType.GENERAL)
                .status(EmergencyStatus.ACTIVE)
                .latitude(REQ_LAT)
                .longitude(REQ_LNG)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .responders(new HashSet<>())
                .build();
    }

    /**
     * Helper to compute a Point at an exact distance north of the requester.
     * Earth radius = 6371000m.
     * Point(x=lng, y=lat).
     */
    private Point pointAtDistance(double distanceMeters) {
        double dLat = (distanceMeters / 6371000.0) * (180.0 / Math.PI);
        return new Point(REQ_LNG, REQ_LAT + dLat);
    }

    private User createCitizen(Long id, String mobile) {
        return User.builder()
                .id(id)
                .name("Citizen " + id)
                .mobileNumber(mobile)
                .role(Role.USER)
                .accountStatus(AccountStatus.ACTIVE)
                .build();
    }

    private User createPolice(Long id, String mobile, boolean verified) {
        return User.builder()
                .id(id)
                .name("Officer " + id)
                .mobileNumber(mobile)
                .role(Role.POLICE)
                .accountStatus(AccountStatus.ACTIVE)
                .verificationStatus(verified ? VerificationStatus.VERIFIED : VerificationStatus.PENDING)
                .policeVerified(verified)
                .build();
    }

    private User createHospital(Long id, String mobile, boolean verified) {
        return User.builder()
                .id(id)
                .name("Hospital " + id)
                .mobileNumber(mobile)
                .role(Role.HOSPITAL)
                .accountStatus(AccountStatus.ACTIVE)
                .verificationStatus(verified ? VerificationStatus.VERIFIED : VerificationStatus.PENDING)
                .build();
    }

    // =========================================================================
    // CITIZEN DISTANCE ACCEPTANCE RULES (1 to 8)
    // Rule: distance > 50m AND distance <= 2000m
    // =========================================================================

    @Test
    @DisplayName("1. Citizen at 49m -> REJECT")
    void citizen_49m_Rejected() {
        User citizen = createCitizen(2L, "+919876543211");
        when(emergencyRepository.findById(100L)).thenReturn(Optional.of(activeEmergency));
        when(userRepository.findByMobileNumber("+919876543211")).thenReturn(Optional.of(citizen));
        when(locationService.getUserCoordinates(2L)).thenReturn(pointAtDistance(49.0));
        when(locationService.isLocationFresh(2L)).thenReturn(true);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> emergencyService.acceptEmergency(100L, "+919876543211"));
        assertTrue(ex.getMessage().contains("too close"));
    }

    @Test
    @DisplayName("2. Citizen at 50m -> REJECT (boundary)")
    void citizen_50m_Rejected() {
        User citizen = createCitizen(2L, "+919876543211");
        when(emergencyRepository.findById(100L)).thenReturn(Optional.of(activeEmergency));
        when(userRepository.findByMobileNumber("+919876543211")).thenReturn(Optional.of(citizen));
        when(locationService.getUserCoordinates(2L)).thenReturn(pointAtDistance(50.0));
        when(locationService.isLocationFresh(2L)).thenReturn(true);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> emergencyService.acceptEmergency(100L, "+919876543211"));
        assertTrue(ex.getMessage().contains("too close"));
    }

    @Test
    @DisplayName("3. Citizen at 51m -> ACCEPT (boundary)")
    void citizen_51m_Accepted() {
        User citizen = createCitizen(2L, "+919876543211");
        when(emergencyRepository.findById(100L)).thenReturn(Optional.of(activeEmergency));
        when(userRepository.findByMobileNumber("+919876543211")).thenReturn(Optional.of(citizen));
        when(locationService.getUserCoordinates(2L)).thenReturn(pointAtDistance(51.0));
        when(locationService.isLocationFresh(2L)).thenReturn(true);
        when(emergencyRepository.save(any(Emergency.class))).thenAnswer(i -> i.getArgument(0));

        EmergencyResponse resp = emergencyService.acceptEmergency(100L, "+919876543211");
        assertNotNull(resp);
        assertEquals(EmergencyStatus.ACCEPTED, resp.getStatus());
        assertNotNull(resp.getDistanceMeters());
        assertEquals(51.0, resp.getDistanceMeters(), 0.5);
    }

    @Test
    @DisplayName("4. Citizen at 100m -> ACCEPT")
    void citizen_100m_Accepted() {
        User citizen = createCitizen(2L, "+919876543211");
        when(emergencyRepository.findById(100L)).thenReturn(Optional.of(activeEmergency));
        when(userRepository.findByMobileNumber("+919876543211")).thenReturn(Optional.of(citizen));
        when(locationService.getUserCoordinates(2L)).thenReturn(pointAtDistance(100.0));
        when(locationService.isLocationFresh(2L)).thenReturn(true);
        when(emergencyRepository.save(any(Emergency.class))).thenAnswer(i -> i.getArgument(0));

        EmergencyResponse resp = emergencyService.acceptEmergency(100L, "+919876543211");
        assertNotNull(resp);
        assertEquals(EmergencyStatus.ACCEPTED, resp.getStatus());
    }

    @Test
    @DisplayName("5. Citizen at 500m -> ACCEPT")
    void citizen_500m_Accepted() {
        User citizen = createCitizen(2L, "+919876543211");
        when(emergencyRepository.findById(100L)).thenReturn(Optional.of(activeEmergency));
        when(userRepository.findByMobileNumber("+919876543211")).thenReturn(Optional.of(citizen));
        when(locationService.getUserCoordinates(2L)).thenReturn(pointAtDistance(500.0));
        when(locationService.isLocationFresh(2L)).thenReturn(true);
        when(emergencyRepository.save(any(Emergency.class))).thenAnswer(i -> i.getArgument(0));

        EmergencyResponse resp = emergencyService.acceptEmergency(100L, "+919876543211");
        assertNotNull(resp);
        assertEquals(EmergencyStatus.ACCEPTED, resp.getStatus());
    }

    @Test
    @DisplayName("6. Citizen at 1000m -> ACCEPT")
    void citizen_1000m_Accepted() {
        User citizen = createCitizen(2L, "+919876543211");
        when(emergencyRepository.findById(100L)).thenReturn(Optional.of(activeEmergency));
        when(userRepository.findByMobileNumber("+919876543211")).thenReturn(Optional.of(citizen));
        when(locationService.getUserCoordinates(2L)).thenReturn(pointAtDistance(1000.0));
        when(locationService.isLocationFresh(2L)).thenReturn(true);
        when(emergencyRepository.save(any(Emergency.class))).thenAnswer(i -> i.getArgument(0));

        EmergencyResponse resp = emergencyService.acceptEmergency(100L, "+919876543211");
        assertNotNull(resp);
        assertEquals(EmergencyStatus.ACCEPTED, resp.getStatus());
    }

    @Test
    @DisplayName("7. Citizen at 2000m -> ACCEPT (boundary)")
    void citizen_2000m_Accepted() {
        User citizen = createCitizen(2L, "+919876543211");
        when(emergencyRepository.findById(100L)).thenReturn(Optional.of(activeEmergency));
        when(userRepository.findByMobileNumber("+919876543211")).thenReturn(Optional.of(citizen));
        when(locationService.getUserCoordinates(2L)).thenReturn(pointAtDistance(2000.0));
        when(locationService.isLocationFresh(2L)).thenReturn(true);
        when(emergencyRepository.save(any(Emergency.class))).thenAnswer(i -> i.getArgument(0));

        EmergencyResponse resp = emergencyService.acceptEmergency(100L, "+919876543211");
        assertNotNull(resp);
        assertEquals(EmergencyStatus.ACCEPTED, resp.getStatus());
    }

    @Test
    @DisplayName("8. Citizen at 2001m -> REJECT (boundary)")
    void citizen_2001m_Rejected() {
        User citizen = createCitizen(2L, "+919876543211");
        when(emergencyRepository.findById(100L)).thenReturn(Optional.of(activeEmergency));
        when(userRepository.findByMobileNumber("+919876543211")).thenReturn(Optional.of(citizen));
        when(locationService.getUserCoordinates(2L)).thenReturn(pointAtDistance(2001.0));
        when(locationService.isLocationFresh(2L)).thenReturn(true);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> emergencyService.acceptEmergency(100L, "+919876543211"));
        assertTrue(ex.getMessage().contains("too far"));
    }

    // =========================================================================
    // VERIFIED POLICE RULES (9 to 12)
    // Rule: distance <= 2000m (NO 50m exclusion)
    // =========================================================================

    @Test
    @DisplayName("9. Verified Police at 20m -> ACCEPT")
    void verifiedPolice_20m_Accepted() {
        User police = createPolice(10L, "+919876543220", true);
        when(emergencyRepository.findById(100L)).thenReturn(Optional.of(activeEmergency));
        when(userRepository.findByMobileNumber("+919876543220")).thenReturn(Optional.of(police));
        when(locationService.getUserCoordinates(10L)).thenReturn(pointAtDistance(20.0));
        when(locationService.isLocationFresh(10L)).thenReturn(true);
        when(emergencyRepository.save(any(Emergency.class))).thenAnswer(i -> i.getArgument(0));

        EmergencyResponse resp = emergencyService.acceptEmergency(100L, "+919876543220");
        assertNotNull(resp);
        assertEquals(EmergencyStatus.ACCEPTED, resp.getStatus());
    }

    @Test
    @DisplayName("10. Verified Police at 50m -> ACCEPT")
    void verifiedPolice_50m_Accepted() {
        User police = createPolice(10L, "+919876543220", true);
        when(emergencyRepository.findById(100L)).thenReturn(Optional.of(activeEmergency));
        when(userRepository.findByMobileNumber("+919876543220")).thenReturn(Optional.of(police));
        when(locationService.getUserCoordinates(10L)).thenReturn(pointAtDistance(50.0));
        when(locationService.isLocationFresh(10L)).thenReturn(true);
        when(emergencyRepository.save(any(Emergency.class))).thenAnswer(i -> i.getArgument(0));

        EmergencyResponse resp = emergencyService.acceptEmergency(100L, "+919876543220");
        assertNotNull(resp);
        assertEquals(EmergencyStatus.ACCEPTED, resp.getStatus());
    }

    @Test
    @DisplayName("11. Verified Police at 2000m -> ACCEPT")
    void verifiedPolice_2000m_Accepted() {
        User police = createPolice(10L, "+919876543220", true);
        when(emergencyRepository.findById(100L)).thenReturn(Optional.of(activeEmergency));
        when(userRepository.findByMobileNumber("+919876543220")).thenReturn(Optional.of(police));
        when(locationService.getUserCoordinates(10L)).thenReturn(pointAtDistance(2000.0));
        when(locationService.isLocationFresh(10L)).thenReturn(true);
        when(emergencyRepository.save(any(Emergency.class))).thenAnswer(i -> i.getArgument(0));

        EmergencyResponse resp = emergencyService.acceptEmergency(100L, "+919876543220");
        assertNotNull(resp);
        assertEquals(EmergencyStatus.ACCEPTED, resp.getStatus());
    }

    @Test
    @DisplayName("12. Verified Police at 2001m -> REJECT")
    void verifiedPolice_2001m_Rejected() {
        User police = createPolice(10L, "+919876543220", true);
        when(emergencyRepository.findById(100L)).thenReturn(Optional.of(activeEmergency));
        when(userRepository.findByMobileNumber("+919876543220")).thenReturn(Optional.of(police));
        when(locationService.getUserCoordinates(10L)).thenReturn(pointAtDistance(2001.0));
        when(locationService.isLocationFresh(10L)).thenReturn(true);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> emergencyService.acceptEmergency(100L, "+919876543220"));
        assertTrue(ex.getMessage().contains("too far"));
    }

    // =========================================================================
    // VERIFIED HOSPITAL RULES (13)
    // =========================================================================

    @Test
    @DisplayName("13a. Verified Hospital at 20m -> ACCEPT")
    void verifiedHospital_20m_Accepted() {
        User hospital = createHospital(30L, "+919876543230", true);
        when(emergencyRepository.findById(100L)).thenReturn(Optional.of(activeEmergency));
        when(userRepository.findByMobileNumber("+919876543230")).thenReturn(Optional.of(hospital));
        when(locationService.getUserCoordinates(30L)).thenReturn(pointAtDistance(20.0));
        when(locationService.isLocationFresh(30L)).thenReturn(true);
        when(emergencyRepository.save(any(Emergency.class))).thenAnswer(i -> i.getArgument(0));

        EmergencyResponse resp = emergencyService.acceptEmergency(100L, "+919876543230");
        assertNotNull(resp);
        assertEquals(EmergencyStatus.ACCEPTED, resp.getStatus());
    }

    @Test
    @DisplayName("13b. Verified Hospital at 50m -> ACCEPT")
    void verifiedHospital_50m_Accepted() {
        User hospital = createHospital(30L, "+919876543230", true);
        when(emergencyRepository.findById(100L)).thenReturn(Optional.of(activeEmergency));
        when(userRepository.findByMobileNumber("+919876543230")).thenReturn(Optional.of(hospital));
        when(locationService.getUserCoordinates(30L)).thenReturn(pointAtDistance(50.0));
        when(locationService.isLocationFresh(30L)).thenReturn(true);
        when(emergencyRepository.save(any(Emergency.class))).thenAnswer(i -> i.getArgument(0));

        EmergencyResponse resp = emergencyService.acceptEmergency(100L, "+919876543230");
        assertNotNull(resp);
        assertEquals(EmergencyStatus.ACCEPTED, resp.getStatus());
    }

    @Test
    @DisplayName("13c. Verified Hospital at 2000m -> ACCEPT")
    void verifiedHospital_2000m_Accepted() {
        User hospital = createHospital(30L, "+919876543230", true);
        when(emergencyRepository.findById(100L)).thenReturn(Optional.of(activeEmergency));
        when(userRepository.findByMobileNumber("+919876543230")).thenReturn(Optional.of(hospital));
        when(locationService.getUserCoordinates(30L)).thenReturn(pointAtDistance(2000.0));
        when(locationService.isLocationFresh(30L)).thenReturn(true);
        when(emergencyRepository.save(any(Emergency.class))).thenAnswer(i -> i.getArgument(0));

        EmergencyResponse resp = emergencyService.acceptEmergency(100L, "+919876543230");
        assertNotNull(resp);
        assertEquals(EmergencyStatus.ACCEPTED, resp.getStatus());
    }

    @Test
    @DisplayName("13d. Verified Hospital at 2001m -> REJECT")
    void verifiedHospital_2001m_Rejected() {
        User hospital = createHospital(30L, "+919876543230", true);
        when(emergencyRepository.findById(100L)).thenReturn(Optional.of(activeEmergency));
        when(userRepository.findByMobileNumber("+919876543230")).thenReturn(Optional.of(hospital));
        when(locationService.getUserCoordinates(30L)).thenReturn(pointAtDistance(2001.0));
        when(locationService.isLocationFresh(30L)).thenReturn(true);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> emergencyService.acceptEmergency(100L, "+919876543230"));
        assertTrue(ex.getMessage().contains("too far"));
    }

    // =========================================================================
    // SECURITY RULES (14 to 16)
    // =========================================================================

    @Test
    @DisplayName("14. Unverified Police -> REJECT")
    void unverifiedPolice_Rejected() {
        User police = createPolice(11L, "+919876543221", false);
        when(emergencyRepository.findById(100L)).thenReturn(Optional.of(activeEmergency));
        when(userRepository.findByMobileNumber("+919876543221")).thenReturn(Optional.of(police));

        AccountStatusException ex = assertThrows(AccountStatusException.class,
                () -> emergencyService.acceptEmergency(100L, "+919876543221"));
        assertTrue(ex.getMessage().contains("Account verification pending"));
    }

    @Test
    @DisplayName("15. Unverified Hospital -> REJECT")
    void unverifiedHospital_Rejected() {
        User hospital = createHospital(31L, "+919876543231", false);
        when(emergencyRepository.findById(100L)).thenReturn(Optional.of(activeEmergency));
        when(userRepository.findByMobileNumber("+919876543231")).thenReturn(Optional.of(hospital));

        AccountStatusException ex = assertThrows(AccountStatusException.class,
                () -> emergencyService.acceptEmergency(100L, "+919876543231"));
        assertTrue(ex.getMessage().contains("Account verification pending"));
    }

    @Test
    @DisplayName("16. Requester self-accept -> REJECT")
    void requester_SelfAccept_Rejected() {
        when(emergencyRepository.findById(100L)).thenReturn(Optional.of(activeEmergency));
        when(userRepository.findByMobileNumber("+919876543210")).thenReturn(Optional.of(requester));

        assertThrows(IllegalArgumentException.class,
                () -> emergencyService.acceptEmergency(100L, "+919876543210"));
    }

    // =========================================================================
    // LOCATION & FRESHNESS RULES (17 to 20)
    // =========================================================================

    @Test
    @DisplayName("17. Missing responder coordinates -> REJECT")
    void missingResponderCoordinates_Rejected() {
        User citizen = createCitizen(2L, "+919876543211");
        when(emergencyRepository.findById(100L)).thenReturn(Optional.of(activeEmergency));
        when(userRepository.findByMobileNumber("+919876543211")).thenReturn(Optional.of(citizen));
        when(locationService.getUserCoordinates(2L)).thenReturn(null);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> emergencyService.acceptEmergency(100L, "+919876543211"));
        assertTrue(ex.getMessage().contains("Live location coordinates not found for responder"));
    }

    @Test
    @DisplayName("18. Missing requester coordinates -> REJECT")
    void missingRequesterCoordinates_Rejected() {
        activeEmergency.setLatitude(null);
        activeEmergency.setLongitude(null);

        User citizen = createCitizen(2L, "+919876543211");
        when(emergencyRepository.findById(100L)).thenReturn(Optional.of(activeEmergency));
        when(userRepository.findByMobileNumber("+919876543211")).thenReturn(Optional.of(citizen));
        when(locationService.getUserCoordinates(1L)).thenReturn(null); // Requester coords in redis also null

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> emergencyService.acceptEmergency(100L, "+919876543211"));
        assertTrue(ex.getMessage().contains("Emergency location coordinates are missing"));
    }

    @Test
    @DisplayName("19. Fresh coordinates within distance rule -> ACCEPT")
    void freshCoordinates_WithinDistance_Accepted() {
        User citizen = createCitizen(2L, "+919876543211");
        when(emergencyRepository.findById(100L)).thenReturn(Optional.of(activeEmergency));
        when(userRepository.findByMobileNumber("+919876543211")).thenReturn(Optional.of(citizen));
        when(locationService.getUserCoordinates(2L)).thenReturn(pointAtDistance(250.0));
        when(locationService.isLocationFresh(2L)).thenReturn(true);
        when(emergencyRepository.save(any(Emergency.class))).thenAnswer(i -> i.getArgument(0));

        EmergencyResponse resp = emergencyService.acceptEmergency(100L, "+919876543211");
        assertNotNull(resp);
        assertEquals(EmergencyStatus.ACCEPTED, resp.getStatus());
    }

    @Test
    @DisplayName("20. Stale coordinates -> REJECT")
    void staleCoordinates_Rejected() {
        User citizen = createCitizen(2L, "+919876543211");
        when(emergencyRepository.findById(100L)).thenReturn(Optional.of(activeEmergency));
        when(userRepository.findByMobileNumber("+919876543211")).thenReturn(Optional.of(citizen));
        when(locationService.getUserCoordinates(2L)).thenReturn(pointAtDistance(250.0));
        when(locationService.isLocationFresh(2L)).thenReturn(false); // Stale!

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> emergencyService.acceptEmergency(100L, "+919876543211"));
        assertTrue(ex.getMessage().contains("Responder location is stale"));
    }

    // =========================================================================
    // ARRIVAL DISTANCE RULES (21 to 24)
    // Rule: distance <= 25m
    // =========================================================================

    @Test
    @DisplayName("21. Arrival at 25m -> ACCEPT (boundary)")
    void arrival_25m_Accepted() {
        User responder = createCitizen(2L, "+919876543211");
        activeEmergency.setStatus(EmergencyStatus.ACCEPTED);
        activeEmergency.addResponder(responder);

        when(emergencyRepository.findByIdWithPessimisticLock(100L)).thenReturn(Optional.of(activeEmergency));
        when(userRepository.findByMobileNumber("+919876543211")).thenReturn(Optional.of(responder));
        when(locationService.getUserCoordinates(2L)).thenReturn(pointAtDistance(25.0));
        when(locationService.isLocationFresh(2L)).thenReturn(true);
        when(emergencyRepository.save(any(Emergency.class))).thenAnswer(i -> i.getArgument(0));

        EmergencyResponse resp = emergencyService.arriveEmergency(100L, "+919876543211");
        assertNotNull(resp);
        assertEquals(2L, resp.getArrivedResponderId());
        assertNotNull(resp.getArrivedAt());
        assertNotNull(resp.getDistanceMeters());
        assertEquals(25.0, resp.getDistanceMeters(), 0.5);
    }

    @Test
    @DisplayName("22. Arrival at 26m -> REJECT (boundary)")
    void arrival_26m_Rejected() {
        User responder = createCitizen(2L, "+919876543211");
        activeEmergency.setStatus(EmergencyStatus.ACCEPTED);
        activeEmergency.addResponder(responder);

        when(emergencyRepository.findByIdWithPessimisticLock(100L)).thenReturn(Optional.of(activeEmergency));
        when(userRepository.findByMobileNumber("+919876543211")).thenReturn(Optional.of(responder));
        when(locationService.getUserCoordinates(2L)).thenReturn(pointAtDistance(26.0));
        when(locationService.isLocationFresh(2L)).thenReturn(true);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> emergencyService.arriveEmergency(100L, "+919876543211"));
        assertTrue(ex.getMessage().contains("Must be within 25 meters"));
    }

    @Test
    @DisplayName("23. Missing arrival coordinates -> REJECT")
    void missingArrivalCoordinates_Rejected() {
        User responder = createCitizen(2L, "+919876543211");
        activeEmergency.setStatus(EmergencyStatus.ACCEPTED);
        activeEmergency.addResponder(responder);

        when(emergencyRepository.findByIdWithPessimisticLock(100L)).thenReturn(Optional.of(activeEmergency));
        when(userRepository.findByMobileNumber("+919876543211")).thenReturn(Optional.of(responder));
        when(locationService.getUserCoordinates(2L)).thenReturn(null);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> emergencyService.arriveEmergency(100L, "+919876543211"));
        assertTrue(ex.getMessage().contains("Live location coordinates not found for responder"));
    }

    @Test
    @DisplayName("24. Stale arrival coordinates -> REJECT")
    void staleArrivalCoordinates_Rejected() {
        User responder = createCitizen(2L, "+919876543211");
        activeEmergency.setStatus(EmergencyStatus.ACCEPTED);
        activeEmergency.addResponder(responder);

        when(emergencyRepository.findByIdWithPessimisticLock(100L)).thenReturn(Optional.of(activeEmergency));
        when(userRepository.findByMobileNumber("+919876543211")).thenReturn(Optional.of(responder));
        when(locationService.getUserCoordinates(2L)).thenReturn(pointAtDistance(10.0));
        when(locationService.isLocationFresh(2L)).thenReturn(false); // Stale

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> emergencyService.arriveEmergency(100L, "+919876543211"));
        assertTrue(ex.getMessage().contains("Responder location is stale"));
    }

    // =========================================================================
    // CONCURRENCY & MULTI-RESPONDER PRESERVATION (25)
    // =========================================================================

    @Test
    @DisplayName("25. Multi-responder acceptance preserved when location passes")
    void multiResponder_AcceptancePreserved() {
        User responder1 = createCitizen(2L, "+919876543211");
        User responder2 = createCitizen(3L, "+919876543212");

        activeEmergency.setStatus(EmergencyStatus.ACCEPTED);
        activeEmergency.setResponder(responder1);
        activeEmergency.addResponder(responder1);

        when(emergencyRepository.findById(100L)).thenReturn(Optional.of(activeEmergency));
        when(userRepository.findByMobileNumber("+919876543212")).thenReturn(Optional.of(responder2));
        when(locationService.getUserCoordinates(3L)).thenReturn(pointAtDistance(150.0));
        when(locationService.isLocationFresh(3L)).thenReturn(true);
        when(emergencyRepository.save(any(Emergency.class))).thenAnswer(i -> i.getArgument(0));

        EmergencyResponse resp = emergencyService.acceptEmergency(100L, "+919876543212");
        assertNotNull(resp);
        assertEquals(EmergencyStatus.ACCEPTED, resp.getStatus());
        assertEquals(2, resp.getResponders().size());
    }
}
