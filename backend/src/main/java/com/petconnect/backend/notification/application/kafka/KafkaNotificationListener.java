package com.petconnect.backend.notification.application.kafka;

import com.petconnect.backend.certificate.application.event.CertificateGeneratedEvent;
import com.petconnect.backend.notification.application.service.NotificationService;
import com.petconnect.backend.pet.application.event.CertificateRequestedEvent;
import com.petconnect.backend.pet.application.event.PetActivatedEvent;
import com.petconnect.backend.pet.application.event.PetActivationRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Kafka Consumer responsible for listening to domain events published by other modules
 * and triggering the appropriate notification logic via NotificationService.
 *
 * @author ibosquet
 */
@Profile("dev")
@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaNotificationListener {

    private final NotificationService notificationService;

    /**
     * Listens to the 'pet-activation-requests' topic.
     * Delegates processing to NotificationService.
     *
     * @param event The deserialized PetActivationRequestedEvent message.
     */
    @KafkaListener(topics = "${kafka.topic.pet-activation-requests}", groupId = "${spring.kafka.consumer.group-id}")
    public void handlePetActivationRequest(@Payload PetActivationRequestedEvent event) {
        log.info("Received PetActivationRequestedEvent: {}", event);
        try {
            notificationService.processActivationRequest(event);
        } catch (Exception e) {
            log.error("Error processing PetActivationRequestedEvent for petId {}: {}", event.petId(), e.getMessage(), e);
        }
    }

    /**
     * Listens to the 'pet-activated' topic.
     * Delegates processing to NotificationService.
     *
     * @param event The deserialized PetActivatedEvent message.
     */
    @KafkaListener(topics = "${kafka.topic.pet-activated}", groupId = "${spring.kafka.consumer.group-id}")
    public void handlePetActivated(@Payload PetActivatedEvent event) {
        log.info("Received PetActivatedEvent: {}", event);
        try {
            notificationService.processPetActivationConfirmation(event);
        } catch (Exception e) {
            log.error("Error processing PetActivatedEvent for petId {}: {}", event.petId(), e.getMessage(), e);
        }
    }

    /**
     * Listens to the 'certificate-requests' topic.
     * Delegates processing to NotificationService.
     *
     * @param event The deserialized CertificateRequestedEvent message.
     */
    @KafkaListener(topics = "${kafka.topic.certificate-requests}", groupId = "${spring.kafka.consumer.group-id}")
    public void handleCertificateRequest(@Payload CertificateRequestedEvent event) {
        log.info("Received CertificateRequestedEvent: {}", event);
        try {
            notificationService.processCertificateRequest(event);
        } catch (Exception e) {
            log.error("Error processing CertificateRequestedEvent for petId {}: {}", event.petId(), e.getMessage(), e);
        }
    }

    /**
     * Listens to the 'certificate-generated' topic.
     * Delegates processing to NotificationService.
     *
     * @param event The deserialized CertificateGeneratedEvent message.
     */
    @KafkaListener(topics = "${kafka.topic.certificate-generated}", groupId = "${spring.kafka.consumer.group-id}")
    public void handleCertificateGenerated(@Payload CertificateGeneratedEvent event) {
        log.info("Received CertificateGeneratedEvent: {}", event);
        try {
            notificationService.processCertificateGenerationConfirmation(event);
        } catch (Exception e) {
            log.error("Error processing CertificateGeneratedEvent for certId {}: {}", event.certificateId(), e.getMessage(), e);
        }
    }
}
