package com.petconnect.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Custom runtime exception thrown when attempting to generate a certificate
 * with an official certificate number that is already in use.
 * Suggests an HTTP 409 Conflict status.
 *
 * @author ibosquet
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class CertificateNumberAlreadyExistsException extends RuntimeException {
    /**
     * Constructs a new CertificateNumberAlreadyExistsException.
     *
     * @param certificateNumber The certificate number that already exists.
     */
    public CertificateNumberAlreadyExistsException(String certificateNumber) {
        super("Certificate number '" + certificateNumber + "' is already in use.");
    }
}
