package com.safewalk.dto.response;

import com.safewalk.entity.Role;
import com.safewalk.entity.VerificationStatus;

import java.time.LocalDateTime;
import java.util.List;

public class PendingVerificationResponse {

    private Long userId;
    private String name;
    private String mobileNumber;
    private Role role;
    private VerificationStatus verificationStatus;
    private LocalDateTime registeredAt;
    private List<VerificationDocumentResponse> documents;

    public PendingVerificationResponse() {
    }

    public PendingVerificationResponse(Long userId, String name, String mobileNumber, Role role, VerificationStatus verificationStatus, LocalDateTime registeredAt, List<VerificationDocumentResponse> documents) {
        this.userId = userId;
        this.name = name;
        this.mobileNumber = mobileNumber;
        this.role = role;
        this.verificationStatus = verificationStatus;
        this.registeredAt = registeredAt;
        this.documents = documents;
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

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public VerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(VerificationStatus verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public LocalDateTime getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(LocalDateTime registeredAt) {
        this.registeredAt = registeredAt;
    }

    public List<VerificationDocumentResponse> getDocuments() {
        return documents;
    }

    public void setDocuments(List<VerificationDocumentResponse> documents) {
        this.documents = documents;
    }

    public static PendingVerificationResponseBuilder builder() {
        return new PendingVerificationResponseBuilder();
    }

    public static class PendingVerificationResponseBuilder {
        private Long userId;
        private String name;
        private String mobileNumber;
        private Role role;
        private VerificationStatus verificationStatus;
        private LocalDateTime registeredAt;
        private List<VerificationDocumentResponse> documents;

        public PendingVerificationResponseBuilder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public PendingVerificationResponseBuilder name(String name) {
            this.name = name;
            return this;
        }

        public PendingVerificationResponseBuilder mobileNumber(String mobileNumber) {
            this.mobileNumber = mobileNumber;
            return this;
        }

        public PendingVerificationResponseBuilder role(Role role) {
            this.role = role;
            return this;
        }

        public PendingVerificationResponseBuilder verificationStatus(VerificationStatus verificationStatus) {
            this.verificationStatus = verificationStatus;
            return this;
        }

        public PendingVerificationResponseBuilder registeredAt(LocalDateTime registeredAt) {
            this.registeredAt = registeredAt;
            return this;
        }

        public PendingVerificationResponseBuilder documents(List<VerificationDocumentResponse> documents) {
            this.documents = documents;
            return this;
        }

        public PendingVerificationResponse build() {
            return new PendingVerificationResponse(userId, name, mobileNumber, role, verificationStatus, registeredAt, documents);
        }
    }
}
