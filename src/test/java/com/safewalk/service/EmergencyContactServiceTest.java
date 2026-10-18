package com.safewalk.service;

import com.safewalk.dto.request.EmergencyContactRequest;
import com.safewalk.dto.response.EmergencyContactResponse;
import com.safewalk.entity.AccountStatus;
import com.safewalk.entity.EmergencyContact;
import com.safewalk.entity.Role;
import com.safewalk.entity.User;
import com.safewalk.exception.DuplicateResourceException;
import com.safewalk.exception.ResourceNotFoundException;
import com.safewalk.repository.EmergencyContactRepository;
import com.safewalk.repository.UserRepository;
import com.safewalk.service.impl.EmergencyContactServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmergencyContactServiceTest {

    @Mock
    private EmergencyContactRepository emergencyContactRepository;

    @Mock
    private UserRepository userRepository;

    private EmergencyContactServiceImpl emergencyContactService;

    private User currentUser;
    private User contactUser1;
    private User contactUser2;

    @BeforeEach
    void setUp() {
        emergencyContactService = new EmergencyContactServiceImpl(emergencyContactRepository, userRepository);

        currentUser = User.builder()
                .id(1L)
                .name("Arjun Mehra")
                .mobileNumber("+919876543210")
                .role(Role.USER)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        contactUser1 = User.builder()
                .id(10245L)
                .name("Kavita Contact")
                .mobileNumber("+919876543211")
                .role(Role.USER)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        contactUser2 = User.builder()
                .id(10246L)
                .name("Rohit Contact")
                .mobileNumber("+919876543212")
                .role(Role.USER)
                .accountStatus(AccountStatus.ACTIVE)
                .build();
    }

    @Test
    void addEmergencyContact_NumericId_Success() {
        EmergencyContactRequest request = EmergencyContactRequest.builder()
                .contactIdentifier("10245")
                .build();

        when(userRepository.findByMobileNumber("+919876543210")).thenReturn(Optional.of(currentUser));
        when(emergencyContactRepository.countByUserId(1L)).thenReturn(0L);
        when(userRepository.findById(10245L)).thenReturn(Optional.of(contactUser1));
        when(emergencyContactRepository.existsByUserIdAndContactUserId(1L, 10245L)).thenReturn(false);

        EmergencyContact savedContact = EmergencyContact.builder()
                .id(50L)
                .user(currentUser)
                .contactUser(contactUser1)
                .createdAt(LocalDateTime.now())
                .build();
        when(emergencyContactRepository.save(any(EmergencyContact.class))).thenReturn(savedContact);

        EmergencyContactResponse response = emergencyContactService.addEmergencyContact("+919876543210", request);

        assertNotNull(response);
        assertEquals(50L, response.getId());
        assertEquals(1L, response.getUserId());
        assertEquals(10245L, response.getContactUserId());
        assertEquals("SW-10245", response.getContactFormattedId());
        assertEquals("Kavita Contact", response.getContactName());
    }

    @Test
    void addEmergencyContact_PrefixedSafeWalkId_Success() {
        EmergencyContactRequest request = EmergencyContactRequest.builder()
                .contactIdentifier("SW-10245")
                .build();

        when(userRepository.findByMobileNumber("+919876543210")).thenReturn(Optional.of(currentUser));
        when(emergencyContactRepository.countByUserId(1L)).thenReturn(0L);
        when(userRepository.findById(10245L)).thenReturn(Optional.of(contactUser1));
        when(emergencyContactRepository.existsByUserIdAndContactUserId(1L, 10245L)).thenReturn(false);

        EmergencyContact savedContact = EmergencyContact.builder()
                .id(51L)
                .user(currentUser)
                .contactUser(contactUser1)
                .createdAt(LocalDateTime.now())
                .build();
        when(emergencyContactRepository.save(any(EmergencyContact.class))).thenReturn(savedContact);

        EmergencyContactResponse response = emergencyContactService.addEmergencyContact("+919876543210", request);

        assertNotNull(response);
        assertEquals(10245L, response.getContactUserId());
        assertEquals("SW-10245", response.getContactFormattedId());
    }

    @Test
    void addEmergencyContact_SelfAddition_ThrowsIllegalArgumentException() {
        EmergencyContactRequest request = EmergencyContactRequest.builder()
                .contactIdentifier("1") // Same as currentUser ID (1L)
                .build();

        when(userRepository.findByMobileNumber("+919876543210")).thenReturn(Optional.of(currentUser));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                emergencyContactService.addEmergencyContact("+919876543210", request));

        assertTrue(ex.getMessage().contains("You cannot add yourself"));
    }

    @Test
    void addEmergencyContact_ExceedsMaxThreeContacts_ThrowsIllegalStateException() {
        EmergencyContactRequest request = EmergencyContactRequest.builder()
                .contactIdentifier("10246")
                .build();

        when(userRepository.findByMobileNumber("+919876543210")).thenReturn(Optional.of(currentUser));
        when(emergencyContactRepository.countByUserId(1L)).thenReturn(3L); // Already has 3 contacts!

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                emergencyContactService.addEmergencyContact("+919876543210", request));

        assertTrue(ex.getMessage().contains("Maximum of 3 emergency contacts allowed"));
    }

    @Test
    void addEmergencyContact_DuplicateContact_ThrowsDuplicateResourceException() {
        EmergencyContactRequest request = EmergencyContactRequest.builder()
                .contactIdentifier("10245")
                .build();

        when(userRepository.findByMobileNumber("+919876543210")).thenReturn(Optional.of(currentUser));
        when(emergencyContactRepository.countByUserId(1L)).thenReturn(1L);
        when(userRepository.findById(10245L)).thenReturn(Optional.of(contactUser1));
        when(emergencyContactRepository.existsByUserIdAndContactUserId(1L, 10245L)).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () ->
                emergencyContactService.addEmergencyContact("+919876543210", request));
    }

    @Test
    void getEmergencyContacts_ReturnsList() {
        EmergencyContact contact = EmergencyContact.builder()
                .id(100L)
                .user(currentUser)
                .contactUser(contactUser1)
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.findByMobileNumber("+919876543210")).thenReturn(Optional.of(currentUser));
        when(emergencyContactRepository.findByUserId(1L)).thenReturn(Collections.singletonList(contact));

        List<EmergencyContactResponse> list = emergencyContactService.getEmergencyContacts("+919876543210");

        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals("SW-10245", list.get(0).getContactFormattedId());
    }

    @Test
    void removeEmergencyContact_Success() {
        EmergencyContact contact = EmergencyContact.builder()
                .id(100L)
                .user(currentUser)
                .contactUser(contactUser1)
                .build();

        when(userRepository.findByMobileNumber("+919876543210")).thenReturn(Optional.of(currentUser));
        when(emergencyContactRepository.findByUserIdAndContactUserId(1L, 10245L)).thenReturn(Optional.of(contact));

        emergencyContactService.removeEmergencyContact("+919876543210", "SW-10245");

        verify(emergencyContactRepository).delete(contact);
    }

    @Test
    void replaceEmergencyContact_Success() {
        EmergencyContact oldContact = EmergencyContact.builder()
                .id(100L)
                .user(currentUser)
                .contactUser(contactUser1)
                .build();

        EmergencyContactRequest replaceRequest = EmergencyContactRequest.builder()
                .contactIdentifier("SW-10246")
                .build();

        when(userRepository.findByMobileNumber("+919876543210")).thenReturn(Optional.of(currentUser));
        when(emergencyContactRepository.findByUserIdAndContactUserId(1L, 10245L)).thenReturn(Optional.of(oldContact));

        when(emergencyContactRepository.countByUserId(1L)).thenReturn(2L); // after delete count is within cap
        when(userRepository.findById(10246L)).thenReturn(Optional.of(contactUser2));
        when(emergencyContactRepository.existsByUserIdAndContactUserId(1L, 10246L)).thenReturn(false);

        EmergencyContact newSavedContact = EmergencyContact.builder()
                .id(101L)
                .user(currentUser)
                .contactUser(contactUser2)
                .createdAt(LocalDateTime.now())
                .build();
        when(emergencyContactRepository.save(any(EmergencyContact.class))).thenReturn(newSavedContact);

        EmergencyContactResponse response = emergencyContactService.replaceEmergencyContact("+919876543210", "SW-10245", replaceRequest);

        verify(emergencyContactRepository).delete(oldContact);
        assertNotNull(response);
        assertEquals(10246L, response.getContactUserId());
        assertEquals("SW-10246", response.getContactFormattedId());
        assertEquals("Rohit Contact", response.getContactName());
    }
}
