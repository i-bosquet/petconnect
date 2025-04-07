package com.petconnect.backend.config;

import io.micrometer.common.lang.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

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
public class MvcConfig implements WebMvcConfigurer {

    /**
     * The base path to the external directory where user-uploaded images are stored.
     * Injected from the 'app.external.images.path' property in application.properties.
     */
    @Value("${app.external.images.path}")
    private String externalImagesPath;

    /**
     * Configures resource handlers for static assets.
     * <p>
     * Maps URL patterns to physical locations:
     * - `/uploaded-images/avatars/**`: Maps to the external filesystem path defined by {@code externalImagesPath} + "/avatars/".
     *   This serves user-uploaded avatars. The "file:" prefix is crucial for accessing the filesystem.
     * - `/images/**`: Maps to the `static/images/` directory within the classpath.
     *   This serves default avatars and potentially other static images packaged with the application.
     * </p>
     *
     * @param registry The registry to add resource handlers to. Must not be {@code null}.
     */
    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {

        // Handler for user-uploaded images from the external directory
        String externalLocation = "file:" + externalImagesPath + "/avatars/";
        registry.addResourceHandler("/uploaded-images/avatars/**")
                .addResourceLocations(externalLocation);

        // Handler for default images packaged within the application (classpath)
        // Note: Spring Boot often configures serving from /static/** automatically,
        // but explicitly defining it ensures clarity and overrides potential conflicts.
        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images/");
    }
}
