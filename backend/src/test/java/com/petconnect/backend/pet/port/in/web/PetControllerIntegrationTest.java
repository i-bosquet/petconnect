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
 * Uses PostgreSQL (Docker), security filters, transactional rollback.
 * Verifies pet registration, retrieval, updates, activation, associations etc.
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
     * Setup initial users, pets, and obtain tokens.
     */
    @BeforeEach
    void setUp() throws Exception {
        // --- Obtain Tokens for existing users ---
        adminToken = obtainJwtToken(new AuthLoginRequestDto("admin_london", "password123"));
        otherAdminToken = obtainJwtToken(new AuthLoginRequestDto("admin_barcelona", "password123"));

        // --- Register a new Owner for testing ---
        String ownerUsername = "pet_ctrl_owner_" + System.currentTimeMillis();
        String ownerEmail = ownerUsername + "@test.com";
        OwnerRegistrationDto ownerReg = new OwnerRegistrationDto(ownerUsername, ownerEmail, "password123", "777-888-999");
        // Delete if exists from previous failed run (optional, @Transactional helps)
        userRepository.findByUsername(ownerUsername).ifPresent(userRepository::delete);
        entityManager.flush(); // Ensure delete completes before insert

        MvcResult ownerRegResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ownerReg)))
                .andExpect(status().isCreated())
                .andReturn();
        OwnerProfileDto ownerDto = objectMapper.readValue(ownerRegResult.getResponse().getContentAsString(), OwnerProfileDto.class);
        ownerId = ownerDto.id(); // Store the ID of the created owner
        ownerToken = obtainJwtToken(new AuthLoginRequestDto(ownerReg.username(), ownerReg.password()));

        // --- Register a new Vet in Clinic 1 (London) for testing ---
        String vetUsername = "pet_ctrl_vet_" + System.currentTimeMillis();
        String vetEmail = vetUsername + "@test.com";
        ClinicStaffCreationDto vetReg = new ClinicStaffCreationDto(
                vetUsername, vetEmail, "password123", "Test", "Vet", RoleEnum.VET,
                "VETLIC" + System.currentTimeMillis(), "VETKEY" + System.currentTimeMillis()
        );
        // Delete if exists
        userRepository.findByUsername(vetUsername).ifPresent(userRepository::delete);
        entityManager.flush();

        MvcResult vetRegResult = mockMvc.perform(post("/api/staff") // Use Admin London to create Vet in Clinic 1
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vetReg)))
                .andExpect(status().isCreated())
                .andReturn();
        ClinicStaffProfileDto vetDto = objectMapper.readValue(vetRegResult.getResponse().getContentAsString(), ClinicStaffProfileDto.class);
        vetId = vetDto.id(); // Store the ID of the created vet
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
    @DisplayName("POST /api/pets (Owner Register)")
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
        @DisplayName("should register pet successfully when called by Owner")
        void registerPet_Success() throws Exception {
            mockMvc.perform(post("/api/pets")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken) // Owner's token
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(petRegDto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", is(notNullValue())))
                    .andExpect(jsonPath("$.name", is(petRegDto.name())))
                    .andExpect(jsonPath("$.status", is(PetStatus.PENDING.name()))) // Verify initial status
                    .andExpect(jsonPath("$.ownerId", is(ownerId.intValue())))
                    .andExpect(jsonPath("$.breedId", is(labradorBreedId.intValue())))
                    .andExpect(jsonPath("$.microchip", is(petRegDto.microchip())));
        }

        @Test
        @DisplayName("should return 400 Bad Request if required fields missing")
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
        @DisplayName("should return 403 Forbidden if called by non-Owner (Admin)")
        void registerPet_Forbidden_NotOwner() throws Exception {
            mockMvc.perform(post("/api/pets")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken) // Use Admin token
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(petRegDto)))
                    .andExpect(status().isForbidden()); // Fails hasRole('OWNER')
        }

        @Test
        @DisplayName("should return 401 Unauthorized if no token")
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
    @DisplayName("GET /api/pets (Owner List)")
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
        @DisplayName("should return page of owner's pets when called by Owner")
        void listPets_Success_Owner() throws Exception {
            mockMvc.perform(get("/api/pets")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                            .param("page", "0").param("size", "5").param("sort", "name,asc")) // Request specific page/sort
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(2))) // Expecting the 2 pets created
                    .andExpect(jsonPath("$.content[0].id", is(petId1.intValue()))) // PetA comes first alphabetically
                    .andExpect(jsonPath("$.content[0].name", is("PetA")))
                    .andExpect(jsonPath("$.content[1].id", is(petId2.intValue())))
                    .andExpect(jsonPath("$.content[1].name", is("PetZ")))
                    .andExpect(jsonPath("$.totalElements", is(2)))
                    .andExpect(jsonPath("$.pageable.sort.sorted", is(true)));
        }

        @Test
        @DisplayName("should return 403 Forbidden if called by non-Owner (Admin)")
        void listPets_Forbidden_NotOwner() throws Exception {
            mockMvc.perform(get("/api/pets")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)) // Use Admin token
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 401 Unauthorized if no token")
        void listPets_Unauthorized() throws Exception {
            mockMvc.perform(get("/api/pets"))
                    .andExpect(status().isUnauthorized());
        }
    }

    /**
     * --- Tests for Owner modifying Pet (PUT owner-update, PUT deactivate, POST/DELETE associations) ---
     */
    @Nested
    @DisplayName("PUT|POST|DELETE /api/pets/{petId}/** (Owner Actions)")
    class OwnerPetModificationTests {

        private Long petIdToModify;
        private Long vetInClinic1Id;

        /**
         * Create pets and get vet ID needed for these tests.
         */
        @BeforeEach
        void ownerModifySetup() throws Exception {
            // Use the vetId created in the main setup
            vetInClinic1Id = vetId;

            // Create two pets owned by 'ownerToken' user
            PetRegistrationDto reg1 = new PetRegistrationDto("ModifiablePet", Specie.CAT, LocalDate.now().minusMonths(8), null, null, "Black", Gender.FEMALE, null);
            PetRegistrationDto reg2 = new PetRegistrationDto("AnotherPet", Specie.RABBIT, LocalDate.now().minusMonths(3), null, null, "White", Gender.MALE, null);

            MvcResult res1 = mockMvc.perform(post("/api/pets").header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(reg1))).andExpect(status().isCreated()).andReturn();
            MvcResult res2 = mockMvc.perform(post("/api/pets").header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(reg2))).andExpect(status().isCreated()).andReturn();
            petIdToModify = extractPetIdFromResult(res1);
            Long anotherPetId = extractPetIdFromResult(res2); // Store ID of the second pet
            assertThat(petIdToModify).isNotNull();
            assertThat(anotherPetId).isNotNull();
        }

        /**
         * Tests for PUT /api/pets/{petId}/owner-update
         */
        @Test
        @DisplayName("[owner-update] should update pet successfully when called by Owner")
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
        @DisplayName("[owner-update] should return 403 Forbidden when called by different Owner")
        void updatePetByOwner_Forbidden_DifferentOwner() throws Exception {
            // Arrange: Register ANOTHER owner and get their token
            String otherOwnerUsername = "other_owner_" + System.currentTimeMillis();
            OwnerRegistrationDto otherReg = new OwnerRegistrationDto(otherOwnerUsername, otherOwnerUsername + "@test.com", "pass123456", "123");
            mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(otherReg))).andExpect(status().isCreated());
            String otherOwnerToken = obtainJwtToken(new AuthLoginRequestDto(otherOwnerUsername, "pass123456"));

            PetOwnerUpdateDto updateDto = new PetOwnerUpdateDto("AttemptUpdate", null,null,null,null,null,null);

            // Act & Assert: Use otherOwnerToken to try update petIdToModify (owned by ownerToken)
            mockMvc.perform(put("/api/pets/{petId}/owner-update", petIdToModify)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherOwnerToken) // Wrong owner
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDto)))
                    .andExpect(status().isForbidden()); // Service layer should deny access
        }

        @Test
        @DisplayName("[owner-update] should return 404 Not Found if Pet ID does not exist")
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
        @DisplayName("[deactivate] should deactivate pet successfully when called by Owner")
        void deactivatePet_Success() throws Exception {
            mockMvc.perform(put("/api/pets/{petId}/deactivate", petIdToModify)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(petIdToModify.intValue())))
                    .andExpect(jsonPath("$.status", is(PetStatus.INACTIVE.name()))); // Check status changed
        }

        @Test
        @DisplayName("[deactivate] should return 400 Bad Request if pet already inactive")
        void deactivatePet_BadRequest_AlreadyInactive() throws Exception {
            // Deactivate first
            mockMvc.perform(put("/api/pets/{petId}/deactivate", petIdToModify)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                    .andExpect(status().isOk());
            // Try again
            mockMvc.perform(put("/api/pets/{petId}/deactivate", petIdToModify)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                    .andExpect(status().isBadRequest()) // Expecting 400 from IllegalStateException
                    .andExpect(jsonPath("$.message", containsString("already in INACTIVE status")));
        }

        /**
         * Tests for POST /api/pets/{petId}/associate-clinic/{clinicId}
         */
        @Test
        @DisplayName("[associate-clinic] should associate pet successfully")
        void associateClinic_Success() throws Exception {
            mockMvc.perform(post("/api/pets/{petId}/associate-clinic/{clinicId}", petIdToModify, clinicId) // Use clinicId=1 (London)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                    .andExpect(status().isNoContent()); // 204 No Content on success

            entityManager.flush();
            entityManager.clear();
            Pet pet = petRepository.findById(petIdToModify).orElseThrow();
            assertThat(pet.getPendingActivationClinic()).isNotNull();
            assertThat(pet.getPendingActivationClinic().getId()).isEqualTo(clinicId);
        }

        @Test
        @DisplayName("[associate-clinic] should return 400 Bad Request if pet not PENDING")
        void associateClinic_BadRequest_NotPending() throws Exception {
            // Activate the pet first (using the Vet created in main setup)
            mockMvc.perform(post("/api/pets/{petId}/associate-clinic/{clinicId}", petIdToModify, clinicId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                    .andExpect(status().isNoContent());

            // Activate the pet first (using the Vet created in main setup)
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

            // Now try to associate again (pet is now ACTIVE)
            mockMvc.perform(post("/api/pets/{petId}/associate-clinic/{clinicId}", petIdToModify, clinicId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                    .andExpect(status().isBadRequest()) // Expecting 400 from IllegalStateException
                    .andExpect(jsonPath("$.message", containsString("must be in PENDING status")));
        }

        /**
         * Tests for POST /api/pets/{petId}/associate-vet/{vetId}
         */
        @Test
        @DisplayName("[associate-vet] should associate vet successfully")
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
        @DisplayName("[associate-vet] should return 400 Bad Request if already associated")
        void associateVet_BadRequest_AlreadyAssociated() throws Exception {
            // Associate first time
            mockMvc.perform(post("/api/pets/{petId}/associate-vet/{vetId}", petIdToModify, vetInClinic1Id)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                    .andExpect(status().isNoContent());
            // Try again
            mockMvc.perform(post("/api/pets/{petId}/associate-vet/{vetId}", petIdToModify, vetInClinic1Id)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                    .andExpect(status().isBadRequest()) // Expect 400 from IllegalStateException
                    .andExpect(jsonPath("$.message", containsString("is already associated")));
        }

        /**
         * Tests for DELETE /api/pets/{petId}/associate-vet/{vetId}
         */
        @Test
        @DisplayName("[disassociate-vet] should disassociate vet successfully")
        void disassociateVet_Success() throws Exception {
            // Associate first
            mockMvc.perform(post("/api/pets/{petId}/associate-vet/{vetId}", petIdToModify, vetInClinic1Id)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                    .andExpect(status().isNoContent());
            // Now disassociate
            mockMvc.perform(delete("/api/pets/{petId}/associate-vet/{vetId}", petIdToModify, vetInClinic1Id)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                    .andExpect(status().isNoContent());

            entityManager.flush();
            entityManager.clear();
            Pet pet = petRepository.findById(petIdToModify).orElseThrow();
            assertThat(pet.getAssociatedVets()).noneMatch(v -> v.getId().equals(vetInClinic1Id));
        }

        @Test
        @DisplayName("[disassociate-vet] should do nothing and return 204 if vet not associated")
        void disassociateVet_NoOp_NotAssociated() throws Exception {
            // Vet is NOT associated initially
            mockMvc.perform(delete("/api/pets/{petId}/associate-vet/{vetId}", petIdToModify, vetInClinic1Id)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                    .andExpect(status().isNoContent());
        }
    }

    /**
     * --- Tests for Staff modifying Pet (PUT activate, PUT clinic-update) ---
     */
    @Nested
    @DisplayName("PUT /api/pets/{petId}/activate & clinic-update (Staff Actions)")
    class StaffPetModificationTests {

        private Long pendingPetId;
        private Long activePetId;
        private PetActivationDto validActivationDto;

        @BeforeEach
        void staffModifySetup() throws Exception {
            // Create PENDING pet
            Long mixedDogBreedId = 56L;
            PetRegistrationDto pendingReg = new PetRegistrationDto("Pender", Specie.DOG, LocalDate.now().minusMonths(5), mixedDogBreedId, "pending.jpg", "Brown", Gender.MALE, "PENDINGCHIP1");
            MvcResult pendingRes = mockMvc.perform(post("/api/pets").header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(pendingReg)))
                    .andExpect(status().isCreated()).andReturn();
            pendingPetId = extractPetIdFromResult(pendingRes);

            // Associate PENDING pet with Clinic 1
            mockMvc.perform(post("/api/pets/{petId}/associate-clinic/{clinicId}", pendingPetId, clinicId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                    .andExpect(status().isNoContent());

            // Create and Activate ACTIVE pet
            Long mixedCatBreedId = 45L;
            PetRegistrationDto activeReg = new PetRegistrationDto("ActivoUpdate", Specie.CAT, LocalDate.now().minusMonths(9), mixedCatBreedId, "active.jpg", "Black", Gender.FEMALE, "ACTIVECHIP1");
            MvcResult activeRegRes = mockMvc.perform(post("/api/pets").header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(activeReg)))
                    .andExpect(status().isCreated()).andReturn();
            activePetId = extractPetIdFromResult(activeRegRes);
            mockMvc.perform(post("/api/pets/{petId}/associate-clinic/{clinicId}", activePetId, clinicId).header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)).andExpect(status().isNoContent());
            // --- Activation Body for 'activePetId' ---
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
        @DisplayName("[activate] should activate pet successfully when called by authorized Vet")
        void activatePet_Success_ByVet() throws Exception {
            // Activate using vetToken and the prepared validActivationDto
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
        @DisplayName("[activate] should return 403 Forbidden when called by Admin (Role not allowed)")
        void activatePet_Forbidden_ByAdmin() throws Exception {
            // Use adminToken (Admin from Clinic 1)
            mockMvc.perform(put("/api/pets/{petId}/activate", pendingPetId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validActivationDto)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("[activate] should return 400 Bad Request if pet not PENDING")
        void activatePet_BadRequest_NotPending() throws Exception {
            // Use activePetId which was already activated in setup
            mockMvc.perform(put("/api/pets/{petId}/activate", activePetId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validActivationDto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("must be in PENDING status to activate")));
        }

        @Test
        @DisplayName("[activate] should return 400 Bad Request if Activation DTO data invalid")
        void activatePet_BadRequest_InvalidDtoData() throws Exception {
            // Create an invalid DTO
            PetActivationDto invalidDto = new PetActivationDto(
                    "Name", "Color", Gender.MALE, LocalDate.now(),
                    null, // Missing Microchip
                    validActivationDto.breedId(), "image.jpg"
            );

            // Act & Assert
            mockMvc.perform(put("/api/pets/{petId}/activate", pendingPetId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidDto))) // Send invalid body
                    .andExpect(status().isBadRequest()) // Expect 400 from @Valid
                    .andExpect(jsonPath("$.error", is("Validation Failed")))
                    .andExpect(jsonPath("$.message.microchip", containsString("cannot be blank")));
        }

        @Test
        @DisplayName("[activate] should return 400 Bad Request if required pet data missing")
        void activatePet_BadRequest_MissingData() throws Exception {
            // Arrange: Ensure a required field is missing
            entityManager.flush(); entityManager.clear();
            Pet petIncomplete = petRepository.findById(pendingPetId).orElseThrow();
            petIncomplete.setMicrochip(null); // Make data incomplete
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
        @DisplayName("[activate] should return 403 Forbidden if called by Staff from different clinic")
        void activatePet_Forbidden_WrongClinic() throws Exception {
            // Use otherAdminToken (Admin from Clinic 5) - This will fail role check first
            // If we used a Vet from Clinic 5, it would fail the clinic check in the service
            String otherVetToken = obtainJwtToken(new AuthLoginRequestDto("admin_barcelona", "password123"));
            mockMvc.perform(put("/api/pets/{petId}/activate", pendingPetId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherVetToken))
                    .andExpect(status().isForbidden());
        }

        // Tests for PUT /api/pets/{petId}/clinic-update
        @Test
        @DisplayName("[clinic-update] should update pet successfully when called by authorized Staff")
        void updatePetByStaff_Success() throws Exception {
            // Use vetToken to update the already ACTIVE pet (activePetId)
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
        @DisplayName("[clinic-update] should return 403 Forbidden if called by unauthorized Staff (wrong clinic)")
        void updatePetByStaff_Forbidden_WrongClinic() throws Exception {
            PetClinicUpdateDto staffUpdateDto = new PetClinicUpdateDto("Color", null, null, null, null);
            mockMvc.perform(put("/api/pets/{petId}/clinic-update", activePetId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherAdminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(staffUpdateDto)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("[clinic-update] should return 403 Forbidden if called by Owner")
        void updatePetByStaff_Forbidden_Owner() throws Exception {
            PetClinicUpdateDto staffUpdateDto = new PetClinicUpdateDto("Color", null, null, null, null);
            mockMvc.perform(put("/api/pets/{petId}/clinic-update", activePetId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(staffUpdateDto)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("[clinic-update] should return 404 Not Found if Pet ID does not exist")
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
    @DisplayName("GET /api/pets/clinic & /clinic/pending (Staff Listing)")
    class StaffPetListingTests {

        private Long pendingPetIdClinic1;
        private Long activePetIdClinic1Vet1; // Associated with vetToken (Clinic 1)
        private Long activePetIdClinic5; // Associated with vet from Clinic 5

        @BeforeEach
        void staffListSetup() throws Exception {
            // --- IDs y Tokens ---
            Long clinic1Id = 1L; // London
            Long clinic5Id = 5L; // Barcelona

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

            // Pet PENDING in Clínic 1
            PetRegistrationDto pendingReg = new PetRegistrationDto("PendingList", Specie.DOG, LocalDate.now().minusDays(10), null, null, null, null, "LISTPENDING");
            MvcResult resPend = mockMvc.perform(post("/api/pets").header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(pendingReg))).andExpect(status().isCreated()).andReturn();
            pendingPetIdClinic1 = extractPetIdFromResult(resPend);
            mockMvc.perform(post("/api/pets/{petId}/associate-clinic/{clinicId}", pendingPetIdClinic1, clinic1Id).header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)).andExpect(status().isNoContent());

            // Pet ACTIVE associate to vetId (Clinic 1)
            PetRegistrationDto activeReg1 = new PetRegistrationDto("ActiveList1", Specie.CAT, LocalDate.now().minusDays(30), null, null, null, null, "LISTACTIVE1");
            MvcResult resAct1 = mockMvc.perform(post("/api/pets").header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(activeReg1))).andExpect(status().isCreated()).andReturn();
            activePetIdClinic1Vet1 = extractPetIdFromResult(resAct1);
            mockMvc.perform(post("/api/pets/{petId}/associate-clinic/{clinicId}", activePetIdClinic1Vet1, clinic1Id).header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)).andExpect(status().isNoContent());
            Pet petAct1 = petRepository.findById(activePetIdClinic1Vet1).orElseThrow();
            PetActivationDto activation1 = new PetActivationDto(petAct1.getName(), "Color1", Gender.MALE, petAct1.getBirthDate(), petAct1.getMicrochip(), petAct1.getBreed().getId(), petAct1.getImage());
            mockMvc.perform(put("/api/pets/{petId}/activate", activePetIdClinic1Vet1).header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(activation1))).andExpect(status().isOk());

            // Pet ACTIVE associate to Vet 5 (Clinic 5)
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
        @DisplayName("[GET /clinic] should return pets associated with staff's clinic (C1)")
        void findMyClinicPets_Success_Clinic1() throws Exception {
            mockMvc.perform(get("/api/pets/clinic")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken)) // Vet from Clinic 1
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(2)))
                    .andExpect(jsonPath("$.content[*].id", containsInAnyOrder(
                            pendingPetIdClinic1.intValue(),
                            activePetIdClinic1Vet1.intValue()
                    )));
        }

        @Test
        @DisplayName("[GET /clinic] should return pet associated with staff's clinic (C5)")
        void findMyClinicPets_Success_Clinic5() throws Exception {
            mockMvc.perform(get("/api/pets/clinic")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherAdminToken)) // Admin from Clinic 5
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].id", is(activePetIdClinic5.intValue())));
        }

        @Test
        @DisplayName("[GET /clinic] should return empty page for clinic with no associated pets")
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
        @DisplayName("[GET /clinic] should return 403 Forbidden if called by Owner")
        void findMyClinicPets_Forbidden_Owner() throws Exception {
            mockMvc.perform(get("/api/pets/clinic")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                    .andExpect(status().isForbidden());
        }

        /**
         * Tests for GET /api/pets/clinic/pending
         */
        @Test
        @DisplayName("[GET /clinic/pending] should return only PENDING pets for staff's clinic")
        void findMyClinicPendingPets_Success() throws Exception {
            mockMvc.perform(get("/api/pets/clinic/pending")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken)) // Vet from Clinic 1
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1))) // Only the one pending pet
                    .andExpect(jsonPath("$[0].id", is(pendingPetIdClinic1.intValue())))
                    .andExpect(jsonPath("$[0].status", is(PetStatus.PENDING.name())));
        }

        @Test
        @DisplayName("[GET /clinic/pending] should return empty list if no pending pets")
        void findMyClinicPendingPets_Success_NoPending() throws Exception {
            // Activate the pending pet first
            Pet petPend = petRepository.findById(pendingPetIdClinic1).orElseThrow();
            PetActivationDto activationPend = new PetActivationDto(petPend.getName(), "ColorPend", Gender.FEMALE, petPend.getBirthDate(), petPend.getMicrochip(), petPend.getBreed().getId(), petPend.getImage());
            mockMvc.perform(put("/api/pets/{petId}/activate", pendingPetIdClinic1).header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(activationPend))).andExpect(status().isOk());

            // Now request pending pets
            mockMvc.perform(get("/api/pets/clinic/pending")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("[GET /clinic/pending] should return 403 Forbidden if called by Owner")
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
    @DisplayName("GET /api/pets/{petId} & /breeds/{specie} (General Retrieval)")
    class GeneralRetrievalTests {

        private Long ownedPetId; // Pet owned by ownerToken
        private Long associatedPetId; // Pet associated with vetToken's clinic

        @BeforeEach
        void generalRetrievalSetup() throws Exception {
            // Create a pet for the owner
            PetRegistrationDto ownerPetReg = new PetRegistrationDto("OwnedPet", Specie.RABBIT, LocalDate.now(), null,null,null,null,null);
            MvcResult resOwner = mockMvc.perform(post("/api/pets").header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(ownerPetReg))).andExpect(status().isCreated()).andReturn();
            ownedPetId = extractPetIdFromResult(resOwner);

            // Create and activate a pet associated with vetToken's clinic
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
        @DisplayName("[GET /{id}] should return pet when requested by Owner")
        void findPetById_Success_Owner() throws Exception {
            mockMvc.perform(get("/api/pets/{id}", ownedPetId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(ownedPetId.intValue())))
                    .andExpect(jsonPath("$.name", is("OwnedPet")));
        }

        @Test
        @DisplayName("[GET /{id}] should return pet when requested by associated Staff")
        void findPetById_Success_AssociatedStaff() throws Exception {
            mockMvc.perform(get("/api/pets/{id}", associatedPetId) // Pet associated with clinic 1
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken)) // Vet from clinic 1
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(associatedPetId.intValue())))
                    .andExpect(jsonPath("$.name", is("AssocPet")));
        }

        @Test
        @DisplayName("[GET /{id}] should return 403 Forbidden when requested by unassociated Staff")
        void findPetById_Forbidden_UnassociatedStaff() throws Exception {
            mockMvc.perform(get("/api/pets/{id}", ownedPetId) // Pet owned by ownerToken, not associated with clinic 5
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherAdminToken)) // Staff from clinic 5
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("[GET /{id}] should return 404 Not Found for non-existent pet")
        void findPetById_NotFound() throws Exception {
            mockMvc.perform(get("/api/pets/{id}", 9999L)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)) // Need token
                    .andExpect(status().isNotFound());
        }

        /**
         * Tests for GET /api/pets/breeds/{specie}
         */
        @Test
        @DisplayName("[GET /breeds] should return list of breeds for DOG")
        void findBreeds_Success_Dog() throws Exception {
            mockMvc.perform(get("/api/pets/breeds/{specie}", Specie.DOG)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)) // Any authenticated user
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(10)))) // Check size based on data.sql
                    .andExpect(jsonPath("$[?(@.name == 'Labrador Retriever')]", hasSize(1))); // Check a specific breed
        }

        @Test
        @DisplayName("[GET /breeds] should return empty list for species with no breeds (if any)")
        void findBreeds_Success_Empty() throws Exception {
            // Assuming FERRET might have fewer entries or could be empty if data.sql changes
            mockMvc.perform(get("/api/pets/breeds/{specie}", Specie.FERRET)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("[GET /breeds] should return 400 Bad Request for invalid species path variable")
        void findBreeds_BadRequest_InvalidSpecies() throws Exception {
            mockMvc.perform(get("/api/pets/breeds/{specie}", "FISH") // Invalid species value
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error", is("Invalid Parameter Type"))); // Check error from TypeMismatch handler
        }

        @Test
        @DisplayName("[GET /breeds] should return 401 Unauthorized if no token")
        void findBreeds_Unauthorized() throws Exception {
            mockMvc.perform(get("/api/pets/breeds/{specie}", Specie.CAT))
                    .andExpect(status().isUnauthorized());
        }
    }
}
