package com.petconnect.backend.certificate.application.dto;

import com.petconnect.backend.pet.application.dto.PetProfileDto;
import com.petconnect.backend.record.application.dto.RecordSummaryDto;
import com.petconnect.backend.user.application.dto.ClinicDto;
import com.petconnect.backend.user.application.dto.VetSummaryDto;

import java.time.LocalDateTime;

/**
 * DTO for displaying detailed information about a generated Certificate.
 * Includes essential certificate data, signatures, and references to related entities.
 *
 * @param id                  The unique ID of the certificate entity.
 * @param certificateNumber   The official certificate number.
 * @param pet                 Summary/Profile DTO of the associated Pet.
 * @param originatingRecord   Summary DTO of the Record this certificate is based on.
 * @param generatorVet        Summary DTO of the Vet who generated the certificate.
 * @param issuingClinic       Summary DTO of the Clinic that issued the certificate.
 * @param createdAt           Timestamp when the certificate entity was created.
 * @param payload             The raw payload data (JSON string) used for the certificate.
 * @param hash                The cryptographic hash of the payload.
 * @param vetSignature        The Base64 encoded digital signature from the Vet.
 * @param clinicSignature     The Base64 encoded digital signature from the Clinic.
 *
 * @author ibosquet
 */
public record CertificateViewDto(
        Long id,
        String certificateNumber,
        PetProfileDto pet,
        RecordSummaryDto originatingRecord,
        VetSummaryDto generatorVet,
        ClinicDto issuingClinic,
        // --- Certificate Data ---
        LocalDateTime createdAt,
        String payload,
        String hash,
        String vetSignature,
        String clinicSignature
) {
}
