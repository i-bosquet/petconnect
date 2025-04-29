package com.petconnect.backend.notification.adapter.kafka.publisher;

import com.petconnect.backend.certificate.application.event.CertificateGeneratedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link KafkaCertificateEventPublisherAdapter}.
 * Verifies that events are sent to the correct Kafka topics using KafkaTemplate.
 *
 * @author ibosquet
 */
@ExtendWith(MockitoExtension.class)
public class KafkaCertificateEventPublisherAdapterTest {
    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private KafkaCertificateEventPublisherAdapter kafkaCertificateEventPublisherAdapter;

    // Captors
    @Captor ArgumentCaptor<String> topicCaptor;
    @Captor ArgumentCaptor<String> keyCaptor;
    @Captor ArgumentCaptor<Object> eventCaptor;

    // Simulated Topic Name
    private final String certificateGeneratedTopic = "test-certificate-generated";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(kafkaCertificateEventPublisherAdapter, "certificateGeneratedTopic", certificateGeneratedTopic);

        // Mock KafkaTemplate response
        @SuppressWarnings("unchecked")
        CompletableFuture<SendResult<String, Object>> successfulFuture = CompletableFuture.completedFuture(mock(SendResult.class));
        given(kafkaTemplate.send(any(String.class), any(String.class), any(Object.class))).willReturn(successfulFuture);
    }

    @Nested
    @DisplayName("publishCertificateGenerated Tests")
    class PublishCertificateGeneratedTests {

        @Test
        @DisplayName("should send CertificateGeneratedEvent to correct topic with correct key and event")
        void shouldSendCertificateGeneratedEvent() {
            // Arrange
            CertificateGeneratedEvent event = new CertificateGeneratedEvent(
                    501L, 101L, 51L, 11L, "CERT-TEST-123", LocalDateTime.now()
            );

            // Act
            kafkaCertificateEventPublisherAdapter.publishCertificateGenerated(event);

            // Assert
            then(kafkaTemplate).should().send(topicCaptor.capture(), keyCaptor.capture(), eventCaptor.capture());

            assertThat(topicCaptor.getValue()).isEqualTo(certificateGeneratedTopic);
            assertThat(keyCaptor.getValue()).isEqualTo(event.certificateId().toString()); // Key is certificateId
            assertThat(eventCaptor.getValue()).isInstanceOf(CertificateGeneratedEvent.class);
            assertThat(eventCaptor.getValue()).isEqualTo(event);
        }
    }
}
