package com.petconnect.backend.record.domain.repository;

import com.petconnect.backend.record.domain.model.Record;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for {@link Record} entities.
 * Provides CRUD operations, pagination/sorting, and custom queries for medical records.
 * Extends JpaSpecificationExecutor for potential dynamic filtering capabilities.
 *
 * @author ibosquet
 */
@Repository
public interface RecordRepository extends JpaRepository<Record, Long>, JpaSpecificationExecutor<Record> {
    /**
     * Finds all records associated with a specific pet, ordered by creation date descending.
     * Allows pagination.
     *
     * @param petId    The ID of the pet whose records are to be retrieved.
     * @param pageable Pagination and sorting information.
     * @return A Page containing the Pet's Records ordered by creation date descending.
     */
    Page<Record> findByPetIdOrderByCreatedAtDesc(Long petId, Pageable pageable);

    /**
     * Finds all medical records associated with any pet belonging to a specific owner.
     * This involves joining through the Pet entity to filter by the owner's ID.
     * Note: This retrieves all records across potentially multiple pets for the owner.
     * Consider performance implications if an owner has many pets with extensive histories.
     * Pagination is not applied here; use with caution or add Pageable if needed.
     *
     * @param ownerId The ID of the owner whose pets' records are to be retrieved.
     * @return A List of all Record entities for all pets owned by the specified owner.
     */
    @Query("SELECT r FROM Record r JOIN r.pet p WHERE p.owner.id = :ownerId")
    List<Record> findByPetOwnerId(@Param("ownerId") Long ownerId);

    /**
     * Deletes all Record entities associated with the specified Pet ID.
     * This is useful for cleanup operations when a Pet is deleted or during test setup/teardown.
     * As this is a modifying operation, it should be executed within a transaction.
     *
     * @param petId The ID of the Pet whose records should be deleted.
     * @return the number of records deleted.
     */
    @Modifying
    long deleteAllByPetId(Long petId); // Método añadido

    /**
     * Checks if a record exists for the given pet ID.
     * @param petId the ID of the pet
     * @return true if a record exists, false otherwise
     */
    boolean existsByPetId(Long petId);
}
