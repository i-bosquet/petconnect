package com.petconnect.backend.user.application.dto;

import java.util.Set;

/**
 * DTO for returning general user profile information.
 * Excludes sensitive data like password. Includes common fields.
 *
 * @param id UserEntity's unique ID.
 * @param username UserEntity's username.
 * @param email UserEntity's email.
 * @param roles UserEntity's roles.
 * @param avatar URL to the user's avatar.
 *
 * @author ibosquet
 */
public record UserProfileDto(
        Long id,
        String username,
        String email,
        Set<String> roles,
        String avatar
) {
}
