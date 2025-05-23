package com.petconnect.backend.user.domain.repository;

import com.petconnect.backend.user.domain.model.Owner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link Owner} entities.
 * Provides standard CRUD operations and allows defining custom query methods.
 *
 * @author ibosquet
 */
@Repository
public interface OwnerRepository extends JpaRepository<Owner, Long> {
}
