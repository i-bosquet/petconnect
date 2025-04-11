package com.petconnect.backend.pet.application.dto;

import com.petconnect.backend.pet.domain.model.Gender;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Data Transfer Object for Clinic Staff (Vet/Admin) updating a Pet's clinical or detailed information.
 * Includes fields typically managed or verified by a clinic.
 * Fields are optional; only non-null values will be considered for update.
 *
 * @param color Optional updated color description (max 100 chars).
 * @param gender Optional updated gender.
 * @param birthDate Optional updated birthdate (must be past or present).
 * @param microchip Optional updated microchip number (max 50 chars, uniqueness checked in service).
 * @param breedId Optional updated ID of the pet's breed. Null might be used to clear the breed association.
 *
 * @author ibosquet
 */
public record PetClinicUpdateDto(
        @Size(max = 30, message = "Color description cannot exceed 30 characters")
        String color,

        Gender gender,

        @PastOrPresent(message = "Birth date must be in the past or present")
        LocalDate birthDate,

        @Size(max = 50, message = "Microchip number cannot exceed 50 characters")
        String microchip,

        Long breedId
) {
}
