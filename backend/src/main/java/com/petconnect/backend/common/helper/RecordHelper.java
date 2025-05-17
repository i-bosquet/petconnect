package com.petconnect.backend.common.helper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.petconnect.backend.exception.HashingException;
import com.petconnect.backend.exception.MissingRecentCheckupException;
import com.petconnect.backend.common.service.SigningService;
import com.petconnect.backend.common.service.HashingService;
import com.petconnect.backend.pet.domain.model.Pet;
import com.petconnect.backend.record.application.dto.RecordCreateDto;
import com.petconnect.backend.record.application.dto.VaccineCreateDto;
import com.petconnect.backend.record.domain.model.RecordType;
import com.petconnect.backend.record.domain.model.Record;
import com.petconnect.backend.record.domain.repository.RecordRepository;
import com.petconnect.backend.user.domain.model.Clinic;
import com.petconnect.backend.user.domain.model.Vet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Helper component containing utility logic specific to Record entities,
 * such as creating canonical data strings for signing.
 *
 * @author ibosquet
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RecordHelper {

    private final RecordRepository recordRepository;
    private final HashingService hashingService;
    private final SigningService signingService;

    /**
     * Creates a consistent string representation of the record data for signing.
     * The order and inclusion of fields MUST be consistent for verification to work.
     * Uses data from the Pet, the creating Vet, and the creation DTO.
     *
     * @param pet        The pet associated with the record. Must not be null.
     * @param vetCreator The Vet creating/signing the record. Must not be null.
     * @param dto        The creation DTO containing the record details. Must not be null.
     * @return A canonical string representation of the data to be signed.
     */
    public String buildSignableData(Pet pet, Vet vetCreator, RecordCreateDto dto) {
        if (pet == null || vetCreator == null || dto == null) {
            log.error("Cannot build signable data: Pet, Vet, or DTO is null.");
            throw new IllegalArgumentException("Pet, Vet creator, and Record DTO must not be null for signing.");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("petId=").append(pet.getId()).append("|");
        sb.append("vetId=").append(vetCreator.getId()).append("|");
        sb.append("vetClinicId=").append(vetCreator.getClinic() != null ? vetCreator.getClinic().getId() : "null").append("|");
        sb.append("vetLicense=").append(vetCreator.getLicenseNumber()).append("|");
        sb.append("recordType=").append(dto.type()).append("|");
        sb.append("description=").append(dto.description() != null ? dto.description() : "").append("|");

        if (dto.type() == RecordType.VACCINE) {
            VaccineCreateDto vDto = dto.vaccine();
            if (vDto == null) {
                log.error("Record type is VACCINE but vaccine details are missing in DTO for Pet ID {}", pet.getId());
                throw new IllegalArgumentException("Vaccine details missing for VACCINE record type during signing preparation.");
            }
            sb.append("vaccineName=").append(vDto.name()).append("|");
            sb.append("vaccineBatch=").append(vDto.batchNumber()).append("|");
            sb.append("vaccineLab=").append(vDto.laboratory() != null ? vDto.laboratory() : "").append("|");
            sb.append("vaccineValidity=").append(vDto.validity()).append("|");
        }

        String data = sb.toString();
        log.debug("Data prepared for signing for Pet ID {}: {}", pet.getId(), data);
        return data;
    }

    /**
     * Validates that a recent, signed annual checkup record exists for the pet.
     * Throws MissingRecentCheckupException if no suitable record is found within the
     * defined validity period (e.g., 1 year).
     *
     * @param petId The ID of the pet.
     * @throws MissingRecentCheckupException if no valid checkup is found.
     */
    public void findValidCheckupRecord(Long petId) {
        LocalDateTime checkupCutoff = LocalDate.now().minusYears(1).atStartOfDay();
        List<RecordType> checkupTypes = List.of(RecordType.ANNUAL_CHECK);
        List<Record> potentialCheckupRecords = recordRepository.findSignedCheckupsAfterDateDesc(petId, checkupTypes, checkupCutoff);
        if (potentialCheckupRecords.isEmpty()) {
            throw new MissingRecentCheckupException(petId, checkupCutoff.toLocalDate());
        }
        log.debug("Found valid recent checkup record ID: {}", potentialCheckupRecords.getFirst().getId());
    }

    /**
     * Serializes the certificate payload map into a JSON string.
     * Uses pretty printing for readability if stored/logged.
     *
     * @param payloadMap The map representing the certificate payload.
     * @param recordId The ID of the source record (for logging purposes).
     * @return The JSON string representation of the payload.
     * @throws RuntimeException if JSON serialization fails.
     */
    public String serializePayload(Map<String, Object> payloadMap, Long recordId) {
        try {
            ObjectMapper localObjectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
            return localObjectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(payloadMap);
        } catch (JsonProcessingException e) {
            log.error("Error serializing certificate payload to JSON for record {}", recordId, e);
            throw new RuntimeException("Failed to create certificate payload.", e);
        }
    }

    /**
     * Hashes the given payload string using the configured hashing service.
     *
     * @param payloadJson The JSON string payload to hash.
     * @param petId The ID of the pet (for logging purposes).
     * @return The hexadecimal representation of the hash.
     * @throws RuntimeException if hashing fails.
     */
    public String hashPayload(String payloadJson, Long petId) {
        try {
            return hashingService.hashString(payloadJson);
        } catch (HashingException e) {
            log.error("Failed to hash certificate payload for Pet ID {}: {}", petId, e.getMessage(), e);
            throw new RuntimeException("Failed to hash certificate payload.", e);
        }
    }

    /**
     * Generates a digital signature for the given data using the Vet's private key.
     * Delegates to the SigningService.
     *
     * @param vet        The Vet performing the signing.
     * @param dataToSign The data (typically a hash) to sign.
     * @return The Base64 encoded signature.
     * @throws RuntimeException if signing fails.
     */
    public String signWithVetKey(Vet vet, String dataToSign, char[] vetKeyPassword) {
        try {
            return signingService.generateVetSignature(vet, dataToSign, vetKeyPassword);
        } catch (RuntimeException e) {
            log.error("Failed to generate Vet signature for Pet by Vet ID {}: {}", vet.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to generate Vet digital signature.", e);
        }
    }

    /**
     * Generates a digital signature for the given data using the Clinic's private key.
     * Delegates to the SigningService.
     *
     * @param clinic     The Clinic performing the signing.
     * @param dataToSign The data (typically a hash) to sign.
     * @return The Base64 encoded signature.
     * @throws RuntimeException if signing fails.
     */
    public String signWithClinicKey(Clinic clinic, String dataToSign, char[] clinicKeyPassword) {
        try {
            return signingService.generateClinicSignature(clinic, dataToSign, clinicKeyPassword);
        } catch (RuntimeException e) {
            log.error("Failed to generate Clinic signature for Clinic ID {}: {}", clinic.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to generate Clinic digital signature.", e);
        }
    }
}
