package com.petconnect.backend.notification.application.service;

import com.petconnect.backend.certificate.application.event.CertificateGeneratedEvent;
import com.petconnect.backend.pet.application.event.CertificateRequestedEvent;
import com.petconnect.backend.pet.application.event.PetActivatedEvent;
import com.petconnect.backend.pet.application.event.PetActivationRequestedEvent;

/**
 * Service responsible for handling the logic of processing consumed domain events
 * and triggering the appropriate notifications (e.g., logging, sending emails).
 *
 * @author ibosquet
 */
public interface NotificationService {

    /**
     * Processes a request for pet activation, determining recipients
     * (e.g., staff of the target clinic) and sending notifications.
     * @param event The consumed event details.
     */
    void processActivationRequest(PetActivationRequestedEvent event);

    /**
     * Processes the confirmation of a pet activation, determining recipients
     * (e.g., the pet owner) and sending notifications.
     * @param event The consumed event details.
     */
    void processPetActivationConfirmation(PetActivatedEvent event);

    /**
     * Processes a request for certificate generation, determining recipients
     * (e.g., the target veterinarian) and sending notifications.
     * @param event The consumed event details.
     */
    void processCertificateRequest(CertificateRequestedEvent event);

    /**
     * Processes the confirmation of certificate generation, determining recipients
     * (e.g., the pet owner) and sending notifications.
     * @param event The consumed event details.
     */
    void processCertificateGenerationConfirmation(CertificateGeneratedEvent event);
}
