package com.petconnect.backend.user.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Represents a token generated for verifying a password reset request.
 * Each token is associated with a specific user and has an expiration date.
 *
 * @author ibosquet
 */
@Getter
@Setter
@ToString(exclude = "user") // Exclude user to avoid potential recursion in logs
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The unique, randomly generated token string sent to the user.
     * Should be indexed for efficient lookup.
     */
    @NotBlank
    @Column(nullable = false, unique = true)
    private String token;

    /**
     * The user associated with this password reset token.
     * Fetched lazily as we usually find the token first, then get the user if needed.
     */
    @NotNull
    @ManyToOne(targetEntity = UserEntity.class, fetch = FetchType.LAZY, optional = false)
    @JoinColumn(nullable = false, name = "user_id", foreignKey = @ForeignKey(name = "fk_pwd_reset_token_user"))
    private UserEntity user;

    /**
     * The exact date and time when this token expires and is no longer valid.
     */
    @NotNull
    @Column(nullable = false, name = "expiry_date")
    private LocalDateTime expiryDate;

    /**
     * Calculates if the token has expired based on the current time.
     *
     * @return true if the token has expired, false otherwise.
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiryDate);
    }
}
