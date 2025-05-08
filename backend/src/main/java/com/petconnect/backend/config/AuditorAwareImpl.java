package com.petconnect.backend.config;

import com.petconnect.backend.user.domain.model.UserEntity;
import com.petconnect.backend.user.domain.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.AuditorAware;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
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

    private final UserRepository userRepository;

    public AuditorAwareImpl(com.petconnect.backend.user.domain.repository.UserRepository userRepository) {
        this.userRepository = userRepository;
    }

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
    @NonNull
    public Optional<String> getCurrentAuditor() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .filter(Authentication::isAuthenticated)
                .filter(auth -> !(auth instanceof AnonymousAuthenticationToken))
                .map(Authentication::getPrincipal)
                .flatMap(this::extractAuditorIdentifier)
                .or(() -> Optional.of("system"));
    }

    /**
     * Extracts the user identifier (username/email) from the principal object.
     * Handles UserDetails and String principals.
     *
     * @param principal The principal object from the Authentication context.
     * @return Optional<String> containing the username/email, or empty if not found/identifiable.
     */
    private Optional<String> extractAuditorIdentifier(Object principal) {
        if (principal == null) {
            return Optional.of("system");
        }

        Optional<String> identifierOpt = Optional.empty();

        switch (principal) {
            case UserDetails userDetails -> identifierOpt = Optional.ofNullable(userDetails.getUsername());
            case String principalString -> identifierOpt = Optional.of(principalString).filter(s -> !s.isEmpty());
            case Long userId -> {
                log.debug("AuditorAware: Principal is User ID {}. Looking up username...", userId);
                identifierOpt = userRepository.findById(userId)
                        .map(UserEntity::getUsername);
                if (identifierOpt.isEmpty()) {
                    log.warn("AuditorAware: User with ID {} from principal not found in DB!", userId);
                }
            }
            default -> log.warn("AuditorAware: Unknown principal type for auditing: {}", principal.getClass());
        }

        return identifierOpt;
    }
}
