package com.petconnect.backend.user.application.service.impl;

import com.petconnect.backend.exception.UsernameAlreadyExistsException;
import com.petconnect.backend.user.application.dto.ClinicStaffProfileDto;
import com.petconnect.backend.user.application.dto.OwnerProfileDto;
import com.petconnect.backend.user.application.dto.OwnerProfileUpdateDto;
import com.petconnect.backend.user.application.dto.UserProfileDto;
import com.petconnect.backend.user.application.dto.UserProfileUpdateDto;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.util.StringUtils;

import java.util.Optional;
import java.util.Set;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;


/**
 * Unit tests for {@link UserServiceImpl}.
 * Verifies logic for retrieving user profiles, finding users, and updating user profiles.
 * Authorization logic relies on Spring Security's @PreAuthorize, which is tested
 * implicitly by assuming the method would not be called if unauthorized, or explicitly
 * by mocking helper methods if they were involved in fetching auth context for the service itself.
 * However, since @PreAuthorize acts before the method, unit tests mainly focus on the method's
 * internal logic assuming authorization passed. Direct testing of @PreAuthorize often requires
 * integration tests with Spring Security context.
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

    // --- Class Under Test ---
    @InjectMocks
    private UserServiceImpl userService;

    // --- Captors ---
    @Captor private ArgumentCaptor<Owner> ownerCaptor;
    @Captor private ArgumentCaptor<ClinicStaff> clinicStaffCaptor;

    // --- Test Data ---
    private Owner ownerUser;
    private ClinicStaff adminUser; // Use an Admin/Vet as an example ClinicStaff
    private OwnerProfileDto ownerProfileDto;
    private ClinicStaffProfileDto staffProfileDto;
    private UserProfileDto genericOwnerDto;
    private UserProfileDto genericStaffDto;

    // Simulate Security Context (replace with mocking SecurityContextHolder if needed for complex scenarios)
    // For simplicity, we'll often mock userRepository directly based on expected identifier
    private final String ownerUsername = "testowner";
    private final String adminUsername = "testadmin";


    @BeforeEach
    void setUp() {
        Clinic clinic = Clinic.builder().name("Clinic A").build(); clinic.setId(1L);
        RoleEntity ownerRole = RoleEntity.builder().roleEnum(RoleEnum.OWNER).build(); ownerRole.setId(1L);
        RoleEntity adminRole = RoleEntity.builder().roleEnum(RoleEnum.ADMIN).build(); adminRole.setId(3L);

        ownerUser = new Owner();
        ownerUser.setId(1L);
        ownerUser.setUsername(ownerUsername);
        ownerUser.setEmail("owner@test.com");
        ownerUser.setPassword("hashedPasswordOwner");
        ownerUser.setPhone("111-222-333");
        ownerUser.setRoles(Set.of(ownerRole));
        ownerUser.setAvatar("avatar_owner.png");

        adminUser = new ClinicStaff(); // Using ClinicStaff as an example staff user
        adminUser.setId(2L);
        adminUser.setUsername(adminUsername);
        adminUser.setEmail("admin@test.com");
        adminUser.setPassword("hashedPasswordAdmin");
        adminUser.setName("Admin");
        adminUser.setSurname("Test");
        adminUser.setClinic(clinic);
        adminUser.setRoles(Set.of(adminRole));
        adminUser.setAvatar("avatar_admin.png");
        adminUser.setActive(true);

        ownerProfileDto = new OwnerProfileDto(ownerUser.getId(), ownerUser.getUsername(), ownerUser.getEmail(), Set.of("OWNER"), ownerUser.getAvatar(), ownerUser.getPhone());
        staffProfileDto = new ClinicStaffProfileDto(adminUser.getId(), adminUser.getUsername(), adminUser.getEmail(), Set.of("ADMIN"), adminUser.getAvatar(), adminUser.getName(), adminUser.getSurname(), adminUser.isActive(), adminUser.getClinic().getId(), adminUser.getClinic().getName(), null, null);
        genericOwnerDto = new UserProfileDto(ownerUser.getId(), ownerUser.getUsername(), ownerUser.getEmail(), Set.of("OWNER"), ownerUser.getAvatar());
        genericStaffDto = new UserProfileDto(adminUser.getId(), adminUser.getUsername(), adminUser.getEmail(), Set.of("ADMIN"), adminUser.getAvatar());

    }

    /** Mocks the SecurityContextHolder to return an authentication for the given user. */
    private void mockSecurityContext(UserEntity user) {
        Authentication auth = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        given(securityContext.getAuthentication()).willReturn(auth);
        SecurityContextHolder.setContext(securityContext);
        given(auth.isAuthenticated()).willReturn(true);
        given(auth.getPrincipal()).willReturn(user.getUsername()); // Or user object if UserDetails is principal
        given(auth.getName()).willReturn(user.getUsername()); // Principal name used by findAuthenticatedUser
        // Mock repository lookup based on the identifier used in findAuthenticatedUser
        given(userRepository.findByEmail(user.getUsername())).willReturn(Optional.empty()); // Assume lookup by username first/only
        given(userRepository.findByUsername(user.getUsername())).willReturn(Optional.of(user));
    }
    /** Mocks the SecurityContextHolder to be unauthenticated. */
    private void mockUnauthenticated() {
        SecurityContextHolder.clearContext(); // Clears context or set an anonymous token
    }


    // --- Tests for getCurrentUserProfile ---
    @Nested
    @DisplayName("getCurrentUserProfile Tests")
    class GetCurrentUserProfileTests {

        @Test
        @DisplayName("should return OwnerProfileDto when authenticated user is Owner")
        void shouldReturnOwnerProfileDtoForOwner() {
            // Arrange
            mockSecurityContext(ownerUser); // Simulate Owner is authenticated
            given(userMapper.toOwnerProfileDto(ownerUser)).willReturn(ownerProfileDto);

            // Act
            Object result = userService.getCurrentUserProfile();

            // Assert
            assertThat(result).isInstanceOf(OwnerProfileDto.class).isEqualTo(ownerProfileDto);
            then(userRepository).should().findByUsername(ownerUsername); // Verify user lookup
            then(userMapper).should().toOwnerProfileDto(ownerUser);
            then(userMapper).should(never()).toClinicStaffProfileDto(any());
        }

        @Test
        @DisplayName("should return ClinicStaffProfileDto when authenticated user is ClinicStaff")
        void shouldReturnClinicStaffProfileDtoForStaff() {
            // Arrange
            mockSecurityContext(adminUser); // Simulate Admin (ClinicStaff) is authenticated
            given(userMapper.toClinicStaffProfileDto(adminUser)).willReturn(staffProfileDto);

            // Act
            Object result = userService.getCurrentUserProfile();

            // Assert
            assertThat(result).isInstanceOf(ClinicStaffProfileDto.class).isEqualTo(staffProfileDto);
            then(userRepository).should().findByUsername(adminUsername);
            then(userMapper).should(never()).toOwnerProfileDto(any());
            then(userMapper).should().toClinicStaffProfileDto(adminUser);
        }

        @Test
        @DisplayName("should throw IllegalStateException when no user is authenticated")
        void shouldThrowIllegalStateWhenNotAuthenticated() {
            // Arrange
            mockUnauthenticated(); // Ensure no user is authenticated

            // Act
            Throwable thrown = Assertions.catchThrowable(() -> userService.getCurrentUserProfile());

            // Assert
            assertThat(thrown)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("No authenticated user found");
        }

        @Test
        @DisplayName("should throw UsernameNotFoundException when authenticated user not in DB")
        void shouldThrowUsernameNotFoundWhenAuthUserNotInDb() {
            // Arrange: Simulate auth context exists but DB lookup fails
            Authentication auth = mock(Authentication.class);
            SecurityContext securityContext = mock(SecurityContext.class);
            given(securityContext.getAuthentication()).willReturn(auth);
            SecurityContextHolder.setContext(securityContext);
            given(auth.isAuthenticated()).willReturn(true);
            given(auth.getName()).willReturn("ghost_user");
            given(userRepository.findByEmail("ghost_user")).willReturn(Optional.empty());
            given(userRepository.findByUsername("ghost_user")).willReturn(Optional.empty()); // User not found

            // Act
            Throwable thrown = Assertions.catchThrowable(() -> userService.getCurrentUserProfile());

            // Assert
            assertThat(thrown)
                    .isInstanceOf(UsernameNotFoundException.class) // Exception from findAuthenticatedUser's orElseThrow
                    .hasMessageContaining("Authenticated user not found");
        }
    }

    // --- Tests for findUserById ---
    // Note: These tests assume @PreAuthorize allows the call. Testing the @PreAuthorize logic
    // itself usually requires integration tests with security context.
    @Nested
    @DisplayName("findUserById Tests")
    class FindUserByIdTests {
        @Test
        @DisplayName("should return UserProfileDto when user exists")
        void shouldReturnUserProfileDtoWhenFound() {
            // Arrange
            given(userRepository.findById(ownerUser.getId())).willReturn(Optional.of(ownerUser));
            given(userMapper.mapToBaseProfileDTO(ownerUser)).willReturn(genericOwnerDto);

            // Act
            Optional<UserProfileDto> result = userService.findUserById(ownerUser.getId());

            // Assert
            assertThat(result).isPresent().contains(genericOwnerDto);
            then(userRepository).should().findById(eq(ownerUser.getId()));
            then(userMapper).should().mapToBaseProfileDTO(ownerUser);
        }

        @Test
        @DisplayName("should return empty Optional when user does not exist")
        void shouldReturnEmptyWhenNotFound() {
            // Arrange
            given(userRepository.findById(999L)).willReturn(Optional.empty());

            // Act
            Optional<UserProfileDto> result = userService.findUserById(999L);

            // Assert
            assertThat(result).isNotPresent();
            then(userRepository).should().findById(eq(999L));
            then(userMapper).should(never()).mapToBaseProfileDTO(any());
        }
    }

    // --- Tests for findUserByEmail ---
    @Nested
    @DisplayName("findUserByEmail Tests")
    class FindUserByEmailTests {
        @Test
        @DisplayName("should return UserProfileDto when user exists")
        void shouldReturnUserProfileDtoWhenFound() {
            // Arrange
            given(userRepository.findByEmail(adminUser.getEmail())).willReturn(Optional.of(adminUser));
            given(userMapper.mapToBaseProfileDTO(adminUser)).willReturn(genericStaffDto);

            // Act
            Optional<UserProfileDto> result = userService.findUserByEmail(adminUser.getEmail());

            // Assert
            assertThat(result).isPresent().contains(genericStaffDto);
            then(userRepository).should().findByEmail(eq(adminUser.getEmail()));
            then(userMapper).should().mapToBaseProfileDTO(adminUser);
        }

        @Test
        @DisplayName("should return empty Optional when user does not exist")
        void shouldReturnEmptyWhenNotFound() {
            // Arrange
            given(userRepository.findByEmail("notfound@test.com")).willReturn(Optional.empty());

            // Act
            Optional<UserProfileDto> result = userService.findUserByEmail("notfound@test.com");

            // Assert
            assertThat(result).isNotPresent();
            then(userRepository).should().findByEmail(eq("notfound@test.com"));
            then(userMapper).should(never()).mapToBaseProfileDTO(any());
        }
    }


    // --- Tests for findUserByUsername ---
    @Nested
    @DisplayName("findUserByUsername Tests")
    class FindUserByUsernameTests {
        @Test
        @DisplayName("should return UserProfileDto when user exists")
        void shouldReturnUserProfileDtoWhenFound() {
            // Arrange
            given(userRepository.findByUsername(ownerUser.getUsername())).willReturn(Optional.of(ownerUser));
            given(userMapper.mapToBaseProfileDTO(ownerUser)).willReturn(genericOwnerDto);

            // Act
            Optional<UserProfileDto> result = userService.findUserByUsername(ownerUser.getUsername());

            // Assert
            assertThat(result).isPresent().contains(genericOwnerDto);
            then(userRepository).should().findByUsername(eq(ownerUser.getUsername()));
            then(userMapper).should().mapToBaseProfileDTO(ownerUser);
        }

        @Test
        @DisplayName("should return empty Optional when user does not exist")
        void shouldReturnEmptyWhenNotFound() {
            // Arrange
            given(userRepository.findByUsername("notfounduser")).willReturn(Optional.empty());

            // Act
            Optional<UserProfileDto> result = userService.findUserByUsername("notfounduser");

            // Assert
            assertThat(result).isNotPresent();
            then(userRepository).should().findByUsername(eq("notfounduser"));
            then(userMapper).should(never()).mapToBaseProfileDTO(any());
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
            mockSecurityContext(ownerUser);
            given(userRepository.existsByUsername(updateDto.username())).willReturn(false); // Username unique

            // Mock save to return the argument passed to it
            given(ownerRepository.save(any(Owner.class))).willAnswer(invocation -> invocation.getArgument(0));

            doAnswer(invocation -> {
                OwnerProfileUpdateDto dtoArg = invocation.getArgument(0);
                Owner ownerArg = invocation.getArgument(1);

                if (StringUtils.hasText(dtoArg.username()) && !ownerArg.getUsername().equals(dtoArg.username())) {
                    ownerArg.setUsername(dtoArg.username());
                }
                if (dtoArg.avatar() != null) {
                    ownerArg.setAvatar(dtoArg.avatar());
                }
                if (StringUtils.hasText(dtoArg.phone())) {
                    ownerArg.setPhone(dtoArg.phone());
                }
                assertThat(ownerArg.getUsername()).isEqualTo("newOwnerUsername");
                return null; // void method
            }).when(userMapper).updateOwnerFromDto(updateDto, ownerUser);

            given(userMapper.toOwnerProfileDto(any(Owner.class))).willReturn(expectedResultDto);


            // Act
            OwnerProfileDto result = userService.updateCurrentOwnerProfile(updateDto);

            // Assert
            assertThat(result).isEqualTo(expectedResultDto);
            then(userRepository).should().existsByUsername(updateDto.username());
            then(userMapper).should().updateOwnerFromDto(updateDto, ownerUser);
            then(ownerRepository).should().save(ownerCaptor.capture());
            then(userMapper).should(times(1)).toOwnerProfileDto(any(Owner.class));

            // Assert on captured entity passed to save
            Owner saved = ownerCaptor.getValue();
            assertThat(saved.getUsername()).isEqualTo(updateDto.username());
            assertThat(saved.getAvatar()).isEqualTo(updateDto.avatar());
            assertThat(saved.getPhone()).isEqualTo(updateDto.phone());
        }

        @Test
        @DisplayName("should throw UsernameAlreadyExistsException if new username is taken")
        void shouldThrowUsernameExistsWhenUpdating() {
            // Arrange
            mockSecurityContext(ownerUser);
            given(userRepository.existsByUsername(updateDto.username())).willReturn(true); // New username EXISTS

            // Act
            Throwable thrown = Assertions.catchThrowable(() -> userService.updateCurrentOwnerProfile(updateDto));

            // Assert
            assertThat(thrown)
                    .isInstanceOf(UsernameAlreadyExistsException.class)
                    .hasMessageContaining(updateDto.username());
            then(ownerRepository).should(never()).save(any());
            then(userMapper).should(never()).updateOwnerFromDto(any(), any());
        }

        @Test
        @DisplayName("should NOT check uniqueness or throw if username is not changed")
        void shouldNotCheckUniquenessIfUsernameNotChanged() {
            // Arrange
            mockSecurityContext(ownerUser);
            OwnerProfileUpdateDto noUsernameChangeDto = new OwnerProfileUpdateDto(ownerUser.getUsername(), "new_avatar.png", "999");
            OwnerProfileDto expectedDto = new OwnerProfileDto(
                    ownerUser.getId(), ownerUser.getUsername(), ownerUser.getEmail(),
                    Set.of("OWNER"), noUsernameChangeDto.avatar(), noUsernameChangeDto.phone()
            );
            // Mock save
            given(ownerRepository.save(any(Owner.class))).willAnswer(i -> i.getArgument(0));
            // Mock toDto
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
            then(userRepository).should(never()).existsByUsername(anyString()); // No check
            then(userMapper).should().updateOwnerFromDto(noUsernameChangeDto, ownerUser); // Update called
            then(ownerRepository).should().save(any(Owner.class)); // Save called
            then(userMapper).should(times(1)).toOwnerProfileDto(any(Owner.class)); // Final map called
        }

        @Test
        @DisplayName("should throw IllegalStateException if authenticated user is not Owner")
        void shouldThrowIllegalStateIfNotOwner() {
            // Arrange
            mockSecurityContext(adminUser); // Authenticate as Admin instead of Owner
            // Act
            Throwable thrown = Assertions.catchThrowable(() -> userService.updateCurrentOwnerProfile(updateDto));
            // Assert
            assertThat(thrown)
                    .isInstanceOf(IllegalStateException.class) // Or AccessDeniedException depending on impl
                    .hasMessageContaining("Current user is not an Owner");
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
                    null,
                    null
            );
        }

        @Test
        @DisplayName("should update staff profile successfully")
        void shouldUpdateStaffProfile() {
            // Arrange
            mockSecurityContext(adminUser);
            given(userRepository.existsByUsername(updateDto.username())).willReturn(false);
            given(clinicStaffRepository.save(any(ClinicStaff.class))).willAnswer(i -> i.getArgument(0));

            doAnswer(invocation -> {
                UserProfileUpdateDto dtoArg = invocation.getArgument(0);
                ClinicStaff staffArg = invocation.getArgument(1);
                if (StringUtils.hasText(dtoArg.username()) && !staffArg.getUsername().equals(dtoArg.username())) {
                    staffArg.setUsername(dtoArg.username());
                }
                if (dtoArg.avatar() != null) {
                    staffArg.setAvatar(dtoArg.avatar());
                }
                assertThat(staffArg.getUsername()).isEqualTo("newStaffUsername");
                return null; // void method
            }).when(userMapper).updateClinicStaffCommonFromDto(updateDto, adminUser);

            given(userMapper.toClinicStaffProfileDto(any(ClinicStaff.class))).willReturn(expectedResultDto);


            // Act
            ClinicStaffProfileDto result = userService.updateCurrentClinicStaffProfile(updateDto);

            // Assert
            assertThat(result).isEqualTo(expectedResultDto);
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
            mockSecurityContext(adminUser);
            given(userRepository.existsByUsername(updateDto.username())).willReturn(true);

            // Act
            Throwable thrown = Assertions.catchThrowable(() -> userService.updateCurrentClinicStaffProfile(updateDto));

            // Assert
            assertThat(thrown)
                    .isInstanceOf(UsernameAlreadyExistsException.class)
                    .hasMessageContaining(updateDto.username());
            then(clinicStaffRepository).should(never()).save(any());
            then(userMapper).should(never()).updateClinicStaffCommonFromDto(any(), any());
        }

        @Test
        @DisplayName("should NOT check uniqueness or throw if username is not changed")
        void shouldNotCheckUniquenessIfUsernameNotChanged() {
            // Arrange
            mockSecurityContext(adminUser);
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
            then(userRepository).should(never()).existsByUsername(anyString());
            then(userMapper).should().updateClinicStaffCommonFromDto(noUsernameChangeDto, adminUser);
            then(clinicStaffRepository).should().save(any(ClinicStaff.class));
            then(userMapper).should(times(1)).toClinicStaffProfileDto(any(ClinicStaff.class));
        }

        @Test
        @DisplayName("should throw IllegalStateException if authenticated user is not ClinicStaff")
        void shouldThrowIllegalStateIfNotStaff() {
            // Arrange
            mockSecurityContext(ownerUser); // Authenticate as Owner

            // Act
            Throwable thrown = Assertions.catchThrowable(() -> userService.updateCurrentClinicStaffProfile(updateDto));

            // Assert
            assertThat(thrown)
                    .isInstanceOf(IllegalStateException.class) // Or AccessDeniedException
                    .hasMessageContaining("Current user is not Clinic Staff");
            then(clinicStaffRepository).should(never()).save(any());
        }
    }
}