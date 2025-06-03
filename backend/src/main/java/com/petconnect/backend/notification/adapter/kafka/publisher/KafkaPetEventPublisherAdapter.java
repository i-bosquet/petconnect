package com.petconnect.backend.notification.adapter.kafka.publisher;

import com.petconnect.backend.pet.application.event.CertificateRequestedEvent;
import com.petconnect.backend.pet.application.event.PetActivationRequestedEvent;
import com.petconnect.backend.pet.application.event.PetActivatedEvent;
import com.petconnect.backend.pet.port.spi.PetEventPublisherPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Kafka Adapter implementation for the PetEventPublisherPort interface.
 * Sends Pet-related domain events to the configured Kafka topics.
 *
 * @author ibosquet
 */
@Profile("dev")
@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaPetEventPublisherAdapter implements PetEventPublisherPort {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topic.pet-activation-requests}")
    private String activationRequestTopic;

    @Value("${kafka.topic.pet-activated}")
    private String activatedTopic;

    @Value("${kafka.topic.certificate-requests}")
    private String certificateRequestTopic;

    /**
     * {@inheritDoc}
     * Sends the PetActivationRequestedEvent to the corresponding Kafka topic.
     * Uses petId as the Kafka message key for potential partitioning.
     */
    @Override
    public void publishPetActivationRequested(PetActivationRequestedEvent event) {
        log.info("Publishing PetActivationRequestedEvent to topic '{}': {}", activationRequestTopic, event);
        try {
            CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(
                    activationRequestTopic,
                    event.petId().toString(),
                    event
            );

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.debug("Successfully sent PetActivationRequestedEvent for petId {} to partition {} with offset {}",
                            event.petId(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                } else {
                    log.error("Failed to send PetActivationRequestedEvent for petId {}: {}",
                            event.petId(), ex.getMessage(), ex);
                }
            });
        } catch (Exception e) {
            log.error("Exception caught while trying to send PetActivationRequestedEvent for petId {}: {}",
                    event.petId(), e.getMessage(), e);
        }
    }

    /**
     * {@inheritDoc}
     * Sends the PetActivatedEvent to the corresponding Kafka topic.
     * Uses petId as the Kafka message key.
     */
    @Override
    public void publishPetActivated(PetActivatedEvent event) {
        log.info("Publishing PetActivatedEvent to topic '{}': {}", activatedTopic, event);
        try {
            CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(
                    activatedTopic,
                    event.petId().toString(),
                    event
            );

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.debug("Successfully sent PetActivatedEvent for petId {} to partition {} with offset {}",
                            event.petId(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                } else {
                    log.error("Failed to send PetActivatedEvent for petId {}: {}",
                            event.petId(), ex.getMessage(), ex);
                }
            });
        } catch (Exception e) {
            log.error("Exception caught while trying to send PetActivatedEvent for petId {}: {}",
                    event.petId(), e.getMessage(), e);
        }
    }

    @Override
    public void publishCertificateRequested(CertificateRequestedEvent event) {
        log.info("Publishing CertificateRequestedEvent to topic '{}': {}", certificateRequestTopic, event);
        try {
            CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(
                    certificateRequestTopic,
                    event.petId().toString(),
                    event
            );
            future.whenComplete(
                    (result, ex) -> {
                        if (ex == null) {
                            log.debug("Successfully sent CertificateRequestedEvent for petId {} to partition {} with offset {}",
                                    event.petId(),
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset());
                        } else {
                            log.error("Failed to send CertificateRequestedEvent for petId {}: {}",
                                    event.petId(), ex.getMessage(), ex);
                        }
                    }
            );
        } catch (Exception e) {
            log.error("Exception caught while trying to send CertificateRequestedEvent for petId {}: {}",
                    event.petId(), e.getMessage(), e);
        }
    }
}
