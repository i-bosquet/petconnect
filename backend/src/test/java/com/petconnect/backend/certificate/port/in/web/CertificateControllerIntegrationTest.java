package com.petconnect.backend.certificate.port.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petconnect.backend.certificate.application.dto.CertificateGenerationRequestDto;
import com.petconnect.backend.certificate.application.dto.CertificateViewDto;
import com.petconnect.backend.pet.application.dto.PetActivationDto;
import com.petconnect.backend.pet.application.dto.PetProfileDto;
import com.petconnect.backend.pet.application.dto.PetRegistrationDto;
import com.petconnect.backend.pet.domain.model.Gender;
import com.petconnect.backend.pet.domain.model.Pet;
import com.petconnect.backend.pet.domain.model.Specie;
import com.petconnect.backend.pet.domain.repository.PetRepository;
import com.petconnect.backend.record.application.dto.RecordCreateDto;
import com.petconnect.backend.record.application.dto.RecordViewDto;
import com.petconnect.backend.record.application.dto.VaccineCreateDto;
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

import java.time.LocalDate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@link CertificateController}.
 * Uses PostgresSQL (Docker), security filters, transactional rollback.
 * Verifies certificate generation and retrieval endpoints, including authorization.
 * Requires significant setup to create users, pets, records, and signatures.
 *
 * @author ibosquet (with AI assistance based on previous tests)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class CertificateControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private PetRepository petRepository;
    @Autowired private RecordRepository recordRepository;
    @Autowired private EntityManager entityManager;

    // --- Tokens ---
    private String ownerToken;
    private String vetToken;
    private String otherOwnerToken;
    private String otherVetToken;

    // --- IDs ---
    private Long vetId;
    private Long petIdEligible;
    private Long petIdNotEligible;
    private Long recordIdRabiesOk;
    private Long recordIdRabiesUnsigned;
    private Long recordIdIllness;
    private final Long clinic1Id = 1L;

    /**
     * Comprehensive setup: Creates users, pets, associates them, creates records (signed/unsigned/eligible/ineligible).
     */
    @BeforeEach
    void setUp() throws Exception {
        // Get Tokens for existing users
        String adminToken;
        adminToken = obtainJwtToken(new AuthLoginRequestDto("admin_london", "password123"));
        otherVetToken = obtainJwtToken(new AuthLoginRequestDto("admin_barcelona", "password123"));

        // Create Owner
        String ownerUsername = "cert_owner_" + System.currentTimeMillis();
        OwnerRegistrationDto ownerReg = new OwnerRegistrationDto(ownerUsername, ownerUsername + "@test.com", "password123", "CertOwnerPhone");
        userRepository.findByUsername(ownerUsername).ifPresent(u -> {
            petRepository.findByOwnerId(u.getId()).forEach(p -> {
                recordRepository.deleteAllByPetId(p.getId());
                petRepository.delete(p);
            });
            userRepository.delete(u);
            entityManager.flush();
        });

        MvcResult ownerRegResult = mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(ownerReg))).andExpect(status().isCreated()).andReturn();
        objectMapper.readValue(ownerRegResult.getResponse().getContentAsString(), OwnerProfileDto.class);
        ownerToken = obtainJwtToken(new AuthLoginRequestDto(ownerReg.username(), ownerReg.password()));

        // Create Vet (associated with Clinic 1)
        String vetUsername = "cert_vet_" + System.currentTimeMillis();
        ClinicStaffCreationDto vetReg = new ClinicStaffCreationDto(vetUsername, vetUsername + "@test.com", "password123", "Cert", "Vet", RoleEnum.VET, "VETCERT" + System.currentTimeMillis(), "VETKEYCERT" + System.currentTimeMillis());
        userRepository.findByUsername(vetUsername).ifPresent(userRepository::delete);
        entityManager.flush();
        MvcResult vetRegResult = mockMvc.perform(post("/api/staff").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(vetReg))).andExpect(status().isCreated()).andReturn();
        ClinicStaffProfileDto vetDto = objectMapper.readValue(vetRegResult.getResponse().getContentAsString(), ClinicStaffProfileDto.class);
        vetId = vetDto.id();
        vetToken = obtainJwtToken(new AuthLoginRequestDto(vetReg.username(), vetReg.password()));

        // Create Pets for Owner
        Long labradorBreedId = 25L;
        PetRegistrationDto petEligibleReg = new PetRegistrationDto("EligibleCertPet", Specie.DOG, LocalDate.now().minusYears(2), labradorBreedId, null, "Brown", Gender.MALE, "CERTELIGIBLE"+ System.currentTimeMillis());
        MvcResult petEligibleRes = mockMvc.perform(post("/api/pets").header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(petEligibleReg))).andExpect(status().isCreated()).andReturn();
        petIdEligible = extractPetIdFromResult(petEligibleRes);

        PetRegistrationDto petNotEligibleReg = new PetRegistrationDto("NotEligibleCertPet", Specie.CAT, LocalDate.now().minusYears(1), 45L, null, "Black", Gender.FEMALE, "CERTNOTELIGIBLE"+ System.currentTimeMillis()); // Use 45 for Mixed/Other Cat
        MvcResult petNotEligibleRes = mockMvc.perform(post("/api/pets").header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(petNotEligibleReg))).andExpect(status().isCreated()).andReturn();
        petIdNotEligible = extractPetIdFromResult(petNotEligibleRes);

        // Activate Eligible Pet and Associate Vet with it
        mockMvc.perform(post("/api/pets/{petId}/associate-clinic/{clinicId}", petIdEligible, clinic1Id).header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)).andExpect(status().isNoContent());
        entityManager.flush(); entityManager.clear();

        Pet petEligibleEntity = petRepository.findById(petIdEligible).orElseThrow(() -> new AssertionError("Eligible Pet not found after creation"));
        PetActivationDto activationEligible = new PetActivationDto(petEligibleEntity.getName(), petEligibleEntity.getColor(), petEligibleEntity.getGender(), petEligibleEntity.getBirthDate(), petEligibleEntity.getMicrochip(), petEligibleEntity.getBreed().getId(), petEligibleEntity.getImage());
        mockMvc.perform(put("/api/pets/{petId}/activate", petIdEligible).header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(activationEligible))).andExpect(status().isOk());
        entityManager.flush(); entityManager.clear();

        petEligibleEntity = petRepository.findById(petIdEligible).orElseThrow(() -> new AssertionError("Eligible Pet not found after activation"));
        assertThat(petEligibleEntity.getAssociatedVets()).as("Vet should be associated with eligible pet after activation/association").anyMatch(v -> v.getId().equals(vetId));


        // Create the necessary records for Eligible Pet (by Vet)
        VaccineCreateDto rabiesVacDto = new VaccineCreateDto("Rabies", 1, "RabiesLab", "BatchRAB" + System.currentTimeMillis());
        RecordCreateDto rabiesRecDto = new RecordCreateDto(petIdEligible, RecordType.VACCINE, "Rabies vaccine administered", rabiesVacDto);
        MvcResult rabiesRes = mockMvc.perform(post("/api/records")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rabiesRecDto)))
                .andExpect(status().isCreated()).andReturn();
        recordIdRabiesOk = extractRecordIdFromResult(rabiesRes);
        RecordViewDto rabiesDto = objectMapper.readValue(rabiesRes.getResponse().getContentAsString(), RecordViewDto.class);
        assertThat(rabiesDto.vetSignature()).isNotNull().isNotBlank();

        VaccineCreateDto vacDto2 = new VaccineCreateDto("Distemper", 1, "DistLab", "BatchDIST" + System.currentTimeMillis());
        RecordCreateDto vacRecDto2 = new RecordCreateDto(petIdEligible, RecordType.VACCINE, "Distemper vaccine administered", vacDto2);
        MvcResult vacRes2 = mockMvc.perform(post("/api/records")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vacRecDto2)))
                .andExpect(status().isCreated()).andReturn();
        Long recordIdSecondVaccineOk = extractRecordIdFromResult(vacRes2);
        assertThat(recordIdSecondVaccineOk).isNotNull();

        // -- Annual Check
        RecordCreateDto annualRecDto = new RecordCreateDto(petIdEligible, RecordType.ANNUAL_CHECK, "Annual checkup passed", null);
        MvcResult annualRes = mockMvc.perform(post("/api/records")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(annualRecDto)))
                .andExpect(status().isCreated()).andReturn();
        Long recordIdAnnualOk = extractRecordIdFromResult(annualRes);
        RecordViewDto annualDto = objectMapper.readValue(annualRes.getResponse().getContentAsString(), RecordViewDto.class);
        assertThat(annualDto.vetSignature()).isNotNull().isNotBlank();

        // Create Records for Ineligible Pet
        VaccineCreateDto rabiesUnsignedDto = new VaccineCreateDto("RabiesUnsigned", 1, "LabU", "BatchU" + System.currentTimeMillis());
        RecordCreateDto rabiesUnsignedRecDto = new RecordCreateDto(petIdNotEligible, RecordType.VACCINE, "Rabies given by owner, not signed", rabiesUnsignedDto);
        MvcResult rabiesUnsignedRes = mockMvc.perform(post("/api/records")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rabiesUnsignedRecDto)))
                .andExpect(status().isCreated()).andReturn();
        recordIdRabiesUnsigned = extractRecordIdFromResult(rabiesUnsignedRes);
        RecordViewDto rabiesUnsignedViewDto = objectMapper.readValue(rabiesUnsignedRes.getResponse().getContentAsString(), RecordViewDto.class);
        assertThat(rabiesUnsignedViewDto.vetSignature()).isNull();

        // -- Signed Illness Record (Wrong Type, for petIdEligible)
        RecordCreateDto illnessRecDto = new RecordCreateDto(petIdEligible, RecordType.ILLNESS, "Treated for cough", null);
        MvcResult illnessRes = mockMvc.perform(post("/api/records")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(illnessRecDto)))
                .andExpect(status().isCreated()).andReturn();
        recordIdIllness = extractRecordIdFromResult(illnessRes);
        RecordViewDto illnessDto = objectMapper.readValue(illnessRes.getResponse().getContentAsString(), RecordViewDto.class);
        assertThat(illnessDto.vetSignature()).isNotNull().isNotBlank();

        Record rabiesRecord = recordRepository.findById(recordIdRabiesOk).orElseThrow();
        rabiesRecord.setCreatedAt(LocalDate.now().minusYears(5).atStartOfDay());
        recordRepository.saveAndFlush(rabiesRecord);
        entityManager.clear();

        // Create Other Owner's Pet (for auth tests)
        String otherOwnerUsername = "cert_other_owner_" + System.currentTimeMillis();
        OwnerRegistrationDto otherOwnerReg = new OwnerRegistrationDto(otherOwnerUsername, otherOwnerUsername + "@test.com", "password123", "CertOtherOwnerPhone");
        userRepository.findByUsername(otherOwnerUsername).ifPresent(userRepository::delete);
        entityManager.flush();
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(otherOwnerReg))).andExpect(status().isCreated()).andReturn();
        otherOwnerToken = obtainJwtToken(new AuthLoginRequestDto(otherOwnerReg.username(), otherOwnerReg.password()));

        PetRegistrationDto otherPetReg = new PetRegistrationDto("OtherCertPet", Specie.FERRET, LocalDate.now().minusMonths(4), null, null, "White", Gender.FEMALE, "CERTOTHER");
        MvcResult otherPetRes = mockMvc.perform(post("/api/pets").header(HttpHeaders.AUTHORIZATION, "Bearer " + otherOwnerToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(otherPetReg))).andExpect(status().isCreated()).andReturn();
        Long petIdOtherOwner = extractPetIdFromResult(otherPetRes);

        assertThat(ownerToken).isNotBlank();
        assertThat(vetToken).isNotBlank();
        assertThat(adminToken).isNotBlank();
        assertThat(otherOwnerToken).isNotBlank();
        assertThat(otherVetToken).isNotBlank();
        assertThat(petIdEligible).isNotNull();
        assertThat(petIdNotEligible).isNotNull();
        assertThat(petIdOtherOwner).isNotNull();
        assertThat(recordIdRabiesOk).isNotNull();
        assertThat(recordIdSecondVaccineOk).isNotNull();
        assertThat(recordIdAnnualOk).isNotNull();
        assertThat(recordIdRabiesUnsigned).isNotNull();
        assertThat(recordIdIllness).isNotNull();
    }

    /** Helper to obtain JWT token */
    private String obtainJwtToken(AuthLoginRequestDto loginRequest) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();
        AuthResponseDto responseDto = objectMapper.readValue(result.getResponse().getContentAsString(), AuthResponseDto.class);
        return responseDto.jwt();
    }

    /** Helper to extract ID from PetProfileDto response */
    private Long extractPetIdFromResult(MvcResult result) throws Exception {
        String json = result.getResponse().getContentAsString();
        PetProfileDto dto = objectMapper.readValue(json, PetProfileDto.class);
        return dto.id();
    }

    /** Helper to extract Record ID */
    private Long extractRecordIdFromResult(MvcResult result) throws Exception {
        String json = result.getResponse().getContentAsString();
        RecordViewDto dto = objectMapper.readValue(json, RecordViewDto.class);
        return dto.id();
    }

    /**
     * --- Tests for POST /api/certificates (Generate Certificate) ---
     */
    @Nested
    @DisplayName("POST /api/certificates")
    class GenerateCertificateTests {
        private Long recordIdSecondVaccineOk;

        @BeforeEach
        void setup() throws Exception {
            VaccineCreateDto vacDto2 = new VaccineCreateDto("Distemper", 1, "DistLab", "BatchDIST" + System.currentTimeMillis());
            RecordCreateDto rec2 = new RecordCreateDto(petIdEligible, RecordType.VACCINE, "Distemper vaccine", vacDto2);
            MvcResult recRes2 = mockMvc.perform(post("/api/records")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(rec2)))
                    .andExpect(status().isCreated()).andReturn();
            recordIdSecondVaccineOk = extractRecordIdFromResult(recRes2);
            assertThat(recordIdSecondVaccineOk).isNotNull();
            entityManager.flush();entityManager.clear();
        }

        @Test
        @DisplayName("should generate certificate successfully when called by authorized Vet with eligible record")
        void generateCertificate_Success() throws Exception {
            CertificateGenerationRequestDto request = new CertificateGenerationRequestDto(recordIdRabiesOk, "AHC-GEN-SUCCESS-" + System.currentTimeMillis());

            mockMvc.perform(post("/api/certificates")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", is(notNullValue())))
                    .andExpect(jsonPath("$.certificateNumber", is(request.certificateNumber())))
                    .andExpect(jsonPath("$.pet.id", is(petIdEligible.intValue())))
                    .andExpect(jsonPath("$.originatingRecord.id", is(recordIdRabiesOk.intValue())))
                    .andExpect(jsonPath("$.generatorVet.id", is(vetId.intValue())))
                    .andExpect(jsonPath("$.issuingClinic.id", is(clinic1Id.intValue())))
                    .andExpect(jsonPath("$.payload", is(notNullValue())))
                    .andExpect(jsonPath("$.hash", is(notNullValue())))
                    .andExpect(jsonPath("$.vetSignature", is(notNullValue())))
                    .andExpect(jsonPath("$.clinicSignature", is(notNullValue())));
        }

        @Test
        @DisplayName("should return 400 Bad Request if record is not suitable (e.g., not signed, wrong type)")
        void generateCertificate_BadRequest_RecordNotSuitable() throws Exception {
            CertificateGenerationRequestDto requestUnsigned = new CertificateGenerationRequestDto(recordIdRabiesUnsigned, "AHC-FAIL-UNSIGNED");
            MvcResult resUnsigned = mockMvc.perform(post("/api/certificates")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestUnsigned)))
                    .andExpect(status().isBadRequest())
                    .andReturn();
            assertThat(resUnsigned.getResponse().getContentAsString()).contains("Source record", "is not signed");

            CertificateGenerationRequestDto requestWrongType = new CertificateGenerationRequestDto(recordIdIllness, "AHC-FAIL-TYPE");
            MvcResult resWrongType = mockMvc.perform(post("/api/certificates")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestWrongType)))
                    .andExpect(status().isBadRequest())
                    .andReturn();
            assertThat(resWrongType.getResponse().getContentAsString()).contains("only supported for signed VACCINE records");

        }

        @Test
        @DisplayName("should return 409 Conflict if certificate number already exists")
        void generateCertificate_Conflict_NumberExists() throws Exception {
            String existingCertNumber = "AHC-DUPLICATE-" + System.currentTimeMillis();
            CertificateGenerationRequestDto firstRequest = new CertificateGenerationRequestDto(recordIdRabiesOk, existingCertNumber);
            mockMvc.perform(post("/api/certificates").header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(firstRequest))).andExpect(status().isCreated());

            entityManager.flush(); entityManager.clear();

            CertificateGenerationRequestDto secondRequest = new CertificateGenerationRequestDto(recordIdSecondVaccineOk, existingCertNumber);
            mockMvc.perform(post("/api/certificates")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(secondRequest)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message", containsString("is already in use")));
        }

        @Test
        @DisplayName("should return 409 Conflict if certificate already exists for the record")
        void generateCertificate_Conflict_CertForRecordExists() throws Exception {
            CertificateGenerationRequestDto firstRequest = new CertificateGenerationRequestDto(recordIdRabiesOk, "AHC-REC-DUP1-" + System.currentTimeMillis());
            mockMvc.perform(post("/api/certificates").header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(firstRequest))).andExpect(status().isCreated());

            entityManager.flush(); entityManager.clear();

            CertificateGenerationRequestDto secondRequest = new CertificateGenerationRequestDto(recordIdRabiesOk, "AHC-REC-DUP2-" + System.currentTimeMillis()); // Same record, different number
            mockMvc.perform(post("/api/certificates")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(secondRequest)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message", containsString("A certificate already exists for record")));
        }

        @Test
        @DisplayName("should return 403 Forbidden if called by non-Vet (Owner)")
        void generateCertificate_Forbidden_Owner() throws Exception {
            CertificateGenerationRequestDto request = new CertificateGenerationRequestDto(recordIdRabiesOk, "AHC-OWNER-FAIL");
            mockMvc.perform(post("/api/certificates")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 404 Not Found if Record ID does not exist")
        void generateCertificate_NotFound_Record() throws Exception {
            CertificateGenerationRequestDto request = new CertificateGenerationRequestDto(9999L, "AHC-REC-NF");
            mockMvc.perform(post("/api/certificates")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 400 Bad Request if required DTO fields missing")
        void generateCertificate_BadRequest_MissingDtoFields() throws Exception {
            CertificateGenerationRequestDto missingRecordId = new CertificateGenerationRequestDto(null, "AHC-VALID");
            mockMvc.perform(post("/api/certificates").header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(missingRecordId)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message.recordId", containsString("cannot be null")));

            CertificateGenerationRequestDto missingCertNumber = new CertificateGenerationRequestDto(recordIdRabiesOk, "");
            mockMvc.perform(post("/api/certificates").header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(missingCertNumber)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message.certificateNumber", containsString("cannot be blank")));
        }
    }


    /**
     * --- Tests for GET /api/certificates (List Certificates) ---
     */
    @Nested
    @DisplayName("GET /api/certificates")
    class ListCertificatesTests {

        private Long certId1, certId2;

        @BeforeEach
        void listSetup() throws Exception {
            // Generate two certificates for the eligible pet
            CertificateGenerationRequestDto req1 = new CertificateGenerationRequestDto(recordIdRabiesOk, "LIST-CERT-1-" + System.currentTimeMillis());
            MvcResult res1 = mockMvc.perform(post("/api/certificates").header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req1))).andExpect(status().isCreated()).andReturn();
            certId1 = objectMapper.readValue(res1.getResponse().getContentAsString(), CertificateViewDto.class).id();

            // Need another valid record for a second certificate
            VaccineCreateDto vac2 = new VaccineCreateDto("FluVacList", 1, "LabListF", "BatchListF" + System.currentTimeMillis());
            RecordCreateDto rec2 = new RecordCreateDto(petIdEligible, RecordType.VACCINE, "Flu vaccine", vac2);
            MvcResult recRes2 = mockMvc.perform(post("/api/records?sign=true").header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(rec2))).andExpect(status().isCreated()).andReturn();
            Long recordIdFlu = extractRecordIdFromResult(recRes2);

            CertificateGenerationRequestDto req2 = new CertificateGenerationRequestDto(recordIdFlu, "LIST-CERT-2-" + System.currentTimeMillis());
            MvcResult res2 = mockMvc.perform(post("/api/certificates").header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req2))).andExpect(status().isCreated()).andReturn();
            certId2 = objectMapper.readValue(res2.getResponse().getContentAsString(), CertificateViewDto.class).id();
            entityManager.flush(); entityManager.clear();
        }

        @Test
        @DisplayName("should return list of certificates when called by Owner")
        void listCertificates_Success_Owner() throws Exception {
            mockMvc.perform(get("/api/certificates")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                            .param("petId", petIdEligible.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[*].id", containsInAnyOrder(certId1.intValue(), certId2.intValue())));
        }

        @Test
        @DisplayName("should return list of certificates when called by associated Vet")
        void listCertificates_Success_Vet() throws Exception {
            mockMvc.perform(get("/api/certificates")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken)
                            .param("petId", petIdEligible.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)));
        }

        @Test
        @DisplayName("should return empty list if pet has no certificates")
        void listCertificates_Success_NoCerts() throws Exception {
            mockMvc.perform(get("/api/certificates")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                            .param("petId", petIdNotEligible.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("should return 403 Forbidden if called by unauthorized user")
        void listCertificates_Forbidden() throws Exception {
            mockMvc.perform(get("/api/certificates")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherOwnerToken)
                            .param("petId", petIdEligible.toString()))
                    .andExpect(status().isForbidden());

            mockMvc.perform(get("/api/certificates")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherVetToken)
                            .param("petId", petIdEligible.toString()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 404 Not Found if petId does not exist")
        void listCertificates_NotFound_Pet() throws Exception {
            mockMvc.perform(get("/api/certificates")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                            .param("petId", "9999"))
                    .andExpect(status().isNotFound());
        }
    }


    /**
     * --- Tests for GET /api/certificates/{certificateId} ---
     */
    @Nested
    @DisplayName("GET /api/certificates/{certificateId}")
    class GetCertificateByIdTests {

        private Long certificateIdToGet;

        @BeforeEach
        void getByIdSetup() throws Exception {
            CertificateGenerationRequestDto req = new CertificateGenerationRequestDto(recordIdRabiesOk, "GET-CERT-" + System.currentTimeMillis());
            MvcResult res = mockMvc.perform(post("/api/certificates").header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req))).andExpect(status().isCreated()).andReturn();
            certificateIdToGet = objectMapper.readValue(res.getResponse().getContentAsString(), CertificateViewDto.class).id();
            entityManager.flush(); entityManager.clear();
        }

        @Test
        @DisplayName("should return certificate when called by Owner")
        void getCertificateById_Success_Owner() throws Exception {
            mockMvc.perform(get("/api/certificates/{certificateId}", certificateIdToGet)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(certificateIdToGet.intValue())));
        }

        @Test
        @DisplayName("should return certificate when called by associated Vet")
        void getCertificateById_Success_Vet() throws Exception {
            mockMvc.perform(get("/api/certificates/{certificateId}", certificateIdToGet)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(certificateIdToGet.intValue())));
        }

        @Test
        @DisplayName("should return 403 Forbidden when called by unauthorized user")
        void getCertificateById_Forbidden() throws Exception {
            mockMvc.perform(get("/api/certificates/{certificateId}", certificateIdToGet)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherOwnerToken))
                    .andExpect(status().isForbidden());
            mockMvc.perform(get("/api/certificates/{certificateId}", certificateIdToGet)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherVetToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 404 Not Found if certificate ID does not exist")
        void getCertificateById_NotFound() throws Exception {
            mockMvc.perform(get("/api/certificates/{certificateId}", 9999L)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                    .andExpect(status().isNotFound());
        }
    }

    /**
     * --- Tests for GET /api/certificates/{certificateId}/qr-data ---
     */
    @Nested
    @DisplayName("GET /api/certificates/{certificateId}/qr-data")
    class GetCertificateQrDataTests {

        private Long certificateIdForQr;

        @BeforeEach
        void getQrSetup() throws Exception {
            CertificateGenerationRequestDto req = new CertificateGenerationRequestDto(recordIdRabiesOk, "QR-CERT-" + System.currentTimeMillis());
            MvcResult res = mockMvc.perform(post("/api/certificates").header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req))).andExpect(status().isCreated()).andReturn();
            certificateIdForQr = objectMapper.readValue(res.getResponse().getContentAsString(), CertificateViewDto.class).id();
            entityManager.flush(); entityManager.clear();
        }

        @Test
        @DisplayName("should return Base45 string when called by Owner")
        void getQrData_Success_Owner() throws Exception {
            mockMvc.perform(get("/api/certificates/{certificateId}/qr-data", certificateIdForQr)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                    .andExpect(content().string(startsWith("HC1:")));
        }

        @Test
        @DisplayName("should return Base45 string when called by associated Vet")
        void getQrData_Success_Vet() throws Exception {
            mockMvc.perform(get("/api/certificates/{certificateId}/qr-data", certificateIdForQr)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                    .andExpect(content().string(startsWith("HC1:")));
        }

        @Test
        @DisplayName("should return 403 Forbidden when called by unauthorized user")
        void getQrData_Forbidden() throws Exception {
            mockMvc.perform(get("/api/certificates/{certificateId}/qr-data", certificateIdForQr)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherOwnerToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 404 Not Found if certificate ID does not exist")
        void getQrData_NotFound() throws Exception {
            mockMvc.perform(get("/api/certificates/{certificateId}/qr-data", 9999L)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 401 Unauthorized if no token provided")
        void getQrData_Unauthorized() throws Exception {
            mockMvc.perform(get("/api/certificates/{certificateId}/qr-data", certificateIdForQr))
                    .andExpect(status().isUnauthorized());
        }
    }
}