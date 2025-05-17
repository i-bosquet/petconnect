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
     * Creates a new clinic staff member (Vet or Admin) within the admin's clinic.
     *
     * This method allows an admin to onboard new staff member details and upload optional public/private key files
     * for users with the Vet role. The creation process validates the provided data and responds with the created
     * staff member's profile or an appropriate error message in case of failure.
     *
     * @param creationDTO The data transfer object containing the clinic staff member's details.
     * @param publicKeyFile The public key file (e.g., .pem or .crt) for the Vet. Required if the role is VET.
     * @param privateKeyFile The private key file corresponding to the Vet's public key. Optional for VET role.
     * @return A ResponseEntity containing the profile of the newly created clinic staff member or an appropriate
     * error response in case of a failure during processing.
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
            @RequestPart(value = "publicKeyFile", required = false) @Nullable MultipartFile publicKeyFile,
            @RequestPart(value = "privateKeyFile", required = false) @Nullable MultipartFile privateKeyFile);

    /**
     * Updates the details of a clinic staff member for an admin user. This operation
     * allows the admin to modify their clinic's staff member details, such as personal information,
     * public key, and private key files, if applicable.
     *
     * @param staffId the unique identifier of the staff member to update. This value is mandatory.
     * @param updateDTO the ClinicStaffUpdateDto object containing updated details for the staff member.
     *                  This includes fields such as name, specialty, and other relevant information.
     * @param publicKeyFile an optional MultipartFile containing the new public key for the staff member.
     *                      This file should be in PEM or CRT format.
     * @param privateKeyFile an optional MultipartFile containing the new private key for the staff member.
     *                       This file should be in PEM or CRT format.
     * @return a ResponseEntity containing the updated ClinicStaffProfileDto object if the update is successful.
     *         On failure, appropriate error messages are returned based on the error condition.
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
            @RequestPart(value = "publicKeyFile", required = false) @Nullable MultipartFile publicKeyFile,
            @RequestPart(value = "privateKeyFile", required = false) @Nullable MultipartFile privateKeyFile);


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