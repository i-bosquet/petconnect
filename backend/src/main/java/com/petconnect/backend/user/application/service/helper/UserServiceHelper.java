package com.petconnect.backend.user.application.service.helper;

import com.petconnect.backend.exception.EntityNotFoundException;
import com.petconnect.backend.user.domain.model.UserEntity;
import com.petconnect.backend.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.security.authentication.AnonymousAuthenticationToken;

/**
 * Helper component to retrieve information about the currently authenticated user
 * from the Spring Security context.
 * Provides methods to get the user entity or just their ID.
 *
 * @author ibosquet
 */
@Component
@RequiredArgsConstructor
public class UserServiceHelper {

    private final UserRepository userRepository;

    /**
     * Retrieves the UserEntity corresponding to the currently authenticated user.
     * Fetches user details from the database based on the identifier obtained
     * from the validated security context.
     *
     * @return The authenticated UserEntity.
     * @throws IllegalStateException if no valid authenticated user is found in the security context.
     * @throws EntityNotFoundException if the authenticated user identifier does not match any user in the database.
     */
    public UserEntity getAuthenticatedUserEntity() {
        // Get the identifier (username/email) from the authenticated principal
        String currentUserIdentifier = getAuthenticatedUserIdentifier();

        // Find the user entity in the repository using the identifier
        return userRepository.findByEmail(currentUserIdentifier)
                .or(() -> userRepository.findByUsername(currentUserIdentifier))
                .orElseThrow(() -> new EntityNotFoundException("Authenticated user '" + currentUserIdentifier + "' not found in database."));
    }

    /**
     * Retrieves the ID of the currently authenticated user.
     * Convenience method using getAuthenticatedUserEntity().
     *
     * @return The ID of the authenticated user.
     * @throws IllegalStateException or EntityNotFoundException if the user cannot be retrieved.
     */
    public Long getAuthenticatedUserId() {
        return getAuthenticatedUserEntity().getId();
    }

    // --- PRIVATE HELPER METHOD ---

    /**
     * Retrieves the identifier (username or email) of the currently authenticated, non-anonymous user
     * from the Spring Security context.
     *
     * @return The identifier String (username/email).
     * @throws IllegalStateException if the security context does not contain a valid,
     *         authenticated, non-anonymous principal name.
     */
    private String getAuthenticatedUserIdentifier() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Check for valid, authenticated, non-anonymous user
        if (authentication == null ||
                !authentication.isAuthenticated() ||
                authentication instanceof AnonymousAuthenticationToken) {
            throw new IllegalStateException("Authentication required. No authenticated user found in security context.");
        }

        // Get principal's name
        String currentUserIdentifier = authentication.getName();
        if (currentUserIdentifier == null) {
            throw new IllegalStateException("Authenticated principal name is null.");
        }

        return currentUserIdentifier;
    }
}
