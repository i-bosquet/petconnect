package com.petconnect.backend.user.application.mapper;

import com.petconnect.backend.common.helper.Utils;
import com.petconnect.backend.user.application.dto.*;
import com.petconnect.backend.user.domain.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import com.petconnect.backend.common.helper.ImageUrlHelper;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * Utility class for manually mapping UserEntity-related entities (Owner, ClinicStaff, Vet)
 * to their corresponding Profile DTOs.
 * Registered as a Spring component to be injectable in services.
 * Creation/Update mappings are handled within the service layer.
 *
 * @author ibosquet
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserMapper {

    private final ImageUrlHelper imageUrlHelper;

    @Value("${app.backend.base-url}")
    private String backendBaseUrl;

    @Value("${app.default.user.image.path}")
    private String defaultUserImageDbPrefix;

    private static final String USER_AVATAR_UPLOAD_SUBDIRECTORY = "users/avatars";
    private static final String DEFAULT_USER_AVATAR_URL_PREFIX = "/images/avatars/users/";
    private static final String UPLOADED_USER_AVATAR_URL_PREFIX = "/storage/users/avatars/";
    private static final String SYSTEM_DEFAULT_USER_AVATAR_FILENAME = "default_avatar.png";

    /**
     * Maps an Owner entity to an OwnerProfileDto.
     * Delegates common field mapping to helper methods.
     *
     * @param owner The Owner entity. Cannot be null.
     * @return The corresponding OwnerProfileDto.
     */
    public OwnerProfileDto toOwnerProfileDto(Owner owner) {
        if (owner == null) return null;
        UserProfileDto baseDto = mapToBaseProfileDTO(owner);
        return new OwnerProfileDto(
                baseDto.id(),
                baseDto.username(),
                baseDto.email(),
                baseDto.roles(),
                baseDto.avatar(),
                owner.getPhone()
        );
    }

    /**
     * Maps a ClinicStaff entity (Vet or Admin) to a ClinicStaffProfileDto.
     * Delegates common field mapping and handles Vet-specific fields.
     *
     * @param staff The ClinicStaff entity. Cannot be null.
     * @return The corresponding ClinicStaffProfileDto.
     */
    public ClinicStaffProfileDto toClinicStaffProfileDto(ClinicStaff staff) {
        if (staff == null) return null;
        UserProfileDto baseDto = mapToBaseProfileDTO(staff);

        String licenseNumber = null;
        String vetPublicKeyPath = null;
        if (staff instanceof Vet vet) {
            licenseNumber = vet.getLicenseNumber();
            vetPublicKeyPath = vet.getVetPublicKey();
        }

        Long clinicId = (staff.getClinic() != null) ? staff.getClinic().getId() : null;
        String clinicName = (staff.getClinic() != null) ? staff.getClinic().getName() : null;

        return new ClinicStaffProfileDto(
                baseDto.id(),
                baseDto.username(),
                baseDto.email(),
                baseDto.roles(),
                baseDto.avatar(),
                staff.getName(),
                staff.getSurname(),
                staff.isActive(),
                clinicId,
                clinicName,
                licenseNumber,
                vetPublicKeyPath,
                staff.getCreatedAt(),
                staff.getCreatedBy(),
                staff.getUpdatedAt(),
                staff.getUpdatedBy()
        );
    }

    /**
     * Maps any UserEntity entity to the generic UserProfileDto containing common fields.
     * This can be used by internal services or generic endpoints.
     * Includes the extracted role names.
     *
     * @param user The UserEntity entity (Owner, Vet, Admin). Cannot be null.
     * @return The corresponding UserProfileDto.
     */
    public UserProfileDto mapToBaseProfileDTO(UserEntity user) {
        if (user == null) return null;
        Set<String> roleNames = extractRoleNames(user);

        String fallbackAvatarUrl = (backendBaseUrl.endsWith("/") ? backendBaseUrl : backendBaseUrl + "/") +
                DEFAULT_USER_AVATAR_URL_PREFIX.substring(1) +
                SYSTEM_DEFAULT_USER_AVATAR_FILENAME;


        String fullAvatarUrl = imageUrlHelper.buildFullImageUrl(
                user.getAvatar(),
                defaultUserImageDbPrefix,
                DEFAULT_USER_AVATAR_URL_PREFIX,
                USER_AVATAR_UPLOAD_SUBDIRECTORY,
                UPLOADED_USER_AVATAR_URL_PREFIX,
                "USER",
                user.getId(),
                fallbackAvatarUrl
        );

        String name = null;
        String surname = null;
        String clinicName = null;

        if (user instanceof ClinicStaff staff) {
            name = staff.getName();
            surname = staff.getSurname();
            if (staff.getClinic() != null) {
                clinicName = staff.getClinic().getName();
            }
        }


        return new UserProfileDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                roleNames,
                fullAvatarUrl,
                name,
                surname,
                clinicName
        );
    }

    /**
     * Updates an Owner entity from an OwnerProfileUpdateDto.
     * Applies updates for fields allowed in the owner's self-update.
     * Uses a helper method for common user field updates.
     *
     * @param dto The OwnerProfileUpdateDto. Cannot be null.
     * @param owner The Owner entity to update. Cannot be null.
     * @return true if any field was changed, false otherwise.
     */
    public boolean  updateOwnerFromDto(OwnerProfileUpdateDto dto, Owner owner) {
        if (dto == null || owner == null) return false;
        boolean changed = false;
        // Update common fields
        changed |= Utils.updateStringFieldIfChanged(owner, dto.username(), owner::getUsername, Owner::setUsername, "username");

        // Update owner-specific fields
        changed |=  Utils.updateStringFieldIfChanged(owner, dto.phone(), owner::getPhone, Owner::setPhone, "phone");
        return changed;
    }

    /**
     * Updates common UserEntity fields (username, avatar) for a ClinicStaff entity
     * from a UserProfileUpdateDto. Used for staff self-update.
     * Uses a helper method for common user field updates.
     *
     * @param dto The UserProfileUpdateDto. Cannot be null.
     * @param staff The ClinicStaff entity to update. Cannot be null.
     * @return true if any field was changed, false otherwise. //
     */
    public boolean updateClinicStaffCommonFromDto(UserProfileUpdateDto dto, ClinicStaff staff) {
        if (dto == null || staff == null) return false;
        boolean changed;
        // Update common fields using the helper
        changed = Utils.updateStringFieldIfChanged(staff, dto.username(), staff::getUsername, ClinicStaff::setUsername, "username");
        return changed;
    }

    /**
     * Extracts the names of the roles associated with a user.
     *
     * @param user The user entity.
     * @return A Set of role names as Strings. Returns an empty set if user or roles are null/empty.
     */
    private Set<String> extractRoleNames(UserEntity user) {
        if (user == null || user.getRoles() == null) {
            return Set.of(); // Return an empty immutable set
        }
        return user.getRoles().stream()
                .map(RoleEntity::getRoleEnum)
                .map(RoleEnum::name)
                .collect(Collectors.toSet());
    }

    /**
     * Maps a list of ClinicStaff entities to a list of ClinicStaffProfileDto.
     * Handles null or empty lists gracefully.
     *
     * @param staffList The list of ClinicStaff entities.
     * @return An immutable list of corresponding ClinicStaffProfileDto objects.
     */
    public List<ClinicStaffProfileDto> toClinicStaffProfileDtoList(List<ClinicStaff> staffList) {
        if (staffList == null || staffList.isEmpty()) {
            return List.of();
        }
        return staffList.stream()
                .map(this::toClinicStaffProfileDto)
                .toList();
    }

    /**
     * Converts a {@link Vet} entity to a {@link VetSummaryDto}.
     * Returns null if the input vet is null.
     *
     * @param vet The Vet entity.
     * @return The corresponding VetSummaryDto, or null.
     */
    public VetSummaryDto toVetSummaryDto(Vet vet) {
        if (vet == null) {
            return null;
        }

        String vetAvatarFullUrl = imageUrlHelper.buildFullImageUrl(
                vet.getAvatar(),
                defaultUserImageDbPrefix,
                DEFAULT_USER_AVATAR_URL_PREFIX,
                USER_AVATAR_UPLOAD_SUBDIRECTORY,
                UPLOADED_USER_AVATAR_URL_PREFIX,
                "USER_VET",
                vet.getId(),
                (backendBaseUrl.endsWith("/") ? backendBaseUrl : backendBaseUrl + "/") + DEFAULT_USER_AVATAR_URL_PREFIX.substring(1) + "vet.png"
        );

        Clinic clinic = vet.getClinic();
        Long clinicId = null;
        String clinicName = null;
        String clinicAddress = null;
        String clinicCity = null;
        String clinicCountry = null;
        String clinicPhone = null;

        if (clinic != null) {
            clinicId = clinic.getId();
            clinicName = clinic.getName();
            clinicAddress = clinic.getAddress();
            clinicCity = clinic.getCity();
            clinicCountry = clinic.getCountry() != null ? clinic.getCountry().name() : null;
            clinicPhone = clinic.getPhone();
        }

        return new VetSummaryDto(
                vet.getId(),
                vet.getName(),
                vet.getSurname(),
                vetAvatarFullUrl,
                vet.getEmail(),
                clinicId,
                clinicName,
                clinicAddress,
                clinicCity,
                clinicCountry,
                clinicPhone
        );
    }

    /**
     * Converts a Set of {@link Vet} entities to an immutable Set of {@link VetSummaryDto}.
     * Returns an empty set if the input set is null or empty.
     *
     * @param vets The set of Vet entities.
     * @return An immutable Set of corresponding VetSummaryDto objects.
     */
    public Set<VetSummaryDto> toVetSummaryDtoSet(Set<Vet> vets) {
        if (vets == null || vets.isEmpty()) {
            return Set.of(); // Immutable empty set
        }
        return vets.stream()
                .map(this::toVetSummaryDto)
                .collect(Collectors.toUnmodifiableSet()); // Collect to an unmodifiable set
    }
}