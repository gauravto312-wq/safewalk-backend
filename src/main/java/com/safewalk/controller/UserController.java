package com.safewalk.controller;

import com.safewalk.dto.request.UserCreateRequest;
import com.safewalk.dto.response.ApiResponse;
import com.safewalk.dto.response.UserResponse;
import com.safewalk.service.AuthService;
import com.safewalk.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.safewalk.dto.request.EmergencyContactRequest;
import com.safewalk.dto.response.EmergencyContactResponse;
import com.safewalk.service.EmergencyContactService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;
    private final AuthService authService;
    private final com.safewalk.service.FcmService fcmService;
    private final EmergencyContactService emergencyContactService;
    private final com.safewalk.service.VerificationService verificationService;

    @Autowired
    public UserController(
            UserService userService,
            AuthService authService,
            com.safewalk.service.FcmService fcmService,
            EmergencyContactService emergencyContactService,
            @Autowired(required = false) com.safewalk.service.VerificationService verificationService) {
        this.userService = userService;
        this.authService = authService;
        this.fcmService = fcmService;
        this.emergencyContactService = emergencyContactService;
        this.verificationService = verificationService;
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(Authentication authentication) {
        String mobileNumber = authentication.getName();
        UserResponse response = authService.getCurrentUserProfile(mobileNumber);
        return ResponseEntity.ok(ApiResponse.success("Current user profile retrieved successfully", response));
    }

    @GetMapping("/me/contacts")
    public ResponseEntity<ApiResponse<List<EmergencyContactResponse>>> getEmergencyContacts(Authentication authentication) {
        List<EmergencyContactResponse> contacts = emergencyContactService.getEmergencyContacts(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Emergency contacts retrieved successfully", contacts));
    }

    @PostMapping("/me/contacts")
    public ResponseEntity<ApiResponse<EmergencyContactResponse>> addEmergencyContact(
            @Valid @RequestBody EmergencyContactRequest request,
            Authentication authentication) {
        EmergencyContactResponse response = emergencyContactService.addEmergencyContact(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Emergency contact added successfully", response));
    }

    @PutMapping("/me/contacts/{contactIdentifier}")
    public ResponseEntity<ApiResponse<EmergencyContactResponse>> replaceEmergencyContact(
            @PathVariable String contactIdentifier,
            @Valid @RequestBody EmergencyContactRequest request,
            Authentication authentication) {
        EmergencyContactResponse response = emergencyContactService.replaceEmergencyContact(authentication.getName(), contactIdentifier, request);
        return ResponseEntity.ok(ApiResponse.success("Emergency contact replaced successfully", response));
    }

    @DeleteMapping("/me/contacts/{contactIdentifier}")
    public ResponseEntity<ApiResponse<String>> removeEmergencyContact(
            @PathVariable String contactIdentifier,
            Authentication authentication) {
        emergencyContactService.removeEmergencyContact(authentication.getName(), contactIdentifier);
        return ResponseEntity.ok(ApiResponse.success("Emergency contact removed successfully"));
    }

    @PostMapping(value = "/me/avatar", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UserResponse>> uploadAvatar(
            @org.springframework.web.bind.annotation.RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            Authentication authentication) {
        UserResponse response = userService.uploadProfilePicture(authentication.getName(), file);
        return ResponseEntity.ok(ApiResponse.success("Profile picture updated successfully", response));
    }

    @GetMapping("/{id}/avatar")
    public ResponseEntity<org.springframework.core.io.Resource> getUserAvatar(@PathVariable Long id) {
        org.springframework.core.io.Resource resource = userService.getProfilePictureResource(id);
        String contentType = "image/jpeg";
        try {
            String filename = resource.getFilename();
            if (filename != null) {
                if (filename.toLowerCase().endsWith(".png")) contentType = "image/png";
                else if (filename.toLowerCase().endsWith(".webp")) contentType = "image/webp";
            }
        } catch (Exception ignored) {}
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_TYPE, contentType)
                .body(resource);
    }

    @GetMapping("/me/avatar")
    public ResponseEntity<org.springframework.core.io.Resource> getCurrentUserAvatar(Authentication authentication) {
        UserResponse user = authService.getCurrentUserProfile(authentication.getName());
        return getUserAvatar(user.getId());
    }

    @PostMapping("/fcm-token")
    public ResponseEntity<ApiResponse<String>> registerFcmToken(
            @Valid @RequestBody com.safewalk.dto.request.FcmTokenRequest request,
            Authentication authentication) {
        fcmService.registerDeviceToken(authentication.getName(), request);
        return ResponseEntity.ok(ApiResponse.success("FCM device token registered successfully"));
    }

    @DeleteMapping("/fcm-token")
    public ResponseEntity<ApiResponse<String>> removeFcmToken(
            @org.springframework.web.bind.annotation.RequestParam String token,
            Authentication authentication) {
        fcmService.removeDeviceToken(authentication.getName(), token);
        return ResponseEntity.ok(ApiResponse.success("FCM device token deactivated successfully"));
    }

    @PostMapping(value = "/me/verification-documents", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<com.safewalk.dto.response.VerificationDocumentResponse>> uploadVerificationDocument(
            @org.springframework.web.bind.annotation.RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @org.springframework.web.bind.annotation.RequestParam(value = "title", required = false) String title,
            Authentication authentication) {
        com.safewalk.dto.response.VerificationDocumentResponse response =
                verificationService.uploadDocument(authentication.getName(), title, file);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Verification document uploaded successfully", response));
    }

    @GetMapping("/me/verification-documents")
    public ResponseEntity<ApiResponse<java.util.List<com.safewalk.dto.response.VerificationDocumentResponse>>> getVerificationDocuments(
            Authentication authentication) {
        java.util.List<com.safewalk.dto.response.VerificationDocumentResponse> response =
                verificationService.getUserDocuments(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Verification documents retrieved successfully", response));
    }

    @GetMapping("/me/verification-documents/{id}/file")
    public ResponseEntity<org.springframework.core.io.Resource> getVerificationDocumentFile(
            @PathVariable Long id,
            Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        org.springframework.core.io.Resource file = verificationService.getDocumentFile(id, authentication.getName(), isAdmin);
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFilename() + "\"")
                .body(file);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> createUser(@Valid @RequestBody UserCreateRequest request) {
        UserResponse response = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User registered successfully", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
        UserResponse response = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success("User retrieved successfully", response));
    }

    @GetMapping("/mobile/{mobileNumber}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserByMobileNumber(@PathVariable String mobileNumber) {
        UserResponse response = userService.getUserByMobileNumber(mobileNumber);
        return ResponseEntity.ok(ApiResponse.success("User retrieved successfully", response));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
        List<UserResponse> responses = userService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.success("Users retrieved successfully", responses));
    }
}
