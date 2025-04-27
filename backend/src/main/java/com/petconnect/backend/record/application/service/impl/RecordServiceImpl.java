package com.petconnect.backend.record.application.service.impl;

import com.petconnect.backend.common.helper.AuthorizationHelper;
import com.petconnect.backend.common.helper.EntityFinderHelper;
import com.petconnect.backend.common.helper.RecordHelper;
import com.petconnect.backend.common.helper.ValidateHelper;
import com.petconnect.backend.common.service.SigningService;
import com.petconnect.backend.exception.RecordSignedException;
import com.petconnect.backend.exception.RecordUpdateVaccineException;
import com.petconnect.backend.pet.domain.model.Pet;
import com.petconnect.backend.record.application.dto.RecordCreateDto;
import com.petconnect.backend.record.application.dto.RecordUpdateDto;
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
    private final SigningService signingService;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public RecordViewDto createRecord(RecordCreateDto createDto, Long creatorUserId) {
        Long petId = createDto.petId();
        Pet pet = entityFinderHelper.findPetByIdOrFail(petId);
        UserEntity creator = entityFinderHelper.findUserOrFail(creatorUserId);
        authorizationHelper.verifyUserAuthorizationForPet(creatorUserId, pet, "create record for");
        validateHelper.validateRecordCreationDto(createDto);

        Record newRecord = Record.builder()
                .pet(pet)
                .creator(creator)
                .type(createDto.type())
                .description(createDto.description())
                .build();

        if (createDto.type() == RecordType.VACCINE) {
            Vaccine vaccineEntity = vaccineMapper.fromCreateDto(createDto.vaccine());
            newRecord.setVaccineDetails(vaccineEntity);
        }
        if (creator instanceof Vet vetCreator) {
            log.info("Creator is Vet (ID: {}), proceeding with signature.", creatorUserId);
            String dataToSign = recordHelper.buildSignableData(pet, vetCreator, createDto);
            String signature = signingService.generateVetSignature(vetCreator, dataToSign);
            newRecord.setVetSignature(signature);
            log.info("Record for Pet {} created and signed by Vet {}", petId, creatorUserId);
        } else {
            log.info("Creator (ID: {}) is not a Vet, record will not be signed.", creatorUserId);
        }

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
    public RecordViewDto updateUnsignedRecord(Long recordId, RecordUpdateDto updateDto, Long requesterUserId) {
        log.info("Attempting to update record ID: {} by User ID: {}", recordId, requesterUserId);
        Record record = entityFinderHelper.findRecordByIdOrFail(recordId);
        UserEntity requester = entityFinderHelper.findUserOrFail(requesterUserId);
        UserEntity creator = record.getCreator();

        // Check if Signed
        if (StringUtils.hasText(record.getVetSignature())) {
            log.warn("Update failed for Record ID {}: Record is signed.", recordId);
            throw new RecordSignedException(recordId);
        }

        // Check if Record Type is VACCINE
        if (record.getType() == RecordType.VACCINE) {
            log.warn("Update failed for Record ID {}: Cannot update records of type VACCINE.", recordId);
            throw new RecordUpdateVaccineException(recordId, "records of type VACCINE cannot be updated.");
        }

        boolean authorized = false;

        if (requester instanceof Owner && creator instanceof Owner && Objects.equals(requesterUserId, creator.getId())) {
            authorized = true;
            log.debug("Authorization check: Owner {} updating their own record {}.", requesterUserId, recordId);
        }

        else if (requester instanceof ClinicStaff requesterStaff && creator instanceof ClinicStaff creatorStaff) {
                if (requesterStaff.getClinic() != null &&
                        creatorStaff.getClinic() != null &&
                        Objects.equals(requesterStaff.getClinic().getId(), creatorStaff.getClinic().getId()))
                {
                    authorized = true;
                    log.debug("Authorization check: Staff {} updating record {} created by admin {} in same clinic {}.",
                            requesterUserId, recordId, creator.getId(), requesterStaff.getClinic().getId());
                } else {
                    log.warn("Authorization denied: Staff {} from clinic {} attempting to update record {} created by admin {} from clinic {}.",
                            requesterUserId, (requesterStaff.getClinic() != null ? requesterStaff.getClinic().getId() : "null"),
                            recordId, creator.getId(), (creatorStaff.getClinic() != null ? creatorStaff.getClinic().getId() : "null"));
                }
        }

        if (!authorized) {
            log.warn("Authorization failed: User {} cannot update Record ID {} created by User {} (Type: {}).",
                    requesterUserId, recordId, creator.getId(), creator.getClass().getSimpleName());
            throw new AccessDeniedException("User " + requesterUserId + " is not authorized to update record " + recordId + ".");
        }

        log.debug("Authorization successful for User {} updating Record ID {}", requesterUserId, recordId);

        boolean changed = false;

        if (updateDto.type() != null && updateDto.type() != record.getType()) {
            if (updateDto.type() == RecordType.VACCINE) {
                log.error("Update failed for Record ID {}: Attempted to change type TO VACCINE.", recordId);
                throw new  RecordUpdateVaccineException(recordId, "cannot change record type to VACCINE.");
            }
            log.info("Updating Record ID {} type from {} to {}", recordId, record.getType(), updateDto.type());
            record.setType(updateDto.type());
            changed = true;
        }

        String newDescription = updateDto.description();
        String currentDescription = record.getDescription();
        String effectiveNewDescription = (newDescription != null && newDescription.isBlank()) ? null : newDescription;
        if (newDescription != null && !Objects.equals(effectiveNewDescription, currentDescription)) {
            log.info("Updating Record ID {} description.", recordId);
            record.setDescription(effectiveNewDescription);
            changed = true;
        }

        Record updatedRecord = record;
        if (changed) {
            updatedRecord = recordRepository.save(record);
            log.info("Record ID {} updated successfully by User ID {}", recordId, requesterUserId);
        } else {
            log.info("No effective changes detected for Record ID {}, update skipped.", recordId);
        }

        return recordMapper.toViewDto(updatedRecord);
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
