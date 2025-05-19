package com.petconnect.backend.user.domain.repository;

import com.petconnect.backend.user.domain.model.RoleEntity;
import com.petconnect.backend.user.domain.model.RoleEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link RoleEntity} entities.
 * Provides CRUD operations and a custom method for finding a RoleEntity by its RoleEnum.
 *
 * @author ibosquet
 */
@Repository
public interface RoleRepository extends JpaRepository<RoleEntity, Long> {

    /**
     * Finds a RoleEntity based on its RoleEnum value.
     *
     * @param roleEnum the enum value representing the role
     * @return an Optional containing the RoleEntity if found, or empty otherwise
     */
    Optional<RoleEntity> findByRoleEnum(RoleEnum roleEnum);
}
