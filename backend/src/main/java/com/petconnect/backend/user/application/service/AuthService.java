package com.petconnect.backend.user.application.service;

import com.petconnect.backend.user.application.dto.AuthLoginRequestDto;
import com.petconnect.backend.user.application.dto.AuthResponseDto;
import com.petconnect.backend.user.application.dto.OwnerProfileDto;
import com.petconnect.backend.user.application.dto.OwnerRegistrationDto;
import com.petconnect.backend.exception.EmailAlreadyExistsException;
import com.petconnect.backend.exception.UsernameAlreadyExistsException;

/**
 * Service interface for authentication and initial user registration processes.
 * Defines operations for registering new owners and authenticating existing users.
 *
 * @author ibosquet
 */
public interface AuthService {
    /**
     * Registers a new Pet Owner user in the system.
     * <p>
     * This method handles the creation of a new user entity specifically for the OWNER role.
     * It includes validation of input data (like email and username uniqueness) and
     * secure password hashing before persisting the new user.
     * </p>
     *
     * @param registrationDTO DTO containing the owner's registration details (username, email, password, phone).
     * @return A DTO representing the profile of the newly created owner upon successful registration.
     * @throws EmailAlreadyExistsException    if the provided email address is already registered in the system.
     * @throws UsernameAlreadyExistsException if the provided username is already taken.
     * @throws IllegalArgumentException       if input validation fails on the DTO.
     * @throws IllegalStateException          if required, system roles (like OWNER) are not configured correctly.
     */
    OwnerProfileDto registerOwner(OwnerRegistrationDto registrationDTO);

    /**
     * Authenticates a user based on the provided login credentials (username and password).
     * <p>
     * Verifies the credentials against stored user data. If authentication is successful,
     * it generates and returns a JSON Web Token (JWT) for later API requests.
     * </p>
     *
     * @param authLoginRequest DTO containing the user's login credentials (username and password).
     * @return An AuthResponseDto containing the username, a success message, the generated JWT,
     *         and a status flag indicating success (true).
     */
    AuthResponseDto loginUser(AuthLoginRequestDto authLoginRequest);
}
