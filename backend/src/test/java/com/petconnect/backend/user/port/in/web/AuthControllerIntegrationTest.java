package com.petconnect.backend.user.port.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petconnect.backend.user.application.dto.AuthLoginRequestDto;
import com.petconnect.backend.user.application.dto.OwnerProfileDto;
import com.petconnect.backend.user.application.dto.OwnerRegistrationDto;
import com.petconnect.backend.user.domain.model.Owner;
import com.petconnect.backend.user.domain.model.RoleEnum;
import com.petconnect.backend.user.domain.model.UserEntity;
import com.petconnect.backend.user.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional; // IMPORTANT


import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests it for {@link AuthController} using a real PostgreSQL database (via Docker)
 * and with Spring Security filters enabled.
 * Tests cover user registration and login functionality.
 * Uses {@link MockMvc} to simulate HTTP requests.
 * Each test runs in a transaction rolled back afterward.
 *
 * @author ibosquet
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    // DTOs used in tests
    private OwnerRegistrationDto validRegistrationDto;
    private AuthLoginRequestDto adminLoginDto;
    private AuthLoginRequestDto ownerLoginDto;
    private AuthLoginRequestDto invalidLoginDto;
    private AuthLoginRequestDto nonExistentUserLoginDto;

    @Value("${app.backend.base-url}")
    private String backendBaseUrl;

    @BeforeEach
    void setUp() {
        // Prepare DTOs used across multiple tests
        validRegistrationDto = new OwnerRegistrationDto(
                "integ_owner_" + System.currentTimeMillis(),
                "integ_owner_" + System.currentTimeMillis() + "@test.com",
                "password123",
                "555-123-456"
        );

        // Use credentials from data.sql
        adminLoginDto = new AuthLoginRequestDto("admin_london", "password123");
        ownerLoginDto = new AuthLoginRequestDto(validRegistrationDto.username(), validRegistrationDto.password());
        invalidLoginDto = new AuthLoginRequestDto(adminLoginDto.username(), "wrongPassword");
        nonExistentUserLoginDto = new AuthLoginRequestDto("nosuchuser", "password123");

    }

    /**
     * Tests for POST /api/auth/register
     */
    @Nested
    @DisplayName("POST /api/auth/register (Register Owner Tests)")
    class RegisterTests{

        @Test
        @DisplayName("should return 201 Created and OwnerProfile when registration data is valid")
        void registerOwner_whenValidData_shouldReturnCreatedAndOwnerProfileAndSaveUser() throws Exception {
            // Act & Assert
            MvcResult result = mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRegistrationDto)))
                    .andExpect(status().isCreated())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)) // Use compatible for potential charset variations
                    .andExpect(jsonPath("$.id", is(notNullValue())))
                    .andExpect(jsonPath("$.username", is(validRegistrationDto.username())))
                    .andExpect(jsonPath("$.email", is(validRegistrationDto.email())))
                    .andExpect(jsonPath("$.phone", is(validRegistrationDto.phone())))
                    .andExpect(jsonPath("$.roles", containsInAnyOrder("OWNER")))
                    .andExpect(jsonPath("$.avatar", is(backendBaseUrl+"/images/avatars/users/owner.png")))
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            OwnerProfileDto responseDto = objectMapper.readValue(responseBody, OwnerProfileDto.class);
            Long newUserId = responseDto.id();

            assertThat(newUserId).isNotNull();

            Optional<UserEntity> userOpt = userRepository.findById(newUserId);
            assertThat(userOpt).isPresent();
            UserEntity user = userOpt.get();
            assertThat(user).isInstanceOf(Owner.class);
            assertThat(user.getUsername()).isEqualTo(validRegistrationDto.username());
            assertThat(user.getEmail()).isEqualTo(validRegistrationDto.email());

            assertThat(user.getPassword()).isNotEqualTo(validRegistrationDto.password());
            assertThat(passwordEncoder.matches(validRegistrationDto.password(), user.getPassword())).isTrue();
            assertThat(((Owner) user).getPhone()).isEqualTo(validRegistrationDto.phone());
            assertThat(user.getRoles()).anyMatch(role -> role.getRoleEnum() == RoleEnum.OWNER);
        }

        @Test
        @DisplayName("should return 409 Conflict when email already exists")
        void registerOwner_whenEmailExists_shouldReturnConflict() throws Exception {
            // Arrange
            OwnerRegistrationDto duplicateEmailDto = new OwnerRegistrationDto(
                    "anotheruser",
                    "admin.london@petconnect.dev",
                    "password123", "111"
            );

            // Act & Assert
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(duplicateEmailDto)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status", is(409)))
                    .andExpect(jsonPath("$.error", is("Data Conflict")))
                    .andExpect(jsonPath("$.message", containsString("Email already in use")));
        }

        @Test
        @DisplayName("should return 409 Conflict when username already exists")
        void registerOwner_whenUsernameExists_shouldReturnConflict() throws Exception {
            // Arrange
            OwnerRegistrationDto duplicateUsernameDto = new OwnerRegistrationDto(
                    "admin_london",
                    "new_" + System.currentTimeMillis() + "@test.com",
                    "password123", "111"
            );

            // Act & Assert
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(duplicateUsernameDto)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status", is(409)))
                    .andExpect(jsonPath("$.error", is("Data Conflict")))
                    .andExpect(jsonPath("$.message", containsString("Username already taken")));
        }

        @Test
        @DisplayName("should return 400 Bad Request when username is blank")
        void registerOwner_whenBlankUsername_shouldReturnBadRequest() throws Exception {
            OwnerRegistrationDto invalidDto = new OwnerRegistrationDto("", "valid@email.com", "password123", "12345");

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidDto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message.username",
                            anyOf(containsString("Username cannot be blank"),
                                    containsString("Username must be between 3 and 50 characters"))));
        }

        @Test
        @DisplayName("should return 400 Bad Request when username is too short")
        void registerOwner_whenShortUsername_shouldReturnBadRequest() throws Exception {
            OwnerRegistrationDto invalidDto = new OwnerRegistrationDto("us", "valid@email.com", "password123", "12345"); // Username "us" (longitud 2)

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidDto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message.username", containsString("Username must be between 3 and 50 characters")));
        }

        @Test
        @DisplayName("should return 400 Bad Request with all errors when multiple fields are invalid")
        void registerOwner_whenMultipleInvalidFields_shouldReturnBadRequestWithAllErrors() throws Exception {
            OwnerRegistrationDto invalidDto = new OwnerRegistrationDto("", "valid@email.com", "short", ""); // Blank username/phone, short pass

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidDto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message.username", anyOf(containsString("Username cannot be blank"), containsString("Username must be between 3")))) // Acepta ambos mensajes de username
                    .andExpect(jsonPath("$.message.password", containsString("Password must be at least 8")))
                    .andExpect(jsonPath("$.message.phone", containsString("Phone number cannot be blank")));
        }
    }

    /**
     * Tests for POST /api/auth/login
     */
    @Nested
    @DisplayName("POST /api/auth/login (Login User Tests)")
    class LoginTests{

    @Test
    @DisplayName("should return 200 OK and JWT token when Admin credentials are valid")
    void login_whenAdminCredentialsValid_shouldReturnOkAndToken() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminLoginDto)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.username", is(adminLoginDto.username())))
                .andExpect(jsonPath("$.message", is("UserEntity logged successfully")))
                .andExpect(jsonPath("$.status", is(true)))
                .andExpect(jsonPath("$.jwt", is(notNullValue())))
                .andExpect(jsonPath("$.jwt", matchesRegex("^[A-Za-z0-9-_=]+\\.[A-Za-z0-9-_=]+\\.?[A-Za-z0-9-_.+/=]*$")));
    }

    @Test
    @DisplayName("should return 200 OK and JWT token when newly registered Owner credentials are valid")
    void login_whenOwnerCredentialsValid_shouldReturnOkAndToken() throws Exception {
        // Arrange
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRegistrationDto)))
                .andExpect(status().isCreated());

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ownerLoginDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is(ownerLoginDto.username())))
                .andExpect(jsonPath("$.jwt", is(notNullValue())));
    }

    @Test
    @DisplayName("should return 401 Unauthorized when password is invalid")
    void login_whenPasswordInvalid_shouldReturnUnauthorized() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidLoginDto)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("Authentication Failed")))
                .andExpect(jsonPath("$.message", is("Invalid username or password provided.")));
    }

    @Test
    @DisplayName("should return 401 Unauthorized when username does not exist")
    void login_whenUserNotFound_shouldReturnUnauthorized() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nonExistentUserLoginDto)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("Authentication Failed")))
                .andExpect(jsonPath("$.message", is("User account not found for the provided identifier.")));
    }

    @Test
    @DisplayName("should return 400 Bad Request when input DTO validation fails")
    void login_whenInputInvalid_shouldReturnBadRequest() throws Exception {
        // Arrange
        AuthLoginRequestDto invalidDto = new AuthLoginRequestDto("user", "");

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Validation Failed")))
                .andExpect(jsonPath("$.message.password", is("must not be blank")));
    }
    }
}