package com.petconnect.backend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.AuditorAware;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

/**
 * Implementation of AuditorAware interface to provide the current auditor
 * (typically the username of the logged-in user) for JPA Auditing.
 * Returns an empty Optional if no user is authenticated.
 *
 * @author ibosquet
 */
@Slf4j
public class AuditorAwareImpl implements AuditorAware<String> {

    /**
     * Retrieves the current auditor's identifier (username).
     * <p>
     * Fetches the current Authentication object from the SecurityContextHolder.
     * If the authentication exists, is authenticated, and the principal is recognizable,
     * it returns the identifier. Otherwise, it returns an empty Optional.
     * </p>
     *
     * @return An Optional containing the identifier (e.g., username) of the current auditor,
     * or empty if no authenticated user is available.
     */
    @Override
    @NonNull // Optional itself handles nullability
    public Optional<String> getCurrentAuditor() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getPrincipal)
                .map(this::extractIdentifierFromPrincipal);
    }

    /**
     * Extracts the user identifier (username/email) from the principal object.
     * Handles UserDetails and String principals.
     *
     * @param principal The principal object from the Authentication context.
     * @return The identifier as a String, or null if it cannot be extracted.
     */
    private String extractIdentifierFromPrincipal(Object principal) {
        if ("anonymousUser".equals(principal)) {
            return null; // Treat anonymous user as unauthenticated for auditing
        }

        switch (principal) {
            case UserDetails userDetails -> {
                // Standard Spring Security UserDetails interface
                return userDetails.getUsername(); // Usually maps to username or email
                // Standard Spring Security UserDetails interface
            }
            case String s -> {
                // Sometimes the principal might just be the username string
                return s;
                // Sometimes the principal might just be the username string
            }
            case null, default -> {
                // If the principal is of an unknown type, cannot determine the auditor
                assert principal != null;
                log.warn("Unknown principal type for auditing: {}", principal.getClass());
                return null;
            }
        }
    }
}
