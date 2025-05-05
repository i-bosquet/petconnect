package com.petconnect.backend.config;

import io.micrometer.common.lang.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Configures Spring MVC resource handling.
 * Specifically, sets up handlers for serving static images:
 * - Default avatars located within the application's classpath.
 * - UserEntity-uploaded avatars stored in an external directory specified by the
 *    'app.external.images.path' application property.
 *
 * @author ibosquet
 */
@Configuration
@Slf4j
public class MvcConfig implements WebMvcConfigurer {

    /**
     * Base path (filesystem) where user-uploaded images are stored.
     * Injected from the 'app.external.images.path' property.
     */
    @Value("${app.external.images.path}")
    private String externalImagesPath;

    /**
     * Public URL path prefix used to access uploaded images.
     * Example: A request to /storage/pets/avatars/img.jpg will be served
     * from the file located at externalImagesPath/pets/avatars/img.jpg.
     */
    private static final String UPLOADED_IMAGES_URL_PATTERN = "/storage/**";

    /**
     * Public URL path prefix used to access default images packaged in the JAR.
     * Example: A request to /images/avatars/users/owner.png will be served
     * from classpath:/static/images/avatars/users/owner.png.
     */
    private static final String DEFAULT_IMAGES_URL_PATTERN = "/images/**";

    /**
     * Classpath location where default images are stored.
     */
    private static final String DEFAULT_IMAGES_CLASSPATH_LOCATION = "classpath:/static/images/";

    /**
     * Configures resource handlers.
     *
     * @param registry The registry to add resource handlers to.
     */
    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {

        // --- Handler for Default Images (from Classpath) ---
        if (!registry.hasMappingForPattern(DEFAULT_IMAGES_URL_PATTERN)) {
            log.info("Configuring resource handler for default images: URL '{}' -> Classpath '{}'",
                    DEFAULT_IMAGES_URL_PATTERN, DEFAULT_IMAGES_CLASSPATH_LOCATION);
            registry.addResourceHandler(DEFAULT_IMAGES_URL_PATTERN)
                    .addResourceLocations(DEFAULT_IMAGES_CLASSPATH_LOCATION);
        }

        // --- Handler for Uploaded Images (from Filesystem) ---
        if (!registry.hasMappingForPattern(UPLOADED_IMAGES_URL_PATTERN)) {
            // Resolve and normalize the absolute path on the filesystem
            Path uploadDir = Paths.get(externalImagesPath).toAbsolutePath().normalize();
            // Convert a path to a resource location URI format
            String uploadLocationUri = uploadDir.toUri().toString();

            // Ensure the URI ends with a slash if it's a directory
            if (!uploadLocationUri.endsWith("/")) {
                uploadLocationUri += "/";
            }

            log.info("Configuring resource handler for uploaded images: URL '{}' -> Filesystem '{}'",
                    UPLOADED_IMAGES_URL_PATTERN, uploadLocationUri);
            registry.addResourceHandler(UPLOADED_IMAGES_URL_PATTERN) // Requests starting with /storage/
                    .addResourceLocations(uploadLocationUri);
        }
    }
}
