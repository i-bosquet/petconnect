package com.petconnect.backend.pet.application.event;

import jakarta.validation.constraints.NotNull;
import org.springframework.lang.Nullable;

import java.time.LocalDateTime;

/**
 * Event published when an Owner requests certificate generation for their Pet,
 * targeting a specific Clinic, and optionally a preferred Vet within that clinic.
 *
 * @param petId          The ID of the pet.
 * @param ownerId        The ID of the Owner who made the request.
 * @param targetVetId    (Nullable) The ID of a specific veterinarian targeted by the request, if any.
 * @param targetClinicId The ID of the Clinic to which the request is directed.
 * @param requestedAt    The timestamp when the request was made.
 *
 * @author ibosquet
 */
public record CertificateRequestedEvent(
        @NotNull Long petId,
        @NotNull Long ownerId,
        @Nullable Long targetVetId,
        @NotNull Long targetClinicId,
        @NotNull LocalDateTime requestedAt
) {
}
