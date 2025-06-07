package com.petconnect.backend.record.application.dto;

import com.petconnect.backend.pet.domain.model.Specie;
import com.petconnect.backend.record.domain.model.RecordType;
import com.petconnect.backend.user.application.dto.UserProfileDto;

import java.time.LocalDateTime;

/**
 * Data Transfer Object representing the detailed view of a medical record.
 * This DTO is used for presenting a comprehensive overview of a single record,
 * including its metadata, associated vaccine details (if any), and information
 * about the pet and creator.
 *
 * @param id                  The unique identifier of the record.
 * @param type                The type of the medical record (e.g., FIRST_VISIT, ANNUAL_CHECK).
 * @param description         A brief description or summary of the record.
 * @param vetSignature        The digital signature or identifier of the vet or clinician
 *                             responsible for this record, if applicable.
 * @param createdAt           The timestamp of when the record was created.
 * @param createdBy           The username or identifier of the user who created the record.
 * @param updatedAt           The timestamp of the last update made to this record, if any.
 * @param updatedBy           The username or identifier of the user who last updated the record,
 *                             if applicable.
 * @param creator             The user profile of the record's creator, providing additional
 *                             details like their ID and username.
 * @param vaccine             Details about an associated vaccine, if the record is of type
 *                             VACCINE (otherwise null).
 * @param createdInClinicId   The unique identifier of the clinic where the record was created.
 * @param createdInClinicName The name of the clinic where the record was created.
 * @param petId               The unique identifier of the pet associated with this record.
 * @param petName             The name of the pet associated with this record.
 * @param petSpecie           The species of the pet associated with this record (e.g., DOG, CAT).
 *
 * @author ibosquet
 */
public record RecordViewDto(
        Long id,
        RecordType type,
        String description,
        String vetSignature,
        LocalDateTime createdAt,
        String createdBy,
        LocalDateTime updatedAt,
        String updatedBy,
        UserProfileDto creator,
        VaccineViewDto vaccine,
        Long createdInClinicId,
        String createdInClinicName,
        Long petId,
        String petName,
        Specie petSpecie,
        boolean isImmutable
) {
}
