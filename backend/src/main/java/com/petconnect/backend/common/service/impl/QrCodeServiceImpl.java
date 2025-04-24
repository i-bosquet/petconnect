package com.petconnect.backend.common.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.petconnect.backend.certificate.domain.model.Certificate;
import com.petconnect.backend.common.service.QrCodeService;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import nl.minvws.encoding.Base45;
import COSE.*;
import com.upokecenter.cbor.CBORObject;
import com.upokecenter.cbor.CBORType;

/**
 * Implementation of the {@link QrCodeService} interface.
 * Handles the complex process of encoding certificate data into a Base45 string for QR codes.
 *
 * @author ibosquet
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QrCodeServiceImpl implements QrCodeService {

    private final ObjectMapper cborMapper = new ObjectMapper(new CBORFactory());
    private final ObjectMapper jsonMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    //  Standard prefix for EU DCC certificates
    private static final String QR_DATA_PREFIX = "HC1:";

    /**
     * {@inheritDoc}
     */
    @Override
    public String generateQrData(Certificate certificate) {
        log.info("Generating QR data for Certificate ID: {}", certificate.getId());
        try {
            String payloadJson = certificate.getPayload();

            // Convert payload to CBOR after hashing the JSON
            byte[] payloadCbor = convertJsonToCbor(payloadJson);

            // Create the COSE structure using the already generated signatures from the Certificate entity
            byte[] coseSignedData = createCoseStructure(certificate, payloadCbor);

            // Compress
            byte[] compressedData = compressWithZlib(coseSignedData);

            // Encode to Base45
            String base45Data = encodeToBase45(compressedData);
            String finalQrData = QR_DATA_PREFIX + base45Data;

            log.info("Successfully generated Base45 QR data for Certificate ID: {}", certificate.getId());
            return finalQrData;

        } catch (CoseException e) {
            log.error("COSE error during QR data generation for Certificate ID {}: {}", certificate.getId(), e.getMessage(), e);
            throw new RuntimeException("COSE processing failed for certificate " + certificate.getId(), e);
        } catch (Exception e) {
            log.error("Failed to generate QR data for Certificate ID {}: {}", certificate.getId(), e.getMessage(), e);
            throw new RuntimeException("QR data generation failed for certificate " + certificate.getId(), e);
        }
    }

    /**
     * Converts a JSON string payload into a CBOR byte array.
     *
     * @param jsonPayload The JSON string representing the certificate payload.
     * @return The CBOR encoded a byte array.
     * @throws RuntimeException if JSON parsing or CBOR writing fails.
     */
    private byte[] convertJsonToCbor(String jsonPayload) {
        log.debug("Converting JSON payload to CBOR...");
        try {
            Map<String, Object> payloadMap = jsonMapper.readValue(jsonPayload, new TypeReference<LinkedHashMap<String, Object>>() {
            });
            byte[] cborBytes = cborMapper.writeValueAsBytes(payloadMap);
            log.debug("JSON to CBOR conversion successful. CBOR size: {} bytes", cborBytes.length);
            return cborBytes;
        } catch (JsonProcessingException e) {
            log.error("Error processing JSON or writing CBOR: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to convert payload to CBOR format.", e);
        }
    }

    /**
     * Creates the COSE_Sign structure embedding the payload and the pre-generated signatures.
     *
     * @param certificate The persisted certificate containing signatures and entity refs.
     * @param payloadCbor The CBOR encoded payload.
     * @return The bytes of the encoded COSE_Sign structure.
     * @throws CoseException If an error occurs during CBOR/COSE processing.
     * @throws IllegalArgumentException if signatures in certificate are invalid Base64.
     */
    private byte[] createCoseStructure(Certificate certificate, byte[] payloadCbor) throws CoseException {
        log.debug("Creating COSE_Sign structure for certificate ID: {}", certificate.getId());

        // Decode existing signatures from Base64
        byte[] vetSignatureBytes;
        byte[] clinicSignatureBytes;
        try {
            vetSignatureBytes = Base64.getDecoder().decode(certificate.getVetSignature());
            clinicSignatureBytes = Base64.getDecoder().decode(certificate.getClinicSignature());
        } catch (IllegalArgumentException e) {
            log.error("Invalid Base64 signature found in Certificate ID {}: {}", certificate.getId(), e.getMessage());
            throw new IllegalArgumentException("Invalid Base64 encoding for signatures.", e);
        }

        // Build COSE_Sign structure: [ protected_empty, unprotected_empty, payload, signatures_array ]
        CBORObject protectedHeadersMap = CBORObject.NewMap();
        protectedHeadersMap.Add(HeaderKeys.Algorithm.AsCBOR(), AlgorithmID.RSA_PSS_256.AsCBOR());
        byte[] protectedHeadersBytes = protectedHeadersMap.EncodeToBytes();

        CBORObject unprotectedHeadersMap = CBORObject.NewMap();

        // Build Signatures Array: [ COSE_Signature_Vet, COSE_Signature_Clinic ]
        CBORObject signaturesArray = CBORObject.NewArray();

        // Assuming RSA_PSS_256 for both for simplicity;
        signaturesArray.Add(buildCoseSignature(vetSignatureBytes, AlgorithmID.RSA_PSS_256));
        signaturesArray.Add(buildCoseSignature(clinicSignatureBytes, AlgorithmID.RSA_PSS_256));

        // Assemble final COSE_Sign Array
        CBORObject coseSignStructure = CBORObject.NewArray();
        coseSignStructure.Add(protectedHeadersBytes);
        coseSignStructure.Add(unprotectedHeadersMap);
        coseSignStructure.Add(CBORObject.FromByteArray(payloadCbor));
        coseSignStructure.Add(signaturesArray);

        // Encode to bytes
        byte[] coseBytes = coseSignStructure.EncodeToBytes();
        log.debug("COSE_Sign structure created successfully. Size: {} bytes", coseBytes.length);
        return coseBytes;
    }

    /**
     * Helper method to build a single COSE_Signature structure (array).
     * [ protected_headers': bstr, unprotected_headers': map, signature: bstr ]
     *
     * @param signatureBytes The raw bytes of the pre-generated signature.
     * @param algorithmID The COSE AlgorithmID used for this signature.
     * @return A CBORObject representing the COSE_Signature array.
     */
    private CBORObject buildCoseSignature(byte[] signatureBytes, AlgorithmID algorithmID)  {
        try {
            // Protected headers for THIS signature
            CBORObject protectedMap = CBORObject.NewMap();
            byte[] protectedBytes = protectedMap.EncodeToBytes();

            CBORObject unprotectedMap = CBORObject.NewMap();

            CBORObject signatureCbor = CBORObject.FromByteArray(signatureBytes);

            CBORObject coseSignature = CBORObject.NewArray();
            coseSignature.Add(protectedBytes);
            coseSignature.Add(unprotectedMap);
            coseSignature.Add(signatureCbor);

            return coseSignature;
        } catch (Exception e) {
            log.error("Error building individual COSE signature structure: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to build COSE signature structure", e);
        }
    }

    /**
     * Compresses the input data using ZLib (DEFLATE algorithm).
     *
     * @param data The byte array to compress.
     * @return The compressed byte array.
     * @throws RuntimeException if an IOException occurs during compression.
     */
    private byte[] compressWithZlib(byte[] data) {
        if (data == null) {
            throw new IllegalArgumentException("Data to be compressed cannot be null");
        }
        log.debug("Compressing data with ZLib (DEFLATE)... Input size: {} bytes", data.length);
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            // Use BEST_COMPRESSION for potentially smaller QR codes
            DeflaterOutputStream deflaterStream = new DeflaterOutputStream(byteStream, new Deflater(Deflater.BEST_COMPRESSION))) {

            deflaterStream.write(data);

            deflaterStream.finish();
            byte[] compressedData = byteStream.toByteArray();
            log.debug("ZLib compression complete. Output size: {} bytes", compressedData.length);
            return compressedData;

        } catch (IOException e) {
            log.error("IOException during ZLib compression: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to compress data using ZLib.", e);
        }
    }

    /**
     * Encodes the input byte array into a Base45 string.
     *
     * @param data The byte array to encode (typically ZLib compressed COSE data).
     * @return The Base45 encoded string.
     */
    private String encodeToBase45(byte[] data) {
        if (data == null) {
            throw new IllegalArgumentException("Data to be encoded cannot be null");
        }
        log.debug("Encoding {} bytes to Base45...", data.length);
        String encoded = Base45.getEncoder().encodeToString(data);
        log.debug("Base45 encoding complete.");
        return encoded;
    }
}
