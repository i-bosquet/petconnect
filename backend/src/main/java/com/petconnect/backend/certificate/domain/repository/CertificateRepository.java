package com.petconnect.backend.certificate.domain.repository;

import com.petconnect.backend.certificate.domain.model.Certificate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Certificate} entities.
 * Provides CRUD operations and custom queries for retrieving certificates.
 *
 * @author ibosquet
 */
@Repository
public interface CertificateRepository extends JpaRepository<Certificate, Long> {

    /**
     * Finds all certificates associated with a specific pet, ordered by creation date descending.
     * Useful for listing all certificates issued for a pet.
     *
     * @param petId The ID of the pet.
     * @return A list of {@link Certificate} objects for the given pet, ordered by creation date desc.
     */
    List<Certificate> findByPetIdOrderByCreatedAtDesc(Long petId);

    /**
     * Finds a certificate by its unique official certificate number.
     * Useful for external lookups or checking for duplicates before generation (although DB constraint handles this).
     *
     * @param certificateNumber The official certificate number.
     * @return An Optional containing the {@link Certificate} if found, empty otherwise.
     */
    Optional<Certificate> findByCertificateNumber(String certificateNumber);

    /**
     * Checks if a certificate already exists for a specific medical record.
     * Prevents issuing multiple certificates for the same record event.
     *
     * @param recordId The ID of the medical record (make sure the field name matches the entity: medicalRecord.id).
     * @return true if a certificate associated with this record exists, false otherwise.
     */
    boolean existsByMedicalRecordId(Long recordId);
}
