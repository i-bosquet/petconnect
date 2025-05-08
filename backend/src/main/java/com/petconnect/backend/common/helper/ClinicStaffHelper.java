package com.petconnect.backend.common.helper;

import com.petconnect.backend.exception.LicenseNumberAlreadyExistsException;
import com.petconnect.backend.exception.VetPublicKeyAlreadyExistsException;
import com.petconnect.backend.user.application.dto.ClinicStaffCreationDto;
import com.petconnect.backend.user.application.dto.ClinicStaffUpdateDto;
import com.petconnect.backend.user.domain.model.*;
import com.petconnect.backend.user.domain.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * ClinicStaffHelper is a utility class responsible for creating, updating, and validating
 * {@link ClinicStaff} and {@link Vet} entities. It handles core functionalities such as
 * entity creation, field updates, role management, and validation of Vet-specific fields.
 * It is designed to be used in conjunction with a clinic management system that requires
 * dynamic creation and update of staff records while adhering to specific business rules.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ClinicStaffHelper {

    private final ValidateHelper validateHelper;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;

    @Value("${app.default.user.image.path}")
    private String defaultUserImagePathBase;

    /**
     * Builds a new {@link Vet} or {@link ClinicStaff} (for ADMIN role) entity based on the provided DTO and clinic.
     * This method populates common fields and role-specific fields (for Vet), including validation.
     * It does *not* save the entity to the database.
     *
     * @param dto The DTO containing the data for the new staff member.
     * @param targetClinic The {@link Clinic} the new staff member will belong to.
     * @return The constructed (but not persisted) {@link ClinicStaff} or {@link Vet} entity.
     * @throws IllegalArgumentException if VET role-specific fields (license, key) are invalid or missing.
     * @throws LicenseNumberAlreadyExistsException if the VET license number is already in use.
     * @throws VetPublicKeyAlreadyExistsException if the VET public key is already in use.
     */
    public ClinicStaff buildNewStaffEntity(ClinicStaffCreationDto dto, @Nullable String publicKeyPath, Clinic targetClinic) {
        ClinicStaff newStaff;
        String defaultAvatarPath;

        if (dto.role() == RoleEnum.VET) {

            Vet newVet = new Vet();
            setCommonStaffFields(newVet, dto, targetClinic); // Populate common fields
            // Set Vet specific fields
            newVet.setLicenseNumber(dto.licenseNumber());
            if (!StringUtils.hasText(publicKeyPath)) {
                throw new IllegalStateException("Internal Error: PublicKey path is missing for VET creation."); // Defensive check
            }
            newVet.setVetPublicKey(publicKeyPath);
            defaultAvatarPath = getDefaultAvatarPath("vet.png");
            newVet.setAvatar(defaultAvatarPath);
            newStaff = newVet;
        } else { // ADMIN
            ClinicStaff newAdmin = new ClinicStaff();
            setCommonStaffFields(newAdmin, dto, targetClinic); // Populate common fields
            defaultAvatarPath = getDefaultAvatarPath("admin.png"); 
            newAdmin.setAvatar(defaultAvatarPath);
            newStaff = newAdmin;
        }
        return newStaff;
    }


    /**
     * Sets common fields shared by all ClinicStaff (including Vets) during entity creation.
     * Hashes the password, assigns the role, associates the clinic, and sets default statuses.
     *
     * @param staff The {@link ClinicStaff} or {@link Vet} entity being created.
     * @param dto The {@link ClinicStaffCreationDto} containing the source data.
     * @param clinic The {@link Clinic} to associate the staff member with.
     * @throws IllegalStateException if the specified role is not found in the database.
     */
    private void setCommonStaffFields(ClinicStaff staff, ClinicStaffCreationDto dto, Clinic clinic) {
        staff.setUsername(dto.username());
        staff.setEmail(dto.email());
        staff.setPassword(passwordEncoder.encode(dto.password()));
        staff.setName(dto.name());
        staff.setSurname(dto.surname());

        RoleEntity staffRole = roleRepository.findByRoleEnum(dto.role())
                .orElseThrow(() -> new IllegalStateException("Role " + dto.role().name() + " not found in database! Check data.sql."));
        Set<RoleEntity> roles = new HashSet<>();
        roles.add(staffRole);
        staff.setRoles(roles);

        staff.setClinic(clinic);
        staff.setActive(true);
        staff.setEnabled(true);
        staff.setAccountNonExpired(true);
        staff.setAccountNonLocked(true);
        staff.setCredentialsNonExpired(true);
    }

    /**
     * Applies updates from a ClinicStaffUpdateDto to an existing ClinicStaff entity.
     * Updates name, surname, roles, and Vet-specific fields. Delegates specific updates
     * to private helper methods.
     *
     * @param staffToUpdate The ClinicStaff entity to be modified.
     * @param updateDTO The DTO containing the potential updates.
     * @param newPublicKeyPath Optional path to a newly uploaded public key file (null if not changed).
     * @return true if any field on the entity was actually changed, false otherwise.
     */
    public boolean applyStaffUpdates(ClinicStaff staffToUpdate, ClinicStaffUpdateDto updateDTO, @Nullable String newPublicKeyPath) {
        boolean nameChanged = updateNameAndSurname(staffToUpdate, updateDTO);
        boolean rolesChanged = updateRoles(staffToUpdate, updateDTO);
        boolean vetFieldsChanged = updateVetSpecificFields(staffToUpdate, updateDTO, newPublicKeyPath);

        return nameChanged || rolesChanged || vetFieldsChanged;
    }

    /**
     * Updates the name and surname of the staff member if provided in the DTO and different.
     *
     * @param staffToUpdate The entity to update.
     * @param updateDTO The DTO with potential updates.
     * @return true if the name or surname was changed, false otherwise.
     */
    private boolean updateNameAndSurname(ClinicStaff staffToUpdate, ClinicStaffUpdateDto updateDTO) {
        boolean changed = false;
        changed |= Utils.updateStringFieldIfChanged(
                staffToUpdate, updateDTO.name(), staffToUpdate::getName, ClinicStaff::setName, "name"
        );
        changed |= Utils.updateStringFieldIfChanged(
                staffToUpdate, updateDTO.surname(), staffToUpdate::getSurname, ClinicStaff::setSurname, "surname"
        );
        return changed;
    }

    /**
     * Updates the roles assigned to the staff member if a non-null, non-empty set of roles
     * is provided in the DTO, and it differs from the current roles.
     *
     * @param staffToUpdate The entity to update.
     * @param updateDTO The DTO with potential updates.
     * @return true if the roles were changed, false otherwise.
     */
    private boolean updateRoles(ClinicStaff staffToUpdate, ClinicStaffUpdateDto updateDTO) {
        if (updateDTO.roles() == null || updateDTO.roles().isEmpty()) {
            return false;
        }

        Set<RoleEntity> newRoleEntities = updateDTO.roles().stream()
                .map(roleEnum -> roleRepository.findByRoleEnum(roleEnum)
                        .orElseThrow(() -> new IllegalArgumentException("Invalid role specified in update: " + roleEnum)))
                .collect(Collectors.toSet());

        if (!Objects.equals(newRoleEntities, staffToUpdate.getRoles())) {
            log.info("Updating roles for staff ID {}: Old={}, New={}", staffToUpdate.getId(),
                    staffToUpdate.getRoles().stream().map(RoleEntity::getRoleEnum).collect(Collectors.toSet()),
                    updateDTO.roles());
            staffToUpdate.setRoles(newRoleEntities);
            return true;
        }
        return false;
    }

    /**
     * Updates Vet-specific fields (license number, public key path) if the staff member
     * is a Vet and has the VET role assigned after potential role updates.
     * Handles consistency checks.
     *
     * @param staffToUpdate The entity to update.
     * @param updateDTO The DTO with potential updates.
     * @param newPublicKeyPath Optional path to a newly uploaded public key file.
     * @return true if any Vet-specific field was changed, false otherwise.
     */
    private boolean updateVetSpecificFields(ClinicStaff staffToUpdate, ClinicStaffUpdateDto updateDTO, @Nullable String newPublicKeyPath) {
        boolean changed = false;
        Set<RoleEnum> currentOrUpdatedRoles = staffToUpdate.getRoles().stream()
                .map(RoleEntity::getRoleEnum)
                .collect(Collectors.toSet());
        boolean shouldBeVet = currentOrUpdatedRoles.contains(RoleEnum.VET);

        if (staffToUpdate instanceof Vet vetToUpdate) {
            if (shouldBeVet) {
                // Update License if changed
                if (StringUtils.hasText(updateDTO.licenseNumber()) && !Objects.equals(updateDTO.licenseNumber(), vetToUpdate.getLicenseNumber())) {
                    validateHelper.validateVetLicenseUpdate(updateDTO.licenseNumber(), vetToUpdate.getId());
                    vetToUpdate.setLicenseNumber(updateDTO.licenseNumber());
                    changed = true;
                }
                // Update Public Key Path if a new file was provided and a path is different
                if (StringUtils.hasText(newPublicKeyPath) && !newPublicKeyPath.equals(vetToUpdate.getVetPublicKey())) {
                    log.info("Updating vetPublicKey path for Vet ID {} to {}", vetToUpdate.getId(), newPublicKeyPath);
                    vetToUpdate.setVetPublicKey(newPublicKeyPath);
                    changed = true;
                }
            } else {
                log.warn("Staff ID {} is instance of Vet but no longer has VET role assigned. Vet-specific fields not updated/cleared.", staffToUpdate.getId());
            }
        } else if (shouldBeVet) {
            // This user *should* be a Vet based on roles but isn't the right entity type.
            log.error("Data inconsistency: Staff ID {} has VET role but is not an instance of Vet.", staffToUpdate.getId());
            throw new IllegalStateException("User " + staffToUpdate.getId() + " has VET role but incorrect entity type.");
        }
        return changed;
    }

    private String getDefaultAvatarPath(String filename) {
        String basePath = defaultUserImagePathBase.endsWith("/") ? defaultUserImagePathBase : defaultUserImagePathBase + '/';
        return basePath + filename;
    }
}