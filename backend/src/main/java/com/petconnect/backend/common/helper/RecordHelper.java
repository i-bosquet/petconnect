package com.petconnect.backend.common.helper;

import com.petconnect.backend.pet.domain.model.Pet;
import com.petconnect.backend.record.application.dto.RecordCreateDto;
import com.petconnect.backend.record.application.dto.VaccineCreateDto;
import com.petconnect.backend.record.domain.model.RecordType;
import com.petconnect.backend.user.domain.model.Vet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Helper component containing utility logic specific to Record entities,
 * such as creating canonical data strings for signing.
 *
 * @author ibosquet
 */
@Component
@Slf4j
public class RecordHelper {

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
}
