package com.petconnect.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when an attempt is made to update a Record in a way
 * that violates rules regarding the VACCINE record type (e.g., changing
 * type to/from VACCINE, or modifying a VACCINE record itself via standard update).
 * Suggests an HTTP 409 Conflict status.
 * Extends IllegalStateException as the operation is invalid based on type constraints.
 *
 * @author ibosquet
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class RecordUpdateVaccineException extends IllegalStateException {
    public RecordUpdateVaccineException(Long recordId, String reason) {
        super("Cannot update record " + recordId + ": " + reason);
    }
}
