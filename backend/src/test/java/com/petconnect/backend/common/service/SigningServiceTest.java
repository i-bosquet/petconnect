package com.petconnect.backend.common.service;

import com.petconnect.backend.user.domain.model.Vet;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Security;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link SigningService}.
 * Verifies the signature generation and verification logic using test keys.
 * Assumes test keys are available in the specified paths relative to the project root.
 *
 * @author ibosquet
 */
@ExtendWith(MockitoExtension.class)
class SigningServiceTest {

    @InjectMocks
    private SigningService signingService;

    private static final String TEST_PRIVATE_KEY_PATH = "../keys/vet_private_key.pem";
    private static final String TEST_PUBLIC_KEY_PATH = "../keys/vet_public_key.pem";
    private static final String TEST_PRIVATE_KEY_PASSWORD = "1234";

    private static String testPublicKeyPemB64;

    private Vet testVet;

    /**
     * Adds BouncyCastle provider and loads the public key content once for all tests.
     * Genera nuevas claves de test si no existen.
     */
    @BeforeAll
    static void setUpClass() throws Exception {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        if (!Files.exists(Paths.get(TEST_PRIVATE_KEY_PATH)) || !Files.exists(Paths.get(TEST_PUBLIC_KEY_PATH))) {
            System.err.println("WARNING: Test keys not found at " + TEST_PRIVATE_KEY_PATH + " / " + TEST_PUBLIC_KEY_PATH);
            System.err.println("Please generate test keys using OpenSSL:");
            System.err.println("openssl genpkey -algorithm RSA -out " + TEST_PRIVATE_KEY_PATH + " -aes256 -pass pass:" + TEST_PRIVATE_KEY_PASSWORD + " -pkeyopt rsa_keygen_bits:2048");
            System.err.println("openssl rsa -pubout -in " + TEST_PRIVATE_KEY_PATH + " -out " + TEST_PUBLIC_KEY_PATH + " -passin pass:" + TEST_PRIVATE_KEY_PASSWORD);
            testPublicKeyPemB64 = null;
        } else {
            String publicKeyPemContent = Files.readString(Paths.get(TEST_PUBLIC_KEY_PATH));
            testPublicKeyPemB64 = publicKeyPemContent
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s+", "");
            assertThat(testPublicKeyPemB64).isNotBlank();
            System.out.println("Test Public Key loaded successfully for verification tests.");
        }
    }

    /**
     * Sets up the test Vet instance and injects test key configuration into the service.
     */
    @BeforeEach
    void setUp() {
        testVet = new Vet();
        testVet.setId(99L);
        testVet.setUsername("testsignvet");

        ReflectionTestUtils.setField(signingService, "privateKeyPath", TEST_PRIVATE_KEY_PATH);
        ReflectionTestUtils.setField(signingService, "privateKeyPassword", TEST_PRIVATE_KEY_PASSWORD.toCharArray());
        ReflectionTestUtils.setField(signingService, "simulatedVetPrivateKey", null);
    }

    /**
     * Test successful signature generation.
     */
    @Test
    @DisplayName("generateSignature should produce a non-blank Base64 string")
    void generateSignature_Success() {
        // Arrange
        String data = "This is the data to be signed.";
        assumePublicKeyIsAvailable();

        // Act
        String signature = signingService.generateSignature(testVet, data);

        // Assert
        assertThat(signature).isNotBlank();
        assertThat(Base64.getDecoder().decode(signature)).isNotEmpty();
        System.out.println("Generated Signature: " + signature);
    }

    /**
     * Test successful signature verification with correct data and keys.
     */
    @Test
    @DisplayName("verifySignature should return true for a valid signature")
    void verifySignature_Success_Valid() {
        // Arrange
        String data = "Data that was signed.";
        assumePublicKeyIsAvailable();
        String signature = signingService.generateSignature(testVet, data);
        assertThat(signature).isNotBlank();

        // Act
        boolean isValid = signingService.verifySignature(testPublicKeyPemB64, data, signature);

        // Assert
        assertThat(isValid).isTrue();
    }

    /**
     * Test signature verification failure when the original data is altered.
     */
    @Test
    @DisplayName("verifySignature should return false if original data is altered")
    void verifySignature_Failure_DataAltered() {
        // Arrange
        String originalData = "Sign this data.";
        String alteredData = "Sign this data ALTERED.";
        assumePublicKeyIsAvailable();
        String signature = signingService.generateSignature(testVet, originalData);
        assertThat(signature).isNotBlank();

        // Act
        boolean isValid = signingService.verifySignature(testPublicKeyPemB64, alteredData, signature);

        // Assert
        assertThat(isValid).isFalse();
    }

    /**
     * Test signature verification failure when the signature itself is corrupted/invalid.
     */
    @Test
    @DisplayName("verifySignature should return false if signature is invalid/corrupted")
    void verifySignature_Failure_SignatureCorrupted() {
        // Arrange
        String data = "Some valid data.";
        assumePublicKeyIsAvailable();
        String invalidSignature = "ThisIsNotABase64SignatureOrItsInvalid";

        // Act
        boolean isValid = signingService.verifySignature(testPublicKeyPemB64, data, invalidSignature);

        // Assert
        assertThat(isValid).isFalse();
    }

    /**
     * Test signature verification failure when the public key is incorrect.
     */
    @Test
    @DisplayName("verifySignature should return false if public key is incorrect")
    void verifySignature_Failure_WrongPublicKey() throws Exception {
        // Arrange
        String data = "Important record data.";
        assumePublicKeyIsAvailable();
        String signature = signingService.generateSignature(testVet, data);

        // Generate a DIFFERENT key pair for verification failure
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair wrongKeyPair = kpg.generateKeyPair();
        String wrongPublicKeyPemB64 = Base64.getEncoder().encodeToString(wrongKeyPair.getPublic().getEncoded());

        // Act
        boolean isValid = signingService.verifySignature(wrongPublicKeyPemB64, data, signature);

        // Assert
        assertThat(isValid).isFalse();
    }

    /**
     * Test failure during signature generation if the private key file is missing.
     */
    @Test
    @DisplayName("generateSignature should throw RuntimeException if private key file not found")
    void generateSignature_Failure_KeyFileNotFound() {
        // Arrange
        String data = "Data.";

        ReflectionTestUtils.setField(signingService, "privateKeyPath", "keys/non_existent_key.pem");
        ReflectionTestUtils.setField(signingService, "simulatedVetPrivateKey", null);

        // Act & Assert
        assertThatThrownBy(() -> signingService.generateSignature(testVet, data))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Unexpected error during signature generation");
    }

    /**
     * Test failure during signature generation if the private key password is incorrect.
     * Note: This depends on the key being password protected in a way BouncyCastle PEMParser handles.
     */
    @Test
    @DisplayName("generateSignature should throw RuntimeException if private key password incorrect")
    void generateSignature_Failure_WrongPassword() {
        // Arrange
        String data = "Data.";
        assumePublicKeyIsAvailable();
        // Override password
        ReflectionTestUtils.setField(signingService, "privateKeyPassword", "wrongpassword".toCharArray());
        ReflectionTestUtils.setField(signingService, "simulatedVetPrivateKey", null);

        // Act & Assert
        assertThatThrownBy(() -> signingService.generateSignature(testVet, data))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to generate digital signature");
    }

    /**
     * Helper method to skip tests if the public key couldn't be loaded in @BeforeAll.
     */
    private void assumePublicKeyIsAvailable() {
        org.junit.jupiter.api.Assumptions.assumeTrue(testPublicKeyPemB64 != null,
                "Skipping test because test public key could not be loaded.");
    }
}