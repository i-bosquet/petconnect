package com.petconnect.backend.pet.application.event;

import java.time.LocalDateTime;

/**
 * Event published when a Pet has been successfully activated by clinic staff.
 *
 * @param petId             The ID of the pet that was activated.
 * @param ownerId           The ID of the pet's owner (to notify).
 * @param activatingStaffId The ID of the Vet or Admin who performed the activation.
 * @param activatedAt       The timestamp when the activation occurred.
 *
 * @author ibosquet
 */
public record PetActivatedEvent(
        Long petId,
        Long ownerId,
        Long activatingStaffId,
        LocalDateTime activatedAt
) {}