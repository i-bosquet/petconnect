package com.petconnect.backend.record.domain.model;

import com.petconnect.backend.pet.domain.model.Pet;
import com.petconnect.backend.user.domain.model.BaseEntity;
import com.petconnect.backend.user.domain.model.Clinic;
import com.petconnect.backend.user.domain.model.UserEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * Represents a single entry in a pet's medical history.
 * Can be created by Owners (informative) or Clinic Staff (clinical/vaccine).
 * Records created by Vets can be digitally signed.
 * Extends BaseEntity for ID and auditing fields (createdAt, createdBy, etc.).
 *
 * @author ibosquet
 */
@Getter
@Setter
@ToString(callSuper = true, exclude = {"pet", "creator", "vaccine"})
@EqualsAndHashCode(callSuper = true, exclude = {"pet", "creator", "vaccine"})
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "record")
public class Record extends BaseEntity {
    /**
     * The type of medical record (e.g., VACCINE, ANNUAL_CHECK).
     * Determines the context and potential associated data (like Vaccine details).
     * Cannot be null. Stored as a string.
     */
    @NotNull(message = "Record type cannot be null")
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private RecordType type;

    /**
     * A textual description of the visit, diagnosis, treatment, or observation.
     * Can be detailed. Optional.
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * The digital signature provided by a Veterinarian when they finalize
     * and attest to the content of this record. Null if not signed or created by Owner/Admin.
     * The format and verification method depend on the chosen cryptographic approach.
     */
    @Column(name = "vet_signature", columnDefinition = "TEXT")
    private String vetSignature;

    /**
     * Flag indicating if this record is considered immutable, typically because
     * it has been used as the basis for a generated Certificate.
     * Defaults to false. Once set to true, deletion or modification might be prevented.
     */
    @NotNull
    @Column(name = "is_immutable", nullable = false)
    private boolean isImmutable = false;

    // --- Relationships ---

    /**
     * The Pet to whom this medical record belongs.
     * This relationship is mandatory. Fetched lazily.
     */
    @NotNull(message = "Record must belong to a Pet")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pet_id", nullable = false, foreignKey = @ForeignKey(name = "fk_record_pet"))
    private Pet pet;

    /**
     * The UserEntity (Owner or ClinicStaff/Vet) who created this record entry.
     * This relationship is mandatory. Fetched lazily.
     * Note: BaseEntity.createdBy stores the username string via AuditorAware for logging.
     * This field stores the actual UserEntity object relation.
     */
    @NotNull(message = "Record must have a creator")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creator_user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_record_creator"))
    private UserEntity creator;
    /**
     * Optional detailed vaccine information. This is populated only when the record
     * 'type' is VACCINE. The relationship is owned by the Vaccine entity (via 'recordEntity' field)
     * but managed here via cascade options.
     */
    @OneToOne(mappedBy = "recordEntity", cascade = CascadeType.ALL, orphanRemoval = true)
    private Vaccine vaccine;

    // --- Convenience Methods ---

    /**
     * Sets the vaccine details and establishes the bidirectional link.
     * Should only be used when the record type is VACCINE.
     * @param vaccine The Vaccine entity containing the details.
     */
    public void setVaccineDetails(Vaccine vaccine) {
        if (vaccine != null) {
            vaccine.setRecordEntity(this);
        }
        this.vaccine = vaccine;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinic_id", foreignKey = @ForeignKey(name = "fk_record_clinic"))
    private Clinic createdInClinic; // Null if created by the Owner
}
