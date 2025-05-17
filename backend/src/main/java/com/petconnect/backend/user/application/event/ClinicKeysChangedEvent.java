package com.petconnect.backend.user.application.event;


import java.time.LocalDateTime;

/**
 * Represents an event that captures changes in the cryptographic keys
 * associated with a specific clinic and its staff.
 * The event provides details about the clinic, the related staff member,
 * the date and time of the change, and specifies which key was changed.
 * @param clinicId              Identifier for the clinic associated with this event.
 * @param adminId               Identifier for the clinic staff associated with this event.
 * @param changedAt             The date and time when the key change occurred.
 * @param publicKeyChanged      Details or identifier for the changed public key.
 * @param privateKeyPathChanged Path or identifier for the changed private key.
 *
 * @author ibosquet
 */
public record  ClinicKeysChangedEvent(
        Long clinicId,
        Long adminId,
        LocalDateTime changedAt,
        boolean publicKeyChanged,
        boolean privateKeyPathChanged
)
{
}
