package com.petconnect.backend.pet.application.service;

import com.petconnect.backend.exception.EntityNotFoundException;
import com.petconnect.backend.pet.application.dto.*;
import com.petconnect.backend.pet.domain.model.PetStatus;
import com.petconnect.backend.pet.domain.model.Specie;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.lang.Nullable;

import java.io.IOException;
import java.util.List;

/**
 * Service interface for managing Pet entities and related operations.
 * Defines the contract for business logic related to pets, including registration,
 * activation, updates, status changes, and retrieval. Authorization checks
 * are expected within the implementations.
 *
 * @author ibosquet
 */
public interface PetService {
    /**
     * Registers a new pet for the currently authenticated owner.
     * Handles image storage if provided.
     *
     * @param registrationDto DTO containing initial pet details.
     * @param ownerId The ID of the owner performing the registration.
     * @param imageFile Optional uploaded image file for the pet's avatar.
     * @return The profile DTO of the newly registered pet.
     * @throws IOException if image storage fails.
     * @throws com.petconnect.backend.exception.EntityNotFoundException if the specified breedId (if any) does not exist.
     */
    PetProfileDto registerPet(PetRegistrationDto registrationDto, Long ownerId, MultipartFile imageFile) throws IOException;

    /**
     * Activates a PENDING pet and updates its details.
     * Sets the pet's status to ACTIVE and clears the pending clinic association.
     * Associates the activating Vet with the pet.
     *
     * @param petId The ID of the pet to activate.
     * @param vetId The ID of the Vet performing the activation.
     * @param activationDto DTO containing required/verified clinical details. Validation rules are applied.
     * @return The profile DTO of the activated pet.
     * @throws IllegalStateException if required fields (name, birthDate, gender, microchip, breed, image) are missing on the Pet entity, or if status is not PENDING.
     * @throws com.petconnect.backend.exception.MicrochipAlreadyExistsException if the pet's existing microchip conflicts with another pet.
     */
    PetProfileDto activatePet(Long petId, PetActivationDto activationDto, Long vetId);

    /**
     * Updates pet information editable by the owner. Handles optional image update.
     *
     * @param petId The ID of the pet to update.
     * @param updateDto DTO containing the fields to update.
     * @param imageFile Optional new image file to replace the existing one.
     * @param ownerId The ID of the currently authenticated owner.
     * @return The updated profile DTO of the pet.
     * @throws com.petconnect.backend.exception.EntityNotFoundException if the pet is not found.
     * @throws org.springframework.security.access.AccessDeniedException if the user is not the owner of the pet.
     */
    PetProfileDto updatePetByOwner(Long petId, PetOwnerUpdateDto updateDto,  Long ownerId, @Nullable MultipartFile imageFile) throws IOException;

    /**
     * Updates clinical pet information editable by authorized clinic staff. Handles optional image update.
     *
     * @param petId The ID of the pet to update.
     * @param updateDto DTO containing the fields to update.
     * @param staffId The ID of the ClinicStaff performing the update.
     * @return The updated profile DTO of the pet.
     */
    PetProfileDto updatePetByClinicStaff(Long petId, PetClinicUpdateDto updateDto, Long staffId);


    /**
     * Marks a pet as INACTIVE. Typically performed by the owner.
     *
     * @param petId The ID of the pet to deactivate.
     * @param ownerId The ID of the currently authenticated owner.
     * @return The profile DTO of the deactivated pet.
     * @throws com.petconnect.backend.exception.EntityNotFoundException if the pet is not found.
     * @throws org.springframework.security.access.AccessDeniedException if the user is not the owner.
     * @throws IllegalStateException if the pet is already inactive.
     */
    PetProfileDto deactivatePet(Long petId, Long ownerId);

    /**
     * Retrieves a paginated list of pets belonging to the specified owner.
     * Filters by status (ACTIVE, PENDING by default). Requires authorization check.
     *
     * @param ownerId The ID of the owner whose pets are requested.
     * @param pageable Pagination information.
     * @return A Page of PetProfileDto objects.
     * @throws org.springframework.security.access.AccessDeniedException if the requester is not owner or authorized staff.
     */
    Page<PetProfileDto> findPetsByOwner(Long ownerId, @Nullable List<PetStatus> statuses, Pageable pageable);

    /**
     * Retrieves the detailed profile of a specific pet by its ID.
     * Includes authorization check (requester must be owner or authorized staff).
     *
     * @param petId The ID of the pet to retrieve.
     * @param requesterUserId The ID of the user requesting the profile.
     * @return The PetProfileDto.
     * @throws com.petconnect.backend.exception.EntityNotFoundException if the pet is not found.
     * @throws org.springframework.security.access.AccessDeniedException if the requester is not authorized.
     */
    PetProfileDto findPetById(Long petId, Long requesterUserId);

