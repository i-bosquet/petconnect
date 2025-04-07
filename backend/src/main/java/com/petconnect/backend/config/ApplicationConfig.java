package com.petconnect.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Application-wide configuration beans, including security components
 * not directly tied to HttpSecurity configuration.
 *
 * @author ibosquet
 */
@Configuration
public class ApplicationConfig {

    /**
     * Provides the PasswordEncoder bean using BCrypt.
     *
     * @return A BCryptPasswordEncoder instance.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
