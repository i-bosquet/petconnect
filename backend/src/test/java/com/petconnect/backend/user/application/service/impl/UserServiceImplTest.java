package com.petconnect.backend.user.application.service.impl;

import com.petconnect.backend.exception.EntityNotFoundException; // Import necessary for tests
import com.petconnect.backend.exception.UsernameAlreadyExistsException;
import com.petconnect.backend.user.application.dto.ClinicStaffProfileDto;
import com.petconnect.backend.user.application.dto.OwnerProfileDto;
import com.petconnect.backend.user.application.dto.OwnerProfileUpdateDto;
import com.petconnect.backend.user.application.dto.UserProfileDto;
import com.petconnect.backend.user.application.dto.UserProfileUpdateDto;
import com.petconnect.backend.user.application.mapper.UserMapper;
import com.petconnect.backend.user.application.service.helper.UserServiceHelper; // Import the helper
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
import org.springframework.util.StringUtils;


import java.util.Optional;
import java.util.Set;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;


/**
 * Unit tests for {@link UserServiceImpl}.
 * Verifies logic for retrieving user profiles, finding users, and updating user profiles.
 * Uses mocked UserServiceHelper to simulate authenticated user context.
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
    @Mock private UserServiceHelper userServiceHelper; // ADDED: Mock the helper

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

    @BeforeEach
    void setUp() {
        Clinic clinic1 = Clinic.builder().name("Clinic A").build(); clinic1.setId(1L);
        Clinic clinic2 = Clinic.builder().name("Clinic B").build(); clinic2.setId(2L); // Another clinic for tests

        RoleEntity ownerRole = RoleEntity.builder().roleEnum(RoleEnum.OWNER).build(); ownerRole.setId(1L);
        RoleEntity adminRole = RoleEntity.builder().roleEnum(RoleEnum.ADMIN).build(); adminRole.setId(3L);
        RoleEntity vetRole = RoleEntity.builder().roleEnum(RoleEnum.VET).build(); vetRole.setId(2L);


        ownerUser = new Owner();
        ownerUser.setId(1L);
        ownerUser.setUsername("testowner");
        ownerUser.setEmail("owner@test.com");
        ownerUser.setPassword("hashedPasswordOwner");
        ownerUser.setPhone("111-222-333");
        ownerUser.setRoles(Set.of(ownerRole));
        ownerUser.setAvatar("avatar_owner.png");

        adminUser = new ClinicStaff();
        adminUser.setId(2L);
        adminUser.setUsername("testadmin");
        adminUser.setEmail("admin@test.com");
        adminUser.setPassword("hashedPasswordAdmin");
        adminUser.setName("Admin");
        adminUser.setSurname("Test");
        adminUser.setClinic(clinic1); // Belongs to Clinic 1
        adminUser.setRoles(Set.of(adminRole));
        adminUser.setAvatar("avatar_admin.png");
        adminUser.setActive(true);

        // Vet in the SAME clinic as adminUser
        vetUserSameClinic = new Vet();
        vetUserSameClinic.setId(3L);
        vetUserSameClinic.setUsername("testvet_c1");
        vetUserSameClinic.setEmail("vet_c1@test.com");
        vetUserSameClinic.setPassword("hashedPasswordVet1");
        vetUserSameClinic.setName("Vet");
        vetUserSameClinic.setSurname("SameClinic");
        vetUserSameClinic.setClinic(clinic1); // Belongs to Clinic 1
        vetUserSameClinic.setRoles(Set.of(vetRole));
        vetUserSameClinic.setLicenseNumber("VET_C1");
        vetUserSameClinic.setVetPublicKey("KEY_C1");
        vetUserSameClinic.setAvatar("avatar_vet.png");
        vetUserSameClinic.setActive(true);

        // Admin in a DIFFERENT clinic
        adminUserOtherClinic = new ClinicStaff();
        adminUserOtherClinic.setId(4L);
        adminUserOtherClinic.setUsername("admin_other");
        adminUserOtherClinic.setEmail("admin_c2@test.com");
        adminUserOtherClinic.setPassword("hashedPasswordAdmin2");
        adminUserOtherClinic.setName("Admin");
        adminUserOtherClinic.setSurname("OtherClinic");
        adminUserOtherClinic.setClinic(clinic2); // Belongs to Clinic 2
        adminUserOtherClinic.setRoles(Set.of(adminRole));
        adminUserOtherClinic.setAvatar("avatar_admin2.png");
        adminUserOtherClinic.setActive(true);


        ownerProfileDto = new OwnerProfileDto(ownerUser.getId(), ownerUser.getUsername(), ownerUser.getEmail(), Set.of("OWNER"), ownerUser.getAvatar(), ownerUser.getPhone());
        staffProfileDto = new ClinicStaffProfileDto(adminUser.getId(), adminUser.getUsername(), adminUser.getEmail(), Set.of("ADMIN"), adminUser.getAvatar(), adminUser.getName(), adminUser.getSurname(), adminUser.isActive(), adminUser.getClinic().getId(), adminUser.getClinic().getName(), null, null);
        genericOwnerDto = new UserProfileDto(ownerUser.getId(), ownerUser.getUsername(), ownerUser.getEmail(), Set.of("OWNER"), ownerUser.getAvatar());
        genericStaffDto = new UserProfileDto(adminUser.getId(), adminUser.getUsername(), adminUser.getEmail(), Set.of("ADMIN"), adminUser.getAvatar());

    }

    // --- Tests for getCurrentUserProfile ---
    @Nested
    @DisplayName("getCurrentUserProfile Tests")
    class GetCurrentUserProfileTests {

        @Test
        @DisplayName("should return OwnerProfileDto when authenticated user is Owner")
        void shouldReturnOwnerProfileDtoForOwner() {
            // Arrange
            given(userServiceHelper.getAuthenticatedUserEntity()).willReturn(ownerUser); // ADDED: Mock helper
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
            // Arrange
            // REMOVED: All direct SecurityContextHolder mocking
            // REMOVED: userRepository findByEmail/Username mocks for "ghost_user"

            // ADDED: Simulate the helper throwing the exception when user not found in DB
            given(userServiceHelper.getAuthenticatedUserEntity())
                    .willThrow(new EntityNotFoundException("Authenticated user 'ghost_user' not found in database."));

            // Act & Assert
            assertThatThrownBy(() -> userService.getCurrentUserProfile())
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Authenticated user 'ghost_user' not found");
            then(userServiceHelper).should().getAuthenticatedUserEntity();
        }
    }

    // --- Tests for findUserById ---
    @Nested
    @DisplayName("findUserById Tests")
    class FindUserByIdTests {

        @Test
        @DisplayName("should return DTO when user finds self by ID")
        void shouldReturnDtoWhenSelfFindById() {
            // Arrange: Requester IS the target
            given(userServiceHelper.getAuthenticatedUserEntity()).willReturn(ownerUser); // Requester is Owner
            given(userRepository.findById(ownerUser.getId())).willReturn(Optional.of(ownerUser)); // Target found
            given(userMapper.mapToBaseProfileDTO(ownerUser)).willReturn(genericOwnerDto);

            // Act
            Optional<UserProfileDto> result = userService.findUserById(ownerUser.getId());

            // Assert
            assertThat(result).isPresent().contains(genericOwnerDto);
            then(userServiceHelper).should().getAuthenticatedUserEntity();
            then(userRepository).should().findById(ownerUser.getId());
            then(userMapper).should().mapToBaseProfileDTO(ownerUser);
        }

        @Test
        @DisplayName("should return DTO when Admin finds Staff in same clinic by ID")
        void shouldReturnDtoWhenAdminFindsStaffInSameClinicById() {
            // Arrange: Requester is adminUser (Clinic 1), Target is vetUserSameClinic (Clinic 1)
            given(userServiceHelper.getAuthenticatedUserEntity()).willReturn(adminUser); // Requester is Admin C1
            given(userRepository.findById(vetUserSameClinic.getId())).willReturn(Optional.of(vetUserSameClinic)); // Target found
            given(userMapper.mapToBaseProfileDTO(vetUserSameClinic)).willReturn( // Assuming a generic DTO for the vet
                    new UserProfileDto(vetUserSameClinic.getId(), vetUserSameClinic.getUsername(), vetUserSameClinic.getEmail(), Set.of("VET"), vetUserSameClinic.getAvatar())
            );

            // Act
            Optional<UserProfileDto> result = userService.findUserById(vetUserSameClinic.getId());

            // Assert
            assertThat(result).isPresent();
            assertThat(result.get().id()).isEqualTo(vetUserSameClinic.getId());
            then(userServiceHelper).should().getAuthenticatedUserEntity();
            then(userRepository).should().findById(vetUserSameClinic.getId());
            then(userMapper).should().mapToBaseProfileDTO(vetUserSameClinic);
        }


        @Test
        @DisplayName("should return empty Optional when target user does not exist")
        void shouldReturnEmptyWhenNotFound() {
            // Arrange: Requester doesn't matter if target not found
            given(userServiceHelper.getAuthenticatedUserEntity()).willReturn(ownerUser); // Assume someone is asking
            given(userRepository.findById(999L)).willReturn(Optional.empty()); // Target NOT found

            // Act
            Optional<UserProfileDto> result = userService.findUserById(999L);

            // Assert
            assertThat(result).isNotPresent();
            then(userServiceHelper).should().getAuthenticatedUserEntity();
            then(userRepository).should().findById(999L);
            then(userMapper).should(never()).mapToBaseProfileDTO(any());
        }

        @Test
        @DisplayName("should throw AccessDeniedException when Owner tries to find Admin by ID")
        void shouldThrowAccessDeniedWhenOwnerFindsAdminById() {
            // Arrange: Requester is Owner, Target is Admin
            given(userServiceHelper.getAuthenticatedUserEntity()).willReturn(ownerUser); // Requester
            given(userRepository.findById(adminUser.getId())).willReturn(Optional.of(adminUser)); // Target found

            // Act
            Throwable thrown = Assertions.catchThrowable(() -> userService.findUserById(adminUser.getId()));
            // Assert
            assertThat(thrown)
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("is not authorized to access profile for user " + adminUser.getId());

            then(userServiceHelper).should().getAuthenticatedUserEntity();
            then(userRepository).should().findById(adminUser.getId());
            then(userMapper).should(never()).mapToBaseProfileDTO(any());
        }

        @Test
        @DisplayName("should throw AccessDeniedException when Admin tries to find staff from different clinic by ID")
        void shouldThrowAccessDeniedWhenAdminFindsStaffDifferentClinicById() {
            // Arrange: Requester is adminUser (Clinic 1), Target is adminUserOtherClinic (Clinic 2)
            given(userServiceHelper.getAuthenticatedUserEntity()).willReturn(adminUser); // Requester C1
            given(userRepository.findById(adminUserOtherClinic.getId())).willReturn(Optional.of(adminUserOtherClinic)); // Target C2 found

            // Act
            Throwable thrown = Assertions.catchThrowable(() -> userService.findUserById(adminUserOtherClinic.getId()));
            // Assert
            assertThat(thrown)
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("is not authorized to access profile for user " + adminUserOtherClinic.getId());

            then(userServiceHelper).should().getAuthenticatedUserEntity();
            then(userRepository).should().findById(adminUserOtherClinic.getId());
            then(userMapper).should(never()).mapToBaseProfileDTO(any());
        }

        @Test
        @DisplayName("should throw AccessDeniedException when Staff tries to find Owner by ID")
        void shouldThrowAccessDeniedWhenStaffFindsOwnerById() {
            // Arrange: Requester is Admin, Target is Owner
            given(userServiceHelper.getAuthenticatedUserEntity()).willReturn(adminUser); // Requester C1
            given(userRepository.findById(ownerUser.getId())).willReturn(Optional.of(ownerUser)); // Target Owner found

            // Act
            Throwable thrown = Assertions.catchThrowable(() -> userService.findUserById(ownerUser.getId()));
            // Assert
            assertThat(thrown)
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("is not authorized to access profile for user " + ownerUser.getId());

            then(userServiceHelper).should().getAuthenticatedUserEntity();
            then(userRepository).should().findById(ownerUser.getId());
            then(userMapper).should(never()).mapToBaseProfileDTO(any());
        }
    }

    // --- Tests for findUserByEmail ---
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


    // --- Tests for findUserByUsername ---
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


    // --- Tests for updateCurrentOwnerProfile ---
    @Nested
    @DisplayName("updateCurrentOwnerProfile Tests")
    class UpdateCurrentOwnerProfileTests {
        private OwnerProfileUpdateDto updateDto;
        private OwnerProfileDto expectedResultDto;

        @BeforeEach
        void updateOwnerSetup() {
            updateDto = new OwnerProfileUpdateDto("newOwnerUsername", "new_avatar.png", "999-888-777");
            expectedResultDto = new OwnerProfileDto(
                    ownerUser.getId(),
                    updateDto.username(),
                    ownerUser.getEmail(),
                    Set.of("OWNER"),
                    updateDto.avatar(),
                    updateDto.phone()
            );
        }

        @Test
        @DisplayName("should update owner profile successfully")
        void shouldUpdateOwnerProfile() {
            // Arrange
            given(userServiceHelper.getAuthenticatedUserEntity()).willReturn(ownerUser); // ADDED: Mock helper
            given(userRepository.existsByUsername(updateDto.username())).willReturn(false); // Username unique
            given(ownerRepository.save(any(Owner.class))).willAnswer(invocation -> invocation.getArgument(0));
            doAnswer(invocation -> { /* Simulate mapper side effects */
                OwnerProfileUpdateDto dtoArg = invocation.getArgument(0);
                Owner ownerArg = invocation.getArgument(1);
                if (StringUtils.hasText(dtoArg.username())) ownerArg.setUsername(dtoArg.username());
                if (dtoArg.avatar() != null) ownerArg.setAvatar(dtoArg.avatar());
                if (StringUtils.hasText(dtoArg.phone())) ownerArg.setPhone(dtoArg.phone());
                return null;
            }).when(userMapper).updateOwnerFromDto(updateDto, ownerUser);
            given(userMapper.toOwnerProfileDto(any(Owner.class))).willReturn(expectedResultDto);

            // Act
            OwnerProfileDto result = userService.updateCurrentOwnerProfile(updateDto);

            // Assert
            assertThat(result).isEqualTo(expectedResultDto);
            then(userServiceHelper).should().getAuthenticatedUserEntity(); // ADDED: Verify helper
            then(userRepository).should().existsByUsername(updateDto.username());
            then(userMapper).should().updateOwnerFromDto(updateDto, ownerUser);
            then(ownerRepository).should().save(ownerCaptor.capture());
            then(userMapper).should(times(1)).toOwnerProfileDto(any(Owner.class));

            Owner saved = ownerCaptor.getValue();
            assertThat(saved.getUsername()).isEqualTo(updateDto.username());
            assertThat(saved.getAvatar()).isEqualTo(updateDto.avatar());
            assertThat(saved.getPhone()).isEqualTo(updateDto.phone());
        }

        @Test
        @DisplayName("should throw UsernameAlreadyExistsException if new username is taken")
        void shouldThrowUsernameExistsWhenUpdating() {
            // Arrange
            given(userServiceHelper.getAuthenticatedUserEntity()).willReturn(ownerUser);
            given(userRepository.existsByUsername(updateDto.username())).willReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> userService.updateCurrentOwnerProfile(updateDto))
                    .isInstanceOf(UsernameAlreadyExistsException.class)
                    .hasMessageContaining(updateDto.username());

            then(userServiceHelper).should().getAuthenticatedUserEntity();
            then(userRepository).should().existsByUsername(updateDto.username());
            then(ownerRepository).should(never()).save(any());
            then(userMapper).should(never()).updateOwnerFromDto(any(), any());
        }

        @Test
        @DisplayName("should NOT check uniqueness or throw if username is not changed")
        void shouldNotCheckUniquenessIfUsernameNotChanged() {
            // Arrange
            given(userServiceHelper.getAuthenticatedUserEntity()).willReturn(ownerUser);
            OwnerProfileUpdateDto noUsernameChangeDto = new OwnerProfileUpdateDto(ownerUser.getUsername(), "new_avatar.png", "999");
            OwnerProfileDto expectedDto = new OwnerProfileDto(
                    ownerUser.getId(), ownerUser.getUsername(), ownerUser.getEmail(),
                    Set.of("OWNER"), noUsernameChangeDto.avatar(), noUsernameChangeDto.phone()
            );
            given(ownerRepository.save(any(Owner.class))).willAnswer(i -> i.getArgument(0));
            given(userMapper.toOwnerProfileDto(any(Owner.class))).willReturn(expectedDto);
            doAnswer(invocation -> {
                Owner ownerArg = invocation.getArgument(1);
                ownerArg.setAvatar(noUsernameChangeDto.avatar());
                ownerArg.setPhone(noUsernameChangeDto.phone());
                return null;
            }).when(userMapper).updateOwnerFromDto(noUsernameChangeDto, ownerUser);

            // Act
            userService.updateCurrentOwnerProfile(noUsernameChangeDto);

            // Assert
            then(userServiceHelper).should().getAuthenticatedUserEntity();
            then(userRepository).should(never()).existsByUsername(anyString());
            then(userMapper).should().updateOwnerFromDto(noUsernameChangeDto, ownerUser);
            then(ownerRepository).should().save(any(Owner.class));
            then(userMapper).should(times(1)).toOwnerProfileDto(any(Owner.class));
        }

        @Test
        @DisplayName("should throw ClassCastException if authenticated user is not Owner")
        void shouldThrowExceptionIfNotOwner() {
            // Arrange
            given(userServiceHelper.getAuthenticatedUserEntity()).willReturn(adminUser);

            // Act & Assert
            assertThatThrownBy(() -> userService.updateCurrentOwnerProfile(updateDto))
                    .isInstanceOf(ClassCastException.class);

            then(userServiceHelper).should().getAuthenticatedUserEntity();
            then(ownerRepository).should(never()).save(any());
        }
    }


    // --- Tests for updateCurrentClinicStaffProfile ---
    @Nested
    @DisplayName("updateCurrentClinicStaffProfile Tests")
    class UpdateCurrentClinicStaffProfileTests {
        private UserProfileUpdateDto updateDto;
        private ClinicStaffProfileDto expectedResultDto;

        @BeforeEach
        void updateStaffSetup() {
            updateDto = new UserProfileUpdateDto("newStaffUsername", "new_staff_avatar.png");

            expectedResultDto = new ClinicStaffProfileDto(
                    adminUser.getId(),
                    updateDto.username(),
                    adminUser.getEmail(),
                    Set.of("ADMIN"),
                    updateDto.avatar(),
                    adminUser.getName(),
                    adminUser.getSurname(),
                    adminUser.isActive(),
                    adminUser.getClinic().getId(),
                    adminUser.getClinic().getName(),
                    null, // Vet field
                    null  // Vet field
            );
        }

        @Test
        @DisplayName("should update staff profile successfully")
        void shouldUpdateStaffProfile() {
            // Arrange
            given(userServiceHelper.getAuthenticatedUserEntity()).willReturn(adminUser); // ADDED: Mock helper
            given(userRepository.existsByUsername(updateDto.username())).willReturn(false); // Username unique
            given(clinicStaffRepository.save(any(ClinicStaff.class))).willAnswer(i -> i.getArgument(0));
            doAnswer(invocation -> {
                UserProfileUpdateDto dtoArg = invocation.getArgument(0);
                ClinicStaff staffArg = invocation.getArgument(1);
                if (StringUtils.hasText(dtoArg.username())) staffArg.setUsername(dtoArg.username());
                if (dtoArg.avatar() != null) staffArg.setAvatar(dtoArg.avatar());
                return null;
            }).when(userMapper).updateClinicStaffCommonFromDto(updateDto, adminUser);
            given(userMapper.toClinicStaffProfileDto(any(ClinicStaff.class))).willReturn(expectedResultDto);

            // Act
            ClinicStaffProfileDto result = userService.updateCurrentClinicStaffProfile(updateDto);

            // Assert
            assertThat(result).isEqualTo(expectedResultDto);
            then(userServiceHelper).should().getAuthenticatedUserEntity();
            then(userRepository).should().existsByUsername(updateDto.username());
            then(userMapper).should().updateClinicStaffCommonFromDto(updateDto, adminUser);
            then(clinicStaffRepository).should().save(clinicStaffCaptor.capture());
            then(userMapper).should(times(1)).toClinicStaffProfileDto(any(ClinicStaff.class));

            ClinicStaff saved = clinicStaffCaptor.getValue();
            assertThat(saved.getUsername()).isEqualTo(updateDto.username());
            assertThat(saved.getAvatar()).isEqualTo(updateDto.avatar());
        }

        @Test
        @DisplayName("should throw UsernameAlreadyExistsException if new username is taken")
        void shouldThrowUsernameExistsWhenUpdating() {
            // Arrange
            given(userServiceHelper.getAuthenticatedUserEntity()).willReturn(adminUser);
            given(userRepository.existsByUsername(updateDto.username())).willReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> userService.updateCurrentClinicStaffProfile(updateDto))
                    .isInstanceOf(UsernameAlreadyExistsException.class)
                    .hasMessageContaining(updateDto.username());

            then(userServiceHelper).should().getAuthenticatedUserEntity();
            then(userRepository).should().existsByUsername(updateDto.username());
            then(clinicStaffRepository).should(never()).save(any());
            then(userMapper).should(never()).updateClinicStaffCommonFromDto(any(), any());
        }

        @Test
        @DisplayName("should NOT check uniqueness or throw if username is not changed")
        void shouldNotCheckUniquenessIfUsernameNotChanged() {
            // Arrange
            given(userServiceHelper.getAuthenticatedUserEntity()).willReturn(adminUser);
            UserProfileUpdateDto noUsernameChangeDto = new UserProfileUpdateDto(adminUser.getUsername(), "new_avatar.png");
            ClinicStaffProfileDto expectedDto = new ClinicStaffProfileDto(
                    adminUser.getId(), adminUser.getUsername(), adminUser.getEmail(),
                    Set.of("ADMIN"), noUsernameChangeDto.avatar(), adminUser.getName(), adminUser.getSurname(),
                    adminUser.isActive(), adminUser.getClinic().getId(), adminUser.getClinic().getName(),
                    null, null
            );
            given(clinicStaffRepository.save(any(ClinicStaff.class))).willAnswer(i -> i.getArgument(0));
            given(userMapper.toClinicStaffProfileDto(any(ClinicStaff.class))).willReturn(expectedDto);
            doAnswer(invocation -> {
                ClinicStaff staffArg = invocation.getArgument(1);
                staffArg.setAvatar(noUsernameChangeDto.avatar());
                return null;
            }).when(userMapper).updateClinicStaffCommonFromDto(noUsernameChangeDto, adminUser);

            // Act
            userService.updateCurrentClinicStaffProfile(noUsernameChangeDto);

            // Assert
            then(userServiceHelper).should().getAuthenticatedUserEntity();
            then(userRepository).should(never()).existsByUsername(anyString());
            then(userMapper).should().updateClinicStaffCommonFromDto(noUsernameChangeDto, adminUser);
            then(clinicStaffRepository).should().save(any(ClinicStaff.class));
            then(userMapper).should(times(1)).toClinicStaffProfileDto(any(ClinicStaff.class));
        }

        @Test
        @DisplayName("should throw ClassCastException if authenticated user is not ClinicStaff")
        void shouldThrowExceptionIfNotStaff() {
            // Arrange
            given(userServiceHelper.getAuthenticatedUserEntity()).willReturn(ownerUser);

            // Act & Assert
            assertThatThrownBy(() -> userService.updateCurrentClinicStaffProfile(updateDto))
                    .isInstanceOf(ClassCastException.class);

            then(userServiceHelper).should().getAuthenticatedUserEntity();
            then(clinicStaffRepository).should(never()).save(any());
        }
    }
}