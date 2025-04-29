package com.petconnect.backend.notification.application.service.impl;

import com.petconnect.backend.certificate.application.event.CertificateGeneratedEvent;
import com.petconnect.backend.exception.EntityNotFoundException;
import com.petconnect.backend.notification.application.service.NotificationService;
import com.petconnect.backend.pet.application.event.CertificateRequestedEvent;
import com.petconnect.backend.pet.application.event.PetActivatedEvent;
import com.petconnect.backend.pet.application.event.PetActivationRequestedEvent;
import com.petconnect.backend.user.domain.model.ClinicStaff;
import com.petconnect.backend.user.domain.model.Owner;
import com.petconnect.backend.user.domain.model.Vet;
import com.petconnect.backend.user.domain.repository.ClinicStaffRepository;
import com.petconnect.backend.user.domain.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementation of the NotificationService.
 * Processes domain events consumed from Kafka and simulates sending notifications
 * by logging detailed messages. In a real application, this service would
 * integrate with email services, push notification providers, etc.
 *
 * @author ibosquet
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final UserRepository userRepository;
    private final ClinicStaffRepository clinicStaffRepository;

    /**
     * {@inheritDoc}
     * Logs a notification simulation intended for clinic staff about a pet activation request.
     */
    @Override
    @Transactional(readOnly = true)
    public void processActivationRequest(PetActivationRequestedEvent event) {
        log.info("Processing PetActivationRequestedEvent for Pet ID: {}, Clinic ID: {}", event.petId(), event.targetClinicId());

        List<ClinicStaff> staffList = clinicStaffRepository.findByClinicId(event.targetClinicId());
        if (staffList.isEmpty()) {
            log.warn("No staff found for target clinic {} to notify about activation request for pet {}.", event.targetClinicId(), event.petId());
            return;
        }

        staffList.forEach(staff -> log.info("--> NOTIFICATION SIMULATION (to Staff: {} {}): Pet activation requested for Pet ID {} at your clinic (ID: {}) by Owner ID {}.",
                staff.getName(), staff.getSurname(), event.petId(), event.targetClinicId(), event.ownerId()));
    }

    /**
     * {@inheritDoc}
     * Logs a notification simulation intended for the pet owner about successful activation.
     */
    @Override
    @Transactional(readOnly = true)
    public void processPetActivationConfirmation(PetActivatedEvent event) {
        log.info("Processing PetActivatedEvent for Pet ID: {}", event.petId());

        Owner owner = findOwner(event.ownerId());
        if (owner == null) return;

        log.info("--> NOTIFICATION SIMULATION (to Owner: {}): Your pet (ID: {}) has been successfully activated by staff member ID {}.",
                owner.getEmail(),
                event.petId(),
                event.activatingStaffId());
    }

    /**
     * {@inheritDoc}
     * Logs a notification simulation intended for the target Veterinarian about a certificate request.
     */
    @Override
    @Transactional(readOnly = true)
    public void processCertificateRequest(CertificateRequestedEvent event) {
        log.info("Processing CertificateRequestedEvent for Pet ID: {}, Target Vet ID: {}", event.petId(), event.targetVetId());

        Vet targetVet = findVet(event.targetVetId());
        if (targetVet == null) return;

        log.info("--> NOTIFICATION SIMULATION (to Vet: {} {}): Owner ID {} requested a certificate generation for Pet ID {}.",
                targetVet.getName(), targetVet.getSurname(), // Usamos nombre/apellido
                event.ownerId(),
                event.petId());
    }

    /**
     * {@inheritDoc}
     * Logs a notification simulation intended for the pet owner about successful certificate generation.
     */
    @Override
    @Transactional(readOnly = true)
    public void processCertificateGenerationConfirmation(CertificateGeneratedEvent event) {
        log.info("Processing CertificateGeneratedEvent for Certificate ID: {}", event.certificateId());

        Owner owner = findOwner(event.ownerId());
        if (owner == null) return;

        log.info("--> NOTIFICATION SIMULATION (to Owner: {}): Certificate (ID: {}, Number: {}) has been generated for your pet (ID: {}) by Vet ID {}.",
                owner.getEmail(),
                event.certificateId(),
                event.certificateNumber(),
                event.petId(),
                event.generatingVetId());
    }

    // --- Helper Methods ---

    /**
     * Finds an Owner by ID, logging an error if not found.
     *
     * @param ownerId The ID of the owner.
     * @return The Owner entity or null if not found.
     */
    private Owner findOwner(Long ownerId) {
        try {
            return userRepository.findById(ownerId)
                    .filter(Owner.class::isInstance)
                    .map(Owner.class::cast)
                    .orElseThrow(() -> new EntityNotFoundException("Owner not found with ID: " + ownerId));
        } catch (EntityNotFoundException e) {
            log.error("Could not find owner for notification: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Finds a Vet by ID, logging an error if not found or a Vet.
     *
     * @param vetId The ID of the Vet.
     * @return The Vet entity or null if not found/not a Vet.
     */
    private Vet findVet(Long vetId) {
        try {
            return userRepository.findById(vetId)
                    .filter(Vet.class::isInstance)
                    .map(Vet.class::cast)
                    .orElseThrow(() -> new EntityNotFoundException("Veterinarian not found with ID: " + vetId));
        } catch (EntityNotFoundException e) {
            log.error("Could not find vet for notification: {}", e.getMessage());
            return null;
        }
    }
}
