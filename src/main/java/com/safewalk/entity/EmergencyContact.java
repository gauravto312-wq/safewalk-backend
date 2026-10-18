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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(
        name = "emergency_contacts",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_contact", columnNames = {"user_id", "contact_user_id"})
        },
        indexes = {
                @Index(name = "idx_ec_user_id", columnList = "user_id"),
                @Index(name = "idx_ec_contact_id", columnList = "contact_user_id")
        }
)
public class EmergencyContact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contact_user_id", nullable = false)
    private User contactUser;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public EmergencyContact() {
    }

    public EmergencyContact(Long id, User user, User contactUser, LocalDateTime createdAt) {
        this.id = id;
        this.user = user;
        this.contactUser = contactUser;
        this.createdAt = createdAt;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
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

    public User getContactUser() {
        return contactUser;
    }

    public void setContactUser(User contactUser) {
        this.contactUser = contactUser;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EmergencyContact that = (EmergencyContact) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public static EmergencyContactBuilder builder() {
        return new EmergencyContactBuilder();
    }

    public static class EmergencyContactBuilder {
        private Long id;
        private User user;
        private User contactUser;
        private LocalDateTime createdAt;

        public EmergencyContactBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public EmergencyContactBuilder user(User user) {
            this.user = user;
            return this;
        }

        public EmergencyContactBuilder contactUser(User contactUser) {
            this.contactUser = contactUser;
            return this;
        }

        public EmergencyContactBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public EmergencyContact build() {
            return new EmergencyContact(id, user, contactUser, createdAt);
        }
    }
}
