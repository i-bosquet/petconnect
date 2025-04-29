package com.petconnect.backend.common.helper;

import com.petconnect.backend.certificate.domain.repository.CertificateRepository;
import com.petconnect.backend.exception.*;
import com.petconnect.backend.record.application.dto.RecordCreateDto;
import com.petconnect.backend.record.domain.model.RecordType;
import com.petconnect.backend.record.domain.model.Record;
import com.petconnect.backend.record.domain.model.Vaccine;
import com.petconnect.backend.record.domain.repository.RecordRepository;
import com.petconnect.backend.user.domain.model.*;
import com.petconnect.backend.user.domain.repository.UserRepository;
import com.petconnect.backend.user.domain.repository.VetRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Helper component containing common validation logic reused across services.
 * Provides methods to check data consistency, uniqueness constraints, etc.
 *
 * @author ibosquet
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ValidateHelper {

    private final UserRepository userRepository;
    private final VetRepository vetRepository;
    private final RecordRepository recordRepository;
    private final CertificateRepository certificateRepository;


    /**
     * Validates that the provided RoleEnum is either VET or ADMIN for clinic staff assignment.
     * @param role The RoleEnum to validate.
     * @throws IllegalArgumentException if the role is not VET or ADMIN.
     */
    public void validateStaffRole(RoleEnum role) {
        if (role != RoleEnum.VET && role != RoleEnum.ADMIN) {
            throw new IllegalArgumentException("Invalid role specified for clinic staff. Must be VET or ADMIN.");
        }
    }

    /**
     * Validates email and username uniqueness during new user/staff creation.
     * Requires a read-only transaction to query repositories.
     * @param email The email to check.
     * @param username The username to check.
     * @throws EmailAlreadyExistsException if email exists.
     * @throws UsernameAlreadyExistsException if a username exists.
     */
    @Transactional(readOnly = true)
    public void validateNewStaffUniqueness(String email, String username) {
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException(email);
        }
        if (userRepository.existsByUsername(username)) {
            throw new UsernameAlreadyExistsException(username);
        }
    }

    /**
     * Validates a Veterinarian's license number for presence (if required) and uniqueness
     * during creation. Requires a read-only transaction.
     *
     * @param licenseNumber The license number to validate.
     * @throws IllegalArgumentException if the license number is blank/null when required.
     * @throws LicenseNumberAlreadyExistsException if the license number is already in use by another Vet.
     */
    @Transactional(readOnly = true)
    public void validateVetLicenseNumber(String licenseNumber) {
        if (!StringUtils.hasText(licenseNumber)) {
            throw new IllegalArgumentException("License number is required for VET role.");
        }
        boolean exists = vetRepository.existsByLicenseNumber(licenseNumber);
        if (exists) {
            throw new LicenseNumberAlreadyExistsException(licenseNumber);
        }
    }

    /**
     * Validates a Veterinarian's public key for presence and uniqueness.
     * Can optionally exclude a specific Vet ID during uniqueness check (for updates).
     * Requires a read-only transaction.
     *
     * @param vetPublicKey The public key to validate.
     * @param vetIdToExclude The ID of the Vet being updated (null if creating a new Vet).
     * @throws IllegalArgumentException if the public key is blank/null when required.
     * @throws VetPublicKeyAlreadyExistsException if a public key is already in use by another Vet.
     */
    @Transactional(readOnly = true)
    public void validateVetPublicKey(String vetPublicKey, Long vetIdToExclude) {
        if (!StringUtils.hasText(vetPublicKey)) {
            throw new IllegalArgumentException("Veterinarian public key is required for VET role.");
        }
        log.debug("Validating public key uniqueness: key={}, excludingId={}", vetPublicKey, vetIdToExclude);
        // Check public key uniqueness
        boolean keyExists;
        if (vetIdToExclude == null) {
            keyExists = vetRepository.existsByVetPublicKey(vetPublicKey);
        } else {
            keyExists = vetRepository.existsByVetPublicKeyAndIdNot(vetPublicKey, vetIdToExclude);
        }

        log.debug("Uniqueness check result for public key: {}", keyExists);
        if (keyExists) {
            throw new VetPublicKeyAlreadyExistsException();
        }
    }

    /**
     * Validates the consistency of the RecordCreateDto, ensuring vaccine details
     * are provided if and only if the record type is VACCINE.
     * @param dto The RecordCreateDto to validate.
     * @throws IllegalArgumentException if vaccine details are missing for type VACCINE,
     *                                 or provided for other types.
     */
    public void validateRecordCreationDto(RecordCreateDto dto) {
        if (dto.type() == RecordType.VACCINE && dto.vaccine() == null) {
            throw new IllegalArgumentException("Vaccine details are required when record type is VACCINE.");
        }
        if (dto.type() != RecordType.VACCINE && dto.vaccine() != null) {
            throw new IllegalArgumentException("Vaccine details should only be provided when record type is VACCINE.");
        }
    }

    /**
     * Validates a Veterinarian's license number for uniqueness during an update operation,
     * excluding the Vet being updated from the uniqueness check.
     * Requires access to VetRepository.
     *
     * @param licenseNumber The new license number to validate. Must not be blank.
     * @param vetIdToExclude The ID of the Vet whose record should be excluded from the check.
     * @throws IllegalArgumentException if the license number is blank/null.
     * @throws LicenseNumberAlreadyExistsException if ANOTHER Vet already uses the license number.
     */
    public void validateVetLicenseUpdate(String licenseNumber, Long vetIdToExclude) {
        if (!StringUtils.hasText(licenseNumber)) {
            throw new IllegalArgumentException("License number cannot be blank during update.");
        }

        if (vetRepository.existsByLicenseNumberAndIdNot(licenseNumber, vetIdToExclude)) {
            throw new LicenseNumberAlreadyExistsException(licenseNumber);
        }
    }

    /**
     * Finds the most recent, signed, and still valid Rabies vaccine record for a given pet.
     * Validity is checked based on the vaccine's validity period and its creation date.
     *
     * @param petId The ID of the pet.
     * @return The valid Record entity for the Rabies vaccine.
     * @throws MissingRabiesVaccineException if no suitable Rabies vaccine record is found.
     */
    public Record findValidRabiesRecord(Long petId) {
        List<Record> potentialRabiesRecords = recordRepository.findAllSignedRabiesVaccinesDesc(petId);
        return potentialRabiesRecords.stream()
                .filter(this::isRabiesRecordValid)
                .findFirst()
                .orElseThrow(() -> new MissingRabiesVaccineException(petId));
    }

    /**
     * Checks if a given Rabies vaccine record is still valid based on its creation date and validity period.
     *
     * @param recordVaccine The VACCINE Record to check (assumed to be Rabies and signed).
     * @return true if the vaccine record is still valid, false otherwise.
     */
    private boolean isRabiesRecordValid(Record recordVaccine) {
        Vaccine vaccine = recordVaccine.getVaccine();
        LocalDateTime vaccinationDate = recordVaccine.getCreatedAt();
        if (vaccine == null || vaccine.getValidity() == null || vaccinationDate == null) {
            log.warn("Skipping rabies record {} due to missing vaccine details, validity, or creation date.", recordVaccine.getId());
            return false;
        }
        if (vaccine.getValidity() >= 0) {
            LocalDate expiryDate = vaccinationDate.toLocalDate().plusYears(vaccine.getValidity());
            return !LocalDate.now().isAfter(expiryDate);
        }
        return false;
    }

    /**
     * Validates the uniqueness constraints before generating a new certificate.
     * Checks if a certificate already exists for the source medical record or
     * if the proposed official certificate number is already in use.
     *
     * @param recordId          The ID of the source medical record (e.g., the valid rabies vaccine record).
     * @param certificateNumber The proposed official certificate number.
     * @throws CertificateAlreadyExistsForRecordException if a certificate for the recordId already exists.
     * @throws CertificateNumberAlreadyExistsException if the certificateNumber is already in use.
     */
    public void validateCertificateUniqueness(Long recordId, String certificateNumber) {
        if (certificateRepository.existsByMedicalRecordId(recordId)) {
            throw new CertificateAlreadyExistsForRecordException(recordId);
        }
        if (certificateRepository.findByCertificateNumber(certificateNumber).isPresent()) {
            throw new CertificateNumberAlreadyExistsException(certificateNumber);
        }
    }
}
