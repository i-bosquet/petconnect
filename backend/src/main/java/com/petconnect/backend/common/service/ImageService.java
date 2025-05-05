package com.petconnect.backend.common.service;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Service interface for handling image uploads and storage.
 *
 * @author ibosquet
 */
public interface ImageService {
    /**
     * Stores an uploaded image file within a specified subdirectory of the base image path.
     * Generates a unique filename to avoid collisions. Validates the file size and type.
     *
     * @param file         The uploaded image file (MultipartFile). Must not be null or empty.
     * @param subDirectory The subdirectory relative to the base external image path
     *                     where the image should be stored.
     *                     This path segment should not start or end with a slash.
     * @return The relative path (including subdirectory and unique filename) where the image was stored.
     *         This path is suitable for storing in the database. Example: "pets/avatars/uuid-image.jpg".
     * @throws IOException              if an error occurs during file writing.
     * @throws IllegalArgumentException if the file is invalid (null, empty, unsupported type, too large).
     */
    String storeImage(MultipartFile file, String subDirectory) throws IOException;

    /**
     * Deletes an image file specified by its relative path, but only if it's not a default image.
     * Checks against known default image paths before attempting deletion.
     *
     * @param relativePath The relative path of the image to delete, as stored in the database.
     */
    void deleteImage(String relativePath);

    /**
     * Constructs the full absolute path for a given relative image path.
     * Used internally to locate files for deletion.
     *
     * @param relativePath The relative path
     * @return The absolute Path object.
     */
    Path getAbsolutePath(String relativePath); // Helper may be useful
}
