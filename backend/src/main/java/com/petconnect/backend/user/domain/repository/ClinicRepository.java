package com.petconnect.backend.user.domain.repository;

import com.petconnect.backend.user.domain.model.Clinic;
import com.petconnect.backend.user.domain.model.Country;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for {@link Clinic} entities.
 * Provides standard CRUD (Create, Read, Update, Delete) operations inherited from JpaRepository.
 * Additionally, it extends {@link JpaSpecificationExecutor}. This allows for the creation
 * of dynamic, type-safe queries in the service layer using the JPA Criteria API.
 * This is particularly useful for implementing complex search functionality, such as
 * filtering clinics based on multiple optional criteria like name, city, or country,
 * without needing to define a repository method for every possible combination of filters.
 *
 * @see org.springframework.data.jpa.domain.Specification
 * @author ibosquet
 */
@Repository
public interface ClinicRepository extends JpaRepository<Clinic, Long>, JpaSpecificationExecutor<Clinic> {
    /**
     * Finds all distinct countries where clinics are registered, ordered alphabetically.
     *
     * @return A List of distinct Country enum values present in the clinic table.
     */
    @Query("SELECT DISTINCT c.country FROM Clinic c ORDER BY c.country ASC") //
    List<Country> findDistinctCountries();
}
