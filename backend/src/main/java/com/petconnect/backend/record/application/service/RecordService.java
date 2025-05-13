package com.petconnect.backend.record.application.service;

import com.petconnect.backend.exception.RecordImmutableException;
import com.petconnect.backend.record.application.dto.*;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


/**
 * Service interface for managing medical records ({@link Record}) for pets.
 * Defines operations for creating, retrieving, and potentially deleting records.
 * Implementations must handle authorization checks (e.g., ensuring owner/staff access)
 * and business logic (e.g., handling vaccine details, signing records).
 *
 * @author ibosquet
 */
public interface RecordService {
    /**
     * Creates a new medical record for a specific pet.
     * Can be called by the pet's owner or authorized clinic staff.
     * If the type is VACCINE, vaccine details from the DTO must be included and persisted.
     * If created by a Vet, allows for optional immediate signing.
     * @param createDto     The DTO containing the record details (type, description, optional vaccine).
     * @param creatorUserId The ID of the User (Owner, Vet, Admin) creating the record.
     * @return The DTO representation of the newly created record.
     * @throws com.petconnect.backend.exception.EntityNotFoundException if the pet or creator user is not found,
     *                                                                  or if vaccine details are provided, but breed is invalid for a vaccine type.
     * @throws org.springframework.security.access.AccessDeniedException  if the creator is not authorized to create a record for this pet.
     * @throws IllegalArgumentException if the type is VACCINE, but vaccine details are missing in the DTO, or vice versa.
     * @throws IllegalStateException if signing is requested but the creator is not a Vet.
     */
    RecordViewDto createRecord( RecordCreateDto createDto, Long creatorUserId);

    /**
     * Retrieves a paginated list of all medical records for a specific pet.
     * Requires the requester to be the pet's owner or authorized clinic staff.
     * Records are typically ordered by the creation date descending.
     *
     * @param petId           The ID of the pet whose records are to be retrieved.
     * @param requesterUserId The ID of the user requesting the records (for authorization).
     * @param pageable        Pagination and sorting information.
     * @return A Page containing {@link RecordViewDto} objects.
     * @throws com.petconnect.backend.exception.EntityNotFoundException if the pet is not found.
     * @throws org.springframework.security.access.AccessDeniedException  if the requester is not authorized to view records for this pet.
     */
    Page<RecordViewDto> findRecordsByPetId(Long petId, Long requesterUserId, Pageable pageable);

    /**
     * Retrieves a specific medical record by its ID.
     * Requires the requester to be the pet's owner or authorized clinic staff.
     *
     * @param recordId        The ID of the record to retrieve.
     * @param requesterUserId The ID of the user requesting the record (for authorization).
     * @return The {@link RecordViewDto} of the found record.
     * @throws com.petconnect.backend.exception.EntityNotFoundException if the record is not found.
     * @throws org.springframework.security.access.AccessDeniedException  if the requester is not authorized to view this record.
     */
    RecordViewDto findRecordById(Long recordId, Long requesterUserId);

    /**
     * Updates an existing *unsigned* medical record.
     * Allows updating the type (excluding VACCINE) and/or description.
     * Requires the requester to be the original creator (Owner/Staff) or an authorized Admin
     * if the record was created by clinic staff.
     *
     * @param recordId        The ID of the unsigned record to update.
     * @param updateDto       The DTO containing the fields to update (type, description).
     * @param requesterUserId The ID of the user attempting the update.
     * @return The DTO representation of the updated record.
     * @throws com.petconnect.backend.exception.EntityNotFoundException if the record or requester is not found.
     * @throws org.springframework.security.access.AccessDeniedException  if the requester is not authorized to update this record.
     * @throws IllegalStateException if the record is signed or if attempting to change the type to/from VACCINE.
     * @throws IllegalArgumentException if the new type is invalid.
     */
    RecordViewDto updateUnsignedRecord(Long recordId, RecordUpdateDto updateDto, Long requesterUserId);

    /**
     * Deletes a medical record based on its ID and the requester's authorization.
     * - Unsigned records can be deleted by their creator or an authorized Admin.
     * - Signed records can ONLY be deleted by the signing Vet IF the record is NOT immutable.
     *
     * @param recordId        The ID of the record to delete.
     * @param requesterUserId The ID of the user attempting the deletion.
     * @throws EntityNotFoundException if the record or requester user is not found.
     * @throws RecordImmutableException if attempting to delete a signed record linked to a certificate.
     * @throws AccessDeniedException if the requester is not authorized to delete the record.
     */
    void deleteRecord(Long recordId, Long requesterUserId);

    /**
     * Generates a temporary access token (JWT) allowing read-only access
     * to a specific pet's signed medical records for a limited duration.
     * Requires the requester to be the owner of the pet.
     *
     * @param petId           The ID of the pet whose records access is requested.
     * @param requestDto      DTO containing the requested duration.
     * @param requesterUserId The ID of the user making the request (must be the owner).
     * @return A DTO containing the generated temporary access token.
     * @throws EntityNotFoundException if the pet is not found.
     * @throws AccessDeniedException  if the requester is not the owner of the pet.
     * @throws IllegalArgumentException if the duration string in the DTO is invalid.
     */
    TemporaryAccessTokenDto generateTemporaryAccessToken(Long petId, TemporaryAccessRequestDto requestDto, Long requesterUserId);

    /**
     * Retrieves a list of medical records linked to a temporary access token.
     * The token provides limited, read-only access to signed medical records.
     *
     * @param tokenValue The value of the temporary access token used for authorization.
     * @return A list of {@link RecordViewDto} objects representing the medical records accessible with the provided token.
     */
    @Transactional(readOnly = true)
    List<RecordViewDto> findRecordsByTemporaryAccessToken(String tokenValue);

    /**
     * Retrieves a paginated list of all medical records created by staff within a specific clinic.
     * This serves as a historical view for the clinic.
     *
     * @param clinicId The ID of the clinic.
     * @param requesterUserId The ID of the user (clinic staff) requesting the records.
     * @param pageable Pagination information.
     * @return A Page of {@link RecordViewDto}.
     * @throws AccessDeniedException if the requester is not authorized, staff of the clinic.
     */
    Page<RecordViewDto> findRecordsCreatedByClinic(Long clinicId, Long requesterUserId, Pageable pageable);
}
