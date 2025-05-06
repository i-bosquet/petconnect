package com.petconnect.backend.common.helper;

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
public class UserHelper {

    private final UserRepository userRepository;

    /**
     * Retrieves the UserEntity corresponding to the currently authenticated user.
     * Fetches user details from the database based on the ID obtained
     * from the validated security context's principal.
     *
     * @return The authenticated UserEntity.
     * @throws IllegalStateException if no valid authenticated user is found in the security context.
     * @throws EntityNotFoundException if the authenticated user ID does not match any user in the database.
     */
    public UserEntity getAuthenticatedUserEntity() {
        Long currentUserId = getAuthenticatedUserId();
        return userRepository.findById(currentUserId)
                .orElseThrow(() -> new EntityNotFoundException("Authenticated user with ID '" + currentUserId + "' not found in database."));
    }

    /**
     * Retrieves the ID (Long) of the currently authenticated user
     * from the Spring Security context's principal.
     *
     * @return The ID Long of the authenticated user.
     * @throws IllegalStateException if the security context does not contain a valid,
     *         authenticated principal, that can be parsed as a Long user ID.
     */
    public Long getAuthenticatedUserId() {
        Object principal = getObject();

        try {
            return switch (principal) {
                case Long l -> l;
                case String s -> Long.parseLong(s);
                case Number number -> number.longValue();
                default ->
                        throw new IllegalStateException("Authenticated principal is not a recognizable User ID type: " + principal.getClass());
            };
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Authenticated principal cannot be parsed into a User ID (Long): " + principal);
        }
    }

    private static Object getObject() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null ||
                !authentication.isAuthenticated() ||
                authentication instanceof AnonymousAuthenticationToken ||
                authentication.getPrincipal() == null) {
            throw new IllegalStateException("Authentication required. No authenticated user principal found in security context.");
        }

        return authentication.getPrincipal();
    }
}
