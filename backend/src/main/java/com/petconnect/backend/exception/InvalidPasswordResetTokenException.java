package com.petconnect.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a password reset token is invalid, expired, or not found.
 * Suggests an HTTP 400 Bad Request status, as the client provided invalid input (the token).
 *
 * @author ibosquet
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidPasswordResetTokenException extends RuntimeException {
    public InvalidPasswordResetTokenException(String message) {
        super(message);
    }
}
