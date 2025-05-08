package com.petconnect.backend.user.application.dto;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * DTO for returning Clinic Staff (Vet/Admin) profile details.
 * Includes common user fields, staff fields, and clinic info.
 *
 * @param id UserEntity's unique ID.
 * @param username UserEntity's username.
 * @param email UserEntity's email.
 * @param roles UserEntity's role (VET or ADMIN).
 * @param avatar URL to the user's avatar.
 * @param name Staff's first name.
 * @param surname Staff's last name.
 * @param isActive Staff's active status.
 * @param clinicId ID of the associated clinic.
 * @param clinicName Name of the associated clinic.
 * @param licenseNumber (Conditional) Vet's license number if role is VET.
 * @param vetPublicKey (Conditional) Vet's public key if a role is VET.
 *
 * @author ibosquet
 */
public record ClinicStaffProfileDto(
        Long id,
        String username,
        String email,
        Set<String> roles,
        String avatar,
        String name,
        String surname,
        boolean isActive,
        Long clinicId,
        String clinicName, // Include clinic name for convenience
        // Only for vets:
        String licenseNumber,
        String vetPublicKey,
        LocalDateTime createdAt,
        String createdBy,
        LocalDateTime updatedAt,
        String updatedBy
) {
}