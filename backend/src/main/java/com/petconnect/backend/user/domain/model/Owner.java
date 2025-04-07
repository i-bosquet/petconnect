package com.petconnect.backend.user.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Represents a Pet Owner user in the system.
 * Inherits common user fields from UserEntity and adds owner-specific fields like phone number.
 * Uses JPA JOINED inheritance strategy.
 *
 * @author ibosquet
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "owner")
@PrimaryKeyJoinColumn(name = "user_id")
public class Owner extends UserEntity {

    /**
     * The primary contact phone number for the pet owner.
     * Cannot be blank. Maximum length of 20 characters.
     */
    @NotBlank(message = "Phone number cannot be blank")
    @Size(max = 20, message = "Phone number cannot exceed 20 characters")
    @Column(name = "phone", nullable = false, length = 20)
    private String phone;
}
