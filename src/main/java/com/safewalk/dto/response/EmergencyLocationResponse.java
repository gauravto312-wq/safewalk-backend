package com.safewalk.dto.response;

import com.safewalk.entity.EmergencyStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class EmergencyLocationResponse {

    private Long emergencyId;
    private EmergencyStatus status;
    private ParticipantLocation requester;
    private List<ParticipantLocation> responders = new ArrayList<>();

    public EmergencyLocationResponse() {
    }

    public EmergencyLocationResponse(Long emergencyId, EmergencyStatus status, ParticipantLocation requester, List<ParticipantLocation> responders) {
        this.emergencyId = emergencyId;
        this.status = status;
        this.requester = requester;
        this.responders = responders != null ? responders : new ArrayList<>();
    }

    public Long getEmergencyId() {
        return emergencyId;
    }

    public void setEmergencyId(Long emergencyId) {
        this.emergencyId = emergencyId;
    }

    public EmergencyStatus getStatus() {
        return status;
    }

    public void setStatus(EmergencyStatus status) {
        this.status = status;
    }

    public ParticipantLocation getRequester() {
        return requester;
    }

    public void setRequester(ParticipantLocation requester) {
        this.requester = requester;
    }

    public List<ParticipantLocation> getResponders() {
        return responders;
    }

    public void setResponders(List<ParticipantLocation> responders) {
        this.responders = responders != null ? responders : new ArrayList<>();
    }

    public static EmergencyLocationResponseBuilder builder() {
        return new EmergencyLocationResponseBuilder();
    }

    public static class EmergencyLocationResponseBuilder {
        private Long emergencyId;
        private EmergencyStatus status;
        private ParticipantLocation requester;
        private List<ParticipantLocation> responders = new ArrayList<>();

        public EmergencyLocationResponseBuilder emergencyId(Long emergencyId) {
            this.emergencyId = emergencyId;
            return this;
        }

        public EmergencyLocationResponseBuilder status(EmergencyStatus status) {
            this.status = status;
            return this;
        }

        public EmergencyLocationResponseBuilder requester(ParticipantLocation requester) {
            this.requester = requester;
            return this;
        }

        public EmergencyLocationResponseBuilder responders(List<ParticipantLocation> responders) {
            this.responders = responders != null ? responders : new ArrayList<>();
            return this;
        }

        public EmergencyLocationResponse build() {
            return new EmergencyLocationResponse(emergencyId, status, requester, responders);
        }
    }

    public static class ParticipantLocation {
        private Long userId;
        private String name;
        private String mobileNumber;
        private Double latitude;
        private Double longitude;
        private Double distanceKm;
        private LocalDateTime updatedAt;

        public ParticipantLocation() {
        }

        public ParticipantLocation(Long userId, String name, String mobileNumber, Double latitude, Double longitude, Double distanceKm, LocalDateTime updatedAt) {
            this.userId = userId;
            this.name = name;
            this.mobileNumber = mobileNumber;
            this.latitude = latitude;
            this.longitude = longitude;
            this.distanceKm = distanceKm;
            this.updatedAt = updatedAt;
        }

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getMobileNumber() {
            return mobileNumber;
        }

        public void setMobileNumber(String mobileNumber) {
            this.mobileNumber = mobileNumber;
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

        public Double getDistanceKm() {
            return distanceKm;
        }

        public void setDistanceKm(Double distanceKm) {
            this.distanceKm = distanceKm;
        }

        public LocalDateTime getUpdatedAt() {
            return updatedAt;
        }

        public void setUpdatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
        }

        public static ParticipantLocationBuilder builder() {
            return new ParticipantLocationBuilder();
        }

        public static class ParticipantLocationBuilder {
            private Long userId;
            private String name;
            private String mobileNumber;
            private Double latitude;
            private Double longitude;
            private Double distanceKm;
            private LocalDateTime updatedAt;

            public ParticipantLocationBuilder userId(Long userId) {
                this.userId = userId;
                return this;
            }

            public ParticipantLocationBuilder name(String name) {
                this.name = name;
                return this;
            }

            public ParticipantLocationBuilder mobileNumber(String mobileNumber) {
                this.mobileNumber = mobileNumber;
                return this;
            }

            public ParticipantLocationBuilder latitude(Double latitude) {
                this.latitude = latitude;
                return this;
            }

            public ParticipantLocationBuilder longitude(Double longitude) {
                this.longitude = longitude;
                return this;
            }

            public ParticipantLocationBuilder distanceKm(Double distanceKm) {
                this.distanceKm = distanceKm;
                return this;
            }

            public ParticipantLocationBuilder updatedAt(LocalDateTime updatedAt) {
                this.updatedAt = updatedAt;
                return this;
            }

            public ParticipantLocation build() {
                return new ParticipantLocation(userId, name, mobileNumber, latitude, longitude, distanceKm, updatedAt);
            }
        }
    }
}
