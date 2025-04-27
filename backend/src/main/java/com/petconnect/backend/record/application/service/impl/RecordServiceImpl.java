package com.petconnect.backend.record.application.service.impl;

import com.petconnect.backend.common.helper.AuthorizationHelper;
import com.petconnect.backend.common.helper.EntityFinderHelper;
import com.petconnect.backend.common.helper.RecordHelper;
import com.petconnect.backend.common.helper.ValidateHelper;
import com.petconnect.backend.common.service.SigningService;
import com.petconnect.backend.exception.RecordSignedException;
import com.petconnect.backend.exception.RecordUpdateVaccineException;
import com.petconnect.backend.pet.domain.model.Pet;
import com.petconnect.backend.record.application.dto.*;
import com.petconnect.backend.record.application.mapper.RecordMapper;
import com.petconnect.backend.record.application.mapper.VaccineMapper;
import com.petconnect.backend.record.application.service.RecordService;
import com.petconnect.backend.record.domain.model.Record;
import com.petconnect.backend.record.domain.model.RecordType;
import com.petconnect.backend.record.domain.model.Vaccine;
import com.petconnect.backend.record.domain.repository.RecordRepository;
import com.petconnect.backend.security.JwtUtils;
import com.petconnect.backend.user.domain.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.format.DateTimeParseException;
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
    private final JwtUtils jwtUtils;

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
        Record recordtoUpdate = entityFinderHelper.findRecordByIdOrFail(recordId);
        UserEntity requester = entityFinderHelper.findUserOrFail(requesterUserId);

        if (StringUtils.hasText(recordtoUpdate.getVetSignature())) {
            log.warn("Update failed for Record ID {}: Record is signed.", recordId);
            throw new RecordSignedException(recordId);
        }

        if (recordtoUpdate.getType() == RecordType.VACCINE) {
            log.warn("Update failed for Record ID {}: Cannot update records of type VACCINE.", recordId);
            throw new RecordUpdateVaccineException(recordId, "records of type VACCINE cannot be updated.");
        }

        if (updateDto.type() != null && updateDto.type() == RecordType.VACCINE) {
            log.error("Update failed for Record ID {}: Attempted to change type TO VACCINE.", recordId);
            throw new RecordUpdateVaccineException(recordId, "cannot change record type to VACCINE.");
        }

        authorizationHelper.verifyUserAuthorizationForUnsignedRecordUpdate(requester, recordtoUpdate);
        log.debug("Authorization successful for User {} updating Record ID {}", requesterUserId, recordId);

        boolean changed = false;

        if (updateDto.type() != null && updateDto.type() != recordtoUpdate.getType()) {

            log.info("Updating Record ID {} type from {} to {}", recordId, recordtoUpdate.getType(), updateDto.type());
            recordtoUpdate.setType(updateDto.type());
            changed = true;
        }
        String newDescription = updateDto.description();
        if (newDescription != null) {
            String effectiveNewDescription = newDescription.isBlank() ? null : newDescription;
            if (!Objects.equals(effectiveNewDescription, recordtoUpdate.getDescription())) {
                log.info("Updating Record ID {} description.", recordId);
                recordtoUpdate.setDescription(effectiveNewDescription);
                changed = true;
            }
        }
        Record updatedRecord = recordtoUpdate;
        if (changed) {
            updatedRecord = recordRepository.save(recordtoUpdate);
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
    public void deleteRecord(Long recordId, Long requesterUserId) {
        log.info("Attempting to delete Record ID {} by User ID {}", recordId, requesterUserId);
        Record recordToDelete = entityFinderHelper.findRecordByIdOrFail(recordId);
        UserEntity requester = entityFinderHelper.findUserOrFail(requesterUserId);

        authorizationHelper.verifyUserAuthorizationForRecordDeletion(requester, recordToDelete);

        recordRepository.delete(recordToDelete);
        log.info("Record ID {} deleted successfully by User ID {}", recordId, requesterUserId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TemporaryAccessTokenDto generateTemporaryAccessToken(Long petId, TemporaryAccessRequestDto requestDto, Long requesterUserId) {
        log.info("Generating temporary access token for Pet ID {} requested by User ID {}", petId, requesterUserId);

        // Verify Pet exists and requester is the Owner
        Pet pet = entityFinderHelper.findPetByIdOrFail(petId);
        if (pet.getOwner() == null || !Objects.equals(requesterUserId, pet.getOwner().getId())) {
            log.warn("Access denied: User {} is not the owner of Pet {}", requesterUserId, petId);
            throw new AccessDeniedException("Only the pet owner can generate temporary access tokens.");
        }

        // Duration
        Duration duration;
        try {
            duration = Duration.parse(requestDto.durationString());
            if (duration.isNegative() || duration.isZero()) {
                throw new IllegalArgumentException("Duration must be positive.");
            }
            // limit: Max 1 week
            if (duration.compareTo(Duration.ofDays(7)) > 0) {
                duration = Duration.ofDays(7); // Cap duration
                log.warn("Requested duration too long for Pet ID {}. Capping at 7 days.", petId);
            }
        } catch (DateTimeParseException e) {
            log.error("Invalid duration string provided: {}", requestDto.durationString(), e);
            throw new IllegalArgumentException("Invalid duration format. Use ISO-8601 duration format (e.g., PT1H, P1D).");
        }

        // Generate Temporary Token using JwtUtils
        String token = jwtUtils.createTemporaryRecordAccessToken(petId, duration);
        log.info("Temporary access token generated successfully for Pet ID {} with duration {}", petId, duration);

        return new TemporaryAccessTokenDto(token);
    }

}
