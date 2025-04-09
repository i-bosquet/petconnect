package com.petconnect.backend.user.port.in.web;

import com.petconnect.backend.user.application.dto.ClinicStaffCreationDto;
import com.petconnect.backend.user.application.dto.ClinicStaffProfileDto;
import com.petconnect.backend.user.application.dto.ClinicStaffUpdateDto;
import com.petconnect.backend.user.application.service.ClinicStaffService;
import com.petconnect.backend.user.application.service.helper.UserServiceHelper;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Implementation of {@link ClinicStaffControllerApi}.
 * Handles incoming HTTP requests for clinic staff management and delegates to the {@link ClinicStaffService}.
 *
 * @author ibosquet
 */
@RestController
@RequestMapping("/api/staff")
@RequiredArgsConstructor
public class ClinicStaffController implements ClinicStaffControllerApi {

    private final ClinicStaffService clinicStaffService;
    private final UserServiceHelper userAuthenticationHelper;

    /**
     * {@inheritDoc}
     */
    @Override
    @PostMapping("")
    public ResponseEntity<ClinicStaffProfileDto> createClinicStaff(@Valid @RequestBody ClinicStaffCreationDto creationDTO) {
        // --- Get the ID of the currently authenticated admin
        Long currentAdminId = userAuthenticationHelper.getAuthenticatedUserId();
        ClinicStaffProfileDto createdStaff = clinicStaffService.createClinicStaff(creationDTO, currentAdminId);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdStaff);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @PutMapping("/{staffId}")
    public ResponseEntity<ClinicStaffProfileDto> updateClinicStaff(
            @PathVariable Long staffId,
            @Valid @RequestBody ClinicStaffUpdateDto updateDTO) {
        // --- Get the ID of the currently authenticated admin
        Long currentAdminId = userAuthenticationHelper.getAuthenticatedUserId();
        ClinicStaffProfileDto updatedStaff = clinicStaffService.updateClinicStaff(staffId, updateDTO, currentAdminId);
        return ResponseEntity.ok(updatedStaff);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @PutMapping("/{staffId}/activate")
    public ResponseEntity<ClinicStaffProfileDto> activateStaff(@PathVariable Long staffId) {
        // --- Get the ID of the currently authenticated admin
        Long currentAdminId = userAuthenticationHelper.getAuthenticatedUserId();
        ClinicStaffProfileDto activatedStaff = clinicStaffService.activateStaff(staffId, currentAdminId);
        return ResponseEntity.ok(activatedStaff);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @PutMapping("/{staffId}/deactivate")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ClinicStaffProfileDto> deactivateStaff(@PathVariable Long staffId) {
        // --- Get the ID of the currently authenticated admin
        Long currentAdminId = userAuthenticationHelper.getAuthenticatedUserId();
        ClinicStaffProfileDto deactivatedStaff = clinicStaffService.deactivateStaff(staffId, currentAdminId);
        return ResponseEntity.ok(deactivatedStaff);
    }
}
