package com.safewalk.dto.response;

import java.time.LocalDateTime;

public class AuditLogResponse {

    private Long id;
    private String adminMobile;
    private String action;
    private String targetType;
    private Long targetId;
    private String details;
    private LocalDateTime createdAt;

    public AuditLogResponse() {
    }

    public AuditLogResponse(Long id, String adminMobile, String action, String targetType, Long targetId, String details, LocalDateTime createdAt) {
        this.id = id;
        this.adminMobile = adminMobile;
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.details = details;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAdminMobile() {
        return adminMobile;
    }

    public void setAdminMobile(String adminMobile) {
        this.adminMobile = adminMobile;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public Long getTargetId() {
        return targetId;
    }

    public void setTargetId(Long targetId) {
        this.targetId = targetId;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public static AuditLogResponseBuilder builder() {
        return new AuditLogResponseBuilder();
    }

    public static class AuditLogResponseBuilder {
        private Long id;
        private String adminMobile;
        private String action;
        private String targetType;
        private Long targetId;
        private String details;
        private LocalDateTime createdAt;

        public AuditLogResponseBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public AuditLogResponseBuilder adminMobile(String adminMobile) {
            this.adminMobile = adminMobile;
            return this;
        }

        public AuditLogResponseBuilder action(String action) {
            this.action = action;
            return this;
        }

        public AuditLogResponseBuilder targetType(String targetType) {
            this.targetType = targetType;
            return this;
        }

        public AuditLogResponseBuilder targetId(Long targetId) {
            this.targetId = targetId;
            return this;
        }

        public AuditLogResponseBuilder details(String details) {
            this.details = details;
            return this;
        }

        public AuditLogResponseBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public AuditLogResponse build() {
            return new AuditLogResponse(id, adminMobile, action, targetType, targetId, details, createdAt);
        }
    }
}
