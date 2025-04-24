package com.petconnect.backend.user.port.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petconnect.backend.user.application.dto.AuthLoginRequestDto;
import com.petconnect.backend.user.application.dto.AuthResponseDto;
import com.petconnect.backend.user.application.dto.ClinicUpdateDto;
import com.petconnect.backend.user.application.dto.OwnerRegistrationDto;
import com.petconnect.backend.user.domain.model.Country;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for {@link ClinicController}.
 * Uses PostgresSQL (Docker), security filters, transactional rollback.
 * Verifies public access, filtering, pagination, and authorized updates/reads.
 *
 * @author ibosquet
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class ClinicControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    private String adminToken;
    private String vetToken;
    private String ownerToken;

    private ClinicUpdateDto clinicUpdateDto;

    @BeforeEach
    void setUp() throws Exception {
        AuthLoginRequestDto adminLoginDto = new AuthLoginRequestDto("admin_london", "password123");
        AuthLoginRequestDto vetLoginDto = new AuthLoginRequestDto("admin_barcelona", "password123");
        clinicUpdateDto = new ClinicUpdateDto("Updated Clinic Name", "123 New Street", "London", Country.UNITED_KINGDOM, "02098765432");

        OwnerRegistrationDto  testOwnerRegDto = new OwnerRegistrationDto(
                "auth_test_owner_" + System.currentTimeMillis(),
                "auth_test_owner_" + System.currentTimeMillis() + "@test.com",
                "password123", "000111222");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testOwnerRegDto)))
                .andExpect(status().isCreated());

        AuthLoginRequestDto  testOwnerLoginDto = new AuthLoginRequestDto(testOwnerRegDto.username(), testOwnerRegDto.password());

        adminToken = obtainJwtToken(adminLoginDto);
        vetToken = obtainJwtToken(vetLoginDto);
        ownerToken = obtainJwtToken(testOwnerLoginDto);
        assertThat(ownerToken).isNotNull();
    }

    /**
     * Helper method to perform login via MockMvc and extract the JWT token.
     * @param loginRequest DTO with login credentials.
     * @return The JWT token string.
     * @throws Exception If MockMvc perform fails.
     */
    private String obtainJwtToken(AuthLoginRequestDto loginRequest) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jwt", is(notNullValue())))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        AuthResponseDto responseDto = objectMapper.readValue(responseBody, AuthResponseDto.class);
        return responseDto.jwt();
    }

    /**
     * Tests for GET /api/clinics (Public Access)
     */
    @Nested
    @DisplayName("GET /api/clinics (Public Search)")
    class GetClinicsPublicTests {

        @Test
        @DisplayName("should return list of clinics without authentication")
        void getClinics_publicAccess_shouldSucceed() throws Exception {
            mockMvc.perform(get("/api/clinics")
                            .param("page", "0")
                            .param("size", "5"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.content", hasSize(lessThanOrEqualTo(5))))
                    .andExpect(jsonPath("$.totalElements", is(greaterThanOrEqualTo(5))));
        }

        @Test
        @DisplayName("should return filtered clinics based on query parameters")
        void getClinics_withFilters_shouldReturnFilteredList() throws Exception {
            mockMvc.perform(get("/api/clinics")
                            .param("country", Country.UNITED_KINGDOM.name()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(2)))
                    .andExpect(jsonPath("$.content[0].country", is(Country.UNITED_KINGDOM.name())))
                    .andExpect(jsonPath("$.content[1].country", is(Country.UNITED_KINGDOM.name())));

            mockMvc.perform(get("/api/clinics")
                            .param("name", "Paris"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].name", containsString("Paris")));

            mockMvc.perform(get("/api/clinics")
                            .param("city", "Barcelona"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].city", is("Barcelona")))
                    .andExpect(jsonPath("$.content[0].country", is(Country.SPAIN.name())));
        }
    }

    /**
     * Tests for GET /api/clinics/{id} (Public Access)
     */
    @Nested
    @DisplayName("GET /api/clinics/{id} (Public Detail)")
    class GetClinicByIdPublicTests {
        @Test
        @DisplayName("should return clinic details without authentication for valid ID")
        void getClinicById_publicAccess_ValidId_shouldSucceed() throws Exception {
            mockMvc.perform(get("/api/clinics/{id}", 1L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.name", containsString("London")));
        }

        @Test
        @DisplayName("should return 404 without authentication for invalid ID")
        void getClinicById_publicAccess_InvalidId_shouldReturnNotFound() throws Exception {
            mockMvc.perform(get("/api/clinics/{id}", 999L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.error", is("Resource Not Found")));
        }
    }

    /**
     * Tests for PUT /api/clinics/{id} (Requires ADMIN Auth)
     */
    @Nested
    @DisplayName("PUT /api/clinics/{id} (Admin Update)")
    class UpdateClinicTests {

        @Test
        @DisplayName("should update clinic successfully when authenticated as Admin of that clinic")
        void updateClinic_whenAuthorizedAdmin_shouldSucceed() throws Exception {
            mockMvc.perform(put("/api/clinics/{id}", 1L)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(clinicUpdateDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.name", is(clinicUpdateDto.name())))
                    .andExpect(jsonPath("$.phone", is(clinicUpdateDto.phone())));
        }

        @Test
        @DisplayName("should return 403 Forbidden when authenticated as Admin of different clinic")
        void updateClinic_whenAdminFromDifferentClinic_shouldReturnForbidden() throws Exception {
            mockMvc.perform(put("/api/clinics/{id}", 1L)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(clinicUpdateDto)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status", is(403)))
                    .andExpect(jsonPath("$.error", is("Forbidden")));
        }

        @Test
        @DisplayName("should return 401 Unauthorized when no token is provided")
        void updateClinic_whenUnauthenticated_shouldReturnUnauthorized() throws Exception {
            mockMvc.perform(put("/api/clinics/{id}", 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(clinicUpdateDto)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("should return 404 Not Found when clinic ID does not exist")
        void updateClinic_whenClinicNotFound_shouldReturnNotFound() throws Exception {
            mockMvc.perform(put("/api/clinics/{id}", 999L)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(clinicUpdateDto)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.error", is("Resource Not Found")));
        }

        @Test
        @DisplayName("should return 403 Forbidden when authenticated as Owner")
        void updateClinic_whenOwner_shouldReturnForbidden() throws Exception {
            mockMvc.perform(put("/api/clinics/{id}", 1L)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(clinicUpdateDto)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status", is(403)))
                    .andExpect(jsonPath("$.error", is("Forbidden")));
        }
    }

    /**
     * Tests for GET /api/clinics/{clinicId}/staff/** (Requires ADMIN/VET Auth)
     */
    @Nested
    @DisplayName("GET /api/clinics/{clinicId}/staff/** (Staff Listing)")
    class GetClinicStaffTests {

        @Test
        @DisplayName("getAllStaffByClinic should succeed when authenticated as Staff of that clinic")
        void getAllStaffByClinic_whenAuthorized_shouldSucceed() throws Exception {
            mockMvc.perform(get("/api/clinics/{clinicId}/staff/all", 1L)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                    .andExpect(jsonPath("$[0].clinicId", is(1)));
        }

        @Test
        @DisplayName("getActiveStaffByClinic should succeed when authenticated as Staff of that clinic")
        void getActiveStaffByClinic_whenAuthorized_shouldSucceed() throws Exception {
            mockMvc.perform(get("/api/clinics/{clinicId}/staff/active", 1L)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[?(@.isActive == true)]", hasSize(greaterThanOrEqualTo(1))))
                    .andExpect(jsonPath("$[0].clinicId", is(1)));
        }

        @Test
        @DisplayName("get staff should return 403 Forbidden when authenticated Staff from different clinic")
        void getStaffByClinic_whenDifferentClinic_shouldReturnForbidden() throws Exception {
            mockMvc.perform(get("/api/clinics/{clinicId}/staff/all", 1L)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status", is(403)))
                    .andExpect(jsonPath("$.error", is("Forbidden")));
        }

        @Test
        @DisplayName("get staff should return 401 Unauthorized when no token is provided")
        void getStaffByClinic_whenUnauthenticated_shouldReturnUnauthorized() throws Exception {
            mockMvc.perform(get("/api/clinics/{clinicId}/staff/all", 1L))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("get staff should return 404 Not Found when clinic ID does not exist")
        void getStaffByClinic_whenClinicNotFound_shouldReturnNotFound() throws Exception {
            mockMvc.perform(get("/api/clinics/{clinicId}/staff/all", 999L)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.error", is("Resource Not Found")));
        }

        @Test
        @DisplayName("get staff should return 403 Forbidden when authenticated as Owner")
        void getStaffByClinic_whenOwner_shouldReturnForbidden() throws Exception {
            mockMvc.perform(get("/api/clinics/{clinicId}/staff/all", 1L)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status", is(403)))
                    .andExpect(jsonPath("$.error", is("Forbidden")));
        }
    }
}
