package com.safewalk.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class EmergencyContactRequest {

    private String contactIdentifier;
    private Object contactUserId;

    public EmergencyContactRequest() {
    }

    public EmergencyContactRequest(String contactIdentifier) {
        this.contactIdentifier = contactIdentifier;
    }

    public EmergencyContactRequest(Object contactUserId) {
        this.contactUserId = contactUserId;
    }

    public String getContactIdentifier() {
        return contactIdentifier;
    }

    public void setContactIdentifier(String contactIdentifier) {
        this.contactIdentifier = contactIdentifier;
    }

    public Object getContactUserId() {
        return contactUserId;
    }

    public void setContactUserId(Object contactUserId) {
        this.contactUserId = contactUserId;
    }

    @JsonIgnore
    public Long getParsedContactUserId() {
        if (contactUserId != null) {
            if (contactUserId instanceof Number) {
                return ((Number) contactUserId).longValue();
            }
            String str = contactUserId.toString().trim();
            if (str.toUpperCase().startsWith("SW-")) {
                str = str.substring(3).trim();
            }
            try {
                return Long.parseLong(str);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid SafeWalk contact user ID format: " + contactUserId);
            }
        }
        if (contactIdentifier != null && !contactIdentifier.trim().isEmpty()) {
            String trimmed = contactIdentifier.trim();
            if (trimmed.toUpperCase().startsWith("SW-")) {
                trimmed = trimmed.substring(3).trim();
            }
            try {
                return Long.parseLong(trimmed);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid SafeWalk contact user ID format: " + contactIdentifier);
            }
        }
        throw new IllegalArgumentException("SafeWalk Contact User ID or identifier must be provided.");
    }

    public static EmergencyContactRequestBuilder builder() {
        return new EmergencyContactRequestBuilder();
    }

    public static class EmergencyContactRequestBuilder {
        private String contactIdentifier;
        private Object contactUserId;

        public EmergencyContactRequestBuilder contactIdentifier(String contactIdentifier) {
            this.contactIdentifier = contactIdentifier;
            return this;
        }

        public EmergencyContactRequestBuilder contactUserId(Object contactUserId) {
            this.contactUserId = contactUserId;
            return this;
        }

        public EmergencyContactRequest build() {
            EmergencyContactRequest req = new EmergencyContactRequest();
            req.setContactIdentifier(contactIdentifier);
            req.setContactUserId(contactUserId);
            return req;
        }
    }
}
