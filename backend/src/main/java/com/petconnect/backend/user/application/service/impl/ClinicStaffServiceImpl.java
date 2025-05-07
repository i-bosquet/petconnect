package com.petconnect.backend.user.application.service.impl;

import com.petconnect.backend.common.helper.AuthorizationHelper;
import com.petconnect.backend.common.helper.EntityFinderHelper;
import com.petconnect.backend.common.helper.ValidateHelper;
import com.petconnect.backend.common.service.KeyStorageService;
import org.springframework.lang.Nullable;
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

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public ClinicStaffProfileDto createClinicStaff(ClinicStaffCreationDto creationDTO, @Nullable MultipartFile publicKeyFile, Long creatingAdminId) {
        // Verify the user performing the action is a valid Admin and get their clinic
        ClinicStaff creatingAdminStaff = entityFinderHelper.findAdminStaffOrFail(creatingAdminId, "create clinic staff");
        Clinic targetClinic = creatingAdminStaff.getClinic(); // New staff will belong to this admin's clinic

        // Validate Role and Uniqueness
        validateHelper.validateStaffRole(creationDTO.role());
        validateHelper.validateNewStaffUniqueness(creationDTO.email(), creationDTO.username());

        // --- Logic to save the public key BEFORE creating the Vet entity---
        String publicKeyPath = null;
        if (creationDTO.role() == RoleEnum.VET) {
            if (publicKeyFile == null || publicKeyFile.isEmpty()) {
                throw new IllegalArgumentException("Public Key file (.pem) is required for VET role.");
            }
            // Validate other VET (license) fields
            validateHelper.validateVetLicenseNumber(creationDTO.licenseNumber());

            try {
                // Create a predictable filename, e.g., vet_<username>_pub
                String desiredFilenameBase = "vet_" + creationDTO.username() + "_pub";
                // Save in the "vets" subdirectory
                publicKeyPath = keyStorageService.storePublicKey(publicKeyFile, "vets", desiredFilenameBase);
                log.info("Stored public key for new vet {} at path: {}", creationDTO.username(), publicKeyPath);
            } catch (IOException | IllegalArgumentException e) {
                log.error("Failed to store public key file for vet {}: {}", creationDTO.username(), e.getMessage(), e);
                throw new RuntimeException("Failed to store public key file: " + e.getMessage(), e);
            }
        }

        // Create Entity (Vet or Admin)
        ClinicStaff newStaff = clinicStaffHelper.buildNewStaffEntity(creationDTO, publicKeyPath, targetClinic);

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
    public ClinicStaffProfileDto updateClinicStaff(Long staffId, ClinicStaffUpdateDto updateDTO, Long updatingAdminId) {
        // Find staff to update
        ClinicStaff staffToUpdate = entityFinderHelper.findClinicStaffOrFail(staffId, "update");
        // Verify Admin authorization (same clinic)
        authorizationHelper.verifyAdminActionOnStaff(updatingAdminId, staffToUpdate, "update");

        // Apply updates
        boolean changed = clinicStaffHelper.applyStaffUpdates(staffToUpdate, updateDTO);

        // Save if changed
        ClinicStaff updatedStaff = staffToUpdate;
        if (changed) {
            updatedStaff = clinicStaffRepository.save(staffToUpdate);
            log.info("Admin {} updated staff {}", updatingAdminId, updatedStaff.getUsername());
        } else {
            log.info("No changes detected for staff ID {}, update skipped.", staffId);
        }

        return userMapper.toClinicStaffProfileDto(updatedStaff);
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

        if (!staffToDeactivate.isActive()) {
            throw new IllegalStateException("Staff member with id " + staffId + " is already inactive.");
        }

        if (staffId.equals(deactivatingAdminId)) {
            throw new IllegalArgumentException("Admin cannot deactivate their own account.");
        }
        staffToDeactivate.setActive(false);
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
}