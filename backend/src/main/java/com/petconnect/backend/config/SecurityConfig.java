package com.petconnect.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petconnect.backend.security.JwtUtils;
import com.petconnect.backend.user.application.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpHeaders;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;
import java.util.List;

/**
 * Configures Spring Security settings including filter chain, authentication providers,
 * and entry points for handling authentication errors.
 * CSRF protection is disabled, which is common for stateless REST APIs.
 * Session management is set to STATELESS.
 *
 * @author ibosquet
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtUtils jwtUtils;
    private final UserService userService;
    private final ObjectMapper objectMapper;


    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_VET = "VET";
    public static final String ROLE_OWNER = "OWNER";
    public static final String MESSAGE = "message";
    public static final String RECORD_ID_URL = "/api/records/{recordId}";


    /**
     * Defines the security filter chain that applies to HTTP requests.
     * Configures CSRF, session management, authorization rules, JWT filter,
     * and custom AuthenticationEntryPoint/AccessDeniedHandler.
     * Order matters: more specific rules should come before more general ones.
     *
     * @param httpSecurity The HttpSecurity object to configure.
     * @return The configured SecurityFilterChain.
     * @throws Exception If an error occurs during configuration.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        return httpSecurity
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(http -> {
                    http.requestMatchers(HttpMethod.OPTIONS, "/api/**").permitAll();
                    // --- 1. PUBLIC ENDPOINTS ---
                    http.requestMatchers("/api/auth/**").permitAll(); // Login/Register
                    http.requestMatchers(HttpMethod.GET, "/api/clinics").permitAll(); // Search clinics
                    http.requestMatchers(HttpMethod.GET, "/api/clinics/{id}").permitAll(); // Get clinic detail
                    http.requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/api-docs/**").permitAll(); // API Docs
                    http.requestMatchers(HttpMethod.GET, "/images/**", "/uploaded-images/**").permitAll(); // Static Images

                    // --- 2. AUTHENTICATED (ANY ROLE - Fine-grained auth in service) ---
                    http.requestMatchers(HttpMethod.GET, "/api/users/me").authenticated(); // Get own profile (Covered by the specific role rules above, but safe to leave)
                    http.requestMatchers(HttpMethod.GET, "/api/pets/breeds/{specie}").authenticated(); // List breeds
                    http.requestMatchers(HttpMethod.GET, "/api/pets/{petId}").authenticated(); // Get Pet details (Owner or associated Staff)
                    http.requestMatchers(HttpMethod.GET, "/api/records").authenticated(); // List records (requires petId param, checked in service)
                    http.requestMatchers(HttpMethod.POST, "/api/records").authenticated(); // Create a record (checked in service)
                    http.requestMatchers(HttpMethod.GET, RECORD_ID_URL).authenticated(); // Get record detail (checked in service)
                    http.requestMatchers(HttpMethod.PUT, RECORD_ID_URL).authenticated(); // Update record
                    http.requestMatchers(HttpMethod.DELETE, RECORD_ID_URL).authenticated(); // Delete record (checked in service)
                    http.requestMatchers(HttpMethod.GET, "/api/certificates").authenticated(); // List certificates (requires petId param, checked in service)
                    http.requestMatchers(HttpMethod.GET, "/api/certificates/{certificateId}").authenticated(); // Get certificate detail (checked in service)
                    http.requestMatchers(HttpMethod.GET, "/api/certificates/{certificateId}/qr-data").authenticated(); // Get QR data (checked in service)

                    // --- 3. ENDPOINTS BY SPECIFIC ROLE ---
                    // --- OWNER ---
                    http.requestMatchers(HttpMethod.PUT, "/api/users/me").hasRole(ROLE_OWNER); // Update own profile
                    http.requestMatchers(HttpMethod.POST, "/api/pets").hasRole(ROLE_OWNER); // Register pet
                    http.requestMatchers(HttpMethod.GET, "/api/pets").hasRole(ROLE_OWNER); // List own pets
                    http.requestMatchers(HttpMethod.PUT, "/api/pets/{petId}/owner-update").hasRole(ROLE_OWNER); // Update own pet
                    http.requestMatchers(HttpMethod.PUT, "/api/pets/{petId}/deactivate").hasRole(ROLE_OWNER); // Deactivate own pet
                    http.requestMatchers(HttpMethod.POST, "/api/pets/{petId}/associate-clinic/{clinicId}").hasRole(ROLE_OWNER); // Associate pet for activation
                    http.requestMatchers(HttpMethod.POST, "/api/pets/{petId}/associate-vet/{vetId}").hasRole(ROLE_OWNER); // Associate vet
                    http.requestMatchers(HttpMethod.DELETE, "/api/pets/{petId}/associate-vet/{vetId}").hasRole(ROLE_OWNER); // Disassociate vet
                    http.requestMatchers(HttpMethod.POST, "/api/records/pet/{petId}/temporary-access").hasRole(ROLE_OWNER); // Temp Access
                    http.requestMatchers(HttpMethod.POST, "/api/pets/{petId}/request-certificate/{vetId}").hasRole(ROLE_OWNER); // Request Cert

                    // --- VET ---
                    http.requestMatchers(HttpMethod.PUT, "/api/pets/{petId}/activate").hasAnyRole(ROLE_VET); // Activate pet
                    http.requestMatchers(HttpMethod.POST, "/api/certificates").hasRole(ROLE_VET); // Generate Certificate

                    // --- ADMIN ---
                    http.requestMatchers(HttpMethod.POST, "/api/staff").hasRole(ROLE_ADMIN); // Create staff
                    http.requestMatchers(HttpMethod.PUT, "/api/staff/{staffId}").hasRole(ROLE_ADMIN); // Update staff
                    http.requestMatchers(HttpMethod.PUT, "/api/staff/{staffId}/activate").hasRole(ROLE_ADMIN); // Activate staff
                    http.requestMatchers(HttpMethod.PUT, "/api/staff/{staffId}/deactivate").hasRole(ROLE_ADMIN); // Deactivate staff
                    http.requestMatchers(HttpMethod.PUT, "/api/clinics/{id}").hasRole(ROLE_ADMIN); // Update an own clinic
                    http.requestMatchers(HttpMethod.GET, "/api/users/{id}").hasRole(ROLE_ADMIN); // Admin viewing specific user by ID
                    http.requestMatchers(HttpMethod.GET, "/api/users/by-email").hasRole(ROLE_ADMIN); // Admin viewing specific user by email

                    // --- ADMIN or VET ---
                    http.requestMatchers(HttpMethod.PUT, "/api/users/me/staff").hasAnyRole(ROLE_ADMIN, ROLE_VET); // Update own staff profile
                    http.requestMatchers(HttpMethod.GET, "/api/clinics/{clinicId}/staff/all").hasAnyRole(ROLE_ADMIN, ROLE_VET); // List all staff in an own clinic
                    http.requestMatchers(HttpMethod.GET, "/api/clinics/{clinicId}/staff/active").hasAnyRole(ROLE_ADMIN, ROLE_VET); // List active staff in an own clinic
                    http.requestMatchers(HttpMethod.PUT, "/api/pets/{petId}/clinic-update").hasAnyRole(ROLE_ADMIN, ROLE_VET); // Update clinical info by staff
                    http.requestMatchers(HttpMethod.GET, "/api/pets/clinic").hasAnyRole(ROLE_ADMIN, ROLE_VET); // List pets associated with MY clinic
                    http.requestMatchers(HttpMethod.GET, "/api/pets/clinic/pending").hasAnyRole(ROLE_ADMIN, ROLE_VET); // List pets pending at MY clinic

                    // --- 4. DEFAULT RULE ---
                    // Any other request requires authentication
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
     * Defines the CORS configuration source.
     * This bean provides the CORS settings (allowed origins, methods, headers)
     * that Spring Security will use.
     *
     * @return CorsConfigurationSource
     */
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:5173"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList(
                HttpHeaders.AUTHORIZATION,
                HttpHeaders.CONTENT_TYPE,
                HttpHeaders.ACCEPT
        ));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
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
        provider.setUserDetailsService(userService::loadUserByUsername);
        return provider;
    }

    /**
     * Creates a custom AuthenticationEntryPoint bean.
     * This entry point is invoked when an unauthenticated user tries to access a protected resource
     * or when authentication fails (e.g., bad credentials during a login attempt handled by DaoAuthenticationProvider).
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
                body.put(MESSAGE, authException.getMessage());
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