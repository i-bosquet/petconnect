package com.petconnect.backend.record.port.in.web;

import com.petconnect.backend.record.application.dto.RecordCreateDto;
import com.petconnect.backend.record.application.dto.RecordUpdateDto;
import com.petconnect.backend.record.application.dto.RecordViewDto;
import com.petconnect.backend.record.application.dto.TemporaryAccessRequestDto;
import com.petconnect.backend.record.application.dto.TemporaryAccessTokenDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * API interface defining endpoints for managing medical Records for Pets.
 * Documented using OpenAPI 3 annotations.
 * Requires authentication for all operations.
 * All paths are relative to /api/pets/{petId}/records.
 *
 * @author ibosquet
 */
@Tag(name = "⚕️ Medical Record Management", description = "Endpoints for creating, retrieving, and deleting pet medical records.")
@RequestMapping("/api/records")
@SecurityRequirement(name = "bearerAuth")
public interface RecordControllerApi {
    /**
     * Creates a new medical record. The pet ID must be included in the request body.
     * Accessible by the pet owner or authorized clinic staff associated with the specified pet.
     * If the type is VACCINE, vaccine details must be provided. Vets can optionally sign.
     *
     * @param createDto The DTO containing record details, including the mandatory petId.
     * @return ResponseEntity with the created RecordViewDto and status 201.
     */
    @Operation(summary = "Create Medical Record",
            description = "Adds a new record to a pet's history. Requires petId in the body and Owner/associated Staff role. Vets can optionally sign.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Record created successfully", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = RecordViewDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data (DTO validation failed, or inconsistent type/vaccine data)", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized (Token missing or invalid)", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden (User not authorized for this pet, or trying to sign as non-Vet)", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "404", description = "Pet specified by petId in DTO not found", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "500", description = "Internal Server Error (e.g., signing failed unexpectedly)", content = @Content(schema = @Schema(implementation = Map.class)))
    })
    @PostMapping("")
    ResponseEntity<RecordViewDto> createRecord(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Details of the record to create. Include 'vaccine' object if type is VACCINE.", required = true, content = @Content(schema = @Schema(implementation = RecordCreateDto.class)))
            @Valid @RequestBody RecordCreateDto createDto);

    /**
     * Retrieves a paginated list of medical records for the specified pet.
     * Accessible by the pet's owner or authorized clinic staff. Ordered by creation date descending.
     *
     * @param petId   Optional ID of the pet whose records are requested. Other filters might be added later.
     * @param pageable  Pagination and sorting parameters.
     * @return ResponseEntity with a Page of RecordViewDto and status 200 (OK).
     */
    @Operation(summary = "List Medical Records for Pet", description = "Retrieves the medical history for a specific pet. Requires Owner or associated Staff role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Records retrieved successfully", content = @Content(schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden (User not authorized for the specified filter, e.g., not owner of petId)", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "404", description = "Resource specified in filter not found", content = @Content(schema = @Schema(implementation = Map.class)))
    })
    @GetMapping("")
    ResponseEntity<Page<RecordViewDto>> findRecords(
            @Parameter(description = "Filter by Pet ID.")
            @RequestParam() Long petId,
            @Parameter(description = "Pagination details")
            @PageableDefault(size = 20, sort = {"createdAt"}, direction = Sort.Direction.DESC)
            Pageable pageable);

    /**
     * Retrieves a specific medical record by its ID.
     * Accessible by the owner of the associated pet or authorized clinic staff.
     *
     * @param recordId The ID of the specific record to retrieve.
     * @return ResponseEntity with the RecordViewDto and status 200.
     */
    @Operation(summary = "Get Specific Medical Record by ID", description = "Retrieves details of a single medical record by its ID. Requires Owner or associated Staff role for the associated pet.")
            @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Record retrieved successfully", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = RecordViewDto.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden (User not authorized for the pet associated with this record)", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "404", description = "Record not found", content = @Content(schema = @Schema(implementation = Map.class))) // 404 si el recordId no existe
            })
            @GetMapping("/{recordId}")
    ResponseEntity<RecordViewDto> findRecordById(
            @Parameter(description = "ID of the record", required = true) @PathVariable Long recordId);

    /**
     * Updates an existing unsigned medical record.
     * Allows modification of the type (excluding VACCINE) and/or description.
     * Requires the user to be the creator or an authorized Admin.
     *
     * @param recordId The ID of the unsigned record to update.
     * @param updateDto DTO containing the optional new type and/or description.
     * @return ResponseEntity with the updated RecordViewDto and status 200.
     */
    @Operation(summary = "Update Unsigned Record",
            description = "Updates the type (non-vaccine) and/or description of an unsigned record. Requires creator or authorized Admin role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Record updated successfully", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = RecordViewDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data (e.g., invalid type)", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden (User not authorized to update)", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "404", description = "Record not found", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "409", description = "Conflict (e.g., trying to update a signed record, or change type to/from VACCINE)", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(example = "{\"error\":\"Conflict\", \"message\":\"Cannot update record X because it has been signed...\"}", implementation = Map.class)))
    })
    @PutMapping("/{recordId}")
    ResponseEntity<RecordViewDto> updateUnsignedRecord(
            @Parameter(description = "ID of the record to update", required = true) @PathVariable Long recordId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Optional new type and/or description.", required = true, content = @Content(schema = @Schema(implementation = RecordUpdateDto.class)))
            @Valid @RequestBody RecordUpdateDto updateDto);

    /**
     * Deletes an unsigned medical record.
     * Accessible only by the original creator or an Admin of the creator's clinic (if creator was staff).
     *
     * @param recordId The ID of the unsigned record to delete.
     * @return ResponseEntity with status 204 (No Content) on successful deletion.
     */
    @Operation(summary = "Delete Unsigned Record by ID", description = "Deletes a medical record by its ID ONLY IF it has not been signed. Requires user to be the creator or an authorized Admin.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Record deleted successfully."),
            @ApiResponse(responseCode = "400", description = "Bad Request (e.g., trying to delete a signed record)", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden (User not authorized to delete this record)", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "404", description = "Record not found (or Pet ID mismatch)", content = @Content(schema = @Schema(implementation = Map.class)))
    })
    @DeleteMapping("/{recordId}")
    ResponseEntity<Void> deleteUnsignedRecord(
            @Parameter(description = "ID of the record to delete", required = true) @PathVariable Long recordId);

    /**
     * Generates a short-lived JWT token that can grant temporary read-only access
     * to a specific pet's signed medical records.
     * Requires the requester to be the owner of the pet.
     *
     * @param petId     The ID of the pet for which access is requested.
     * @param requestDto DTO containing the requested duration string (e.g., "PT1H", "P1D").
     * @return ResponseEntity with a TemporaryAccessTokenDto containing the token and status 200 (OK).
     */
    @Operation(summary = "Generate Temporary Access Token (Owner)",
            description = "Allows the Pet Owner to generate a short-lived token for sharing read-only access to signed records.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Temporary access token generated successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = TemporaryAccessTokenDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid duration format provided", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden (User is not the owner of the pet)", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "404", description = "Pet not found", content = @Content(schema = @Schema(implementation = Map.class)))
    })
    @PostMapping("/{petId}/temporary-access")
    ResponseEntity<TemporaryAccessTokenDto> generateTemporaryAccessToken(
            @Parameter(description = "ID of the pet", required = true) @PathVariable Long petId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Requested duration for the token.", required = true, content = @Content(schema = @Schema(implementation = TemporaryAccessRequestDto.class)))
            @Valid @RequestBody TemporaryAccessRequestDto requestDto);

    /**
     * Verifies a temporary access token and retrieves the signed medical records for the associated pet.
     * This endpoint is publicly accessible and uses the token for authorization.
     *
     * @param token The temporary access token.
     * @return ResponseEntity with a List of RecordViewDto (only signed records) and status 200 (OK).
     *         Returns 400 if the token is invalid or expired.
     *         Returns 404 if the pet associated with the token is not found.
     */
    @Operation(summary = "Verify Temporary Access Token & Get Signed Records",
            description = "Validates a temporary access token and returns signed medical records for the pet. Publicly accessible via token.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Signed records retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = RecordViewDto.class)))),
            @ApiResponse(responseCode = "400", description = "Invalid, expired, or malformed token",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "404", description = "Pet not found for the token provided",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = Map.class)))
    })
    @GetMapping("/verify-temporary-access")
    ResponseEntity<List<RecordViewDto>> getRecordsByTemporaryToken(
            @Parameter(description = "The temporary access token provided in the shared link/QR", required = true)
            @RequestParam String token
    );

    /**
     * Retrieves a paginated list of all medical records created by staff members
     * associated with the specified clinic. The user requesting this information
     * must have proper authorization for the clinic.
     *
     * @param clinicId The unique identifier of the clinic whose records are to be retrieved.
     * @param pageable Pagination and sorting information for the request.
     * @return A ResponseEntity containing a Page of RecordViewDto objects, which represent
     *         the details of the records created by the specified clinic's staff.
     */
    @Operation(summary = "Get records created by a specific clinic",
            description = "Retrieves a paginated list of all medical records created by staff within the specified clinic. Requires clinic staff authorization for that clinic.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Records retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User not authorized for this clinic"),
            @ApiResponse(responseCode = "404", description = "Clinic not found")
    })
    @GetMapping("/clinic/{clinicId}/created-by")
    @SecurityRequirement(name = "bearerAuth")
    ResponseEntity<Page<RecordViewDto>> getRecordsCreatedByClinic(
            @Parameter(description = "ID of the clinic") @PathVariable Long clinicId,
            @Parameter(hidden = true) Pageable pageable
    );
}
