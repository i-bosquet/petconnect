package com.petconnect.backend.user.application.dto;

import com.petconnect.backend.user.domain.model.RoleEnum;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO for creating new Clinic Staff (Vet or Admin) by an existing Admin.
 * Includes necessary fields for UserEntity and ClinicStaff.
 * Password should be handled appropriately (e.g., temporary or set by user later).
 *
 * @param username New staff's unique username.
 * @param email New staff's unique email.
 * @param password Initial password (plain text). Consider security implications/flow.
 * @param name Staff's first name.
 * @param surname Staff's last name.
 * @param role RoleEnum assigned (VET or ADMIN).
 * @param licenseNumber (Optional, only for VET) Vet's license number.
 * @param vetPublicKey (Optional, only for VET) Vet's public key.
 *
 * @author ibosquet
 */
public record ClinicStaffCreationDto(
        @NotBlank(message = "Username cannot be blank")
        @Size(min = 3, max = 50) String username,
        @NotBlank(message = "Email cannot be blank")
        @Email String email,
        @NotBlank(message = "Password cannot be blank")
        @Size(min = 8) String password, // Consider a different approach for initial passwords
        @NotBlank(message = "First name cannot be blank")
        @Size(max = 100) String name,
        @NotBlank(message = "Surname cannot be blank")
        @Size(max = 100) String surname,
        @NotNull(message = "Role cannot be null") RoleEnum role , // Must be VET or ADMIN
        // Vet specific fields - validation might depend on role='VET'
        String licenseNumber, // Required if role is VET
        String vetPublicKey   // Required if role is VET
        // Note: isActive defaults to true in entity
) {
}
