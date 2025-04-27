package com.petconnect.backend.config;


import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.petconnect.backend.security.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collection;

/**
 * Filter that intercepts HTTP requests to validate and process JWT tokens.
 * Extends OncePerRequestFilter to ensure the filter is executed once per request.
 *
 * @author ibosquet
 */
@RequiredArgsConstructor
@Slf4j
public class JwtTokenFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils; // JWT utility for token validation and extraction

    /**
     * Performs the filtering logic for JWT authentication.
     *
     * @param request     The incoming HttpServletRequest.
     * @param response    The outgoing HttpServletResponse.
     * @param filterChain The filter chain to pass the request along.
     * @throws ServletException If a servlet-related error occurs.
     * @throws IOException      If an I/O error occurs.
     */
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        // Retrieve the JWT token from the Authorization header
        String jwtToken = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (jwtToken != null && jwtToken.startsWith("Bearer ")) {
            // Remove the "Bearer" prefix (assumed to be 7 characters long)
            jwtToken = jwtToken.substring(7);

            try {
                // Validate the JWT token and retrieve the decoded token
                DecodedJWT decodedJWT = jwtUtils.validateToken(jwtToken);

                // Extract the username from the decoded token
                String username = jwtUtils.extractUsername(decodedJWT);

                // Extract the authorities claim as a comma-separated string
                String stringAuthorities = jwtUtils.getSpecificClaim(decodedJWT, "authorities").asString();

                // Convert the comma-separated string into a collection of GrantedAuthority objects
                Collection<? extends GrantedAuthority> authorities =
                        AuthorityUtils.commaSeparatedStringToAuthorityList(stringAuthorities);

                // Create an Authentication object using the username and authorities
                Authentication authentication =
                        new UsernamePasswordAuthenticationToken(username, null, authorities);

                // Set the authentication in the SecurityContext
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("JWT Filter - Setting authentication for user: {}, Authorities: {}", username, authorities);

            } catch (JWTVerificationException e) {
                // If token validation fails (expired, invalid signature, etc.)
                log.warn("Invalid JWT token received: {}. Reason: {}", jwtToken, e.getMessage());
                // Explicitly clear the context to ensure no potentially stale/invalid authentication remains
                SecurityContextHolder.clearContext();
            }
        } else {
            // Log if the Authorization header is missing or doesn't start with Bearer
            if (jwtToken != null) { log.trace("Authorization header present but not Bearer type."); }
            else { log.trace("No Authorization header found."); }
        }
        // Continue with the filter chain regardless of whether authentication was set or not
        filterChain.doFilter(request, response);
    }
}
