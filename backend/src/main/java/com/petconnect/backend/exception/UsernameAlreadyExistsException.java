package com.petconnect.backend.exception;

/**
 * Custom runtime exception thrown when attempting to register a user
 * with a username that is already present in the system.
 *
 * @author ibosquet
 */
public class UsernameAlreadyExistsException extends RuntimeException {
    /**
     * Constructs a new UsernameAlreadyExistsException with the specified detail message.
     *
     * @param username the detail message.
     */
    public UsernameAlreadyExistsException(String username) {
        super("Username already taken: " + username);
    }
}
