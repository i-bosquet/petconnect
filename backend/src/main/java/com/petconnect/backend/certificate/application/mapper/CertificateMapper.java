package com.petconnect.backend.certificate.application.mapper;

import com.petconnect.backend.certificate.application.dto.CertificateViewDto;
import com.petconnect.backend.certificate.domain.model.Certificate;
import com.petconnect.backend.pet.application.dto.PetProfileDto;
import com.petconnect.backend.pet.application.mapper.PetMapper;
import com.petconnect.backend.record.application.dto.RecordViewDto;
import com.petconnect.backend.record.application.mapper.RecordMapper;
import com.petconnect.backend.user.application.dto.VetSummaryDto;
import com.petconnect.backend.user.application.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Mapper component for converting between {@link Certificate} entities and {@link CertificateViewDto}.
 * Uses other mappers (Pet, Record, User, Clinic) for mapping related entity summaries.
 *
 * @author ibosquet
 */
@Component
@RequiredArgsConstructor
public class CertificateMapper {

    private final PetMapper petMapper;
    private final RecordMapper recordMapper;
    private final UserMapper userMapper;

    /**
     * Converts a {@link Certificate} entity to a {@link CertificateViewDto}.
     * Handles mapping of related entity summaries. Returns null if the entity is null.
     *
     * @param entity The Certificate entity to convert.
     * @return The corresponding CertificateViewDto, or null.
     */
    public CertificateViewDto toViewDto(Certificate entity) {
        if (entity == null) {
            return null;
        }

        PetProfileDto petDto = petMapper.toProfileDto(
                Objects.requireNonNull(entity.getPet(), "Certificate entity must have a non-null Pet association.")
        );
        RecordViewDto originatingRecordDto = recordMapper.toViewDto(
                Objects.requireNonNull(entity.getMedicalRecord(), "Certificate entity must have a non-null Record association.")
        );
        VetSummaryDto generatorVetDto = userMapper.toVetSummaryDto(
                Objects.requireNonNull(entity.getGeneratorVet(), "Certificate entity must have a non-null generating Vet.")
        );

        LocalDate initialEuEntryExpiryDate = null;
        LocalDate travelValidityEndDate = null;
        if (entity.getCreatedAt() != null) {
            LocalDate createdAtDate = entity.getCreatedAt().toLocalDate();
            initialEuEntryExpiryDate = createdAtDate.plusDays(10);
            travelValidityEndDate = createdAtDate.plusMonths(4);
        }

        return new CertificateViewDto(
                entity.getId(),
                entity.getCertificateNumber(),
                petDto,
                originatingRecordDto,
                generatorVetDto,
                entity.getCreatedAt(),
                initialEuEntryExpiryDate,
                travelValidityEndDate,
                entity.getPayload(),
                entity.getHash(),
                entity.getVetSignature(),
                entity.getClinicSignature()
        );
    }

    /**
     * Converts a list of {@link Certificate} entities to an unmodifiable list of {@link CertificateViewDto}.
     * Returns an empty list if the input is null or empty.
     *
     * @param certificates The list of Certificate entities.
     * @return An unmodifiable list of corresponding CertificateViewDto objects.
     */
    public List<CertificateViewDto> toViewDtoList(List<Certificate> certificates) {
        if (certificates == null || certificates.isEmpty()) {
            return Collections.emptyList();
        }
        return certificates.stream()
                .map(this::toViewDto)
                .toList();
    }
}
