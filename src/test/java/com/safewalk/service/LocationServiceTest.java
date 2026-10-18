package com.safewalk.service;

import com.safewalk.dto.request.LocationUpdateRequest;
import com.safewalk.dto.response.NearbyUserResponse;
import com.safewalk.entity.AccountStatus;
import com.safewalk.entity.Role;
import com.safewalk.entity.User;
import com.safewalk.exception.ResourceNotFoundException;
import com.safewalk.repository.UserRepository;
import com.safewalk.service.impl.LocationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocationServiceTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private GeoOperations<String, String> geoOperations;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MapsService mapsService;

    private LocationServiceImpl locationService;

    private User activeUser;
    private User deactivatedUser;

    @BeforeEach
    void setUp() {
        locationService = new LocationServiceImpl(stringRedisTemplate, userRepository, mapsService, 2.0);

        activeUser = User.builder()
                .id(1L)
                .name("Alice User")
                .mobileNumber("+919876543210")
                .role(Role.USER)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        deactivatedUser = User.builder()
                .id(2L)
                .name("Bob Suspended")
                .mobileNumber("+919876543211")
                .role(Role.USER)
                .accountStatus(AccountStatus.DEACTIVATED)
                .build();
    }

    @Test
    void updateUserLocation_Success() {
        when(userRepository.findByMobileNumber("+919876543210")).thenReturn(Optional.of(activeUser));
        when(stringRedisTemplate.opsForGeo()).thenReturn(geoOperations);

        LocationUpdateRequest request = new LocationUpdateRequest(28.6139, 77.2090);
        locationService.updateUserLocation("+919876543210", request);

        ArgumentCaptor<Point> pointCaptor = ArgumentCaptor.forClass(Point.class);
        verify(geoOperations).add(eq("safewalk:users:locations"), pointCaptor.capture(), eq("1"));

        Point point = pointCaptor.getValue();
        assertEquals(77.2090, point.getX());
        assertEquals(28.6139, point.getY());
    }

    @Test
    void updateUserLocation_SavesCoordinatesAndTimestamp() {
        when(userRepository.findByMobileNumber("+919876543210")).thenReturn(Optional.of(activeUser));
        when(stringRedisTemplate.opsForGeo()).thenReturn(geoOperations);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        long timestamp = 1700000000000L;
        LocationUpdateRequest request = new LocationUpdateRequest(28.6139, 77.2090, 5.0, 1.2, 90.0, timestamp);
        locationService.updateUserLocation("+919876543210", request);

        verify(geoOperations).add(eq("safewalk:users:locations"), any(Point.class), eq("1"));
        verify(valueOperations).set("safewalk:users:location_timestamp:1", String.valueOf(timestamp));
    }

    @Test
    void isLocationFresh_ReturnsTrueForRecentAndFalseForStale() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        long recentTimestamp = System.currentTimeMillis() - 60_000L;
        when(valueOperations.get("safewalk:users:location_timestamp:1")).thenReturn(String.valueOf(recentTimestamp));
        assertTrue(locationService.isLocationFresh(1L));

        long staleTimestamp = System.currentTimeMillis() - 600_000L;
        when(valueOperations.get("safewalk:users:location_timestamp:1")).thenReturn(String.valueOf(staleTimestamp));
        org.junit.jupiter.api.Assertions.assertFalse(locationService.isLocationFresh(1L));
    }

    @Test
    void getUserCurrentLocation_ReturnsCoordinatesAndFreshness() {
        when(userRepository.findByMobileNumber("+919876543210")).thenReturn(Optional.of(activeUser));
        when(stringRedisTemplate.opsForGeo()).thenReturn(geoOperations);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        Point point = new Point(77.2090, 28.6139);
        when(geoOperations.position("safewalk:users:locations", "1")).thenReturn(Collections.singletonList(point));
        long now = System.currentTimeMillis();
        when(valueOperations.get("safewalk:users:location_timestamp:1")).thenReturn(String.valueOf(now));

        com.safewalk.dto.response.UserLocationResponse response = locationService.getUserCurrentLocation("+919876543210");
        assertNotNull(response);
        assertEquals(1L, response.getUserId());
        assertEquals(28.6139, response.getLatitude());
        assertEquals(77.2090, response.getLongitude());
        assertTrue(response.getFresh());
    }

    @Test
    void updateUserLocation_InvalidLatitude_ThrowsException() {
        LocationUpdateRequest request = new LocationUpdateRequest(95.0, 77.2090);
        assertThrows(IllegalArgumentException.class, () ->
                locationService.updateUserLocation("+919876543210", request));
    }

    @Test
    void updateUserLocation_InvalidLongitude_ThrowsException() {
        LocationUpdateRequest request = new LocationUpdateRequest(28.6139, -190.0);
        assertThrows(IllegalArgumentException.class, () ->
                locationService.updateUserLocation("+919876543210", request));
    }

    @Test
    void updateUserLocation_UserNotFound_ThrowsException() {
        when(userRepository.findByMobileNumber("+919876543210")).thenReturn(Optional.empty());
        LocationUpdateRequest request = new LocationUpdateRequest(28.6139, 77.2090);

        assertThrows(ResourceNotFoundException.class, () ->
                locationService.updateUserLocation("+919876543210", request));
    }

    @Test
    void removeUserLocation_Success() {
        when(userRepository.findByMobileNumber("+919876543210")).thenReturn(Optional.of(activeUser));
        when(stringRedisTemplate.opsForGeo()).thenReturn(geoOperations);

        locationService.removeUserLocation("+919876543210");

        verify(geoOperations).remove("safewalk:users:locations", "1");
    }

    @Test
    void removeUserLocation_UserNotFound_ThrowsException() {
        when(userRepository.findByMobileNumber("+919876543210")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                locationService.removeUserLocation("+919876543210"));
    }

    @Test
    void findNearbyResponders_Success_FiltersOutDeactivatedUsers() {
        when(stringRedisTemplate.opsForGeo()).thenReturn(geoOperations);

        Point activeUserPoint = new Point(77.2095, 28.6140);
        Point deactivatedUserPoint = new Point(77.2100, 28.6150);

        RedisGeoCommands.GeoLocation<String> location1 = new RedisGeoCommands.GeoLocation<>("1", activeUserPoint);
        RedisGeoCommands.GeoLocation<String> location2 = new RedisGeoCommands.GeoLocation<>("2", deactivatedUserPoint);

        GeoResult<RedisGeoCommands.GeoLocation<String>> geoResult1 = new GeoResult<>(location1, new Distance(0.5, Metrics.KILOMETERS));
        GeoResult<RedisGeoCommands.GeoLocation<String>> geoResult2 = new GeoResult<>(location2, new Distance(1.2, Metrics.KILOMETERS));

        GeoResults<RedisGeoCommands.GeoLocation<String>> geoResults = new GeoResults<>(Arrays.asList(geoResult1, geoResult2));

        when(geoOperations.search(eq("safewalk:users:locations"), any(GeoReference.class), any(Distance.class), any(RedisGeoCommands.GeoSearchCommandArgs.class)))
                .thenReturn(geoResults);

        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
        when(userRepository.findById(2L)).thenReturn(Optional.of(deactivatedUser));

        List<NearbyUserResponse> responders = locationService.findNearbyResponders(28.6139, 77.2090, 2.0);

        assertNotNull(responders);
        assertEquals(1, responders.size());

        NearbyUserResponse responder = responders.get(0);
        assertEquals(1L, responder.getUserId());
        assertEquals("Alice User", responder.getName());
        assertEquals("+919876543210", responder.getMobileNumber());
        assertEquals(0.5, responder.getDistanceKm());
        assertEquals(AccountStatus.ACTIVE, responder.getAccountStatus());
    }

    @Test
    void findNearbyResponders_InvalidCoordinates_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () ->
                locationService.findNearbyResponders(null, 77.2090, 2.0));
        assertThrows(IllegalArgumentException.class, () ->
                locationService.findNearbyResponders(100.0, 77.2090, 2.0));
        assertThrows(IllegalArgumentException.class, () ->
                locationService.findNearbyResponders(28.6139, -200.0, 2.0));
    }

    @Test
    void findNearbyResponders_InvalidRadius_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () ->
                locationService.findNearbyResponders(28.6139, 77.2090, -1.0));
    }

    @Test
    void findNearbyResponders_NoNearbyUsers_ReturnsEmptyList() {
        when(stringRedisTemplate.opsForGeo()).thenReturn(geoOperations);
        when(geoOperations.search(eq("safewalk:users:locations"), any(GeoReference.class), any(Distance.class), any(RedisGeoCommands.GeoSearchCommandArgs.class)))
                .thenReturn(new GeoResults<>(Collections.emptyList()));

        List<NearbyUserResponse> responders = locationService.findNearbyResponders(28.6139, 77.2090, 2.0);

        assertNotNull(responders);
        assertTrue(responders.isEmpty());
    }
}
