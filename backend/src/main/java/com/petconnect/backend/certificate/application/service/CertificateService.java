package com.petconnect.backend.certificate.application.service;

import com.petconnect.backend.certificate.application.dto.CertificateGenerationRequestDto;
import com.petconnect.backend.certificate.application.dto.CertificateViewDto;

import java.util.List;

/**
 * Service interface defining operations for managing digital Certificates.
 * This contract outlines the core functionalities related to certificate
 * generation and retrieval within the application. Implementations are
 * responsible for handling business logic, validation, authorization,
 * and interactions with repositories and cryptographic services.
 *
 * @author ibosquet
 */
public interface CertificateService {

    /**
     * Generates a new digital certificate based on an eligible medical record.
     * Requires appropriate Vet authorization and validation of the source record and certificate number.
     * Handles payload creation, hashing, and signing.
     *
     * @param requestDto      DTO containing source record ID and certificate number.
     * @param generatingVetId ID of the authenticated Vet performing the generation.
     * @return DTO representing the newly generated certificate.
     * @throws com.petconnect.backend.exception.EntityNotFoundException if related entities (Record, Vet, Pet, Clinic) are not found.
     * @throws org.springframework.security.access.AccessDeniedException  if the Vet is not authorized.
     * @throws IllegalStateException if the Record is unsuitable or a certificate already exists.
     * @throws IllegalArgumentException if the certificate number is invalid or already exists.
     * @throws RuntimeException for cryptographic errors.
     */
    CertificateViewDto generateCertificate(CertificateGenerationRequestDto requestDto, Long generatingVetId);

    /**
     * Retrieves all certificates issued for a specific pet, ordered by creation date descending.
     * Requires authorization (Owner or associated Staff).
     *
     * @param petId           ID of the pet whose certificates are requested.
     * @param requesterUserId ID of the user making the request.
     * @return A list of {@link CertificateViewDto} objects for the pet.
     * @throws com.petconnect.backend.exception.EntityNotFoundException if the pet is not found.
     * @throws org.springframework.security.access.AccessDeniedException  if the requester is not authorized for this pet.
     */
    List<CertificateViewDto> findCertificatesByPet(Long petId, Long requesterUserId);

    /**
     * Retrieves the details of a specific certificate by its ID.
     * Requires authorization (Owner or associated Staff for the related pet).
     *
     * @param certificateId   ID of the certificate to retrieve.
     * @param requesterUserId ID of the user making the request.
     * @return The {@link CertificateViewDto} of the found certificate.
     * @throws com.petconnect.backend.exception.EntityNotFoundException if the certificate is not found.
     * @throws org.springframework.security.access.AccessDeniedException  if the requester is not authorized for the associated pet.
     */
    CertificateViewDto findCertificateById(Long certificateId, Long requesterUserId);

    /**
     * Generates and retrieves the Base45 encoded data string for a specific certificate,
     * suitable for embedding in a QR code.
     * Requires authorization check (Owner or associated Staff).
     *
     * @param certificateId   The ID of the certificate.
     * @param requesterUserId The ID of the user making the request.
     * @return The Base45 encoded string.
     * @throws com.petconnect.backend.exception.EntityNotFoundException if the certificate is not found.
     * @throws org.springframework.security.access.AccessDeniedException  if the requester is not authorized.
     * @throws RuntimeException if the QR data generation process fails.
     */
    String getQrDataForCertificate(Long certificateId, Long requesterUserId);

}
