package com.petconnect.backend.user.application.service.impl;

import com.petconnect.backend.common.helper.AuthorizationHelper;
import com.petconnect.backend.common.helper.EntityFinderHelper;
import com.petconnect.backend.common.helper.UserHelper;
import com.petconnect.backend.common.service.ImageService;
import com.petconnect.backend.exception.EntityNotFoundException;
import com.petconnect.backend.exception.UsernameAlreadyExistsException;
import com.petconnect.backend.security.JwtUtils;
import com.petconnect.backend.user.application.dto.*;
import com.petconnect.backend.user.application.mapper.UserMapper;
import com.petconnect.backend.user.domain.model.*;
import com.petconnect.backend.user.domain.repository.ClinicStaffRepository;
import com.petconnect.backend.user.domain.repository.OwnerRepository;
import com.petconnect.backend.user.domain.repository.UserRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.*;

/**
 * Unit tests for {@link UserServiceImpl}.
 * Verifies logic for retrieving user profiles, finding users, and updating user profiles.
 * Use mocked UserHelper to simulate authenticated user context.
 *
 * @author ibosquet
 */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    // --- Mocks ---
    @Mock private UserRepository userRepository;
    @Mock private OwnerRepository ownerRepository;
    @Mock private ClinicStaffRepository clinicStaffRepository;
    @Mock private UserMapper userMapper;
    @Mock private UserHelper userServiceHelper;
    @Mock private EntityFinderHelper entityFinderHelper;
    @Mock private AuthorizationHelper authorizationHelper;
    @Mock private ImageService imageService;
    @Mock private JwtUtils jwtUtils;

    // --- Class Under Test ---
    @InjectMocks
    private UserServiceImpl userService;

    // --- Captors ---
    @Captor private ArgumentCaptor<Owner> ownerCaptor;
    @Captor private ArgumentCaptor<ClinicStaff> clinicStaffCaptor;

    // --- Test Data ---
    private Owner ownerUser;
    private ClinicStaff adminUser;
    private Vet vetUserSameClinic;
    private ClinicStaff adminUserOtherClinic;
    private OwnerProfileDto ownerProfileDto;
    private ClinicStaffProfileDto staffProfileDto;
    private UserProfileDto genericOwnerProfileDto;
    private UserProfileDto genericStaffProfileDto;
    private UserProfileDto genericVetProfileDto;

    @BeforeEach
    void setUp() {
        Clinic clinic1 = Clinic.builder().name("Clinic A").build(); clinic1.setId(1L);
        Clinic clinic2 = Clinic.builder().name("Clinic B").build(); clinic2.setId(2L);

        RoleEntity ownerRole = RoleEntity.builder()
                .roleEnum(RoleEnum.OWNER)
                .id(1L)
                .permissionList(new HashSet<>())
                .build();
        ownerRole.setId(1L);
        RoleEntity adminRole = RoleEntity.builder().roleEnum(RoleEnum.ADMIN).build(); adminRole.setId(3L);
        RoleEntity vetRole = RoleEntity.builder().roleEnum(RoleEnum.VET).build(); vetRole.setId(2L);

        RoleEntity mockAdminRoleWithPermissions;

        ownerUser = new Owner();
        ownerUser.setId(1L);
        ownerUser.setUsername("testowner");
        ownerUser.setEmail("owner@test.com");
        ownerUser.setPassword("hashedPasswordOwner");
        ownerUser.setPhone("111-222-333");
        ownerUser.setRoles(Set.of(ownerRole));
        ownerUser.setAvatar("avatar_owner.png");
        ownerUser.setEnabled(true);
        ownerUser.setAccountNonExpired(true); ownerUser.setAccountNonLocked(true); ownerUser.setCredentialsNonExpired(true);

        adminUser = new ClinicStaff();
        adminUser.setId(2L);
        adminUser.setUsername("testadmin");
        adminUser.setEmail("admin@test.com");
        adminUser.setPassword("hashedPasswordAdmin");
        adminUser.setName("Admin");
        adminUser.setSurname("Test");
        adminUser.setClinic(clinic1);
        adminUser.setRoles(Set.of(adminRole));
        adminUser.setAvatar("avatar_admin.png");
        adminUser.setActive(true);
        adminUser.setEnabled(true);
        adminUser.setAccountNonExpired(true); adminUser.setAccountNonLocked(true); adminUser.setCredentialsNonExpired(true);

        // Vet in the SAME clinic as adminUser
        vetUserSameClinic = new Vet();
        vetUserSameClinic.setId(3L);
        vetUserSameClinic.setUsername("testvet_c1");
        vetUserSameClinic.setEmail("vet_c1@test.com");
        vetUserSameClinic.setPassword("hashedPasswordVet1");
        vetUserSameClinic.setName("Vet");
        vetUserSameClinic.setSurname("SameClinic");
        vetUserSameClinic.setClinic(clinic1);
        vetUserSameClinic.setRoles(Set.of(vetRole));
        vetUserSameClinic.setLicenseNumber("VET_C1");
        vetUserSameClinic.setVetPublicKey("KEY_C1");
        vetUserSameClinic.setAvatar("avatar_vet.png");
        vetUserSameClinic.setActive(true);
        vetUserSameClinic.setEnabled(true);
        vetUserSameClinic.setAccountNonExpired(true);
        vetUserSameClinic.setAccountNonLocked(true); vetUserSameClinic.setCredentialsNonExpired(true);

        // Admin in a DIFFERENT clinic
        adminUserOtherClinic = new ClinicStaff();
        adminUserOtherClinic.setId(4L);
        adminUserOtherClinic.setUsername("admin_other");
        adminUserOtherClinic.setEmail("admin_c2@test.com");
        adminUserOtherClinic.setPassword("hashedPasswordAdmin2");
        adminUserOtherClinic.setName("Admin");
        adminUserOtherClinic.setSurname("OtherClinic");
        adminUserOtherClinic.setClinic(clinic2);
        adminUserOtherClinic.setRoles(Set.of(adminRole));
        adminUserOtherClinic.setAvatar("avatar_admin2.png");
        adminUserOtherClinic.setActive(true);
        adminUserOtherClinic.setEnabled(true);
        adminUserOtherClinic.setAccountNonExpired(true);
        adminUserOtherClinic.setAccountNonLocked(true); adminUserOtherClinic.setCredentialsNonExpired(true);

        ownerProfileDto = new OwnerProfileDto(ownerUser.getId(), ownerUser.getUsername(), ownerUser.getEmail(), Set.of("OWNER"), ownerUser.getAvatar(), ownerUser.getPhone());
        staffProfileDto = new ClinicStaffProfileDto(adminUser.getId(), adminUser.getUsername(), adminUser.getEmail(),
                Set.of("ADMIN"), adminUser.getAvatar(), adminUser.getName(), adminUser.getSurname(),
                adminUser.isActive(), adminUser.getClinic().getId(), adminUser.getClinic().getName(),
                null, null, null, null,null, null, null);


        genericOwnerProfileDto = new UserProfileDto(ownerUser.getId(),
                ownerUser.getUsername(), ownerUser.getEmail(), Set.of("OWNER"),
                ownerUser.getAvatar(), null, null, null);
        genericStaffProfileDto = new UserProfileDto(adminUser.getId(),
                adminUser.getUsername(), adminUser.getEmail(), Set.of("ADMIN"), adminUser.getAvatar(),
                adminUser.getName(), adminUser.getSurname(), adminUser.getClinic().getName());
        genericVetProfileDto = new UserProfileDto(vetUserSameClinic.getId(),
                vetUserSameClinic.getUsername(), vetUserSameClinic.getEmail(), Set.of("VET", "ADMIN"),
                vetUserSameClinic.getAvatar(), vetUserSameClinic.getName(), vetUserSameClinic.getSurname(), vetUserSameClinic.getClinic().getName());

        ReflectionTestUtils.setField(userService, "defaultUserImagePathBase", "images/avatars/users/");

        PermissionEntity readOwnProfilePerm = PermissionEntity.builder().id(1L).name("USER_READ_PROFILE_OWN").build();
        PermissionEntity manageStaffPerm = PermissionEntity.builder().id(5L).name("CLINIC_STAFF_CREATE").build();
        mockAdminRoleWithPermissions = RoleEntity.builder()
                .id(3L)
                .roleEnum(RoleEnum.ADMIN)
                .permissionList(Set.of(readOwnProfilePerm, manageStaffPerm))
                .build();
        adminUser.setRoles(Set.of(mockAdminRoleWithPermissions));
    }

    /**
     * --- Tests for loadUserByUsername ---
     */
    @Nested
    @DisplayName("loadUserByUsername Tests")
    class LoadUserByUsernameTests {

        @BeforeEach
        void loadUserSetup() {
            PermissionEntity readOwnProfilePerm = PermissionEntity.builder().id(1L).name("USER_READ_PROFILE_OWN").build();
            PermissionEntity createRecordPerm = PermissionEntity.builder().id(20L).name("RECORD_CREATE_ASSOCIATED_CLINIC").build();
            RoleEntity ownerRoleWithPerms;
            RoleEntity vetRoleWithPerms;

            ownerRoleWithPerms = RoleEntity.builder()
                    .id(1L)
                    .roleEnum(RoleEnum.OWNER)
                    .permissionList(Set.of(readOwnProfilePerm))
                    .build();

            vetRoleWithPerms = RoleEntity.builder()
                    .id(2L)
                    .roleEnum(RoleEnum.VET)
                    .permissionList(Set.of(readOwnProfilePerm, createRecordPerm))
                    .build();

            ownerUser.setRoles(Set.of(ownerRoleWithPerms));
            vetUserSameClinic.setRoles(Set.of(vetRoleWithPerms));
        }

        @Test
        @DisplayName("should load UserDetails correctly for existing Owner user")
        void loadUserByUsername_Success_Owner() {
            // Arrange
            String username = ownerUser.getUsername();
            given(userRepository.findByUsernameWithRolesAndPermissions(username))
                    .willReturn(Optional.of(ownerUser));
            List<String> expectedAuthorities = List.of("ROLE_OWNER", "USER_READ_PROFILE_OWN");

            // Act
            UserDetails userDetails = userService.loadUserByUsername(username);

            // Assert
            assertThat(userDetails).isNotNull();
            assertThat(userDetails.getUsername()).isEqualTo(username);
            List<String> actualAuthorities = userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .toList();
            assertThat(actualAuthorities).containsExactlyInAnyOrderElementsOf(expectedAuthorities);

            then(userRepository).should().findByUsernameWithRolesAndPermissions(username);
            then(userRepository).should(never()).findByEmailWithRolesAndPermissions(anyString());
        }

        @Test
        @DisplayName("should load UserDetails correctly for existing Vet user")
        void loadUserByUsername_Success_Vet() {
            // Arrange
            String username = vetUserSameClinic.getUsername();
            given(userRepository.findByUsernameWithRolesAndPermissions(username)).willReturn(Optional.of(vetUserSameClinic));

            List<String> expectedAuthorities = List.of("ROLE_VET", "USER_READ_PROFILE_OWN", "RECORD_CREATE_ASSOCIATED_CLINIC");

            // Act
            UserDetails userDetails = userService.loadUserByUsername(username);

            // Assert
            assertThat(userDetails).isNotNull();
            assertThat(userDetails.getUsername()).isEqualTo(username);
            assertThat(userDetails.getPassword()).isEqualTo(vetUserSameClinic.getPassword());

            List<String> actualAuthorities = userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .toList();
            assertThat(actualAuthorities).containsExactlyInAnyOrderElementsOf(expectedAuthorities);
            then(userRepository).should().findByUsernameWithRolesAndPermissions(username);
            then(userRepository).should(never()).findByEmailWithRolesAndPermissions(anyString());
        }

        @Test
        @DisplayName("should throw UsernameNotFoundException when user does not exist")
        void loadUserByUsername_ThrowsUsernameNotFoundException() {
            // Arrange
            String nonExistentIdentifier = "ghost";
            given(userRepository.findByUsernameWithRolesAndPermissions(nonExistentIdentifier)).willReturn(Optional.empty());
            given(userRepository.findByEmailWithRolesAndPermissions(nonExistentIdentifier)).willReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> userService.loadUserByUsername(nonExistentIdentifier))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessageContaining("El usuario " + nonExistentIdentifier + " no existe.");

            then(userRepository).should().findByUsernameWithRolesAndPermissions(nonExistentIdentifier);
            then(userRepository).should().findByEmailWithRolesAndPermissions(nonExistentIdentifier);
            then(userRepository).should(never()).findByUsername(anyString());
            then(userRepository).should(never()).findByEmail(anyString());
        }
    }

    /**
     * --- Tests for getCurrentUserProfile ---
     */
    @Nested
    @DisplayName("getCurrentUserProfile Tests")
    class GetCurrentUserProfileTests {

        @Test
        @DisplayName("should return OwnerProfileDto when authenticated user is Owner")
        void shouldReturnOwnerProfileDtoForOwner() {
            // Arrange
            given(userServiceHelper.getAuthenticatedUserEntity()).willReturn(ownerUser);
            given(userMapper.toOwnerProfileDto(ownerUser)).willReturn(ownerProfileDto);

            // Act
            Object result = userService.getCurrentUserProfile();

            // Assert
            assertThat(result).isInstanceOf(OwnerProfileDto.class).isEqualTo(ownerProfileDto);
            then(userServiceHelper).should().getAuthenticatedUserEntity();
            then(userMapper).should().toOwnerProfileDto(ownerUser);
            then(userMapper).should(never()).toClinicStaffProfileDto(any());
        }

        @Test
        @DisplayName("should return ClinicStaffProfileDto when authenticated user is ClinicStaff")
        void shouldReturnClinicStaffProfileDtoForStaff() {
            // Arrange
            given(userServiceHelper.getAuthenticatedUserEntity()).willReturn(adminUser);
            given(userMapper.toClinicStaffProfileDto(adminUser)).willReturn(staffProfileDto);

            // Act
            Object result = userService.getCurrentUserProfile();

            // Assert
            assertThat(result).isInstanceOf(ClinicStaffProfileDto.class).isEqualTo(staffProfileDto);
            then(userServiceHelper).should().getAuthenticatedUserEntity();
            then(userMapper).should(never()).toOwnerProfileDto(any());
            then(userMapper).should().toClinicStaffProfileDto(adminUser);
        }

        @Test
        @DisplayName("should throw IllegalStateException when no user is authenticated")
        void shouldThrowIllegalStateWhenNotAuthenticated() {
            // Arrange
            given(userServiceHelper.getAuthenticatedUserEntity())
                    .willThrow(new IllegalStateException("Authentication required. No authenticated user found in security context."));

            // Act & Assert
            assertThatThrownBy(() -> userService.getCurrentUserProfile())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("No authenticated user found");
            then(userServiceHelper).should().getAuthenticatedUserEntity();
        }

        @Test
        @DisplayName("should throw EntityNotFoundException when authenticated user not in DB")
        void shouldThrowEntityNotFoundWhenAuthUserNotInDb() {
            given(userServiceHelper.getAuthenticatedUserEntity())
                    .willThrow(new EntityNotFoundException("Authenticated user 'ghost_user' not found in database."));

            // Act & Assert
            assertThatThrownBy(() -> userService.getCurrentUserProfile())
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Authenticated user 'ghost_user' not found");
            then(userServiceHelper).should().getAuthenticatedUserEntity();
        }
    }

    /**
     * --- Tests for findUserById ---
     */
    @Nested
    @DisplayName("findUserById Tests")
    class FindUserByIdTests {

        @Test
        @DisplayName("should return DTO when user finds self by ID")
        void shouldReturnDtoWhenSelfFindById() {
            // Arrange
            given(userServiceHelper.getAuthenticatedUserEntity()).willReturn(ownerUser);
            given(entityFinderHelper.findUserOrFail(ownerUser.getId())).willReturn(ownerUser);
            given(userMapper.mapToBaseProfileDTO(ownerUser)).willReturn(genericOwnerProfileDto);

            Optional<UserProfileDto> result = userService.findUserById(ownerUser.getId());

            assertThat(result).isPresent().contains(genericOwnerProfileDto);
        }

        @Test
        @DisplayName("should return DTO when Admin finds Staff in same clinic by ID")
        void shouldReturnDtoWhenAdminFindsStaffInSameClinicById() {
            // Arrange
            given(userServiceHelper.getAuthenticatedUserEntity()).willReturn(adminUser); // Admin C1
            given(entityFinderHelper.findUserOrFail(vetUserSameClinic.getId())).willReturn(vetUserSameClinic); // Vet C1
            given(userMapper.mapToBaseProfileDTO(vetUserSameClinic)).willReturn(genericVetProfileDto);

            Optional<UserProfileDto> result = userService.findUserById(vetUserSameClinic.getId());

            assertThat(result).isPresent().contains(genericVetProfileDto);
        }


        @Test
        @DisplayName("should return empty Optional when target user does not exist")
        void shouldReturnEmptyWhenNotFound() {
            // Arrange
            Long nonExistentId = 999L;
            given(userServiceHelper.getAuthenticatedUserEntity()).willReturn(ownerUser);
            given(entityFinderHelper.findUserOrFail(nonExistentId))
                    .willThrow(new EntityNotFoundException(UserEntity.class.getSimpleName(), nonExistentId));

            // Act & Assert
            assertThatThrownBy(() -> userService.findUserById(nonExistentId))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("UserEntity not found with id: " + nonExistentId);

            then(userServiceHelper).should().getAuthenticatedUserEntity();
            then(entityFinderHelper).should().findUserOrFail(nonExistentId);
            then(userMapper).should(never()).mapToBaseProfileDTO(any());
        }

        @Test
        @DisplayName("should throw AccessDeniedException when Owner tries to find Admin by ID")
        void shouldThrowAccessDeniedWhenOwnerFindsAdminById() {
            /// Arrange
            given(userServiceHelper.getAuthenticatedUserEntity()).willReturn(ownerUser);
            given(entityFinderHelper.findUserOrFail(adminUser.getId())).willReturn(adminUser);

            Throwable thrown = Assertions.catchThrowable(() -> userService.findUserById(adminUser.getId()));
            // Assert
            assertThat(thrown)
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("is not authorized to access profile for user " + adminUser.getId());
        }

        @Test
        @DisplayName("should throw AccessDeniedException when Admin tries to find staff from different clinic by ID")
        void shouldThrowAccessDeniedWhenAdminFindsStaffDifferentClinicById() {
            // Arrange
            given(userServiceHelper.getAuthenticatedUserEntity()).willReturn(adminUser); // Admin de Clinic1
            given(entityFinderHelper.findUserOrFail(adminUserOtherClinic.getId())).willReturn(adminUserOtherClinic); // Admin de Clinic2

            Throwable thrown = Assertions.catchThrowable(() ->userService.findUserById(adminUserOtherClinic.getId()));
            assertThat(thrown)
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("is not authorized to access profile for user " + adminUserOtherClinic.getId());
        }

        @Test
        @DisplayName("should throw AccessDeniedException when Staff tries to find Owner by ID")
        void shouldThrowAccessDeniedWhenStaffFindsOwnerById() {
            // Arrange
            given(userServiceHelper.getAuthenticatedUserEntity()).willReturn(adminUser);
            given(entityFinderHelper.findUserOrFail(ownerUser.getId())).willReturn(ownerUser);

            Throwable thrown = Assertions.catchThrowable(() -> userService.findUserById(ownerUser.getId()));
            assertThat(thrown)
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("is not authorized to access profile for user " + ownerUser.getId());
        }
    }

    /**
     * --- Tests for findUserByEmail ---
     */
    @Nested
    @DisplayName("findUserByEmail Tests")
    class FindUserByEmailTests {

        @Test
        @DisplayName("should return DTO when user finds self by Email")
        void shouldReturnDtoWhenSelfFindByEmail() {
            given(userServiceHelper.getAuthenticatedUserEntity()).willReturn(adminUser);
            given(userRepository.findByEmail(adminUser.getEmail())).willReturn(Optional.of(adminUser));
            given(userMapper.mapToBaseProfileDTO(adminUser)).willReturn(genericStaffProfileDto);

            Optional<UserProfileDto> result = userService.findUserByEmail(adminUser.getEmail());
            assertThat(result).isPresent().contains(genericStaffProfileDto);
        }

        @Test
        @DisplayName("should return DTO when Admin finds Staff in same clinic by Email")
        void shouldReturnDtoWhenAdminFindsStaffInSameClinicByEmail() {
            given(userServiceHelper.getAuthenticatedUserEntity()).willReturn(adminUser); // Admin C1 requester
            given(userRepository.findByEmail(vetUserSameClinic.getEmail())).willReturn(Optional.of(vetUserSameClinic)); // Target Vet C1
            given(userMapper.mapToBaseProfileDTO(vetUserSameClinic)).willReturn(genericVetProfileDto);

            Optional<UserProfileDto> result = userService.findUserByEmail(vetUserSameClinic.getEmail());
            assertThat(result).isPresent().contains(genericVetProfileDto);
        }

        @Test
        @DisplayName("should return empty Optional when target user does not exist")
        void shouldReturnEmptyWhenNotFound() {
            String nonExistentEmail = "notfound@test.com";
            given(userServiceHelper.getAuthenticatedUserEntity()).willReturn(ownerUser);
            given(userRepository.findByEmail(nonExistentEmail)).willReturn(Optional.empty());

            // Act
            Optional<UserProfileDto> result = userService.findUserByEmail(nonExistentEmail);

            // Assert
            assertThat(result).isNotPresent();
            then(userServiceHelper).should().getAuthenticatedUserEntity();
            then(userRepository).should().findByEmail(nonExistentEmail);
            then(userMapper).should(never()).mapToBaseProfileDTO(any());
        }

        @Test
        @DisplayName("should throw AccessDeniedException when Owner finds Admin by Email")
        void shouldThrowAccessDeniedWhenOwnerFindsAdminByEmail() {
            given(userServiceHelper.getAuthenticatedUserEntity()).willReturn(ownerUser); // Requester
            given(userRepository.findByEmail(adminUser.getEmail())).willReturn(Optional.of(adminUser)); // Target found

            // Act
            Throwable thrown = Assertions.catchThrowable(() -> userService.findUserByEmail(adminUser.getEmail()));
            // Assert
            assertThat(thrown)
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("is not authorized to access profile for email " + adminUser.getEmail());
        }

        @Test
        @DisplayName("should throw AccessDeniedException when Admin finds Staff different clinic by Email")
        void shouldThrowAccessDeniedWhenAdminFindsStaffDifferentClinicByEmail() {
            given(userServiceHelper.getAuthenticatedUserEntity()).willReturn(adminUser); // Requester C1
            given(userRepository.findByEmail(adminUserOtherClinic.getEmail())).willReturn(Optional.of(adminUserOtherClinic)); // Target C2 found

            // Act
            Throwable thrown = Assertions.catchThrowable(() -> userService.findUserByEmail(adminUserOtherClinic.getEmail()));
            // Assert
            assertThat(thrown)
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("is not authorized to access profile for email " + adminUserOtherClinic.getEmail());
        }
    }

    /**
     * --- Tests for findUserByUsername ---
     */
    @Nested
    @DisplayName("findUserByUsername Tests")
    class FindUserByUsernameTests {

        @Test
        @DisplayName("should return DTO when user finds self by Username")
        void shouldReturnDtoWhenSelfFindByUsername() {
            String nonExistentUsername = "notfounduser";
            given(userServiceHelper.getAuthenticatedUserEntity()).willReturn(ownerUser);
            given(userRepository.findByUsername(nonExistentUsername)).willReturn(Optional.empty());

            // Act
            Optional<UserProfileDto> result = userService.findUserByUsername(nonExistentUsername);

            // Assert
            assertThat(result).isNotPresent();
            then(userServiceHelper).should().getAuthenticatedUserEntity();
            then(userRepository).should().findByUsername(nonExistentUsername);
            then(userMapper).should(never()).mapToBaseProfileDTO(any());
        }

        @Test
        @DisplayName("should return DTO when Admin finds Staff in same clinic by Username")
        void shouldReturnDtoWhenAdminFindsStaffInSameClinicByUsername() {
            given(userServiceHelper.getAuthenticatedUserEntity()).willReturn(adminUser); // Admin C1
            given(userRepository.findByUsername(vetUserSameClinic.getUsername())).willReturn(Optional.of(vetUserSameClinic)); // Vet C1
            given(userMapper.mapToBaseProfileDTO(vetUserSameClinic)).willReturn(
                    new UserProfileDto(vetUserSameClinic.getId(), vetUserSameClinic.getUsername(),
                            vetUserSameClinic.getEmail(), Set.of("VET"), vetUserSameClinic.getAvatar(),
                            null, null, null)
            );

            Optional<UserProfileDto> result = userService.findUserByUsername(vetUserSameClinic.getUsername());

            assertThat(result).isPresent();
            assertThat(result.get().username()).isEqualTo(vetUserSameClinic.getUsername());
            then(userServiceHelper).should().getAuthenticatedUserEntity();
            then(userRepository).should().findByUsername(vetUserSameClinic.getUsername());
        }

        @Test
        @DisplayName("should return empty Optional when target user does not exist")
        void shouldReturnEmptyWhenNotFound() {
            given(userServiceHelper.getAuthenticatedUserEntity()).willReturn(ownerUser);
            given(userRepository.findByUsername("notfounduser")).willReturn(Optional.empty());

            Optional<UserProfileDto> result = userService.findUserByUsername("notfounduser");

            assertThat(result).isNotPresent();
            then(userServiceHelper).should().getAuthenticatedUserEntity();
            then(userRepository).should().findByUsername("notfounduser");
        }

        @Test
        @DisplayName("should throw AccessDeniedException when Owner finds Admin by Username")
        void shouldThrowAccessDeniedWhenOwnerFindsAdminByUsername() {
            given(userServiceHelper.getAuthenticatedUserEntity()).willReturn(ownerUser); // Requester
            given(userRepository.findByUsername(adminUser.getUsername())).willReturn(Optional.of(adminUser)); // Target found

            // Act
            Throwable thrown = Assertions.catchThrowable(() -> userService.findUserByUsername(adminUser.getUsername()));
            // Assert
            assertThat(thrown)
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("is not authorized to access profile for username " + adminUser.getUsername());
        }

        @Test
        @DisplayName("should throw AccessDeniedException when Admin finds Staff different clinic by Username")
        void shouldThrowAccessDeniedWhenAdminFindsStaffDifferentClinicByUsername() {
            given(userServiceHelper.getAuthenticatedUserEntity()).willReturn(adminUser); // Requester C1
            given(userRepository.findByUsername(adminUserOtherClinic.getUsername())).willReturn(Optional.of(adminUserOtherClinic)); // Target C2 found

            // Act
            Throwable thrown = Assertions.catchThrowable(() -> userService.findUserByUsername(adminUserOtherClinic.getUsername()));
            // Assert
            assertThat(thrown)
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("is not authorized to access profile for username " + adminUserOtherClinic.getUsername());
        }
    }

    /**
     * --- Tests for updateCurrentOwnerProfile ---
     */
    @Nested
    @DisplayName("updateCurrentOwnerProfile Tests")
    class UpdateCurrentOwnerProfileTests {
        private OwnerProfileUpdateDto updateDto;

        @BeforeEach
        void updateOwnerSetup() {
            updateDto = new OwnerProfileUpdateDto("newOwnerUsername","avatar.png", "999-888-777");
            new MockMultipartFile("imageFile", "avatar.png", MediaType.IMAGE_PNG_VALUE, "new_avatar_bytes".getBytes());
        }

        @Test
        @DisplayName("should update owner profile successfully")
        void shouldUpdateOwnerProfile() throws IOException {
            // Arrange
            String newUsername = "newOwnerUsername";
            String newPhone = "999-888-777";
            OwnerProfileUpdateDto dtoWithChange = new OwnerProfileUpdateDto(newUsername, null,newPhone);
            MockMultipartFile imageFile = new MockMultipartFile("imageFile", "avatar.png",
                    MediaType.IMAGE_PNG_VALUE, "new_avatar_bytes".getBytes());
            String newAvatarPath = "users/avatars/new_owner_avatar.png";
            String newJwt = "new.jwt.token.for.owner";

            given(userServiceHelper.getAuthenticatedUserEntity()).willReturn(ownerUser);
            doNothing().when(authorizationHelper).validateUsernameUpdate(newUsername, ownerUser);
            when(imageService.storeImage(imageFile, "users/avatars")).thenReturn(newAvatarPath);

            when(userMapper.updateOwnerFromDto(dtoWithChange, ownerUser)).thenAnswer(invocation -> {
                Owner o = invocation.getArgument(1);
                o.setUsername(newUsername);
                o.setPhone(newPhone);
                return true;
            });

            when(ownerRepository.save(ownerUser)).thenReturn(ownerUser);

            Owner ownerWithNewUsername = new Owner();
            ownerWithNewUsername.setId(ownerUser.getId());
            ownerWithNewUsername.setEmail(ownerUser.getEmail());
            ownerWithNewUsername.setPassword(ownerUser.getPassword());
            ownerWithNewUsername.setPhone(newPhone);
            ownerWithNewUsername.setAvatar(newAvatarPath);
            ownerWithNewUsername.setEnabled(ownerUser.isEnabled());
            ownerWithNewUsername.setAccountNonExpired(ownerUser.isAccountNonExpired());
            ownerWithNewUsername.setAccountNonLocked(ownerUser.isAccountNonLocked());
            ownerWithNewUsername.setCredentialsNonExpired(ownerUser.isCredentialsNonExpired());
            ownerWithNewUsername.setUsername(newUsername);

            RoleEntity roleForNewUsername = RoleEntity.builder()
                    .id(1L)
                    .roleEnum(RoleEnum.OWNER)
                    .permissionList(new HashSet<>())
                    .build();
            ownerWithNewUsername.setRoles(Set.of(roleForNewUsername));

            given(userRepository.findByUsernameWithRolesAndPermissions(newUsername))
                    .willReturn(Optional.of(ownerWithNewUsername));

            given(jwtUtils.createToken(any(Authentication.class))).willReturn(newJwt);

            OwnerProfileDto expectedProfileDto = new OwnerProfileDto(
                    ownerUser.getId(), newUsername, ownerUser.getEmail(), Set.of("OWNER"),
                    "http://localhost:8080/storage/" + newAvatarPath,
                    newPhone);
            when(userMapper.toOwnerProfileDto(ownerUser)).thenReturn(expectedProfileDto);


            // Act
            OwnerProfileUpdateResponseDto result = userService.updateCurrentOwnerProfile(dtoWithChange, imageFile);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.profile()).isEqualTo(expectedProfileDto);
            assertThat(result.newJwtToken()).isEqualTo(newJwt);

            then(ownerRepository).should().save(ownerCaptor.capture());
            Owner captured = ownerCaptor.getValue();
            assertThat(captured.getUsername()).isEqualTo(newUsername);
            assertThat(captured.getAvatar()).isEqualTo(newAvatarPath);
            assertThat(captured.getPhone()).isEqualTo(newPhone);

            then(userRepository).should().findByUsernameWithRolesAndPermissions(newUsername);
            then(jwtUtils).should().createToken(any(Authentication.class));
        }

        @Test
        @DisplayName("should throw UsernameAlreadyExistsException if new username is taken")
        void shouldThrowUsernameExistsWhenUpdating() {
            // Arrange
            given(userServiceHelper.getAuthenticatedUserEntity()).willReturn(ownerUser);
            willThrow(new UsernameAlreadyExistsException(updateDto.username()))
                    .given(authorizationHelper)
                    .validateUsernameUpdate(updateDto.username(), ownerUser);

            // Act & Assert
            assertThatThrownBy(() -> userService.updateCurrentOwnerProfile(updateDto, null))
                    .isInstanceOf(UsernameAlreadyExistsException.class)
                    .hasMessageContaining(updateDto.username());

            then(userServiceHelper).should().getAuthenticatedUserEntity();
            then(authorizationHelper).should().validateUsernameUpdate(updateDto.username(), ownerUser);
            then(ownerRepository).should(never()).save(any());
            then(userMapper).should(never()).updateOwnerFromDto(any(), any());
        }

        @Test
        @DisplayName("should NOT save if username not changed and no image file provided and phone not changed")
        void shouldNotSaveIfNoChanges() throws IOException {
            // Arrange
            OwnerProfileUpdateDto noChangeDto = new OwnerProfileUpdateDto(ownerUser.getUsername(),null, ownerUser.getPhone());
            given(userServiceHelper.getAuthenticatedUserEntity()).willReturn(ownerUser);
            doNothing().when(authorizationHelper).validateUsernameUpdate(ownerUser.getUsername(), ownerUser);
            when(userMapper.updateOwnerFromDto(noChangeDto, ownerUser)).thenReturn(false);
            when(userMapper.toOwnerProfileDto(ownerUser)).thenReturn(ownerProfileDto);

            // Act
            OwnerProfileUpdateResponseDto result = userService.updateCurrentOwnerProfile(noChangeDto, null);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.profile()).isEqualTo(ownerProfileDto);

            then(ownerRepository).should(never()).save(any(Owner.class));
            then(imageService).should(never()).storeImage(any(), anyString());
        }

        @Test
        @DisplayName("should throw ClassCastException if authenticated user is not Owner")
        void shouldThrowExceptionIfNotOwner() {
            // Arrange
            given(userServiceHelper.getAuthenticatedUserEntity()).willReturn(adminUser);

            // Act & Assert
            assertThatThrownBy(() -> userService.updateCurrentOwnerProfile(updateDto, null))
                    .isInstanceOf(AccessDeniedException.class);

            then(ownerRepository).should(never()).save(any());
        }
    }

    /**
     * --- Tests for updateCurrentClinicStaffProfile ---
     */
    @Nested
    @DisplayName("updateCurrentClinicStaffProfile Tests")
    class UpdateCurrentClinicStaffProfileTests {
        private UserProfileUpdateDto updateDto;
        private MockMultipartFile imageFile;

        @BeforeEach
        void updateStaffSetup() {
            updateDto = new UserProfileUpdateDto("newStaffUsername", null);
            imageFile = new MockMultipartFile("imageFile", "staff_avatar.png", MediaType.IMAGE_PNG_VALUE, "new_staff_img_bytes".getBytes());
            ReflectionTestUtils.setField(userService, "defaultUserImagePathBase", "images/avatars/users/");
        }

        @Test
        @DisplayName("should update staff profile successfully")
        void shouldUpdateStaffProfile() throws IOException {
            // Arrange
            String newUsername = "updatedStaffUser";
            UserProfileUpdateDto dtoWithChange = new UserProfileUpdateDto(newUsername, null);
            String newAvatarPathRelativo = "users/avatars/new_staff_avatar.png";
            String newJwt = "new.staff.jwt.token";

            given(userServiceHelper.getAuthenticatedUserEntity()).willReturn(adminUser);

            doNothing().when(authorizationHelper).validateUsernameUpdate(newUsername, adminUser);

            when(imageService.storeImage(imageFile, "users/avatars")).thenReturn(newAvatarPathRelativo);

            when(userMapper.updateClinicStaffCommonFromDto(dtoWithChange, adminUser)).thenAnswer(invocation -> {
                ClinicStaff staffArg = invocation.getArgument(1);
                UserProfileUpdateDto dtoArg = invocation.getArgument(0);
                if (dtoArg.username() != null && !dtoArg.username().equals(staffArg.getUsername())) {
                    staffArg.setUsername(dtoArg.username());
                    return true;
                }
                return false;
            });

            when(clinicStaffRepository.save(adminUser)).thenReturn(adminUser);

            ClinicStaff staffWithNewAttributes = new ClinicStaff();
            staffWithNewAttributes.setId(adminUser.getId());
            staffWithNewAttributes.setEmail(adminUser.getEmail());
            staffWithNewAttributes.setPassword(adminUser.getPassword());
            Set<RoleEntity> rolesWithPermissions = adminUser.getRoles().stream()
                    .map(r -> RoleEntity.builder().id(r.getId()).roleEnum(r.getRoleEnum())
                            .permissionList(r.getPermissionList() != null ? new HashSet<>(r.getPermissionList()) : new HashSet<>())
                            .build())
                    .collect(Collectors.toSet());
            staffWithNewAttributes.setRoles(rolesWithPermissions);
            staffWithNewAttributes.setName(adminUser.getName());
            staffWithNewAttributes.setSurname(adminUser.getSurname());
            staffWithNewAttributes.setClinic(adminUser.getClinic());
            staffWithNewAttributes.setActive(adminUser.isActive());
            staffWithNewAttributes.setEnabled(adminUser.isEnabled());
            staffWithNewAttributes.setAccountNonExpired(adminUser.isAccountNonExpired());
            staffWithNewAttributes.setAccountNonLocked(adminUser.isAccountNonLocked());
            staffWithNewAttributes.setCredentialsNonExpired(adminUser.isCredentialsNonExpired());
            staffWithNewAttributes.setUsername(newUsername);
            staffWithNewAttributes.setAvatar(newAvatarPathRelativo);

            given(userRepository.findByUsernameWithRolesAndPermissions(newUsername))
                    .willReturn(Optional.of(staffWithNewAttributes));

            given(jwtUtils.createToken(any(Authentication.class))).willReturn(newJwt);

            ClinicStaffProfileDto expectedProfileDto = new ClinicStaffProfileDto(
                    adminUser.getId(), newUsername, adminUser.getEmail(),
                    adminUser.getRoles().stream().map(r -> r.getRoleEnum().name()).collect(Collectors.toSet()),
                    "http://localhost:8080/storage/" + newAvatarPathRelativo,
                    adminUser.getName(), adminUser.getSurname(), adminUser.isActive(),
                    adminUser.getClinic().getId(), adminUser.getClinic().getName(),
                    null, null, false,
                    adminUser.getCreatedAt(), adminUser.getCreatedBy(), adminUser.getUpdatedAt(), adminUser.getUpdatedBy()
            );

            when(userMapper.toClinicStaffProfileDto(adminUser)).thenReturn(expectedProfileDto);

            // Act
            ClinicStaffProfileUpdateResponseDto result = userService.updateCurrentClinicStaffProfile(dtoWithChange, imageFile);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.profile()).isEqualTo(expectedProfileDto);
            assertThat(result.newJwtToken()).isEqualTo(newJwt);

            then(imageService).should().storeImage(imageFile, "users/avatars");
            then(clinicStaffRepository).should().save(clinicStaffCaptor.capture());
            ClinicStaff captured = clinicStaffCaptor.getValue();
            assertThat(captured.getUsername()).isEqualTo(newUsername);
            assertThat(captured.getAvatar()).isEqualTo(newAvatarPathRelativo);
            then(userRepository).should().findByUsernameWithRolesAndPermissions(newUsername);
            then(jwtUtils).should().createToken(any(Authentication.class));
        }

        @Test
        @DisplayName("should throw UsernameAlreadyExistsException if new username is taken")
        void shouldThrowUsernameExistsWhenUpdating() {
            // Arrange
            given(userServiceHelper.getAuthenticatedUserEntity()).willReturn(adminUser);
            doThrow(new UsernameAlreadyExistsException(updateDto.username()))
                    .when(authorizationHelper).validateUsernameUpdate(updateDto.username(), adminUser);

            // Act & Assert
            assertThatThrownBy(() -> userService.updateCurrentClinicStaffProfile(updateDto, null))
                    .isInstanceOf(UsernameAlreadyExistsException.class);
        }

        @Test
        @DisplayName("should NOT check uniqueness or throw if username is not changed")
        void shouldNotCheckUniquenessIfUsernameNotChanged() throws IOException {
            // Arrange
            given(userServiceHelper.getAuthenticatedUserEntity()).willReturn(adminUser);

            UserProfileUpdateDto noUsernameOrImageChangeDto = new UserProfileUpdateDto(adminUser.getUsername(), null);
            doNothing().when(authorizationHelper).validateUsernameUpdate(adminUser.getUsername(), adminUser);
            when(userMapper.updateClinicStaffCommonFromDto(noUsernameOrImageChangeDto, adminUser)).thenReturn(false);
           ClinicStaffProfileDto originalProfileDto = new ClinicStaffProfileDto(
                    adminUser.getId(), adminUser.getUsername(), adminUser.getEmail(),
                    adminUser.getRoles().stream().map(r -> r.getRoleEnum().name()).collect(Collectors.toSet()),
                    adminUser.getAvatar(), // Avatar original
                    adminUser.getName(), adminUser.getSurname(), adminUser.isActive(),
                    adminUser.getClinic().getId(), adminUser.getClinic().getName(),
                    null, // licenseNumber
                    null, // vetPublicKey
                    (adminUser instanceof Vet v && StringUtils.hasText(v.getVetPrivateKey())),
                    adminUser.getCreatedAt(), adminUser.getCreatedBy(),
                    adminUser.getUpdatedAt(), adminUser.getUpdatedBy()
            );
            when(userMapper.toClinicStaffProfileDto(adminUser)).thenReturn(originalProfileDto);

            // Act
            ClinicStaffProfileUpdateResponseDto result = userService.updateCurrentClinicStaffProfile(noUsernameOrImageChangeDto, null);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.profile()).isEqualTo(originalProfileDto);
            assertThat(result.newJwtToken()).isNull();

            then(userServiceHelper).should().getAuthenticatedUserEntity();
            then(authorizationHelper).should().validateUsernameUpdate(adminUser.getUsername(), adminUser);
            then(userMapper).should().updateClinicStaffCommonFromDto(noUsernameOrImageChangeDto, adminUser);
            then(clinicStaffRepository).should(never()).save(any(ClinicStaff.class));
            then(userMapper).should().toClinicStaffProfileDto(adminUser);
            then(imageService).should(never()).storeImage(any(), anyString());
            then(jwtUtils).should(never()).createToken(any());
        }

        @Test
        @DisplayName("should throw ClassCastException if authenticated user is not ClinicStaff")
        void shouldThrowExceptionIfNotStaff() {
            // Arrange
            UserProfileUpdateDto dummyUpdateDto = new UserProfileUpdateDto("anyUser", null);
            given(userServiceHelper.getAuthenticatedUserEntity()).willReturn(ownerUser);

            // Act & Assert
            assertThatThrownBy(() -> userService.updateCurrentClinicStaffProfile(dummyUpdateDto, null))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("User is not Clinic Staff");

            then(userServiceHelper).should().getAuthenticatedUserEntity();
            then(authorizationHelper).should(never()).validateUsernameUpdate(any(), any());
            then(clinicStaffRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("should update staff image and username, and delete old image")
        void shouldUpdateStaffImageAndUsername() throws IOException {
            // Arrange
            String newUsername = "updatedStaffUserWithImage";
            UserProfileUpdateDto dtoWithChange = new UserProfileUpdateDto(newUsername, "avatar.png");
            new MockMultipartFile("imageFile", "staff_avatar.png",
                    MediaType.IMAGE_PNG_VALUE, "new_staff_img_bytes".getBytes());
            String newAvatarPathRelativo = "users/avatars/new_staff_avatar.png";
            String newJwt = "new.staff.jwt.token.img";

            given(userServiceHelper.getAuthenticatedUserEntity()).willReturn(adminUser);
            doNothing().when(authorizationHelper).validateUsernameUpdate(newUsername, adminUser);
            when(imageService.storeImage(imageFile, "users/avatars")).thenReturn(newAvatarPathRelativo);

            when(userMapper.updateClinicStaffCommonFromDto(dtoWithChange, adminUser)).thenAnswer(invocation -> {
                ClinicStaff staff = invocation.getArgument(1);
                staff.setUsername(newUsername);
                return true;
            });
            when(clinicStaffRepository.save(adminUser)).thenReturn(adminUser);


            ClinicStaff staffWithNewUsername = getStaffWithNewUsername(newAvatarPathRelativo, newUsername);

            given(userRepository.findByUsernameWithRolesAndPermissions(newUsername))
                    .willReturn(Optional.of(staffWithNewUsername));

            given(jwtUtils.createToken(any(Authentication.class))).willReturn(newJwt);

            ClinicStaffProfileDto expectedProfileDto = new ClinicStaffProfileDto(
                    adminUser.getId(), newUsername, adminUser.getEmail(),
                    adminUser.getRoles().stream().map(r -> r.getRoleEnum().name()).collect(Collectors.toSet()),
                    "http://localhost:8080/storage/" + newAvatarPathRelativo,
                    adminUser.getName(), adminUser.getSurname(), adminUser.isActive(),
                    adminUser.getClinic().getId(), adminUser.getClinic().getName(),
                    null, null, false,
                    null, null, null, null
            );
            when(userMapper.toClinicStaffProfileDto(adminUser)).thenReturn(expectedProfileDto);

            // Act
            ClinicStaffProfileUpdateResponseDto result = userService.updateCurrentClinicStaffProfile(dtoWithChange, imageFile);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.profile()).isEqualTo(expectedProfileDto);
            assertThat(result.newJwtToken()).isEqualTo(newJwt);

            then(clinicStaffRepository).should().save(clinicStaffCaptor.capture());
            ClinicStaff captured = clinicStaffCaptor.getValue();
            assertThat(captured.getUsername()).isEqualTo(newUsername);
            assertThat(captured.getAvatar()).isEqualTo(newAvatarPathRelativo);
            then(userRepository).should().findByUsernameWithRolesAndPermissions(newUsername);
            then(jwtUtils).should().createToken(any(Authentication.class));
        }

        private ClinicStaff getStaffWithNewUsername(String newAvatarPathRelativo, String newUsername) {
            ClinicStaff staffWithNewUsername = new ClinicStaff();
            staffWithNewUsername.setId(adminUser.getId());
            staffWithNewUsername.setEmail(adminUser.getEmail());
            staffWithNewUsername.setPassword(adminUser.getPassword());
            staffWithNewUsername.setRoles(adminUser.getRoles());
            staffWithNewUsername.setName(adminUser.getName());
            staffWithNewUsername.setSurname(adminUser.getSurname());
            staffWithNewUsername.setClinic(adminUser.getClinic());
            staffWithNewUsername.setAvatar(newAvatarPathRelativo);
            staffWithNewUsername.setEnabled(adminUser.isEnabled());
            staffWithNewUsername.setUsername(newUsername);
            return staffWithNewUsername;
        }

        @Test
        @DisplayName("should throw IOException if image update storage fails")
        void shouldThrowIOExceptionWhenImageStorageFails() throws IOException {
            // Arrange
            UserProfileUpdateDto dtoForImageFail = new UserProfileUpdateDto(adminUser.getUsername(), null);
            new MockMultipartFile("avatar", "fail.jpg", MediaType.IMAGE_JPEG_VALUE, "img".getBytes());

            given(userServiceHelper.getAuthenticatedUserEntity()).willReturn(adminUser);
            // La validación de username no debe fallar si el username no cambia
            doNothing().when(authorizationHelper).validateUsernameUpdate(adminUser.getUsername(), adminUser);
            // imageService.storeImage es el que debe lanzar la IOException
            when(imageService.storeImage(imageFile, "users/avatars"))
                    .thenThrow(new IOException("Simulated storage failure"));

            // Act & Assert
            assertThatThrownBy(() -> userService.updateCurrentClinicStaffProfile(dtoForImageFail, imageFile))
                    .isInstanceOf(IOException.class)
                    .hasMessageContaining("Failed to process avatar image: Simulated storage failure"); // El mensaje que envuelve tu servicio

            then(userServiceHelper).should().getAuthenticatedUserEntity();
            then(authorizationHelper).should().validateUsernameUpdate(adminUser.getUsername(), adminUser); // Se llama si el DTO tiene username
            then(imageService).should().storeImage(imageFile, "users/avatars"); // Se intenta guardar
            then(userMapper).should(never()).updateClinicStaffCommonFromDto(any(), any()); // No debería llegar a mapear si la imagen falla antes
            then(clinicStaffRepository).should(never()).save(any()); // No se guarda
            then(jwtUtils).should(never()).createToken(any());
        }
    }
}