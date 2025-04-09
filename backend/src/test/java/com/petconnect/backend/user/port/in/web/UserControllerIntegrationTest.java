package com.petconnect.backend.user.port.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petconnect.backend.user.application.dto.*;
import com.petconnect.backend.user.domain.model.*;
import com.petconnect.backend.user.domain.repository.UserRepository;
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


import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@link UserController}.
 * Uses PostgreSQL (Docker), security filters, transactional rollback.
 * Verifies retrieving user profiles (own and by ID/email for authorized users)
 * and updating own user profile.
 *
 * @author ibosquet
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class UserControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;

    // --- Tokens ---
    private String adminToken;    // User: admin_london (ID 2, Clinic 1)
    private String ownerToken;      // Dynamically registered owner
    private String vetToken;        // User: admin_barcelona (ID 6, Clinic 5) - used as Vet role example

    // --- User IDs from data.sql ---
    private final Long adminLondonId = 2L;
    private final String ownerRegisteredUsername = "test_owner_for_user_ctrl";
    private final String ownerRegisteredEmail = "test_owner_uc@test.com";


    @BeforeEach
    void setUp() throws Exception {
        // --- Obtain Tokens ---
        adminToken = obtainJwtToken(new AuthLoginRequestDto("admin_london", "password123"));

        // Register a fresh owner for these tests to avoid conflicts and know the ID
        OwnerRegistrationDto ownerReg = new OwnerRegistrationDto(
                ownerRegisteredUsername, ownerRegisteredEmail, "password123", "555-000-111");

        // Ensure this user doesn't exist from a previous failed run if needed (though @Transactional helps)
        userRepository.findByUsername(ownerRegisteredUsername).ifPresent(userRepository::delete);

        // Register Owner
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ownerReg)))
                .andExpect(status().isCreated());

        // Login as the new Owner to get their token
        ownerToken = obtainJwtToken(new AuthLoginRequestDto(ownerReg.username(), ownerReg.password()));

        // Get token for another staff member (e.g., admin_barcelona acting as Vet for permission tests)
        vetToken = obtainJwtToken(new AuthLoginRequestDto("admin_barcelona", "password123"));

        assertThat(adminToken).isNotNull();
        assertThat(ownerToken).isNotNull();
        assertThat(vetToken).isNotNull();
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

    /**
     * Tests for GET /api/users/me
     */
    @Nested
    @DisplayName("GET /api/users/me")
    class GetCurrentUserProfileTests {

        @Test
        @DisplayName("should return Owner profile when called by Owner")
        void getCurrentProfile_whenOwner_shouldReturnOwnerDto() throws Exception {
            mockMvc.perform(get("/api/users/me")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username", is(ownerRegisteredUsername)))
                    .andExpect(jsonPath("$.email", is(ownerRegisteredEmail)))
                    .andExpect(jsonPath("$.roles", contains("OWNER")))
                    .andExpect(jsonPath("$.phone", is("555-000-111")));
        }

        @Test
        @DisplayName("should return ClinicStaff profile when called by Admin")
        void getCurrentProfile_whenAdmin_shouldReturnStaffDto() throws Exception {
            mockMvc.perform(get("/api/users/me")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username", is("admin_london")))
                    .andExpect(jsonPath("$.email", is("admin.london@petconnect.dev")))
                    .andExpect(jsonPath("$.roles", contains("ADMIN")))
                    .andExpect(jsonPath("$.clinicId", is(1))) // Assuming clinic 1 for admin_london
                    .andExpect(jsonPath("$.name", is("John"))); // From data.sql
        }

        @Test
        @DisplayName("should return 401 Unauthorized when no token provided")
        void getCurrentProfile_whenNoToken_shouldReturnUnauthorized() throws Exception {
            mockMvc.perform(get("/api/users/me"))
                    .andExpect(status().isUnauthorized());
        }
    }

    /**
     * Tests for GET /api/users/{id}
     */
    @Nested
    @DisplayName("GET /api/users/{id}")
    class GetUserByIdTests {

        @Test
        @DisplayName("should return 403 Forbidden when Owner requests ANY ID via this endpoint")
        void getUserById_whenOwnerRequestsAnyId_shouldReturnForbidden() throws Exception {
                UserEntity owner = userRepository.findByUsername(ownerRegisteredUsername).orElseThrow();
                Long ownerId = owner.getId();

                mockMvc.perform(get("/api/users/{id}", ownerId)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                        .andExpect(status().isForbidden());

                mockMvc.perform(get("/api/users/{id}", adminLondonId)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                        .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return profile when Admin requests any valid ID")
        void getUserById_whenAdminRequestsAnyId_shouldSucceed() throws Exception {
            // Admin requests the dynamically created owner's profile
            UserEntity owner = userRepository.findByUsername(ownerRegisteredUsername).orElseThrow();
            Long ownerId = owner.getId();

            mockMvc.perform(get("/api/users/{id}", ownerId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)) // Use Admin token
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(ownerId.intValue())))
                    .andExpect(jsonPath("$.username", is(ownerRegisteredUsername)));

            // Admin requests own profile (ID 2 from data.sql)
            mockMvc.perform(get("/api/users/{id}", adminLondonId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)) // Use Admin token
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(adminLondonId.intValue())))
                    .andExpect(jsonPath("$.username", is("admin_london")));
        }

        @Test
        @DisplayName("should return 403 Forbidden when Owner requests another user's ID")
        void getUserById_whenOwnerRequestsOther_shouldReturnForbidden() throws Exception {
            // Owner (dynamic) tries to request Admin London's profile (ID 2)
            mockMvc.perform(get("/api/users/{id}", adminLondonId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)) // Use Owner token
                    .andExpect(status().isForbidden()); // Fails @PreAuthorize check
        }

        @Test
        @DisplayName("should return 404 Not Found when ID does not exist (as Admin)")
        void getUserById_whenIdNotFound_shouldReturnNotFound() throws Exception {
            mockMvc.perform(get("/api/users/{id}", 9999L)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)) // Use Admin token
                    .andExpect(status().isNotFound()); // Expect 404 from orElseGet() in controller
        }

        @Test
        @DisplayName("should return 401 Unauthorized when no token provided")
        void getUserById_whenNoToken_shouldReturnUnauthorized() throws Exception {
            mockMvc.perform(get("/api/users/{id}", adminLondonId))
                    .andExpect(status().isUnauthorized());
        }
    }

    /**
     * Tests for GET /api/users/by-email
     */
    @Nested
    @DisplayName("GET /api/users/by-email")
    class GetUserByEmailTests {

        @Test
        @DisplayName("should return profile when Admin requests valid email")
        void getUserByEmail_whenAdminRequests_shouldSucceed() throws Exception {
            mockMvc.perform(get("/api/users/by-email")
                            .param("email", "admin.london@petconnect.dev") // Email from data.sql
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email", is("admin.london@petconnect.dev")))
                    .andExpect(jsonPath("$.username", is("admin_london")));
        }

        @Test
        @DisplayName("should return 403 Forbidden when Owner requests by email")
        void getUserByEmail_whenOwnerRequests_shouldReturnForbidden() throws Exception {
            mockMvc.perform(get("/api/users/by-email")
                            .param("email", "admin.london@petconnect.dev")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)) // Owner token
                    .andExpect(status().isForbidden()); // Fails @PreAuthorize
        }

        @Test
        @DisplayName("should return 404 Not Found when email does not exist (as Admin)")
        void getUserByEmail_whenEmailNotFound_shouldReturnNotFound() throws Exception {
            mockMvc.perform(get("/api/users/by-email")
                            .param("email", "nosuch@email.com")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 401 Unauthorized when no token provided")
        void getUserByEmail_whenNoToken_shouldReturnUnauthorized() throws Exception {
            mockMvc.perform(get("/api/users/by-email")
                            .param("email", "admin.london@petconnect.dev"))
                    .andExpect(status().isUnauthorized());
        }
    }

    /**
     * Tests for PUT /api/users/me (Update Owner Profile)
     */
    @Nested
    @DisplayName("PUT /api/users/me (Owner Update)")
    class UpdateOwnerProfileTests {
        private OwnerProfileUpdateDto ownerUpdateDto;

        @BeforeEach
        void updateOwnerSetup() {
            ownerUpdateDto = new OwnerProfileUpdateDto("owner_updated_username", "avatar_new.png", "555-UPD-ATED");
        }

        @Test
        @DisplayName("should update own profile successfully when called by Owner")
        void updateOwnProfile_whenOwner_shouldSucceed() throws Exception {
            mockMvc.perform(put("/api/users/me")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken) // Owner's token
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(ownerUpdateDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username", is(ownerUpdateDto.username())))
                    .andExpect(jsonPath("$.avatar", is(ownerUpdateDto.avatar())))
                    .andExpect(jsonPath("$.phone", is(ownerUpdateDto.phone())));
            // Optional: Verify DB change before rollback
        }

        @Test
        @DisplayName("should return 403 Forbidden when called by non-Owner (e.g., Admin)")
        void updateOwnProfile_whenNotOwner_shouldReturnForbidden() throws Exception {
            mockMvc.perform(put("/api/users/me")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken) // Use Admin token
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(ownerUpdateDto)))
                    .andExpect(status().isForbidden()); // Fails @PreAuthorize("hasRole('OWNER')")
        }

        @Test
        @DisplayName("should return 409 Conflict if new username already exists")
        void updateOwnProfile_whenUsernameExists_shouldReturnConflict() throws Exception {
            // Arrange: Use an existing username from data.sql
            OwnerProfileUpdateDto duplicateUsernameDto = new OwnerProfileUpdateDto("admin_london", null, null);

            mockMvc.perform(put("/api/users/me")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(duplicateUsernameDto)))
                    .andExpect(status().isConflict()) // Expect 409 from service validation
                    .andExpect(jsonPath("$.message", containsString("Username already taken")));
        }

        @Test
        @DisplayName("should return 400 Bad Request if update data invalid")
        void updateOwnProfile_whenInvalidData_shouldReturnBadRequest() throws Exception {
            OwnerProfileUpdateDto invalidDto = new OwnerProfileUpdateDto("u", null, null); // Username too short

            mockMvc.perform(put("/api/users/me")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidDto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error", is("Validation Failed")))
                    .andExpect(jsonPath("$.message.username", containsString("must be between 3 and 50")));
        }
    }

    /**
     * Tests for PUT /api/users/me/staff (Update Staff Common Info)
     */
    @Nested
    @DisplayName("PUT /api/users/me/staff (Staff Update)")
    class UpdateStaffProfileTests {
        private UserProfileUpdateDto staffUpdateDto;

        @BeforeEach
        void updateStaffSetup() {
            staffUpdateDto = new UserProfileUpdateDto("admin_london_updated", "new_admin_avatar.png");
        }

        @Test
        @DisplayName("should update own profile successfully when called by Admin")
        void updateOwnProfile_whenAdmin_shouldSucceed() throws Exception {
            mockMvc.perform(put("/api/users/me/staff")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken) // Admin's token
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(staffUpdateDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username", is(staffUpdateDto.username())))
                    .andExpect(jsonPath("$.avatar", is(staffUpdateDto.avatar())));
            // Optional: Verify DB change
        }

        @Test
        @DisplayName("should update own profile successfully when called by Vet")
        void updateOwnProfile_whenVet_shouldSucceed() throws Exception {
            // Using vetToken (which is admin_barcelona's token in this setup)
            UserProfileUpdateDto vetUpdateDto = new UserProfileUpdateDto("admin_barcelona_upd", "vet_avatar.png");
            mockMvc.perform(put("/api/users/me/staff")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken) // Vet's token
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(vetUpdateDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username", is(vetUpdateDto.username())))
                    .andExpect(jsonPath("$.avatar", is(vetUpdateDto.avatar())));
            // Optional: Verify DB change
        }

        @Test
        @DisplayName("should return 403 Forbidden when called by non-Staff (Owner)")
        void updateOwnProfile_whenNotStaff_shouldReturnForbidden() throws Exception {
            mockMvc.perform(put("/api/users/me/staff")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken) // Use Owner token
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(staffUpdateDto)))
                    .andExpect(status().isForbidden()); // Fails @PreAuthorize("hasAnyRole('ADMIN','VET')")
        }

        @Test
        @DisplayName("should return 409 Conflict if new username already exists")
        void updateOwnProfile_whenUsernameExists_shouldReturnConflict() throws Exception {
            // Arrange: Try to update admin_london to use admin_barcelona's username
            UserProfileUpdateDto duplicateUsernameDto = new UserProfileUpdateDto("admin_barcelona", null);

            mockMvc.perform(put("/api/users/me/staff")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken) // Use Admin London's token
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(duplicateUsernameDto)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message", containsString("Username already taken")));
        }

        @Test
        @DisplayName("should return 400 Bad Request if update data invalid")
        void updateOwnProfile_whenInvalidData_shouldReturnBadRequest() throws Exception {
            UserProfileUpdateDto invalidDto = new UserProfileUpdateDto("u", null); // Username too short

            mockMvc.perform(put("/api/users/me/staff")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidDto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error", is("Validation Failed")))
                    .andExpect(jsonPath("$.message.username", containsString("must be between 3 and 50")));
        }
    }
}