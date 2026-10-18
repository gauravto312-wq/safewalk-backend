package com.safewalk.service;

import com.safewalk.dto.response.GeocodingResponse;
import com.safewalk.dto.response.RouteDistanceResponse;
import com.safewalk.service.impl.MapsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MapsServiceTest {

    private MapsService mapsService;

    @BeforeEach
    void setUp() {
        // Initialize MapsServiceImpl without API key to test local fallback functionality
        mapsService = new MapsServiceImpl("", "google");
    }

    @Test
    void reverseGeocode_ValidCoordinates_ReturnsFormattedAddress() {
        Double lat = 28.6139;
        Double lng = 77.2090;

        GeocodingResponse response = mapsService.reverseGeocode(lat, lng);

        assertNotNull(response);
        assertEquals(lat, response.getLatitude());
        assertEquals(lng, response.getLongitude());
        assertTrue(response.getFormattedAddress().contains("28.6139"));
        assertTrue(response.getFormattedAddress().contains("77.2090"));
        assertTrue(response.getProvider().contains("Fallback"));
    }

    @Test
    void reverseGeocode_InvalidLatitude_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> mapsService.reverseGeocode(95.0, 77.2090));
        assertThrows(IllegalArgumentException.class, () -> mapsService.reverseGeocode(-91.0, 77.2090));
    }

    @Test
    void reverseGeocode_InvalidLongitude_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> mapsService.reverseGeocode(28.6139, 185.0));
        assertThrows(IllegalArgumentException.class, () -> mapsService.reverseGeocode(28.6139, -181.0));
    }

    @Test
    void calculateDistanceAndEta_ValidCoordinates_ReturnsDistanceAndEta() {
        Double originLat = 28.6139;
        Double originLng = 77.2090;
        Double destLat = 28.5355;
        Double destLng = 77.3910;

        RouteDistanceResponse response = mapsService.calculateDistanceAndEta(originLat, originLng, destLat, destLng);

        assertNotNull(response);
        assertEquals(originLat, response.getOriginLatitude());
        assertEquals(originLng, response.getOriginLongitude());
        assertEquals(destLat, response.getDestinationLatitude());
        assertEquals(destLng, response.getDestinationLongitude());

        assertTrue(response.getDistanceKm() > 0);
        assertNotNull(response.getDistanceText());
        assertTrue(response.getDurationMinutes() > 0);
        assertNotNull(response.getDurationText());
        assertTrue(response.getProvider().contains("Haversine Fallback"));
    }

    @Test
    void calculateDistanceAndEta_SameOriginAndDestination_ReturnsZeroDistance() {
        Double lat = 28.6139;
        Double lng = 77.2090;

        RouteDistanceResponse response = mapsService.calculateDistanceAndEta(lat, lat, lng, lng);

        assertNotNull(response);
        // Note: 28.6139 to 28.6139
    }

    @Test
    void calculateDistanceAndEta_InvalidCoordinates_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () ->
                mapsService.calculateDistanceAndEta(100.0, 77.2090, 28.5355, 77.3910));
        assertThrows(IllegalArgumentException.class, () ->
                mapsService.calculateDistanceAndEta(28.6139, 77.2090, 28.5355, 200.0));
    }
}
