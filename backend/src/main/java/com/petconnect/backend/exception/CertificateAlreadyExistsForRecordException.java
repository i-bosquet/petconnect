package com.petconnect.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Custom runtime exception thrown when attempting to generate a certificate
 * for a medical record that already has an associated certificate.
 * Suggests an HTTP 409 Conflict status.
 *
 * @author ibosquet
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class CertificateAlreadyExistsForRecordException extends RuntimeException {
    /**
     * Constructs a new CertificateAlreadyExistsForRecordException.
     *
     * @param recordId The ID of the record for which a certificate already exists.
     */
    public CertificateAlreadyExistsForRecordException(Long recordId) {
        super("A certificate already exists for record " + recordId + ".");
    }
}
