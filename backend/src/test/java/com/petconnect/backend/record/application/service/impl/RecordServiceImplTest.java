package com.petconnect.backend.record.application.service.impl;

import com.petconnect.backend.common.helper.AuthorizationHelper;
import com.petconnect.backend.common.helper.EntityFinderHelper;
import com.petconnect.backend.common.helper.RecordHelper;
import com.petconnect.backend.common.helper.ValidateHelper;
import com.petconnect.backend.common.service.SigningService;
import com.petconnect.backend.exception.EntityNotFoundException;
import com.petconnect.backend.exception.RecordImmutableException;
import com.petconnect.backend.pet.domain.model.Pet;
import com.petconnect.backend.pet.domain.model.Breed;
import com.petconnect.backend.pet.domain.model.Specie;
import com.petconnect.backend.record.application.dto.*;
import com.petconnect.backend.record.application.mapper.RecordMapper;
import com.petconnect.backend.record.application.mapper.VaccineMapper;
import com.petconnect.backend.record.domain.model.Record;
import com.petconnect.backend.record.domain.model.RecordType;
import com.petconnect.backend.record.domain.model.Vaccine;
import com.petconnect.backend.record.domain.repository.RecordRepository;
import com.petconnect.backend.user.application.mapper.UserMapper;
import com.petconnect.backend.user.domain.model.*;
import com.petconnect.backend.security.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link RecordServiceImpl}.
 * Verifies the business logic for managing medical records using Mockito.
 *
 * @author ibosquet
 */
@ExtendWith(MockitoExtension.class)
class RecordServiceImplTest {

    // --- Mocks ---
    @Mock private RecordRepository recordRepository;
    @Mock private RecordMapper recordMapper;
    @Mock private VaccineMapper vaccineMapper;
    @Mock private EntityFinderHelper entityFinderHelper;
    @Mock private AuthorizationHelper authorizationHelper;
    @Mock private ValidateHelper validateHelper;
    @Mock private UserMapper userMapper;
    @Mock private RecordHelper recordHelper;
    @Mock private SigningService signingService;
    @Mock private JwtUtils jwtUtils;

    @InjectMocks
    private RecordServiceImpl recordService;

    @Captor private ArgumentCaptor<Record> recordCaptor;

    private Pet pet;
    private Owner owner;
    private Vet vet;
    private ClinicStaff adminSameClinic;
    private Record record1, record2;
    private RecordViewDto recordDto1, recordDto2;
    private final Long petId = 1L;
    private final Long ownerId = 10L;
    private final Long vetId = 11L;
    private final Long adminSameClinicId = 50L;
    private final Long recordId1 = 101L;
    private RoleEntity adminRole;

    @BeforeEach
    void setUp() {

        Clinic clinic;

        Long recordId2 = 102L;
        adminRole = RoleEntity.builder().roleEnum(RoleEnum.ADMIN).build(); adminRole.setId(3L);
        owner = new Owner(); owner.setId(ownerId);owner.setUsername("testowner");
        clinic = new Clinic(); clinic.setId(1L); clinic.setName("Clinic One");
        vet = new Vet(); vet.setId(vetId);vet.setUsername("testvet"); vet.setClinic(clinic);

        Breed breed = Breed.builder().id(5L).name("Siamese").specie(Specie.CAT).build();

        pet = new Pet();pet.setId(petId);pet.setOwner(owner);pet.setBreed(breed);

        adminSameClinic = new ClinicStaff(); adminSameClinic.setId(adminSameClinicId); adminSameClinic.setClinic(clinic);
        adminSameClinic.setRoles(Set.of(adminRole));

        record1 = new Record();
        record1.setId(recordId1);
        record1.setPet(pet);
        record1.setCreator(owner);
        record1.setType(RecordType.OTHER);
        record1.setDescription("Observation by owner");
        record1.setCreatedAt(LocalDateTime.now().minusDays(1));

        record2 = new Record();
        record2.setId(recordId2);
        record2.setPet(pet);
        record2.setCreator(vet);
        record2.setType(RecordType.ANNUAL_CHECK);
        record2.setDescription("Annual checkup results");
        record2.setVetSignature("SIGNED_BY_VET_XYZ");
        record2.setCreatedAt(LocalDateTime.now());

        recordDto1 = new RecordViewDto(recordId1,
                RecordType.OTHER, "Observation by owner",
                null, record1.getCreatedAt(),
                null, null, null, null, null, null, null, null, null,
                null);
        recordDto2 = new RecordViewDto(recordId2,
                RecordType.ANNUAL_CHECK, "Annual checkup results", "SIGNED_BY_VET_XYZ",
                record2.getCreatedAt(),  null, null, null, null, null, null, null, null, null,
                null);

        adminSameClinic = new ClinicStaff();
        adminSameClinic.setId(50L);
        adminSameClinic.setClinic(clinic);
        adminSameClinic.setRoles(Set.of(adminRole));
    }

    /**
     * --- Tests for createRecord ---
     */
    @Nested
    @DisplayName("createRecord Tests")
    class CreateRecordTests {

        private RecordCreateDto ownerRecordDto;
        private RecordCreateDto vetVaccineDto;
        private VaccineCreateDto vaccineDetailsDto;
        private Vaccine newVaccineEntity;
        private RecordViewDto expectedOwnerRecordViewDto;
        private RecordViewDto expectedVetVaccineViewDto;
        private final String dummyVetPassword = "vetPassword123";
        private final Long petIdForCreate = petId;

        @BeforeEach
        void createSetup() {
            ownerRecordDto = new RecordCreateDto( petIdForCreate,RecordType.OTHER, "Felt warm yesterday", null, null);

            vaccineDetailsDto = new VaccineCreateDto("RabiesVac", 1, "LabX", "Batch123", true);
            vetVaccineDto = new RecordCreateDto(petIdForCreate,RecordType.VACCINE,
                    "Rabies vaccination administered", vaccineDetailsDto, dummyVetPassword);

            Record newRecordFromVet;
            Record.builder().pet(pet).creator(owner).type(ownerRecordDto.type())
                    .description(ownerRecordDto.description()).build();
            newRecordFromVet = Record.builder().pet(pet).creator(vet).type(vetVaccineDto.type())
                    .description(vetVaccineDto.description()).build();
            newVaccineEntity = Vaccine.builder().name(vaccineDetailsDto.name()).validity(vaccineDetailsDto.validity())
                    .laboratory(vaccineDetailsDto.laboratory()).batchNumber(vaccineDetailsDto.batchNumber()).build();
            newRecordFromVet.setVaccineDetails(newVaccineEntity);

            expectedOwnerRecordViewDto = new RecordViewDto(200L, ownerRecordDto.type(),
                    ownerRecordDto.description(), null, LocalDateTime.now(),
                    null, null, null, null, null, null, null,
            null, null, null);
            expectedVetVaccineViewDto = new RecordViewDto(201L, vetVaccineDto.type(),
                    vetVaccineDto.description(), "TEMP_SIGNATURE", LocalDateTime.now(),
                    null, null, null, null, null, null, null,
                    null, null, null);
        }

