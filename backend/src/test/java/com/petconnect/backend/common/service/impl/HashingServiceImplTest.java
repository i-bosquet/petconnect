package com.petconnect.backend.common.service.impl;

import com.petconnect.backend.exception.HashingException;
import com.petconnect.backend.common.service.HashingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for {@link HashingServiceImpl}.
 * Verifies the SHA-256 hashing logic.
 *
 * @author ibosquet
 */
class HashingServiceImplTest {

    private HashingService hashingService;

    @BeforeEach
    void setUp() {
        hashingService = new HashingServiceImpl();
    }

    @Test
    @DisplayName("hashString should return correct SHA-256 hex string for known input")
    void hashString_Success_KnownInput() throws HashingException {
        // Arrange
        String input = "TestInputString123!@#";
        String expectedHash = "6084e87876e062b6f2efcba9fa66baea37b50df1380e615645ed6573b612abee";

        // Act
        String actualHash = hashingService.hashString(input);

        // Assert
        assertThat(actualHash)
                .isNotNull()
                .isEqualToIgnoringCase(expectedHash)
                .hasSize(64);
    }

    @Test
    @DisplayName("hashString should return different hashes for different inputs")
    void hashString_Success_DifferentInputs() throws HashingException {
        // Arrange
        String input1 = "Input One";
        String input2 = "Input Two";

        // Act
        String hash1 = hashingService.hashString(input1);
        String hash2 = hashingService.hashString(input2);

        // Assert
        assertThat(hash1).isNotNull().isNotBlank().hasSize(64);
        assertThat(hash2).isNotNull().isNotBlank().hasSize(64);
        assertThat(hash1).isNotEqualTo(hash2);
    }

    @Test
    @DisplayName("hashString should return same hash for same input")
    void hashString_Success_SameInput() throws HashingException {
        // Arrange
        String input = "Consistent Input";

        // Act
        String hash1 = hashingService.hashString(input);
        String hash2 = hashingService.hashString(input);

        // Assert
        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    @DisplayName("hashString should throw IllegalArgumentException for null input")
    void hashString_Failure_NullInput() {
        // Act & Assert
        assertThatThrownBy(() -> hashingService.hashString(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Input string for hashing cannot be null");
    }
}