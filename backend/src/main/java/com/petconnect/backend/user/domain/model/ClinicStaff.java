package com.petconnect.backend.user.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Represents a staff member working at a veterinary clinic (either Vet or Admin).
 * Inherits common user fields from UserEntity and adds staff-specific fields like name, surname,
 * active status, and the associated clinic.
 * Uses JPA JOINED inheritance strategy as it's a base class for Vet.
 *
 * @author ibosquet
 */
@Data
@EqualsAndHashCode(callSuper = true, exclude = {"clinic"})
@ToString(callSuper = true, exclude = {"clinic"})
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "clinic_staff")
@PrimaryKeyJoinColumn(name = "user_id")
@Inheritance(strategy = InheritanceType.JOINED)
public class ClinicStaff extends UserEntity {
    /**
     * The first name of the clinic staff member.
     * Cannot be blank. Maximum length of 100 characters.
     */
    @NotBlank(message = "First name cannot be blank")
    @Size(max = 100, message = "First name cannot exceed 100 characters")
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * The last name (surname) of the clinic staff member.
     * Cannot be blank. Maximum length of 100 characters.
     */
    @NotBlank(message = "Surname cannot be blank")
    @Size(max = 100, message = "Surname cannot exceed 100 characters")
    @Column(name = "surname", nullable = false, length = 100)
    private String surname;

    /**
     * Flag indicating if the staff member's account is currently active within the clinic.
     * Defaults to true. Cannot be null.
     */
    @NotNull
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    /**
     * The clinic where this staff member works.
     * This is the owning side of the OneToMany relationship in Clinic.
     * Cannot be null. Fetched lazily by default for ManyToOne.
     */
    @NotNull(message = "Clinic association cannot be null")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clinic_id", nullable = false)
    private Clinic clinic;
}
