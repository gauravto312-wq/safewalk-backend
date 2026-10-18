package com.safewalk.service.impl;

import com.safewalk.service.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Local filesystem implementation of {@link FileStorageService}.
 * Stores files under a configurable base storage directory using portable relative identifiers.
 * Also seamlessly resolves legacy absolute file paths for backward compatibility.
 */
@Service
public class LocalFileSystemStorageService implements FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalFileSystemStorageService.class);

    private final Path baseLocation;

    public LocalFileSystemStorageService(
            @Value("${safewalk.storage.base-dir:${safewalk.upload.base-dir:${UPLOAD_DIR:./uploads}}}") String baseDir) {
        this.baseLocation = Paths.get(baseDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.baseLocation);
            log.info("Initialized local file storage at: {}", this.baseLocation);
        } catch (IOException e) {
            log.error("Could not initialize local file storage at {}", this.baseLocation, e);
        }
    }

    @Override
    public String storeFile(String subDirectory, String originalFilename, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Cannot store an empty or null file.");
        }

        String cleanedName = StringUtils.cleanPath(originalFilename != null ? originalFilename : "file");
        if (cleanedName.contains("..") || cleanedName.contains("/") || cleanedName.contains("\\")) {
            throw new IllegalArgumentException("Invalid filename containing path traversal characters: " + originalFilename);
        }

        String safeSubDir = subDirectory != null ? StringUtils.cleanPath(subDirectory) : "";
        if (safeSubDir.contains("..")) {
            throw new IllegalArgumentException("Invalid sub-directory containing path traversal characters: " + subDirectory);
        }

        Path targetDir = this.baseLocation.resolve(safeSubDir).normalize();
        if (!targetDir.startsWith(this.baseLocation)) {
            throw new SecurityException("Attempted directory traversal outside base storage directory.");
        }
        Files.createDirectories(targetDir);

        String storedFilename = UUID.randomUUID() + "_" + cleanedName;
        Path targetFilePath = targetDir.resolve(storedFilename).normalize();

        Files.copy(file.getInputStream(), targetFilePath, StandardCopyOption.REPLACE_EXISTING);

        // Compute portable relative identifier (using forward slashes for cross-platform portability)
        String relativeIdentifier = safeSubDir.isEmpty()
                ? storedFilename
                : (safeSubDir.replace('\\', '/') + "/" + storedFilename);

        log.debug("Stored file '{}' at '{}' with portable identifier '{}'", originalFilename, targetFilePath, relativeIdentifier);
        return relativeIdentifier;
    }

    @Override
    public Resource loadAsResource(String storageIdentifier) throws IOException {
        if (!StringUtils.hasText(storageIdentifier)) {
            throw new FileNotFoundException("Storage identifier cannot be empty.");
        }

        Path candidatePath = Paths.get(storageIdentifier).normalize();

        // Check if candidatePath is an existing absolute path (legacy record backward compatibility)
        if (candidatePath.isAbsolute() && Files.exists(candidatePath) && Files.isReadable(candidatePath)) {
            return new UrlResource(candidatePath.toUri());
        }

        // Otherwise resolve candidatePath as a relative identifier inside baseLocation
        Path resolvedPath = this.baseLocation.resolve(storageIdentifier).normalize();
        if (!resolvedPath.startsWith(this.baseLocation)) {
            throw new SecurityException("Attempted path traversal outside base storage directory: " + storageIdentifier);
        }

        try {
            Resource resource = new UrlResource(resolvedPath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new FileNotFoundException("File not found or unreadable on storage: " + storageIdentifier);
            }
        } catch (MalformedURLException e) {
            throw new FileNotFoundException("Invalid storage URL for identifier: " + storageIdentifier);
        }
    }

    @Override
    public boolean deleteFile(String storageIdentifier) {
        if (!StringUtils.hasText(storageIdentifier)) {
            return false;
        }
        try {
            Path candidatePath = Paths.get(storageIdentifier).normalize();
            if (candidatePath.isAbsolute() && Files.exists(candidatePath)) {
                return Files.deleteIfExists(candidatePath);
            }
            Path resolvedPath = this.baseLocation.resolve(storageIdentifier).normalize();
            if (resolvedPath.startsWith(this.baseLocation) && Files.exists(resolvedPath)) {
                return Files.deleteIfExists(resolvedPath);
            }
        } catch (Exception e) {
            log.warn("Failed to delete file with identifier {}: {}", storageIdentifier, e.getMessage());
        }
        return false;
    }

    @Override
    public boolean exists(String storageIdentifier) {
        if (!StringUtils.hasText(storageIdentifier)) {
            return false;
        }
        try {
            Path candidatePath = Paths.get(storageIdentifier).normalize();
            if (candidatePath.isAbsolute() && Files.exists(candidatePath)) {
                return true;
            }
            Path resolvedPath = this.baseLocation.resolve(storageIdentifier).normalize();
            return resolvedPath.startsWith(this.baseLocation) && Files.exists(resolvedPath);
        } catch (Exception e) {
            return false;
        }
    }

    public Path getBaseLocation() {
        return baseLocation;
    }
}
