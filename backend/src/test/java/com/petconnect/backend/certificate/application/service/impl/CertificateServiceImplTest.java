package com.petconnect.backend.certificate.application.service.impl;

import com.petconnect.backend.certificate.application.dto.CertificateGenerationRequestDto;
import com.petconnect.backend.certificate.application.dto.CertificateViewDto;
import com.petconnect.backend.certificate.application.mapper.CertificateMapper;
import com.petconnect.backend.certificate.domain.model.Certificate;
import com.petconnect.backend.certificate.domain.repository.CertificateRepository;
import com.petconnect.backend.exception.HashingException;
import com.petconnect.backend.common.helper.AuthorizationHelper;
import com.petconnect.backend.common.helper.CertificateHelper;
import com.petconnect.backend.common.helper.EntityFinderHelper;
import com.petconnect.backend.common.service.HashingService;
import com.petconnect.backend.common.service.SigningService;
import com.petconnect.backend.pet.domain.model.Pet;
import com.petconnect.backend.record.domain.model.Record;
import com.petconnect.backend.record.domain.model.RecordType;
import com.petconnect.backend.record.domain.model.Vaccine;
import com.petconnect.backend.user.domain.model.Clinic;
import com.petconnect.backend.user.domain.model.Owner;
import com.petconnect.backend.user.domain.model.Vet;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * Unit tests for {@link CertificateServiceImpl}.
 * Verifies the business logic for generating and retrieving certificates using Mockito.
 *
 * @author ibosquet
 */
@ExtendWith(MockitoExtension.class)
class CertificateServiceImplTest {

    // --- Mocks ---
    @Mock private CertificateRepository certificateRepository;
    @Mock private CertificateMapper certificateMapper;
    @Mock private EntityFinderHelper entityFinderHelper;
    @Mock private AuthorizationHelper authorizationHelper;
    @Mock private HashingService hashingService;
    @Mock private SigningService signingService;
    @Mock private CertificateHelper certificateHelper;

    // --- Class Under Test ---
    @InjectMocks
    private CertificateServiceImpl certificateService;

    // --- Captors ---
    @Captor ArgumentCaptor<Certificate> certificateCaptor;

    private Vet generatingVet;
    private Record sourceRecord;
    private Pet pet;
    private Clinic clinic;
    private Owner owner;
    private CertificateGenerationRequestDto generationRequestDto;
    private Certificate savedCertificate;
    private CertificateViewDto expectedViewDto;

    private final Long vetId = 1L;
    private final Long recordId = 10L;

    @BeforeEach
    void setUp() {
        Vaccine vaccine;
        Long petId = 100L;
        Long clinicId = 5L;
        Long ownerId = 50L;
        Long certificateId = 500L;

        clinic = Clinic.builder().name("Test Clinic Cert").build();
        clinic.setId(clinicId);

        owner = new Owner(); owner.setId(ownerId); owner.setUsername("certowner");

        generatingVet = new Vet(); generatingVet.setId(vetId); generatingVet.setClinic(clinic); generatingVet.setUsername("certvet");

        pet = new Pet(); pet.setId(petId); pet.setOwner(owner);

        vaccine = Vaccine.builder().name("Rabies Test").build();
        vaccine.setId(recordId);

        sourceRecord = new Record(); sourceRecord.setId(recordId); sourceRecord.setPet(pet); sourceRecord.setCreator(generatingVet);
        sourceRecord.setVetSignature("EXISTING_RECORD_SIGNATURE");
        sourceRecord.setType(RecordType.VACCINE);
        sourceRecord.setVaccine(vaccine); vaccine.setRecordEntity(sourceRecord);

        generationRequestDto = new CertificateGenerationRequestDto(recordId, "AHC-12345-XYZ");

        savedCertificate = Certificate.builder()
                .medicalRecord(sourceRecord).pet(pet).generatorVet(generatingVet).issuingClinic(clinic)
                .certificateNumber(generationRequestDto.certificateNumber())
                .payload("{\"json\":\"payload\"}")
                .hash("hashedPayload")
                .vetSignature("vetSignedHash")
                .clinicSignature("clinicSignedHash")
                .build();
        savedCertificate.setId(certificateId);
        savedCertificate.setCreatedAt(LocalDateTime.now());

        expectedViewDto = new CertificateViewDto(certificateId, "AHC-12345-XYZ", null, null, null, null, savedCertificate.getCreatedAt(), "{\"json\":\"payload\"}", "hashedPayload", "vetSignedHash", "clinicSignedHash");
    }

    /**
     * --- Tests for generateCertificate ---
     */
    @Nested
    @DisplayName("generateCertificate Tests")
    class GenerateCertificateTests {