        @Test
        @DisplayName("should create OTHER record successfully when called by Owner")
        void createRecord_Success_Owner_Other() {
            // Arrange
            given(entityFinderHelper.findPetByIdOrFail(petId)).willReturn(pet);
            given(entityFinderHelper.findUserOrFail(ownerId)).willReturn(owner);
            doNothing().when(authorizationHelper).verifyUserAuthorizationForPet(ownerId, pet, "create record for");
            doNothing().when(validateHelper).validateRecordCreationDto(ownerRecordDto);

            given(recordRepository.save(any(Record.class))).willAnswer(inv -> {
                Record recordEntity = inv.getArgument(0);
                recordEntity.setId(200L);
                return recordEntity;
            });
            given(recordMapper.toViewDto(any(Record.class))).willReturn(expectedOwnerRecordViewDto);

            // Act
            RecordViewDto result = recordService.createRecord(ownerRecordDto, ownerId);

            // Assert
            assertThat(result).isNotNull().isEqualTo(expectedOwnerRecordViewDto);
            then(entityFinderHelper).should().findPetByIdOrFail(petId);
            then(entityFinderHelper).should().findUserOrFail(ownerId);
            then(authorizationHelper).should().verifyUserAuthorizationForPet(ownerId, pet, "create record for");
            then(validateHelper).should().validateRecordCreationDto(ownerRecordDto);
            then(vaccineMapper).should(never()).fromCreateDto(any());
            then(recordRepository).should().save(recordCaptor.capture());
            then(recordMapper).should().toViewDto(any(Record.class));

            Record saved = recordCaptor.getValue();
            assertThat(saved.getType()).isEqualTo(RecordType.OTHER);
            assertThat(saved.getCreator()).isEqualTo(owner);
            assertThat(saved.getPet()).isEqualTo(pet);
            assertThat(saved.getVaccine()).isNull();
            assertThat(saved.getVetSignature()).isNull();
        }

        @Test
        @DisplayName("should create VACCINE record successfully when called by Vet and signRecord=true")
        void createRecord_Success_Vet_Vaccine_Signed() {
            // Arrange
            String expectedDataToSign = "petId=1|vetId=11|...";
            String expectedSignature = "GENERATED_SIGNATURE_BASE64";

            given(entityFinderHelper.findPetByIdOrFail(petId)).willReturn(pet);
            given(entityFinderHelper.findUserOrFail(vetId)).willReturn(vet); // Return Vet user
            doNothing().when(authorizationHelper).verifyUserAuthorizationForPet(vetId, pet, "create record for");
            doNothing().when(validateHelper).validateRecordCreationDto(vetVaccineDto);
            given(vaccineMapper.fromCreateDto(vaccineDetailsDto)).willReturn(newVaccineEntity);
            given(recordHelper.buildSignableData(pet, vet, vetVaccineDto)).willReturn(expectedDataToSign);
            given(signingService.generateVetSignature(
                    (vet),
                    (expectedDataToSign),
                    (dummyVetPassword.toCharArray())
            )).willReturn(expectedSignature);

            given(recordRepository.save(any(Record.class))).willAnswer(inv -> {
                Record recordEntity = inv.getArgument(0);
                recordEntity.setId(201L);
                return recordEntity;
            });

            expectedVetVaccineViewDto = new RecordViewDto(
                    201L,
                    vetVaccineDto.type(),
                    vetVaccineDto.description(),
                    expectedSignature,
                    any(LocalDateTime.class),
                    vet.getCreatedBy(), vet.getUpdatedAt(), vet.getUpdatedBy(),
                    userMapper.mapToBaseProfileDTO(vet),
                    vaccineMapper.toViewDto(newVaccineEntity),
                    vet.getClinic() != null ? vet.getClinic().getId() : null,
                    vet.getClinic() != null ? vet.getClinic().getName() : null,
                    pet.getId(),
                    pet.getName(),
                    pet.getBreed() != null ? pet.getBreed().getSpecie() : null
            );
            given(recordMapper.toViewDto(any(Record.class))).willReturn(expectedVetVaccineViewDto);

            // Act
            RecordViewDto result = recordService.createRecord(vetVaccineDto, vetId);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.vetSignature()).isEqualTo(expectedSignature);

            // Assert
            then(entityFinderHelper).should().findPetByIdOrFail(petId);
            then(entityFinderHelper).should().findUserOrFail(vetId);
            then(authorizationHelper).should().verifyUserAuthorizationForPet(vetId, pet, "create record for");
            then(validateHelper).should().validateRecordCreationDto(vetVaccineDto);
            then(vaccineMapper).should().fromCreateDto(vaccineDetailsDto);
            then(recordHelper).should().buildSignableData(pet, vet, vetVaccineDto);
            then(signingService).should().generateVetSignature(eq(vet), eq(expectedDataToSign), eq(dummyVetPassword.toCharArray()));
            then(recordRepository).should().save(recordCaptor.capture());
            then(recordMapper).should().toViewDto(any(Record.class));

            Record saved = recordCaptor.getValue();
            assertThat(saved.getType()).isEqualTo(RecordType.VACCINE);
            assertThat(saved.getCreator()).isEqualTo(vet);
            assertThat(saved.getVaccine()).isNotNull();
            assertThat(saved.getVetSignature()).isEqualTo(expectedSignature);
        }

