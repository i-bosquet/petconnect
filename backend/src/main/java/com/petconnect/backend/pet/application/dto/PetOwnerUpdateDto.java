package com.petconnect.backend.pet.application.dto;

import com.petconnect.backend.pet.domain.model.Gender;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Data Transfer Object for an Owner updating their Pet's basic information.
 * Allows changing the name and image. Other fields are typically managed by clinic staff.
 * Fields are optional; only non-null values will be considered for update.
 *
 * @param name The new name for the pet (optional, max 50 chars).
 * @param image The new URL/path for the pet's image (optional).
 *
 * @author ibosquet
 */
public record PetOwnerUpdateDto(
        @Size(max = 50, message = "Pet name cannot exceed 100 characters")
        String name,

        String image,

        @Size(max = 30, message = "Color description cannot exceed 50 characters")
        String color,

        Gender gender,

        @PastOrPresent(message = "Birth date must be in the past or present")
        LocalDate birthDate,

        @Size(max = 50, message = "Microchip number cannot exceed 50 characters")
        String microchip,
        Long breedId
) {
}
