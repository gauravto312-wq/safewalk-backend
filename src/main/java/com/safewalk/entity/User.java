package com.safewalk.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "mobile_number", nullable = false, unique = true, length = 20)
    private String mobileNumber;

    @Column(nullable = false)
    private String password;

    @Column(name = "emergency_password", length = 100)
    private String emergencyPassword;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role = Role.USER;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", nullable = false, length = 20)
    private AccountStatus accountStatus = AccountStatus.ACTIVE;

    @Column(name = "police_verified", nullable = false)
    private boolean policeVerified = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", length = 20)
    private VerificationStatus verificationStatus = VerificationStatus.NONE;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "badge_number", length = 50)
    private String badgeNumber;

    @Column(name = "department", length = 100)
    private String department;

    @Column(name = "rejection_reason", length = 255)
    private String rejectionReason;

    @Column(name = "email", length = 100, unique = true)
    private String email;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public User() {
    }

    public User(Long id, String name, String mobileNumber, String password, String emergencyPassword, Role role, AccountStatus accountStatus, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this(id, name, mobileNumber, password, emergencyPassword, role, accountStatus, false, VerificationStatus.NONE, createdAt, updatedAt);
    }

    public User(Long id, String name, String mobileNumber, String password, String emergencyPassword, Role role, AccountStatus accountStatus, boolean policeVerified, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this(id, name, mobileNumber, password, emergencyPassword, role, accountStatus, policeVerified,
                (role == Role.POLICE || role == Role.HOSPITAL) ? (policeVerified ? VerificationStatus.VERIFIED : VerificationStatus.PENDING) : VerificationStatus.NONE,
                createdAt, updatedAt);
    }

    public User(Long id, String name, String mobileNumber, String password, String emergencyPassword, Role role, AccountStatus accountStatus, boolean policeVerified, VerificationStatus verificationStatus, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this(id, name, mobileNumber, password, emergencyPassword, role, accountStatus, policeVerified, verificationStatus, null, createdAt, updatedAt);
    }

    public User(Long id, String name, String mobileNumber, String password, String emergencyPassword, Role role, AccountStatus accountStatus, boolean policeVerified, VerificationStatus verificationStatus, String avatarUrl, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this(id, name, mobileNumber, password, emergencyPassword, role, accountStatus, policeVerified, verificationStatus, avatarUrl, null, null, null, createdAt, updatedAt);
    }

    public User(Long id, String name, String mobileNumber, String password, String emergencyPassword, Role role, AccountStatus accountStatus, boolean policeVerified, VerificationStatus verificationStatus, String avatarUrl, String badgeNumber, String department, String rejectionReason, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.mobileNumber = mobileNumber;
        this.password = password;
        this.emergencyPassword = emergencyPassword;
        this.role = role != null ? role : Role.USER;
        this.accountStatus = accountStatus != null ? accountStatus : AccountStatus.ACTIVE;
        this.policeVerified = policeVerified;
        this.verificationStatus = verificationStatus != null ? verificationStatus :
                ((this.role == Role.POLICE || this.role == Role.HOSPITAL) ? (policeVerified ? VerificationStatus.VERIFIED : VerificationStatus.PENDING) : VerificationStatus.NONE);
        this.avatarUrl = avatarUrl;
        this.badgeNumber = badgeNumber;
        this.department = department;
        this.rejectionReason = rejectionReason;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.accountStatus == null) {
            this.accountStatus = AccountStatus.ACTIVE;
        }
        if (this.role == null) {
            this.role = Role.USER;
        }
        if (this.verificationStatus == null || this.verificationStatus == VerificationStatus.NONE) {
            if (this.role == Role.POLICE || this.role == Role.HOSPITAL) {
                this.verificationStatus = this.policeVerified ? VerificationStatus.VERIFIED : VerificationStatus.PENDING;
            } else {
                this.verificationStatus = VerificationStatus.NONE;
            }
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
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
        if (this.role == Role.POLICE) {
            if (policeVerified) {
                this.verificationStatus = VerificationStatus.VERIFIED;
            } else if (this.verificationStatus != VerificationStatus.REJECTED) {
                this.verificationStatus = VerificationStatus.PENDING;
            }
        }
    }

    public VerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(VerificationStatus verificationStatus) {
        this.verificationStatus = verificationStatus;
        if (this.role == Role.POLICE) {
            this.policeVerified = (verificationStatus == VerificationStatus.VERIFIED);
        }
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
        this.email = email != null ? email.trim().toLowerCase() : null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return id != null && id.equals(user.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    // Builder
    public static UserBuilder builder() {
        return new UserBuilder();
    }

    public static class UserBuilder {
        private Long id;
        private String name;
        private String mobileNumber;
        private String password;
        private String emergencyPassword;
        private Role role = Role.USER;
        private AccountStatus accountStatus = AccountStatus.ACTIVE;
        private boolean policeVerified = false;
        private VerificationStatus verificationStatus;
        private String avatarUrl;
        private String badgeNumber;
        private String department;
        private String rejectionReason;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public UserBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public UserBuilder name(String name) {
            this.name = name;
            return this;
        }

        public UserBuilder mobileNumber(String mobileNumber) {
            this.mobileNumber = mobileNumber;
            return this;
        }

        public UserBuilder password(String password) {
            this.password = password;
            return this;
        }

        public UserBuilder emergencyPassword(String emergencyPassword) {
            this.emergencyPassword = emergencyPassword;
            return this;
        }

        public UserBuilder role(Role role) {
            this.role = role;
            return this;
        }

        public UserBuilder accountStatus(AccountStatus accountStatus) {
            this.accountStatus = accountStatus;
            return this;
        }

        public UserBuilder policeVerified(boolean policeVerified) {
            this.policeVerified = policeVerified;
            return this;
        }

        public UserBuilder verificationStatus(VerificationStatus verificationStatus) {
            this.verificationStatus = verificationStatus;
            return this;
        }

        public UserBuilder avatarUrl(String avatarUrl) {
            this.avatarUrl = avatarUrl;
            return this;
        }

        public UserBuilder badgeNumber(String badgeNumber) {
            this.badgeNumber = badgeNumber;
            return this;
        }

        public UserBuilder department(String department) {
            this.department = department;
            return this;
        }

        public UserBuilder rejectionReason(String rejectionReason) {
            this.rejectionReason = rejectionReason;
            return this;
        }

        public UserBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public UserBuilder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        private String email;

        public UserBuilder email(String email) {
            this.email = email != null ? email.trim().toLowerCase() : null;
            return this;
        }

        public User build() {
            User user = new User(id, name, mobileNumber, password, emergencyPassword, role, accountStatus, policeVerified, verificationStatus, avatarUrl, badgeNumber, department, rejectionReason, createdAt, updatedAt);
            user.setEmail(email);
            return user;
        }
    }
}
