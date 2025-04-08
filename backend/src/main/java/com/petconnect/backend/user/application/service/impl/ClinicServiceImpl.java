package com.petconnect.backend.user.application.service.impl;

import com.petconnect.backend.exception.EntityNotFoundException;
import com.petconnect.backend.user.application.dto.ClinicDto;
import com.petconnect.backend.user.application.dto.ClinicUpdateDto;
import com.petconnect.backend.user.application.mapper.ClinicMapper;
import com.petconnect.backend.user.application.service.ClinicService;
import com.petconnect.backend.user.domain.model.Clinic;
import com.petconnect.backend.user.domain.model.ClinicStaff;
import com.petconnect.backend.user.domain.model.RoleEnum;
import com.petconnect.backend.user.domain.model.UserEntity;
import com.petconnect.backend.user.domain.repository.ClinicRepository;
import com.petconnect.backend.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.criteria.Predicate;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of the {@link ClinicService}  interface.
 * Handles the business logic for retrieving and updating Clinic entities.
 * Uses ClinicRepository for data access and ClinicMapper for DTO conversion.
 * Operations are transactional.
 *
 * @author ibosquet
 */
@Service
@RequiredArgsConstructor
public class ClinicServiceImpl implements ClinicService {
    // Dependencies injected via constructor (final fields + @RequiredArgsConstructor)
    private final ClinicRepository clinicRepository;
    private final ClinicMapper clinicMapper;
    private final UserRepository userRepository;

    /**
     * {@inheritDoc}
     * Uses ClinicSpecification to build dynamic query based on filters.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<ClinicDto> findClinics(String name, String city, String country, Pageable pageable) {
        // Create a specification based on the filter criteria
        Specification<Clinic> spec = filterBy(name, city, country);

        // Execute the query with the specification and pagination
        Page<Clinic> clinicPage = clinicRepository.findAll(spec, pageable);

        // Map the Page<Clinic> to Page<ClinicDto>
        return clinicPage.map(clinicMapper::toDto);
    }

    /**
     * {@inheritDoc}
     * Finds a clinic by ID or throws an exception if not found.
     * Maps the found entity to a DTO.
     * This operation is read-only.
     */
    @Override
    @Transactional(readOnly = true)
    public ClinicDto findClinicById(Long id) {
        Clinic clinic = clinicRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Clinic not found with id: " + id));
        return clinicMapper.toDto(clinic);
    }

    /**
     * {@inheritDoc}
     * Finds the clinic by ID, updates its properties using the mapper based on the DTO,
     * saves the changes, and returns the updated DTO.
     * This operation requires a transaction to ensure atomicity.
     * Authorization checks (e.g., if the current user is an ADMIN of this clinic)
     * would typically be added here or via Spring Security annotations.
     */
    @Override
    @Transactional // Default transaction (read-write)
    public ClinicDto updateClinic(Long id, ClinicUpdateDto clinicUpdateDTO, Long updatingAdminId) {

        // Find the admin performing the action
        UserEntity adminUser = userRepository.findById(updatingAdminId)
                .orElseThrow(() -> new EntityNotFoundException("Admin user not found with id: " + updatingAdminId));

        // Verify user is ClinicStaff and has ADMIN role
        if (!(adminUser instanceof ClinicStaff adminStaff) ||
                adminStaff.getRoles().stream().noneMatch(role -> role.getRoleEnum() == RoleEnum.ADMIN)) {
            throw new AccessDeniedException("User " + updatingAdminId + " is not authorized to update clinic information.");
        }

        // Find the existing clinic to update
        Clinic existingClinic = clinicRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Clinic not found with id: " + id));

        // Authorization Check: Ensure admin belongs to the clinic being updated
        // Make sure clinic is loaded if lazy
        Clinic adminClinic = adminStaff.getClinic();
        if (adminClinic == null || !adminClinic.getId().equals(existingClinic.getId())) {
            throw new AccessDeniedException("Admin user " + updatingAdminId + " is not authorized to update clinic " + id + ".");
        }

        // Map updates from DTO to the existing entity
        clinicMapper.updateFromDto(clinicUpdateDTO, existingClinic);

        // Map the updated entity back to DTO
        return clinicMapper.toDto(clinicRepository.save(existingClinic));
    }


    /**
     * Creates a Specification<Clinic> based on optional filter criteria.
     * Filters are combined using AND logic. String comparisons are case-insensitive and use LIKE '%value%'.
     *
     * @param name    Optional name fragment to filter by.
     * @param city    Optional city name to filter by.
     * @param country Optional country name to filter by.
     * @return A Specification<Clinic> representing the combined filter criteria.
     */
    public static Specification<Clinic> filterBy(String name, String city, String country) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Add predicate for name if provided
            if (StringUtils.hasText(name)) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), "%" + name.toLowerCase() + "%"));
            }

            // Add predicate for city if provided
            if (StringUtils.hasText(city)) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("city")), "%" + city.toLowerCase() + "%"));
            }

            // Add predicate for country if provided
            if (StringUtils.hasText(country)) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("country")), "%" + country.toLowerCase() + "%"));
            }

            // Combine all predicates with AND
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
