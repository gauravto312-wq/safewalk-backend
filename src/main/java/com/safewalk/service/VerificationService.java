package com.safewalk.service;

import com.safewalk.dto.response.VerificationDocumentResponse;
import com.safewalk.entity.VerificationDocument;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface VerificationService {

    VerificationDocumentResponse uploadDocument(String userMobile, String title, MultipartFile file);

    List<VerificationDocumentResponse> getUserDocuments(String userMobile);

    List<VerificationDocumentResponse> getDocumentsByUserId(Long userId);

    Resource getDocumentFile(Long documentId, String requestingMobile, boolean isAdmin);

    Resource getDocumentForAdminPreview(Long userId, Long documentId);

    VerificationDocument getDocumentEntity(Long documentId);
}