        @Test
        @DisplayName("should generate certificate successfully when all prerequisites met")
        void generateCertificate_Success() throws HashingException {
            // Arrange
            String payloadHash = "a1b2c3d4e5f6...";
            String vetSig = "base64VetSig==";
            String clinicSig = "base64ClinicSig==";

            given(entityFinderHelper.findVetOrFail(vetId)).willReturn(generatingVet);
            given(entityFinderHelper.findRecordByIdOrFail(recordId)).willReturn(sourceRecord);
            willDoNothing().given(certificateHelper).validateCertificateGenerationPrerequisites(generatingVet, sourceRecord, generationRequestDto.certificateNumber());
            given(certificateHelper.buildPayload(pet, sourceRecord, generatingVet, clinic, generationRequestDto.certificateNumber()))
                    .willReturn(Map.of("key", "value"));

            given(hashingService.hashString(anyString())).willReturn(payloadHash);
            given(signingService.generateVetSignature(generatingVet, payloadHash)).willReturn(vetSig);
            given(signingService.generateClinicSignature(clinic, payloadHash)).willReturn(clinicSig);

            given(certificateRepository.save(any(Certificate.class))).willReturn(savedCertificate);
            given(certificateMapper.toViewDto(savedCertificate)).willReturn(expectedViewDto);


            // Act
            CertificateViewDto result = certificateService.generateCertificate(generationRequestDto, vetId);

            // Assert
            assertThat(result).isNotNull().isEqualTo(expectedViewDto);

            // Verify interactions
            then(entityFinderHelper).should().findVetOrFail(vetId);
            then(entityFinderHelper).should().findRecordByIdOrFail(recordId);
            then(certificateHelper).should().validateCertificateGenerationPrerequisites(generatingVet, sourceRecord, generationRequestDto.certificateNumber());
            then(certificateHelper).should().buildPayload(pet, sourceRecord, generatingVet, clinic, generationRequestDto.certificateNumber());
            then(hashingService).should().hashString(anyString());
            then(signingService).should().generateVetSignature(generatingVet, payloadHash);
            then(signingService).should().generateClinicSignature(clinic, payloadHash);
            then(certificateRepository).should().save(certificateCaptor.capture());
            then(certificateMapper).should().toViewDto(savedCertificate);

            // Verify saved entity details
            Certificate captured = certificateCaptor.getValue();
            assertThat(captured.getCertificateNumber()).isEqualTo(generationRequestDto.certificateNumber());
            assertThat(captured.getHash()).isEqualTo(payloadHash);
            assertThat(captured.getVetSignature()).isEqualTo(vetSig);
            assertThat(captured.getClinicSignature()).isEqualTo(clinicSig);
        }

