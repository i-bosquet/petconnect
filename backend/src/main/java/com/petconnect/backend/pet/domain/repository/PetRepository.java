package com.petconnect.backend.pet.domain.repository;

import com.petconnect.backend.pet.domain.model.Pet;
import com.petconnect.backend.pet.domain.model.PetStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * Spring Data JPA repository for {@link Pet} entities.
 * Provides CRUD operations and methods for finding pets based on various criteria.
 * Extends JpaSpecificationExecutor for potential complex dynamic queries.
 *
 * @author ibosquet
 */
@Repository
public interface PetRepository extends JpaRepository<Pet, Long>, JpaSpecificationExecutor<Pet> {


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
     * Finds a list of pets that are pending a certificate at a specific clinic, filtered by status.
     *
     * @param clinicId The ID of the clinic where the pets are pending a certificate.
     * @param status The status of the pets to filter by.
     * @return A list of Pet entities matching the specified clinic and status.
     */
    List<Pet> findByPendingCertificateClinicIdAndStatus(Long clinicId, PetStatus status);

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

    /**
     * Finds a paginated list of distinct pets associated with a specific clinic.
     * Association is defined as either being in PENDING status at the clinic
     * (via {@code pendingActivationClinic}) OR being actively associated with a
     * Veterinarian ({@code associatedVets}) who belongs to that clinic.
     * Uses a custom JPQL query to handle the OR condition across relationships and grouping.
     *
     * @param clinicId The ID of the clinic.
     * @param pageable Pagination information (page number, size, sort order).
     * @return A {@link Page} containing the distinct {@link Pet} entities associated with the clinic.
     */
    @Query("SELECT p FROM Pet p LEFT JOIN p.associatedVets vet " +
            "WHERE p.pendingActivationClinic.id = :clinicId OR vet.clinic.id = :clinicId " +
            "GROUP BY p")
    Page<Pet> findPetsAssociatedWithClinic(@Param("clinicId") Long clinicId, Pageable pageable);

    /**
     * Finds all pets belonging to a specific owner, regardless of their status
     * (includes PENDING, ACTIVE, and INACTIVE).
     *
     * @param ownerId The ID of the owner whose pets are to be retrieved.
     * @return A {@link List} containing all {@link Pet} entities owned by the specified owner.
     */
    List<Pet> findByOwnerId(Long ownerId);
}
