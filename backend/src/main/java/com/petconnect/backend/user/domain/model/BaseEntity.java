package com.petconnect.backend.user.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Abstract base class for entities requiring auditing information.
 * Includes common fields like id, created/updated timestamps, and created/updated user identifiers.
 * Uses Spring Data JPA Auditing for automatic population of audit fields.
 *
 * @author ibosquet
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {
    /**
     * The unique identifier for the entity.
     * Generated automatically using a sequence strategy.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "entity_id_seq")
    @SequenceGenerator(name = "entity_id_seq", sequenceName = "entity_id_sequence", allocationSize = 1)
    private Long id;

    /**
     * Timestamp when the entity was first created.
     * Automatically set by Spring Data JPA Auditing.
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when the entity was last modified.
     * Automatically set by Spring Data JPA Auditing.
     */
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Identifier (e.g., username/email) of the user who created the entity.
     * Automatically set by Spring Data JPA Auditing via AuditorAware.
     * Nullable to handle system-generated entities or initial data.
     */
    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private String createdBy;

    /**
     * Identifier (e.g., username/email) of the user who last modified the entity.
     * Automatically set by Spring Data JPA Auditing via AuditorAware.
     * Nullable.
     */
    @LastModifiedBy
    @Column(name = "updated_by")
    private String updatedBy;
}
