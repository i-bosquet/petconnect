package com.petconnect.backend.certificate.application.dto;

import com.petconnect.backend.pet.application.dto.PetProfileDto;
import com.petconnect.backend.record.application.dto.RecordViewDto;
import com.petconnect.backend.user.application.dto.VetSummaryDto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Data Transfer Object representing a detailed view of a digital certificate.
 * This record is used to expose the information related to a certificate,
 * including its metadata, linked pet profile, originating record, and associated veterinarian.
 * It also includes cryptographic data such as the payload, hash, and signatures.
 *
 * @param id                          The unique identifier of the certificate.
 * @param certificateNumber           The official certificate number assigned.
 * @param pet                         The pet profile associated with this certificate.
 * @param originatingRecord           The medical record from which this certificate originates.
 * @param generatorVet                The veterinarian who generated this certificate.
 * @param createdAt                   The timestamp when this certificate was created.
 * @param initialEuEntryExpiryDate    The initial expiry date for EU entry, calculated as 10 days after creation.
 * @param travelValidityEndDate       The date until which the certificate remains valid for travel, calculated as 4 months after creation.
 * @param payload                     The payload of the certificate, often containing signed data or additional information.
 * @param hash                        The cryptographic hash of the certificate's payload.
 * @param vetSignature                The digital signature of the veterinarian for this certificate.
 * @param clinicSignature             The digital signature of the clinic for this certificate.
 *
 * @author ibosquet
 */
public record CertificateViewDto(
        Long id,
        String certificateNumber,
        PetProfileDto pet,
        RecordViewDto originatingRecord,
        VetSummaryDto generatorVet,
        // --- Certificate Data ---
        LocalDateTime createdAt,
        LocalDate initialEuEntryExpiryDate, // createdAt + 10 days
        LocalDate travelValidityEndDate, // createdAt + 4 moths
        String payload,
        String hash,
        String vetSignature,
        String clinicSignature
) {
}
