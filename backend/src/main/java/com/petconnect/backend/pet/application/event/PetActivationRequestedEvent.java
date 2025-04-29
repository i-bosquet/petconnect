package com.petconnect.backend.pet.application.event;

import java.time.LocalDateTime;

/**
 * Event published when an Owner requests activation for their Pet at a specific Clinic.
 *
 * @param petId          The ID of the pet requiring activation.
 * @param ownerId        The ID of the Owner who made the request.
 * @param targetClinicId The ID of the Clinic where activation was requested.
 * @param requestedAt    The timestamp when the request was made.
 *
 * @author ibosquet
 */
public record PetActivationRequestedEvent(
        Long petId,
        Long ownerId,
        Long targetClinicId,
        LocalDateTime requestedAt
) {}