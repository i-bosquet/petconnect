package com.petconnect.backend.user.port.in.web;

import com.petconnect.backend.user.application.dto.ClinicStaffProfileDto;
import com.petconnect.backend.user.application.dto.OwnerProfileDto;
import com.petconnect.backend.user.application.dto.OwnerProfileUpdateDto;
import com.petconnect.backend.user.application.dto.UserProfileDto;
import com.petconnect.backend.user.application.dto.UserProfileUpdateDto;
import io.micrometer.common.lang.Nullable;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

/**
 * API interface defining endpoints for general user operations,
 * including retrieving profile information and self-updating profiles.
 * Authorization rules are applied at the service or configuration level.
 * Documented using OpenAPI 3 annotations.
 *
 * @author ibosquet
 */
@Tag(name = "User Management \uD83D\uDC64", description = "Endpoints for retrieving user information and managing user profiles.")
@SecurityRequirement(name = "bearerAuth")
public interface UserControllerApi {

    /**
     * Retrieves the profile of the currently authenticated user.
     * Returns specific profile details ({@link OwnerProfileDto} or {@link ClinicStaffProfileDto})
     * based on the authenticated user's role. Requires a valid JWT token.
     *
     * @return A {@link ResponseEntity} containing the user's specific profile DTO (as Object)
     *         and HTTP status 200 (OK).
     *         Returns 401 if not authenticated or token is invalid.
     *         Returns 404 if the authenticated user is somehow not found in the database.
     */
    @Operation(summary = "Get Current User Profile",
            description = "Retrieves the complete profile details for the currently authenticated user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(description = "OwnerProfileDto or ClinicStaffProfileDto depending on user role",
                                    oneOf = {OwnerProfileDto.class, ClinicStaffProfileDto.class}))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "404", description = "Authenticated User Not Found in DB",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = Map.class)))
    })
    @GetMapping("/me")
    ResponseEntity<Object> getCurrentUserProfile();


    /**
     * Retrieves a generic user profile by their unique ID.
     * Requires an ADMIN role or the user requesting their own ID. Returns a basic {@link UserProfileDto} containing common, non-sensitive information.
     *
     * @param id The unique ID of the user to retrieve.
     * @return A {@link ResponseEntity} containing the {@link UserProfileDto} and HTTP status 200 (OK) if found and authorized.
     *         Returns 401 if not authenticated.
     *         Returns 403 if the authenticated user is not an Admin and not requesting their own ID.
     *         Returns 404 if the user ID does not exist.
     */
    @Operation(summary = "Get User Profile by ID",
            description = "Retrieves basic profile information for a user by ID. Requires ADMIN role or requesting own profile.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User profile retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UserProfileDto.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "404", description = "User Not Found", content = @Content(schema = @Schema(implementation = Map.class)))
    })
    @GetMapping("/{id}")
    ResponseEntity<UserProfileDto> getUserById(
            @Parameter(description = "ID of the user to retrieve", required = true)
            @PathVariable Long id);


    /**
     * Retrieves a generic user profile by their unique email address.
     * Requires an ADMIN role.
     * Returns a basic {@link UserProfileDto}.
     *
     * @param email The email address of the user to retrieve.
     * @return A {@link ResponseEntity} containing the {@link UserProfileDto} and HTTP status 200 (OK) if found and authorized.
     *         Returns 401 if not authenticated.
     *         Returns 403 if the authenticated user is not an Admin.
     *         Returns 404 if no user exists with the given email.
     */
    @Operation(summary = "Get User Profile by Email",
            description = "Retrieves basic profile information for a user by email. Requires ADMIN role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User profile retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UserProfileDto.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "404", description = "User Not Found", content = @Content(schema = @Schema(implementation = Map.class)))
    })
    @GetMapping("/by-email")
    ResponseEntity<UserProfileDto> getUserByEmail(
            @Parameter(description = "Email address of the user to search for", required = true)
            @RequestParam String email);

    /**
     * Updates the profile information for the currently authenticated Pet Owner.
     * Allows an Owner to update their username, avatar, and phone number.
     * Requires an OWNER role. The service validates username uniqueness.
     *
     * @param updateDTO An {@link OwnerProfileUpdateDto} containing the fields to update. Validation rules are applied.
     * @return A {@link ResponseEntity} containing the updated {@link OwnerProfileDto} and HTTP status 200 (OK).
     *         Returns 400 for invalid input data.
     *         Returns 401 if not authenticated.
     *         Returns 403 if the authenticated user is not an Owner.
     *         Returns 409 if the new username conflicts with an existing user.
     */
    @Operation(summary = "Update Current Owner Profile",
            description = "Allows the authenticated Owner to update their own profile details (username, avatar, phone).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Owner profile updated successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = OwnerProfileDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid Update Data", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden (User is not an Owner)", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "409", description = "Conflict (New username already exists)", content = @Content(schema = @Schema(implementation = Map.class)))
    })
    @PutMapping(value = "/me", consumes = MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<OwnerProfileDto> updateCurrentOwnerProfile(
            @Parameter(description = "Owner profile data (JSON part)", schema = @Schema(implementation = OwnerProfileUpdateDto.class))
            @RequestPart("dto") @Valid OwnerProfileUpdateDto updateDTO,
            @Parameter(description = "Optional new avatar image file")
            @RequestPart(value = "imageFile", required = false) @Nullable MultipartFile imageFile
    ) throws IOException;


    /**
     * Updates common profile information (username, avatar) for the currently authenticated Clinic Staff member (Vet or Admin).
     * Requires a VET or ADMIN role.
     *
     * @param updateDTO A {@link UserProfileUpdateDto} containing the common fields to update. Validation rules are applied.
     * @return A {@link ResponseEntity} containing the updated {@link ClinicStaffProfileDto} and HTTP status 200 (OK).
     *         Returns 400 for invalid input data.
     *         Returns 401 if not authenticated.
     *         Returns 403 if the authenticated user is not Clinic Staff (Vet/Admin).
     *         Returns 409 if the new username conflicts with an existing user.
     */
    @Operation(summary = "Update Current Staff Profile (Common Fields)",
            description = "Allows authenticated Clinic Staff (Vet/Admin) to update their own common profile details (username, avatar).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Staff profile updated successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ClinicStaffProfileDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid Update Data", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden (User is not Clinic Staff)", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "409", description = "Conflict (New username already exists)", content = @Content(schema = @Schema(implementation = Map.class)))
    })
    @PutMapping(value = "/me/staff", consumes = MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<ClinicStaffProfileDto> updateCurrentClinicStaffProfile(
            @Parameter(description = "Staff common profile data (JSON part)", schema = @Schema(implementation = UserProfileUpdateDto.class))
            @RequestPart("dto") @Valid UserProfileUpdateDto updateDTO,
            @Parameter(description = "Optional new avatar image file")
            @RequestPart(value = "imageFile", required = false) @Nullable MultipartFile imageFile
    ) throws IOException;
}