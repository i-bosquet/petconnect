package com.petconnect.backend.user.application.service.impl;


import com.petconnect.backend.common.helper.AuthorizationHelper;
import com.petconnect.backend.common.helper.EntityFinderHelper;
import com.petconnect.backend.common.helper.UserHelper;
import com.petconnect.backend.user.application.dto.*;
import com.petconnect.backend.user.application.mapper.UserMapper;
import com.petconnect.backend.user.application.service.UserService;
import com.petconnect.backend.user.domain.model.*;
import com.petconnect.backend.user.domain.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
    private final UserHelper userServiceHelper;
    private final EntityFinderHelper entityFinderHelper;
    private final AuthorizationHelper authorizationHelper;


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
                // Should not happen with the current model if users are always Owner or ClinicStaff
                log.error("Unhandled UserEntity subtype in getCurrentUserProfile: {}", currentUser.getClass());
                throw new IllegalStateException("User profile type mapping not handled for: " + currentUser.getClass().getSimpleName());
            }
        };
    }


    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<UserProfileDto> findUserById(Long id) {
        // Get requester
        UserEntity requester = userServiceHelper.getAuthenticatedUserEntity();

        // Find the target user
        UserEntity targetUserOpt = entityFinderHelper.findUserOrFail(id);
        if (targetUserOpt==null) {
            log.debug("User with ID {} not found.", id);
            return Optional.empty(); // Return empty if the target doesn't exist
        }

        // Authorization Checks
        if (Objects.equals(requester.getId(), targetUserOpt.getId())) {
            log.debug("User {} accessing own profile via ID {}", requester.getUsername(), id);
            return Optional.of(userMapper.mapToBaseProfileDTO(targetUserOpt));
        }

        // Allow Admin to view staff from the same clinic
        if (requester instanceof ClinicStaff requesterStaff) {
            // Check if the requester is ADMIN
            boolean isAdmin = requesterStaff.getRoles().stream().anyMatch(r -> r.getRoleEnum() == RoleEnum.ADMIN);
            // Check if the target is ClinicStaff and from the same clinic
            if (isAdmin && targetUserOpt instanceof ClinicStaff targetStaff) {
                Clinic requesterClinic = requesterStaff.getClinic();
                Clinic targetClinic = targetStaff.getClinic();
                if (requesterClinic != null && targetClinic != null && requesterClinic.getId().equals(targetClinic.getId())) {
                    log.debug("Admin {} accessing profile of staff {} from same clinic {}", requester.getUsername(), targetUserOpt.getUsername(), requesterClinic.getId());
                    return Optional.of(userMapper.mapToBaseProfileDTO(targetUserOpt));
                }
            }
        }

        // If no rule allows access, deny it
        log.warn("Access denied for user {} attempting to access profile for user {}", requester.getId(), id);
        throw new AccessDeniedException("User " + requester.getId() + " is not authorized to access profile for user " + id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    // @PreAuthorize removed
    public Optional<UserProfileDto> findUserByEmail(String email) {
        // Get requester
        UserEntity requester = userServiceHelper.getAuthenticatedUserEntity();

        // Find the target user by email
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

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    @PreAuthorize("hasRole('OWNER')")
    public OwnerProfileDto updateCurrentOwnerProfile(OwnerProfileUpdateDto updateDTO) {
        UserEntity currentUser = userServiceHelper.getAuthenticatedUserEntity();
        Owner owner = (Owner) currentUser;
        authorizationHelper.validateUsernameUpdate(updateDTO.username(), owner);
        userMapper.updateOwnerFromDto(updateDTO, owner);
        Owner updatedOwner = ownerRepository.save(owner);
        return userMapper.toOwnerProfileDto(updatedOwner);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'VET')")
    public ClinicStaffProfileDto updateCurrentClinicStaffProfile(UserProfileUpdateDto updateDTO) {
        UserEntity currentUser = userServiceHelper.getAuthenticatedUserEntity();
        ClinicStaff staff = (ClinicStaff) currentUser;
        authorizationHelper.validateUsernameUpdate(updateDTO.username(), staff);
        userMapper.updateClinicStaffCommonFromDto(updateDTO, staff);
        ClinicStaff updatedStaff = clinicStaffRepository.save(staff);
        return userMapper.toClinicStaffProfileDto(updatedStaff);
    }

}