package com.petconnect.backend.user.application.service;

import com.petconnect.backend.user.application.dto.*; // Import necessary DTOs
import jakarta.persistence.EntityNotFoundException;

import java.util.Optional;

/**
 * Service interface for managing general UserEntity operations, including retrieving profiles,
 * finding users, and potentially updating common profile information.
 * Specific role operations (Owner registration, Clinic Staff creation/management)
 * might be handled in more specialized services (AuthService, ClinicStaffService).
 *
 * @author ibosquet
 */
public interface UserService {

    /**
     * Retrieves the profile information of the currently authenticated user.
     * Determines the user type and returns the appropriate specific profile DTO.
     *
     * @return A profile DTO (OwnerProfileDto or ClinicStaffProfileDto) cast to Object,
     *         or consider a common Profile interface if preferred. Returns null if no user found.
     * @throws IllegalStateException if there is no authenticated user in the security context.
     */
    Object getCurrentUserProfile(); // Return Object to allow specific DTO types

    /**
     * Finds a user by their unique identifier.
     *
     * @param id The ID of the user.
     * @return An Optional containing the generic UserProfileDto if found, otherwise empty.
     */
    Optional<UserProfileDto> findUserById(Long id);

    /**
     * Finds a user by their unique email address.
     *
     * @param email The email address.
     * @return An Optional containing the generic UserProfileDto if found, otherwise empty.
     */
    Optional<UserProfileDto> findUserByEmail(String email);

    /**
     * Finds a user by their unique username.
     *
     * @param username The username.
     * @return An Optional containing the generic UserProfileDto if found, otherwise empty.
     */
    Optional<UserProfileDto> findUserByUsername(String username);


    /**
     * Updates the profile of the currently authenticated Owner user.
     *
     * @param updateDTO DTO containing the fields to update.
     * @return The updated OwnerProfileDto.
     * @throws IllegalStateException if the current user is not an Owner or not authenticated.
     * @throws EntityNotFoundException if the user cannot be found.
     */
    OwnerProfileDto updateCurrentOwnerProfile(OwnerProfileUpdateDto updateDTO);

    /**
     * Updates the profile of the currently authenticated ClinicStaff user.
     * (Note: Admins updating OTHER staff might be in ClinicStaffService).
     *
     * @param updateDTO DTO containing the fields to update.
     * @return The updated ClinicStaffProfileDto.
     * @throws IllegalStateException if the current user is not ClinicStaff or not authenticated.
     * @throws EntityNotFoundException if the user cannot be found.
     */
    ClinicStaffProfileDto updateCurrentClinicStaffProfile(UserProfileUpdateDto updateDTO);
}
