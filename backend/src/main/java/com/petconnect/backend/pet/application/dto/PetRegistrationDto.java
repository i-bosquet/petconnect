package com.petconnect.backend.pet.application.dto;

import com.petconnect.backend.pet.domain.model.Gender;
import com.petconnect.backend.pet.domain.model.Specie;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Data Transfer Object for registering a new Pet by an Owner.
 * Contains the initial basic information provided by the owner.
 * Other details (microchip, birthdate, etc.) are typically added later by clinic staff.
 *
 * @param name The chosen name for the pet. Cannot be blank.
 * @param specie The species of the pet. Cannot be null.
 * @param image Optional: URL or identifier for a user-uploaded image for the pet. If null or blank, a default will be assigned based on species/breed.
 * @param breedId Optional: The ID of the selected Breed for the pet. If null, it implies mixed or unknown breed initially.
 *
 * @author ibosquet
 */
public record PetRegistrationDto(
        @NotBlank(message = "Pet name cannot be blank")
        @Size(max = 50, message = "Pet name cannot exceed 50 characters")
        String name,

        @NotNull(message = "Pet species cannot be null")
        Specie specie,

        @NotNull(message = "Pet birth date cannot be null")
        @PastOrPresent(message = "Birth date must be in the past or present")
        LocalDate birthDate,

        /// Optional fields owner might provide
        Long breedId,

        String image,

        @Size(max = 30) String color,

        Gender gender,

        @Size(max = 50) String microchip
) {
}
