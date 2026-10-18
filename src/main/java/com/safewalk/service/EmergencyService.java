package com.safewalk.service;

import com.safewalk.dto.request.CancelEmergencyRequest;
import com.safewalk.dto.request.CreateEmergencyRequest;
import com.safewalk.dto.response.EmergencyLocationResponse;
import com.safewalk.dto.response.EmergencyResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EmergencyService {

    EmergencyResponse createEmergency(CreateEmergencyRequest request, String currentUserMobile);

    EmergencyResponse getEmergencyById(Long emergencyId, String currentUserMobile, boolean isAdmin);

    EmergencyLocationResponse getEmergencyLocations(Long emergencyId, String currentUserMobile, boolean isAdmin);

    EmergencyResponse getMyActiveEmergency(String currentUserMobile);

    java.util.List<EmergencyResponse> getActiveEmergencies(String currentUserMobile);

    Page<EmergencyResponse> getMyEmergencyHistory(String currentUserMobile, Pageable pageable);

    EmergencyResponse acceptEmergency(Long emergencyId, String currentUserMobile);

    EmergencyResponse arriveEmergency(Long emergencyId, String currentUserMobile);

    EmergencyResponse rejectEmergency(Long emergencyId, String currentUserMobile);

    EmergencyResponse cancelEmergency(Long emergencyId, CancelEmergencyRequest request, String currentUserMobile);

    EmergencyResponse resolveEmergency(Long emergencyId, String currentUserMobile);

    int expireOverdueEmergencies();

    int discoverAndDispatchNewResponders();
}
