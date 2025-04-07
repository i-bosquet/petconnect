package com.petconnect.backend.user.domain.repository;

import com.petconnect.backend.user.domain.model.ClinicStaff;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Spring Data JPA repository for {@link ClinicStaff} entities.
 * Provides CRUD operations and methods for finding staff by clinic.
 *
 * @author ibosquet
 */
public interface ClinicStaffRepository extends JpaRepository<ClinicStaff, Long> {

    /**
     * Finds all ClinicStaff members belonging to a specific clinic.
     * Allows filtering by active status.
     *
     * @param clinicId The ID of the clinic.
     * @param isActive The active status to filter by.
     * @return A list of ClinicStaff members matching the criteria.
     */
    List<ClinicStaff> findByClinicIdAndIsActive(Long clinicId, boolean isActive);

    /**
     * Finds all ClinicStaff members belonging to a specific clinic, regardless of active status.
     *
     * @param clinicId The ID of the clinic.
     * @return A list of all ClinicStaff members for the clinic.
     */
    List<ClinicStaff> findByClinicId(Long clinicId);

}
