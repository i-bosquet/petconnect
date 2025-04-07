package com.petconnect.backend.user.port.in.web;

import com.petconnect.backend.user.application.dto.*;
import com.petconnect.backend.user.application.service.UserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*; // Standard web annotations

/**
 * REST Controller for handling general UserEntity related requests.
 * Exposes endpoints for retrieving user profile information.
 * Delegates business logic to the UserService.
 * Base path for all user endpoints is "/api/users".
 * Requires authentication for most endpoints (though currently permitted by SecurityConfig).
 *
 * @author ibosquet
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * Handles GET requests to retrieve the profile of the currently authenticated user.
     * The specific type of profile returned (Owner or ClinicStaff) depends on the user's role.
     * Requires authentication.
     *
     * @return A ResponseEntity containing the specific user profile DTO (OwnerProfileDto or ClinicStaffProfileDto)
     *         cast to Object, and HTTP status 200 (OK).
     *         Returns 404 or 500 based on exceptions handled by GlobalExceptionHandler.
     */
    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Object> getCurrentUserProfile() {
        // userService.getCurrentUserProfile() returns Object which holds the specific DTO
        Object userProfile = userService.getCurrentUserProfile();
        return ResponseEntity.ok(userProfile);
    }

    /**
     * Handles GET requests to retrieve a user profile by their unique ID.
     * Returns a generic profile DTO.
     * Requires appropriate authorization (e.g., Admin or if retrieving own profile - to be added later).
     *
     * @param id The ID of the user to retrieve.
     * @return A ResponseEntity containing the UserProfileDto and HTTP status 200 (OK) if found,
     *         or 404 (Not Found) if not found.
     */
    @GetMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<UserProfileDto> getUserById(@PathVariable Long id) {
        return userService.findUserById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Handles GET requests to retrieve a user profile by their unique email address.
     * Returns a generic profile DTO.
     * Requires appropriate authorization (e.g., Admin - to be added later).
     *
     * @param email The email address to search for, passed as a request parameter.
     * @return A ResponseEntity containing the UserProfileDto and HTTP status 200 (OK) if found,
     *         or 404 (Not Found) if not found.
     */
    @GetMapping("/by-email")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<UserProfileDto> getUserByEmail(@RequestParam String email) {
        return userService.findUserByEmail(email)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Handles PUT requests for the authenticated Owner to update their own profile.
     * Requires OWNER role (to be enforced later by Spring Security).
     * Validates the incoming update data.
     *
     * @param updateDTO The DTO containing fields to update for the owner profile.
     * @return A ResponseEntity containing the updated OwnerProfileDto and HTTP status 200 (OK).
     */
    @PutMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<OwnerProfileDto> updateCurrentOwnerProfile(@Valid @RequestBody OwnerProfileUpdateDto updateDTO) {
        OwnerProfileDto updatedProfile = userService.updateCurrentOwnerProfile(updateDTO);
        return ResponseEntity.ok(updatedProfile);
    }

    /**
     * Handles PUT requests for authenticated Clinic Staff (Vet/Admin) to update their own common profile info (username, avatar).
     * Requires VET or ADMIN role (to be enforced later by Spring Security).
     * Validates the incoming update data.
     *
     * @param updateDTO The DTO containing common user fields to update.
     * @return A ResponseEntity containing the updated ClinicStaffProfileDto and HTTP status 200 (OK).
     */
    @PutMapping("/me/staff")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ClinicStaffProfileDto> updateCurrentClinicStaffProfile(@Valid @RequestBody UserProfileUpdateDto updateDTO) {
        // Note: We use the generic UserProfileUpdateDto as input here
        ClinicStaffProfileDto updatedProfile = userService.updateCurrentClinicStaffProfile(updateDTO);
        return ResponseEntity.ok(updatedProfile);
    }
}
