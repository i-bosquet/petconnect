package com.petconnect.backend.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petconnect.backend.pet.application.dto.PetProfileDto;
import com.petconnect.backend.record.application.dto.RecordViewDto;
import com.petconnect.backend.user.application.dto.*;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Utility class providing helper methods specifically for integration tests.
 * This includes common tasks like getting JWT tokens via simulated login requests
 * and extracting IDs or other data from MockMvc results.
 * Methods are static for easy use across different integration test classes.
 *
 * @author ibosquet
 */
public class IntegrationTestUtils {

    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private IntegrationTestUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Gets a JWT token by performing a login request.
     *
     * @param mockMvc        The MockMvc instance from the test class.
     * @param objectMapper   The ObjectMapper instance from the test class.
     * @param loginRequest   The login credentials DTO.
     * @return The JWT token string.
     * @throws Exception If the MockMvc call fails.
     */
    public static String obtainJwtToken(MockMvc mockMvc, ObjectMapper objectMapper, AuthLoginRequestDto loginRequest) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();
        AuthResponseDto responseDto = objectMapper.readValue(result.getResponse().getContentAsString(), AuthResponseDto.class);
        return responseDto.jwt();
    }

    /**
     * Extracts the Pet ID from the JSON response of an MvcResult.
     * Assumes the response body contains a PetProfileDto.
     *
     * @param objectMapper The ObjectMapper instance from the test class.
     * @param result       The MvcResult containing the response.
     * @return The extracted Pet ID.
     * @throws Exception If JSON parsing fails.
     */
    public static Long extractPetIdFromResult(ObjectMapper objectMapper, MvcResult result) throws Exception {
        String json = result.getResponse().getContentAsString();
        PetProfileDto dto = objectMapper.readValue(json, PetProfileDto.class);
        return dto.id();
    }

    /**
     * Extracts the Record ID from the JSON response of an MvcResult.
     * Assumes the response body contains a RecordViewDto.
     *
     * @param objectMapper The ObjectMapper instance from the test class.
     * @param result       The MvcResult containing the response.
     * @return The extracted Record ID.
     * @throws Exception If JSON parsing fails.
     */
    public static Long extractRecordIdFromResult(ObjectMapper objectMapper, MvcResult result) throws Exception {
        String json = result.getResponse().getContentAsString();
        RecordViewDto dto = objectMapper.readValue(json, RecordViewDto.class);
        return dto.id();
    }

    /**
     * Extracts the Clinic Staff ID from the JSON response of an MvcResult.
     * Assumes the response body contains a JSON representation of {@link ClinicStaffProfileDto}.
     *
     * @param objectMapper The ObjectMapper instance from the test class.
     * @param result       The MvcResult containing the HTTP response.
     * @return The extracted Clinic Staff User ID.
     * @throws Exception If JSON parsing fails or the ID is not found.
     */
    public static Long extractStaffIdFromResult(ObjectMapper objectMapper, MvcResult result) throws Exception {
        String json = result.getResponse().getContentAsString();
        ClinicStaffProfileDto dto = objectMapper.readValue(json, ClinicStaffProfileDto.class);
        if (dto == null || dto.id() == null) {
            throw new IllegalStateException("Could not extract Clinic Staff ID from MvcResult: " + json);
        }
        return dto.id();
    }
}
