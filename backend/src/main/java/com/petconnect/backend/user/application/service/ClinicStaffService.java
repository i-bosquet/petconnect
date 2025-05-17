package com.petconnect.backend.user.application.service;

import com.petconnect.backend.user.application.dto.ClinicStaffCreationDto;
import com.petconnect.backend.user.application.dto.ClinicStaffProfileDto;
import com.petconnect.backend.user.application.dto.ClinicStaffUpdateDto;
import jakarta.persistence.EntityExistsException;
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
     * Creates a new clinic staff member (VET or ADMIN) with the specified details.
     * This method is executed by an ADMIN and optionally uploads the public and
     * private key files for secure operations, typically required for VET roles.
     *
     * @param creationDTO A DTO containing the details of the staff member to be created,
     *                    such as username, email, name, role (VET or ADMIN), etc.
     * @param publicKeyFile The public key file for the staff member may be null if not applicable.
     * @param privateKeyFileEncrypted The encrypted private key file for the staff member may be null if not applicable.
     * @param creatingAdminId The unique ID of the ADMIN creating the staff member. Used for authorization and auditing.
     * @return A ClinicStaffProfileDto representing the newly created staff member's profile data.
     * @throws EntityExistsException if a user with the provided username or email already exists.
     * @throws AccessDeniedException if the creating admin lacks the necessary permissions.
     * @throws IllegalArgumentException if the provided data is invalid or missing required fields.
     */
    ClinicStaffProfileDto createClinicStaff(ClinicStaffCreationDto creationDTO,
                                            @Nullable MultipartFile publicKeyFile,
                                            @Nullable MultipartFile privateKeyFileEncrypted,
                                            Long creatingAdminId);

    /**
     * Activates a Clinic Staff member account.
     * Activated staff can log in and perform their designated roles.
     *
     * @param staffId The ID of the staff member to activate.
     * @param updatingAdminId The ID of the ADMIN performing the activation.
     * @return A ClinicStaffProfileDto representing the activated staff member's profile details.
     * @throws EntityNotFoundException if the specified staff member does not exist.
     * @throws AccessDeniedException if the updating user lacks necessary permissions.
     * @throws IllegalStateException if the staff member is already active.
     */
    ClinicStaffProfileDto activateStaff(Long staffId,
                                        Long updatingAdminId);

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
    ClinicStaffProfileDto updateClinicStaff(Long staffId, ClinicStaffUpdateDto updateDTO, @Nullable MultipartFile publicKeyFile, @Nullable MultipartFile privateKeyFile, Long updatingAdminId);

}