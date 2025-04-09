package com.petconnect.backend.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Utility class for JWT operations such as token creation, validation, and claim extraction.
 * It uses the HMAC256 algorithm with a secret key provided via application properties.
 *
 * @author ibosquet
 */
@Component
public class JwtUtils {

    @Value("${jwt.secret.key}")
    private String privatekey; // The secret key for signing JWT tokens

    @Value("${jwt.secret.generator}")
    public String userGenerator; // The issuer identifier for JWT tokens

    /**
     * Creates a JWT token using the provided authentication details.
     *
     * @param authentication the authentication object containing user details and authorities
     * @return a JWT token as a String
     */
    public String createToken(Authentication authentication) {
        Algorithm algorithm = Algorithm.HMAC256(this.privatekey);

        String username = authentication.getPrincipal().toString();

        String authorities = authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        String jwtToken;
        jwtToken = JWT.create()
                .withIssuer(this.userGenerator)
                .withSubject(username)
                .withClaim("authorities", authorities)
                .withIssuedAt(new Date())
                // expires in 30 min -> 1.800.000ms
                .withExpiresAt(new Date(System.currentTimeMillis() + 1800000))
                .withJWTId(UUID.randomUUID().toString())
                .withNotBefore(new Date(System.currentTimeMillis()))
                .sign(algorithm);

        return jwtToken;
    }

    /**
     * Validates a JWT token and returns the decoded token.
     *
     * @param token the JWT token to validate
     * @return the decoded JWT token
     * @throws JWTVerificationException if the token is invalid or verification fails
     */
    public DecodedJWT validateToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(this.privatekey);
            JWTVerifier verifier = JWT.require(algorithm)
                    // specify any specific claim validations
                    .withIssuer(this.userGenerator)
                    // reusable verifier instance
                    .build();
            DecodedJWT decodedJWT;
            decodedJWT = verifier.verify(token);
            return decodedJWT;
        } catch (JWTVerificationException exception) {
            throw new JWTVerificationException("Token invalid. not authorized");
        }
    }

    /**
     * Extracts the username from the decoded JWT token.
     *
     * @param decodedJWT the decoded JWT token
     * @return the username contained in the token's subject
     */
    public String extractUsername(DecodedJWT decodedJWT) {
        return decodedJWT.getSubject();
    }

    /**
     * Retrieves a specific claim from the decoded JWT token.
     *
     * @param decodedJWT the decoded JWT token
     * @param claimName the name of the claim to retrieve
     * @return the claim corresponding to the specified name
     */
    public Claim getSpecificClaim (DecodedJWT decodedJWT, String claimName) {
        return decodedJWT.getClaim(claimName);
    }

}
