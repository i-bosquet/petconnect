package com.petconnect.backend.user.port.in.web;

import com.petconnect.backend.user.application.dto.*;
import com.petconnect.backend.user.application.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Implementation of the {@link AuthControllerApi}.
 * Handles incoming HTTP requests for authentication and delegates to the {@link AuthService}.
 *
 * @author ibosquet
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController implements AuthControllerApi {

    private final AuthService authService;

    /**
     * {@inheritDoc}
     */
    @PostMapping("/register")
    public ResponseEntity<OwnerProfileDto> registerOwner(@Valid @RequestBody OwnerRegistrationDto registrationDTO){
        OwnerProfileDto createdOwner = authService.registerOwner(registrationDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdOwner);}

    /**
     * {@inheritDoc}
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody AuthLoginRequestDto userRequest){
        return new ResponseEntity<>(this.authService.loginUser(userRequest), HttpStatus.OK);}

    /**
     * {@inheritDoc}
     */
    @Override
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequestDto requestDto) {
        authService.requestPasswordReset(requestDto);
        // Always return OK for security reasons
        return ResponseEntity.ok(Map.of("message", "Password reset instructions sent if email is registered."));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(
            @Valid @RequestBody PasswordResetDto resetDto) {
        authService.resetPassword(resetDto);
        return ResponseEntity.ok(Map.of("message", "Password has been reset successfully."));
    }
}
