package com.petconnect.backend.certificate.application.service.impl;

import com.petconnect.backend.certificate.port.spi.CertificateEventPublisherPort;
import com.petconnect.backend.certificate.application.event.CertificateGeneratedEvent;
import com.petconnect.backend.certificate.application.dto.CertificateGenerationRequestDto;
import com.petconnect.backend.certificate.application.dto.CertificateViewDto;
import com.petconnect.backend.certificate.application.mapper.CertificateMapper;
import com.petconnect.backend.certificate.application.service.CertificateService;
import com.petconnect.backend.certificate.domain.model.Certificate;
import com.petconnect.backend.certificate.domain.repository.CertificateRepository;
import com.petconnect.backend.common.helper.*;
import com.petconnect.backend.common.service.QrCodeService;
import com.petconnect.backend.pet.domain.model.Pet;
import com.petconnect.backend.pet.domain.repository.PetRepository;
import com.petconnect.backend.record.domain.model.Record;
import com.petconnect.backend.user.domain.model.Clinic;
import com.petconnect.backend.user.domain.model.ClinicStaff;
import com.petconnect.backend.user.domain.model.Vet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Implementation of the {@link CertificateService} interface.
 * Handles the business logic for generating and retrieving digital certificates.
 *
 * @author ibosquet
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CertificateServiceImpl implements CertificateService {
    private final CertificateRepository certificateRepository;
    private final CertificateMapper certificateMapper;
    private final EntityFinderHelper entityFinderHelper;
    private final AuthorizationHelper authorizationHelper;
    private final ValidateHelper validateHelper;
    private final RecordHelper recordHelper;
    private final CertificateHelper certificateHelper;
    private final QrCodeService qrCodeService;
    private final PetRepository petRepository;

    private CertificateEventPublisherPort certificateEventPublisher;

    /**
     * Sets the certificate event publisher for the application, enabling or disabling
     * the publishing of certificate-related events. If an instance of
     * {@code CertificateEventPublisherPort} is provided, event publishing is enabled
     * and a log entry is recorded for successful injection. If no instance is provided,
     * a warning is logged and event publishing will be skipped.
     *
     * @param certificateEventPublisher an implementation of {@code CertificateEventPublisherPort},
     *                                   or {@code null} if event publishing should be skipped
     */
    @Autowired(required = false)
    public void setCertificateEventPublisher(CertificateEventPublisherPort certificateEventPublisher) {
        this.certificateEventPublisher = certificateEventPublisher;
        if (certificateEventPublisher != null) {
            log.info("CertificateEventPublisherPort was successfully injected into CertificateServiceImpl.");
        } else {
            log.warn("CertificateEventPublisherPort was NOT injected into CertificateServiceImpl (likely due to profile configuration). Event publishing will be skipped.");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public CertificateViewDto generateCertificate(CertificateGenerationRequestDto requestDto, Long generatingVetId) {
        log.info("Attempting to generate certificate for Pet ID: {} by Vet ID: {}", requestDto.petId(), generatingVetId);

        // Find Entities
        Vet generatingVet = entityFinderHelper.findVetOrFail(generatingVetId);
        Pet pet = entityFinderHelper.findPetByIdOrFail(requestDto.petId());
        Clinic clinic = Objects.requireNonNull(generatingVet.getClinic(), "Generating Vet must be associated with a Clinic.");

        Record validRabiesRecord = validateHelper.findValidRabiesRecord(pet.getId());
        log.debug("Found valid rabies vaccine record ID: {}", validRabiesRecord.getId());

        Record validCheckupRecord = recordHelper.findValidCheckupRecord(pet.getId());
        log.debug("Found valid annual checkup record ID: {}", validCheckupRecord.getId());

        validateHelper.validateCertificateUniqueness(validRabiesRecord.getId(), requestDto.certificateNumber());

        Map<String, Object> payloadMap = certificateHelper.buildPayload(pet, validRabiesRecord, generatingVet, clinic, requestDto.certificateNumber());
        String payloadJson = recordHelper.serializePayload(payloadMap, validRabiesRecord.getId());
        String payloadHash = recordHelper.hashPayload(payloadJson, pet.getId());
        // Send passwords fron Dto to helper to sign payload with keys
        String vetSignature = recordHelper.signWithVetKey(generatingVet, payloadHash, requestDto.vetPrivateKeyPassword().toCharArray());
        String clinicSignature = recordHelper.signWithClinicKey(clinic, payloadHash, requestDto.clinicPrivateKeyPassword().toCharArray());

        // Build and Save Certificate Entity
        Certificate newCertificate = Certificate.builder()
                .pet(pet)
                .medicalRecord(validRabiesRecord)
                .generatorVet(generatingVet)
                .issuingClinic(clinic)
                .certificateNumber(requestDto.certificateNumber())
                .payload(payloadJson)
                .hash(payloadHash)
                .vetSignature(vetSignature)
                .clinicSignature(clinicSignature)
                .build();

        if (!validRabiesRecord.isImmutable()) {
            validRabiesRecord.setImmutable(true);
            log.info("Marking Record ID {} as immutable.", validRabiesRecord.getId());
        }

        if (!validCheckupRecord.isImmutable()) {
            validCheckupRecord.setImmutable(true);
            log.info("Marking Annual Checkup Record ID {} as immutable.", validCheckupRecord.getId());
        }

        Certificate savedCertificate = certificateRepository.save(newCertificate);

        // After saving the certificate, update the pet to remove the pending request
        if (pet.getPendingCertificateClinic() != null && pet.getPendingCertificateClinic().getId().equals(clinic.getId())) {
            log.info("Certificate ID {} generated for Pet ID {}. Clearing pending certificate request from clinic ID {}.",
                    savedCertificate.getId(), pet.getId(), clinic.getId());
            pet.setPendingCertificateClinic(null);
            petRepository.save(pet); // Save the change to the pet
        } else if (pet.getPendingCertificateClinic() != null) {
            log.warn("Certificate ID {} generated for Pet ID {}, but the pending request was for a different clinic (ID: {}) or clinic was null. Pending request not cleared automatically.",
                    savedCertificate.getId(), pet.getId(), pet.getPendingCertificateClinic().getId());
        }

        if (this.certificateEventPublisher != null) {
            try {
                CertificateGeneratedEvent event = new CertificateGeneratedEvent(
                        savedCertificate.getId(),
                        pet.getId(),
                        pet.getOwner().getId(),
                        generatingVetId,
                        savedCertificate.getCertificateNumber(),
                        LocalDateTime.now()
                );
                this.certificateEventPublisher.publishCertificateGenerated(event);
                log.info("CertificateGeneratedEvent published (attempted) for Cert ID {}.", savedCertificate.getId());
            } catch (Exception e) {
                log.error("Failed to publish CertificateGeneratedEvent for certId {} after generation.", savedCertificate.getId(), e);
            }
        } else {
            log.warn("CertificateEventPublisherPort not available. Skipping event publication for Cert ID {}.", savedCertificate.getId());
        }


        log.info("Certificate ID {} generated successfully for Pet ID {} from Record ID {} by Vet ID {}. Event published (attempted).",
                savedCertificate.getId(), pet.getId(), validRabiesRecord.getId(), generatingVetId);

        return certificateMapper.toViewDto(savedCertificate);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public List<CertificateViewDto> findCertificatesByPet(Long petId, Long requesterUserId) {
        Pet pet = entityFinderHelper.findPetByIdOrFail(petId);
        authorizationHelper.verifyUserAuthorizationForPet(requesterUserId, pet, "view certificates for");

        List<Certificate> certificates = certificateRepository.findByPetIdOrderByCreatedAtDesc(petId);
        return certificateMapper.toViewDtoList(certificates);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public CertificateViewDto findCertificateById(Long certificateId, Long requesterUserId) {
        Certificate certificate = entityFinderHelper.findCertificateOrFail(certificateId);
        Pet pet = Objects.requireNonNull(certificate.getPet(), "Certificate must have an associated Pet.");
        authorizationHelper.verifyUserAuthorizationForPet(requesterUserId, pet, "view certificate for");

        return certificateMapper.toViewDto(certificate);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public String getQrDataForCertificate(Long certificateId, Long requesterUserId) {
        log.debug("Request received for QR data for certificate ID: {} by User ID: {}", certificateId, requesterUserId);
        Certificate certificate = entityFinderHelper.findCertificateOrFail(certificateId);

        authorizationHelper.verifyUserAuthorizationForCertificate(requesterUserId, certificate);

        return qrCodeService.generateQrData(certificate);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CertificateViewDto> findCertificatesByClinic(Long clinicId, Long requesterUserId, Pageable pageable) {
        ClinicStaff staff = entityFinderHelper.findClinicStaffOrFail(requesterUserId, "view issued certificates for clinic");
        if (!staff.getClinic().getId().equals(clinicId)) {
            throw new AccessDeniedException("Staff " + requesterUserId + " is not authorized to view certificates for clinic " + clinicId);
        }
        log.info("Staff {} requesting certificates issued by clinic {}", requesterUserId, clinicId);

        Page<Certificate> certificatePage = certificateRepository.findByIssuingClinicIdOrderByCreatedAtDesc(clinicId, pageable);
        return certificatePage.map(certificateMapper::toViewDto);
    }
}
