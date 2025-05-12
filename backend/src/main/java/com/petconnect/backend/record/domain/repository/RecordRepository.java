package com.petconnect.backend.record.domain.repository;

import com.petconnect.backend.record.domain.model.Record;
import com.petconnect.backend.record.domain.model.RecordType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
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
     * Deletes all Record entities associated with the specified Pet ID.
     * This is useful for cleanup operations when a Pet is deleted or during test setup/teardown.
     * As this is a modifying operation, it should be executed within a transaction.
     *
     * @param petId The ID of the Pet whose records should be deleted.
     */
    @Modifying
    void deleteAllByPetId(Long petId);

    /**
     * Finds all signed Rabies vaccine records for a specific pet, ordered most recent first.
     * Filtering for date validity must be done in the service layer.
     *
     * @param petId The ID of the pet.
     * @return A list of signed Rabies vaccine Records, ordered by CreatedAt descending.
     */
    @Query("SELECT r FROM Record r JOIN r.vaccine v " +
            "WHERE r.pet.id = :petId " +
            "AND r.type = com.petconnect.backend.record.domain.model.RecordType.VACCINE " +
            "AND v.isRabiesVaccine = true " +
            "AND r.vetSignature IS NOT NULL " +
            "ORDER BY r.createdAt DESC")
    List<Record> findAllSignedRabiesVaccinesDesc(@Param("petId") Long petId);

    /**
     * Finds all signed checkup records (of specified types) for a specific pet created on or after
     * a certain date, ordered most recent first.
     * Further validation (e.g., if the latest is within 1 year) must be done in the service layer.
     *
     * @param petId         The ID of the pet.
     * @param checkupTypes  A collection of RecordType considered as checkups.
     * @param cutoffDateTime The earliest creation timestamp (inclusive).
     * @return A list of signed checkup Records after the cutoff date, ordered by CreatedAt descending.
     */
    @Query("SELECT r FROM Record r " +
            "WHERE r.pet.id = :petId " +
            "AND r.type IN :checkupTypes " +
            "AND r.vetSignature IS NOT NULL " +
            "AND r.createdAt >= :cutoffDateTime " +
            "ORDER BY r.createdAt DESC")
    List<Record> findSignedCheckupsAfterDateDesc(
            @Param("petId") Long petId,
            @Param("checkupTypes") Collection<RecordType> checkupTypes,
            @Param("cutoffDateTime") LocalDateTime cutoffDateTime);

    /**
     * Finds all records created within a specific clinic,
     * ordered by creation date descending.
     * Allows pagination.
     *
     * @param clinicId The ID of the clinic where the records were created.
     * @param pageable Pagination and sorting information.
     * @return A Page containing Records created in the specified clinic.
     */
    Page<Record> findByCreatedInClinicIdOrderByCreatedAtDesc(Long clinicId, Pageable pageable);
}
