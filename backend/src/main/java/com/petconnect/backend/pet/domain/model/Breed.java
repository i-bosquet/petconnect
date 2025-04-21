package com.petconnect.backend.pet.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a specific breed within a species (e.g., Labrador Retriever within DOG).
 * This allows for more detailed pet classification.
 * Inherits auditing fields from BaseEntity.
 *
 * @author ibosquet
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "breed", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"name", "specie"}, name = "en_breed_name_specie")})
@ToString(exclude = {"pets"})
public class Breed{

    /**
     * The unique identifier for the breed.
     * Uses a sequence generator for automatic ID assignment.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "breed_id_seq")
    @SequenceGenerator(name = "breed_id_seq", sequenceName = "breed_id_sequence", allocationSize = 1)
    private Long id;

    /**
     * The name of the breed (e.g., "Labrador Retriever", "Siamese", "Holland Lop").
     * Cannot be blank and has a maximum length.
     */
    @NotBlank(message = "Breed name cannot be blank")
    @Size(max = 50, message = "Breed name cannot exceed 50 characters")
    @Column(name = "name", nullable = false, length = 50)
    private String name;

    /**
     * The species to which this breed belongs.
     * Stored as a string in the database (EnumType.STRING).
     * Cannot be null.
     */
    @NotNull(message = "Species cannot be null")
    @Enumerated(EnumType.STRING)
    @Column(name = "specie", nullable = false)
    private Specie specie;

    @Column(
            name = "image_url",
            columnDefinition = "TEXT")
    private String imageUrl;

    /**
     * The list of pets associated with this breed.
     * This is the inverse side of the ManyToOne relationship in Pet.
     * Typically, fetched lazily. Cascade operations are usually not needed here
     * (managing pets lifecycle independently).
     */
    @OneToMany(
            mappedBy = "breed", // Field in Pet entity that owns the relationship
            fetch = FetchType.LAZY // Load pets only when explicitly requested
    )
    @Builder.Default // Initialize for a builder pattern
    private List<Pet> pets = new ArrayList<>();

    /**
     * Determines equality based solely on the entity's unique identifier (ID).
     * Two {@code Breed} instances are considered equal if they both have a non-null ID
     * and their IDs are equal. This implementation is robust against changes
     * in other attributes and handles JPA proxy objects correctly by using {@code getClass()}.
     * Entities without an ID (transient) are only equal if they are the same instance.
     *
     * @param o The object to compare this {@code Breed} against.
     * @return {@code true} if the given object represents the same entity (based on ID), {@code false} otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Breed breed = (Breed) o;
        return id != null && Objects.equals(id, breed.id);
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
        return id != null ? Objects.hash(id) : getClass().hashCode();
    }
}
