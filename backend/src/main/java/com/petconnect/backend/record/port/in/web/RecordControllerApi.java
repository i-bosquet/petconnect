package com.petconnect.backend.record.port.in.web;

import com.petconnect.backend.record.application.dto.RecordCreateDto;
import com.petconnect.backend.record.application.dto.RecordViewDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
     * @param sign      Optional query parameter (defaults to false). If true and the user is a Vet, attempts to sign.
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
            @Valid @RequestBody RecordCreateDto createDto,
            @Parameter(description = "Set to true if a Veterinarian wants to sign this record upon creation.")
            @RequestParam(required = false, defaultValue = "false") boolean sign);

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
}
