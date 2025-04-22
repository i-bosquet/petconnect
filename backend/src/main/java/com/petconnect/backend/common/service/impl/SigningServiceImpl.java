package com.petconnect.backend.common.service.impl;

import com.petconnect.backend.common.service.SigningService;
import com.petconnect.backend.user.domain.model.Clinic;
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
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.RSAPublicKeySpec;

import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
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
public class SigningServiceImpl implements SigningService {
    // --- Injected Configuration ---
    @Value("${app.security.vet.privatekey.path}")
    private String vetPrivateKeyPath;
    @Value("${app.security.vet.privatekey.password}")
    private char[] vetPrivateKeyPassword;

    @Value("${app.security.clinic.privatekey.path}")
    private String clinicPrivateKeyPath;
    @Value("${app.security.clinic.privatekey.password}")
    private char[] clinicPrivateKeyPassword;

    // logs messages
    private static final String MSG_VET_KEY_LOADED = "Simulated Vet private key loaded successfully.";
    private static final String MSG_CLINIC_KEY_LOADED = "Simulated Clinic private key loaded successfully.";
    private static final String MSG_TFG_ONLY_VET = "LOADING SIMULATED VET PRIVATE KEY FROM {}";
    private static final String MSG_TFG_ONLY_CLINIC = "LOADING SIMULATED CLINIC PRIVATE KEY FROM {}";

    // --- Key Caches ---
    private PrivateKey simulatedVetPrivateKey;
    private PrivateKey simulatedClinicPrivateKey;

