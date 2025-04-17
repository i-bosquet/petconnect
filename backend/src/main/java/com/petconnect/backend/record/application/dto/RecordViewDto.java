package com.petconnect.backend.record.application.dto;

import com.petconnect.backend.record.domain.model.RecordType;
import com.petconnect.backend.user.application.dto.UserProfileDto;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for displaying details of a medical record.
 * Includes information about the creator, the type, description, signature status,
 * and nested vaccine details if applicable.
 *
 * @param id           The unique ID of the record.
 * @param type         The type of the record.
 * @param description  The textual description.
 * @param vetSignature The digital signature of the Vet, if signed (null otherwise).
 * @param createdAt    Timestamp when the record was created.
 * @param creator      A summary DTO of the user (Owner/Vet/Admin) who created the record.
 * @param vaccine      Optional details of the vaccine, present only if type is VACCINE.
 *
 * @author ibosquet
 */
public record RecordViewDto(
        Long id,
        RecordType type,
        String description,
        String vetSignature, // Or potentially a boolean like 'isSigned'
        LocalDateTime createdAt,
        UserProfileDto creator, // DTO of the user who created it
        VaccineViewDto vaccine
) {
}
