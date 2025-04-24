package com.petconnect.backend.pet.application.dto;

import com.petconnect.backend.pet.domain.model.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Data Transfer Object containing information required or verified by Clinic Staff
 * when activating a Pet's status from PENDING to ACTIVE.
 * Fields here are generally considered mandatory for activation.
 *
 * @param color Verified/updated color description (max 50 chars). Can be blank if none.
 * @param gender Verified/updated gender. Cannot be null.
 * @param birthDate Verified/updated birthdate. Cannot be null, must be past/present.
 * @param microchip Verified/updated microchip number. Cannot be blank, max 50 chars must be unique.
 * @param breedId ID of the verified/updated breed. Can be null if confirmed mixed/unknown.
 *
 * @author ibosquet
 */
public record PetActivationDto(
        @NotBlank(message = "Pet name cannot be blank")
        @Size(max = 50) String name,

        @NotBlank(message = "Color description cannot be blank")
        @Size(max = 30) String color,

        @NotNull(message = "Gender must be specified")
        Gender gender,

        @NotNull(message = "Birth date must be specified")
        @PastOrPresent(message = "Birth date must be in the past or present")
        LocalDate birthDate,

        @NotBlank(message = "Microchip number cannot be blank")
        @Size(max = 50) String microchip,

        @NotNull(message = "Breed must be specified")
        Long breedId,

        @NotBlank(message = "Image path/URL cannot be blank")
        String image
) {
}
