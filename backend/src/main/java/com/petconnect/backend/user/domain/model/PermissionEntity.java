package com.petconnect.backend.user.domain.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * Represents a permission entity used for managing user roles and access rights.
 * Each permission is identified by a unique name.
 *
 * @author ibosquet
 */
@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "permissions")
public class PermissionEntity {

    /**
     * Unique identifier for the permission.
     * Automatically generated using the identity strategy.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique name of the permission.
     * Cannot be null and is not modifiable after creation.
     */
    @Column(unique = true, nullable = false, updatable = false)
    private String name;

    /**
     * A detailed explanation of what the permission allows or restricts.
     * Helps clarify the purpose beyond the permission name.
     * Can be null.
     */
    @Column(columnDefinition = "TEXT")
    private String description;
}
