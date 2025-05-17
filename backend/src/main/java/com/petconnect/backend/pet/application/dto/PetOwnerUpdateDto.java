package com.petconnect.backend.pet.application.dto;

import com.petconnect.backend.pet.domain.model.Gender;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import org.springframework.lang.Nullable;

import java.time.LocalDate;

/**
 * Data Transfer Object for an Owner updating their Pet's information.
 * Allows the owner to modify any of the pet's details, including name, image,
 * color, gender, birthdate, microchip, and breed association.
 * This reflects the owner's ability to manage their pet's data independently,
 * regardless of clinic association. Clinic verification occurs separately before
 * activation or certificate issuance.
 * Fields are optional; only non-null/non-blank values will be considered for update in the service layer.
 *
 * @param name The new name for the pet (optional, max 50 chars).
 * @param image The new URL/path for the pet's image (optional).
 * @param color Optional updated color description (max 30 chars).
 * @param gender Optional updated gender.
 * @param birthDate Optional updated birthdate (must be past or present).
 * @param microchip Optional updated microchip number (max 50 chars, uniqueness checked in service).
 * @param breedId Optional updated ID of the pet's breed.
 *
 * @author ibosquet
 */
public record PetOwnerUpdateDto(
        @Size(max = 50, message = "Pet name cannot exceed 50 characters")
        String name,

        String image,

        @Size(max = 30, message = "Color description cannot exceed 30 characters")
        String color,

        Gender gender,

        @PastOrPresent(message = "Birth date must be in the past or present")
        LocalDate birthDate,

        @Size(max = 50, message = "Microchip number cannot exceed 50 characters")
        String microchip,

        Long breedId,

        @Nullable LocalDate newEuEntryDate,
        @Nullable LocalDate newEuExitDate
) {
}
