package com.petconnect.backend.user.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents a role entity used for managing user roles.
 * Each role is associated with a set of permissions.
 * Use JPA annotations for ORM mapping.
 * Note: RoleEnum defines the possible roles.
 *
 * @author ibosquet
 */
@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "roles")
public class RoleEntity {

    /**
     * Unique identifier for the role.
     * Auto-generated using the identity strategy.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Enum representing the role name.
     * Stored as a string in the database.
     */
    @Column(name = "role_name", unique = true)
    @Enumerated(EnumType.STRING)
    private RoleEnum roleEnum;

    /**
     * Set of permissions associated with the role.
     * Mapped using a many-to-many relationship with PermissionEntity.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name="role_permission",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id"))
    private Set<PermissionEntity> permissionList = new HashSet<>();
}