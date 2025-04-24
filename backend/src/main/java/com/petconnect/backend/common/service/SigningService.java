package com.petconnect.backend.common.service;

import com.petconnect.backend.user.domain.model.Clinic;
import com.petconnect.backend.user.domain.model.Vet;

import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * Service interface defining cryptographic signing and verification operations.
 * Provides methods for generating signatures using veterinarian or clinic keys
 * and verifying signatures using public keys.
 *
 * @author ibosquet
 */
public interface SigningService {

    /**
     * Generates a digital signature using the specific Vet's configured private key.
     *
     * @param vet        The Vet performing the signing.
     * @param dataToSign The data string (typically a hash) to be signed.
     * @return Base64 encoded signature string.
     * @throws RuntimeException if signing fails.
     */
    String generateVetSignature(Vet vet, String dataToSign);

    /**
     * Generates a digital signature using the issuing Clinic's configured private key.
     * (For TFG, this might use a single system-wide clinic key).
     *
     * @param clinic     The Clinic issuing the signature.
     * @param dataToSign The data string (typically a hash) to be signed.
     * @return Base64 encoded signature string.
     * @throws RuntimeException if signing fails.
     */
    String generateClinicSignature(Clinic clinic, String dataToSign);

    /**
     * Verifies a digital signature using a provided public key.
     *
     * @param publicKeyPemB64 The public key (Base64 content, no headers/footers).
     * @param originalData    The original data that was supposedly signed.
     * @param signatureB64    The Base64 encoded signature to verify.
     * @return true if the signature is valid, false otherwise.
     */
    boolean verifySignature(String publicKeyPemB64, String originalData, String signatureB64);

    /**
     * Retrieves the PrivateKey object for the specified Vet.
     * FOR TFG/DEMO USE ONLY - Loads from the configured file.
     * @param vet The veterinarian.
     * @return The PrivateKey.
     * @throws RuntimeException if key loading fails.
     */
    PrivateKey getVetPrivateKey(Vet vet);

    /**
     * Retrieves the PublicKey object corresponding to the Vet's private key.
     * This might load from a file or be derived if not stored separately.
     * @param vet The veterinarian.
     * @return The PublicKey.
     * @throws RuntimeException if key loading/derivation fails.
     */
    PublicKey getVetPublicKey(Vet vet);

    /**
     * Retrieves the PrivateKey object for the specified Clinic.
     * FOR TFG/ DEMO, USE ONLY - Loads from a configured file.
     * @param clinic The clinic.
     * @return The PrivateKey.
     * @throws RuntimeException if key loading fails.
     */
    PrivateKey getClinicPrivateKey(Clinic clinic);

    /**
     * Retrieves the PublicKey object corresponding to the Clinic's private key.
     * @param clinic The clinic.
     * @return The PublicKey.
     * @throws RuntimeException if key loading/derivation fails.
     */
    PublicKey getClinicPublicKey(Clinic clinic);
}
