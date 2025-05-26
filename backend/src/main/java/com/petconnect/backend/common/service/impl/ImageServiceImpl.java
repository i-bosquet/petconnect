package com.petconnect.backend.common.service.impl;

import com.petconnect.backend.common.helper.ValidateHelper;
import com.petconnect.backend.common.service.ImageService;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Implementation of ImageService for storing images in the local filesystem.
 * Uses the base path configured in application properties.
 * Active when the 'dev' profile is active
 *
 * @author ibosquet
 */
@Service
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class ImageServiceImpl implements ImageService {

    private final ValidateHelper validateHelper;

    @Value("${app.external.images.path}")
    private String externalImagesPathString;

    private Path baseStorageLocation;

    private static final String DEFAULT_IMAGE_PREFIX = "images/avatars/";

    /**
     * Initializes the base storage location after dependency injection.
     * Creates the directory if it doesn't exist.
     *
     * @throws IOException If the storage directory cannot be created.
     */
    @PostConstruct
    public void init() throws IOException {
        if (!StringUtils.hasText(this.externalImagesPathString)) {
            throw new IllegalStateException("Configuration property 'app.external.images.path' cannot be empty.");
        }
        this.baseStorageLocation = Paths.get(this.externalImagesPathString).toAbsolutePath().normalize();
        log.info("Base image storage location initialized at: {}", this.baseStorageLocation);
        try {
            Files.createDirectories(this.baseStorageLocation);
            log.debug("Ensured base storage directory exists: {}", this.baseStorageLocation);
        } catch (IOException e) {
            log.error("Could not create image storage directory: {}", this.baseStorageLocation, e);
            throw e;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String storeImage(MultipartFile file, String subDirectory) throws IOException {
        // Validate Input
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Image file cannot be null or empty.");
        }
        if (!StringUtils.hasText(subDirectory)) {
            throw new IllegalArgumentException("Subdirectory for image storage cannot be blank.");
        }
        validateHelper.validateFileSize(file);
        validateHelper.validateFileType(file);

        // Sanitize a subdirectory path (prevent directory traversal)
        String cleanSubDirectory = StringUtils.cleanPath(subDirectory);
        if (cleanSubDirectory.contains("..")) {
            throw new IllegalArgumentException("Invalid subdirectory path provided.");
        }

        // Generate Unique Filename
        String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        String fileExtension = "";
        int extensionIndex = originalFilename.lastIndexOf('.');
        if (extensionIndex > 0) {
            fileExtension = originalFilename.substring(extensionIndex);
        }
        // Ensure extension is one of the allowed ones (defensive check)
        if (!List.of(".jpg", ".jpeg", ".png", ".gif").contains(fileExtension.toLowerCase())) {
            throw new IllegalArgumentException("Invalid file extension derived from filename: " + fileExtension);
        }

        String uniqueFilename = UUID.randomUUID() + fileExtension;

        // Resolve Destination Path
        Path targetDirectory = this.baseStorageLocation.resolve(cleanSubDirectory).normalize();
        // Ensure the target directory is within the base storage location
        if (!targetDirectory.startsWith(this.baseStorageLocation)) {
            throw new IllegalArgumentException("Calculated target directory is outside base storage location.");
        }
        Files.createDirectories(targetDirectory); // Create a subdirectory if needed
        Path destinationFile = targetDirectory.resolve(uniqueFilename).normalize();

        // Copy File
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            log.info("Successfully stored image '{}' to {}", uniqueFilename, destinationFile);
        } catch (IOException e) {
            log.error("Failed to store image file '{}': {}", uniqueFilename, e.getMessage(), e);
            throw e;
        }

        // Return Relative Path for DB Storage. Combine subdirectory and filename
        String relativePath = Paths.get(cleanSubDirectory, uniqueFilename).toString().replace("\\", "/");
        log.debug("Returning relative path for stored image: {}", relativePath);
        return relativePath;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteImage(String relativePath) {
        if (!StringUtils.hasText(relativePath)) {
            log.warn("Attempted to delete image with empty or null path. Skipping.");
            return;
        }

        // Prevent deleting default images
        String normalizedRelativePath = relativePath.replace("\\", "/");
        if (normalizedRelativePath.startsWith(DEFAULT_IMAGE_PREFIX)) {
            log.warn("Deletion skipped: Path '{}' appears to be a default image (starts with '{}').",
                    normalizedRelativePath, DEFAULT_IMAGE_PREFIX);
            return;
        }

        try {
            Path absolutePath = getAbsolutePath(normalizedRelativePath);
            if (!absolutePath.startsWith(this.baseStorageLocation)) {
                log.error("Security risk: Attempted to delete file outside base storage directory: {}", absolutePath);
                return;
            }

            boolean deleted = Files.deleteIfExists(absolutePath);
            if (deleted) {
                log.info("Successfully deleted image file: {}", absolutePath);
            } else {
                log.warn("Image file not found for deletion or already deleted: {}", absolutePath);
            }
        } catch (IOException e) {
            log.error("Failed to delete image file '{}': {}", relativePath, e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            log.error("Invalid path provided for deletion '{}': {}", relativePath, e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
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
}