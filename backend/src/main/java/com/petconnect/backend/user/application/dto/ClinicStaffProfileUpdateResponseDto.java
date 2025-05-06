package com.petconnect.backend.user.application.dto;

/**
 * DTO for the response after updating a Clinic Staff's profile.
 * Includes the updated profile and an optional new JWT if the username is changed.
 * @param profile The updated staff profile.
 * @param newJwtToken The new JWT if the username was changed, null otherwise.
 * @author ibosquet
 */
public record ClinicStaffProfileUpdateResponseDto(
        ClinicStaffProfileDto profile,
        String newJwtToken
) {
}
