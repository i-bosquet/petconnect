package com.petconnect.backend.record.application.mapper;

import com.petconnect.backend.record.application.dto.RecordViewDto;
import com.petconnect.backend.record.application.dto.VaccineViewDto;
import com.petconnect.backend.record.domain.model.Record;
import com.petconnect.backend.user.application.dto.UserProfileDto;
import com.petconnect.backend.user.application.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

/**
 * Mapper component for converting between {@link Record} entities and {@link RecordViewDto}.
 * Uses UserMapper and VaccineMapper for nested object mapping.
 *
 * @author ibosquet
 */
@Component
@RequiredArgsConstructor
public class RecordMapper {
    private final UserMapper userMapper;
    private final VaccineMapper vaccineMapper;

    /**
     * Converts a {@link Record} entity to a {@link RecordViewDto} for display.
     * Handles mapping of the creator and optional vaccine details.
     * Returns null if the input entity is null.
     *
     * @param entity The Record entity to convert.
     * @return The corresponding RecordViewDto, or null.
     */
    public RecordViewDto toViewDto(Record entity) {
        if (entity == null) {
            return null;
        }

        // Map the creator using UserMapper
        UserProfileDto creatorDto = userMapper.mapToBaseProfileDTO(entity.getCreator());

        // Map the vaccine details if present (only expected if type is VACCINE)
        VaccineViewDto vaccineDto = vaccineMapper.toViewDto(entity.getVaccine());

        return new RecordViewDto(
                entity.getId(),
                entity.getType(),
                entity.getDescription(),
                entity.getVetSignature(),
                entity.getCreatedAt(),
                creatorDto,
                vaccineDto
        );
    }

    /**
     * Converts a Page of {@link Record} entities to a Page of {@link RecordViewDto}.
     * Preserves pagination information.
     *
     * @param recordPage The Page of Record entities.
     * @return A Page containing corresponding RecordViewDto objects.
     */
    public Page<RecordViewDto> toViewDtoPage(Page<Record> recordPage) {
        if (recordPage == null) {
            return Page.empty();
        }
        return recordPage.map(this::toViewDto);
    }
}
