package com.petconnect.backend.user.port.in.web;

import com.petconnect.backend.user.application.dto.ClinicStaffCreationDto;
import com.petconnect.backend.user.application.dto.ClinicStaffProfileDto;
import com.petconnect.backend.user.application.dto.ClinicStaffUpdateDto;
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
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * API interface defining endpoints for managing Clinic Staff (Vets and Admins).
 * These operations typically require ADMIN privileges for the associated clinic.
 *
 * @author ibosquet
 */
@Tag(name = "Clinic Staff Management \uD83D\uDC65", description = "Endpoints for Admins to create, update, activate, and deactivate clinic staff members.")
@SecurityRequirement(name = "bearerAuth")
public interface ClinicStaffControllerApi {

    /**
     * Creates a new clinic staff member (Veterinarian or Administrator).
     * This endpoint allows an authenticated Admin user to create a new user account
     * (Vet or Admin role) and associate it with their own clinic. The service layer handles
     * password hashing, role assignment, and validation (e.g., uniqueness of username, email, license number).
     *
     * @param creationDTO A {@link ClinicStaffCreationDto} containing the details for the new staff member.
     *                    Requires a role (VET or ADMIN), unique username/email, password, name, surname.
     *                    If a role is VET, licenseNumber and vetPublicKey are also required and validated for uniqueness.
     * @return A {@link ResponseEntity} containing the {@link ClinicStaffProfileDto} of the newly created staff member
     *         with HTTP status 201 (Created).
     *         See possible error responses for details on failures.
     */
    @Operation(summary = "Create New Clinic Staff",
            description = "Allows an Admin to create a new Vet or Admin user within their own clinic.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Staff member created successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ClinicStaffProfileDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid Input Data (Validation failed or missing required fields for role)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized (JWT token missing or invalid)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden (User does not have ADMIN role)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "409", description = "Conflict (Username, Email, License Number, or Public Key already exists)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = Map.class)))
    })
    @PostMapping(value = "", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<ClinicStaffProfileDto> createClinicStaff(
            @Parameter(description = "Details for the new clinic staff member.", required = true, schema = @Schema(implementation = ClinicStaffCreationDto.class))
            @RequestPart("dto") @Valid ClinicStaffCreationDto creationDTO,
            @Parameter(description = "Vet's Public Key file (.pem/.crt) - Required if role is VET")
            @RequestPart(value = "publicKeyFile", required = false) @Nullable MultipartFile publicKeyFile);

    /**
     * Updates the details of a clinic staff member.
     * This method is intended to be used by an Admin to modify the information
     * of a staff member within the same clinic to which the Admin belongs.
     *
     * @param staffId the unique identifier of the staff member to update
     * @param updateDTO the details of the staff member to update;
     *                  must be provided in a JSON format corresponding to ClinicStaffUpdateDto
     * @param publicKeyFile an optional file containing the new public key for the staff member
     *                      (must be in .pem or .crt format)
     * @return ResponseEntity containing the updated staff profile as ClinicStaffProfileDto
     */
    @Operation(summary = "Update Clinic Staff Details",
            description = "Allows an Admin to update details of a staff member in their own clinic.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Staff member updated successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ClinicStaffProfileDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid Update Data Provided (Validation failed)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden (User is not Admin or attempting to update staff in another clinic)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "404", description = "Staff Member Not Found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "409", description = "Conflict (Updated License Number or Public Key already exists)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = Map.class)))
    })
    @PutMapping(value = "/{staffId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<ClinicStaffProfileDto> updateClinicStaff(
            @Parameter(description = "ID of the staff member to update", required = true)
            @PathVariable Long staffId,
            @Parameter(description = "Staff details to update (JSON part)", required = true, schema = @Schema(implementation = ClinicStaffUpdateDto.class))
            @RequestPart("dto") @Valid ClinicStaffUpdateDto updateDTO,
            @Parameter(description = "Optional new Vet's Public Key file (.pem/.crt)")
            @RequestPart(value = "publicKeyFile", required = false) @Nullable MultipartFile publicKeyFile);


    /**
     * Activates a previously deactivated clinic staff member's account.
     * Allows an authenticated Admin to re-enable a staff account within their own clinic.
     *
     * @param staffId The unique ID of the staff member to activate.
     * @return A {@link ResponseEntity} containing the {@link ClinicStaffProfileDto} of the activated staff member
     *         with HTTP status 200 (OK).
     *         See possible error responses for details on failures.
     */
    @Operation(summary = "Activate Clinic Staff Account",
            description = "Allows an Admin to activate an inactive staff member account in their own clinic.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Staff member activated successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ClinicStaffProfileDto.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request (e.g., account is already active)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden (User is not Admin or attempting action in another clinic)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "404", description = "Staff Member Not Found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = Map.class)))
    })
    @PutMapping("/{staffId}/activate")
    ResponseEntity<ClinicStaffProfileDto> activateStaff(
            @Parameter(description = "ID of the staff member to activate", required = true)
            @PathVariable Long staffId);

    /**
     * Deactivates an active clinic staff member's account.
     * Allows an authenticated Admin to disable a staff account within their own clinic.
     * The Admin cannot deactivate their own account via this endpoint.
     *
     * @param staffId The unique ID of the staff member to deactivate.
     * @return A {@link ResponseEntity} containing the {@link ClinicStaffProfileDto} of the deactivated staff member
     *         with HTTP status 200 (OK).
     *         See possible error responses for details on failures.
     */
    @Operation(summary = "Deactivate Clinic Staff Account",
            description = "Allows an Admin to deactivate an active staff member account in their own clinic (cannot deactivate self).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Staff member deactivated successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ClinicStaffProfileDto.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request (e.g., account is already inactive, attempting self-deactivation)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden (User is not Admin or attempting action in another clinic)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "404", description = "Staff Member Not Found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = Map.class)))
    })
    @PutMapping("/{staffId}/deactivate")
    ResponseEntity<ClinicStaffProfileDto> deactivateStaff(
            @Parameter(description = "ID of the staff member to deactivate", required = true)
            @PathVariable Long staffId);
}