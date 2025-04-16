package com.petconnect.backend.record.domain.repository;

import com.petconnect.backend.record.domain.model.RecordType;
import com.petconnect.backend.record.domain.model.Record;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

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
     * Finds all records associated with a specific pet and matching a specific type,
     * ordered by creation date descending. Allows pagination.
     *
     * @param petId    The ID of the pet.
     * @param type     The type of record to filter by.
     * @param pageable Pagination and sorting information.
     * @return A Page containing the Pet's Records of the specified type.
     */
    Page<Record> findByPetIdAndTypeOrderByCreatedAtDesc(Long petId, RecordType type, Pageable pageable);

    /**
     * Finds the most recent record of a specific type for a given pet.
     * Useful for checking the date of the last ANNUAL_CHECK or a specific VACCINE.
     *
     * @param petId The ID of the pet.
     * @param type  The type of record to find.
     * @return An Optional containing the most recent Record of that type, or empty if none exist.
     */
    Optional<Record> findFirstByPetIdAndTypeOrderByCreatedAtDesc(Long petId, RecordType type);

    /**
     * Finds all records created by a specific UserEntity ID, ordered by creation date descending.
     * Useful for auditing or specific user views if needed.
     *
     * @param creatorId The ID of the user who created the records.
     * @param pageable  Pagination and sorting information.
     * @return A Page containing Records created by the specified user.
     */
    Page<Record> findByCreatorIdOrderByCreatedAtDesc(Long creatorId, Pageable pageable);
}
