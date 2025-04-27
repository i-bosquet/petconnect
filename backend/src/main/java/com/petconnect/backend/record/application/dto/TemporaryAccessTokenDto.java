package com.petconnect.backend.record.application.dto;

/**
 * DTO containing the generated temporary access token.
 *
 * @param token The JWT token granting temporary read-only access.
 * @author ibosquet
 */
public record TemporaryAccessTokenDto(
        String token
) {
}