        @Test
        @DisplayName("should throw IllegalArgumentException if type is VACCINE but vaccine DTO is null")
        void createRecord_Failure_VaccineDtoMissing() {
            // Arrange
            RecordCreateDto invalidDto = new RecordCreateDto(petIdForCreate,RecordType.VACCINE, "Forgot details", null, null); // Vaccine DTO null
            given(entityFinderHelper.findPetByIdOrFail(petId)).willReturn(pet);
            given(entityFinderHelper.findUserOrFail(vetId)).willReturn(vet);
            doNothing().when(authorizationHelper).verifyUserAuthorizationForPet(vetId, pet, "create record for");
            doThrow(new IllegalArgumentException("Vaccine details are required"))
                    .when(validateHelper).validateRecordCreationDto(invalidDto);

            // Act & Assert
            assertThatThrownBy(() -> recordService.createRecord(invalidDto, vetId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Vaccine details are required");

            then(recordRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("should throw IllegalArgumentException if type is not VACCINE but vaccine DTO is provided")
        void createRecord_Failure_VaccineDtoUnexpected() {
            // Arrange
            RecordCreateDto invalidDto = new RecordCreateDto(petIdForCreate,RecordType.ILLNESS, "Flu", vaccineDetailsDto, null);
            given(entityFinderHelper.findPetByIdOrFail(petId)).willReturn(pet);
            given(entityFinderHelper.findUserOrFail(vetId)).willReturn(vet);
            doNothing().when(authorizationHelper).verifyUserAuthorizationForPet(vetId, pet, "create record for");
            // Mock validateHelper to throw
            doThrow(new IllegalArgumentException("Vaccine details should only be provided"))
                    .when(validateHelper).validateRecordCreationDto(invalidDto);

            // Act & Assert
            assertThatThrownBy(() -> recordService.createRecord(invalidDto, vetId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Vaccine details should only be provided");

            then(recordRepository).should(never()).save(any());
        }
    }

    /**
     * --- Tests for findRecordsByPetId ---
     */
    @Nested
    @DisplayName("findRecordsByPetId Tests")
    class FindRecordsByPetIdTests {

        private Pageable pageable;
        private final Long requesterOwnerId = ownerId;
        private final Long petToFindId = petId;

        @BeforeEach
        void findRecordsSetup() {
            pageable = PageRequest.of(0, 10);
            record1.setPet(pet); record1.setCreator(owner);
            record2.setPet(pet); record2.setCreator(vet);
        }

        /**
         * Test successful retrieval when the requester is the owner.
         */
        @Test
        @DisplayName("should return page of records when requested by owner")
        void findRecords_Success_ByOwner() {
            // Arrange
            List<Record> recordList = List.of(record2, record1);
            Page<Record> recordPage = new PageImpl<>(recordList, pageable, 2);
            Page<RecordViewDto> expectedDtoPage = new PageImpl<>(List.of(recordDto2, recordDto1), pageable, 2);

            given(entityFinderHelper.findPetByIdOrFail(petToFindId)).willReturn(pet);
            doNothing().when(authorizationHelper).verifyUserAuthorizationForPet(requesterOwnerId, pet, "view records for");
            given(recordRepository.findByPetIdOrderByCreatedAtDesc(petToFindId, pageable)).willReturn(recordPage);
            given(recordMapper.toViewDtoPage(recordPage)).willReturn(expectedDtoPage);


            // Act
            Page<RecordViewDto> result = recordService.findRecordsByPetId(petToFindId, requesterOwnerId, pageable);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result.getContent()).hasSize(2).containsExactly(recordDto2, recordDto1);

            then(entityFinderHelper).should().findPetByIdOrFail(petToFindId);
            then(authorizationHelper).should().verifyUserAuthorizationForPet(requesterOwnerId, pet, "view records for");
            then(recordRepository).should().findByPetIdOrderByCreatedAtDesc(petToFindId, pageable);
            then(recordMapper).should().toViewDtoPage(recordPage);
        }

        /**
         * Test successful retrieval when the requester is authorized staff.
         */
        @Test
        @DisplayName("should return page of records when requested by authorized staff")
        void findRecords_Success_ByStaff() {
            // Arrange
            Long requesterVetId = vetId;
            List<Record> recordList = List.of(record2, record1);
            Page<Record> recordPage = new PageImpl<>(recordList, pageable, 2);
            Page<RecordViewDto> expectedDtoPage = new PageImpl<>(List.of(recordDto2, recordDto1), pageable, 2);

            given(entityFinderHelper.findPetByIdOrFail(petToFindId)).willReturn(pet);
            doNothing().when(authorizationHelper).verifyUserAuthorizationForPet(requesterVetId, pet, "view records for");
            given(recordRepository.findByPetIdOrderByCreatedAtDesc(petToFindId, pageable)).willReturn(recordPage);
            given(recordMapper.toViewDtoPage(recordPage)).willReturn(expectedDtoPage);

            // Act
            Page<RecordViewDto> result = recordService.findRecordsByPetId(petToFindId, requesterVetId, pageable);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result.getContent()).hasSize(2).containsExactly(recordDto2, recordDto1);

            then(entityFinderHelper).should().findPetByIdOrFail(petToFindId);
            then(authorizationHelper).should().verifyUserAuthorizationForPet(requesterVetId, pet, "view records for");
            then(recordRepository).should().findByPetIdOrderByCreatedAtDesc(petToFindId, pageable);
            then(recordMapper).should().toViewDtoPage(recordPage);
        }

        /**
         * Test retrieval when a pet has no records.
         */
        @Test
        @DisplayName("should return empty page when pet has no records")
        void findRecords_Success_NoRecords() {
            // Arrange
            given(entityFinderHelper.findPetByIdOrFail(petToFindId)).willReturn(pet);
            doNothing().when(authorizationHelper).verifyUserAuthorizationForPet(requesterOwnerId, pet, "view records for");
            given(recordRepository.findByPetIdOrderByCreatedAtDesc(petToFindId, pageable)).willReturn(Page.empty(pageable));
            given(recordMapper.toViewDtoPage(Page.empty(pageable))).willReturn(Page.empty(pageable));

            // Act
            Page<RecordViewDto> result = recordService.findRecordsByPetId(petToFindId, requesterOwnerId, pageable);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.isEmpty()).isTrue();

            then(entityFinderHelper).should().findPetByIdOrFail(petToFindId);
            then(authorizationHelper).should().verifyUserAuthorizationForPet(requesterOwnerId, pet, "view records for");
            then(recordRepository).should().findByPetIdOrderByCreatedAtDesc(petToFindId, pageable);
            then(recordMapper).should().toViewDtoPage(Page.empty(pageable));
        }

        /**
         * Test failure when a pet is not found.
         */
        @Test
        @DisplayName("should throw EntityNotFoundException if pet not found")
        void findRecords_Failure_PetNotFound() {
            // Arrange
            given(entityFinderHelper.findPetByIdOrFail(999L))
                    .willThrow(new EntityNotFoundException(Pet.class.getSimpleName(), 999L));

            // Act & Assert
            assertThatThrownBy(() -> recordService.findRecordsByPetId(999L, requesterOwnerId, pageable))
                    .isInstanceOf(EntityNotFoundException.class);

            then(authorizationHelper).should(never()).verifyUserAuthorizationForPet(anyLong(), any(), anyString());
            then(recordRepository).should(never()).findByPetIdOrderByCreatedAtDesc(anyLong(), any());
        }

        /**
         * Test failure when a requester is not authorized for the pet.
         */
        @Test
        @DisplayName("should throw AccessDeniedException if requester not authorized")
        void findRecords_Failure_Unauthorized() {
            // Arrange
            Long unauthorizedUserId = 998L;
            given(entityFinderHelper.findPetByIdOrFail(petToFindId)).willReturn(pet);
            doThrow(new AccessDeniedException("User is not authorized..."))
                    .when(authorizationHelper).verifyUserAuthorizationForPet(unauthorizedUserId, pet, "view records for");

            // Act & Assert
            assertThatThrownBy(() -> recordService.findRecordsByPetId(petToFindId, unauthorizedUserId, pageable))
                    .isInstanceOf(AccessDeniedException.class);

            then(entityFinderHelper).should().findPetByIdOrFail(petToFindId);
            then(authorizationHelper).should().verifyUserAuthorizationForPet(unauthorizedUserId, pet, "view records for");
            then(recordRepository).should(never()).findByPetIdOrderByCreatedAtDesc(anyLong(), any());
        }
    }

    /**
     * --- Tests for findRecordById ---
     */
    @Nested
    @DisplayName("findRecordById Tests")
    class FindRecordByIdTests {

        private final Long targetRecordId = recordId1;
        private final Long ownerRequesterId = ownerId;
        private final Long unauthorizedUserId = 99L;
        private final String actionContext = "view record for";

        @BeforeEach
        void findRecordByIdSetup() {
            record1.setPet(pet);
            record1.setCreator(owner);
        }

        /**
         * Test successful retrieval when the requester is the owner of the pet associated with the record.
         */
        @Test
        @DisplayName("should return record view DTO when requested by pet owner")
        void findRecordById_Success_ByOwner() {
            // Arrange
            given(entityFinderHelper.findRecordByIdOrFail(targetRecordId)).willReturn(record1);
            doNothing().when(authorizationHelper).verifyUserAuthorizationForPet(ownerRequesterId, pet, actionContext);
            given(recordMapper.toViewDto(record1)).willReturn(recordDto1);

            // Act
            RecordViewDto result = recordService.findRecordById(targetRecordId, ownerRequesterId);

            // Assert
            assertThat(result).isNotNull().isEqualTo(recordDto1);

            then(entityFinderHelper).should().findRecordByIdOrFail(targetRecordId);
            then(authorizationHelper).should().verifyUserAuthorizationForPet(ownerRequesterId, pet, actionContext);
            then(recordMapper).should().toViewDto(record1);
        }

        /**
         * Test successful retrieval when the requester is authorized staff for the pet associated with the record.
         */
        @Test
        @DisplayName("should return record view DTO when requested by authorized staff")
        void findRecordById_Success_ByStaff() {
            // Arrange
            Long vetRequesterId = vetId;
            given(entityFinderHelper.findRecordByIdOrFail(targetRecordId)).willReturn(record1);
            doNothing().when(authorizationHelper).verifyUserAuthorizationForPet(vetRequesterId, pet, actionContext);
            given(recordMapper.toViewDto(record1)).willReturn(recordDto1);


            // Act
            RecordViewDto result = recordService.findRecordById(targetRecordId, vetRequesterId);

            // Assert
            assertThat(result).isNotNull().isEqualTo(recordDto1);

            then(entityFinderHelper).should().findRecordByIdOrFail(targetRecordId);
            then(authorizationHelper).should().verifyUserAuthorizationForPet(vetRequesterId, pet, actionContext);
            then(recordMapper).should().toViewDto(record1);
        }

        /**
         * Test failure when the Record ID does not exist.
         */
        @Test
        @DisplayName("should throw EntityNotFoundException if record not found")
        void findRecordById_Failure_RecordNotFound() {
            // Arrange
            Long nonExistentRecordId = 888L;
            given(entityFinderHelper.findRecordByIdOrFail(nonExistentRecordId))
                    .willThrow(new EntityNotFoundException(Record.class.getSimpleName(), nonExistentRecordId));

            // Act & Assert
            assertThatThrownBy(() -> recordService.findRecordById(nonExistentRecordId, ownerRequesterId))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Record not found with id: " + nonExistentRecordId);

            then(authorizationHelper).should(never()).verifyUserAuthorizationForPet(anyLong(), any(), anyString());
            then(recordMapper).should(never()).toViewDto(any());
        }

        /**
         * Test failure when the requester is not authorized for the pet associated with the record.
         */
        @Test
        @DisplayName("should throw AccessDeniedException if requester not authorized for pet")
        void findRecordById_Failure_Unauthorized() {
            // Arrange
            given(entityFinderHelper.findRecordByIdOrFail(targetRecordId)).willReturn(record1);
            doThrow(new AccessDeniedException(String.format("is not authorized to %s pet", actionContext)))
                    .when(authorizationHelper).verifyUserAuthorizationForPet(unauthorizedUserId, pet, actionContext);

            // Act & Assert
            assertThatThrownBy(() -> recordService.findRecordById(targetRecordId, unauthorizedUserId))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining(String.format("is not authorized to %s pet", actionContext));

            then(entityFinderHelper).should().findRecordByIdOrFail(targetRecordId);
            then(authorizationHelper).should().verifyUserAuthorizationForPet(unauthorizedUserId, pet, actionContext);
            then(recordMapper).should(never()).toViewDto(any());
        }
    }

    /**
     * --- Tests for updateUnsignedRecord ---
     */
    @Nested
    @DisplayName("updateUnsignedRecord Tests")
    class UpdateUnsignedRecordTests {

        private Record recordToUpdateOwner;
        private Record recordToUpdateStaff;
        private ClinicStaff requesterStaffOtherClinic;
        private RecordUpdateDto updateDtoDesc;
        private RecordUpdateDto updateDtoBoth;

        private final Long recordOwnerRecId = 301L;
        private final Long recordStaffRecId = 302L;
        private final Long ownerUserId = ownerId;
        private final Long requesterAdminSameClinicId = adminSameClinicId;
        private final Long requesterStaffOtherClinicId = 13L;

        @BeforeEach
        void updateSetup() {
            ClinicStaff recordCreatorStaff = vet;
            Record recordSigned;

            requesterStaffOtherClinic = new ClinicStaff();
            requesterStaffOtherClinic.setId(requesterStaffOtherClinicId);
            Clinic clinic2 = new Clinic(); clinic2.setId(2L); clinic2.setName("Clinic Two");
            requesterStaffOtherClinic.setClinic(clinic2);
            requesterStaffOtherClinic.setRoles(Set.of(adminRole));

            recordToUpdateOwner = new Record();
            recordToUpdateOwner.setId(recordOwnerRecId);
            recordToUpdateOwner.setPet(pet);
            recordToUpdateOwner.setCreator(owner);
            recordToUpdateOwner.setType(RecordType.OTHER);
            recordToUpdateOwner.setDescription("Original Owner Desc");
            recordToUpdateOwner.setVetSignature(null);

            recordToUpdateStaff = new Record();
            recordToUpdateStaff.setId(recordStaffRecId);
            recordToUpdateStaff.setPet(pet);
            recordToUpdateStaff.setCreator(recordCreatorStaff);
            recordToUpdateStaff.setType(RecordType.ILLNESS);
            recordToUpdateStaff.setDescription("Original Staff Desc");
            recordToUpdateStaff.setVetSignature(null);

            Long recordSignedRecId = 303L;
            recordSigned = new Record();
            recordSigned.setId(recordSignedRecId);
            recordSigned.setPet(pet);
            recordSigned.setCreator(recordCreatorStaff);
            recordSigned.setType(RecordType.ANNUAL_CHECK);
            recordSigned.setDescription("Signed record");
            recordSigned.setVetSignature("SIGNATURE_PRESENT");

            Long recordVaccineRecId = 304L;
            Record recordVaccine = new Record();
            recordVaccine.setId(recordVaccineRecId);
            recordVaccine.setPet(pet);
            recordVaccine.setCreator(recordCreatorStaff);
            recordVaccine.setType(RecordType.VACCINE);
            recordVaccine.setDescription("Vaccine Record");
            recordVaccine.setVetSignature(null);
            recordVaccine.setVaccine(new Vaccine());

            new RecordUpdateDto(RecordType.ILLNESS, null);
            updateDtoDesc = new RecordUpdateDto(null, "Updated Description");
            updateDtoBoth = new RecordUpdateDto(RecordType.ANNUAL_CHECK, "Updated Description Both");
            new RecordUpdateDto(recordToUpdateOwner.getType(), recordToUpdateOwner.getDescription());
            new RecordUpdateDto(RecordType.VACCINE, "Trying to change to vaccine");
        }

        @Test
        @DisplayName("should update record successfully when Owner updates own record")
        void update_Success_OwnerUpdatesOwn() {
            // Arrange
            given(entityFinderHelper.findRecordByIdOrFail(recordOwnerRecId)).willReturn(recordToUpdateOwner);
            given(entityFinderHelper.findUserOrFail(ownerUserId)).willReturn(owner);
            given(recordRepository.save(any(Record.class))).willAnswer(inv -> inv.getArgument(0));
            RecordViewDto expectedDto = new RecordViewDto(recordOwnerRecId, updateDtoBoth.type(),
                    updateDtoBoth.description(), null, recordToUpdateOwner.getCreatedAt(),
                    null, null, null, null, null, null, null,
                    null, null,null);
            given(recordMapper.toViewDto(any(Record.class))).willReturn(expectedDto);

            // Act
            RecordViewDto result = recordService.updateUnsignedRecord(recordOwnerRecId, updateDtoBoth, ownerUserId);

            // Assert
            assertThat(result).isEqualTo(expectedDto);
            then(recordRepository).should().save(recordCaptor.capture());
            Record saved = recordCaptor.getValue();
            assertThat(saved.getType()).isEqualTo(updateDtoBoth.type());
            assertThat(saved.getDescription()).isEqualTo(updateDtoBoth.description());
            then(recordMapper).should().toViewDto(saved);
            assertThat(saved.getCreator()).isEqualTo(owner);
        }

        @Test
        @DisplayName("should update record successfully when Staff updates record from same clinic staff")
        void update_Success_StaffUpdatesSameClinicStaff() {
            // Arrange
            RecordUpdateDto updateDtoWithChanges = new RecordUpdateDto(RecordType.ANNUAL_CHECK, "Updated Staff Description");
            given(entityFinderHelper.findRecordByIdOrFail(recordStaffRecId)).willReturn(recordToUpdateStaff);
            given(entityFinderHelper.findUserOrFail(requesterAdminSameClinicId)).willReturn(adminSameClinic);
            given(recordRepository.save(any(Record.class))).willAnswer(inv -> inv.getArgument(0));

            RecordViewDto expectedDto = new RecordViewDto(
                    recordStaffRecId,
                    updateDtoWithChanges.type(),
                    updateDtoWithChanges.description(),
                    null, recordToUpdateStaff.getCreatedAt(), null, null,
                    null, null, null, null, null, null, null,
                    null);

            given(recordMapper.toViewDto(any(Record.class))).willReturn(expectedDto);

            // Act
            RecordViewDto result = recordService.updateUnsignedRecord(recordStaffRecId, updateDtoWithChanges, requesterAdminSameClinicId);

            // Assert
            assertThat(result).isEqualTo(expectedDto);
            then(recordRepository).should().save(recordCaptor.capture());
            Record saved = recordCaptor.getValue();
            assertThat(saved.getType()).isEqualTo(updateDtoWithChanges.type());
            assertThat(saved.getDescription()).isEqualTo(updateDtoWithChanges.description());
            then(recordMapper).should().toViewDto(saved);
            assertThat(saved.getCreator()).isEqualTo(vet);
        }

        @Test
        @DisplayName("should throw AccessDeniedException if Owner tries to update Staff record")
        void update_Failure_OwnerUpdatesStaffRecord() {
            // Arrange
            given(entityFinderHelper.findRecordByIdOrFail(recordStaffRecId)).willReturn(recordToUpdateStaff);
            given(entityFinderHelper.findUserOrFail(ownerUserId)).willReturn(owner);
            doThrow(new AccessDeniedException("User " + ownerUserId + " is not authorized to update record " + recordStaffRecId + "."))
                    .when(authorizationHelper).verifyUserAuthorizationForUnsignedRecordUpdate(owner, recordToUpdateStaff);

            // Act & Assert
            assertThatThrownBy(() -> recordService.updateUnsignedRecord(recordStaffRecId, updateDtoDesc, ownerUserId))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("is not authorized to update record " + recordStaffRecId);

            then(entityFinderHelper).should().findRecordByIdOrFail(recordStaffRecId);
            then(entityFinderHelper).should().findUserOrFail(ownerUserId);
            then(authorizationHelper).should().verifyUserAuthorizationForUnsignedRecordUpdate(owner, recordToUpdateStaff);
            then(recordRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("should throw AccessDeniedException if Staff tries to update Owner record")
        void update_Failure_StaffUpdatesOwnerRecord() {
            // Arrange
            given(entityFinderHelper.findRecordByIdOrFail(recordOwnerRecId)).willReturn(recordToUpdateOwner);
            given(entityFinderHelper.findUserOrFail(requesterAdminSameClinicId)).willReturn(adminSameClinic);
            doThrow(new AccessDeniedException("User " + requesterAdminSameClinicId + " is not authorized to update record " + recordOwnerRecId + "."))
                    .when(authorizationHelper).verifyUserAuthorizationForUnsignedRecordUpdate(adminSameClinic, recordToUpdateOwner);

            // Act & Assert
            assertThatThrownBy(() -> recordService.updateUnsignedRecord(recordOwnerRecId, updateDtoDesc, requesterAdminSameClinicId))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("is not authorized to update record " + recordOwnerRecId);

            then(entityFinderHelper).should().findRecordByIdOrFail(recordOwnerRecId);
            then(entityFinderHelper).should().findUserOrFail(requesterAdminSameClinicId);
            then(authorizationHelper).should().verifyUserAuthorizationForUnsignedRecordUpdate(adminSameClinic, recordToUpdateOwner);
            then(recordRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("should throw AccessDeniedException if Staff tries to update record from DIFFERENT clinic staff")
        void update_Failure_StaffUpdatesDifferentClinicStaff() {
            // Arrange
            given(entityFinderHelper.findRecordByIdOrFail(recordStaffRecId)).willReturn(recordToUpdateStaff);
            given(entityFinderHelper.findUserOrFail(requesterStaffOtherClinicId)).willReturn(requesterStaffOtherClinic);
            doThrow(new AccessDeniedException("User " + requesterStaffOtherClinicId + " is not authorized to update record " + recordStaffRecId + "."))
                    .when(authorizationHelper).verifyUserAuthorizationForUnsignedRecordUpdate(requesterStaffOtherClinic, recordToUpdateStaff);

            // Act & Assert
            assertThatThrownBy(() -> recordService.updateUnsignedRecord(recordStaffRecId, updateDtoDesc, requesterStaffOtherClinicId))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("is not authorized to update record " + recordStaffRecId);

            then(entityFinderHelper).should().findRecordByIdOrFail(recordStaffRecId);
            then(entityFinderHelper).should().findUserOrFail(requesterStaffOtherClinicId);
            then(authorizationHelper).should().verifyUserAuthorizationForUnsignedRecordUpdate(requesterStaffOtherClinic, recordToUpdateStaff); // Verificar llamada al helper
            then(recordRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("should throw EntityNotFoundException if record not found")
        void update_Failure_RecordNotFound() {
            // Arrange
            given(entityFinderHelper.findRecordByIdOrFail(999L)).willThrow(new EntityNotFoundException(Record.class.getSimpleName(), 999L));

            // Act & Assert
            assertThatThrownBy(() -> recordService.updateUnsignedRecord(999L, updateDtoDesc, recordStaffRecId))
                    .isInstanceOf(EntityNotFoundException.class);

            then(recordRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("should throw EntityNotFoundException if requester not found")
        void update_Failure_RequesterNotFound() {
            // Arrange
            given(entityFinderHelper.findRecordByIdOrFail(recordStaffRecId)).willReturn(recordToUpdateOwner);
            given(entityFinderHelper.findUserOrFail(999L)).willThrow(new EntityNotFoundException(UserEntity.class.getSimpleName(), 999L));

            // Act & Assert
            assertThatThrownBy(() -> recordService.updateUnsignedRecord(recordStaffRecId, updateDtoDesc, 999L))
                    .isInstanceOf(EntityNotFoundException.class);

            then(recordRepository).should(never()).save(any());
        }
    }

    /**
     * --- Tests for deleteRecord ---
     */
    @Nested
    @DisplayName("deleteRecord Tests")
    class DeleteRecordTests {

        private Record recordToDeleteUnsigned;
        private Record recordToDeleteSigned;
        private Record recordToDeleteByOtherStaff;
        private ClinicStaff adminSameClinic;
        private final Long recordUnsignedId = 501L;
        private final Long recordSignedId = 502L;
        private final Long recordByVetId = 503L;
        private final Long creatorVetId = vetId;
        private final Long deleterAdminId = 50L;

        @BeforeEach
        void deleteSetup() {
            // Record created by Owner, unsigned
            recordToDeleteUnsigned = new Record();
            recordToDeleteUnsigned.setId(recordUnsignedId);
            recordToDeleteUnsigned.setPet(pet);
            recordToDeleteUnsigned.setCreator(owner);
            recordToDeleteUnsigned.setVetSignature(null);
            recordToDeleteUnsigned.setType(RecordType.OTHER);

            // Record created by Vet, signed
            recordToDeleteSigned = new Record();
            recordToDeleteSigned.setId(recordSignedId);
            recordToDeleteSigned.setPet(pet);
            recordToDeleteSigned.setCreator(vet);
            recordToDeleteSigned.setVetSignature("SIGNATURE_XYZ");
            recordToDeleteSigned.setType(RecordType.ANNUAL_CHECK);

            Clinic clinic = vet.getClinic();
            if (clinic == null) {
                clinic = Clinic.builder().build(); clinic.setId(1L);
                vet.setClinic(clinic);
            }
            recordToDeleteByOtherStaff = new Record();
            recordToDeleteByOtherStaff.setId(recordByVetId);
            recordToDeleteByOtherStaff.setPet(pet);
            recordToDeleteByOtherStaff.setCreator(vet);
            recordToDeleteByOtherStaff.setVetSignature(null);
            recordToDeleteByOtherStaff.setType(RecordType.ILLNESS);

            adminSameClinic = new ClinicStaff();
            adminSameClinic.setId(deleterAdminId);
            adminSameClinic.setClinic(clinic);
            adminSameClinic.setRoles(Set.of(RoleEntity.builder().roleEnum(RoleEnum.ADMIN).build()));
        }

        /**
         * Test successful deletion when the requester is the creator and the record is unsigned.
         */
        @Test
        @DisplayName("should delete UNSIGNED record successfully when requester is creator (Owner)")
        void deleteUnsigned_Success_ByOwnerCreator() {
            Long creatorOwnerId = ownerId;
            // Arrange
            given(entityFinderHelper.findRecordByIdOrFail(recordUnsignedId)).willReturn(recordToDeleteUnsigned);
            given(entityFinderHelper.findUserOrFail(creatorOwnerId)).willReturn(owner);
            doNothing().when(recordRepository).delete(recordToDeleteUnsigned);

            // Act
            recordService.deleteRecord(recordUnsignedId, creatorOwnerId);

            // Assert
            then(entityFinderHelper).should().findRecordByIdOrFail(recordUnsignedId);
            then(entityFinderHelper).should().findUserOrFail(creatorOwnerId);
            then(recordRepository).should().delete(recordToDeleteUnsigned);
        }

        /**
         * Test successful deletion when the requester is Admin from the same clinic as the staff creator,
         * and the record is unsigned.
         */
        @Test
        @DisplayName("should delete UNSIGNED record successfully when requester is Admin from same clinic")
        void deleteUnsigned_Success_ByAdminSameClinic() {
            // Arrange
            given(entityFinderHelper.findRecordByIdOrFail(recordByVetId)).willReturn(recordToDeleteByOtherStaff);
            given(entityFinderHelper.findUserOrFail(deleterAdminId)).willReturn(adminSameClinic);
            doNothing().when(recordRepository).delete(recordToDeleteByOtherStaff);

            // Act
            recordService.deleteRecord(recordByVetId, deleterAdminId);

            // Assert
            then(entityFinderHelper).should().findRecordByIdOrFail(recordByVetId);
            then(entityFinderHelper).should().findUserOrFail(deleterAdminId);
            then(recordRepository).should().delete(recordToDeleteByOtherStaff);
        }

        @Test
        @DisplayName("should throw AccessDeniedException if unauthorized user tries to delete UNSIGNED record")
        void deleteUnsigned_Failure_Unauthorized() {
            Long unauthorizedUserId = 99L;
            UserEntity unauthorizedUser = new Owner(); unauthorizedUser.setId(unauthorizedUserId);

            given(entityFinderHelper.findRecordByIdOrFail(recordUnsignedId)).willReturn(recordToDeleteUnsigned);
            given(entityFinderHelper.findUserOrFail(unauthorizedUserId)).willReturn(unauthorizedUser);

            doThrow(new AccessDeniedException("User " + unauthorizedUserId + " is not authorized to delete unsigned record " + recordUnsignedId + "."))
                    .when(authorizationHelper).verifyUserAuthorizationForRecordDeletion(unauthorizedUser, recordToDeleteUnsigned);

            // Act & Assert
            assertThatThrownBy(() -> recordService.deleteRecord(recordUnsignedId, unauthorizedUserId))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("User " + unauthorizedUserId + " is not authorized to delete unsigned record " + recordUnsignedId + ".");

            then(entityFinderHelper).should().findRecordByIdOrFail(recordUnsignedId);
            then(entityFinderHelper).should().findUserOrFail(unauthorizedUserId);
            then(authorizationHelper).should().verifyUserAuthorizationForRecordDeletion(unauthorizedUser, recordToDeleteUnsigned);
            then(recordRepository).should(never()).delete(any(Record.class));
        }

        @Test
        @DisplayName("should throw AccessDeniedException if Admin from different clinic tries to delete UNSIGNED record")
        void deleteUnsigned_Failure_AdminDifferentClinic() {
            // Arrange
            Clinic otherClinic = new Clinic(); otherClinic.setId(99L);
            ClinicStaff adminOtherClinic = new ClinicStaff(); adminOtherClinic.setId(88L); adminOtherClinic.setClinic(otherClinic); adminOtherClinic.setRoles(Set.of(adminRole));

            given(entityFinderHelper.findRecordByIdOrFail(recordByVetId)).willReturn(recordToDeleteByOtherStaff);
            given(entityFinderHelper.findUserOrFail(88L)).willReturn(adminOtherClinic);
            doThrow(new AccessDeniedException("User " + 88L + " is not authorized to delete unsigned record " + recordByVetId + "."))
                    .when(authorizationHelper).verifyUserAuthorizationForRecordDeletion(adminOtherClinic, recordToDeleteByOtherStaff);

            // Act & Assert
            assertThatThrownBy(() -> recordService.deleteRecord(recordByVetId, 88L))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("is not authorized to delete unsigned record " + recordByVetId);

            then(entityFinderHelper).should().findRecordByIdOrFail(recordByVetId);
            then(entityFinderHelper).should().findUserOrFail(88L);
            then(authorizationHelper).should().verifyUserAuthorizationForRecordDeletion(adminOtherClinic, recordToDeleteByOtherStaff);
            then(recordRepository).should(never()).delete(any(Record.class));
        }


        @Test
        @DisplayName("should delete SIGNED record successfully when requester is signing Vet AND record is NOT immutable")
        void deleteSigned_Success_SigningVet_NotImmutable() {
            // Arrange
            recordToDeleteSigned.setImmutable(false);
            given(entityFinderHelper.findRecordByIdOrFail(recordSignedId)).willReturn(recordToDeleteSigned);
            given(entityFinderHelper.findUserOrFail(creatorVetId)).willReturn(vet);
            doNothing().when(authorizationHelper).verifyUserAuthorizationForRecordDeletion(vet, recordToDeleteSigned);
            doNothing().when(recordRepository).delete(recordToDeleteSigned);

            // Act
            recordService.deleteRecord(recordSignedId, creatorVetId);

            // Assert
            then(entityFinderHelper).should().findRecordByIdOrFail(recordSignedId);
            then(entityFinderHelper).should().findUserOrFail(creatorVetId);
            then(authorizationHelper).should().verifyUserAuthorizationForRecordDeletion(vet, recordToDeleteSigned);
            then(recordRepository).should().delete(recordToDeleteSigned);
        }

        @Test
        @DisplayName("should throw RecordImmutableException when trying to delete SIGNED and IMMUTABLE record (even by signing Vet)")
        void deleteSigned_Failure_Immutable() {
            // Arrange
            recordToDeleteSigned.setImmutable(true); // *** Marcar como inmutable ***
            given(entityFinderHelper.findRecordByIdOrFail(recordSignedId)).willReturn(recordToDeleteSigned);
            given(entityFinderHelper.findUserOrFail(creatorVetId)).willReturn(vet);
            doThrow(new RecordImmutableException(recordSignedId))
                    .when(authorizationHelper).verifyUserAuthorizationForRecordDeletion(vet, recordToDeleteSigned);

            // Act & Assert
            assertThatThrownBy(() -> recordService.deleteRecord(recordSignedId, creatorVetId))
                    .isInstanceOf(RecordImmutableException.class)
                    .hasMessageContaining("Record " + recordSignedId + " is immutable and cannot be modified or deleted");

            then(entityFinderHelper).should().findRecordByIdOrFail(recordSignedId);
            then(entityFinderHelper).should().findUserOrFail(creatorVetId);
            then(authorizationHelper).should().verifyUserAuthorizationForRecordDeletion(vet, recordToDeleteSigned);
            then(recordRepository).should(never()).delete(any(Record.class));
        }

        @Test
        @DisplayName("should throw AccessDeniedException when NON-signing user tries to delete SIGNED record (even if not immutable)")
        void deleteSigned_Failure_NotSigningVet() {
            // Arrange
            recordToDeleteSigned.setImmutable(false);
            Long nonSigningUserId = ownerId;

            given(entityFinderHelper.findRecordByIdOrFail(recordSignedId)).willReturn(recordToDeleteSigned);
            given(entityFinderHelper.findUserOrFail(nonSigningUserId)).willReturn(owner);
            doThrow(new AccessDeniedException("Only the signing veterinarian can delete a signed record..."))
                    .when(authorizationHelper).verifyUserAuthorizationForRecordDeletion(owner, recordToDeleteSigned);

            // Act & Assert
            assertThatThrownBy(() -> recordService.deleteRecord(recordSignedId, nonSigningUserId))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("Only the signing veterinarian can delete a signed record");

            then(entityFinderHelper).should().findRecordByIdOrFail(recordSignedId);
            then(entityFinderHelper).should().findUserOrFail(nonSigningUserId);
            then(authorizationHelper).should().verifyUserAuthorizationForRecordDeletion(owner, recordToDeleteSigned);
            then(recordRepository).should(never()).delete(any(Record.class));
        }

        @Test
        @DisplayName("should throw EntityNotFoundException if record not found")
        void delete_Failure_RecordNotFound() {
            // Arrange
            given(entityFinderHelper.findRecordByIdOrFail(999L))
                    .willThrow(new EntityNotFoundException(Record.class.getSimpleName(), 999L));

            // Act & Assert
            assertThatThrownBy(() -> recordService.deleteRecord(999L, ownerId))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Record not found with id: 999");

            then(entityFinderHelper).should(never()).findUserOrFail(anyLong());
            then(recordRepository).should(never()).delete(any(Record.class));
        }

        @Test
        @DisplayName("should throw EntityNotFoundException if requester user not found")
        void delete_Failure_RequesterNotFound() {
            // Arrange
            Long nonExistentUserId = 777L;
            given(entityFinderHelper.findRecordByIdOrFail(recordUnsignedId)).willReturn(recordToDeleteUnsigned);
            given(entityFinderHelper.findUserOrFail(nonExistentUserId))
                    .willThrow(new EntityNotFoundException(UserEntity.class.getSimpleName(), nonExistentUserId));

            // Act & Assert
            assertThatThrownBy(() -> recordService.deleteRecord(recordUnsignedId, nonExistentUserId))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("UserEntity not found with id: " + nonExistentUserId);

            then(recordRepository).should(never()).delete(any(Record.class));
        }
    }

    /**
     * --- Tests for generateTemporaryAccessToken ---
     */
    @Nested
    @DisplayName("generateTemporaryAccessToken Tests")
    class GenerateTemporaryAccessTokenTests{

        private TemporaryAccessRequestDto validRequestDtoShort;
        private TemporaryAccessRequestDto validRequestDtoLong;
        private TemporaryAccessRequestDto invalidDurationDto;
        private final Long petIdForToken = petId;
        private final Long requesterOwnerId = ownerId;
        private final Long unauthorizedUserId = vetId;

        @BeforeEach
        void tempTokenSetup() {
            validRequestDtoShort = new TemporaryAccessRequestDto("PT1H"); // 1 Hour
            validRequestDtoLong = new TemporaryAccessRequestDto("P10D"); // 10 Days (will be capped)
            invalidDurationDto = new TemporaryAccessRequestDto("INVALID");
            // Ensure the pet has the correct owner set from the main setup
            pet.setOwner(owner);
        }

        @Test
        @DisplayName("should generate token successfully when requested by Owner with valid duration")
        void generateToken_Success_ValidDuration() {
            // Arrange
            String expectedToken = "temp.jwt.token.1h";
            Duration expectedDuration = Duration.ofHours(1);
            given(entityFinderHelper.findPetByIdOrFail(petIdForToken)).willReturn(pet);
            given(jwtUtils.createTemporaryRecordAccessToken(petIdForToken, expectedDuration)).willReturn(expectedToken);

            // Act
            TemporaryAccessTokenDto result = recordService.generateTemporaryAccessToken(petIdForToken, validRequestDtoShort, requesterOwnerId);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.token()).isEqualTo(expectedToken);
            then(entityFinderHelper).should().findPetByIdOrFail(petIdForToken);
            then(jwtUtils).should().createTemporaryRecordAccessToken(petIdForToken, expectedDuration);
        }

        @Test
        @DisplayName("should cap duration at 7 days if requested duration is longer")
        void generateToken_Success_DurationCapped() {
            // Arrange
            String expectedToken = "temp.jwt.token.7d";
            Duration cappedDuration = Duration.ofDays(7); // Max duration
            given(entityFinderHelper.findPetByIdOrFail(petIdForToken)).willReturn(pet);
            given(jwtUtils.createTemporaryRecordAccessToken(petIdForToken, cappedDuration)).willReturn(expectedToken);

            // Act
            TemporaryAccessTokenDto result = recordService.generateTemporaryAccessToken(petIdForToken, validRequestDtoLong, requesterOwnerId);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.token()).isEqualTo(expectedToken);
            then(entityFinderHelper).should().findPetByIdOrFail(petIdForToken);
            // Verify it was called with the CAPPED duration
            then(jwtUtils).should().createTemporaryRecordAccessToken(petIdForToken, cappedDuration);
        }


        @Test
        @DisplayName("should throw AccessDeniedException if requester is not the Owner")
        void generateToken_Failure_NotOwner() {
            // Arrange
            given(entityFinderHelper.findPetByIdOrFail(petIdForToken)).willReturn(pet);

            // Act & Assert
            assertThatThrownBy(() -> recordService.generateTemporaryAccessToken(petIdForToken, validRequestDtoShort, unauthorizedUserId))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("Only the pet owner can generate temporary access tokens.");

            then(entityFinderHelper).should().findPetByIdOrFail(petIdForToken);
            then(jwtUtils).should(never()).createTemporaryRecordAccessToken(anyLong(), any());
        }

        @Test
        @DisplayName("should throw EntityNotFoundException if pet not found")
        void generateToken_Failure_PetNotFound() {
            // Arrange
            given(entityFinderHelper.findPetByIdOrFail(999L))
                    .willThrow(new EntityNotFoundException(Pet.class.getSimpleName(), 999L));

            // Act & Assert
            assertThatThrownBy(() -> recordService.generateTemporaryAccessToken(999L, validRequestDtoShort, requesterOwnerId))
                    .isInstanceOf(EntityNotFoundException.class);

            then(jwtUtils).should(never()).createTemporaryRecordAccessToken(anyLong(), any());
        }

        @Test
        @DisplayName("should throw IllegalArgumentException if duration string is invalid")
        void generateToken_Failure_InvalidDuration() {
            // Arrange
            given(entityFinderHelper.findPetByIdOrFail(petIdForToken)).willReturn(pet);

            // Act & Assert
            assertThatThrownBy(() -> recordService.generateTemporaryAccessToken(petIdForToken, invalidDurationDto, requesterOwnerId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid duration format");

            then(entityFinderHelper).should().findPetByIdOrFail(petIdForToken);
            then(jwtUtils).should(never()).createTemporaryRecordAccessToken(anyLong(), any());
        }

        @Test
        @DisplayName("should throw IllegalArgumentException if duration is zero or negative")
        void generateToken_Failure_ZeroOrNegativeDuration() {
            // Arrange
            TemporaryAccessRequestDto zeroDurationDto = new TemporaryAccessRequestDto("PT0S");
            TemporaryAccessRequestDto negativeDurationDto = new TemporaryAccessRequestDto("PT-1S");
            given(entityFinderHelper.findPetByIdOrFail(petIdForToken)).willReturn(pet);

            // Act & Assert for Zero
            assertThatThrownBy(() -> recordService.generateTemporaryAccessToken(petIdForToken, zeroDurationDto, requesterOwnerId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Duration must be positive.");

            // Act & Assert for Negative
            assertThatThrownBy(() -> recordService.generateTemporaryAccessToken(petIdForToken, negativeDurationDto, requesterOwnerId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Duration must be positive.");

            then(entityFinderHelper).should(times(2)).findPetByIdOrFail(petIdForToken);
            then(jwtUtils).should(never()).createTemporaryRecordAccessToken(anyLong(), any());
        }

    }
}
