package com.petconnect.backend.record.domain.repository;

import com.petconnect.backend.record.domain.model.Record;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;


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
     * Deletes all Record entities associated with the specified Pet ID.
     * This is useful for cleanup operations when a Pet is deleted or during test setup/teardown.
     * As this is a modifying operation, it should be executed within a transaction.
     *
     * @param petId The ID of the Pet whose records should be deleted.
     */
    @Modifying
    void deleteAllByPetId(Long petId);
}
