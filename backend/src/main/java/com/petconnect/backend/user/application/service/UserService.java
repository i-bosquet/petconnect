package com.petconnect.backend.user.application.service;

import com.petconnect.backend.user.application.dto.*;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.lang.Nullable;
import java.io.IOException;

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
     * Loads user-specific data. This method is required by the UserDetailsService interface.
     * It uses the username (which could be email in our case) to find the user.
     *
     * @param username The username (or email) identifying the user whose data is required.
     * @return a fully populated {@link UserDetails} object (never {@code null})
     * @throws UsernameNotFoundException if the user could not be found or the user has no
     *                                   GrantedAuthority
     */
    UserDetails loadUserByUsername(String username) throws UsernameNotFoundException ;

    /**
     * Retrieves the profile information of the currently authenticated user.
     * Determines the user type and returns the appropriate specific profile DTO.
     *
     * @return A profile DTO (OwnerProfileDto or ClinicStaffProfileDto) cast to Object,
     *         or consider a common Profile interface if preferred. Returns null if no user is found.
     * @throws IllegalStateException if there is no authenticated user in the security context.
     */
    Object getCurrentUserProfile();

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
     * @return The {@link OwnerProfileUpdateResponseDto} containing the updated profile and potentially a new JWT.
     * @throws IOException if image processing fails.
     */
    OwnerProfileUpdateResponseDto updateCurrentOwnerProfile(OwnerProfileUpdateDto updateDTO, @Nullable MultipartFile imageFile )throws IOException;

    /**
     * Updates the profile of the currently authenticated ClinicStaff user.
     * (Note: Admins updating OTHER staff might be in ClinicStaffService).
     *
     * @param updateDTO DTO containing the fields to update.
     * @return The {@link ClinicStaffProfileUpdateResponseDto} containing the updated profile and potentially a new JWT.
     * @throws IOException if image processing fails.
     */
    ClinicStaffProfileUpdateResponseDto updateCurrentClinicStaffProfile(UserProfileUpdateDto updateDTO, @Nullable MultipartFile imageFile ) throws IOException;
}