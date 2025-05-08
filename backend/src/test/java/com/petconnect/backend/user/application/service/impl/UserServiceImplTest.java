package com.petconnect.backend.user.application.service.impl;

import com.petconnect.backend.common.helper.AuthorizationHelper;
import com.petconnect.backend.common.helper.EntityFinderHelper;
import com.petconnect.backend.common.helper.UserHelper;
import com.petconnect.backend.common.service.ImageService;
import com.petconnect.backend.exception.EntityNotFoundException;
import com.petconnect.backend.user.application.dto.ClinicStaffProfileDto;
import com.petconnect.backend.user.application.dto.OwnerProfileDto;
import com.petconnect.backend.user.application.dto.UserProfileDto;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;
import java.util.Optional;
import java.util.Set;

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
    private UserProfileDto genericOwnerDto;
    private UserProfileDto genericStaffDto;

//    @BeforeEach
//    void setUp() {
//        Clinic clinic1 = Clinic.builder().name("Clinic A").build(); clinic1.setId(1L);
//        Clinic clinic2 = Clinic.builder().name("Clinic B").build(); clinic2.setId(2L);
//
//        RoleEntity ownerRole = RoleEntity.builder().roleEnum(RoleEnum.OWNER).build(); ownerRole.setId(1L);
//        RoleEntity adminRole = RoleEntity.builder().roleEnum(RoleEnum.ADMIN).build(); adminRole.setId(3L);
//        RoleEntity vetRole = RoleEntity.builder().roleEnum(RoleEnum.VET).build(); vetRole.setId(2L);
//
//        ownerUser = new Owner();
//        ownerUser.setId(1L);
//        ownerUser.setUsername("testowner");
//        ownerUser.setEmail("owner@test.com");
//        ownerUser.setPassword("hashedPasswordOwner");
//        ownerUser.setPhone("111-222-333");
//        ownerUser.setRoles(Set.of(ownerRole));
//        ownerUser.setAvatar("avatar_owner.png");
//
//        adminUser = new ClinicStaff();
//        adminUser.setId(2L);
//        adminUser.setUsername("testadmin");
//        adminUser.setEmail("admin@test.com");
//        adminUser.setPassword("hashedPasswordAdmin");
//        adminUser.setName("Admin");
//        adminUser.setSurname("Test");
//        adminUser.setClinic(clinic1);
//        adminUser.setRoles(Set.of(adminRole));
//        adminUser.setAvatar("avatar_admin.png");
//        adminUser.setActive(true);
//
//        // Vet in the SAME clinic as adminUser
//        vetUserSameClinic = new Vet();
//        vetUserSameClinic.setId(3L);
//        vetUserSameClinic.setUsername("testvet_c1");
//        vetUserSameClinic.setEmail("vet_c1@test.com");
//        vetUserSameClinic.setPassword("hashedPasswordVet1");
//        vetUserSameClinic.setName("Vet");
//        vetUserSameClinic.setSurname("SameClinic");
//        vetUserSameClinic.setClinic(clinic1);
//        vetUserSameClinic.setRoles(Set.of(vetRole));
//        vetUserSameClinic.setLicenseNumber("VET_C1");
//        vetUserSameClinic.setVetPublicKey("KEY_C1");
//        vetUserSameClinic.setAvatar("avatar_vet.png");
//        vetUserSameClinic.setActive(true);
//
//        // Admin in a DIFFERENT clinic
//        adminUserOtherClinic = new ClinicStaff();
//        adminUserOtherClinic.setId(4L);
//        adminUserOtherClinic.setUsername("admin_other");
//        adminUserOtherClinic.setEmail("admin_c2@test.com");
//        adminUserOtherClinic.setPassword("hashedPasswordAdmin2");
//        adminUserOtherClinic.setName("Admin");
//        adminUserOtherClinic.setSurname("OtherClinic");
//        adminUserOtherClinic.setClinic(clinic2);
//        adminUserOtherClinic.setRoles(Set.of(adminRole));
//        adminUserOtherClinic.setAvatar("avatar_admin2.png");
//        adminUserOtherClinic.setActive(true);
//
//        ownerProfileDto = new OwnerProfileDto(ownerUser.getId(), ownerUser.getUsername(), ownerUser.getEmail(), Set.of("OWNER"), ownerUser.getAvatar(), ownerUser.getPhone());
//        staffProfileDto = new ClinicStaffProfileDto(adminUser.getId(), adminUser.getUsername(), adminUser.getEmail(), Set.of("ADMIN"), adminUser.getAvatar(), adminUser.getName(), adminUser.getSurname(), adminUser.isActive(), adminUser.getClinic().getId(), adminUser.getClinic().getName(), null, null);
//        genericOwnerDto = new UserProfileDto(ownerUser.getId(), ownerUser.getUsername(), ownerUser.getEmail(), Set.of("OWNER"), ownerUser.getAvatar());
//        genericStaffDto = new UserProfileDto(adminUser.getId(), adminUser.getUsername(), adminUser.getEmail(), Set.of("ADMIN"), adminUser.getAvatar());
//    }

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
            given(userMapper.mapToBaseProfileDTO(ownerUser)).willReturn(genericOwnerDto);

            // Act
            Optional<UserProfileDto> result = userService.findUserById(ownerUser.getId());

            // Assert
            assertThat(result).isPresent().contains(genericOwnerDto);
            then(userServiceHelper).should().getAuthenticatedUserEntity();
            then(entityFinderHelper).should().findUserOrFail(ownerUser.getId());
            then(userMapper).should().mapToBaseProfileDTO(ownerUser);
        }

        @Test
        @DisplayName("should return DTO when Admin finds Staff in same clinic by ID")
        void shouldReturnDtoWhenAdminFindsStaffInSameClinicById() {
            // Arrange
            given(userServiceHelper.getAuthenticatedUserEntity()).willReturn(adminUser);
            given(entityFinderHelper.findUserOrFail(vetUserSameClinic.getId())).willReturn(vetUserSameClinic);
            given(userMapper.mapToBaseProfileDTO(vetUserSameClinic)).willReturn(
                    new UserProfileDto(vetUserSameClinic.getId(), vetUserSameClinic.getUsername(), vetUserSameClinic.getEmail(), Set.of("VET"), vetUserSameClinic.getAvatar())
            );

            // Act
            Optional<UserProfileDto> result = userService.findUserById(vetUserSameClinic.getId());

            // Assert
            assertThat(result).isPresent();
            assertThat(result.get().id()).isEqualTo(vetUserSameClinic.getId());
            then(userServiceHelper).should().getAuthenticatedUserEntity();
            then(entityFinderHelper).should().findUserOrFail(vetUserSameClinic.getId()); // Verificar Helper
            then(userMapper).should().mapToBaseProfileDTO(vetUserSameClinic);
        }


        @Test
        @DisplayName("should return empty Optional when target user does not exist")
        void shouldReturnEmptyWhenNotFound() {
            // Arrange
            given(userServiceHelper.getAuthenticatedUserEntity()).willReturn(ownerUser);
            given(entityFinderHelper.findUserOrFail(999L))
                    .willThrow(new EntityNotFoundException(UserEntity.class.getSimpleName(), 999L));

            // Act & Assert: Verificar que se lanza la excepción
            assertThatThrownBy(() -> userService.findUserById(999L))
                    .isInstanceOf(EntityNotFoundException.class);

            then(userServiceHelper).should().getAuthenticatedUserEntity();
            then(entityFinderHelper).should().findUserOrFail(999L);
            then(userMapper).should(never()).mapToBaseProfileDTO(any());
        }

        @Test
        @DisplayName("should throw AccessDeniedException when Owner tries to find Admin by ID")
        void shouldThrowAccessDeniedWhenOwnerFindsAdminById() {
            /// Arrange
            given(userServiceHelper.getAuthenticatedUserEntity()).willReturn(ownerUser);
            given(entityFinderHelper.findUserOrFail(adminUser.getId())).willReturn(adminUser);

            // Act
            Throwable thrown = Assertions.catchThrowable(() ->userService.findUserById(adminUser.getId()));
            // Assert
            assertThat(thrown)
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("is not authorized to access profile for user " + adminUser.getId());

            then(userServiceHelper).should().getAuthenticatedUserEntity();
            then(entityFinderHelper).should().findUserOrFail(adminUser.getId());
            then(userMapper).should(never()).mapToBaseProfileDTO(any());
        }

        @Test
        @DisplayName("should throw AccessDeniedException when Admin tries to find staff from different clinic by ID")
        void shouldThrowAccessDeniedWhenAdminFindsStaffDifferentClinicById() {
            // Arrange
            given(userServiceHelper.getAuthenticatedUserEntity()).willReturn(adminUser);
            given(entityFinderHelper.findUserOrFail(adminUserOtherClinic.getId())).willReturn(adminUserOtherClinic);

            // Act
            Throwable thrown = Assertions.catchThrowable(() -> userService.findUserById(adminUserOtherClinic.getId()));
            // Assert
            assertThat(thrown)
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("is not authorized to access profile for user " + adminUserOtherClinic.getId());

            then(userServiceHelper).should().getAuthenticatedUserEntity();
            then(entityFinderHelper).should().findUserOrFail(adminUserOtherClinic.getId());
            then(userMapper).should(never()).mapToBaseProfileDTO(any());
        }

        @Test
        @DisplayName("should throw AccessDeniedException when Staff tries to find Owner by ID")
        void shouldThrowAccessDeniedWhenStaffFindsOwnerById() {
            // Arrange
            given(userServiceHelper.getAuthenticatedUserEntity()).willReturn(adminUser);
            given(entityFinderHelper.findUserOrFail(ownerUser.getId())).willReturn(ownerUser);

            // Act
            Throwable thrown = Assertions.catchThrowable(() -> userService.findUserById(ownerUser.getId()));
            // Assert
            assertThat(thrown)
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("is not authorized to access profile for user " + ownerUser.getId());

            then(userServiceHelper).should().getAuthenticatedUserEntity();
            then(entityFinderHelper).should().findUserOrFail(ownerUser.getId());
            then(userMapper).should(never()).mapToBaseProfileDTO(any());
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
            given(userMapper.mapToBaseProfileDTO(adminUser)).willReturn(genericStaffDto);

            Optional<UserProfileDto> result = userService.findUserByEmail(adminUser.getEmail());

            assertThat(result).isPresent().contains(genericStaffDto);
            then(userServiceHelper).should().getAuthenticatedUserEntity();
            then(userRepository).should().findByEmail(adminUser.getEmail());
        }

        @Test
        @DisplayName("should return DTO when Admin finds Staff in same clinic by Email")
        void shouldReturnDtoWhenAdminFindsStaffInSameClinicByEmail() {
            given(userServiceHelper.getAuthenticatedUserEntity()).willReturn(adminUser); // Admin C1 requester
            given(userRepository.findByEmail(vetUserSameClinic.getEmail())).willReturn(Optional.of(vetUserSameClinic)); // Target Vet C1
            given(userMapper.mapToBaseProfileDTO(vetUserSameClinic)).willReturn(
                    new UserProfileDto(vetUserSameClinic.getId(), vetUserSameClinic.getUsername(), vetUserSameClinic.getEmail(), Set.of("VET"), vetUserSameClinic.getAvatar())
            );

            Optional<UserProfileDto> result = userService.findUserByEmail(vetUserSameClinic.getEmail());

            assertThat(result).isPresent();
            assertThat(result.get().email()).isEqualTo(vetUserSameClinic.getEmail());
            then(userServiceHelper).should().getAuthenticatedUserEntity();
            then(userRepository).should().findByEmail(vetUserSameClinic.getEmail());
        }

        @Test
        @DisplayName("should return empty Optional when target user does not exist")
        void shouldReturnEmptyWhenNotFound() {
            given(userServiceHelper.getAuthenticatedUserEntity()).willReturn(ownerUser); // Assume someone is asking
            given(userRepository.findByEmail("notfound@test.com")).willReturn(Optional.empty()); // Target NOT found

            Optional<UserProfileDto> result = userService.findUserByEmail("notfound@test.com");

            assertThat(result).isNotPresent();
            then(userServiceHelper).should().getAuthenticatedUserEntity();
            then(userRepository).should().findByEmail("notfound@test.com");
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
            given(userServiceHelper.getAuthenticatedUserEntity()).willReturn(ownerUser);
            given(userRepository.findByUsername(ownerUser.getUsername())).willReturn(Optional.of(ownerUser));
            given(userMapper.mapToBaseProfileDTO(ownerUser)).willReturn(genericOwnerDto);

            Optional<UserProfileDto> result = userService.findUserByUsername(ownerUser.getUsername());

            assertThat(result).isPresent().contains(genericOwnerDto);
            then(userServiceHelper).should().getAuthenticatedUserEntity();
            then(userRepository).should().findByUsername(ownerUser.getUsername());
        }

        @Test
        @DisplayName("should return DTO when Admin finds Staff in same clinic by Username")
        void shouldReturnDtoWhenAdminFindsStaffInSameClinicByUsername() {
            given(userServiceHelper.getAuthenticatedUserEntity()).willReturn(adminUser); // Admin C1
            given(userRepository.findByUsername(vetUserSameClinic.getUsername())).willReturn(Optional.of(vetUserSameClinic)); // Vet C1
            given(userMapper.mapToBaseProfileDTO(vetUserSameClinic)).willReturn(
                    new UserProfileDto(vetUserSameClinic.getId(), vetUserSameClinic.getUsername(), vetUserSameClinic.getEmail(), Set.of("VET"), vetUserSameClinic.getAvatar())
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
//    @Nested
//    @DisplayName("updateCurrentOwnerProfile Tests")
//    class UpdateCurrentOwnerProfileTests {
//        private OwnerProfileUpdateDto updateDto;
//
//        @BeforeEach
//        void updateOwnerSetup() {
//            updateDto = new OwnerProfileUpdateDto("newOwnerUsername", "new_avatar.png", "999-888-777");
//        }
//
//        @Test
//        @DisplayName("should update owner profile successfully")
//        void shouldUpdateOwnerProfile() throws IOException {
//            // Arrange
//            given(userServiceHelper.getAuthenticatedUserEntity()).willReturn(ownerUser);
//            doNothing().when(authorizationHelper).validateUsernameUpdate(updateDto.username(), ownerUser);
//            when(userMapper.updateOwnerFromDto(updateDto, ownerUser)).thenReturn(true);
//            when(ownerRepository.save(any(Owner.class))).thenReturn(ownerUser);
//
//            // Act
//            OwnerProfileDto expectedDto = new OwnerProfileDto(
//                    ownerUser.getId(), updateDto.username(), ownerUser.getEmail(), Set.of("OWNER"),
//                    ownerUser.getAvatar(), // Avatar original
//                    updateDto.phone()
//            );
//            when(userMapper.toOwnerProfileDto(any(Owner.class))).thenReturn(expectedDto);
//
//            // Act
//            OwnerProfileDto result = userService.updateCurrentOwnerProfile(updateDto, null);
//
//            // Assert
//            assertThat(result).isEqualTo(expectedDto);
//            then(userServiceHelper).should().getAuthenticatedUserEntity();
//            then(authorizationHelper).should().validateUsernameUpdate(updateDto.username(), ownerUser);
//            then(userMapper).should().updateOwnerFromDto(updateDto, ownerUser);
//            then(ownerRepository).should().save(ownerCaptor.capture());
//            then(userMapper).should(times(1)).toOwnerProfileDto(any(Owner.class));
//        }
//
//        @Test
//        @DisplayName("should throw UsernameAlreadyExistsException if new username is taken")
//        void shouldThrowUsernameExistsWhenUpdating() {
//            // Arrange
//            given(userServiceHelper.getAuthenticatedUserEntity()).willReturn(ownerUser);
//            willThrow(new UsernameAlreadyExistsException(updateDto.username()))
//                    .given(authorizationHelper)
//                    .validateUsernameUpdate(updateDto.username(), ownerUser);
//
//            // Act & Assert
//            assertThatThrownBy(() -> userService.updateCurrentOwnerProfile(updateDto, null))
//                    .isInstanceOf(UsernameAlreadyExistsException.class)
//                    .hasMessageContaining(updateDto.username());
//
//            then(userServiceHelper).should().getAuthenticatedUserEntity();
//            then(authorizationHelper).should().validateUsernameUpdate(updateDto.username(), ownerUser);
//            then(ownerRepository).should(never()).save(any());
//            then(userMapper).should(never()).updateOwnerFromDto(any(), any());
//        }
//
//        @Test
//        @DisplayName("should NOT save if username not changed and no image file provided and phone not changed")
//        void shouldNotSaveIfNoChanges() throws IOException {
//            // Arrange
//            OwnerProfileUpdateDto noChangeDto = new OwnerProfileUpdateDto(ownerUser.getUsername(),null,  ownerUser.getPhone());
//            given(userServiceHelper.getAuthenticatedUserEntity()).willReturn(ownerUser);
//            doNothing().when(authorizationHelper).validateUsernameUpdate(ownerUser.getUsername(), ownerUser);
//            when(userMapper.updateOwnerFromDto(noChangeDto, ownerUser)).thenReturn(false);
//            OwnerProfileDto originalDto = ownerProfileDto;
//            when(userMapper.toOwnerProfileDto(ownerUser)).thenReturn(originalDto);
//
//            // Act
//            OwnerProfileDto result = userService.updateCurrentOwnerProfile(noChangeDto, null);
//
//            // Assert
//            assertThat(result).isEqualTo(originalDto);
//            then(authorizationHelper).should().validateUsernameUpdate(ownerUser.getUsername(), ownerUser);
//            then(userMapper).should().updateOwnerFromDto(noChangeDto, ownerUser);
//            then(ownerRepository).should(never()).save(any(Owner.class));
//            then(userMapper).should().toOwnerProfileDto(ownerUser);
//            then(fileService).should(never()).storeImage(any(), anyString());
//            then(fileService).should(never()).deleteImage(anyString());
//        }
//
//        @Test
//        @DisplayName("should throw ClassCastException if authenticated user is not Owner")
//        void shouldThrowExceptionIfNotOwner() {
//            // Arrange
//            given(userServiceHelper.getAuthenticatedUserEntity()).willReturn(adminUser);
//
//            // Act & Assert
//            assertThatThrownBy(() -> userService.updateCurrentOwnerProfile(updateDto, null))
//                    .isInstanceOf(AccessDeniedException.class);
//
//            then(ownerRepository).should(never()).save(any());
//        }
//    }

    /**
     * --- Tests for updateCurrentClinicStaffProfile ---
     */
//    @Nested
//    @DisplayName("updateCurrentClinicStaffProfile Tests")
//    class UpdateCurrentClinicStaffProfileTests {
//        private UserProfileUpdateDto baseUpdateDto;
//
//        @BeforeEach
//        void updateStaffSetup() {
//            baseUpdateDto = new UserProfileUpdateDto("newStaffUsername", null);
//            ReflectionTestUtils.setField(userService, "defaultUserImagePathBase", "images/avatars/users/");
//        }
//
//        @Test
//        @DisplayName("should update staff profile successfully")
//        void shouldUpdateStaffProfile() throws IOException {
//            // Arrange
//            UserProfileUpdateDto updateDtoWithUsernameChange = new UserProfileUpdateDto("newStaffUsername", null);
//            given(userServiceHelper.getAuthenticatedUserEntity()).willReturn(adminUser);
//            doNothing().when(authorizationHelper).validateUsernameUpdate(updateDtoWithUsernameChange.username(), adminUser);
//            when(userMapper.updateClinicStaffCommonFromDto(updateDtoWithUsernameChange, adminUser)).thenReturn(true);
//            when(clinicStaffRepository.save(any(ClinicStaff.class))).thenReturn(adminUser);
//            ClinicStaffProfileDto expectedDto = new ClinicStaffProfileDto(
//                    adminUser.getId(),
//                    updateDtoWithUsernameChange.username(),
//                    adminUser.getEmail(),
//                    Set.of("ADMIN"),
//                    adminUser.getAvatar(),
//                    adminUser.getName(),
//                    adminUser.getSurname(),
//                    adminUser.isActive(),
//                    adminUser.getClinic().getId(),
//                    adminUser.getClinic().getName(),
//                    null, null
//            );
//            when(userMapper.toClinicStaffProfileDto(any(ClinicStaff.class))).thenReturn(expectedDto);
//
//            // Act
//            ClinicStaffProfileDto result = userService.updateCurrentClinicStaffProfile(updateDtoWithUsernameChange, null);
//
//            // Assert
//            assertThat(result).isNotNull();
//            assertThat(result.username()).isEqualTo(updateDtoWithUsernameChange.username());
//            assertThat(result.avatar()).isEqualTo(adminUser.getAvatar());
//            assertThat(result).isEqualTo(expectedDto);
//
//            then(userServiceHelper).should().getAuthenticatedUserEntity();
//            then(authorizationHelper).should().validateUsernameUpdate(updateDtoWithUsernameChange.username(), adminUser);
//            then(userMapper).should().updateClinicStaffCommonFromDto(updateDtoWithUsernameChange, adminUser);
//            then(clinicStaffRepository).should().save(clinicStaffCaptor.capture());
//            then(userMapper).should().toClinicStaffProfileDto(any(ClinicStaff.class));
//            then(fileService).should(never()).storeImage(any(), anyString());
//            then(fileService).should(never()).deleteImage(anyString());
//
//            ClinicStaff saved = clinicStaffCaptor.getValue();
//            assertThat(saved).isNotNull();
//        }
//
//        @Test
//        @DisplayName("should throw UsernameAlreadyExistsException if new username is taken")
//        void shouldThrowUsernameExistsWhenUpdating() {
//            // Arrange
//            given(userServiceHelper.getAuthenticatedUserEntity()).willReturn(adminUser);
//            doThrow(new UsernameAlreadyExistsException(baseUpdateDto.username()))
//                    .when(authorizationHelper).validateUsernameUpdate(baseUpdateDto.username(), adminUser);
//
//            // Act & Assert
//            assertThatThrownBy(() -> userService.updateCurrentClinicStaffProfile(baseUpdateDto, null))
//                    .isInstanceOf(UsernameAlreadyExistsException.class)
//                    .hasMessageContaining(baseUpdateDto.username());
//
//            then(userServiceHelper).should().getAuthenticatedUserEntity();
//            then(authorizationHelper).should().validateUsernameUpdate(baseUpdateDto.username(), adminUser);
//            then(clinicStaffRepository).should(never()).save(any());
//            then(userMapper).should(never()).updateClinicStaffCommonFromDto(any(), any());
//        }
//
//        @Test
//        @DisplayName("should NOT check uniqueness or throw if username is not changed")
//        void shouldNotCheckUniquenessIfUsernameNotChanged() throws IOException {
//            // Arrange
//            given(userServiceHelper.getAuthenticatedUserEntity()).willReturn(adminUser);
//            UserProfileUpdateDto noUsernameChangeDto = new UserProfileUpdateDto(adminUser.getUsername(), "new_avatar.png");
//            when(userMapper.updateClinicStaffCommonFromDto(noUsernameChangeDto, adminUser)).thenReturn(false);
//
//            ClinicStaffProfileDto originalDto = new ClinicStaffProfileDto(
//                    adminUser.getId(), adminUser.getUsername(), adminUser.getEmail(),
//                    Set.of("ADMIN"), adminUser.getAvatar(),
//                    adminUser.getName(), adminUser.getSurname(), adminUser.isActive(),
//                    adminUser.getClinic().getId(), adminUser.getClinic().getName(),
//                    null, null
//            );
//            when(userMapper.toClinicStaffProfileDto(adminUser)).thenReturn(originalDto);
//
//            // Act
//            ClinicStaffProfileDto result = userService.updateCurrentClinicStaffProfile(noUsernameChangeDto, null);
//
//            // Assert
//            assertThat(result).isEqualTo(originalDto);
//
//            then(userServiceHelper).should().getAuthenticatedUserEntity();
//            then(authorizationHelper).should().validateUsernameUpdate(adminUser.getUsername(), adminUser);
//            then(userMapper).should().updateClinicStaffCommonFromDto(noUsernameChangeDto, adminUser);
//            then(clinicStaffRepository).should(never()).save(any(ClinicStaff.class));
//            then(userMapper).should().toClinicStaffProfileDto(adminUser);
//            then(fileService).should(never()).storeImage(any(), anyString());
//            then(fileService).should(never()).deleteImage(anyString());
//        }
//
//        @Test
//        @DisplayName("should throw ClassCastException if authenticated user is not ClinicStaff")
//        void shouldThrowExceptionIfNotStaff() throws IOException {
//            // Arrange
//            UserProfileUpdateDto dummyUpdateDto = new UserProfileUpdateDto("anyUser", null);
//            given(userServiceHelper.getAuthenticatedUserEntity()).willReturn(ownerUser);
//
//            // Act & Assert
//            assertThatThrownBy(() -> userService.updateCurrentClinicStaffProfile(dummyUpdateDto, null))
//                    .isInstanceOf(AccessDeniedException.class)
//                    .hasMessageContaining("User is not Clinic Staff");
//
//            then(userServiceHelper).should().getAuthenticatedUserEntity();
//            then(authorizationHelper).should(never()).validateUsernameUpdate(any(), any());
//            then(clinicStaffRepository).should(never()).save(any());
//            then(fileService).should(never()).storeImage(any(), anyString());
//        }
//
//        @Test
//        @DisplayName("should update staff image and username, and delete old image")
//        void shouldUpdateStaffImageAndUsername() throws IOException {
//            // Arrange
//            MockMultipartFile imageFile = new MockMultipartFile("avatar", "new_staff.png", MediaType.IMAGE_PNG_VALUE, "staff_img".getBytes());
//            String oldAvatar = adminUser.getAvatar();
//            String newAvatarPath = "users/avatars/staff_uuid.png";
//            UserProfileUpdateDto dtoWithUsernameChange = new UserProfileUpdateDto("updatedStaffUser", null);
//
//            // Mocks setup
//            given(userServiceHelper.getAuthenticatedUserEntity()).willReturn(adminUser);
//            doNothing().when(authorizationHelper).validateUsernameUpdate(dtoWithUsernameChange.username(), adminUser);
//            given(fileService.storeImage(imageFile, "users/avatars")).willReturn(newAvatarPath);
//            assertThat(oldAvatar).isNotEqualTo("images/avatars/users/admin.png");
//            doNothing().when(fileService).deleteImage(oldAvatar);
//
//            when(userMapper.updateClinicStaffCommonFromDto(dtoWithUsernameChange, adminUser))
//                    .thenAnswer(invocation -> {
//                        ClinicStaff staffArg = invocation.getArgument(1);
//                        UserProfileUpdateDto dtoArg = invocation.getArgument(0);
//                        boolean changed;
//
//                        changed = Utils.updateStringFieldIfChanged(staffArg, dtoArg.username(), staffArg::getUsername, ClinicStaff::setUsername, "username");
//                        return changed;
//                    });
//
//            when(clinicStaffRepository.save(any(ClinicStaff.class))).thenAnswer(i -> i.getArgument(0));
//
//            String expectedFullAvatarUrl = "http://localhost:8080/" + newAvatarPath;
//            ClinicStaffProfileDto expectedDto = new ClinicStaffProfileDto(
//                    adminUser.getId(), dtoWithUsernameChange.username(), adminUser.getEmail(), Set.of("ADMIN"),
//                    expectedFullAvatarUrl, adminUser.getName(), adminUser.getSurname(), adminUser.isActive(),
//                    adminUser.getClinic().getId(), adminUser.getClinic().getName(), null, null );
//            when(userMapper.toClinicStaffProfileDto(any(ClinicStaff.class))).thenReturn(expectedDto);
//
//            // Act
//            ClinicStaffProfileDto result = userService.updateCurrentClinicStaffProfile(dtoWithUsernameChange, imageFile);
//
//            // Assert
//            assertThat(result).isNotNull();
//            assertThat(result.avatar()).isEqualTo(expectedFullAvatarUrl);
//            assertThat(result.username()).isEqualTo(dtoWithUsernameChange.username());
//            assertThat(result).isEqualTo(expectedDto);
//
//            // Verify interactions
//            then(userServiceHelper).should().getAuthenticatedUserEntity();
//            then(authorizationHelper).should().validateUsernameUpdate(dtoWithUsernameChange.username(), adminUser);
//            then(fileService).should().storeImage(imageFile, "users/avatars");
//            then(userMapper).should().updateClinicStaffCommonFromDto(dtoWithUsernameChange, adminUser);
//            then(clinicStaffRepository).should().save(clinicStaffCaptor.capture());
//            then(fileService).should().deleteImage(oldAvatar);
//            then(userMapper).should().toClinicStaffProfileDto(any(ClinicStaff.class));
//
//            ClinicStaff capturedStaff = clinicStaffCaptor.getValue();
//            assertThat(capturedStaff.getAvatar()).isEqualTo(newAvatarPath);
//            assertThat(capturedStaff.getUsername()).isEqualTo(dtoWithUsernameChange.username());
//        }
//
//        @Test
//        @DisplayName("should throw IOException if image update storage fails")
//        void shouldThrowIOExceptionWhenImageStorageFails() throws IOException {
//            // Arrange
//            MockMultipartFile imageFile = new MockMultipartFile("avatar", "fail.jpg", MediaType.IMAGE_JPEG_VALUE, "img".getBytes());
//            given(userServiceHelper.getAuthenticatedUserEntity()).willReturn(adminUser);
//
//            given(fileService.storeImage(imageFile, "users/avatars")).willThrow(new IOException("Disk full"));
//
//            // Act & Assert
//            assertThatThrownBy(() -> userService.updateCurrentClinicStaffProfile(baseUpdateDto, imageFile))
//                    .isInstanceOf(IOException.class)
//                    .hasMessageContaining("Failed to process avatar image");
//
//            then(userServiceHelper).should().getAuthenticatedUserEntity();
//            then(authorizationHelper).should().validateUsernameUpdate(baseUpdateDto.username(), adminUser);
//            then(fileService).should().storeImage(imageFile, "users/avatars");
//            then(userMapper).should(never()).updateClinicStaffCommonFromDto(any(), any());
//            then(clinicStaffRepository).should(never()).save(any());
//            then(fileService).should(never()).deleteImage(anyString());
//        }
//    }
}