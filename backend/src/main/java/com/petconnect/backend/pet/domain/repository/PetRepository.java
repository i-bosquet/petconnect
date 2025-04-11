package com.petconnect.backend.pet.domain.repository;

import com.petconnect.backend.pet.domain.model.Pet;
import com.petconnect.backend.pet.domain.model.PetStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Pet} entities.
 * Provides CRUD operations and methods for finding pets based on various criteria.
 * Extends JpaSpecificationExecutor for potential complex dynamic queries.
 *
 * @author ibosquet
 */
public interface PetRepository extends JpaRepository<Pet, Long>, JpaSpecificationExecutor<Pet> {

    /**
     * Finds a page of pets belonging to a specific owner.
     *
     * @param ownerId The ID of the owner.
     * @param pageable Pagination information.
     * @return A Page of Pet entities.
     */
    Page<Pet> findByOwnerId(Long ownerId, Pageable pageable);

    /**
     * Finds a page of pets belonging to a specific owner, filtered by a list of statuses.
     * Useful for getting ACTIVE and PENDING pets, excluding INACTIVE ones by default.
     *
     * @param ownerId The ID of the owner.
     * @param statuses A collection of PetStatus values to include.
     * @param pageable Pagination information.
     * @return A Page of Pet entities matching the criteria.
     */
    Page<Pet> findByOwnerIdAndStatusIn(Long ownerId, Collection<PetStatus> statuses, Pageable pageable);

    /**
     * Finds a list of pets that are PENDING activation at a specific clinic.
     *
     * @param clinicId The ID of the clinic.
     * @param status The status to filter by (should typically be PetStatus PENDING).
     * @return A List of Pet entities pending activation at the clinic.
     */
    List<Pet> findByPendingActivationClinicIdAndStatus(Long clinicId, PetStatus status);

    /**
     * Finds a pet by its unique microchip number.
     *
     * @param microchip The microchip number.
     * @return An Optional containing the Pet if found.
     */
    Optional<Pet> findByMicrochip(String microchip);

    /**
     * Checks if a pet with the given microchip number exists.
     * More efficient than findByMicrochip(...).isPresent().
     *
     * @param microchip The microchip number.
     * @return true if a pet with this microchip exists, false otherwise.
     */
    boolean existsByMicrochip(String microchip);

    /**
     * Checks if a pet with the given microchip number exists, excluding a specific pet ID.
     * Useful for validating uniqueness when updating a microchip.
     *
     * @param microchip The microchip number to check.
     * @param petIdToExclude The ID of the pet whose record should be excluded.
     * @return true if another pet with the microchip exists, false otherwise.
     */
    boolean existsByMicrochipAndIdNot(String microchip, Long petIdToExclude);

    @Query("SELECT p FROM Pet p LEFT JOIN p.associatedVets vet " +
            "WHERE p.pendingActivationClinic.id = :clinicId OR vet.clinic.id = :clinicId " +
            "GROUP BY p") // Group by to avoid duplicates if you have multiple vets from the same clinic
    Page<Pet> findPetsAssociatedWithClinic(@Param("clinicId") Long clinicId, Pageable pageable);
}
