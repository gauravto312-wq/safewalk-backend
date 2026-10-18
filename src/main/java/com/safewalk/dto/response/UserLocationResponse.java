package com.safewalk.dto.response;

public class UserLocationResponse {

    private Long userId;
    private Double latitude;
    private Double longitude;
    private Long timestamp;
    private Boolean fresh;

    public UserLocationResponse() {
    }

    public UserLocationResponse(Long userId, Double latitude, Double longitude, Long timestamp, Boolean fresh) {
        this.userId = userId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
        this.fresh = fresh;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public Boolean getFresh() {
        return fresh;
    }

    public void setFresh(Boolean fresh) {
        this.fresh = fresh;
    }

    public static UserLocationResponseBuilder builder() {
        return new UserLocationResponseBuilder();
    }

    public static class UserLocationResponseBuilder {
        private Long userId;
        private Double latitude;
        private Double longitude;
        private Long timestamp;
        private Boolean fresh;

        public UserLocationResponseBuilder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public UserLocationResponseBuilder latitude(Double latitude) {
            this.latitude = latitude;
            return this;
        }

        public UserLocationResponseBuilder longitude(Double longitude) {
            this.longitude = longitude;
            return this;
        }

        public UserLocationResponseBuilder timestamp(Long timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public UserLocationResponseBuilder fresh(Boolean fresh) {
            this.fresh = fresh;
            return this;
        }

        public UserLocationResponse build() {
            return new UserLocationResponse(userId, latitude, longitude, timestamp, fresh);
        }
    }
}
