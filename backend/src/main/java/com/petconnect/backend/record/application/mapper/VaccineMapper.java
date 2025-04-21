package com.petconnect.backend.record.application.mapper;

import com.petconnect.backend.record.application.dto.VaccineCreateDto;
import com.petconnect.backend.record.application.dto.VaccineViewDto;
import com.petconnect.backend.record.domain.model.Vaccine;
import org.springframework.stereotype.Component;

/**
 * Mapper component for converting between {@link Vaccine} entities and related DTOs
 * ({@link VaccineCreateDto}, {@link VaccineViewDto}).
 *
 * @author ibosquet
 */
@Component
public class VaccineMapper {
    /**
     * Creates a new {@link Vaccine} entity from a {@link VaccineCreateDto}.
     * Note: This does NOT set the 'record' or 'id' field, as those are handled
     * by the owning Record entity and the @MapsId relationship during persistence.
     * Returns null if the input DTO is null.
     *
     * @param dto The DTO containing vaccine creation data.
     * @return A new Vaccine entity populated from the DTO (transient state).
     */
    public Vaccine fromCreateDto(VaccineCreateDto dto) {
        if (dto == null) {
            return null;
        }
        return Vaccine.builder()
                .name(dto.name())
                .validity(dto.validity())
                .laboratory(dto.laboratory())
                .batchNumber(dto.batchNumber())
                .build();
    }

    /**
     * Converts a {@link Vaccine} entity to a {@link VaccineViewDto} for display.
     * Returns null if the input entity is null.
     *
     * @param entity The Vaccine entity to convert.
     * @return The corresponding VaccineViewDto, or null.
     */
    public VaccineViewDto toViewDto(Vaccine entity) {
        if (entity == null) {
            return null;
        }
        return new VaccineViewDto(
                entity.getName(),
                entity.getValidity(),
                entity.getLaboratory(),
                entity.getBatchNumber()
        );
    }
}
