package com.petconnect.backend.user.application.service;

import com.petconnect.backend.user.application.dto.AuthLoginRequestDto;
import com.petconnect.backend.user.application.dto.AuthResponseDto;
import com.petconnect.backend.user.application.dto.OwnerProfileDto;
import com.petconnect.backend.user.application.dto.OwnerRegistrationDto;
import org.springframework.security.core.userdetails.UserDetails;
import com.petconnect.backend.exception.EmailAlreadyExistsException;
import com.petconnect.backend.exception.UsernameAlreadyExistsException;

/**
 * Service interface for authentication-related operations,
 * primarily handling user registration (initially Owner) and login (later).
 *
 * @author ibosquet
 */
public interface AuthService {
    /**
     * Registers a new Pet Owner user in the system.
     * Handles password hashing and saving the new user.
     *
     * @param registrationDTO DTO containing the owner's registration details.
     * @return A DTO representing the profile of the newly created owner.
     * @throws EmailAlreadyExistsException if the email is already registered.
     * @throws UsernameAlreadyExistsException if the username is already registered.
     */
    OwnerProfileDto registerOwner(OwnerRegistrationDto registrationDTO);

    /**
     * Authenticates a user using the provided credentials.
     *
     * @param authLoginRequest the authentication request containing a username and password.
     * @return an AuthResponseDto containing the authentication result and JWT token if successful.
     */
    AuthResponseDto loginUser(AuthLoginRequestDto authLoginRequest);

    /**
     * Loads the user's details by username.
     *
     * @param username the username of the user to load.
     * @return a UserDetails object containing user information needed for authentication.
     */
    UserDetails loadUserByUsername(String username);
}
