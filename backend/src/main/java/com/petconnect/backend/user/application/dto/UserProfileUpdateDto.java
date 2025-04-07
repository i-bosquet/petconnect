package com.petconnect.backend.user.application.dto;

import jakarta.validation.constraints.Size;

/**
 * DTO for updating a user's own profile information.
 * Contains fields common to all users that they can update.
 *
 * @param username New username (optional update).
 * @param avatar New avatar URL (optional update).
 *
 * @author ibosquet
 */
public record UserProfileUpdateDto(
        @Size(min = 3, max = 50) String username, // Optional: Only update if provided
        String avatar // Optional: Only update if provided
        // Email/Password changes might require separate DTOs/Endpoints for security
) {
}
