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
import com.petconnect.backend.common.service.HashingService;
import com.petconnect.backend.common.service.QrCodeService;
import com.petconnect.backend.exception.*;
import com.petconnect.backend.pet.domain.model.Pet;
import com.petconnect.backend.record.domain.model.Record;
import com.petconnect.backend.record.domain.model.RecordType;
import com.petconnect.backend.record.domain.model.Vaccine;
import com.petconnect.backend.record.domain.repository.RecordRepository;
import com.petconnect.backend.user.domain.model.Clinic;
import com.petconnect.backend.user.domain.model.Vet;
import com.petconnect.backend.common.helper.AuthorizationHelper;
import com.petconnect.backend.common.helper.EntityFinderHelper;
import com.petconnect.backend.common.helper.CertificateHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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
    private final HashingService hashingService;
    private final SigningService signingService;
    private final CertificateHelper certificateHelper;
    private final QrCodeService qrCodeService;
    private final RecordRepository recordRepository;
    private static final int CHECKUP_VALIDITY_YEARS = 1;

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

        // --- Find and Validate Rabies Vaccine Record ---
        List<Record> potentialRabiesRecords = recordRepository
                .findAllSignedRabiesVaccinesDesc(pet.getId());

        Record validRabiesRecord = potentialRabiesRecords.stream()
                .filter(r -> {
                    Vaccine v = r.getVaccine();
                    LocalDateTime vaccinationDate = r.getCreatedAt();
                    if (v == null || v.getValidity() == null || vaccinationDate == null) {
                        log.warn("Skipping rabies record {} due to missing vaccine details, validity, or creation date.", r.getId());
                        return false;
                    }
                    if (v.getValidity() >= 0) {
                        LocalDate expiryDate = vaccinationDate.toLocalDate().plusYears(v.getValidity());
                        return !LocalDate.now().isAfter(expiryDate);
                    }
                    return false;
                })
                .findFirst()
                .orElseThrow(() -> new MissingRabiesVaccineException(pet.getId()));
        log.debug("Found valid rabies vaccine record ID: {}", validRabiesRecord.getId());


        // --- Find and Validate Recent Signed Checkup ---
        LocalDateTime checkupCutoff = LocalDate.now().minusYears(CHECKUP_VALIDITY_YEARS).atStartOfDay();
        List<RecordType> checkupTypes = List.of(RecordType.ANNUAL_CHECK);

        List<Record> potentialCheckupRecords = recordRepository
                .findSignedCheckupsAfterDateDesc(pet.getId(), checkupTypes, checkupCutoff);

        if (potentialCheckupRecords.isEmpty()) {
            throw new MissingRecentCheckupException(pet.getId(), checkupCutoff.toLocalDate());
        }
        Record validCheckupRecord = potentialCheckupRecords.getFirst();
        log.debug("Found valid recent checkup record ID: {}", validCheckupRecord.getId());

        // --- Check Certificate Uniqueness
        if (certificateRepository.existsByMedicalRecordId(validRabiesRecord.getId())) {
            throw new CertificateAlreadyExistsForRecordException(validRabiesRecord.getId());
        }
        if (certificateRepository.findByCertificateNumber(requestDto.certificateNumber()).isPresent()) {
            throw new CertificateNumberAlreadyExistsException(requestDto.certificateNumber());
        }

        // --- Construct Payload, Hash, Sign, Persist

        Map<String, Object> payloadMap = certificateHelper.buildPayload(pet, validRabiesRecord, generatingVet, clinic, requestDto.certificateNumber());

        String payloadJson;
        try {
            ObjectMapper localObjectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
            payloadJson = localObjectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(payloadMap);
        } catch (JsonProcessingException e) {
            log.error("Error serializing certificate payload to JSON for record {}", validRabiesRecord.getId(), e);
            throw new RuntimeException("Failed to create certificate payload.", e);
        }

        // Hash Payload
        String payloadHash;
        try {
            payloadHash = hashingService.hashString(payloadJson);
        } catch (HashingException e) {
            log.error("Failed to hash certificate payload for Pet ID {}: {}", requestDto.petId(), e.getMessage(), e);
            throw new RuntimeException("Failed to hash certificate payload.", e);
        }

        // Generate Signatures
        String vetSignature;
        try {
            vetSignature = signingService.generateVetSignature(generatingVet, payloadHash);
        } catch (RuntimeException e) {
            log.error("Failed to generate Vet signature for Pet ID {} by Vet ID {}: {}", requestDto.petId(), generatingVetId, e.getMessage(), e);
            throw new RuntimeException("Failed to generate Vet digital signature.", e);
        }

        String clinicSignature;
        try {
            clinicSignature = signingService.generateClinicSignature(clinic, payloadHash);
        } catch (RuntimeException e) {
            log.error("Failed to generate Clinic signature for Pet ID {} by Clinic ID {}: {}", requestDto.petId(), clinic.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to generate Clinic digital signature.", e);
        }

        if (!validRabiesRecord.isImmutable()) {
            log.info("Marking Record ID {} as immutable because it is being used for Certificate {}", validRabiesRecord.getId(), requestDto.certificateNumber());
            validRabiesRecord.setImmutable(true);
        } else {
            log.warn("Record ID {} was already marked as immutable before certificate generation commit.", validRabiesRecord.getId());
        }

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

        Certificate savedCertificate = certificateRepository.save(newCertificate);
        log.info("Certificate ID {} generated successfully for Pet ID {} from Record ID {} by Vet ID {}",
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
