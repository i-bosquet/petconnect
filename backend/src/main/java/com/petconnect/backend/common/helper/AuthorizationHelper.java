package com.petconnect.backend.common.helper;

import com.petconnect.backend.exception.EntityNotFoundException;
import com.petconnect.backend.exception.UsernameAlreadyExistsException;
import com.petconnect.backend.pet.domain.model.Pet;
import com.petconnect.backend.pet.domain.model.PetStatus;
import com.petconnect.backend.user.domain.model.*;
import com.petconnect.backend.user.domain.repository.ClinicRepository;
import com.petconnect.backend.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Objects;

/**
 * Helper component containing common authorization logic reused across different services.
 * Provides methods to verify user permissions for specific actions or entities.
 *
 * @author ibosquet
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuthorizationHelper {
    private final EntityFinderHelper entityFinderHelper;
    private final UserRepository userRepository;
    private final ClinicRepository clinicRepository;


    /**
     * Verifies if a user (Owner or ClinicStaff) is authorized to perform an action on a Pet.
     * Ensures transactional context for potential lazy loading checks.
     * Delegates specific checks to private helper methods.
     *
     * @param requesterUserId   The ID of the user performing the action.
     * @param pet               The Pet entity being accessed (must not be null, assumed loaded).
     * @param actionDescription Description of the action (e.g., "view", "create record for").
     * @throws IllegalArgumentException if pet is null.
     * @throws EntityNotFoundException if the requester user is not found.
     * @throws AccessDeniedException if the user is not authorized based on ownership or staff association rules.
     * @throws RuntimeException      if a lazy loading issue occurs unexpectedly.
     */
    @Transactional(readOnly = true)
    public void verifyUserAuthorizationForPet(Long requesterUserId, Pet pet, String actionDescription) {
        if (pet == null) {
            log.error("Authorization check cannot be performed: Pet entity is null.");
            throw new IllegalArgumentException("Pet entity cannot be null for authorization check.");
        }
        UserEntity requesterUser = entityFinderHelper.findUserOrFail(requesterUserId);

        // Is the requester the owner?
        if (isOwner(requesterUser, pet)) {
            log.debug("Authorization granted for action '{}': User {} is the owner of pet {}", actionDescription, requesterUserId, pet.getId());
            return;
        }

        // Is the requester authorized staff?
        if (isAuthorizedStaff(requesterUser, pet)) {
            log.debug("Authorization granted for action '{}': User {} is authorized staff for pet {}", actionDescription, requesterUserId, pet.getId());
            return;
        }

        // If none of the checks passed:
        log.warn("Authorization DENIED for User ID: {} attempting to {} Pet ID: {}", requesterUserId, actionDescription, pet.getId());
        throw new AccessDeniedException(String.format("User (ID: %d) is not authorized to %s pet (ID: %d).",
                requesterUserId, actionDescription, pet.getId()));
    }

    /**
     * Checks if the given user is the owner of the pet.
     *
     * @param user The UserEntity requesting access.
     * @param pet  The Pet entity being accessed.
     * @return true if the user is the owner, false otherwise.
     */
    private boolean isOwner(UserEntity user, Pet pet) {
        return user instanceof Owner owner && pet.getOwner() != null && Objects.equals(owner.getId(), pet.getOwner().getId());
    }

    /**
     * Checks if the given user is ClinicStaff authorized to access the pet, either because
     * the pet is PENDING at their clinic or ACTIVE and associated with a Vet from their clinic.
     * Requires associatedVets to be accessible (expects caller to be @Transactional).
     *
     * @param user The UserEntity requesting access.
     * @param pet  The Pet entity being accessed.
     * @return true if the user is authorized staff, false otherwise.
     * @throws AccessDeniedException if the user is staff but has no associated clinic (data inconsistency).
     * @throws RuntimeException      if a lazy loading issue occurs unexpectedly.
     */
    private boolean isAuthorizedStaff(UserEntity user, Pet pet) {
        if (!(user instanceof ClinicStaff staff)) {
            return false;
        }

        Clinic staffClinic = staff.getClinic();
        if (staffClinic == null) {
            log.error("Data inconsistency: Staff user {} has no clinic.", user.getId());
            throw new AccessDeniedException("Staff user " + user.getId() + " has no clinic assignment.");
        }
        Long staffClinicId = staffClinic.getId();

        // Is Pet PENDING at staff's clinic?
        if (pet.getStatus() == PetStatus.PENDING && Objects.equals(staffClinic, pet.getPendingActivationClinic())) {
            return true;
        }

        // Pet ACTIVE and associated with ANY vet from the staff's clinic?
        if (pet.getStatus() == PetStatus.ACTIVE) {
            try {
                log.trace("Checking association for Pet ID {} with Clinic ID {}. Accessing associatedVets...", pet.getId(), staffClinicId);
                boolean associated = pet.getAssociatedVets().stream()
                        .anyMatch(vet -> vet.getClinic() != null && staffClinicId.equals(vet.getClinic().getId()));
                log.trace("Association check completed for Pet ID {}. Result: {}", pet.getId(), associated);
                return associated;
            } catch (org.hibernate.LazyInitializationException e) {
                log.error("LazyInitializationException checking association for pet {} within @Transactional method. Investigate context/proxy.", pet.getId(), e);
                throw new RuntimeException("Failed to check pet-clinic association due to unexpected lazy loading issue.", e);
            }
        }

        return false;
    }


    /**
     * Validates if a potential new username can be used for updating an existing user.
     * Checks if the new username is provided, different from the current one,
     * and if it already exists in the database for another user.
     * Requires a read-only transaction to query the repository.
     *
     * @param newUsername The potential new username from the update DTO (can be null or blank).
     * @param existingUser The UserEntity being updated.
     * @throws UsernameAlreadyExistsException if the new username is provided, different, and already taken.
     */
    @Transactional(readOnly = true)
    public void validateUsernameUpdate(String newUsername, UserEntity existingUser) {
        if (StringUtils.hasText(newUsername) && !existingUser.getUsername().equals(newUsername) && userRepository.existsByUsername(newUsername))
            throw new UsernameAlreadyExistsException(newUsername);
    }

    /**
     * Verifies if the Admin user performing an action belongs to the same clinic as the target staff member.
     * Uses {@link EntityFinderHelper} to locate the involved Admin user.
     *
     * @param adminId The ID of the Admin user performing the action.
     * @param targetStaff The ClinicStaff entity being acted upon (assumed loaded).
     * @param actionDescription Description of the action. Used in logs/exceptions.
     * @throws EntityNotFoundException if the admin user (specified by adminId) is not found.
     * @throws AccessDeniedException if the found user is not an Admin, or if the Admin and targetStaff belong to different clinics.
     * @throws IllegalStateException if the targetStaff has no associated clinic (data inconsistency).
     */
    public void verifyAdminActionOnStaff(Long adminId, ClinicStaff targetStaff, String actionDescription) {
        ClinicStaff adminStaff = entityFinderHelper.findAdminStaffOrFail(adminId, actionDescription + " staff " + targetStaff.getId());
        Clinic adminClinic = adminStaff.getClinic();
        Clinic targetClinic = targetStaff.getClinic();

        if (targetClinic == null) {
            log.error("Data inconsistency: Target staff {} has no associated clinic.", targetStaff.getId());
            throw new IllegalStateException("Target staff " + targetStaff.getId() + " is not associated with any clinic.");
        }

        if (adminClinic == null) {
            log.error("Data inconsistency: Admin staff {} has no associated clinic.", adminId);
            throw new IllegalStateException("Admin user " + adminId + " performing action [" + actionDescription + "] is not associated with any clinic.");
        }

        if (!adminClinic.getId().equals(targetClinic.getId())) {
            throw new AccessDeniedException(String.format("Admin (ID: %d, Clinic: %d) cannot %s staff (ID: %d, Clinic: %d) from a different clinic.",
                    adminId, adminClinic.getId(), actionDescription,
                    targetStaff.getId(), targetClinic.getId()));
        }
    }

    /**
     * Verifies if the requesting user (must be Vet or Admin) belongs to the target clinic
     * they are trying to access information for. Uses {@link EntityFinderHelper}.
     *
     * @param requesterUserId The ID of the user making the request.
     * @param targetClinicId The ID of the clinic being accessed.
     * @param actionDescription Description of the action (e.g., "view staff for"). Used in logs/exceptions.
     * @throws EntityNotFoundException if the requester user or target clinic is not found.
     * @throws AccessDeniedException if the requester is not ClinicStaff, not a Vet/Admin, or not associated with the target clinic.
     */
    public void verifyClinicStaffAccess(Long requesterUserId, Long targetClinicId, String actionDescription) {
        UserEntity requesterUser = entityFinderHelper.findUserOrFail(requesterUserId);

        // check if the target clinic actually exists
        if (!clinicRepository.existsById(targetClinicId)) {
            throw new EntityNotFoundException("Target clinic not found with id: " + targetClinicId);
        }

        if (!(requesterUser instanceof ClinicStaff requesterStaff)) {
            throw new AccessDeniedException("User " + requesterUserId + " is not Clinic Staff and cannot " + actionDescription + " clinic " + targetClinicId);
        }

        boolean isAuthorizedRole = requesterStaff.getRoles().stream()
                .anyMatch(role -> role.getRoleEnum() == RoleEnum.ADMIN || role.getRoleEnum() == RoleEnum.VET);
        if (!isAuthorizedRole) {
            throw new AccessDeniedException("User " + requesterUserId + " role is not authorized to " + actionDescription + " clinic " + targetClinicId);
        }

        Clinic requesterClinic = requesterStaff.getClinic();
        if (requesterClinic == null) {
            log.error("Data inconsistency: Requester staff {} has no associated clinic.", requesterUserId);
            throw new IllegalStateException("Requesting user " + requesterUserId + " is not associated with any clinic.");
        }

        if (!requesterClinic.getId().equals(targetClinicId)) {
            throw new AccessDeniedException(String.format("User (ID: %d, Clinic: %d) cannot %s clinic %d.",
                    requesterUserId, requesterClinic.getId(), actionDescription, targetClinicId));
        }
    }
}
