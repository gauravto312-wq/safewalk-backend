package com.safewalk.dto.request;

import com.safewalk.entity.EmergencyType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateEmergencyRequest {

    private EmergencyType type = EmergencyType.GENERAL;

    @DecimalMin(value = "-90.0", message = "Latitude must be greater than or equal to -90.0")
    @DecimalMax(value = "90.0", message = "Latitude must be less than or equal to 90.0")
    private Double latitude;

    @DecimalMin(value = "-180.0", message = "Longitude must be greater than or equal to -180.0")
    @DecimalMax(value = "180.0", message = "Longitude must be less than or equal to 180.0")
    private Double longitude;

    public CreateEmergencyRequest() {
    }

    public CreateEmergencyRequest(EmergencyType type, Double latitude, Double longitude) {
        this.type = type != null ? type : EmergencyType.GENERAL;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public EmergencyType getType() {
        return type;
    }

    public void setType(EmergencyType type) {
        this.type = type;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public static CreateEmergencyRequestBuilder builder() {
        return new CreateEmergencyRequestBuilder();
    }

    public static class CreateEmergencyRequestBuilder {
        private EmergencyType type = EmergencyType.GENERAL;
        private Double latitude;
        private Double longitude;

        public CreateEmergencyRequestBuilder type(EmergencyType type) {
            this.type = type;
            return this;
        }

        public CreateEmergencyRequestBuilder latitude(Double latitude) {
            this.latitude = latitude;
            return this;
        }

        public CreateEmergencyRequestBuilder longitude(Double longitude) {
            this.longitude = longitude;
            return this;
        }

        public CreateEmergencyRequest build() {
            return new CreateEmergencyRequest(type, latitude, longitude);
        }
    }
}
