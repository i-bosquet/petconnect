package com.petconnect.backend.user.application.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Represents the login request data for authentication.
 * Contains the username and password provided by the user.
 *
 * @param username the username of the user
 * @param password the password of the user
 *
 * @author ibosquet
 */
public record AuthLoginRequestDto(
        @NotBlank String username,
        @NotBlank String password
) {
}
