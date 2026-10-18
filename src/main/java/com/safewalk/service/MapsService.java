package com.safewalk.service;

import com.safewalk.dto.response.GeocodingResponse;
import com.safewalk.dto.response.RouteDistanceResponse;

public interface MapsService {

    GeocodingResponse reverseGeocode(Double latitude, Double longitude);

    RouteDistanceResponse calculateDistanceAndEta(Double originLat, Double originLng, Double destLat, Double destLng);
}
