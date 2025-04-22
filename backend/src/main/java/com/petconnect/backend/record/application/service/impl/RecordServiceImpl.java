package com.petconnect.backend.record.application.service.impl;

import com.petconnect.backend.common.helper.AuthorizationHelper;
import com.petconnect.backend.common.helper.EntityFinderHelper;
import com.petconnect.backend.common.helper.RecordHelper;
import com.petconnect.backend.common.helper.ValidateHelper;
import com.petconnect.backend.common.service.impl.SigningServiceImpl;
import com.petconnect.backend.pet.domain.model.Pet;
import com.petconnect.backend.record.application.dto.RecordCreateDto;
import com.petconnect.backend.record.application.dto.RecordViewDto;
import com.petconnect.backend.record.application.mapper.RecordMapper;
import com.petconnect.backend.record.application.mapper.VaccineMapper;
import com.petconnect.backend.record.application.service.RecordService;
import com.petconnect.backend.record.domain.model.Record;
import com.petconnect.backend.record.domain.model.RecordType;
import com.petconnect.backend.record.domain.model.Vaccine;
import com.petconnect.backend.record.domain.repository.RecordRepository;
import com.petconnect.backend.user.domain.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Objects;

/**
 * Implementation of the {@link RecordService} interface.
 * Handles the business logic for managing medical records.
 *
 * @author ibosquet
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecordServiceImpl implements RecordService {

    private final RecordRepository recordRepository;
    private final RecordMapper recordMapper;
    private final VaccineMapper vaccineMapper;
    private final EntityFinderHelper entityFinderHelper;
    private final AuthorizationHelper authorizationHelper;
    private final ValidateHelper validateHelper;
    private final RecordHelper recordHelper;
    private final SigningServiceImpl signingServiceImpl;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public RecordViewDto createRecord(RecordCreateDto createDto, Long creatorUserId, boolean signRecord) {
        // Find Pet and Creator User
        Long petId = createDto.petId();
        Pet pet = entityFinderHelper.findPetByIdOrFail(petId);

        UserEntity creator = entityFinderHelper.findUserOrFail(creatorUserId);
        authorizationHelper.verifyUserAuthorizationForPet(creatorUserId, pet, "create record for");
        validateHelper.validateRecordCreationDto(createDto);

        // Build the Record entity
        Record newRecord = Record.builder()
                .pet(pet)
                .creator(creator)
                .type(createDto.type())
                .description(createDto.description())
                .build();

        // Handle Vaccine Details if applicable
        if (createDto.type() == RecordType.VACCINE) {
            Vaccine vaccineEntity = vaccineMapper.fromCreateDto(createDto.vaccine());
            newRecord.setVaccineDetails(vaccineEntity);
        }

        // Handle Signing request (if applicable)
        if (signRecord) {
            if (!(creator instanceof Vet vetCreator)) {
                log.warn("Attempt to sign record by non-Vet user: {}", creatorUserId);
                throw new IllegalStateException("Only Veterinarians can sign records. User " + creatorUserId + " is not a Vet.");
            }
            String dataToSign = recordHelper.buildSignableData(pet, vetCreator, createDto);
            String signature = signingServiceImpl.generateVetSignature(vetCreator, dataToSign);
            newRecord.setVetSignature(signature);
            log.info("Record for Pet {} created and signed by Vet {}", petId, creatorUserId);
        }

        // Save the record (and cascaded Vaccine if present)
        Record savedRecord = recordRepository.save(newRecord);
        log.info("User {} created new record ID {} for Pet {}", creatorUserId, savedRecord.getId(), petId);
        return recordMapper.toViewDto(savedRecord);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public Page<RecordViewDto> findRecordsByPetId( Long petId,Long requesterUserId, Pageable pageable) {
        Pet pet = entityFinderHelper.findPetByIdOrFail(petId);
        authorizationHelper.verifyUserAuthorizationForPet(requesterUserId, pet, "view records for");

        Page<Record> recordPage = recordRepository.findByPetIdOrderByCreatedAtDesc(petId, pageable);
        return recordMapper.toViewDtoPage(recordPage);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public RecordViewDto findRecordById(Long recordId, Long requesterUserId) {
        Record recordEntity = entityFinderHelper.findRecordByIdOrFail(recordId);
        Pet associatedPet = recordEntity.getPet();
        if (associatedPet == null) {
            log.error("Record {} found but has no associated Pet.", recordId);
            throw new IllegalStateException("Record data inconsistent: Missing associated Pet.");
        }
        authorizationHelper.verifyUserAuthorizationForPet(requesterUserId, associatedPet, "view record for");

        return recordMapper.toViewDto(recordEntity);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void deleteUnsignedRecord(Long recordId, Long requesterUserId) {
        Record recordEntity = entityFinderHelper.findRecordByIdOrFail(recordId);
        UserEntity requester = entityFinderHelper.findUserOrFail(requesterUserId);

        if (StringUtils.hasText(recordEntity.getVetSignature())) {
            throw new IllegalStateException("Cannot delete record " + recordId + " because it has been signed by a veterinarian.");
        }

        boolean isCreator = Objects.equals(recordEntity.getCreator().getId(), requesterUserId);
        boolean isAdminDeletingStaffRecord = false;
        switch (requester) {
            case ClinicStaff requesterStaff when recordEntity.getCreator() instanceof ClinicStaff recordCreatorStaff && recordCreatorStaff.getClinic() != null && Objects.equals(requesterStaff.getClinic().getId(), recordCreatorStaff.getClinic().getId()) && requesterStaff.getRoles().stream().anyMatch(r -> r.getRoleEnum() == RoleEnum.ADMIN) ->
                    isAdminDeletingStaffRecord = true;
            case null, default -> {
            }
        }

        if (!isCreator && !isAdminDeletingStaffRecord) {
            throw new AccessDeniedException("User " + requesterUserId + " is not authorized to delete record " + recordId + ".");
        }

        recordRepository.delete(recordEntity);
        log.info("User {} deleted unsigned record ID {}", requesterUserId, recordId);
    }
}
