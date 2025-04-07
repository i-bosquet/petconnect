package com.petconnect.backend.user.port.in.web;

import com.petconnect.backend.user.application.dto.ClinicDto;
import com.petconnect.backend.user.application.dto.ClinicStaffProfileDto;
import com.petconnect.backend.user.application.dto.ClinicUpdateDto;
import com.petconnect.backend.user.application.service.ClinicService;
import com.petconnect.backend.user.application.service.ClinicStaffService;
import com.petconnect.backend.user.application.service.helper.UserServiceHelper;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for handling Clinic related requests.
 * Exposes endpoints for retrieving and updating clinic information.
 * Delegates business logic to the ClinicService.
 * Base path for all clinic endpoints is "/api/clinics".
 *
 * @author ibosquet
 */
@RestController
@RequestMapping("/api/clinics")
@RequiredArgsConstructor
public class ClinicController {
    private final ClinicService clinicService;
    private final ClinicStaffService clinicStaffService;
    private final UserServiceHelper userServiceHelper;

    /**
     * Handles GET requests to retrieve clinics, supporting filtering and pagination.
     * Accessible publicly.
     *
     * @param name Optional filter by clinic name fragment.
     * @param city Optional filter by city name.
     * @param country Optional filter by country name.
     * @param pageable Pagination parameters (page, size, sort). Defaults defined by @PageableDefault.
     * @return A ResponseEntity containing a Page of ClinicDTOs and HTTP status 200 (OK).
     */
    @GetMapping
    public ResponseEntity<Page<ClinicDto>> getClinics(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String country,
            @PageableDefault(sort = "name") Pageable pageable) { // Example default: 10 per page, sort by name
        Page<ClinicDto> clinicsPage = clinicService.findClinics(name, city, country, pageable);
        return ResponseEntity.ok(clinicsPage);
    }

    /**
     * Handles GET requests to retrieve a specific clinic by its ID.
     *
     * @param id The ID of the clinic passed as a path variable.
     * @return A ResponseEntity containing the ClinicDto and HTTP status 200 (OK) if found.
     *         Returns HTTP status 404 (Not Found) if the clinic doesn't exist (handled by exception handler later).
     */
    @GetMapping("/{id}")
    public ResponseEntity<ClinicDto> getClinicById(@PathVariable Long id) {
        ClinicDto clinic = clinicService.findClinicById(id);
        return ResponseEntity.ok(clinic);
    }

    /**
     * Handles PUT requests to update an existing clinic.
     * Requires appropriate authorization (e.g., ADMIN role for the specific clinic - to be added later).
     * Validates the incoming ClinicUpdateDto.
     *
     * @param id The ID of the clinic to update, passed as a path variable.
     * @param clinicUpdateDTO The DTO containing updated clinic data, passed in the request body.
     *                       The @Valid annotation triggers validation based on constraints in the DTO.
     * @return A ResponseEntity containing the updated ClinicDto and HTTP status 200 (OK).
     *         Returns HTTP status 404 (Not Found) if the clinic doesn't exist.
     *         Returns HTTP status 400 (Bad Request) if validation fails (handled by exception handler later).
     */
    @PutMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ClinicDto> updateClinic(
            @PathVariable Long id,
            @Valid @RequestBody ClinicUpdateDto clinicUpdateDTO) {
        Long currentAdminId = userServiceHelper.getAuthenticatedUserId();
        ClinicDto updatedClinic = clinicService.updateClinic(id, clinicUpdateDTO, currentAdminId);
        return ResponseEntity.ok(updatedClinic);
    }

    /**
     * Handles GET requests to retrieve all staff members (active and inactive) for a specific clinic.
     * Requires authentication and authorization (Vet/Admin of the same clinic).
     *
     * @param clinicId The ID of the clinic whose staff should be retrieved.
     * @return A ResponseEntity containing a list of ClinicStaffProfileDTOs.
     */
    @GetMapping("/{clinicId}/staff/all")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<List<ClinicStaffProfileDto>> getAllStaffByClinic(@PathVariable Long clinicId) {
        Long requesterUserId = userServiceHelper.getAuthenticatedUserId();
        // Authorization should ideally happen within the service method
        List<ClinicStaffProfileDto> staffList = clinicStaffService.findAllStaffByClinic(clinicId, requesterUserId);
        return ResponseEntity.ok(staffList);
    }

    /**
     * Handles GET requests to retrieve only active staff members for a specific clinic.
     * Requires authentication and authorization (Vet/Admin of the same clinic).
     *
     * @param clinicId The ID of the clinic whose active staff should be retrieved.
     * @return A ResponseEntity containing a list of ClinicStaffProfileDTOs for active staff.
     */
    @GetMapping("/{clinicId}/staff/active")
    @SecurityRequirement(name = "bearerAuth") // Mark as protected
    public ResponseEntity<List<ClinicStaffProfileDto>> getActiveStaffByClinic(@PathVariable Long clinicId) {
        Long requesterUserId = userServiceHelper.getAuthenticatedUserId();
        // Authorization should happen within the service method
        List<ClinicStaffProfileDto> staffList = clinicStaffService.findActiveStaffByClinic(clinicId, requesterUserId);
        return ResponseEntity.ok(staffList);
    }
}
