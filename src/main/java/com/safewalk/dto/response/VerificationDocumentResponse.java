package com.safewalk.dto.response;

import java.time.LocalDateTime;

public class VerificationDocumentResponse {

    private Long id;
    private Long userId;
    private String title;
    private String fileName;
    private String mimeType;
    private Long fileSize;
    private LocalDateTime uploadedAt;

    public VerificationDocumentResponse() {
    }

    public VerificationDocumentResponse(Long id, Long userId, String title, String fileName, String mimeType, Long fileSize, LocalDateTime uploadedAt) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.fileName = fileName;
        this.mimeType = mimeType;
        this.fileSize = fileSize;
        this.uploadedAt = uploadedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public static VerificationDocumentResponseBuilder builder() {
        return new VerificationDocumentResponseBuilder();
    }

    public static class VerificationDocumentResponseBuilder {
        private Long id;
        private Long userId;
        private String title;
        private String fileName;
        private String mimeType;
        private Long fileSize;
        private LocalDateTime uploadedAt;

        public VerificationDocumentResponseBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public VerificationDocumentResponseBuilder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public VerificationDocumentResponseBuilder title(String title) {
            this.title = title;
            return this;
        }

        public VerificationDocumentResponseBuilder fileName(String fileName) {
            this.fileName = fileName;
            return this;
        }

        public VerificationDocumentResponseBuilder mimeType(String mimeType) {
            this.mimeType = mimeType;
            return this;
        }

        public VerificationDocumentResponseBuilder fileSize(Long fileSize) {
            this.fileSize = fileSize;
            return this;
        }

        public VerificationDocumentResponseBuilder uploadedAt(LocalDateTime uploadedAt) {
            this.uploadedAt = uploadedAt;
            return this;
        }

        public VerificationDocumentResponse build() {
            return new VerificationDocumentResponse(id, userId, title, fileName, mimeType, fileSize, uploadedAt);
        }
    }
}
