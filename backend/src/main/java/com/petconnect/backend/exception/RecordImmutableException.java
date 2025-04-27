package com.petconnect.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when an attempt is made to modify or delete a medical Record
 * that has been marked as immutable, typically because it is linked to a
 * generated Certificate.
 * Suggests an HTTP 409 Conflict status.
 * Extends IllegalStateException as the operation is invalid due to the record's state.
 *
 * @author ibosquet
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class RecordImmutableException extends IllegalStateException{
    /**
     * Constructs a new RecordImmutableException.
     *
     * @param recordId The ID of the immutable record.
     */
    public RecordImmutableException(Long recordId) {
        super("Operation failed: Record " + recordId + " is immutable and cannot be modified or deleted because it is linked to a certificate.");
    }
}
