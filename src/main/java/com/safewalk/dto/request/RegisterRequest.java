package com.safewalk.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegisterRequest {

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

    private com.safewalk.entity.Role role;

    @Size(max = 50, message = "Badge number cannot exceed 50 characters")
    private String badgeNumber;

    @Size(max = 100, message = "Department cannot exceed 100 characters")
    private String department;

    private String policeStation;

    public RegisterRequest() {
    }

    public RegisterRequest(String name, String mobileNumber, String password, String emergencyPassword) {
        this(name, mobileNumber, password, emergencyPassword, null, null, null);
    }

    public RegisterRequest(String name, String mobileNumber, String password, String emergencyPassword, com.safewalk.entity.Role role) {
        this(name, mobileNumber, password, emergencyPassword, role, null, null);
    }

    public RegisterRequest(String name, String mobileNumber, String password, String emergencyPassword, com.safewalk.entity.Role role, String badgeNumber, String department) {
        this.name = name;
        this.mobileNumber = mobileNumber;
        this.password = password;
        this.emergencyPassword = emergencyPassword;
        this.role = role;
        this.badgeNumber = badgeNumber;
        this.department = department;
        this.policeStation = department;
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

    public com.safewalk.entity.Role getRole() {
        return role;
    }

    public void setRole(com.safewalk.entity.Role role) {
        this.role = role;
    }

    public String getBadgeNumber() {
        return badgeNumber;
    }

    public void setBadgeNumber(String badgeNumber) {
        this.badgeNumber = badgeNumber;
    }

    public String getDepartment() {
        return department != null ? department : policeStation;
    }

    public void setDepartment(String department) {
        this.department = department;
        if (this.policeStation == null) {
            this.policeStation = department;
        }
    }

    public String getPoliceStation() {
        return policeStation != null ? policeStation : department;
    }

    public void setPoliceStation(String policeStation) {
        this.policeStation = policeStation;
        if (this.department == null) {
            this.department = policeStation;
        }
    }

    public static RegisterRequestBuilder builder() {
        return new RegisterRequestBuilder();
    }

    public static class RegisterRequestBuilder {
        private String name;
        private String mobileNumber;
        private String password;
        private String emergencyPassword;
        private com.safewalk.entity.Role role;
        private String badgeNumber;
        private String department;
        private String policeStation;

        public RegisterRequestBuilder name(String name) {
            this.name = name;
            return this;
        }

        public RegisterRequestBuilder mobileNumber(String mobileNumber) {
            this.mobileNumber = mobileNumber;
            return this;
        }

        public RegisterRequestBuilder password(String password) {
            this.password = password;
            return this;
        }

        public RegisterRequestBuilder emergencyPassword(String emergencyPassword) {
            this.emergencyPassword = emergencyPassword;
            return this;
        }

        public RegisterRequestBuilder role(com.safewalk.entity.Role role) {
            this.role = role;
            return this;
        }

        public RegisterRequestBuilder badgeNumber(String badgeNumber) {
            this.badgeNumber = badgeNumber;
            return this;
        }

        public RegisterRequestBuilder department(String department) {
            this.department = department;
            if (this.policeStation == null) {
                this.policeStation = department;
            }
            return this;
        }

        public RegisterRequestBuilder policeStation(String policeStation) {
            this.policeStation = policeStation;
            if (this.department == null) {
                this.department = policeStation;
            }
            return this;
        }

        public RegisterRequest build() {
            String resolvedDept = department != null ? department : policeStation;
            return new RegisterRequest(name, mobileNumber, password, emergencyPassword, role, badgeNumber, resolvedDept);
        }
    }
}
