package com.petconnect.backend.user.application.service.impl;

import com.petconnect.backend.common.service.EmailService;
import com.petconnect.backend.exception.EmailAlreadyExistsException;
import com.petconnect.backend.exception.InvalidPasswordResetTokenException;
import com.petconnect.backend.exception.UsernameAlreadyExistsException;
import com.petconnect.backend.security.JwtUtils;
import com.petconnect.backend.user.application.dto.*;
import com.petconnect.backend.user.application.mapper.UserMapper;
import com.petconnect.backend.user.application.service.AuthService;
import com.petconnect.backend.user.application.service.UserService;
import com.petconnect.backend.user.domain.model.*;
import com.petconnect.backend.user.domain.repository.OwnerRepository;
import com.petconnect.backend.user.domain.repository.PasswordResetTokenRepository;
import com.petconnect.backend.user.domain.repository.RoleRepository;
import com.petconnect.backend.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Implementation of the AuthService interface.
 * Handles Owner registration.
 *
 * @author ibosquet
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService{
    private final JwtUtils jwtUtils;
    private final UserRepository userRepository;
    private final OwnerRepository ownerRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private static final String DEFAULT_OWNER_AVATAR = "images/avatars/users/owner.png";
    private final RoleRepository roleRepository;
    private final UserService userService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;

    private static final int EXPIRATION_HOURS = 1;


    /**
     * {@inheritDoc}
     * Registers a new Owner after validating email/username uniqueness and hashing the password.
     */
    @Override
    @Transactional
    public OwnerProfileDto registerOwner(OwnerRegistrationDto registrationDTO) {

        if (userRepository.existsByEmail(registrationDTO.email())) {
            throw new EmailAlreadyExistsException(registrationDTO.email());
        }

        if (userRepository.existsByUsername(registrationDTO.username())) {
         throw new UsernameAlreadyExistsException(registrationDTO.username());
        }


        // Create a new Owner entity
        Owner newOwner = new Owner();

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
        newOwner.setAvatar(DEFAULT_OWNER_AVATAR);
        newOwner.setEnabled(true);
        newOwner.setAccountNonExpired(true);
        newOwner.setAccountNonLocked(true);
        newOwner.setCredentialsNonExpired(true);

        Owner savedOwner = ownerRepository.save(newOwner);
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
        return new AuthResponseDto(username, "UserEntity logged successfully", accessToken, true);
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

        UserDetails userDetails = this.userService.loadUserByUsername(username);
        if (userDetails == null) throw new BadCredentialsException("Invalid username or password");
        if (!passwordEncoder.matches(password, userDetails.getPassword())) throw new BadCredentialsException("Incorrect Password");
        return new UsernamePasswordAuthenticationToken(username, userDetails.getPassword(), userDetails.getAuthorities());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void requestPasswordReset(PasswordResetRequestDto requestDto) {
        log.info("Processing password reset request for email: {}", requestDto.email());
        Optional<UserEntity> userOpt = userRepository.findByEmail(requestDto.email());

        if (userOpt.isPresent()) {
            UserEntity user = userOpt.get();

            String token = UUID.randomUUID().toString();
            LocalDateTime expiryDate = LocalDateTime.now().plusHours(EXPIRATION_HOURS);

            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .token(token)
                    .user(user)
                    .expiryDate(expiryDate)
                    .build();
            passwordResetTokenRepository.save(resetToken);
            log.info("Generated and saved password reset token for user ID {}", user.getId());

            String recipientName;
            if ((user instanceof Owner owner)) {
                recipientName = owner.getUsername();
            } else {
                if (user instanceof ClinicStaff staff) recipientName = staff.getName();
                else recipientName = user.getUsername();
            }

            emailService.sendPasswordResetEmail(user.getEmail(), recipientName, token);

        } else {
            log.warn("Password reset requested for non-existent email: {}", requestDto.email());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void resetPassword(PasswordResetDto resetDto) {
        log.info("Attempting to reset password with token: {}", resetDto.token());

        if (!resetDto.newPassword().equals(resetDto.confirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match.");
        }

        PasswordResetToken passwordResetToken = passwordResetTokenRepository.findByToken(resetDto.token())
                .orElseThrow(() -> new InvalidPasswordResetTokenException("Token not found."));

        if (passwordResetToken.isExpired()) {
            passwordResetTokenRepository.delete(passwordResetToken);
            throw new InvalidPasswordResetTokenException("Token has expired.");
        }

        UserEntity user = passwordResetToken.getUser();
        if (user == null) {
            log.error("PasswordResetToken {} has no associated user!", passwordResetToken.getId());
            passwordResetTokenRepository.delete(passwordResetToken);
            throw new InvalidPasswordResetTokenException("Invalid token state.");
        }

        user.setPassword(passwordEncoder.encode(resetDto.newPassword()));
        userRepository.save(user);
        log.info("Password successfully reset for user ID {}", user.getId());

        passwordResetTokenRepository.delete(passwordResetToken);
        log.info("Password reset token {} invalidated after use.", resetDto.token());
    }
}
