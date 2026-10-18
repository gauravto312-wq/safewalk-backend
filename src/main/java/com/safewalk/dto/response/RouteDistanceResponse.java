package com.safewalk.dto.response;

public class RouteDistanceResponse {

    private Double distanceKm;
    private String distanceText;
    private Double durationMinutes;
    private String durationText;
    private Double originLatitude;
    private Double originLongitude;
    private Double destinationLatitude;
    private Double destinationLongitude;
    private String provider;

    public RouteDistanceResponse() {
    }

    public RouteDistanceResponse(Double distanceKm, String distanceText, Double durationMinutes, String durationText, Double originLatitude, Double originLongitude, Double destinationLatitude, Double destinationLongitude, String provider) {
        this.distanceKm = distanceKm;
        this.distanceText = distanceText;
        this.durationMinutes = durationMinutes;
        this.durationText = durationText;
        this.originLatitude = originLatitude;
        this.originLongitude = originLongitude;
        this.destinationLatitude = destinationLatitude;
        this.destinationLongitude = destinationLongitude;
        this.provider = provider;
    }

    public Double getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(Double distanceKm) {
        this.distanceKm = distanceKm;
    }

    public String getDistanceText() {
        return distanceText;
    }

    public void setDistanceText(String distanceText) {
        this.distanceText = distanceText;
    }

    public Double getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Double durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public String getDurationText() {
        return durationText;
    }

    public void setDurationText(String durationText) {
        this.durationText = durationText;
    }

    public Double getOriginLatitude() {
        return originLatitude;
    }

    public void setOriginLatitude(Double originLatitude) {
        this.originLatitude = originLatitude;
    }

    public Double getOriginLongitude() {
        return originLongitude;
    }

    public void setOriginLongitude(Double originLongitude) {
        this.originLongitude = originLongitude;
    }

    public Double getDestinationLatitude() {
        return destinationLatitude;
    }

    public void setDestinationLatitude(Double destinationLatitude) {
        this.destinationLatitude = destinationLatitude;
    }

    public Double getDestinationLongitude() {
        return destinationLongitude;
    }

    public void setDestinationLongitude(Double destinationLongitude) {
        this.destinationLongitude = destinationLongitude;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public static RouteDistanceResponseBuilder builder() {
        return new RouteDistanceResponseBuilder();
    }

    public static class RouteDistanceResponseBuilder {
        private Double distanceKm;
        private String distanceText;
        private Double durationMinutes;
        private String durationText;
        private Double originLatitude;
        private Double originLongitude;
        private Double destinationLatitude;
        private Double destinationLongitude;
        private String provider;

        public RouteDistanceResponseBuilder distanceKm(Double distanceKm) {
            this.distanceKm = distanceKm;
            return this;
        }

        public RouteDistanceResponseBuilder distanceText(String distanceText) {
            this.distanceText = distanceText;
            return this;
        }

        public RouteDistanceResponseBuilder durationMinutes(Double durationMinutes) {
            this.durationMinutes = durationMinutes;
            return this;
        }

        public RouteDistanceResponseBuilder durationText(String durationText) {
            this.durationText = durationText;
            return this;
        }

        public RouteDistanceResponseBuilder originLatitude(Double originLatitude) {
            this.originLatitude = originLatitude;
            return this;
        }

        public RouteDistanceResponseBuilder originLongitude(Double originLongitude) {
            this.originLongitude = originLongitude;
            return this;
        }

        public RouteDistanceResponseBuilder destinationLatitude(Double destinationLatitude) {
            this.destinationLatitude = destinationLatitude;
            return this;
        }

        public RouteDistanceResponseBuilder destinationLongitude(Double destinationLongitude) {
            this.destinationLongitude = destinationLongitude;
            return this;
        }

        public RouteDistanceResponseBuilder provider(String provider) {
            this.provider = provider;
            return this;
        }

        public RouteDistanceResponse build() {
            return new RouteDistanceResponse(distanceKm, distanceText, durationMinutes, durationText, originLatitude, originLongitude, destinationLatitude, destinationLongitude, provider);
        }
    }
}
