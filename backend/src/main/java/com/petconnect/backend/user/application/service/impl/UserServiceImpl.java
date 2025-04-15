package com.petconnect.backend.user.application.service.impl;


import com.petconnect.backend.exception.UsernameAlreadyExistsException;
import com.petconnect.backend.user.application.dto.*;
import com.petconnect.backend.user.application.mapper.UserMapper;
import com.petconnect.backend.user.application.service.UserService;
import com.petconnect.backend.user.application.service.helper.UserServiceHelper;
import com.petconnect.backend.user.domain.model.*;
import com.petconnect.backend.user.domain.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Objects;
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
    private final UserServiceHelper userServiceHelper;


    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public Object getCurrentUserProfile() {
        UserEntity currentUser = userServiceHelper.getAuthenticatedUserEntity();
        return switch (currentUser) {
            case Owner owner -> userMapper.toOwnerProfileDto(owner);
            case ClinicStaff staff -> userMapper.toClinicStaffProfileDto(staff);
            default -> {
                // Should not happen with current model if users are always Owner or ClinicStaff
                log.error("Unhandled UserEntity subtype in getCurrentUserProfile: {}", currentUser.getClass());
                throw new IllegalStateException("User profile type mapping not handled for: " + currentUser.getClass().getSimpleName());
            }
        };
    }


    /**
     * {@inheritDoc}
     * Finds a user by ID and maps to the generic UserProfileDto.
     * Authorization Rules:
     * Allows any user to retrieve their own profile.
     * Allows an ADMIN user to retrieve the profile of another ClinicStaff (Vet or Admin)
     *    belonging to the SAME clinic.
     * Access is denied in all other cases (e.g., Owner viewing other Owner, Staff viewing Owner, Staff viewing Staff from other clinic).
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<UserProfileDto> findUserById(Long id) {
        // Get requester
        UserEntity requester = userServiceHelper.getAuthenticatedUserEntity();

        // Find target user
        Optional<UserEntity> targetUserOpt = userRepository.findById(id);
        if (targetUserOpt.isEmpty()) {
            log.debug("User with ID {} not found.", id);
            return Optional.empty(); // Return empty if target doesn't exist
        }
        UserEntity targetUser = targetUserOpt.get();

        // Authorization Checks
        // Allow self-lookup
        if (Objects.equals(requester.getId(), targetUser.getId())) {
            log.debug("User {} accessing own profile via ID {}", requester.getUsername(), id);
            return Optional.of(userMapper.mapToBaseProfileDTO(targetUser));
        }

        // Allow Admin to view staff from the same clinic
        if (requester instanceof ClinicStaff requesterStaff) {
            // Check if requester is ADMIN
            boolean isAdmin = requesterStaff.getRoles().stream().anyMatch(r -> r.getRoleEnum() == RoleEnum.ADMIN);
            // Check if target is ClinicStaff and from the same clinic
            if (isAdmin && targetUser instanceof ClinicStaff targetStaff) {
                Clinic requesterClinic = requesterStaff.getClinic();
                Clinic targetClinic = targetStaff.getClinic();
                if (requesterClinic != null && targetClinic != null && requesterClinic.getId().equals(targetClinic.getId())) {
                    log.debug("Admin {} accessing profile of staff {} from same clinic {}", requester.getUsername(), targetUser.getUsername(), requesterClinic.getId());
                    return Optional.of(userMapper.mapToBaseProfileDTO(targetUser));
                }
            }
        }

        // If no rule allows access, deny it
        log.warn("Access denied for user {} attempting to access profile for user {}", requester.getId(), id);
        throw new AccessDeniedException("User " + requester.getId() + " is not authorized to access profile for user " + id);
    }

    /**
     * {@inheritDoc}
     * Finds user by email and maps to the generic UserProfileDto.
     * Authorization Rules: Similar to findUserById, but using email.
     * Allows any user to retrieve their own profile via email.
     * Allows an ADMIN user to retrieve the profile of another ClinicStaff
     *    belonging to the SAME clinic via email.
     * Access is denied in all other cases.
     */
    @Override
    @Transactional(readOnly = true)
    // @PreAuthorize removed
    public Optional<UserProfileDto> findUserByEmail(String email) {
        // Get requester
        UserEntity requester = userServiceHelper.getAuthenticatedUserEntity();

        // Find target user by email
        Optional<UserEntity> targetUserOpt = userRepository.findByEmail(email);
        if (targetUserOpt.isEmpty()) {
            return Optional.empty();
        }
        UserEntity targetUser = targetUserOpt.get();

        // Authorization Checks (same logic as findUserById)
        if (Objects.equals(requester.getId(), targetUser.getId())) {
            log.debug("User {} accessing own profile via email {}", requester.getUsername(), email);
            return Optional.of(userMapper.mapToBaseProfileDTO(targetUser));
        }
        if (requester instanceof ClinicStaff requesterStaff) {
            boolean isAdmin = requesterStaff.getRoles().stream().anyMatch(r -> r.getRoleEnum() == RoleEnum.ADMIN);
            if (isAdmin && targetUser instanceof ClinicStaff targetStaff) {
                Clinic requesterClinic = requesterStaff.getClinic();
                Clinic targetClinic = targetStaff.getClinic();
                if (requesterClinic != null && targetClinic != null && requesterClinic.getId().equals(targetClinic.getId())) {
                    log.debug("Admin {} accessing profile of staff {} (email {}) from same clinic {}", requester.getUsername(), targetUser.getUsername(), email, requesterClinic.getId());
                    return Optional.of(userMapper.mapToBaseProfileDTO(targetUser));
                }
            }
        }

        // Deny access
        log.warn("Access denied for user {} attempting to access profile for email {}", requester.getId(), email);
        throw new AccessDeniedException("User " + requester.getId() + " is not authorized to access profile for email " + email);
    }

    /**
     * {@inheritDoc}
     * Finds user by username and maps to the generic UserProfileDto.
     * Authorization Rules: Similar to findUserById/findByEmail.
     * Allows self-lookup.
     * Allows Admin to lookup staff in the same clinic.
     * Denied otherwise.
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<UserProfileDto> findUserByUsername(String username) {
        UserEntity requester = userServiceHelper.getAuthenticatedUserEntity();
        Optional<UserEntity> targetUserOpt = userRepository.findByUsername(username);
        if (targetUserOpt.isEmpty()) {
            return Optional.empty();
        }
        UserEntity targetUser = targetUserOpt.get();

        if (Objects.equals(requester.getId(), targetUser.getId())) {
            log.debug("User {} accessing own profile via username {}", requester.getUsername(), username);
            return Optional.of(userMapper.mapToBaseProfileDTO(targetUser));
        }
        if (requester instanceof ClinicStaff requesterStaff) {
            boolean isAdmin = requesterStaff.getRoles().stream().anyMatch(r -> r.getRoleEnum() == RoleEnum.ADMIN);
            if (isAdmin && targetUser instanceof ClinicStaff targetStaff) {
                Clinic requesterClinic = requesterStaff.getClinic();
                Clinic targetClinic = targetStaff.getClinic();
                if (requesterClinic != null && targetClinic != null && requesterClinic.getId().equals(targetClinic.getId())) {
                    log.debug("Admin {} accessing profile of staff {} (username {}) from same clinic {}", requester.getUsername(), targetUser.getUsername(), username, requesterClinic.getId());
                    return Optional.of(userMapper.mapToBaseProfileDTO(targetUser));
                }
            }
        }

        log.warn("Access denied for user {} attempting to access profile for username {}", requester.getId(), username);
        throw new AccessDeniedException("User " + requester.getId() + " is not authorized to access profile for username " + username);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('OWNER')")
    public OwnerProfileDto updateCurrentOwnerProfile(OwnerProfileUpdateDto updateDTO) {
        UserEntity currentUser = userServiceHelper.getAuthenticatedUserEntity();
        Owner owner = (Owner) currentUser;
        validateUsernameUpdate(updateDTO.username(), owner);
        userMapper.updateOwnerFromDto(updateDTO, owner);
        Owner updatedOwner = ownerRepository.save(owner);
        return userMapper.toOwnerProfileDto(updatedOwner);
    }


    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'VET')")
    public ClinicStaffProfileDto updateCurrentClinicStaffProfile(UserProfileUpdateDto updateDTO) {
        UserEntity currentUser = userServiceHelper.getAuthenticatedUserEntity();
        ClinicStaff staff = (ClinicStaff) currentUser;
        validateUsernameUpdate(updateDTO.username(), staff);
        userMapper.updateClinicStaffCommonFromDto(updateDTO, staff);
        ClinicStaff updatedStaff = clinicStaffRepository.save(staff);
        return userMapper.toClinicStaffProfileDto(updatedStaff);
    }

    // --- PRIVATE HELPER METHODS ---
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
        if (StringUtils.hasText(newUsername) && !existingUser.getUsername().equals(newUsername) && userRepository.existsByUsername(newUsername))
            throw new UsernameAlreadyExistsException(newUsername);
    }
}