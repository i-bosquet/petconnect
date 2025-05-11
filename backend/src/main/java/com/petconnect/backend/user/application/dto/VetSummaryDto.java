package com.petconnect.backend.user.application.dto;

/**
 * Data Transfer Object representing a summary of a Veterinarian,
 * including their avatar, email, and basic details of their primary clinic.
 * Used when listing vets associated with a pet.
 *
 * @param id           The ID of the Veterinarian.
 * @param name         The first name of the Veterinarian.
 * @param surname      The surname of the Veterinarian.
 * @param avatar       The URL/path to the Veterinarian's avatar.
 * @param email        The email of the Veterinarian.
 * @param clinicId     The ID of the Vet's primary clinic.
 * @param clinicName   The name of the Vet's primary clinic.
 * @param clinicCity   The city of the Vet's primary clinic.
 * @param clinicCountry The country of the Vet's primary clinic.
 * @param clinicPhone  The phone number of the Vet's primary clinic.
 * @author ibosquet
 */
public record VetSummaryDto(
        Long id,
        String name,
        String surname,
        String avatar,
        String email,
        Long clinicId,
        String clinicName,
        String clinicAddress,
        String clinicCity,
        String clinicCountry,
        String clinicPhone
) {}