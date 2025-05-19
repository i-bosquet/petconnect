package com.petconnect.backend.pet.port.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petconnect.backend.pet.application.dto.*;
import com.petconnect.backend.pet.domain.model.*;
import com.petconnect.backend.pet.domain.repository.PetRepository;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;

import static com.petconnect.backend.util.IntegrationTestUtils.extractPetIdFromResult;
import static com.petconnect.backend.util.IntegrationTestUtils.obtainJwtToken;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@link PetController}.
 * Uses PostgresSQL (Docker), security filters, transactional rollback.
 * Verifies pet registration, retrieval, updates, activation, associations, etc.
 *
 * @author ibosquet (Generated based on UserControllerIntegrationTest structure)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class PetControllerIntegrationTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private PetRepository petRepository;
    @Autowired private EntityManager entityManager;

    // --- Tokens ---
    private String ownerToken;
    private String adminToken;
    private String vetToken;
    private String otherAdminToken;


    // --- IDs ---
    private Long ownerId;
    private Long vetId;
    private final Long clinicId = 1L;
    private final Long labradorBreedId = 25L;


    /**
     * Set up initial users, pets, and get tokens.
     */
    @BeforeEach
    void setUp() throws Exception {

        adminToken = obtainJwtToken(mockMvc, objectMapper, new AuthLoginRequestDto("admin_london", "password123"));
        otherAdminToken = obtainJwtToken(mockMvc, objectMapper, new AuthLoginRequestDto("admin_barcelona", "password123"));

        String ownerUsername = "pet_ctrl_owner_" + System.currentTimeMillis();
        String ownerEmail = ownerUsername + "@test.com";
        OwnerRegistrationDto ownerReg = new OwnerRegistrationDto(ownerUsername, ownerEmail, "password123", "777-888-999");

        userRepository.findByUsername(ownerUsername).ifPresent(userRepository::delete);
        entityManager.flush();

        MvcResult ownerRegResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ownerReg)))
                .andExpect(status().isCreated())
                .andReturn();
        OwnerProfileDto ownerDto = objectMapper.readValue(ownerRegResult.getResponse().getContentAsString(), OwnerProfileDto.class);
        ownerId = ownerDto.id();
        ownerToken = obtainJwtToken(mockMvc, objectMapper, new AuthLoginRequestDto(ownerReg.username(), ownerReg.password()));

        String vetUsername = "pet_ctrl_vet_" + System.currentTimeMillis();
        String vetEmail = vetUsername + "@test.com";
        ClinicStaffCreationDto vetRegDto  = new ClinicStaffCreationDto(
                vetUsername, vetEmail, "password123", "Test", "Vet", RoleEnum.VET,
                "VELICA" + System.currentTimeMillis());

        String vetRegDtoJson = objectMapper.writeValueAsString(vetRegDto);
        MockMultipartFile vetDtoPart = new MockMultipartFile("dto", "", MediaType.APPLICATION_JSON_VALUE, vetRegDtoJson.getBytes());

        MockMultipartFile vetPublicKeyFile = new MockMultipartFile("publicKeyFile", "vet_pub.pem", MediaType.TEXT_PLAIN_VALUE, "fake public key".getBytes());
        MockMultipartFile vetPrivateKeyFile = new MockMultipartFile("privateKeyFile", "vet_pri_enc.pem", MediaType.TEXT_PLAIN_VALUE, "fake private key".getBytes());

        userRepository.findByUsername(vetUsername).ifPresent(userRepository::delete);
        entityManager.flush();

        MvcResult vetRegResult = mockMvc.perform(multipart("/api/staff")
                        .file(vetDtoPart)
                        .file(vetPublicKeyFile)
                        .file(vetPrivateKeyFile)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isCreated())
                .andReturn();
        ClinicStaffProfileDto vetDto = objectMapper.readValue(vetRegResult.getResponse().getContentAsString(), ClinicStaffProfileDto.class);
        vetId = vetDto.id();
        vetToken = obtainJwtToken(mockMvc, objectMapper, new AuthLoginRequestDto(vetRegDto.username(), vetRegDto.password()));

        assertThat(ownerToken).isNotNull();
        assertThat(adminToken).isNotNull();
        assertThat(vetToken).isNotNull();
        assertThat(otherAdminToken).isNotNull();
    }

    /**
     * --- Tests for POST /api/pets (Register Pet) ---
     */
    @Nested
    @DisplayName("POST /api/pets (Register Pet Tests 'Owner')")
    class RegisterPetTests {
        private PetRegistrationDto petRegDto;
        private MockMultipartFile petDtoPart;

        @BeforeEach
        void registerPetSetup() throws Exception {
            petRegDto = new PetRegistrationDto(
                    "IntTestBuddy", Specie.DOG, LocalDate.now().minusMonths(6),
                    labradorBreedId, null, "Golden", Gender.MALE, "123456789101112" + System.currentTimeMillis()
            );
            String petRegDtoJson = objectMapper.writeValueAsString(petRegDto);
            petDtoPart = new MockMultipartFile("dto", "", MediaType.APPLICATION_JSON_VALUE, petRegDtoJson.getBytes());
        }

        @Test
        @DisplayName("should return 201 Created and PetProfileDto when called by Owner with valid data")
        void registerPet_Success() throws Exception {
            mockMvc.perform(multipart("/api/pets")
                            .file(petDtoPart)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name", is(petRegDto.name())))
                    .andExpect(jsonPath("$.status", is(PetStatus.PENDING.name())))
                    .andExpect(jsonPath("$.ownerId", is(ownerId.intValue())))
                    .andExpect(jsonPath("$.breedId", is(labradorBreedId.intValue())))
                    .andExpect(jsonPath("$.microchip", is(petRegDto.microchip())));
        }

        @Test
        @DisplayName("should return 201 Created when Owner registers pet with an image file")
        void registerPet_Success_WithImage() throws Exception {
            MockMultipartFile imageFile = new MockMultipartFile(
                    "imageFile",
                    "buddy.jpg",
                    MediaType.IMAGE_JPEG_VALUE,
                    "fake_image_content".getBytes()
            );

            mockMvc.perform(multipart("/api/pets")
                            .file(petDtoPart)
                            .file(imageFile)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name", is(petRegDto.name())));
        }

        @Test
        @DisplayName("should return 400 Bad Request when required fields are missing")
        void registerPet_BadRequest_MissingFields() throws Exception {
            PetRegistrationDto invalidDto = new PetRegistrationDto(null, null, null, null, null, null, null, null);
            String invalidDtoJson = objectMapper.writeValueAsString(invalidDto);
            MockMultipartFile invalidDtoPart = new MockMultipartFile("dto", "", MediaType.APPLICATION_JSON_VALUE, invalidDtoJson.getBytes());

            mockMvc.perform(multipart("/api/pets")
                            .file(invalidDtoPart)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error", is("Validation Failed")))
                    .andExpect(jsonPath("$.message.name", containsString("cannot be blank")))
                    .andExpect(jsonPath("$.message.specie", containsString("cannot be null")))
                    .andExpect(jsonPath("$.message.birthDate", containsString("cannot be null")));
        }

        @Test
        @DisplayName("should return 403 Forbidden when called by non-Owner")
        void registerPet_Forbidden_NotOwner() throws Exception {
            mockMvc.perform(multipart("/api/pets")
                            .file(petDtoPart)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 401 Unauthorized when no authentication token is provided")
        void registerPet_Unauthorized() throws Exception {
            mockMvc.perform(multipart("/api/pets")
                            .file(petDtoPart))
                    .andExpect(status().isUnauthorized());
        }
    }

    /**
     * --- Tests for GET /api/pets (List Owner's Pets) ---
     */
    @Nested
    @DisplayName("GET /api/pets (List Owner's Pets Tests)")
    class ListOwnerPetsTests {

        private Long petId1, petId2;

        @BeforeEach
        void listSetup() throws Exception {
            PetRegistrationDto reg1Dto = new PetRegistrationDto(
                    "PetA", Specie.CAT, LocalDate.now().minusYears(1), null,null,null,null,null);
            String reg1Json = objectMapper.writeValueAsString(reg1Dto);
            MockMultipartFile reg1Part = new MockMultipartFile("dto", "", MediaType.APPLICATION_JSON_VALUE, reg1Json.getBytes());

            PetRegistrationDto reg2Dto = new PetRegistrationDto(
                    "PetZ", Specie.DOG, LocalDate.now().minusMonths(2), null,null,null,null,null);
            String reg2Json = objectMapper.writeValueAsString(reg2Dto);
            MockMultipartFile reg2Part = new MockMultipartFile("dto", "", MediaType.APPLICATION_JSON_VALUE, reg2Json.getBytes());

            MvcResult res1 = mockMvc.perform(multipart("/api/pets")
                            .file(reg1Part)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                    .andExpect(status().isCreated()).andReturn();
            MvcResult res2 = mockMvc.perform(multipart("/api/pets")
                            .file(reg2Part)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                    .andExpect(status().isCreated()).andReturn();
            petId1 = extractPetIdFromResult(objectMapper,res1);
            petId2 = extractPetIdFromResult(objectMapper,res2);
        }

        @Test
        @DisplayName("should return 200 OK with page of owner's pets when called by Owner")
        void listPets_Success_Owner() throws Exception {
            mockMvc.perform(get("/api/pets")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                            .param("page", "0").param("size", "5").param("sort", "name,asc")) // Request a specific page /sort
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(2)))
                    .andExpect(jsonPath("$.content[0].id", is(petId1.intValue())))
                    .andExpect(jsonPath("$.content[0].name", is("PetA")))
                    .andExpect(jsonPath("$.content[1].id", is(petId2.intValue())))
                    .andExpect(jsonPath("$.content[1].name", is("PetZ")))
                    .andExpect(jsonPath("$.totalElements", is(2)))
                    .andExpect(jsonPath("$.pageable.sort.sorted", is(true)));
        }

        @Test
        @DisplayName("should return 403 Forbidden when called by non-Owner")
        void listPets_Forbidden_NotOwner() throws Exception {
            mockMvc.perform(get("/api/pets")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 401 Unauthorized when no authentication token is provided")
        void listPets_Unauthorized() throws Exception {
            mockMvc.perform(get("/api/pets"))
                    .andExpect(status().isUnauthorized());
        }
    }

    /**
     * --- Tests for Owner modifying Pet (PUT owner-update, PUT deactivate, POST/DELETE associations) ---
     */
    @Nested
    @DisplayName("PUT|POST|DELETE /api/pets/{petId}/** (Owner Pet Modification Tests)")
    class OwnerPetModificationTests {

        private Long petIdToModify;

        /**
         * Create pets and get vet ID needed for these tests.
         */
        @BeforeEach
        void ownerModifySetup() throws Exception {

            PetRegistrationDto reg1Dto = new PetRegistrationDto(
                    "ModifiablePet", Specie.CAT, LocalDate.now().minusMonths(8), null, null, "Black", Gender.FEMALE, "123456789101114");
            String reg1Json = objectMapper.writeValueAsString(reg1Dto);
            MockMultipartFile reg1Part = new MockMultipartFile("dto", "", MediaType.APPLICATION_JSON_VALUE, reg1Json.getBytes());

            PetRegistrationDto reg2Dto = new PetRegistrationDto(
                    "AnotherPet", Specie.RABBIT, LocalDate.now().minusMonths(3), null, null, "White", Gender.MALE, "123456789101115");
            String reg2Json = objectMapper.writeValueAsString(reg2Dto);
            MockMultipartFile reg2Part = new MockMultipartFile("dto", "", MediaType.APPLICATION_JSON_VALUE, reg2Json.getBytes());

            MvcResult res1 = mockMvc.perform(multipart("/api/pets")
                            .file(reg1Part)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                    .andExpect(status().isCreated()).andReturn();

            MvcResult res2 = mockMvc.perform(multipart("/api/pets")
                            .file(reg2Part)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                    .andExpect(status().isCreated()).andReturn();
            petIdToModify = extractPetIdFromResult(objectMapper,res1);
            Long anotherPetId = extractPetIdFromResult(objectMapper,res2);
            assertThat(petIdToModify).isNotNull();
            assertThat(anotherPetId).isNotNull();
        }

        /**
         * Tests for PUT /api/pets/{petId}/owner-update
         */
        @Test
        @DisplayName("should return 200 OK and updated PetProfileDto when called by Owner")
        void updatePetByOwner_Success() throws Exception {
            Long mixedCatBreedId = 45L;
            PetOwnerUpdateDto updateDto = new PetOwnerUpdateDto("UpdatedName",null,"Gray", null, null, "123456789101112", mixedCatBreedId, null,null);
            String updateDtoJson = objectMapper.writeValueAsString(updateDto);
            MockMultipartFile dtoPart = new MockMultipartFile("dto", "", MediaType.APPLICATION_JSON_VALUE, updateDtoJson.getBytes());

            mockMvc.perform(multipart("/api/pets/{petId}/owner-update", petIdToModify)
                            .file(dtoPart)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                            .with(request -> {
                                request.setMethod("PUT");
                                return request;
                            }))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(petIdToModify.intValue())))
                    .andExpect(jsonPath("$.name", is(updateDto.name())))
                    .andExpect(jsonPath("$.image", is(notNullValue())))
                    .andExpect(jsonPath("$.color", is(updateDto.color())))
                    .andExpect(jsonPath("$.microchip", is(updateDto.microchip())))
                    .andExpect(jsonPath("$.breedId", is(mixedCatBreedId.intValue())));
        }

        @Test
        @DisplayName("should return 403 Forbidden when called by a different Owner")
        void updatePetByOwner_Forbidden_DifferentOwner() throws Exception {
            // Arrange
            String otherOwnerUsername = "other_owner_" + System.currentTimeMillis();
            OwnerRegistrationDto otherReg = new OwnerRegistrationDto(otherOwnerUsername, otherOwnerUsername + "@test.com", "pass123456", "123");
            mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(otherReg))).andExpect(status().isCreated());
            String otherOwnerToken = obtainJwtToken(mockMvc, objectMapper,new AuthLoginRequestDto(otherOwnerUsername, "pass123456"));

            PetOwnerUpdateDto updateDto = new PetOwnerUpdateDto("AttemptUpdate", null,null,null,null,null,null,null,null);
            String updateDtoJson = objectMapper.writeValueAsString(updateDto);
            MockMultipartFile dtoPart = new MockMultipartFile("dto", "", MediaType.APPLICATION_JSON_VALUE, updateDtoJson.getBytes());

            mockMvc.perform(multipart("/api/pets/{petId}/owner-update", petIdToModify)
                            .file(dtoPart)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherOwnerToken)
                            .with(request -> { request.setMethod("PUT"); return request; }))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 404 Not Found when Pet ID does not exist")
        void updatePetByOwner_NotFound() throws Exception {
            PetOwnerUpdateDto updateDto = new PetOwnerUpdateDto("AnyName", null,null,null,null,null,null,null,null);
            String updateDtoJson = objectMapper.writeValueAsString(updateDto);
            MockMultipartFile dtoPart = new MockMultipartFile("dto", "", MediaType.APPLICATION_JSON_VALUE, updateDtoJson.getBytes());

            mockMvc.perform(multipart("/api/pets/{petId}/owner-update", 9999L)
                            .file(dtoPart)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                            .with(request -> { request.setMethod("PUT"); return request; }))
                    .andExpect(status().isNotFound());
        }

        /**
         * Tests for PUT /api/pets/{petId}/deactivate
         */
        @Test
        @DisplayName("should return 200 OK and updated PetProfileDto when called by Owner")
        void deactivatePet_Success() throws Exception {
            mockMvc.perform(put("/api/pets/{petId}/deactivate", petIdToModify)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(petIdToModify.intValue())))
                    .andExpect(jsonPath("$.status", is(PetStatus.INACTIVE.name())));
        }

        /**
         * Tests for POST /api/pets/{petId}/associate-clinic/{clinicId}
         */
        @Test
        @DisplayName("should return 204 No Content when association is successful")
        void associateClinic_Success() throws Exception {
            mockMvc.perform(post("/api/pets/{petId}/associate-clinic/{clinicId}", petIdToModify, clinicId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                    .andExpect(status().isNoContent()); // 204 No Content on success

            entityManager.flush();
            entityManager.clear();
            Pet pet = petRepository.findById(petIdToModify).orElseThrow();
            assertThat(pet.getPendingActivationClinic()).isNotNull();
            assertThat(pet.getPendingActivationClinic().getId()).isEqualTo(clinicId);
        }

        @Test
        @DisplayName("should return 400 Bad Request when Pet status is not PENDING")
        void associateClinic_BadRequest_NotPending() throws Exception {
            mockMvc.perform(post("/api/pets/{petId}/associate-clinic/{clinicId}", petIdToModify, clinicId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                    .andExpect(status().isNoContent());

            entityManager.flush(); entityManager.clear();
            Pet petToActivateNow = petRepository.findById(petIdToModify).orElseThrow();
            PetActivationDto activationBody = new PetActivationDto(
                    petToActivateNow.getColor() != null ? petToActivateNow.getColor() : "DefaultColor",
                    petToActivateNow.getGender() != null ? petToActivateNow.getGender() : Gender.MALE,
                    petToActivateNow.getBirthDate() != null ? petToActivateNow.getBirthDate() : LocalDate.now().minusYears(1),
                    StringUtils.hasText(petToActivateNow.getMicrochip()) ? petToActivateNow.getMicrochip() : "TEMPCHIP" + petIdToModify,
                    petToActivateNow.getBreed().getId());

            mockMvc.perform(put("/api/pets/{petId}/activate", petIdToModify)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(activationBody)))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/api/pets/{petId}/associate-clinic/{clinicId}", petIdToModify, clinicId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("must be in PENDING status")));
        }

        /**
         * Tests for POST /api/pets/{petId}/associate-vet/{vetId}
         */
        @Test
        @DisplayName("should return 204 No Content when association is successful")
        void associateVet_Success() throws Exception {

            mockMvc.perform(post("/api/pets/{petId}/associate-vet/{vetId}", petIdToModify, vetId )
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                    .andExpect(status().isNoContent());

            entityManager.flush();
            entityManager.clear();
            Pet pet = petRepository.findById(petIdToModify).orElseThrow();
            assertThat(pet.getAssociatedVets()).anyMatch(v -> v.getId().equals(vetId ));
        }

        @Test
        @DisplayName("should return 400 Bad Request when Vet is already associated")
        void associateVet_BadRequest_AlreadyAssociated() throws Exception {
            mockMvc.perform(post("/api/pets/{petId}/associate-vet/{vetId}", petIdToModify, vetId )
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                    .andExpect(status().isNoContent());
            mockMvc.perform(post("/api/pets/{petId}/associate-vet/{vetId}", petIdToModify, vetId )
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("is already associated")));
        }

        /**
         * Tests for DELETE /api/pets/{petId}/associate-vet/{vetId}
         */
        @Test
        @DisplayName("should return 204 No Content when disassociation is successful")
        void disassociateVet_Success() throws Exception {
            mockMvc.perform(post("/api/pets/{petId}/associate-vet/{vetId}", petIdToModify, vetId )
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                    .andExpect(status().isNoContent());
            mockMvc.perform(delete("/api/pets/{petId}/associate-vet/{vetId}", petIdToModify, vetId )
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                    .andExpect(status().isNoContent());

            entityManager.flush();
            entityManager.clear();
            Pet pet = petRepository.findById(petIdToModify).orElseThrow();
            assertThat(pet.getAssociatedVets()).noneMatch(v -> v.getId().equals(vetId ));
        }

        @Test
        @DisplayName("should return 204 No Content when Vet is not associated")
        void disassociateVet_NoOp_NotAssociated() throws Exception {
            mockMvc.perform(delete("/api/pets/{petId}/associate-vet/{vetId}", petIdToModify, vetId )
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                    .andExpect(status().isNoContent());
        }
    }

    /**
     * --- Tests for Staff modifying Pet (PUT activate, PUT clinic-update) ---
     */
    @Nested
    @DisplayName("PUT /api/pets/{petId}/activate & clinic-update (Staff Pet Modification Tests)")
    class StaffPetModificationTests {

        private Long pendingPetId;
        private Long activePetId;
        private PetActivationDto validActivationDto;

        @BeforeEach
        void staffModifySetup() throws Exception {
            Long mixedDogBreedId = 56L;
            Long mixedCatBreedId = 45L;

            String uniqueMicrochipPending = String.format("%015d", Math.abs(System.nanoTime() % 1000000000000000L));
            String uniqueMicrochipActive = String.format("%015d", Math.abs((System.nanoTime() + 1) % 1000000000000000L));

            PetRegistrationDto pendingRegDto = new PetRegistrationDto(
                    "PenderStaffTest" + System.currentTimeMillis()%1000,
                    Specie.DOG, LocalDate.now().minusMonths(5),
                    mixedDogBreedId,
                    null,
                    "Brown", Gender.MALE, uniqueMicrochipPending
            );
            String pendingRegJson = objectMapper.writeValueAsString(pendingRegDto);
            MockMultipartFile pendingDtoPart = new MockMultipartFile("dto", "", MediaType.APPLICATION_JSON_VALUE, pendingRegJson.getBytes());

            MvcResult pendingRes = mockMvc.perform(multipart("/api/pets")
                            .file(pendingDtoPart)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                    .andExpect(status().isCreated()).andReturn();
            pendingPetId = extractPetIdFromResult(objectMapper, pendingRes);
            assertThat(pendingPetId).isNotNull();

            mockMvc.perform(post("/api/pets/{petId}/associate-clinic/{clinicId}", pendingPetId, clinicId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                    .andExpect(status().isNoContent());

            PetRegistrationDto activeRegDto = new PetRegistrationDto(
                    "ActivoUpdateStaffTest"  + System.currentTimeMillis()%1000,
                    Specie.CAT, LocalDate.now().minusMonths(9),
                    mixedCatBreedId,
                    null, // image path
                    "Black", Gender.FEMALE, uniqueMicrochipActive
            );
            String activeRegJson = objectMapper.writeValueAsString(activeRegDto);
            MockMultipartFile activeDtoPart = new MockMultipartFile("dto", "", MediaType.APPLICATION_JSON_VALUE, activeRegJson.getBytes());

            MvcResult activeRegRes = mockMvc.perform(multipart("/api/pets")
                            .file(activeDtoPart)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                    .andExpect(status().isCreated()).andReturn();
            activePetId = extractPetIdFromResult(objectMapper, activeRegRes);
            assertThat(activePetId).isNotNull();

            mockMvc.perform(post("/api/pets/{petId}/associate-clinic/{clinicId}", activePetId, clinicId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                    .andExpect(status().isNoContent());

            // Activar activePet
            entityManager.flush(); entityManager.clear();
            Pet petToActivate = petRepository.findById(activePetId).orElseThrow();
            PetActivationDto activationForActivePet = new PetActivationDto(
                    StringUtils.hasText(petToActivate.getColor()) ? petToActivate.getColor() : "DefaultColor",
                    petToActivate.getGender() != null ? petToActivate.getGender() : Gender.FEMALE,
                    petToActivate.getBirthDate() != null ? petToActivate.getBirthDate() : LocalDate.now().minusYears(2),
                    petToActivate.getMicrochip(),
                    petToActivate.getBreed().getId()
            );
            mockMvc.perform(put("/api/pets/{petId}/activate", activePetId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(activationForActivePet)))
                    .andExpect(status().isOk());

            entityManager.flush(); entityManager.clear();
            Pet petForActivationDto = petRepository.findById(pendingPetId).orElseThrow();
            validActivationDto = new PetActivationDto(
                    StringUtils.hasText(petForActivationDto.getColor()) ? petForActivationDto.getColor() : "ColorForPender",
                    petForActivationDto.getGender() != null ? petForActivationDto.getGender() : Gender.MALE,
                    petForActivationDto.getBirthDate() != null ? petForActivationDto.getBirthDate() : LocalDate.now().minusYears(1),
                    petForActivationDto.getMicrochip(),
                    petForActivationDto.getBreed().getId()
            );
        }

        /**
         * Tests for PUT /api/pets/{petId}/activate
         */
        @Test
        @DisplayName("should return 200 OK and activated PetProfileDto when called by authorized Vet")
        void activatePet_Success_ByVet() throws Exception {
            mockMvc.perform(put("/api/pets/{petId}/activate", pendingPetId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validActivationDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(pendingPetId.intValue())))
                    .andExpect(jsonPath("$.status", is(PetStatus.ACTIVE.name())))
                    .andExpect(jsonPath("$.pendingActivationClinicId", is(nullValue())))
                    .andExpect(jsonPath("$.associatedVets[0].id", is(vetId.intValue())));
        }

        @Test
        @DisplayName("should return 403 Forbidden when called by Admin (role not allowed)")
        void activatePet_Forbidden_ByAdmin() throws Exception {
            mockMvc.perform(put("/api/pets/{petId}/activate", pendingPetId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validActivationDto)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 400 Bad Request when Pet status is not PENDING")
        void activatePet_BadRequest_NotPending() throws Exception {
            mockMvc.perform(put("/api/pets/{petId}/activate", activePetId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validActivationDto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("must be in PENDING status to activate")));
        }

        @Test
        @DisplayName("should return 400 Bad Request when Activation DTO data is invalid")
        void activatePet_BadRequest_InvalidDtoData() throws Exception {
            PetActivationDto invalidDto = new PetActivationDto("Color", Gender.MALE, LocalDate.now(),
                    null, // Missing Microchip
                    validActivationDto.breedId());

            // Act & Assert
            mockMvc.perform(put("/api/pets/{petId}/activate", pendingPetId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidDto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error", is("Validation Failed")))
                    .andExpect(jsonPath("$.message.microchip", containsString("cannot be blank")));
        }

        @Test
        @DisplayName("should return 400 Bad Request when required pet data is missing before activation")
        void activatePet_BadRequest_MissingData() throws Exception {
            // Arrange
            entityManager.flush(); entityManager.clear();
            Pet petIncomplete = petRepository.findById(pendingPetId).orElseThrow();
            petIncomplete.setMicrochip(null);
            petRepository.saveAndFlush(petIncomplete);
            entityManager.clear();

            Pet reloadedPet = petRepository.findById(pendingPetId).orElseThrow();
            assertThat(reloadedPet.getMicrochip()).as("Microchip should be null before activation attempt").isNull();

            // Act & Assert
            mockMvc.perform(put("/api/pets/{petId}/activate", pendingPetId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("Request body is missing or cannot be parsed.")));
        }

        @Test
        @DisplayName("should return 403 Forbidden when called by Staff from a different clinic")
        void activatePet_Forbidden_WrongClinic() throws Exception {
            String otherVetToken = obtainJwtToken(mockMvc, objectMapper, new AuthLoginRequestDto("admin_barcelona", "password123"));
            mockMvc.perform(put("/api/pets/{petId}/activate", pendingPetId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherVetToken))
                    .andExpect(status().isForbidden());
        }

        /**
         * Tests for PUT /api/pets/{petId}/clinic-update
         */
        @Test
        @DisplayName("should return 200 OK and updated PetProfileDto when called by authorized Staff")
        void updatePetByStaff_Success() throws Exception {
            PetClinicUpdateDto staffUpdateDto = new PetClinicUpdateDto("Updated Color", Gender.MALE, null, "123456789101119", null);
            mockMvc.perform(put("/api/pets/{petId}/clinic-update", activePetId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(staffUpdateDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(activePetId.intValue())))
                    .andExpect(jsonPath("$.color", is(staffUpdateDto.color())))
                    .andExpect(jsonPath("$.gender", is(staffUpdateDto.gender().name())))
                    .andExpect(jsonPath("$.microchip", is(staffUpdateDto.microchip())));
        }

        @Test
        @DisplayName("should return 403 Forbidden when called by unauthorized Staff")
        void updatePetByStaff_Forbidden_WrongClinic() throws Exception {
            PetClinicUpdateDto staffUpdateDto = new PetClinicUpdateDto("Color", null, null, null, null);
            mockMvc.perform(put("/api/pets/{petId}/clinic-update", activePetId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherAdminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(staffUpdateDto)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 403 Forbidden when called by Owner")
        void updatePetByStaff_Forbidden_Owner() throws Exception {
            PetClinicUpdateDto staffUpdateDto = new PetClinicUpdateDto("Color", null, null, null, null);
            mockMvc.perform(put("/api/pets/{petId}/clinic-update", activePetId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(staffUpdateDto)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 404 Not Found when Pet ID does not exist")
        void updatePetByStaff_NotFound_Pet() throws Exception {
            PetClinicUpdateDto staffUpdateDto = new PetClinicUpdateDto("Color", null, null, null, null);
            mockMvc.perform(put("/api/pets/{petId}/clinic-update", 9999L)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(staffUpdateDto)))
                    .andExpect(status().isNotFound());
        }
    }

    /**
     * --- Tests for Staff retrieving Pet lists (GET /clinic, GET /clinic/pending) ---
     */
    @Nested
    @DisplayName("GET /api/pets/clinic & /clinic/pending (Staff Pet Listing Tests)")
    class StaffPetListingTests {

        private Long pendingPetIdClinic1;
        private Long activePetIdClinic1Vet1;
        private Long activePetIdClinic5;

        @BeforeEach
        void staffListSetup() throws Exception {
            Long clinic1Id = 1L;
            Long clinic5Id = 5L;
            String vet5Username = "pet_ctrl_vet5_" + System.currentTimeMillis();
            String vet5Email = vet5Username + "@test.com";
            String vet5License = "VETLIC5" + (System.currentTimeMillis() % 100000);
            ClinicStaffCreationDto vet5RegDto = new ClinicStaffCreationDto(
                    vet5Username, vet5Email, "password123", "VetFiveName", "VetFiveSurname", RoleEnum.VET, vet5License
            );
            String vet5RegDtoJson = objectMapper.writeValueAsString(vet5RegDto);
            MockMultipartFile vet5DtoPart = new MockMultipartFile("dto", "", MediaType.APPLICATION_JSON_VALUE, vet5RegDtoJson.getBytes());
            MockMultipartFile vet5PublicKeyFile = new MockMultipartFile("publicKeyFile", "vet5_pub.pem", MediaType.TEXT_PLAIN_VALUE, "fake_vet5_public_key_content".getBytes());
            MockMultipartFile vet5PrivateKeyFile = new MockMultipartFile("privateKeyFile", "vet5_pri_enc.pem", MediaType.TEXT_PLAIN_VALUE, "fake_vet5_private_key_content_encrypted".getBytes());

            userRepository.findByUsername(vet5Username).ifPresent(user -> userRepository.delete(user));
            entityManager.flush();

            mockMvc.perform(multipart("/api/staff")
                            .file(vet5DtoPart)
                            .file(vet5PublicKeyFile)
                            .file(vet5PrivateKeyFile)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherAdminToken))
                    .andExpect(status().isCreated());

            String vet5Token = obtainJwtToken(mockMvc, objectMapper, new AuthLoginRequestDto(vet5RegDto.username(), vet5RegDto.password()));
            assertThat(vet5Token).isNotNull();

            String microchipPending = String.format("777%012d", Math.abs(System.nanoTime() % 1000000000000L));
            PetRegistrationDto pendingRegDto = new PetRegistrationDto(
                    "PendingList_" + System.currentTimeMillis()%1000, Specie.DOG, LocalDate.now().minusDays(10),
                    null, null, "Grey", Gender.MALE, microchipPending);
            String pendingRegJson = objectMapper.writeValueAsString(pendingRegDto);
            MockMultipartFile pendingDtoPart = new MockMultipartFile("dto", "", MediaType.APPLICATION_JSON_VALUE, pendingRegJson.getBytes());

            MvcResult resPend = mockMvc.perform(multipart("/api/pets")
                            .file(pendingDtoPart)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                    .andExpect(status().isCreated()).andReturn();
            pendingPetIdClinic1 = extractPetIdFromResult(objectMapper, resPend);
            mockMvc.perform(post("/api/pets/{petId}/associate-clinic/{clinicId}", pendingPetIdClinic1, clinic1Id)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                    .andExpect(status().isNoContent());

            String microchipActive1 = String.format("888%012d", Math.abs(System.nanoTime() % 1000000000000L));
            PetRegistrationDto activeReg1Dto = new PetRegistrationDto(
                    "ActiveList1_" + System.currentTimeMillis()%1000, Specie.CAT, LocalDate.now().minusDays(30),
                    null, null, "Orange", Gender.FEMALE, microchipActive1);
            String activeReg1Json = objectMapper.writeValueAsString(activeReg1Dto);
            MockMultipartFile active1DtoPart = new MockMultipartFile("dto", "", MediaType.APPLICATION_JSON_VALUE, activeReg1Json.getBytes());

            MvcResult resAct1 = mockMvc.perform(multipart("/api/pets")
                            .file(active1DtoPart)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                    .andExpect(status().isCreated()).andReturn();
            activePetIdClinic1Vet1 = extractPetIdFromResult(objectMapper, resAct1);
            mockMvc.perform(post("/api/pets/{petId}/associate-clinic/{clinicId}", activePetIdClinic1Vet1, clinic1Id)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                    .andExpect(status().isNoContent());

            entityManager.flush(); entityManager.clear();
            Pet petAct1 = petRepository.findById(activePetIdClinic1Vet1).orElseThrow();
            PetActivationDto activation1 = new PetActivationDto(
                    StringUtils.hasText(petAct1.getColor()) ? petAct1.getColor() : "DefaultColorForTest1",
                    petAct1.getGender() != null ? petAct1.getGender() : Gender.MALE,
                    petAct1.getBirthDate() != null ? petAct1.getBirthDate() : LocalDate.now().minusYears(1),
                    petAct1.getMicrochip(),
                    petAct1.getBreed().getId()
            );
            mockMvc.perform(put("/api/pets/{petId}/activate", activePetIdClinic1Vet1)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(activation1)))
                    .andExpect(status().isOk());

            String microchipActive5 = String.format("999%012d", Math.abs(System.nanoTime() % 1000000000000L));
            PetRegistrationDto activeReg5Dto = new PetRegistrationDto(
                    "ActiveList5_" + System.currentTimeMillis()%1000, Specie.RABBIT, LocalDate.now().minusDays(50),
                    null, null, "White", Gender.FEMALE, microchipActive5);
            String activeReg5Json = objectMapper.writeValueAsString(activeReg5Dto);
            MockMultipartFile active5DtoPart = new MockMultipartFile("dto", "", MediaType.APPLICATION_JSON_VALUE, activeReg5Json.getBytes());

            MvcResult resAct5 = mockMvc.perform(multipart("/api/pets")
                            .file(active5DtoPart)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                    .andExpect(status().isCreated()).andReturn();
            activePetIdClinic5 = extractPetIdFromResult(objectMapper, resAct5);
            mockMvc.perform(post("/api/pets/{petId}/associate-clinic/{clinicId}", activePetIdClinic5, clinic5Id)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                    .andExpect(status().isNoContent());


            entityManager.flush(); entityManager.clear();
            Pet petAct5 = petRepository.findById(activePetIdClinic5).orElseThrow();
            PetActivationDto activation5 = new PetActivationDto(
                    StringUtils.hasText(petAct5.getColor()) ? petAct5.getColor() : "DefaultColorForTest5",
                    petAct5.getGender() != null ? petAct5.getGender() : Gender.FEMALE,
                    petAct5.getBirthDate() != null ? petAct5.getBirthDate() : LocalDate.now().minusYears(1),
                    petAct5.getMicrochip(),
                    petAct5.getBreed().getId()
            );
            mockMvc.perform(put("/api/pets/{petId}/activate", activePetIdClinic5)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + vet5Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(activation5)))
                    .andExpect(status().isOk());

            entityManager.clear();
        }

        /**
         * Tests for GET /api/pets/clinic
         */
        @Test
        @DisplayName("should return 200 OK with page of associated pets when called by Staff of that clinic")
        void findMyClinicPets_Success_Clinic1() throws Exception {
            mockMvc.perform(get("/api/pets/clinic")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(2)))
                    .andExpect(jsonPath("$.content[*].id", containsInAnyOrder(
                            pendingPetIdClinic1.intValue(),
                            activePetIdClinic1Vet1.intValue()
                    )));
        }

        @Test
        @DisplayName("should return 200 OK with page of associated pets when called by Staff of another clinic")
        void findMyClinicPets_Success_Clinic5() throws Exception {
            mockMvc.perform(get("/api/pets/clinic")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherAdminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].id", is(activePetIdClinic5.intValue())));
        }

        @Test
        @DisplayName("should return 200 OK with empty page when clinic has no associated pets")
        void findMyClinicPets_Success_EmptyClinic() throws Exception {
            // Arrange
            String adminManchesterToken = obtainJwtToken(mockMvc, objectMapper, new AuthLoginRequestDto("admin_manchester", "password123"));

            // Act & Assert
            mockMvc.perform(get("/api/pets/clinic")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminManchesterToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(0)))
                    .andExpect(jsonPath("$.totalElements", is(0)));
        }

        @Test
        @DisplayName("should return 403 Forbidden when called by Owner")
        void findMyClinicPets_Forbidden_Owner() throws Exception {
            mockMvc.perform(get("/api/pets/clinic")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                    .andExpect(status().isForbidden());
        }

        /**
         * Tests for GET /api/pets/clinic/pending
         */
        @Test
        @DisplayName("should return 200 OK with list of PENDING pets when called by Staff of that clinic")
        void findMyClinicPendingPets_Success() throws Exception {
            mockMvc.perform(get("/api/pets/clinic/pending")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].id", is(pendingPetIdClinic1.intValue())))
                    .andExpect(jsonPath("$[0].status", is(PetStatus.PENDING.name())));
        }

        @Test
        @DisplayName("should return 200 OK with empty list when clinic has no pending pets")
        void findMyClinicPendingPets_Success_NoPending() throws Exception {
            Pet petPend = petRepository.findById(pendingPetIdClinic1).orElseThrow();
            PetActivationDto activationPend = new PetActivationDto( "ColorPend", Gender.FEMALE, petPend.getBirthDate(), petPend.getMicrochip(), petPend.getBreed().getId());
            mockMvc.perform(put("/api/pets/{petId}/activate", pendingPetIdClinic1).header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(activationPend))).andExpect(status().isOk());

            mockMvc.perform(get("/api/pets/clinic/pending")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("should return 403 Forbidden when called by Owner")
        void findMyClinicPendingPets_Forbidden_Owner() throws Exception {
            mockMvc.perform(get("/api/pets/clinic/pending")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                    .andExpect(status().isForbidden());
        }
    }

    /**
     * --- Tests for General Retrieval (GET /pets/{id}, GET /breeds/{specie}) ---
     */
    @Nested
    @DisplayName("GET /api/pets/{petId} & /breeds/{specie} (General Pet Retrieval Tests)")
    class GeneralRetrievalTests {
        private Long ownedPetId;
        private Long associatedPetIdByVet1;

        @BeforeEach
        void generalRetrievalSetup() throws Exception {

            String microchipOwned = String.format("201%012d", Math.abs(System.nanoTime() % 1000000000000L));
            PetRegistrationDto ownerPetRegDto = new PetRegistrationDto(
                    "OwnedPetForRetrieval", Specie.RABBIT, LocalDate.now().minusDays(5),
                    null, null, "White", Gender.MALE, microchipOwned);
            String ownerPetRegJson = objectMapper.writeValueAsString(ownerPetRegDto);
            MockMultipartFile ownerPetDtoPart = new MockMultipartFile("dto", "", MediaType.APPLICATION_JSON_VALUE, ownerPetRegJson.getBytes());
            MvcResult resOwner = mockMvc.perform(multipart("/api/pets")
                            .file(ownerPetDtoPart)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                    .andExpect(status().isCreated()).andReturn();
            ownedPetId = extractPetIdFromResult(objectMapper,resOwner);

            String microchipAssoc = String.format("202%012d", Math.abs(System.nanoTime() % 1000000000000L));
            PetRegistrationDto assocPetRegDto = new PetRegistrationDto(
                    "AssocPetForRetrieval", Specie.FERRET, LocalDate.now().minusDays(15),
                    null, null, "Brown", Gender.FEMALE, microchipAssoc);
            String assocPetRegJson = objectMapper.writeValueAsString(assocPetRegDto);
            MockMultipartFile assocPetDtoPart = new MockMultipartFile("dto", "", MediaType.APPLICATION_JSON_VALUE, assocPetRegJson.getBytes());
            MvcResult resAssoc = mockMvc.perform(multipart("/api/pets")
                            .file(assocPetDtoPart)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                    .andExpect(status().isCreated()).andReturn();
            associatedPetIdByVet1 = extractPetIdFromResult(objectMapper, resAssoc);

            mockMvc.perform(post("/api/pets/{petId}/associate-clinic/{clinicId}", associatedPetIdByVet1, clinicId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                    .andExpect(status().isNoContent());
            entityManager.flush(); entityManager.clear();
            Pet petToActivate = petRepository.findById(associatedPetIdByVet1).orElseThrow();
            PetActivationDto activationDto = new PetActivationDto(
                    StringUtils.hasText(petToActivate.getColor()) ? petToActivate.getColor() : "DefaultColorAssoc",
                    petToActivate.getGender()!= null ? petToActivate.getGender() : Gender.FEMALE,
                    petToActivate.getBirthDate()!= null ? petToActivate.getBirthDate() : LocalDate.now().minusYears(1),
                    petToActivate.getMicrochip(),
                    petToActivate.getBreed().getId());
            mockMvc.perform(put("/api/pets/{petId}/activate", associatedPetIdByVet1)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(activationDto)))
                    .andExpect(status().isOk());
            entityManager.flush(); entityManager.clear();
        }

        /**
         * Tests for GET /api/pets/{id}
         */
        @Test
        @DisplayName("should return 200 OK with pet profile when requested by Owner")
        void findPetById_Success_Owner() throws Exception {
            mockMvc.perform(get("/api/pets/{id}", ownedPetId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(ownedPetId.intValue())))
                    .andExpect(jsonPath("$.name", is("OwnedPetForRetrieval")));
        }

        @Test
        @DisplayName("should return 200 OK with pet profile when requested by associated Staff")
        void findPetById_Success_AssociatedStaff() throws Exception {
            mockMvc.perform(get("/api/pets/{id}", associatedPetIdByVet1)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(associatedPetIdByVet1.intValue())))
                    .andExpect(jsonPath("$.name", is("AssocPetForRetrieval")));
        }

        @Test
        @DisplayName("should return 403 Forbidden when requested by unassociated Staff")
        void findPetById_Forbidden_UnassociatedStaff() throws Exception {
            mockMvc.perform(get("/api/pets/{id}", ownedPetId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherAdminToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 404 Not Found when Pet ID does not exist")
        void findPetById_NotFound() throws Exception {
            mockMvc.perform(get("/api/pets/{id}", 9999L)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)) // Need token
                    .andExpect(status().isNotFound());
        }

        /**
         * Tests for GET /api/pets/breeds/{specie}
         */
        @Test
        @DisplayName("should return 200 OK with list of breeds when called by authenticated user for valid species (DOG)")
        void findBreeds_Success_Dog() throws Exception {
            mockMvc.perform(get("/api/pets/breeds/{specie}", Specie.DOG)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(10))))
                    .andExpect(jsonPath("$[?(@.name == 'Labrador Retriever')]", hasSize(1)));
        }

        @Test
        @DisplayName("should return 200 OK with empty list when called by authenticated user for species with no specific breeds")
        void findBreeds_Success_Empty() throws Exception {
            mockMvc.perform(get("/api/pets/breeds/{specie}", Specie.FERRET)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("should return 400 Bad Request when path variable is not a valid Species enum")
        void findBreeds_BadRequest_InvalidSpecies() throws Exception {
            mockMvc.perform(get("/api/pets/breeds/{specie}", "FISH")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error", is("Invalid Parameter Type")));
        }

        @Test
        @DisplayName("should return 401 Unauthorized when no authentication token is provided")
        void findBreeds_Unauthorized() throws Exception {
            mockMvc.perform(get("/api/pets/breeds/{specie}", Specie.CAT))
                    .andExpect(status().isUnauthorized());
        }
    }
}
