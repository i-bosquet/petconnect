package com.petconnect.backend.record.application.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO for requesting temporary access to a pet's signed medical records.
 *
 * @param durationString A string representing the requested duration (e.g., "PT1H", "P1D", "PT24H").
 *                       Must be parseable by {@link java.time.Duration#parse(CharSequence)}.
 * @author ibosquet
 */
public record TemporaryAccessRequestDto(
        @NotBlank(message = "Duration string cannot be blank (e.g., PT1H, P1D)")
        String durationString
) {
}
