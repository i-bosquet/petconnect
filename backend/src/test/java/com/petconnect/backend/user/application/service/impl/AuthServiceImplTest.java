package com.petconnect.backend.user.application.service.impl;

import com.petconnect.backend.exception.EmailAlreadyExistsException;
import com.petconnect.backend.exception.UsernameAlreadyExistsException;
import com.petconnect.backend.user.application.dto.AuthLoginRequestDto;
import com.petconnect.backend.user.application.dto.AuthResponseDto;
import com.petconnect.backend.user.application.dto.OwnerProfileDto;
import com.petconnect.backend.user.application.dto.OwnerRegistrationDto;
import com.petconnect.backend.user.application.mapper.UserMapper;
import com.petconnect.backend.user.domain.model.*;
import com.petconnect.backend.user.domain.repository.OwnerRepository;
import com.petconnect.backend.user.domain.repository.RoleRepository;
import com.petconnect.backend.user.domain.repository.UserRepository;
import com.petconnect.backend.security.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;


import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;


/**
 * Unit tests for {@link AuthServiceImpl}.
 * Uses Mockito to mock dependencies.
 *
 * @author ibosquet
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {
    @Mock
    private JwtUtils jwtUtils; // Mocked, not used in registerOwner test directly
    @Mock
    private UserRepository userRepository;
    @Mock
    private OwnerRepository ownerRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private RoleRepository roleRepository;

    @InjectMocks // Creates an instance of AuthServiceImpl and injects the mocks
    private AuthServiceImpl authService;

    // --- Test Data ---
    private OwnerRegistrationDto registrationDto;
    private RoleEntity ownerRole;
    private Owner savedOwner;
    private OwnerProfileDto expectedDto;

    @BeforeEach // Set up common data before each test
    void setUp() {
        registrationDto = new OwnerRegistrationDto(
                "testowner",
                "test@example.com",
                "password123",
                "123456789"
        );

        ownerRole = RoleEntity.builder().id(1L).roleEnum(RoleEnum.OWNER).build(); // Simulate role from DB

        // Simulate the owner entity AFTER it's saved (with ID, hashed password, etc.)
        savedOwner = new Owner();
        savedOwner.setId(1L);
        savedOwner.setUsername("testowner");
        savedOwner.setEmail("test@example.com");
        savedOwner.setPassword("hashedPassword"); // Assume this is the result of encoding
        savedOwner.setPhone("123456789");
        savedOwner.setRoles(Set.of(ownerRole));
        savedOwner.setAvatar("images/avatars/users/owner.png");
        // Set other UserEntity fields if necessary (isEnabled, etc.)
        savedOwner.setEnabled(true);
        savedOwner.setAccountNonExpired(true);
        savedOwner.setAccountNonLocked(true);
        savedOwner.setCredentialsNonExpired(true);


        // Simulate the DTO expected as the result
        expectedDto = new OwnerProfileDto(
                1L,
                "testowner",
                "test@example.com",
                Set.of("OWNER"),
                "images/avatars/users/owner.png",
                "123456789"
        );
    }

    // --- Tests for registerOwner ---

    @Test
    void registerOwner_shouldRegisterSuccessfully_whenDataIsValid() {
        // Given (Arrange): Define mock behavior
        given(userRepository.existsByEmail(registrationDto.email())).willReturn(false);
        given(userRepository.existsByUsername(registrationDto.username())).willReturn(false);
        given(roleRepository.findByRoleEnum(RoleEnum.OWNER)).willReturn(Optional.of(ownerRole));
        given(passwordEncoder.encode(registrationDto.password())).willReturn("hashedPassword");
        // When ownerRepository.save is called with ANY Owner object, return our simulated savedOwner
        given(ownerRepository.save(any(Owner.class))).willReturn(savedOwner);
        // When userMapper.toOwnerProfileDto is called with the savedOwner, return the expected DTO
        given(userMapper.toOwnerProfileDto(savedOwner)).willReturn(expectedDto);

        // When (Act): Call the method under test
        OwnerProfileDto actualDto = authService.registerOwner(registrationDto);

        // Then (Assert): Verify results and interactions
        assertThat(actualDto)
                .isNotNull()
                .isEqualTo(expectedDto); // Check if the returned DTO is as expected

        // Verify that save was called exactly once on ownerRepository with an Owner object
        then(ownerRepository).should().save(any(Owner.class));
        // Verify other mock interactions if necessary (e.g., passwordEncoder.encode was called)
        then(passwordEncoder).should().encode("password123");
    }

    @Test
    void registerOwner_shouldThrowEmailAlreadyExistsException_whenEmailExists() {
        // Given: Email already exists
        given(userRepository.existsByEmail(registrationDto.email())).willReturn(true);

        // When & Then: Expect the exception
        assertThatThrownBy(() -> authService.registerOwner(registrationDto))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining(registrationDto.email());

        // Verify that save was NEVER called
        then(ownerRepository).should(never()).save(any(Owner.class));
    }

    @Test
    void registerOwner_shouldThrowUsernameAlreadyExistsException_whenUsernameExists() {
        // Given: Email is ok, but Username already exists
        given(userRepository.existsByEmail(registrationDto.email())).willReturn(false);
        given(userRepository.existsByUsername(registrationDto.username())).willReturn(true);

        // When & Then: Expect the exception
        assertThatThrownBy(() -> authService.registerOwner(registrationDto))
                .isInstanceOf(UsernameAlreadyExistsException.class)
                .hasMessageContaining(registrationDto.username());

        // Verify that save was NEVER called
        then(ownerRepository).should(never()).save(any(Owner.class));
    }

    @Test
    void registerOwner_shouldThrowIllegalStateException_whenOwnerRoleNotFound() {
        // Given: Email and username are ok, but an OWNER role doesn't exist in DB (mock returns empty)
        given(userRepository.existsByEmail(registrationDto.email())).willReturn(false);
        given(userRepository.existsByUsername(registrationDto.username())).willReturn(false);
        given(roleRepository.findByRoleEnum(RoleEnum.OWNER)).willReturn(Optional.empty());
        given(passwordEncoder.encode(registrationDto.password())).willReturn("hashedPassword"); // Still need to mock encode

        // When & Then: Expect the exception
        assertThatThrownBy(() -> authService.registerOwner(registrationDto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("OWNER role not found");

        // Verify that save was NEVER called
        then(ownerRepository).should(never()).save(any(Owner.class));
    }


    @Test
    void loadUserByUsername_shouldReturnUserDetails_whenUserFound() {
        // Given: Set up a UserEntity with roles and permissions
        // Use 'savedOwner' from setUp or create a new one specific for this test
        UserEntity foundUser = new Owner(); // Example using Owner
        foundUser.setId(1L);
        foundUser.setUsername("testuser");
        foundUser.setEmail("test@example.com");
        foundUser.setPassword("hashedPassword"); // The hashed password
        foundUser.setEnabled(true);
        foundUser.setAccountNonExpired(true);
        foundUser.setAccountNonLocked(true);
        foundUser.setCredentialsNonExpired(true);

        RoleEntity roleOwner = RoleEntity.builder().id(1L).roleEnum(RoleEnum.OWNER).build();
        PermissionEntity permReadOwn = PermissionEntity.builder().id(1L).name("PET_READ_OWN").build();
        PermissionEntity permCreateOwn = PermissionEntity.builder().id(2L).name("PET_CREATE_OWN").build();
        roleOwner.setPermissionList(Set.of(permReadOwn, permCreateOwn)); // Associate permissions with the role
        foundUser.setRoles(Set.of(roleOwner)); // Assign the role to the user

        // Mock repository behavior
        given(userRepository.findByUsername("testuser")).willReturn(Optional.of(foundUser));

        // When: Call the method under test
        UserDetails userDetails = authService.loadUserByUsername("testuser");

        // Then: Verify the UserDetails object
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("testuser");
        assertThat(userDetails.getPassword()).isEqualTo("hashedPassword");
        assertThat(userDetails.isEnabled()).isTrue();
        assertThat(userDetails.isAccountNonExpired()).isTrue();
        assertThat(userDetails.isAccountNonLocked()).isTrue();
        assertThat(userDetails.isCredentialsNonExpired()).isTrue();

        // Verify authorities include the role (with ROLE_ prefix) and the permissions
        assertThat(userDetails.getAuthorities()).hasSize(3); // ROLE_OWNER + 2 permissions
        assertThat(userDetails.getAuthorities())
                .extracting(GrantedAuthority::getAuthority) // Extract authority strings
                .containsExactlyInAnyOrder("ROLE_OWNER", "PET_READ_OWN", "PET_CREATE_OWN");

        // Verify repository interaction
        then(userRepository).should().findByUsername("testuser");
    }

    @Test
    void loadUserByUsername_shouldThrowUsernameNotFoundException_whenUserNotFound() {
        // Given: Mock repository returns empty Optional
        given(userRepository.findByUsername("unknownuser")).willReturn(Optional.empty());

        // When & Then: Expect the exception
        assertThatThrownBy(() -> authService.loadUserByUsername("unknownuser"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("El usuario unknownuser no existe."); // Check your exception message

        // Verify repository interaction
        then(userRepository).should().findByUsername("unknownuser");
    }


    // --- Tests for loginUser (that internally use authenticating and loadUserByUsername) ---

    @Test
    void loginUser_shouldReturnAuthResponseDtoWithJwt_whenCredentialsAreValid() {
        // Given: Setup login request and mock underlying authentication process
        AuthLoginRequestDto loginRequest = new AuthLoginRequestDto("testuser", "password123");
        String expectedJwt = "mocked.jwt.token";

        // --- Mocking the steps within loginUser and authenticate ---
        // 1. Mock loadUserByUsername (called by authenticating)
        UserEntity foundUser = new Owner(); // Reuse setup from the previous test or create new
        foundUser.setUsername("testuser");
        foundUser.setPassword("hashedPassword"); // This MUST match the encoded password check
        foundUser.setEnabled(true);
        foundUser.setAccountNonExpired(true);
        foundUser.setAccountNonLocked(true);
        foundUser.setCredentialsNonExpired(true);
        RoleEntity roleOwner = RoleEntity.builder().id(1L).roleEnum(RoleEnum.OWNER).permissionList(Set.of()).build(); // Simple role
        foundUser.setRoles(Set.of(roleOwner));
        given(userRepository.findByUsername("testuser")).willReturn(Optional.of(foundUser));

        // 2. Mock passwordEncoder.matches (called by authenticating)
        given(passwordEncoder.matches("password123", "hashedPassword")).willReturn(true); // Crucial: Simulate the correct password match

        // Mock jwtUtils.createToken (called by loginUser after successful authentication)
        // We need to capture the Authentication object passed to createToken to verify it if needed,
        // or just mock its return value.
        // ArgumentCaptor could be used for detailed verification, but mocking return is simpler here.
        given(jwtUtils.createToken(any(Authentication.class))).willReturn(expectedJwt);
        // --- End Mocking ---


        // When: Call loginUser
        AuthResponseDto responseDto = authService.loginUser(loginRequest);

        // Then: Verify the response DTO
        assertThat(responseDto).isNotNull();
        assertThat(responseDto.username()).isEqualTo("testuser");
        assertThat(responseDto.jwt()).isEqualTo(expectedJwt);
        assertThat(responseDto.status()).isTrue();
        assertThat(responseDto.message()).isEqualTo("UserEntity logged successfully"); // Check a message if important

        // Verify mocks were called as expected
        then(userRepository).should().findByUsername("testuser");
        then(passwordEncoder).should().matches("password123", "hashedPassword");
        then(jwtUtils).should().createToken(any(Authentication.class)); // Verify token creation was triggered
    }

    @Test
    void loginUser_shouldThrowBadCredentialsException_whenPasswordIsIncorrect() {
        // Given: Setup login request
        AuthLoginRequestDto loginRequest = new AuthLoginRequestDto("testuser", "wrongPassword");

        // --- Mocking for failure ---
        // Mock loadUserByUsername - User is found
        UserEntity foundUser = new Owner();
        foundUser.setUsername("testuser");
        foundUser.setPassword("hashedPassword"); // Correct hash
        foundUser.setEnabled(true); // ... set other flags ...
        given(userRepository.findByUsername("testuser")).willReturn(Optional.of(foundUser));

        // ock passwordEncoder.matches - Simulate INCORRECT password match
        given(passwordEncoder.matches("wrongPassword", "hashedPassword")).willReturn(false);
        // --- End Mocking ---

        // When & Then: Expect BadCredentialsException (thrown by authenticating, called by loginUser)
        assertThatThrownBy(() -> authService.loginUser(loginRequest))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Incorrect Password"); // Check the specific message from authenticate()

        // Verify mocks
        then(userRepository).should().findByUsername("testuser");
        then(passwordEncoder).should().matches("wrongPassword", "hashedPassword");
        then(jwtUtils).should(never()).createToken(any(Authentication.class)); // Token should NOT be created
    }

    @Test
    void loginUser_shouldThrowUsernameNotFoundException_whenUserNotFound() {
        // Given: Setup login request
        AuthLoginRequestDto loginRequest = new AuthLoginRequestDto("unknownuser", "password123");

        // --- Mocking for failure ---
        // 1. Mock loadUserByUsername - User is NOT found (called by authenticating)
        given(userRepository.findByUsername("unknownuser")).willReturn(Optional.empty());
        // --- End Mocking ---

        // When & Then: Expect UsernameNotFoundException (thrown by loadUserByUsername, propagated by authenticating)
        assertThatThrownBy(() -> authService.loginUser(loginRequest))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("El usuario unknownuser no existe.");

        // Verify mocks
        then(userRepository).should().findByUsername("unknownuser");
        then(passwordEncoder).should(never()).matches(anyString(), anyString()); // Password check shouldn't happen
        then(jwtUtils).should(never()).createToken(any(Authentication.class)); // Token should NOT be created
    }
}

