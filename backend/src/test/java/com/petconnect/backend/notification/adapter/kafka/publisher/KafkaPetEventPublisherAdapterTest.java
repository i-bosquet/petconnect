package com.petconnect.backend.notification.adapter.kafka.publisher;

import com.petconnect.backend.pet.application.event.CertificateRequestedEvent;
import com.petconnect.backend.pet.application.event.PetActivatedEvent;
import com.petconnect.backend.pet.application.event.PetActivationRequestedEvent;
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
 * Unit tests for {@link KafkaPetEventPublisherAdapter}.
 * Verifies that events are sent to the correct Kafka topics using KafkaTemplate.
 *
 * @author ibosquet
 */
@ExtendWith(MockitoExtension.class)
class KafkaPetEventPublisherAdapterTest {
    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private KafkaPetEventPublisherAdapter kafkaPetEventPublisherAdapter;

    @Captor ArgumentCaptor<String> topicCaptor;
    @Captor ArgumentCaptor<String> keyCaptor;
    @Captor ArgumentCaptor<Object> eventCaptor;

    private final String activationRequestTopic = "test-pet-activation-requests";
    private final String activatedTopic = "test-pet-activated";
    private final String certificateRequestTopic = "test-certificate-requests";

    @BeforeEach
    void setUp() {

        ReflectionTestUtils.setField(kafkaPetEventPublisherAdapter, "activationRequestTopic", activationRequestTopic);
        ReflectionTestUtils.setField(kafkaPetEventPublisherAdapter, "activatedTopic", activatedTopic);
        ReflectionTestUtils.setField(kafkaPetEventPublisherAdapter, "certificateRequestTopic", certificateRequestTopic);

        @SuppressWarnings("unchecked")
        CompletableFuture<SendResult<String, Object>> successfulFuture = CompletableFuture.completedFuture(mock(SendResult.class));
        given(kafkaTemplate.send(any(String.class), any(String.class), any(Object.class))).willReturn(successfulFuture);
    }

    @Nested
    @DisplayName("publishPetActivationRequested Tests")
    class PublishPetActivationRequestedTests {

        @Test
        @DisplayName("should send PetActivationRequestedEvent to correct topic with correct key and event")
        void shouldSendPetActivationRequestedEvent() {
            // Arrange
            PetActivationRequestedEvent event = new PetActivationRequestedEvent(1L, 10L, 5L, LocalDateTime.now());

            // Act
            kafkaPetEventPublisherAdapter.publishPetActivationRequested(event);

            // Assert
            then(kafkaTemplate).should().send(topicCaptor.capture(), keyCaptor.capture(), eventCaptor.capture());

            assertThat(topicCaptor.getValue()).isEqualTo(activationRequestTopic);
            assertThat(keyCaptor.getValue()).isEqualTo(event.petId().toString());
            assertThat(eventCaptor.getValue()).isInstanceOf(PetActivationRequestedEvent.class);
            assertThat(eventCaptor.getValue()).isEqualTo(event);
        }
    }

    @Nested
    @DisplayName("publishPetActivated Tests")
    class PublishPetActivatedTests {

        @Test
        @DisplayName("should send PetActivatedEvent to correct topic with correct key and event")
        void shouldSendPetActivatedEvent() {
            // Arrange
            PetActivatedEvent event = new PetActivatedEvent(2L, 11L, 21L, LocalDateTime.now());

            // Act
            kafkaPetEventPublisherAdapter.publishPetActivated(event);

            // Assert
            then(kafkaTemplate).should().send(topicCaptor.capture(), keyCaptor.capture(), eventCaptor.capture());

            assertThat(topicCaptor.getValue()).isEqualTo(activatedTopic);
            assertThat(keyCaptor.getValue()).isEqualTo(event.petId().toString());
            assertThat(eventCaptor.getValue()).isInstanceOf(PetActivatedEvent.class);
            assertThat(eventCaptor.getValue()).isEqualTo(event);
        }
    }

//    @Nested
//    @DisplayName("publishCertificateRequested Tests")
//    class PublishCertificateRequestedTests {
//
//        @Test
//        @DisplayName("should send CertificateRequestedEvent to correct topic with correct key and event")
//        void shouldSendCertificateRequestedEvent() {
//            // Arrange
//            CertificateRequestedEvent event = new CertificateRequestedEvent(3L, 12L, 22L, LocalDateTime.now());
//
//            // Act
//            kafkaPetEventPublisherAdapter.publishCertificateRequested(event);
//
//            // Assert
//            then(kafkaTemplate).should().send(topicCaptor.capture(), keyCaptor.capture(), eventCaptor.capture());
//
//            assertThat(topicCaptor.getValue()).isEqualTo(certificateRequestTopic);
//            assertThat(keyCaptor.getValue()).isEqualTo(event.petId().toString());
//            assertThat(eventCaptor.getValue()).isInstanceOf(CertificateRequestedEvent.class);
//            assertThat(eventCaptor.getValue()).isEqualTo(event);
//        }
//    }
}
