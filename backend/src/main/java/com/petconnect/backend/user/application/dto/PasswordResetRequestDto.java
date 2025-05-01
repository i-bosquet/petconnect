package com.petconnect.backend.user.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO for requesting a password reset link.
 *
 * @param email The email address associated with the account.
 * @author ibosquet
 */
public record PasswordResetRequestDto(
        @NotBlank(message = "Email cannot be blank")
        @Email(message = "Email should be valid")
        String email
) {
}
