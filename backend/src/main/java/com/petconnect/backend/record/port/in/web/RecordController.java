package com.petconnect.backend.record.port.in.web;

import com.petconnect.backend.common.helper.UserHelper;
import com.petconnect.backend.record.application.dto.RecordCreateDto;
import com.petconnect.backend.record.application.dto.RecordUpdateDto;
import com.petconnect.backend.record.application.dto.RecordViewDto;
import com.petconnect.backend.record.application.dto.TemporaryAccessRequestDto;
import com.petconnect.backend.record.application.dto.TemporaryAccessTokenDto;
import com.petconnect.backend.record.application.service.RecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * Implementation of {@link RecordControllerApi}.
 * Handles incoming HTTP requests for medical record management and delegates to {@link RecordService}.
 *
 * @author ibosquet
 */
@RestController
@RequestMapping("/api/records")
@RequiredArgsConstructor
@Slf4j
public class RecordController implements RecordControllerApi {
    private final RecordService recordService;
    private final UserHelper userHelper;

    @Override
    @PostMapping("")
    public ResponseEntity<RecordViewDto> createRecord(
            @Valid @RequestBody RecordCreateDto createDto) {

        Long creatorUserId = userHelper.getAuthenticatedUserId();
        RecordViewDto createdRecord = recordService.createRecord(createDto, creatorUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdRecord);
    }

    @Override
    @GetMapping("")
    public ResponseEntity<Page<RecordViewDto>> findRecords( @RequestParam Long petId,
                                                            Pageable pageable) {

        Long requesterUserId = userHelper.getAuthenticatedUserId();
        Page<RecordViewDto> recordPage = recordService.findRecordsByPetId(petId, requesterUserId, pageable);
        return ResponseEntity.ok(recordPage);
    }

    @Override
    @GetMapping("/{recordId}")
    public ResponseEntity<RecordViewDto> findRecordById(
            @PathVariable Long recordId) {

        Long requesterUserId = userHelper.getAuthenticatedUserId();
        RecordViewDto recordDto = recordService.findRecordById(recordId, requesterUserId);
        return ResponseEntity.ok(recordDto);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @PutMapping("/{recordId}")
    public ResponseEntity<RecordViewDto> updateUnsignedRecord(
            @PathVariable Long recordId,
            @Valid @RequestBody RecordUpdateDto updateDto) {
        Long requesterUserId = userHelper.getAuthenticatedUserId();
        log.info("Received request to update record ID {} by User ID {}", recordId, requesterUserId);
        RecordViewDto updatedRecord = recordService.updateUnsignedRecord(recordId, updateDto, requesterUserId);
        return ResponseEntity.ok(updatedRecord);
    }

    @Override
    @DeleteMapping("/{recordId}")
    public ResponseEntity<Void> deleteUnsignedRecord(
            @PathVariable Long recordId) {

        Long requesterUserId = userHelper.getAuthenticatedUserId();
        recordService.deleteRecord(recordId, requesterUserId);
        return ResponseEntity.noContent().build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @PostMapping("/{petId}/temporary-access")
    public ResponseEntity<TemporaryAccessTokenDto> generateTemporaryAccessToken(
            @PathVariable Long petId,
            @Valid @RequestBody TemporaryAccessRequestDto requestDto) {
        Long requesterUserId = userHelper.getAuthenticatedUserId();
        log.info("Received request for temporary access token for Pet ID {} by User ID {}, duration: {}",
                petId, requesterUserId, requestDto.durationString());
        TemporaryAccessTokenDto tokenDto = recordService.generateTemporaryAccessToken(petId, requestDto, requesterUserId);
        return ResponseEntity.ok(tokenDto);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @GetMapping("/verify-temporary-access")
    public ResponseEntity<List<RecordViewDto>> getRecordsByTemporaryToken(@RequestParam String token) {
        log.info("Received request to verify temporary access token.");
        List<RecordViewDto> records = recordService.findRecordsByTemporaryAccessToken(token);
        return ResponseEntity.ok(records);
    }

    @Override
    @GetMapping("/clinic/{clinicId}/created-by")
    public ResponseEntity<Page<RecordViewDto>> getRecordsCreatedByClinic(
            @PathVariable Long clinicId,
            Pageable pageable) {
        Long requesterUserId = userHelper.getAuthenticatedUserId();
        Page<RecordViewDto> recordPage = recordService.findRecordsCreatedByClinic(clinicId, requesterUserId, pageable);
        return ResponseEntity.ok(recordPage);
    }
}
