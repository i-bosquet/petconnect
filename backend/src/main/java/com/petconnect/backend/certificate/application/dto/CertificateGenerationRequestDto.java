package com.petconnect.backend.certificate.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO containing the necessary information to request the generation of a new certificate.
 *
 * @param recordId          The ID of the specific medical record (usually VACCINE type and signed)
 *                          that forms the basis for this certificate. Cannot be null.
 * @param certificateNumber The official certificate number assigned externally.
 *                          Cannot be blank.
 *
 * @author ibosquet
 */
public record CertificateGenerationRequestDto(
        @NotNull(message = "Originating Record ID cannot be null")
        Long recordId,

        @NotBlank(message = "Official Certificate Number cannot be blank")
        String certificateNumber
) {
}
