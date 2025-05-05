package com.petconnect.backend.common.service.impl;

import com.petconnect.backend.user.domain.model.Clinic;
import com.petconnect.backend.user.domain.model.Vet;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link SigningServiceImpl}. // Probar la implementación
 * Verifies the signature generation (Vet & Clinic) and verification logic using test keys.
 * Assumes test keys are available in the specified paths relative to the project root.
 *
 * @author ibosquet
 */
@ExtendWith(MockitoExtension.class)
class SigningServiceImplTest {

    @InjectMocks
    private SigningServiceImpl signingService;

    private static final String TEST_VET_PRIVATE_KEY_PATH = "../keys/vet_private_key.pem";
    private static final String TEST_VET_PUBLIC_KEY_PATH = "../keys/vet_public_key.pem";
    private static final String TEST_VET_PRIVATE_KEY_PASSWORD = "1234";

    private static final String TEST_CLINIC_PRIVATE_KEY_PATH = "../keys/clinic_private_key.pem";
    private static final String TEST_CLINIC_PUBLIC_KEY_PATH = "../keys/clinic_public_key.pem";
    private static final String TEST_CLINIC_PRIVATE_KEY_PASSWORD = "1234";

    private static String testVetPublicKeyPemB64;
    private static String testClinicPublicKeyPemB64;

    private Vet testVet;
    private Clinic testClinic;

    /**
     * Adds BouncyCastle provider and loads the public key content once for all tests.
     * Genera nuevas claves de test si no existen.
     */
    @BeforeAll
    static void setUpClass() throws Exception {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }

