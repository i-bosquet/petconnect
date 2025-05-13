package com.petconnect.backend.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.time.Duration;
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
@Slf4j
public class JwtUtils {

    @Value("${jwt.secret.key}")
    private String privateKey; // The secret key for signing JWT tokens

    @Value("${jwt.secret.generator}")
    public String userGenerator; // The issuer identifier for JWT tokens

    public  static final String PET_ID_CLAIM = "petId";
    private static final String TOKEN_TYPE_CLAIM = "type";
    private static final String TEMPORARY_ACCESS_TYPE = "TEMP_RECORD_ACCESS";
    private static final String AUTHORITIES_CLAIM = "authorities";

    /**
     * Creates a JWT token using the provided authentication details.
     *
     * @param authentication the authentication object containing user details and authorities
     * @return a JWT token as a String
     */
    public String createToken(Authentication authentication) {
        Algorithm algorithm = Algorithm.HMAC256(this.privateKey);

        long nowMillis = System.currentTimeMillis();
        long expirationMillis = nowMillis + (8 * 60 * 60 * 1000); // 8 HOURS

        String userId = authentication.getPrincipal().toString();

        String authorities = authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        return JWT.create()
                .withIssuer(this.userGenerator)
                .withSubject(userId)
                .withClaim(AUTHORITIES_CLAIM, authorities)
                .withIssuedAt(new Date(nowMillis))
                .withExpiresAt(new Date(expirationMillis))
                .withJWTId(UUID.randomUUID().toString())
                .withNotBefore(new Date(nowMillis))
                .sign(algorithm);
    }

    /**
     * Creates a temporary JWT token specifically for granting read-only access
     * to a pet's signed medical records for a limited duration.
     *
     * @param petId    The ID of the pet whose records can be accessed.
     * @param duration The duration for which the token should be valid.
     * @return A temporary JWT token as a String.
     */
    public String createTemporaryRecordAccessToken(Long petId, Duration duration) {
        Algorithm algorithm = Algorithm.HMAC256(this.privateKey);
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + duration.toMillis());

        return JWT.create()
                .withIssuer(this.userGenerator)
                .withSubject("PetRecordViewer_" + petId)
                .withClaim(PET_ID_CLAIM, petId)
                .withClaim(TOKEN_TYPE_CLAIM, TEMPORARY_ACCESS_TYPE)
                .withIssuedAt(now)
                .withExpiresAt(expiryDate)
                .withJWTId(UUID.randomUUID().toString())
                .withNotBefore(now)
                .sign(algorithm);
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
            Algorithm algorithm = Algorithm.HMAC256(this.privateKey);
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
     * Validates a temporary JWT access token specifically for pet records.
     * Checks issuer, token type, and extracts petId.
     *
     * @param token The temporary JWT token string.
     * @return DecodedJWT of a valid and correct type, otherwise throws JWTVerificationException.
     * @throws JWTVerificationException if the token is invalid, expired, or not of the expected type.
     */
    public DecodedJWT validateAndParseTemporaryRecordAccessToken(String token) throws JWTVerificationException {
        log.debug("Attempting to validate temporary record access token.");
        try {
            Algorithm algorithm = Algorithm.HMAC256(this.privateKey);
            JWTVerifier verifier = JWT.require(algorithm)
                    .withIssuer(this.userGenerator)
                    .withClaim(TOKEN_TYPE_CLAIM, TEMPORARY_ACCESS_TYPE)
                    .build();

            DecodedJWT decodedJWT = verifier.verify(token);
            log.debug("Temporary token validated successfully for subject: {}", decodedJWT.getSubject());

            Claim petIdClaim = decodedJWT.getClaim(PET_ID_CLAIM);
            if (petIdClaim.isNull() || petIdClaim.asLong() == null) {
                log.error("Temporary token is missing or has invalid petId claim. Token Subject: {}", decodedJWT.getSubject());
                throw new JWTVerificationException("Token is valid but incomplete (missing petId).");
            }
            log.info("Successfully validated temporary access token for petId: {}", petIdClaim.asLong());
            return decodedJWT;
        } catch (JWTVerificationException e) {
            log.warn("Temporary access token validation failed: {}. Token: [{}]", e.getMessage(), token.substring(0, Math.min(token.length(), 30)) + "...");
            throw e;
        }
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

    /**
     * Extracts the User ID (as a String) from the decoded JWT token's subject.
     *
     * @param decodedJWT the decoded JWT token
     * @return the User ID contained in the token's subject
     */
    public String extractUserId(DecodedJWT decodedJWT) {
        return decodedJWT.getSubject();
    }

}