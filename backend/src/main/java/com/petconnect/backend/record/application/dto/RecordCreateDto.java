package com.petconnect.backend.record.application.dto;

import com.petconnect.backend.record.domain.model.RecordType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.lang.Nullable;

/**
 * Data Transfer Object for creating a new medical record.
 * Used by both Owners (for informative records) and Clinic Staff/Vets (for clinical records).
 * Includes optional nested DTO for vaccine details if the record type is VACCINE.
 *
 * @param type        The type of record being created. Cannot be null.
 * @param description A textual description for the record. Optional, max 2000 chars.
 * @param vaccine     Optional details of the vaccine, required if the type is VACCINE. Must be valid if present.
 *
 * @author ibosquet
 */
public record RecordCreateDto(
        @NotNull(message = "Pet ID cannot be null")
        Long petId,
        @NotNull(message = "Record type cannot be null")
        RecordType type,
        @Size(max = 2000, message = "Description cannot exceed 2000 characters")
        String description,
        @Valid
        VaccineCreateDto vaccine,
        @Nullable
        String vetPrivateKeyPassword
) {
}
