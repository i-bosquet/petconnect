package com.petconnect.backend.record.application.mapper;

import com.petconnect.backend.record.application.dto.RecordSummaryDto;
import com.petconnect.backend.record.domain.model.Record;
import org.springframework.stereotype.Component;

/**
 * Maps {@link Record} entity to {@link RecordSummaryDto}.
 * Provides a summarized view of a medical record, suitable for embedding in other DTOs.
 *
 * @author ibosquet
 */
@Component
public class RecordSummaryMapper {
    /**
     * Converts a Record entity to its summary DTO representation.
     *
     * @param recordEntity The Record entity to map.
     * @return A {@link RecordSummaryDto} containing summarized information, or null if the input entity is null.
     */
    public RecordSummaryDto toSummaryDto(Record recordEntity) {
        if (recordEntity == null) {
            return null;
        }

        String descriptionSummary = null;
        if (recordEntity.getDescription() != null) {
            int maxLength = 200;
            descriptionSummary = recordEntity.getDescription().length() <= maxLength
                    ? recordEntity.getDescription()
                    : recordEntity.getDescription().substring(0, maxLength) + "...";
        }

        return new RecordSummaryDto(
                recordEntity.getId(),
                recordEntity.getType(),
                descriptionSummary,
                recordEntity.getCreatedAt()
        );
    }
}
