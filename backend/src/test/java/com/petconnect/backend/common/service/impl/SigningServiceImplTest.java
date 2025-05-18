package com.petconnect.backend.common.service.impl;

import com.petconnect.backend.common.service.KeyStorageService;
import com.petconnect.backend.user.domain.model.Clinic;
import com.petconnect.backend.user.domain.model.Vet;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PublicKey;
import java.security.Security;
import java.util.Base64;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SigningServiceImpl}.
 * Verifies the signature generation (Vet & Clinic) and verification logic using test keys.
 * Assumes test keys are available in the specified paths relative to the project root.
 *
 * @author ibosquet
 */
@ExtendWith(MockitoExtension.class)
class SigningServiceImplTest {

    @Mock
    private KeyStorageService keyStorageService;

    @InjectMocks
    private SigningServiceImpl signingService;

    private static final String TEST_VET_RELATIVE_PRIVATE_KEY_PATH = "keys_for_test/vet_private_key.pem";
    private static final String TEST_VET_RELATIVE_PUBLIC_KEY_PATH = "keys_for_test/vet_public_key.pem";
    private static final String TEST_VET_PRIVATE_KEY_PASSWORD = "1234";

    private static final String TEST_CLINIC_RELATIVE_PRIVATE_KEY_PATH = "keys_for_test/clinic_private_key.pem";
    private static final String TEST_CLINIC_RELATIVE_PUBLIC_KEY_PATH = "keys_for_test/clinic_public_key.pem";
    private static final String TEST_CLINIC_PRIVATE_KEY_PASSWORD = "1234";

    private static Path vetTestPrivateKeyFile;
    private static Path vetTestPublicKeyFile;
    private static Path clinicTestPrivateKeyFile;
    private static Path clinicTestPublicKeyFile;

    private static String testVetPublicKeyPemB64;
    private static String testClinicPublicKeyPemB64;

    private Vet testVet;
    private Clinic testClinic;

    /**
     * Adds BouncyCastle provider and loads the public key content once for all tests.
     * Genera nuevas claves de test si no existen.
     */
    @BeforeAll
    static void setUpClass()  {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }

        vetTestPrivateKeyFile = Paths.get("src/test/resources/" + TEST_VET_RELATIVE_PRIVATE_KEY_PATH).toAbsolutePath();
        vetTestPublicKeyFile = Paths.get("src/test/resources/" + TEST_VET_RELATIVE_PUBLIC_KEY_PATH).toAbsolutePath();
        clinicTestPrivateKeyFile = Paths.get("src/test/resources/" + TEST_CLINIC_RELATIVE_PRIVATE_KEY_PATH).toAbsolutePath();
        clinicTestPublicKeyFile = Paths.get("src/test/resources/" + TEST_CLINIC_RELATIVE_PUBLIC_KEY_PATH).toAbsolutePath();

        try {
            assumeTrue(Files.exists(vetTestPrivateKeyFile), "Vet private test key file missing: " + vetTestPrivateKeyFile);
            assumeTrue(Files.exists(vetTestPublicKeyFile), "Vet public test key file missing: " + vetTestPublicKeyFile);
            testVetPublicKeyPemB64 = Files.readString(vetTestPublicKeyFile, StandardCharsets.UTF_8)
                    .replace("-----BEGIN PUBLIC KEY-----", "").replace("-----END PUBLIC KEY-----", "").replaceAll("\\s+", "");
        } catch (IOException e) {
            System.err.println("FATAL: Could not read Vet test public key: " + e.getMessage());
            testVetPublicKeyPemB64 = null;
        } catch (Exception e) {
            System.err.println("FATAL: Unexpected error loading Vet test public key: " + e.getMessage());
            testVetPublicKeyPemB64 = null;
        }

