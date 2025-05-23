package com.petconnect.backend.user.application.service.impl;


import com.petconnect.backend.common.helper.AuthorizationHelper;
import com.petconnect.backend.common.helper.EntityFinderHelper;
import com.petconnect.backend.common.helper.UserHelper;
import com.petconnect.backend.common.service.ImageService;
import com.petconnect.backend.security.JwtUtils;
import com.petconnect.backend.user.application.dto.*;
import com.petconnect.backend.user.application.mapper.UserMapper;
import com.petconnect.backend.user.application.service.UserService;
import com.petconnect.backend.user.domain.model.*;
import com.petconnect.backend.user.domain.repository.*;
import io.micrometer.common.lang.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
    private final ImageService imageService;
    private final JwtUtils jwtUtils;

    @Value("${app.default.user.image.path}")
    private String defaultUserImagePathBase;


    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException  {
        log.debug("Attempting to load user by username or email: {}", username);

        UserEntity userEntity = userRepository.findByUsernameWithRolesAndPermissions(username)
                .or(() -> userRepository.findByEmailWithRolesAndPermissions(username))
                .orElseThrow(() -> {
                    log.warn("User not found with identifier: {}", username);
                    return new UsernameNotFoundException("El usuario " + username + " no existe.");
                });

        log.debug("User found: {}. Roles loaded: {}", userEntity.getUsername(), userEntity.getRoles().size());

        boolean effectivelyEnabled = userEntity.isEnabled();

        if (userEntity instanceof ClinicStaff staffMember) {
            effectivelyEnabled = staffMember.isActive();
            log.debug("User is ClinicStaff, using isActive status: {}", effectivelyEnabled);
        }

        List<SimpleGrantedAuthority> authorityList = new ArrayList<>();

        userEntity.getRoles().forEach(role -> {
            authorityList.add(new SimpleGrantedAuthority("ROLE_" + role.getRoleEnum().name()));
            log.trace("Added role authority: ROLE_{}", role.getRoleEnum().name());

            role.getPermissionList().forEach(permission -> {
                authorityList.add(new SimpleGrantedAuthority(permission.getName()));
                log.trace("Added permission authority: {}", permission.getName());
            });
        });

        log.debug("Total authorities loaded for user {}: {}", userEntity.getUsername(), authorityList.size());

        return new User(userEntity.getUsername(),
                userEntity.getPassword(),
                effectivelyEnabled,
                userEntity.isAccountNonExpired(),
                userEntity.isCredentialsNonExpired(),
                userEntity.isAccountNonLocked(),
                authorityList);
    }

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
                    log.debug("Admin {} accessing profile of staff {} from same clinic {}",
                            requester.getUsername(), targetUserOpt.getUsername(), requesterClinic.getId());
                    return Optional.of(userMapper.mapToBaseProfileDTO(targetUserOpt));
                }
            }
        }

        log.warn("Access denied for user {} attempting to access profile for user {}", requester.getId(), id);
        throw new AccessDeniedException("User " + requester.getId() + " is not authorized to access profile for user " + id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<UserProfileDto> findUserByEmail(String email) {

        UserEntity requester = userServiceHelper.getAuthenticatedUserEntity();

        Optional<UserEntity> targetUserOpt = userRepository.findByEmail(email);
        if (targetUserOpt.isEmpty()) {
            return Optional.empty();
        }
        UserEntity targetUser = targetUserOpt.get();

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
                    log.debug("Admin {} accessing profile of staff {} (email {}) from same clinic {}",
                            requester.getUsername(), targetUser.getUsername(), email, requesterClinic.getId());
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
                    log.debug("Admin {} accessing profile of staff {} (username {}) from same clinic {}",
                            requester.getUsername(), targetUser.getUsername(), username, requesterClinic.getId());
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
    public OwnerProfileUpdateResponseDto  updateCurrentOwnerProfile(
            OwnerProfileUpdateDto updateDTO,
            @Nullable MultipartFile imageFile) throws IOException {

        UserEntity currentUser = userServiceHelper.getAuthenticatedUserEntity();
        if (!(currentUser instanceof Owner owner)) {
            log.error("Security Mismatch: User {} attempted to update owner profile but is not an Owner.", currentUser.getId());
            throw new AccessDeniedException("User is not an Owner.");
        }
        log.info("Attempting to update profile for Owner ID: {}", owner.getId());

        String oldUsername = owner.getUsername();
        authorizationHelper.validateUsernameUpdate(updateDTO.username(), owner);

        String oldAvatarPath = owner.getAvatar();
        boolean imageChanged = false;

        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                String newAvatarPath = imageService.storeImage(imageFile, "users/avatars");
                owner.setAvatar(newAvatarPath);
                imageChanged = true;
                log.info("New avatar stored for owner {}: {}", owner.getId(), newAvatarPath);
            } catch (IOException | IllegalArgumentException e) {
                log.error("Failed to store uploaded avatar for owner {}: {}", owner.getId(), e.getMessage());
                throw new IOException("Failed to process avatar image: " + e.getMessage(), e);
            }
        }

        boolean otherFieldsChanged = userMapper.updateOwnerFromDto(updateDTO, owner);

        Owner updatedOwner = owner;
        String newJwtToken = null;

        if (imageChanged || otherFieldsChanged) {
            log.info("Changes detected (image: {}, other fields: {}), saving Owner {}", imageChanged, otherFieldsChanged, owner.getId());
            updatedOwner = ownerRepository.save(owner);

            if (imageChanged && oldAvatarPath != null && !isDefaultUserAvatar(oldAvatarPath) && !oldAvatarPath.equals(updatedOwner.getAvatar()) ) {
                imageService.deleteImage(oldAvatarPath);
            }
            log.info("Owner {} profile updated successfully.", owner.getId());

            if (updateDTO.username() != null && !oldUsername.equals(updatedOwner.getUsername())) {
                log.info("Username changed for Owner ID {}. Generating new JWT.", updatedOwner.getId());

                UserDetails userDetailsForToken = loadUserByUsername(updatedOwner.getUsername());

                Authentication newAuthentication = new UsernamePasswordAuthenticationToken(
                        updatedOwner.getId(),
                        null,
                        userDetailsForToken.getAuthorities()
                );
                newJwtToken = jwtUtils.createToken(newAuthentication);
                log.debug("New JWT generated: {}", newJwtToken != null ? "Yes" : "No");
            }
        } else {
            log.info("No effective changes detected for Owner {}, update skipped.", owner.getId());
        }

        OwnerProfileDto profileDto = userMapper.toOwnerProfileDto(updatedOwner);
        return new OwnerProfileUpdateResponseDto(profileDto, newJwtToken);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'VET')")
    public ClinicStaffProfileUpdateResponseDto  updateCurrentClinicStaffProfile(
            UserProfileUpdateDto updateDTO,
            @Nullable MultipartFile imageFile) throws IOException {
        UserEntity currentUser = userServiceHelper.getAuthenticatedUserEntity();
        if (!(currentUser instanceof ClinicStaff staff)) {
            log.error("Security Mismatch: User {} attempted to update staff profile but is not ClinicStaff.", currentUser.getId());
            throw new AccessDeniedException("User is not Clinic Staff.");
        }
        log.info("Attempting to update profile for Staff ID: {}", staff.getId());

        String oldUsername = staff.getUsername();
        authorizationHelper.validateUsernameUpdate(updateDTO.username(), staff); // Valida si se proporciona un nuevo username

        String oldAvatarPath = staff.getAvatar();
        boolean imageChanged = false;

        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                String newAvatarPath = imageService.storeImage(imageFile, "users/avatars");
                staff.setAvatar(newAvatarPath);
                imageChanged = true;
                log.info("New avatar stored for staff {}: {}", staff.getId(), newAvatarPath);
            } catch (IOException | IllegalArgumentException e) {
                log.error("Failed to store uploaded avatar for staff {}: {}", staff.getId(), e.getMessage());
                throw new IOException("Failed to process avatar image: " + e.getMessage(), e);
            }
        }

        boolean otherFieldsChanged = userMapper.updateClinicStaffCommonFromDto(updateDTO, staff);

        ClinicStaff updatedStaff = staff;
        String newJwtToken = null;

        if (imageChanged || otherFieldsChanged) {
            log.info("Changes detected (image: {}, other fields: {}), saving Staff {}", imageChanged, otherFieldsChanged, staff.getId());
            updatedStaff = clinicStaffRepository.save(staff);

            if (imageChanged && oldAvatarPath != null && !isDefaultUserAvatar(oldAvatarPath) && !oldAvatarPath.equals(updatedStaff.getAvatar())) {
                imageService.deleteImage(oldAvatarPath);
            }
            log.info("Staff {} profile updated successfully.", staff.getId());

            if (updateDTO.username() != null && !oldUsername.equals(updatedStaff.getUsername())) {
                log.info("Username changed for ClinicStaff ID {}. Generating new JWT.", updatedStaff.getId());

                UserDetails userDetailsForToken = loadUserByUsername(updatedStaff.getUsername());

                Authentication newAuthentication = new UsernamePasswordAuthenticationToken(
                        updatedStaff.getId(),
                        null,
                        userDetailsForToken.getAuthorities()
                );
                newJwtToken = jwtUtils.createToken(newAuthentication);
                log.debug("New JWT generated: {}", newJwtToken != null ? "Yes" : "No");
            }
        } else {
            log.info("No effective changes detected for Staff {}, update skipped.", staff.getId());
        }

        ClinicStaffProfileDto profileDto = userMapper.toClinicStaffProfileDto(updatedStaff);
        return new ClinicStaffProfileUpdateResponseDto(profileDto, newJwtToken);
    }

    /**
     * Helper method to check if an avatar path corresponds to one of the known defaults.
     * Uses the configured base path. Default paths are configured to start with 'images/avatars/'.
     * Stored paths might or might not have the base URL prefix depending on how they were saved/retrieved.
     *
     * @param path The avatar path to check (can be null or a relative path like 'images/avatars/users/owner.png' or a full path).
     * @return true if the path represents a default user image path, false otherwise.
     */
    private boolean isDefaultUserAvatar(String path) {
        if (!StringUtils.hasText(path)) {
            log.trace("Path is null or empty, considered default.");
            return true;
        }
        // Define known prefixes/patterns for default avatars
        String prefix = this.defaultUserImagePathBase;
        // Use the path as stored
        boolean isDefault = path.startsWith(prefix);
        log.trace("Comparing path '{}' against default prefix '{}'. Is default? {}", path, prefix, isDefault);
        return isDefault;
    }
}