package com.petconnect.backend.user.domain.repository;

import com.petconnect.backend.user.domain.model.Vet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Vet} entities.
 * Provides CRUD operations and methods specific to veterinarians.
 *
 * @author ibosquet
 */
public interface VetRepository extends JpaRepository<Vet, Long> {

    /**
     * Checks if a Vet with the given license number exists, excluding a specific user ID.
     * Useful for validating uniqueness when updating a license number.
     *
     * @param licenseNumber The license number to check.
     * @param userIdToExclude The ID of the user whose record should be excluded from the check.
     * @return true if another vet with the license number exists, false otherwise.
     */
    boolean existsByLicenseNumberAndIdNot(String licenseNumber, Long userIdToExclude);

    /**
     * Checks if a Vet with the given public key exists.
     * Useful for validating uniqueness when creating a new Vet or checking before update.
     *
     * @param vetPublicKey The public key to check.
     * @return true if a vet with the public key exists, false otherwise.
     */
    boolean existsByVetPublicKey(String vetPublicKey);

    /**
     * Checks if a Vet with the given public key exists, excluding a specific user ID.
     * Useful for validating uniqueness when updating a public key.
     *
     * @param vetPublicKey The public key to check.
     * @param userIdToExclude The ID of the user (Vet) whose record should be excluded from the check.
     * @return true if another vet with the public key exists, false otherwise.
     */
    boolean existsByVetPublicKeyAndIdNot(String vetPublicKey, Long userIdToExclude); // For checking on update

    /**
     * Checks if a Vet with the given license number exists.
     * Useful for validating uniqueness when creating a new Vet.
     *
     * @param licenseNumber The license number to check.
     * @return true if a vet with the license number exists, false otherwise.
     */
    boolean existsByLicenseNumber(String licenseNumber);

    /**
     * Finds the first available Veterinarian associated with a specific clinic.
     * Useful for assigning a default Vet when an Admin activates a pet.
     * The ordering is not guaranteed unless specified (e.g., findFirstByClinicIdOrderByIdAsc).
     *
     * @param clinicId The ID of the clinic.
     * @return An Optional containing the first found Vet, or empty if no Vets exist for the clinic.
     */
    Optional<Vet> findFirstByClinicId(Long clinicId);
}