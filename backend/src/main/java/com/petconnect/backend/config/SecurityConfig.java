package com.petconnect.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petconnect.backend.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * Configures Spring Security settings including filter chain, authentication providers,
 * and entry points for handling authentication errors.
 * CSRF protection is disabled, which is common for stateless REST APIs.
 * Session management is set to STATELESS.
 *
 * @author ibosquet
 */
@Configuration
@EnableWebSecurity // Enables Spring Security's web security support
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtUtils jwtUtils;
    private final UserDetailsService userDetailsService;
    private final ObjectMapper objectMapper;


    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_VET = "VET";
    public static final String ROLE_OWNER = "OWNER";
    public static final String MESSAGE = "message";


    /**
     * Defines the security filter chain that applies to HTTP requests.
     * Configures CSRF, session management, authorization rules, JWT filter,
     * and a custom AuthenticationEntryPoint for handling authentication failures
     *
     * @param httpSecurity The HttpSecurity object to configure.
     * @return The configured SecurityFilterChain.
     * @throws Exception If an error occurs during configuration.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        return httpSecurity
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(http -> {
                    // --- PUBLIC ENDPOINTS ---
                    // Authentication (Login/Register)
                    http.requestMatchers("/api/auth/**").permitAll();
                    // Public Clinic Search & Details
                    http.requestMatchers(HttpMethod.GET, "/api/clinics").permitAll();
                    http.requestMatchers(HttpMethod.GET, "/api/clinics/{id}").permitAll();
                    // API Documentation
                    http.requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/api-docs/**").permitAll();
                    // Static Resources (Images) - Allow GET requests
                    http.requestMatchers(HttpMethod.GET, "/images/**", "/uploaded-images/**").permitAll();


                    // --- AUTHENTICATED USER ENDPOINTS (Specific Roles Checked Later/in Service) ---
                    // Get Own Profile
                    http.requestMatchers(HttpMethod.GET, "/api/users/me").authenticated(); // Any authenticated user can get their own profile

                    // --- ROLE-SPECIFIC ENDPOINTS ---
                    // Owner updating own profile
                    http.requestMatchers(HttpMethod.PUT, "/api/users/me").hasRole(ROLE_OWNER);
                    // Staff (Admin/Vet) updating own common profile info
                    http.requestMatchers(HttpMethod.PUT, "/api/users/me/staff").hasAnyRole(ROLE_ADMIN, ROLE_VET);

                    // Admin managing their own clinic staff
                    http.requestMatchers(HttpMethod.POST, "/api/staff").hasRole(ROLE_ADMIN);
                    http.requestMatchers(HttpMethod.PUT, "/api/staff/{staffId}").hasRole(ROLE_ADMIN);
                    http.requestMatchers(HttpMethod.PUT, "/api/staff/{staffId}/activate").hasRole(ROLE_ADMIN);
                    http.requestMatchers(HttpMethod.PUT, "/api/staff/{staffId}/deactivate").hasRole(ROLE_ADMIN);

                    // Admin updating their own clinic details
                    http.requestMatchers(HttpMethod.PUT, "/api/clinics/{id}").hasRole(ROLE_ADMIN);

                    // Staff (Admin/Vet) viewing staff list for their clinic
                    // Note: Further check in service ensures it's THEIR clinic
                    http.requestMatchers(HttpMethod.GET, "/api/clinics/{clinicId}/staff/all").hasAnyRole(ROLE_ADMIN, ROLE_VET);
                    http.requestMatchers(HttpMethod.GET, "/api/clinics/{clinicId}/staff/active").hasAnyRole(ROLE_ADMIN, ROLE_VET);

                    // Admin/Superuser viewing specific users by ID/Email (Example - Protected stronger)
                    http.requestMatchers(HttpMethod.GET, "/api/users/{id}").hasAnyRole(ROLE_ADMIN);
                    http.requestMatchers(HttpMethod.GET, "/api/users/by-email").hasAnyRole(ROLE_ADMIN);

                    // --- DEFAULT RULE ---
                    // Any other request not specifically matched above requires authentication
                    http.anyRequest().authenticated();
                })
                .addFilterBefore(new JwtTokenFilter(jwtUtils), UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(exceptions -> exceptions
                                .authenticationEntryPoint(customAuthenticationEntryPoint())
                                .accessDeniedHandler(customAccessDeniedHandler())
                )
                .build();
    }

    /**
     * Provides the AuthenticationManager bean using the provided configuration.
     *
     * @param authenticationConfiguration The authentication configuration.
     * @return The AuthenticationManager.
     * @throws Exception If an error occurs during configuration.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)throws Exception  {
        return authenticationConfiguration.getAuthenticationManager();
    }

    /**
     * Configures the AuthenticationProvider.
     *
     * @return The configured AuthenticationProvider.
     */
    @Bean
    public AuthenticationProvider authenticationProvider(PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setPasswordEncoder(passwordEncoder);
        provider.setUserDetailsService(userDetailsService);
        return provider;
    }

    /**
     * Creates a custom AuthenticationEntryPoint bean.
     * This entry point is invoked when an unauthenticated user tries to access a protected resource
     * or when authentication fails (e.g., bad credentials during login attempt handled by DaoAuthenticationProvider).
     * It returns a 401 Unauthorized response with a JSON error message.
     *
     * @return The custom AuthenticationEntryPoint.
     */
    @Bean
    public AuthenticationEntryPoint customAuthenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);

            Map<String, Object> body = new HashMap<>();
            body.put("timestamp", System.currentTimeMillis());
            body.put("status", HttpStatus.UNAUTHORIZED.value());
            body.put("error", "Unauthorized");

            // Provide a more specific message for bad credentials
            if (authException instanceof BadCredentialsException) {
                body.put(MESSAGE, "Invalid username or password.");
            } else {
                body.put(MESSAGE, authException.getMessage()); // Use message from the exception
            }

            body.put("path", request.getRequestURI());

            // Use ObjectMapper to write the JSON response
            objectMapper.writeValue(response.getWriter(), body);
        };
    }

    /**
     * Creates a custom AccessDeniedHandler bean.
     * This handler is invoked when an authenticated user tries to access a resource
     * they do not have permission for (resulting in AccessDeniedException).
     * It returns a 403 Forbidden response with a standardized JSON error body.
     *
     * @return The custom AccessDeniedHandler.
     */
    @Bean
    public AccessDeniedHandler customAccessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);

            Map<String, Object> body = new HashMap<>();
            body.put("timestamp", System.currentTimeMillis());
            body.put("status", HttpStatus.FORBIDDEN.value());
            body.put("error", "Forbidden");
            body.put(MESSAGE, "Access Denied: You do not have the required permissions to access this resource.");
            body.put("path", request.getRequestURI());

            objectMapper.writeValue(response.getWriter(), body);
        };
    }
}