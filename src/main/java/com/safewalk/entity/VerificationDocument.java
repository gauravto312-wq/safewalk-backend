package com.safewalk.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "verification_documents")
public class VerificationDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "stored_path", nullable = false, length = 500)
    private String storedPath;

    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private LocalDateTime uploadedAt;

    public VerificationDocument() {
    }

    public VerificationDocument(Long id, User user, String title, String fileName, String storedPath, String mimeType, Long fileSize, LocalDateTime uploadedAt) {
        this.id = id;
        this.user = user;
        this.title = title;
        this.fileName = fileName;
        this.storedPath = storedPath;
        this.mimeType = mimeType;
        this.fileSize = fileSize;
        this.uploadedAt = uploadedAt;
    }

    @PrePersist
    protected void onCreate() {
        this.uploadedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
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

    public String getStoredPath() {
        return storedPath;
    }

    public void setStoredPath(String storedPath) {
        this.storedPath = storedPath;
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

    public static VerificationDocumentBuilder builder() {
        return new VerificationDocumentBuilder();
    }

    public static class VerificationDocumentBuilder {
        private Long id;
        private User user;
        private String title;
        private String fileName;
        private String storedPath;
        private String mimeType;
        private Long fileSize;
        private LocalDateTime uploadedAt;

        public VerificationDocumentBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public VerificationDocumentBuilder user(User user) {
            this.user = user;
            return this;
        }

        public VerificationDocumentBuilder title(String title) {
            this.title = title;
            return this;
        }

        public VerificationDocumentBuilder fileName(String fileName) {
            this.fileName = fileName;
            return this;
        }

        public VerificationDocumentBuilder storedPath(String storedPath) {
            this.storedPath = storedPath;
            return this;
        }

        public VerificationDocumentBuilder mimeType(String mimeType) {
            this.mimeType = mimeType;
            return this;
        }

        public VerificationDocumentBuilder fileSize(Long fileSize) {
            this.fileSize = fileSize;
            return this;
        }

        public VerificationDocumentBuilder uploadedAt(LocalDateTime uploadedAt) {
            this.uploadedAt = uploadedAt;
            return this;
        }

        public VerificationDocument build() {
            return new VerificationDocument(id, user, title, fileName, storedPath, mimeType, fileSize, uploadedAt);
        }
    }
}
