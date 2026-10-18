package com.safewalk.dto.request;

import com.safewalk.entity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UserCreateRequest {

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^\\+?[1-9]\\d{7,14}$", message = "Invalid mobile number format. Must follow E.164 format (e.g. +919876543210 or 9876543210)")
    private String mobileNumber;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 100, message = "Password must be at least 6 characters long")
    private String password;

    @Size(max = 100, message = "Emergency password cannot exceed 100 characters")
    private String emergencyPassword;

    private Role role = Role.USER;

    public UserCreateRequest() {
    }

    public UserCreateRequest(String name, String mobileNumber, String password, String emergencyPassword) {
        this.name = name;
        this.mobileNumber = mobileNumber;
        this.password = password;
        this.emergencyPassword = emergencyPassword;
        this.role = Role.USER;
    }

    public UserCreateRequest(String name, String mobileNumber, String password, String emergencyPassword, Role role) {
        this.name = name;
        this.mobileNumber = mobileNumber;
        this.password = password;
        this.emergencyPassword = emergencyPassword;
        this.role = role != null ? role : Role.USER;
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmergencyPassword() {
        return emergencyPassword;
    }

    public void setEmergencyPassword(String emergencyPassword) {
        this.emergencyPassword = emergencyPassword;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public static UserCreateRequestBuilder builder() {
        return new UserCreateRequestBuilder();
    }

    public static class UserCreateRequestBuilder {
        private String name;
        private String mobileNumber;
        private String password;
        private String emergencyPassword;
        private Role role = Role.USER;

        public UserCreateRequestBuilder name(String name) {
            this.name = name;
            return this;
        }

        public UserCreateRequestBuilder mobileNumber(String mobileNumber) {
            this.mobileNumber = mobileNumber;
            return this;
        }

        public UserCreateRequestBuilder password(String password) {
            this.password = password;
            return this;
        }

        public UserCreateRequestBuilder emergencyPassword(String emergencyPassword) {
            this.emergencyPassword = emergencyPassword;
            return this;
        }

        public UserCreateRequestBuilder role(Role role) {
            this.role = role;
            return this;
        }

        public UserCreateRequest build() {
            return new UserCreateRequest(name, mobileNumber, password, emergencyPassword, role);
        }
    }
}
