package com.petconnect.backend.user.domain.repository;

import com.petconnect.backend.user.domain.model.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data JPA repository for base {@link UserEntity} entities.
 * Provides standard CRUD operations and allows finding users by email.
 * As UserEntity uses JOINED inheritance, queries here might involve joins across user type tables.
 *
 * @author ibosquet
 */
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    /**
     * Finds a UserEntity entity by its unique email address.
     * This query works across all user types (Owner, Vet, Admin) due to inheritance.
     *
     * @param email The email address to search for.
     * @return An {@link Optional} containing the found UserEntity, or {@link Optional#empty()} if not found.
     */
    Optional<UserEntity> findByEmail(String email);

    /**
     * Finds a UserEntity entity by its unique username.
     * This query works across all user types (Owner, Vet, Admin) due to inheritance.
     *
     * @param username The username to search for.
     * @return An {@link Optional} containing the found UserEntity, or {@link Optional#empty()} if not found.
     */
    Optional<UserEntity> findByUsername(String username);

    /**
     * Checks if a user with the given email address already exists.
     * More efficient than findByEmail(email).isPresent() if you only need existence check.
     *
     * @param email The email address to check.
     * @return true if a user with the email exists, false otherwise.
     */
    boolean existsByEmail(String email);

    /**
     * Checks if a user with the given username already exists.
     * More efficient than findByUsername(username).isPresent().
     *
     * @param username The username to check.
     * @return true if a user with the username exists, false otherwise.
     */
    boolean existsByUsername(String username);
}
