package com.safewalk.dto.request;

import jakarta.validation.constraints.NotNull;

public class PoliceVerificationRequest {

    @NotNull(message = "Verified status is required")
    private Boolean verified;

    private String reason;

    public PoliceVerificationRequest() {
    }

    public PoliceVerificationRequest(Boolean verified) {
        this.verified = verified;
    }

    public PoliceVerificationRequest(Boolean verified, String reason) {
        this.verified = verified;
        this.reason = reason;
    }

    public Boolean getVerified() {
        return verified;
    }

    public boolean isVerified() {
        return verified != null && verified;
    }

    public void setVerified(Boolean verified) {
        this.verified = verified;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public static PoliceVerificationRequestBuilder builder() {
        return new PoliceVerificationRequestBuilder();
    }

    public static class PoliceVerificationRequestBuilder {
        private Boolean verified;
        private String reason;

        public PoliceVerificationRequestBuilder verified(Boolean verified) {
            this.verified = verified;
            return this;
        }

        public PoliceVerificationRequestBuilder reason(String reason) {
            this.reason = reason;
            return this;
        }

        public PoliceVerificationRequest build() {
            return new PoliceVerificationRequest(verified, reason);
        }
    }
}
