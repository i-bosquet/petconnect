package com.petconnect.backend.user.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO for registering a new Pet Owner.
 * Includes basic user info and owner-specific phone.
 * Validation constraints are applied.
 *
 * @param username UserEntity's unique username.
 * @param email UserEntity's unique email address.
 * @param password UserEntity's chosen password (plain text, will be hashed in service).
 * @param phone Owner's contact phone number.
 *
 * @author ibosquet
 */
public record OwnerRegistrationDto(
        @NotBlank(message = "Username cannot be blank")
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        String username,

        @NotBlank(message = "Email cannot be blank")
        @Email(message = "Email should be valid")
        String email,

        @NotBlank(message = "Password cannot be blank")
        @Size(min = 8, message = "Password must be at least 8 characters long") // Example password policy
        String password,

        @NotBlank(message = "Phone number cannot be blank")
        @Size(max = 20, message = "Phone number cannot exceed 20 characters")
        String phone
) {
}
