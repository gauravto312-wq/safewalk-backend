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
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "emergencies", indexes = {
        @Index(name = "idx_emergencies_requester", columnList = "requester_id"),
        @Index(name = "idx_emergencies_responder", columnList = "responder_id"),
        @Index(name = "idx_emergencies_status", columnList = "status"),
        @Index(name = "idx_emergencies_created_at", columnList = "created_at")
})
public class Emergency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responder_id")
    private User responder;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "emergency_responders",
            joinColumns = @JoinColumn(name = "emergency_id"),
            inverseJoinColumns = @JoinColumn(name = "responder_id")
    )
    private Set<User> responders = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private EmergencyType type = EmergencyType.GENERAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EmergencyStatus status = EmergencyStatus.ACTIVE;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "expired_at")
    private LocalDateTime expiredAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "arrived_responder_id")
    private User arrivedResponder;

    @Column(name = "arrived_at")
    private LocalDateTime arrivedAt;

    @Column(name = "requester_resolve_confirmed", nullable = false)
    private boolean requesterResolveConfirmed = false;

    @Column(name = "responder_resolve_confirmed", nullable = false)
    private boolean responderResolveConfirmed = false;

    public Emergency() {
    }

    public Emergency(Long id, User requester, User responder, EmergencyType type, EmergencyStatus status, Double latitude, Double longitude, LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime acceptedAt, LocalDateTime resolvedAt, LocalDateTime cancelledAt) {
        this(id, requester, responder, type, status, latitude, longitude, createdAt, updatedAt, acceptedAt, resolvedAt, cancelledAt, new HashSet<>());
    }

    public Emergency(Long id, User requester, User responder, EmergencyType type, EmergencyStatus status, Double latitude, Double longitude, LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime acceptedAt, LocalDateTime resolvedAt, LocalDateTime cancelledAt, Set<User> responders) {
        this(id, requester, responder, type, status, latitude, longitude, createdAt, updatedAt, acceptedAt, resolvedAt, cancelledAt, responders, null, null, false, false);
    }

    public Emergency(Long id, User requester, User responder, EmergencyType type, EmergencyStatus status, Double latitude, Double longitude, LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime acceptedAt, LocalDateTime resolvedAt, LocalDateTime cancelledAt, Set<User> responders, User arrivedResponder, LocalDateTime arrivedAt, boolean requesterResolveConfirmed, boolean responderResolveConfirmed) {
        this.id = id;
        this.requester = requester;
        this.responder = responder;
        this.type = type != null ? type : EmergencyType.GENERAL;
        this.status = status != null ? status : EmergencyStatus.ACTIVE;
        this.latitude = latitude;
        this.longitude = longitude;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.acceptedAt = acceptedAt;
        this.resolvedAt = resolvedAt;
        this.cancelledAt = cancelledAt;
        this.responders = responders != null ? responders : new HashSet<>();
        this.arrivedResponder = arrivedResponder;
        this.arrivedAt = arrivedAt;
        this.requesterResolveConfirmed = requesterResolveConfirmed;
        this.responderResolveConfirmed = responderResolveConfirmed;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.type == null) {
            this.type = EmergencyType.GENERAL;
        }
        if (this.status == null) {
            this.status = EmergencyStatus.ACTIVE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getRequester() {
        return requester;
    }

    public void setRequester(User requester) {
        this.requester = requester;
    }

    public EmergencyType getType() {
        return type;
    }

    public void setType(EmergencyType type) {
        this.type = type;
    }

    public EmergencyStatus getStatus() {
        return status;
    }

    public void setStatus(EmergencyStatus status) {
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

    public User getResponder() {
        return responder;
    }

    public void setResponder(User responder) {
        this.responder = responder;
    }

    public LocalDateTime getAcceptedAt() {
        return acceptedAt;
    }

    public void setAcceptedAt(LocalDateTime acceptedAt) {
        this.acceptedAt = acceptedAt;
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

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public LocalDateTime getExpiredAt() {
        return expiredAt;
    }

    public void setExpiredAt(LocalDateTime expiredAt) {
        this.expiredAt = expiredAt;
    }

    public Set<User> getResponders() {
        if (responders == null) {
            responders = new HashSet<>();
        }
        return responders;
    }

    public void setResponders(Set<User> responders) {
        this.responders = responders != null ? responders : new HashSet<>();
    }

    public void addResponder(User responder) {
        if (this.responders == null) {
            this.responders = new HashSet<>();
        }
        if (responder != null) {
            this.responders.add(responder);
        }
    }

    public void removeResponder(User responder) {
        if (this.responders != null && responder != null) {
            this.responders.remove(responder);
        }
    }

    public User getArrivedResponder() {
        return arrivedResponder;
    }

    public void setArrivedResponder(User arrivedResponder) {
        this.arrivedResponder = arrivedResponder;
    }

    public LocalDateTime getArrivedAt() {
        return arrivedAt;
    }

    public void setArrivedAt(LocalDateTime arrivedAt) {
        this.arrivedAt = arrivedAt;
    }

    public boolean isRequesterResolveConfirmed() {
        return requesterResolveConfirmed;
    }

    public void setRequesterResolveConfirmed(boolean requesterResolveConfirmed) {
        this.requesterResolveConfirmed = requesterResolveConfirmed;
    }

    public boolean isResponderResolveConfirmed() {
        return responderResolveConfirmed;
    }

    public void setResponderResolveConfirmed(boolean responderResolveConfirmed) {
        this.responderResolveConfirmed = responderResolveConfirmed;
    }

    // Builder
    public static EmergencyBuilder builder() {
        return new EmergencyBuilder();
    }

    public static class EmergencyBuilder {
        private Long id;
        private User requester;
        private User responder;
        private Set<User> responders = new HashSet<>();
        private EmergencyType type = EmergencyType.GENERAL;
        private EmergencyStatus status = EmergencyStatus.ACTIVE;
        private Double latitude;
        private Double longitude;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private LocalDateTime acceptedAt;
        private LocalDateTime resolvedAt;
        private LocalDateTime cancelledAt;
        private LocalDateTime expiredAt;
        private User arrivedResponder;
        private LocalDateTime arrivedAt;
        private boolean requesterResolveConfirmed = false;
        private boolean responderResolveConfirmed = false;

        public EmergencyBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public EmergencyBuilder requester(User requester) {
            this.requester = requester;
            return this;
        }

        public EmergencyBuilder responder(User responder) {
            this.responder = responder;
            return this;
        }

        public EmergencyBuilder responders(Set<User> responders) {
            this.responders = responders != null ? responders : new HashSet<>();
            return this;
        }

        public EmergencyBuilder addResponder(User responder) {
            if (this.responders == null) {
                this.responders = new HashSet<>();
            }
            if (responder != null) {
                this.responders.add(responder);
            }
            return this;
        }

        public EmergencyBuilder type(EmergencyType type) {
            this.type = type;
            return this;
        }

        public EmergencyBuilder status(EmergencyStatus status) {
            this.status = status;
            return this;
        }

        public EmergencyBuilder latitude(Double latitude) {
            this.latitude = latitude;
            return this;
        }

        public EmergencyBuilder longitude(Double longitude) {
            this.longitude = longitude;
            return this;
        }

        public EmergencyBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public EmergencyBuilder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public EmergencyBuilder acceptedAt(LocalDateTime acceptedAt) {
            this.acceptedAt = acceptedAt;
            return this;
        }

        public EmergencyBuilder resolvedAt(LocalDateTime resolvedAt) {
            this.resolvedAt = resolvedAt;
            return this;
        }

        public EmergencyBuilder cancelledAt(LocalDateTime cancelledAt) {
            this.cancelledAt = cancelledAt;
            return this;
        }

        public EmergencyBuilder expiredAt(LocalDateTime expiredAt) {
            this.expiredAt = expiredAt;
            return this;
        }

        public EmergencyBuilder arrivedResponder(User arrivedResponder) {
            this.arrivedResponder = arrivedResponder;
            return this;
        }

        public EmergencyBuilder arrivedAt(LocalDateTime arrivedAt) {
            this.arrivedAt = arrivedAt;
            return this;
        }

        public EmergencyBuilder requesterResolveConfirmed(boolean requesterResolveConfirmed) {
            this.requesterResolveConfirmed = requesterResolveConfirmed;
            return this;
        }

        public EmergencyBuilder responderResolveConfirmed(boolean responderResolveConfirmed) {
            this.responderResolveConfirmed = responderResolveConfirmed;
            return this;
        }

        public Emergency build() {
            Emergency emergency = new Emergency(id, requester, responder, type, status, latitude, longitude, createdAt, updatedAt, acceptedAt, resolvedAt, cancelledAt, responders, arrivedResponder, arrivedAt, requesterResolveConfirmed, responderResolveConfirmed);
            emergency.setExpiredAt(this.expiredAt);
            return emergency;
        }
    }
}
