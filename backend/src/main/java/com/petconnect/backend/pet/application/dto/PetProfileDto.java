package com.petconnect.backend.pet.application.dto;

import com.petconnect.backend.pet.domain.model.Gender;
import com.petconnect.backend.pet.domain.model.PetStatus;
import com.petconnect.backend.pet.domain.model.Specie;
import com.petconnect.backend.user.application.dto.VetSummaryDto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * Data Transfer Object representing the full profile of a Pet,
 * including details about its owner, breed, and associated veterinarians.
 * Used for displaying detailed pet information.
 *
 * @param id The unique identifier of the pet.
 * @param name The name of the pet.
 * @param specie The species of the pet (derived from Breed).
 * @param color Optional color description.
 * @param gender Optional gender of the pet.
 * @param birthDate Optional date of birth.
 * @param microchip Optional microchip number.
 * @param image URL/path to the pet's image (will always have a value, potentially default).
 * @param status The current status (PENDING, ACTIVE, INACTIVE).
 * @param ownerId The ID of the owner user.
 * @param ownerUsername The username of the owner.
 * @param breedId Optional ID of the breed.
 * @param breedName Optional name of the breed.
 * @param pendingActivationClinicId Optional ID of the clinic where activation is pending.
 * @param associatedVets A set containing summary information of veterinarians associated with the pet. // NUEVO
 * @param createdAt Timestamp of creation (from BaseEntity).
 * @param updatedAt Timestamp of last update (from BaseEntity).
 *
 * @author ibosquet
 */
public record PetProfileDto(
        Long id,
        String name,
        Specie specie,
        String color,
        Gender gender,
        LocalDate birthDate,
        String microchip,
        String image,
        PetStatus status,
        Long ownerId,
        String ownerUsername,
        Long breedId,
        String breedName,
        Long pendingActivationClinicId,
        Set<VetSummaryDto> associatedVets,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

