package com.petconnect.backend.common.helper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * General utility methods for common tasks.
 * Provides helpers for conditional updates of entity fields.
 * This class cannot be instantiated.
 *
 * @author ibosquet
 */
@Slf4j
public class Utils {

    private static final List<String> ALLOWED_KEY_EXTENSIONS = List.of(".pem", ".crt");

    /**
     * Private constructor to prevent instantiation.
     */
    private Utils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Updates a target field using a setter if the source value is not null
     * and different from the current value obtained via a getter.
     * Specifically for updating fields on a target object.
     *
     * @param target The target object (e.g., the Pet entity).
     * @param sourceValue The new value from the DTO (can be null).
     * @param getter Supplier function to get the current value from the entity.
     * @param setter BiConsumer function to set the new value on the entity (accepts target and value).
     * @param <E> The type of the target entity.
     * @param <T> The type of the field being updated.
     * @return true if the setter was called (value was updated), false otherwise.
     */
    public static <E, T> boolean updateFieldIfChanged(E target, T sourceValue, Supplier<T> getter, BiConsumer<E, T> setter, String fieldName) {
        if (sourceValue == null) {
            log.debug("Skipping field '{}': Source value is null.", fieldName);
            return false;
        }
        T currentValue = getter.get();
        if (!Objects.equals(sourceValue, currentValue)) {
            log.debug("Updating field '{}': Current='{}', New='{}'", fieldName, currentValue, sourceValue);
            setter.accept(target, sourceValue);
            return true;
        } else {
            log.debug("Skipping field '{}': Value '{}' is the same as current.", fieldName, sourceValue);
            return false;
        }
    }

    /**
     * Updates a target String field using a setter if the source value is not null or blank
     * and different from the current value obtained via a getter.
     * Handles blank strings by setting the target field to null.
     *
     * @param target The target object (e.g., the Pet entity).
     * @param sourceValue The new String value from the DTO (can be null or blank).
     * @param getter Supplier function to get the current String value from the entity.
     * @param setter BiConsumer function to set the new String value on the entity.
     * @param <E> The type of the target entity.
     * @return true if the setter was called (value was updated), false otherwise.
     */
    public static <E> boolean updateStringFieldIfChanged(E target, String sourceValue, Supplier<String> getter, BiConsumer<E, String> setter, String fieldName) {
        if (sourceValue == null) {
            log.debug("Field '{}' not updated: Source value is null.", fieldName);
            return false;
        }

        // Treat blank source string as null for comparison and setting
        String effectiveSourceValue = sourceValue.isBlank() ? null : sourceValue;
        String currentValue = getter.get();

        if (!Objects.equals(effectiveSourceValue, currentValue)) {
            log.debug("Updating field '{}': Current='{}', New='{}' (Effective='{}')", fieldName, currentValue, sourceValue, effectiveSourceValue);
            setter.accept(target, effectiveSourceValue);
            return true;
        } else {
            log.debug("Skipping field '{}': Effective value '{}' is the same as current '{}'.", fieldName, effectiveSourceValue, currentValue);
            return false;
        }
    }

    /**
     * Validates the MIME type (by extension) of an uploaded key file.
     *
     * @param file The MultipartFile representing the uploaded key.
     * @throws IllegalArgumentException if the file's extension is not in the allowed list.
     */
    public static void validateKeyFileType(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf('.')).toLowerCase();
        }
        if (!ALLOWED_KEY_EXTENSIONS.contains(extension)) {
            log.error("Invalid key file extension: '{}'. Allowed extensions: {}", extension, ALLOWED_KEY_EXTENSIONS);
            throw new IllegalArgumentException("Invalid key file type. Please upload a .pem or .crt file. Found: " + extension);
        }
    }

    /**
     * Generates a final filename for a key file, ensuring a valid extension (.pem or .crt).
     *
     * @param file The uploaded MultipartFile.
     * @param desiredFilenameBase The base name for the file, without extension.
     * @return The final filename including a validated extension (e.g., "mykey.pem").
     */
    public static String generateFinalFilenameForKey(MultipartFile file, String desiredFilenameBase) {
        String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        String fileExtension = "";
        int extensionIndex = originalFilename.lastIndexOf('.');
        if (extensionIndex > 0) {
            fileExtension = originalFilename.substring(extensionIndex).toLowerCase();
        }
        if (!ALLOWED_KEY_EXTENSIONS.contains(fileExtension)) {
            fileExtension = ".pem"; // Default to .pem if extension is not allowed or missing
        }
        return StringUtils.cleanPath(desiredFilenameBase) + fileExtension;
    }
}
