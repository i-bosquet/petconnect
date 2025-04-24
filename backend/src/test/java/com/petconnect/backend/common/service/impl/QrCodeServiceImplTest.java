package com.petconnect.backend.common.service.impl;

import COSE.AlgorithmID;
import COSE.HeaderKeys;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.petconnect.backend.certificate.domain.model.Certificate;
import com.petconnect.backend.pet.domain.model.Pet;
import com.petconnect.backend.record.domain.model.Record;
import com.petconnect.backend.user.domain.model.Clinic;
import com.petconnect.backend.user.domain.model.Vet;
import com.upokecenter.cbor.CBORObject;
import com.upokecenter.cbor.CBORType;
import nl.minvws.encoding.Base45;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Base64;


/**
 * Unit tests for {@link QrCodeServiceImpl}.
 * Focuses on verifying the correct assembly of the data structure
 * through CBOR, COSE (manual structure), ZLib, and Base45 steps.
 * Does NOT verify the cryptographic validity of COSE signatures.
 *
 * @author ibosquet
 */
@ExtendWith(MockitoExtension.class)
class QrCodeServiceImplTest {

    @InjectMocks
    private QrCodeServiceImpl qrCodeService;

    private Certificate testCertificate;
    private String samplePayloadJson;
    private byte[] sampleVetSigBytes;
    private byte[] sampleClinicSigBytes;

    @BeforeEach
    void setUp() {

        samplePayloadJson = """
                {
                  "certType" : "PET_VACCINATION_CERT_V1",
                  "issuanceTimestamp" : 1678886400000,
                  "certificateNumber" : "TEST-CERT-001",
                  "issuer" : {
                    "id" : 5,
                    "name" : "Test Clinic QR",
                    "country" : "SPAIN"
                  },
                  "subject" : {
                    "petId" : 101,
                    "petName" : "QRBuddy"
                  },
                  "event" : {
                    "recordId" : 201,
                    "recordType": "VACCINE"
                   }
                }
                """;
        String sampleVetSigB64 = "dmV0U2lnVmFsaWQ=";
        String sampleClinicSigB64 = "Y2xpbmljU2lnVmFsaWQ=";
        sampleVetSigBytes = Base64.getDecoder().decode(sampleVetSigB64);
        sampleClinicSigBytes = Base64.getDecoder().decode(sampleClinicSigB64);

        Vet simulatedVet = Vet.builder().build();
        simulatedVet.setId(10L);
        simulatedVet.setUsername("qrvet");

        Clinic simulatedClinic = Clinic.builder().name("Test Clinic QR").build();
        simulatedClinic.setId(5L);

        testCertificate = Certificate.builder()
                .payload(samplePayloadJson)
                .vetSignature(sampleVetSigB64)
                .clinicSignature(sampleClinicSigB64)
                .generatorVet(simulatedVet)
                .issuingClinic(simulatedClinic)
                .certificateNumber("TEST-CERT-001")
                               .pet(new Pet())
                .medicalRecord(new Record())
                .build();
        testCertificate.setId(1L);
    }

    /**
     * --- Tests for the main generateQrData method ---
     */
    @Nested
    @DisplayName("generateQrData Main Flow Tests")
    class GenerateQrDataFlowTests {
        @Test
        @DisplayName("generateQrData should produce valid Base45 string")
        void generateQrData_Success_ValidBase45()  {
            // Act
            String finalQrData = qrCodeService.generateQrData(testCertificate);

            // Assert
            String base45Payload = finalQrData.substring(4);
            assertThatCode(() -> Base45.getDecoder().decode(base45Payload))
                    .doesNotThrowAnyException();

            System.out.println("Generated QR Data (Full): " + finalQrData);
        }

        @Test
        @DisplayName("generateQrData should handle JSON processing error during CBOR conversion")
        void generateQrData_Failure_JsonError()  {
            // Arrange
            String invalidJsonPayload = "{\"key\": \"value\",";
            testCertificate.setPayload(invalidJsonPayload);
            // Act & Assert
            assertThatThrownBy(() -> qrCodeService.generateQrData(testCertificate))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("QR data generation failed for certificate " + testCertificate.getId())
                    .hasRootCauseInstanceOf(JsonProcessingException.class);
        }

        @Test
        @DisplayName("generateQrData should handle Base64 decoding error for signatures")
        void generateQrData_Failure_Base64Error() {
            // Arrange
            testCertificate.setVetSignature("Invalid Base64!");

            // Act & Assert
            assertThatThrownBy(() -> qrCodeService.generateQrData(testCertificate))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("QR data generation failed");
        }

    }

    /**
     * --- Tests for convertJsonToCbor ---
     */
    @Nested
    @DisplayName("convertJsonToCbor Tests")
    class ConvertJsonToCborTests {

        @Test
        @DisplayName("convertJsonToCbor should convert valid JSON to CBOR bytes")
        void convertJsonToCbor_Success()  {
            // Act
            byte[] cborResult = ReflectionTestUtils.invokeMethod(qrCodeService, "convertJsonToCbor", samplePayloadJson);

            // Assert
            assertThat(cborResult).isNotNull().isNotEmpty();

            Assertions.assertNotNull(cborResult);
            CBORObject decodedCbor = CBORObject.DecodeFromBytes(cborResult);
            assertThat(decodedCbor.getType()).isEqualTo(CBORType.Map);
            assertThat(decodedCbor.get("certType").AsString()).isEqualTo("PET_VACCINATION_CERT_V1");
            assertThat(decodedCbor.get("subject").get("petName").AsString()).isEqualTo("QRBuddy");
        }
    }

