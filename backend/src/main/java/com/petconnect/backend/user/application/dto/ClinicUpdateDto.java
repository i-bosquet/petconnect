package com.petconnect.backend.user.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO for receiving clinic update requests.
 * Contains only the fields that are allowed to be updated by an Admin.
 * Includes validation constraints.
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
        @NotBlank(message = "Clinic name cannot be blank")
        @Size(max = 255, message = "Clinic name cannot exceed 255 characters")
        String name,

        @NotBlank(message = "Address cannot be blank")
        String address,

        @NotBlank(message = "City cannot be blank")
        @Size(max = 100, message = "City name cannot exceed 100 characters")
        String city,

        @NotBlank(message = "Country cannot be blank")
        @Size(max = 100, message = "Country name cannot exceed 100 characters")
        String country,

        @NotBlank(message = "Phone number cannot be blank")
        @Size(max = 20, message = "Phone number cannot exceed 20 characters")
        String phone
        // Note: publicKey is likely not updatable via this DTO
) {}
