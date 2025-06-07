package com.petconnect.backend.user.application.service.impl;

import com.petconnect.backend.common.helper.AuthorizationHelper;
import com.petconnect.backend.common.helper.EntityFinderHelper;
import com.petconnect.backend.common.helper.ValidateHelper;
import com.petconnect.backend.common.service.EmailService;
import com.petconnect.backend.common.service.KeyStorageService;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import com.petconnect.backend.user.application.dto.ClinicStaffCreationDto;
import com.petconnect.backend.user.application.dto.ClinicStaffProfileDto;
import com.petconnect.backend.user.application.dto.ClinicStaffUpdateDto;
import com.petconnect.backend.user.application.mapper.UserMapper;
import com.petconnect.backend.user.application.service.ClinicStaffService;
import com.petconnect.backend.common.helper.ClinicStaffHelper;
import com.petconnect.backend.user.domain.model.*;
import com.petconnect.backend.user.domain.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * Implementation of the {@link ClinicStaffService} interface.
 * Handles creation, updating, activation/deactivation, and retrieval of Clinic Staff (Vets and Admins).
 * Includes authorization checks to ensure operations are performed by authorized users
 * within the appropriate clinic context.
 *
 * @author ibosquet
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ClinicStaffServiceImpl implements ClinicStaffService {

    private final ClinicStaffRepository clinicStaffRepository;
    private final UserMapper userMapper;
    private final EntityFinderHelper entityFinderHelper;
    private final AuthorizationHelper authorizationHelper;
    private final ValidateHelper validateHelper;
    private final ClinicStaffHelper clinicStaffHelper;
    private final KeyStorageService keyStorageService;
    private final EmailService emailService;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public ClinicStaffProfileDto createClinicStaff(ClinicStaffCreationDto creationDTO,
                                                   @Nullable MultipartFile publicKeyFile,
                                                   @Nullable MultipartFile privateKeyFileEncrypted,
                                                   Long creatingAdminId) {
        // Verify the user performing the action is a valid Admin and get their clinic
        ClinicStaff creatingAdminStaff = entityFinderHelper.findAdminStaffOrFail(creatingAdminId, "create clinic staff");
        Clinic targetClinic = creatingAdminStaff.getClinic(); // New staff will belong to this admin's clinic

        // Validate Role and Uniqueness
        validateHelper.validateStaffRole(creationDTO.role());
        validateHelper.validateNewStaffUniqueness(creationDTO.email(), creationDTO.username());

        String savedPublicKeyPath = null;
        String savedPrivateKeyPath = null;

        // --- Logic to save the public key BEFORE creating the Vet entity---
        if (creationDTO.role() == RoleEnum.VET) {

            validateHelper.validateVetLicenseNumber(creationDTO.licenseNumber());

            if (publicKeyFile == null || publicKeyFile.isEmpty()) {
                throw new IllegalArgumentException("Public Key file (.pem) is required for VET role.");
            }

            try {
                // Create a predictable filename, e.g., vet_<username>_pub
                String desiredFilenameBase = "vet_" + creationDTO.username() + "_pub";
                // Save in the "vets" subdirectory
                savedPublicKeyPath  = keyStorageService.storePublicKey(publicKeyFile, "public_keys/vets", desiredFilenameBase);
                log.info("Stored public key for new vet {} at path: {}", creationDTO.username(), savedPublicKeyPath );
            } catch (IOException | IllegalArgumentException e) {
                log.error("Failed to store public key file for vet {}: {}", creationDTO.username(), e.getMessage(), e);
                throw new RuntimeException("Failed to store public key file: " + e.getMessage(), e);
            }

            if (privateKeyFileEncrypted == null || privateKeyFileEncrypted.isEmpty()) {
                throw new IllegalArgumentException("Encrypted Private Key file is required for VET role.");
            }
            try {
                // Create a predictable filename, e.g., vet_<username>_priv
                String desiredPrivKeyFilenameBase = "vet_" + creationDTO.username() + "_priv";
                // Save in the "vets" subdirectory
                savedPrivateKeyPath = keyStorageService.storeEncryptedPrivateKey(privateKeyFileEncrypted, "private_encrypted_keys/vets", desiredPrivKeyFilenameBase);
                log.info("Stored encrypted private key for new vet {} at path: {}", creationDTO.username(), savedPrivateKeyPath);
            } catch (IOException | IllegalArgumentException e) {
                log.error("Failed to store encrypted private key file for vet {}: {}", creationDTO.username(), e.getMessage(), e);
                if (savedPublicKeyPath != null) keyStorageService.deleteKey(savedPublicKeyPath);
                throw new RuntimeException("Failed to store encrypted private key file: " + e.getMessage(), e);
            }
        }

        // Create Entity (Vet or Admin)
        ClinicStaff newStaff = clinicStaffHelper.buildNewStaffEntity(creationDTO, savedPublicKeyPath, savedPrivateKeyPath, targetClinic);

        // Save and Map Response
        ClinicStaff savedStaff = clinicStaffRepository.save(newStaff);
        log.info("Admin {} created new staff {} ({}) for clinic {}",
                creatingAdminStaff.getUsername(), savedStaff.getUsername(), savedStaff.getClass().getSimpleName(), targetClinic.getName());
        return userMapper.toClinicStaffProfileDto(savedStaff);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public ClinicStaffProfileDto updateClinicStaff(Long staffId,
                                                   ClinicStaffUpdateDto updateDTO,
                                                   @Nullable MultipartFile newPublicKeyFile,
                                                   @Nullable MultipartFile newPrivateKeyFile,
                                                   Long updatingAdminId) {
        ClinicStaff staffToUpdate = validateAndAuthorizeUpdate(staffId, updatingAdminId);

        KeyUpdateResult keyUpdateResult = handleKeyUpdates(staffToUpdate, updateDTO, newPublicKeyFile, newPrivateKeyFile);

        boolean otherFieldsChanged = clinicStaffHelper.applyStaffUpdates(staffToUpdate, updateDTO,
                keyUpdateResult.finalPublicKeyPath(), keyUpdateResult.finalPrivateKeyPath());

        return processStaffUpdates(staffToUpdate, keyUpdateResult, otherFieldsChanged, updatingAdminId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public ClinicStaffProfileDto activateStaff(Long staffId, Long activatingAdminId) {
        ClinicStaff staffToActivate = entityFinderHelper.findClinicStaffOrFail(staffId, "activate");
        authorizationHelper.verifyAdminActionOnStaff(activatingAdminId, staffToActivate, "activate");

        if (staffToActivate.isActive()) {
            throw new IllegalStateException("Staff member with id " + staffId + " is already active.");
        }
        staffToActivate.setActive(true);
        staffToActivate.setEnabled(true);
        ClinicStaff updatedStaff = clinicStaffRepository.save(staffToActivate);
        log.info("Admin {} activated staff member: {}", activatingAdminId, updatedStaff.getUsername());
        return userMapper.toClinicStaffProfileDto(updatedStaff);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public ClinicStaffProfileDto deactivateStaff(Long staffId, Long deactivatingAdminId) {
        ClinicStaff staffToDeactivate = entityFinderHelper.findClinicStaffOrFail(staffId, "deactivate");
        authorizationHelper.verifyAdminActionOnStaff(deactivatingAdminId, staffToDeactivate, "deactivate");

        if (!staffToDeactivate.isActive() && !staffToDeactivate.isEnabled()) {
            throw new IllegalStateException("Staff member with id " + staffId + " is already inactive.");
        }

        if (staffId.equals(deactivatingAdminId)) {
            throw new IllegalArgumentException("Admin cannot deactivate their own account.");
        }
        staffToDeactivate.setActive(false);
        staffToDeactivate.setEnabled(false);
        ClinicStaff updatedStaff = clinicStaffRepository.save(staffToDeactivate);
        log.info("Admin {} deactivated staff member: {}", deactivatingAdminId, updatedStaff.getUsername());
        return userMapper.toClinicStaffProfileDto(updatedStaff);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public List<ClinicStaffProfileDto> findActiveStaffByClinic(Long clinicId, Long requesterUserId) {
        authorizationHelper.verifyClinicStaffAccess(requesterUserId, clinicId, "view active staff for");
        List<ClinicStaff> staffList = clinicStaffRepository.findByClinicIdAndIsActive(clinicId, true);
        return userMapper.toClinicStaffProfileDtoList(staffList);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public List<ClinicStaffProfileDto> findAllStaffByClinic(Long clinicId, Long requesterUserId) {
        authorizationHelper.verifyClinicStaffAccess(requesterUserId, clinicId, "view all staff for");
        List<ClinicStaff> staffList = clinicStaffRepository.findByClinicId(clinicId);
        return userMapper.toClinicStaffProfileDtoList(staffList);
    }
    
    /**
     * Validates and authorizes an update operation for a clinic staff member.
     * This method checks if the staff member with the given ID is active and,
     * if so, verifies whether the administrator performing the operation is authorized 
     * to make updates to the staff member's record.
     *
     * @param staffId The ID of the staff member to update.
     * @param updatingAdminId The ID of the administrator attempting to perform the update.
     * @return The ClinicStaff entity that is being updated.
     * @throws IllegalStateException If the staff member is inactive and cannot be updated.
     * @throws SecurityException If the administrator is not authorized to perform the update.
     */
    // Private methods
    private ClinicStaff validateAndAuthorizeUpdate(Long staffId, Long updatingAdminId) {
        ClinicStaff staffToUpdate = entityFinderHelper.findClinicStaffOrFail(staffId, "update");
        if (!staffToUpdate.isActive()) {
            throw new IllegalStateException("Cannot update an inactive staff member (ID: " + staffId + "). Please activate the account first.");
        }
        authorizationHelper.verifyAdminActionOnStaff(updatingAdminId, staffToUpdate, "update");
        return staffToUpdate;
    }

    /**
     * Represents the result of a key update process, including the paths for the updated
     * public and private keys, flags indicating whether the keys were changed, and paths to
     * the old key files.
     */
    private record KeyUpdateResult(
            String finalPublicKeyPath,
            String finalPrivateKeyPath,
            boolean publicKeyChanged,
            boolean privateKeyChanged,
            String oldPublicKeyPath,
            String oldPrivateKeyPath
    ) {
    }

    /**
     * Handles the update of key files for a given clinic staff member, particularly if they are assigned
     * the role of a veterinarian. Updates include storing new public and private keys and tracking changes.
     *
     * @param staffToUpdate the clinic staff member whose keys are to be updated
     * @param updateDTO the data transfer object containing the updated details for the clinic staff member
     * @param newPublicKeyFile the multipart file containing the new public key, if provided
     * @param newPrivateKeyFile the multipart file containing the new private key, if provided
     * @return a {@code KeyUpdateResult} instance containing the paths of the updated keys, the status of 
     *         key changes, and the old key paths
     */
    private KeyUpdateResult handleKeyUpdates(ClinicStaff staffToUpdate, ClinicStaffUpdateDto updateDTO,
                                             MultipartFile newPublicKeyFile, MultipartFile newPrivateKeyFile) {
        String oldPublicKeyPath = (staffToUpdate instanceof Vet v) ? v.getVetPublicKey() : null;
        String oldPrivateKeyPath = (staffToUpdate instanceof Vet v) ? v.getVetPrivateKey() : null;

        String finalNewPublicKeyPath = oldPublicKeyPath;
        String finalNewPrivateKeyPath = oldPrivateKeyPath;
        boolean publicKeyChanged = false;
        boolean privateKeyChanged = false;

        boolean isTargetRoleVet = (updateDTO.roles() != null && !updateDTO.roles().isEmpty())
                ? updateDTO.roles().contains(RoleEnum.VET)
                : staffToUpdate.getRoles().stream().anyMatch(r -> r.getRoleEnum() == RoleEnum.VET);

        if (isTargetRoleVet && newPublicKeyFile != null && !newPublicKeyFile.isEmpty()) {
            try {
                String desiredFilenameBase = "vet_" + staffToUpdate.getUsername() + "_pub";
                finalNewPublicKeyPath = keyStorageService.storePublicKey(newPublicKeyFile, "public_keys/vets", desiredFilenameBase);
                publicKeyChanged = !Objects.equals(oldPublicKeyPath, finalNewPublicKeyPath);
            } catch (IOException | IllegalArgumentException e) {
                log.error("Failed to store new public key file for staff {}: {}", staffToUpdate.getUsername(), e.getMessage(), e);
                throw new RuntimeException("Failed to store new public key file: " + e.getMessage(), e);
            }
        }

        if (isTargetRoleVet && newPrivateKeyFile != null && !newPrivateKeyFile.isEmpty()) {
            try {
                String desiredFilenameBase = "vet_" + staffToUpdate.getUsername() + "_priv";
                finalNewPrivateKeyPath = keyStorageService.storeEncryptedPrivateKey(newPrivateKeyFile, "private_encrypted_keys/vets", desiredFilenameBase);
                privateKeyChanged = !Objects.equals(oldPrivateKeyPath, finalNewPrivateKeyPath);
            } catch (IOException | IllegalArgumentException e) {
                log.error("Failed to store new encrypted private key file for staff {}: {}", staffToUpdate.getUsername(), e.getMessage(), e);
                throw new RuntimeException("Failed to store new encrypted private key file: " + e.getMessage(), e);
            }
        }

        return new KeyUpdateResult(finalNewPublicKeyPath, finalNewPrivateKeyPath, publicKeyChanged,
                privateKeyChanged, oldPublicKeyPath, oldPrivateKeyPath);
    }

    /**
     * Processes updates to a clinic staff's profile, including key updates and other field changes.
     * If key changes are detected, old keys are removed, and notifications may be sent for specific staff types.
     * The updated staff entity is saved to the repository, and a corresponding DTO is returned.
     *
     * @param staffToUpdate       The clinic staff entity to be updated.
     * @param keyResult           The result object containing information about key changes (public and private).
     * @param otherFieldsChanged  A flag indicating whether other fields (besides keys) have changed.
     * @param updatingAdminId     The ID of the admin performing the update.
     * @return A ClinicStaffProfileDto representing the updated profile of the clinic staff.
     */
    private ClinicStaffProfileDto processStaffUpdates(ClinicStaff staffToUpdate, KeyUpdateResult keyResult,
                                                      boolean otherFieldsChanged, Long updatingAdminId) {
        ClinicStaff updatedStaffEntity = staffToUpdate;
        if (otherFieldsChanged || keyResult.publicKeyChanged() || keyResult.privateKeyChanged()) {
            if (keyResult.publicKeyChanged() && StringUtils.hasText(keyResult.oldPublicKeyPath())) {
                keyStorageService.deleteKey(keyResult.oldPublicKeyPath());
            }
            if (keyResult.privateKeyChanged() && StringUtils.hasText(keyResult.oldPrivateKeyPath())) {
                keyStorageService.deleteKey(keyResult.oldPrivateKeyPath());
            }
            updatedStaffEntity = clinicStaffRepository.save(staffToUpdate);
            log.info("Admin {} updated staff {}", updatingAdminId, updatedStaffEntity.getUsername());

            if (staffToUpdate instanceof Vet && (keyResult.publicKeyChanged() || keyResult.privateKeyChanged())) {
                emailService.sendVetKeysChangedNotification(updatedStaffEntity.getEmail(), updatedStaffEntity.getName());
                log.info("Vet keys changed for {}. Notification would be sent.", updatedStaffEntity.getUsername());
            }
        } else {
            log.info("No changes detected for staff ID {}, update skipped.", staffToUpdate.getId());
        }
        return userMapper.toClinicStaffProfileDto(updatedStaffEntity);
    }
}