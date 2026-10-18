package com.safewalk.dto.event;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.UUID;

public class RealTimeEvent {

    private String eventId;
    private String eventType;
    private String message;
    private Object data;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    public RealTimeEvent() {
        this.eventId = UUID.randomUUID().toString();
        this.timestamp = LocalDateTime.now();
    }

    public RealTimeEvent(String eventType, String message, Object data) {
        this();
        this.eventType = eventType;
        this.message = message;
        this.data = data;
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

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public static RealTimeEventBuilder builder() {
        return new RealTimeEventBuilder();
    }

    public static class RealTimeEventBuilder {
        private String eventId = UUID.randomUUID().toString();
        private String eventType;
        private String message;
        private Object data;
        private LocalDateTime timestamp = LocalDateTime.now();

        public RealTimeEventBuilder eventId(String eventId) {
            this.eventId = eventId;
            return this;
        }

        public RealTimeEventBuilder eventType(String eventType) {
            this.eventType = eventType;
            return this;
        }

        public RealTimeEventBuilder message(String message) {
            this.message = message;
            return this;
        }

        public RealTimeEventBuilder data(Object data) {
            this.data = data;
            return this;
        }

        public RealTimeEventBuilder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public RealTimeEvent build() {
            RealTimeEvent event = new RealTimeEvent(eventType, message, data);
            if (eventId != null) event.setEventId(eventId);
            if (timestamp != null) event.setTimestamp(timestamp);
            return event;
        }
    }
}
