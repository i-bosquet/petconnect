package com.petconnect.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petconnect.backend.security.JwtUtils;
import com.petconnect.backend.user.application.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.*;

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
@Slf4j
public class SecurityConfig {

    private final JwtUtils jwtUtils;
    private final UserService userService;
    private final ObjectMapper objectMapper;

    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_VET = "VET";
    public static final String ROLE_OWNER = "OWNER";
    public static final String MESSAGE = "message";
    public static final String RECORD_ID_URL = "/api/records/{recordId}";

    @Value("${app.frontend.prod.url:#{null}}")
    private String frontendProdUrl;

    @Value("${app.frontend.dev.url:#{null}}")
    private String frontendDevUrl;


    /**
     * Configures the security filter chain for HTTP requests.
     * Sets up CORS, CSRF, session management policies, request authorization rules,
     * JWT token filter, and exception handling mechanisms for the application.
     *
     * @param httpSecurity The HttpSecurity instance to configure security settings.
     * @return The configured SecurityFilterChain instance.
     * @throws Exception If a configuration error occurs.
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
                    http.requestMatchers(HttpMethod.GET, "/api/clinics/countries").permitAll(); // List countries
                    http.requestMatchers(HttpMethod.GET, "/api/clinics/{id}").permitAll(); // Get clinic detail
                    http.requestMatchers(HttpMethod.GET, "/api/records/verify-temporary-access").permitAll(); // Temporal access
                    http.requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/api-docs/**").permitAll(); // API Docs
                    http.requestMatchers(HttpMethod.GET, "/images/**", "/storage/**").permitAll(); // Static Images

                    // --- 2. AUTHENTICATED (ANY ROLE - Fine-grained auth in service) ---
                    http.requestMatchers(HttpMethod.GET, "/api/users/me").authenticated(); // Get own profile (Covered by the specific role rules above, but safe to leave)
                    http.requestMatchers(HttpMethod.GET, "/api/pets/breeds/{specie}").authenticated(); // List breeds
                    http.requestMatchers(HttpMethod.GET, "/api/pets/{petId}").authenticated(); // Get Pet details (Owner or associated Staff)
                    http.requestMatchers(HttpMethod.GET, "/api/records").authenticated(); // List records (requires petId param, checked in service)
                    http.requestMatchers(HttpMethod.POST, "/api/records").authenticated(); // Create a record (checked in service)
                    http.requestMatchers(HttpMethod.GET, RECORD_ID_URL).authenticated(); // Get record detail (checked in service)
                    http.requestMatchers(HttpMethod.PUT, RECORD_ID_URL).authenticated(); // Update record
                    http.requestMatchers(HttpMethod.DELETE, RECORD_ID_URL).authenticated(); // Delete record (checked in service)
                    http.requestMatchers(HttpMethod.GET, "/api/clinics/{clinicId}/vets-for-selection").authenticated(); // List vets by clinic
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
                    http.requestMatchers(HttpMethod.POST, "/api/records/{petId}/temporary-access").hasRole(ROLE_OWNER); // Temp Access
                    http.requestMatchers(HttpMethod.POST, "/api/pets/{petId}/request-certificate/{clinicId}").hasRole(ROLE_OWNER); // Request Cert

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
                    http.requestMatchers(HttpMethod.GET, "/api/clinics/{clinicId}/public-key/download").hasAnyRole(ROLE_ADMIN, ROLE_VET); // Download the public-key file
                    http.requestMatchers(HttpMethod.GET, "/api/records/clinic/{clinicId}/created-by").hasAnyRole(ROLE_ADMIN, ROLE_VET); // List all historic records of the clinic
                    http.requestMatchers(HttpMethod.GET, "/api/pets/{clinicId}/pending-certificate-requests").hasAnyRole(ROLE_ADMIN, ROLE_VET); // List requests for certificates from pets associated with MY clinic
                    http.requestMatchers(HttpMethod.GET, "/api/certificates/clinic/{clinicId}").hasAnyRole(ROLE_ADMIN, ROLE_VET); // List all certificates of the clinic

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

        List<String> allowedOriginsList  = new ArrayList<>();

        if (StringUtils.hasText(frontendDevUrl)) {
            log.debug("CORS: Adding dev frontend URL to allowed origins: {}", frontendDevUrl);
            allowedOriginsList.add(frontendDevUrl);
        }

        if (StringUtils.hasText(frontendProdUrl) && !allowedOriginsList.contains(frontendProdUrl)) {
            log.debug("CORS: Adding prod frontend URL to allowed origins: {}", frontendProdUrl);
            allowedOriginsList.add(frontendProdUrl);
        }

        if (allowedOriginsList.isEmpty()) {
            log.warn("CORS Misconfiguration: No 'allowedOrigins' were determined. " +
                    "Frontend applications might be unable to connect to the API. " +
                    "Please verify 'app.frontend.prod.url' and 'app.frontend.dev.url' in your properties files.");
        }

        configuration.setAllowedOrigins(allowedOriginsList );
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