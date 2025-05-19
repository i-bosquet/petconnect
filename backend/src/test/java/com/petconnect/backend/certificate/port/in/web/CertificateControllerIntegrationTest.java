package com.petconnect.backend.certificate.port.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petconnect.backend.certificate.application.dto.CertificateGenerationRequestDto;
import com.petconnect.backend.certificate.application.dto.CertificateViewDto;
import com.petconnect.backend.pet.application.dto.PetActivationDto;
import com.petconnect.backend.pet.application.dto.PetRegistrationDto;
import com.petconnect.backend.pet.domain.model.Gender;
import com.petconnect.backend.pet.domain.model.Pet;
import com.petconnect.backend.pet.domain.model.Specie;
import com.petconnect.backend.pet.domain.repository.PetRepository;
import com.petconnect.backend.record.application.dto.RecordCreateDto;
import com.petconnect.backend.record.application.dto.VaccineCreateDto;
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
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.mock.web.MockMultipartFile;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;

import static com.petconnect.backend.util.IntegrationTestUtils.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@link CertificateController}.
 * Uses PostgresSQL (Docker), security filters, and transactional rollback.
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

    private String ownerToken;
    private String vetToken;
    private String otherOwnerToken;
    private String otherVetToken;

    private Long vetId;
    private Long petIdEligible;
    private Long petIdNotEligible;
    private Long recordIdRabiesOk;
    private final Long clinic1Id = 1L;

    @BeforeEach
    void setUp() throws Exception {
        String adminAuthToken = obtainJwtToken(mockMvc, objectMapper, new AuthLoginRequestDto("admin_london", "password123"));
        otherVetToken = obtainJwtToken(mockMvc, objectMapper, new AuthLoginRequestDto("admin_barcelona", "password123"));

        String clinic1RelativePrivateKeyPath = "clinics/lon_pri_key.pem";
        String clinic1RelativePublicKeyPath = "clinics/lon_pub_key.pem";

        Path testBaseDir = Paths.get("").toAbsolutePath().resolve("target");
        Path testPrivateKeysBase = testBaseDir.resolve("test-keys_private");
        Path testPublicKeysBase = testBaseDir.resolve("test-keys_public");

        Files.createDirectories(testPrivateKeysBase.resolve("clinics"));
        Files.createDirectories(testPublicKeysBase.resolve("clinics"));

        Path targetClinic1PrivateKey = testPrivateKeysBase.resolve(clinic1RelativePrivateKeyPath);
        Path sourceClinic1PrivateKey = new ClassPathResource("keys_for_test/clinic_private_key.pem").getFile().toPath();
        Files.copy(sourceClinic1PrivateKey, targetClinic1PrivateKey, StandardCopyOption.REPLACE_EXISTING);

        Path targetClinic1PublicKey = testPublicKeysBase.resolve(clinic1RelativePublicKeyPath);
        Path sourceClinic1PublicKey = new ClassPathResource("keys_for_test/clinic_public_key.pem").getFile().toPath();
        Files.copy(sourceClinic1PublicKey, targetClinic1PublicKey, StandardCopyOption.REPLACE_EXISTING);

        String ownerUsername = "cert_owner_" + System.currentTimeMillis();
        OwnerRegistrationDto ownerRegDto = new OwnerRegistrationDto(
                ownerUsername, ownerUsername + "@test.com", "password123", "CertOwnerPhone");
        userRepository.findByUsername(ownerUsername).ifPresent(user -> {
            petRepository.findByOwnerId(user.getId()).forEach(pet -> {
                recordRepository.deleteAllByPetId(pet.getId());
                petRepository.delete(pet);
            });
            userRepository.delete(user);
            entityManager.flush();});
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(ownerRegDto))).andExpect(status().isCreated());
        userRepository.findByUsername(ownerUsername).get().getId();
        ownerToken = obtainJwtToken(mockMvc, objectMapper, new AuthLoginRequestDto(ownerRegDto.username(), ownerRegDto.password()));

        String vetUsername = "cert_vet_" + System.currentTimeMillis();
        String vetLoginPassword = "password123";
        ClinicStaffCreationDto vetCreationDto = new ClinicStaffCreationDto(
                vetUsername, vetUsername + "@test.com", vetLoginPassword, "CertSetup", "VetSetup", RoleEnum.VET, "VET-CERT" + System.currentTimeMillis());
        userRepository.findByUsername(vetUsername).ifPresent(user -> {
            userRepository.delete(user);
            entityManager.flush();});

        MockMultipartFile dtoPartVet = new MockMultipartFile("dto", "", MediaType.APPLICATION_JSON_VALUE, objectMapper.writeValueAsBytes(vetCreationDto));
        MockMultipartFile publicKeyFileVet = new MockMultipartFile(
                "publicKeyFile", "vet_test_pub.pem", "application/x-pem-file", new ClassPathResource("keys_for_test/vet_public_key.pem").getInputStream());
        MockMultipartFile privateKeyFileVet = new MockMultipartFile(
                "privateKeyFile", "vet_test_priv_enc.pem", "application/x-pem-file", new ClassPathResource("keys_for_test/vet_private_key.pem").getInputStream());

        MvcResult vetRegResult = mockMvc.perform(multipart("/api/staff").file(dtoPartVet).file(publicKeyFileVet).file(privateKeyFileVet).header(HttpHeaders.AUTHORIZATION, "Bearer " + adminAuthToken)).andExpect(status().isCreated()).andReturn();
        ClinicStaffProfileDto vetDto = objectMapper.readValue(vetRegResult.getResponse().getContentAsString(), ClinicStaffProfileDto.class);
        vetId = vetDto.id();
        vetToken = obtainJwtToken(mockMvc, objectMapper, new AuthLoginRequestDto(vetCreationDto.username(), vetCreationDto.password()));

        Long labradorBreedId = 25L;
        PetRegistrationDto petEligibleRegDto = new PetRegistrationDto(
                "EligibleCertPet", Specie.DOG, LocalDate.now().minusYears(2), labradorBreedId, null, "Brown", Gender.MALE, "100000000000001");
        MockMultipartFile petDtoPartEligible = new MockMultipartFile(
                "dto", "", MediaType.APPLICATION_JSON_VALUE, objectMapper.writeValueAsBytes(petEligibleRegDto));
        MvcResult petEligibleRes = mockMvc.perform(
                multipart("/api/pets").file(petDtoPartEligible).header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)).andExpect(status().isCreated()).andReturn();
        petIdEligible = extractPetIdFromResult(objectMapper, petEligibleRes);

        PetRegistrationDto petNotEligibleRegDto = new PetRegistrationDto(
                "NotEligibleCertPet", Specie.CAT, LocalDate.now().minusYears(1), 45L, null, "Black", Gender.FEMALE, "100000000000002");
        MockMultipartFile petDtoPartNotEligible = new MockMultipartFile(
                "dto", "", MediaType.APPLICATION_JSON_VALUE, objectMapper.writeValueAsBytes(petNotEligibleRegDto));
        MvcResult petNotEligibleRes = mockMvc.perform(multipart(
                "/api/pets").file(petDtoPartNotEligible).header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)).andExpect(status().isCreated()).andReturn();
        petIdNotEligible = extractPetIdFromResult(objectMapper, petNotEligibleRes);

        mockMvc.perform(post("/api/pets/{petId}/associate-clinic/{clinicId}", petIdEligible, clinic1Id).header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)).andExpect(status().isNoContent());
        entityManager.flush(); entityManager.clear();

        Pet petEligibleEntity = petRepository.findById(petIdEligible).orElseThrow();
        PetActivationDto activationDto = new PetActivationDto(petEligibleEntity.getColor(), petEligibleEntity.getGender(), petEligibleEntity.getBirthDate(), petEligibleEntity.getMicrochip(), petEligibleEntity.getBreed().getId());
        mockMvc.perform(put("/api/pets/{petId}/activate", petIdEligible).header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(activationDto))).andExpect(status().isOk());
        entityManager.flush(); entityManager.clear();

        Pet reloadedPetEligible = petRepository.findById(petIdEligible).orElseThrow();
        assertThat(reloadedPetEligible.getAssociatedVets()).anyMatch(v -> v.getId().equals(vetId));

        VaccineCreateDto rabiesVacDto = new VaccineCreateDto("Rabies TestCert", 1, "RabiesLab", "BatchRABCERT" + System.currentTimeMillis(), true);
        String vetTestPemPassword = "1234";
        RecordCreateDto rabiesRecDto = new RecordCreateDto(petIdEligible, RecordType.VACCINE, "Rabies vaccine (AHC test)", rabiesVacDto, vetTestPemPassword);
        MvcResult rabiesRes = mockMvc.perform(post("/api/records").header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(rabiesRecDto))).andExpect(status().isCreated()).andReturn();
        recordIdRabiesOk = extractRecordIdFromResult(objectMapper, rabiesRes);

        RecordCreateDto annualRecDto = new RecordCreateDto(petIdEligible, RecordType.ANNUAL_CHECK, "Annual checkup (AHC test)", null, vetTestPemPassword);
        mockMvc.perform(post("/api/records").header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(annualRecDto))).andExpect(status().isCreated());

        entityManager.flush(); entityManager.clear();

        String otherOwnerUsername = "cert_other_owner_" + System.currentTimeMillis();
        OwnerRegistrationDto otherOwnerRegDto = new OwnerRegistrationDto(otherOwnerUsername, otherOwnerUsername + "@test.com", "password123", "OtherPhone");
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(otherOwnerRegDto))).andExpect(status().isCreated());
        otherOwnerToken = obtainJwtToken(mockMvc, objectMapper, new AuthLoginRequestDto(otherOwnerRegDto.username(), otherOwnerRegDto.password()));

        PetRegistrationDto otherPetRegDto = new PetRegistrationDto("OtherOwnersCertPet", Specie.FERRET, LocalDate.now().minusMonths(6), 47L, null, "White", Gender.FEMALE, "100000000000003");
        MockMultipartFile otherPetDtoPart = new MockMultipartFile("dto", "", MediaType.APPLICATION_JSON_VALUE, objectMapper.writeValueAsString(otherPetRegDto).getBytes());
        mockMvc.perform(multipart("/api/pets").file(otherPetDtoPart).header(HttpHeaders.AUTHORIZATION, "Bearer " + otherOwnerToken)).andExpect(status().isCreated());
    }

    /**
     * --- Tests for POST /api/certificates (Generate Certificate) ---
     */
    @Nested
    @DisplayName("POST /api/certificates (Generate Certificate Tests)")
    class GenerateCertificateTests {
        private Long recordIdSecondVaccineOk;

        @BeforeEach
        void setup() throws Exception {
            VaccineCreateDto vacDto2 = new VaccineCreateDto("Distemper", 1, "DistLab", "BatchDIST" + System.currentTimeMillis(), true);
            RecordCreateDto rec2 = new RecordCreateDto(petIdEligible, RecordType.VACCINE, "Distemper vaccine", vacDto2, "1234");
            MvcResult recRes2 = mockMvc.perform(post("/api/records")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(rec2))).andDo(print())
                    .andExpect(status().isCreated()).andReturn();
            recordIdSecondVaccineOk = extractRecordIdFromResult(objectMapper,recRes2);
            assertThat(recordIdSecondVaccineOk).isNotNull();
            entityManager.flush();entityManager.clear();
        }

        @Test
        @DisplayName("should return 201 Created and CertificateViewDto when called by authorized Vet with eligible record")
        void generateCertificate_Success() throws Exception {
            CertificateGenerationRequestDto request = new CertificateGenerationRequestDto(
                    petIdEligible, "AHC-GEN-SUCCESS-" + System.currentTimeMillis(), "1234", "1234");

            mockMvc.perform(post("/api/certificates")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))).andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", is(notNullValue())))
                    .andExpect(jsonPath("$.certificateNumber", is(request.certificateNumber())))
                    .andExpect(jsonPath("$.pet.id", is(petIdEligible.intValue())))
                    .andExpect(jsonPath("$.originatingRecord.id", is(recordIdSecondVaccineOk.intValue())))
                    .andExpect(jsonPath("$.generatorVet.id", is(vetId.intValue())))
                    .andExpect(jsonPath("$.generatorVet.clinicId", is(clinic1Id.intValue())))
                    .andExpect(jsonPath("$.payload", is(notNullValue())))
                    .andExpect(jsonPath("$.hash", is(notNullValue())))
                    .andExpect(jsonPath("$.vetSignature", is(notNullValue())))
                    .andExpect(jsonPath("$.clinicSignature", is(notNullValue())));
        }

        @Test
        @DisplayName("should return 400 Bad Request when source record is not suitable (unsigned or wrong type)")
        void generateCertificate_BadRequest_RecordNotSuitable() throws Exception {
            CertificateGenerationRequestDto requestUnsignedRabiesPet = new CertificateGenerationRequestDto(petIdNotEligible, "AHC-FAIL-UNSIGNED", "1234", "1234");
            mockMvc.perform(post("/api/certificates")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestUnsignedRabiesPet))).andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("No valid, signed, and current Rabies vaccine record found")));
        }

        @Test
        @DisplayName("should return 409 Conflict when certificate number already exists")
        void generateCertificate_Conflict_NumberExists() throws Exception {
            String existingCertNumber = "AHC-DUPLICATE-" + System.currentTimeMillis();
            CertificateGenerationRequestDto firstRequest = new CertificateGenerationRequestDto(petIdEligible, existingCertNumber, "1234", "1234");

            mockMvc.perform(post("/api/certificates")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(firstRequest)))
                    .andExpect(status().isCreated());

            entityManager.flush(); entityManager.clear();

            VaccineCreateDto rabiesVacDto2 = new VaccineCreateDto(
                    "Rabies2", 1, "RabiesLab2", "BatchRAB2" + System.currentTimeMillis(), true);
            RecordCreateDto rabiesRecDto2 = new RecordCreateDto(
                    petIdEligible, RecordType.VACCINE, "Second Rabies vaccine", rabiesVacDto2, "1234");
            MvcResult rabiesRes2 = mockMvc.perform(post("/api/records")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(rabiesRecDto2)))
                    .andExpect(status().isCreated()).andReturn();
            extractRecordIdFromResult(objectMapper,rabiesRes2);
            entityManager.flush();entityManager.clear();

            CertificateGenerationRequestDto secondRequest = new CertificateGenerationRequestDto(
                    petIdEligible, existingCertNumber, "1234", "1234");
            mockMvc.perform(post("/api/certificates")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(secondRequest))).andDo(print())
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message", containsString("'" + existingCertNumber + "' is already in use")));
        }

        @Test
        @DisplayName("should return 409 Conflict when certificate already exists for the source record")
        void generateCertificate_Conflict_CertForRecordExists() throws Exception {
            CertificateGenerationRequestDto firstRequest = new CertificateGenerationRequestDto(
                    petIdEligible, "AHC-REC-DUP1-" + System.currentTimeMillis(), "1234", "1234");
            mockMvc.perform(post("/api/certificates")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(firstRequest)))
                    .andExpect(status().isCreated());

            entityManager.flush(); entityManager.clear();

            CertificateGenerationRequestDto secondRequest = new CertificateGenerationRequestDto(
                    petIdEligible, "AHC-REC-DUP2-" + System.currentTimeMillis(), "1234", "1234");
            mockMvc.perform(post("/api/certificates")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(secondRequest))).andDo(print())
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message", containsString("A certificate already exists for record")));
        }

        @Test
        @DisplayName("should return 403 Forbidden when called by non-Vet")
        void generateCertificate_Forbidden_Owner() throws Exception {
            CertificateGenerationRequestDto request = new CertificateGenerationRequestDto(
                    recordIdRabiesOk, "AHC-OWNER-FAIL", "1234", "1234");
            mockMvc.perform(post("/api/certificates")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))).andDo(print())
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 404 Not Found when source Record ID does not exist")
        void generateCertificate_NotFound_Record() throws Exception {
            CertificateGenerationRequestDto request = new CertificateGenerationRequestDto(
                    9999L, "AHC-REC-NF", "1234", "1234");
            mockMvc.perform(post("/api/certificates")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))).andDo(print())
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 404 Not Found when source Record ID does not exist")
        void generateCertificate_BadRequest_MissingDtoFields() throws Exception {
            CertificateGenerationRequestDto missingPetId = new CertificateGenerationRequestDto(
                    null, "AHC-VALID", "1234", "1234");
            mockMvc.perform(post("/api/certificates")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(missingPetId)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message.petId", containsString("Pet ID cannot be null")));

            CertificateGenerationRequestDto missingCertNumber = new CertificateGenerationRequestDto(petIdEligible, "", "1234", "1234");
            mockMvc.perform(post("/api/certificates")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(missingCertNumber))).andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message.certificateNumber", containsString("cannot be blank")));
        }
    }


    /**
     * --- Tests for GET /api/certificates (List Certificates) ---
     */
    @Nested
    @DisplayName("GET /api/certificates (List Certificates Tests)")
    class ListCertificatesTests {

        private Long certId1, certId2;

        @BeforeEach
        void listSetup() throws Exception {
            // Generate two certificates for the eligible pet
            CertificateGenerationRequestDto req1 = new CertificateGenerationRequestDto(
                    petIdEligible, "LIST-CERT-1-" + System.currentTimeMillis(), "1234", "1234");
            MvcResult res1 = mockMvc.perform(post("/api/certificates")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req1)))
                    .andExpect(status().isCreated())
                    .andReturn();
            certId1 = objectMapper.readValue(res1.getResponse().getContentAsString(), CertificateViewDto.class).id();

            VaccineCreateDto vac2 = new VaccineCreateDto("FluVacList", 1, "LabListF", "BatchListF" + System.currentTimeMillis(), true);
            RecordCreateDto rec2 = new RecordCreateDto(petIdEligible, RecordType.VACCINE, "Second vaccine", vac2, "1234");
            MvcResult recRes2 = mockMvc.perform(post("/api/records")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(rec2)))
                    .andExpect(status().isCreated()).andReturn();
            extractRecordIdFromResult(objectMapper,recRes2);

            CertificateGenerationRequestDto req2 = new CertificateGenerationRequestDto(
                    petIdEligible, "LIST-CERT-2-" + System.currentTimeMillis(), "1234", "1234");
            MvcResult res2 = mockMvc.perform(post("/api/certificates")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req2))).andDo(print())
                    .andExpect(status().isCreated())
                    .andReturn();
            certId2 = objectMapper.readValue(res2.getResponse().getContentAsString(), CertificateViewDto.class).id();
            entityManager.flush(); entityManager.clear();
        }

        @Test
        @DisplayName("should return 200 OK with list of certificates when called by Owner")
        void listCertificates_Success_Owner() throws Exception {
            mockMvc.perform(get("/api/certificates")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                            .param("petId", petIdEligible.toString())).andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[*].id", containsInAnyOrder(certId1.intValue(), certId2.intValue())));
        }

        @Test
        @DisplayName("should return 200 OK with list of certificates when called by associated Vet")
        void listCertificates_Success_Vet() throws Exception {
            mockMvc.perform(get("/api/certificates")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken)
                            .param("petId", petIdEligible.toString())).andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)));
        }

        @Test
        @DisplayName("should return 200 OK with empty list when pet has no certificates")
        void listCertificates_Success_NoCerts() throws Exception {
            mockMvc.perform(get("/api/certificates")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                            .param("petId", petIdNotEligible.toString())).andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("should return 403 Forbidden when called by unauthorized user")
        void listCertificates_Forbidden() throws Exception {
            mockMvc.perform(get("/api/certificates")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherOwnerToken)
                            .param("petId", petIdEligible.toString()))
                    .andExpect(status().isForbidden());

            mockMvc.perform(get("/api/certificates")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherVetToken)
                            .param("petId", petIdEligible.toString())).andDo(print())
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 404 Not Found when Pet ID does not exist")
        void listCertificates_NotFound_Pet() throws Exception {
            mockMvc.perform(get("/api/certificates")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                            .param("petId", "9999")).andDo(print())
                    .andExpect(status().isNotFound());
        }
    }


    /**
     * --- Tests for GET /api/certificates/{certificateId} ---
     */
    @Nested
    @DisplayName("GET /api/certificates/{certificateId}  (Get Certificate By ID Tests)")
    class GetCertificateByIdTests {

        private Long certificateIdToGet;

        @BeforeEach
        void getByIdSetup() throws Exception {
            CertificateGenerationRequestDto req = new CertificateGenerationRequestDto(
                    petIdEligible, "GET-CERT-" + System.currentTimeMillis(),"1234","1234" );
            MvcResult res = mockMvc.perform(post("/api/certificates")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req))).andDo(print())
                    .andExpect(status().isCreated())
                    .andReturn();
            certificateIdToGet = objectMapper.readValue(res.getResponse().getContentAsString(), CertificateViewDto.class).id();
            assertThat(certificateIdToGet).isNotNull();
            entityManager.flush(); entityManager.clear();
        }

        @Test
        @DisplayName("should return 200 OK with certificate details when called by Owner")
        void getCertificateById_Success_Owner() throws Exception {
            mockMvc.perform(get("/api/certificates/{certificateId}", certificateIdToGet)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)).andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(certificateIdToGet.intValue())));
        }

        @Test
        @DisplayName("should return 200 OK with certificate details when called by associated Vet")
        void getCertificateById_Success_Vet() throws Exception {
            mockMvc.perform(get("/api/certificates/{certificateId}", certificateIdToGet)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken)).andDo(print())
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
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherVetToken)).andDo(print())
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 404 Not Found when certificate ID does not exist")
        void getCertificateById_NotFound() throws Exception {
            mockMvc.perform(get("/api/certificates/{certificateId}", 9999L)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)).andDo(print())
                    .andExpect(status().isNotFound());
        }
    }

    /**
     * --- Tests for GET /api/certificates/{certificateId}/qr-data ---
     */
    @Nested
    @DisplayName("GET /api/certificates/{certificateId}/qr-data  (Get Certificate QR Data Tests)")
    class GetCertificateQrDataTests {

        private Long certificateIdForQr;

        @BeforeEach
        void getQrSetup() throws Exception {
            CertificateGenerationRequestDto req = new CertificateGenerationRequestDto(
                    petIdEligible, "QR-CERT-" + System.currentTimeMillis(), "1234", "1234");
            MvcResult res = mockMvc.perform(post("/api/certificates")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req))).andDo(print())
                    .andExpect(status().isCreated())
                    .andReturn();
            certificateIdForQr = objectMapper.readValue(res.getResponse().getContentAsString(), CertificateViewDto.class).id();
            assertThat(certificateIdForQr).isNotNull();
            entityManager.flush(); entityManager.clear();
        }

        @Test
        @DisplayName("should return 200 OK with Base45 string when called by Owner")
        void getQrData_Success_Owner() throws Exception {
            mockMvc.perform(get("/api/certificates/{certificateId}/qr-data", certificateIdForQr)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)).andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                    .andExpect(content().string(startsWith("HC1:")));
        }

        @Test
        @DisplayName("should return 200 OK with Base45 string when called by associated Vet")
        void getQrData_Success_Vet() throws Exception {
            mockMvc.perform(get("/api/certificates/{certificateId}/qr-data", certificateIdForQr)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken)).andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                    .andExpect(content().string(startsWith("HC1:")));
        }

        @Test
        @DisplayName("should return 403 Forbidden when called by unauthorized user")
        void getQrData_Forbidden() throws Exception {
            mockMvc.perform(get("/api/certificates/{certificateId}/qr-data", certificateIdForQr)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherOwnerToken)).andDo(print())
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 404 Not Found when certificate ID does not exist")
        void getQrData_NotFound() throws Exception {
            mockMvc.perform(get("/api/certificates/{certificateId}/qr-data", 9999L)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)).andDo(print())
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 401 Unauthorized when no authentication token is provided")
        void getQrData_Unauthorized() throws Exception {
            mockMvc.perform(get("/api/certificates/{certificateId}/qr-data", certificateIdForQr)).andDo(print())
                    .andExpect(status().isUnauthorized());
        }
    }
}