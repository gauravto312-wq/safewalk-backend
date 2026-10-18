package com.safewalk.dto.response;

import com.safewalk.entity.Role;

import java.time.LocalDateTime;

public class EmergencyContactResponse {

    private Long id;
    private Long userId;
    private Long contactUserId;
    private String contactFormattedId;
    private String contactName;
    private String contactMobile;
    private Role contactRole;
    private LocalDateTime createdAt;

    public EmergencyContactResponse() {
    }

    public EmergencyContactResponse(Long id, Long userId, Long contactUserId, String contactFormattedId, String contactName, String contactMobile, Role contactRole, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.contactUserId = contactUserId;
        this.contactFormattedId = contactFormattedId;
        this.contactName = contactName;
        this.contactMobile = contactMobile;
        this.contactRole = contactRole;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getContactUserId() {
        return contactUserId;
    }

    public void setContactUserId(Long contactUserId) {
        this.contactUserId = contactUserId;
    }

    public String getContactFormattedId() {
        return contactFormattedId;
    }

    public void setContactFormattedId(String contactFormattedId) {
        this.contactFormattedId = contactFormattedId;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getContactMobile() {
        return contactMobile;
    }

    public void setContactMobile(String contactMobile) {
        this.contactMobile = contactMobile;
    }

    public Role getContactRole() {
        return contactRole;
    }

    public void setContactRole(Role contactRole) {
        this.contactRole = contactRole;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public static EmergencyContactResponseBuilder builder() {
        return new EmergencyContactResponseBuilder();
    }

    public static class EmergencyContactResponseBuilder {
        private Long id;
        private Long userId;
        private Long contactUserId;
        private String contactFormattedId;
        private String contactName;
        private String contactMobile;
        private Role contactRole;
        private LocalDateTime createdAt;

        public EmergencyContactResponseBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public EmergencyContactResponseBuilder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public EmergencyContactResponseBuilder contactUserId(Long contactUserId) {
            this.contactUserId = contactUserId;
            return this;
        }

        public EmergencyContactResponseBuilder contactFormattedId(String contactFormattedId) {
            this.contactFormattedId = contactFormattedId;
            return this;
        }

        public EmergencyContactResponseBuilder contactName(String contactName) {
            this.contactName = contactName;
            return this;
        }

        public EmergencyContactResponseBuilder contactMobile(String contactMobile) {
            this.contactMobile = contactMobile;
            return this;
        }

        public EmergencyContactResponseBuilder contactRole(Role contactRole) {
            this.contactRole = contactRole;
            return this;
        }

        public EmergencyContactResponseBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public EmergencyContactResponse build() {
            return new EmergencyContactResponse(id, userId, contactUserId, contactFormattedId, contactName, contactMobile, contactRole, createdAt);
        }
    }
}
