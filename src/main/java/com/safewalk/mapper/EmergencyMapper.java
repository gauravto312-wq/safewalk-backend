package com.safewalk.mapper;

import com.safewalk.dto.request.CreateEmergencyRequest;
import com.safewalk.dto.response.EmergencyResponse;
import com.safewalk.dto.response.UserResponse;
import com.safewalk.entity.Emergency;
import com.safewalk.entity.EmergencyStatus;
import com.safewalk.entity.EmergencyType;
import com.safewalk.entity.User;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class EmergencyMapper {

    public Emergency toEntity(CreateEmergencyRequest request, User requester) {
        if (request == null) {
            return null;
        }
        return Emergency.builder()
                .requester(requester)
                .type(request.getType() != null ? request.getType() : EmergencyType.GENERAL)
                .status(EmergencyStatus.ACTIVE)
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .build();
    }

    public EmergencyResponse toResponse(Emergency entity) {
        if (entity == null) {
            return null;
        }
        User requester = entity.getRequester();
        User responder = entity.getResponder();

        Set<User> allResponders = new LinkedHashSet<>();
        if (entity.getResponders() != null) {
            allResponders.addAll(entity.getResponders());
        }
        if (responder != null) {
            allResponders.add(responder);
        }

        List<UserResponse> responderResponses = new ArrayList<>();
        for (User u : allResponders) {
            responderResponses.add(UserResponse.builder()
                    .id(u.getId())
                    .name(u.getName())
                    .mobileNumber(u.getMobileNumber())
                    .role(u.getRole())
                    .accountStatus(u.getAccountStatus())
                    .createdAt(u.getCreatedAt())
                    .updatedAt(u.getUpdatedAt())
                    .build());
        }

        Long responderId = responder != null ? responder.getId() : (!responderResponses.isEmpty() ? responderResponses.get(0).getId() : null);
        String responderName = responder != null ? responder.getName() : (!responderResponses.isEmpty() ? responderResponses.get(0).getName() : null);
        String responderMobile = responder != null ? responder.getMobileNumber() : (!responderResponses.isEmpty() ? responderResponses.get(0).getMobileNumber() : null);

        return EmergencyResponse.builder()
                .id(entity.getId())
                .requesterId(requester != null ? requester.getId() : null)
                .requesterName(requester != null ? requester.getName() : null)
                .requesterMobile(requester != null ? requester.getMobileNumber() : null)
                .responderId(responderId)
                .responderName(responderName)
                .responderMobile(responderMobile)
                .responders(responderResponses)
                .type(entity.getType())
                .status(entity.getStatus())
                .latitude(entity.getLatitude())
                .longitude(entity.getLongitude())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .acceptedAt(entity.getAcceptedAt())
                .resolvedAt(entity.getResolvedAt())
                .cancelledAt(entity.getCancelledAt())
                .expiredAt(entity.getExpiredAt())
                .arrivedResponderId(entity.getArrivedResponder() != null ? entity.getArrivedResponder().getId() : null)
                .arrivedResponderName(entity.getArrivedResponder() != null ? entity.getArrivedResponder().getName() : null)
                .arrivedAt(entity.getArrivedAt())
                .requesterResolveConfirmed(entity.isRequesterResolveConfirmed())
                .responderResolveConfirmed(entity.isResponderResolveConfirmed())
                .build();
    }
}
