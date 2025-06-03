package com.petconnect.backend.notification.adapter.kafka.publisher;

import com.petconnect.backend.user.application.event.ClinicKeysChangedEvent;
import com.petconnect.backend.user.port.spi.ClinicEventPublisherPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import java.util.concurrent.CompletableFuture;

/**
 * Kafka Adapter implementation for the ClinicEventPublisherPort interface.
 * This class is responsible for sending Clinic-related domain events, such as
 * ClinicKeysChangedEvent, to the appropriate Kafka topic.
 *
 * @author ibosquet
 */
@Profile("dev")
@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaClinicEventPublisherAdapter implements ClinicEventPublisherPort{
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topic.clinic-keys-changed}")
    private String clinicKeysChangedTopic;

    @Override
    public void publishClinicKeysChangedEvent(ClinicKeysChangedEvent event) {
        log.info("Publishing ClinicKeysChangedEvent to topic '{}': {}", clinicKeysChangedTopic, event);
        try {
            CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(
                    clinicKeysChangedTopic,
                    event.clinicId().toString(),
                    event
            );
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.debug("Successfully sent ClinicKeysChangedEvent for clinicId {}", event.clinicId());
                } else {
                    log.error("Failed to send ClinicKeysChangedEvent for clinicId {}: {}", event.clinicId(), ex.getMessage(), ex);
                }
            });
        } catch (Exception e) {
            log.error("Exception caught while trying to send ClinicKeysChangedEvent for clinicId {}: {}", event.clinicId(), e.getMessage(), e);
        }
    }
}