        testVetPublicKeyPemB64 = loadPublicKeyForTest(TEST_VET_PUBLIC_KEY_PATH, TEST_VET_PRIVATE_KEY_PATH, TEST_VET_PRIVATE_KEY_PASSWORD);
        testClinicPublicKeyPemB64 = loadPublicKeyForTest(TEST_CLINIC_PUBLIC_KEY_PATH, TEST_CLINIC_PRIVATE_KEY_PATH, TEST_CLINIC_PRIVATE_KEY_PASSWORD);

    }

    private static String loadPublicKeyForTest(String publicKeyPath, String privateKeyPath, String password) throws Exception {
            Path pubPath = Paths.get(publicKeyPath);
            Path privPath = Paths.get(privateKeyPath);

            if (!Files.exists(privPath) || !Files.exists(pubPath)) {
                System.err.println("WARNING: Test keys not found at " + privateKeyPath + " / " + publicKeyPath);
                System.err.println("Please generate test keys using OpenSSL:");
                System.err.println("openssl genpkey -algorithm RSA -spi " + privateKeyPath + " -aes256 -pass pass:" + password + " -pkeyopt rsa_keygen_bits:2048");
                System.err.println("openssl rsa -pubout -in " + privateKeyPath + " -spi " + publicKeyPath + " -passin pass:" + password);
                return null;
            } else {
                String publicKeyPemContent = Files.readString(pubPath);
                String publicKeyB64 = publicKeyPemContent
                        .replace("-----BEGIN PUBLIC KEY-----", "")
                        .replace("-----END PUBLIC KEY-----", "")
                        .replaceAll("\\s+", "");
                assertThat(publicKeyB64).isNotBlank();
                System.out.println("Test Public Key loaded successfully from: " + publicKeyPath);
                return publicKeyB64;
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

        testClinic = new Clinic();
        testClinic.setId(1L);
        testClinic.setName("Test Signing Clinic");

        ReflectionTestUtils.setField(signingService, "vetPrivateKeyPath", TEST_VET_PRIVATE_KEY_PATH);
        ReflectionTestUtils.setField(signingService, "vetPrivateKeyPassword", TEST_VET_PRIVATE_KEY_PASSWORD.toCharArray());
        ReflectionTestUtils.setField(signingService, "clinicPrivateKeyPath", TEST_CLINIC_PRIVATE_KEY_PATH);
        ReflectionTestUtils.setField(signingService, "clinicPrivateKeyPassword", TEST_CLINIC_PRIVATE_KEY_PASSWORD.toCharArray());

        ReflectionTestUtils.setField(signingService, "simulatedVetPrivateKey", null);
        ReflectionTestUtils.setField(signingService, "simulatedClinicPrivateKey", null);
    }

    /**
     * --- Tests for generateVetSignature ---
     */
    @Nested
    @DisplayName("Vet Signature Generation")
    class VetSignatureGeneration {
        @Test
        @DisplayName("generateVetSignature should produce a non-blank Base64 string")
        void generateVetSignature_Success() {
            String data = "Vet signature data.";
            assumeVetKeysAvailable();
            String signature = signingService.generateVetSignature(testVet, data);
            assertThat(signature).isNotBlank();
            assertThat(Base64.getDecoder().decode(signature)).isNotEmpty();
        }

        @Test
        @DisplayName("generateVetSignature should throw RuntimeException if private key file not found")
        void generateVetSignature_Failure_KeyFileNotFound() {
            ReflectionTestUtils.setField(signingService, "vetPrivateKeyPath", "keys/non_existent_vet_key.pem");
            assertThatThrownBy(() -> signingService.generateVetSignature(testVet, "data"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to generate Vet digital signature");
        }

        @Test
        @DisplayName("generateVetSignature should throw RuntimeException if private key password incorrect")
        void generateVetSignature_Failure_WrongPassword() {
            assumeVetKeysAvailable();
            ReflectionTestUtils.setField(signingService, "vetPrivateKeyPassword", "WRONGPASS".toCharArray());
            assertThatThrownBy(() -> signingService.generateVetSignature(testVet, "data"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to generate Vet digital signature");
        }
    }

    /**
     * --- Tests for generateClinicSignature
     */
    @Nested
    @DisplayName("Clinic Signature Generation")
    class ClinicSignatureGeneration {
        @Test
        @DisplayName("generateClinicSignature should produce a non-blank Base64 string")
        void generateClinicSignature_Success() {
            String data = "Clinic signature data.";
            assumeClinicKeysAvailable();
            String signature = signingService.generateClinicSignature(testClinic, data);
            assertThat(signature).isNotBlank();
            assertThat(Base64.getDecoder().decode(signature)).isNotEmpty();
        }

        @Test
        @DisplayName("generateClinicSignature should throw RuntimeException if private key file not found")
        void generateClinicSignature_Failure_KeyFileNotFound() {
            ReflectionTestUtils.setField(signingService, "clinicPrivateKeyPath", "keys/non_existent_clinic_key.pem");
            assertThatThrownBy(() -> signingService.generateClinicSignature(testClinic, "data"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to generate Clinic digital signature");
        }

        @Test
        @DisplayName("generateClinicSignature should throw RuntimeException if private key password incorrect")
        void generateClinicSignature_Failure_WrongPassword() {
            assumeClinicKeysAvailable();
            ReflectionTestUtils.setField(signingService, "clinicPrivateKeyPassword", "WRONGPASS".toCharArray());
            assertThatThrownBy(() -> signingService.generateClinicSignature(testClinic, "data"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to generate Clinic digital signature");
        }
    }

    /**
     * --- Tests for verifySignature ---
     */
    @Nested
    @DisplayName("Signature Verification")
    class SignatureVerification {
        @Test
        @DisplayName("verifySignature should return true for a valid VET signature")
        void verifyVetSignature_Success_Valid() {
            String data = "Data signed by Vet.";
            assumeVetKeysAvailable();
            String signature = signingService.generateVetSignature(testVet, data);
            boolean isValid = signingService.verifySignature(testVetPublicKeyPemB64, data, signature);
            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("verifySignature should return true for a valid CLINIC signature")
        void verifyClinicSignature_Success_Valid() {
            String data = "Data signed by Clinic.";
            assumeClinicKeysAvailable();
            String signature = signingService.generateClinicSignature(testClinic, data);
            boolean isValid = signingService.verifySignature(testClinicPublicKeyPemB64, data, signature);
            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("verifySignature should return false if VET signature data is altered")
        void verifyVetSignature_Failure_DataAltered() {
            String originalData = "Sign this.";
            String alteredData = "Sign that.";
            assumeVetKeysAvailable();
            String signature = signingService.generateVetSignature(testVet, originalData);
            boolean isValid = signingService.verifySignature(testVetPublicKeyPemB64, alteredData, signature);
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("verifySignature should return false if CLINIC signature data is altered")
        void verifyClinicSignature_Failure_DataAltered() {
            String originalData = "Sign this by clinic.";
            String alteredData = "Sign that by clinic.";
            assumeClinicKeysAvailable();
            String signature = signingService.generateClinicSignature(testClinic, originalData);
            boolean isValid = signingService.verifySignature(testClinicPublicKeyPemB64, alteredData, signature);
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("verifySignature should return false if signature is invalid/corrupted")
        void verifySignature_Failure_SignatureCorrupted() {
            String data = "Some data.";
            assumeVetKeysAvailable(); // Need a valid public key for the test
            String invalidSignature = "NotABase64SignatureOrJustWrong==";
            boolean isValid = signingService.verifySignature(testVetPublicKeyPemB64, data, invalidSignature);
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("verifySignature should return false if public key is incorrect (Vet Sig)")
        void verifyVetSignature_Failure_WrongPublicKey() {
            String data = "Data for Vet Sig.";
            assumeVetKeysAvailable();
            assumeClinicKeysAvailable();
            String signature = signingService.generateVetSignature(testVet, data);

            boolean isValid = signingService.verifySignature(testClinicPublicKeyPemB64, data, signature);
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("verifySignature should return false if public key is incorrect (Clinic Sig)")
        void verifyClinicSignature_Failure_WrongPublicKey() {
            String data = "Data for Clinic Sig.";
            assumeVetKeysAvailable();
            assumeClinicKeysAvailable();
            String signature = signingService.generateClinicSignature(testClinic, data);

            boolean isValid = signingService.verifySignature(testVetPublicKeyPemB64, data, signature);
            assertThat(isValid).isFalse();
        }
    }

    /**
     * --- Tests for getVetPrivateKey ---
     */
    @Nested
    @DisplayName("getVetPrivateKey Tests")
    class GetVetPrivateKeyTests {

        @Test
        @DisplayName("should return non-null PrivateKey when key file exists")
        void getVetPrivateKey_Success() {
            assumeVetKeysAvailable(); // Ensure test keys exist for loading

            PrivateKey key = signingService.getVetPrivateKey(testVet);

            assertThat(key).isNotNull();
            assertThat(key.getAlgorithm()).isEqualTo("RSA"); // Check algorithm
        }

        @Test
        @DisplayName("should throw RuntimeException when key file not found")
        void getVetPrivateKey_Failure_NotFound() {
            ReflectionTestUtils.setField(signingService, "vetPrivateKeyPath", "non/existent/path.pem");
            ReflectionTestUtils.setField(signingService, "simulatedVetPrivateKey", null); // Reset cache

            assertThatThrownBy(() -> signingService.getVetPrivateKey(testVet))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Could not load Vet private key");
        }
    }

    /**
     * --- Tests for getVetPublicKey ---
     */
    @Nested
    @DisplayName("getVetPublicKey Tests")
    class GetVetPublicKeyTests {

        @Test
        @DisplayName("should return non-null PublicKey when key file exists")
        void getVetPublicKey_Success() {
            assumeVetKeysAvailable();

            PublicKey key = signingService.getVetPublicKey(testVet);

            assertThat(key).isNotNull();
            assertThat(key.getAlgorithm()).isEqualTo("RSA");
        }

        @Test
        @DisplayName("should throw RuntimeException when key file not found for derivation")
        void getVetPublicKey_Failure_NotFound() {
            ReflectionTestUtils.setField(signingService, "vetPrivateKeyPath", "non/existent/path.pem");
            ReflectionTestUtils.setField(signingService, "simulatedVetPrivateKey", null); // Reset cache

            assertThatThrownBy(() -> signingService.getVetPublicKey(testVet))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Could not load/derive Vet public key");
        }
    }

    /**
     * --- Tests for getClinicPrivateKey
     */
    @Nested
    @DisplayName("getClinicPrivateKey Tests")
    class GetClinicPrivateKeyTests {

        @Test
        @DisplayName("should return non-null PrivateKey when key file exists")
        void getClinicPrivateKey_Success() {
            assumeClinicKeysAvailable();

            PrivateKey key = signingService.getClinicPrivateKey(testClinic);

            assertThat(key).isNotNull();
            assertThat(key.getAlgorithm()).isEqualTo("RSA");
        }

        @Test
        @DisplayName("should throw RuntimeException when key file not found")
        void getClinicPrivateKey_Failure_NotFound() {
            ReflectionTestUtils.setField(signingService, "clinicPrivateKeyPath", "non/existent/path.pem");
            ReflectionTestUtils.setField(signingService, "simulatedClinicPrivateKey", null); // Reset cache

            assertThatThrownBy(() -> signingService.getClinicPrivateKey(testClinic))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Could not load Clinic private key");
        }
    }

    /**
     * --- Tests for getClinicPublicKey
     */
    @Nested
    @DisplayName("getClinicPublicKey Tests")
    class GetClinicPublicKeyTests {

        @Test
        @DisplayName("should return non-null PublicKey when key file exists")
        void getClinicPublicKey_Success() {
            assumeClinicKeysAvailable();

            PublicKey key = signingService.getClinicPublicKey(testClinic);

            assertThat(key).isNotNull();
            assertThat(key.getAlgorithm()).isEqualTo("RSA");
        }

        @Test
        @DisplayName("should throw RuntimeException when key file not found for derivation")
        void getClinicPublicKey_Failure_NotFound() {
            ReflectionTestUtils.setField(signingService, "clinicPrivateKeyPath", "non/existent/path.pem");
            ReflectionTestUtils.setField(signingService, "simulatedClinicPrivateKey", null); // Reset cache

            assertThatThrownBy(() -> signingService.getClinicPublicKey(testClinic))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Could not load/derive Clinic public key");
        }
    }

    /**
     * --- Helper Methods for Assumptions ---
     */
    private void assumeVetKeysAvailable() {
        Assumptions.assumeTrue(testVetPublicKeyPemB64 != null,
                "Skipping test because VET test public key could not be loaded.");
    }

    private void assumeClinicKeysAvailable() {
        Assumptions.assumeTrue(testClinicPublicKeyPemB64 != null,
                "Skipping test because CLINIC test public key could not be loaded.");
    }
}