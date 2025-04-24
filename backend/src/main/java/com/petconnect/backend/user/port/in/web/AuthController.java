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
import org.springframework.web.bind.annotation.RestController;

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
}
