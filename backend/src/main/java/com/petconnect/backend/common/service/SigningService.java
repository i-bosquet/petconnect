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
     * Generates a digital signature for a given data string using the veterinarian's private key.
     * The private key is decrypted using the provided password.
     *
     * @param vet            The veterinarian whose private key will be used for signing.
     * @param dataToSign     The string data to be signed, typically a precomputed hash.
     * @param vetKeyPassword The password used to decrypt the veterinarian's private key.
     * @return A Base64 encoded string representation of the generated digital signature.
     * @throws RuntimeException if signing fails due to key access, decryption issues, or other errors.
     */
    String generateVetSignature(Vet vet, String dataToSign, char[] vetKeyPassword);

    /**
     * Generates a digital signature using the issuing Clinic's configured private key.
     * (For TFG, this might use a single system-wide clinic key).
     *
     * @param clinic     The Clinic issuing the signature.
     * @param dataToSign The data string (typically a hash) to be signed.
     * @return Base64 encoded signature string.
     * @throws RuntimeException if signing fails.
     */
    String generateClinicSignature(Clinic clinic, String dataToSign, char[] clinicKeyPassword);

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
     * Retrieves the PublicKey object corresponding to the Vet's private key.
     * This might load from a file or be derived if not stored separately.
     * @param vet The veterinarian.
     * @return The PublicKey.
     * @throws RuntimeException if key loading/derivation fails.
     */
    PublicKey getVetPublicKey(Vet vet);


    /**
     * Retrieves the PublicKey object corresponding to the Clinic's private key.
     * @param clinic The clinic.
     * @return The PublicKey.
     * @throws RuntimeException if key loading/derivation fails.
     */
    PublicKey getClinicPublicKey(Clinic clinic);
}
