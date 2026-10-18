package com.safewalk.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CancelEmergencyRequest {

    @NotBlank(message = "Emergency password is required to cancel an emergency request")
    private String emergencyPassword;

    public CancelEmergencyRequest() {
    }

    public CancelEmergencyRequest(String emergencyPassword) {
        this.emergencyPassword = emergencyPassword;
    }

    public String getEmergencyPassword() {
        return emergencyPassword;
    }

    public void setEmergencyPassword(String emergencyPassword) {
        this.emergencyPassword = emergencyPassword;
    }

    public static CancelEmergencyRequestBuilder builder() {
        return new CancelEmergencyRequestBuilder();
    }

    public static class CancelEmergencyRequestBuilder {
        private String emergencyPassword;

        public CancelEmergencyRequestBuilder emergencyPassword(String emergencyPassword) {
            this.emergencyPassword = emergencyPassword;
            return this;
        }

        public CancelEmergencyRequest build() {
            return new CancelEmergencyRequest(emergencyPassword);
        }
    }
}
