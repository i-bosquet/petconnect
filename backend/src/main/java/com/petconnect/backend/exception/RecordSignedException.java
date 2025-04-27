package com.petconnect.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when an attempt is made to modify a medical Record
 * that has already been digitally signed by a Veterinarian.
 * Suggests an HTTP 409 Conflict status.
 * Extends IllegalStateException as the operation is invalid due to the record's state.
 *
 * @author ibosquet
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class RecordSignedException extends IllegalStateException {
    public RecordSignedException(Long recordId) {
        super("Cannot update record " + recordId + " because it has been signed by a veterinarian.");
    }
}
