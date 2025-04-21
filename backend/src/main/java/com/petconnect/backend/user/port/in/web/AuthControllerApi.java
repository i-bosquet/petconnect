package com.petconnect.backend.user.port.in.web;

import com.petconnect.backend.user.application.dto.AuthLoginRequestDto;
import com.petconnect.backend.user.application.dto.AuthResponseDto;
import com.petconnect.backend.user.application.dto.OwnerProfileDto;
import com.petconnect.backend.user.application.dto.OwnerRegistrationDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * API interface defining authentication-related endpoints for PetConnect.
 * Implementations of this interface handle the actual logic via {@link RestController}.
 * This interface uses OpenAPI 3 annotations to document the API for tools like Swagger UI.
 *
 * @author ibosquet
 */
@Tag(name = "Authentication \uD83D\uDD11", description = "Endpoints for user registration (Owners) and login for all user types.")
public interface AuthControllerApi {

    /**
     * Registers a new Pet Owner user.
     * <p>
     * This endpoint allows prospective users to create an account with the OWNER role.
     * It validates the provided username and email for uniqueness and checks password complexity
     * (as defined by validation constraints). On successful registration, the user's profile
     * information (excluding sensitive data) is returned.
     * </p>
     *
     * @param registrationDTO A {@link OwnerRegistrationDto} containing the required details: unique username,
     *                        unique email, password (meeting complexity requirements), and phone number.
     *                        Validation rules are applied.
     * @return A {@link ResponseEntity} containing the {@link OwnerProfileDto} of the newly created user
     *         with HTTP status 201 (Created).
     * @see OwnerRegistrationDto
     * @see OwnerProfileDto
     */
    @Operation(summary = "Register New Pet Owner",
            description = "Creates a new user account with the OWNER role. Requires a unique username and email.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Owner registered successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = OwnerProfileDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid Registration Data Provided",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(description = "Map of validation errors", example = "{\"username\": \"Username cannot be blank\", \"password\": \"Password must be at least 8 characters long\"}", implementation = Map.class))),
            @ApiResponse(responseCode = "409", description = "Conflict - Username or Email Already Exists",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(description = "Error message indicating conflict", example = "{\"error\":\"Data Conflict\", \"message\":\"Username already taken: existing user\"}", implementation = Map.class)))
    })
    @PostMapping("/register")
    ResponseEntity<OwnerProfileDto> registerOwner(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Owner registration details: ", required = true,
                    content = @Content(schema = @Schema(implementation = OwnerRegistrationDto.class)))
            @Valid @org.springframework.web.bind.annotation.RequestBody OwnerRegistrationDto registrationDTO);

    /**
     * Authenticates a user and returns a JWT token.
     * <p>
     * Users provide their username and password. If the credentials are valid,
     * the service generates a JSON Web Token (JWT) containing user identification
     * and authorities (roles and permissions). This token must be included in the
     * 'Authorization: Bearer <token>' header for later requests to protected endpoints.
     * </p>
     *
     * @param userRequest An {@link AuthLoginRequestDto} containing the username and password. Validation rules are applied.
     * @return A {@link ResponseEntity} containing the {@link AuthResponseDto} (which includes the JWT)
     *         with HTTP status 200 (OK) on success.
     * @see AuthLoginRequestDto
     * @see AuthResponseDto
     */
    @Operation(summary = "Authenticate User (Login)",
            description = "Authenticates a user with username and password, returning a JWT token upon success.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Authentication successful, JWT token returned",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AuthResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid Login Data Provided (e.g., blank fields)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(description = "Map of validation errors", example = "{\"username\": \"must not be blank\"}", implementation = Map.class))),
            @ApiResponse(responseCode = "401", description = "Authentication Failed (Invalid Credentials or User Not Found)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(description = "Standardized error response", example = "{\"timestamp\":1678886400000,\"status\":401,\"error\":\"Authentication Failed\",\"message\":\"Invalid username or password provided.\"}", implementation = Map.class)))
    })
    @PostMapping("/login")
    ResponseEntity<AuthResponseDto> login(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "User login : ", required = true,
                    content = @Content(schema = @Schema(implementation = AuthLoginRequestDto.class)))
            @Valid @org.springframework.web.bind.annotation.RequestBody AuthLoginRequestDto userRequest);
}