    // --- Static Initializer for BouncyCastle ---
    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            log.info("Adding BouncyCastleProvider.");
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String generateVetSignature(Vet vet, String dataToSign) { // Renombrado para claridad
        log.info("Attempting to generate Vet signature for Vet ID: {}", vet.getId());
        try {
            PrivateKey key = getOrLoadVetPrivateKey();
            return signData(key, dataToSign);
        } catch (Exception e) {
            log.error("Error generating Vet signature for Vet ID {}: {}", vet.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to generate Vet digital signature.", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String generateClinicSignature(Clinic clinic, String dataToSign) {
        log.info("Attempting to generate Clinic signature for Clinic ID: {}", clinic.getId());
        try {
            PrivateKey key = getOrLoadClinicPrivateKey();
            return signData(key, dataToSign);
        } catch (Exception e) {
            log.error("Error generating Clinic signature for Clinic ID {}: {}", clinic.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to generate Clinic digital signature.", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
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

    @Override
    public PrivateKey getVetPrivateKey(Vet vet) {
        try {
            return getOrLoadVetPrivateKey();
        } catch (Exception e) {
            log.error("Failed to get Vet private key for Vet ID {}: {}", vet.getId(), e.getMessage(), e);
            throw new RuntimeException("Could not load Vet private key.", e);
        }
    }

    @Override
    public PublicKey getVetPublicKey(Vet vet) {
        try {
            PrivateKey privateKey = getOrLoadVetPrivateKey();
            // Derivar clave pública desde la privada (común para RSA)
            if (privateKey instanceof RSAPrivateCrtKey rsaPrivateKey) {
                KeyFactory kf = KeyFactory.getInstance("RSA", BouncyCastleProvider.PROVIDER_NAME);
                RSAPublicKeySpec spec = new RSAPublicKeySpec(rsaPrivateKey.getModulus(), rsaPrivateKey.getPublicExponent());
                return kf.generatePublic(spec);
            } else {
                throw new RuntimeException("Cannot derive public key from Vet private key of type: " + privateKey.getClass());
            }
        } catch (Exception e) {
            log.error("Failed to get Vet public key for Vet ID {}: {}", vet.getId(), e.getMessage(), e);
            throw new RuntimeException("Could not load/derive Vet public key.", e);
        }
    }

    @Override
    public PrivateKey getClinicPrivateKey(Clinic clinic) {
        try {
            return getOrLoadClinicPrivateKey();
        } catch (Exception e) {
            log.error("Failed to get Clinic private key for Clinic ID {}: {}", clinic.getId(), e.getMessage(), e);
            throw new RuntimeException("Could not load Clinic private key.", e);
        }
    }

    @Override
    public PublicKey getClinicPublicKey(Clinic clinic) {
        try {
            PrivateKey privateKey = getOrLoadClinicPrivateKey();
            if (privateKey instanceof RSAPrivateCrtKey rsaPrivateKey) {
                KeyFactory kf = KeyFactory.getInstance("RSA", BouncyCastleProvider.PROVIDER_NAME);
                RSAPublicKeySpec spec = new RSAPublicKeySpec(rsaPrivateKey.getModulus(), rsaPrivateKey.getPublicExponent());
                return kf.generatePublic(spec);
            } else {
                throw new RuntimeException("Cannot derive public key from Clinic private key of type: " + privateKey.getClass());
            }
        } catch (Exception e) {
            log.error("Failed to get Clinic public key for Clinic ID {}: {}", clinic.getId(), e.getMessage(), e);
            throw new RuntimeException("Could not load/derive Clinic public key.", e);
        }
    }

    /**
     * Loads the Vet's private key from the configured PEM file path, caching it.
     * FOR TFG/DEMO PURPOSES ONLY.
     *
     * @return The PrivateKey object for the Vet.
     * @throws Exception if loading or decryption fails.
     */
    private PrivateKey getOrLoadVetPrivateKey() throws Exception {
        if (this.simulatedVetPrivateKey == null) {
            log.warn(MSG_TFG_ONLY_VET, vetPrivateKeyPath);
            this.simulatedVetPrivateKey = loadPrivateKeyFromPEM(vetPrivateKeyPath, vetPrivateKeyPassword);
            log.info(MSG_VET_KEY_LOADED);
            Arrays.fill(vetPrivateKeyPassword, ' ');
        }
        return this.simulatedVetPrivateKey;
    }

    /**
     * Loads the Clinic's private key from the configured PEM file path, caching it.
     *
     * @return The PrivateKey object for the Clinic (system-wide).
     * @throws Exception if loading or decryption fails.
     */
    private PrivateKey getOrLoadClinicPrivateKey() throws Exception {
        if (this.simulatedClinicPrivateKey == null) {
            log.warn(MSG_TFG_ONLY_CLINIC, clinicPrivateKeyPath);
            this.simulatedClinicPrivateKey = loadPrivateKeyFromPEM(clinicPrivateKeyPath, clinicPrivateKeyPassword);
            log.info(MSG_CLINIC_KEY_LOADED);
            Arrays.fill(clinicPrivateKeyPassword, ' ');
        }
        return this.simulatedClinicPrivateKey;
    }

    /**
     * Generic method to load a private key from a PEM file.
     * Handles different PEM formats (PKCS#8, traditional, encrypted/unencrypted).
     *
     * @param keyPath Path to the PEM file.
     * @param password Password for the PEM file (can be null or empty if not encrypted).
     * @return The loaded PrivateKey.
     * @throws Exception if loading fails.
     */
    private PrivateKey loadPrivateKeyFromPEM(String keyPath, char[] password) throws Exception {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        try (FileReader keyReader = new FileReader(keyPath);
             PEMParser pemParser = new PEMParser(keyReader)) {

            Object pemObject = pemParser.readObject();
            if (pemObject == null) {
                throw new GeneralSecurityException("No key found in PEM file: " + keyPath);
            }

            log.debug("Object read from PEM ({}): {}", keyPath, pemObject.getClass().getName());
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME);
            PrivateKey privateKey;

            switch (pemObject) {
                case PrivateKeyInfo keyInfo -> { // Unencrypted PKCS#8
                    log.debug("Loading unencrypted PKCS8 private key from {}", keyPath);
                    privateKey = converter.getPrivateKey(keyInfo);
                }
                case PEMKeyPair pemKeyPair -> {
                    log.debug("Loading PEMKeyPair (Unencrypted PKCS#1) from {}", keyPath);
                    privateKey = convertPemKeyPair(converter, pemKeyPair);
                }
                case PKCS8EncryptedPrivateKeyInfo encryptedInfo -> { // Encrypted PKCS#8
                    log.debug("Loading encrypted PKCS8 private key from {}", keyPath);
                    if (password == null || password.length == 0) throw new PEMException("Password required for encrypted key in " + keyPath);
                    InputDecryptorProvider decryptorProvider = new JceOpenSSLPKCS8DecryptorProviderBuilder().build(password);
                    PrivateKeyInfo keyInfo = encryptedInfo.decryptPrivateKeyInfo(decryptorProvider);
                    privateKey = converter.getPrivateKey(keyInfo);
                }
                case PEMEncryptedKeyPair encryptedKeyPair -> { // Encrypted PKCS#1 (Legacy)
                    log.debug("Loading PEMEncryptedKeyPair (Legacy encrypted) from {}", keyPath);
                    if (password == null || password.length == 0) throw new PEMException("Password required for encrypted key pair in " + keyPath);
                    PEMDecryptorProvider pemDecryptorProvider = new JcePEMDecryptorProviderBuilder().build(password);
                    PEMKeyPair decryptedKeyPair = encryptedKeyPair.decryptKeyPair(pemDecryptorProvider);
                    privateKey = converter.getKeyPair(decryptedKeyPair).getPrivate();
                }
                default -> throw new GeneralSecurityException("Unsupported private key format in PEM file: " + pemObject.getClass().getName());
            }

            if (privateKey == null) {
                throw new GeneralSecurityException("Failed to extract private key from PEM object in " + keyPath);
            }
            return privateKey;

        } catch (PEMException | PKCSException | OperatorCreationException | GeneralSecurityException e) {
            log.error("Security error loading or decrypting private key from {}: {}", keyPath, e.getMessage(), e);
            throw new RuntimeException("Security error processing private key from " + keyPath + ": " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Failed to load private key from {}: {}", keyPath, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Performs the actual signing operation using the provided key and data.
     *
     * @param privateKey The private key to use for signing.
     * @param dataToSign The data string to sign.
     * @return Base64 encoded signature.
     * @throws GeneralSecurityException If a signing error occurs.
     */
    private String signData(PrivateKey privateKey, String dataToSign) throws GeneralSecurityException {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA", BouncyCastleProvider.PROVIDER_NAME);
            signature.initSign(privateKey);
            signature.update(dataToSign.getBytes(StandardCharsets.UTF_8));
            byte[] signatureBytes = signature.sign();
            return Base64.getEncoder().encodeToString(signatureBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            log.error("Cryptographic error during signing: {}", e.getMessage(), e);
            throw new GeneralSecurityException("Failed to sign data.", e);
        }
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

    private PrivateKey convertPemKeyPair(JcaPEMKeyConverter converter, PEMKeyPair pemKeyPair) throws PEMException {
        try {
            return converter.getKeyPair(pemKeyPair).getPrivate();
        } catch (Exception e) {
            log.error("Failed to convert unencrypted PEMKeyPair: {}", e.getMessage());
            throw new PEMException("Could not process unencrypted PEMKeyPair.", e);
        }
    }
}
