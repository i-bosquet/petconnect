package com.petconnect.backend.user.application.dto;

import jakarta.validation.constraints.Size;

/**
 * DTO for an Owner updating their specific profile information.
 * Includes UserProfileUpdateDto fields plus owner-specific updatable fields.
 *
 * @param username New username (optional update).
 * @param avatar New avatar URL (optional update).
 * @param phone New phone number (optional update).
 *
 * @author ibosquet
 */
public record OwnerProfileUpdateDto(
        @Size(min = 3, max = 50) String username,
        String avatar,
        @Size(max = 20) String phone
) {
}
