package com.safewalk.service;

import com.safewalk.dto.request.EmergencyContactRequest;
import com.safewalk.dto.response.EmergencyContactResponse;

import java.util.List;

public interface EmergencyContactService {

    List<EmergencyContactResponse> getEmergencyContacts(String currentUserMobile);

    EmergencyContactResponse addEmergencyContact(String currentUserMobile, EmergencyContactRequest request);

    void removeEmergencyContact(String currentUserMobile, String contactIdentifier);

    EmergencyContactResponse replaceEmergencyContact(String currentUserMobile, String oldContactIdentifier, EmergencyContactRequest request);
}
