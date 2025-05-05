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
import com.petconnect.backend.record.domain.model.Record;
import com.petconnect.backend.user.domain.model.Clinic;
import com.petconnect.backend.user.domain.model.Vet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final CertificateEventPublisherPort certificateEventPublisher;

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

        recordHelper.findValidCheckupRecord(pet.getId());
        validateHelper.validateCertificateUniqueness(validRabiesRecord.getId(), requestDto.certificateNumber());

        Map<String, Object> payloadMap = certificateHelper.buildPayload(pet, validRabiesRecord, generatingVet, clinic, requestDto.certificateNumber());
        String payloadJson = recordHelper.serializePayload(payloadMap, validRabiesRecord.getId());
        String payloadHash = recordHelper.hashPayload(payloadJson, pet.getId());
        String vetSignature = recordHelper.signWithVetKey(generatingVet, payloadHash);
        String clinicSignature = recordHelper.signWithClinicKey(clinic, payloadHash);

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

        Certificate savedCertificate = certificateRepository.save(newCertificate);

        try {
            CertificateGeneratedEvent event = new CertificateGeneratedEvent(
                    savedCertificate.getId(),
                    pet.getId(),
                    pet.getOwner().getId(),
                    generatingVetId,
                    savedCertificate.getCertificateNumber(),
                    LocalDateTime.now()
            );
            certificateEventPublisher.publishCertificateGenerated(event);
        } catch (Exception e) {
            log.error("Failed to publish CertificateGeneratedEvent for certId {} after generation.", savedCertificate.getId(), e);
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

        Pet pet = Objects.requireNonNull(certificate.getPet(), "Certificate must have an associated Pet.");
        authorizationHelper.verifyUserAuthorizationForPet(requesterUserId, pet, "get QR data for certificate");

        return qrCodeService.generateQrData(certificate);
    }
}
