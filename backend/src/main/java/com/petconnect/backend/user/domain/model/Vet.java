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
     * The path to the public cryptographic key file associated with the veterinarian,
     * relative to the application's classpath.
     * Used for verifying digital signatures created by this vet.
     * Cannot be blank. Must be unique across all veterinarians.
     */
    @NotBlank(message = "Veterinarian public key path cannot be blank")
    @Column(name = "vet_public_key", nullable = false, unique = true, length = 255)
    private String vetPublicKey;

    /**
     * The server-side path to the veterinarian's ENCRYPTED private key PEM file.
     * This path is internal to the server's file system or a configured secure storage.
     * The key itself is encrypted with the vet's personal password.
     */
    @NotBlank(message = "Path to vet's encrypted private key file cannot be blank")
    @Column(name = "vet_private_key", nullable = false, unique = true, length = 255)
    private String vetPrivateKey;
}
