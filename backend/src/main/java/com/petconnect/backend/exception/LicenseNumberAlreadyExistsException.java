package com.petconnect.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Custom runtime exception thrown when attempting to create or update a Vet
 * with a license number that is already assigned to another Vet.
 *
 * @author ibosquet
 */
@ResponseStatus(value = HttpStatus.CONFLICT)
public class LicenseNumberAlreadyExistsException extends RuntimeException {
    /**
     * Constructs a new LicenseNumberAlreadyExistsException with the specified license number.
     *
     * @param licenseNumber The license number that already exists.
     */
    public LicenseNumberAlreadyExistsException(String licenseNumber) {
        super("License number already in use: " + licenseNumber);
    }
}