        try {
            assumeTrue(Files.exists(clinicTestPrivateKeyFile), "Clinic private test key file missing: " + clinicTestPrivateKeyFile);
            assumeTrue(Files.exists(clinicTestPublicKeyFile), "Clinic public test key file missing: " + clinicTestPublicKeyFile);
            testClinicPublicKeyPemB64 = Files.readString(clinicTestPublicKeyFile, StandardCharsets.UTF_8)
                    .replace("-----BEGIN PUBLIC KEY-----", "").replace("-----END PUBLIC KEY-----", "").replaceAll("\\s+", "");
        } catch (IOException e) {
            System.err.println("FATAL: Could not read Clinic test public key: " + e.getMessage());
            testClinicPublicKeyPemB64 = null;
        } catch (Exception e) {
            System.err.println("FATAL: Unexpected error loading Clinic test public key: " + e.getMessage());
            testClinicPublicKeyPemB64 = null;
        }
    }

    @BeforeEach
    void setUp() {
        testVet = new Vet();
        testVet.setId(99L);
        testVet.setUsername("investigate");
        testVet.setVetPrivateKey(TEST_VET_RELATIVE_PRIVATE_KEY_PATH);
        testVet.setVetPublicKey(TEST_VET_RELATIVE_PUBLIC_KEY_PATH);

        testClinic = new Clinic();
        testClinic.setId(1L);
        testClinic.setName("Test Signing Clinic");
        testClinic.setPrivateKey(TEST_CLINIC_RELATIVE_PRIVATE_KEY_PATH);
        testClinic.setPublicKey(TEST_CLINIC_RELATIVE_PUBLIC_KEY_PATH);

        lenient().when(keyStorageService.getAbsolutePathForPrivateKey(TEST_VET_RELATIVE_PRIVATE_KEY_PATH))
                .thenReturn(vetTestPrivateKeyFile);
        lenient().when(keyStorageService.getAbsolutePathForPublicKey(TEST_VET_RELATIVE_PUBLIC_KEY_PATH))
                .thenReturn(vetTestPublicKeyFile);
        lenient().when(keyStorageService.getAbsolutePathForPrivateKey(TEST_CLINIC_RELATIVE_PRIVATE_KEY_PATH))
                .thenReturn(clinicTestPrivateKeyFile);
        lenient().when(keyStorageService.getAbsolutePathForPublicKey(TEST_CLINIC_RELATIVE_PUBLIC_KEY_PATH))
                .thenReturn(clinicTestPublicKeyFile);
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
            String signature = signingService.generateVetSignature(testVet, data, TEST_VET_PRIVATE_KEY_PASSWORD.toCharArray());
            assertThat(signature).isNotBlank();
            assertThat(Base64.getDecoder().decode(signature)).isNotEmpty();
        }

        @Test
        @DisplayName("generateVetSignature should throw RuntimeException if key file resolution fails")
        void generateVetSignature_Failure_KeyFileResolutionError() {
            when(keyStorageService.getAbsolutePathForPrivateKey(TEST_VET_RELATIVE_PRIVATE_KEY_PATH))
                    .thenThrow(new IllegalArgumentException("Simulated path resolution error"));
            Throwable thrown = catchThrowable(() -> signingService.generateVetSignature(testVet, "data", TEST_VET_PRIVATE_KEY_PASSWORD.toCharArray()));
            assertThat(thrown)
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to generate Vet digital signature");
        }

        @Test
        @DisplayName("generateVetSignature should throw RuntimeException if key file is not found by FileReader")
        void generateVetSignature_Failure_KeyFileNotFoundByFileReader() {
            when(keyStorageService.getAbsolutePathForPrivateKey(TEST_VET_RELATIVE_PRIVATE_KEY_PATH))
                    .thenReturn(Paths.get("src/test/resources/keys_for_test/non_existent_vet_key.pem").toAbsolutePath());

            Throwable thrown = catchThrowable(() ->signingService.generateVetSignature(testVet, "data", TEST_VET_PRIVATE_KEY_PASSWORD.toCharArray()));
            assertThat(thrown)
                .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to generate Vet digital signature")
                    .hasCauseInstanceOf(FileNotFoundException.class);
        }


        @Test
        @DisplayName("generateVetSignature should throw RuntimeException if private key password incorrect")
        void generateVetSignature_Failure_WrongPassword() {
            Throwable thrown = catchThrowable(() -> signingService.generateVetSignature(testVet, "data", "WRONGNESS".toCharArray()));
            assertThat(thrown)
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to decrypt Vet private key");
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
            String signature = signingService.generateClinicSignature(testClinic, data, TEST_CLINIC_PRIVATE_KEY_PASSWORD.toCharArray());
            assertThat(signature).isNotBlank();
            assertThat(Base64.getDecoder().decode(signature)).isNotEmpty();
        }

        @Test
        @DisplayName("generateClinicSignature should throw RuntimeException if key file resolution fails")
        void generateClinicSignature_Failure_KeyFileResolutionError() {
            when(keyStorageService.getAbsolutePathForPrivateKey(TEST_CLINIC_RELATIVE_PRIVATE_KEY_PATH))
                    .thenThrow(new IllegalArgumentException("Simulated path resolution error"));
            Throwable thrown = catchThrowable(() -> signingService.generateClinicSignature(testClinic, "data", TEST_CLINIC_PRIVATE_KEY_PASSWORD.toCharArray()));
            assertThat(thrown)
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to generate Clinic digital signature");
        }

        @Test
        @DisplayName("generateClinicSignature should throw RuntimeException if key file is not found by FileReader")
        void generateClinicSignature_Failure_KeyFileNotFoundByFileReader() {
            when(keyStorageService.getAbsolutePathForPrivateKey(TEST_CLINIC_RELATIVE_PRIVATE_KEY_PATH))
                    .thenReturn(Paths.get("src/test/resources/keys_for_test/non_existent_clinic_key.pem").toAbsolutePath());
            Throwable thrown = catchThrowable(() -> signingService.generateClinicSignature(testClinic, "data", TEST_CLINIC_PRIVATE_KEY_PASSWORD.toCharArray()));
            assertThat(thrown)
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to generate Clinic digital signature")
                    .hasCauseInstanceOf(FileNotFoundException.class);
        }

        @Test
        @DisplayName("generateClinicSignature should throw RuntimeException if private key password incorrect")
        void generateClinicSignature_Failure_WrongPassword() {
            Throwable thrown = catchThrowable(() -> signingService.generateClinicSignature(testClinic, "data", "WRONGNESS".toCharArray()));
            assertThat(thrown)
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to decrypt Clinic private key");
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
            String signature = signingService.generateVetSignature(testVet, data, TEST_VET_PRIVATE_KEY_PASSWORD.toCharArray());
            boolean isValid = signingService.verifySignature(testVetPublicKeyPemB64, data, signature);
            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("verifySignature should return true for a valid CLINIC signature")
        void verifyClinicSignature_Success_Valid() {
            String data = "Data signed by Clinic.";
            String signature = signingService.generateClinicSignature(testClinic, data, TEST_CLINIC_PRIVATE_KEY_PASSWORD.toCharArray());
            boolean isValid = signingService.verifySignature(testClinicPublicKeyPemB64, data, signature);
            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("verifySignature should return false if VET signature data is altered")
        void verifyVetSignature_Failure_DataAltered() {
            String originalData = "Sign this.";
            String alteredData = "Sign that.";
            assumeVetKeysAvailable();
            String signature = signingService.generateVetSignature(testVet, originalData, TEST_VET_PRIVATE_KEY_PASSWORD.toCharArray());
            boolean isValid = signingService.verifySignature(testVetPublicKeyPemB64, alteredData, signature);
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("verifySignature should return false if CLINIC signature data is altered")
        void verifyClinicSignature_Failure_DataAltered() {
            String originalData = "Sign this by clinic.";
            String alteredData = "Sign that by clinic.";
            assumeClinicKeysAvailable();
            String signature = signingService.generateClinicSignature(testClinic, originalData, TEST_CLINIC_PRIVATE_KEY_PASSWORD.toCharArray());
            boolean isValid = signingService.verifySignature(testClinicPublicKeyPemB64, alteredData, signature);
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("verifySignature should return false if signature is invalid/corrupted")
        void verifySignature_Failure_SignatureCorrupted() {
            String data = "Some data.";
            assumeVetKeysAvailable();
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
            String signature = signingService.generateVetSignature(testVet, data, TEST_VET_PRIVATE_KEY_PASSWORD.toCharArray());

            boolean isValid = signingService.verifySignature(testClinicPublicKeyPemB64, data, signature);
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("verifySignature should return false if public key is incorrect (Clinic Sig)")
        void verifyClinicSignature_Failure_WrongPublicKey() {
            String data = "Data for Clinic Sig.";
            assumeVetKeysAvailable();
            assumeClinicKeysAvailable();
            String signature = signingService.generateClinicSignature(testClinic, data, TEST_CLINIC_PRIVATE_KEY_PASSWORD.toCharArray());

            boolean isValid = signingService.verifySignature(testVetPublicKeyPemB64, data, signature);
            assertThat(isValid).isFalse();
        }
    }

    /**
     * --- Tests for getPublicKey ---
     */
    @Nested
    @DisplayName("Get Public Key Methods")
    class GetPublicKeyMethods {
        @Test
        @DisplayName("getVetPublicKey should return PublicKey when key file exists")
        void getVetPublicKey_Success() {
            PublicKey key = signingService.getVetPublicKey(testVet);
            assertThat(key).isNotNull();
            assertThat(key.getAlgorithm()).isEqualTo("RSA");
        }

        @Test
        @DisplayName("getVetPublicKey should throw RuntimeException when KeyStorageService fails to resolve or read public key path")
        void getVetPublicKey_Failure_KeyStorageError() {
            when(keyStorageService.getAbsolutePathForPublicKey(TEST_VET_RELATIVE_PUBLIC_KEY_PATH))
                    .thenThrow(new RuntimeException("Simulated KeyStorageService error"));

            assertThatThrownBy(() -> signingService.getVetPublicKey(testVet))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Unexpected error loading Vet public key: " + TEST_VET_RELATIVE_PUBLIC_KEY_PATH);
        }

        @Test
        @DisplayName("getClinicPublicKey should return PublicKey when key file exists")
        void getClinicPublicKey_Success() {
            PublicKey key = signingService.getClinicPublicKey(testClinic);
            assertThat(key).isNotNull();
            assertThat(key.getAlgorithm()).isEqualTo("RSA");
        }

        @Test
        @DisplayName("getClinicPublicKey should throw RuntimeException when KeyStorageService fails to resolve or read public key path")
        void getClinicPublicKey_Failure_KeyStorageError() {
            when(keyStorageService.getAbsolutePathForPublicKey(TEST_CLINIC_RELATIVE_PUBLIC_KEY_PATH))
                    .thenThrow(new RuntimeException("Simulated KeyStorageService error"));

            assertThatThrownBy(() -> signingService.getClinicPublicKey(testClinic))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Unexpected error loading Clinic public key: " + TEST_CLINIC_RELATIVE_PUBLIC_KEY_PATH);
        }
    }

    /**
     * --- Helper Methods for Assumptions ---
     */
    private void assumeVetKeysAvailable() {
        assumeTrue(Files.exists(vetTestPrivateKeyFile), "Vet private test key file missing: " + vetTestPrivateKeyFile.toString());
        assumeTrue(testVetPublicKeyPemB64 != null, "VET test public key could not be loaded for verification tests.");
    }

    private void assumeClinicKeysAvailable() {
        assumeTrue(Files.exists(clinicTestPrivateKeyFile), "Clinic private test key file missing: " + clinicTestPrivateKeyFile.toString());
        assumeTrue(testClinicPublicKeyPemB64 != null, "CLINIC test public key could not be loaded for verification tests.");
    }
}