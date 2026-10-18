package com.safewalk.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public class FcmNotificationRequest {

    private String targetToken;

    @NotBlank(message = "Notification title is required")
    private String title;

    @NotBlank(message = "Notification body is required")
    private String body;

    private Map<String, String> data;

    public FcmNotificationRequest() {
    }

    public FcmNotificationRequest(String targetToken, String title, String body, Map<String, String> data) {
        this.targetToken = targetToken;
        this.title = title;
        this.body = body;
        this.data = data;
    }

    public String getTargetToken() {
        return targetToken;
    }

    public void setTargetToken(String targetToken) {
        this.targetToken = targetToken;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public Map<String, String> getData() {
        return data;
    }

    public void setData(Map<String, String> data) {
        this.data = data;
    }
}
