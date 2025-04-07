package com.petconnect.backend.user.domain.model;

import jakarta.persistence.*; // Jakarta Persistence
import jakarta.validation.constraints.*; // Validation constraints
import lombok.*; // Lombok

import java.util.HashSet;
import java.util.Set;

/**
 * Represents the base user entity in the system.
 * Contains common fields for all user types (Owners, Clinic Staff).
 * Uses JPA JOINED inheritance strategy.
 * Inherits auditing fields from BaseEntity.
 *
 * @author ibosquet
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.JOINED)
public class UserEntity extends BaseEntity{

    /**
     * The unique username for the user within the system.
     * Used for identification and potentially login. Cannot be blank.
     * Must be unique across all users. Maximum length of 50 characters.
     */
    @NotBlank(message = "Username cannot be blank")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    /**
     * The unique email address for the user.
     * Used for login, communication, and password recovery. Cannot be blank.
     * Must be a valid email format and unique across all users.
     */
    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Email should be valid") // Checks email format
    @Column(name = "email", nullable = false, unique = true)
    private String email;

    /**
     * The hashed password for the user. Never store plain text passwords.
     * Cannot be blank (after hashing). Handled by Spring Security during user creation/update.
     * Validation might be applied on the DTO level before hashing.
     */
    @NotBlank(message = "Password cannot be blank") // Ensures the hashed password is stored
    @Column(name = "password", nullable = false)
    private String password; // This will store the HASHED password

    /**
     * URL pointing to the user's avatar image.
     * Can be null if the user hasn't set one or uses a default.
     */
    @Column(name = "avatar") // Nullable by default
    private String avatar; // URL to the image

    /**
     * Indicates whether the user account is enabled.
     */
    @Column(name = "is_Enabled")
    private boolean isEnabled;

    /**
     * Indicates whether the user account is non-expired.
     */
    @Column(name = "account_Non_Expired")
    private boolean accountNonExpired;

    /**
     * Indicates whether the user account is non-locked.
     */
    @Column(name = "account_Non_Locked")
    private boolean accountNonLocked;

    /**
     * Indicates whether the user credentials (password) are non-expired.
     */
    @Column(name = "credentials_Non_Expired")
    private boolean credentialsNonExpired;

    /**
     * The set of roles associated with the user.
     * Mapped using a many-to-many relationship with RoleEntity.
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name="user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id", referencedColumnName = "id"))
    private Set<RoleEntity> roles = new HashSet<>();
}
