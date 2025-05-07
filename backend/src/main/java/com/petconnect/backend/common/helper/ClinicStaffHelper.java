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
import java.util.Set;

/**
 * Helper component specifically for Clinic Staff related operations like
 * building new staff entities and applying updates, incorporating validation logic.
 *
 * @author ibosquet
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
     * Applies updates from a {@link ClinicStaffUpdateDto} to an existing {@link ClinicStaff} entity.
     * Updates name, surname, and Vet-specific fields (license number, public key) if the entity is a Vet
     * and the new values are provided and different from existing ones. Includes validation for Vet fields.
     *
     * @param staffToUpdate The {@link ClinicStaff} entity to be modified.
     * @param updateDTO The DTO containing the potential updates.
     * @return {@code true} if any field on the entity was actually changed, {@code false} otherwise.
     * @throws IllegalArgumentException if attempting to update Vet fields with invalid data.
     * @throws LicenseNumberAlreadyExistsException if the new license number conflicts with another Vet.
     * @throws VetPublicKeyAlreadyExistsException if the new public key conflicts with another Vet.
     */
    public boolean applyStaffUpdates(ClinicStaff staffToUpdate, ClinicStaffUpdateDto updateDTO) {
        boolean changed = false;
        if (updateDTO.name() != null && !updateDTO.name().isBlank() && !updateDTO.name().equals(staffToUpdate.getName())) {
            staffToUpdate.setName(updateDTO.name());
            changed = true;
        }
        if (updateDTO.surname() != null && !updateDTO.surname().isBlank() && !updateDTO.surname().equals(staffToUpdate.getSurname())) {
            staffToUpdate.setSurname(updateDTO.surname());
            changed = true;
        }

        if (staffToUpdate instanceof Vet vetToUpdate) {
            if (StringUtils.hasText(updateDTO.licenseNumber()) && !updateDTO.licenseNumber().equals(vetToUpdate.getLicenseNumber())) {
                validateHelper.validateVetLicenseUpdate(updateDTO.licenseNumber(), vetToUpdate.getId());
                vetToUpdate.setLicenseNumber(updateDTO.licenseNumber());
                changed = true;
            }
            if (StringUtils.hasText(updateDTO.vetPublicKey()) && !updateDTO.vetPublicKey().equals(vetToUpdate.getVetPublicKey())) {
                validateHelper.validateVetPublicKey(updateDTO.vetPublicKey(), vetToUpdate.getId());
                vetToUpdate.setVetPublicKey(updateDTO.vetPublicKey());
                changed = true;
            }
        } else {
            if (StringUtils.hasText(updateDTO.licenseNumber()) || StringUtils.hasText(updateDTO.vetPublicKey())) {
                log.warn("Attempted to update Vet-specific fields (license/key) for non-Vet staff ID {}", staffToUpdate.getId());
            }
        }
        return changed;
    }

    private String getDefaultAvatarPath(String filename) {
        String basePath = defaultUserImagePathBase.endsWith("/") ? defaultUserImagePathBase : defaultUserImagePathBase + '/';
        return basePath + filename;
    }
}
