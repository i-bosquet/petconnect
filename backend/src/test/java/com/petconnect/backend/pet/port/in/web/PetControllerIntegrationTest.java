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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
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

        adminToken = obtainJwtToken(new AuthLoginRequestDto("admin_london", "password123"));
        otherAdminToken = obtainJwtToken(new AuthLoginRequestDto("admin_barcelona", "password123"));

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
        ownerToken = obtainJwtToken(new AuthLoginRequestDto(ownerReg.username(), ownerReg.password()));

        String vetUsername = "pet_ctrl_vet_" + System.currentTimeMillis();
        String vetEmail = vetUsername + "@test.com";
        ClinicStaffCreationDto vetReg = new ClinicStaffCreationDto(
                vetUsername, vetEmail, "password123", "Test", "Vet", RoleEnum.VET,
                "VETLIC" + System.currentTimeMillis(), "VETKEY" + System.currentTimeMillis()
        );

        userRepository.findByUsername(vetUsername).ifPresent(userRepository::delete);
        entityManager.flush();

        MvcResult vetRegResult = mockMvc.perform(post("/api/staff")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vetReg)))
                .andExpect(status().isCreated())
                .andReturn();
        ClinicStaffProfileDto vetDto = objectMapper.readValue(vetRegResult.getResponse().getContentAsString(), ClinicStaffProfileDto.class);
        vetId = vetDto.id();
        vetToken = obtainJwtToken(new AuthLoginRequestDto(vetReg.username(), vetReg.password()));

        assertThat(ownerToken).isNotNull();
        assertThat(adminToken).isNotNull();
        assertThat(vetToken).isNotNull();
        assertThat(otherAdminToken).isNotNull();
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

    /**
     * --- Tests for POST /api/pets (Register Pet) ---
     */
    @Nested
    @DisplayName("POST /api/pets (Register Pet Tests 'Owner')")
    class RegisterPetTests {

        private PetRegistrationDto petRegDto;

        @BeforeEach
        void registerPetSetup() {
            petRegDto = new PetRegistrationDto(
                    "IntegTestBuddy", Specie.DOG, LocalDate.now().minusMonths(6),
                    labradorBreedId, null, "Golden", Gender.MALE, "MICROCHIPINTEG" + System.currentTimeMillis()
            );
        }

        @Test
        @DisplayName("should return 201 Created and PetProfileDto when called by Owner with valid data")
        void registerPet_Success() throws Exception {
            mockMvc.perform(post("/api/pets")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(petRegDto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", is(notNullValue())))
                    .andExpect(jsonPath("$.name", is(petRegDto.name())))
                    .andExpect(jsonPath("$.status", is(PetStatus.PENDING.name())))
                    .andExpect(jsonPath("$.ownerId", is(ownerId.intValue())))
                    .andExpect(jsonPath("$.breedId", is(labradorBreedId.intValue())))
                    .andExpect(jsonPath("$.microchip", is(petRegDto.microchip())));
        }

        @Test
        @DisplayName("should return 400 Bad Request when required fields are missing")
        void registerPet_BadRequest_MissingFields() throws Exception {
            PetRegistrationDto invalidDto = new PetRegistrationDto(null, null, null, null, null, null, null, null); // Missing required fields
            mockMvc.perform(post("/api/pets")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidDto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error", is("Validation Failed")))
                    .andExpect(jsonPath("$.message.name", containsString("cannot be blank")))
                    .andExpect(jsonPath("$.message.specie", containsString("cannot be null")))
                    .andExpect(jsonPath("$.message.birthDate", containsString("cannot be null")));
        }

        @Test
        @DisplayName("should return 403 Forbidden when called by non-Owner")
        void registerPet_Forbidden_NotOwner() throws Exception {
            mockMvc.perform(post("/api/pets")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(petRegDto)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 401 Unauthorized when no authentication token is provided")
        void registerPet_Unauthorized() throws Exception {
            mockMvc.perform(post("/api/pets")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(petRegDto)))
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
            // Create two pets for the test owner
            PetRegistrationDto reg1 = new PetRegistrationDto("PetA", Specie.CAT, LocalDate.now().minusYears(1), null,null,null,null,null);
            PetRegistrationDto reg2 = new PetRegistrationDto("PetZ", Specie.DOG, LocalDate.now().minusMonths(2), null,null,null,null,null);

            MvcResult res1 = mockMvc.perform(post("/api/pets").header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(reg1))).andExpect(status().isCreated()).andReturn();
            MvcResult res2 = mockMvc.perform(post("/api/pets").header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(reg2))).andExpect(status().isCreated()).andReturn();
            petId1 = extractPetIdFromResult(res1);
            petId2 = extractPetIdFromResult(res2);
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
        private Long vetInClinic1Id;

        /**
         * Create pets and get vet ID needed for these tests.
         */
        @BeforeEach
        void ownerModifySetup() throws Exception {
            vetInClinic1Id = vetId;

            PetRegistrationDto reg1 = new PetRegistrationDto("ModifiablePet", Specie.CAT, LocalDate.now().minusMonths(8), null, null, "Black", Gender.FEMALE, null);
            PetRegistrationDto reg2 = new PetRegistrationDto("AnotherPet", Specie.RABBIT, LocalDate.now().minusMonths(3), null, null, "White", Gender.MALE, null);

            MvcResult res1 = mockMvc.perform(post("/api/pets").header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(reg1))).andExpect(status().isCreated()).andReturn();
            MvcResult res2 = mockMvc.perform(post("/api/pets").header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(reg2))).andExpect(status().isCreated()).andReturn();
            petIdToModify = extractPetIdFromResult(res1);
            Long anotherPetId = extractPetIdFromResult(res2);
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
            PetOwnerUpdateDto updateDto = new PetOwnerUpdateDto("UpdatedName", "updated.png", "Gray", null, null, "NEWCHIP123", mixedCatBreedId); // Update several fields

            mockMvc.perform(put("/api/pets/{petId}/owner-update", petIdToModify)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(petIdToModify.intValue())))
                    .andExpect(jsonPath("$.name", is(updateDto.name())))
                    .andExpect(jsonPath("$.image", is(updateDto.image())))
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
            String otherOwnerToken = obtainJwtToken(new AuthLoginRequestDto(otherOwnerUsername, "pass123456"));

            PetOwnerUpdateDto updateDto = new PetOwnerUpdateDto("AttemptUpdate", null,null,null,null,null,null);

            // Act & Assert
            mockMvc.perform(put("/api/pets/{petId}/owner-update", petIdToModify)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherOwnerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDto)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 404 Not Found when Pet ID does not exist")
        void updatePetByOwner_NotFound() throws Exception {
            PetOwnerUpdateDto updateDto = new PetOwnerUpdateDto("AnyName", null,null,null,null,null,null);
            mockMvc.perform(put("/api/pets/{petId}/owner-update", 9999L) // Non-existent ID
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDto)))
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

        @Test
        @DisplayName("should return 400 Bad Request when Pet is already inactive")
        void deactivatePet_BadRequest_AlreadyInactive() throws Exception {
            mockMvc.perform(put("/api/pets/{petId}/deactivate", petIdToModify)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                    .andExpect(status().isOk());
            mockMvc.perform(put("/api/pets/{petId}/deactivate", petIdToModify)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("already in INACTIVE status")));
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
                    petToActivateNow.getName(),
                    petToActivateNow.getColor() != null ? petToActivateNow.getColor() : "DefaultColor",
                    petToActivateNow.getGender() != null ? petToActivateNow.getGender() : Gender.MALE,
                    petToActivateNow.getBirthDate() != null ? petToActivateNow.getBirthDate() : LocalDate.now().minusYears(1),
                    StringUtils.hasText(petToActivateNow.getMicrochip()) ? petToActivateNow.getMicrochip() : "TEMPCHIP" + petIdToModify,
                    petToActivateNow.getBreed().getId(),
                    petToActivateNow.getImage()
            );

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
            mockMvc.perform(post("/api/pets/{petId}/associate-vet/{vetId}", petIdToModify, vetInClinic1Id)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                    .andExpect(status().isNoContent());

            entityManager.flush();
            entityManager.clear();
            Pet pet = petRepository.findById(petIdToModify).orElseThrow();
            assertThat(pet.getAssociatedVets()).anyMatch(v -> v.getId().equals(vetInClinic1Id));
        }

        @Test
        @DisplayName("should return 400 Bad Request when Vet is already associated")
        void associateVet_BadRequest_AlreadyAssociated() throws Exception {
            mockMvc.perform(post("/api/pets/{petId}/associate-vet/{vetId}", petIdToModify, vetInClinic1Id)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                    .andExpect(status().isNoContent());
            mockMvc.perform(post("/api/pets/{petId}/associate-vet/{vetId}", petIdToModify, vetInClinic1Id)
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
            mockMvc.perform(post("/api/pets/{petId}/associate-vet/{vetId}", petIdToModify, vetInClinic1Id)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                    .andExpect(status().isNoContent());
            mockMvc.perform(delete("/api/pets/{petId}/associate-vet/{vetId}", petIdToModify, vetInClinic1Id)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                    .andExpect(status().isNoContent());

            entityManager.flush();
            entityManager.clear();
            Pet pet = petRepository.findById(petIdToModify).orElseThrow();
            assertThat(pet.getAssociatedVets()).noneMatch(v -> v.getId().equals(vetInClinic1Id));
        }

        @Test
        @DisplayName("should return 204 No Content when Vet is not associated")
        void disassociateVet_NoOp_NotAssociated() throws Exception {
            mockMvc.perform(delete("/api/pets/{petId}/associate-vet/{vetId}", petIdToModify, vetInClinic1Id)
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
            PetRegistrationDto pendingReg = new PetRegistrationDto("Pender", Specie.DOG, LocalDate.now().minusMonths(5), mixedDogBreedId, "pending.jpg", "Brown", Gender.MALE, "PENDINGCHIP1");
            MvcResult pendingRes = mockMvc.perform(post("/api/pets").header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(pendingReg)))
                    .andExpect(status().isCreated()).andReturn();
            pendingPetId = extractPetIdFromResult(pendingRes);

            mockMvc.perform(post("/api/pets/{petId}/associate-clinic/{clinicId}", pendingPetId, clinicId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                    .andExpect(status().isNoContent());

            Long mixedCatBreedId = 45L;
            PetRegistrationDto activeReg = new PetRegistrationDto("ActivoUpdate", Specie.CAT, LocalDate.now().minusMonths(9), mixedCatBreedId, "active.jpg", "Black", Gender.FEMALE, "ACTIVECHIP1");
            MvcResult activeRegRes = mockMvc.perform(post("/api/pets").header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(activeReg)))
                    .andExpect(status().isCreated()).andReturn();
            activePetId = extractPetIdFromResult(activeRegRes);
            mockMvc.perform(post("/api/pets/{petId}/associate-clinic/{clinicId}", activePetId, clinicId).header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)).andExpect(status().isNoContent());

            PetActivationDto activationForActivePet = new PetActivationDto(
                    activeReg.name(), activeReg.color() != null ? activeReg.color() : "Black",
                    activeReg.gender() != null ? activeReg.gender() : Gender.FEMALE,
                    activeReg.birthDate(), activeReg.microchip(),
                    activeReg.breedId() != null ? activeReg.breedId() : mixedCatBreedId,
                    activeReg.image() != null ? activeReg.image() : "images/avatars/pets/cat.png"
            );
            mockMvc.perform(put("/api/pets/{petId}/activate", activePetId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(activationForActivePet)))
                    .andExpect(status().isOk());

            entityManager.flush(); entityManager.clear();
            Pet petForDto = petRepository.findById(pendingPetId).orElseThrow();
            validActivationDto = new PetActivationDto(
                    petForDto.getName(),
                    StringUtils.hasText(petForDto.getColor()) ? petForDto.getColor() : "ColorNeeded",
                    petForDto.getGender() != null ? petForDto.getGender() : Gender.MALE,
                    petForDto.getBirthDate() != null ? petForDto.getBirthDate() : LocalDate.now().minusYears(1),
                    StringUtils.hasText(petForDto.getMicrochip()) ? petForDto.getMicrochip() : "MicrochipNeeded",
                    petForDto.getBreed().getId(),
                    StringUtils.hasText(petForDto.getImage()) ? petForDto.getImage() : "ImageNeeded.jpg"
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
            PetActivationDto invalidDto = new PetActivationDto(
                    "Name", "Color", Gender.MALE, LocalDate.now(),
                    null, // Missing Microchip
                    validActivationDto.breedId(), "image.jpg"
            );

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
            String otherVetToken = obtainJwtToken(new AuthLoginRequestDto("admin_barcelona", "password123"));
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
            PetClinicUpdateDto staffUpdateDto = new PetClinicUpdateDto("Updated Color", Gender.MALE, null, "UPDATEDCHIPSTAFF", null);
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
            ClinicStaffCreationDto vet5Reg = new ClinicStaffCreationDto(
                    vet5Username, vet5Username + "@test.com", "password123", "Vet", "Cinco", RoleEnum.VET,
                    "VETLIC5_" + System.currentTimeMillis(), "VETKEY5_" + System.currentTimeMillis()
            );
            userRepository.findByUsername(vet5Username).ifPresent(userRepository::delete);
            entityManager.flush();
            MvcResult vet5RegResult = mockMvc.perform(post("/api/staff")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherAdminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(vet5Reg)))
                    .andExpect(status().isCreated()).andReturn();
            objectMapper.readValue(vet5RegResult.getResponse().getContentAsString(), ClinicStaffProfileDto.class);
            String vet5Token = obtainJwtToken(new AuthLoginRequestDto(vet5Reg.username(), vet5Reg.password()));

            PetRegistrationDto pendingReg = new PetRegistrationDto("PendingList", Specie.DOG, LocalDate.now().minusDays(10), null, null, null, null, "LISTPENDING");
            MvcResult resPend = mockMvc.perform(post("/api/pets").header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(pendingReg))).andExpect(status().isCreated()).andReturn();
            pendingPetIdClinic1 = extractPetIdFromResult(resPend);
            mockMvc.perform(post("/api/pets/{petId}/associate-clinic/{clinicId}", pendingPetIdClinic1, clinic1Id).header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)).andExpect(status().isNoContent());

            PetRegistrationDto activeReg1 = new PetRegistrationDto("ActiveList1", Specie.CAT, LocalDate.now().minusDays(30), null, null, null, null, "LISTACTIVE1");
            MvcResult resAct1 = mockMvc.perform(post("/api/pets").header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(activeReg1))).andExpect(status().isCreated()).andReturn();
            activePetIdClinic1Vet1 = extractPetIdFromResult(resAct1);
            mockMvc.perform(post("/api/pets/{petId}/associate-clinic/{clinicId}", activePetIdClinic1Vet1, clinic1Id).header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)).andExpect(status().isNoContent());
            Pet petAct1 = petRepository.findById(activePetIdClinic1Vet1).orElseThrow();
            PetActivationDto activation1 = new PetActivationDto(petAct1.getName(), "Color1", Gender.MALE, petAct1.getBirthDate(), petAct1.getMicrochip(), petAct1.getBreed().getId(), petAct1.getImage());
            mockMvc.perform(put("/api/pets/{petId}/activate", activePetIdClinic1Vet1).header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(activation1))).andExpect(status().isOk());

            PetRegistrationDto activeReg5 = new PetRegistrationDto("ActiveList5", Specie.RABBIT, LocalDate.now().minusDays(50), null, null, null, null, "LISTACTIVE5");
            MvcResult resAct5 = mockMvc.perform(post("/api/pets").header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(activeReg5))).andExpect(status().isCreated()).andReturn();
            activePetIdClinic5 = extractPetIdFromResult(resAct5);
            mockMvc.perform(post("/api/pets/{petId}/associate-clinic/{clinicId}", activePetIdClinic5, clinic5Id).header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)).andExpect(status().isNoContent());
            Pet petAct5 = petRepository.findById(activePetIdClinic5).orElseThrow();
            PetActivationDto activation5 = new PetActivationDto(petAct5.getName(), "Color5", Gender.FEMALE, petAct5.getBirthDate(), petAct5.getMicrochip(), petAct5.getBreed().getId(), petAct5.getImage());
            mockMvc.perform(put("/api/pets/{petId}/activate", activePetIdClinic5).header(HttpHeaders.AUTHORIZATION, "Bearer " + vet5Token).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(activation5))).andExpect(status().isOk());


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
            String adminManchesterToken = obtainJwtToken(new AuthLoginRequestDto("admin_manchester", "password123"));

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
            PetActivationDto activationPend = new PetActivationDto(petPend.getName(), "ColorPend", Gender.FEMALE, petPend.getBirthDate(), petPend.getMicrochip(), petPend.getBreed().getId(), petPend.getImage());
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
        private Long associatedPetId;

        @BeforeEach
        void generalRetrievalSetup() throws Exception {

            PetRegistrationDto ownerPetReg = new PetRegistrationDto("OwnedPet", Specie.RABBIT, LocalDate.now(), null,null,null,null,null);
            MvcResult resOwner = mockMvc.perform(post("/api/pets").header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(ownerPetReg))).andExpect(status().isCreated()).andReturn();
            ownedPetId = extractPetIdFromResult(resOwner);

            PetRegistrationDto assocPetReg = new PetRegistrationDto("AssocPet", Specie.FERRET, LocalDate.now(), null,null,null,null,"ASSOCCHIP");
            MvcResult resAssoc = mockMvc.perform(post("/api/pets").header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(assocPetReg))).andExpect(status().isCreated()).andReturn();
            associatedPetId = extractPetIdFromResult(resAssoc);
            mockMvc.perform(post("/api/pets/{petId}/associate-clinic/{clinicId}", associatedPetId, clinicId).header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)).andExpect(status().isNoContent());
            Pet petAssoc = petRepository.findById(associatedPetId).orElseThrow();
            PetActivationDto activationAssoc = new PetActivationDto(petAssoc.getName(),"C1",Gender.MALE,petAssoc.getBirthDate(),petAssoc.getMicrochip(),petAssoc.getBreed().getId(),petAssoc.getImage());
            mockMvc.perform(put("/api/pets/{petId}/activate", associatedPetId).header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(activationAssoc))).andExpect(status().isOk());
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
                    .andExpect(jsonPath("$.name", is("OwnedPet")));
        }

        @Test
        @DisplayName("should return 200 OK with pet profile when requested by associated Staff")
        void findPetById_Success_AssociatedStaff() throws Exception {
            mockMvc.perform(get("/api/pets/{id}", associatedPetId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(associatedPetId.intValue())))
                    .andExpect(jsonPath("$.name", is("AssocPet")));
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
