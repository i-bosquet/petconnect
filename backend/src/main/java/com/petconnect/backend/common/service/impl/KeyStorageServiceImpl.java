package com.petconnect.backend.common.service.impl;

import com.petconnect.backend.common.helper.Utils;
import com.petconnect.backend.common.service.KeyStorageService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.nio.file.*;
/**
 * Implementation of KeyStorageService for storing key files in the local filesystem.
 * Uses the base path configured in application properties ('app.external.keys.path').
 *
 * @author ibosquet
 */
@Service
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class KeyStorageServiceImpl implements KeyStorageService {

    @Value("${app.external.pub.keys.path}")
    private String externalPublicKeysPathString;

    @Value("${app.external.pri.keys.path}")
    private String externalPrivateKeysPathString;

    private Path publicKeysBaseLocation;
    private Path privateKeysBaseLocation;

    private static final String DEFAULT_PUBLIC_KEY_SUBDIRECTORY = "public keys";
    private static final String DEFAULT_PRIVATE_KEY_SUBDIRECTORY = "private keys";

    @PostConstruct
    public void init() throws IOException {
        this.publicKeysBaseLocation = initializeBaseLocation(this.externalPublicKeysPathString, DEFAULT_PUBLIC_KEY_SUBDIRECTORY);
        this.privateKeysBaseLocation = initializeBaseLocation(this.externalPrivateKeysPathString, DEFAULT_PRIVATE_KEY_SUBDIRECTORY);
    }

    @Override
    public String storePublicKey(MultipartFile file, String subDirectory, String desiredFilenameBase) throws IOException {
        return storeKeyFile(file, subDirectory, desiredFilenameBase, this.publicKeysBaseLocation);
    }

    @Override
    public String storeEncryptedPrivateKey(MultipartFile file, String subDirectory, String desiredFilenameBase) throws IOException {
        return storeKeyFile(file, subDirectory, desiredFilenameBase, this.privateKeysBaseLocation);
    }

    @Override
    public void deleteKey(String relativePath) {
        deleteKeyFromLocation(relativePath, this.publicKeysBaseLocation);
        deleteKeyFromLocation(relativePath, this.privateKeysBaseLocation);
    }

    @Override
    public InputStream getPublicKeyContent(String relativePublicKeyPath) throws IOException {
        Path absolutePath = getAbsolutePathForPublicKey(relativePublicKeyPath);
        if (!Files.exists(absolutePath) || !Files.isReadable(absolutePath)) {
            throw new IOException("Public key file not found or not readable at: " + absolutePath);
        }
        return new FileInputStream(absolutePath.toFile());
    }

    @Override
    public InputStream getPrivateKeyContent(String relativePrivateKeyPath) throws IOException {
        Path absolutePath = getAbsolutePathForPrivateKey(relativePrivateKeyPath);
        if (!Files.exists(absolutePath) || !Files.isReadable(absolutePath)) {
            throw new IOException("Private key file not found or not readable at: " + absolutePath);
        }
        return new FileInputStream(absolutePath.toFile());
    }

    @Override
    public Path getAbsolutePathForPublicKey(String relativePublicKeyPath) {
        return getAbsolutePath(relativePublicKeyPath, this.publicKeysBaseLocation);
    }

    @Override
    public Path getAbsolutePathForPrivateKey(String relativePrivateKeyPath) {
        return getAbsolutePath(relativePrivateKeyPath, this.privateKeysBaseLocation);
    }

    // Private methods

    /**
     * Initializes the base storage location based on the provided path string and key type description.
     * This method ensures the directory exists by creating it if it does not already exist.
     *
     * @param pathString the string representation of the path to initialize
     * @param keyTypeDescription a description of the key type, used for logging purposes
     * @return the resolved and normalized Path object for the base location
     * @throws IOException if an I/O error occurs while creating the directory
     * @throws IllegalStateException if the provided path string is empty or null
     */
    private Path initializeBaseLocation(String pathString, String keyTypeDescription) throws IOException {
        if (!StringUtils.hasText(pathString)) {
            throw new IllegalStateException("Configuration property for " + keyTypeDescription + " path cannot be empty.");
        }
        Path location = Paths.get(pathString).toAbsolutePath().normalize();
        log.info("Base {} storage location initialized at: {}", keyTypeDescription, location);
        try {
            Files.createDirectories(location);
            log.debug("Ensured base {} storage directory exists: {}", keyTypeDescription, location);
        } catch (IOException e) {
            log.error("Could not create {} storage directory: {}", keyTypeDescription, location, e);
            throw e;
        }
        return location;
    }

    /**
     * Stores the provided key file in the specified directory, using the given base directory
     * and filename structure. Validates a file type, directory, and filename, and ensures the
     * necessary directory structure exists. Copies the file to the target location, replacing
     * any existing file with the same name.
     *
     * @param file the key file to be stored; must not be null or empty
     * @param subDirectory the subdirectory within the base directory to store the file;
     *                     must not be blank
     * @param desiredFilenameBase the desired base name for the stored file; must not be blank
     * @param baseDir the base directory where the file will be stored; must not be null
     * @return the relative path of the stored file as a string, with directories normalized
     * @throws IOException if an I/O error occurs while storing the file
     * @throws IllegalArgumentException if the file is null, empty, or invalid, or if the
     *                                  subdirectory or desired filename base is blank
     */
    private String storeKeyFile(MultipartFile file, String subDirectory, String desiredFilenameBase, Path baseDir) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Key file cannot be null or empty.");
        }
        if (!StringUtils.hasText(subDirectory)) {
            throw new IllegalArgumentException("Subdirectory for key storage cannot be blank.");
        }
        if (!StringUtils.hasText(desiredFilenameBase)) {
            throw new IllegalArgumentException("Desired filename base cannot be blank.");
        }

        Utils.validateKeyFileType(file);

        String cleanSubDirectory = validateAndCleanPath(subDirectory, baseDir);
        String finalFilename = Utils.generateFinalFilenameForKey(file, desiredFilenameBase);
        Path targetDirectory = baseDir.resolve(cleanSubDirectory).normalize();

        ensureTargetDirectory(targetDirectory, baseDir);
        Path destinationFile = targetDirectory.resolve(finalFilename).normalize();

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            log.info("Successfully stored key file '{}' to {}", finalFilename, destinationFile);
        } catch (IOException e) {
            log.error("Failed to store key file '{}': {}", finalFilename, e.getMessage(), e);
            throw e;
        }
        return Paths.get(cleanSubDirectory, finalFilename).toString().replace("\\", "/");
    }

    /**
     * Deletes a file specified by a relative path within a base location if it exists. Ensures the file path is within
     * the bounds of the provided base location to avoid security risks, such as deleting files outside the allowed directory.
     *
     * @param relativePath the relative path of the file to be deleted relative to the base location. Should not be null or empty.
     * @param baseLocation the base directory against which the relative path is resolved. Used to ensure the deletion is restricted within this directory.
     */
    private void deleteKeyFromLocation(String relativePath, Path baseLocation) {
        if (!StringUtils.hasText(relativePath)) {
            log.warn("Attempted to delete key with empty or null path from {}. Skipping.", baseLocation);
            return;
        }
        try {
            log.debug("deleteKeyFromLocation: Attempting to delete relativePath '{}' from baseLocation '{}'", relativePath, baseLocation);
            Path absolutePath = getAbsolutePath(relativePath, baseLocation);
            log.debug("deleteKeyFromLocation: Resolved absolutePath: '{}'", absolutePath);
            if (!absolutePath.startsWith(baseLocation)) {
                log.error("Security risk: Attempted to delete file outside base key storage directory ({}): {}", baseLocation, absolutePath);
                return;
            }
            boolean startsWithBase = absolutePath.startsWith(baseLocation);
            log.debug("deleteKeyFromLocation: Does absolutePath start with baseLocation? {}", startsWithBase);

            if (!startsWithBase) {
                    log.error("Security risk: Attempted to delete file outside base key storage directory. Base: '{}', Absolute: '{}'", baseLocation, absolutePath);
                    return;
                }
            boolean fileExists = Files.exists(absolutePath);
            log.debug("deleteKeyFromLocation: Does file at absolutePath exist before deletion attempt? {}", fileExists);

            if (fileExists) {
                boolean deleted = Files.deleteIfExists(absolutePath);
                if (deleted) {
                    log.info("Successfully deleted key file: {}", absolutePath);
                } else {
                    log.warn("Key file not found for deletion or already deleted from ({}): {}", baseLocation, absolutePath);
                }
            }else {
                log.warn("Key file not found for deletion at resolved absolutePath: {}", absolutePath);
            }
        } catch (IOException | IllegalArgumentException e) {
            log.error("Failed to delete key file '{}' from {}: {}", relativePath, baseLocation, e.getMessage(), e);
        }
    }

    /**
     * Validates and sanitizes a path segment against a base directory. Ensures the provided path segment
     * does not traverse outside the bounds of the specified base directory or include illegal path elements,
     * such as "." or absolute paths.
     *
     * @param pathSegment the unvalidated path segment to be processed. Should not be null or empty.
     * @param baseDir the base directory against which the path segment is resolved. Used for security checks
     *                to ensure the resolved path does not escape the base directory.
     * @return the sanitized and validated path segment if it meets all validation rules.
     * @throws IllegalArgumentException if the path segment is invalid, contains illegal path elements, or resolves
     *                                  to a location outside the specified base directory.
     */
    private String validateAndCleanPath(String pathSegment, Path baseDir) {
        String cleanedSegment = StringUtils.cleanPath(pathSegment);
        if (cleanedSegment.contains("..") || Paths.get(cleanedSegment).isAbsolute()) {
            throw new IllegalArgumentException("Invalid path segment provided: " + pathSegment);
        }
        Path resolvedPath = baseDir.resolve(cleanedSegment).normalize();
        if (!resolvedPath.startsWith(baseDir)) {
            throw new IllegalArgumentException("Calculated target directory is outside base storage location.");
        }
        return cleanedSegment;
    }

    /**
     * Ensures the existence of the target directory within a specified base directory.
     * Validates that the target directory is properly nested within the base directory
     * to prevent traversing outside allowed storage boundaries.
     *
     * @param targetDirectory the target directory to be created if it does not already exist. Must be a valid path
     *                        and located within the boundaries of the base directory.
     * @param baseDir the base directory used as the root context for validation. Ensures that the target directory
     *                is nested within this directory.
     * @throws IllegalArgumentException if the target directory is outside the base directory.
     * @throws IOException if an I/O error occurs while creating the directory.
     */
    private void ensureTargetDirectory(Path targetDirectory, Path baseDir) throws IOException {
        if (!targetDirectory.startsWith(baseDir)) {
            throw new IllegalArgumentException("Calculated target directory is outside base key storage location.");
        }
        Files.createDirectories(targetDirectory);
    }

    /**
     * Resolves the given relative path against the specified base location and returns an absolute path.
     * Ensures the resolved path is properly normalized and does not contain invalid elements like "."
     * @param relativePath the relative path to be resolved; must not be null, empty, or contain invalid elements like "."
     * @param baseLocation the base location against which the relative path will be resolved; must not be null
     * @return the resolved absolute path as a {@code Path} object
     * @throws IllegalArgumentException if the relative path is null, empty, or contains invalid elements
     */
    private Path getAbsolutePath(String relativePath, Path baseLocation) {
        if (!StringUtils.hasText(relativePath)) {
            throw new IllegalArgumentException("Relative path cannot be empty or null.");
        }
        String cleanRelativePath = StringUtils.cleanPath(relativePath);
        if (cleanRelativePath.contains("..")) {
            throw new IllegalArgumentException("Invalid relative path containing '..'. Path: " + relativePath);
        }
        return baseLocation.resolve(cleanRelativePath).normalize();
    }
}