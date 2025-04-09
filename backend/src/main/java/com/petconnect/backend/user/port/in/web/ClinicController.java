package com.petconnect.backend.user.port.in.web;

import com.petconnect.backend.user.application.dto.ClinicDto;
import com.petconnect.backend.user.application.dto.ClinicStaffProfileDto;
import com.petconnect.backend.user.application.dto.ClinicUpdateDto;
import com.petconnect.backend.user.application.service.ClinicService;
import com.petconnect.backend.user.application.service.ClinicStaffService;
import com.petconnect.backend.user.application.service.helper.UserServiceHelper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
public class ClinicController implements ClinicControllerApi {

    private final ClinicService clinicService;
    private final ClinicStaffService clinicStaffService;
    private final UserServiceHelper userServiceHelper;

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
    @PutMapping("/{id}")
    public ResponseEntity<ClinicDto> updateClinic(
            @PathVariable Long id,
            @Valid @RequestBody ClinicUpdateDto clinicUpdateDTO) {
        Long currentAdminId = userServiceHelper.getAuthenticatedUserId();
        ClinicDto updatedClinic = clinicService.updateClinic(id, clinicUpdateDTO, currentAdminId);
        return ResponseEntity.ok(updatedClinic);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @GetMapping("/{clinicId}/staff/all")
    public ResponseEntity<List<ClinicStaffProfileDto>> getAllStaffByClinic(@PathVariable Long clinicId) {
        Long requesterUserId = userServiceHelper.getAuthenticatedUserId();
        // Authorization should ideally happen within the service method
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
        // Authorization should happen within the service method
        List<ClinicStaffProfileDto> staffList = clinicStaffService.findActiveStaffByClinic(clinicId, requesterUserId);
        return ResponseEntity.ok(staffList);
    }
}
