package com.petconnect.backend.user.port.in.web;

import com.petconnect.backend.user.application.dto.*;
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
import org.springframework.web.bind.annotation.RequestBody;
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

    /**
     * Initiates the password reset process by sending a reset link to the user's email.
     * This endpoint is publicly accessible. It accepts the user's email address.
     * For security reasons, it always returns a 200-OK response, regardless of
     * whether the email exists in the system, to prevent email enumeration attacks.
     * The actual email sending happens asynchronously if the user is found.
     *
     * @param requestDto DTO containing the email address.
     * @return A {@link ResponseEntity} with HTTP status 200 (OK) and a generic success message.
     */
    @Operation(summary = "Request Password Reset Link",
            description = "Sends a password reset link to the provided email address if it's associated with an account.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password reset request processed. If the email is registered, a reset link will be sent.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Map.class, example = "{\"message\": \"Password reset instructions sent if email is registered.\"}"))),
            @ApiResponse(responseCode = "400", description = "Invalid Email Format",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Map.class)))
    })
    @PostMapping("/forgot-password")
    ResponseEntity<Map<String, String>> requestPasswordReset(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "User's email address.", required = true,
                    content = @Content(schema = @Schema(implementation = PasswordResetRequestDto.class)))
            @Valid @RequestBody PasswordResetRequestDto requestDto);


    /**
     * Resets the user's password using a valid token received via email.
     * This endpoint is publicly accessible but requires a valid, non-expired token.
     * It validates the token and the new password confirmation.
     *
     * @param resetDto DTO containing the reset token, new password, and confirmation.
     * @return A {@link ResponseEntity} with HTTP status 200 (OK) and a success message upon successful password reset.
     */
    @Operation(summary = "Reset Password with Token",
            description = "Sets a new password for the user associated with the provided valid reset token.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password reset successfully.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Map.class, example = "{\"message\": \"Password has been reset successfully.\"}"))),
            @ApiResponse(responseCode = "400", description = "Invalid or Expired Token, Passwords Don't Match, or Invalid Input",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Map.class)))
    })
    @PostMapping("/reset-password")
    ResponseEntity<Map<String, String>> resetPassword(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Password reset token and new password details.", required = true,
                    content = @Content(schema = @Schema(implementation = PasswordResetDto.class)))
            @Valid @RequestBody PasswordResetDto resetDto);
}