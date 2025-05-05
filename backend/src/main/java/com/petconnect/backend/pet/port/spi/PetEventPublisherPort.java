package com.petconnect.backend.pet.port.spi;

import com.petconnect.backend.pet.application.event.CertificateRequestedEvent;
import com.petconnect.backend.pet.application.event.PetActivationRequestedEvent;
import com.petconnect.backend.pet.application.event.PetActivatedEvent;

/**
 * Output Port interface defining the contract for publishing events
 * related to the Pet domain to an external messaging system (Kafka).
 * Implementations of this port (Adapters) will handle the actual message sending.
 *
 * @author ibosquet
 */
public interface PetEventPublisherPort {
    /**
     * Publishes an event indicating that an owner has requested pet activation at a clinic.
     *
     * @param event The PetActivationRequestedEvent containing details.
     */
    void publishPetActivationRequested(PetActivationRequestedEvent event);

    /**
     * Publishes an event indicating that a pet has been successfully activated by clinic staff.
     *
     * @param event The PetActivatedEvent containing details.
     */
    void publishPetActivated(PetActivatedEvent event);

    /**
     * Publishes an event indicating an Owner-requested certificate generation.
     *
     * @param event The CertificateRequestedEvent containing details.
     */
    void publishCertificateRequested(CertificateRequestedEvent event);
}
