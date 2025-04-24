package com.petconnect.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Custom runtime exception thrown when attempting to create or update a Vet
 * with a public key that is already assigned to another Vet.
 * Suggests an HTTP 409 Conflict status.
 *
 * @author ibosquet
 */
@ResponseStatus(value = HttpStatus.CONFLICT)
public class VetPublicKeyAlreadyExistsException extends RuntimeException {
    /**
     * Constructs a new VetPublicKeyAlreadyExistsException with a message
     * indicating the public key is already in use.
     */
    public VetPublicKeyAlreadyExistsException() {
        super("Veterinarian public key is already in use.");
    }
}
