package com.petconnect.backend.user.application.service;

import com.petconnect.backend.exception.EmailAlreadyExistsException;
import com.petconnect.backend.exception.LicenseNumberAlreadyExistsException;
import com.petconnect.backend.exception.UsernameAlreadyExistsException;
import com.petconnect.backend.user.application.dto.ClinicStaffCreationDto;
import com.petconnect.backend.user.application.dto.ClinicStaffProfileDto;
import com.petconnect.backend.user.application.dto.ClinicStaffUpdateDto;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;

/**
 * Service interface for managing Clinic Staff members (Vets and Admins).
 * Handles creation, activation/deactivation, and potentially updates initiated by Admins.
 *
 * @author ibosquet
 */
public interface ClinicStaffService {
    /**
     * Creates a new Clinic Staff member (Vet or Admin) associated with a specific clinic.
     * This operation should typically be performed by an existing ADMIN of that clinic.
     * Handles password hashing, role assignment, and association with the clinic.
     * Validates uniqueness of email, username, and potentially license number for Vets.
     *
     * @param creationDTO DTO containing the details of the staff member to create.
     * @param creatingAdminId The ID of the ADMIN user performing the creation (for authorization checks).
     * @return A DTO representing the profile of the newly created staff member.
     * @throws EmailAlreadyExistsException if email is taken.
     * @throws UsernameAlreadyExistsException if a username is taken.
     * @throws LicenseNumberAlreadyExistsException if the license number is taken (for Vets).
     * @throws EntityNotFoundException if the specified clinicId does not exist.
     * @throws IllegalArgumentException if the provided role in DTO is not VET or ADMIN,
     *         or if required Vet fields are missing when a role is VET.
     * @throws AccessDeniedException if the creating user is not authorized.
     */
    ClinicStaffProfileDto createClinicStaff(ClinicStaffCreationDto creationDTO, Long creatingAdminId);

    /**
     * Activates a previously deactivated Clinic Staff member account.
     *
     * @param staffId The ID of the staff member to activate.
     * @param activatingAdminId The ID of the ADMIN performing the action.
     * @return The profile DTO of the activated staff member.
     * @throws EntityNotFoundException if the staff member is not found.
     * @throws AccessDeniedException if the activating user is not authorized.
     * @throws IllegalStateException if the staff member is already active.
     */
    ClinicStaffProfileDto activateStaff(Long staffId, Long activatingAdminId);

    /**
     * Deactivates a Clinic Staff member account.
     * Deactivated staff can no longer log in, but their records remain.
     *
     * @param staffId The ID of the staff member to deactivate.
     * @param deactivatingAdminId The ID of the ADMIN performing the action.
     * @return The profile DTO of the deactivated staff member.
     * @throws EntityNotFoundException if the staff member is not found.
     * @throws AccessDeniedException if the deactivating user is not authorized.
     * @throws IllegalStateException if the staff member is already inactive or if trying to deactivate oneself.
     */
    ClinicStaffProfileDto deactivateStaff(Long staffId, Long deactivatingAdminId);


    /**
     * Finds all active staff members for a given clinic.
     *
     * @param clinicId The ID of the clinic.
     * @return A list of ClinicStaffProfileDto for active staff.
     */
    List<ClinicStaffProfileDto> findActiveStaffByClinic(Long clinicId, Long requesterUserId);

    /**
     * Finds all staff members (active and inactive) for a given clinic.
     * Useful for administrative views.
     *
     * @param clinicId The ID of the clinic.
     * @return A list of ClinicStaffProfileDto for all staff.
     */
    List<ClinicStaffProfileDto> findAllStaffByClinic(Long clinicId, Long requesterUserId);

    /**
     * Updates the details of an existing Clinic Staff member.
     * Allows updating name, surname, and Vet-specific fields if applicable.
     * Requires authorization check (Admin of the same clinic).
     *
     * @param staffId The ID of the staff member to update.
     * @param updateDTO DTO containing the fields to update.
     * @param updatingAdminId The ID of the Admin performing the update.
     * @return The updated ClinicStaffProfileDto.
     * @throws EntityNotFoundException if staff member not found.
     * @throws AccessDeniedException if admin isn't authorized.
     * @throws LicenseNumberAlreadyExistsException if updating the license number to an existing one.
     */
    ClinicStaffProfileDto updateClinicStaff(Long staffId, ClinicStaffUpdateDto updateDTO, Long updatingAdminId);

}
