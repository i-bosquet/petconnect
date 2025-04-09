package com.petconnect.backend.pet.domain.model;

/**
 * Enumeration representing the lifecycle status of a pet within the PetConnect system.
 *
 * @author ibosquet
 */
public enum PetStatus {
    /** Initial state after owner registration, requires clinic verification. */
    PENDING,
    /** Verified and actively managed in the system. */
    ACTIVE,
    /** No longer actively managed. */
    INACTIVE
}
