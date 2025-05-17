package com.petconnect.backend.common.service.impl;

import com.petconnect.backend.common.service.KeyStorageService;
import com.petconnect.backend.common.service.SigningService;
import com.petconnect.backend.user.domain.model.Clinic;
import com.petconnect.backend.user.domain.model.Vet;
import lombok.RequiredArgsConstructor;
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
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.KeyFactory;

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
@RequiredArgsConstructor
@Slf4j
public class SigningServiceImpl implements SigningService {

    private final KeyStorageService keyStorageService;

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
    public String generateVetSignature(Vet vet, String dataToSign, char[] vetKeyPassword) {
        if (vet == null || !StringUtils.hasText(vet.getVetPrivateKey())) {
            log.error("Vet or vetPrivateKey path is missing for Vet ID: {}", vet != null ? vet.getId() : "null");
            throw new IllegalArgumentException("Veterinarian's private key path not configured.");
        }
        if (vetKeyPassword == null || vetKeyPassword.length == 0) {
            log.error("Password for Vet's private key (ID: {}) was not provided.", vet.getId());
            throw new IllegalArgumentException("Password for veterinarian's private key is required for signing.");
        }

        log.info("Attempting to generate Vet signature for Vet ID: {}", vet.getId());
        PrivateKey vetPrivateKeyLoaded;
        try {
            Path privateKeyAbsolutePath = keyStorageService.getAbsolutePathForPrivateKey(vet.getVetPrivateKey());
            vetPrivateKeyLoaded = loadPrivateKeyFromPEM(privateKeyAbsolutePath.toString(), vetKeyPassword);
            return signData(vetPrivateKeyLoaded, dataToSign);
        } catch (Exception e) {
            log.error("Error generating Vet signature for Vet ID {}: {}", vet.getId(), e.getMessage(), e);
            if (e instanceof PEMException ||
                    e instanceof PKCSException ||
                    (e.getCause() instanceof IOException &&
                            e.getCause().getMessage() !=
                                    null && e.getCause().getMessage().contains("AEADBadTagException"))) {
                throw new RuntimeException("Failed to decrypt Vet private key. Incorrect password or key format for Vet ID " + vet.getId() + ".", e);
            }
            throw new RuntimeException("Failed to generate Vet digital signature for Vet ID " + vet.getId() + ".", e);
        } finally {
            Arrays.fill(vetKeyPassword, ' ');
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String generateClinicSignature(Clinic clinic, String dataToSign, char[] clinicKeyPassword ) {
        if (clinic == null || !StringUtils.hasText(clinic.getPrivateKey())) {
            log.error("Clinic or privateKey path is missing for Clinic ID: {}", clinic != null ? clinic.getId() : "null");
            throw new IllegalArgumentException("Clinic's private key path not configured.");
        }
        if (clinicKeyPassword == null || clinicKeyPassword.length == 0) {
            log.error("Password for Clinic's private key (ID: {}) was not provided.", clinic.getId());
            throw new IllegalArgumentException("Password for clinic's private key is required for signing.");
        }
        log.info("Attempting to generate Clinic signature for Clinic ID: {}", clinic.getId());
        PrivateKey clinicPkLoaded;
        try {
            Path privateKeyAbsolutePath = keyStorageService.getAbsolutePathForPrivateKey(clinic.getPrivateKey());
            clinicPkLoaded = loadPrivateKeyFromPEM(privateKeyAbsolutePath.toString(), clinicKeyPassword);
            return signData(clinicPkLoaded, dataToSign);
        } catch (Exception e) {
            log.error("Error generating Clinic signature for Clinic ID {}: {}", clinic.getId(), e.getMessage(), e);
            if (e instanceof PEMException || e instanceof PKCSException || (e.getCause() instanceof IOException && e.getCause().getMessage() != null && e.getCause().getMessage().contains("AEADBadTagException"))) {
                throw new RuntimeException("Failed to decrypt Clinic private key. Incorrect password or key format for Clinic ID " + clinic.getId() + ".", e);
            }
            throw new RuntimeException("Failed to generate Clinic digital signature for Clinic ID " + clinic.getId() + ".", e);
        } finally {
            Arrays.fill(clinicKeyPassword, ' ');
        }
    }

    @Override
    public PublicKey getVetPublicKey(Vet vet) {
        if (vet == null || !StringUtils.hasText(vet.getVetPublicKey())) {
            log.error("Vet entity or vetPublicKey path is null/empty for vet ID: {}", (vet != null ? vet.getId() : "null"));
            throw new IllegalArgumentException("Vet or Vet's public key path is missing.");
        }
        String publicKeyRelativePath = vet.getVetPublicKey();
        log.info("Attempting to load Vet public key from relative path: {}", publicKeyRelativePath);
        try {
            Path publicKeyAbsolutePath = keyStorageService.getAbsolutePathForPublicKey(publicKeyRelativePath);
            String publicKeyPemContent = Files.readString(publicKeyAbsolutePath, StandardCharsets.UTF_8);
            return loadPublicKeyFromPemString(publicKeyPemContent);
        } catch (IOException e) {
            log.error("IOException loading Vet public key from path {}: {}", publicKeyRelativePath, e.getMessage(), e);
            throw new RuntimeException("Could not read Vet public key file: " + publicKeyRelativePath, e);
        } catch (GeneralSecurityException e) {
            log.error("SecurityException parsing Vet public key from path {}: {}", publicKeyRelativePath, e.getMessage(), e);
            throw new RuntimeException("Could not parse Vet public key: " + publicKeyRelativePath, e);
        } catch (Exception e) {
            log.error("Unexpected error loading Vet public key from path {}: {}", publicKeyRelativePath, e.getMessage(), e);
            throw new RuntimeException("Unexpected error loading Vet public key: " + publicKeyRelativePath, e);
        }
    }

    @Override
    public PublicKey getClinicPublicKey(Clinic clinic) {
        if (clinic == null || !StringUtils.hasText(clinic.getPublicKey())) {
            log.error("Clinic entity or publicKey path is null/empty for clinic ID: {}", (clinic != null ? clinic.getId() : "null"));
            throw new IllegalArgumentException("Clinic or Clinic's public key path is missing.");
        }
        String publicKeyRelativePath = clinic.getPublicKey();
        log.info("Attempting to load Clinic public key from relative path: {}", publicKeyRelativePath);
        try {
            Path publicKeyAbsolutePath = keyStorageService.getAbsolutePathForPublicKey(publicKeyRelativePath);
            String publicKeyPemContent = Files.readString(publicKeyAbsolutePath, StandardCharsets.UTF_8);
            return loadPublicKeyFromPemString(publicKeyPemContent);
        } catch (IOException e) {
            log.error("IOException loading Clinic public key from path {}: {}", publicKeyRelativePath, e.getMessage(), e);
            throw new RuntimeException("Could not read Clinic public key file: " + publicKeyRelativePath, e);
        } catch (GeneralSecurityException e) {
            log.error("SecurityException parsing Clinic public key from path {}: {}", publicKeyRelativePath, e.getMessage(), e);
            throw new RuntimeException("Could not parse Clinic public key: " + publicKeyRelativePath, e);
        } catch (Exception e) {
            log.error("Unexpected error loading Clinic public key from path {}: {}", publicKeyRelativePath, e.getMessage(), e);
            throw new RuntimeException("Unexpected error loading Clinic public key: " + publicKeyRelativePath, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean verifySignature(String publicKeyPemB64, String originalData, String signatureB64) {
        if (!StringUtils.hasText(publicKeyPemB64) || !StringUtils.hasText(originalData) || !StringUtils.hasText(signatureB64)) {
            log.warn("Verification skipped: Missing public key content, data, or signature.");
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
            log.error("Error verifying signature: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Generic method to load a private key from a PEM file.
     * Handles different PEM formats (PKCS#8, traditional, encrypted/unencrypted).
     *
     * @param keyPathOnServer Path to the PEM file.
     * @param password Password for the PEM file (can be null or empty if not encrypted).
     * @return The loaded PrivateKey.
     * @throws Exception if loading fails.
     */
    private PrivateKey loadPrivateKeyFromPEM(String keyPathOnServer, char[] password) throws Exception {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        try (FileReader keyReader = new FileReader(keyPathOnServer);
             PEMParser pemParser = new PEMParser(keyReader)) {
            Object pemObject = pemParser.readObject();
            if (pemObject == null) {
                throw new GeneralSecurityException("No key found in PEM file: " + keyPathOnServer);
            }
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME);
            PrivateKey privateKey;
            switch (pemObject) {
                case PrivateKeyInfo keyInfo -> privateKey = converter.getPrivateKey(keyInfo);
                case PEMKeyPair pemKeyPair -> privateKey = converter.getKeyPair(pemKeyPair).getPrivate();
                case PKCS8EncryptedPrivateKeyInfo encryptedInfo -> {
                    if (password == null || password.length == 0) throw new PEMException("Password required for encrypted key in " + keyPathOnServer);
                    InputDecryptorProvider decryptProvider = new JceOpenSSLPKCS8DecryptorProviderBuilder().build(password);
                    PrivateKeyInfo keyInfoDecrypted = encryptedInfo.decryptPrivateKeyInfo(decryptProvider);
                    privateKey = converter.getPrivateKey(keyInfoDecrypted);
                }
                case PEMEncryptedKeyPair encryptedKeyPair -> {
                    if (password == null || password.length == 0) throw new PEMException("Password required for encrypted key pair in " + keyPathOnServer);
                    PEMDecryptorProvider pemDecryptorProvider = new JcePEMDecryptorProviderBuilder().build(password);
                    PEMKeyPair decryptedKeyPair = encryptedKeyPair.decryptKeyPair(pemDecryptorProvider);
                    privateKey = converter.getKeyPair(decryptedKeyPair).getPrivate();
                }
                default -> throw new GeneralSecurityException("Unsupported private key format in PEM file: " + pemObject.getClass().getName());
            }
            if (privateKey == null) throw new GeneralSecurityException("Failed to extract private key from " + keyPathOnServer);
            log.info("Private key loaded successfully from path: {}", keyPathOnServer);
            return privateKey;
        } catch (PEMException | PKCSException | OperatorCreationException | GeneralSecurityException e) {
            log.error("Security error loading or decrypting private key from {}: {}", keyPathOnServer, e.getMessage());
            throw e;
        } catch (IOException e) {
            log.error("IOException reading private key file {}: {}", keyPathOnServer, e.getMessage());
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
        Signature signature = Signature.getInstance("SHA256withRSA", BouncyCastleProvider.PROVIDER_NAME);
        signature.initSign(privateKey);
        signature.update(dataToSign.getBytes(StandardCharsets.UTF_8));
        byte[] signatureBytes = signature.sign();
        return Base64.getEncoder().encodeToString(signatureBytes);
    }

    /**
     * Loads a PublicKey from its Base64 PEM string representation (without headers/footers).
     */
    private PublicKey loadPublicKeyFromPemString(String publicKeyPemB64) throws Exception {
        String pureBase64 = publicKeyPemB64
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s+", "");
        byte[] keyBytes = Base64.getDecoder().decode(pureBase64);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA", BouncyCastleProvider.PROVIDER_NAME);
        return kf.generatePublic(spec);
    }
}