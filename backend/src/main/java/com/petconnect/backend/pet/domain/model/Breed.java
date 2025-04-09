package com.petconnect.backend.pet.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a specific breed within a species (e.g., Labrador Retriever within DOG).
 * This allows for more detailed pet classification.
 * Inherits auditing fields from BaseEntity.
 *
 * @author ibosquet
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "breed")
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
    @Size(max = 100, message = "Breed name cannot exceed 100 characters")
    @Column(name = "name", nullable = false, length = 100)
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
     * Typically fetched lazily. Cascade operations are usually not needed here
     * (managing pets lifecycle independently).
     */
    @OneToMany(
            mappedBy = "breed", // Field in Pet entity that owns the relationship
            fetch = FetchType.LAZY // Load pets only when explicitly requested
    )
    @Builder.Default // Initialize for builder pattern
    private List<Pet> pets = new ArrayList<>();
}
