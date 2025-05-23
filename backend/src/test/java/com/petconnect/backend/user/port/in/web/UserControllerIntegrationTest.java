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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static com.petconnect.backend.util.IntegrationTestUtils.obtainJwtToken;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for {@link UserController}.
 * Uses PostgresSQL (Docker), security filters, transactional rollback.
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

    private String adminToken;
    private String ownerToken;
    private String vetToken;

    private final Long adminLondonId = 2L;
    private final String ownerRegisteredUsername = "test_owner_for_user_ctrl";
    private final String ownerRegisteredEmail = "test_owner_uc@test.com";


    @BeforeEach
    void setUp() throws Exception {
        adminToken = obtainJwtToken(mockMvc, objectMapper, new AuthLoginRequestDto("admin_london", "password123"));

        OwnerRegistrationDto ownerReg = new OwnerRegistrationDto(
                ownerRegisteredUsername, ownerRegisteredEmail, "password123", "555-000-111");

        userRepository.findByUsername(ownerRegisteredUsername).ifPresent(userRepository::delete);
        userRepository.findByEmail(ownerRegisteredEmail).ifPresent(userRepository::delete);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ownerReg)))
                .andExpect(status().isCreated());

        ownerToken = obtainJwtToken(mockMvc, objectMapper, new AuthLoginRequestDto(ownerReg.username(), ownerReg.password()));
        vetToken = obtainJwtToken(mockMvc, objectMapper, new AuthLoginRequestDto("admin_barcelona", "password123"));

        assertThat(adminToken).isNotNull();
        assertThat(ownerToken).isNotNull();
        assertThat(vetToken).isNotNull();
    }

    /**
     * Tests for GET /api/users/me
     */
    @Nested
    @DisplayName("GET /api/users/me (Get Current User Profile Tests)")
    class GetCurrentUserProfileTests {

        @Test
        @DisplayName("should return 200 OK and OwnerProfileDto when called by authenticated Owner")
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
        @DisplayName("should return 200 OK and ClinicStaffProfileDto when called by authenticated Admin")
        void getCurrentProfile_whenAdmin_shouldReturnStaffDto() throws Exception {
            mockMvc.perform(get("/api/users/me")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username", is("admin_london")))
                    .andExpect(jsonPath("$.email", is("admin.london@petconnect.dev")))
                    .andExpect(jsonPath("$.roles", contains("ADMIN")))
                    .andExpect(jsonPath("$.clinicId", is(1)))
                    .andExpect(jsonPath("$.name", is("John")));
        }

        @Test
        @DisplayName("should return 401 Unauthorized when no authentication token is provided")
        void getCurrentProfile_whenNoToken_shouldReturnUnauthorized() throws Exception {
            mockMvc.perform(get("/api/users/me"))
                    .andExpect(status().isUnauthorized());
        }
    }

    /**
     * Tests for GET /api/users/{id}
     */
    @Nested
    @DisplayName("GET /api/users/{id} (Get User By ID Tests)")
    class GetUserByIdTests {

        @Test
        @DisplayName("should return 403 Forbidden when Owner requests any user ID")
        void getUserById_whenOwnerRequestsAnyId_shouldReturnForbidden() throws Exception {
            // Arrange
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
        @DisplayName("should return 200 OK and UserProfileDto when Admin requests own profile by ID")
        void getUserById_whenAdminRequestsSelf_shouldSucceed() throws Exception {
            mockMvc.perform(get("/api/users/{id}", adminLondonId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(adminLondonId.intValue())))
                    .andExpect(jsonPath("$.username", is("admin_london")));
        }

        @Test
        @DisplayName("should return 403 Forbidden when Admin requests Owner profile by ID")
        void getUserById_whenAdminRequestsOwner_shouldReturnForbidden() throws Exception {
            // Arrange
            UserEntity owner = userRepository.findByUsername(ownerRegisteredUsername).orElseThrow();
            Long ownerId = owner.getId();

            mockMvc.perform(get("/api/users/{id}", ownerId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status", is(403)))
                    .andExpect(jsonPath("$.error", is("Forbidden")));
        }

        @Test
        @DisplayName("should return 403 Forbidden when Owner requests another user's profile by ID")
        void getUserById_whenOwnerRequestsOther_shouldReturnForbidden() throws Exception {
            mockMvc.perform(get("/api/users/{id}", adminLondonId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                    .andExpect(status().isForbidden()); // Fails @PreAuthorize check
        }

        @Test
        @DisplayName("should return 404 Not Found when Admin requests non-existent user ID")
        void getUserById_whenIdNotFound_shouldReturnNotFound() throws Exception {
            mockMvc.perform(get("/api/users/{id}", 9999L)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 401 Unauthorized when no authentication token is provided")
        void getUserById_whenNoToken_shouldReturnUnauthorized() throws Exception {
            mockMvc.perform(get("/api/users/{id}", adminLondonId))
                    .andExpect(status().isUnauthorized());
        }
    }

    /**
     * Tests for GET /api/users/by-email
     */
    @Nested
    @DisplayName("GET /api/users/by-email (Get User By Email Tests)")
    class GetUserByEmailTests {

        @Test
        @DisplayName("should return 200 OK and UserProfileDto when Admin requests staff from same clinic by email")
        void getUserByEmail_whenAdminRequests_shouldSucceed() throws Exception {
            mockMvc.perform(get("/api/users/by-email")
                            .param("email", "admin.london@petconnect.dev")
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
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 404 Not Found when Admin requests non-existent email")
        void getUserByEmail_whenEmailNotFound_shouldReturnNotFound() throws Exception {
            mockMvc.perform(get("/api/users/by-email")
                            .param("email", "nosuch@email.com")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 401 Unauthorized when no authentication token is provided")
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
    @DisplayName("PUT /api/users/me (Update Owner Profile Tests)")
    class UpdateOwnerProfileTests {
        private OwnerProfileUpdateDto ownerUpdateDto;
        private MockMultipartFile ownerUpdateDtoPart;

        @BeforeEach
        void updateOwnerSetup() throws Exception {
            ownerUpdateDto = new OwnerProfileUpdateDto("owner_updated_username", null,"555-555-5555");
            String dtoJson = objectMapper.writeValueAsString(ownerUpdateDto);
            ownerUpdateDtoPart = new MockMultipartFile("dto", "", MediaType.APPLICATION_JSON_VALUE, dtoJson.getBytes());
        }

        @Test
        @DisplayName("should return 200 OK and updated profile when Owner updates own profile (no image)")
        void updateOwnProfile_whenOwnerNoImage_shouldSucceed() throws Exception {
            mockMvc.perform(multipart("/api/users/me")
                            .file(ownerUpdateDtoPart)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                            .with(request -> {
                                request.setMethod("PUT");
                                return request;
                            }))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.profile.username", is(ownerUpdateDto.username())))
                    .andExpect(jsonPath("$.profile.phone", is(ownerUpdateDto.phone())));
        }

        @Test
        @DisplayName("should return 200 OK and updated profile when Owner updates own profile with image")
        void updateOwnProfile_whenOwnerWithImage_shouldSucceed() throws Exception {
            MockMultipartFile imageFile = new MockMultipartFile(
                    "imageFile",
                    "avatar.png",
                    MediaType.IMAGE_PNG_VALUE,
                    "fakeImageData".getBytes()
            );

            mockMvc.perform(multipart("/api/users/me")
                            .file(ownerUpdateDtoPart)
                            .file(imageFile)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                            .with(request -> {
                                request.setMethod("PUT");
                                return request;
                            }))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.profile.username", is(ownerUpdateDto.username())));
        }


        @Test
        @DisplayName("should return 403 Forbidden when non-Owner attempts to update owner profile")
        void updateOwnProfile_whenNotOwner_shouldReturnForbidden() throws Exception {
            mockMvc.perform(multipart("/api/users/me")
                            .file(ownerUpdateDtoPart)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                            .with(request -> { request.setMethod("PUT"); return request; }))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 409 Conflict when updated username already exists")
        void updateOwnProfile_whenUsernameExists_shouldReturnConflict() throws Exception {
            OwnerProfileUpdateDto duplicateUsernameDto = new OwnerProfileUpdateDto("admin_london",null, null);
            String dtoJson = objectMapper.writeValueAsString(duplicateUsernameDto);
            MockMultipartFile dtoPart = new MockMultipartFile("dto", "", MediaType.APPLICATION_JSON_VALUE, dtoJson.getBytes());

            mockMvc.perform(multipart("/api/users/me")
                            .file(dtoPart)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                            .with(request -> { request.setMethod("PUT"); return request; }))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message", containsString("Username already taken: admin_london")));
        }

        @Test
        @DisplayName("should return 400 Bad Request when update DTO data is invalid (username too short)")
        void updateOwnProfile_whenInvalidData_shouldReturnBadRequest() throws Exception {
            UserProfileUpdateDto invalidDto = new UserProfileUpdateDto("u", null);
            String dtoJson = objectMapper.writeValueAsString(invalidDto);
            MockMultipartFile dtoPart = new MockMultipartFile("dto", "", MediaType.APPLICATION_JSON_VALUE, dtoJson.getBytes());

            mockMvc.perform(multipart("/api/users/me/staff")
                            .file(dtoPart)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                            .with(request -> { request.setMethod("PUT"); return request; }))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error", is("Validation Failed")))
                    .andExpect(jsonPath("$.message.username", containsString("size must be between 3 and 50")));
        }
    }

    /**
     * Tests for PUT /api/users/me/staff (Update Staff Common Info)
     */
    @Nested
    @DisplayName("PUT /api/users/me/staff (Update Staff Profile Tests)")
    class UpdateStaffProfileTests {
        private UserProfileUpdateDto staffUpdateDto;
        private MockMultipartFile staffUpdateDtoPart;

        @BeforeEach
        void updateStaffSetup() throws Exception  {
            staffUpdateDto = new UserProfileUpdateDto("admin_london_updated", null);
            String dtoJson = objectMapper.writeValueAsString(staffUpdateDto);
            staffUpdateDtoPart = new MockMultipartFile("dto", "", MediaType.APPLICATION_JSON_VALUE, dtoJson.getBytes());
        }

        @Test
        @DisplayName("should return 200 OK and updated profile when Admin updates own profile (no image)")
        void updateOwnProfile_whenAdminNoImage_shouldSucceed() throws Exception {
            mockMvc.perform(multipart("/api/users/me/staff")
                            .file(staffUpdateDtoPart)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                            .with(request -> { request.setMethod("PUT"); return request; }))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.profile.username", is(staffUpdateDto.username())));
        }

        @Test
        @DisplayName("should return 200 OK and updated profile when Admin updates own profile with image")
        void updateOwnProfile_whenAdminWithImage_shouldSucceed() throws Exception {
            MockMultipartFile imageFile = new MockMultipartFile(
                    "imageFile", "avatar.png", MediaType.IMAGE_PNG_VALUE, "fakeImageData".getBytes());

            mockMvc.perform(multipart("/api/users/me/staff")
                            .file(staffUpdateDtoPart)
                            .file(imageFile)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                            .with(request -> { request.setMethod("PUT"); return request; }))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.profile.username", is(staffUpdateDto.username())));
        }

        @Test
        @DisplayName("should return 200 OK and updated StaffProfileDto when Vet updates own profile")
        void updateOwnProfile_whenVet_shouldSucceed() throws Exception {
            UserProfileUpdateDto vetUpdateDto = new UserProfileUpdateDto("admin_barcelona_upd", null);
            String dtoJson = objectMapper.writeValueAsString(vetUpdateDto);
            MockMultipartFile dtoPart = new MockMultipartFile("dto", "", MediaType.APPLICATION_JSON_VALUE, dtoJson.getBytes());

            mockMvc.perform(multipart("/api/users/me/staff")
                            .file(dtoPart)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken)
                            .with(request -> { request.setMethod("PUT"); return request; }))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.profile.username", is(vetUpdateDto.username())));
        }

        @Test
        @DisplayName("should return 403 Forbidden when Owner attempts to update staff profile")
        void updateOwnProfile_whenNotStaff_shouldReturnForbidden() throws Exception {
            mockMvc.perform(multipart("/api/users/me/staff")
                            .file(staffUpdateDtoPart)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                            .with(request -> { request.setMethod("PUT"); return request; }))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 409 Conflict when updated username already exists")
        void updateOwnProfile_whenUsernameExists_shouldReturnConflict() throws Exception {
            OwnerProfileUpdateDto duplicateUsernameDto = new OwnerProfileUpdateDto("admin_london", null, null);
            String dtoJson = objectMapper.writeValueAsString(duplicateUsernameDto);
            MockMultipartFile dtoPart = new MockMultipartFile("dto", "", MediaType.APPLICATION_JSON_VALUE, dtoJson.getBytes());

            mockMvc.perform(multipart("/api/users/me")
                            .file(dtoPart)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                            .with(request -> { request.setMethod("PUT"); return request; }))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message", is("Username already taken: admin_london")));
        }

        @Test
        @DisplayName("should return 400 Bad Request when update DTO data is invalid")
        void updateOwnProfile_whenInvalidData_shouldReturnBadRequest() throws Exception {
            OwnerProfileUpdateDto invalidDto = new OwnerProfileUpdateDto("u", null,null);
            String dtoJson = objectMapper.writeValueAsString(invalidDto);
            MockMultipartFile dtoPart = new MockMultipartFile("dto", "", MediaType.APPLICATION_JSON_VALUE, dtoJson.getBytes());

            mockMvc.perform(multipart("/api/users/me")
                            .file(dtoPart)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                            .with(request -> { request.setMethod("PUT"); return request; }))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error", is("Validation Failed")))
                    .andExpect(jsonPath("$.message.username", containsString("size must be between 3 and 50")));
        }
    }
}