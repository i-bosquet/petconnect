package com.petconnect.backend.pet.domain.repository;

import com.petconnect.backend.pet.domain.model.Breed;
import com.petconnect.backend.pet.domain.model.Specie;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Breed} entities.
 * Provides CRUD operations and methods for finding breeds by species.
 *
 * @author ibosquet
 */
@Repository
public interface BreedRepository extends JpaRepository<Breed, Long> {

    /**
     * Finds all breeds belonging to a specific species, ordered by name ascending.
     * Useful for populating dropdown lists based on selected species.
     *
     * @param specie The species to filter breeds by.
     * @return A list of {@link Breed} objects for the given species, sorted by name.
     */
    List<Breed> findBySpecieOrderByNameAsc(@NotNull Specie specie);

    /**
     * Finds a specific breed by its name and species.
     * Used primarily for finding fallback breeds like "Mixed/Other".
     *
     * @param name The exact name of the breed (case-sensitive depending on DB collation).
     * @param specie The species the breed belongs to.
     * @return An Optional containing the Breed if found, empty otherwise.
     */
    Optional<Breed> findByNameAndSpecie(String name, Specie specie);
}
