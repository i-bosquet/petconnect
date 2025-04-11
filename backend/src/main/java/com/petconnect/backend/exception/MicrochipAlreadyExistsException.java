package com.petconnect.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Custom runtime exception thrown when attempting to assign a microchip number
 * to a pet, but that microchip number is already registered to another pet.
 *
 * Suggests an HTTP 409 Conflict status.
 *
 * @author ibosquet
 */
@ResponseStatus(value = HttpStatus.CONFLICT)
public class MicrochipAlreadyExistsException extends RuntimeException {
    /**
     * Constructs a new MicrochipAlreadyExistsException with a default message
     * indicating the conflict.
     *
     * @param microchip The microchip number that already exists.
     */
    public MicrochipAlreadyExistsException(String microchip) {
        super("Microchip number already exists: " + microchip);
    }
}
