package com.petconnect.backend.user.application.mapper;

import com.petconnect.backend.user.application.dto.*;
import com.petconnect.backend.user.domain.model.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * Utility class for manually mapping UserEntity-related entities (Owner, ClinicStaff, Vet)
 * to their corresponding Profile DTOs.
 * Registered as a Spring component to be injectable in services.
 * Creation/Update mappings are handled within the service layer.
 *
 * @author ibosquet
 */
@Component
public class UserMapper {

    /**
     * Maps an Owner entity to an OwnerProfileDto.
     * Delegates common field mapping to helper methods.
     *
     * @param owner The Owner entity. Cannot be null.
     * @return The corresponding OwnerProfileDto.
     */
    public OwnerProfileDto toOwnerProfileDto(Owner owner) {
        if (owner == null) return null;
        UserProfileDto baseDto = mapToBaseProfileDTO(owner); // Map common fields first
        return new OwnerProfileDto(
                baseDto.id(),
                baseDto.username(),
                baseDto.email(),
                baseDto.roles(), // Roles from base DTO
                baseDto.avatar(),
                owner.getPhone() // Add owner specific field
        );
    }

    /**
     * Maps a ClinicStaff entity (Vet or Admin) to a ClinicStaffProfileDto.
     * Delegates common field mapping and handles Vet-specific fields.
     *
     * @param staff The ClinicStaff entity. Cannot be null.
     * @return The corresponding ClinicStaffProfileDto.
     */
    public ClinicStaffProfileDto toClinicStaffProfileDto(ClinicStaff staff) {
        if (staff == null) return null;
        UserProfileDto baseDto = mapToBaseProfileDTO(staff); // Map common fields first

        String licenseNumber = null;
        String vetPublicKey = null;
        if (staff instanceof Vet vet) {
            licenseNumber = vet.getLicenseNumber();
            vetPublicKey = vet.getVetPublicKey();
        }

        Long clinicId = (staff.getClinic() != null) ? staff.getClinic().getId() : null;
        String clinicName = (staff.getClinic() != null) ? staff.getClinic().getName() : null;

        return new ClinicStaffProfileDto(
                baseDto.id(),
                baseDto.username(),
                baseDto.email(),
                baseDto.roles(), // Roles from base DTO
                baseDto.avatar(),
                staff.getName(),    // Staff specific
                staff.getSurname(), // Staff specific
                staff.isActive(),   // Staff specific
                clinicId,           // Staff specific
                clinicName,         // Staff specific
                licenseNumber,      // Vet specific
                vetPublicKey        // Vet specific
        );
    }

    /**
     * Maps any UserEntity entity to the generic UserProfileDto containing common fields.
     * This can be used by internal services or generic endpoints.
     * Includes the extracted role names.
     *
     * @param user The UserEntity entity (Owner, Vet, Admin). Cannot be null.
     * @return The corresponding UserProfileDto.
     */
    public UserProfileDto mapToBaseProfileDTO(UserEntity user) { // Renamed from mapToGenericProfileDTO for clarity
        if (user == null) return null;
        Set<String> roleNames = extractRoleNames(user);
        return new UserProfileDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                roleNames, // Pass the set of role names
                user.getAvatar()
        );
    }

    /**
     * Updates an Owner entity from an OwnerProfileUpdateDto.
     * Applies updates for fields allowed in the owner's self-update.
     * Uses a helper method for common user field updates.
     *
     * @param dto The OwnerProfileUpdateDto. Cannot be null.
     * @param owner The Owner entity to update. Cannot be null.
     */
    public void updateOwnerFromDto(OwnerProfileUpdateDto dto, Owner owner) {
        if (dto == null || owner == null) return;
        // Update common fields first using the DTO's fields
        updateUserCommonFields(owner, dto.username(), dto.avatar());
        // Update owner-specific fields
        if (dto.phone() != null && !dto.phone().isBlank()) {
            owner.setPhone(dto.phone());
        }
    }

    /**
     * Updates common UserEntity fields (username, avatar) for a ClinicStaff entity
     * from a UserProfileUpdateDto. Used for staff self-update.
     * Uses a helper method for common user field updates.
     *
     * @param dto The UserProfileUpdateDto. Cannot be null.
     * @param staff The ClinicStaff entity to update. Cannot be null.
     */
    public void updateClinicStaffCommonFromDto(UserProfileUpdateDto dto, ClinicStaff staff) {
        if (dto == null || staff == null) return;
        // Update common fields using the helper
        updateUserCommonFields(staff, dto.username(), dto.avatar());
        // No staff-specific fields updated here (name, surname, etc. updated by Admin via different DTO/method)
    }

    /**
     * Extracts the names of the roles associated with a user.
     *
     * @param user The user entity.
     * @return A Set of role names as Strings. Returns an empty set if user or roles are null/empty.
     */
    private Set<String> extractRoleNames(UserEntity user) {
        if (user == null || user.getRoles() == null) {
            return Set.of(); // Return empty immutable set
        }
        return user.getRoles().stream()
                .map(RoleEntity::getRoleEnum)
                .map(RoleEnum::name)
                .collect(Collectors.toSet());
    }

    /**
     * Helper method to update common user fields (username, avatar) if new values are provided.
     *
     * @param user The UserEntity entity to update.
     * @param newUsername The potential new username (can be null).
     * @param newAvatar The potential new avatar URL (can be null).
     */
    private void updateUserCommonFields(UserEntity user, String newUsername, String newAvatar) {
        // Update username only if it's provided, not blank, AND different from current
        // Note: Uniqueness check MUST happen in the service layer BEFORE calling this update
        if (newUsername != null && !newUsername.isBlank() && !user.getUsername().equals(newUsername)) {
            user.setUsername(newUsername);
        }
        // Update avatar if provided (handle null vs. empty string if necessary)
        if (newAvatar != null) {
            user.setAvatar(newAvatar);
        }
    }

    /**
     * Maps a list of ClinicStaff entities to a list of ClinicStaffProfileDto.
     * Handles null or empty lists gracefully.
     *
     * @param staffList The list of ClinicStaff entities.
     * @return An immutable list of corresponding ClinicStaffProfileDto objects.
     */
    public List<ClinicStaffProfileDto> toClinicStaffProfileDtoList(List<ClinicStaff> staffList) {
        if (staffList == null || staffList.isEmpty()) {
            return List.of(); // Return immutable empty list
        }
        return staffList.stream()
                .map(this::toClinicStaffProfileDto) // Reuse the single DTO mapping method
                .toList(); // Use List.of() or Collectors.toUnmodifiableList() if preferred
    }
}