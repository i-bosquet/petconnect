package com.petconnect.backend.certificate.port.spi;

import com.petconnect.backend.certificate.application.event.CertificateGeneratedEvent;

/**
 * Output Port interface defining the contract for publishing events
 * related to the Certificate domain to an external messaging system (Kafka).
 * Implementations of this port (Adapters) will handle the actual message sending.
 *
 * @author ibosquet
 */
public interface CertificateEventPublisherPort {

    /**
     * Publishes an event indicating that a new digital certificate has been generated.
     *
     * @param event The CertificateGeneratedEvent containing details.
     */
    void publishCertificateGenerated(CertificateGeneratedEvent event);
}
