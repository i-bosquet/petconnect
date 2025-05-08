package com.petconnect.backend.config;

import com.petconnect.backend.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Configuration class for JPA Auditing.
 * Defines the AuditorAware bean.
 *
 * @author ibosquet
 */
@Configuration
@EnableJpaAuditing
@RequiredArgsConstructor
public class JpaAuditingConfig {

    private final UserRepository userRepository;

    /**
     * Provides the AuditorAware bean implementation.
     * This bean is used by Spring Data JPA to automatically set createdBy and lastModifiedBy fields.
     *
     * @return An instance of AuditorAwareImpl.
     */
    @Bean
    public AuditorAware<String> auditorProvider() {
        return new AuditorAwareImpl(userRepository);
    }
}
