package com.petconnect.backend.user.application.service.impl;

import com.petconnect.backend.exception.UsernameAlreadyExistsException;
import com.petconnect.backend.user.application.dto.*;
import com.petconnect.backend.user.application.mapper.UserMapper;
import com.petconnect.backend.user.application.service.UserService;
import com.petconnect.backend.user.domain.model.*;
import com.petconnect.backend.user.domain.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Optional;

/**
 * Implementation of the UserService interface.
 * Handles retrieving user profiles and finding users.
 * Update methods to be implemented later.
 *
 * @author ibosquet
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final OwnerRepository ownerRepository;
    private final ClinicStaffRepository clinicStaffRepository;
    private final UserMapper userMapper;

    /**
     * {@inheritDoc}
     * Determines the user type (Owner or ClinicStaff) and returns the appropriate DTO.
     */
    @Override
    @Transactional(readOnly = true)
    public Object getCurrentUserProfile() {
        UserEntity currentUser = findAuthenticatedUser();
        return switch (currentUser) {
            case Owner owner -> userMapper.toOwnerProfileDto(owner);
            case ClinicStaff staff -> userMapper.toClinicStaffProfileDto(staff);
            default -> throw new IllegalStateException("UserEntity type mapping not handled: " + currentUser.getClass());
        };
    }


    /**
     * {@inheritDoc}
     * Finds user by ID and maps to the generic UserProfileDto using the mapper.
     * Authorization: Allow if user is requesting their own profile OR if user has ADMIN role.
     */
    @Override
    @Transactional(readOnly = true)
    // SpEL: '#id == authentication.principal.id' (approx) OR hasRole('ADMIN')
    // We use the helper method via a bean reference '@userServiceHelper'
    // Or compare ID directly if principal holds UserEntity/UserDetails with ID
    @PreAuthorize("#id == @userServiceHelper.getAuthenticatedUserId() or hasRole('ADMIN')")
    public Optional<UserProfileDto> findUserById(Long id) {
        log.debug("Attempting to find user by ID: {} (Authorized check passed)", id);
        return userRepository.findById(id)
                .map(userMapper::mapToBaseProfileDTO); // Uses the refactored mapper
    }

    /**
     * {@inheritDoc}
     * Finds user by email and maps to the generic UserProfileDto using the mapper.
     * Authorization: Restrict to ADMIN role only.
     */
    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')") // Only ADMINs can search by email
    public Optional<UserProfileDto> findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(userMapper::mapToBaseProfileDTO);
    }

    /**
     * {@inheritDoc}
     * Finds user by username and maps to the generic UserProfileDto using the mapper.
     * Authorization: Restrict to ADMIN role only (similar to search by email).
     * Or potentially allow self-lookup if needed? For now, restrict to ADMIN.
     */
    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')") // Only ADMINs can search by username (unless self-lookup needed)
    public Optional<UserProfileDto> findUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(userMapper::mapToBaseProfileDTO);
    }

    /**
     * {@inheritDoc}
     * Updates the profile for the currently authenticated Owner.
     * Handles username uniqueness check before delegating update to mapper.
     * Authorization: Implicitly handled by checking 'currentUser instanceof Owner'.
     * Further @PreAuthorize("hasRole('OWNER')") could be added for clarity/defense-in-depth.
     */
    @Override
    @Transactional
    @PreAuthorize("hasRole('OWNER')")
    public OwnerProfileDto updateCurrentOwnerProfile(OwnerProfileUpdateDto updateDTO) {
        UserEntity currentUser = findAuthenticatedUser();
        if (!(currentUser instanceof Owner owner)) {
            throw new IllegalStateException("Access denied: Current user is not an Owner.");
        }

        // Validate username uniqueness If it's being changed
        validateUsernameUpdate(updateDTO.username(), owner);

        // Apply all updates using the mapper
        userMapper.updateOwnerFromDto(updateDTO, owner); // Mapper handles common + specific fields

        Owner updatedOwner = ownerRepository.save(owner);
        return userMapper.toOwnerProfileDto(updatedOwner);
    }

    /**
     * {@inheritDoc}
     * Updates common profile fields for the currently authenticated ClinicStaff.
     * Handles username uniqueness check before delegating update to mapper.
     * Authorization: Implicitly handled by checking 'currentUser instanceof ClinicStaff'.
     * Further @PreAuthorize("hasAnyRole('ADMIN', 'VET')") could be added.
     */
    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'VET')")
    public ClinicStaffProfileDto updateCurrentClinicStaffProfile(UserProfileUpdateDto updateDTO) {
        UserEntity currentUser = findAuthenticatedUser();
        if (!(currentUser instanceof ClinicStaff staff)) {
            throw new IllegalStateException("Access denied: Current user is not Clinic Staff.");
        }

        // Validate username uniqueness IF it's being changed
        validateUsernameUpdate(updateDTO.username(), staff);
        // Apply common updates using the mapper
        userMapper.updateClinicStaffCommonFromDto(updateDTO, staff); // Mapper handles common fields

        ClinicStaff updatedStaff = clinicStaffRepository.save(staff);
        return userMapper.toClinicStaffProfileDto(updatedStaff);
    }

    // --- Helper Methods ---

    /**
     * Retrieves the currently authenticated UserEntity entity from the database.
     * @return The authenticated UserEntity entity.
     * @throws IllegalStateException if no user is authenticated.
     * @throws UsernameNotFoundException if the authenticated user is not found in the repository.
     */
    private UserEntity findAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new IllegalStateException("No authenticated user found in security context.");
        }
        String currentUserIdentifier = authentication.getName();
        return userRepository.findByEmail(currentUserIdentifier)
                .or(() -> userRepository.findByUsername(currentUserIdentifier))
                .orElseThrow(() -> new UsernameNotFoundException("Authenticated user not found with identifier: " + currentUserIdentifier));
    }

    /**
     * Validates if a potential new username can be used for updating an existing user.
     * Checks if the new username is provided, different from the current one,
     * and if it already exists in the database for another user.
     *
     * @param newUsername The potential new username from the update DTO (can be null or blank).
     * @param existingUser The UserEntity being updated.
     * @throws UsernameAlreadyExistsException if the new username is provided, different, and already taken.
     */
    private void validateUsernameUpdate(String newUsername, UserEntity existingUser) {
        // Perform check only if newUsername is provided and different from the existing one
        if (StringUtils.hasText(newUsername) && !existingUser.getUsername().equals(newUsername) && userRepository.existsByUsername(newUsername)) {
            throw new UsernameAlreadyExistsException(newUsername);
        }
    }
}
