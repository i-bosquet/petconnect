package com.petconnect.backend.user.port.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petconnect.backend.user.application.dto.*;
import com.petconnect.backend.user.domain.model.ClinicStaff;
import com.petconnect.backend.user.domain.model.RoleEnum;
import com.petconnect.backend.user.domain.model.UserEntity;
import com.petconnect.backend.user.domain.model.Vet;
import com.petconnect.backend.user.domain.repository.ClinicStaffRepository;
import com.petconnect.backend.user.domain.repository.UserRepository;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
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


import java.util.Optional;

import static com.petconnect.backend.util.IntegrationTestUtils.extractStaffIdFromResult;
import static com.petconnect.backend.util.IntegrationTestUtils.obtainJwtToken;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@link ClinicStaffController}.
 * Uses PostgresSQL (Docker), security filters, transactional rollback.
 * Verifies creating, updating, activating, and deactivating clinic staff members,
 * including authorization checks based on the authenticated Admin's clinic.
 *
 * @author ibosquet
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
@Slf4j
class ClinicStaffControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private ClinicStaffRepository clinicStaffRepository;
    @Autowired private EntityManager entityManager;

    private String adminLondonToken;
    private String adminBarcelonaToken;
    private String ownerToken;

    private ClinicStaffCreationDto vetCreationDto;
    private ClinicStaffCreationDto adminCreationDto;

    private final Long clinicLondonId = 1L;

//    /**
//     * Sets up required data and gets JWT tokens before each test.
//     * Registers an Owner for testing unauthorized access.
//     * Prepares DTOs for creating staff.
//     */
//    @BeforeEach
//    void setUp() throws Exception {
//        adminLondonToken = obtainJwtToken(mockMvc, objectMapper,new AuthLoginRequestDto("admin_london", "password123"));
//        adminBarcelonaToken = obtainJwtToken(mockMvc, objectMapper,new AuthLoginRequestDto("admin_barcelona", "password123"));
//
//        OwnerRegistrationDto ownerReg = new OwnerRegistrationDto("staff_test_owner_" + System.currentTimeMillis(), "staff.owner."+System.currentTimeMillis()+"@test.com", "password123", "111");
//        mockMvc.perform(post("/api/auth/register")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(ownerReg)))
//                .andExpect(status().isCreated());
//        ownerToken = obtainJwtToken(mockMvc, objectMapper,new AuthLoginRequestDto(ownerReg.username(), ownerReg.password()));
//
//        long timestamp = System.currentTimeMillis();
//        vetCreationDto = new ClinicStaffCreationDto(
//                "test_vet_" + timestamp, "test.vet" + timestamp + "@test.com",
//                "password123", "Test", "Vet", RoleEnum.VET,
//                "VETL" + timestamp, "VETKEY" + timestamp
//        );
//        adminCreationDto = new ClinicStaffCreationDto(
//                "test_admin_" + timestamp, "test.admin" + timestamp + "@test.com",
//                "password123", "Test", "Admin", RoleEnum.ADMIN,
//                null, null
//        );
//        new ClinicStaffUpdateDto("UpdatedName", "UpdatedSurname", "UPD-LIC-" + timestamp, "UPD-KEY-" + timestamp);
//    }

