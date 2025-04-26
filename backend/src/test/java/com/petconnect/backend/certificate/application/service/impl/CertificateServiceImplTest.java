package com.petconnect.backend.certificate.application.service.impl;

import com.petconnect.backend.certificate.application.dto.CertificateGenerationRequestDto;
import com.petconnect.backend.certificate.application.dto.CertificateViewDto;
import com.petconnect.backend.certificate.application.mapper.CertificateMapper;
import com.petconnect.backend.certificate.domain.model.Certificate;
import com.petconnect.backend.certificate.domain.repository.CertificateRepository;
import com.petconnect.backend.common.service.QrCodeService;
import com.petconnect.backend.exception.*;
import com.petconnect.backend.common.helper.AuthorizationHelper;
import com.petconnect.backend.common.helper.CertificateHelper;
import com.petconnect.backend.common.helper.EntityFinderHelper;
import com.petconnect.backend.common.service.HashingService;
import com.petconnect.backend.common.service.SigningService;
import com.petconnect.backend.pet.domain.model.Pet;
import com.petconnect.backend.record.domain.model.Record;
import com.petconnect.backend.record.domain.model.RecordType;
import com.petconnect.backend.record.domain.model.Vaccine;
import com.petconnect.backend.record.domain.repository.RecordRepository;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
    @Mock private RecordRepository recordRepository;
    @Mock private CertificateMapper certificateMapper;
    @Mock private EntityFinderHelper entityFinderHelper;
    @Mock private AuthorizationHelper authorizationHelper;
    @Mock private HashingService hashingService;
    @Mock private SigningService signingService;
    @Mock private CertificateHelper certificateHelper;
    @Mock private QrCodeService qrCodeService;

    // --- Class Under Test ---
    @InjectMocks
    private CertificateServiceImpl certificateService;

    // --- Captors ---
    @Captor ArgumentCaptor<Certificate> certificateCaptor;

    // --- Test Data ---
    private Vet generatingVet;
    private Pet pet;
    private Clinic clinic;
    private Owner owner;
    private CertificateGenerationRequestDto generationRequestDto;
    private Certificate savedCertificate;
    private CertificateViewDto expectedViewDto;
    private Record validRabiesRecord;
    private Record validCheckupRecord;

    private final Long vetId = 1L;
    private final String certNumber = "AHC-12345-XYZ";

    @BeforeEach
    void setUp() {
        Long petId = 100L;
        Long clinicId = 5L;
        Long ownerId = 50L;
        Long certificateId = 500L;

        clinic = Clinic.builder().name("Test Clinic Cert").build(); clinic.setId(clinicId);
        owner = new Owner(); owner.setId(ownerId); owner.setUsername("certowner");
        generatingVet = new Vet(); generatingVet.setId(vetId); generatingVet.setClinic(clinic); generatingVet.setUsername("certvet");
        pet = new Pet(); pet.setId(petId); pet.setOwner(owner);

        Vaccine rabiesVaccine = Vaccine.builder().name("Rabivax").isRabiesVaccine(true).validity(1).build();
        Long rabiesRecordId = 10L;
        rabiesVaccine.setId(rabiesRecordId);
        validRabiesRecord = new Record(); validRabiesRecord.setId(rabiesRecordId); validRabiesRecord.setPet(pet); validRabiesRecord.setCreator(generatingVet);
        validRabiesRecord.setVetSignature("RABIES_SIG"); validRabiesRecord.setType(RecordType.VACCINE);
        validRabiesRecord.setCreatedAt(LocalDateTime.now().minusMonths(6));
        validRabiesRecord.setVaccine(rabiesVaccine); rabiesVaccine.setRecordEntity(validRabiesRecord);

        validCheckupRecord = new Record();
        Long checkupRecordId = 11L;
        validCheckupRecord.setId(checkupRecordId); validCheckupRecord.setPet(pet); validCheckupRecord.setCreator(generatingVet);
        validCheckupRecord.setVetSignature("CHECKUP_SIG"); validCheckupRecord.setType(RecordType.ANNUAL_CHECK);
        validCheckupRecord.setCreatedAt(LocalDateTime.now().minusMonths(2));

        generationRequestDto = new CertificateGenerationRequestDto(petId, certNumber);

        savedCertificate = Certificate.builder()
                .medicalRecord(validRabiesRecord)
                .pet(pet).generatorVet(generatingVet).issuingClinic(clinic)
                .certificateNumber(certNumber)
                .payload("{\"key\":\"value\"}")
                .hash("hashedPayload")
                .vetSignature("vetSignedHash")
                .clinicSignature("clinicSignedHash")
                .build();
        savedCertificate.setId(certificateId);
        savedCertificate.setCreatedAt(LocalDateTime.now());

        expectedViewDto = new CertificateViewDto(certificateId, certNumber, null, null, null, null, savedCertificate.getCreatedAt(), "{\"key\":\"value\"}", "hashedPayload", "vetSignedHash", "clinicSignedHash");
    }

    /**
     * --- Tests for generateCertificate ---
     */
    @Nested
    @DisplayName("generateCertificate Tests")
    class GenerateCertificateTests {
        Long petId = 100L;

        @Test
        @DisplayName("should generate certificate successfully when Rabies vac and Checkup are valid")
        void generateCertificate_Success_AllValid() throws HashingException {
            // --- Arrange --
            String payloadHash = "a1b2c3d4e5f6...";
            String vetSig = "base64VetSig==";
            String clinicSig = "base64ClinicSig==";
            Map<String, Object> mockPayloadMap = Map.of("key", "value");

            given(entityFinderHelper.findVetOrFail(vetId)).willReturn(generatingVet);
            given(entityFinderHelper.findPetByIdOrFail(petId)).willReturn(pet);

            given(recordRepository.findAllSignedRabiesVaccinesDesc(petId))
                    .willReturn(List.of(validRabiesRecord));
            given(recordRepository.findSignedCheckupsAfterDateDesc(eq(petId), eq(List.of(RecordType.ANNUAL_CHECK)), any(LocalDateTime.class)))
                    .willReturn(List.of(validCheckupRecord));

            given(certificateRepository.existsByMedicalRecordId(validRabiesRecord.getId())).willReturn(false);
            given(certificateRepository.findByCertificateNumber(certNumber)).willReturn(Optional.empty());
            given(certificateHelper.buildPayload(pet, validRabiesRecord, generatingVet, clinic, certNumber))
                    .willReturn(mockPayloadMap);
            given(hashingService.hashString(anyString())).willReturn(payloadHash);
            given(signingService.generateVetSignature(generatingVet, payloadHash)).willReturn(vetSig);
            given(signingService.generateClinicSignature(clinic, payloadHash)).willReturn(clinicSig);
            given(certificateRepository.save(any(Certificate.class))).willReturn(savedCertificate);
            given(certificateMapper.toViewDto(savedCertificate)).willReturn(expectedViewDto);

            // --- Act ---
            CertificateViewDto result = certificateService.generateCertificate(generationRequestDto, vetId);

            // --- Assert ---
            assertThat(result).isNotNull().isEqualTo(expectedViewDto);

            then(entityFinderHelper).should().findVetOrFail(vetId);
            then(entityFinderHelper).should().findPetByIdOrFail(petId);
            then(recordRepository).should().findAllSignedRabiesVaccinesDesc(eq(petId));
            then(recordRepository).should().findSignedCheckupsAfterDateDesc(eq(petId), eq(List.of(RecordType.ANNUAL_CHECK)), any(LocalDateTime.class));
            then(certificateRepository).should().existsByMedicalRecordId(validRabiesRecord.getId());
            then(certificateRepository).should().findByCertificateNumber(certNumber);
            then(certificateHelper).should().buildPayload(pet, validRabiesRecord, generatingVet, clinic, certNumber);
            then(certificateHelper).should().buildPayload(pet, validRabiesRecord, generatingVet, clinic, certNumber);
            then(hashingService).should().hashString(anyString());
            then(signingService).should().generateVetSignature(generatingVet, payloadHash);
            then(signingService).should().generateClinicSignature(clinic, payloadHash);
            then(certificateRepository).should().save(certificateCaptor.capture());
            then(certificateMapper).should().toViewDto(savedCertificate);

            Certificate captured = certificateCaptor.getValue();
            assertThat(captured.getMedicalRecord()).isEqualTo(validRabiesRecord);
            assertThat(captured.getCertificateNumber()).isEqualTo(certNumber);
            assertThat(captured.getHash()).isEqualTo(payloadHash);
            assertThat(captured.getVetSignature()).isEqualTo(vetSig);
            assertThat(captured.getClinicSignature()).isEqualTo(clinicSig);
        }

        @Test
        @DisplayName("should throw MissingRabiesVaccineException if no valid rabies record found")
        void generateCertificate_Failure_NoValidRabies() {
            given(entityFinderHelper.findVetOrFail(vetId)).willReturn(generatingVet);
            given(entityFinderHelper.findPetByIdOrFail(petId)).willReturn(pet);

            given(recordRepository.findAllSignedRabiesVaccinesDesc(petId))
                    .willReturn(Collections.emptyList());

            assertThatThrownBy(() -> certificateService.generateCertificate(generationRequestDto, vetId))
                    .isInstanceOf(MissingRabiesVaccineException.class)
                    .hasMessageContaining("No valid, signed, and current Rabies vaccine record found");

            then(recordRepository).should().findAllSignedRabiesVaccinesDesc(eq(petId));
            then(recordRepository).should(never()).findSignedCheckupsAfterDateDesc(anyLong(), anyList(), any());
            then(certificateRepository).should(never()).save(any());
            then(certificateHelper).should(never()).buildPayload(any(), any(), any(), any(), anyString());
        }

        @Test
        @DisplayName("should throw MissingRecentCheckupException if no recent checkup found")
        void generateCertificate_Failure_NoRecentCheckup() {
            // Arrange
            given(entityFinderHelper.findVetOrFail(vetId)).willReturn(generatingVet);
            given(entityFinderHelper.findPetByIdOrFail(petId)).willReturn(pet);
            given(recordRepository.findAllSignedRabiesVaccinesDesc(petId))
                    .willReturn(List.of(validRabiesRecord));

            given(recordRepository.findSignedCheckupsAfterDateDesc(eq(petId), eq(List.of(RecordType.ANNUAL_CHECK)), any(LocalDateTime.class)))
                    .willReturn(Collections.emptyList());

            // Act & Assert
            assertThatThrownBy(() -> certificateService.generateCertificate(generationRequestDto, vetId))
                    .isInstanceOf(MissingRecentCheckupException.class)
                    .hasMessageContaining("No signed ANNUAL_CHECK found since");

            then(recordRepository).should().findAllSignedRabiesVaccinesDesc(eq(petId));
            then(recordRepository).should().findSignedCheckupsAfterDateDesc(eq(petId), eq(List.of(RecordType.ANNUAL_CHECK)), any(LocalDateTime.class));
            then(certificateRepository).should(never()).existsByMedicalRecordId(anyLong());
            then(certificateRepository).should(never()).save(any());
            then(certificateHelper).should(never()).buildPayload(any(), any(), any(), any(), anyString());
        }

        @Test
        @DisplayName("should throw CertificateAlreadyExistsForRecordException if certificate already exists for the found rabies record")
        void generateCertificate_Failure_CertExistsForFoundRabiesRecord() {
            // Arrange
            given(entityFinderHelper.findVetOrFail(vetId)).willReturn(generatingVet);
            given(entityFinderHelper.findPetByIdOrFail(petId)).willReturn(pet);
            given(recordRepository.findAllSignedRabiesVaccinesDesc(petId))
                    .willReturn(List.of(validRabiesRecord));
            given(recordRepository.findSignedCheckupsAfterDateDesc(eq(petId), eq(List.of(RecordType.ANNUAL_CHECK)), any(LocalDateTime.class)))
                    .willReturn(List.of(validCheckupRecord));

            given(certificateRepository.existsByMedicalRecordId(validRabiesRecord.getId())).willReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> certificateService.generateCertificate(generationRequestDto, vetId))
                    .isInstanceOf(CertificateAlreadyExistsForRecordException.class)
                    .hasMessageContaining("A certificate already exists for record " + validRabiesRecord.getId());

            then(recordRepository).should().findAllSignedRabiesVaccinesDesc(eq(petId));
            then(recordRepository).should().findSignedCheckupsAfterDateDesc(eq(petId), eq(List.of(RecordType.ANNUAL_CHECK)), any(LocalDateTime.class));
            then(certificateRepository).should().existsByMedicalRecordId(validRabiesRecord.getId());
            then(certificateRepository).should(never()).findByCertificateNumber(anyString());
            then(certificateRepository).should(never()).save(any());
            then(certificateHelper).should(never()).buildPayload(any(), any(), any(), any(), anyString());
        }


        @Test
        @DisplayName("should throw CertificateNumberAlreadyExistsException if certificate number conflicts")
        void generateCertificate_Failure_CertNumberExists() {
            // Arrange
            given(entityFinderHelper.findVetOrFail(vetId)).willReturn(generatingVet);
            given(entityFinderHelper.findPetByIdOrFail(petId)).willReturn(pet);
            given(recordRepository.findAllSignedRabiesVaccinesDesc(petId))
                    .willReturn(List.of(validRabiesRecord));
            given(recordRepository.findSignedCheckupsAfterDateDesc(eq(petId), eq(List.of(RecordType.ANNUAL_CHECK)), any(LocalDateTime.class)))
                    .willReturn(List.of(validCheckupRecord));

            given(certificateRepository.existsByMedicalRecordId(validRabiesRecord.getId())).willReturn(false);
            given(certificateRepository.findByCertificateNumber(certNumber)).willReturn(Optional.of(new Certificate()));

            // Act & Assert
            assertThatThrownBy(() -> certificateService.generateCertificate(generationRequestDto, vetId))
                    .isInstanceOf(CertificateNumberAlreadyExistsException.class)
                    .hasMessageContaining("'" + certNumber + "' is already in use");

            then(recordRepository).should().findAllSignedRabiesVaccinesDesc(eq(petId));
            then(recordRepository).should().findSignedCheckupsAfterDateDesc(eq(petId), eq(List.of(RecordType.ANNUAL_CHECK)), any(LocalDateTime.class));
            then(certificateRepository).should().existsByMedicalRecordId(validRabiesRecord.getId());
            then(certificateRepository).should().findByCertificateNumber(certNumber);
            then(certificateRepository).should(never()).save(any());
            then(certificateHelper).should(never()).buildPayload(any(), any(), any(), any(), anyString());
        }

        @Test
        @DisplayName("should throw RuntimeException if hashing fails")
        void generateCertificate_Failure_HashingError() {
            // Arrange
            given(entityFinderHelper.findVetOrFail(vetId)).willReturn(generatingVet);
            given(entityFinderHelper.findPetByIdOrFail(petId)).willReturn(pet);

            given(recordRepository.findAllSignedRabiesVaccinesDesc(petId))
                    .willReturn(List.of(validRabiesRecord));
            given(recordRepository.findSignedCheckupsAfterDateDesc(eq(petId), eq(List.of(RecordType.ANNUAL_CHECK)), any(LocalDateTime.class)))
                    .willReturn(List.of(validCheckupRecord));

            given(certificateRepository.existsByMedicalRecordId(validRabiesRecord.getId())).willReturn(false);
            given(certificateRepository.findByCertificateNumber(certNumber)).willReturn(Optional.empty());
            given(certificateHelper.buildPayload(any(), any(), any(), any(), anyString())).willReturn(Map.of("key", "value"));
            given(hashingService.hashString(anyString())).willThrow(new HashingException("Hash fail", null));

            // Act & Assert
            assertThatThrownBy(() -> certificateService.generateCertificate(generationRequestDto, vetId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to hash certificate payload")
                    .hasCauseInstanceOf(HashingException.class)
                    .hasRootCauseMessage("Hash fail");

            then(recordRepository).should().findAllSignedRabiesVaccinesDesc(eq(petId));
            then(recordRepository).should().findSignedCheckupsAfterDateDesc(eq(petId), eq(List.of(RecordType.ANNUAL_CHECK)), any(LocalDateTime.class));
            then(certificateRepository).should().existsByMedicalRecordId(validRabiesRecord.getId());
            then(certificateRepository).should().findByCertificateNumber(certNumber);
            then(certificateHelper).should().buildPayload(any(), any(), any(), any(), anyString());
            then(hashingService).should().hashString(anyString());
            then(signingService).should(never()).generateVetSignature(any(), any());
            then(recordRepository).should().findAllSignedRabiesVaccinesDesc(eq(petId));
            then(recordRepository).should().findSignedCheckupsAfterDateDesc(eq(petId), eq(List.of(RecordType.ANNUAL_CHECK)), any(LocalDateTime.class));
            then(certificateRepository).should().existsByMedicalRecordId(validRabiesRecord.getId());
            then(certificateRepository).should().findByCertificateNumber(certNumber);
            then(certificateHelper).should().buildPayload(any(), any(), any(), any(), anyString());
            then(hashingService).should().hashString(anyString());
            then(certificateRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("should throw RuntimeException if Vet signing fails")
        void generateCertificate_Failure_VetSigningError() {
            // Arrange
            String payloadHash = "hash123";
            given(entityFinderHelper.findVetOrFail(vetId)).willReturn(generatingVet);
            given(entityFinderHelper.findPetByIdOrFail(petId)).willReturn(pet);
            given(recordRepository.findAllSignedRabiesVaccinesDesc(petId))
                    .willReturn(List.of(validRabiesRecord));
            given(recordRepository.findSignedCheckupsAfterDateDesc(eq(petId), eq(List.of(RecordType.ANNUAL_CHECK)), any(LocalDateTime.class)))
                    .willReturn(List.of(validCheckupRecord));

            given(certificateRepository.existsByMedicalRecordId(validRabiesRecord.getId())).willReturn(false);
            given(certificateRepository.findByCertificateNumber(certNumber)).willReturn(Optional.empty());
            given(certificateHelper.buildPayload(any(), any(), any(), any(), anyString())).willReturn(Map.of("key", "value"));
            given(hashingService.hashString(anyString())).willReturn(payloadHash);
            given(signingService.generateVetSignature(generatingVet, payloadHash)).willThrow(new RuntimeException("Vet key error"));

            // --- Act & Assert
            assertThatThrownBy(() -> certificateService.generateCertificate(generationRequestDto, vetId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to generate Vet digital signature")
                    .hasCauseInstanceOf(RuntimeException.class)
                    .hasRootCauseMessage("Vet key error");

            then(recordRepository).should().findAllSignedRabiesVaccinesDesc(eq(petId));
            then(recordRepository).should().findSignedCheckupsAfterDateDesc(eq(petId), eq(List.of(RecordType.ANNUAL_CHECK)), any(LocalDateTime.class));
            then(certificateRepository).should().existsByMedicalRecordId(validRabiesRecord.getId());
            then(certificateRepository).should().findByCertificateNumber(certNumber);
            then(certificateHelper).should().buildPayload(any(), any(), any(), any(), anyString());
            then(hashingService).should().hashString(anyString());
            then(signingService).should().generateVetSignature(generatingVet, payloadHash);
            then(signingService).should(never()).generateClinicSignature(any(), any());
            then(certificateRepository).should(never()).save(any());
        }
        @Test
        @DisplayName("should throw RuntimeException if Clinic signing fails")
        void generateCertificate_Failure_ClinicSigningError() {
            // Arrange
            String payloadHash = "hash123";
            String vetSig = "vetSigOk";
            given(entityFinderHelper.findVetOrFail(vetId)).willReturn(generatingVet);
            given(entityFinderHelper.findPetByIdOrFail(petId)).willReturn(pet);
            given(recordRepository.findAllSignedRabiesVaccinesDesc(petId))
                    .willReturn(List.of(validRabiesRecord));
            given(recordRepository.findSignedCheckupsAfterDateDesc(eq(petId), eq(List.of(RecordType.ANNUAL_CHECK)), any(LocalDateTime.class)))
                    .willReturn(List.of(validCheckupRecord));

            given(certificateRepository.existsByMedicalRecordId(validRabiesRecord.getId())).willReturn(false);
            given(certificateRepository.findByCertificateNumber(certNumber)).willReturn(Optional.empty());
            given(certificateHelper.buildPayload(any(), any(), any(), any(), anyString())).willReturn(Map.of("key", "value"));
            given(hashingService.hashString(anyString())).willReturn(payloadHash);
            given(signingService.generateVetSignature(generatingVet, payloadHash)).willReturn(vetSig);
            given(signingService.generateClinicSignature(clinic, payloadHash)).willThrow(new RuntimeException("Clinic key error"));

            // --- Act & Assert
            assertThatThrownBy(() -> certificateService.generateCertificate(generationRequestDto, vetId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to generate Clinic digital signature")
                    .hasCauseInstanceOf(RuntimeException.class)
                    .hasRootCauseMessage("Clinic key error");
            then(recordRepository).should().findAllSignedRabiesVaccinesDesc(eq(petId));
            then(recordRepository).should().findSignedCheckupsAfterDateDesc(eq(petId), eq(List.of(RecordType.ANNUAL_CHECK)), any(LocalDateTime.class));
            then(certificateRepository).should().existsByMedicalRecordId(validRabiesRecord.getId());
            then(certificateRepository).should().findByCertificateNumber(certNumber);
            then(certificateHelper).should().buildPayload(any(), any(), any(), any(), anyString());
            then(hashingService).should().hashString(anyString());
            then(signingService).should().generateVetSignature(generatingVet, payloadHash);
            then(signingService).should().generateClinicSignature(clinic, payloadHash);
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

    /**
     * --- Tests for getQrDataForCertificate ---
     */
    @Nested
    @DisplayName("getQrDataForCertificate Tests")
    class GetQrDataForCertificateTests {

        private final Long certificateId = 500L;
        private final Long requesterOwnerId = 50L;
        private final Long unauthorizedUserId = 999L;

        @BeforeEach
        void qrDataSetup() {
            savedCertificate.setId(certificateId);
        }

        @Test
        @DisplayName("should return Base45 string when requested by authorized Owner")
        void getQrData_Success_ByOwner() {
            // Arrange
            String expectedQrData = "HC1:BASE45DATA...";
            given(entityFinderHelper.findCertificateOrFail(certificateId)).willReturn(savedCertificate);
            Pet associatedPet = savedCertificate.getPet();
            willDoNothing().given(authorizationHelper).verifyUserAuthorizationForPet(requesterOwnerId, associatedPet, "get QR data for certificate");
            given(qrCodeService.generateQrData(savedCertificate)).willReturn(expectedQrData);

            // Act
            String actualQrData = certificateService.getQrDataForCertificate(certificateId, requesterOwnerId);

            // Assert
            assertThat(actualQrData).isEqualTo(expectedQrData);
            then(entityFinderHelper).should().findCertificateOrFail(certificateId);
            then(authorizationHelper).should().verifyUserAuthorizationForPet(requesterOwnerId, associatedPet, "get QR data for certificate");
            then(qrCodeService).should().generateQrData(savedCertificate);
        }

        @Test
        @DisplayName("should return Base45 string when requested by authorized Staff")
        void getQrData_Success_ByStaff() {
            Long requesterVetId = 1L;
            // Arrange
            String expectedQrData = "HC1:STAFFBASE45...";
            given(entityFinderHelper.findCertificateOrFail(certificateId)).willReturn(savedCertificate);
            Pet associatedPet = savedCertificate.getPet();
            assertThat(associatedPet).isNotNull();
            willDoNothing().given(authorizationHelper).verifyUserAuthorizationForPet(requesterVetId, associatedPet, "get QR data for certificate");
            given(qrCodeService.generateQrData(savedCertificate)).willReturn(expectedQrData);

            // Act
            String actualQrData = certificateService.getQrDataForCertificate(certificateId, requesterVetId);

            // Assert
            assertThat(actualQrData).isEqualTo(expectedQrData);
            then(entityFinderHelper).should().findCertificateOrFail(certificateId);
            then(authorizationHelper).should().verifyUserAuthorizationForPet(requesterVetId, associatedPet, "get QR data for certificate");
            then(qrCodeService).should().generateQrData(savedCertificate);
        }

        @Test
        @DisplayName("should throw EntityNotFoundException if certificate not found")
        void getQrData_Failure_CertNotFound() {
            Long nonExistentCertId = 888L;
            given(entityFinderHelper.findCertificateOrFail(nonExistentCertId))
                    .willThrow(new com.petconnect.backend.exception.EntityNotFoundException(Certificate.class.getSimpleName(), nonExistentCertId));

            assertThatThrownBy(() -> certificateService.getQrDataForCertificate(nonExistentCertId, requesterOwnerId))
                    .isInstanceOf(com.petconnect.backend.exception.EntityNotFoundException.class);

            then(authorizationHelper).should(never()).verifyUserAuthorizationForPet(anyLong(), any(), anyString());
            then(qrCodeService).should(never()).generateQrData(any());
        }

        @Test
        @DisplayName("should throw AccessDeniedException if requester not authorized for pet")
        void getQrData_Failure_Unauthorized() {
            given(entityFinderHelper.findCertificateOrFail(certificateId)).willReturn(savedCertificate);
            Pet associatedPet = savedCertificate.getPet();
            willThrow(new AccessDeniedException("User not authorized for pet"))
                    .given(authorizationHelper).verifyUserAuthorizationForPet(unauthorizedUserId, associatedPet, "get QR data for certificate");

            assertThatThrownBy(() -> certificateService.getQrDataForCertificate(certificateId, unauthorizedUserId))
                    .isInstanceOf(AccessDeniedException.class);

            then(qrCodeService).should(never()).generateQrData(any());
        }

        @Test
        @DisplayName("should throw RuntimeException if QrCodeService fails")
        void getQrData_Failure_QrServiceError() {
            given(entityFinderHelper.findCertificateOrFail(certificateId)).willReturn(savedCertificate);
            Pet associatedPet = savedCertificate.getPet();
            willDoNothing().given(authorizationHelper).verifyUserAuthorizationForPet(requesterOwnerId, associatedPet, "get QR data for certificate");

            given(qrCodeService.generateQrData(savedCertificate)).willThrow(new RuntimeException("CBOR failed"));

            assertThatThrownBy(() -> certificateService.getQrDataForCertificate(certificateId, requesterOwnerId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("CBOR failed");
        }
    }
}