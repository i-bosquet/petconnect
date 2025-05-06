package com.petconnect.backend.common.helper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * Helper component to construct full, publicly accessible image URLs
 * from stored database paths for different entity types (e.g., users, pets).
 * It distinguishes between default images (served from classpath) and
 * uploaded images (served from external storage).
 *
 * @author ibosquet
 */
@Component
@Slf4j
public class ImageUrlHelper {
    @Value("${app.backend.base-url}")
    private String backendBaseUrl;

    /**
     * Constructs the full public URL for an image.
     *
     * @param imagePathInDb The path of the image as stored in the database.
     *                      (e.g., "images/avatars/users/default.png" or "users/avatars/uuid.png")
     * @param defaultImageDbPrefix The prefix used in the DB for default images of this type.
     *                             (e.g., "images/avatars/users/")
     * @param defaultImageUrlPrefix The public URL prefix for serving default images of this type from the classpath.
     *                              (e.g., "/images/avatars/users/")
     * @param uploadedImageDbSubdirectory The subdirectory string used by ImageService when storing uploaded images of this type.
     *                                   (e.g., "users/avatars")
     * @param uploadedImageUrlPrefix The public URL prefix for serving uploaded images of this type from external storage.
     *                                (e.g., "/storage/users/avatars/")
     * @param entityTypeForLog A string identifying the entity type for logging (e.g., "USER", "PET").
     * @param entityIdForLog The ID of the entity for logging.
     * @param fallbackDefaultImageUrl The full URL to a system-wide default image if imagePathInDb is null or empty.
     * @return The full public URL for the image, or the fallbackDefaultImageUrl.
     */
    public String buildFullImageUrl(
            String imagePathInDb,
            String defaultImageDbPrefix,
            String defaultImageUrlPrefix,
            String uploadedImageDbSubdirectory,
            String uploadedImageUrlPrefix,
            String entityTypeForLog,
            Long entityIdForLog,
            String fallbackDefaultImageUrl) {

        if (StringUtils.hasText(imagePathInDb)) {
            String baseUrl = backendBaseUrl.endsWith("/") ? backendBaseUrl.substring(0, backendBaseUrl.length() -1) : backendBaseUrl;
            String relativePathSegment; // The part after "baseUrl + /"

            if (defaultImageDbPrefix != null && imagePathInDb.startsWith(defaultImageDbPrefix)) {
                // Default image
                String filename = imagePathInDb.substring(defaultImageDbPrefix.length());
                relativePathSegment = defaultImageUrlPrefix.substring(1) + filename; // remove leading / from prefix
                log.trace("Mapping default {} avatar path '{}' to URL segment: {}", entityTypeForLog, imagePathInDb, relativePathSegment);

            } else if (uploadedImageDbSubdirectory != null && imagePathInDb.startsWith(uploadedImageDbSubdirectory + "/")) {
                // Uploaded image
                String filename = imagePathInDb.substring(uploadedImageDbSubdirectory.length() + 1); // +1 for the slash
                relativePathSegment = uploadedImageUrlPrefix.substring(1) + filename; // remove leading / from prefix
                log.trace("Mapping uploaded {} avatar path '{}' to URL segment: {}", entityTypeForLog, imagePathInDb, relativePathSegment);
            } else {
                log.warn("{} avatar path '{}' for ID {} does not match known default or specific uploaded prefixes. Attempting general /storage/ mapping.",
                        entityTypeForLog, imagePathInDb, entityIdForLog);
                relativePathSegment = "storage/" + imagePathInDb; // Generic fallback
            }
            return baseUrl + "/" + relativePathSegment;
        } else {
            log.warn("{} ID {} has null or empty image path in database. Using fallback.", entityTypeForLog, entityIdForLog);
            return fallbackDefaultImageUrl;
        }
    }
}