package com.petconnect.backend.user.application.dto;

import java.util.Set;

/**
 * DTO specifically for returning Pet Owner profile details.
 * Extends or includes fields from UserProfileDto plus owner-specific fields.
 *
 * @param id UserEntity's unique ID.
 * @param username UserEntity's username.
 * @param email UserEntity's email.
 * @param roles UserEntity's role (should always be "OWNER").
 * @param avatar URL to the user's avatar.
 * @param phone Owner's contact phone number.
 *
 * @author ibosquet
 */
public record OwnerProfileDto(
        Long id,
        String username,
        String email,
        Set<String> roles,
        String avatar,
        String phone
) {
}
