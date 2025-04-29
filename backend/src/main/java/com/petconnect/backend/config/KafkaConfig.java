package com.petconnect.backend.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Configuration class for Apache Kafka integration.
 * Defines beans for topic creation and potentially other Kafka settings.
 * Uses values injected from application properties for topic names.
 *
 * @author ibosquet
 */
@Configuration
public class KafkaConfig {
    // Inject topic names from application.properties
    @Value("${kafka.topic.pet-activation-requests}")
    private String petActivationRequestsTopic;

    @Value("${kafka.topic.pet-activated}")
    private String petActivatedTopic;

    @Value("${kafka.topic.certificate-requests}")
    private String certificateRequestsTopic;

    @Value("${kafka.topic.certificate-generated}")
    private String certificateGeneratedTopic;

    /**
     * Defines the NewTopic bean for the pet activation request topic.
     * Spring Kafka's KafkaAdmin bean will automatically use this definition
     * to create the topic on the broker if it doesn't exist.
     * Configured with 1 partition and replication factor 1 (suitable for dev).
     *
     * @return NewTopic bean definition.
     */
    @Bean
    public NewTopic petActivationRequestsTopic() {
        return TopicBuilder.name(petActivationRequestsTopic)
                .partitions(1)
                .replicas(1)
                .build();
    }

    /**
     * Defines the NewTopic bean for the pet activated event topic.
     *
     * @return NewTopic bean definition.
     */
    @Bean
    public NewTopic petActivatedTopic() {
        return TopicBuilder.name(petActivatedTopic)
                .partitions(1)
                .replicas(1)
                .build();
    }

    /**
     * Defines the NewTopic bean for the certificate request topic.
     *
     * @return NewTopic bean definition.
     */
    @Bean
    public NewTopic certificateRequestsTopic() {
        return TopicBuilder.name(certificateRequestsTopic)
                .partitions(1)
                .replicas(1)
                .build();
    }

    /**
     * Defines the NewTopic bean for the certificate-generated event topic.
     *
     * @return NewTopic bean definition.
     */
    @Bean
    public NewTopic certificateGeneratedTopic() {
        return TopicBuilder.name(certificateGeneratedTopic)
                .partitions(1)
                .replicas(1)
                .build();
    }
}
