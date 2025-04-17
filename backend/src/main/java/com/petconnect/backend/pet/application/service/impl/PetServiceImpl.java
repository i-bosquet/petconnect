package com.petconnect.backend.pet.application.service.impl;

import com.petconnect.backend.common.helper.EntityFinderHelper;
import com.petconnect.backend.common.helper.AuthorizationHelper;
import com.petconnect.backend.exception.MicrochipAlreadyExistsException;
import com.petconnect.backend.pet.application.dto.*;
import com.petconnect.backend.pet.application.mapper.BreedMapper;
import com.petconnect.backend.pet.application.mapper.PetMapper;
import com.petconnect.backend.pet.application.service.PetService;
import com.petconnect.backend.pet.domain.model.Breed;
import com.petconnect.backend.pet.domain.model.Pet;
import com.petconnect.backend.pet.domain.model.PetStatus;
import com.petconnect.backend.pet.domain.model.Specie;
import com.petconnect.backend.pet.domain.repository.BreedRepository;
import com.petconnect.backend.pet.domain.repository.PetRepository;
import com.petconnect.backend.user.domain.model.*;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Implementation of the {@link PetService} interface.
 * Handles business logic for managing Pet entities, including creation, activation,
 * updates (by owner and staff), status changes, breed lookups, and vet associations.
 * Includes authorization checks based on the requesting user's role and relationship to the pet/clinic.
 *
 * @author ibosquet
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PetServiceImpl implements PetService {

    // --- Repositories ---
    private final PetRepository petRepository;
    private final BreedRepository breedRepository;

    // --- Mappers & Helpers ---
    private final PetMapper petMapper;
    private final BreedMapper breedMapper;
    private final EntityFinderHelper entityFinderHelper;
    private final AuthorizationHelper authorizationHelper;

    // --- Configuration ---
    // Default path base for pet avatars if not provided. Configurable via application properties.
    @Value("${app.default.pet.image.path:images/avatars/pets/}")
    private String defaultPetImagePathBase;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public PetProfileDto registerPet(PetRegistrationDto registrationDto, Long ownerId) {
        Owner owner = entityFinderHelper.findOwnerOrFail(ownerId); // Find and validate owner
        Breed assignedBreed = resolveBreed(registrationDto.breedId(), registrationDto.specie()); // Ensures non-null breed
        String imagePath = determineInitialImagePath(registrationDto.image(), assignedBreed); // Determine image

        // Create new Pet entity - Owner provides initial details
        Pet newPet = Pet.builder()
                .name(registrationDto.name())
                .owner(owner)
                .breed(assignedBreed)
                .image(imagePath)
                .status(PetStatus.PENDING) // Start as PENDING
                // Set optional fields provided by owner during registration
                .birthDate(registrationDto.birthDate())
                .color(registrationDto.color())
                .gender(registrationDto.gender())
                .microchip(registrationDto.microchip())
                .build();

        Pet savedPet = petRepository.save(newPet);
        log.info("Owner {} registered new pet {} (ID: {}) with status PENDING", owner.getUsername(), savedPet.getName(), savedPet.getId());
        return petMapper.toProfileDto(savedPet);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void associatePetToClinicForActivation(Long petId, Long clinicId, Long ownerId) {
        Pet pet = findPetByIdAndOwnerOrFail(petId, ownerId); // Verify owner and find pet
        ensurePetIsInStatus(pet, PetStatus.PENDING, "associate for activation");
        if (pet.getPendingActivationClinic() != null) {
            throw new IllegalStateException("Pet ID " + petId + " is already pending activation at clinic " + pet.getPendingActivationClinic().getId());
        }

        Clinic targetClinic = entityFinderHelper.findClinicOrFail(clinicId);

        pet.setPendingActivationClinic(targetClinic);
        petRepository.save(pet);
        log.info("Owner {} associated PENDING pet {} with clinic {} for activation.", ownerId, petId, clinicId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public PetProfileDto activatePet(Long petId, PetActivationDto activationDto, Long staffId) {
        ClinicStaff activatingStaff = entityFinderHelper.findClinicStaffOrFail(staffId, "activate pet");
        Clinic staffClinic = activatingStaff.getClinic();
        Pet petToActivate = entityFinderHelper.findPetByIdOrFail(petId);
        ensurePetIsInStatus(petToActivate, PetStatus.PENDING, "activate");
        if (petToActivate.getPendingActivationClinic() == null || !petToActivate.getPendingActivationClinic().getId().equals(staffClinic.getId())) {
            throw new AccessDeniedException("Staff " + staffId + " from clinic " + staffClinic.getId() +
                    " is not authorized to activate pet " + petId + " (not pending at this clinic)");
        }

        validateMicrochipUniqueness(activationDto.microchip(), petId);

        Specie originalSpecie = petToActivate.getBreed().getSpecie(); // Especie no debe cambiar
        Breed resolvedBreed = resolveBreed(activationDto.breedId(), originalSpecie);
        updatePetEntityFromActivationDto(petToActivate, activationDto, resolvedBreed);

        petToActivate.setStatus(PetStatus.ACTIVE);
        petToActivate.setPendingActivationClinic(null);
        Vet assignedVet = assignVetOnActivation(activatingStaff); // Ya simplificado para Vet
        petToActivate.addVet(assignedVet);

        // Assign a Vet automatically
        Pet activatedPet = petRepository.save(petToActivate);
        log.info("Vet {} activated Pet {}. Assigned Vet: {}", staffId, petId, assignedVet.getId());
        return petMapper.toProfileDto(activatedPet);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public PetProfileDto deactivatePet(Long petId, Long ownerId) {
        Pet petToDeactivate = findPetByIdAndOwnerOrFail(petId, ownerId); // Verify ownership
        ensurePetIsNotInStatus(petToDeactivate, PetStatus.INACTIVE, "deactivate"); // Check not already inactive

        // Deactivate: Change status and potentially clear associations if needed
        petToDeactivate.setStatus(PetStatus.INACTIVE);

        Pet deactivatedPet = petRepository.save(petToDeactivate);
        log.info("Owner {} deactivated Pet {}", ownerId, petId);
        return petMapper.toProfileDto(deactivatedPet);
    }

    /**
     * {@inheritDoc}
     * Owner can update all non-status/association fields. Auditing tracks the change.
     */
    @Override
    @Transactional
    public PetProfileDto updatePetByOwner(Long petId, PetOwnerUpdateDto updateDto, Long ownerId) {
        Pet petToUpdate = findPetByIdAndOwnerOrFail(petId, ownerId);

        log.info("Pet entity found (before update): ID={}, Name='{}', Color='{}', Image='{}', Microchip='{}', BreedId={}, OwnerId={}",
                petToUpdate.getId(), petToUpdate.getName(), petToUpdate.getColor(), petToUpdate.getImage(),
                petToUpdate.getMicrochip(), (petToUpdate.getBreed() != null ? petToUpdate.getBreed().getId() : null), ownerId);

        boolean changed = applyPetUpdates(petToUpdate, updateDto, null);

        log.info("Pet entity state AFTER applying updates (before save): ID={}, Name='{}', Color='{}', Image='{}', Microchip='{}', BreedId={}, Changed={}",
                petToUpdate.getId(), petToUpdate.getName(), petToUpdate.getColor(), petToUpdate.getImage(),
                petToUpdate.getMicrochip(), (petToUpdate.getBreed() != null ? petToUpdate.getBreed().getId() : null), changed);

        // Save if changed and map response
        Pet updatedPet = petToUpdate;
        if (changed) {
            log.info("Changes detected, attempting to save Pet {}", petId);
            updatedPet = petRepository.save(petToUpdate);
            log.info("Owner {} successfully updated Pet {}", ownerId, petId);
        } else {
            log.info("No changes detected for Pet {}, update by owner {} skipped.", petId, ownerId);
        }
        return petMapper.toProfileDto(updatedPet);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public PetProfileDto updatePetByClinicStaff(Long petId, PetClinicUpdateDto updateDto, Long staffId) {
        Pet petToUpdate = entityFinderHelper.findPetByIdOrFail(petId);
        authorizationHelper.verifyUserAuthorizationForPet(staffId, petToUpdate, "update clinical info for");

        boolean changed = applyPetUpdates(petToUpdate, null, updateDto);

        // Save if changed and map response
        Pet updatedPet = petToUpdate;
        if (changed) {
            updatedPet = petRepository.save(petToUpdate);
            log.info("Staff {} updated Pet {}", staffId, petId);
        } else {
            log.info("No changes detected for Pet {}, update by staff {} skipped.", petId, staffId);
        }
        return petMapper.toProfileDto(updatedPet);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public Page<PetProfileDto> findPetsByOwner(Long ownerId, Pageable pageable) {
        List<PetStatus> defaultStatuses = List.of(PetStatus.ACTIVE, PetStatus.PENDING);
        Page<Pet> petPage = petRepository.findByOwnerIdAndStatusIn(ownerId, defaultStatuses, pageable);
        return petPage.map(petMapper::toProfileDto);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public Page<PetProfileDto> findPetsByClinic(Long requesterUserId, Pageable pageable) {
        ClinicStaff staff = entityFinderHelper.findClinicStaffOrFail(requesterUserId, "find pets for clinic");
        Long clinicId = staff.getClinic().getId();
        Page<Pet> petPage = petRepository.findPetsAssociatedWithClinic(clinicId, pageable);
        return petPage.map(petMapper::toProfileDto);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public List<PetProfileDto> findPendingActivationPetsByClinic(Long requesterUserId) {
        ClinicStaff staff = entityFinderHelper.findClinicStaffOrFail(requesterUserId, "find pending pets for clinic");
        Long clinicId = staff.getClinic().getId();
        List<Pet> pendingPets = petRepository.findByPendingActivationClinicIdAndStatus(clinicId, PetStatus.PENDING);
        return petMapper.toProfileDtoList(pendingPets);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public PetProfileDto findPetById(Long petId, Long requesterUserId) {
        Pet pet = entityFinderHelper.findPetByIdOrFail(petId);
        authorizationHelper.verifyUserAuthorizationForPet(requesterUserId, pet, "view"); // Verify owner or associated staff
        return petMapper.toProfileDto(pet);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public List<BreedDto> findBreedsBySpecie(Specie specie) {
        if (specie == null) {
            log.warn("findBreedsBySpecie called with null specie.");
            return Collections.emptyList(); // Return empty list for null input
        }
        List<Breed> breeds = breedRepository.findBySpecieOrderByNameAsc(specie);
        return breedMapper.toDtoList(breeds);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void associateVetWithPet(Long petId, Long vetId, Long ownerId) {
        Pet pet = findPetByIdAndOwnerOrFail(petId, ownerId);
        Vet vet = entityFinderHelper.findVetOrFail(vetId);

        log.debug("Checking existing associations for Pet ID {}. Current associatedVets size: {}", petId, pet.getAssociatedVets().size());

        if (pet.getAssociatedVets().contains(vet)) {
            log.warn("Vet {} is already associated with Pet {}", vetId, petId);
            throw new IllegalStateException("Vet " + vetId + " is already associated with pet " + petId + ".");
        }

        pet.addVet(vet);
        if (pet.getStatus() == PetStatus.INACTIVE && !pet.getAssociatedVets().isEmpty()) {
            log.info("Reactivating Pet {} as a Vet has been associated.", petId);
            pet.setStatus(PetStatus.ACTIVE);
        }
        petRepository.save(pet);
        log.info("Owner {} associated Vet {} with Pet {}", ownerId, vetId, petId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void disassociateVetFromPet(Long petId, Long vetId, Long ownerId) {
        Pet pet = findPetByIdAndOwnerOrFail(petId, ownerId);
        Vet vet = entityFinderHelper.findVetOrFail(vetId);

        if (!pet.getAssociatedVets().contains(vet)) {
            log.warn("Vet {} is not associated with Pet {}. Cannot disassociate.", vetId, petId);
            return;
        }

        pet.removeVet(vet);
        log.info("Owner {} disassociated Vet {} from Pet {}", ownerId, vetId, petId);

        if (pet.getAssociatedVets().isEmpty() && pet.getStatus() == PetStatus.ACTIVE) {
            pet.setStatus(PetStatus.INACTIVE);
            log.warn("Pet {} automatically set to INACTIVE as last associated Vet was removed.", petId);
        }
        petRepository.save(pet);
    }


    // --- PRIVATE HELPER METHODS ---

    /**
     * Finds a Pet by ID and verifies ownership or throws.
     */
    private Pet findPetByIdAndOwnerOrFail(Long petId, Long ownerId) {
        Pet pet = entityFinderHelper.findPetByIdOrFail(petId);
        if (pet.getOwner() == null || !Objects.equals(pet.getOwner().getId(), ownerId)) {
            throw new AccessDeniedException("User " + ownerId + " is not the owner of pet " + petId);
        }
        return pet;
    }

    /**
     * Resolves Breed, falling back to Mixed/Other for the given species.
     */
    private Breed resolveBreed(Long breedId, @NotNull Specie specie) {
        if (breedId != null) {
            Breed specificBreed = entityFinderHelper.findBreedOrFail(breedId);
            if (specificBreed.getSpecie() != specie) {
                log.error("Breed ID {} ({}) does not match the expected species {}", breedId, specificBreed.getSpecie(), specie);
                throw new IllegalArgumentException("Provided Breed ID " + breedId + " does not belong to species " + specie);
            }
            return specificBreed;
        } else {
            log.debug("No breed ID provided for species {}, finding 'Mixed/Other'.", specie);
            return breedRepository.findByNameAndSpecie("Mixed/Other", specie)
                    .orElseThrow(() -> {
                        log.error("CRITICAL: Fallback breed 'Mixed/Other' not found for species {}!", specie);
                        return new IllegalStateException("Default breed configuration error for species " + specie);
                    });
        }
    }

    /**
     * Determines initial image path using provided path or breed's default.
     */
    private String determineInitialImagePath(String providedImagePath, @NotNull Breed assignedBreed) {
        // Use user-provided image if available
        if (StringUtils.hasText(providedImagePath)) {
            log.debug("Using provided image path: {}", providedImagePath);
            return providedImagePath;
        }

        // Use specific breed image if the breed is not generic and has a URL
        boolean isGenericBreed = "Mixed/Other".equals(assignedBreed.getName()) || "Standard/Other".equals(assignedBreed.getName());
        if (!isGenericBreed && StringUtils.hasText(assignedBreed.getImageUrl())) {
            log.debug("Using specific breed image path: {}", assignedBreed.getImageUrl());
            return assignedBreed.getImageUrl();
        }

        // Fallback to generic species image (Species is guaranteed from assignedBreed)
        Specie specie = assignedBreed.getSpecie();
        String basePath = defaultPetImagePathBase.endsWith("/") ? defaultPetImagePathBase : defaultPetImagePathBase + '/';
        String defaultImage = basePath + specie.name().toLowerCase() + ".png";
        log.debug("Assigning default species image for {}: {}", specie, defaultImage);
        return defaultImage;
    }


    /**
     * Validates microchip uniqueness, excluding the pet being updated.
     */
    private void validateMicrochipUpdate(String microchip, Pet petBeingUpdated) {
        // Check only if changed
        if (StringUtils.hasText(microchip) &&
                !Objects.equals(microchip, petBeingUpdated.getMicrochip()) && petRepository.existsByMicrochipAndIdNot(microchip, petBeingUpdated.getId())) {
            throw new MicrochipAlreadyExistsException(microchip);
        }
    }

    /**
     * Returns the Veterinarian performing the activation.
     * Assumes the activating staff member is guaranteed to be a Vet due to security constraints.
     *
     * @param activatingStaff The ClinicStaff member performing the activation (must be a Vet).
     * @return The activating Vet.
     * @throws AccessDeniedException if the activating staff member is unexpectedly not a Vet instance
     *                              (should not happen if security config is correct, defensive check).
     */
    private Vet assignVetOnActivation(ClinicStaff activatingStaff) {
        if (!(activatingStaff instanceof Vet activatingVet)) {
            // Defensive check - Should ideally be unreachable if SecurityConfig enforces ROLE_VET
            log.error("CRITICAL: Activation process reached assignVetOnActivation but activator (ID: {}) is not a Vet instance.", activatingStaff.getId());
            throw new AccessDeniedException("Activation failed: The user performing the activation is not a veterinarian.");
        }
        log.debug("Assigning activating Vet (ID: {}) to the pet.", activatingVet.getId());
        return activatingVet;
    }

    /**
     * Checks if the given Pet is in the expected status.
     * Throws IllegalStateException if the status does not match.
     *
     * @param pet               The Pet entity to check.
     * @param expectedStatus    The required PetStatus.
     * @param actionDescription Description of the action requiring this status (for error message).
     * @throws IllegalStateException if the pet's status is not the expected one.
     */
    private void ensurePetIsInStatus(Pet pet, PetStatus expectedStatus, String actionDescription) {
        if (pet.getStatus() != expectedStatus) {
            throw new IllegalStateException(String.format("Pet %d must be in %s status to %s, but was %s.",
                    pet.getId(), expectedStatus, actionDescription, pet.getStatus()));
        }
    }

    /**
     * Checks if the given Pet is NOT in the specified status.
     * Throws IllegalStateException if the status matches the forbidden status.
     *
     * @param pet               The Pet entity to check.
     * @param forbiddenStatus   The PetStatus that the pet should NOT have.
     * @param actionDescription Description of the action being attempted (for error message).
     * @throws IllegalStateException if the pet's status matches the forbidden one.
     */
    private void ensurePetIsNotInStatus(Pet pet, PetStatus forbiddenStatus, String actionDescription) {
        if (pet.getStatus() == forbiddenStatus) {
            throw new IllegalStateException(String.format("Cannot %s pet %d because it is already in %s status.",
                    actionDescription, pet.getId(), forbiddenStatus));
        }
    }

    /**
     * Validates the uniqueness of a microchip number, optionally excluding the pet being updated.
     * Throws MicrochipAlreadyExistsException if a conflict is found.
     *
     * @param microchip      The microchip number to validate (can be null or blank, validation handles this).
     * @param petIdToExclude The ID of the pet being updated (null if creating a new pet).
     * @throws MicrochipAlreadyExistsException if the microchip is already used by another pet.
     */
    private void validateMicrochipUniqueness(String microchip, Long petIdToExclude) {
        if (!StringUtils.hasText(microchip)) {
            return;
        }

        boolean exists;
        if (petIdToExclude == null) {
            exists = petRepository.existsByMicrochip(microchip);
        } else {
            exists = petRepository.existsByMicrochipAndIdNot(microchip, petIdToExclude);
        }

        if (exists) {
            throw new MicrochipAlreadyExistsException(microchip);
        }
    }

    /**
     * Applies updates to a Pet entity from either an Owner or ClinicStaff DTO.
     * Handles fetching original species, validating microchip uniqueness, resolving breed,
     * and calling the appropriate mapper update method.
     *
     * @param petToUpdate     The Pet entity to modify.
     * @param ownerUpdateDto  The DTO from owner update (null if called by staff).
     * @param clinicUpdateDto The DTO from staff update (null if called by owner).
     * @return true if any changes were applied, false otherwise.
     */
    private boolean applyPetUpdates(Pet petToUpdate, PetOwnerUpdateDto ownerUpdateDto, PetClinicUpdateDto clinicUpdateDto) {
        Specie originalSpecie = petToUpdate.getBreed().getSpecie();

        // Resolve Breed if ID is provided and different
        Breed resolvedBreed = petToUpdate.getBreed();

        // Determine which fields to potentially update based on which DTO is present
        String newMicrochip;
        if ((ownerUpdateDto != null)) newMicrochip = ownerUpdateDto.microchip();
        else if (clinicUpdateDto != null) newMicrochip = clinicUpdateDto.microchip();
        else newMicrochip = null;

        Long newBreedId;
        if ((ownerUpdateDto != null)) newBreedId = ownerUpdateDto.breedId();
        else if (clinicUpdateDto != null) newBreedId = clinicUpdateDto.breedId();
        else newBreedId = null;

        if (newBreedId != null && !Objects.equals(newBreedId, petToUpdate.getBreed().getId())) {
            resolvedBreed = resolveBreed(newBreedId, originalSpecie);
        }

        log.info("Calling mapper. DTO(Owner): {}, DTO(Clinic): {}, ResolvedBreedId: {}",
                ownerUpdateDto, clinicUpdateDto, (resolvedBreed != null ? resolvedBreed.getId() : null));

        // Validate Microchip Uniqueness if it's being changed
        validateMicrochipUpdate(newMicrochip, petToUpdate);

        // Call the appropriate mapper method
        boolean changed;
        if (ownerUpdateDto != null) {
            changed = petMapper.updateFromOwnerDto(ownerUpdateDto, petToUpdate, resolvedBreed);
        } else if (clinicUpdateDto != null) {
            changed = petMapper.updateFromClinicDto(clinicUpdateDto, petToUpdate, resolvedBreed);
        } else {
            changed = false;
        }

        log.info("Mapper finished applying updates. Overall changed status: {}", changed);
        return changed;
    }

    /**
     * Updates the Pet entity with data from the validated PetActivationDto.
     * This ensures the pet record reflects the data confirmed during activation.
     */
    private void updatePetEntityFromActivationDto(Pet pet, PetActivationDto dto, Breed resolvedBreed) {
        pet.setName(dto.name());
        pet.setColor(dto.color());
        pet.setGender(dto.gender());
        pet.setBirthDate(dto.birthDate());
        pet.setMicrochip(dto.microchip());
        pet.setBreed(resolvedBreed);
        pet.setImage(dto.image());
    }

}