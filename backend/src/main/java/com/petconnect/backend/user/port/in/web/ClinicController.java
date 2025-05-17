package com.petconnect.backend.user.port.in.web;

import com.petconnect.backend.user.application.dto.ClinicDto;
import com.petconnect.backend.user.application.dto.ClinicStaffProfileDto;
import com.petconnect.backend.user.application.dto.ClinicUpdateDto;
import com.petconnect.backend.user.application.dto.VetSummaryDto;
import com.petconnect.backend.user.application.service.ClinicService;
import com.petconnect.backend.user.application.service.ClinicStaffService;
import com.petconnect.backend.common.helper.UserHelper;
import com.petconnect.backend.user.domain.model.Country;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

/**
 * Implementation of {@link ClinicControllerApi}.
 * Handles incoming HTTP requests for clinic operations and delegates to services.
 *
 * @author ibosquet
 */
@RestController
@RequestMapping("/api/clinics")
@RequiredArgsConstructor
@Slf4j
public class ClinicController implements ClinicControllerApi {

    private final ClinicService clinicService;
    private final ClinicStaffService clinicStaffService;
    private final UserHelper userServiceHelper;

    /**
     * {@inheritDoc}
     */
    @Override
    @GetMapping
    public ResponseEntity<Page<ClinicDto>> getClinics(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String country,
            @PageableDefault(sort = "name") Pageable pageable) {
        Page<ClinicDto> clinicsPage = clinicService.findClinics(name, city, country, pageable);
        return ResponseEntity.ok(clinicsPage);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @GetMapping("/{id}")
    public ResponseEntity<ClinicDto> getClinicById(@PathVariable Long id) {
        ClinicDto clinic = clinicService.findClinicById(id);
        return ResponseEntity.ok(clinic);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ClinicDto> updateClinic(
            @PathVariable Long id,
            @RequestPart("dto") @Valid ClinicUpdateDto clinicUpdateDTO,
            @RequestPart(value = "publicKeyFile", required = false) @Nullable MultipartFile publicKeyFile,
            @RequestPart(value = "privateKeyFile", required = false) @Nullable MultipartFile privateKeyFile) {
        Long currentAdminId = userServiceHelper.getAuthenticatedUserId();
        ClinicDto updatedClinic = clinicService.updateClinic(id, clinicUpdateDTO, publicKeyFile, privateKeyFile, currentAdminId);
        return ResponseEntity.ok(updatedClinic);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @GetMapping("/{clinicId}/staff/all")
    public ResponseEntity<List<ClinicStaffProfileDto>> getAllStaffByClinic(@PathVariable Long clinicId) {
        Long requesterUserId = userServiceHelper.getAuthenticatedUserId();
        List<ClinicStaffProfileDto> staffList = clinicStaffService.findAllStaffByClinic(clinicId, requesterUserId);
        return ResponseEntity.ok(staffList);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @GetMapping("/{clinicId}/staff/active")
    public ResponseEntity<List<ClinicStaffProfileDto>> getActiveStaffByClinic(@PathVariable Long clinicId) {
        Long requesterUserId = userServiceHelper.getAuthenticatedUserId();
        List<ClinicStaffProfileDto> staffList = clinicStaffService.findActiveStaffByClinic(clinicId, requesterUserId);
        return ResponseEntity.ok(staffList);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @GetMapping("/countries")
    public ResponseEntity<List<Country>> getDistinctCountries() {
        List<Country> countries = clinicService.getDistinctClinicCountries();
        return ResponseEntity.ok(countries);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @GetMapping("/{clinicId}/public-key/download")
    public ResponseEntity<Resource> downloadClinicPublicKey(@PathVariable Long clinicId) {
        Long requesterUserId = userServiceHelper.getAuthenticatedUserId();
        try {
            Resource resource = clinicService.getClinicPublicKeyResource(clinicId, requesterUserId);

            String contentType = "application/octet-stream";
            String filename = resource.getFilename();

            if (filename == null) {
                filename = "clinic_" + clinicId + "_pub.pem";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .body(resource);

        } catch (FileNotFoundException e) {
            log.warn("Public key file not found for clinic {}: {}", clinicId, e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (IOException e) {
            log.error("Error reading public key file for clinic {}: {}", clinicId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @GetMapping("/{clinicId}/vets-for-selection")
    public ResponseEntity<List<VetSummaryDto>> getActiveVetsForSelectionByClinic(@PathVariable Long clinicId) {
        List<VetSummaryDto> vets = clinicService.findActiveVetsForSelectionByClinicId(clinicId);
        return ResponseEntity.ok(vets);
    }
}
