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

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @Value("${aws.s3.bucket.images.public-url:#{null}}")
    private String s3ImagesPublicBaseUrl;

    /**
     * Builds the full URL of an image based on its path in the database and a set of associated prefixes and configurations.
     *
     * @param imagePathInDb The image path stored in the database. May represent a default image, an uploaded image, or an unrecognized path requiring fallback handling.
     * @param defaultImageDbPrefix The prefix identifying default images in the database path. Used to detect and construct URLs for default images.
     * @param defaultImageUrlPrefix The URL prefix to prepend to default image paths when constructing their full URLs.
     * @param uploadedImageDbSubdirectory The subdirectory where uploaded images are stored in the database. Used to detect and construct URLs for uploaded images.
     * @param uploadedImageUrlPrefix The URL prefix to prepend to uploaded image paths when constructing their full URLs in the dev environment.
     * @param entityTypeForLog The type of the associated entity, used for logging purposes.
     * @param entityIdForLog The identifier of the associated entity, used for logging purposes.
     * @param fallbackDefaultImageUrl The fallback image URL to return if the image path is null, empty, or doesn't match known patterns.
     * @return The full URL of the image constructed based on the input paths and prefixes. Returns the fallback URL if the input path is null, empty, or unrecognized.
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

        if (!StringUtils.hasText(imagePathInDb)) {
            log.warn("{} ID {} has null or empty image path in database. Using fallback: {}", entityTypeForLog, entityIdForLog, fallbackDefaultImageUrl);
            return fallbackDefaultImageUrl;
        }
        String baseUrl;
        String relativePathSegment;

        // It is a default image (packaged in the JAR)
        if (StringUtils.hasText(defaultImageDbPrefix) && imagePathInDb.startsWith(defaultImageDbPrefix)) {
            baseUrl = this.backendBaseUrl; // Always served by the backend
            String filename = imagePathInDb.substring(defaultImageDbPrefix.length());
            relativePathSegment = (defaultImageUrlPrefix.startsWith("/") ? defaultImageUrlPrefix.substring(1) : defaultImageUrlPrefix) + filename;
            log.trace("Default image: pathInDb='{}', baseUrl='{}', relativePathSegment='{}'", imagePathInDb, baseUrl, relativePathSegment);

        }
        // It is an uploaded image
        else if (StringUtils.hasText(uploadedImageDbSubdirectory) && imagePathInDb.startsWith(StringUtils.cleanPath(uploadedImageDbSubdirectory) + "/")) {
            if ("prod".equalsIgnoreCase(activeProfile)) {
                if (!StringUtils.hasText(s3ImagesPublicBaseUrl)) {
                    log.error("S3 images public base URL (aws.s3.bucket.images.public-url) is not configured for prod profile for uploaded image '{}'!", imagePathInDb);
                    return fallbackDefaultImageUrl;
                }
                baseUrl = this.s3ImagesPublicBaseUrl;
                // It doesn't need the uploadedImageDbSubdirectory prefix here because the S3 key already includes it.
                relativePathSegment = imagePathInDb;
                log.trace("Uploaded image (prod): S3 key='{}', baseUrl='{}', relativePathSegment='{}'", imagePathInDb, baseUrl, relativePathSegment);
            } else { // Profile "dev"
                baseUrl = this.backendBaseUrl;
                if (uploadedImageUrlPrefix.startsWith("/"))
                    relativePathSegment = uploadedImageUrlPrefix.substring(1) + imagePathInDb;
                else relativePathSegment = uploadedImageUrlPrefix + imagePathInDb;
                log.trace("Uploaded image (dev): pathInDb='{}', baseUrl='{}', relativePathSegment='{}'", imagePathInDb, baseUrl, relativePathSegment);
            }
        }
        // Fallback or unrecognized route
        else {
            log.warn("{} path '{}' for ID {} did not match known patterns. Using generic /storage/ or fallback.",
                    entityTypeForLog, imagePathInDb, entityIdForLog);
            if ("dev".equalsIgnoreCase(activeProfile)) {
                baseUrl = this.backendBaseUrl;
                relativePathSegment = (uploadedImageUrlPrefix.startsWith("/") ? uploadedImageUrlPrefix.substring(1) : uploadedImageUrlPrefix) + imagePathInDb;
                log.trace("Fallback to dev storage path: baseUrl='{}', relativePathSegment='{}'", baseUrl, relativePathSegment);
            } else {
                return fallbackDefaultImageUrl;
            }
        }

        // Assemble the final URL
        String cleanBase = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String cleanPath = relativePathSegment.startsWith("/") ? relativePathSegment.substring(1) : relativePathSegment;

        return cleanBase + "/" + cleanPath;
    }
}