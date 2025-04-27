package com.petconnect.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.time.LocalDate;

/**
 * Exception thrown when a required recent, signed health checkup record
 * (ANNUAL_CHECK) is not found for a pet when attempting
 * to generate a certificate.
 * Suggests an HTTP 400 Bad Request status.
 *
 * @author ibosquet
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class MissingRecentCheckupException extends RuntimeException {
    /**
     * Constructs a new MissingRecentCheckupException.
     *
     * @param petId      The ID of the pet for which the checkup is missing.
     * @param cutoffDate The earliest date the checkup should have occurred (inclusive).
     */
    public MissingRecentCheckupException(Long petId, LocalDate cutoffDate) {
        super(String.format("Cannot generate certificate for pet %d: No signed ANNUAL_CHECK found since %s.",
                petId, cutoffDate));
    }
}
