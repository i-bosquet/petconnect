package com.petconnect.backend.record.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object containing details needed to create a Vaccine record.
 * This is typically nested within a {@link RecordCreateDto} when the record type is VACCINE.
 *
 * @param name         The commercial name of the vaccine. Cannot be blank. Max 100 chars.
 * @param validity     The validity period in years. Cannot be null, must be >= 0.
 * @param laboratory   The manufacturer/laboratory. Optional. Max 100 chars.
 * @param batchNumber  The batch number of the vaccine vial. Cannot be blank. Max 50 chars.
 *
 * @author ibosquet
 */
public record VaccineCreateDto(
        @NotBlank(message = "Vaccine name cannot be blank")
        @Size(max = 100)
        String name,

        @NotNull(message = "Vaccine validity cannot be null")
        @Min(value = 0, message = "Validity must be non-negative")
        Integer validity,

        @Size(max = 100)
        String laboratory,

        @NotBlank(message = "Batch number cannot be blank")
        @Size(max = 50)
        String batchNumber,

        @NotNull(message = "Must specify if it is a rabies vaccine")
        Boolean isRabiesVaccine
) {
}
