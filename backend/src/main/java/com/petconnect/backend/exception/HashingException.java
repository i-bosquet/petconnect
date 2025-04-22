package com.petconnect.backend.exception;

/**
 * Custom runtime exception thrown when an error occurs during cryptographic hashing.
 *
 * @author ibosquet
 */
public class HashingException extends RuntimeException {
    /**
     * Constructs a new HashingException with the specified detail message and cause.
     *
     * @param message the detail message.
     * @param cause   the underlying cause of the exception.
     */
    public HashingException(String message, Throwable cause) {
        super(message, cause);
    }
}
