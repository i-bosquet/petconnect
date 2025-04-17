package com.petconnect.backend.record.application.dto;

/**
 * Data Transfer Object for displaying details of an administered vaccine.
 * This is typically nested within a {@link RecordViewDto}.
 *
 * @param name        The commercial name of the vaccine.
 * @param validity    The validity period in years.
 * @param laboratory  The manufacturer/laboratory.
 * @param batchNumber The batch number of the vaccine vial.
 *
 * @author ibosquet
 */
public record VaccineViewDto(
        String name,
        Integer validity,
        String laboratory,
        String batchNumber
) {
}
