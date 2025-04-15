package com.petconnect.backend.pet.application.mapper;

import com.petconnect.backend.pet.application.dto.BreedDto;
import com.petconnect.backend.pet.domain.model.Breed;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Mapper component for converting between {@link Breed} entities and {@link BreedDto}.
 * Handles null checks and list mapping.
 *
 * @author ibosquet
 */
@Component
public class BreedMapper {
    /**
     * Converts a {@link Breed} entity to a {@link BreedDto}.
     * Returns null if the input entity is null.
     *
     * @param breed The Breed entity to convert.
     * @return The corresponding BreedDto, or null if the input was null.
     */
    public BreedDto toDto(Breed breed) {
        if (breed == null) {
            return null;
        }
        return new BreedDto(
                breed.getId(),
                breed.getName()
        );
    }

    /**
     * Converts a list of {@link Breed} entities to an unmodifiable list of {@link BreedDto}.
     * Returns an empty list if the input list is null or empty.
     *
     * @param breeds The list of Breed entities to convert.
     * @return An unmodifiable list of corresponding BreedDto objects.
     */
    public List<BreedDto> toDtoList(List<Breed> breeds) {
        if (breeds == null || breeds.isEmpty()) {
            return Collections.emptyList();
        }
        return breeds.stream()
                .map(this::toDto)
                .toList();
    }
}
