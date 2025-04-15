package com.petconnect.backend.user.port.in.web;

import com.petconnect.backend.user.application.dto.*;
import com.petconnect.backend.user.application.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Implementation of {@link UserControllerApi}.
 * Handles incoming HTTP requests for user operations and delegates to {@link UserService}.
 *
 * @author ibosquet
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController implements UserControllerApi {

    private final UserService userService;

    /**
     * {@inheritDoc}
     */
    @Override
    @GetMapping("/me")
    public ResponseEntity<Object> getCurrentUserProfile() {
        Object userProfile = userService.getCurrentUserProfile();
        return ResponseEntity.ok(userProfile);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @GetMapping("/{id}")
    public ResponseEntity<UserProfileDto> getUserById(@PathVariable Long id) {
        return userService.findUserById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @GetMapping("/by-email")
    public ResponseEntity<UserProfileDto> getUserByEmail(@RequestParam String email) {
        return userService.findUserByEmail(email)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @PutMapping("/me")
    public ResponseEntity<OwnerProfileDto> updateCurrentOwnerProfile(@Valid @RequestBody OwnerProfileUpdateDto updateDTO) {
        OwnerProfileDto updatedProfile = userService.updateCurrentOwnerProfile(updateDTO);
        return ResponseEntity.ok(updatedProfile);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @PutMapping("/me/staff")
    public ResponseEntity<ClinicStaffProfileDto> updateCurrentClinicStaffProfile(@Valid @RequestBody UserProfileUpdateDto updateDTO) {
        // Note: We use the generic UserProfileUpdateDto as input here
        ClinicStaffProfileDto updatedProfile = userService.updateCurrentClinicStaffProfile(updateDTO);
        return ResponseEntity.ok(updatedProfile);
    }
}
