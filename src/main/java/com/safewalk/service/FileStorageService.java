package com.safewalk.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Storage service abstraction for handling file persistence.
 * Decouples file storage from local filesystem paths, allowing
 * seamless transition to cloud object storage (e.g. OCI Object Storage)
 * without altering verification or user profile workflows.
 */
public interface FileStorageService {

    /**
     * Stores a file under the given relative directory and returns a portable storage identifier.
     *
     * @param subDirectory Target sub-directory (e.g. "documents/1" or "avatars")
     * @param originalFilename Cleaned original filename
     * @param file Uploaded multipart file
     * @return Portable relative storage identifier (e.g. "documents/1/uuid_filename.pdf")
     * @throws IOException If file persistence fails
     */
    String storeFile(String subDirectory, String originalFilename, MultipartFile file) throws IOException;

    /**
     * Loads a file resource given its storage identifier.
     * Supports both portable relative identifiers and legacy absolute paths for backward compatibility.
     *
     * @param storageIdentifier Portable storage identifier or legacy path
     * @return Readable Spring Resource
     * @throws IOException If the resource cannot be loaded or resolved
     */
    Resource loadAsResource(String storageIdentifier) throws IOException;

    /**
     * Deletes a stored file.
     *
     * @param storageIdentifier Portable storage identifier
     * @return true if deleted, false if file did not exist
     */
    boolean deleteFile(String storageIdentifier);

    /**
     * Checks if a file exists for the given storage identifier.
     *
     * @param storageIdentifier Portable storage identifier
     * @return true if exists and readable
     */
    boolean exists(String storageIdentifier);
}
