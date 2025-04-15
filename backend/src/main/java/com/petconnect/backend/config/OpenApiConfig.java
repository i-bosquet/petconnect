package com.petconnect.backend.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for OpenAPI (Swagger) documentation generation.
 * Defines API info, servers, and security schemes.
 * Grouping of endpoints by module is handled in OpenApiGroupConfig.
 *
 * @author ibosquet
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "\uD83D\uDC3E PetConnect API ",
                version = "${application.version}",
                description = "${application.description}",
                contact = @Contact(
                        name = "${application.author}",
                        email = "${application.email}")
        ),
        servers = {
                @Server(
                        url = "http://localhost:8080",
                        description = "LOCAL SERVER"
                ),
        }
)
@SecurityScheme(
        name = "bearerAuth",
        description = "JWT Authentication token",
        scheme = "bearer",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER
)
public class OpenApiConfig {
        private static final String USER_PACKAGE = "com.petconnect.backend.user.port.in.web";
        private static final String PET_PACKAGE = "com.petconnect.backend.pet.port.in.web";
        private static final String RECORD_PACKAGE = "com.petconnect.backend.record.port.in.web";
        private static final String CERTIFICATE_PACKAGE = "com.petconnect.backend.certificate.port.in.web";

        /**
         * Defines the OpenAPI group for the User module, scanning only user-related controllers.
         * @return GroupedOpenApi bean for the User module.
         */
        @Bean
        public GroupedOpenApi userApi() {
                return GroupedOpenApi.builder()
                        .group("user")
                        .displayName("1.- 👤 User Module")
                        .packagesToScan(USER_PACKAGE)
                        .build();
        }

        /**
         * Defines the OpenAPI group for the Pet module.
         * @return GroupedOpenApi bean for the Pet module.
         */
        @Bean
        public GroupedOpenApi petApi() {
                return GroupedOpenApi.builder()
                        .group("pet")
                        .displayName("2.- 🐶 Pet Module")
                        .packagesToScan(PET_PACKAGE)
                        .build();
        }

        /**
         * Defines the OpenAPI group for the Medical Record module.
         * @return GroupedOpenApi bean for the Medical Record module.
         */
        @Bean
        public GroupedOpenApi medicalRecordApi() {
                return GroupedOpenApi.builder()
                        .group("record")
                        .displayName("3.- ⚕️ Medical Record Module")
                        .packagesToScan(RECORD_PACKAGE)
                        .build();
        }

        /**
         * Defines the OpenAPI group for the Certificate module.
         * @return GroupedOpenApi bean for the Certificate module.
         */
        @Bean
        public GroupedOpenApi certificateApi() {
                return GroupedOpenApi.builder()
                        .group("certificate")
                        .displayName("4.- 📜 Certificate Module")
                        .packagesToScan(CERTIFICATE_PACKAGE)
                        .build();
        }
}
