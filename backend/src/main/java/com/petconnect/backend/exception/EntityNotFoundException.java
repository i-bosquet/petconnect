package com.petconnect.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Custom runtime exception thrown when a requested entity
 * (e.g., UserEntity, Clinic, Pet) cannot be found in the system,
 * typically queried by its ID or other unique identifier.
 * Suggests an HTTP 404 Not Found status.
 *
 * @author ibosquet
 */
@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class EntityNotFoundException extends RuntimeException {
    /**
     * Constructs a new EntityNotFoundException with the specified detail message.
     *
     * @param message the detail message explaining which entity was not found and why.
     */
    public EntityNotFoundException(String message) {
        super(message);
    }

    /**
     * Constructs a new EntityNotFoundException with a default message
     * indicating the entity type and identifier.
     *
     * @param entityName The simple name of the entity class (e.g., "Clinic", "UserEntity").
     * @param id The identifier used to search for the entity.
     */
    public EntityNotFoundException(String entityName, Object id) {
        super(String.format("%s not found with id: %s", entityName, id));
    }
}
