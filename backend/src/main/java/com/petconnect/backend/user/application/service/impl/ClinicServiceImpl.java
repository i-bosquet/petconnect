package com.petconnect.backend.user.application.service.impl;

import com.petconnect.backend.common.helper.AuthorizationHelper;
import com.petconnect.backend.common.helper.EntityFinderHelper;
import com.petconnect.backend.common.helper.Utils;
import com.petconnect.backend.common.service.EmailService;
import com.petconnect.backend.exception.EntityNotFoundException;
import com.petconnect.backend.user.application.dto.ClinicDto;
import com.petconnect.backend.user.application.dto.ClinicUpdateDto;
import com.petconnect.backend.user.application.dto.VetSummaryDto;
import com.petconnect.backend.user.application.event.ClinicKeysChangedEvent;
import com.petconnect.backend.user.application.mapper.ClinicMapper;
import com.petconnect.backend.user.application.mapper.UserMapper;
import com.petconnect.backend.user.application.service.ClinicService;
import com.petconnect.backend.user.domain.model.*;
import com.petconnect.backend.user.domain.repository.ClinicRepository;
import com.petconnect.backend.user.domain.repository.ClinicStaffRepository;
import com.petconnect.backend.user.port.spi.ClinicEventPublisherPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.Nullable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.criteria.Predicate;
import org.springframework.util.StringUtils;
import com.petconnect.backend.common.service.KeyStorageService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.io.IOException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Implementation of the {@link ClinicService}  interface.
 * Handles the business logic for retrieving and updating Clinic entities.
 * Use ClinicRepository for data access and ClinicMapper for DTO conversion.
 * Operations are transactional.
 *
 * @author ibosquet
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ClinicServiceImpl implements ClinicService {

    private final ClinicRepository clinicRepository;
    private final ClinicStaffRepository clinicStaffRepository;
    private final ClinicMapper clinicMapper;
    private final UserMapper userMapper;
    private final EntityFinderHelper entityFinderHelper;
    private final KeyStorageService keyStorageService;
    private final AuthorizationHelper authorizationHelper;
    private final EmailService emailService;
    private final ClinicEventPublisherPort clinicEventPublisher;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public Page<ClinicDto> findClinics(String name, String city, String country, Pageable pageable) {
        // Create a specification based on the filter criteria
        Specification<Clinic> spec = filterBy(name, city, country);

        // Execute the query with the specification and pagination
        Page<Clinic> clinicPage = clinicRepository.findAll(spec, pageable);

        // Map the Page<Clinic> to Page<ClinicDto>
        return clinicPage.map(clinicMapper::toDto);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public ClinicDto findClinicById(Long id) {
        Clinic clinic = clinicRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Clinic not found with id: " + id));
        return clinicMapper.toDto(clinic);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public ClinicDto updateClinic(Long id, ClinicUpdateDto clinicUpdateDTO,
                                  @Nullable MultipartFile newPublicKeyFile,
                                  @Nullable MultipartFile newEncryptedPrivateKeyFile,
                                  Long updatingAdminId) {

        ClinicStaff adminStaff = entityFinderHelper.findAdminStaffOrFail(updatingAdminId, "update clinic information");
        Clinic existingClinic = entityFinderHelper.findClinicOrFail(id);
        if (!adminStaff.getClinic().getId().equals(existingClinic.getId())) {
            throw new AccessDeniedException("Admin user " + updatingAdminId + " is not authorized to update clinic " + id + ".");
        }

        String oldPublicKeyPath = existingClinic.getPublicKey();
        String oldPrivateKeyPath = existingClinic.getPrivateKey();
        boolean publicKeyChanged = false;
        boolean privateKeyChanged = false;

        if (newPublicKeyFile != null && !newPublicKeyFile.isEmpty()) {
            try {
                String desiredFilenameBase = "clinic_" + id + "_pub";
                String newPath = keyStorageService.storePublicKey(newPublicKeyFile, "clinics", desiredFilenameBase);
                if (!Objects.equals(oldPublicKeyPath, newPath)) {
                    existingClinic.setPublicKey(newPath);
                    publicKeyChanged = true;
                }
            } catch (IOException | IllegalArgumentException e) {
                log.error("Failed to store new public key file for clinic {}: {}", id, e.getMessage(), e);
                throw new RuntimeException("Failed to store new public key file: " + e.getMessage(), e);
            }
        }

        if (newEncryptedPrivateKeyFile != null && !newEncryptedPrivateKeyFile.isEmpty()) {
            try {
                String desiredFilenameBase = "clinic_" + id + "_priv";
                String newPath = keyStorageService.storeEncryptedPrivateKey(newEncryptedPrivateKeyFile, "clinics", desiredFilenameBase);
                if (!Objects.equals(oldPrivateKeyPath, newPath)) {
                    existingClinic.setPrivateKey(newPath);
                    privateKeyChanged = true;
                }
            } catch (IOException | IllegalArgumentException e) {
                log.error("Failed to store new encrypted private key file for clinic {}: {}", id, e.getMessage(), e);
                throw new RuntimeException("Failed to store new encrypted private key file: " + e.getMessage(), e);
            }
        }

        boolean otherFieldsChanged = applyClinicDtoUpdates(clinicUpdateDTO, existingClinic);

        if (otherFieldsChanged || publicKeyChanged || privateKeyChanged) {
            log.info("Changes detected (other: {}, publicKey: {}, privateKey: {}), saving Clinic {}",
                    otherFieldsChanged, publicKeyChanged, privateKeyChanged, id);
            Clinic savedClinic = clinicRepository.save(existingClinic);

            if (publicKeyChanged && StringUtils.hasText(oldPublicKeyPath) && !oldPublicKeyPath.equals(savedClinic.getPublicKey())) {
                log.info("Attempting to delete old public key: {}", oldPublicKeyPath);
                keyStorageService.deleteKey(oldPublicKeyPath);
            }
            if (privateKeyChanged && StringUtils.hasText(oldPrivateKeyPath) && !oldPrivateKeyPath.equals(savedClinic.getPrivateKey())) {
                log.info("Attempting to delete old private key: {}", oldPrivateKeyPath);
                keyStorageService.deleteKey(oldPrivateKeyPath);
            }

             if (publicKeyChanged || privateKeyChanged) {
                String adminEmail = adminStaff.getEmail();
                Long adminId = adminStaff.getId();
                String clinicName = savedClinic.getName();
                emailService.sendClinicKeysChangedNotification(adminEmail,clinicName);
                clinicEventPublisher.publishClinicKeysChangedEvent(
                     new ClinicKeysChangedEvent(savedClinic.getId(), adminId, LocalDateTime.now(), publicKeyChanged, privateKeyChanged)
                 );
                 log.info("ClinicKeysChangedEvent published for clinic {}, admin {}.", clinicName, adminEmail);
             }

            return clinicMapper.toDto(savedClinic);
        } else {
            log.info("No effective changes detected for Clinic {}, update skipped.", id);
            return clinicMapper.toDto(existingClinic);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public List<Country> getDistinctClinicCountries() {
        return clinicRepository.findDistinctCountries();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public Resource getClinicPublicKeyResource(Long clinicId, Long requesterUserId) throws IOException {
        log.info("User {} requesting download for public key of clinic {}", requesterUserId, clinicId);

        // Authorization: Verify that the user is clinic staff
        authorizationHelper.verifyClinicStaffAccess(requesterUserId, clinicId, "download public key for");

        // Get the file path from the Clinic entity
        Clinic clinic = entityFinderHelper.findClinicOrFail(clinicId);
        String relativePath = clinic.getPublicKey();
        if (!StringUtils.hasText(relativePath)) {
            log.error("Clinic {} has no public key path configured.", clinicId);
            throw new EntityNotFoundException("Public key file path not configured for clinic " + clinicId);
        }

        // Get the absolute path using KeyStorageService
        Path absolutePath = keyStorageService.getAbsolutePathForPublicKey(relativePath);
        log.debug("Attempting to load public key resource from: {}", absolutePath);

        // Create the Resource object
        Resource resource;
        try {
            resource = new UrlResource(absolutePath.toUri());
        } catch (MalformedURLException e) {
            log.error("MalformedURLException for path {}: {}", absolutePath, e.getMessage());
            throw new IOException("Could not create resource URL for the key file.", e);
        }

        // Verify that the file exists and is readable
        if (resource.exists() && resource.isReadable()) {
            return resource;
        } else {
            log.error("Public key file not found or not readable at path: {}", absolutePath);
            throw new FileNotFoundException("Public key file not found for clinic " + clinicId + " at path " + relativePath);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public List<VetSummaryDto> findActiveVetsForSelectionByClinicId(Long clinicId) {
        entityFinderHelper.findClinicOrFail(clinicId);
        List<ClinicStaff> activeStaff = clinicStaffRepository.findByClinicIdAndIsActive(clinicId, true);

        List<Vet> activeVets = activeStaff.stream()
                .filter(Vet.class::isInstance)
                .map(Vet.class::cast)
                .toList();

        log.debug("Found {} active vets for selection in clinic ID {}", activeVets.size(), clinicId);
        if (activeVets.isEmpty()) {
            return Collections.emptyList();
        }
        List<VetSummaryDto> list = new ArrayList<>();
        for (Vet activeVet : activeVets) {
            VetSummaryDto vetSummaryDto = userMapper.toVetSummaryDto(activeVet);
            list.add(vetSummaryDto);
        }
        return list;
    }

    /**
     * Creates a Specification<Clinic> based on optional filter criteria.
     * Filters are combined using AND logic. String comparisons are case-insensitive and use LIKE '%value%'.
     *
     * @param name    Optional name fragment to filter by.
     * @param city    Optional city name to filter by.
     * @param country Optional country name to filter by.
     * @return A Specification<Clinic> representing the combined filter criteria.
     */
    public static Specification<Clinic> filterBy(String name, String city, String country) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Add predicate for name if provided
            if (StringUtils.hasText(name)) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), "%" + name.toLowerCase() + "%"));
            }

            // Add predicate for city if provided
            if (StringUtils.hasText(city)) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("city")), "%" + city.toLowerCase() + "%"));
            }

            // Add predicate for country if provided
            if (StringUtils.hasText(country)) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("country")), "%" + country.toLowerCase() + "%"));
            }

            // Combine all predicates with AND
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Updates the properties of the given Clinic object using the data from the provided ClinicUpdateDto.
     * This method checks if fields in the clinic need to be updated based on the values in the dto
     * and performs the updates if necessary.
     *
     * @param dto the ClinicUpdateDto containing the new values for the clinic's fields
     * @param clinic the Clinic objects to be updated
     * @return true if any field in the Clinic object was updated; false otherwise
     */
    private boolean applyClinicDtoUpdates(ClinicUpdateDto dto, Clinic clinic) {
        boolean changed = false;
        changed |= Utils.updateStringFieldIfChanged(clinic, dto.name(),    clinic::getName, Clinic::setName,    "name");
        changed |= Utils.updateStringFieldIfChanged(clinic, dto.address(), clinic::getAddress, Clinic::setAddress, "address");
        changed |= Utils.updateStringFieldIfChanged(clinic, dto.city(),    clinic::getCity, Clinic::setCity,    "city");
        changed |= Utils.updateFieldIfChanged(clinic,       dto.country(), clinic::getCountry, Clinic::setCountry, "country");
        changed |= Utils.updateStringFieldIfChanged(clinic, dto.phone(),   clinic::getPhone, Clinic::setPhone,   "phone");
        return changed;
    }
}
