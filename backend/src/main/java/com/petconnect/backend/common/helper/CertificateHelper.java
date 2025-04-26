package com.petconnect.backend.common.helper;

import com.petconnect.backend.pet.domain.model.Pet;
import com.petconnect.backend.record.domain.model.Record;
import com.petconnect.backend.record.domain.model.RecordType;
import com.petconnect.backend.record.domain.model.Vaccine;
import com.petconnect.backend.user.domain.model.Clinic;
import com.petconnect.backend.user.domain.model.Owner;
import com.petconnect.backend.user.domain.model.Vet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Helper component containing business logic related to Certificates,
 * such as validation prerequisites and payload construction.
 *
 * @author ibosquet
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CertificateHelper {

    /**
     * Constructs the certificate payload as a Map.
     * Ensure the structure is well-defined and includes all necessary information.
     * Use LinkedHashMap to maintain insertion order if relevant for canonicalization/hashing.
     * Delegates event payload creation to buildEventPayload.
     *
     * @param pet                The Pet.
     * @param sourceRecord       The source Record.
     * @param vet                The generating Vet.
     * @param clinic             The issuing Clinic.
     * @param certificateNumber  The official certificate number.
     * @return A Map representing the certificate payload.
     */
    public Map<String, Object> buildPayload(Pet pet, Record sourceRecord, Vet vet, Clinic clinic, String certificateNumber) {
        Map<String, Object> payload = new LinkedHashMap<>();
        Owner owner = pet.getOwner();

        // --- Root Level ---
        payload.put("certType", "PET_VACCINATION_CERT_V1");
        payload.put("issuanceTimestamp", System.currentTimeMillis());
        payload.put("certificateNumber", certificateNumber);

        // --- Issuer Details ---
        Map<String, Object> issuer = new LinkedHashMap<>();
        issuer.put("id", clinic.getId());
        issuer.put("name", clinic.getName());
        issuer.put("country", clinic.getCountry().name());
        issuer.put("issuingVetId", vet.getId());
        issuer.put("issuingVetName", vet.getName() + " " + vet.getSurname());
        issuer.put("issuingVetLicense", vet.getLicenseNumber());
        payload.put("issuer", issuer);

        // --- Subject Details (Pet & Owner) ---
        Map<String, Object> subject = new LinkedHashMap<>();
        subject.put("petId", pet.getId());
        subject.put("petName", pet.getName());
        subject.put("petSpecies", pet.getBreed().getSpecie().name());
        subject.put("petBreed", pet.getBreed().getName());
        subject.put("petBirthDate", pet.getBirthDate() != null ? pet.getBirthDate().toString() : null);
        subject.put("petGender", pet.getGender() != null ? pet.getGender().name() : null);
        subject.put("petColor", pet.getColor());
        subject.put("petMicrochip", pet.getMicrochip());
        if (owner != null) {
            subject.put("ownerInfo", Map.of("id", owner.getId()));
        }
        payload.put("subject", subject);

        // --- Event Details (Record & Vaccine if applicable) ---
        payload.put("event", buildEventPayload(sourceRecord));

        log.debug("Payload constructed for certificate generation: {}", payload);
        return payload;
    }

    /**
     * Builds the 'event' part of the certificate payload based on the source record.
     * @param sourceRecord The medical record (must be a VACCINE type for vaccine details).
     * @return A Map containing event details.
     */
    private Map<String, Object> buildEventPayload(Record sourceRecord) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("recordId", sourceRecord.getId());
        event.put("recordType", sourceRecord.getType().name());
        LocalDate recordDate = (sourceRecord.getCreatedAt() != null) ? sourceRecord.getCreatedAt().toLocalDate() : null;
        event.put("recordDate", recordDate != null ? recordDate.toString() : null);

        if (sourceRecord.getType() == RecordType.VACCINE && sourceRecord.getVaccine() != null) {
            Vaccine vaccine = sourceRecord.getVaccine();
            Map<String, Object> vaccineInfo = new LinkedHashMap<>();
            vaccineInfo.put("name", vaccine.getName());
            vaccineInfo.put("batch", vaccine.getBatchNumber());
            vaccineInfo.put("manufacturer", vaccine.getLaboratory());
            vaccineInfo.put("validityYears", vaccine.getValidity());
            if (recordDate != null && vaccine.getValidity() != null && vaccine.getValidity() > 0) {
                vaccineInfo.put("expiryDate", recordDate.plusYears(vaccine.getValidity()).toString());
            }
            event.put("vaccinationDetails", vaccineInfo);
        } else {
            if (sourceRecord.getType() != RecordType.VACCINE) {
                event.put("description", sourceRecord.getDescription());
            }
        }
        return event;
    }
}
