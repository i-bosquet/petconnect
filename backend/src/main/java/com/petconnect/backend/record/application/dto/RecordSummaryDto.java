package com.petconnect.backend.record.application.dto;

import com.petconnect.backend.record.domain.model.RecordType;
import java.time.LocalDateTime;

/**
 * A summary DTO for Record information, used within CertificateViewDto.
 * Provides basic identification and context of the originating record.
 *
 * @param id          The ID of the originating record.
 * @param type        The type of the originating record.
 * @param description A brief description from the record.
 * @param createdAt   The creation timestamp of the original record.
 *
 * @author ibosquet
 */
public record RecordSummaryDto(
        Long id,
        RecordType type,
        String description,
        LocalDateTime createdAt
) {
}
