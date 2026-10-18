package com.safewalk.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "emergency_dispatches",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_emergency_responder", columnNames = {"emergency_id", "responder_id"})
        },
        indexes = {
                @Index(name = "idx_dispatch_emergency", columnList = "emergency_id"),
                @Index(name = "idx_dispatch_responder", columnList = "responder_id"),
                @Index(name = "idx_dispatch_status", columnList = "status")
        }
)
public class EmergencyDispatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "emergency_id", nullable = false)
    private Emergency emergency;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "responder_id", nullable = false)
    private User responder;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DispatchStatus status = DispatchStatus.PENDING;

    @Column(name = "distance_meters")
    private Double distanceMeters;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "fcm_message_id", length = 255)
    private String fcmMessageId;

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    @Column(name = "last_attempt_at")
    private LocalDateTime lastAttemptAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public EmergencyDispatch() {
    }

    public EmergencyDispatch(Long id, Emergency emergency, User responder, DispatchStatus status, Double distanceMeters, LocalDateTime sentAt, String fcmMessageId, int retryCount, LocalDateTime lastAttemptAt, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.emergency = emergency;
        this.responder = responder;
        this.status = status != null ? status : DispatchStatus.PENDING;
        this.distanceMeters = distanceMeters;
        this.sentAt = sentAt;
        this.fcmMessageId = fcmMessageId;
        this.retryCount = retryCount;
        this.lastAttemptAt = lastAttemptAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public EmergencyDispatch(Long id, Emergency emergency, User responder, DispatchStatus status, Double distanceMeters, LocalDateTime sentAt, String fcmMessageId, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this(id, emergency, responder, status, distanceMeters, sentAt, fcmMessageId, 0, null, createdAt, updatedAt);
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = DispatchStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Emergency getEmergency() {
        return emergency;
    }

    public void setEmergency(Emergency emergency) {
        this.emergency = emergency;
    }

    public User getResponder() {
        return responder;
    }

    public void setResponder(User responder) {
        this.responder = responder;
    }

    public DispatchStatus getStatus() {
        return status;
    }

    public void setStatus(DispatchStatus status) {
        this.status = status;
    }

    public Double getDistanceMeters() {
        return distanceMeters;
    }

    public void setDistanceMeters(Double distanceMeters) {
        this.distanceMeters = distanceMeters;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }

    public String getFcmMessageId() {
        return fcmMessageId;
    }

    public void setFcmMessageId(String fcmMessageId) {
        this.fcmMessageId = fcmMessageId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public LocalDateTime getLastAttemptAt() {
        return lastAttemptAt;
    }

    public void setLastAttemptAt(LocalDateTime lastAttemptAt) {
        this.lastAttemptAt = lastAttemptAt;
    }

    public static EmergencyDispatchBuilder builder() {
        return new EmergencyDispatchBuilder();
    }

    public static class EmergencyDispatchBuilder {
        private Long id;
        private Emergency emergency;
        private User responder;
        private DispatchStatus status = DispatchStatus.PENDING;
        private Double distanceMeters;
        private LocalDateTime sentAt;
        private String fcmMessageId;
        private int retryCount = 0;
        private LocalDateTime lastAttemptAt;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public EmergencyDispatchBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public EmergencyDispatchBuilder emergency(Emergency emergency) {
            this.emergency = emergency;
            return this;
        }

        public EmergencyDispatchBuilder responder(User responder) {
            this.responder = responder;
            return this;
        }

        public EmergencyDispatchBuilder status(DispatchStatus status) {
            this.status = status;
            return this;
        }

        public EmergencyDispatchBuilder distanceMeters(Double distanceMeters) {
            this.distanceMeters = distanceMeters;
            return this;
        }

        public EmergencyDispatchBuilder sentAt(LocalDateTime sentAt) {
            this.sentAt = sentAt;
            return this;
        }

        public EmergencyDispatchBuilder fcmMessageId(String fcmMessageId) {
            this.fcmMessageId = fcmMessageId;
            return this;
        }

        public EmergencyDispatchBuilder retryCount(int retryCount) {
            this.retryCount = retryCount;
            return this;
        }

        public EmergencyDispatchBuilder lastAttemptAt(LocalDateTime lastAttemptAt) {
            this.lastAttemptAt = lastAttemptAt;
            return this;
        }

        public EmergencyDispatchBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public EmergencyDispatchBuilder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public EmergencyDispatch build() {
            EmergencyDispatch dispatch = new EmergencyDispatch(id, emergency, responder, status, distanceMeters, sentAt, fcmMessageId, retryCount, lastAttemptAt, createdAt, updatedAt);
            return dispatch;
        }
    }
}
