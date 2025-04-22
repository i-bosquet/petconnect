package com.petconnect.backend.certificate.application.mapper;

import com.petconnect.backend.certificate.application.dto.CertificateViewDto;
import com.petconnect.backend.certificate.domain.model.Certificate;
import com.petconnect.backend.pet.application.mapper.PetMapper;
import com.petconnect.backend.record.application.mapper.RecordSummaryMapper;
import com.petconnect.backend.user.application.mapper.ClinicMapper;
import com.petconnect.backend.user.application.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

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
    private final RecordSummaryMapper recordSummaryMapper;
    private final UserMapper userMapper;
    private final ClinicMapper clinicMapper;

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

        var petDto = Objects.requireNonNull(entity.getPet(), "Certificate entity must have a non-null Pet association.") ;
        var medicalRecordDto = Objects.requireNonNull(entity.getMedicalRecord(), "Certificate entity must have a non-null Record association.");
        var generatorVetDto = Objects.requireNonNull(entity.getGeneratorVet(), "Certificate entity must have a non-null generating Vet.");
        var issuingClinicDto = Objects.requireNonNull(entity.getIssuingClinic(), "Certificate entity must have a non-null issuing Clinic.");

        return new CertificateViewDto(
                entity.getId(),
                entity.getCertificateNumber(),

                petMapper.toProfileDto(petDto),
                recordSummaryMapper.toSummaryDto(medicalRecordDto),
                userMapper.toVetSummaryDto(generatorVetDto),
                clinicMapper.toDto(issuingClinicDto),

                entity.getCreatedAt(),
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
