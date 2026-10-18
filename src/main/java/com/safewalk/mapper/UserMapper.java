package com.safewalk.mapper;

import com.safewalk.dto.request.RegisterRequest;
import com.safewalk.dto.request.UserCreateRequest;
import com.safewalk.dto.response.UserResponse;
import com.safewalk.entity.AccountStatus;
import com.safewalk.entity.Role;
import com.safewalk.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public User toEntity(RegisterRequest request) {
        if (request == null) {
            return null;
        }
        Role assignedRole = request.getRole() != null ? request.getRole() : Role.USER;
        com.safewalk.entity.VerificationStatus status = (assignedRole == Role.POLICE || assignedRole == Role.HOSPITAL)
                ? com.safewalk.entity.VerificationStatus.PENDING
                : com.safewalk.entity.VerificationStatus.NONE;

        return User.builder()
                .name(request.getName().trim())
                .mobileNumber(request.getMobileNumber().trim())
                .password(request.getPassword())
                .emergencyPassword(request.getEmergencyPassword() != null ? request.getEmergencyPassword().trim() : null)
                .role(assignedRole)
                .accountStatus(AccountStatus.ACTIVE)
                .verificationStatus(status)
                .policeVerified(false)
                .badgeNumber(request.getBadgeNumber() != null ? request.getBadgeNumber().trim() : null)
                .department(request.getDepartment() != null ? request.getDepartment().trim() : null)
                .build();
    }

    public User toEntity(UserCreateRequest request) {
        if (request == null) {
            return null;
        }
        Role assignedRole = request.getRole() != null ? request.getRole() : Role.USER;
        com.safewalk.entity.VerificationStatus status = (assignedRole == Role.POLICE || assignedRole == Role.HOSPITAL)
                ? com.safewalk.entity.VerificationStatus.PENDING
                : com.safewalk.entity.VerificationStatus.NONE;

        return User.builder()
                .name(request.getName().trim())
                .mobileNumber(request.getMobileNumber().trim())
                .password(request.getPassword())
                .emergencyPassword(request.getEmergencyPassword() != null ? request.getEmergencyPassword().trim() : null)
                .role(assignedRole)
                .accountStatus(AccountStatus.ACTIVE)
                .verificationStatus(status)
                .policeVerified(false)
                .build();
    }

    public UserResponse toResponse(User entity) {
        if (entity == null) {
            return null;
        }
        return UserResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .mobileNumber(entity.getMobileNumber())
                .email(entity.getEmail())
                .role(entity.getRole())
                .accountStatus(entity.getAccountStatus())
                .policeVerified(entity.isPoliceVerified())
                .verificationStatus(entity.getVerificationStatus())
                .avatarUrl(entity.getAvatarUrl())
                .badgeNumber(entity.getBadgeNumber())
                .department(entity.getDepartment())
                .policeStation(entity.getDepartment())
                .rejectionReason(entity.getRejectionReason())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
