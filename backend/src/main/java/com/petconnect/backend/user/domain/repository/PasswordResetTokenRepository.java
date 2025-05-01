package com.petconnect.backend.user.domain.repository;

import com.petconnect.backend.user.domain.model.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link PasswordResetToken} entities.
 * Provides CRUD operations and a method to find a token by its unique string value.
 *
 * @author ibosquet
 */
@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    /**
     * Finds a PasswordResetToken entity by its unique token string.
     *
     * @param token The token string to search for.
     * @return An {@link Optional} containing the found token entity, or empty if not found.
     */
    Optional<PasswordResetToken> findByToken(String token);
}
