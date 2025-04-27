package com.petconnect.backend.record.application.dto;

import com.petconnect.backend.record.domain.model.RecordType;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object for updating an existing medical record.
 * Allows updating the record type (excluding VACCINE) and/or the description.
 * Both fields are optional; if a field is null, it will not be updated.
 * Attempting to change the type TO or FROM VACCINE is not allowed via this DTO.
 *
 * @param type Optional new type for the record (cannot be VACCINE).
 * @param description Optional new description for the record (max 2000 chars).
 *
 * @author ibosquet
 */
public record RecordUpdateDto(
        RecordType type,
        @Size(max = 2000, message = "Description cannot exceed 2000 characters")
        String description
) {
}