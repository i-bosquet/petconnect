package com.petconnect.backend.common.helper;

import com.petconnect.backend.exception.EntityNotFoundException;
import com.petconnect.backend.pet.domain.model.Breed;
import com.petconnect.backend.pet.domain.model.Pet;
import com.petconnect.backend.pet.domain.repository.BreedRepository;
import com.petconnect.backend.pet.domain.repository.PetRepository;
import com.petconnect.backend.record.domain.model.Record;
import com.petconnect.backend.record.domain.repository.RecordRepository;
import com.petconnect.backend.user.domain.model.*;
import com.petconnect.backend.user.domain.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

/**
 * Helper component responsible for finding common entities by ID
 * and throwing a standard EntityNotFoundException if not found.
 * Also includes helpers for finding specific user subtypes.
 *
 * @author ibosquet
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EntityFinderHelper {
    private final UserRepository userRepository;
    private final PetRepository petRepository;
    private final ClinicRepository clinicRepository;
    private final VetRepository vetRepository;
    private final RecordRepository recordRepository;
    private final BreedRepository breedRepository;

    /**
     * Finds a UserEntity by ID or throws EntityNotFoundException.
     * @param userId The ID of the user.
     * @return The found UserEntity.
     * @throws EntityNotFoundException if not found.
     */
    public UserEntity findUserOrFail(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException(UserEntity.class.getSimpleName(), userId));
    }

    /**
     * Finds an Owner by ID or throws EntityNotFoundException.
     * Ensures the found user is actually an Owner.
     * @param ownerId The ID of the owner.
     * @return The found Owner entity.
     * @throws EntityNotFoundException if user not found or is not an Owner.
     */
    public Owner findOwnerOrFail(Long ownerId) {
        return userRepository.findById(ownerId)
                .filter(Owner.class::isInstance)
                .map(Owner.class::cast)
                .orElseThrow(() -> new EntityNotFoundException(Owner.class.getSimpleName(), ownerId));
    }

    /**
     * Finds a ClinicStaff by ID, checks they belong to a clinic, or throws appropriate exceptions.
     * @param staffId The ID of the staff member.
     * @param actionContext Description of the action being performed (for error messages).
     * @return The found and validated ClinicStaff entity.
     * @throws EntityNotFoundException if user not found.
     * @throws AccessDeniedException if user is not ClinicStaff.
     * @throws IllegalStateException if the staff member has no associated clinic.
     */
    public ClinicStaff findClinicStaffOrFail(Long staffId, String actionContext) {
        UserEntity user = findUserOrFail(staffId);
        if (!(user instanceof ClinicStaff staff)) {
            throw new AccessDeniedException("User " + staffId + " is not Clinic Staff and cannot " + actionContext);
        }
        if (staff.getClinic() == null) {
            log.error("Data inconsistency: Staff {} has no associated clinic.", staffId);
            throw new IllegalStateException("Staff user " + staffId + " is not associated with any clinic.");
        }
        return staff;
    }

    /**
     * Finds a UserEntity by ID, ensures it's a ClinicStaff and specifically an ADMIN,
     * and checks if they are associated with a clinic. Throws appropriate exceptions.
     * @param adminId The ID of the user expected to be an admin.
     * @param actionContext Description of the action being performed (for error messages).
     * @return The found and validated ClinicStaff entity (as an Admin).
     * @throws AccessDeniedException if user is not ClinicStaff or not an ADMIN.
     */
    public ClinicStaff findAdminStaffOrFail(Long adminId, String actionContext) {
        ClinicStaff staff = findClinicStaffOrFail(adminId, actionContext);

        boolean isAdmin = staff.getRoles().stream().anyMatch(r -> r.getRoleEnum() == RoleEnum.ADMIN);
        if (!isAdmin) {
            throw new AccessDeniedException("User " + adminId + " is not an authorized Admin to perform action [" + actionContext + "].");
        }
        return staff;
    }

    /**
     * Finds a Vet by ID or throws EntityNotFoundException.
     * Ensures the found user is actually a Vet.
     * @param vetId The ID of the vet.
     * @return The found Vet entity.
     * @throws EntityNotFoundException if user not found or is not a Vet.
     */
    public Vet findVetOrFail(Long vetId) {
        // Vet repo already returns Vet instances or empty
        return vetRepository.findById(vetId)
                .orElseThrow(() -> new EntityNotFoundException(Vet.class.getSimpleName(), vetId));
    }

    /**
     * Finds a Clinic by ID or throws EntityNotFoundException.
     * @param clinicId The ID of the clinic.
     * @return The found Clinic entity.
     * @throws EntityNotFoundException if not found.
     */
    public Clinic findClinicOrFail(Long clinicId) {
        return clinicRepository.findById(clinicId)
                .orElseThrow(() -> new EntityNotFoundException(Clinic.class.getSimpleName(), clinicId));
    }


    /**
     * Finds a Pet by ID or throws EntityNotFoundException.
     * @param petId The ID of the pet.
     * @return The found Pet entity.
     * @throws EntityNotFoundException if not found.
     */
    public Pet findPetByIdOrFail(Long petId) {
        return petRepository.findById(petId)
                .orElseThrow(() -> new EntityNotFoundException(Pet.class.getSimpleName(), petId));
    }

    /**
     * Finds a Record by ID or throws EntityNotFoundException.
     * @param recordId The ID of the record.
     * @return The found Record entity.
     * @throws EntityNotFoundException if not found.
     */
    public Record findRecordByIdOrFail(Long recordId) {
        return recordRepository.findById(recordId)
                .orElseThrow(() -> new EntityNotFoundException(Record.class.getSimpleName(), recordId));
    }

    /**
     * Finds a Breed by ID or throws EntityNotFoundException.
     * @param breedId The ID of the breed.
     * @return The found Breed entity.
     * @throws EntityNotFoundException if not found.
     */
    public Breed findBreedOrFail(Long breedId) {
        return breedRepository.findById(breedId)
                .orElseThrow(() -> new EntityNotFoundException(Breed.class.getSimpleName(), breedId));
    }

}
