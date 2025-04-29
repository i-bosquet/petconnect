package com.petconnect.backend.pet.application.event;

import java.time.LocalDateTime;

/**
 * Event published when an Owner requests certificate generation for their Pet from a specific Vet.
 *
 * @param petId          The ID of the pet needing the certificate.
 * @param ownerId        The ID of the Owner making the request.
 * @param targetVetId    The ID of the Veterinarian requested to generate the certificate.
 * @param requestedAt    The timestamp of the request.
 *
 * @author ibosquet
 */
public record CertificateRequestedEvent(
        Long petId,
        Long ownerId,
        Long targetVetId,
        LocalDateTime requestedAt
) {
}
