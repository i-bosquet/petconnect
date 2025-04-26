package com.petconnect.backend.certificate.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO containing the necessary information to request the generation of a new certificate.
 *
 * @param petId          The ID of the specific pet Cannot be null.
 * @param certificateNumber The official certificate number assigned externally.
 *                          Cannot be blank.
 *
 * @author ibosquet
 */
public record CertificateGenerationRequestDto(
        @NotNull(message = "Pet ID cannot be null")
        Long petId,

        @NotBlank(message = "Official Certificate Number cannot be blank")
        String certificateNumber
) {
}
