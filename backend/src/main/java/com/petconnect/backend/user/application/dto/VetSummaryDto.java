package com.petconnect.backend.user.application.dto;

/**
 * A data transfer object representing a summary of a veterinarian.
 * Provides essential details about the vet and their associated clinic, useful for list views or overviews.
 *
 * @param id The unique identifier of the veterinarian.
 * @param name The first name of the veterinarian.
 * @param surname The last name of the veterinarian.
 * @param avatar The URL or path to the veterinarian's profile avatar image.
 * @param email The email address associated with the veterinarian's account.
 * @param licenseNumber The professional license number of the veterinarian.
 * @param clinicId The unique identifier of the clinic where the veterinarian works.
 * @param clinicName The official name of the associated clinic.
 * @param clinicAddress The full street address of the clinic.
 * @param clinicCity The city where the clinic is located.
 * @param clinicCountry The country where the clinic is located.
 * @param clinicPhone The primary contact phone number of the clinic.
 *
 * @author ibosquet
 */
public record VetSummaryDto(
        Long id,
        String name,
        String surname,
        String avatar,
        String email,
        String licenseNumber,
        Long clinicId,
        String clinicName,
        String clinicAddress,
        String clinicCity,
        String clinicCountry,
        String clinicPhone
) {}