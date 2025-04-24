package com.petconnect.backend.user.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a veterinary clinic registered in the system.
 * Contains clinic details and manages associated staff members.
 * Inherits auditing fields from BaseEntity.
 *
 * @author ibosquet
 */
@Data
@EqualsAndHashCode(callSuper = true, exclude = {"staff"}) // Inherit BaseEntity's equals/hashCode, exclude collections
@ToString(callSuper = true, exclude = {"staff"}) // Inherit BaseEntity's toString, exclude collections to avoid recursion
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "clinic")
public class Clinic extends BaseEntity{

    /**
     * The official name of the clinic.
     * Cannot be blank. Maximum length of 255 characters.
     */
    @NotBlank(message = "Clinic name cannot be blank")
    @Size(max = 255, message = "Clinic name cannot exceed 255 characters")
    @Column(name = "name", nullable = false)
    private String name;

    /**
     * The full street address of the clinic.
     * Cannot be blank.
     */
    @NotBlank(message = "Address cannot be blank")
    @Column(name = "address", nullable = false, columnDefinition = "TEXT")
    private String address;

    /**
     * The city where the clinic is located.
     * Cannot be blank. Maximum length of 100 characters.
     */
    @NotBlank(message = "City cannot be blank")
    @Size(max = 100, message = "City name cannot exceed 100 characters")
    @Column(name = "city", nullable = false, length = 100)
    private String city;

    /**
     * The country where the clinic is located.
     * Stored as a string in the database based on the Enum name.
     * Cannot be null.
     */
    @NotNull(message = "Country cannot be null")
    @Enumerated(EnumType.STRING)
    @Column(name = "country", nullable = false)
    private Country country;

    /**
     * The primary contact phone number for the clinic.
     * Cannot be blank. Maximum length of 20 characters.
     */
    @NotBlank(message = "Phone number cannot be blank")
    @Size(max = 20, message = "Phone number cannot exceed 20 characters")
    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    /**
     * The public cryptographic key associated with the clinic.
     * Used for verifying digital signatures originating from the clinic.
     * Cannot be null. Stored as TEXT for potentially long keys.
     * Must be unique across all clinics.
     */
    @NotBlank(message = "Clinic public key cannot cannot be blank")
    @Column(name = "public_key", nullable = false, unique = true, columnDefinition = "TEXT")
    private String publicKey;

    /**
     * The list of staff members (Vets, Admins) associated with this clinic.
     * Mapped by the 'clinic' field in the ClinicStaff entity.
     * Fetched a lazily and cascade type is typically ALL for managing staff lifecycle with the clinic,
     * or specific types like PERSIST, MERGE, REMOVE depending on requirements.
     * orphanRemoval=true ensures that if a staff member is removed from this list
     * and is not associated with any other clinic (which shouldn't happen with ManyToOne),
     * it gets deleted from the database.
     */
    @OneToMany(
            mappedBy = "clinic",
            cascade = CascadeType.ALL,
            fetch = FetchType.LAZY,
            orphanRemoval = true
    )
    @Builder.Default
    private List<ClinicStaff> staff = new ArrayList<>();
}
