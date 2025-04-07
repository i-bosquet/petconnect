package com.petconnect.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Custom runtime exception thrown when attempting to register a user
 * with an email address that is already present in the system.
 *
 * @author ibosquet
 */
@ResponseStatus(value = HttpStatus.CONFLICT) // Suggests HTTP 409 Conflict status
public class EmailAlreadyExistsException extends RuntimeException {

    /**
     * Constructs a new EmailAlreadyExistsException with the specified detail message.
     *
     * @param email the detail message.
     */
    public EmailAlreadyExistsException(String email) {
        super("Email already in use: " + email);
    }
}
