package com.petconnect.backend.user.application.dto;

import com.petconnect.backend.user.domain.model.Country;

/**
 * DTO for transferring detailed Clinic information (typically in responses).
 * Using Java Record for immutability and conciseness.
 * Includes fields inherited from BaseEntity like id, createdAt etc. if needed in response.
 *
 * @param id The unique identifier of the clinic.
 * @param name The official name of the clinic.
 * @param address The full street address.
 * @param city The city where the clinic is located.
 * @param country The country where the clinic is located.
 * @param phone The primary contact phone number.
 * @param publicKey The clinic's public key (usually not needed in simple list views).
 *
 * @author ibosquet
 */
public record ClinicDto(
        Long id,
        String name,
        String address,
        String city,
        Country country,
        String phone,
        String publicKey // Consider if publicKey should always be returned
) {}

