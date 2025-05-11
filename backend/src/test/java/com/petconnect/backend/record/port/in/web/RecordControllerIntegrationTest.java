package com.petconnect.backend.record.port.in.web;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petconnect.backend.pet.application.dto.PetActivationDto;
import com.petconnect.backend.pet.application.dto.PetRegistrationDto;
import com.petconnect.backend.pet.domain.model.Gender;
import com.petconnect.backend.pet.domain.model.Pet;
import com.petconnect.backend.pet.domain.model.Specie;
import com.petconnect.backend.pet.domain.repository.PetRepository;
import com.petconnect.backend.record.application.dto.*;
import com.petconnect.backend.record.domain.model.RecordType;
import com.petconnect.backend.record.domain.model.Record;
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
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Date;
import java.util.Optional;

import static com.petconnect.backend.util.IntegrationTestUtils.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
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
    @Autowired private PetRepository petRepository;
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
        adminBarcelonaToken = obtainJwtToken(mockMvc, objectMapper, new AuthLoginRequestDto("admin_barcelona", "password123"));
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

//    /**
//     * --- Tests for PUT /api/records/{recordId} (Update Record) ---
//     */
//    @Nested
//    @DisplayName("PUT /api/records/{recordId} (Update Unsigned Record)")
//    class UpdateRecordIntegrationTests {
//        private Long unsignedOwnerRecordId;
//        private Long unsignedStaffRecordId;
//        private Long signedVetRecordId;
//        private Long vaccineRecordId;
//
//        /**
//         * Create records needed specifically for update tests.
//         */
//        @BeforeEach
//        void updateSetup() throws Exception {
//            Long clinicId = 1L;
//
//            mockMvc.perform(post("/api/pets/{petId}/associate-clinic/{clinicId}", petIdOwned, clinicId)
//                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
//                    .andExpect(status().isNoContent());
//            entityManager.flush(); entityManager.clear();
//
//            Pet petEligibleEntity = petRepository.findById(petIdOwned).orElseThrow();
//            PetActivationDto activationEligible = new PetActivationDto(
//                    petEligibleEntity.getName(),
//                    StringUtils.hasText(petEligibleEntity.getColor()) ? petEligibleEntity.getColor() : "DefaultColor",
//                    petEligibleEntity.getGender() != null ? petEligibleEntity.getGender() : Gender.MALE,
//                    petEligibleEntity.getBirthDate() != null ? petEligibleEntity.getBirthDate() : LocalDate.now().minusYears(1),
//                    StringUtils.hasText(petEligibleEntity.getMicrochip()) ? petEligibleEntity.getMicrochip() : "MicrochipNeeded",
//                    petEligibleEntity.getBreed().getId(),
//                    StringUtils.hasText(petEligibleEntity.getImage()) ? petEligibleEntity.getImage() : "ImageNeeded.jpg"
//            );
//
//            mockMvc.perform(put("/api/pets/{petId}/activate", petIdOwned)
//                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken)
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(objectMapper.writeValueAsString(activationEligible)))
//                    .andExpect(status().isOk());
//
//            entityManager.flush();
//            entityManager.clear();
//
//            Pet reloadedPet = petRepository.findById(petIdOwned).orElseThrow();
//            assertThat(reloadedPet.getAssociatedVets()).as("Vet should be associated after activation").anyMatch(v -> v.getId().equals(vetId));
//
//            RecordCreateDto ownerRecDto = new RecordCreateDto(petIdOwned, RecordType.OTHER, "Owner Rec To Update", null);
//            MvcResult resOwner = mockMvc.perform(post("/api/records").header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(ownerRecDto))).andExpect(status().isCreated()).andReturn();
//            unsignedOwnerRecordId = extractRecordIdFromResult(objectMapper, resOwner);
//
//            RecordCreateDto staffRecDto = new RecordCreateDto(petIdOwned, RecordType.ILLNESS, "Staff Rec To Update (By Admin)", null);
//            MvcResult resStaff = mockMvc.perform(post("/api/records")
//                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(objectMapper.writeValueAsString(staffRecDto)))
//                    .andExpect(status().isCreated()).andReturn();
//            unsignedStaffRecordId = extractRecordIdFromResult(objectMapper, resStaff);
//
//            VaccineCreateDto vacDto = new VaccineCreateDto("Vac Signed", 1, "LUpd", "BUpd", true);
//            RecordCreateDto signedDto = new RecordCreateDto(petIdOwned, RecordType.VACCINE, "Signed Rec", vacDto);
//            MvcResult resSigned = mockMvc.perform(post("/api/records")
//                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken)
//                    .contentType(MediaType.APPLICATION_JSON)
//                    .content(objectMapper.writeValueAsString(signedDto))
//            ).andExpect(status().isCreated()).andReturn();
//            signedVetRecordId = extractRecordIdFromResult(objectMapper, resSigned);
//            vaccineRecordId = signedVetRecordId;
//
//            VaccineCreateDto vacUnsignedDto = new VaccineCreateDto("VacUnsigned", 3, "LabUnsigned", "BUnsigned", false);
//            RecordCreateDto unsignedVaccineRecDto = new RecordCreateDto(petIdOwned, RecordType.VACCINE, "Unsigned Vaccine Rec", vacUnsignedDto);
//
//            MvcResult resUnsignedVac = mockMvc.perform(post("/api/records")
//                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(objectMapper.writeValueAsString(unsignedVaccineRecDto)))
//                    .andExpect(status().isCreated()).andReturn();
//            vaccineRecordId = extractRecordIdFromResult(objectMapper, resUnsignedVac);
//            RecordViewDto createdUnsignedVacRec = objectMapper.readValue(resUnsignedVac.getResponse().getContentAsString(), RecordViewDto.class);
//            assertThat(createdUnsignedVacRec.vetSignature()).as("VACCINE Record created by Admin should be unsigned").isNull();
//            assertThat(createdUnsignedVacRec.type()).isEqualTo(RecordType.VACCINE);
//
//            entityManager.flush(); entityManager.clear();
//            assertThat(unsignedOwnerRecordId).isNotNull();
//            assertThat(unsignedStaffRecordId).isNotNull();
//            assertThat(signedVetRecordId).isNotNull();
//            assertThat(vaccineRecordId).isNotNull();
//        }
//
//        @Test
//        @DisplayName("Should update record successfully when Owner updates own unsigned record")
//        void updateRecord_Success_OwnerUpdatesOwn() throws Exception {
//            RecordUpdateDto updateDto = new RecordUpdateDto(RecordType.ILLNESS, "Owner Updated Description");
//            mockMvc.perform(put("/api/records/{recordId}", unsignedOwnerRecordId)
//                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(objectMapper.writeValueAsString(updateDto)))
//                    .andExpect(status().isOk())
//                    .andExpect(jsonPath("$.id", is(unsignedOwnerRecordId.intValue())))
//                    .andExpect(jsonPath("$.type", is(updateDto.type().name())))
//                    .andExpect(jsonPath("$.description", is(updateDto.description())));
//        }
//
//        @Test
//        @DisplayName("Should update record successfully when Staff updates unsigned record from same clinic staff")
//        void updateRecord_Success_StaffUpdatesStaff() throws Exception {
//            RecordUpdateDto updateDto = new RecordUpdateDto(RecordType.ANNUAL_CHECK, "Admin Updated Description");
//
//            mockMvc.perform(put("/api/records/{recordId}", unsignedStaffRecordId)
//                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(objectMapper.writeValueAsString(updateDto)))
//                    .andExpect(status().isOk())
//                    .andExpect(jsonPath("$.id", is(unsignedStaffRecordId.intValue())))
//                    .andExpect(jsonPath("$.type", is(updateDto.type().name())))
//                    .andExpect(jsonPath("$.description", is(updateDto.description())))
//                    .andExpect(jsonPath("$.vetSignature", is(nullValue())));
//
//            Optional<Record> recordOpt = recordRepository.findById(unsignedStaffRecordId);
//            assertThat(recordOpt).isPresent();
//            assertThat(recordOpt.get().getType()).isEqualTo(updateDto.type());
//            assertThat(recordOpt.get().getDescription()).isEqualTo(updateDto.description());
//            assertThat(recordOpt.get().getVetSignature()).isNull();
//        }
//
//        @Test
//        @DisplayName("Should return 409 Conflict when trying to update a signed record")
//        void updateRecord_Conflict_RecordSigned() throws Exception {
//            RecordUpdateDto updateDto = new RecordUpdateDto(null, "Attempt to update signed");
//            mockMvc.perform(put("/api/records/{recordId}", signedVetRecordId)
//                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken)
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(objectMapper.writeValueAsString(updateDto)))
//                    .andExpect(status().isConflict())
//                    .andExpect(jsonPath("$.message", containsString("because it has been signed")));
//        }
//
//        @Test
//        @DisplayName("Should return 409 Conflict when trying to update a VACCINE record")
//        void updateRecord_Conflict_RecordIsVaccine() throws Exception {
//            RecordUpdateDto updateDto = new RecordUpdateDto(null, "Attempt to update vaccine record");
//            mockMvc.perform(put("/api/records/{recordId}", vaccineRecordId)
//                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(objectMapper.writeValueAsString(updateDto)))
//                    .andExpect(status().isConflict())
//                    .andExpect(jsonPath("$.message", containsString("Cannot update record " + vaccineRecordId + ": records of type VACCINE cannot be updated.")));
//        }
//
//        @Test
//        @DisplayName("Should return 409 Conflict when trying to change type TO VACCINE")
//        void updateRecord_Conflict_ChangeToVaccine() throws Exception {
//            RecordUpdateDto updateDto = new RecordUpdateDto(RecordType.VACCINE, "Changing to vaccine");
//            mockMvc.perform(put("/api/records/{recordId}", unsignedOwnerRecordId)
//                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(objectMapper.writeValueAsString(updateDto)))
//                    .andExpect(status().isConflict())
//                    .andExpect(jsonPath("$.message", containsString("Cannot update record " + unsignedOwnerRecordId + ": cannot change record type to VACCINE.")));
//        }
//        @Test
//        @DisplayName("Should return 403 Forbidden if Staff tries to update Owner's record")
//        void updateRecord_Forbidden_StaffUpdatesOwner() throws Exception {
//            RecordUpdateDto updateDto = new RecordUpdateDto(null, "Staff trying owner update");
//            mockMvc.perform(put("/api/records/{recordId}", unsignedOwnerRecordId)
//                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken)
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(objectMapper.writeValueAsString(updateDto)))
//                    .andExpect(status().isForbidden())
//                    .andExpect(jsonPath("$.message", is("You do not have permission to perform this action or access this resource.")));
//        }
//
//        @Test
//        @DisplayName("Should return 403 Forbidden if Owner tries to update Staff's record")
//        void updateRecord_Forbidden_OwnerUpdatesStaff() throws Exception {
//            RecordUpdateDto updateDto = new RecordUpdateDto(null, "Owner trying staff update");
//            mockMvc.perform(put("/api/records/{recordId}", unsignedStaffRecordId)
//                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(objectMapper.writeValueAsString(updateDto)))
//                    .andExpect(status().isForbidden())
//                    .andExpect(jsonPath("$.message", is("You do not have permission to perform this action or access this resource.")));
//        }
//
//        @Test
//        @DisplayName("Should return 403 Forbidden if Staff updates record from DIFFERENT clinic staff")
//        void updateRecord_Forbidden_StaffUpdatesDifferentClinicStaff() throws Exception {
//            RecordUpdateDto updateDto = new RecordUpdateDto(null, "Other clinic trying update");
//            mockMvc.perform(put("/api/records/{recordId}", unsignedStaffRecordId)
//                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminBarcelonaToken) // Admin from another clinic
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(objectMapper.writeValueAsString(updateDto)))
//                    .andExpect(status().isForbidden())
//                    .andExpect(jsonPath("$.message", is("You do not have permission to perform this action or access this resource.")));
//        }
//
//        @Test
//        @DisplayName("Should return 404 Not Found if record ID does not exist")
//        void updateRecord_NotFound_Record() throws Exception {
//            RecordUpdateDto updateDto = new RecordUpdateDto(null, "Update non-existent");
//            mockMvc.perform(put("/api/records/{recordId}", 9999L)
//                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(objectMapper.writeValueAsString(updateDto)))
//                    .andExpect(status().isNotFound());
//        }
//
//        @Test
//        @DisplayName("Should return 401 Unauthorized if no token provided")
//        void updateRecord_Unauthorized() throws Exception {
//            RecordUpdateDto updateDto = new RecordUpdateDto(null, "Update non-existent");
//            mockMvc.perform(put("/api/records/{recordId}", unsignedOwnerRecordId)
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(objectMapper.writeValueAsString(updateDto)))
//                    .andExpect(status().isUnauthorized());
//        }
//
//    }

    /**
     * --- Test for DELETE /api/records/{recordId} ---
     */
    @Nested
    @DisplayName("DELETE /api/records/{recordId}")
    class DeleteRecordIntegrationTests {

        private Long unsignedOwnerRecordId;
        private Long signedVetRecordId;
        private Long unsignedAdminRecordId;

        @BeforeEach
        void deleteRecordSetup() throws Exception {
            Long clinicId = 1L;
            mockMvc.perform(post("/api/pets/{petId}/associate-clinic/{clinicId}", petIdOwned, clinicId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                    .andExpect(status().isNoContent());

            RecordCreateDto ownerRecDto = new RecordCreateDto(petIdOwned, RecordType.OTHER, "Unsigned Owner Rec", null);
            MvcResult resOwner = mockMvc.perform(post("/api/records").header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(ownerRecDto))).andExpect(status().isCreated()).andReturn();
            unsignedOwnerRecordId = extractRecordIdFromResult(objectMapper, resOwner);

            VaccineCreateDto vacDto = new VaccineCreateDto("VacDeleteTest", 1, "LDel", "BDel", true);
            RecordCreateDto vetSignedDto = new RecordCreateDto(petIdOwned, RecordType.VACCINE, "Signed Vet Rec", vacDto);
            MvcResult resVetSigned = mockMvc.perform(post("/api/records")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(vetSignedDto)))
                    .andExpect(status().isCreated()).andReturn();
            signedVetRecordId = extractRecordIdFromResult(objectMapper, resVetSigned);

            Record signedRec = recordRepository.findById(signedVetRecordId).orElseThrow();
            assertThat(signedRec.getVetSignature()).as("Record created by Vet should be signed").isNotNull();
            assertThat(signedRec.isImmutable()).as("Record should not be immutable yet").isFalse();

            RecordCreateDto adminUnsignedDto = new RecordCreateDto(petIdOwned, RecordType.ILLNESS, "Unsigned Admin Rec", null);
            MvcResult resAdminUnsigned = mockMvc.perform(post("/api/records")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(adminUnsignedDto)))
                    .andExpect(status().isCreated()).andReturn();
            unsignedAdminRecordId = extractRecordIdFromResult(objectMapper, resAdminUnsigned);

            Record unsignedAdminRec = recordRepository.findById(unsignedAdminRecordId).orElseThrow();
            assertThat(unsignedAdminRec.getVetSignature()).as("Record created by Admin should be unsigned").isNull();
            assertThat(unsignedAdminRec.getCreator().getId()).as("Creator should be the Admin").isEqualTo(2L);

            RecordCreateDto otherOwnerDto = new RecordCreateDto(petIdOther, RecordType.OTHER, "Other Owner Rec", null);
            MvcResult resOther = mockMvc.perform(post("/api/records")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherOwnerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(otherOwnerDto)))
                    .andExpect(status().isCreated()).andReturn();
            Long recordFromOtherOwnerId = extractRecordIdFromResult(objectMapper, resOther);

            entityManager.flush();
            entityManager.clear();

            assertThat(unsignedOwnerRecordId).isNotNull();
            assertThat(signedVetRecordId).isNotNull();
            assertThat(unsignedAdminRecordId).isNotNull();
            assertThat(recordFromOtherOwnerId).isNotNull();
        }

        @Test
        @DisplayName("[Unsigned] Should return 204 No Content when Owner deletes own record")
        void deleteUnsigned_Success_ByOwnerCreator() throws Exception {
            mockMvc.perform(delete("/api/records/{recordId}", unsignedOwnerRecordId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                    .andExpect(status().isNoContent());
            assertThat(recordRepository.findById(unsignedOwnerRecordId)).isEmpty();
        }

        @Test
        @DisplayName("[Unsigned] Should return 204 No Content when Admin deletes record from staff in same clinic")
        void deleteUnsigned_Success_ByAdminSameClinic() throws Exception {
            mockMvc.perform(delete("/api/records/{recordId}", unsignedAdminRecordId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                    .andExpect(status().isNoContent());
            assertThat(recordRepository.findById(unsignedAdminRecordId)).isEmpty();
        }


        @Test
        @DisplayName("[Unsigned] Should return 403 Forbidden when Admin tries to delete Owner's record")
        void deleteUnsigned_Forbidden_AdminDeletingOwnerRecord() throws Exception {
            mockMvc.perform(delete("/api/records/{recordId}", unsignedOwnerRecordId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message", containsString("You do not have permission to perform this action or access this resource.")));
            assertThat(recordRepository.findById(unsignedOwnerRecordId)).isPresent();
        }


        @Test
        @DisplayName("[Signed] Should return 204 No Content when signing Vet deletes OWN signed but NOT IMMUTABLE record")
        void deleteSigned_Success_SigningVet_NotImmutable() throws Exception {
            mockMvc.perform(delete("/api/records/{recordId}", signedVetRecordId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken))
                    .andExpect(status().isNoContent());
            assertThat(recordRepository.findById(signedVetRecordId)).isEmpty();
        }


        @Test
        @DisplayName("[Signed] Should return 409 Conflict when trying to delete SIGNED and IMMUTABLE record")
        void deleteSigned_Conflict_RecordIsImmutable() throws Exception {
            // Arrange
            Record recordToDelete = recordRepository.findById(signedVetRecordId).orElseThrow();
            recordToDelete.setImmutable(true);
            recordRepository.saveAndFlush(recordToDelete);
            entityManager.clear();

            // Act & Assert
            mockMvc.perform(delete("/api/records/{recordId}", signedVetRecordId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message", containsString("is immutable and cannot be modified or deleted")));

            assertThat(recordRepository.findById(signedVetRecordId)).isPresent();
        }


        @Test
        @DisplayName("[Signed] Should return 403 Forbidden when non-creator (Owner) tries to delete signed record")
        void deleteSigned_Forbidden_OwnerDeletingSigned() throws Exception {
            // Arrange
            Record recordToDelete = recordRepository.findById(signedVetRecordId).orElseThrow();
            recordToDelete.setImmutable(false);
            recordRepository.saveAndFlush(recordToDelete);
            entityManager.clear();

            // Act & Assert
            mockMvc.perform(delete("/api/records/{recordId}", signedVetRecordId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message", containsString("You do not have permission to perform this action or access this resource.")));

            assertThat(recordRepository.findById(signedVetRecordId)).isPresent();
        }

        @Test
        @DisplayName("[Signed] Should return 403 Forbidden when non-creator (Admin) tries to delete signed record")
        void deleteSigned_Forbidden_AdminDeletingSigned() throws Exception {
            // Arrange
            Record recordToDelete = recordRepository.findById(signedVetRecordId).orElseThrow();
            recordToDelete.setImmutable(false);
            recordRepository.saveAndFlush(recordToDelete);
            entityManager.clear();

            // Act & Assert
            mockMvc.perform(delete("/api/records/{recordId}", signedVetRecordId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message", containsString("You do not have permission to perform this action or access this resource.")));

            assertThat(recordRepository.findById(signedVetRecordId)).isPresent();
        }

        @Test
        @DisplayName("Should return 404 Not Found if record ID does not exist")
        void delete_NotFound_Record() throws Exception {
            mockMvc.perform(delete("/api/records/{recordId}", 9999L)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 401 Unauthorized if no token provided")
        void delete_Unauthorized() throws Exception {
            mockMvc.perform(delete("/api/records/{recordId}", unsignedOwnerRecordId))
                    .andExpect(status().isUnauthorized());
        }
    }

    /**
     * --- Tests for POST /api/records/{petId}/temporary-access ---
     */
    @Nested
    @DisplayName("POST /api/records/{petId}/temporary-access")
    class GenerateTemporaryAccessTokenIntegrationTests{
        private Long petIdForTokenTest;

        /**
         * Setup: Ensure the pet exists and belongs to the owner with ownerToken.
         */
        @BeforeEach
        void setupTokenTest() {
            petIdForTokenTest = petIdOwned;
            assertThat(petRepository.findById(petIdForTokenTest)).isPresent();
            Optional<Pet> petOpt = petRepository.findById(petIdForTokenTest);
            assertThat(petOpt).as("Pet with ID " + petIdForTokenTest + " should exist for token test setup")
                    .isPresent();
            assertThat(petOpt.get().getOwner()).as("Owner of pet " + petIdForTokenTest + " should not be null")
                    .isNotNull();
            assertThat(petOpt.get().getOwner().getId()).as("Owner ID should match")
                    .isEqualTo(ownerId);
        }

        @Test
        @DisplayName("Should return 200 OK with token when called by Owner with valid duration")
        void generateToken_Success_Owner() throws Exception {
            TemporaryAccessRequestDto requestDto = new TemporaryAccessRequestDto("PT1H"); // 1 hour

            MvcResult result = mockMvc.perform(post("/api/records/{petId}/temporary-access", petIdForTokenTest)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token", is(notNullValue())))
                    .andExpect(jsonPath("$.token", matchesRegex("^[A-Za-z0-9-_=]+\\.[A-Za-z0-9-_=]+\\.?[A-Za-z0-9-_.+/=]*$")))
                    .andReturn();

            String token = objectMapper.readValue(result.getResponse().getContentAsString(), TemporaryAccessTokenDto.class).token();
            DecodedJWT decodedJWT = JWT.decode(token);
            assertThat(decodedJWT.getIssuer()).isEqualTo("PetConnectTestIssuer");

            assertThat(decodedJWT.getClaim("petId").asLong()).isEqualTo(petIdForTokenTest);
            assertThat(decodedJWT.getClaim("type").asString()).isEqualTo("TEMP_RECORD_ACCESS");
            assertThat(decodedJWT.getExpiresAt()).isAfter(new Date());
            assertThat(decodedJWT.getExpiresAt().getTime() - decodedJWT.getIssuedAt().getTime())
                    .isCloseTo(Duration.ofHours(1).toMillis(), within(1000L)); // Check expiry ~1h later
        }

        @Test
        @DisplayName("Should return 403 Forbidden when called by non-Owner (Vet)")
        void generateToken_Forbidden_NotOwner() throws Exception {
            TemporaryAccessRequestDto requestDto = new TemporaryAccessRequestDto("PT1H");

            mockMvc.perform(post("/api/records/{petId}/temporary-access", petIdForTokenTest)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 404 Not Found when Pet ID does not exist")
        void generateToken_NotFound_Pet() throws Exception {
            TemporaryAccessRequestDto requestDto = new TemporaryAccessRequestDto("PT1H");

            mockMvc.perform(post("/api/records/{petId}/temporary-access", 9999L)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 400 Bad Request when duration format is invalid")
        void generateToken_BadRequest_InvalidDuration() throws Exception {
            TemporaryAccessRequestDto requestDto = new TemporaryAccessRequestDto("1_hour_invalid");

            mockMvc.perform(post("/api/records/{petId}/temporary-access", petIdForTokenTest)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error", is("Bad Request")))
                    .andExpect(jsonPath("$.message", containsString("Invalid duration format")));
        }

        @Test
        @DisplayName("Should return 401 Unauthorized when no token provided")
        void generateToken_Unauthorized() throws Exception {
            TemporaryAccessRequestDto requestDto = new TemporaryAccessRequestDto("PT1H");

            mockMvc.perform(post("/api/records/{petId}/temporary-access", petIdForTokenTest)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isUnauthorized());
        }
    }
}