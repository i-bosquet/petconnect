package com.petconnect.backend.user.application.dto;

import jakarta.validation.constraints.Size;

/**
 * DTO for receiving clinic update requests.
 *
 * @param name The updated name of the clinic.
 * @param address The updated full street address.
 * @param city The updated city.
 * @param country The updated country.
 * @param phone The updated contact phone number.
 *
 * @author ibosquet
 */
public record ClinicUpdateDto(
        @Size(max = 255, message = "Clinic name cannot exceed 255 characters")
        String name,

        String address,

        @Size(max = 100, message = "City name cannot exceed 100 characters")
        String city,

        @Size(max = 100, message = "Country name cannot exceed 100 characters")
        String country,

        @Size(max = 20, message = "Phone number cannot exceed 20 characters")
        String phone
) {}
