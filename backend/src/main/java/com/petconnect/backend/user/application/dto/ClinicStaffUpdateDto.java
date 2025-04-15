package com.petconnect.backend.user.application.dto;

import jakarta.validation.constraints.Size;

/**
 * DTO for an Admin updating Clinic Staff information.
 * Allows updating name, surname. Activation status handled separately.
 *
 * @param name Updated first name.
 * @param surname Updated last name.
 * @param licenseNumber Updated license number (only for VET).
 * @param vetPublicKey Updated public key (only for VET).
 *
 * @author ibosquet
 */
public record ClinicStaffUpdateDto(
        @Size(max = 100) String name,
        @Size(max = 100) String surname,
        // Include Vet fields if they are updatable by Admin
        String licenseNumber,
        String vetPublicKey
) {
}
