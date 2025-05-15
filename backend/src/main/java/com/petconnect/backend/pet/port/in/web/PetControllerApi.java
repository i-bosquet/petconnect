package com.petconnect.backend.pet.port.in.web;

import com.petconnect.backend.pet.application.dto.*;
import com.petconnect.backend.pet.domain.model.PetStatus;
import com.petconnect.backend.pet.domain.model.Specie;
import io.micrometer.common.lang.Nullable;
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
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * API interface defining endpoints for managing Pets.
 * Includes operations for owners (registration, updates, listing, deactivation, vet association)
 * and clinic staff (activation, clinical updates, listing associated pets).
 * Documented using OpenAPI 3 annotations.
 *
 * @author ibosquet
 */
@Tag(name = "🐶 Pet Management", description = "Endpoints for creating, retrieving, updating, and managing pets.")
@SecurityRequirement(name = "bearerAuth")
public interface PetControllerApi {

    // --- Owner Operations ---

    /**
     * Registers a new pet for the currently authenticated owner.
     * Accepts pet details as JSON part ('dto') and an optional image file part ('imageFile').
     * Requires 'multipart/form-data' content type.
     *
     * @param registrationDTO DTO with initial pet details.
     * @param imageFile Optional uploaded image file.
     * @return ResponseEntity with the created PetProfileDto and status 201.
     */
    @Operation(summary = "Register New Pet (multipart)",
            description = "Allows Owner to register a pet. Send DTO as 'dto' part (Content-Type: application/json) and image as 'imageFile' part (optional).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Pet registered successfully", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = PetProfileDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data or image processing failed", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden (User is not an Owner)", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "404", description = "Breed specified in DTO not found", content = @Content(schema = @Schema(implementation = Map.class)))
    })
    @PostMapping(value = "", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<PetProfileDto> registerPet(
            @Parameter(description = "Pet details (JSON part)", schema = @Schema(implementation = PetRegistrationDto.class))
            @RequestPart("dto") @Valid PetRegistrationDto registrationDTO,
            @Parameter(description = "Optional image file")
            @RequestPart(value = "imageFile", required = false) @Nullable MultipartFile imageFile
    ) throws IOException;

    /**
     * Retrieves a paginated list of pets that belong to a specific owner.
     * This method requires either an authenticated owner or an ADMIN role
     * for access. If the user is not authorized or the owner does not exist,
     * appropriate error responses are returned.
     *
     * @param statuses an optional list of pet statuses to filter the pets (e.g., ACTIVE, INACTIVE)
     * @param pageable a Pageable object specifying pagination and sorting preferences
     * @return a ResponseEntity containing a Page of PetProfileDto objects with the pet details
     */
    @Operation(summary = "List Pets by Owner", description = "Retrieves pets for a specific owner. Requires owner authentication or ADMIN role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pets retrieved successfully", content = @Content(schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden (User is not the specified owner or an Admin)", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "404", description = "Owner not found", content = @Content(schema = @Schema(implementation = Map.class)))
    })
    @GetMapping("")
    ResponseEntity<Page<PetProfileDto>> findMyPets(
            @Parameter(description = "Pagination details")
            @RequestParam(required = false) List<PetStatus> statuses,
            @PageableDefault(sort = "name") Pageable pageable);

    /**
     * Updates information for a pet owned by the authenticated user.
     * Accepts pet details update DTO as JSON part ('dto') and an optional new image file part ('imageFile').
     * Requires 'multipart/form-data' content type.
     *
     * @param petId ID of the pet to update.
     * @param updateDto DTO containing fields to update.
     * @param imageFile Optional new uploaded image file.
     * @return ResponseEntity with the updated PetProfileDto and status 200.
     */
    @Operation(summary = "Update Pet (Owner, multipart)",
            description = "Allows Owner to update pet details and optionally upload new avatar. Send DTO as 'dto' part and image as 'imageFile' part.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pet updated successfully", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = PetProfileDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data or image processing failed", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden (User is not the owner)", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "404", description = "Pet or Breed not found", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "409", description = "Conflict (e.g., Microchip already exists)", content = @Content(schema = @Schema(implementation = Map.class)))
    })
    @PutMapping(value = "/{petId}/owner-update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<PetProfileDto> updatePetByOwner(
            @Parameter(description = "ID of the pet to update", required = true) @PathVariable Long petId,
            @Parameter(description = "Pet details to update (JSON part)", schema = @Schema(implementation = PetOwnerUpdateDto.class))
            @RequestPart("dto") @Valid PetOwnerUpdateDto updateDto,
            @Parameter(description = "Optional new image file")
            @RequestPart(value = "imageFile", required = false) @Nullable MultipartFile imageFile
    ) throws IOException;

    /**
     * Deactivates a pet owned by the authenticated user. Status changes to INACTIVE.
     *
     * @param petId ID of the pet to deactivate.
     * @return ResponseEntity with the updated PetProfileDto (status INACTIVE) and status 200.
     */
    @Operation(summary = "Deactivate Pet (Owner)", description = "Allows an authenticated Owner to mark their own pet as inactive.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pet deactivated successfully", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = PetProfileDto.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request (e.g., Pet already inactive)", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden (User is not the owner)", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "404", description = "Pet not found", content = @Content(schema = @Schema(implementation = Map.class)))
    })
    @PutMapping("/{petId}/deactivate")
    ResponseEntity<PetProfileDto> deactivatePet(
            @Parameter(description = "ID of the pet to deactivate", required = true) @PathVariable Long petId);


    /**
     * Associates a pet (in PENDING status) with a clinic for activation.
     *
     * @param petId ID of the PENDING pet.
     * @param clinicId ID of the target clinic.
     * @return ResponseEntity with status 204 (No Content) on success.
     */
    @Operation(summary = "Associate Pet with Clinic for Activation (Owner)",
            description = "Allows an Owner to link a PENDING pet to a specific clinic where it will be activated.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Association successful, no content returned."),
            @ApiResponse(responseCode = "400", description = "Bad Request (e.g., Pet not PENDING, already associated)", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden (User is not the owner)", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "404", description = "Pet or Clinic not found", content = @Content(schema = @Schema(implementation = Map.class)))
    })
    @PostMapping("/{petId}/associate-clinic/{clinicId}")
    ResponseEntity<Void> associatePetToClinicForActivation(
            @Parameter(description = "ID of the PENDING pet", required = true) @PathVariable Long petId,
            @Parameter(description = "ID of the clinic for activation", required = true) @PathVariable Long clinicId);


    /**
     * Associates a Veterinarian with one of the owner's pets.
     *
     * @param petId ID of the owner's pet.
     * @param vetId ID of the Veterinarian to associate.
     * @return ResponseEntity with status 204 (No Content) on success.
     */
    @Operation(summary = "Associate Vet with Pet (Owner)",
            description = "Allows an Owner to associate a registered Veterinarian with their pet.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Association successful."),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden (User is not the owner)", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "404", description = "Pet or Vet not found", content = @Content(schema = @Schema(implementation = Map.class))),
    })
    @PostMapping("/{petId}/associate-vet/{vetId}")
    ResponseEntity<Void> associateVetWithPet(
            @Parameter(description = "ID of the pet", required = true) @PathVariable Long petId,
            @Parameter(description = "ID of the Vet to associate", required = true) @PathVariable Long vetId);

    /**
     * Disassociates a Veterinarian from one of the owner's pets.
     * If this is the last associated vet, the pet might become INACTIVE.
     *
     * @param petId ID of the owner's pet.
     * @param vetId ID of the Veterinarian to disassociate.
     * @return ResponseEntity with status 204 (No Content) on success.
     */
    @Operation(summary = "Disassociate Vet from Pet (Owner)",
            description = "Allows an Owner to remove an associated Veterinarian from their pet. May deactivate pet if it's the last vet.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Disassociation successful."),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden (User is not the owner)", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "404", description = "Pet or Vet not found, or Vet not associated", content = @Content(schema = @Schema(implementation = Map.class)))
    })
    @DeleteMapping("/{petId}/associate-vet/{vetId}")
    ResponseEntity<Void> disassociateVetFromPet(
            @Parameter(description = "ID of the pet", required = true) @PathVariable Long petId,
            @Parameter(description = "ID of the Vet to disassociate", required = true) @PathVariable Long vetId);


    // --- Staff Operations ---

    /**
     * Activates a pet currently in PENDING status. Requires Vet authorization.
     * The request body must contain all required pet details, verified by the clinic staff.
     *
     * @param petId ID of the pet to activate.
     * @param activationDto DTO containing verified/updated details required for activation.
     * @return ResponseEntity with the activated PetProfileDto and status 200.
     */
    @Operation(summary = "Activate Pending Pet (Vet)",
            description = "Allows authorized Vet to activate a PENDING pet associated with their clinic, providing complete verified data in the request body.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pet activated successfully", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = PetProfileDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid Activation Data in Request Body or Pet not PENDING", content = @Content(schema = @Schema(implementation = Map.class))), // Error 400 ahora es por el DTO o estado
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden (User is not Vet or not authorized for this pet/clinic)", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "404", description = "Pet, Vet, or Breed specified in DTO not found", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "409", description = "Conflict (Microchip in DTO already exists for another pet)", content = @Content(schema = @Schema(implementation = Map.class)))
    })
    @PutMapping("/{petId}/activate")
    ResponseEntity<PetProfileDto> activatePet(
            @Parameter(description = "ID of the PENDING pet to activate", required = true) @PathVariable Long petId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Verified and complete pet details for activation.", required = true,
                    content = @Content(schema = @Schema(implementation = PetActivationDto.class)))
            @Valid @RequestBody PetActivationDto activationDto
    );


    /**
     * Updates clinical/detailed information for an ACTIVE pet. Requires clinic staff authorization.
     *
     * @param petId ID of the pet to update.
     * @param updateDto DTO containing optional clinical fields to update.
     * @return ResponseEntity with the updated PetProfileDto and status 200.
     */
    @Operation(summary = "Update Pet Clinical Info (Staff)",
            description = "Allows authorized Clinic Staff (Vet/Admin associated with the pet) to update clinical details.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pet updated successfully", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = PetProfileDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid Update Data", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden (Staff not authorized for this pet)", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "404", description = "Pet or Breed not found", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "409", description = "Conflict (Microchip already exists)", content = @Content(schema = @Schema(implementation = Map.class)))
    })
    @PutMapping("/{petId}/clinic-update") // Specific path for clinic staff updates
    ResponseEntity<PetProfileDto> updatePetByClinicStaff(
            @Parameter(description = "ID of the pet to update", required = true) @PathVariable Long petId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Clinical fields to update.", required = true, content = @Content(schema = @Schema(implementation = PetClinicUpdateDto.class)))
            @Valid @RequestBody PetClinicUpdateDto updateDto);


    // --- Retrieval Operations (Mixed Access) ---

    /**
     * Retrieves the detailed profile for a specific pet.
     * Accessible by the pet's owner or authorized clinic staff associated with the pet.
     *
     * @param petId ID of the pet to retrieve.
     * @return ResponseEntity with the PetProfileDto and status 200.
     */
    @Operation(summary = "Get Pet Details by ID", description = "Retrieves detailed profile for a specific pet. Requires Owner or associated Staff authentication.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pet profile retrieved successfully", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = PetProfileDto.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden (User is not Owner or associated Staff)", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "404", description = "Pet Not Found", content = @Content(schema = @Schema(implementation = Map.class)))
    })
    @GetMapping("/{petId}")
    ResponseEntity<PetProfileDto> findPetById(
            @Parameter(description = "ID of the pet", required = true) @PathVariable Long petId);

    /**
     * Retrieves a paginated list of pets associated with the clinic of the authenticated staff member.
     * Association means the pet is pending activation at the clinic OR is associated with any vet working at the clinic.
     * Requires an ADMIN or VET role. The clinic is determined from the authenticated user's profile.
     *
     * @param pageable Pagination and sorting information.
     * @return A {@link ResponseEntity} containing a {@link Page} of {@link PetProfileDto} objects associated with the staff's clinic,
     *         with HTTP status 200 (OK).
     */
    @Operation(summary = "List Pets Associated with My Clinic (Staff)",
            description = "Retrieves pets associated (pending or via vet) with the clinic of the authenticated staff member (Vet/Admin).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pets retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden (User is not Vet/Admin or has no associated clinic)", content = @Content(schema = @Schema(implementation = Map.class))),
    })
    @GetMapping("/clinic")
    ResponseEntity<Page<PetProfileDto>> findMyClinicPets(
            @Parameter(description = "Pagination details (e.g., page=0&size=10&sort=name)")
            @PageableDefault(sort = "name") Pageable pageable);

    /**
     * Retrieves a list of pets pending activation at the clinic of the authenticated staff member.
     * Requires an ADMIN or VET role. The clinic is determined from the authenticated user's profile.
     *
     * @return A {@link ResponseEntity} containing a {@link List} of {@link PetProfileDto} objects for pets pending activation
     *         at the staff's clinic, with HTTP status 200 (OK).
     */
    @Operation(summary = "List Pending Activation Pets for My Clinic (Staff)",
            description = "Retrieves pets pending activation at the clinic of the authenticated staff member (Vet/Admin).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pending pets retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = PetProfileDto.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden (User is not Vet/Admin or has no associated clinic)", content = @Content(schema = @Schema(implementation = Map.class))),
    })
    @GetMapping("/clinic/pending")
    ResponseEntity<List<PetProfileDto>> findMyClinicPendingPets();


    // --- Breed Retrieval ---

    /**
     * Retrieves a list of breeds for a given species.
     * Publicly accessible or requires basic authentication.
     *
     * @param specie The species (DOG, CAT, FERRET, RABBIT).
     * @return ResponseEntity with a List of BreedDto and status 200.
     */
    @Operation(summary = "List Breeds by Species", description = "Retrieves available breeds for a specified species. Useful for dropdowns.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Breeds retrieved successfully", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = List.class))),
            @ApiResponse(responseCode = "400", description = "Invalid species value provided", content = @Content(schema = @Schema(implementation = Map.class)))
    })
    @GetMapping("/breeds/{specie}")
    ResponseEntity<List<BreedDto>> findBreedsBySpecie(
            @Parameter(description = "Species enum value (DOG, CAT, FERRET, RABBIT)", required = true, schema = @Schema(implementation = Specie.class))
            @PathVariable Specie specie);


    /**
     * Requests the generation of a certificate from a specific clinic on behalf of a pet owner.
     * This request is associated with a pet and a clinic where one of the pet's veterinarians operates.
     *
     * @param petId   the ID of the pet for which the certificate is being requested
     * @param clinicId the ID of the clinic to which the certificate generation request is sent
     * @return a {@link ResponseEntity} with a status code:
     *         204 (No Content) if the certificate request is successfully submitted,
     *         401 (Unauthorized) if the user is not authenticated,
     *         403 (Forbidden) if the user is not the owner or the clinic is not associated,
     *         404 (Not Found) if the pet or clinic does not exist
     */
    @Operation(summary = "Request Certificate Generation from Clinic (Owner)",
            description = "Allows the Pet Owner to request a certificate generation from a specific clinic with which one of their pet's veterinarians is associated.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Certificate request submitted successfully."),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden (User not owner or Clinic not associated)", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "404", description = "Pet or Clinic not found", content = @Content(schema = @Schema(implementation = Map.class)))
    })
    @PostMapping("/{petId}/request-certificate/{clinicId }")
    ResponseEntity<Void> requestCertificateGeneration(
            @Parameter(description = "ID of the pet", required = true) @PathVariable Long petId,
            @Parameter(description = "ID of the Clinic to request the certificate from", required = true) @PathVariable Long clinicId );

    /**
     * Retrieves a list of pets that have a pending certificate request for a specific clinic.
     * This endpoint is intended for clinic staff (Admin or Vet) to view these requests.
     *
     * @param clinicId The ID of the clinic for which to retrieve pending certificate requests.
     * @return A ResponseEntity containing a list of {@link PetProfileDto} objects for pets
     *         with pending certificate requests at the specified clinic.
     *         Returns 403 if the requester is not staff of the clinic.
     */
    @Operation(summary = "List Pets with Pending Certificate Requests for a Clinic",
            description = "For clinic staff: Retrieves pets that have an active certificate generation request " +
                    "directed to the specified clinic. Shows pets that are currently ACTIVE.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of pets with pending requests retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = PetProfileDto.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User is not staff of the specified clinic"),
            @ApiResponse(responseCode = "404", description = "Clinic not found")
    })
    @GetMapping("/{clinicId}/pending-certificate-requests")
    @SecurityRequirement(name = "bearerAuth")
    ResponseEntity<List<PetProfileDto>> getPetsWithPendingCertificateRequests(
            @Parameter(description = "ID of the clinic", required = true) @PathVariable Long clinicId);
}

