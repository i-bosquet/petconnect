package com.petconnect.backend.pet.domain.repository;

import com.petconnect.backend.pet.domain.model.Breed;
import com.petconnect.backend.pet.domain.model.Specie;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

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
}
