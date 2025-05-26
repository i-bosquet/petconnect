package com.petconnect.backend.common.service;

import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;
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
     * Stores an uploaded encrypted private key file within a specified subdirectory
     * of the base key storage path. Generates a secure filename based on the desired filename base
     * and validates the file type before storage.
     *
     * @param file                The uploaded encrypted private key file (MultipartFile). Must not be null or empty.
     * @param subDirectory        The subdirectory relative to the base external key path (e.g., "users/keys").
     * @param desiredFilenameBase The base filename to use when generating the stored file's name
     *                            (e.g., "user_private_key"). The full name will include an appropriate extension.
     * @return The relative path (including subdirectory and filename) where the key was stored.
     *         This path can be used for referencing the file in the database. Example: "users/keys/user_private_key.enc".
     * @throws IOException              If an error occurs during file writing.
     * @throws IllegalArgumentException If the file is invalid (null, empty, unsupported type).
     */
    String storeEncryptedPrivateKey(MultipartFile file, String subDirectory, String desiredFilenameBase) throws IOException;

    /**
     * Deletes a key file specified by its relative path.
     *
     * @param relativePath The relative path of the key file to delete, as stored in the database.
     */
    void deleteKey(String relativePath);

    /**
     * Retrieves the content of a public key file as an InputStream.
     *
     * @param publicKeyS3Key The S3 key (or relative path for dev) of the public key file.
     * @return An InputStream containing the public key content.
     * @throws IOException If the key cannot be found or read.
     */
    InputStream getPublicKeyContent(String publicKeyS3Key) throws IOException;

    /**
     * Retrieves the content of an encrypted private key file as an InputStream.
     *
     * @param privateKeyS3Key The S3 key (or relative path for dev) of the encrypted private key file.
     * @return An InputStream containing the encrypted private key content.
     * @throws IOException If the key cannot be found or read.
     */
    InputStream getPrivateKeyContent(String privateKeyS3Key) throws IOException;

    /**
     * Gets the absolute path for a given relative public key path.
     *
     * @param relativePublicKeyPath The relative path of the public key file.
     * @return The absolute Path object.
     */
    Path getAbsolutePathForPublicKey(String relativePublicKeyPath);

    /**
     * Gets the absolute path for a given relative (encrypted) private key path.
     *
     * @param relativePrivateKeyPath The relative path of the private key file.
     * @return The absolute Path object.
     */
    Path getAbsolutePathForPrivateKey(String relativePrivateKeyPath);
}
