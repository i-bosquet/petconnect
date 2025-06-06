package com.petconnect.backend.user.port.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petconnect.backend.user.application.dto.AuthLoginRequestDto;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.mock.web.MockMultipartFile;

import static com.petconnect.backend.util.IntegrationTestUtils.obtainJwtToken;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

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

        adminToken = obtainJwtToken(mockMvc, objectMapper,adminLoginDto);
        vetToken = obtainJwtToken(mockMvc, objectMapper,vetLoginDto);
        ownerToken = obtainJwtToken(mockMvc, objectMapper,testOwnerLoginDto);
        assertThat(ownerToken).isNotNull();
    }

    /**
     * Tests for GET /api/clinics (Public Access)
     */
    @Nested
    @DisplayName("GET /api/clinics (Public Clinic Search Tests)")
    class GetClinicsPublicTests {

        @Test
        @DisplayName("should return 200 OK with clinic list when accessed publicly without filters")
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
        @DisplayName("should return 200 OK with filtered clinic list when query parameters are provided")
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
    @DisplayName("GET /api/clinics/{id} (Public Clinic Detail Tests)")
    class GetClinicByIdPublicTests {
        @Test
        @DisplayName("should return 200 OK with clinic details when accessed publicly with valid ID")
        void getClinicById_publicAccess_ValidId_shouldSucceed() throws Exception {
            mockMvc.perform(get("/api/clinics/{id}", 1L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.name", containsString("London")));
        }

        @Test
        @DisplayName("should return 404 Not Found when accessed publicly with invalid ID")
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
    @DisplayName("PUT /api/clinics/{id} (Admin Update Clinic Tests)")
    class UpdateClinicTests {

        @Test
        @DisplayName("should return 200 OK and updated ClinicDto when called by authorized Admin of the clinic without files")
        void updateClinic_whenAuthorizedAdminNoFiles_shouldSucceed() throws Exception {
            String clinicUpdateDtoJson = objectMapper.writeValueAsString(clinicUpdateDto);
            MockMultipartFile dtoPart = new MockMultipartFile(
                    "dto",
                    "",
                    MediaType.APPLICATION_JSON_VALUE,
                    clinicUpdateDtoJson.getBytes()
            );

            mockMvc.perform(multipart("/api/clinics/{id}", 1L)
                            .file(dtoPart)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                            .with(request -> {
                                request.setMethod("PUT");
                                return request;
                            }))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.name", is(clinicUpdateDto.name())))
                    .andExpect(jsonPath("$.phone", is(clinicUpdateDto.phone())));
        }

        @Test
        @DisplayName("should return 200 OK and updated ClinicDto when called by authorized Admin with new key files")
        void updateClinic_whenAuthorizedAdminWithFiles_shouldSucceed() throws Exception {
            String clinicUpdateDtoJson = objectMapper.writeValueAsString(clinicUpdateDto);
            MockMultipartFile dtoPart = new MockMultipartFile("dto", "", MediaType.APPLICATION_JSON_VALUE, clinicUpdateDtoJson.getBytes());

            MockMultipartFile publicKeyFile = new MockMultipartFile(
                    "publicKeyFile",
                    "test_pub.pem",
                    MediaType.TEXT_PLAIN_VALUE,
                    "fake public key content".getBytes()
            );
            MockMultipartFile privateKeyFile = new MockMultipartFile(
                    "privateKeyFile",
                    "test_pri.pem",
                    MediaType.TEXT_PLAIN_VALUE,
                    "fake encrypted private key content".getBytes()
            );

            mockMvc.perform(multipart("/api/clinics/{id}", 1L)
                            .file(dtoPart)
                            .file(publicKeyFile)
                            .file(privateKeyFile)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                            .with(request -> {
                                request.setMethod("PUT");
                                return request;
                            }))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name", is(clinicUpdateDto.name())));
        }

        @Test
        @DisplayName("should return 403 Forbidden when called by Admin of a different clinic")
        void updateClinic_whenAdminFromDifferentClinic_shouldReturnForbidden() throws Exception {
            String clinicUpdateDtoJson = objectMapper.writeValueAsString(clinicUpdateDto);
            MockMultipartFile dtoPart = new MockMultipartFile("dto", "",
                    MediaType.APPLICATION_JSON_VALUE, clinicUpdateDtoJson.getBytes());

            mockMvc.perform(multipart("/api/clinics/{id}", 1L)
                            .file(dtoPart)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken)
                            .with(request -> {
                                request.setMethod("PUT");
                                return request;
                            }))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status", is(403)))
                    .andExpect(jsonPath("$.error", is("Forbidden")));
        }

        @Test
        @DisplayName("should return 401 Unauthorized when no authentication token is provided")
        void updateClinic_whenUnauthenticated_shouldReturnUnauthorized() throws Exception {
            String clinicUpdateDtoJson = objectMapper.writeValueAsString(clinicUpdateDto);
            MockMultipartFile dtoPart = new MockMultipartFile("dto", "",
                    MediaType.APPLICATION_JSON_VALUE, clinicUpdateDtoJson.getBytes());

            mockMvc.perform(multipart("/api/clinics/{id}", 1L)
                            .file(dtoPart)
                            .with(request -> {
                                request.setMethod("PUT");
                                return request;
                            }))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("should return 404 Not Found when clinic ID does not exist")
        void updateClinic_whenClinicNotFound_shouldReturnNotFound() throws Exception {
            String clinicUpdateDtoJson = objectMapper.writeValueAsString(clinicUpdateDto);
            MockMultipartFile dtoPart = new MockMultipartFile("dto", "",
                    MediaType.APPLICATION_JSON_VALUE, clinicUpdateDtoJson.getBytes());

            mockMvc.perform(multipart("/api/clinics/{id}", 999L)
                            .file(dtoPart)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                            .with(request -> {
                                request.setMethod("PUT");
                                return request;
                            }))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.error", is("Resource Not Found")));
        }

        @Test
        @DisplayName("should return 403 Forbidden when called by Owner")
        void updateClinic_whenOwner_shouldReturnForbidden() throws Exception {
            String clinicUpdateDtoJson = objectMapper.writeValueAsString(clinicUpdateDto);
            MockMultipartFile dtoPart = new MockMultipartFile("dto", "",
                    MediaType.APPLICATION_JSON_VALUE, clinicUpdateDtoJson.getBytes());

            mockMvc.perform(multipart("/api/clinics/{id}", 1L)
                            .file(dtoPart)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                            .with(request -> {
                                request.setMethod("PUT");
                                return request;
                            }))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status", is(403)))
                    .andExpect(jsonPath("$.error", is("Forbidden")));
        }
    }

    /**
     * Tests for GET /api/clinics/{clinicId}/staff/** (Requires ADMIN/VET Auth)
     */
    @Nested
    @DisplayName("GET /api/clinics/{clinicId}/staff/** (Staff Listing Tests)")
    class GetClinicStaffTests {

        @Test
        @DisplayName("should return 200 OK with all staff list when called by authorized Staff of the clinic")
        void getAllStaffByClinic_whenAuthorized_shouldSucceed() throws Exception {
            mockMvc.perform(get("/api/clinics/{clinicId}/staff/all", 1L)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                    .andExpect(jsonPath("$[0].clinicId", is(1)));
        }

        @Test
        @DisplayName("should return 200 OK with active staff list when called by authorized Staff of the clinic")
        void getActiveStaffByClinic_whenAuthorized_shouldSucceed() throws Exception {
            mockMvc.perform(get("/api/clinics/{clinicId}/staff/active", 1L)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[?(@.isActive == true)]", hasSize(greaterThanOrEqualTo(1))))
                    .andExpect(jsonPath("$[0].clinicId", is(1)));
        }

        @Test
        @DisplayName("should return 403 Forbidden when called by Staff from a different clinic")
        void getStaffByClinic_whenDifferentClinic_shouldReturnForbidden() throws Exception {
            mockMvc.perform(get("/api/clinics/{clinicId}/staff/all", 1L)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status", is(403)))
                    .andExpect(jsonPath("$.error", is("Forbidden")));
        }

        @Test
        @DisplayName("should return 401 Unauthorized when no authentication token is provided")
        void getStaffByClinic_whenUnauthenticated_shouldReturnUnauthorized() throws Exception {
            mockMvc.perform(get("/api/clinics/{clinicId}/staff/all", 1L))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("should return 404 Not Found when clinic ID does not exist")
        void getStaffByClinic_whenClinicNotFound_shouldReturnNotFound() throws Exception {
            mockMvc.perform(get("/api/clinics/{clinicId}/staff/all", 999L)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.error", is("Resource Not Found")));
        }

        @Test
        @DisplayName("should return 403 Forbidden when called by Owner")
        void getStaffByClinic_whenOwner_shouldReturnForbidden() throws Exception {
            mockMvc.perform(get("/api/clinics/{clinicId}/staff/all", 1L)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status", is(403)))
                    .andExpect(jsonPath("$.error", is("Forbidden")));
        }
    }

    /**
     * --- Tests for GET /api/clinics/countries (Public Access) ---
     */
    @Nested
    @DisplayName("GET /api/clinics/countries (Get Distinct Countries Tests)")
    class GetDistinctCountriesTests {

        @Test
        @DisplayName("should return 200 OK with list of distinct country strings")
        void getDistinctCountries_shouldSucceed() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/api/clinics/countries"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(4))))
                    .andExpect(jsonPath("$", hasItem(Country.UNITED_KINGDOM.name())))
                    .andExpect(jsonPath("$", hasItem(Country.SPAIN.name())))
                    .andExpect(jsonPath("$", hasItem(Country.FRANCE.name())))
                    .andExpect(jsonPath("$", hasItem(Country.GERMANY.name())));
        }
    }
}
