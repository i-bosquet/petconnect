package com.petconnect.backend.user.application.service.impl;

import com.petconnect.backend.exception.EmailAlreadyExistsException;
import com.petconnect.backend.exception.UsernameAlreadyExistsException;
import com.petconnect.backend.security.JwtUtils;
import com.petconnect.backend.user.application.dto.AuthLoginRequestDto;
import com.petconnect.backend.user.application.dto.AuthResponseDto;
import com.petconnect.backend.user.application.dto.OwnerProfileDto;
import com.petconnect.backend.user.application.dto.OwnerRegistrationDto;
import com.petconnect.backend.user.application.mapper.UserMapper;
import com.petconnect.backend.user.application.service.UserService;
import com.petconnect.backend.user.domain.model.Owner;
import com.petconnect.backend.user.domain.model.RoleEntity;
import com.petconnect.backend.user.domain.model.RoleEnum;
import com.petconnect.backend.user.domain.repository.OwnerRepository;
import com.petconnect.backend.user.domain.repository.RoleRepository;
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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.never;

/**
 * Unit tests for {@link AuthServiceImpl}.
 * Verifies the logic for owner registration and user login/authentication.
 *
 * @author ibosquet
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {
    // --- Mocks ---
    @Mock private JwtUtils jwtUtils;
    @Mock private UserRepository userRepository;
    @Mock private OwnerRepository ownerRepository;
    @Mock private UserMapper userMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private RoleRepository roleRepository;
    @Mock private UserService userService;

    // --- Class Under Test ---
    @InjectMocks
    private AuthServiceImpl authService;

    // --- Captors ---
    @Captor private ArgumentCaptor<Owner> ownerCaptor;
    @Captor private ArgumentCaptor<Authentication> authenticationCaptor;

    // --- Test Data ---
    private OwnerRegistrationDto registrationDto;
    private RoleEntity ownerRole;
    private Owner savedOwner;
    private OwnerProfileDto expectedOwnerDto;
    private AuthLoginRequestDto loginRequestDto;
    private UserDetails userDetails;
    private String defaultAvatar;


    @BeforeEach
    void setUp() {
        registrationDto = new OwnerRegistrationDto("testuser", "test@example.com", "password123", "123456789");
        ownerRole = RoleEntity.builder().roleEnum(RoleEnum.OWNER).id(1L).build();

        savedOwner = new Owner();
        savedOwner.setId(1L);
        savedOwner.setUsername(registrationDto.username());
        savedOwner.setEmail(registrationDto.email());
        savedOwner.setPhone(registrationDto.phone());
        savedOwner.setPassword("hashedPassword");
        savedOwner.setRoles(Set.of(ownerRole));
        savedOwner.setAvatar("images/avatars/users/owner.png");
        savedOwner.setEnabled(true);
        savedOwner.setAccountNonExpired(true);
        savedOwner.setAccountNonLocked(true);
        savedOwner.setCredentialsNonExpired(true);

        expectedOwnerDto = new OwnerProfileDto(savedOwner.getId(), savedOwner.getUsername(), savedOwner.getEmail(), Set.of("OWNER"), savedOwner.getAvatar(), savedOwner.getPhone());

        loginRequestDto = new AuthLoginRequestDto("testuser", "password123");

        userDetails = new User(savedOwner.getUsername(), savedOwner.getPassword(), true, true, true, true,
                List.of(new SimpleGrantedAuthority("ROLE_OWNER")));
        defaultAvatar = (String) ReflectionTestUtils.getField(AuthServiceImpl.class, "DEFAULT_OWNER_AVATAR");

    }

    /**
     * --- Tests for registerOwner ---
     */
    @Nested
    @DisplayName("registerOwner Tests")
    class RegisterOwnerTests {

        @Test
        @DisplayName("should register Owner successfully when data is valid")
        void shouldRegisterOwnerSuccessfully() {
            // Arrange
            given(userRepository.existsByEmail(registrationDto.email())).willReturn(false);
            given(userRepository.existsByUsername(registrationDto.username())).willReturn(false);
            given(passwordEncoder.encode(registrationDto.password())).willReturn("hashedPassword");
            given(roleRepository.findByRoleEnum(RoleEnum.OWNER)).willReturn(Optional.of(ownerRole));
            given(ownerRepository.save(any(Owner.class))).willReturn(savedOwner);
            given(userMapper.toOwnerProfileDto(savedOwner)).willReturn(expectedOwnerDto);

            // Act
            OwnerProfileDto result = authService.registerOwner(registrationDto);

            // Assert
            assertThat(result).isEqualTo(expectedOwnerDto);

            then(userRepository).should().existsByEmail(registrationDto.email());
            then(userRepository).should().existsByUsername(registrationDto.username());
            then(passwordEncoder).should().encode(registrationDto.password());
            then(roleRepository).should().findByRoleEnum(RoleEnum.OWNER);
            then(ownerRepository).should().save(ownerCaptor.capture());
            then(userMapper).should().toOwnerProfileDto(savedOwner);

            Owner ownerToSave = ownerCaptor.getValue();
            assertThat(ownerToSave.getUsername()).isEqualTo(registrationDto.username());
            assertThat(ownerToSave.getEmail()).isEqualTo(registrationDto.email());
            assertThat(ownerToSave.getPhone()).isEqualTo(registrationDto.phone());
            assertThat(ownerToSave.getPassword()).isEqualTo("hashedPassword");
            assertThat(ownerToSave.getRoles()).containsExactly(ownerRole);
            assertThat(ownerToSave.getAvatar()).isEqualTo(defaultAvatar);
            assertThat(ownerToSave.isEnabled()).isTrue();
            assertThat(ownerToSave.isAccountNonExpired()).isTrue();
            assertThat(ownerToSave.isAccountNonLocked()).isTrue();
            assertThat(ownerToSave.isCredentialsNonExpired()).isTrue();
        }

        @Test
        @DisplayName("should throw EmailAlreadyExistsException when email is taken")
        void shouldThrowEmailExists() {
            // Arrange
            given(userRepository.existsByEmail(registrationDto.email())).willReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> authService.registerOwner(registrationDto))
                    .isInstanceOf(EmailAlreadyExistsException.class)
                    .hasMessageContaining(registrationDto.email());

            then(userRepository).should().existsByEmail(registrationDto.email());
            then(userRepository).should(never()).existsByUsername(anyString());
            then(ownerRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("should throw UsernameAlreadyExistsException when username is taken")
        void shouldThrowUsernameExists() {
            // Arrange
            given(userRepository.existsByEmail(registrationDto.email())).willReturn(false);
            given(userRepository.existsByUsername(registrationDto.username())).willReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> authService.registerOwner(registrationDto))
                    .isInstanceOf(UsernameAlreadyExistsException.class)
                    .hasMessageContaining(registrationDto.username());

            then(userRepository).should().existsByEmail(registrationDto.email());
            then(userRepository).should().existsByUsername(registrationDto.username());
            then(ownerRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("should throw IllegalStateException when OWNER role not found")
        void shouldThrowIllegalStateWhenRoleNotFound() {
            // Arrange
            given(userRepository.existsByEmail(registrationDto.email())).willReturn(false);
            given(userRepository.existsByUsername(registrationDto.username())).willReturn(false);
            given(passwordEncoder.encode(registrationDto.password())).willReturn("hashedPassword");
            given(roleRepository.findByRoleEnum(RoleEnum.OWNER)).willReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> authService.registerOwner(registrationDto))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("OWNER role not found");

            then(userRepository).should().existsByEmail(registrationDto.email());
            then(userRepository).should().existsByUsername(registrationDto.username());
            then(passwordEncoder).should().encode(registrationDto.password());
            then(roleRepository).should().findByRoleEnum(RoleEnum.OWNER);
            then(ownerRepository).should(never()).save(any());
        }
    }

    /**
     * --- Tests for loginUser ---
     */
    @Nested
    @DisplayName("loginUser Tests")
    class LoginUserTests {

        @Test
        @DisplayName("should return AuthResponseDto with JWT on successful login")
        void shouldReturnJwtOnSuccessfulLogin() {
            String jwtToken = "test.jwt.token";
            // Arrange
            given(userService.loadUserByUsername(loginRequestDto.username())).willReturn(userDetails);
            given(passwordEncoder.matches(loginRequestDto.password(), userDetails.getPassword())).willReturn(true);
            given(jwtUtils.createToken(any(Authentication.class))).willReturn(jwtToken);

            // Act
            AuthResponseDto response = authService.loginUser(loginRequestDto);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.username()).isEqualTo(loginRequestDto.username());
            assertThat(response.jwt()).isEqualTo(jwtToken);
            assertThat(response.status()).isTrue();
            assertThat(response.message()).contains("logged successfully");

            then(userService).should().loadUserByUsername(loginRequestDto.username());
            then(passwordEncoder).should().matches(loginRequestDto.password(), userDetails.getPassword());
            then(jwtUtils).should().createToken(authenticationCaptor.capture());

            Authentication authPassedToJwt = authenticationCaptor.getValue();
            assertThat(authPassedToJwt).isNotNull();
            assertThat(authPassedToJwt.getName()).isEqualTo(loginRequestDto.username());

            List<String> expectedAuthorityNames = userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .toList();
            List<String> actualAuthorityNames = authPassedToJwt.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .toList();

            assertThat(actualAuthorityNames).containsExactlyInAnyOrderElementsOf(expectedAuthorityNames);
        }

        @Test
        @DisplayName("should throw BadCredentialsException when username not found during login")
        void shouldThrowBadCredentialsWhenUsernameNotFound() {
            // Arrange
            given(userService.loadUserByUsername(loginRequestDto.username()))
                    .willThrow(new UsernameNotFoundException("User not found"));

            // Act & Assert
            assertThatThrownBy(() -> authService.loginUser(loginRequestDto))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessageContaining("not found");

            then(userService).should().loadUserByUsername(loginRequestDto.username());
            then(passwordEncoder).should(never()).matches(anyString(), anyString());
            then(jwtUtils).should(never()).createToken(any());
        }

        @Test
        @DisplayName("should throw BadCredentialsException when password does not match")
        void shouldThrowBadCredentialsWhenPasswordMismatch() {
            // Arrange
            given(userService.loadUserByUsername(loginRequestDto.username())).willReturn(userDetails);
            given(passwordEncoder.matches(loginRequestDto.password(), userDetails.getPassword())).willReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> authService.loginUser(loginRequestDto))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessageContaining("Incorrect Password");

            then(userService).should().loadUserByUsername(loginRequestDto.username());
            then(passwordEncoder).should().matches(loginRequestDto.password(), userDetails.getPassword());
            then(jwtUtils).should(never()).createToken(any());
        }
    }

    /**
     * --- Tests for authenticate method directly
     */
    @Nested
    @DisplayName("authenticate Method Tests")
    class AuthenticateMethodTests {

        @Test
        @DisplayName("authenticate should return Authentication object on success")
        void authenticateSuccess() {
            // Arrange
            given(userService.loadUserByUsername(loginRequestDto.username())).willReturn(userDetails);
            given(passwordEncoder.matches(loginRequestDto.password(), userDetails.getPassword())).willReturn(true);

            // Act
            Authentication result = authService.authenticate(loginRequestDto.username(), loginRequestDto.password());

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo(loginRequestDto.username());
            assertThat(result).isInstanceOf(UsernamePasswordAuthenticationToken.class);
            assertThat(result.isAuthenticated()).isTrue();

            List<String> expectedAuthorityNames = userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .toList();
            List<String> actualAuthorityNames = result.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .toList();
            assertThat(actualAuthorityNames).containsExactlyInAnyOrderElementsOf(expectedAuthorityNames);
        }

        @Test
        @DisplayName("authenticate should throw BadCredentialsException for null UserDetails")
        void authenticateThrowsForNullUserDetails() {
            // Arrange
            given(userService.loadUserByUsername(loginRequestDto.username())).willReturn(null);

            // Act
            Throwable thrown = Assertions.catchThrowable(() ->authService.authenticate(loginRequestDto.username(), loginRequestDto.password()));
            // Assert
            assertThat(thrown)
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessageContaining("Invalid username or password");

            then(passwordEncoder).should(never()).matches(any(), any());
        }

        @Test
        @DisplayName("authenticate should throw BadCredentialsException for wrong password")
        void authenticateThrowsForWrongPassword() {
            // Arrange
            given(userService.loadUserByUsername(loginRequestDto.username())).willReturn(userDetails);
            given(passwordEncoder.matches(loginRequestDto.password(), userDetails.getPassword())).willReturn(false); // Mismatch


            // Act
            Throwable thrown = Assertions.catchThrowable(() ->authService.authenticate(loginRequestDto.username(), loginRequestDto.password()));
            // Assert
            assertThat(thrown)
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessageContaining("Incorrect Password");
        }

        @Test
        @DisplayName("authenticate should propagate UsernameNotFoundException from UserService")
        void authenticatePropagatesUsernameNotFound() {
            // Arrange
            given(userService.loadUserByUsername(loginRequestDto.username()))
                    .willThrow(new UsernameNotFoundException("Test User Not Found"));

            // Act
            Throwable thrown = Assertions.catchThrowable(() ->authService.authenticate(loginRequestDto.username(), loginRequestDto.password()));
            // Assert
            assertThat(thrown)
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessageContaining("Test User Not Found");

            then(passwordEncoder).should(never()).matches(any(), any());
        }
    }
}

