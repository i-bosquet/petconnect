package com.petconnect.backend.pet.domain.model;

import com.petconnect.backend.user.domain.model.BaseEntity;
import com.petconnect.backend.user.domain.model.Clinic;
import com.petconnect.backend.user.domain.model.Owner;
import com.petconnect.backend.user.domain.model.Vet;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a pet animal registered in the system.
 * Contains identification details, owner information, medical status,
 * and links to breed and potentially associated veterinarians.
 * Inherits auditing fields from BaseEntity.
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
@Table(name = "pet", indexes = { // Add index for faster lookup by microchip
        @Index(name = "idx_pet_microchip", columnList = "microchip", unique = true)
})
public class Pet extends BaseEntity {

    /**
     * The given name of the pet.
     * Cannot be blank.
     */
    @NotBlank(message = "Pet name cannot be blank")
    @Size(max = 100, message = "Pet name cannot exceed 100 characters")
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * The primary color or description of the pet's coat/appearance. Optional.
     */
    @Size(max = 100, message = "Color description cannot exceed 100 characters")
    @Column(name = "color", length = 100)
    private String color;

    /**
     * The gender of the pet. Optional.
     * Stored as a string representation of the Gender enum.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "gender")
    private Gender gender;

    /**
     * The date of birth of the pet. Optional.
     * Must be a date in the past or present.
     */
    @PastOrPresent(message = "Birth date must be in the past or present")
    @Column(name = "birth_date")
    private LocalDate birthDate;

    /**
     * The unique microchip identification number implanted in the pet.
     * Must be unique across all pets. Optional during initial registration,
     * but likely required for activation or certificate generation.
     * Indexed for efficient lookup.
     */
    @Size(max = 50, message = "Microchip number cannot exceed 50 characters")
    @Column(name = "microchip", unique = true, length = 50)
    private String microchip;

    /**
     * URL or path to the pet's primary image/avatar.
     * Cannot be null (a default can be assigned by the application).
     */
    @NotBlank(message = "Image path/URL cannot be blank")
    @Column(name = "image", nullable = false)
    private String image;

    /**
     * The current status of the pet within the system (Pending, Active, Inactive).
     * Cannot be null. Defaults likely handled in service layer upon creation.
     */
    @NotNull(message = "Pet status cannot be null")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PetStatus status;

    // --- Relationships ---

    /**
     * The Owner user who registered and owns this pet.
     * This is the owning side of the relationship. Cannot be null.
     * Fetched lazily for performance.
     */
    @NotNull(message = "Pet must have an owner")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private Owner owner;

    /**
     * The Breed of the pet. Optional.
     * Fetched lazily.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "breed_id")
    private Breed breed;

    /**
     * Reference to the Clinic where activation is pending. Optional.
     * This field is populated when an owner associates a PENDING pet
     * with a specific clinic for activation (H-20). It should be cleared
     * once the pet becomes ACTIVE. Fetched lazily.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pending_activation_clinic_id")
    private Clinic pendingActivationClinic;

    /**
     * The set of Veterinarians associated with providing care for this pet.
     * Mapped using a Many-to-Many relationship through the join table "pet_vet_association".
     * Fetched lazily. Cascade type is typically limited (PERSIST, MERGE) as Vets exist independently.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "pet_vet_association",
            joinColumns = @JoinColumn(name = "pet_id", referencedColumnName = "id"), // FK to pet table
            inverseJoinColumns = @JoinColumn(name = "vet_id", referencedColumnName = "user_id"), // FK to vet table (user_id)
            foreignKey = @ForeignKey(name = "fk_petvet_pet"),
            inverseForeignKey = @ForeignKey(name = "fk_petvet_vet")
    )
    @Builder.Default
    private Set<Vet> associatedVets = new HashSet<>();

    /*
    @OneToMany(mappedBy = "pet", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Record> medicalRecords = new ArrayList<>(); // Medical Record domain
    */

    /*
    @OneToMany(mappedBy = "pet", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE) // Removing pet might remove certs
    @Builder.Default
    private List<Certificate> certificates = new ArrayList<>(); // Certificate domain
    */

    // --- Helper methods for managing associatedVets collection (optional but good practice) ---

    /**
     * Associates a Veterinarian with this Pet.
     * @param vet The Vet to associate.
     */
    public void addVet(Vet vet) {
        this.associatedVets.add(vet);
    }

    /**
     * Disassociates a Veterinarian from this Pet.
     * @param vet The Vet to disassociate.
     */
    public void removeVet(Vet vet) {
        this.associatedVets.remove(vet);
    }

}