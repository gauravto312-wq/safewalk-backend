package com.safewalk.service.impl;

import com.safewalk.dto.request.EmergencyContactRequest;
import com.safewalk.dto.response.EmergencyContactResponse;
import com.safewalk.entity.AccountStatus;
import com.safewalk.entity.EmergencyContact;
import com.safewalk.entity.User;
import com.safewalk.exception.AccountStatusException;
import com.safewalk.exception.DuplicateResourceException;
import com.safewalk.exception.ResourceNotFoundException;
import com.safewalk.repository.EmergencyContactRepository;
import com.safewalk.repository.UserRepository;
import com.safewalk.service.EmergencyContactService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class EmergencyContactServiceImpl implements EmergencyContactService {

    private static final Logger log = LoggerFactory.getLogger(EmergencyContactServiceImpl.class);

    private final EmergencyContactRepository emergencyContactRepository;
    private final UserRepository userRepository;

    @Autowired
    public EmergencyContactServiceImpl(EmergencyContactRepository emergencyContactRepository, UserRepository userRepository) {
        this.emergencyContactRepository = emergencyContactRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmergencyContactResponse> getEmergencyContacts(String currentUserMobile) {
        log.debug("Fetching emergency contacts for user mobile: {}", currentUserMobile);

        User currentUser = userRepository.findByMobileNumber(currentUserMobile.trim())
                .orElseThrow(() -> new ResourceNotFoundException("User not found for mobile: " + currentUserMobile));

        return emergencyContactRepository.findByUserId(currentUser.getId()).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EmergencyContactResponse addEmergencyContact(String currentUserMobile, EmergencyContactRequest request) {
        log.info("Processing emergency contact addition for user mobile: {}", currentUserMobile);

        if (request == null) {
            throw new IllegalArgumentException("Emergency contact request cannot be null.");
        }

        User currentUser = userRepository.findByMobileNumber(currentUserMobile.trim())
                .orElseThrow(() -> new ResourceNotFoundException("User not found for mobile: " + currentUserMobile));

        Long contactUserId = request.getParsedContactUserId();

        if (currentUser.getId().equals(contactUserId)) {
            log.warn("User ID {} attempted to add themselves as an emergency contact", currentUser.getId());
            throw new IllegalArgumentException("You cannot add yourself as an emergency contact.");
        }

        long currentCount = emergencyContactRepository.countByUserId(currentUser.getId());
        if (currentCount >= 3) {
            log.warn("User ID {} already has maximum allowed emergency contacts ({})", currentUser.getId(), currentCount);
            throw new IllegalStateException("Maximum of 3 emergency contacts allowed. Remove or replace an existing contact before adding a new one.");
        }

        User contactUser = userRepository.findById(contactUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Contact user not found with SafeWalk ID: " + contactUserId));

        if (contactUser.getAccountStatus() != AccountStatus.ACTIVE) {
            log.warn("Cannot add user ID {} as emergency contact because account status is: {}", contactUserId, contactUser.getAccountStatus());
            throw new AccountStatusException("Cannot add user as emergency contact because their account is " + contactUser.getAccountStatus().name());
        }

        if (emergencyContactRepository.existsByUserIdAndContactUserId(currentUser.getId(), contactUserId)) {
            log.warn("Duplicate contact attempt: User ID {} already has Contact ID {}", currentUser.getId(), contactUserId);
            throw new DuplicateResourceException("This user is already in your emergency contacts.");
        }

        EmergencyContact contact = EmergencyContact.builder()
                .user(currentUser)
                .contactUser(contactUser)
                .build();

        EmergencyContact saved = emergencyContactRepository.save(contact);
        log.info("Successfully added emergency contact ID: {} (SafeWalk ID: SW-{}) for user ID: {}",
                saved.getId(), contactUserId, currentUser.getId());

        return toResponse(saved);
    }

    @Override
    @Transactional
    public void removeEmergencyContact(String currentUserMobile, String contactIdentifier) {
        log.info("Processing emergency contact removal for user mobile: {} with identifier: {}", currentUserMobile, contactIdentifier);

        if (contactIdentifier == null || contactIdentifier.trim().isEmpty()) {
            throw new IllegalArgumentException("Contact identifier must be provided.");
        }

        User currentUser = userRepository.findByMobileNumber(currentUserMobile.trim())
                .orElseThrow(() -> new ResourceNotFoundException("User not found for mobile: " + currentUserMobile));

        String trimmed = contactIdentifier.trim();
        if (trimmed.toUpperCase().startsWith("SW-")) {
            trimmed = trimmed.substring(3).trim();
        }

        Long parsedId;
        try {
            parsedId = Long.parseLong(trimmed);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid contact identifier format: " + contactIdentifier);
        }

        // Try deleting by contact_user_id first
        Optional<EmergencyContact> contactOpt = emergencyContactRepository.findByUserIdAndContactUserId(currentUser.getId(), parsedId);

        // If not found by contact_user_id, check if parsedId was the primary key ID of the EmergencyContact entity
        if (contactOpt.isEmpty()) {
            Optional<EmergencyContact> byIdOpt = emergencyContactRepository.findById(parsedId);
            if (byIdOpt.isPresent() && byIdOpt.get().getUser().getId().equals(currentUser.getId())) {
                contactOpt = byIdOpt;
            }
        }

        EmergencyContact contact = contactOpt.orElseThrow(() ->
                new ResourceNotFoundException("Emergency contact not found with identifier: " + contactIdentifier));

        emergencyContactRepository.delete(contact);
        log.info("Successfully removed emergency contact ID: {} for user ID: {}", contact.getId(), currentUser.getId());
    }

    @Override
    @Transactional
    public EmergencyContactResponse replaceEmergencyContact(String currentUserMobile, String oldContactIdentifier, EmergencyContactRequest request) {
        log.info("Processing emergency contact replacement for user mobile: {} replacing: {}", currentUserMobile, oldContactIdentifier);

        if (request == null) {
            throw new IllegalArgumentException("Emergency contact request cannot be null.");
        }

        // 1. Remove old contact
        removeEmergencyContact(currentUserMobile, oldContactIdentifier);

        // 2. Add new contact
        return addEmergencyContact(currentUserMobile, request);
    }

    private EmergencyContactResponse toResponse(EmergencyContact contact) {
        User contactUser = contact.getContactUser();
        return EmergencyContactResponse.builder()
                .id(contact.getId())
                .userId(contact.getUser().getId())
                .contactUserId(contactUser.getId())
                .contactFormattedId("SW-" + contactUser.getId())
                .contactName(contactUser.getName())
                .contactMobile(contactUser.getMobileNumber())
                .contactRole(contactUser.getRole())
                .createdAt(contact.getCreatedAt())
                .build();
    }
}
