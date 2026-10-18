package com.safewalk.service;

import com.safewalk.dto.request.LocationUpdateRequest;
import com.safewalk.dto.response.GeocodingResponse;
import com.safewalk.dto.response.NearbyUserResponse;
import com.safewalk.dto.response.RouteDistanceResponse;
import org.springframework.data.geo.Point;

import java.util.List;

public interface LocationService {

    void updateUserLocation(String mobileNumber, LocationUpdateRequest request);

    void removeUserLocation(String mobileNumber);

    List<NearbyUserResponse> findNearbyResponders(Double latitude, Double longitude, Double radiusKm);

    Point getUserCoordinates(Long userId);

    GeocodingResponse reverseGeocode(Double latitude, Double longitude);

    RouteDistanceResponse calculateRouteDistance(Double originLat, Double originLng, Double destLat, Double destLng);

    com.safewalk.dto.response.UserLocationResponse getUserCurrentLocation(String mobileNumber);

    Long getUserLocationTimestamp(Long userId);

    boolean isLocationFresh(Long userId);
}
