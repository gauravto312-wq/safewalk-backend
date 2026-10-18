package com.safewalk.dto.request;

import com.safewalk.entity.VerificationStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class AdminVerificationRequest {

    @NotNull(message = "Verification status is required")
    private VerificationStatus status;

    @Size(max = 255, message = "Reason cannot exceed 255 characters")
    private String reason;

    public AdminVerificationRequest() {
    }

    public AdminVerificationRequest(VerificationStatus status, String reason) {
        this.status = status;
        this.reason = reason;
    }

    public VerificationStatus getStatus() {
        return status;
    }

    public void setStatus(VerificationStatus status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
