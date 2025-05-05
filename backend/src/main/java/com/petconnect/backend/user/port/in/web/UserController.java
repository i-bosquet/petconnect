package com.petconnect.backend.user.port.in.web;

import com.petconnect.backend.user.application.dto.*;
import com.petconnect.backend.user.application.service.UserService;
import io.micrometer.common.lang.Nullable;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

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
    @PutMapping(value = "/me", consumes = MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<OwnerProfileDto> updateCurrentOwnerProfile(
            @RequestPart("dto") @Valid OwnerProfileUpdateDto updateDTO,
            @RequestPart(value = "imageFile", required = false) @Nullable MultipartFile imageFile
    ) throws IOException {
        OwnerProfileDto updatedProfile = userService.updateCurrentOwnerProfile(updateDTO, imageFile);
        return ResponseEntity.ok(updatedProfile);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @PutMapping(value = "/me/staff", consumes = MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ClinicStaffProfileDto> updateCurrentClinicStaffProfile( @RequestPart("dto") @Valid UserProfileUpdateDto updateDTO,
                                                                                  @RequestPart(value = "imageFile", required = false) @Nullable MultipartFile imageFile
    ) throws IOException {
        ClinicStaffProfileDto updatedProfile = userService.updateCurrentClinicStaffProfile(updateDTO, imageFile);
        return ResponseEntity.ok(updatedProfile);
    }
}
