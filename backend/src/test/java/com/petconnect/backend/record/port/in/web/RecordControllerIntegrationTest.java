package com.petconnect.backend.record.port.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petconnect.backend.pet.application.dto.PetRegistrationDto;
import com.petconnect.backend.pet.domain.model.Gender;
import com.petconnect.backend.pet.domain.model.Specie;
import com.petconnect.backend.record.application.dto.*;
import com.petconnect.backend.record.domain.model.RecordType;
import com.petconnect.backend.record.domain.repository.RecordRepository;
import com.petconnect.backend.user.application.dto.*;
import com.petconnect.backend.user.domain.model.RoleEnum;
import com.petconnect.backend.user.domain.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;

import static com.petconnect.backend.util.IntegrationTestUtils.extractPetIdFromResult;
import static com.petconnect.backend.util.IntegrationTestUtils.obtainJwtToken;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


/**
 * Integration tests for {@link RecordController}.
 * Uses PostgresSQL (Docker), security filters, transactional rollback.
 * Verifies creating, retrieving, and deleting medical records via API endpoints.
 *
 * @author ibosquet
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class RecordControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private RecordRepository recordRepository;
    @Autowired private EntityManager entityManager;

    // --- Tokens & IDs
    private String ownerToken;
    private String vetToken;
    private String adminToken;
    private String otherOwnerToken;
    private String adminBarcelonaToken;

    private Long ownerId;
    private Long vetId;
    private Long petIdOwned;
    private Long petIdOther;

    /**
     * Set up users and pets for record tests.
     */
    @BeforeEach
    void setUp() throws Exception {

        adminToken = obtainJwtToken(mockMvc, objectMapper, new AuthLoginRequestDto("admin_london", "password123"));
        adminBarcelonaToken = obtainJwtToken(mockMvc, objectMapper, new AuthLoginRequestDto("admin_barcelona", "password123")); // Usado en tests de delete
        Long mixedBreedCatId = 45L;

        String ownerUsername = "rec_owner_" + System.currentTimeMillis();
        OwnerRegistrationDto ownerReg = new OwnerRegistrationDto(ownerUsername, ownerUsername + "@test.com", "password123", "111");
        userRepository.findByUsername(ownerUsername).ifPresent(userRepository::delete);
        entityManager.flush();
        MvcResult ownerRegResult = mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(ownerReg))).andExpect(status().isCreated()).andReturn();
        OwnerProfileDto ownerDto = objectMapper.readValue(ownerRegResult.getResponse().getContentAsString(), OwnerProfileDto.class);
        ownerId = ownerDto.id();
        ownerToken = obtainJwtToken(mockMvc, objectMapper, new AuthLoginRequestDto(ownerReg.username(), ownerReg.password()));

        String otherOwnerUsername = "rec_other_owner_" + System.currentTimeMillis();
        OwnerRegistrationDto otherOwnerReg = new OwnerRegistrationDto(otherOwnerUsername, otherOwnerUsername + "@test.com", "password123", "222");
        userRepository.findByUsername(otherOwnerUsername).ifPresent(userRepository::delete);
        entityManager.flush();

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(otherOwnerReg))).andExpect(status().isCreated());
        otherOwnerToken = obtainJwtToken(mockMvc, objectMapper, new AuthLoginRequestDto(otherOwnerReg.username(), otherOwnerReg.password()));

        String vetUsername = "rec_vet_" + System.currentTimeMillis();
        ClinicStaffCreationDto vetReg = new ClinicStaffCreationDto(vetUsername, vetUsername + "@test.com", "password123", "Record", "Vet", RoleEnum.VET, "VETREC" + System.currentTimeMillis(), "VETKEYREC" + System.currentTimeMillis());
        userRepository.findByUsername(vetUsername).ifPresent(userRepository::delete);
        entityManager.flush();
        MvcResult vetRegResult = mockMvc.perform(post("/api/staff").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(vetReg))).andExpect(status().isCreated()).andReturn();
        ClinicStaffProfileDto vetDto = objectMapper.readValue(vetRegResult.getResponse().getContentAsString(), ClinicStaffProfileDto.class);
        vetId = vetDto.id();
        vetToken = obtainJwtToken(mockMvc, objectMapper, new AuthLoginRequestDto(vetReg.username(), vetReg.password()));

        PetRegistrationDto petReg = new PetRegistrationDto("RecordPet", Specie.CAT, LocalDate.now().minusYears(1), mixedBreedCatId, null, "Calico", Gender.FEMALE, null);
        MvcResult petRegRes = mockMvc.perform(post("/api/pets").header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(petReg))).andExpect(status().isCreated()).andReturn();
        petIdOwned = extractPetIdFromResult(objectMapper,petRegRes);

        PetRegistrationDto otherPetReg = new PetRegistrationDto("OtherPet", Specie.DOG, LocalDate.now().minusYears(2), null, null, null, null, null);
        MvcResult otherPetRes = mockMvc.perform(post("/api/pets").header(HttpHeaders.AUTHORIZATION, "Bearer " + otherOwnerToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(otherPetReg))).andExpect(status().isCreated()).andReturn();
        petIdOther = extractPetIdFromResult(objectMapper,otherPetRes);

        assertThat(ownerToken).isNotBlank();
        assertThat(vetToken).isNotBlank();
        assertThat(adminToken).isNotBlank();
        assertThat(otherOwnerToken).isNotBlank();
        assertThat(petIdOwned).isNotNull();
        assertThat(petIdOther).isNotNull();
    }

    /**
     * --- Tests for POST /api/records (Create Record) ---
     */
    @Nested
    @DisplayName("POST /api/records (Create Record)")
    class CreateRecordIntegrationTests {

        private RecordCreateDto ownerCreateDto;
        private RecordCreateDto vetVaccineDto;
        private VaccineCreateDto vaccineDetailsDto;

        @BeforeEach
        void createRecordSetup() throws Exception {
            ownerCreateDto = new RecordCreateDto(petIdOwned, RecordType.OTHER, "Owner observation", null);
            vaccineDetailsDto = new VaccineCreateDto("Rabies TestVaccine", 1, "TestLab", "BatchTest", true);
            vetVaccineDto = new RecordCreateDto(petIdOwned, RecordType.VACCINE, "Vaccination performed", vaccineDetailsDto);

            Long clinicId = 1L;

            mockMvc.perform(post("/api/pets/{petId}/associate-clinic/{clinicId}", petIdOwned, clinicId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                    .andExpect(status().isNoContent());
            entityManager.flush();
            entityManager.clear();
        }

        @Test
        @DisplayName("Should create OTHER record when called by Owner")
        void createRecord_Success_Owner() throws Exception {
            mockMvc.perform(post("/api/records")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(ownerCreateDto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", is(notNullValue())))
                    .andExpect(jsonPath("$.type", is(RecordType.OTHER.name())))
                    .andExpect(jsonPath("$.description", is(ownerCreateDto.description())))
                    .andExpect(jsonPath("$.creator.id", is(ownerId.intValue())))
                    .andExpect(jsonPath("$.vaccine", is(nullValue())))
                    .andExpect(jsonPath("$.vetSignature", is(nullValue())));
        }

        @Test
        @DisplayName("Should create VACCINE record SIGNED when called by Vet")
        void createRecord_Success_Vet_Vaccine_Signed() throws Exception {
            mockMvc.perform(post("/api/records")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(vetVaccineDto))
                            .param("sign", "true"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", is(notNullValue())))
                    .andExpect(jsonPath("$.type", is(RecordType.VACCINE.name())))
                    .andExpect(jsonPath("$.creator.id", is(vetId.intValue())))
                    .andExpect(jsonPath("$.vaccine.name", is(vaccineDetailsDto.name())))
                    .andExpect(jsonPath("$.vetSignature", is(notNullValue())))
                    .andExpect(jsonPath("$.vetSignature", not(emptyOrNullString())))
                    .andExpect(jsonPath("$.vetSignature", matchesRegex("^(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=)?$")));
        }

        @Test
        @DisplayName("Should return 400 Bad Request if VACCINE type but no vaccine details")
        void createRecord_BadRequest_VaccineDataMissing() throws Exception {
            RecordCreateDto invalidDto = new RecordCreateDto(petIdOwned, RecordType.VACCINE, "Missing details", null);
            mockMvc.perform(post("/api/records")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidDto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("Vaccine details are required")));
        }

        @Test
        @DisplayName("Should return 400 Bad Request if not VACCINE type but vaccine details provided")
        void createRecord_BadRequest_VaccineDataUnexpected() throws Exception {
            RecordCreateDto invalidDto = new RecordCreateDto(petIdOwned, RecordType.ILLNESS, "Flu", vaccineDetailsDto);
            mockMvc.perform(post("/api/records")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidDto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("Vaccine details should only be provided")));
        }

        @Test
        @DisplayName("Should return 403 Forbidden if user not authorized for pet")
        void createRecord_Forbidden_UserNotAuthorized() throws Exception {
            RecordCreateDto dtoForOtherPet = new RecordCreateDto(petIdOther, RecordType.OTHER, "Wrong pet", null);
            mockMvc.perform(post("/api/records")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dtoForOtherPet)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message", is("You do not have permission to perform this action or access this resource.")));
        }

        @Test
        @DisplayName("Should return 404 Not Found if petId in DTO not found")
        void createRecord_NotFound_Pet() throws Exception {
            RecordCreateDto dtoWithBadPetId = new RecordCreateDto(9999L, RecordType.OTHER, "Bad Pet", null);
            mockMvc.perform(post("/api/records")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dtoWithBadPetId)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message", containsString("Pet not found with id: 9999")));
        }
    }

    /**
     * --- Tests for GET /api/records (List Records for Pet) --- ---
     */
    @Nested
    @DisplayName("GET /api/records (List Records for Pet)")
    class ListRecordsIntegrationTests {

        private Long recordIdOwner;
        private Long recordIdVetSigned;
        private Long recordIdVetUnsigned;

        @BeforeEach
        void listRecordsSetup() throws Exception {
            Long clinicId = 1L;
            mockMvc.perform(post("/api/pets/{petId}/associate-clinic/{clinicId}", petIdOwned, clinicId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                    .andExpect(status().isNoContent());
            entityManager.flush();
            entityManager.clear();

            RecordCreateDto ownerRecDto = new RecordCreateDto(petIdOwned, RecordType.OTHER, "Owner record for list test", null);
            MvcResult resOwnerRec = mockMvc.perform(post("/api/records")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(ownerRecDto)))
                    .andExpect(status().isCreated()).andReturn();
            recordIdOwner = objectMapper.readValue(resOwnerRec.getResponse().getContentAsString(), RecordViewDto.class).id();

            VaccineCreateDto vacDtoSigned = new VaccineCreateDto("VacListSigned", 1, "LabListS", "BatchListS", true);
            RecordCreateDto vetRecDtoSigned = new RecordCreateDto(petIdOwned, RecordType.VACCINE, "Signed record for list test", vacDtoSigned);
            MvcResult resVetSigned = mockMvc.perform(post("/api/records?sign=true")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(vetRecDtoSigned)))
                    .andExpect(status().isCreated()).andReturn();
            recordIdVetSigned = objectMapper.readValue(resVetSigned.getResponse().getContentAsString(), RecordViewDto.class).id();

            // Vet creates an ILLNESS record, unsigned
            RecordCreateDto vetRecDtoUnsigned = new RecordCreateDto(petIdOwned, RecordType.ILLNESS, "Unsigned record for list test", null);
            MvcResult resVetUnsigned = mockMvc.perform(post("/api/records?sign=false")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(vetRecDtoUnsigned)))
                    .andExpect(status().isCreated()).andReturn();
            recordIdVetUnsigned = objectMapper.readValue(resVetUnsigned.getResponse().getContentAsString(), RecordViewDto.class).id();

            mockMvc.perform(post("/api/pets/{petId}/associate-vet/{vetId}", petIdOwned, vetId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                    .andExpect(status().isNoContent());

            entityManager.flush();
            entityManager.clear();

            assertThat(recordIdOwner).isNotNull();
            assertThat(recordIdVetSigned).isNotNull();
            assertThat(recordIdVetUnsigned).isNotNull();
        }

        @Test
        @DisplayName("Should return success records when called by Owner for own pet")
        void listRecords_Success_Owner() throws Exception {
            mockMvc.perform(get("/api/records")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                            .param("petId", petIdOwned.toString())
                            .param("page", "0")
                            .param("size", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(3)))
                    .andExpect(jsonPath("$.totalElements", is(3)))
                    .andExpect(jsonPath("$.content[0].id", is(recordIdVetUnsigned.intValue())))
                    .andExpect(jsonPath("$.content[1].id", is(recordIdVetSigned.intValue())))
                    .andExpect(jsonPath("$.content[2].id", is(recordIdOwner.intValue())));
        }

        @Test
        @DisplayName("Should return success records when called by associated Vet")
        void listRecords_Success_AssociatedVet() throws Exception {
            mockMvc.perform(get("/api/records")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken)
                            .param("petId", petIdOwned.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(3)));
        }

        @Test
        @DisplayName("Should return success empty page when pet has no records")
        void listRecords_Success_NoRecords() throws Exception {
            mockMvc.perform(get("/api/records")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherOwnerToken)
                            .param("petId", petIdOther.toString()))
                    .andExpect(status().isOk()) // Espera 200 OK
                    .andExpect(jsonPath("$.content", hasSize(0)))
                    .andExpect(jsonPath("$.totalElements", is(0)));
        }


        @Test
        @DisplayName("Should return 403 Forbidden when Owner requests records for other's pet")
        void listRecords_Forbidden_OwnerForOtherPet() throws Exception {
            mockMvc.perform(get("/api/records")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                            .param("petId", petIdOther.toString()))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message", is("You do not have permission to perform this action or access this resource.")));
        }

        @Test
        @DisplayName("Should return 403 Forbidden when Staff requests records for unassociated pet")
        void listRecords_Forbidden_StaffForUnassociatedPet() throws Exception {
            mockMvc.perform(get("/api/records")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken)
                            .param("petId", petIdOther.toString()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 404 Not Found when Pet ID does not exist")
        void listRecords_NotFound_Pet() throws Exception {
            mockMvc.perform(get("/api/records")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                            .param("petId", "9999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message", containsString("Pet not found with id: 9999")));
        }

        @Test
        @DisplayName("Should return 401 Unauthorized when no token provided")
        void listRecords_Unauthorized() throws Exception {
            mockMvc.perform(get("/api/records")
                            .param("petId", petIdOwned.toString()))
                    .andExpect(status().isUnauthorized());
        }


        @Test
        @DisplayName("[Pagination/Sort] should respect pagination and sorting parameters")
        void listRecords_PaginationAndSort() throws Exception {
            mockMvc.perform(get("/api/records")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                            .param("petId", petIdOwned.toString())
                            .param("page", "1")
                            .param("size", "1")
                            .param("sort", "type,asc"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.totalElements", is(3)))
                    .andExpect(jsonPath("$.pageable.pageNumber", is(1)))
                    .andExpect(jsonPath("$.pageable.pageSize", is(1)))
                    .andExpect(jsonPath("$.content[0].type", is(RecordType.VACCINE.name())));

            mockMvc.perform(get("/api/records")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                            .param("petId", petIdOwned.toString())
                            .param("page", "0")
                            .param("size", "2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(2)))
                    .andExpect(jsonPath("$.totalElements", is(3)))
                    .andExpect(jsonPath("$.content[0].id", is(recordIdVetUnsigned.intValue())))
                    .andExpect(jsonPath("$.content[1].id", is(recordIdVetSigned.intValue())));
        }
    }

    /**
     * --- Test for GET /api/records/{recordId} ---
     */
    @Nested
    @DisplayName("GET /api/records/{recordId}")
    class GetRecordByIdIntegrationTests {

        private Long ownerRecordId;
        private Long vetSignedRecordId;

        @BeforeEach
        void getRecordByIdSetup() throws Exception {
            Long clinicId = 1L;
            Long otherOwnerRecordId;
            mockMvc.perform(post("/api/pets/{petId}/associate-clinic/{clinicId}", petIdOwned, clinicId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                    .andExpect(status().isNoContent());

            RecordCreateDto ownerRecDto = new RecordCreateDto(petIdOwned, RecordType.OTHER, "Record for GetByID test - Owner", null);
            MvcResult resOwnerRec = mockMvc.perform(post("/api/records")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(ownerRecDto)))
                    .andExpect(status().isCreated()).andReturn();
            ownerRecordId = objectMapper.readValue(resOwnerRec.getResponse().getContentAsString(), RecordViewDto.class).id();

            VaccineCreateDto vacDtoSigned = new VaccineCreateDto("VacGetByID", 1, "LabGet", "BatchGet", true);
            RecordCreateDto vetRecDtoSigned = new RecordCreateDto(petIdOwned, RecordType.VACCINE, "Signed record for GetByID test", vacDtoSigned);
            MvcResult resVetSigned = mockMvc.perform(post("/api/records?sign=true")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(vetRecDtoSigned)))
                    .andExpect(status().isCreated()).andReturn();
            vetSignedRecordId = objectMapper.readValue(resVetSigned.getResponse().getContentAsString(), RecordViewDto.class).id();

            RecordCreateDto otherOwnerRecDto = new RecordCreateDto(petIdOther, RecordType.OTHER, "Record from other owner", null);
            MvcResult resOtherOwnerRec = mockMvc.perform(post("/api/records")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherOwnerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(otherOwnerRecDto)))
                    .andExpect(status().isCreated()).andReturn();
            otherOwnerRecordId = objectMapper.readValue(resOtherOwnerRec.getResponse().getContentAsString(), RecordViewDto.class).id();

            mockMvc.perform(post("/api/pets/{petId}/associate-vet/{vetId}", petIdOwned, vetId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                    .andExpect(status().isNoContent());

            entityManager.flush();
            entityManager.clear();

            assertThat(ownerRecordId).isNotNull();
            assertThat(vetSignedRecordId).isNotNull();
            assertThat(otherOwnerRecordId).isNotNull();
        }

        @Test
        @DisplayName("Should return success record when requested by Owner of associated pet")
        void getRecordById_Success_Owner() throws Exception {
            mockMvc.perform(get("/api/records/{recordId}", vetSignedRecordId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(vetSignedRecordId.intValue())))
                    .andExpect(jsonPath("$.type", is(RecordType.VACCINE.name())))
                    .andExpect(jsonPath("$.vetSignature", is(notNullValue())));

            mockMvc.perform(get("/api/records/{recordId}", ownerRecordId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(ownerRecordId.intValue())))
                    .andExpect(jsonPath("$.type", is(RecordType.OTHER.name())))
                    .andExpect(jsonPath("$.vetSignature", is(nullValue())));
        }

        @Test
        @DisplayName("Should return success record when requested by associated Staff")
        void getRecordById_Success_AssociatedStaff() throws Exception {
            mockMvc.perform(get("/api/records/{recordId}", ownerRecordId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(ownerRecordId.intValue())));

            mockMvc.perform(get("/api/records/{recordId}", vetSignedRecordId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(vetSignedRecordId.intValue())));
        }

        @Test
        @DisplayName("Should return 403 Forbidden when requested by unauthorized user (different owner)")
        void getRecordById_Forbidden_DifferentOwner() throws Exception {
            mockMvc.perform(get("/api/records/{recordId}", ownerRecordId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherOwnerToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 403  Forbidden when requested by unassociated Staff")
        void getRecordById_Forbidden_UnassociatedStaff() throws Exception {
            String otherAdminTokenFromSetup = obtainJwtToken(mockMvc, objectMapper, new AuthLoginRequestDto("admin_barcelona", "password123"));
            mockMvc.perform(get("/api/records/{recordId}", ownerRecordId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherAdminTokenFromSetup))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 404 Not Found if record ID does not exist")
        void getRecordById_NotFound_Record() throws Exception {
            mockMvc.perform(get("/api/records/{recordId}", 9999L)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message", containsString("Record not found with id: 9999")));
        }

        @Test
        @DisplayName("Should return 401 Unauthorized if no token provided")
        void getRecordById_Unauthorized() throws Exception {
            mockMvc.perform(get("/api/records/{recordId}", ownerRecordId))
                    .andExpect(status().isUnauthorized());
        }
    }

    /**
     * --- Test for DELETE /api/records/{recordId} ---
     */
    @Nested
    @DisplayName("DELETE /api/records/{recordId}")
    class DeleteRecordIntegrationTests {

        private Long unsignedOwnerRecordId;
        private Long signedVetRecordId;


        @BeforeEach
        void deleteRecordSetup() throws Exception {
            Long clinicId = 1L;
            Long recordFromOtherOwnerId;
            mockMvc.perform(post("/api/pets/{petId}/associate-clinic/{clinicId}", petIdOwned, clinicId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                    .andExpect(status().isNoContent());

            RecordCreateDto ownerRecDto = new RecordCreateDto(petIdOwned, RecordType.OTHER, "Unsigned Owner Rec", null);
            MvcResult resOwner = mockMvc.perform(post("/api/records").header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(ownerRecDto))).andExpect(status().isCreated()).andReturn();
            unsignedOwnerRecordId = objectMapper.readValue(resOwner.getResponse().getContentAsString(), RecordViewDto.class).id();

            VaccineCreateDto vacDto = new VaccineCreateDto("VacDeleteTest", 1, "LDel", "BDel", true);
            RecordCreateDto vetSignedDto = new RecordCreateDto(petIdOwned, RecordType.VACCINE, "Signed Vet Rec", vacDto);
            MvcResult resVetSigned = mockMvc.perform(post("/api/records?sign=true").header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(vetSignedDto))).andExpect(status().isCreated()).andReturn();
            signedVetRecordId = objectMapper.readValue(resVetSigned.getResponse().getContentAsString(), RecordViewDto.class).id();

            RecordCreateDto otherOwnerDto = new RecordCreateDto(petIdOther, RecordType.OTHER, "Other Owner Rec", null);
            MvcResult resOther = mockMvc.perform(post("/api/records")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherOwnerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(otherOwnerDto)))
                    .andExpect(status().isCreated()).andReturn();
            recordFromOtherOwnerId = objectMapper.readValue(resOther.getResponse().getContentAsString(), RecordViewDto.class).id();

            entityManager.flush();
            entityManager.clear();

            assertThat(unsignedOwnerRecordId).isNotNull();
            assertThat(signedVetRecordId).isNotNull();
            assertThat(recordFromOtherOwnerId).isNotNull();
        }

        @Test
        @DisplayName("Should return success  delete unsigned record when called by Owner creator")
        void deleteUnsigned_Success_ByOwnerCreator() throws Exception {
            mockMvc.perform(delete("/api/records/{recordId}", unsignedOwnerRecordId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                    .andExpect(status().isNoContent());
            assertThat(recordRepository.findById(unsignedOwnerRecordId)).isEmpty();
        }

        @Test
        @DisplayName("Should return 403 Forbidden when Admin tries to delete Owner's record")
        void deleteUnsigned_Forbidden_AdminDeletingOwnerRecord() throws Exception {
            mockMvc.perform(delete("/api/records/{recordId}", unsignedOwnerRecordId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message", containsString("You do not have permission to perform this action or access this resource.")));          assertThat(recordRepository.findById(unsignedOwnerRecordId)).isPresent();
        }

        @Test
        @DisplayName("Should return 400 Bad Request when trying to delete SIGNED record")
        void deleteUnsigned_BadRequest_RecordIsSigned() throws Exception {
            mockMvc.perform(delete("/api/records/{recordId}", signedVetRecordId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("because it has been signed")));
            assertThat(recordRepository.findById(signedVetRecordId)).isPresent();
        }

        @Test
        @DisplayName("Should return 403 Forbidden if non-creator Owner tries to delete record")
        void deleteUnsigned_Forbidden_WrongOwner() throws Exception {
            mockMvc.perform(delete("/api/records/{recordId}", unsignedOwnerRecordId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherOwnerToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 403 Forbidden if Vet tries to delete Owner's record")
        void deleteUnsigned_Forbidden_VetDeletingOwnerRecord() throws Exception {
            mockMvc.perform(delete("/api/records/{recordId}", unsignedOwnerRecordId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 403 Forbidden if Admin from DIFFERENT clinic tries to delete Owner's unsigned record")
        void deleteUnsigned_Forbidden_AdminDifferentClinicDeletingOwnerRecord() throws Exception {
            mockMvc.perform(delete("/api/records/{recordId}", unsignedOwnerRecordId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminBarcelonaToken))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message", containsString("You do not have permission to perform this action or access this resource.")));
            assertThat(recordRepository.findById(unsignedOwnerRecordId)).isPresent();
        }

        @Test
        @DisplayName("Should return 404 Not Found if record ID does not exist")
        void deleteUnsigned_NotFound_Record() throws Exception {
            mockMvc.perform(delete("/api/records/{recordId}", 9999L)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 401 Unauthorized if no token provided")
        void deleteUnsigned_Unauthorized() throws Exception {
            mockMvc.perform(delete("/api/records/{recordId}", unsignedOwnerRecordId))
                    .andExpect(status().isUnauthorized());
        }

    }
}