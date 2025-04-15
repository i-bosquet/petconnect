package com.petconnect.backend.user.application.service.impl;

import com.petconnect.backend.exception.*;
import com.petconnect.backend.user.application.dto.ClinicStaffCreationDto;
import com.petconnect.backend.user.application.dto.ClinicStaffProfileDto;
import com.petconnect.backend.user.application.dto.ClinicStaffUpdateDto;
import com.petconnect.backend.user.application.mapper.UserMapper;
import com.petconnect.backend.user.application.service.ClinicStaffService;
import com.petconnect.backend.user.domain.model.*;
import com.petconnect.backend.user.domain.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ClinicRepository clinicRepository; // Needed for verifying clinic access
    private final ClinicStaffRepository clinicStaffRepository;
    private final VetRepository vetRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    static final String DEFAULT_VET_AVATAR = "images/avatars/users/vet.png";
    static final String DEFAULT_ADMIN_AVATAR = "images/avatars/users/admin.png";


    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public ClinicStaffProfileDto createClinicStaff(ClinicStaffCreationDto creationDTO, Long creatingAdminId) {
        // Verify the user performing the action is a valid Admin and get their clinic
        ClinicStaff creatingAdminStaff = findAdminStaffOrFail(creatingAdminId, "create clinic staff");
        Clinic targetClinic = creatingAdminStaff.getClinic(); // New staff will belong to this admin's clinic

        // Validate Role and Uniqueness
        validateStaffRole(creationDTO.role());
        validateNewStaffUniqueness(creationDTO.email(), creationDTO.username());

        // Create Entity (Vet or Admin)
        ClinicStaff newStaff = buildNewStaffEntity(creationDTO, targetClinic);

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
        ClinicStaff staffToUpdate = findStaffOrFail(staffId);
        // Verify Admin authorization (same clinic)
        verifyAdminActionOnStaff(updatingAdminId, staffToUpdate, "update");

        // Apply updates
        boolean changed = applyStaffUpdates(staffToUpdate, updateDTO);

        // Save if changed
        ClinicStaff updatedStaff = staffToUpdate;
        if (changed) {
            updatedStaff = clinicStaffRepository.save(staffToUpdate);
            log.info("Admin {} updated staff {}", updatingAdminId, updatedStaff.getUsername());
        } else {
            log.info("No changes detected for staff ID {}, update skipped.", staffId);
        }

        // Map and return
        return userMapper.toClinicStaffProfileDto(updatedStaff);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public ClinicStaffProfileDto activateStaff(Long staffId, Long activatingAdminId) {
        ClinicStaff staffToActivate = findStaffOrFail(staffId);
        verifyAdminActionOnStaff(activatingAdminId, staffToActivate, "activate"); // Verify admin is authorized for this staff

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
        ClinicStaff staffToDeactivate = findStaffOrFail(staffId);
        verifyAdminActionOnStaff(deactivatingAdminId, staffToDeactivate, "deactivate"); // Verify admin is authorized for this staff

        if (!staffToDeactivate.isActive()) {
            throw new IllegalStateException("Staff member with id " + staffId + " is already inactive.");
        }
        // Prevent self-deactivation
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
        verifyClinicStaffAccess(requesterUserId, clinicId, "view active staff for"); // Verify requester can access this clinic's data
        List<ClinicStaff> staffList = clinicStaffRepository.findByClinicIdAndIsActive(clinicId, true);
        return userMapper.toClinicStaffProfileDtoList(staffList); // Use mapper
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public List<ClinicStaffProfileDto> findAllStaffByClinic(Long clinicId, Long requesterUserId) {
        verifyClinicStaffAccess(requesterUserId, clinicId, "view all staff for"); // Verify requester can access this clinic's data
        List<ClinicStaff> staffList = clinicStaffRepository.findByClinicId(clinicId);
        return userMapper.toClinicStaffProfileDtoList(staffList); // Use mapper
    }

    // --- PRIVATE HELPER METHODS ---

    /**
     * Finds a ClinicStaff entity by its ID or throws EntityNotFoundException.
     * @param staffId The ID of the staff member.
     * @return The found ClinicStaff entity.
     * @throws EntityNotFoundException if not found.
     */
    private ClinicStaff findStaffOrFail(Long staffId) {
        return clinicStaffRepository.findById(staffId)
                .orElseThrow(() -> new EntityNotFoundException(ClinicStaff.class.getSimpleName(), staffId));
    }

    /**
     * Finds a UserEntity by ID, ensures it's a ClinicStaff and specifically an ADMIN,
     * and checks if they are associated with a clinic. Throws appropriate exceptions.
     * @param adminId The ID of the user expected to be an admin.
     * @param actionContext Description of the action being performed (for error messages).
     * @return The found and validated ClinicStaff entity (as an Admin).
     * @throws EntityNotFoundException if user not found.
     * @throws AccessDeniedException if user is not ClinicStaff or not an ADMIN.
     * @throws IllegalStateException if the admin is not linked to a clinic (data consistency issue).
     */
    private ClinicStaff findAdminStaffOrFail(Long adminId, String actionContext) {
        UserEntity user = userRepository.findById(adminId)
                .orElseThrow(() -> new EntityNotFoundException("User performing action [" + actionContext + "] not found with id: " + adminId));

        if (!(user instanceof ClinicStaff staff)) {
            throw new AccessDeniedException("User " + adminId + " is not Clinic Staff and cannot perform action [" + actionContext + "].");
        }

        boolean isAdmin = staff.getRoles().stream().anyMatch(r -> r.getRoleEnum() == RoleEnum.ADMIN);
        if (!isAdmin) {
            throw new AccessDeniedException("User " + adminId + " is not an authorized Admin to perform action [" + actionContext + "].");
        }

        if (staff.getClinic() == null) {
            log.error("Data inconsistency: Admin user {} performing action [{}] has no associated clinic.", adminId, actionContext);
            throw new IllegalStateException("Admin user " + adminId + " is not associated with any clinic.");
        }
        return staff; // Safe to return as ClinicStaff
    }

    /**
     * Verifies if the Admin user performing an action belongs to the same clinic as the target staff member.
     * @param adminId The ID of the Admin user.
     * @param targetStaff The ClinicStaff entity being acted upon.
     * @param actionDescription Description of the action (e.g., "update", "activate").
     * @throws EntityNotFoundException if admin user not found (via findAdminStaffOrFail).
     * @throws AccessDeniedException if admin is not authorized or clinics don't match.
     */
    private void verifyAdminActionOnStaff(Long adminId, ClinicStaff targetStaff, String actionDescription) {
        ClinicStaff adminStaff = findAdminStaffOrFail(adminId, actionDescription + " staff " + targetStaff.getId());
        Clinic adminClinic = adminStaff.getClinic(); // Not null due to check in findAdminStaffOrFail
        Clinic targetClinic = targetStaff.getClinic();

        if (targetClinic == null) {
            log.error("Data inconsistency: Target staff {} has no associated clinic.", targetStaff.getId());
            throw new IllegalStateException("Target staff " + targetStaff.getId() + " is not associated with any clinic.");
        }

        if (!adminClinic.getId().equals(targetClinic.getId())) {
            throw new AccessDeniedException(String.format("Admin (ID: %d, Clinic: %d) cannot %s staff (ID: %d, Clinic: %d) from a different clinic.",
                    adminId, adminClinic.getId(), actionDescription,
                    targetStaff.getId(), targetClinic.getId()));
        }
    }

    /**
     * Verifies if the requesting user (must be Vet or Admin) belongs to the target clinic
     * they are trying to access information for.
     * @param requesterUserId The ID of the user making the request.
     * @param targetClinicId The ID of the clinic being accessed.
     * @param actionDescription Description of the action (e.g., "view staff for").
     * @throws EntityNotFoundException if requester user or target clinic not found.
     * @throws AccessDeniedException if requester is not ClinicStaff, not Vet/Admin, or not from the target clinic.
     */
    private void verifyClinicStaffAccess(Long requesterUserId, Long targetClinicId, String actionDescription) {
        UserEntity requesterUser = userRepository.findById(requesterUserId)
                .orElseThrow(() -> new EntityNotFoundException("Requesting user not found with id: " + requesterUserId));

        // check if the target clinic actually exist
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

    /**
     * Validates that the provided RoleEnum is either VET or ADMIN.
     * @param role The RoleEnum to validate.
     * @throws IllegalArgumentException if the role is invalid.
     */
    private void validateStaffRole(RoleEnum role) {
        if (role != RoleEnum.VET && role != RoleEnum.ADMIN) {
            throw new IllegalArgumentException("Invalid role specified for clinic staff. Must be VET or ADMIN.");
        }
    }

    /**
     * Validates email and username uniqueness when creating a new staff member.
     * @param email The email to check.
     * @param username The username to check.
     * @throws EmailAlreadyExistsException if email exists.
     * @throws UsernameAlreadyExistsException if username exists.
     */
    private void validateNewStaffUniqueness(String email, String username) {
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException(email);
        }
        if (userRepository.existsByUsername(username)) {
            throw new UsernameAlreadyExistsException(username);
        }
    }

    /**
     * Validates Vet license number for requirement and uniqueness.
     *
     * @param licenseNumber The license number to validate.
     * @throws IllegalArgumentException if license number is blank.
     * @throws LicenseNumberAlreadyExistsException if license number is already in use by another Vet.
     */
    private void validateVetLicenseNumber(String licenseNumber) {
        if (!StringUtils.hasText(licenseNumber)) {
            throw new IllegalArgumentException("License number is required for VET role.");
        }
        boolean exists = vetRepository.existsByLicenseNumber(licenseNumber);
        if (exists) {
            throw new LicenseNumberAlreadyExistsException(licenseNumber);
        }
    }

    /**
     * Validates Vet public key for requirement and uniqueness.
     *
     * @param vetPublicKey The public key to validate.
     * @param vetIdToExclude The ID of the Vet being updated (null if creating a new Vet).
     * @throws IllegalArgumentException if public key is blank.
     * @throws VetPublicKeyAlreadyExistsException if public key is already in use by another Vet.
     */
    private void validateVetPublicKey(String vetPublicKey, Long vetIdToExclude) {
        if (!StringUtils.hasText(vetPublicKey)) {
            throw new IllegalArgumentException("Veterinarian public key is required for VET role.");
        }
        log.debug("Validating public key uniqueness: key={}, excludingId={}", vetPublicKey, vetIdToExclude);
        // Check public key uniqueness
        boolean keyExists = vetRepository.existsByVetPublicKey(vetPublicKey);
        log.debug("Result from existsByVetPublicKeyAndIdNot: {}", keyExists);
        if (keyExists) throw new VetPublicKeyAlreadyExistsException();
    }

    /**
     * Builds a new {@link Vet} or {@link ClinicStaff} (for ADMIN role) entity based on the provided DTO and clinic.
     * This method populates common fields and role-specific fields (for Vet), including validation.
     * It does *not* save the entity to the database.
     *
     * @param dto The DTO containing the data for the new staff member.
     * @param targetClinic The {@link Clinic} the new staff member will belong to.
     * @return The constructed (but not persisted) {@link ClinicStaff} or {@link Vet} entity.
     * @throws IllegalArgumentException if VET role specific fields (license, key) are invalid or missing.
     * @throws LicenseNumberAlreadyExistsException if the VET license number is already in use.
     * @throws VetPublicKeyAlreadyExistsException if the VET public key is already in use.
     */
    private ClinicStaff buildNewStaffEntity(ClinicStaffCreationDto dto, Clinic targetClinic) {
        ClinicStaff newStaff;
        if (dto.role() == RoleEnum.VET) {
            // Validate VET specific fields first
            validateVetLicenseNumber(dto.licenseNumber());
            validateVetPublicKey(dto.vetPublicKey(), null);

            Vet newVet = new Vet();
            setCommonStaffFields(newVet, dto, targetClinic); // Populate common fields
            // Set Vet specific fields
            newVet.setLicenseNumber(dto.licenseNumber());
            newVet.setVetPublicKey(dto.vetPublicKey());
            newVet.setAvatar(DEFAULT_VET_AVATAR);
            newStaff = newVet;
        } else { // ADMIN
            ClinicStaff newAdmin = new ClinicStaff();
            setCommonStaffFields(newAdmin, dto, targetClinic); // Populate common fields
            newAdmin.setAvatar(DEFAULT_ADMIN_AVATAR);
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
        staff.setPassword(passwordEncoder.encode(dto.password())); // Hash password
        staff.setName(dto.name());
        staff.setSurname(dto.surname());

        RoleEntity staffRole = roleRepository.findByRoleEnum(dto.role())
                .orElseThrow(() -> new IllegalStateException("Role " + dto.role().name() + " not found in database! Check data.sql."));
        Set<RoleEntity> roles = new HashSet<>();
        roles.add(staffRole);
        staff.setRoles(roles);

        staff.setClinic(clinic);        // Assign the clinic
        staff.setActive(true);          // New staff are active by default
        staff.setEnabled(true);         // Enable user account fields from UserEntity
        staff.setAccountNonExpired(true);
        staff.setAccountNonLocked(true);
        staff.setCredentialsNonExpired(true);
        // Avatar is set in the buildNewStaffEntity method based on role
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
    private boolean applyStaffUpdates(ClinicStaff staffToUpdate, ClinicStaffUpdateDto updateDTO) {
        boolean changed = false;
        // Update Name
        if (updateDTO.name() != null && !updateDTO.name().isBlank() && !updateDTO.name().equals(staffToUpdate.getName())) {
            staffToUpdate.setName(updateDTO.name());
            changed = true;
        }
        // Update Surname
        if (updateDTO.surname() != null && !updateDTO.surname().isBlank() && !updateDTO.surname().equals(staffToUpdate.getSurname())) {
            staffToUpdate.setSurname(updateDTO.surname());
            changed = true;
        }

        // Update Vet-specific fields only if the entity is a Vet
        if (staffToUpdate instanceof Vet vetToUpdate) {
            // Update License Number if provided and different
            if (StringUtils.hasText(updateDTO.licenseNumber()) && !updateDTO.licenseNumber().equals(vetToUpdate.getLicenseNumber())) {
                validateVetLicenseNumber(updateDTO.licenseNumber()); // Validate license# only
                vetToUpdate.setLicenseNumber(updateDTO.licenseNumber());
                changed = true;
            }
            // Update Public Key if provided and different
            if (StringUtils.hasText(updateDTO.vetPublicKey()) && !updateDTO.vetPublicKey().equals(vetToUpdate.getVetPublicKey())) {
                validateVetPublicKey(updateDTO.vetPublicKey(), vetToUpdate.getId()); // Validate public key only
                vetToUpdate.setVetPublicKey(updateDTO.vetPublicKey());
                changed = true;
            }
        } else {
            // Log warning if trying to update Vet fields on a non-Vet
            if (StringUtils.hasText(updateDTO.licenseNumber()) || StringUtils.hasText(updateDTO.vetPublicKey())) {
                log.warn("Attempted to update Vet-specific fields (license/key) for non-Vet staff ID {}", staffToUpdate.getId());
                // Depending on requirements, you could throw an IllegalArgumentException here
            }
        }
        return changed;
    }
}