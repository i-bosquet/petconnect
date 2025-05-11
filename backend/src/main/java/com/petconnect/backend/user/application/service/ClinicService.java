package com.petconnect.backend.user.application.service;

import com.petconnect.backend.user.application.dto.ClinicDto;
import com.petconnect.backend.user.application.dto.ClinicUpdateDto;
import com.petconnect.backend.user.application.dto.VetSummaryDto;
import com.petconnect.backend.user.domain.model.Country;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.Nullable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

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
     * Updates the specified clinic's details based on the provided update data and optional public key file.
     * The operation is performed under the authority of an admin user identified by their ID.
     *
     * @param id The unique identifier of the clinic to update.
     * @param clinicUpdateDTO An object containing the updated clinic details, such as name, address, city, country, and phone.
     * @param publicKeyFile An optional file representing the clinic's new public key (nullable).
     * @param updatingAdminId The ID of the admin user performing the update.
     * @return A {@link ClinicDto} object representing the updated clinic information.
     * @throws com.petconnect.backend.exception.EntityNotFoundException if the clinic with the specified ID does not exist.
     * @throws AccessDeniedException if the updating admin does not have sufficient permissions.
     * @throws IllegalArgumentException if the provided update data is invalid.
     */
    ClinicDto updateClinic(Long id, ClinicUpdateDto clinicUpdateDTO, @Nullable MultipartFile publicKeyFile, Long updatingAdminId);

    /**
     * Retrieves a distinct list of countries where clinics currently exist.
     *
     * @return A list of Country enum values, ordered alphabetically.
     */
    List<Country> getDistinctClinicCountries();

    /**
     * Retrieves the clinic's public key file as a Resource for downloading.
     * Includes authorization checks.
     *
     * @param clinicId The ID of the clinic.
     * @param requesterUserId The ID of the user requesting the download.
     * @return The public key file as a Resource.
     * @throws com.petconnect.backend.exception.EntityNotFoundException if a clinic or key file isn't found.
     * @throws org.springframework.security.access.AccessDeniedException if requester is not authorized staff.
     * @throws java.io.FileNotFoundException if the physical file is missing despite a path existing.
     */
    Resource getClinicPublicKeyResource(Long clinicId, Long requesterUserId) throws IOException;

    /**
     * Retrieves a list of active veterinarians associated with a specific clinic
     * who are available for selection. Each veterinarian is represented in a simplified DTO format.
     *
     * @param clinicId The unique identifier of the clinic for which to retrieve active veterinarians.
     * @return A list of {@code VetSummaryForSelectionDto} objects, each representing an active veterinarian
     *         from the specified clinic. The list will be empty if no active veterinarians are found.
     */
    List<VetSummaryDto> findActiveVetsForSelectionByClinicId(Long clinicId);
}
