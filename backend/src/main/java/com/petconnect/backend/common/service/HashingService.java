package com.petconnect.backend.common.service;

import com.petconnect.backend.exception.HashingException;

/**
 * Service interface for cryptographic hashing operations.
 *
 * @author ibosquet
 */
public interface HashingService {
    /**
     * Hashes the input string using a standard algorithm (e.g., SHA-256).
     *
     * @param input The string data to hash.
     * @return The hash value, typically represented as a hexadecimal string.
     * @throws HashingException if an error occurs during the hashing process.
     */
    String hashString(String input) throws HashingException;
}
