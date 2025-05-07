package com.petconnect.backend.common.service;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Service interface for handling storage of cryptographic key files.
 *
 * @author ibosquet
 */
public interface KeyStorageService {
    /**
     * Stores an uploaded public key file (.pem/.crt) within a specified subdirectory
     * of the base key storage path. Generates a safe filename. Validates a file type.
     *
     * @param file         The uploaded key file (MultipartFile). Must not be null or empty.
     * @param subDirectory The subdirectory relative to the base external key path
     *                     (e.g., "vets", "clinics").
     * @param desiredFilename The base filename to use (e.g., "vet_username_pub"). Extension will be added.
     * @return The relative path (including subdirectory and filename) where the key was stored.
     *         Suitable for storing in the database. Example: "vets/vet_username_pub.pem".
     * @throws IOException              if an error occurs during file writing.
     * @throws IllegalArgumentException if the file is invalid (null, empty, unsupported type).
     */
    String storePublicKey(MultipartFile file, String subDirectory, String desiredFilename) throws IOException;

    /**
     * Deletes a key file specified by its relative path.
     *
     * @param relativePath The relative path of the key file to delete, as stored in the database.
     */
    void deleteKey(String relativePath);

    /**
     * Constructs the full absolute path for a given relative key path.
     *
     * @param relativePath The relative path.
     * @return The absolute Path object.
     */
    Path getAbsolutePath(String relativePath);

    /**
     * Reads the content of a public key file specified by its relative path.
     *
     * @param relativePath The relative path of the key file to read.
     * @return The content of the file as a String.
     * @throws IOException If an error occurs, reading the file or the file doesn't exist.
     */
    String readPublicKeyContent(String relativePath) throws IOException;
}
