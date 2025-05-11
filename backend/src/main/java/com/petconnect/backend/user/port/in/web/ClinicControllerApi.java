package com.petconnect.backend.user.port.in.web;

import com.petconnect.backend.user.application.dto.ClinicDto;
import com.petconnect.backend.user.application.dto.ClinicStaffProfileDto;
import com.petconnect.backend.user.application.dto.ClinicUpdateDto;
import com.petconnect.backend.user.application.dto.VetSummaryDto;
import com.petconnect.backend.user.domain.model.Country;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.*;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * API interface defining endpoints related to veterinary clinics.
 * Covers public search/retrieval and authorized operations like updates
 * and staff listing (for authorized personnel).
 * Documented using OpenAPI 3 annotations.
 *
 * @author ibosquet
 */
@Tag(name = "Clinics \uD83C\uDFE5", description = "Endpoints for managing and retrieving clinic information.")
public interface ClinicControllerApi {

    /**
     * Retrieves a paginated list of clinics, optionally filtered by name, city, or country.
     * This endpoint is publicly accessible and supports pagination and sorting via standard
     * Spring Data Pageable parameters
     *
     * @param name Optional filter for clinics whose name contains the provided string (case-insensitive).
     * @param city Optional filter for clinics located in the specified city (case-insensitive).
     * @param country Optional filter for clinics located in the specified country (case-insensitive).
     * @param pageable Pagination and sorting information. Defaults to page 0, size 10, sorted by name.
     * @return A {@link ResponseEntity} containing a {@link Page} of {@link ClinicDto} objects matching the criteria,
     *         with HTTP status 200 (OK).
     */
    @Operation(summary = "Search/List Clinics",
            description = "Retrieves a paginated list of clinics. Publicly accessible. Supports filtering by name, city, and country.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Clinics retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Page.class)))
    })
    @GetMapping
    ResponseEntity<Page<ClinicDto>> getClinics(
            @Parameter(description = "Filter by clinic name (partial match, case-insensitive)", in = ParameterIn.QUERY, name = "name")
            @RequestParam(required = false) String name,
            @Parameter(description = "Filter by city (partial match, case-insensitive)", in = ParameterIn.QUERY, name = "city")
            @RequestParam(required = false) String city,
            @Parameter(description = "Filter by country (partial match, case-insensitive)", in = ParameterIn.QUERY, name = "country")
            @RequestParam(required = false) String country,
            @Parameter(description = "Pagination and sorting parameters (e.g., page=0&size=10&sort=name,asc)")
            @PageableDefault(sort = "name") Pageable pageable);

    /**
     * Retrieves the details of a specific clinic by its unique ID.
     * This endpoint is publicly accessible.
     *
     * @param id The unique ID of the clinic to retrieve.
     * @return A {@link ResponseEntity} containing the {@link ClinicDto} and HTTP status 200 (OK) if found.
     *         Returns HTTP status 404 (Not Found) if no clinic exists with the provided ID.
     */
    @Operation(summary = "Get Clinic by ID", description = "Retrieves details for a specific clinic. Publicly accessible.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Clinic details retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ClinicDto.class))),
            @ApiResponse(responseCode = "404", description = "Clinic Not Found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Map.class)))
    })
    @GetMapping("/{id}")
    ResponseEntity<ClinicDto> getClinicById(
            @Parameter(description = "ID of the clinic to retrieve", required = true)
            @PathVariable Long id);

    /**
     * Updates the details of a specific clinic. This method is used to modify editable fields
     * of a clinic and requires the user to have an ADMIN role for that clinic.
     *
     * @param id the unique identifier of the clinic to be updated
     * @param clinicUpdateDTO an object containing the updated clinic details
     * @param publicKeyFile an optional public key file to be associated with the clinic
     * @return a ResponseEntity containing the updated ClinicDto object if the update is successful
     */
    @Operation(summary = "Update Clinic Details",
            description = "Updates editable information for a specific clinic. Requires ADMIN role for that clinic.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Clinic updated successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ClinicDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid Update Data Provided",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have ADMIN role or is not admin of this clinic",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "404", description = "Clinic Not Found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = Map.class)))
    })
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<ClinicDto> updateClinic(
            @PathVariable Long id,
            @RequestPart("dto") @Valid ClinicUpdateDto clinicUpdateDTO,
            @RequestPart(value = "publicKeyFile", required = false) @Nullable MultipartFile publicKeyFile);

    /**
     * Retrieves a list of all staff members (active and inactive) for a specific clinic.
     * Requires the requesting user to be authenticated and have either the ADMIN or VET role
     * for the clinic specified by {@code clinicId}. Authorization is handled by the service layer.
     *
     * @param clinicId The unique ID of the clinic whose staff list is to be retrieved.
     * @return A {@link ResponseEntity} containing a {@link List} of {@link ClinicStaffProfileDto} objects
     *         and HTTP status 200 (OK).
     *         Returns 401 (Unauthorized) if not authenticated.
     *         Returns 403 (Forbidden) if the user is not an authorized Admin/Vet for this clinic.
     *         Returns 404 (Not Found) if the clinic ID does not exist.
     */
    @Operation(summary = "Get All Staff for a Clinic",
            description = "Retrieves a list of all staff (active and inactive) for a specific clinic. Requires ADMIN or VET role for that clinic.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Staff list retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = List.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "404", description = "Clinic Not Found", content = @Content(schema = @Schema(implementation = Map.class)))
    })
    @GetMapping("/{clinicId}/staff/all")
    ResponseEntity<List<ClinicStaffProfileDto>> getAllStaffByClinic(
            @Parameter(description = "ID of the clinic", required = true)
            @PathVariable Long clinicId);

    /**
     * Retrieves a list of only the active staff members for a specific clinic.
     * Requires the requesting user to be authenticated and have either the ADMIN or VET role
     * for the clinic specified by {@code clinicId}. Authorization is handled by the service layer.
     *
     * @param clinicId The unique ID of the clinic whose active staff list is to be retrieved.
     * @return A {@link ResponseEntity} containing a {@link List} of {@link ClinicStaffProfileDto} objects
     *         for active staff and HTTP status 200 (OK).
     *         See {@link #getAllStaffByClinic(Long)} for possible error responses (401, 403, 404).
     */
    @Operation(summary = "Get Active Staff for a Clinic",
            description = "Retrieves a list of active staff for a specific clinic. Requires ADMIN or VET role for that clinic.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Active staff list retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = List.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "404", description = "Clinic Not Found", content = @Content(schema = @Schema(implementation = Map.class)))
    })
    @GetMapping("/{clinicId}/staff/active")
    ResponseEntity<List<ClinicStaffProfileDto>> getActiveStaffByClinic(
            @Parameter(description = "ID of the clinic", required = true)
            @PathVariable Long clinicId);

    /**
     * Retrieves a distinct list of countries where clinics are registered.
     * Useful for populating filter dropdowns. Publicly accessible.
     *
     * @return ResponseEntity with a List of Country enum string values and status 200.
     */
    @Operation(summary = "Get Distinct Clinic Countries",
            description = "Retrieves a list of unique countries that have registered clinics.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of countries retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(type = "string"))))
    })
    @GetMapping("/countries")
    ResponseEntity<List<Country>> getDistinctCountries();

    /**
     * Downloads the public key file (.pem) associated with the specified clinic.
     * Requires the requester to be authenticated staff (Vet or Admin) of that clinic.
     *
     * @param clinicId The ID of the clinic whose public key is requested.
     * @return A ResponseEntity containing the key file as a Resource for download.
     *         Returns 404 if the clinic or its key file is not found.
     *         Returns 403 if the user is not authorized.
     */
    @Operation(summary = "Download Clinic Public Key",
            description = "Downloads the .pem file containing the clinic's public key. Requires Admin or Vet role for the clinic.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Public key file ready for download.",
                    content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE)),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden (User not staff of this clinic)", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "404", description = "Clinic or Public Key File Not Found", content = @Content(schema = @Schema(implementation = Map.class)))
    })
    @GetMapping("/{clinicId}/public-key/download")
    ResponseEntity<Resource> downloadClinicPublicKey(
            @Parameter(description = "ID of the clinic", required = true) @PathVariable Long clinicId);

    /**
     * Retrieves a list of active veterinarians from the specified clinic for owner selection.
     *
     * @param clinicId the unique identifier of the clinic from which active veterinarians need to be retrieved
     * @return a ResponseEntity containing a list of VetSummaryDto objects representing the active veterinarians
     */
    @Operation(summary = "Get active veterinarians for selection from a specific clinic", description = "Retrieves a list of active veterinarians from the specified clinic, suitable for an owner to select for association.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of active veterinarians retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = VetSummaryDto[].class))),
            @ApiResponse(responseCode = "404", description = "Clinic not found", content = @Content)
    })
    ResponseEntity<List<VetSummaryDto>> getActiveVetsForSelectionByClinic(
            @Parameter(description = "ID of the clinic to retrieve veterinarians from") @PathVariable Long clinicId
    );
}