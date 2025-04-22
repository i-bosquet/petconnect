package com.petconnect.backend.certificate.application.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.petconnect.backend.certificate.application.dto.CertificateGenerationRequestDto;
import com.petconnect.backend.certificate.application.dto.CertificateViewDto;
import com.petconnect.backend.certificate.application.mapper.CertificateMapper;
import com.petconnect.backend.certificate.application.service.CertificateService;
import com.petconnect.backend.certificate.domain.model.Certificate;
import com.petconnect.backend.certificate.domain.repository.CertificateRepository;
import com.petconnect.backend.common.service.SigningService;
import com.petconnect.backend.exception.HashingException;
import com.petconnect.backend.common.service.HashingService;
import com.petconnect.backend.common.service.QrCodeService;
import com.petconnect.backend.pet.domain.model.Pet;
import com.petconnect.backend.record.domain.model.Record;
import com.petconnect.backend.user.domain.model.Clinic;
import com.petconnect.backend.user.domain.model.Vet;
import com.petconnect.backend.common.helper.AuthorizationHelper;
import com.petconnect.backend.common.helper.EntityFinderHelper;
import com.petconnect.backend.common.helper.CertificateHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final HashingService hashingService;
    private final SigningService signingService;
    private final CertificateHelper certificateHelper;
    private final QrCodeService qrCodeService;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public CertificateViewDto generateCertificate(CertificateGenerationRequestDto requestDto, Long generatingVetId) {
        log.info("Attempting to generate certificate for record ID: {} by Vet ID: {}", requestDto.recordId(), generatingVetId);

        // Find Entities and Validate Prerequisites
        Vet generatingVet = entityFinderHelper.findVetOrFail(generatingVetId);
        Record sourceRecord = entityFinderHelper.findRecordByIdOrFail(requestDto.recordId());
        Pet pet = Objects.requireNonNull(sourceRecord.getPet(), "Source record must be associated with a Pet.");
        Clinic clinic = Objects.requireNonNull(generatingVet.getClinic(), "Generating Vet must be associated with a Clinic.");

        certificateHelper.validateCertificateGenerationPrerequisites(generatingVet, sourceRecord, requestDto.certificateNumber());

        // Construct Payload
        Map<String, Object> payloadMap = certificateHelper.buildPayload(pet, sourceRecord, generatingVet, clinic, requestDto.certificateNumber());

        String payloadJson;
        try {
            payloadJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(payloadMap);
        } catch (JsonProcessingException e) {
            log.error("Error serializing certificate payload to JSON for record {}", sourceRecord.getId(), e);
            throw new RuntimeException("Failed to create certificate payload.", e);
        }

        // Hash Payload
        String payloadHash;
        try {
            payloadHash = hashingService.hashString(payloadJson);
        } catch (HashingException e) {
            log.error("Error hashing certificate payload for record {}", sourceRecord.getId(), e);
            throw new RuntimeException("Failed to hash certificate payload.", e);
        }

        // Generate Signatures
        String vetSignature;
        try {
            vetSignature = signingService.generateVetSignature(generatingVet, payloadHash);
        } catch (RuntimeException e) {
            log.error("Error generating Vet signature for record {}: {}", sourceRecord.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to generate Vet digital signature.", e);
        }
        String clinicSignature;
        try {
            clinicSignature = signingService.generateClinicSignature(clinic, payloadHash);
        } catch (RuntimeException e) {
            log.error("Error generating Clinic signature for record {}: {}", sourceRecord.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to generate Clinic digital signature.", e);
        }

        // Build and Save Certificate Entity
        Certificate newCertificate = Certificate.builder()
                .pet(pet)
                .medicalRecord(sourceRecord)
                .generatorVet(generatingVet)
                .issuingClinic(clinic)
                .certificateNumber(requestDto.certificateNumber())
                .payload(payloadJson)
                .hash(payloadHash)
                .vetSignature(vetSignature)
                .clinicSignature(clinicSignature)
                .build();

        Certificate savedCertificate = certificateRepository.save(newCertificate);
        log.info("Certificate ID {} generated successfully for Pet ID {} from Record ID {} by Vet ID {}",
                savedCertificate.getId(), pet.getId(), sourceRecord.getId(), generatingVetId);

        // Map and Return DTO
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
