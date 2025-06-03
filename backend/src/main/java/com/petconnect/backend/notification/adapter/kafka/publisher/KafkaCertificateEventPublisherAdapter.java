package com.petconnect.backend.notification.adapter.kafka.publisher;

import com.petconnect.backend.certificate.application.event.CertificateGeneratedEvent;
import com.petconnect.backend.certificate.port.spi.CertificateEventPublisherPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Kafka Adapter implementation for the CertificateEventPublisherPort interface.
 * Sends Certificate-related domain events to the configured Kafka topics.
 *
 * @author ibosquet
 */
@Profile("dev")
@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaCertificateEventPublisherAdapter implements CertificateEventPublisherPort {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topic.certificate-generated}")
    private String certificateGeneratedTopic;

    /**
     * {@inheritDoc}
     * Sends the CertificateGeneratedEvent to the corresponding Kafka topic.
     * Uses certificateId as the Kafka message key.
     */
    @Override
    public void publishCertificateGenerated(CertificateGeneratedEvent event) {
        log.info("Publishing CertificateGeneratedEvent to topic '{}': {}", certificateGeneratedTopic, event);
        try {
            CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(
                    certificateGeneratedTopic,
                    event.certificateId().toString(),
                    event
            );

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.debug("Successfully sent CertificateGeneratedEvent for certId {} to partition {} with offset {}",
                            event.certificateId(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                } else {
                    log.error("Failed to send CertificateGeneratedEvent for certId {}: {}",
                            event.certificateId(), ex.getMessage(), ex);
                }
            });
        } catch (Exception e) {
            log.error("Exception caught while trying to send CertificateGeneratedEvent for certId {}: {}",
                    event.certificateId(), e.getMessage(), e);
        }
    }
}
