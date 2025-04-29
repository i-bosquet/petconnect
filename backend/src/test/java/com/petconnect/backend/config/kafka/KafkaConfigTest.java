package com.petconnect.backend.config.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests to verify that Kafka topic beans are correctly defined and
 * properties are loaded in the Spring application context.
 * This does NOT require a running Kafka broker.
 */
@SpringBootTest
class KafkaConfigTest {
    @Autowired
    private ApplicationContext context;

    @Value("${kafka.topic.pet-activation-requests}") private String petActivationRequestsTopicName;
    @Value("${kafka.topic.pet-activated}") private String petActivatedTopicName;
    @Value("${kafka.topic.certificate-requests}") private String certificateRequestsTopicName;
    @Value("${kafka.topic.certificate-generated}") private String certificateGeneratedTopicName;

    @Test
    @DisplayName("Should load Kafka topic names from TEST properties")
    void shouldLoadTopicNameProperties() {
        assertThat(petActivationRequestsTopicName).isEqualTo("test-pet-activation-requests");
        assertThat(petActivatedTopicName).isEqualTo("test-pet-activated");
        assertThat(certificateRequestsTopicName).isEqualTo("test-certificate-requests");
        assertThat(certificateGeneratedTopicName).isEqualTo("test-certificate-generated");
    }

    @Test
    @DisplayName("Should define NewTopic beans using TEST topic names")
    void shouldDefineNewTopicBeans() {
        NewTopic topic1 = context.getBean("petActivationRequestsTopic", NewTopic.class);
        NewTopic topic2 = context.getBean("petActivatedTopic", NewTopic.class);
        NewTopic topic3 = context.getBean("certificateRequestsTopic", NewTopic.class);
        NewTopic topic4 = context.getBean("certificateGeneratedTopic", NewTopic.class);

        assertThat(topic1).isNotNull();
        assertThat(topic1.name()).isEqualTo(petActivationRequestsTopicName);
        assertThat(topic1.name()).isEqualTo("test-pet-activation-requests");
        assertThat(topic1.numPartitions()).isEqualTo(1);

        assertThat(topic2).isNotNull();
        assertThat(topic2.name()).isEqualTo(petActivatedTopicName);
        assertThat(topic2.name()).isEqualTo("test-pet-activated");

        assertThat(topic3).isNotNull();
        assertThat(topic3.name()).isEqualTo(certificateRequestsTopicName);
        assertThat(topic3.name()).isEqualTo("test-certificate-requests");

        assertThat(topic4).isNotNull();
        assertThat(topic4.name()).isEqualTo(certificateGeneratedTopicName);
        assertThat(topic4.name()).isEqualTo("test-certificate-generated");
    }

    @Test
    @DisplayName("Should automatically create KafkaTemplate bean")
    void shouldAutoCreateKafkaTemplate() {
        org.springframework.kafka.core.KafkaTemplate<?, ?> kafkaTemplate =
                context.getBean(org.springframework.kafka.core.KafkaTemplate.class);
        assertThat(kafkaTemplate).isNotNull();
    }

    @Test
    @DisplayName("Should automatically create KafkaAdmin bean")
    void shouldAutoCreateKafkaAdmin() {
        org.springframework.kafka.core.KafkaAdmin kafkaAdmin =
                context.getBean(org.springframework.kafka.core.KafkaAdmin.class);
        assertThat(kafkaAdmin).isNotNull();
    }
}
