package com.safewalk.dto.request;

import jakarta.validation.constraints.NotBlank;

public class FcmTokenRequest {

    @NotBlank(message = "FCM device token is required")
    private String token;

    private String platform = "ANDROID";

    public FcmTokenRequest() {
    }

    public FcmTokenRequest(String token, String platform) {
        this.token = token;
        this.platform = platform != null ? platform : "ANDROID";
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
}
