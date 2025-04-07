package com.petconnect.backend.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;

import org.springframework.context.annotation.Configuration;

/**
 * Configuration for OpenAPI (Swagger) documentation generation.
 * Defines API info, servers, and security schemes.
 *
 * @author ibosquet
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "${application.title}",
                version = "${application.version}",
                description = "${application.description}",
                contact = @Contact(name = "${application.author}", email = "${application.email}")
        ),
        servers = { // Define server URLs (useful if deploying)
                @Server(url = "http://localhost:8080", description = "Local Development Server"),
        }
)
@SecurityScheme(
        name = "bearerAuth", // The name referenced in @SecurityRequirement
        description = "JWT Authentication token",
        scheme = "bearer", // Standard scheme name for Bearer tokens
        type = SecuritySchemeType.HTTP, // Type of security scheme
        bearerFormat = "JWT", // Hint about the format of the token
        in = SecuritySchemeIn.HEADER // Location of the token (Authorization header)
)
public class OpenApiConfig {
}
