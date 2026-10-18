package com.safewalk.dto.response;

import com.safewalk.entity.AccountStatus;
import com.safewalk.entity.Role;

public class NearbyUserResponse {

    private Long userId;
    private String name;
    private String mobileNumber;
    private Double latitude;
    private Double longitude;
    private Double distanceKm;
    private Role role;
    private AccountStatus accountStatus;
    private boolean policeVerified;

    public NearbyUserResponse() {
    }

    public NearbyUserResponse(Long userId, String name, String mobileNumber, Double latitude, Double longitude, Double distanceKm, Role role, AccountStatus accountStatus) {
        this(userId, name, mobileNumber, latitude, longitude, distanceKm, role, accountStatus, false);
    }

    public NearbyUserResponse(Long userId, String name, String mobileNumber, Double latitude, Double longitude, Double distanceKm, Role role, AccountStatus accountStatus, boolean policeVerified) {
        this.userId = userId;
        this.name = name;
        this.mobileNumber = mobileNumber;
        this.latitude = latitude;
        this.longitude = longitude;
        this.distanceKm = distanceKm;
        this.role = role;
        this.accountStatus = accountStatus;
        this.policeVerified = policeVerified;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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

    public Double getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(Double distanceKm) {
        this.distanceKm = distanceKm;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public AccountStatus getAccountStatus() {
        return accountStatus;
    }

    public void setAccountStatus(AccountStatus accountStatus) {
        this.accountStatus = accountStatus;
    }

    public boolean isPoliceVerified() {
        return policeVerified;
    }

    public void setPoliceVerified(boolean policeVerified) {
        this.policeVerified = policeVerified;
    }

    public static NearbyUserResponseBuilder builder() {
        return new NearbyUserResponseBuilder();
    }

    public static class NearbyUserResponseBuilder {
        private Long userId;
        private String name;
        private String mobileNumber;
        private Double latitude;
        private Double longitude;
        private Double distanceKm;
        private Role role;
        private AccountStatus accountStatus;
        private boolean policeVerified;

        public NearbyUserResponseBuilder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public NearbyUserResponseBuilder name(String name) {
            this.name = name;
            return this;
        }

        public NearbyUserResponseBuilder mobileNumber(String mobileNumber) {
            this.mobileNumber = mobileNumber;
            return this;
        }

        public NearbyUserResponseBuilder latitude(Double latitude) {
            this.latitude = latitude;
            return this;
        }

        public NearbyUserResponseBuilder longitude(Double longitude) {
            this.longitude = longitude;
            return this;
        }

        public NearbyUserResponseBuilder distanceKm(Double distanceKm) {
            this.distanceKm = distanceKm;
            return this;
        }

        public NearbyUserResponseBuilder role(Role role) {
            this.role = role;
            return this;
        }

        public NearbyUserResponseBuilder accountStatus(AccountStatus accountStatus) {
            this.accountStatus = accountStatus;
            return this;
        }

        public NearbyUserResponseBuilder policeVerified(boolean policeVerified) {
            this.policeVerified = policeVerified;
            return this;
        }

        public NearbyUserResponse build() {
            return new NearbyUserResponse(userId, name, mobileNumber, latitude, longitude, distanceKm, role, accountStatus, policeVerified);
        }
    }
}
