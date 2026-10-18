package com.safewalk.service;

import com.safewalk.dto.response.NearbyUserResponse;
import com.safewalk.entity.AccountStatus;
import com.safewalk.entity.Role;
import com.safewalk.entity.User;
import com.safewalk.repository.UserRepository;
import com.safewalk.service.impl.LocationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocationServiceFeatureTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private GeoOperations<String, String> geoOperations;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MapsService mapsService;

    private LocationServiceImpl locationService;

    @BeforeEach
    void setUp() {
        locationService = new LocationServiceImpl(stringRedisTemplate, userRepository, mapsService, 2.0);
    }

    @Test
    void findNearbyResponders_NormalUsers_Enforces50mExclusionZone() {
        when(stringRedisTemplate.opsForGeo()).thenReturn(geoOperations);

        // User 1: 20 meters away (0.02 km) -> Within 50m exclusion zone -> MUST BE EXCLUDED
        User userAt20m = User.builder().id(1L).name("User 20m").mobileNumber("+911111111111")
                .role(Role.USER).accountStatus(AccountStatus.ACTIVE).build();

        // User 2: Exactly 50 meters away (0.05 km) -> <= 50m -> MUST BE EXCLUDED
        User userAt50m = User.builder().id(2L).name("User 50m").mobileNumber("+912222222222")
                .role(Role.USER).accountStatus(AccountStatus.ACTIVE).build();

        // User 3: 100 meters away (0.10 km) -> > 50m and <= 2km -> MUST BE INCLUDED
        User userAt100m = User.builder().id(3L).name("User 100m").mobileNumber("+913333333333")
                .role(Role.USER).accountStatus(AccountStatus.ACTIVE).build();

        // User 4: 1.8 km away (1.80 km) -> > 50m and <= 2km -> MUST BE INCLUDED
        User userAt1800m = User.builder().id(4L).name("User 1800m").mobileNumber("+914444444444")
                .role(Role.USER).accountStatus(AccountStatus.ACTIVE).build();

        // User 5: 2.5 km away (2.50 km) -> > 2km -> MUST BE EXCLUDED
        User userAt2500m = User.builder().id(5L).name("User 2500m").mobileNumber("+915555555555")
                .role(Role.USER).accountStatus(AccountStatus.ACTIVE).build();

        GeoResults<RedisGeoCommands.GeoLocation<String>> geoResults = new GeoResults<>(Arrays.asList(
                new GeoResult<>(new RedisGeoCommands.GeoLocation<>("1", new Point(77.209, 28.613)), new Distance(0.02, Metrics.KILOMETERS)),
                new GeoResult<>(new RedisGeoCommands.GeoLocation<>("2", new Point(77.209, 28.613)), new Distance(0.05, Metrics.KILOMETERS)),
                new GeoResult<>(new RedisGeoCommands.GeoLocation<>("3", new Point(77.209, 28.613)), new Distance(0.10, Metrics.KILOMETERS)),
                new GeoResult<>(new RedisGeoCommands.GeoLocation<>("4", new Point(77.209, 28.613)), new Distance(1.80, Metrics.KILOMETERS)),
                new GeoResult<>(new RedisGeoCommands.GeoLocation<>("5", new Point(77.209, 28.613)), new Distance(2.50, Metrics.KILOMETERS))
        ));

        when(geoOperations.search(eq("safewalk:users:locations"), any(GeoReference.class), any(Distance.class), any(RedisGeoCommands.GeoSearchCommandArgs.class))).thenReturn(geoResults);
        when(userRepository.findById(1L)).thenReturn(Optional.of(userAt20m));
        when(userRepository.findById(2L)).thenReturn(Optional.of(userAt50m));
        when(userRepository.findById(3L)).thenReturn(Optional.of(userAt100m));
        when(userRepository.findById(4L)).thenReturn(Optional.of(userAt1800m));
        when(userRepository.findById(5L)).thenReturn(Optional.of(userAt2500m));

        List<NearbyUserResponse> responses = locationService.findNearbyResponders(28.6139, 77.2090, 2.0);

        assertNotNull(responses);
        assertEquals(2, responses.size(), "Only users at 100m and 1800m should be discovered");

        List<Long> discoveredIds = responses.stream().map(NearbyUserResponse::getUserId).toList();
        assertTrue(discoveredIds.contains(3L));
        assertTrue(discoveredIds.contains(4L));
        assertFalse(discoveredIds.contains(1L), "User within 20m must be excluded");
        assertFalse(discoveredIds.contains(2L), "User at 50m must be excluded");
        assertFalse(discoveredIds.contains(5L), "User beyond 2km must be excluded");
    }

    @Test
    void findNearbyResponders_VerifiedPolice_50mExclusionDoesNotApply() {
        when(stringRedisTemplate.opsForGeo()).thenReturn(geoOperations);

        // Police 1: 15 meters away (0.015 km), verified -> MUST BE INCLUDED (50m exclusion does NOT apply to verified police)
        User verifiedPoliceNearby = User.builder().id(10L).name("Officer Singham").mobileNumber("+911010101010")
                .role(Role.POLICE).policeVerified(true).accountStatus(AccountStatus.ACTIVE).build();

        // Police 2: 500 meters away (0.50 km), unverified -> MUST BE EXCLUDED
        User unverifiedPolice = User.builder().id(11L).name("Unverified Cop").mobileNumber("+911111111111")
                .role(Role.POLICE).policeVerified(false).accountStatus(AccountStatus.ACTIVE).build();

        // Police 3: 1.2 km away (1.20 km), verified -> MUST BE INCLUDED
        User verifiedPoliceDistant = User.builder().id(12L).name("Officer Kabir").mobileNumber("+911212121212")
                .role(Role.POLICE).policeVerified(true).accountStatus(AccountStatus.ACTIVE).build();

        GeoResults<RedisGeoCommands.GeoLocation<String>> geoResults = new GeoResults<>(Arrays.asList(
                new GeoResult<>(new RedisGeoCommands.GeoLocation<>("10", new Point(77.209, 28.613)), new Distance(0.015, Metrics.KILOMETERS)),
                new GeoResult<>(new RedisGeoCommands.GeoLocation<>("11", new Point(77.209, 28.613)), new Distance(0.50, Metrics.KILOMETERS)),
                new GeoResult<>(new RedisGeoCommands.GeoLocation<>("12", new Point(77.209, 28.613)), new Distance(1.20, Metrics.KILOMETERS))
        ));

        when(geoOperations.search(eq("safewalk:users:locations"), any(GeoReference.class), any(Distance.class), any(RedisGeoCommands.GeoSearchCommandArgs.class))).thenReturn(geoResults);
        when(userRepository.findById(10L)).thenReturn(Optional.of(verifiedPoliceNearby));
        when(userRepository.findById(11L)).thenReturn(Optional.of(unverifiedPolice));
        when(userRepository.findById(12L)).thenReturn(Optional.of(verifiedPoliceDistant));

        List<NearbyUserResponse> responses = locationService.findNearbyResponders(28.6139, 77.2090, 2.0);

        assertNotNull(responses);
        assertEquals(2, responses.size(), "Verified police at 15m and 1.2km must be discovered; unverified must be excluded");

        List<Long> discoveredIds = responses.stream().map(NearbyUserResponse::getUserId).toList();
        assertTrue(discoveredIds.contains(10L), "Verified police within 50m must be included");
        assertTrue(discoveredIds.contains(12L), "Verified police at 1.2km must be included");
        assertFalse(discoveredIds.contains(11L), "Unverified police must be excluded");
    }
}
