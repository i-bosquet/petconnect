package com.petconnect.backend.notification.application.service.impl;

import com.petconnect.backend.certificate.application.event.CertificateGeneratedEvent;
import com.petconnect.backend.common.helper.EntityFinderHelper;
import com.petconnect.backend.pet.application.event.CertificateRequestedEvent;
import com.petconnect.backend.pet.application.event.PetActivatedEvent;
import com.petconnect.backend.pet.application.event.PetActivationRequestedEvent;
import com.petconnect.backend.pet.domain.model.Pet;
import com.petconnect.backend.user.domain.model.*;
import com.petconnect.backend.user.domain.repository.ClinicStaffRepository;
import com.petconnect.backend.user.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;


import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link NotificationServiceImpl}.
 * Verifies the logic for processing events and logging simulated notifications.
 * Mocks repositories to simulate finding recipients.
 *
 * @author ibosquet
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {
    @Mock private UserRepository userRepository;
    @Mock private ClinicStaffRepository clinicStaffRepository;
    @Mock private EntityFinderHelper entityFinderHelper;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private Owner testOwner;
    private Vet testVet;
    private ClinicStaff testStaff1, testStaff2;
    private Pet testPet;
    private final Long ownerId = 1L;
    private final Long vetId = 2L;
    private final Long staff1Id = 3L;
    private final Long clinicId = 5L;
    private final Long petId = 10L;
    private final Long certId = 20L;

    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void setUp() {
        Clinic testClinic = Clinic.builder().name("Notify Clinic").build();
        testClinic.setId(clinicId);

        testOwner = new Owner();
        testOwner.setId(ownerId);
        testOwner.setUsername("notify_owner");
        testOwner.setEmail("notify@owner.com");

        testVet = new Vet();
        testVet.setId(vetId);
        testVet.setName("Notify");
        testVet.setSurname("Vet");
        testVet.setEmail("notify@vet.com");
        testVet.setClinic(testClinic);

        testPet = new Pet();
        testPet.setId(petId);
        testPet.setName("PetForCertReq");

        testStaff1 = new Vet();
        testStaff1.setId(staff1Id);
        testStaff1.setName("Staff");
        testStaff1.setSurname("One");
        testStaff1.setEmail("staff1@clinic.com");
        testStaff1.setClinic(testClinic);

        Long staff2Id = 4L;
        testStaff2 = new ClinicStaff();
        testStaff2.setId(staff2Id);
        testStaff2.setName("Staff");
        testStaff2.setSurname("Two");
        testStaff2.setEmail("staff2@clinic.com");
        testStaff2.setClinic(testClinic);

        Logger logger = (Logger) LoggerFactory.getLogger(NotificationServiceImpl.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
        logger.setLevel(Level.INFO);
    }

    @Nested
    @DisplayName("processActivationRequest Tests")
    class ProcessActivationRequestTests {

        @Test
        @DisplayName("should log notifications for all staff in the target clinic")
        void shouldLogNotificationsForClinicStaff() {
            // Arrange
            PetActivationRequestedEvent event = new PetActivationRequestedEvent(petId, ownerId, clinicId, LocalDateTime.now());
            given(clinicStaffRepository.findByClinicId(clinicId)).willReturn(List.of(testStaff1, testStaff2));

            // Act
            notificationService.processActivationRequest(event);

            // Assert
            List<ILoggingEvent> logsList = listAppender.list;
            assertThat(logsList)
                    .anyMatch(log -> log.getFormattedMessage().contains("Processing PetActivationRequestedEvent for Pet ID: " + petId))
                    .anyMatch(log -> log.getFormattedMessage().contains("--> NOTIFICATION SIMULATION (to Staff: Staff One): Pet activation requested for Pet ID " + petId))
                    .anyMatch(log -> log.getFormattedMessage().contains("--> NOTIFICATION SIMULATION (to Staff: Staff Two): Pet activation requested for Pet ID " + petId));
            verify(clinicStaffRepository).findByClinicId(clinicId);
        }

        @Test
        @DisplayName("should log warning if no staff found for the clinic")
        void shouldLogWarningIfNoStaffFound() {
            // Arrange
            PetActivationRequestedEvent event = new PetActivationRequestedEvent(petId, ownerId, clinicId, LocalDateTime.now());
            given(clinicStaffRepository.findByClinicId(clinicId)).willReturn(Collections.emptyList());

            // Act
            notificationService.processActivationRequest(event);

            // Assert
            List<ILoggingEvent> logsList = listAppender.list;
            assertThat(logsList).anyMatch(log -> log.getFormattedMessage().contains("Processing PetActivationRequestedEvent"))
                .anyMatch(log -> log.getLevel() == Level.WARN && log.getFormattedMessage().contains("No staff found for target clinic"))
                .noneMatch(log -> log.getFormattedMessage().contains("--> NOTIFICATION SIMULATION"));
            verify(clinicStaffRepository).findByClinicId(clinicId);
        }
    }

    @Nested
    @DisplayName("processPetActivationConfirmation Tests")
    class ProcessPetActivationConfirmationTests {

        @Test
        @DisplayName("should log notification for the owner upon pet activation")
        void shouldLogNotificationForOwner() {
            // Arrange
            PetActivatedEvent event = new PetActivatedEvent(petId, ownerId, staff1Id, LocalDateTime.now());
            given(userRepository.findById(ownerId)).willReturn(Optional.of(testOwner));

            // Act
            notificationService.processPetActivationConfirmation(event);

            // Assert
            List<ILoggingEvent> logsList = listAppender.list;
            assertThat(logsList).anyMatch(log -> log.getFormattedMessage().contains("Processing PetActivatedEvent"))
                    .anyMatch(log -> log.getFormattedMessage().contains("--> NOTIFICATION SIMULATION (to Owner: " + testOwner.getEmail() + "): Your pet (ID: " + petId + ") has been successfully activated"));
            verify(userRepository).findById(ownerId);
        }

        @Test
        @DisplayName("should log error if owner not found")
        void shouldLogErrorIfOwnerNotFound() {
            // Arrange
            PetActivatedEvent event = new PetActivatedEvent(petId, 999L, staff1Id, LocalDateTime.now());
            given(userRepository.findById(999L)).willReturn(Optional.empty());

            // Act
            notificationService.processPetActivationConfirmation(event);

            // Assert
            List<ILoggingEvent> logsList = listAppender.list;
            assertThat(logsList).anyMatch(log -> log.getFormattedMessage().contains("Processing PetActivatedEvent"))
                    .anyMatch(log -> log.getLevel() == Level.ERROR && log.getFormattedMessage().contains("Could not find owner for notification"))
                    .noneMatch(log -> log.getFormattedMessage().contains("--> NOTIFICATION SIMULATION"));
            verify(userRepository).findById(999L);
        }
    }

    @Nested
    @DisplayName("processCertificateRequest Tests")
    class ProcessCertificateRequestTests {

        @Test
        @DisplayName("should log notification for the target vet")
        void shouldLogNotificationForVet() {
            CertificateRequestedEvent event = new CertificateRequestedEvent(petId, ownerId, vetId, clinicId, LocalDateTime.now());

            given(clinicStaffRepository.findByClinicIdAndIsActive(clinicId, true))
                    .willReturn(List.of(testStaff1));
            given(entityFinderHelper.findPetByIdOrFail(petId)).willReturn(testPet);
            given(userRepository.findById(ownerId)).willReturn(Optional.of(testOwner));
            given(userRepository.findById(vetId)).willReturn(Optional.of(testVet));

            notificationService.processCertificateRequest(event);

            List<ILoggingEvent> logsList = listAppender.list;

            assertThat(logsList.stream().map(ILoggingEvent::getFormattedMessage).toList())
                    .as("Checking for processing log")
                    .anyMatch(message -> message.contains("Processing CertificateRequestedEvent for Pet ID: " + petId + ", Target Clinic ID: " + clinicId));

            assertThat(logsList.stream().map(ILoggingEvent::getFormattedMessage).toList())
                    .as("Checking for notification simulation log")
                    .anyMatch(message -> message.contains("--> NOTIFICATION SIMULATION (to Vet: Staff One in Clinic Notify Clinic): Owner notify_owner has requested a certificate for pet PetForCertReq. Please review in clinic dashboard."));

            assertThat(logsList.stream().map(ILoggingEvent::getFormattedMessage).toList())
                    .as("Checking for specific target vet log")
                    .anyMatch(message -> message.contains("----> Specific request was targeted to Vet: Notify Vet"));


            verify(clinicStaffRepository).findByClinicIdAndIsActive(clinicId, true);
            verify(entityFinderHelper).findPetByIdOrFail(petId);
            verify(userRepository).findById(ownerId);
            verify(userRepository).findById(vetId);
        }

        @Test
        @DisplayName("should log error if specific target vet in event is not found, even if clinic has other vets")
        void shouldLogErrorIfSpecificTargetVetNotFoundButClinicHasVets() {
            Long nonExistentVetId = 998L;
            Long clinicIdForThisTest = 1L;
            CertificateRequestedEvent event = new CertificateRequestedEvent(petId, ownerId, nonExistentVetId, clinicIdForThisTest, LocalDateTime.now());

            given(clinicStaffRepository.findByClinicIdAndIsActive(clinicIdForThisTest, true))
                    .willReturn(List.of(testStaff1));

            given(userRepository.findById(nonExistentVetId)).willReturn(Optional.empty());
            given(entityFinderHelper.findPetByIdOrFail(petId)).willReturn(testPet);
            given(userRepository.findById(ownerId)).willReturn(Optional.of(testOwner));
            notificationService.processCertificateRequest(event);

            List<ILoggingEvent> logsList = listAppender.list;

            assertThat(logsList.stream().map(ILoggingEvent::getFormattedMessage).toList())
                    .as("Checking for processing log")
                    .anyMatch(message -> message.contains("Processing CertificateRequestedEvent for Pet ID: " + petId + ", Target Clinic ID: " + clinicIdForThisTest));

            assertThat(logsList.stream().map(ILoggingEvent::getFormattedMessage).toList())
                    .as("Checking for notification to clinic vet")
                    .anyMatch(message -> message.contains("--> NOTIFICATION SIMULATION (to Vet: " + testStaff1.getName() + " " + testStaff1.getSurname()));

            assertThat(logsList.stream().filter(log -> log.getLevel() == Level.ERROR).map(ILoggingEvent::getFormattedMessage).toList())
                    .as("Checking for ERROR: Could not find specific target vet for notification")
                    .anyMatch(message -> message.contains("Could not find vet for notification: Veterinarian not found with ID: " + nonExistentVetId));

            verify(clinicStaffRepository).findByClinicIdAndIsActive(clinicIdForThisTest, true);
            verify(userRepository).findById(nonExistentVetId);
        }
    }

    @Nested
    @DisplayName("processCertificateGenerationConfirmation Tests")
    class ProcessCertificateGenerationConfirmationTests {

        @Test
        @DisplayName("should log notification for the owner upon certificate generation")
        void shouldLogNotificationForOwner() {
            // Arrange
            CertificateGeneratedEvent event = new CertificateGeneratedEvent(certId, petId, ownerId, vetId, "CERT-123", LocalDateTime.now());
            given(userRepository.findById(ownerId)).willReturn(Optional.of(testOwner));

            // Act
            notificationService.processCertificateGenerationConfirmation(event);

            // Assert
            List<ILoggingEvent> logsList = listAppender.list;
            assertThat(logsList).anyMatch(log -> log.getFormattedMessage().contains("Processing CertificateGeneratedEvent"))
                    .anyMatch(log -> log.getFormattedMessage().contains("--> NOTIFICATION SIMULATION (to Owner: " + testOwner.getEmail() + "): Certificate (ID: " + certId + ", Number: CERT-123) has been generated"));
            verify(userRepository).findById(ownerId);
        }

        @Test
        @DisplayName("should log error if owner not found")
        void shouldLogErrorIfOwnerNotFound() {
            // Arrange
            CertificateGeneratedEvent event = new CertificateGeneratedEvent(certId, petId, 999L, vetId, "CERT-123", LocalDateTime.now());
            given(userRepository.findById(999L)).willReturn(Optional.empty());

            // Act
            notificationService.processCertificateGenerationConfirmation(event);

            // Assert
            List<ILoggingEvent> logsList = listAppender.list;
            assertThat(logsList).anyMatch(log -> log.getFormattedMessage().contains("Processing CertificateGeneratedEvent"))
                    .anyMatch(log -> log.getLevel() == Level.ERROR && log.getFormattedMessage().contains("Could not find owner for notification"))
                    .noneMatch(log -> log.getFormattedMessage().contains("--> NOTIFICATION SIMULATION"));
            verify(userRepository).findById(999L);
        }
    }
}
