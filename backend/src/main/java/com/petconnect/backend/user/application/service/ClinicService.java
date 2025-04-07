package com.petconnect.backend.user.application.service;

import com.petconnect.backend.user.application.dto.ClinicDto;
import com.petconnect.backend.user.application.dto.ClinicUpdateDto;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

/**
 * Service interface for managing Clinic entities.
 * Defines operations for retrieving and updating clinic information.
 * Clinic creation is handled separately.
 *
 * @author ibosquet
 */
public interface ClinicService {
    /**
     * Retrieves a paginated and potentially filtered list of registered clinics.
     * Supports filtering by name, city, and country.
     *
     * @param name Optional name fragment to filter by (case-insensitive).
     * @param city Optional city to filter by (case-insensitive).
     * @param country Optional country to filter by (case-insensitive).
     * @param pageable Pagination information (page number, size, sort).
     * @return A Page containing {@link ClinicDto} objects matching the criteria.
     */
    Page<ClinicDto> findClinics(String name, String city, String country, Pageable pageable);

    /**
     * Retrieves a specific clinic by its unique identifier.
     *
     * @param id The ID of the clinic to retrieve.
     * @return The {@link ClinicDto} of the found clinic.
     * @throws EntityNotFoundException if no clinic is found with the given ID.
     */
    ClinicDto findClinicById(Long id);

    /**
     * Updates the details of an existing clinic.
     * Requires the calling user to be an ADMIN associated with the clinic being updated.
     *
     * @param id The ID of the clinic to update.
     * @param clinicUpdateDTO DTO containing the fields to update.
     * @param updatingAdminId The ID of the ADMIN user performing the update. // ADDED PARAMETER
     * @return The updated {@link ClinicDto}.
     * @throws EntityNotFoundException if no clinic or admin user is found with the given IDs.
     * @throws AccessDeniedException if the user is not an authorized Admin for this clinic. // ADDED EXCEPTION
     */
    ClinicDto updateClinic(Long id, ClinicUpdateDto clinicUpdateDTO, Long updatingAdminId);
}
