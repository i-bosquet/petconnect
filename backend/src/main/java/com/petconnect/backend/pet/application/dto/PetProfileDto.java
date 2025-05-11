package com.petconnect.backend.pet.application.dto;

import com.petconnect.backend.pet.domain.model.Gender;
import com.petconnect.backend.pet.domain.model.PetStatus;
import com.petconnect.backend.pet.domain.model.Specie;
import com.petconnect.backend.user.application.dto.OwnerSummaryDto;
import com.petconnect.backend.user.application.dto.VetSummaryDto;

import java.time.LocalDate;
import java.util.Set;

/**
 * Data Transfer Object representing the complete profile of a pet within the system.
 * Includes a combination of owner-provided data, clinic-verified details, and associated metadata.
 *
 * @param id The unique identifier of the pet.
 * @param name The name of the pet.
 * @param specie The species of the pet (e.g., Dog, Cat, Rabbit, etc.).
 * @param color The color description of the pet.
 * @param gender The gender of the pet.
 * @param birthDate The date of birth of the pet.
 * @param microchip The microchip number of the pet.
 * @param image The URL or path to the pet's image on the system.
 * @param status The current status of the pet (e.g., Pending, Active, Inactive).
 * @param ownerId The unique identifier of the owner linked to the pet.
 * @param ownerUsername The username of the pet owner.
 * @param breedId The unique identifier of the breed associated with the pet.
 * @param breedName The descriptive name of the breed associated with the pet.
 * @param pendingActivationClinicId The unique ID of the clinic handling the pending activation of the pet (if applicable).
 * @param pendingActivationClinicName The name of the clinic handling the pending activation of the pet (if applicable).
 * @param associatedVets A set of veterinarians associated with the pet, providing summary details.
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
        String pendingActivationClinicName,
        Set<VetSummaryDto> associatedVets,
        OwnerSummaryDto ownerDetails
) {
}

