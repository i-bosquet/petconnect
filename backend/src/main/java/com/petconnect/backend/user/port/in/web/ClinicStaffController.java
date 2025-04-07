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
 * REST Controller for managing Clinic Staff (Vets and Admins).
 * These endpoints are typically restricted to users with the ADMIN role
 * for the relevant clinic.
 * Base path is "/api/staff". Endpoints for listing staff might be under "/api/clinics".
 *
 * @author ibosquet
 */
@RestController
@RequestMapping("/api/staff")
@RequiredArgsConstructor
public class ClinicStaffController {

    private final ClinicStaffService clinicStaffService;
    private final UserServiceHelper userAuthenticationHelper;

    /**
     * Handles POST requests to create a new Clinic Staff member (Vet or Admin).
     * Requires ADMIN role for the clinic specified in the DTO.
     * Validates the incoming creation data.
     *
     * @param creationDTO The DTO containing the details of the staff to create.
     * @return A ResponseEntity containing the profile DTO of the newly created staff member
     *         and HTTP status 201 (Created).
     */
    @PostMapping("") // Endpoint for creating staff
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ClinicStaffProfileDto> createClinicStaff(@Valid @RequestBody ClinicStaffCreationDto creationDTO) {
        // --- Get the ID of the currently authenticated admin
        Long currentAdminId = userAuthenticationHelper.getAuthenticatedUserId();
        ClinicStaffProfileDto createdStaff = clinicStaffService.createClinicStaff(creationDTO, currentAdminId);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdStaff);
    }

    @PutMapping("/{staffId}")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ClinicStaffProfileDto> updateClinicStaff(
            @PathVariable Long staffId,
            @Valid @RequestBody ClinicStaffUpdateDto updateDTO) {
        // --- Get the ID of the currently authenticated admin
        Long currentAdminId = userAuthenticationHelper.getAuthenticatedUserId();
        ClinicStaffProfileDto updatedStaff = clinicStaffService.updateClinicStaff(staffId, updateDTO, currentAdminId);
        return ResponseEntity.ok(updatedStaff);
    }

    /**
     * Handles PUT requests to activate a Clinic Staff member's account.
     * Requires ADMIN role for the clinic of the staff member being activated.
     *
     * @param staffId The ID of the staff member to activate.
     * @return A ResponseEntity containing the profile DTO of the activated staff member.
     */
    @PutMapping("/{staffId}/activate")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ClinicStaffProfileDto> activateStaff(@PathVariable Long staffId) {
        // --- Get the ID of the currently authenticated admin
        Long currentAdminId = userAuthenticationHelper.getAuthenticatedUserId();
        ClinicStaffProfileDto activatedStaff = clinicStaffService.activateStaff(staffId, currentAdminId);
        return ResponseEntity.ok(activatedStaff);
    }

    /**
     * Handles PUT requests to deactivate a Clinic Staff member's account.
     * Requires ADMIN role for the clinic of the staff member being deactivated.
     *
     * @param staffId The ID of the staff member to deactivate.
     * @return A ResponseEntity containing the profile DTO of the deactivated staff member.
     */
    @PutMapping("/{staffId}/deactivate")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ClinicStaffProfileDto> deactivateStaff(@PathVariable Long staffId) {
        // --- Get the ID of the currently authenticated admin
        Long currentAdminId = userAuthenticationHelper.getAuthenticatedUserId();
        ClinicStaffProfileDto deactivatedStaff = clinicStaffService.deactivateStaff(staffId, currentAdminId);
        return ResponseEntity.ok(deactivatedStaff);
    }
}
