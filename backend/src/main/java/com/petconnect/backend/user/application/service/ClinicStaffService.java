package com.petconnect.backend.user.application.service;

import com.petconnect.backend.exception.EmailAlreadyExistsException;
import com.petconnect.backend.exception.LicenseNumberAlreadyExistsException;
import com.petconnect.backend.exception.UsernameAlreadyExistsException;
import com.petconnect.backend.user.application.dto.ClinicStaffCreationDto;
import com.petconnect.backend.user.application.dto.ClinicStaffProfileDto;
import com.petconnect.backend.user.application.dto.ClinicStaffUpdateDto;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.lang.Nullable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.multipart.MultipartFile;

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
     * @param publicKeyFile The public key file for the staff member's digital signature capability can be null for an ADMIN role.
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
    ClinicStaffProfileDto createClinicStaff(ClinicStaffCreationDto creationDTO, @Nullable MultipartFile publicKeyFile, Long creatingAdminId);

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
     * This operation can only be performed by an ADMIN user with proper authorization.
     * The update includes editable fields in the ClinicStaffUpdateDto and optionally uploads a public key file
     * if the staff role is VET.
     *
     * @param staffId The unique ID of the staff member to update.
     * @param updateDTO A DTO containing updated staff details such as name, surname, and license number (if applicable).
     * @param publicKeyFile The public key file for the staff member's digital signature capability. May be null if not applicable.
     * @param updatingAdminId The ID of the ADMIN user performing the update operation.
     * @return A ClinicStaffProfileDto representing the updated staff member's profile details.
     * @throws EntityNotFoundException if the specified staff member does not exist.
     * @throws AccessDeniedException if the updating user lacks authorization.
     * @throws IllegalArgumentException if the provided update data is invalid or missing required fields.
     */
    ClinicStaffProfileDto updateClinicStaff(Long staffId, ClinicStaffUpdateDto updateDTO, @Nullable MultipartFile publicKeyFile, Long updatingAdminId);

}