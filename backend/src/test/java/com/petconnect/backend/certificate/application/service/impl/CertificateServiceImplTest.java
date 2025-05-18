package com.petconnect.backend.certificate.application.service.impl;

import com.petconnect.backend.certificate.application.dto.CertificateGenerationRequestDto;
import com.petconnect.backend.certificate.application.dto.CertificateViewDto;
import com.petconnect.backend.certificate.application.event.CertificateGeneratedEvent;
import com.petconnect.backend.certificate.application.mapper.CertificateMapper;
import com.petconnect.backend.certificate.domain.model.Certificate;
import com.petconnect.backend.certificate.domain.repository.CertificateRepository;
import com.petconnect.backend.certificate.port.spi.CertificateEventPublisherPort;
import com.petconnect.backend.common.helper.*;
import com.petconnect.backend.common.service.QrCodeService;
import com.petconnect.backend.exception.*;
import com.petconnect.backend.common.service.HashingService;
import com.petconnect.backend.common.service.SigningService;
import com.petconnect.backend.pet.application.dto.PetProfileDto;
import com.petconnect.backend.pet.application.mapper.PetMapper;
import com.petconnect.backend.pet.domain.model.Breed;
import com.petconnect.backend.pet.domain.model.Pet;
import com.petconnect.backend.pet.domain.model.PetStatus;
import com.petconnect.backend.pet.domain.model.Specie;
import com.petconnect.backend.record.application.dto.RecordViewDto;
import com.petconnect.backend.record.application.mapper.RecordMapper;
import com.petconnect.backend.record.domain.model.Record;
import com.petconnect.backend.record.domain.model.RecordType;
import com.petconnect.backend.record.domain.model.Vaccine;
import com.petconnect.backend.record.domain.repository.RecordRepository;
import com.petconnect.backend.user.application.dto.VetSummaryDto;
import com.petconnect.backend.user.application.mapper.UserMapper;
import com.petconnect.backend.user.domain.model.Clinic;
import com.petconnect.backend.user.domain.model.Country;
import com.petconnect.backend.user.domain.model.Owner;
import com.petconnect.backend.user.domain.model.Vet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.AdditionalMatchers.aryEq;
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
    @Mock private RecordHelper recordHelper;
    @Mock private SigningService signingService;
    @Mock private ValidateHelper validateHelper;
    @Mock private CertificateHelper certificateHelper;
    @Mock private QrCodeService qrCodeService;
    @Mock private CertificateEventPublisherPort certificateEventPublisher;
    @Mock private UserMapper userMapper;
    @Mock private PetMapper petMapper;
    @Mock private RecordMapper recordMapper;

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

    private final Long vetId = 1L;
    private final String certNumber = "AHC-12345-XYZ";
    private final Long petId = 100L;

    @BeforeEach
    void setUp() {
        Long clinicId = 5L;
        Long certificateId = 500L;
        Long rabiesRecordId = 10L;


        clinic = Clinic.builder().name("Test Clinic Cert").address("123 Cert Lane").
                city("Certville").country(Country.UNITED_KINGDOM).phone("555-0001").build();
        clinic.setId(clinicId);

        owner = new Owner();
        Long ownerId = 50L;
        owner.setId(ownerId);
        owner.setUsername("certowner");
        owner.setEmail("certowner@mail.com");
        owner.setPhone("123123123");

        generatingVet = new Vet();
        generatingVet.setId(vetId);
        generatingVet.setClinic(clinic);
        generatingVet.setUsername("certvet");
        generatingVet.setName("Certifying");
        generatingVet.setSurname("Vet");
        generatingVet.setEmail("certvet@clinic.com");
        generatingVet.setLicenseNumber("VETCERT987");
        generatingVet.setAvatar("path/to/vet_avatar.png");


        Breed petBreed = Breed.builder().id(1L).name("Labrador").specie(Specie.DOG).build();
        pet = new Pet();
        pet.setId(petId);
        pet.setOwner(owner);
        pet.setName("CertiPet");
        pet.setBreed(petBreed);
        pet.setImage("certipet.png");
        pet.setStatus(PetStatus.ACTIVE);
        pet.setAssociatedVets(Set.of(generatingVet));

        Vaccine rabiesVaccine = Vaccine.builder().name("Rabivax").isRabiesVaccine(true).validity(1).batchNumber("RB001").build();

        validRabiesRecord = new Record();
        validRabiesRecord.setId(rabiesRecordId);
        validRabiesRecord.setPet(pet);
        validRabiesRecord.setCreator(generatingVet);
        validRabiesRecord.setVetSignature("RABIES_SIG_BY_CERT_VET");
        validRabiesRecord.setType(RecordType.VACCINE);
        validRabiesRecord.setCreatedAt(LocalDateTime.now().minusMonths(6));
        validRabiesRecord.setVaccineDetails(rabiesVaccine);

        generationRequestDto = new CertificateGenerationRequestDto(
                petId,
                certNumber,
                "vetPass123",
                "clinicPass123"
        );

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

        PetProfileDto mockPetProfileDto = mock(PetProfileDto.class);
        RecordViewDto mockOriginatingRecordDto = mock(RecordViewDto.class);
        VetSummaryDto mockGeneratorVetDto = new VetSummaryDto(
                generatingVet.getId(), generatingVet.getName(), generatingVet.getSurname(),
                generatingVet.getAvatar(), generatingVet.getEmail(), generatingVet.getLicenseNumber(),
                clinic.getId(), clinic.getName(), clinic.getAddress(), clinic.getCity(),
                clinic.getCountry().name(), clinic.getPhone());

        expectedViewDto = new CertificateViewDto(
                certificateId,
                certNumber,
                mockPetProfileDto,
                mockOriginatingRecordDto,
                mockGeneratorVetDto,
                savedCertificate.getCreatedAt(),
                savedCertificate.getCreatedAt() != null ? savedCertificate.getCreatedAt().toLocalDate().plusDays(10) : null,
                savedCertificate.getCreatedAt() != null ? savedCertificate.getCreatedAt().toLocalDate().plusMonths(4) : null,
                "{\"key\":\"value\"}",
                "hashedPayload",
                "vetSignedHash",
                "clinicSignedHash"
        );
    }

    /**
     * --- Tests for generateCertificate ---
     */
    @Nested
    @DisplayName("generateCertificate Tests")
    class GenerateCertificateTests {
        private char[] vetPassChars;
        private char[] clinicPassChars;

        @BeforeEach
        void generateCertificateTestSetup() {
            vetPassChars = CertificateServiceImplTest.this.generationRequestDto.vetPrivateKeyPassword().toCharArray();
            clinicPassChars = CertificateServiceImplTest.this.generationRequestDto.clinicPrivateKeyPassword().toCharArray();
        }

        @Test
        @DisplayName("should generate certificate successfully when Rabies vac and Checkup are valid")
        void generateCertificate_Success_AllValid() throws HashingException {
            // --- Arrange --
            String expectedPayloadJson = "{\"key\":\"value\"}";
            String payloadHash = "a1b2c3d4e5f6";
            String vetSig = "base64VetSig==";
            String clinicSig = "base64ClinicSig==";
            Map<String, Object> mockPayloadMap = Map.of("key", "value");

            Long currentPetId = CertificateServiceImplTest.this.generationRequestDto.petId();
            String currentCertNumber = CertificateServiceImplTest.this.generationRequestDto.certificateNumber();

            given(entityFinderHelper.findVetOrFail(vetId)).willReturn(generatingVet);
            given(entityFinderHelper.findPetByIdOrFail(currentPetId)).willReturn(pet);
            given(validateHelper.findValidRabiesRecord(currentPetId)).willReturn(validRabiesRecord);
            willDoNothing().given(recordHelper).findValidCheckupRecord(currentPetId);
            willDoNothing().given(validateHelper).validateCertificateUniqueness(validRabiesRecord.getId(), currentCertNumber);
            given(certificateHelper.buildPayload(pet, validRabiesRecord, generatingVet, clinic, currentCertNumber))
                    .willReturn(mockPayloadMap);

            given(recordHelper.serializePayload(mockPayloadMap, validRabiesRecord.getId())).willReturn(expectedPayloadJson);
            given(recordHelper.hashPayload(expectedPayloadJson, currentPetId)).willReturn(payloadHash);
            given(recordHelper.signWithVetKey(generatingVet, payloadHash, vetPassChars)).willReturn(vetSig);
            given(recordHelper.signWithClinicKey(clinic, payloadHash, clinicPassChars)).willReturn(clinicSig);
            when(certificateRepository.save(certificateCaptor.capture())).thenAnswer(invocation -> {
                Certificate certToSave = invocation.getArgument(0);
                certToSave.setId(500L);
                certToSave.setCreatedAt(LocalDateTime.now());
                return certToSave;
            });
            PetProfileDto realPetProfileDto = petMapper.toProfileDto(pet);
            RecordViewDto realOriginatingRecordDto = recordMapper.toViewDto(validRabiesRecord);
            VetSummaryDto realGeneratorVetDto = userMapper.toVetSummaryDto(generatingVet);

            expectedViewDto = new CertificateViewDto(
                    500L,
                    currentCertNumber,
                    realPetProfileDto,
                    realOriginatingRecordDto,
                    realGeneratorVetDto,
                    null,
                    null,
                    null,
                    expectedPayloadJson,
                    payloadHash,
                    vetSig,
                    clinicSig
            );

            given(certificateMapper.toViewDto(any(Certificate.class))).willReturn(expectedViewDto);
            willDoNothing().given(certificateEventPublisher).publishCertificateGenerated(any());
            CertificateViewDto result = certificateService.generateCertificate(CertificateServiceImplTest.this.generationRequestDto, vetId);

            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(expectedViewDto.id());
            assertThat(result.certificateNumber()).isEqualTo(expectedViewDto.certificateNumber());
            assertThat(result.vetSignature()).isEqualTo(vetSig);
            assertThat(result.clinicSignature()).isEqualTo(clinicSig);

            then(certificateRepository).should().save(any(Certificate.class));
            Certificate capturedCertificate = certificateCaptor.getValue();

            assertThat(capturedCertificate.getMedicalRecord()).isEqualTo(validRabiesRecord);
            assertThat(capturedCertificate.getCertificateNumber()).isEqualTo(currentCertNumber);
            assertThat(capturedCertificate.getPayload()).isEqualTo(expectedPayloadJson);
            assertThat(capturedCertificate.getHash()).isEqualTo(payloadHash);
            assertThat(capturedCertificate.getVetSignature()).isEqualTo(vetSig);
            assertThat(capturedCertificate.getClinicSignature()).isEqualTo(clinicSig);
            assertThat(capturedCertificate.getMedicalRecord().isImmutable()).isTrue();

            then(certificateEventPublisher).should().publishCertificateGenerated(any(CertificateGeneratedEvent.class));
            then(certificateMapper).should().toViewDto(capturedCertificate);
        }

        @Test
        @DisplayName("should throw MissingRabiesVaccineException if no valid rabies record found")
        void generateCertificate_Failure_NoValidRabies() {
            // --- Arrange --
            given(entityFinderHelper.findVetOrFail(vetId)).willReturn(generatingVet);
            given(entityFinderHelper.findPetByIdOrFail(petId)).willReturn(pet);
            given(validateHelper.findValidRabiesRecord(petId)).willThrow(new MissingRabiesVaccineException(petId));

            // Act & Assert
            assertThatThrownBy(() -> certificateService.generateCertificate(generationRequestDto, vetId))
                    .isInstanceOf(MissingRabiesVaccineException.class)
                    .hasMessageContaining("No valid, signed, and current Rabies vaccine record found");

            then(entityFinderHelper).should().findVetOrFail(vetId);
            then(entityFinderHelper).should().findPetByIdOrFail(petId);
            then(validateHelper).should().findValidRabiesRecord(petId);
            then(recordRepository).should(never()).findAllSignedRabiesVaccinesDesc(anyLong());
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
            given(validateHelper.findValidRabiesRecord(petId)).willReturn(validRabiesRecord);
            doThrow(new MissingRecentCheckupException(petId, LocalDate.now().minusYears(1)))
                    .when(recordHelper).findValidCheckupRecord(petId);

            // Act & Assert
            assertThatThrownBy(() -> certificateService.generateCertificate(generationRequestDto, vetId))
                    .isInstanceOf(MissingRecentCheckupException.class)
                    .hasMessageContaining("No signed ANNUAL_CHECK found since");

            then(entityFinderHelper).should().findVetOrFail(vetId);
            then(entityFinderHelper).should().findPetByIdOrFail(petId);
            then(validateHelper).should().findValidRabiesRecord(petId);
            then(recordHelper).should().findValidCheckupRecord(petId);
            then(validateHelper).should(never()).validateCertificateUniqueness(anyLong(), anyString());
            then(certificateHelper).should(never()).buildPayload(any(), any(), any(), any(), anyString());
            then(certificateRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("should throw CertificateAlreadyExistsForRecordException if certificate already exists for the found rabies record")
        void generateCertificate_Failure_CertExistsForFoundRabiesRecord() {
            // Arrange
            given(entityFinderHelper.findVetOrFail(vetId)).willReturn(generatingVet);
            given(entityFinderHelper.findPetByIdOrFail(petId)).willReturn(pet);
            given(validateHelper.findValidRabiesRecord(petId)).willReturn(validRabiesRecord);
            willDoNothing().given(recordHelper).findValidCheckupRecord(petId);
            willThrow(new CertificateAlreadyExistsForRecordException(validRabiesRecord.getId()))
                    .given(validateHelper).validateCertificateUniqueness(validRabiesRecord.getId(), certNumber);

            // Act & Assert
            assertThatThrownBy(() -> certificateService.generateCertificate(generationRequestDto, vetId))
                    .isInstanceOf(CertificateAlreadyExistsForRecordException.class)
                    .hasMessageContaining("A certificate already exists for record " + validRabiesRecord.getId());

            then(entityFinderHelper).should().findVetOrFail(vetId);
            then(entityFinderHelper).should().findPetByIdOrFail(petId);
            then(validateHelper).should().findValidRabiesRecord(petId);
            then(recordHelper).should().findValidCheckupRecord(petId);
            then(validateHelper).should().validateCertificateUniqueness(validRabiesRecord.getId(), certNumber);

            then(certificateHelper).should(never()).buildPayload(any(), any(), any(), any(), anyString());
            then(certificateRepository).should(never()).save(any());
        }


        @Test
        @DisplayName("should throw CertificateNumberAlreadyExistsException if certificate number conflicts")
        void generateCertificate_Failure_CertNumberExists() {
            // Arrange
            given(entityFinderHelper.findVetOrFail(vetId)).willReturn(generatingVet);
            given(entityFinderHelper.findPetByIdOrFail(petId)).willReturn(pet);

            given(validateHelper.findValidRabiesRecord(petId)).willReturn(validRabiesRecord);
            willDoNothing().given(recordHelper).findValidCheckupRecord(petId);

            willThrow(new CertificateNumberAlreadyExistsException(certNumber))
                    .given(validateHelper).validateCertificateUniqueness(eq(validRabiesRecord.getId()), eq(certNumber));

            // Act & Assert
            assertThatThrownBy(() -> certificateService.generateCertificate(generationRequestDto, vetId))
                    .isInstanceOf(CertificateNumberAlreadyExistsException.class)
                    .hasMessageContaining("'" + certNumber + "' is already in use");

            then(entityFinderHelper).should().findVetOrFail(vetId);
            then(entityFinderHelper).should().findPetByIdOrFail(petId);
            then(validateHelper).should().findValidRabiesRecord(petId);
            then(recordHelper).should().findValidCheckupRecord(petId);
            then(validateHelper).should().validateCertificateUniqueness(eq(validRabiesRecord.getId()), eq(certNumber));
            then(certificateRepository).should(never()).save(any());
            then(certificateHelper).should(never()).buildPayload(any(), any(), any(), any(), anyString());
            then(hashingService).should(never()).hashString(anyString());
            then(signingService).should(never()).generateVetSignature(any(), anyString(), any());
        }

        @Test
        @DisplayName("should throw RuntimeException if hashing fails")
        void generateCertificate_Failure_HashingError() {
            // Arrange
            given(entityFinderHelper.findVetOrFail(vetId)).willReturn(generatingVet);
            given(entityFinderHelper.findPetByIdOrFail(petId)).willReturn(pet);
            given(validateHelper.findValidRabiesRecord(petId)).willReturn(validRabiesRecord);
            doNothing().when(recordHelper).findValidCheckupRecord(petId);
            doNothing().when(validateHelper).validateCertificateUniqueness(validRabiesRecord.getId(), certNumber);
            given(certificateHelper.buildPayload(any(), any(), any(), any(), anyString())).willReturn(Map.of("key", "value"));
            given(recordHelper.serializePayload(anyMap(), anyLong())).willReturn("{\"key\":\"value\"}");
            given(recordHelper.hashPayload(anyString(), eq(petId)))
                    .willThrow(new HashingException("Hash fail", null));

            // Act & Assert
            assertThatThrownBy(() -> certificateService.generateCertificate(generationRequestDto, vetId))
                    .isInstanceOf(HashingException.class)
                    .hasMessageContaining("Hash fail");

            then(recordRepository).should(never()).findAllSignedRabiesVaccinesDesc(anyLong());
            then(recordRepository).should(never()).findSignedCheckupsAfterDateDesc(anyLong(), any(), any());
            then(certificateRepository).should(never()).existsByMedicalRecordId(anyLong());
            then(certificateRepository).should(never()).findByCertificateNumber(anyString());

            then(entityFinderHelper).should().findVetOrFail(vetId);
            then(entityFinderHelper).should().findPetByIdOrFail(petId);
            then(validateHelper).should().findValidRabiesRecord(petId);
            then(recordHelper).should().findValidCheckupRecord(petId);
            then(validateHelper).should().validateCertificateUniqueness(validRabiesRecord.getId(), certNumber);
            then(certificateHelper).should().buildPayload(any(), any(), any(), any(), anyString());
            then(recordHelper).should().serializePayload(anyMap(), eq(validRabiesRecord.getId()));
            then(recordHelper).should().hashPayload(anyString(), eq(petId));
            then(recordHelper).should(never()).signWithVetKey(any(), any(), any());
            then(certificateRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("should throw RuntimeException if Vet signing fails")
        void generateCertificate_Failure_VetSigningError() {
            // Arrange
            String payloadHash = "hash123";

            given(entityFinderHelper.findVetOrFail(vetId)).willReturn(generatingVet);
            given(entityFinderHelper.findPetByIdOrFail(petId)).willReturn(pet);
            given(validateHelper.findValidRabiesRecord(petId)).willReturn(validRabiesRecord);
            doNothing().when(recordHelper).findValidCheckupRecord(petId);
            doNothing().when(validateHelper).validateCertificateUniqueness(validRabiesRecord.getId(), certNumber);
            given(certificateHelper.buildPayload(pet, validRabiesRecord, generatingVet, clinic, certNumber))
                    .willReturn(Map.of("key", "value"));
            given(recordHelper.serializePayload(any(), eq(validRabiesRecord.getId()))).willReturn("{\"key\":\"value\"}");
            given(recordHelper.hashPayload(anyString(), eq(petId))).willReturn(payloadHash);
            given(recordHelper.signWithVetKey(
                    eq(generatingVet),
                    eq(payloadHash),
                    aryEq(CertificateServiceImplTest.this.generationRequestDto.vetPrivateKeyPassword().toCharArray())
            )).willThrow(new RuntimeException("Vet key error"));

            // --- Act & Assert ---
            assertThatThrownBy(() -> certificateService.generateCertificate(CertificateServiceImplTest.this.generationRequestDto, vetId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Vet key error");

            then(entityFinderHelper).should().findVetOrFail(vetId);
            then(entityFinderHelper).should().findPetByIdOrFail(petId);
            then(validateHelper).should().findValidRabiesRecord(petId);
            then(validateHelper).should().validateCertificateUniqueness(validRabiesRecord.getId(), certNumber);
            then(certificateHelper).should().buildPayload(pet, validRabiesRecord, generatingVet, clinic, certNumber);

            then(recordHelper).should().findValidCheckupRecord(petId);
            then(recordHelper).should().serializePayload(any(), eq(validRabiesRecord.getId()));
            then(recordHelper).should().hashPayload(anyString(), eq(petId));
            then(recordHelper).should().signWithVetKey(
                    eq(generatingVet),
                    eq(payloadHash),
                    aryEq(CertificateServiceImplTest.this.generationRequestDto.vetPrivateKeyPassword().toCharArray())
            );
            then(recordHelper).should(never()).signWithClinicKey(any(), anyString(), any());

            then(certificateRepository).should(never()).save(any());
            then(certificateEventPublisher).should(never()).publishCertificateGenerated(any());
            then(certificateMapper).should(never()).toViewDto(any());
        }

        @Test
        @DisplayName("should throw RuntimeException if Clinic signing fails")
        void generateCertificate_Failure_ClinicSigningError() {
            // Arrange
            String payloadJson = "{\"key\":\"value\"}";
            String payloadHash = "hash123";
            Map<String, Object> mockPayloadMap = Map.of("key", "value");

            given(entityFinderHelper.findVetOrFail(vetId)).willReturn(generatingVet);
            given(entityFinderHelper.findPetByIdOrFail(petId)).willReturn(pet);
            given(validateHelper.findValidRabiesRecord(petId)).willReturn(validRabiesRecord);
            willDoNothing().given(recordHelper).findValidCheckupRecord(petId);
            willDoNothing().given(validateHelper).validateCertificateUniqueness(validRabiesRecord.getId(), certNumber);
            given(certificateHelper.buildPayload(pet, validRabiesRecord, generatingVet, clinic, certNumber))
                    .willReturn(mockPayloadMap);
            given(recordHelper.serializePayload(mockPayloadMap, validRabiesRecord.getId())).willReturn(payloadJson);
            given(recordHelper.hashPayload(payloadJson, pet.getId())).willReturn(payloadHash);
            given(recordHelper.signWithVetKey(
                    eq(generatingVet),
                    eq(payloadHash),
                    aryEq(CertificateServiceImplTest.this.generationRequestDto.vetPrivateKeyPassword().toCharArray())
            )).willReturn("dummyVetSignature");
            given(recordHelper.signWithClinicKey(
                    eq(clinic),
                    eq(payloadHash),
                    aryEq(CertificateServiceImplTest.this.generationRequestDto.clinicPrivateKeyPassword().toCharArray())
            )).willThrow(new RuntimeException("Failed to generate Clinic digital signature.", new RuntimeException("Original signing error")));

            // --- Act & Assert ---
            assertThatThrownBy(() -> certificateService.generateCertificate(CertificateServiceImplTest.this.generationRequestDto, vetId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Failed to generate Clinic digital signature.")
                    .cause().hasMessage("Original signing error");

            then(entityFinderHelper).should().findVetOrFail(vetId);
            then(entityFinderHelper).should().findPetByIdOrFail(petId);
            then(validateHelper).should().findValidRabiesRecord(petId);
            then(recordHelper).should().findValidCheckupRecord(petId);
            then(validateHelper).should().validateCertificateUniqueness(validRabiesRecord.getId(), certNumber);
            then(certificateHelper).should().buildPayload(pet, validRabiesRecord, generatingVet, clinic, certNumber);
            then(recordHelper).should().serializePayload(mockPayloadMap, validRabiesRecord.getId());
            then(recordHelper).should().hashPayload(payloadJson, pet.getId());
            then(recordHelper).should().signWithVetKey(
                    eq(generatingVet),
                    eq(payloadHash),
                    aryEq(CertificateServiceImplTest.this.generationRequestDto.vetPrivateKeyPassword().toCharArray())
            );
            then(recordHelper).should().signWithClinicKey(
                    eq(clinic),
                    eq(payloadHash),
                    aryEq(CertificateServiceImplTest.this.generationRequestDto.clinicPrivateKeyPassword().toCharArray()));
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
            Throwable thrown = catchThrowable(() -> certificateService.findCertificatesByPet(pet.getId(), unauthorizedUserId));
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
                    .willThrow(new EntityNotFoundException(Pet.class.getSimpleName(), nonExistentPetId));

            Throwable thrown = catchThrowable(() -> certificateService.findCertificatesByPet(nonExistentPetId, owner.getId()));
            assertThat(thrown)
                    .isInstanceOf(EntityNotFoundException.class);

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
                    .willThrow(new EntityNotFoundException(Certificate.class.getSimpleName(), certId));

            Throwable thrown = catchThrowable(() -> certificateService.findCertificateById(certId, owner.getId()));
            assertThat(thrown)
                    .isInstanceOf(EntityNotFoundException.class);

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
            Throwable thrown = catchThrowable(() ->  certificateService.findCertificateById(savedCertificate.getId(), unauthorizedUserId));
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
            willDoNothing().given(authorizationHelper).verifyUserAuthorizationForPet(requesterOwnerId,
                    associatedPet, "get QR data for certificate");
            given(qrCodeService.generateQrData(savedCertificate)).willReturn(expectedQrData);

            // Act
            String actualQrData = certificateService.getQrDataForCertificate(certificateId, requesterOwnerId);

            // Assert
            assertThat(actualQrData).isEqualTo(expectedQrData);
            then(entityFinderHelper).should().findCertificateOrFail(certificateId);
            then(authorizationHelper).should().verifyUserAuthorizationForPet(requesterOwnerId,
                    associatedPet, "get QR data for certificate");
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
            willDoNothing().given(authorizationHelper).verifyUserAuthorizationForPet(requesterVetId,
                    associatedPet, "get QR data for certificate");
            given(qrCodeService.generateQrData(savedCertificate)).willReturn(expectedQrData);

            // Act
            String actualQrData = certificateService.getQrDataForCertificate(certificateId, requesterVetId);

            // Assert
            assertThat(actualQrData).isEqualTo(expectedQrData);
            then(entityFinderHelper).should().findCertificateOrFail(certificateId);
            then(authorizationHelper).should().verifyUserAuthorizationForPet(requesterVetId,
                    associatedPet, "get QR data for certificate");
            then(qrCodeService).should().generateQrData(savedCertificate);
        }

        @Test
        @DisplayName("should throw EntityNotFoundException if certificate not found")
        void getQrData_Failure_CertNotFound() {
            Long nonExistentCertId = 888L;
            given(entityFinderHelper.findCertificateOrFail(nonExistentCertId))
                    .willThrow(new EntityNotFoundException(Certificate.class.getSimpleName(), nonExistentCertId));

            assertThatThrownBy(() -> certificateService.getQrDataForCertificate(nonExistentCertId, requesterOwnerId))
                    .isInstanceOf(EntityNotFoundException.class);

            then(authorizationHelper).should(never()).verifyUserAuthorizationForPet(anyLong(), any(), anyString());
            then(qrCodeService).should(never()).generateQrData(any());
        }

        @Test
        @DisplayName("should throw AccessDeniedException if requester not authorized for pet")
        void getQrData_Failure_Unauthorized() {
            given(entityFinderHelper.findCertificateOrFail(certificateId)).willReturn(savedCertificate);
            Pet associatedPet = savedCertificate.getPet();
            willThrow(new AccessDeniedException("User not authorized for pet"))
                    .given(authorizationHelper).verifyUserAuthorizationForPet(unauthorizedUserId,
                            associatedPet, "get QR data for certificate");

            assertThatThrownBy(() -> certificateService.getQrDataForCertificate(certificateId, unauthorizedUserId))
                    .isInstanceOf(AccessDeniedException.class);

            then(qrCodeService).should(never()).generateQrData(any());
        }

        @Test
        @DisplayName("should throw RuntimeException if QrCodeService fails")
        void getQrData_Failure_QrServiceError() {
            given(entityFinderHelper.findCertificateOrFail(certificateId)).willReturn(savedCertificate);
            Pet associatedPet = savedCertificate.getPet();
            willDoNothing().given(authorizationHelper).verifyUserAuthorizationForPet(requesterOwnerId,
                    associatedPet, "get QR data for certificate");

            given(qrCodeService.generateQrData(savedCertificate)).willThrow(new RuntimeException("CBOR failed"));

            assertThatThrownBy(() -> certificateService.getQrDataForCertificate(certificateId, requesterOwnerId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("CBOR failed");
        }
    }
}