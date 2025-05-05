package com.petconnect.backend.pet.application.dto;

/**
 * Data Transfer Object representing a simplified view of a Breed,
 * typically used for populating selection lists in the UI.
 *
 * @param id The unique identifier of the breed.
 * @param name The common name of the breed (e.g., "Labrador Retriever").
 *
 * @author ibosquet
 */
public record BreedDto(
        Long id,
        String name,
        String imageUrl
) {
}
