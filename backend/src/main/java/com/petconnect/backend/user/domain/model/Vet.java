package com.petconnect.backend.user.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * Represents a Veterinarian user in the system, who is also a Clinic Staff member.
 * Inherits fields from ClinicStaff (and transitively from UserEntity and BaseEntity).
 * Adds veterinarian-specific fields like license number and public key.
 * Uses JPA JOINED inheritance strategy.
 *
 * @author ibosquet
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "vet")
@PrimaryKeyJoinColumn(name = "user_id")
public class Vet extends ClinicStaff {

    /**
     * The official license number of the veterinarian.
     * Must be unique across all veterinarians. Cannot be blank.
     */
    @NotBlank(message = "License number cannot be blank")
    @Column(name = "license_number", nullable = false, unique = true)
    private String licenseNumber;

    /**
     * The public cryptographic key associated with the veterinarian.
     * Used for verifying digital signatures created by this vet (e.g., on medical records).
     * Cannot be blank. Stored as TEXT for potentially long keys.
     * Must be unique across all veterinarians.
     */
    @NotBlank(message = "Veterinarian public key cannot be blank")
    @Column(name = "vet_public_key", nullable = false, unique = true, columnDefinition = "TEXT")
    private String vetPublicKey; // Consider byte[] if storing raw key bytes
}
