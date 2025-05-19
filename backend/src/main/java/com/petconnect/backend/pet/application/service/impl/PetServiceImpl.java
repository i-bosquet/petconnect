package com.petconnect.backend.pet.application.service.impl;

import com.petconnect.backend.pet.application.event.CertificateRequestedEvent;
import com.petconnect.backend.common.helper.EntityFinderHelper;
import com.petconnect.backend.common.helper.AuthorizationHelper;
import com.petconnect.backend.exception.MicrochipAlreadyExistsException;
import com.petconnect.backend.pet.port.spi.PetEventPublisherPort;
import com.petconnect.backend.pet.application.event.PetActivationRequestedEvent;
import com.petconnect.backend.pet.application.event.PetActivatedEvent;
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
import com.petconnect.backend.common.service.ImageService;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.lang.Nullable;
import java.io.IOException;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

    private final PetRepository petRepository;
    private final BreedRepository breedRepository;

    private final PetMapper petMapper;
    private final BreedMapper breedMapper;
    private final EntityFinderHelper entityFinderHelper;
    private final AuthorizationHelper authorizationHelper;
    private final PetEventPublisherPort petEventPublisher;
    private final ImageService imageService;

    @Value("${app.default.pet.image.path:images/avatars/pets/}")
    private String defaultPetImagePathBase;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public PetProfileDto registerPet(PetRegistrationDto registrationDto, Long ownerId, @Nullable MultipartFile imageFile) {
        Owner owner = entityFinderHelper.findOwnerOrFail(ownerId);
        Breed assignedBreed = resolveBreed(registrationDto.breedId(), registrationDto.specie());

        String imagePathToSave = null;

        if (imageFile != null && !imageFile.isEmpty()) {
            log.debug("Image file provided for new pet registration by owner {}", ownerId);
            try {
                imagePathToSave = imageService.storeImage(imageFile, "pets/avatars");
            } catch (IOException | IllegalArgumentException e) {
                log.error("Failed to store uploaded image for pet registration. Error: {}", e.getMessage());
                throw new RuntimeException("Failed to process uploaded image: " + e.getMessage(), e);
            }
        } else {
            log.debug("No image file provided for registration, will use default determined by mapper/entity.");
        }

        if (StringUtils.hasText(registrationDto.microchip()) && petRepository.existsByMicrochip(registrationDto.microchip())) {
            log.warn("Attempt to register pet with existing microchip: {}", registrationDto.microchip());
            throw new MicrochipAlreadyExistsException(registrationDto.microchip());
        }

        Pet newPet = Pet.builder()
                .name(registrationDto.name())
                .owner(owner)
                .breed(assignedBreed)
                .image(imagePathToSave)
                .status(PetStatus.PENDING)
                .birthDate(registrationDto.birthDate())
                .color(registrationDto.color())
                .gender(registrationDto.gender())
                .microchip(registrationDto.microchip())
                .build();
        if (imagePathToSave == null) {
            newPet.setImage(determineInitialImagePath(assignedBreed));
            log.debug("Assigning default image path: {}", newPet.getImage());
        }

        Pet savedPet = petRepository.save(newPet);
        log.info("Owner {} registered new pet {} (ID: {}) with image path '{}'", owner.getUsername(), savedPet.getName(), savedPet.getId(), savedPet.getImage());
        return petMapper.toProfileDto(savedPet);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void associatePetToClinicForActivation(Long petId, Long clinicId, Long ownerId) {
        Pet pet = findPetByIdAndOwnerOrFail(petId, ownerId);
        ensurePetIsInStatus(pet, "associate for activation");
        if (pet.getPendingActivationClinic() != null) {
            throw new IllegalStateException("Pet ID " + petId + " is already pending activation at clinic " + pet.getPendingActivationClinic().getId());
        }

        Clinic targetClinic = entityFinderHelper.findClinicOrFail(clinicId);

        pet.setPendingActivationClinic(targetClinic);
        petRepository.save(pet);
        log.info("Owner {} associated PENDING pet {} with clinic {} for activation.", ownerId, petId, clinicId);

        try {
            PetActivationRequestedEvent event = new PetActivationRequestedEvent(
                    petId,
                    ownerId,
                    clinicId,
                    LocalDateTime.now()
            );
            petEventPublisher.publishPetActivationRequested(event);
        } catch (Exception e) {
            log.error("Failed to publish PetActivationRequestedEvent for petId {} after association.", petId, e);
        }

        log.info("Owner {} associated PENDING pet {} with clinic {}. Event published (attempted).", ownerId, petId, clinicId);
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
        ensurePetIsInStatus(petToActivate, "activate");
        if (petToActivate.getPendingActivationClinic() == null || !petToActivate.getPendingActivationClinic().getId().equals(staffClinic.getId())) {
            throw new AccessDeniedException("Staff " + staffId + " from clinic " + staffClinic.getId() +
                    " is not authorized to activate pet " + petId + " (not pending at this clinic)");
        }

        validateMicrochipUniqueness(activationDto.microchip(), petId);

        Specie originalSpecie = petToActivate.getBreed().getSpecie();
        Breed resolvedBreed = resolveBreed(activationDto.breedId(), originalSpecie);
        updatePetEntityFromActivationDto(petToActivate, activationDto, resolvedBreed);

        petToActivate.setStatus(PetStatus.ACTIVE);
        petToActivate.setPendingActivationClinic(null);
        Vet assignedVet = assignVetOnActivation(activatingStaff);
        petToActivate.addVet(assignedVet);

        Pet activatedPet = petRepository.save(petToActivate);

        try {
            PetActivatedEvent event = new PetActivatedEvent(
                    petId,
                    activatedPet.getOwner().getId(),
                    staffId,
                    LocalDateTime.now()
            );
            petEventPublisher.publishPetActivated(event);
        } catch (Exception e) {
            log.error("Failed to publish PetActivatedEvent for petId {} after activation.", petId, e);
        }

        log.info("Vet {} activated Pet {}. Assigned Vet: {}", staffId, petId, assignedVet.getId());
        return petMapper.toProfileDto(activatedPet);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public PetProfileDto deactivatePet(Long petId, Long ownerId) {
        Pet petToDeactivate = findPetByIdAndOwnerOrFail(petId, ownerId);

        if (petToDeactivate.getStatus() == PetStatus.INACTIVE) {
            log.warn("Pet {} is already INACTIVE. No action taken.", petId);
            return petMapper.toProfileDto(petToDeactivate);
        }

        petToDeactivate.setStatus(PetStatus.INACTIVE);

        if (!petToDeactivate.getAssociatedVets().isEmpty()) {
            log.info("Deactivating Pet {}: Clearing all ({}) associated veterinarians.", petId, petToDeactivate.getAssociatedVets().size());
            petToDeactivate.getAssociatedVets().clear();
        }

        Pet deactivatedPet = petRepository.save(petToDeactivate);
        log.info("Owner {} deactivated Pet {}. Status set to INACTIVE and vets cleared.", ownerId, petId);
        return petMapper.toProfileDto(deactivatedPet);
    }

    /**
     * {@inheritDoc}
     * Owner can update all non-status/association fields. Auditing tracks the change.
     */
    @Override
    @Transactional
    public PetProfileDto updatePetByOwner(Long petId, PetOwnerUpdateDto updateDto, Long ownerId,  @Nullable MultipartFile imageFile ) {
        Pet petToUpdate = findPetByIdAndOwnerOrFail(petId, ownerId);
        String oldImagePath = petToUpdate.getImage();
        boolean imageChanged = false;

        if (imageFile != null && !imageFile.isEmpty()) {
            log.debug("New image file provided for pet update (Pet ID: {})", petId);
            try {
                String newImagePath = imageService.storeImage(imageFile, "pets/avatars");
                log.info("New image stored for pet {}. New path: {}", petId, newImagePath);
                petToUpdate.setImage(newImagePath);
                imageChanged = true;
                if (!Objects.equals(oldImagePath, newImagePath)) {
                    imageService.deleteImage(oldImagePath);
                }
            } catch (IOException | IllegalArgumentException e) {
                log.error("Failed to store updated image for pet {}. Update failed. Error: {}", petId, e.getMessage());
                throw new RuntimeException("Failed to process updated image: " + e.getMessage(), e);
            }
        }

        Breed resolvedBreed = petToUpdate.getBreed();
        if (updateDto.breedId() != null && !Objects.equals(updateDto.breedId(), petToUpdate.getBreed().getId())) {
            resolvedBreed = resolveBreed(updateDto.breedId(), petToUpdate.getBreed().getSpecie());
        }
        validateMicrochipUpdate(updateDto.microchip(), petToUpdate);

        boolean otherFieldsChanged = petMapper.updateFromOwnerDto(updateDto, petToUpdate, resolvedBreed);
        boolean travelDatesChanged = false;
        if (updateDto.newEuEntryDate() != null) {
            LocalDate newEntry = updateDto.newEuEntryDate();
            if (petToUpdate.getLastEuExitDate() != null && !newEntry.isAfter(petToUpdate.getLastEuExitDate())) {
                throw new IllegalArgumentException("New EU entry date must be after the last EU exit date.");
            }
            if (!newEntry.equals(petToUpdate.getLastEuEntryDate())) {
                petToUpdate.setLastEuEntryDate(newEntry);
                travelDatesChanged = true;
                log.info("Pet {} EU entry date updated to: {}", petId, newEntry);
            }
        }

        if (updateDto.newEuExitDate() != null) {
            LocalDate newExit = updateDto.newEuExitDate();
            if (petToUpdate.getLastEuEntryDate() == null) {
                throw new IllegalArgumentException("Cannot set EU exit date without an EU entry date.");
            }
            if (!newExit.isAfter(petToUpdate.getLastEuEntryDate())) {
                throw new IllegalArgumentException("EU exit date must be after the current EU entry date.");
            }
            if (!newExit.equals(petToUpdate.getLastEuExitDate())) {
                petToUpdate.setLastEuExitDate(newExit);
                travelDatesChanged = true;
                log.info("Pet {} EU exit date updated to: {}", petId, newExit);
            }
        }

        Pet updatedPet = petToUpdate;
        if (imageChanged || otherFieldsChanged) {
            log.info("Changes detected (image: {}, other: {}), saving Pet {}", imageChanged, otherFieldsChanged, petId);
            updatedPet = petRepository.save(petToUpdate);
            log.info("Owner {} successfully updated Pet {}", ownerId, petId);
        } else {
            log.info("No effective changes detected for Pet {}, update by owner {} skipped.", petId, ownerId);
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

        // Resolve Breed if provided
        Breed resolvedBreed = petToUpdate.getBreed();
        if (updateDto.breedId() != null && !Objects.equals(updateDto.breedId(), petToUpdate.getBreed().getId())) {
            resolvedBreed = resolveBreed(updateDto.breedId(), petToUpdate.getBreed().getSpecie());
        }

        // Validate Microchip if provided
        validateMicrochipUpdate(updateDto.microchip(), petToUpdate);

        // Apply DTO updates using PetMapper
        boolean changed = petMapper.updateFromClinicDto(updateDto, petToUpdate, resolvedBreed);

        // Save if needed and return mapped DTO
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
    public Page<PetProfileDto> findPetsByOwner(Long ownerId, @Nullable List<PetStatus> statuses, Pageable pageable) {
        List<PetStatus> statusesToSearch;
        if (statuses == null || statuses.isEmpty()) {
            statusesToSearch = List.of(PetStatus.ACTIVE, PetStatus.PENDING);
            log.debug("Finding pets for owner {} with default statuses: {}", ownerId, statusesToSearch);
        } else {
            statusesToSearch = statuses;
            log.debug("Finding pets for owner {} with specified statuses: {}", ownerId, statusesToSearch);
        }
        Page<Pet> petPage = petRepository.findByOwnerIdAndStatusIn(ownerId, statusesToSearch, pageable);
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
        authorizationHelper.verifyUserAuthorizationForPet(requesterUserId, pet, "view"); // Verify an owner or associated staff
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
            return Collections.emptyList();
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
            pet.setStatus(PetStatus.PENDING);
            log.warn("Pet {} automatically set to PENDING as last associated Vet was removed.", petId);
        }
        petRepository.save(pet);
    }

    /**
     * {@inheritDoc}
     */
    @Transactional
    public void requestCertificateGeneration(Long petId, Long clinicId, Long ownerId) {
        log.info("Processing certificate generation request for Pet ID {} to Clinic ID {} by Owner ID {}", petId, clinicId, ownerId);
        Pet pet = findPetByIdAndOwnerOrFail(petId, ownerId);
        Clinic targetClinic = entityFinderHelper.findClinicOrFail(clinicId);

        if (pet.getStatus() != PetStatus.ACTIVE) {
            log.warn("Certificate request failed: Pet {} is not ACTIVE (status is {}).", petId, pet.getStatus());
            throw new IllegalStateException("Pet " + pet.getName() + " must be in ACTIVE status to request a certificate.");
        }

        boolean isAssociatedWithTargetClinic = pet.getAssociatedVets().stream()
                .anyMatch(vet -> vet.getClinic() != null && vet.getClinic().getId().equals(clinicId));

        if (!isAssociatedWithTargetClinic) {
            log.warn("Certificate request failed: Pet {} is not associated with any vet from Clinic {}", petId, clinicId);
            throw new AccessDeniedException("To request a certificate, Pet " + petId + " must be associated with a veterinarian from the selected Clinic (ID: " + clinicId + ").");
        }

        if (pet.getPendingCertificateClinic() != null) {
            if (pet.getPendingCertificateClinic().getId().equals(clinicId)) {
                log.warn("Pet {} already has a pending certificate request at clinic {}. New request to clinic {} rejected.",
                        petId, pet.getPendingCertificateClinic().getId(), clinicId);
                // Allow overwriting with the same clinic (will simply republish the event)
            } else {
                log.warn("Pet {} already has a pending certificate request at a different clinic (ID: {}). New request to clinic {} rejected.",
                        petId, pet.getPendingCertificateClinic().getId(), clinicId);
                throw new IllegalStateException("Pet " + pet.getName() + " already has a pending certificate request at clinic '" +
                        pet.getPendingCertificateClinic().getName() + "'. Please wait for it to be processed or contact that clinic.");
            }
        }

        pet.setPendingCertificateClinic(targetClinic);
        log.info("Pet ID {} has been marked with pendingCertificateClinic ID: {}", petId, clinicId);

        try {
            Long representativeVetIdForEvent = pet.getAssociatedVets().stream()
                    .filter(vet -> vet.getClinic() != null && vet.getClinic().getId().equals(clinicId))
                    .map(UserEntity::getId)
                    .findFirst()
                    .orElse(null);

            CertificateRequestedEvent eventToPublish = new CertificateRequestedEvent(
                    petId,
                    ownerId,
                    representativeVetIdForEvent,
                    targetClinic.getId(),
                    LocalDateTime.now()
            );
            petEventPublisher.publishCertificateRequested(eventToPublish);

            log.info("CertificateRequestedEvent published successfully for Pet ID {}, target Clinic ID {}. Representative Vet ID (if any): {}",
                    petId, targetClinic.getId(), representativeVetIdForEvent);
        } catch (Exception e) {
            log.error("Failed to publish CertificateRequestedEvent for petId {}: {}", petId, e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<PetProfileDto> findPetsWithPendingCertRequestsForClinic(Long clinicId, Long requesterStaffId) {
        ClinicStaff staff = entityFinderHelper.findClinicStaffOrFail(requesterStaffId, "view pending certificate requests");
        if (!staff.getClinic().getId().equals(clinicId)) {
            throw new AccessDeniedException("Staff " + requesterStaffId + " is not authorized for clinic " + clinicId);
        }
        List<Pet> petsWithRequests = petRepository.findByPendingCertificateClinicIdAndStatus(clinicId, PetStatus.ACTIVE);
        return petMapper.toProfileDtoList(petsWithRequests);
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
     * Determines an initial image path using a provided path or breed's default.
     */
    private String determineInitialImagePath(@NotNull Breed assignedBreed) {
        StringUtils.hasText(null);

        boolean isGenericBreed = "Mixed/Other".equals(assignedBreed.getName()) || "Standard/Other".equals(assignedBreed.getName());
        if (!isGenericBreed && StringUtils.hasText(assignedBreed.getImageUrl())) {
            log.debug("Using specific breed image path: {}", assignedBreed.getImageUrl());
            return assignedBreed.getImageUrl();
        }

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
     * @param actionDescription Description of the action requiring this status (for an error message).
     * @throws IllegalStateException if the pet's status is not the expected one.
     */
    private void ensurePetIsInStatus(Pet pet, String actionDescription) {
        if (pet.getStatus() != PetStatus.PENDING) {
            throw new IllegalStateException(String.format("Pet %d must be in %s status to %s, but was %s.",
                    pet.getId(), PetStatus.PENDING, actionDescription, pet.getStatus()));
        }
    }

    /**
     * Validates the uniqueness of a microchip number, optionally excluding the pet being updated.
     * Throws MicrochipAlreadyExistsException if a conflict is found.
     *
     * @param microchip      The microchip number to validate (can be null or blank, validation handles this).
     * @param petIdToExclude The ID of the pet being updated (null if creating a new pet).
     * @throws MicrochipAlreadyExistsException if another pet already uses the microchip.
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
     * Updates the Pet entity with data from the validated PetActivationDto.
     * This ensures the pet record reflects the data confirmed during activation.
     */
    private void updatePetEntityFromActivationDto(Pet pet, PetActivationDto dto, Breed resolvedBreed) {
        log.debug("Updating pet {} from activation DTO. . Name: {}. Current image: '{}'", pet.getId(),pet.getName(), pet.getImage());
        pet.setColor(dto.color());
        pet.setGender(dto.gender());
        pet.setBirthDate(dto.birthDate());
        pet.setMicrochip(dto.microchip());
        pet.setBreed(resolvedBreed);
        log.debug("Pet {} details (color, gender, birthDate, microchip, breed) updated from activation DTO. Image remains: '{}'", pet.getId(), pet.getImage());
    }
}