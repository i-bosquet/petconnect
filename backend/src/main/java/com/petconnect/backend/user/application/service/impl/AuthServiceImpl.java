package com.petconnect.backend.user.application.service.impl;

import com.petconnect.backend.exception.EmailAlreadyExistsException;
import com.petconnect.backend.exception.UsernameAlreadyExistsException;
import com.petconnect.backend.security.JwtUtils;
import com.petconnect.backend.user.application.dto.AuthLoginRequestDto;
import com.petconnect.backend.user.application.dto.AuthResponseDto;
import com.petconnect.backend.user.application.dto.OwnerProfileDto;
import com.petconnect.backend.user.application.dto.OwnerRegistrationDto;
import com.petconnect.backend.user.application.mapper.UserMapper;
import com.petconnect.backend.user.application.service.AuthService;
import com.petconnect.backend.user.domain.model.Owner;
import com.petconnect.backend.user.domain.model.RoleEntity;
import com.petconnect.backend.user.domain.model.RoleEnum;
import com.petconnect.backend.user.domain.model.UserEntity;
import com.petconnect.backend.user.domain.repository.OwnerRepository;
import com.petconnect.backend.user.domain.repository.RoleRepository;
import com.petconnect.backend.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Implementation of the AuthService interface.
 * Handles Owner registration.
 *
 * @author ibosquet
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService, UserDetailsService {
    private final JwtUtils jwtUtils;
    private final UserRepository userRepository;
    private final OwnerRepository ownerRepository; // Need Owner repo to save Owner specific data
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private static final String DEFAULT_OWNER_AVATAR = "images/avatars/users/owner.png";
    private final RoleRepository roleRepository;

    /**
     * Loads user-specific data. This method is required by the UserDetailsService interface.
     * It uses the username (which could be email in our case) to find the user.
     *
     * @param username The username (or email) identifying the user whose data is required.
     * @return a fully populated {@link UserDetails} object (never {@code null})
     * @throws UsernameNotFoundException if the user could not be found or the user has no
     *                                   GrantedAuthority
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException  {
        // Find user by username
        UserEntity userEntity = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("El usuario " + username + " no existe."));

        List<SimpleGrantedAuthority> authorityList = new ArrayList<>();

        // Add role authorities (prefix "ROLE_")
        userEntity.getRoles()
                .forEach(role -> authorityList.
                        add(new SimpleGrantedAuthority("ROLE_".concat(role.getRoleEnum().name()))));

        // Add permission authorities from roles
        userEntity.getRoles()
                .stream()
                .flatMap(role -> role
                        .getPermissionList()
                        .stream())
                .forEach(permission -> authorityList.add(new SimpleGrantedAuthority(permission.getName())));

        // Return Spring Security User object with credentials and authorities
        return new User(userEntity.getUsername(),
                userEntity.getPassword(),
                userEntity.isEnabled(),
                userEntity.isAccountNonExpired(),
                userEntity.isCredentialsNonExpired(),
                userEntity.isAccountNonLocked(),
                authorityList) ;
    }

    /**
     * {@inheritDoc}
     * Registers a new Owner after validating email/username uniqueness and hashing the password.
     */
    @Override
    @Transactional
    public OwnerProfileDto registerOwner(OwnerRegistrationDto registrationDTO) {

        // Check if email already exists
        if (userRepository.existsByEmail(registrationDTO.email())) {
            throw new EmailAlreadyExistsException(registrationDTO.email());
        }
        // Check if username already exists (Assuming UserRepository gets a existsByUsername method)
        if (userRepository.existsByUsername(registrationDTO.username())) {
         throw new UsernameAlreadyExistsException(registrationDTO.username());
        }


        // Create new Owner entity
        Owner newOwner = new Owner(); // Using NoArgsConstructor + Setters

        // Set fields from DTO
        newOwner.setUsername(registrationDTO.username());
        newOwner.setEmail(registrationDTO.email());
        newOwner.setPhone(registrationDTO.phone());
        // Hash the password before setting
        newOwner.setPassword(passwordEncoder.encode(registrationDTO.password()));
        // Set defaults
        RoleEntity ownerRole = roleRepository.findByRoleEnum(RoleEnum.OWNER)
                .orElseThrow(() -> new IllegalStateException("OWNER role not found in database!"));
        Set<RoleEntity> roles = new HashSet<>();
        roles.add(ownerRole);
        newOwner.setRoles(roles);
        newOwner.setAvatar(DEFAULT_OWNER_AVATAR); // Set default avatar path
        newOwner.setEnabled(true);
        newOwner.setAccountNonExpired(true);
        newOwner.setAccountNonLocked(true);
        newOwner.setCredentialsNonExpired(true);

        // Save the new Owner
        // Because Owner extends UserEntity with JOINED strategy, saving Owner will also insert into UserEntity table.
        Owner savedOwner = ownerRepository.save(newOwner);

        // Map to DTO and return
        return userMapper.toOwnerProfileDto(savedOwner);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AuthResponseDto loginUser(AuthLoginRequestDto authLoginRequest) {
        String username = authLoginRequest.username();
        String password = authLoginRequest.password();

        Authentication authentication = this.authenticate(username, password);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        String accessToken = jwtUtils.createToken(authentication);
        return new AuthResponseDto(username, "UserEntity loged succesfully", accessToken, true);
    }

    /**
     * Authenticates a user with the provided username and password.
     *
     * @param username the username to authenticate
     * @param password the password to authenticate
     * @return an Authentication object if authentication is successful
     * @throws BadCredentialsException if the username is not found or the password is incorrect
     */
    public Authentication authenticate(String username, String password) throws BadCredentialsException {
        UserDetails userDetails = this.loadUserByUsername(username);

        if (userDetails == null) throw new BadCredentialsException("Invalid username or password");

        if (!passwordEncoder.matches(password, userDetails.getPassword())) throw new BadCredentialsException("Incorrect Password");

        // Return an authentication token with granted authorities
        return new UsernamePasswordAuthenticationToken(username, userDetails.getPassword(), userDetails.getAuthorities());
    }
}
