package com.petconnect.backend.user.application.dto;

import java.util.Set;

/**
 * Data Transfer Object representing the profile information of a user in the system.
 * Primarily used to encapsulate and transfer user-related data across the application layers.
 * Includes details such as the user's identification, credentials, roles, and clinic association.
 *
 * @param id The unique identifier of the user.
 * @param username The user's chosen username.
 * @param email The email address associated with the user's account.
 * @param roles A set of roles assigned to the user, representing their permissions within the system.
 * @param avatar The URL or path to the user's avatar image.
 * @param name The first name of the user.
 * @param surname The surname of the user.
 * @param clinicName The name of the associated clinic, if any.
 *
 * @author ibosquet
 */
public record UserProfileDto(
        Long id,
        String username,
        String email,
        Set<String> roles,
        String avatar,
        String name,
        String surname,
        String clinicName
) {
}
