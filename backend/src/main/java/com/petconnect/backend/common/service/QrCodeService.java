package com.petconnect.backend.common.service;

import com.petconnect.backend.certificate.domain.model.Certificate;

/**
 * Service interface for generating the final encoded string representation
 * suitable for embedding within a QR code, based on the EU Digital Green Certificate (DGC)
 * principles (CBOR, COSE, ZLib, Base45).
 *
 * @author ibosquet
 */
public interface QrCodeService {
    /**
     * Generates the Base45 encoded string representing the signed and compressed certificate data.
     * This involves:
     * 1. Retrieving necessary data from the Certificate entity (payload, signatures).
     * 2. Potentially reconstructing or re-serializing payload into CBOR format.
     * 3. Creating a COSE_Sign1 structure containing the payload and signatures.
     * 4. Compressing the COSE structure using ZLib.
     * 5. Encoding the compressed bytes using Base45.
     *
     * @param certificate The persisted Certificate entity containing all necessary data.
     * @return The final Base45 encoded string ready for QR code generation.
     * @throws RuntimeException if any step in the process fails (e.g., CBOR/COSE error, compression error).
     */
    String generateQrData(Certificate certificate);
}
