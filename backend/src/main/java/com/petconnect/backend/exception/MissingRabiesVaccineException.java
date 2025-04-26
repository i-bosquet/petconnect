package com.petconnect.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a required, valid, signed Rabies vaccine record
 * is not found for a pet when attempting to generate a certificate.
 * Suggests an HTTP 400 Bad Request status.
 *
 * @author ibosquet
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class MissingRabiesVaccineException extends RuntimeException {
    public MissingRabiesVaccineException(Long petId) {
        super(String.format("Cannot generate certificate for pet %d: No valid, signed, and current Rabies vaccine record found.", petId));
    }
}
