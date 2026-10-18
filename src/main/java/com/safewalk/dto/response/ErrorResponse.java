package com.safewalk.dto.response;

import java.time.LocalDateTime;
import java.util.Map;

public class ErrorResponse {

    private boolean success = false;
    private String message;
    private Map<String, String> errors;
    private String path;
    private LocalDateTime timestamp;

    public ErrorResponse() {
        this.success = false;
        this.timestamp = LocalDateTime.now();
    }

    public ErrorResponse(boolean success, String message, Map<String, String> errors, String path, LocalDateTime timestamp) {
        this.success = success;
        this.message = message;
        this.errors = errors;
        this.path = path;
        this.timestamp = timestamp != null ? timestamp : LocalDateTime.now();
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Map<String, String> getErrors() {
        return errors;
    }

    public void setErrors(Map<String, String> errors) {
        this.errors = errors;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public static ErrorResponse of(String message, String path) {
        return ErrorResponse.builder()
                .success(false)
                .message(message)
                .path(path)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static ErrorResponse of(String message, Map<String, String> errors, String path) {
        return ErrorResponse.builder()
                .success(false)
                .message(message)
                .errors(errors)
                .path(path)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static ErrorResponseBuilder builder() {
        return new ErrorResponseBuilder();
    }

    public static class ErrorResponseBuilder {
        private boolean success = false;
        private String message;
        private Map<String, String> errors;
        private String path;
        private LocalDateTime timestamp = LocalDateTime.now();

        public ErrorResponseBuilder success(boolean success) {
            this.success = success;
            return this;
        }

        public ErrorResponseBuilder message(String message) {
            this.message = message;
            return this;
        }

        public ErrorResponseBuilder errors(Map<String, String> errors) {
            this.errors = errors;
            return this;
        }

        public ErrorResponseBuilder path(String path) {
            this.path = path;
            return this;
        }

        public ErrorResponseBuilder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public ErrorResponse build() {
            return new ErrorResponse(success, message, errors, path, timestamp);
        }
    }
}
