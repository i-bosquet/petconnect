package com.petconnect.backend.common.service.impl;

import com.petconnect.backend.common.service.KeyStorageService;
import com.petconnect.backend.common.service.SigningService;
import com.petconnect.backend.user.domain.model.Clinic;
import com.petconnect.backend.user.domain.model.Vet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
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

import java.io.*;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.KeyFactory;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
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
            log.error("Vet or vetPrivateKey path (S3 key) is missing for Vet ID: {}", vet != null ? vet.getId() : "null");
            throw new IllegalArgumentException("Veterinarian's private key S3 key not configured.");
        }
        if (vetKeyPassword == null || vetKeyPassword.length == 0) {
            log.error("Password for Vet's private key (ID: {}) was not provided.", vet.getId());
            throw new IllegalArgumentException("Password for veterinarian's private key is required for signing.");
        }

        log.info("Attempting to generate Vet signature for Vet ID: {}", vet.getId());
        try (InputStream privateKeyStream = keyStorageService.getPrivateKeyContent(vet.getVetPrivateKey())) {
            PrivateKey vetPrivateKeyLoaded = loadPrivateKeyFromPEMStream(privateKeyStream, vetKeyPassword, "Vet " + vet.getId());
            return signData(vetPrivateKeyLoaded, dataToSign);
        } catch (Exception e) {
            log.error("Error generating Vet signature for Vet ID {}: {}", vet.getId(), e.getMessage(), e);
            if (e instanceof PEMException || e instanceof PKCSException ||
                    (e.getCause() instanceof IOException && e.getCause().getMessage() != null &&
                            e.getCause().getMessage().contains("AEADBadTagException")) ||
                    (e.getMessage() != null && e.getMessage().contains("decryption failed"))) {
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
        try (InputStream privateKeyStream = keyStorageService.getPrivateKeyContent(clinic.getPrivateKey())) {
            PrivateKey clinicPkLoaded = loadPrivateKeyFromPEMStream(privateKeyStream, clinicKeyPassword, "Clinic " + clinic.getId());
            return signData(clinicPkLoaded, dataToSign);
        } catch (Exception e) {
            log.error("Error generating Clinic signature for Clinic ID {}: {}", clinic.getId(), e.getMessage(), e);
            if (e instanceof PEMException || e instanceof PKCSException ||
                    (e.getCause() instanceof IOException && e.getCause().getMessage() != null &&
                            e.getCause().getMessage().contains("AEADBadTagException")) ||
                    (e.getMessage() != null && e.getMessage().contains("decryption failed"))) {
                throw new RuntimeException("Failed to decrypt Clinic private key. Incorrect password or key format for Clinic ID " + clinic.getId() + ".", e);
            }
            throw new RuntimeException("Failed to generate Clinic digital signature for Clinic ID " + clinic.getId() + ".", e);
        } finally {
            Arrays.fill(clinicKeyPassword, ' ');
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PublicKey getVetPublicKey(Vet vet) {
        if (vet == null || !StringUtils.hasText(vet.getVetPublicKey())) {
            log.error("Vet entity or vetPublicKey path is null/empty for vet ID: {}", (vet != null ? vet.getId() : "null"));
            throw new IllegalArgumentException("Vet or Vet's public key path is missing.");
        }
        String publicKeyRelativePath = vet.getVetPublicKey();
        log.info("Attempting to load Vet public key from relative path: {}", publicKeyRelativePath);
        try (InputStream publicKeyStream = keyStorageService.getPublicKeyContent(publicKeyRelativePath)) {
            return loadPublicKeyFromPemStream(publicKeyStream, "Vet " + vet.getId());
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

    /**
     * {@inheritDoc}
     */
    @Override
    public PublicKey getClinicPublicKey(Clinic clinic) {
        if (clinic == null || !StringUtils.hasText(clinic.getPublicKey())) {
            log.error("Clinic entity or publicKey path is null/empty for clinic ID: {}", (clinic != null ? clinic.getId() : "null"));
            throw new IllegalArgumentException("Clinic or Clinic's public key path is missing.");
        }
        String publicKeyRelativePath = clinic.getPublicKey();
        log.info("Attempting to load Clinic public key from relative path: {}", publicKeyRelativePath);
        try  (InputStream publicKeyStream = keyStorageService.getPublicKeyContent(publicKeyRelativePath)) {
            return loadPublicKeyFromPemStream(publicKeyStream, "Clinic " + clinic.getId());
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
            PublicKey publicKey = loadPublicKeyFromPemStringContent(publicKeyPemB64);
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

    // Private methods

    /**
     * Loads a private key from a PEM-encoded input stream.
     * The method supports both encrypted and unencrypted keys and uses the BouncyCastle library for parsing and decryption.
     *
     * @param keyStream the input stream containing the PEM-encoded private key; must not be null
     * @param password the password for decrypting the private key, if encrypted, may be null or empty for unencrypted keys
     * @param keyIdentifierInfo a string identifier to provide additional context in case of errors or logs
     * @return the {@code PrivateKey} object extracted from the PEM stream
     * @throws IOException if there is an issue reading the input stream or if the stream is null
     * @throws GeneralSecurityException if the private key cannot be extracted or an unsupported format is encountered
     * @throws PEMException if an error occurs while handling encrypted keys and a password is required but not provided
     * @throws PKCSException if a PKCS-specific error occurs during private key decryption
     * @throws OperatorCreationException if an error occurs while creating a decryption operator
     */
    private PrivateKey loadPrivateKeyFromPEMStream(InputStream keyStream, char[] password, String keyIdentifierInfo) throws Exception {
        if (keyStream == null) {
            throw new IOException("Key stream cannot be null for " + keyIdentifierInfo);
        }
        try (InputStreamReader keyReader = new InputStreamReader(keyStream, StandardCharsets.UTF_8);
             PEMParser pemParser = new PEMParser(keyReader)) {
            Object pemObject = pemParser.readObject();
            if (pemObject == null) {
                throw new GeneralSecurityException("No key found in PEM stream for " + keyIdentifierInfo);
            }
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME);
            PrivateKey privateKey;
            switch (pemObject) {
                case PrivateKeyInfo keyInfo -> privateKey = converter.getPrivateKey(keyInfo);
                case PEMKeyPair pemKeyPair -> privateKey = converter.getKeyPair(pemKeyPair).getPrivate();
                case PKCS8EncryptedPrivateKeyInfo encryptedInfo -> {
                    if (password == null || password.length == 0) throw new PEMException("Password required for encrypted key in stream for " + keyIdentifierInfo);
                    InputDecryptorProvider decryptProvider = new JceOpenSSLPKCS8DecryptorProviderBuilder().build(password);
                    PrivateKeyInfo keyInfoDecrypted = encryptedInfo.decryptPrivateKeyInfo(decryptProvider);
                    privateKey = converter.getPrivateKey(keyInfoDecrypted);
                }
                case PEMEncryptedKeyPair encryptedKeyPair -> {
                    if (password == null || password.length == 0) throw new PEMException("Password required for encrypted key pair in stream for " + keyIdentifierInfo);
                    PEMDecryptorProvider pemDecryptorProvider = new JcePEMDecryptorProviderBuilder().build(password);
                    PEMKeyPair decryptedKeyPair = encryptedKeyPair.decryptKeyPair(pemDecryptorProvider);
                    privateKey = converter.getKeyPair(decryptedKeyPair).getPrivate();
                }
                default -> throw new GeneralSecurityException("Unsupported private key format in PEM stream for " + keyIdentifierInfo + ": " + pemObject.getClass().getName());
            }
            if (privateKey == null) throw new GeneralSecurityException("Failed to extract private key from PEM stream for " + keyIdentifierInfo);
            log.info("Private key for {} loaded successfully from stream.", keyIdentifierInfo);
            return privateKey;
        } catch (PEMException | PKCSException | OperatorCreationException | GeneralSecurityException e) {
            log.error("Security error loading or decrypting private key from stream for {}: {}", keyIdentifierInfo, e.getMessage());
            throw e;
        } catch (IOException e) {
            log.error("IOException reading private key stream for {}: {}", keyIdentifierInfo, e.getMessage());
            throw e;
        }
    }

    /**
     * Loads a public key from a PEM formatted InputStream.
     * Supports PEM objects of type SubjectPublicKeyInfo and X509CertificateHolder.
     *
     * @param keyStream the InputStream containing the PEM encoded public key. Must not be null.
     * @param keyIdentifierInfo additional identifier information for the key, used in exception messages for context.
     * @return the extracted PublicKey instance.
     * @throws IOException if the provided keyStream is null or an I/O error occurs.
     * @throws GeneralSecurityException if the PEM object format is unsupported or the public key cannot be parsed.
     * @throws Exception if any other unexpected issue occurs during processing.
     */
    private PublicKey loadPublicKeyFromPemStream(InputStream keyStream, String keyIdentifierInfo) throws Exception {
        if (keyStream == null) {
            throw new IOException("Key stream cannot be null for " + keyIdentifierInfo);
        }
        try (InputStreamReader keyReader = new InputStreamReader(keyStream, StandardCharsets.UTF_8);
             PEMParser pemParser = new PEMParser(keyReader)) {
            Object pemObject = pemParser.readObject();
            if (pemObject == null) {
                throw new GeneralSecurityException("No public key data found in PEM stream for " + keyIdentifierInfo);
            }

            JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME);
            return switch (pemObject) {
                case SubjectPublicKeyInfo subjectPublicKeyInfo -> converter.getPublicKey(subjectPublicKeyInfo);
                case X509CertificateHolder certHolder ->
                        new JcaX509CertificateConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME)
                                .getCertificate(certHolder).getPublicKey();
                case PEMKeyPair pemKeyPair -> converter.getKeyPair(pemKeyPair).getPublic();
                default ->
                        throw new GeneralSecurityException("Unsupported public key format in PEM stream for " + keyIdentifierInfo + ": " + pemObject.getClass().getName());
            };
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


    private PublicKey loadPublicKeyFromPemStringContent(String publicKeyPemContent) throws Exception {
        String pureBase64 = publicKeyPemContent
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replace("-----BEGIN CERTIFICATE-----", "")
                .replace("-----END CERTIFICATE-----", "")
                .replaceAll("\\s+", "");
        byte[] keyBytes = Base64.getDecoder().decode(pureBase64);

        try {
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA", BouncyCastleProvider.PROVIDER_NAME);
            return kf.generatePublic(spec);
        } catch (InvalidKeySpecException e) {
            log.warn("Failed to parse as X509EncodedKeySpec, attempting as Certificate for content of length: {}", publicKeyPemContent.length());
            try (InputStream certStream = new ByteArrayInputStream(keyBytes)) {
                CertificateFactory cf = CertificateFactory.getInstance("X.509", BouncyCastleProvider.PROVIDER_NAME);
                X509Certificate certificate = (X509Certificate) cf.generateCertificate(certStream);
                return certificate.getPublicKey();
            } catch (Exception certEx) {
                log.error("Failed to parse public key content as X509EncodedKeySpec or X509Certificate", certEx);
                throw new GeneralSecurityException("Could not parse public key content.", certEx);
            }
        }
    }
}