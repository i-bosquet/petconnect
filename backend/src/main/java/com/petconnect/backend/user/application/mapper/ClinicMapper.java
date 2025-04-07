package com.petconnect.backend.user.application.mapper;

import com.petconnect.backend.user.application.dto.ClinicDto;
import com.petconnect.backend.user.application.dto.ClinicUpdateDto;
import com.petconnect.backend.user.domain.model.Clinic;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Utility class for manually mapping between Clinic entity and its DTOs.
 * Registered as a Spring component to be injectable in services.
 *
 * @author ibosquet
 */
@Component // Make it a Spring bean so it can be injected
public class ClinicMapper {

    /**
     * Maps a Clinic entity to a ClinicDto.
     *
     * @param clinic The Clinic entity. Must not be null.
     * @return The corresponding ClinicDto.
     */
    public ClinicDto toDto(Clinic clinic) {
        if (clinic == null) {
            return null;
        }
        return new ClinicDto(
                clinic.getId(),
                clinic.getName(),
                clinic.getAddress(),
                clinic.getCity(),
                clinic.getCountry(),
                clinic.getPhone(),
                clinic.getPublicKey()
        );
    }

    /**
     * Updates an existing Clinic entity from a ClinicUpdateDto.
     * Applies non-null values from the DTO to the entity.
     * Does not modify fields not present in the DTO (like publicKey).
     *
     * @param dto The DTO containing updated information. Must not be null.
     * @param clinic The Clinic entity to be updated (target). Must not be null.
     */
    public void updateFromDto(ClinicUpdateDto dto, Clinic clinic) {
        if (dto == null || clinic == null) {
            throw new IllegalArgumentException("DTO and Clinic must not be null");
        }

        if (StringUtils.hasText(dto.name()) && !dto.name().equals(clinic.getName())) {
            clinic.setName(dto.name());
        }
        if (StringUtils.hasText(dto.address()) && !dto.address().equals(clinic.getAddress())) {
            clinic.setAddress(dto.address());
        }
        if (StringUtils.hasText(dto.city()) && !dto.city().equals(clinic.getCity())) {
            clinic.setCity(dto.city());
        }
        if (StringUtils.hasText(dto.country()) && !dto.country().equals(clinic.getCountry())) {
            clinic.setCountry(dto.country());
        }
        if (StringUtils.hasText(dto.phone()) && !dto.phone().equals(clinic.getPhone())) {
            clinic.setPhone(dto.phone());
        }
    }
}
