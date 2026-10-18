package com.safewalk.dto.request;

import com.safewalk.entity.AccountStatus;
import jakarta.validation.constraints.NotNull;

public class AdminUserStatusUpdateRequest {

    @NotNull(message = "Account status is required")
    private AccountStatus status;

    private String reason;

    public AdminUserStatusUpdateRequest() {
    }

    public AdminUserStatusUpdateRequest(AccountStatus status, String reason) {
        this.status = status;
        this.reason = reason;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
