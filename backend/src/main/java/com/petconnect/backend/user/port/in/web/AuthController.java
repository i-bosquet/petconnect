package com.petconnect.backend.user.port.in.web;

import com.petconnect.backend.user.application.dto.AuthLoginRequestDto;
import com.petconnect.backend.user.application.dto.OwnerProfileDto;
import com.petconnect.backend.user.application.dto.AuthResponseDto;
import com.petconnect.backend.user.application.dto.OwnerRegistrationDto;
import com.petconnect.backend.user.application.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for handling authentication-related requests,
 * such as user registration and login (login to be implemented later).
 * Base path is "/api/auth".
 *
 * @author ibosquet
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Handles POST requests to register a new Pet Owner.
     * Validates the incoming registration data.
     * Delegates the registration logic to the AuthService.
     *
     * @param registrationDTO The registration data received in the request body.
     * @return A ResponseEntity containing the profile DTO of the newly created owner
     *         and HTTP status 201 (Created).
     *         Returns HTTP status 400 (Bad Request) if validation fails.
     *         Returns HTTP status 409 (Conflict) if email or username already exist
     *         (handled by global exception handler later).
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<OwnerProfileDto> registerOwner(@Valid @RequestBody OwnerRegistrationDto registrationDTO) {
        OwnerProfileDto createdOwner = authService.registerOwner(registrationDTO);
        // Return 201 Created status along with the created owner's profile
        return ResponseEntity.status(HttpStatus.CREATED).body(createdOwner);
    }

    /**
     * Handles POST requests for user login.
     * Validates the incoming login credentials and delegates authentication to the AuthService.
     *
     * @param userRequest The authentication request data containing username and password.
     * @return A ResponseEntity containing the authentication response DTO (with JWT token)
     *         and HTTP status 200 (OK).
     */
    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody AuthLoginRequestDto userRequest) {
        return new ResponseEntity<>(this.authService.loginUser(userRequest), HttpStatus.OK);
    }
}
