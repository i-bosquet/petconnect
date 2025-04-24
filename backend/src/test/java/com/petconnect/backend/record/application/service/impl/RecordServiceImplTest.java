package com.petconnect.backend.record.application.service.impl;

import com.petconnect.backend.common.helper.AuthorizationHelper;
import com.petconnect.backend.common.helper.EntityFinderHelper;
import com.petconnect.backend.common.helper.RecordHelper;
import com.petconnect.backend.common.helper.ValidateHelper;
import com.petconnect.backend.common.service.impl.SigningServiceImpl;
import com.petconnect.backend.exception.EntityNotFoundException;
import com.petconnect.backend.pet.domain.model.Pet;
import com.petconnect.backend.pet.domain.model.Breed;
import com.petconnect.backend.pet.domain.model.Specie;
import com.petconnect.backend.record.application.dto.RecordCreateDto;
import com.petconnect.backend.record.application.dto.RecordViewDto;
import com.petconnect.backend.record.application.dto.VaccineCreateDto;
import com.petconnect.backend.record.application.mapper.RecordMapper;
import com.petconnect.backend.record.application.mapper.VaccineMapper;
import com.petconnect.backend.record.domain.model.Record;
import com.petconnect.backend.record.domain.model.RecordType;
import com.petconnect.backend.record.domain.model.Vaccine;
import com.petconnect.backend.record.domain.repository.RecordRepository;
import com.petconnect.backend.user.application.dto.UserProfileDto;
import com.petconnect.backend.user.application.mapper.UserMapper;
import com.petconnect.backend.user.domain.model.*;
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
    @Mock private SigningServiceImpl signingServiceImpl;

    @InjectMocks
    private RecordServiceImpl recordService;

    @Captor private ArgumentCaptor<Record> recordCaptor;

    private Pet pet;
    private Owner owner;
    private Vet vet;
    private Record record1, record2;
    private RecordViewDto recordDto1, recordDto2;
    private final Long petId = 1L;
    private final Long ownerId = 10L;
    private final Long vetId = 11L;
    private final Long recordId1 = 101L;
    private RoleEntity adminRole;

    @BeforeEach
    void setUp() {


        Long recordId2 = 102L;
        adminRole = RoleEntity.builder().roleEnum(RoleEnum.ADMIN).build(); adminRole.setId(3L);

        owner = new Owner(); owner.setId(ownerId);
        owner.setUsername("testowner");
        vet = new Vet(); vet.setId(vetId);
        vet.setUsername("testvet");
        Breed breed = Breed.builder().id(5L).name("Siamese").specie(Specie.CAT).build();

        pet = new Pet();
        pet.setId(petId);
        pet.setOwner(owner);
        pet.setBreed(breed);

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

        UserProfileDto ownerDto = new UserProfileDto(ownerId, "testowner", null, Set.of("OWNER"), null);
        UserProfileDto vetDto = new UserProfileDto(vetId, "testvet", null, Set.of("VET"), null);
        recordDto1 = new RecordViewDto(recordId1, RecordType.OTHER, "Observation by owner", null, record1.getCreatedAt(), ownerDto, null);
        recordDto2 = new RecordViewDto(recordId2, RecordType.ANNUAL_CHECK, "Annual checkup results", "SIGNED_BY_VET_XYZ", record2.getCreatedAt(), vetDto, null);

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
        private final Long petIdForCreate = petId;

        @BeforeEach
        void createSetup() {
            ownerRecordDto = new RecordCreateDto( petIdForCreate,RecordType.OTHER, "Felt warm yesterday", null);

            vaccineDetailsDto = new VaccineCreateDto("RabiesVac", 1, "LabX", "Batch123");
            vetVaccineDto = new RecordCreateDto(petIdForCreate,RecordType.VACCINE, "Rabies vaccination administered", vaccineDetailsDto);

            Record newRecordFromVet;
            Record.builder().pet(pet).creator(owner).type(ownerRecordDto.type())
                    .description(ownerRecordDto.description()).build();
            newRecordFromVet = Record.builder().pet(pet).creator(vet).type(vetVaccineDto.type())
                    .description(vetVaccineDto.description()).build();
            newVaccineEntity = Vaccine.builder().name(vaccineDetailsDto.name()).validity(vaccineDetailsDto.validity())
                    .laboratory(vaccineDetailsDto.laboratory()).batchNumber(vaccineDetailsDto.batchNumber()).build();
            newRecordFromVet.setVaccineDetails(newVaccineEntity);

            expectedOwnerRecordViewDto = new RecordViewDto(200L, ownerRecordDto.type(), ownerRecordDto.description(), null, LocalDateTime.now(), userMapper.mapToBaseProfileDTO(owner), null);
            expectedVetVaccineViewDto = new RecordViewDto(201L, vetVaccineDto.type(), vetVaccineDto.description(), "TEMP_SIGNATURE", LocalDateTime.now(), userMapper.mapToBaseProfileDTO(vet), vaccineMapper.toViewDto(newVaccineEntity)); // Assume signed
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
            given(signingServiceImpl.generateVetSignature(vet, expectedDataToSign)).willReturn(expectedSignature);

            given(recordRepository.save(any(Record.class))).willAnswer(inv -> {
                Record recordEntity = inv.getArgument(0);
                recordEntity.setId(201L);
                if (recordEntity.getVaccine() != null) recordEntity.getVaccine().setRecordEntity(recordEntity);
                return recordEntity;
            });

            expectedVetVaccineViewDto = new RecordViewDto(201L, vetVaccineDto.type(), vetVaccineDto.description(),
                    expectedSignature,
                    LocalDateTime.now(), userMapper.mapToBaseProfileDTO(vet),
                    vaccineMapper.toViewDto(newVaccineEntity));
            given(recordMapper.toViewDto(any(Record.class))).willReturn(expectedVetVaccineViewDto);


            // Act
            RecordViewDto result = recordService.createRecord( vetVaccineDto, vetId);

            // Assert
            assertThat(result).isNotNull().isEqualTo(expectedVetVaccineViewDto);
            assertThat(result.vetSignature()).isEqualTo(expectedSignature);

            then(entityFinderHelper).should().findPetByIdOrFail(petId);
            then(entityFinderHelper).should().findUserOrFail(vetId);
            then(authorizationHelper).should().verifyUserAuthorizationForPet(vetId, pet, "create record for");
            then(validateHelper).should().validateRecordCreationDto(vetVaccineDto);
            then(vaccineMapper).should().fromCreateDto(vaccineDetailsDto);
            then(recordHelper).should().buildSignableData(pet, vet, vetVaccineDto);
            then(signingServiceImpl).should().generateVetSignature(vet, expectedDataToSign);
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
            RecordCreateDto invalidDto = new RecordCreateDto(petIdForCreate,RecordType.VACCINE, "Forgot details", null); // Vaccine DTO null
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
            RecordCreateDto invalidDto = new RecordCreateDto(petIdForCreate,RecordType.ILLNESS, "Flu", vaccineDetailsDto);
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
     * --- Tests for deleteUnsignedRecord ---
     */
    @Nested
    @DisplayName("deleteUnsignedRecord Tests")
    class DeleteUnsignedRecordTests {

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
        @DisplayName("should delete record successfully when requester is creator and record unsigned")
        void delete_Success_ByCreator() {
            Long creatorOwnerId = ownerId;
            // Arrange
            given(entityFinderHelper.findRecordByIdOrFail(recordUnsignedId)).willReturn(recordToDeleteUnsigned);
            given(entityFinderHelper.findUserOrFail(creatorOwnerId)).willReturn(owner);
            doNothing().when(recordRepository).delete(recordToDeleteUnsigned);

            // Act
            recordService.deleteUnsignedRecord(recordUnsignedId, creatorOwnerId);

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
        @DisplayName("should delete record successfully when requester is Admin from same clinic and record unsigned")
        void delete_Success_ByAdminSameClinic() {
            // Arrange
            given(entityFinderHelper.findRecordByIdOrFail(recordByVetId)).willReturn(recordToDeleteByOtherStaff);
            given(entityFinderHelper.findUserOrFail(deleterAdminId)).willReturn(adminSameClinic);
            doNothing().when(recordRepository).delete(recordToDeleteByOtherStaff);

            // Act
            recordService.deleteUnsignedRecord(recordByVetId, deleterAdminId);

            // Assert
            then(entityFinderHelper).should().findRecordByIdOrFail(recordByVetId);
            then(entityFinderHelper).should().findUserOrFail(deleterAdminId);
            then(recordRepository).should().delete(recordToDeleteByOtherStaff);
        }

        /**
         * Test failure when attempting to delete a signed record.
         */
        @Test
        @DisplayName("should throw IllegalStateException when attempting to delete signed record")
        void delete_Failure_RecordSigned() {
            // Arrange
            given(entityFinderHelper.findRecordByIdOrFail(recordSignedId)).willReturn(recordToDeleteSigned);
            given(entityFinderHelper.findUserOrFail(creatorVetId)).willReturn(vet);

            // Act & Assert
            assertThatThrownBy(() -> recordService.deleteUnsignedRecord(recordSignedId, creatorVetId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot delete record " + recordSignedId + " because it has been signed");

            then(entityFinderHelper).should().findRecordByIdOrFail(recordSignedId);
            then(entityFinderHelper).should().findUserOrFail(creatorVetId);
            then(recordRepository).should(never()).delete(any(Record.class));
        }

        /**
         * Test failure when the requester is not the creator nor an authorized Admin.
         */
        @Test
        @DisplayName("should throw AccessDeniedException if requester not creator or authorized Admin")
        void delete_Failure_Unauthorized() {
            // Arrange
            Long unauthorizedUserId = 99L;
            UserEntity unauthorizedUser = new Owner(); unauthorizedUser.setId(unauthorizedUserId);

            given(entityFinderHelper.findRecordByIdOrFail(recordUnsignedId)).willReturn(recordToDeleteUnsigned);
            given(entityFinderHelper.findUserOrFail(unauthorizedUserId)).willReturn(unauthorizedUser);

            // Act & Assert
            assertThatThrownBy(() -> recordService.deleteUnsignedRecord(recordUnsignedId, unauthorizedUserId))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("is not authorized to delete record " + recordUnsignedId);

            then(entityFinderHelper).should().findRecordByIdOrFail(recordUnsignedId);
            then(entityFinderHelper).should().findUserOrFail(unauthorizedUserId);
            then(recordRepository).should(never()).delete(any(Record.class));
        }

        /**
         * Test failure when Admin tries to delete record created by staff from a DIFFERENT clinic.
         */
        @Test
        @DisplayName("should throw AccessDeniedException if Admin from different clinic")
        void delete_Failure_AdminDifferentClinic() {
            // Arrange
            Clinic otherClinic = new Clinic();
            otherClinic.setId(99L);
            otherClinic.setName("Other Clinic Test");

            ClinicStaff adminOtherClinic = new ClinicStaff();
            adminOtherClinic.setId(88L);
            adminOtherClinic.setClinic(otherClinic);
            adminOtherClinic.setRoles(Set.of(adminRole));

            given(entityFinderHelper.findRecordByIdOrFail(recordByVetId)).willReturn(recordToDeleteByOtherStaff);
            given(entityFinderHelper.findUserOrFail(88L)).willReturn(adminOtherClinic);

            // Act & Assert
            assertThatThrownBy(() -> recordService.deleteUnsignedRecord(recordByVetId, 88L))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("is not authorized to delete record " + recordByVetId);

            then(recordRepository).should(never()).delete(any(Record.class));
        }

        /**
         * Test failure when the record ID does not exist.
         */
        @Test
        @DisplayName("should throw EntityNotFoundException if record not found")
        void delete_Failure_RecordNotFound() {
            // Arrange
            given(entityFinderHelper.findRecordByIdOrFail(999L))
                    .willThrow(new EntityNotFoundException(Record.class.getSimpleName(), 999L));

            // Act & Assert
            assertThatThrownBy(() -> recordService.deleteUnsignedRecord(999L, ownerId))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Record not found with id: 999");

            then(entityFinderHelper).should(never()).findUserOrFail(anyLong());
            then(recordRepository).should(never()).delete(any(Record.class));
        }

        /**
         * Test failure when the requester user ID does not exist.
         */
        @Test
        @DisplayName("should throw EntityNotFoundException if requester user not found")
        void delete_Failure_RequesterNotFound() {
            // Arrange
            Long nonExistentUserId = 777L;
            given(entityFinderHelper.findRecordByIdOrFail(recordUnsignedId)).willReturn(recordToDeleteUnsigned);
            given(entityFinderHelper.findUserOrFail(nonExistentUserId))
                    .willThrow(new EntityNotFoundException(UserEntity.class.getSimpleName(), nonExistentUserId));

            // Act & Assert
            assertThatThrownBy(() -> recordService.deleteUnsignedRecord(recordUnsignedId, nonExistentUserId))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("UserEntity not found with id: " + nonExistentUserId);

            then(recordRepository).should(never()).delete(any(Record.class));
        }
    }
}