//    /**
//     * Tests for POST /api/staff (Create Staff)
//     */
//    @Nested
//    @DisplayName("POST /api/staff (Create Staff Tests)")
//    class CreateStaffTests {
//
//        @Test
//        @DisplayName("should return 201 Created and VetProfileDto when creating VET by authorized Admin")
//        void createVet_Success() throws Exception {
//            MvcResult result = mockMvc.perform(post("/api/staff")
//                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminLondonToken)
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(objectMapper.writeValueAsString(vetCreationDto)))
//                    .andExpect(status().isCreated())
//                    .andExpect(jsonPath("$.username", is(vetCreationDto.username())))
//                    .andExpect(jsonPath("$.roles", contains("VET")))
//                    .andExpect(jsonPath("$.clinicId", is(clinicLondonId.intValue())))
//                    .andExpect(jsonPath("$.licenseNumber", is(vetCreationDto.licenseNumber())))
//                    .andReturn();
//
//            Long newId = extractStaffIdFromResult(objectMapper, result);
//            Optional<UserEntity> userOpt = userRepository.findById(newId);
//            assertThat(userOpt).isPresent().get().isInstanceOf(Vet.class);
//        }
//
//        @Test
//        @DisplayName("should return 201 Created and StaffProfileDto when creating ADMIN by authorized Admin")
//        void createAdmin_Success() throws Exception {
//            MvcResult result = mockMvc.perform(post("/api/staff")
//                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminLondonToken)
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(objectMapper.writeValueAsString(adminCreationDto)))
//                    .andExpect(status().isCreated())
//                    .andExpect(jsonPath("$.username", is(adminCreationDto.username())))
//                    .andExpect(jsonPath("$.roles", contains("ADMIN")))
//                    .andExpect(jsonPath("$.clinicId", is(clinicLondonId.intValue())))
//                    .andReturn();
//            Long newId = extractStaffIdFromResult(objectMapper, result);
//            Optional<UserEntity> userOpt = userRepository.findById(newId);
//            assertThat(userOpt).isPresent().get().isInstanceOf(ClinicStaff.class);
//            assertThat(userOpt.get()).isNotInstanceOf(Vet.class);
//        }
//
//        @Test
//        @DisplayName("should return 401 Unauthorized when no authentication token is provided")
//        void createStaff_Unauthorized() throws Exception {
//            mockMvc.perform(post("/api/staff")
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(objectMapper.writeValueAsString(adminCreationDto)))
//                    .andExpect(status().isUnauthorized());
//        }
//
//        @Test
//        @DisplayName("should return 403 Forbidden when called by Owner")
//        void createStaff_Forbidden_Owner() throws Exception {
//            mockMvc.perform(post("/api/staff")
//                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(objectMapper.writeValueAsString(adminCreationDto)))
//                    .andExpect(status().isForbidden());
//        }
//
//        @Test
//        @DisplayName("should return 400 Bad Request when creation DTO data is invalid")
//        void createStaff_BadRequest_InvalidData() throws Exception {
//            ClinicStaffCreationDto invalidDto = new ClinicStaffCreationDto("", "", "", "", "", null, null, null);
//            mockMvc.perform(post("/api/staff")
//                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminLondonToken)
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(objectMapper.writeValueAsString(invalidDto)))
//                    .andExpect(status().isBadRequest())
//                    .andExpect(jsonPath("$.error", is("Validation Failed")));
//        }
//
//        @Test
//        @DisplayName("should return 409 Conflict when username already exists")
//        void createStaff_Conflict_UsernameExists() throws Exception {
//            ClinicStaffCreationDto duplicateUserDto = new ClinicStaffCreationDto("admin_london", "unique@test.com", "password123", "Nicholas", "Smith", RoleEnum.ADMIN, null, null);
//            mockMvc.perform(post("/api/staff")
//                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminLondonToken)
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(objectMapper.writeValueAsString(duplicateUserDto)))
//                    .andExpect(status().isConflict())
//                    .andExpect(jsonPath("$.message", containsString("Username already taken")));
//        }
//
//        @Test
//        @DisplayName("should return 409 Conflict when VET license number already exists")
//        void createVet_Conflict_LicenseExists() throws Exception {
//            // Arrange
//            String conflictingLicense = "CONFLICT_LIC_" + System.currentTimeMillis();
//            ClinicStaffCreationDto firstVetDto = new ClinicStaffCreationDto(
//                    "firstvet_" + conflictingLicense, "first." + conflictingLicense + "@test.com",
//                    "password123", "First", "VetLic", RoleEnum.VET,
//                    conflictingLicense,
//                    "KEY_" + conflictingLicense
//            );
//            log.info("Attempting to create first vet with license: {}", conflictingLicense);
//            mockMvc.perform(post("/api/staff")
//                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminLondonToken)
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(objectMapper.writeValueAsString(firstVetDto)))
//                    .andExpect(status().isCreated());
//
//            entityManager.flush();
//
//            ClinicStaffCreationDto duplicateLicenseDto = new ClinicStaffCreationDto(
//                    "vet_dup_lic", "vet_dup_lic@test.com", "password123",
//                    "Dup", "License", RoleEnum.VET,
//                    conflictingLicense,
//                    "UNIQUE_K_" + System.currentTimeMillis()
//            );
//            log.info("Attempting to create second vet with duplicate license: {}", conflictingLicense);
//
//            // Act & Assert
//            mockMvc.perform(post("/api/staff")
//                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminLondonToken)
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(objectMapper.writeValueAsString(duplicateLicenseDto)))
//                    .andExpect(status().isConflict()) // Expect 409
//                    .andExpect(jsonPath("$.message", containsString("License number already in use")));
//        }
//
//        @Test
//        @DisplayName("should return 409 Conflict when VET public key already exists")
//        void createVet_Conflict_PublicKeyExists() throws Exception {
//            // Arrange
//            String conflictingKey = "KEY_" + System.currentTimeMillis();
//            ClinicStaffCreationDto firstVetDto = new ClinicStaffCreationDto(
//                    "firstvetkey_" + conflictingKey, "first.key." + conflictingKey + "@test.com",
//                    "password123", "First", "VetKey", RoleEnum.VET,
//                    "LIC_" + conflictingKey,
//                    conflictingKey
//            );
//            log.info("Attempting to create first vet with public key: {}", conflictingKey);
//            mockMvc.perform(post("/api/staff")
//                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminLondonToken)
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(objectMapper.writeValueAsString(firstVetDto)))
//                    .andExpect(status().isCreated());
//            log.info("First vet created. Flushing EntityManager...");
//
//            entityManager.flush();
//            log.info("EntityManager flushed.");
//
//            ClinicStaffCreationDto duplicateKeyDto = new ClinicStaffCreationDto(
//                    "vet_dup_key", "vet_dup_key@test.com", "password123",
//                    "Dup", "Key", RoleEnum.VET,
//                    "UNIQUE_L_" + System.currentTimeMillis(),
//                    conflictingKey
//            );
//            log.info("Attempting to create second vet with duplicate public key: {}", conflictingKey);
//
//            // Act & Assert
//            mockMvc.perform(post("/api/staff")
//                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminLondonToken)
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(objectMapper.writeValueAsString(duplicateKeyDto)))
//                    .andExpect(status().isConflict())
//                    .andExpect(jsonPath("$.message", containsString("Veterinarian public key is already in use")));
//
//        }
//    }
//
//    /**
//     * Tests for PUT /api/staff/{staffId} (Update)
//     */
//    @Nested
//    @DisplayName("PUT /api/staff/{staffId} (Update Staff Tests)")
//    class UpdateStaffTests {
//
//        private Long staffAdminToUpdateId;
//        private Long staffVetToUpdateId;
//
//        @BeforeEach
//        void setupStaffToUpdate() throws Exception {
//
//            MvcResult adminResult = mockMvc.perform(post("/api/staff")
//                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminLondonToken)
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(objectMapper.writeValueAsString(adminCreationDto)))
//                    .andExpect(status().isCreated()).andReturn();
//            staffAdminToUpdateId = extractStaffIdFromResult(objectMapper, adminResult);
//
//            MvcResult vetResult = mockMvc.perform(post("/api/staff")
//                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminLondonToken)
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(objectMapper.writeValueAsString(vetCreationDto)))
//                    .andExpect(status().isCreated()).andReturn();
//            staffVetToUpdateId = extractStaffIdFromResult(objectMapper, vetResult);
//        }
//
//        @Test
//        @DisplayName("should return 200 OK and updated StaffProfileDto when updating ADMIN by authorized Admin")
//        void updateAdminStaff_Success() throws Exception {
//            ClinicStaffUpdateDto adminUpdate = new ClinicStaffUpdateDto("AdminUpdated", "StaffUpdated", null, null);
//            mockMvc.perform(put("/api/staff/{staffId}", staffAdminToUpdateId)
//                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminLondonToken)
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(objectMapper.writeValueAsString(adminUpdate)))
//                    .andExpect(status().isOk())
//                    .andExpect(jsonPath("$.id", is(staffAdminToUpdateId.intValue())))
//                    .andExpect(jsonPath("$.name", is(adminUpdate.name())))
//                    .andExpect(jsonPath("$.surname", is(adminUpdate.surname())))
//                    .andExpect(jsonPath("$.licenseNumber", is(nullValue())));
//        }
//
//        @Test
//        @DisplayName("should return 200 OK and updated VetProfileDto when updating VET by authorized Admin")
//        void updateVetStaff_Success() throws Exception {
//            ClinicStaffUpdateDto vetUpdate = new ClinicStaffUpdateDto("VetUpdated", "StaffUpdated", "VET_LIC_NEW", "VET_KEY_NEW");
//            mockMvc.perform(put("/api/staff/{staffId}", staffVetToUpdateId) // Update the newly created Vet
//                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminLondonToken)
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(objectMapper.writeValueAsString(vetUpdate)))
//                    .andExpect(status().isOk())
//                    .andExpect(jsonPath("$.id", is(staffVetToUpdateId.intValue())))
//                    .andExpect(jsonPath("$.name", is(vetUpdate.name())))
//                    .andExpect(jsonPath("$.surname", is(vetUpdate.surname())))
//                    .andExpect(jsonPath("$.licenseNumber", is(vetUpdate.licenseNumber())))
//                    .andExpect(jsonPath("$.vetPublicKey", is(vetUpdate.vetPublicKey())));
//        }
//
//        @Test
//        @DisplayName("should return 409 Conflict when updated VET license number already exists")
//        void updateVet_Conflict_DuplicateLicense() throws Exception {
//            // Arrange
//            String conflictingLicense = "LIC_CONFLICT_" + System.currentTimeMillis();
//            ClinicStaffCreationDto otherVetDto = new ClinicStaffCreationDto("other_vet_lic_"+conflictingLicense, "otherlic."+conflictingLicense+"@test.com", "password123", "Other", "VetLic", RoleEnum.VET, conflictingLicense, "OTHER_KEY_"+conflictingLicense);
//            mockMvc.perform(post("/api/staff").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminLondonToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(otherVetDto))).andExpect(status().isCreated());
//
//            entityManager.flush();
//            log.info("Conflicting vet created and flushed.");
//
//            ClinicStaffUpdateDto updateToDuplicateLicenseDto = new ClinicStaffUpdateDto("NameChangeLic", "SurnameChangeLic", conflictingLicense, "UNIQUE_KEY_FOR_LIC_TEST_" + System.currentTimeMillis());
//            log.info("Attempting to update Vet {} with conflicting license {}", staffVetToUpdateId, conflictingLicense);
//
//            // Act & Assert
//            mockMvc.perform(put("/api/staff/{staffId}", staffVetToUpdateId)
//                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminLondonToken)
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(objectMapper.writeValueAsString(updateToDuplicateLicenseDto)))
//                    .andExpect(status().isConflict())
//                    .andExpect(jsonPath("$.message", containsString("License number already in use")));
//            log.info("Conflict correctly detected for license: {}", conflictingLicense);
//        }
//
//        @Test
//        @DisplayName("should return 409 Conflict when updated VET public key already exists")
//        void updateVet_Conflict_DuplicatePublicKey() throws Exception {
//            // Arrange
//            String conflictingKey = "KEY_CONFLICT_" + System.currentTimeMillis();
//            ClinicStaffCreationDto otherVetDto = new ClinicStaffCreationDto("other_vet_key_"+conflictingKey, "otherkey."+conflictingKey+"@test.com", "password123", "Other", "VetKey", RoleEnum.VET, "OTHER_LIC_"+conflictingKey, conflictingKey);
//            mockMvc.perform(post("/api/staff").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminLondonToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(otherVetDto))).andExpect(status().isCreated());
//
//            entityManager.flush();
//            log.info("Conflicting key vet created and flushed.");
//
//            ClinicStaffUpdateDto conflictingKeyUpdate = new ClinicStaffUpdateDto("NameChangeKey", "SurnameChangeKey", "UNIQUE_LIC_FOR_KEY_TEST_" + System.currentTimeMillis(), conflictingKey);
//            log.info("Attempting to update Vet {} with conflicting public key {}", staffVetToUpdateId, conflictingKey);
//
//            // Act & Assert
//            mockMvc.perform(put("/api/staff/{staffId}", staffVetToUpdateId)
//                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminLondonToken)
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(objectMapper.writeValueAsString(conflictingKeyUpdate)))
//                    .andExpect(status().isConflict())
//                    .andExpect(jsonPath("$.message", containsString("Veterinarian public key is already in use")));
//            log.info("Conflict correctly detected for public key: {}", conflictingKey);
//        }
//
//
//        @Test
//        @DisplayName("should return 403 Forbidden when called by Admin from a different clinic")
//        void updateStaff_Forbidden_DifferentClinic() throws Exception {
//            ClinicStaffUpdateDto update = new ClinicStaffUpdateDto("Attempt", "Failed", null, null);
//            mockMvc.perform(put("/api/staff/{staffId}", staffAdminToUpdateId)
//                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminBarcelonaToken)
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(objectMapper.writeValueAsString(update)))
//                    .andExpect(status().isForbidden());
//        }
//
//        @Test
//        @DisplayName("should return 401 Unauthorized when no authentication token is provided")
//        void updateStaff_Unauthorized() throws Exception {
//            ClinicStaffUpdateDto update = new ClinicStaffUpdateDto("Attempt", "Failed", null, null);
//            mockMvc.perform(put("/api/staff/{staffId}", staffAdminToUpdateId)
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(objectMapper.writeValueAsString(update)))
//                    .andExpect(status().isUnauthorized());
//        }
//
//        @Test
//        @DisplayName("should return 404 Not Found when staff ID does not exist")
//        void updateStaff_NotFound() throws Exception {
//            ClinicStaffUpdateDto update = new ClinicStaffUpdateDto("Attempt", "Failed", null, null);
//            mockMvc.perform(put("/api/staff/{staffId}", 9999L)
//                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminLondonToken)
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(objectMapper.writeValueAsString(update)))
//                    .andExpect(status().isNotFound());
//        }
//    }

    /**
     * Tests for PUT /api/staff/{staffId}/activate|deactivate
     */
    @Nested
    @DisplayName("PUT /api/staff/{staffId}/activate & deactivate (Toggle Staff Status Tests)")
    class ToggleStaffStatusTests {
        private Long staffToToggleId;

        @BeforeEach
        void setupStaffToToggle() throws Exception {
            MvcResult result = mockMvc.perform(post("/api/staff")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminLondonToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(vetCreationDto)))
                    .andExpect(status().isCreated()).andReturn();
            staffToToggleId = extractStaffIdFromResult(objectMapper, result);
            assertThat(staffToToggleId).isNotNull();
        }

        @Test
        @DisplayName("should return 200 OK and updated profile when deactivating active staff by authorized Admin")
        void deactivateStaff_Success() throws Exception {
            mockMvc.perform(put("/api/staff/{staffId}/deactivate", staffToToggleId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminLondonToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isActive", is(false)));
            // Verify DB
            Optional<ClinicStaff> staffOpt = clinicStaffRepository.findById(staffToToggleId);
            assertThat(staffOpt)
                    .isPresent()
                    .hasValueSatisfying(staff -> assertThat(staff.isActive()).isFalse());
        }

        @Test
        @DisplayName("should return 200 OK and updated profile when activating inactive staff by authorized Admin")
        void activateStaff_Success() throws Exception {
            mockMvc.perform(put("/api/staff/{staffId}/deactivate", staffToToggleId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminLondonToken))
                    .andExpect(status().isOk());
            mockMvc.perform(put("/api/staff/{staffId}/activate", staffToToggleId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminLondonToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isActive", is(true)));
            Optional<ClinicStaff> staffOpt = clinicStaffRepository.findById(staffToToggleId);
            assertThat(staffOpt)
                    .isPresent()
                    .hasValueSatisfying(staff -> assertThat(staff.isActive()).isTrue());
        }

        @Test
        @DisplayName("should return 400 Bad Request when deactivating already inactive staff")
        void deactivateStaff_Error_AlreadyInactive() throws Exception {
            mockMvc.perform(put("/api/staff/{staffId}/deactivate", staffToToggleId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminLondonToken))
                    .andExpect(status().isOk());
            mockMvc.perform(put("/api/staff/{staffId}/deactivate", staffToToggleId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminLondonToken))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("is already inactive")));
        }

        @Test
        @DisplayName("should return 400 Bad Request when activating already active staff")
        void activateStaff_Error_AlreadyActive() throws Exception {
            mockMvc.perform(put("/api/staff/{staffId}/activate", staffToToggleId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminLondonToken))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("is already active")));
        }

        @Test
        @DisplayName("should return 400 Bad Request when Admin attempts self-deactivation")
        void deactivateStaff_Error_SelfDeactivation() throws Exception {
            Long adminLondonId = 2L;
            mockMvc.perform(put("/api/staff/{staffId}/deactivate", adminLondonId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminLondonToken))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("cannot deactivate their own account")));
        }

        @Test
        @DisplayName("should return 403 Forbidden when called by Admin from a different clinic")
        void toggleStatus_Forbidden_DifferentClinic() throws Exception {
            mockMvc.perform(put("/api/staff/{staffId}/deactivate", staffToToggleId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminBarcelonaToken))
                    .andExpect(status().isForbidden());
            mockMvc.perform(put("/api/staff/{staffId}/activate", staffToToggleId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminBarcelonaToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 401 Unauthorized when no authentication token is provided")
        void toggleStatus_Unauthorized() throws Exception {
            mockMvc.perform(put("/api/staff/{staffId}/deactivate", staffToToggleId))
                    .andExpect(status().isUnauthorized());
            mockMvc.perform(put("/api/staff/{staffId}/activate", staffToToggleId))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("should return 404 Not Found when staff ID does not exist")
        void toggleStatus_NotFound() throws Exception {
            mockMvc.perform(put("/api/staff/{staffId}/deactivate", 9999L)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminLondonToken))
                    .andExpect(status().isNotFound());
            mockMvc.perform(put("/api/staff/{staffId}/activate", 9999L)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminLondonToken))
                    .andExpect(status().isNotFound());
        }
    }
}