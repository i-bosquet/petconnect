package com.petconnect.backend.pet.application.dto;

import com.petconnect.backend.pet.domain.model.Gender;
import com.petconnect.backend.pet.domain.model.PetStatus;
import com.petconnect.backend.pet.domain.model.Specie;
import com.petconnect.backend.user.application.dto.OwnerSummaryDto;
import com.petconnect.backend.user.application.dto.VetSummaryDto;

import java.time.LocalDate;
import java.util.Set;

/**
 * Data Transfer Object representing the detailed profile of a pet within the system.
 * This record encapsulates information about the pet, its owner, current status,
 * associated veterinarians, pending activations, and other relevant details.
 * Primarily used for displaying the full information of a pet in the system.
 *
 * @param id                           The unique identifier of the pet.
 * @param name                         The name of the pet.
 * @param specie                       The species classification of the pet (e.g., DOG, CAT).
 * @param color                        The pet's predominant color.
 * @param gender                       The gender of the pet (e.g., MALE, FEMALE).
 * @param birthDate                    The pet's date of birth.
 * @param microchip                    The microchip number associated with the pet.
 * @param image                        The URL/path to the pet's image.
 * @param status                       The current lifecycle status of the pet (e.g., PENDING, ACTIVE).
 * @param ownerId                      The unique ID of the pet's owner.
 * @param ownerUsername                The username of the pet's owner.
 * @param breedId                      The unique ID of the pet's breed.
 * @param breedName                    The name of the pet's breed.
 * @param pendingActivationClinicId    The unique ID of the clinic where the pet's activation is pending (if any).
 * @param pendingActivationClinicName  The name of the clinic where the pet's activation is pending (if any).
 * @param associatedVets               A set of summaries for veterinarians associated with the pet.
 * @param ownerDetails                 A summary of the owner's information, including username and contact details.
 * @param canRequestAhcCertificate     Flag indicating whether the pet is eligible for requesting an AHC certificate.
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
        String pendingActivationClinicName,
        Set<VetSummaryDto> associatedVets,
        OwnerSummaryDto ownerDetails,
        boolean canRequestAhcCertificate
) {
}

