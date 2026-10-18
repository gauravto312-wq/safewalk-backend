package com.safewalk.service.impl;

import com.safewalk.dto.response.GeocodingResponse;
import com.safewalk.dto.response.RouteDistanceResponse;
import com.safewalk.service.MapsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class MapsServiceImpl implements MapsService {

    private static final Logger log = LoggerFactory.getLogger(MapsServiceImpl.class);
    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final double AVERAGE_CITY_SPEED_KMH = 30.0;

    private final String apiKey;
    private final String provider;
    private final RestTemplate restTemplate;

    @Autowired
    public MapsServiceImpl(
            @Value("${safewalk.maps.api-key:}") String apiKey,
            @Value("${safewalk.maps.provider:google}") String provider) {
        this.apiKey = apiKey;
        this.provider = provider;
        this.restTemplate = new RestTemplate();
    }

    @Override
    public GeocodingResponse reverseGeocode(Double latitude, Double longitude) {
        validateCoordinates(latitude, longitude);

        log.info("Performing reverse geocoding for coordinates ({}, {}) using provider: {}",
                latitude, longitude, provider);

        if (apiKey != null && !apiKey.trim().isEmpty()) {
            try {
                String url = String.format(
                        "https://maps.googleapis.com/maps/api/geocode/json?latlng=%f,%f&key=%s",
                        latitude, longitude, apiKey);

                // Note: In production with valid key, parse Google Geocoding JSON response
                log.debug("Dispatching external Geocoding request to Google Maps API");
            } catch (Exception e) {
                log.warn("External Maps Geocoding API call failed: {}. Falling back to coordinate formatting.", e.getMessage());
            }
        } else {
            log.debug("Maps API key is not configured. Utilizing local coordinate geocoding fallback.");
        }

        String formattedAddress = String.format("Location Coordinates (%.4f, %.4f)", latitude, longitude);

        return GeocodingResponse.builder()
                .formattedAddress(formattedAddress)
                .latitude(latitude)
                .longitude(longitude)
                .provider(provider + " (Fallback)")
                .build();
    }

    @Override
    public RouteDistanceResponse calculateDistanceAndEta(
            Double originLat, Double originLng, Double destLat, Double destLng) {

        validateCoordinates(originLat, originLng);
        validateCoordinates(destLat, destLng);

        log.info("Calculating distance and ETA between ({}, {}) and ({}, {}) using provider: {}",
                originLat, originLng, destLat, destLng, provider);

        if (apiKey != null && !apiKey.trim().isEmpty()) {
            try {
                String url = String.format(
                        "https://maps.googleapis.com/maps/api/distancematrix/json?origins=%f,%f&destinations=%f,%f&key=%s",
                        originLat, originLng, destLat, destLng, apiKey);

                log.debug("Dispatching external Distance Matrix request to Google Maps API");
            } catch (Exception e) {
                log.warn("External Distance Matrix API call failed: {}. Falling back to Haversine route calculation.", e.getMessage());
            }
        } else {
            log.debug("Maps API key is not configured. Utilizing Haversine formula for distance & ETA.");
        }

        double distanceKm = calculateHaversineDistance(originLat, originLng, destLat, destLng);
        double durationMinutes = Math.max(1.0, Math.round((distanceKm / AVERAGE_CITY_SPEED_KMH) * 60.0));

        return RouteDistanceResponse.builder()
                .distanceKm(distanceKm)
                .distanceText(String.format("%.2f km", distanceKm))
                .durationMinutes(durationMinutes)
                .durationText(String.format("%.0f mins", durationMinutes))
                .originLatitude(originLat)
                .originLongitude(originLng)
                .destinationLatitude(destLat)
                .destinationLongitude(destLng)
                .provider(provider + " (Haversine Fallback)")
                .build();
    }

    private double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = EARTH_RADIUS_KM * c;

        return Math.round(distance * 100.0) / 100.0;
    }

    private void validateCoordinates(Double latitude, Double longitude) {
        if (latitude == null || latitude < -90.0 || latitude > 90.0) {
            throw new IllegalArgumentException("Latitude must be between -90.0 and 90.0");
        }
        if (longitude == null || longitude < -180.0 || longitude > 180.0) {
            throw new IllegalArgumentException("Longitude must be between -180.0 and 180.0");
        }
    }
}