    /**
     * --- Tests for createCoseStructure ---
     */
    @Nested
    @DisplayName("createCoseStructure Tests")
    class CreateCoseStructureTests {

        @Test
        @DisplayName("createCoseStructure should build CBOR array structure")
        void createCoseStructure_Success_StructureCheck() {
            // Arrange
            byte[] sampleCborPayload = ReflectionTestUtils.invokeMethod(qrCodeService, "convertJsonToCbor", samplePayloadJson);

            // Act
            byte[] coseBytes = ReflectionTestUtils.invokeMethod(qrCodeService, "createCoseStructure", testCertificate, sampleCborPayload);

            // Assert
            assertThat(coseBytes).isNotNull().isNotEmpty();

            Assertions.assertNotNull(coseBytes);
            CBORObject coseStructure = CBORObject.DecodeFromBytes(coseBytes);
            assertThat(coseStructure.getType()).isEqualTo(CBORType.Array);
            assertThat(coseStructure.size()).isEqualTo(4); // protected, unprotected, payload, signatures

            assertThat(coseStructure.get(2).GetByteString()).isEqualTo(sampleCborPayload);

            CBORObject signaturesArray = coseStructure.get(3);
            assertThat(signaturesArray.getType()).isEqualTo(CBORType.Array);
            assertThat(signaturesArray.size()).isEqualTo(2);

            CBORObject vetSigStruct = signaturesArray.get(0);
            assertThat(vetSigStruct.getType()).isEqualTo(CBORType.Array);
            assertThat(vetSigStruct.size()).isEqualTo(3); // protected', unprotected', signature
            assertThat(vetSigStruct.get(2).GetByteString()).isEqualTo(sampleVetSigBytes);
            byte[] protectedVetBytes = vetSigStruct.get(0).GetByteString();
            CBORObject protectedVetMap = CBORObject.DecodeFromBytes(protectedVetBytes);
            assertThat(protectedVetMap.getType()).isEqualTo(CBORType.Map);

            CBORObject clinicSigStruct = signaturesArray.get(1);
            assertThat(clinicSigStruct.get(2).GetByteString()).isEqualTo(sampleClinicSigBytes);
            byte[] protectedClinicBytes = clinicSigStruct.get(0).GetByteString();
            CBORObject.DecodeFromBytes(protectedClinicBytes);
            assertThat(protectedVetMap.getType()).isEqualTo(CBORType.Map);
        }
    }

    /**
     * --- Tests for compressWithZlib ---
     */
    @Nested
    @DisplayName("compressWithZlib Tests")
    class CompressWithZlibTests {

        @Test
        @DisplayName("should compress data correctly")
        void compress_Success(){

            // Arrange
            String original = "This is the string to be compressed. ".repeat(5);
            byte[] originalBytes = original.getBytes(StandardCharsets.UTF_8);

            // Act
            byte[] compressedBytes = ReflectionTestUtils.invokeMethod(qrCodeService, "compressWithZlib", (Object) originalBytes);

            // Assert
            assertThat(compressedBytes).isNotNull().isNotEmpty().hasSizeLessThan(originalBytes.length);

        }

        @Test
        @DisplayName("should handle empty input data")
        void compress_EmptyInput()  {
            // Arrange
            byte[] originalBytes = new byte[0];

            // Act
            byte[] compressedBytes = ReflectionTestUtils.invokeMethod(qrCodeService, "compressWithZlib", (Object) originalBytes);

            // Assert
            assertThat(compressedBytes).isNotNull();
        }

        @Test
        @DisplayName("should handle null input data gracefully (or throw)")
        void compress_NullInput() {
            assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(qrCodeService, "compressWithZlib", (Object) null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Data to be compressed cannot be null");
        }
    }

    /**
     * --- Tests for encodeToBase45 ---
     */
    @Nested
    @DisplayName("encodeToBase45 Tests")
    class EncodeToBase45Tests {

        @Test
        @DisplayName("should encode data correctly")
        void encode_Success()  {
            // Arrange
            byte[] originalBytes = "Hello Base45!".getBytes(StandardCharsets.UTF_8);
            String expectedBase45 = "%69 VD82ESH8LQE0R6X0";

            // Act
            String encodedString = ReflectionTestUtils.invokeMethod(qrCodeService, "encodeToBase45", (Object) originalBytes);

            // Assert
            assertThat(encodedString).isNotNull().isEqualTo(expectedBase45);
        }

        @Test
        @DisplayName("should handle empty input data")
        void encode_EmptyInput() {
            // Arrange
            byte[] originalBytes = new byte[0];

            // Act
            String encodedString = ReflectionTestUtils.invokeMethod(qrCodeService, "encodeToBase45", (Object) originalBytes);

            // Assert
            assertThat(encodedString).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("should throw IllegalArgumentException for null input")
        void encode_NullInput() {
            // Act & Assert
            assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(qrCodeService, "encodeToBase45", (Object) null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Data to be encoded cannot be null");
        }
    }
}
