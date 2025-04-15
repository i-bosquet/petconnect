package com.petconnect.backend.user.application.dto;

/**
 * A summary DTO for Veterinarian information, typically used within other DTOs like PetProfileDto.
 * Provides basic identification details of the vet.
 *
 * @param id The Vet's unique user ID.
 * @param name The Vet's first name.
 * @param surname The Vet's last name.
 * @author ibosquet
 */
public record VetSummaryDto(Long id, String name, String surname) {
}
