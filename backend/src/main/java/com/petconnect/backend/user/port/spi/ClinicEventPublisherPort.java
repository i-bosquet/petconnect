package com.petconnect.backend.user.port.spi;

import com.petconnect.backend.user.application.event.ClinicKeysChangedEvent;

/**
 * Defines a port for publishing user-related events that notify downstream systems
 * about changes or updates pertinent to user data or settings.
 *
 * @author ibosquet
 */
public interface ClinicEventPublisherPort {
    /**
     * Publishes an event to notify that cryptographic key information associated
     * with a clinic and its staff has been changed.
     *
     * @param event The event containing details about changes
     *                               to the cryptographic keys, including the clinic
     *                               and staff identifiers, the date and time of the
     *                               change, and details about the updated keys.
     */
    void publishClinicKeysChangedEvent(ClinicKeysChangedEvent event);
}
