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
import java.util.Objects;
import java.util.Set;

/**
 * Represents a pet animal registered in the system.
 * Contains identification details, owner information, medical status,
 * and links to breed and potentially associated veterinarians.
 * Inherits auditing fields from BaseEntity.
 *
 * @author ibosquet
 */
@Getter
@Setter
@ToString(callSuper = true, exclude = {"owner", "breed", "associatedVets"})
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "pet", indexes = {
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
     * The date of birth of the pet. Must be a date in the past or present.
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
     * Cannot be null. Defaults are likely handled in the service layer upon creation.
     */
    @NotNull(message = "Pet status cannot be null")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PetStatus status;

    /**
     * Determines equality based solely on the entity's unique identifier (ID).
     * Two {@code Pet} instances are considered equal if they both have a non-null ID
     * and their IDs are equal. This implementation is robust against changes
     * in other attributes and handles JPA proxy objects correctly by using {@code getClass()}.
     * Entities without an ID (transient) are only equal if they are the same instance.
     *
     * @param o The object to compare this {@code Pet} against.
     * @return {@code true} if the given object represents the same entity (based on ID), {@code false} otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pet pet = (Pet) o;
        return getId() != null && Objects.equals(getId(), pet.getId());
    }

    /**
     * Computes the hash code based solely on the entity's unique identifier (ID).
     * This implementation is consistent with the {@link #equals(Object)} method.
     * If the entity has a non-null ID, the hash code is derived from the ID.
     * If the entity is transient (ID is null), it uses the default hash code
     * provided by {@code Object.hashCode()} (based on object identity) or {@code getClass().hashCode()}
     * to ensure consistency during the entity lifecycle within a persistence context or collections.
     *
     * @return The hash code based on the entity's ID, or the class's hash code if the ID is null.
     */
    @Override
    public int hashCode() {
        return getId() != null ? Objects.hash(getId()) : getClass().hashCode();
    }

    // --- Relationships ---

    /**
     * The Owner user who registered and owns this pet.
     * This is the owning side of the relationship. Cannot be null.
     * Fetched lazily for performance.
     */
    @NotNull(message = "Pet must have an owner")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false, foreignKey = @ForeignKey(name = "fk_pet_owner"))
    private Owner owner;

    /**
     * The Breed of the pet. Required. A default 'Mixed/Other' breed should be
     * assigned if a specific one is not selected during registration/update.
     * Fetched lazily.
     */
    @NotNull(message = "Pet must have a breed assigned (use 'Mixed/Other' if specific breed is unknown)")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "breed_id", nullable = false, foreignKey = @ForeignKey(name = "fk_pet_breed"))
    private Breed breed;

    /**
     * Reference to the Clinic where activation is pending. Optional.
     * This field is populated when an owner associates a PENDING pet
     * with a specific clinic for activation (H-20). It should be cleared
     * once the pet becomes ACTIVE. Fetched lazily.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pending_activation_clinic_id", foreignKey = @ForeignKey(name = "fk_pet_pending_clinic"))
    private Clinic pendingActivationClinic;

    /**
     * Represents the clinic associated with a pending certificate request for a pet.
     * This field maps to the "pending_certificate_clinic_id" column in the database
     * and establishes a many-to-one relationship with the {@code Clinic} entity.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pending_certificate_clinic_id", foreignKey = @ForeignKey(name = "fk_pet_pending_cert_clinic"))
    private Clinic pendingCertificateClinic;

    /**
     * The set of Veterinarians associated with providing care for this pet.
     * Mapped using a Many-to-Many relationship through the join table "pet_vet_association".
     * Fetched lazily. Cascade type is typically limited (PERSIST, MERGE) as Vets exist independently.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "pet_vet_association",
            joinColumns = @JoinColumn(name = "pet_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "vet_id", referencedColumnName = "user_id"),
            foreignKey = @ForeignKey(name = "fk_petvet_pet"),
            inverseForeignKey = @ForeignKey(name = "fk_petvet_vet")
    )
    @Builder.Default
    private Set<Vet> associatedVets = new HashSet<>();

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