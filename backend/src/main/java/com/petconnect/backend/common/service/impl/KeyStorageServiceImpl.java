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
import java.nio.charset.StandardCharsets;
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
    @Value("${app.external.keys.path}")
    private String externalKeysPathString;

    private Path baseStorageLocation;

    private static final List<String> ALLOWED_KEY_TYPES = List.of(
            "application/x-x509-ca-cert", // .crt
            "application/pkix-cert",       // .crt
            "application/x-pem-file",     // .pem (common but may vary)
            "application/pkcs8",          // Sometimes used for PEM keys
            "text/plain"                  // Often used for PEM files
    );
    private static final List<String> ALLOWED_KEY_EXTENSIONS = List.of(".pem", ".crt");

    @PostConstruct
    public void init() throws IOException {
        if (!StringUtils.hasText(this.externalKeysPathString)) {
            throw new IllegalStateException("Configuration property 'app.external.keys.path' cannot be empty.");
        }
        this.baseStorageLocation = Paths.get(this.externalKeysPathString).toAbsolutePath().normalize();
        log.info("Base key storage location initialized at: {}", this.baseStorageLocation);
        try {
            Files.createDirectories(this.baseStorageLocation);
            log.debug("Ensured base key storage directory exists: {}", this.baseStorageLocation);
        } catch (IOException e) {
            log.error("Could not create key storage directory: {}", this.baseStorageLocation, e);
            throw e;
        }
    }

    @Override
    public String storePublicKey(MultipartFile file, String subDirectory, String desiredFilenameBase) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Key file cannot be null or empty.");
        }
        if (!StringUtils.hasText(subDirectory)) {
            throw new IllegalArgumentException("Subdirectory for key storage cannot be blank.");
        }
        if (!StringUtils.hasText(desiredFilenameBase)) {
            throw new IllegalArgumentException("Desired filename base cannot be blank.");
        }

        validateFileType(file);

        String cleanSubDirectory = StringUtils.cleanPath(subDirectory);
        if (cleanSubDirectory.contains("..")) {
            throw new IllegalArgumentException("Invalid subdirectory path provided.");
        }

        String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        String fileExtension = "";
        int extensionIndex = originalFilename.lastIndexOf('.');
        if (extensionIndex > 0) {
            fileExtension = originalFilename.substring(extensionIndex).toLowerCase();
        }
        if (!ALLOWED_KEY_EXTENSIONS.contains(fileExtension)) {
            log.warn("File extension '{}' not in allowed list, defaulting to .pem", fileExtension);
            fileExtension = ".pem";
        }

        String finalFilename = StringUtils.cleanPath(desiredFilenameBase) + fileExtension;


        Path targetDirectory = this.baseStorageLocation.resolve(cleanSubDirectory).normalize();
        if (!targetDirectory.startsWith(this.baseStorageLocation)) {
            throw new IllegalArgumentException("Calculated target directory is outside base key storage location.");
        }
        Files.createDirectories(targetDirectory);
        Path destinationFile = targetDirectory.resolve(finalFilename).normalize();

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            log.info("Successfully stored public key '{}' to {}", finalFilename, destinationFile);
        } catch (IOException e) {
            log.error("Failed to store key file '{}': {}", finalFilename, e.getMessage(), e);
            throw e;
        }

        String relativePath = Paths.get(cleanSubDirectory, finalFilename).toString().replace("\\", "/");
        log.debug("Returning relative path for stored key: {}", relativePath);
        return relativePath;
    }

    @Override
    public void deleteKey(String relativePath) {
        if (!StringUtils.hasText(relativePath)) {
            log.warn("Attempted to delete key with empty or null path. Skipping.");
            return;
        }
        try {
            Path absolutePath = getAbsolutePath(relativePath);
            if (!absolutePath.startsWith(this.baseStorageLocation)) {
                log.error("Security risk: Attempted to delete file outside base key storage directory: {}", absolutePath);
                return;
            }
            boolean deleted = Files.deleteIfExists(absolutePath);
            if (deleted) {
                log.info("Successfully deleted key file: {}", absolutePath);
            } else {
                log.warn("Key file not found for deletion or already deleted: {}", absolutePath);
            }
        } catch (IOException | IllegalArgumentException e) {
            log.error("Failed to delete key file '{}': {}", relativePath, e.getMessage(), e);
        }
    }

    @Override
    public Path getAbsolutePath(String relativePath) {
        if (!StringUtils.hasText(relativePath)) {
            throw new IllegalArgumentException("Relative path cannot be empty or null.");
        }
        String cleanRelativePath = StringUtils.cleanPath(relativePath);
        if (cleanRelativePath.contains("..")) {
            throw new IllegalArgumentException("Invalid relative path containing '..'.");
        }
        return this.baseStorageLocation.resolve(cleanRelativePath).normalize();
    }

    @Override
    public String readPublicKeyContent(String relativePath) throws IOException {
        Path absolutePath = getAbsolutePath(relativePath);
        if (!Files.exists(absolutePath)) {
            log.error("Public key file not found at path: {}", absolutePath);
            throw new IOException("Public key file not found: " + relativePath);
        }
        if (!absolutePath.startsWith(this.baseStorageLocation)) {
            log.error("Security risk: Attempted to read file outside base key storage directory: {}", absolutePath);
            throw new IOException("Access denied to key file path.");
        }
        try {
            return Files.readString(absolutePath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Failed to read public key content from {}: {}", absolutePath, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Validates the MIME type or extension of the uploaded key file.
     */
    private void validateFileType(MultipartFile file) {
        String contentType = file.getContentType();
        String filename = file.getOriginalFilename();
        String extension = "";
        if (filename != null && filename.contains(".")) {
            extension = filename.substring(filename.lastIndexOf('.')).toLowerCase();
        }

        if ((contentType != null && ALLOWED_KEY_TYPES.contains(contentType.toLowerCase())) ||
                ALLOWED_KEY_EXTENSIONS.contains(extension)) {
            return;
        }

        log.error("Invalid key file type: ContentType='{}', Extension='{}'. Allowed types: {}, Allowed extensions: {}",
                contentType, extension, ALLOWED_KEY_TYPES, ALLOWED_KEY_EXTENSIONS);
        throw new IllegalArgumentException("Invalid key file type. Please upload a .pem or .crt file.");
    }
}