    /**
     * Retrieves a list of breeds filtered by the specified species, ordered by name.
     *
     * @param specie The species to filter by.
     * @return A list of BreedDto objects.
     */
    List<BreedDto> findBreedsBySpecie(Specie specie);

    /**
     * Associates a pet (in PENDING state) with a clinic for activation.
     *
     * @param petId The ID of the PENDING pet.
     * @param clinicId The ID of the target clinic.
     * @param ownerId The ID of the authenticated owner making the association.
     * @throws com.petconnect.backend.exception.EntityNotFoundException if a pet or clinic isn't found.
     * @throws org.springframework.security.access.AccessDeniedException if the user is not the owner.
     * @throws IllegalStateException if the pet is not in PENDING status.
     */
    void associatePetToClinicForActivation(Long petId, Long clinicId, Long ownerId);

    /**
     * Associates a Veterinarian with a specific Pet. Performed by the Pet's Owner.
     *
     * @param petId The ID of the Pet.
     * @param vetId The ID of the Vet to associate.
     * @param ownerId The ID of the authenticated Owner performing the action.
     * @throws com.petconnect.backend.exception.EntityNotFoundException if pet or vet not found.
     * @throws org.springframework.security.access.AccessDeniedException if the user is not the owner.
     * @throws IllegalStateException if the association already exists.
     */
    void associateVetWithPet(Long petId, Long vetId, Long ownerId);

    /**
     * Disassociates a Veterinarian from a specific Pet. Performed by the Pet's Owner.
     *
     * @param petId The ID of the Pet.
     * @param vetId The ID of the Vet to disassociate.
     * @param ownerId The ID of the authenticated Owner performing the action.
     * @throws com.petconnect.backend.exception.EntityNotFoundException if pet or vet not found.
     * @throws org.springframework.security.access.AccessDeniedException if the user is not the owner.
     */
    void disassociateVetFromPet(Long petId, Long vetId, Long ownerId);

    /**
     * Retrieves a paginated list of pets associated with a specific clinic.
     * Association is determined if the pet is pending activation at the clinic
     * or if it's associated with any Vet working at that clinic.
     * Requires authorization (requester must be staff of that clinic).
     *
     * @param requesterUserId The ID of the staff member making the request.
     * @param pageable Pagination information.
     * @return A Page of PetProfileDto objects associated with the clinic.
     * @throws com.petconnect.backend.exception.EntityNotFoundException if a clinic not found.
     * @throws org.springframework.security.access.AccessDeniedException if requester is not authorized staff of the clinic.
     */
    Page<PetProfileDto> findPetsByClinic(Long requesterUserId, Pageable pageable);

    /**
     * Finds pets pending activation at a specific clinic.
     * Requires authorization (requester must be staff of that clinic).
     *
     * @param requesterUserId The ID of the staff member making the request.
     * @return A List of PetProfileDto for pets pending activation.
     * @throws com.petconnect.backend.exception.EntityNotFoundException if a clinic not found.
     * @throws org.springframework.security.access.AccessDeniedException if requester is not authorized staff of the clinic.
     */
    List<PetProfileDto> findPendingActivationPetsByClinic(Long requesterUserId);

    /**
     * Handles the owner's request to generate a certificate for their pet,
     * directed at a specific associated Veterinarian.
     * Publishes a CertificateRequestedEvent.
     *
     * @param petId The ID of the Pet.
     * @param clinicId The ID of the Clinic with vets associated with the pet.
     * @param ownerId The ID of the authenticated Owner making the request.
     * @throws EntityNotFoundException if pet or vet not found.
     * @throws AccessDeniedException if the user is not the owner, or the vet is not associated with the pet.
     */
    void requestCertificateGeneration(Long petId, Long clinicId, Long ownerId);

    /**
     * Finds pets with pending certificate requests for a specific clinic.
     * Requires authorization (requester must be a staff member of the clinic).
     *
     * @param clinicId The ID of the clinic for which to find pending certificate requests.
     * @param requesterStaffId The ID of the clinic staff member making the request.
     * @return A list of PetProfileDto objects representing pets with pending certificate requests for the requested clinic.
     * @throws EntityNotFoundException if the clinic is not found.
     * @throws AccessDeniedException if the requester is not authorized staff of the clinic.
     */
    List<PetProfileDto> findPetsWithPendingCertRequestsForClinic(Long clinicId, Long requesterStaffId);
}
