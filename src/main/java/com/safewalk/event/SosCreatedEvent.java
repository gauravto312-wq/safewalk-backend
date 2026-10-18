package com.safewalk.event;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.UUID;

public class SosCreatedEvent {

    private String eventId;
    private String eventType;
    private int version;
    private Long emergencyId;
    private Long requesterId;
    private String requesterName;
    private String requesterMobile;
    private String type;
    private String status;
    private Double latitude;
    private Double longitude;
    private String requesterAvatarUrl;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    public SosCreatedEvent() {
        this.eventId = UUID.randomUUID().toString();
        this.eventType = "SOS_CREATED";
        this.version = 1;
        this.timestamp = LocalDateTime.now();
    }

    public SosCreatedEvent(Long emergencyId, Long requesterId, String requesterMobile, String type, String status, Double latitude, Double longitude) {
        this(emergencyId, requesterId, null, requesterMobile, type, status, latitude, longitude);
    }

    public SosCreatedEvent(Long emergencyId, Long requesterId, String requesterName, String requesterMobile, String type, String status, Double latitude, Double longitude) {
        this();
        this.emergencyId = emergencyId;
        this.requesterId = requesterId;
        this.requesterName = requesterName;
        this.requesterMobile = requesterMobile;
        this.type = type;
        this.status = status;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public Long getEmergencyId() {
        return emergencyId;
    }

    public void setEmergencyId(Long emergencyId) {
        this.emergencyId = emergencyId;
    }

    public Long getRequesterId() {
        return requesterId;
    }

    public void setRequesterId(Long requesterId) {
        this.requesterId = requesterId;
    }

    public String getRequesterName() {
        return requesterName;
    }

    public void setRequesterName(String requesterName) {
        this.requesterName = requesterName;
    }

    public String getRequesterMobile() {
        return requesterMobile;
    }

    public void setRequesterMobile(String requesterMobile) {
        this.requesterMobile = requesterMobile;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getRequesterAvatarUrl() {
        return requesterAvatarUrl;
    }

    public void setRequesterAvatarUrl(String requesterAvatarUrl) {
        this.requesterAvatarUrl = requesterAvatarUrl;
    }

    public static SosCreatedEventBuilder builder() {
        return new SosCreatedEventBuilder();
    }

    public static class SosCreatedEventBuilder {
        private String eventId = UUID.randomUUID().toString();
        private String eventType = "SOS_CREATED";
        private int version = 1;
        private Long emergencyId;
        private Long requesterId;
        private String requesterName;
        private String requesterMobile;
        private String requesterAvatarUrl;
        private String type;
        private String status;
        private Double latitude;
        private Double longitude;
        private LocalDateTime timestamp = LocalDateTime.now();

        public SosCreatedEventBuilder requesterAvatarUrl(String requesterAvatarUrl) {
            this.requesterAvatarUrl = requesterAvatarUrl;
            return this;
        }

        public SosCreatedEventBuilder eventId(String eventId) {
            this.eventId = eventId;
            return this;
        }

        public SosCreatedEventBuilder eventType(String eventType) {
            this.eventType = eventType;
            return this;
        }

        public SosCreatedEventBuilder version(int version) {
            this.version = version;
            return this;
        }

        public SosCreatedEventBuilder emergencyId(Long emergencyId) {
            this.emergencyId = emergencyId;
            return this;
        }

        public SosCreatedEventBuilder requesterId(Long requesterId) {
            this.requesterId = requesterId;
            return this;
        }

        public SosCreatedEventBuilder requesterName(String requesterName) {
            this.requesterName = requesterName;
            return this;
        }

        public SosCreatedEventBuilder requesterMobile(String requesterMobile) {
            this.requesterMobile = requesterMobile;
            return this;
        }

        public SosCreatedEventBuilder type(String type) {
            this.type = type;
            return this;
        }

        public SosCreatedEventBuilder status(String status) {
            this.status = status;
            return this;
        }

        public SosCreatedEventBuilder latitude(Double latitude) {
            this.latitude = latitude;
            return this;
        }

        public SosCreatedEventBuilder longitude(Double longitude) {
            this.longitude = longitude;
            return this;
        }

        public SosCreatedEventBuilder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public SosCreatedEvent build() {
            SosCreatedEvent event = new SosCreatedEvent(emergencyId, requesterId, requesterName, requesterMobile, type, status, latitude, longitude);
            if (eventId != null) event.setEventId(eventId);
            if (eventType != null) event.setEventType(eventType);
            event.setVersion(version);
            if (timestamp != null) event.setTimestamp(timestamp);
            if (requesterAvatarUrl != null) event.setRequesterAvatarUrl(requesterAvatarUrl);
            return event;
        }
    }
}
