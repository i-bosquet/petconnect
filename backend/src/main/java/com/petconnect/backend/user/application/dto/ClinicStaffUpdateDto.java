package com.petconnect.backend.user.application.dto;

import com.petconnect.backend.user.domain.model.RoleEnum;
import jakarta.validation.constraints.Size;
import java.util.Set;

/**
 * DTO for an Admin updating Clinic Staff information.
 * Allows updating name, surname. Activation status is handled separately.
 *
 * @param name Updated first name.
 * @param surname Updated last name.
 * @param licenseNumber Updated license number (only for VET).
 *
 * @author ibosquet
 */
public record ClinicStaffUpdateDto(
        @Size(max = 100) String name,
        @Size(max = 100) String surname,
        Set<RoleEnum> roles,
        // Include Vet fields if they are updatable by Admin
        String licenseNumber
) {
}