        @Test
        @DisplayName("should throw IllegalStateException if source record not signed")
        void generateCertificate_Failure_RecordNotSigned() {
            // Arrange
            sourceRecord.setVetSignature(null);
            given(entityFinderHelper.findVetOrFail(vetId)).willReturn(generatingVet);
            given(entityFinderHelper.findRecordByIdOrFail(recordId)).willReturn(sourceRecord);
            willThrow(new IllegalStateException("Source record ... is not signed."))
                    .given(certificateHelper).validateCertificateGenerationPrerequisites(generatingVet, sourceRecord, generationRequestDto.certificateNumber());

            // Act & Assert
            assertThatThrownBy(() -> certificateService.generateCertificate(generationRequestDto, vetId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("is not signed");

            then(certificateRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("should throw AccessDeniedException if Vet did not sign the record")
        void generateCertificate_Failure_VetNotSigner() {
            Vet anotherVet = new Vet(); anotherVet.setId(999L); anotherVet.setClinic(clinic);
            sourceRecord.setCreator(anotherVet); // Record created/signed by another vet
            given(entityFinderHelper.findVetOrFail(generatingVet.getId())).willReturn(generatingVet);
            given(entityFinderHelper.findRecordByIdOrFail(generationRequestDto.recordId())).willReturn(sourceRecord);
            willThrow(new AccessDeniedException("Vet ... cannot generate certificate for record ... as they did not create/sign it."))
                    .given(certificateHelper).validateCertificateGenerationPrerequisites(generatingVet, sourceRecord, generationRequestDto.certificateNumber());

            // Act
            Throwable thrown = Assertions.catchThrowable(() -> certificateService.generateCertificate(generationRequestDto, generatingVet.getId()));
            // Assert
            assertThat(thrown)
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("did not create/sign it");

            then(certificateRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("should throw IllegalStateException if certificate already exists for record")
        void generateCertificate_Failure_CertExistsForRecord() {
            given(entityFinderHelper.findVetOrFail(generatingVet.getId())).willReturn(generatingVet);
            given(entityFinderHelper.findRecordByIdOrFail(generationRequestDto.recordId())).willReturn(sourceRecord);
            willThrow(new IllegalStateException("A certificate already exists for record..."))
                    .given(certificateHelper).validateCertificateGenerationPrerequisites(generatingVet, sourceRecord, generationRequestDto.certificateNumber());

            // Act
            Throwable thrown = Assertions.catchThrowable(() -> certificateService.generateCertificate(generationRequestDto, generatingVet.getId()));
            // Assert
            assertThat(thrown)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("certificate already exists for record");

            then(certificateRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("should throw IllegalArgumentException if certificate number already exists")
        void generateCertificate_Failure_CertNumberExists() {
            // Arrange
            given(entityFinderHelper.findVetOrFail(vetId)).willReturn(generatingVet);
            given(entityFinderHelper.findRecordByIdOrFail(recordId)).willReturn(sourceRecord);
            willThrow(new IllegalArgumentException("Certificate number '" + generationRequestDto.certificateNumber() + "' is already in use."))
                    .given(certificateHelper).validateCertificateGenerationPrerequisites(generatingVet, sourceRecord, generationRequestDto.certificateNumber());

            // Act & Assert
            assertThatThrownBy(() -> certificateService.generateCertificate(generationRequestDto, vetId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("is already in use");

            then(certificateRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("should throw IllegalStateException if record type is not VACCINE")
        void generateCertificate_Failure_WrongRecordType() {
            // Arrange
            sourceRecord.setType(RecordType.ILLNESS);
            sourceRecord.setVaccine(null);
            given(entityFinderHelper.findVetOrFail(vetId)).willReturn(generatingVet);
            given(entityFinderHelper.findRecordByIdOrFail(recordId)).willReturn(sourceRecord);
            willThrow(new IllegalStateException("Certificate generation is currently only supported for signed VACCINE records"))
                    .given(certificateHelper).validateCertificateGenerationPrerequisites(generatingVet, sourceRecord, generationRequestDto.certificateNumber());

            // Act & Assert
            assertThatThrownBy(() -> certificateService.generateCertificate(generationRequestDto, vetId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("only supported for signed VACCINE records");

            then(certificateRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("should throw RuntimeException if hashing fails")
        void generateCertificate_Failure_HashingError() {
            given(entityFinderHelper.findVetOrFail(vetId)).willReturn(generatingVet);
            given(entityFinderHelper.findRecordByIdOrFail(recordId)).willReturn(sourceRecord);
            willDoNothing().given(certificateHelper).validateCertificateGenerationPrerequisites(any(), any(), anyString());
            given(certificateHelper.buildPayload(any(), any(), any(), any(), anyString())).willReturn(Map.of("key", "value"));
            given(hashingService.hashString(anyString())).willThrow(new HashingException("SHA-256 Error", new RuntimeException()));


            // Act & Assert
            assertThatThrownBy(() -> certificateService.generateCertificate(generationRequestDto, vetId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to hash certificate payload");

            then(signingService).should(never()).generateVetSignature(any(), any());
            then(signingService).should(never()).generateClinicSignature(any(), any());
            then(certificateRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("should throw RuntimeException if Vet signing fails")
        void generateCertificate_Failure_VetSigningError() {
            String payloadHash = "hash123";
            given(entityFinderHelper.findVetOrFail(vetId)).willReturn(generatingVet);
            given(entityFinderHelper.findRecordByIdOrFail(recordId)).willReturn(sourceRecord);
            willDoNothing().given(certificateHelper).validateCertificateGenerationPrerequisites(any(), any(), anyString());
            given(certificateHelper.buildPayload(any(), any(), any(), any(), anyString())).willReturn(Map.of("key", "value"));
            given(hashingService.hashString(anyString())).willReturn(payloadHash);
            given(signingService.generateVetSignature(generatingVet, payloadHash)).willThrow(new RuntimeException("Vet signing failed"));


            // Act & Assert
            assertThatThrownBy(() -> certificateService.generateCertificate(generationRequestDto, vetId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to generate Vet digital signature");

            then(signingService).should(never()).generateClinicSignature(any(), any());
            then(certificateRepository).should(never()).save(any());
        }

    }

    /**
     * --- Tests for findCertificatesByPet ---
     */
    @Nested
    @DisplayName("findCertificatesByPet Tests")
    class FindCertificatesByPetTests {
        @Test
        @DisplayName("should return certificates when requested by authorized user")
        void findByPet_Success() {
            given(entityFinderHelper.findPetByIdOrFail(pet.getId())).willReturn(pet);
            willDoNothing().given(authorizationHelper).verifyUserAuthorizationForPet(owner.getId(), pet, "view certificates for");
            given(certificateRepository.findByPetIdOrderByCreatedAtDesc(pet.getId())).willReturn(List.of(savedCertificate));
            given(certificateMapper.toViewDtoList(List.of(savedCertificate))).willReturn(List.of(expectedViewDto));

            List<CertificateViewDto> result = certificateService.findCertificatesByPet(pet.getId(), owner.getId());

            assertThat(result).isNotNull().hasSize(1).containsExactly(expectedViewDto);
            then(authorizationHelper).should().verifyUserAuthorizationForPet(owner.getId(), pet, "view certificates for");
            then(certificateRepository).should().findByPetIdOrderByCreatedAtDesc(pet.getId());
        }
        @Test
        @DisplayName("should throw AccessDeniedException if requester not authorized")
        void findByPet_Failure_Unauthorized() {
            Long unauthorizedUserId = 999L;
            given(entityFinderHelper.findPetByIdOrFail(pet.getId())).willReturn(pet);
            willThrow(new AccessDeniedException("User not authorized"))
                    .given(authorizationHelper).verifyUserAuthorizationForPet(unauthorizedUserId, pet, "view certificates for");

            // Act
            Throwable thrown = Assertions.catchThrowable(() -> certificateService.findCertificatesByPet(pet.getId(), unauthorizedUserId));
            // Assert
            assertThat(thrown)
                    .isInstanceOf(AccessDeniedException.class);

            then(certificateRepository).should(never()).findByPetIdOrderByCreatedAtDesc(anyLong());
        }

        @Test
        @DisplayName("should throw EntityNotFoundException if pet not found")
        void findByPet_Failure_PetNotFound() {
            Long nonExistentPetId = 999L;
            given(entityFinderHelper.findPetByIdOrFail(nonExistentPetId))
                    .willThrow(new com.petconnect.backend.exception.EntityNotFoundException(Pet.class.getSimpleName(), nonExistentPetId));

            assertThatThrownBy(() -> certificateService.findCertificatesByPet(nonExistentPetId, owner.getId()))
                    .isInstanceOf(com.petconnect.backend.exception.EntityNotFoundException.class);

            then(authorizationHelper).should(never()).verifyUserAuthorizationForPet(anyLong(), any(), anyString());
            then(certificateRepository).should(never()).findByPetIdOrderByCreatedAtDesc(anyLong());
        }
    }

    /**
     * --- Tests for findCertificateById ---
     */
    @Nested
    @DisplayName("findCertificateById Tests")
    class FindCertificateByIdTests {
        @Test
        @DisplayName("should return certificate when requested by authorized user")
        void findById_Success() {
            given(entityFinderHelper.findCertificateOrFail(savedCertificate.getId())).willReturn(savedCertificate);
            willDoNothing().given(authorizationHelper).verifyUserAuthorizationForPet(owner.getId(), pet, "view certificate for");
            given(certificateMapper.toViewDto(savedCertificate)).willReturn(expectedViewDto);

            CertificateViewDto result = certificateService.findCertificateById(savedCertificate.getId(), owner.getId());

            assertThat(result).isNotNull().isEqualTo(expectedViewDto);
            then(authorizationHelper).should().verifyUserAuthorizationForPet(owner.getId(), pet, "view certificate for");
            then(certificateMapper).should().toViewDto(savedCertificate);
        }
        @Test
        @DisplayName("should throw EntityNotFoundException if certificate not found")
        void findById_Failure_CertNotFound() {
            Long certId = 999L;
            given(entityFinderHelper.findCertificateOrFail(certId))
                    .willThrow(new com.petconnect.backend.exception.EntityNotFoundException(Certificate.class.getSimpleName(), certId));

            assertThatThrownBy(() -> certificateService.findCertificateById(certId, owner.getId()))
                    .isInstanceOf(com.petconnect.backend.exception.EntityNotFoundException.class);

            then(authorizationHelper).should(never()).verifyUserAuthorizationForPet(anyLong(), any(), anyString());
        }

        @Test
        @DisplayName("should throw AccessDeniedException if requester not authorized for pet")
        void findById_Failure_Unauthorized() {
            Long unauthorizedUserId = 998L;
            given(entityFinderHelper.findCertificateOrFail(savedCertificate.getId())).willReturn(savedCertificate);
            willThrow(new AccessDeniedException("User not authorized for pet"))
                    .given(authorizationHelper).verifyUserAuthorizationForPet(unauthorizedUserId, pet, "view certificate for");

            // Act
            Throwable thrown = Assertions.catchThrowable(() ->  certificateService.findCertificateById(savedCertificate.getId(), unauthorizedUserId));
            // Assert
            assertThat(thrown)
                    .isInstanceOf(AccessDeniedException.class);

            then(certificateMapper).should(never()).toViewDto(any());
        }
    }
}