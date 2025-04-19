package com.petconnect.backend.common.service;

import com.petconnect.backend.user.domain.model.Vet;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.*;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8DecryptorProviderBuilder;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import org.bouncycastle.operator.InputDecryptorProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.pkcs.PKCSException;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Service responsible for handling digital signature creation and verification
 * using RSA and SHA256withRSA algorithm via BouncyCastle provider.
 * Handles loading simulated private keys and stored public keys.
 *
 * @author ibosquet
 */
@Service
@Slf4j
public class SigningService {
    // --- Injected Configuration ---
    @Value("${app.security.vet.privatekey.path}")
    private String privateKeyPath;

    @Value("${app.security.vet.privatekey.password}")
    private char[] privateKeyPassword;

    private PrivateKey simulatedVetPrivateKey;

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            log.info("Adding BouncyCastleProvider.");
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    /**
     * Generates a digital signature for the given data using the simulated Vet's private key.
     * Loads the key on first use or if not already cached.
     *
     * @param vet         The Vet performing the signing (used for logging/context).
     * @param dataToSign  The String data to be signed (canonical representation of the record).
     * @return Base64 encoded signature string.
     * @throws RuntimeException if key loading or signing fails.
     */
    public String generateSignature(Vet vet, String dataToSign) {
        log.info("Attempting to generate signature for Vet ID: {}", vet.getId());
        try {
            PrivateKey key = getOrLoadPrivateKey();

            Signature signature = Signature.getInstance("SHA256withRSA", BouncyCastleProvider.PROVIDER_NAME);
            signature.initSign(key);
            signature.update(dataToSign.getBytes(StandardCharsets.UTF_8));
            byte[] signatureBytes = signature.sign();

            String base64Signature = Base64.getEncoder().encodeToString(signatureBytes);
            log.info("Signature generated successfully for Vet ID: {}.", vet.getId());
            return base64Signature;

        } catch (GeneralSecurityException | RuntimeException e) {
            log.error("Error generating signature for Vet ID {}: {}", vet.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to generate digital signature.", e);
        } catch (Exception e) {
            log.error("Unexpected error during signature generation for Vet ID {}: {}", vet.getId(), e.getMessage(), e);
            throw new RuntimeException("Unexpected error during signature generation.", e);
        }
    }

    /**
     * Verifies a digital signature using the provided public key PEM string.
     *
     * @param publicKeyPemB64 The Vet's public key (Base64 content only, no headers/footers).
     * @param originalData    The original data that was supposedly signed.
     * @param signatureB64    The Base64 encoded signature to verify.
     * @return true if the signature is valid, false otherwise.
     */
    public boolean verifySignature(String publicKeyPemB64, String originalData, String signatureB64) {
        if (!StringUtils.hasText(publicKeyPemB64) || !StringUtils.hasText(originalData) || !StringUtils.hasText(signatureB64)) {
            log.warn("Verification skipped: Missing public key, data, or signature.");
            return false;
        }
        log.debug("Verifying signature...");
        try {
            PublicKey publicKey = loadPublicKeyFromPemString(publicKeyPemB64);
            byte[] signatureBytes = Base64.getDecoder().decode(signatureB64);

            Signature signature = Signature.getInstance("SHA256withRSA", BouncyCastleProvider.PROVIDER_NAME);
            signature.initVerify(publicKey);
            signature.update(originalData.getBytes(StandardCharsets.UTF_8));

            boolean isValid = signature.verify(signatureBytes);
            log.debug("Signature verification result: {}", isValid);
            return isValid;

        } catch (Exception e) {
            log.error("Error verifying signature: {}", e.getMessage());
            return false;
        }
    }


    /**
     * Loads the private key from the configured PEM file path.
     * Handles password-protected PEM files using BouncyCastle. Caches the key after the first load.
     * FOR TFG/DEMO PURPOSES ONLY.
     * Uses switch expression and pattern matching.
     *
     * @return The PrivateKey object.
     * @throws Exception if loading or decryption fails.
     */
    private PrivateKey getOrLoadPrivateKey() throws Exception{
        if (this.simulatedVetPrivateKey == null) {
            log.warn("LOADING SIMULATED VET PRIVATE KEY FROM {} - TFG USE ONLY!", privateKeyPath);
            if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
                Security.addProvider(new BouncyCastleProvider());
            }

            try (FileReader keyReader = new FileReader(privateKeyPath);
                 PEMParser pemParser = new PEMParser(keyReader)) {

                Object pemObject = pemParser.readObject();
                JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME);

                if (pemObject == null) {
                    throw new GeneralSecurityException("No key found in PEM file: " + privateKeyPath);
                }

                log.debug("Object read from PEM: {}", pemObject.getClass().getName());

                this.simulatedVetPrivateKey = switch (pemObject) {
                    case PrivateKeyInfo keyInfo -> {
                        log.debug("Loading unencrypted PKCS8 private key.");
                        yield converter.getPrivateKey(keyInfo);
                    }
                    case PEMKeyPair pemKeyPair -> {
                        log.debug("Loading PEMKeyPair (Unencrypted PKCS#1).");
                        try {
                            yield converter.getKeyPair(pemKeyPair).getPrivate();
                        } catch (Exception e) {
                            log.error("Failed to convert unencrypted PEMKeyPair: {}", e.getMessage());
                            throw new PEMException("Could not process unencrypted PEMKeyPair.", e);
                        }
                    }
                    case PKCS8EncryptedPrivateKeyInfo encryptedInfo -> {
                        log.debug("Loading encrypted PKCS8 private key.");
                        InputDecryptorProvider decryptorProvider = new JceOpenSSLPKCS8DecryptorProviderBuilder()
                                .build(privateKeyPassword);
                        PrivateKeyInfo keyInfo = encryptedInfo.decryptPrivateKeyInfo(decryptorProvider);
                        yield converter.getPrivateKey(keyInfo);
                    }
                    case PEMEncryptedKeyPair encryptedKeyPair -> {
                        log.debug("Loading PEMEncryptedKeyPair (Legacy encrypted).");
                        PEMDecryptorProvider pemDecryptorProvider = new JcePEMDecryptorProviderBuilder().build(privateKeyPassword);
                        PEMKeyPair decryptedKeyPair = encryptedKeyPair.decryptKeyPair(pemDecryptorProvider);
                        yield converter.getKeyPair(decryptedKeyPair).getPrivate();
                    }
                    default -> {
                        log.error("Unsupported private key format found in PEM file: {}", pemObject.getClass().getName());
                        throw new GeneralSecurityException("Unsupported or unrecognized private key format in PEM file.");
                    }
                };

                if (this.simulatedVetPrivateKey == null) {
                    throw new GeneralSecurityException("Failed to extract private key from PEM object.");
                }
                log.info("Simulated Vet private key loaded successfully.");

            } catch (PEMException | PKCSException | OperatorCreationException | GeneralSecurityException e) {
                log.error("Security error loading or decrypting private key from {}: {}", privateKeyPath, e.getMessage(), e);
                this.simulatedVetPrivateKey = null;
                throw new RuntimeException("Security error processing private key: " + e.getMessage(), e);
            } catch (Exception e) {
                log.error("Failed to load private key from {}: {}", privateKeyPath, e.getMessage(), e);
                this.simulatedVetPrivateKey = null;
                throw e;
            } finally {
                java.util.Arrays.fill(privateKeyPassword, ' ');
            }
        }
        return this.simulatedVetPrivateKey;
    }

    /**
     * Loads a PublicKey from its Base64 PEM string representation (without headers/footers).
     */
    private PublicKey loadPublicKeyFromPemString(String publicKeyPemB64) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(publicKeyPemB64.replaceAll("\\s+", ""));
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA", BouncyCastleProvider.PROVIDER_NAME);
        return kf.generatePublic(spec);
    }
}
