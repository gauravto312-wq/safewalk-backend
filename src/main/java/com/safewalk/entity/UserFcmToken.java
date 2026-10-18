package com.safewalk.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
    name = "user_fcm_tokens",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_fcm_token", columnNames = {"token"})
    },
    indexes = {
        @Index(name = "idx_fcm_token", columnList = "token"),
        @Index(name = "idx_user_active", columnList = "user_id, active")
    }
)
public class UserFcmToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token", nullable = false, length = 500)
    private String token;

    @Column(name = "platform", nullable = false, length = 50)
    private String platform = "ANDROID";

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public UserFcmToken() {
    }

    public UserFcmToken(User user, String token, String platform) {
        this.user = user;
        this.token = token;
        this.platform = platform != null ? platform : "ANDROID";
        this.active = true;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
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

    public static UserFcmTokenBuilder builder() {
        return new UserFcmTokenBuilder();
    }

    public static class UserFcmTokenBuilder {
        private Long id;
        private User user;
        private String token;
        private String platform = "ANDROID";
        private boolean active = true;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public UserFcmTokenBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public UserFcmTokenBuilder user(User user) {
            this.user = user;
            return this;
        }

        public UserFcmTokenBuilder token(String token) {
            this.token = token;
            return this;
        }

        public UserFcmTokenBuilder platform(String platform) {
            this.platform = platform;
            return this;
        }

        public UserFcmTokenBuilder active(boolean active) {
            this.active = active;
            return this;
        }

        public UserFcmTokenBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public UserFcmTokenBuilder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public UserFcmToken build() {
            UserFcmToken tokenEntity = new UserFcmToken(user, token, platform);
            if (id != null) tokenEntity.setId(id);
            tokenEntity.setActive(active);
            if (createdAt != null) tokenEntity.setCreatedAt(createdAt);
            if (updatedAt != null) tokenEntity.setUpdatedAt(updatedAt);
            return tokenEntity;
        }
    }
}
