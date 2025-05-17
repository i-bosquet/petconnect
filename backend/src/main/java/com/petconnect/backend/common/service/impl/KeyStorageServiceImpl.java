package com.petconnect.backend.common.service.impl;

import com.petconnect.backend.common.service.KeyStorageService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.List;
import java.util.Objects;

/**
 * Implementation of KeyStorageService for storing key files in the local filesystem.
 * Uses the base path configured in application properties ('app.external.keys.path').
 *
 * @author ibosquet
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KeyStorageServiceImpl implements KeyStorageService {

    @Value("${app.external.pub.keys.path}")
    private String externalPublicKeysPathString;

    @Value("${app.external.pri.keys.path}")
    private String externalPrivateKeysPathString;

    private Path publicKeysBaseLocation;
    private Path privateKeysBaseLocation;

    private static final List<String> ALLOWED_KEY_EXTENSIONS = List.of(".pem", ".crt");
    private static final String DEFAULT_PUBLIC_KEY_SUBDIRECTORY = "public keys";
    private static final String DEFAULT_PRIVATE_KEY_SUBDIRECTORY = "private keys";

    @PostConstruct
    public void init() throws IOException {
        this.publicKeysBaseLocation = initializeBaseLocation(this.externalPublicKeysPathString, DEFAULT_PUBLIC_KEY_SUBDIRECTORY);
        this.privateKeysBaseLocation = initializeBaseLocation(this.externalPrivateKeysPathString, DEFAULT_PRIVATE_KEY_SUBDIRECTORY);
    }

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

    @Override
    public String storePublicKey(MultipartFile file, String subDirectory, String desiredFilenameBase) throws IOException {
        return storeKeyFile(file, subDirectory, desiredFilenameBase, this.publicKeysBaseLocation);
    }

    @Override
    public String storeEncryptedPrivateKey(MultipartFile file, String subDirectory, String desiredFilenameBase) throws IOException {
        return storeKeyFile(file, subDirectory, desiredFilenameBase, this.privateKeysBaseLocation);
    }

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

        validateKeyFileType(file);

        String cleanSubDirectory = validateAndCleanPath(subDirectory, baseDir);
        String finalFilename = generateFinalFilename(file, desiredFilenameBase);
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

    @Override
    public void deleteKey(String relativePath) {
        deleteKeyFromLocation(relativePath, this.publicKeysBaseLocation);
        deleteKeyFromLocation(relativePath, this.privateKeysBaseLocation);
    }

    @Override
    public Path getAbsolutePathForPublicKey(String relativePublicKeyPath) {
        return getAbsolutePath(relativePublicKeyPath, this.publicKeysBaseLocation);
    }

    @Override
    public Path getAbsolutePathForPrivateKey(String relativePrivateKeyPath) {
        return getAbsolutePath(relativePrivateKeyPath, this.privateKeysBaseLocation);
    }

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

    private void validateKeyFileType(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf('.')).toLowerCase();
        }

        if (ALLOWED_KEY_EXTENSIONS.contains(extension)) {
            return;
        }

        log.error("Invalid key file extension: '{}'. Allowed extensions: {}", extension, ALLOWED_KEY_EXTENSIONS);
        throw new IllegalArgumentException("Invalid key file type. Please upload a .pem or .crt file.");
    }

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

    private String generateFinalFilename(MultipartFile file, String desiredFilenameBase) {
        String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        String fileExtension = "";
        int extensionIndex = originalFilename.lastIndexOf('.');
        if (extensionIndex > 0) {
            fileExtension = originalFilename.substring(extensionIndex).toLowerCase();
        }
        if (!ALLOWED_KEY_EXTENSIONS.contains(fileExtension)) {
            fileExtension = ".pem";
        }
        return StringUtils.cleanPath(desiredFilenameBase) + fileExtension;
    }

    private void ensureTargetDirectory(Path targetDirectory, Path baseDir) throws IOException {
        if (!targetDirectory.startsWith(baseDir)) {
            throw new IllegalArgumentException("Calculated target directory is outside base key storage location.");
        }
        Files.createDirectories(targetDirectory);
    }

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