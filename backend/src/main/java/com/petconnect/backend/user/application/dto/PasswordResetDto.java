package com.petconnect.backend.user.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO for submitting a new password using a reset token.
 *
 * @param token The password reset token received via email.
 * @param newPassword The new password for the account.
 * @param confirmPassword Confirmation of the new password.
 * @author ibosquet
 */
public record PasswordResetDto(
        @NotBlank(message = "Token cannot be blank")
        String token,

        @NotBlank(message = "New password cannot be blank")
        @Size(min = 8, message = "Password must be at least 8 characters long")
        String newPassword,

        @NotBlank(message = "Password confirmation cannot be blank")
        String confirmPassword
) {
}
