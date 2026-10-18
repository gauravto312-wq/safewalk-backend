package com.safewalk.dto.response;

import com.safewalk.entity.AccountStatus;
import com.safewalk.entity.Role;

import java.time.LocalDateTime;

public class UserResponse {

    private Long id;
    private String name;
    private String mobileNumber;
    private Role role;
    private AccountStatus accountStatus;
    private boolean policeVerified;
    private com.safewalk.entity.VerificationStatus verificationStatus;
    private String avatarUrl;
    private String badgeNumber;
    private String department;
    private String policeStation;
    private String rejectionReason;
    private String email;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public UserResponse() {
    }

    public UserResponse(Long id, String name, String mobileNumber, Role role, AccountStatus accountStatus, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this(id, name, mobileNumber, role, accountStatus, false, com.safewalk.entity.VerificationStatus.NONE, null, null, null, null, createdAt, updatedAt);
    }

    public UserResponse(Long id, String name, String mobileNumber, Role role, AccountStatus accountStatus, boolean policeVerified, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this(id, name, mobileNumber, role, accountStatus, policeVerified,
                (role == Role.POLICE || role == Role.HOSPITAL) ? (policeVerified ? com.safewalk.entity.VerificationStatus.VERIFIED : com.safewalk.entity.VerificationStatus.PENDING) : com.safewalk.entity.VerificationStatus.NONE,
                null, null, null, null, createdAt, updatedAt);
    }

    public UserResponse(Long id, String name, String mobileNumber, Role role, AccountStatus accountStatus, boolean policeVerified, com.safewalk.entity.VerificationStatus verificationStatus, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this(id, name, mobileNumber, role, accountStatus, policeVerified, verificationStatus, null, null, null, null, createdAt, updatedAt);
    }

    public UserResponse(Long id, String name, String mobileNumber, Role role, AccountStatus accountStatus, boolean policeVerified, com.safewalk.entity.VerificationStatus verificationStatus, String avatarUrl, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this(id, name, mobileNumber, role, accountStatus, policeVerified, verificationStatus, avatarUrl, null, null, null, createdAt, updatedAt);
    }

    public UserResponse(Long id, String name, String mobileNumber, Role role, AccountStatus accountStatus, boolean policeVerified, com.safewalk.entity.VerificationStatus verificationStatus, String avatarUrl, String badgeNumber, String department, String rejectionReason, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.mobileNumber = mobileNumber;
        this.role = role;
        this.accountStatus = accountStatus;
        this.policeVerified = policeVerified;
        this.verificationStatus = verificationStatus;
        this.avatarUrl = avatarUrl;
        this.badgeNumber = badgeNumber;
        this.department = department;
        this.policeStation = department;
        this.rejectionReason = rejectionReason;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public com.safewalk.entity.VerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(com.safewalk.entity.VerificationStatus verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getBadgeNumber() {
        return badgeNumber;
    }

    public void setBadgeNumber(String badgeNumber) {
        this.badgeNumber = badgeNumber;
    }

    public String getDepartment() {
        return department;
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

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public static UserResponseBuilder builder() {
        return new UserResponseBuilder();
    }

    public static class UserResponseBuilder {
        private Long id;
        private String name;
        private String mobileNumber;
        private Role role;
        private AccountStatus accountStatus;
        private boolean policeVerified;
        private com.safewalk.entity.VerificationStatus verificationStatus;
        private String avatarUrl;
        private String badgeNumber;
        private String department;
        private String policeStation;
        private String rejectionReason;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public UserResponseBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public UserResponseBuilder name(String name) {
            this.name = name;
            return this;
        }

        public UserResponseBuilder mobileNumber(String mobileNumber) {
            this.mobileNumber = mobileNumber;
            return this;
        }

        public UserResponseBuilder role(Role role) {
            this.role = role;
            return this;
        }

        public UserResponseBuilder accountStatus(AccountStatus accountStatus) {
            this.accountStatus = accountStatus;
            return this;
        }

        public UserResponseBuilder policeVerified(boolean policeVerified) {
            this.policeVerified = policeVerified;
            return this;
        }

        public UserResponseBuilder verificationStatus(com.safewalk.entity.VerificationStatus verificationStatus) {
            this.verificationStatus = verificationStatus;
            return this;
        }

        public UserResponseBuilder avatarUrl(String avatarUrl) {
            this.avatarUrl = avatarUrl;
            return this;
        }

        public UserResponseBuilder badgeNumber(String badgeNumber) {
            this.badgeNumber = badgeNumber;
            return this;
        }

        public UserResponseBuilder department(String department) {
            this.department = department;
            if (this.policeStation == null) {
                this.policeStation = department;
            }
            return this;
        }

        public UserResponseBuilder policeStation(String policeStation) {
            this.policeStation = policeStation;
            if (this.department == null) {
                this.department = policeStation;
            }
            return this;
        }

        public UserResponseBuilder rejectionReason(String rejectionReason) {
            this.rejectionReason = rejectionReason;
            return this;
        }

        public UserResponseBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public UserResponseBuilder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        private String email;

        public UserResponseBuilder email(String email) {
            this.email = email;
            return this;
        }

        public UserResponse build() {
            String resolvedDept = department != null ? department : policeStation;
            UserResponse res = new UserResponse(id, name, mobileNumber, role, accountStatus, policeVerified, verificationStatus, avatarUrl, badgeNumber, resolvedDept, rejectionReason, createdAt, updatedAt);
            res.setEmail(email);
            return res;
        }
    }
}
