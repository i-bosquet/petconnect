package com.petconnect.backend.certificate.application.event;

import java.time.LocalDateTime;

/**
 * Event published when a new digital Certificate has been successfully generated for a Pet.
 *
 * @param certificateId   The ID of the newly generated certificate.
 * @param petId           The ID of the pet for which the certificate was generated.
 * @param ownerId         The ID of the pet's owner (to notify).
 * @param generatingVetId The ID of the Vet who generated the certificate.
 * @param certificateNumber The official certificate number assigned.
 * @param generatedAt     The timestamp when the certificate was generated.
 *
 * @author ibosquet
 */
public record CertificateGeneratedEvent(
        Long certificateId,
        Long petId,
        Long ownerId,
        Long generatingVetId,
        String certificateNumber,
        LocalDateTime generatedAt
) {}
