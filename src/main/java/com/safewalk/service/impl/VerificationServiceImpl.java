package com.safewalk.service.impl;

import com.safewalk.dto.response.VerificationDocumentResponse;
import com.safewalk.entity.AuditLog;
import com.safewalk.entity.Role;
import com.safewalk.entity.User;
import com.safewalk.entity.VerificationDocument;
import com.safewalk.entity.VerificationStatus;
import com.safewalk.exception.ResourceNotFoundException;
import com.safewalk.repository.AuditLogRepository;
import com.safewalk.repository.UserRepository;
import com.safewalk.repository.VerificationDocumentRepository;
import com.safewalk.service.FileStorageService;
import com.safewalk.service.VerificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class VerificationServiceImpl implements VerificationService {

    private static final Logger log = LoggerFactory.getLogger(VerificationServiceImpl.class);

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("pdf", "jpg", "jpeg", "png");
    private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList("application/pdf", "image/jpeg", "image/png");

    private final VerificationDocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final AuditLogRepository auditLogRepository;

    @Autowired
    public VerificationServiceImpl(
            VerificationDocumentRepository documentRepository,
            UserRepository userRepository,
            FileStorageService fileStorageService,
            @Autowired(required = false) AuditLogRepository auditLogRepository) {
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
        this.fileStorageService = fileStorageService;
        this.auditLogRepository = auditLogRepository;
    }

    // Legacy constructor for backward compatibility with existing tests
    public VerificationServiceImpl(
            VerificationDocumentRepository documentRepository,
            UserRepository userRepository,
            @Value("${safewalk.upload.dir:#{systemProperties['java.io.tmpdir'] + '/safewalk/uploads/verification_documents'}}") String uploadDir) {
        this(documentRepository, userRepository, new LocalFileSystemStorageService(uploadDir), null);
    }

    @Override
    @Transactional
    public VerificationDocumentResponse uploadDocument(String userMobile, String title, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Upload file cannot be null or empty.");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds maximum limit of 10MB.");
        }

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "document");
        if (originalFilename.contains("..") || originalFilename.contains("/") || originalFilename.contains("\\")) {
            throw new IllegalArgumentException("Filename contains invalid path characters: " + originalFilename);
        }

        String extension = getFileExtension(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new IllegalArgumentException("Invalid file type ." + extension + ". Allowed formats: " + ALLOWED_EXTENSIONS);
        }

        String mimeType = file.getContentType();
        if (mimeType == null || !ALLOWED_MIME_TYPES.contains(mimeType.toLowerCase())) {
            // Fallback derivation based on validated extension
            if ("pdf".equalsIgnoreCase(extension)) {
                mimeType = "application/pdf";
            } else if ("png".equalsIgnoreCase(extension)) {
                mimeType = "image/png";
            } else {
                mimeType = "image/jpeg";
            }
        }

        User user = userRepository.findByMobileNumber(userMobile.trim())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with mobile number: " + userMobile));

        if (user.getRole() != Role.POLICE && user.getRole() != Role.HOSPITAL && user.getRole() != Role.ADMIN) {
            throw new IllegalArgumentException("Verification documents can only be submitted for POLICE or HOSPITAL accounts.");
        }

        String docTitle = (title != null && !title.trim().isEmpty())
                ? title.trim()
                : (user.getRole() == Role.POLICE ? "Police Verification Credential" : "Hospital Accreditation Proof");

        String subDir = "verification_documents/" + user.getId();
        String storedRelativePath;
        try {
            storedRelativePath = fileStorageService.storeFile(subDir, originalFilename, file);
        } catch (IOException e) {
            log.error("Failed to store verification file for user ID {}", user.getId(), e);
            throw new RuntimeException("Failed to store verification document. Please try again.", e);
        }

        try {
            VerificationDocument doc = VerificationDocument.builder()
                    .user(user)
                    .title(docTitle)
                    .fileName(originalFilename)
                    .storedPath(storedRelativePath)
                    .mimeType(mimeType)
                    .fileSize(file.getSize())
                    .build();

            VerificationDocument savedDoc = documentRepository.save(doc);

            // Defect B Fix: If a police or hospital user was REJECTED (or NONE), resubmitting documents
            // returns them to the PENDING queue while keeping dispatch eligibility strictly false.
            if (user.getRole() == Role.POLICE || user.getRole() == Role.HOSPITAL) {
                if (user.getVerificationStatus() == VerificationStatus.REJECTED || user.getVerificationStatus() == VerificationStatus.NONE) {
                    VerificationStatus previousStatus = user.getVerificationStatus();
                    user.setVerificationStatus(VerificationStatus.PENDING);
                    user.setPoliceVerified(false); // Unverified officer remains strictly blocked from dispatch
                    userRepository.save(user);

                    log.info("User ID {} status transitioned from {} to PENDING upon document resubmission", user.getId(), previousStatus);

                    if (auditLogRepository != null) {
                        String auditDetails = String.format("User resubmitted verification document '%s'. Status transitioned from %s to PENDING. Prior rejection reason: %s",
                                originalFilename, previousStatus, user.getRejectionReason() != null ? user.getRejectionReason() : "N/A");
                        AuditLog auditLog = AuditLog.builder()
                                .adminMobile(user.getMobileNumber())
                                .action("DOCUMENT_RESUBMISSION")
                                .targetType("USER")
                                .targetId(user.getId())
                                .details(auditDetails)
                                .build();
                        auditLogRepository.save(auditLog);
                    }
                }
            }

            log.info("Successfully uploaded verification document ID {} for user ID {}", savedDoc.getId(), user.getId());
            return toResponse(savedDoc);
        } catch (Exception e) {
            log.error("Failed to persist document metadata for user ID {}. Cleaning up stored file: {}", user.getId(), storedRelativePath, e);
            fileStorageService.deleteFile(storedRelativePath);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<VerificationDocumentResponse> getUserDocuments(String userMobile) {
        User user = userRepository.findByMobileNumber(userMobile.trim())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with mobile number: " + userMobile));

        return documentRepository.findByUserIdOrderByUploadedAtDesc(user.getId())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<VerificationDocumentResponse> getDocumentsByUserId(Long userId) {
        return documentRepository.findByUserIdOrderByUploadedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Resource getDocumentFile(Long documentId, String requestingMobile, boolean isAdmin) {
        VerificationDocument doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Verification document not found with ID: " + documentId));

        User owner = doc.getUser();
        if (!isAdmin && (requestingMobile == null || !owner.getMobileNumber().equals(requestingMobile.trim()))) {
            throw new AccessDeniedException("You are not authorized to view this verification document.");
        }

        try {
            Resource resource = fileStorageService.loadAsResource(doc.getStoredPath());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ResourceNotFoundException("File not found on storage: " + doc.getFileName());
            }
        } catch (IOException e) {
            throw new ResourceNotFoundException("File path is invalid or unreadable: " + doc.getStoredPath());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Resource getDocumentForAdminPreview(Long userId, Long documentId) {
        VerificationDocument doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Verification document not found with ID: " + documentId));

        if (doc.getUser() == null || !doc.getUser().getId().equals(userId)) {
            log.warn("IDOR / Mismatch attempt: document ID {} does not belong to user ID {}", documentId, userId);
            throw new IllegalArgumentException("Document ID " + documentId + " does not belong to user ID " + userId);
        }

        try {
            Resource resource = fileStorageService.loadAsResource(doc.getStoredPath());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ResourceNotFoundException("File not found on storage for document: " + doc.getFileName());
            }
        } catch (IOException e) {
            throw new ResourceNotFoundException("Could not load preview for document ID " + documentId + ": " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public VerificationDocument getDocumentEntity(Long documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Verification document not found with ID: " + documentId));
    }

    private String getFileExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return (dotIndex > 0 && dotIndex < filename.length() - 1) ? filename.substring(dotIndex + 1) : "";
    }

    private VerificationDocumentResponse toResponse(VerificationDocument doc) {
        return VerificationDocumentResponse.builder()
                .id(doc.getId())
                .userId(doc.getUser() != null ? doc.getUser().getId() : null)
                .title(doc.getTitle())
                .fileName(doc.getFileName())
                .mimeType(doc.getMimeType())
                .fileSize(doc.getFileSize())
                .uploadedAt(doc.getUploadedAt())
                .build();
    }
}
