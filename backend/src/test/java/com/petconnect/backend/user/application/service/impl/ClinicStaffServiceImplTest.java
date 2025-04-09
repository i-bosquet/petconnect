package com.petconnect.backend.user.application.service.impl;

import com.petconnect.backend.exception.*; // Import all your custom exceptions
import com.petconnect.backend.user.application.dto.ClinicStaffCreationDto;
import com.petconnect.backend.user.application.dto.ClinicStaffProfileDto;
import com.petconnect.backend.user.application.dto.ClinicStaffUpdateDto;
import com.petconnect.backend.user.application.mapper.UserMapper;
import com.petconnect.backend.user.domain.model.*;
import com.petconnect.backend.user.domain.repository.*;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.petconnect.backend.user.application.service.impl.ClinicStaffServiceImpl.DEFAULT_ADMIN_AVATAR;
import static com.petconnect.backend.user.application.service.impl.ClinicStaffServiceImpl.DEFAULT_VET_AVATAR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ClinicStaffServiceImpl}.
 * Verifies the business logic for creating, updating, activating, deactivating,
 * and retrieving clinic staff members, including authorization checks.
 *
 * @author ibosquet
 */
@ExtendWith(MockitoExtension.class) // Initialize Mockito environment
class ClinicStaffServiceImplTest {
    // --- Mocks ---
    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private ClinicRepository clinicRepository;
    @Mock private ClinicStaffRepository clinicStaffRepository;
    @Mock private VetRepository vetRepository;
    @Mock private UserMapper userMapper;
    @Mock private PasswordEncoder passwordEncoder;

    // --- Class Under Test ---
    @InjectMocks
    private ClinicStaffServiceImpl clinicStaffService;

    // --- Captors ---
    @Captor private ArgumentCaptor<ClinicStaff> clinicStaffCaptor;

    // --- Test Data ---
    private Clinic clinic1;
    private ClinicStaff adminUser; // The admin performing actions
    private RoleEntity adminRole, vetRole;
    private ClinicStaff existingStaffMember; // Staff being acted upon
    private Vet existingVetMember; // Vet being acted upon


    @BeforeEach
    void setUp() {
        clinic1 = Clinic.builder().name("Test Clinic 1").city("Test City").build();
        clinic1.setId(1L);

        adminRole = RoleEntity.builder().roleEnum(RoleEnum.ADMIN).build();
        adminRole.setId(3L);
        vetRole = RoleEntity.builder().roleEnum(RoleEnum.VET).build();
        vetRole.setId(2L);

        adminUser = new ClinicStaff();
        adminUser.setId(10L);
        adminUser.setUsername("admin_user");
        adminUser.setName("Admin");
        adminUser.setSurname("Test");
        adminUser.setClinic(clinic1);
        adminUser.setRoles(Set.of(adminRole));
        adminUser.setActive(true);

        existingStaffMember = new ClinicStaff(); // This is an Admin staff member
        existingStaffMember.setId(11L);
        existingStaffMember.setUsername("existing_admin");
        existingStaffMember.setName("Existing");
        existingStaffMember.setSurname("Admin");
        existingStaffMember.setEmail("existing.admin@test.com");
        existingStaffMember.setClinic(clinic1);
        existingStaffMember.setRoles(Set.of(adminRole));
        existingStaffMember.setActive(true);
        existingStaffMember.setAvatar("images/avatars/users/admin.png");

        existingVetMember = new Vet(); // This is a Vet staff member
        existingVetMember.setId(12L);
        existingVetMember.setUsername("existing_vet");
        existingVetMember.setName("Existing");
        existingVetMember.setSurname("Vet");
        existingVetMember.setEmail("existing.vet@test.com");
        existingVetMember.setLicenseNumber("VET123");
        existingVetMember.setVetPublicKey("VETKEY123");
        existingVetMember.setClinic(clinic1);
        existingVetMember.setRoles(Set.of(vetRole));
        existingVetMember.setActive(true);
        existingVetMember.setAvatar("images/avatars/users/vet.png");
    }

    // --- Tests for createClinicStaff ---
    @Nested
    @DisplayName("createClinicStaff Tests")
    class CreateClinicStaffTests {

        private ClinicStaffCreationDto vetCreationDto;
        private ClinicStaffCreationDto adminCreationDto;
        private Vet savedVet;
        private ClinicStaff savedAdmin;

        private ClinicStaffProfileDto vetProfileDto;
        private ClinicStaffProfileDto adminProfileDto;


        @BeforeEach
        void createDtoSetup() {
            vetCreationDto = new ClinicStaffCreationDto(
                    "newvet", "new.vet@test.com", "password123",
                    "New", "Vet", RoleEnum.VET,
                    // clinicId removed from DTO
                    "VET999", "VETKEY999");

            adminCreationDto = new ClinicStaffCreationDto(
                    "newadmin", "new.admin@test.com", "password123",
                    "New", "Admin", RoleEnum.ADMIN,
                    null, null); // No vet fields

            // Simulate saved entities (IDs assigned, password hashed)
            savedVet = new Vet();
            savedVet.setId(100L);
            savedVet.setUsername(vetCreationDto.username());
            savedVet.setEmail(vetCreationDto.email());
            savedVet.setPassword("hashedPassword");
            savedVet.setName(vetCreationDto.name());
            savedVet.setSurname(vetCreationDto.surname());
            savedVet.setLicenseNumber(vetCreationDto.licenseNumber());
            savedVet.setVetPublicKey(vetCreationDto.vetPublicKey());
            savedVet.setClinic(clinic1);
            savedVet.setRoles(Set.of(vetRole));
            savedVet.setActive(true);
            savedVet.setAvatar(DEFAULT_VET_AVATAR);

            savedAdmin = new ClinicStaff();
            savedAdmin.setId(101L);
            savedAdmin.setUsername(adminCreationDto.username());
            savedAdmin.setEmail(adminCreationDto.email());
            savedAdmin.setPassword("hashedPassword");
            savedAdmin.setName(adminCreationDto.name());
            savedAdmin.setSurname(adminCreationDto.surname());
            savedAdmin.setClinic(clinic1);
            savedAdmin.setRoles(Set.of(adminRole));
            savedAdmin.setActive(true);
            savedAdmin.setAvatar(DEFAULT_ADMIN_AVATAR);

            vetProfileDto = new ClinicStaffProfileDto(
                    savedVet.getId(), savedVet.getUsername(), savedVet.getEmail(),
                    Set.of(RoleEnum.VET.name()), savedVet.getAvatar(),
                    savedVet.getName(), savedVet.getSurname(), savedVet.isActive(),
                    savedVet.getClinic().getId(), savedVet.getClinic().getName(),
                    savedVet.getLicenseNumber(), savedVet.getVetPublicKey()
            );
            adminProfileDto = new ClinicStaffProfileDto(
                    savedAdmin.getId(), savedAdmin.getUsername(), savedAdmin.getEmail(),
                    Set.of(RoleEnum.ADMIN.name()), savedAdmin.getAvatar(),
                    savedAdmin.getName(), savedAdmin.getSurname(), savedAdmin.isActive(),
                    savedAdmin.getClinic().getId(), savedAdmin.getClinic().getName(),
                    null, null // No vet fields for admin
            );
        }

        @Test
        @DisplayName("should create VET successfully when data is valid and admin authorized")
        void createClinicStaff_Success_Vet() {
            // Arrange
            when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));
            when(userRepository.existsByEmail(vetCreationDto.email())).thenReturn(false);
            when(userRepository.existsByUsername(vetCreationDto.username())).thenReturn(false);
            when(vetRepository.existsByLicenseNumber(vetCreationDto.licenseNumber())).thenReturn(false);
            when(vetRepository.existsByVetPublicKey(vetCreationDto.vetPublicKey())).thenReturn(false);
            when(passwordEncoder.encode(vetCreationDto.password())).thenReturn("hashedPassword");
            when(roleRepository.findByRoleEnum(RoleEnum.VET)).thenReturn(Optional.of(vetRole));
            when(clinicStaffRepository.save(any(Vet.class))).thenReturn(savedVet);
            when(userMapper.toClinicStaffProfileDto(any(Vet.class))).thenReturn(vetProfileDto);

            // Act
            ClinicStaffProfileDto result = clinicStaffService.createClinicStaff(vetCreationDto, adminUser.getId());

            // Assert
            assertEquals("newvet", result.username());
            assertEquals("New", result.name());
            assertEquals("VET999", result.licenseNumber());
        }

        @Test
        @DisplayName("should create ADMIN successfully when data is valid and admin authorized")
        void createClinicStaff_Success_Admin() {
            // Arrange
            given(userRepository.findById(adminUser.getId())).willReturn(Optional.of(adminUser));
            given(userRepository.existsByEmail(adminCreationDto.email())).willReturn(false);
            given(userRepository.existsByUsername(adminCreationDto.username())).willReturn(false);
            // No Vet checks needed for Admin role
            given(roleRepository.findByRoleEnum(RoleEnum.ADMIN)).willReturn(Optional.of(adminRole));
            given(passwordEncoder.encode(adminCreationDto.password())).willReturn("hashedPassword");
            given(clinicStaffRepository.save(any(ClinicStaff.class))).willReturn(savedAdmin); // Save returns the saved admin
            given(userMapper.toClinicStaffProfileDto(savedAdmin)).willReturn(adminProfileDto);

            // Act
            ClinicStaffProfileDto result = clinicStaffService.createClinicStaff(adminCreationDto, adminUser.getId());

            // Assert
            assertThat(result).isNotNull().isEqualTo(adminProfileDto);
            then(clinicStaffRepository).should().save(clinicStaffCaptor.capture()); // Capture ClinicStaff entity
            ClinicStaff captured = clinicStaffCaptor.getValue();
            assertThat(captured.getUsername()).isEqualTo(adminCreationDto.username());
            assertThat(captured.getClinic().getId()).isEqualTo(clinic1.getId());
            assertThat(captured.getRoles()).contains(adminRole);
            assertThat(captured.getPassword()).isEqualTo("hashedPassword");
        }

        @Test
        @DisplayName("should throw IllegalArgumentException for invalid role")
        void createClinicStaff_Error_InvalidRole() {
            ClinicStaffCreationDto dto = new ClinicStaffCreationDto("u", "e@e.c", "p", "N", "S", RoleEnum.OWNER, null, null);
            given(userRepository.findById(adminUser.getId())).willReturn(Optional.of(adminUser)); // Admin lookup still happens

            //Act
            Throwable thrown = Assertions.catchThrowable(() ->clinicStaffService.createClinicStaff(dto, adminUser.getId()));
            // Assert
            assertThat(thrown)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid role specified");
            then(clinicStaffRepository).should(never()).save(any());

        }

        @Test
        @DisplayName("should throw EmailAlreadyExistsException")
        void createClinicStaff_Error_EmailExists() {
            given(userRepository.findById(adminUser.getId())).willReturn(Optional.of(adminUser));
            given(userRepository.existsByEmail(vetCreationDto.email())).willReturn(true); // Email exists
            // Act
            Throwable thrown = Assertions.catchThrowable(() ->clinicStaffService.createClinicStaff(vetCreationDto, adminUser.getId()));
            // Assert
            assertThat(thrown)
                    .isInstanceOf(EmailAlreadyExistsException.class);

            then(clinicStaffRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("should throw UsernameAlreadyExistsException")
        void createClinicStaff_Error_UsernameExists() {
            given(userRepository.findById(adminUser.getId())).willReturn(Optional.of(adminUser));
            given(userRepository.existsByEmail(vetCreationDto.email())).willReturn(false); // Email ok
            given(userRepository.existsByUsername(vetCreationDto.username())).willReturn(true); // Username exists

            //Act
            Throwable thrown = Assertions.catchThrowable(() -> clinicStaffService.createClinicStaff(vetCreationDto, adminUser.getId()));
            // Assert
            assertThat(thrown)
                    .isInstanceOf(UsernameAlreadyExistsException.class);
            then(clinicStaffRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("should throw EntityNotFoundException if creating admin not found")
        void createClinicStaff_Error_AdminNotFound() {
            given(userRepository.findById(999L)).willReturn(Optional.empty()); // Admin not found

            assertThatThrownBy(() -> clinicStaffService.createClinicStaff(vetCreationDto, 999L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("performing action [create clinic staff] not found");
            then(clinicStaffRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("should throw AccessDeniedException if creator is not Admin")
        void createClinicStaff_Error_CreatorNotAdmin() {
            // Simulate creator being a Vet
            ClinicStaff creatorVet = new ClinicStaff();
            creatorVet.setId(15L);
            creatorVet.setClinic(clinic1);
            creatorVet.setRoles(Set.of(vetRole)); // Is a VET
            given(userRepository.findById(15L)).willReturn(Optional.of(creatorVet));

            assertThatThrownBy(() -> clinicStaffService.createClinicStaff(vetCreationDto, 15L))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("is not an authorized Admin");
            then(clinicStaffRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("should throw IllegalStateException if creator has no clinic")
        void createClinicStaff_Error_CreatorNoClinic() {
            // Simulate creator admin having clinic set to null
            adminUser.setClinic(null);
            given(userRepository.findById(adminUser.getId())).willReturn(Optional.of(adminUser));

            // Act
            Throwable thrown = Assertions.catchThrowable(() -> clinicStaffService.createClinicStaff(vetCreationDto, adminUser.getId()));

            // Assert
            assertThat(thrown)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("is not associated with any clinic");
            then(clinicStaffRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("should throw IllegalArgumentException if Vet license is missing")
        void createClinicStaff_Error_VetLicenseMissing() {
            ClinicStaffCreationDto missingLicenseDto = new ClinicStaffCreationDto("v","e@v.c","p","N","S",RoleEnum.VET,null, "KEY");
            given(userRepository.findById(adminUser.getId())).willReturn(Optional.of(adminUser));
            given(userRepository.existsByEmail(missingLicenseDto.email())).willReturn(false);
            given(userRepository.existsByUsername(missingLicenseDto.username())).willReturn(false);

            // Act
            Throwable thrown = Assertions.catchThrowable(() -> clinicStaffService.createClinicStaff(missingLicenseDto, adminUser.getId()));
            // Assert
            assertThat(thrown)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("License number is required");
            then(clinicStaffRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("should throw IllegalArgumentException if Vet public key is missing")
        void createClinicStaff_Error_VetKeyMissing() {
            ClinicStaffCreationDto missingKeyDto = new ClinicStaffCreationDto("v","e@v.c","p","N","S",RoleEnum.VET,"LICENSE123", null);
            given(userRepository.findById(adminUser.getId())).willReturn(Optional.of(adminUser));
            given(userRepository.existsByEmail(missingKeyDto.email())).willReturn(false);
            given(userRepository.existsByUsername(missingKeyDto.username())).willReturn(false);

            // Act
            Throwable thrown = Assertions.catchThrowable(() ->clinicStaffService.createClinicStaff(missingKeyDto, adminUser.getId()));
            // Assert
            assertThat(thrown)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("public key is required");
            then(clinicStaffRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("should throw LicenseNumberAlreadyExistsException")
        void createClinicStaff_Error_VetLicenseExists() {
            // Arrange
            when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));
            when(userRepository.existsByEmail("new.vet@test.com")).thenReturn(false);
            when(userRepository.existsByUsername("newvet")).thenReturn(false);
            doThrow(new LicenseNumberAlreadyExistsException("VET999"))
                    .when(vetRepository).existsByLicenseNumber("VET999");

            // Act
            Executable executable = () -> clinicStaffService.createClinicStaff(vetCreationDto, adminUser.getId());

            // Assert
            assertThrows(LicenseNumberAlreadyExistsException.class, executable);
        }

        @Test
        @DisplayName("should throw VetPublicKeyAlreadyExistsException")
        void createClinicStaff_Error_VetPublicKeyExists() {
            // Arrange
            when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));
            when(userRepository.existsByEmail("new.vet@test.com")).thenReturn(false);
            when(userRepository.existsByUsername("newvet")).thenReturn(false);
            when(vetRepository.existsByLicenseNumber("VET999")).thenReturn(false);
            doThrow(new VetPublicKeyAlreadyExistsException())
                    .when(vetRepository).existsByVetPublicKey("VETKEY999");

            // Act
            Executable executable = () -> clinicStaffService.createClinicStaff(vetCreationDto, adminUser.getId());

            // Assert
            assertThrows(VetPublicKeyAlreadyExistsException.class, executable);
        }
    }

    // --- Tests for updateClinicStaff ---
    @Nested
    @DisplayName("updateClinicStaff Tests")
    class UpdateClinicStaffTests {
        private ClinicStaffUpdateDto adminUpdateDto;
        private ClinicStaffUpdateDto vetUpdateDto;
        private ClinicStaffProfileDto updatedAdminProfileDto;
        private ClinicStaffProfileDto updatedVetProfileDto;

        @BeforeEach
        void updateDtoSetup() {
            adminUpdateDto = new ClinicStaffUpdateDto("UpdatedAdminName", "UpdatedAdminSurname", null, null);
            vetUpdateDto = new ClinicStaffUpdateDto("UpdatedVetName", "UpdatedVetSurname", "VET999UPD", "VETKEY999UPD");

            // Simulate expected DTOs after update
            updatedAdminProfileDto = new ClinicStaffProfileDto(
                    existingStaffMember.getId(), existingStaffMember.getUsername(), existingStaffMember.getEmail(),
                    Set.of(RoleEnum.ADMIN.name()), existingStaffMember.getAvatar(),
                    "UpdatedAdminName", "UpdatedAdminSurname", // Updated fields
                    existingStaffMember.isActive(),
                    existingStaffMember.getClinic().getId(), existingStaffMember.getClinic().getName(),
                    null, null
            );
            updatedVetProfileDto = new ClinicStaffProfileDto(
                    existingVetMember.getId(), existingVetMember.getUsername(), existingVetMember.getEmail(),
                    Set.of(RoleEnum.VET.name()), existingVetMember.getAvatar(),
                    "UpdatedVetName", "UpdatedVetSurname", // Updated fields
                    existingVetMember.isActive(),
                    existingVetMember.getClinic().getId(), existingVetMember.getClinic().getName(),
                    "VET999UPD", "VETKEY999UPD" // Updated vet fields
            );
        }

        @Test
        @DisplayName("should update ADMIN successfully when authorized")
        void updateClinicStaff_Success_Admin() {
            // Arrange
            given(userRepository.findById(adminUser.getId())).willReturn(Optional.of(adminUser)); // Find updater
            given(clinicStaffRepository.findById(existingStaffMember.getId())).willReturn(Optional.of(existingStaffMember)); // Find staff to update
            given(clinicStaffRepository.save(any(ClinicStaff.class))).willAnswer(i -> i.getArgument(0)); // Save returns updated
            given(userMapper.toClinicStaffProfileDto(any(ClinicStaff.class))).willReturn(updatedAdminProfileDto); // Mapper returns DTO

            // Act
            ClinicStaffProfileDto result = clinicStaffService.updateClinicStaff(existingStaffMember.getId(), adminUpdateDto, adminUser.getId());

            // Assert
            assertThat(result).isEqualTo(updatedAdminProfileDto);
            then(clinicStaffRepository).should().save(clinicStaffCaptor.capture());
            assertThat(clinicStaffCaptor.getValue().getName()).isEqualTo(adminUpdateDto.name());
            assertThat(clinicStaffCaptor.getValue().getSurname()).isEqualTo(adminUpdateDto.surname());
        }

        @Test
        @DisplayName("should update VET successfully when authorized")
        void updateClinicStaff_Success_Vet() {
            // Arrange
            when(clinicStaffRepository.findById(existingVetMember.getId())).thenReturn(Optional.of(existingVetMember));
            when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));
            when(vetRepository.existsByLicenseNumber("VET999UPD")).thenReturn(false);
            when(vetRepository.existsByVetPublicKey("VETKEY999UPD")).thenReturn(false);
            when(clinicStaffRepository.save(any(Vet.class))).thenReturn(existingVetMember);
            when(userMapper.toClinicStaffProfileDto(existingVetMember)).thenReturn(updatedVetProfileDto);

            // Act
            ClinicStaffProfileDto result = clinicStaffService.updateClinicStaff(existingVetMember.getId(), vetUpdateDto, adminUser.getId());

            // Assert
            assertEquals(updatedVetProfileDto, result);
            verify(clinicStaffRepository).save(any(Vet.class));
        }

        @Test
        @DisplayName("should throw EntityNotFoundException if staff to update not found")
        void updateClinicStaff_Error_StaffNotFound() {
            given(clinicStaffRepository.findById(999L)).willReturn(Optional.empty());
            // No need to mock admin find as it fails finding the staff

            // Act
            Throwable thrown = Assertions.catchThrowable(() -> clinicStaffService.updateClinicStaff(999L, adminUpdateDto, adminUser.getId()));
            // Assert
            assertThat(thrown)
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("ClinicStaff not found with id: 999");
            then(clinicStaffRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("should throw AccessDeniedException if updater not authorized Admin")
        void updateClinicStaff_Error_UpdaterNotAuthAdmin() {
            // Simulate updater is a Vet
            ClinicStaff updaterVet = new ClinicStaff(); // ... setup Vet user ...
            updaterVet.setId(15L);
            updaterVet.setRoles(Set.of(vetRole));
            updaterVet.setClinic(clinic1);

            given(userRepository.findById(15L)).willReturn(Optional.of(updaterVet));
            given(clinicStaffRepository.findById(existingStaffMember.getId())).willReturn(Optional.of(existingStaffMember)); // Target staff found

            // Act
            Throwable thrown = Assertions.catchThrowable(() -> clinicStaffService.updateClinicStaff(existingStaffMember.getId(), adminUpdateDto, 15L));
            // Assert
            assertThat(thrown)
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("is not an authorized Admin");
            then(clinicStaffRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("should throw AccessDeniedException if Admin from different clinic")
        void updateClinicStaff_Error_DifferentClinic() {
            // Simulate admin from another clinic
            Clinic clinic2 = Clinic.builder().build(); clinic2.setId(2L);
            ClinicStaff adminOtherClinic = new ClinicStaff(); // ... setup admin ...
            adminOtherClinic.setId(20L);
            adminOtherClinic.setRoles(Set.of(adminRole));
            adminOtherClinic.setClinic(clinic2); // Different clinic

            given(userRepository.findById(20L)).willReturn(Optional.of(adminOtherClinic));
            given(clinicStaffRepository.findById(existingStaffMember.getId())).willReturn(Optional.of(existingStaffMember)); // Target staff (clinic 1) found

            // Act: Use simplified lambda
            Throwable thrown = Assertions.catchThrowable(() -> clinicStaffService.updateClinicStaff(existingStaffMember.getId(), adminUpdateDto, 20L));
            // Assert
            assertThat(thrown)
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("cannot update staff");
            then(clinicStaffRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("should throw LicenseNumberAlreadyExistsException on update")
        void updateClinicStaff_Error_DuplicateLicense() {
            // Arrange
            when(clinicStaffRepository.findById(existingVetMember.getId())).thenReturn(Optional.of(existingVetMember));
            when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));
            doThrow(new LicenseNumberAlreadyExistsException("VET999UPD")).when(vetRepository).existsByLicenseNumber("VET999UPD");

            // Act
            Executable executable = () -> clinicStaffService.updateClinicStaff(existingVetMember.getId(), vetUpdateDto, adminUser.getId());

            // Assert
            assertThrows(LicenseNumberAlreadyExistsException.class, executable);
        }

        @Test
        @DisplayName("should throw VetPublicKeyAlreadyExistsException on update")
        void updateClinicStaff_Error_DuplicatePublicKey() {
            // Arrange
            when(clinicStaffRepository.findById(existingVetMember.getId())).thenReturn(Optional.of(existingVetMember));
            when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));
            doThrow(new VetPublicKeyAlreadyExistsException()).when(vetRepository).existsByVetPublicKey("VETKEY999UPD");

            // Act
            Executable executable = () -> clinicStaffService.updateClinicStaff(existingVetMember.getId(), vetUpdateDto, adminUser.getId());

            // Assert
            assertThrows(VetPublicKeyAlreadyExistsException.class, executable);
        }

        @Test
        @DisplayName("should log warning when attempting to update Vet fields on Admin")
        void updateClinicStaff_Warns_UpdatingVetFieldsOnAdmin() {
            // Arrange
            ClinicStaffUpdateDto vetFieldsOnAdminDto = new ClinicStaffUpdateDto(
                    "AdminNewName", "AdminNewSurname", "TRYING_LIC", "TRYING_KEY");

            given(userRepository.findById(adminUser.getId())).willReturn(Optional.of(adminUser));
            given(clinicStaffRepository.findById(existingStaffMember.getId())).willReturn(Optional.of(existingStaffMember));
            // Mock save and mapper
            given(clinicStaffRepository.save(any(ClinicStaff.class))).willAnswer(i -> i.getArgument(0));
            given(userMapper.toClinicStaffProfileDto(any(ClinicStaff.class))).willAnswer(invocation -> {
                ClinicStaff staff = invocation.getArgument(0);
                return new ClinicStaffProfileDto(staff.getId(), staff.getUsername(), staff.getEmail(),
                        Set.of("ADMIN"), staff.getAvatar(), staff.getName(), staff.getSurname(),
                        staff.isActive(), staff.getClinic().getId(), staff.getClinic().getName(),
                        null, null);
            });

            // Act
            ClinicStaffProfileDto result = clinicStaffService.updateClinicStaff(existingStaffMember.getId(), vetFieldsOnAdminDto, adminUser.getId());

            // Assert
            assertThat(result.name()).isEqualTo("AdminNewName");
            assertThat(result.surname()).isEqualTo("AdminNewSurname");
            assertThat(result.licenseNumber()).isNull();
            assertThat(result.vetPublicKey()).isNull();
            then(clinicStaffRepository).should().save(any(ClinicStaff.class));
            then(vetRepository).should(never()).existsByLicenseNumberAndIdNot(anyString(), anyLong());
            then(vetRepository).should(never()).existsByVetPublicKeyAndIdNot(anyString(), anyLong());
        }

    }

    // --- Tests for activateStaff ---
    @Nested
    @DisplayName("activateStaff Tests")
    class ActivateStaffTests {
        private ClinicStaff inactiveStaff;
        private ClinicStaffProfileDto activeProfileDto;

        @BeforeEach
        void activateSetup() {
            inactiveStaff = new ClinicStaff(); // Same as existingStaffMember but inactive
            inactiveStaff.setId(existingStaffMember.getId());
            inactiveStaff.setUsername(existingStaffMember.getUsername());
            inactiveStaff.setClinic(clinic1);
            inactiveStaff.setRoles(Set.of(adminRole));
            inactiveStaff.setActive(false); // Important: Start inactive

            activeProfileDto = new ClinicStaffProfileDto(
                    inactiveStaff.getId(), inactiveStaff.getUsername(), inactiveStaff.getEmail(),
                    Set.of(RoleEnum.ADMIN.name()), inactiveStaff.getAvatar(),
                    inactiveStaff.getName(), inactiveStaff.getSurname(),
                    true, // isActive is now TRUE
                    inactiveStaff.getClinic().getId(), inactiveStaff.getClinic().getName(),
                    null, null
            );
        }

        @Test
        @DisplayName("should activate staff successfully")
        void activateStaff_Success() {
            given(clinicStaffRepository.findById(inactiveStaff.getId())).willReturn(Optional.of(inactiveStaff));
            given(userRepository.findById(adminUser.getId())).willReturn(Optional.of(adminUser)); // Check authorization
            given(clinicStaffRepository.save(any(ClinicStaff.class))).willAnswer(i -> i.getArgument(0));
            given(userMapper.toClinicStaffProfileDto(any(ClinicStaff.class))).willReturn(activeProfileDto);

            ClinicStaffProfileDto result = clinicStaffService.activateStaff(inactiveStaff.getId(), adminUser.getId());

            assertThat(result).isEqualTo(activeProfileDto);
            then(clinicStaffRepository).should().save(clinicStaffCaptor.capture());
            assertThat(clinicStaffCaptor.getValue().isActive()).isTrue();
        }

        @Test
        @DisplayName("should throw IllegalStateException if staff already active")
        void activateStaff_Error_AlreadyActive() {
            existingStaffMember.setActive(true); // Ensure it's active
            given(clinicStaffRepository.findById(existingStaffMember.getId())).willReturn(Optional.of(existingStaffMember));
            given(userRepository.findById(adminUser.getId())).willReturn(Optional.of(adminUser)); // Auth check still happens

            // Act
            Throwable thrown = Assertions.catchThrowable(() -> clinicStaffService.activateStaff(existingStaffMember.getId(), adminUser.getId()));
            // Assert
            assertThat(thrown)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("is already active");
            then(clinicStaffRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("should throw EntityNotFoundException if staff to activate not found")
        void activateStaff_Error_StaffNotFound() {
            given(clinicStaffRepository.findById(999L)).willReturn(Optional.empty());

            // Act
            Throwable thrown = Assertions.catchThrowable(() -> clinicStaffService.activateStaff(999L, adminUser.getId()));
            // Assert
            assertThat(thrown)
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("ClinicStaff not found with id: 999");
        }

        @Test
        @DisplayName("should throw AccessDeniedException if activator not Admin")
        void activateStaff_Error_ActivatorNotAdmin() {
            ClinicStaff inactive = new ClinicStaff();
            inactive.setId(11L);
            inactive.setActive(false);
            inactive.setClinic(clinic1);
            ClinicStaff activatorVet = new ClinicStaff(); // Simulate activator is a Vet
            activatorVet.setId(15L); activatorVet.setRoles(Set.of(vetRole)); activatorVet.setClinic(clinic1);

            given(clinicStaffRepository.findById(11L)).willReturn(Optional.of(inactive));
            given(userRepository.findById(15L)).willReturn(Optional.of(activatorVet)); // Found user, but is Vet

            assertThatThrownBy(() -> clinicStaffService.activateStaff(11L, 15L))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("is not an authorized Admin");
        }

        @Test
        @DisplayName("should throw AccessDeniedException if Admin from different clinic")
        void activateStaff_Error_DifferentClinic() {
            ClinicStaff inactive = new ClinicStaff(); inactive.setId(11L); inactive.setActive(false); inactive.setClinic(clinic1); // Staff from clinic 1
            Clinic clinic2 = Clinic.builder().build(); clinic2.setId(2L);
            ClinicStaff adminOtherClinic = new ClinicStaff(); // Admin from clinic 2
            adminOtherClinic.setId(20L); adminOtherClinic.setRoles(Set.of(adminRole)); adminOtherClinic.setClinic(clinic2);

            given(clinicStaffRepository.findById(11L)).willReturn(Optional.of(inactive));
            given(userRepository.findById(20L)).willReturn(Optional.of(adminOtherClinic));

            assertThatThrownBy(() -> clinicStaffService.activateStaff(11L, 20L))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("cannot activate staff");
        }

    }

    // --- Tests for deactivateStaff ---
    @Nested
    @DisplayName("deactivateStaff Tests")
    class DeactivateStaffTests {
        private ClinicStaff activeStaff;
        private ClinicStaffProfileDto inactiveProfileDto;

        @BeforeEach
        void deactivateSetup() {
            activeStaff = existingStaffMember; // Use the active one from main setup
            activeStaff.setActive(true);
            inactiveProfileDto = new ClinicStaffProfileDto(
                    activeStaff.getId(), activeStaff.getUsername(), activeStaff.getEmail(),
                    Set.of(RoleEnum.ADMIN.name()), activeStaff.getAvatar(),
                    activeStaff.getName(), activeStaff.getSurname(),
                    false, // isActive is now FALSE
                    activeStaff.getClinic().getId(), activeStaff.getClinic().getName(),
                    null, null
            );
        }

        @Test
        @DisplayName("should deactivate staff successfully")
        void deactivateStaff_Success() {
            given(clinicStaffRepository.findById(activeStaff.getId())).willReturn(Optional.of(activeStaff));
            given(userRepository.findById(adminUser.getId())).willReturn(Optional.of(adminUser)); // Check auth
            given(clinicStaffRepository.save(any(ClinicStaff.class))).willAnswer(i -> i.getArgument(0));
            given(userMapper.toClinicStaffProfileDto(any(ClinicStaff.class))).willReturn(inactiveProfileDto);

            ClinicStaffProfileDto result = clinicStaffService.deactivateStaff(activeStaff.getId(), adminUser.getId());

            assertThat(result).isEqualTo(inactiveProfileDto);
            then(clinicStaffRepository).should().save(clinicStaffCaptor.capture());
            assertThat(clinicStaffCaptor.getValue().isActive()).isFalse();
        }

        @Test
        @DisplayName("should throw IllegalStateException if staff already inactive")
        void deactivateStaff_Error_AlreadyInactive() {
            activeStaff.setActive(false); // Make it inactive
            given(clinicStaffRepository.findById(activeStaff.getId())).willReturn(Optional.of(activeStaff));
            given(userRepository.findById(adminUser.getId())).willReturn(Optional.of(adminUser));

            // Act
            Throwable thrown = Assertions.catchThrowable(() ->clinicStaffService.deactivateStaff(activeStaff.getId(), adminUser.getId()));
            // Assert
            assertThat(thrown)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("is already inactive");
            then(clinicStaffRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("should throw IllegalArgumentException if admin tries to deactivate self")
        void deactivateStaff_Error_SelfDeactivation() {
            // Use adminUser's ID as the target staffId
            given(clinicStaffRepository.findById(adminUser.getId())).willReturn(Optional.of(adminUser));
            given(userRepository.findById(adminUser.getId())).willReturn(Optional.of(adminUser)); // Auth check

            // Act
            Throwable thrown = Assertions.catchThrowable(() -> clinicStaffService.deactivateStaff(adminUser.getId(), adminUser.getId()));
            // Assert
            assertThat(thrown)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot deactivate their own account");
            then(clinicStaffRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("should throw EntityNotFoundException if staff to deactivate not found")
        void deactivateStaff_Error_StaffNotFound() {
            given(clinicStaffRepository.findById(999L)).willReturn(Optional.empty());

            // Act
            Throwable thrown = Assertions.catchThrowable(() -> clinicStaffService.deactivateStaff(999L, adminUser.getId()));
            // Assert
            assertThat(thrown)
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("ClinicStaff not found with id: 999");
        }

        @Test
        @DisplayName("should throw AccessDeniedException if deactivator not Admin")
        void deactivateStaff_Error_DeactivatorNotAdmin() {
            ClinicStaff active = existingStaffMember; active.setActive(true);
            ClinicStaff deactivatorVet = new ClinicStaff(); // Simulate deactivator is Vet
            deactivatorVet.setId(15L); deactivatorVet.setRoles(Set.of(vetRole)); deactivatorVet.setClinic(clinic1);

            given(clinicStaffRepository.findById(active.getId())).willReturn(Optional.of(active));
            given(userRepository.findById(15L)).willReturn(Optional.of(deactivatorVet));

            // Act
            Throwable thrown = Assertions.catchThrowable(() ->  clinicStaffService.deactivateStaff(active.getId(), 15L));
            // Assert
            assertThat(thrown)
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("is not an authorized Admin");
        }

        @Test
        @DisplayName("should throw AccessDeniedException if Admin from different clinic")
        void deactivateStaff_Error_DifferentClinic() {
            ClinicStaff active = existingStaffMember; active.setActive(true); // Staff from clinic 1
            Clinic clinic2 = Clinic.builder().build(); clinic2.setId(2L);
            ClinicStaff adminOtherClinic = new ClinicStaff(); // Admin from clinic 2
            adminOtherClinic.setId(20L); adminOtherClinic.setRoles(Set.of(adminRole)); adminOtherClinic.setClinic(clinic2);

            given(clinicStaffRepository.findById(active.getId())).willReturn(Optional.of(active));
            given(userRepository.findById(20L)).willReturn(Optional.of(adminOtherClinic));

            // Act
            Throwable thrown = Assertions.catchThrowable(() -> clinicStaffService.deactivateStaff(active.getId(), 20L));
            // Assert
            assertThat(thrown)
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("cannot deactivate staff");
        }
    }


    // --- Tests for findActiveStaffByClinic / findAllStaffByClinic ---
    @Nested
    @DisplayName("find Staff By Clinic Tests")
    class FindStaffByClinicTests {
        private ClinicStaff activeStaffInClinic1; // Could be Vet or Admin
        private ClinicStaff inactiveStaffInClinic1; // Could be Vet or Admin
        private ClinicStaffProfileDto activeDto;
        private ClinicStaffProfileDto inactiveDto;


        @BeforeEach
        void findSetup() {
            // Assumes adminUser (ID 10) is the requester from clinic 1
            activeStaffInClinic1 = existingVetMember; // This IS a Vet object
            activeStaffInClinic1.setActive(true);

            inactiveStaffInClinic1 = existingStaffMember; // This IS a ClinicStaff (Admin) object
            inactiveStaffInClinic1.setActive(false);

            activeDto = new ClinicStaffProfileDto(
                    activeStaffInClinic1.getId(), activeStaffInClinic1.getUsername(), activeStaffInClinic1.getEmail(),
                    Set.of(RoleEnum.VET.name()), activeStaffInClinic1.getAvatar(),
                    activeStaffInClinic1.getName(), activeStaffInClinic1.getSurname(),
                    true, // Active
                    activeStaffInClinic1.getClinic().getId(), activeStaffInClinic1.getClinic().getName(),
                    // --- CASTE TO VET TO ACCESS VET METHODS ---
                    ((Vet) activeStaffInClinic1).getLicenseNumber(),
                    ((Vet) activeStaffInClinic1).getVetPublicKey()
            );
            inactiveDto = new ClinicStaffProfileDto(
                    inactiveStaffInClinic1.getId(), inactiveStaffInClinic1.getUsername(), inactiveStaffInClinic1.getEmail(),
                    Set.of(RoleEnum.ADMIN.name()), inactiveStaffInClinic1.getAvatar(),
                    inactiveStaffInClinic1.getName(), inactiveStaffInClinic1.getSurname(),
                    false, // Inactive
                    inactiveStaffInClinic1.getClinic().getId(), inactiveStaffInClinic1.getClinic().getName(),
                    null, null
            );
        }

        @Test
        @DisplayName("findAllStaffByClinic should return all staff when authorized")
        void findAllStaffByClinic_Success() {
            given(userRepository.findById(adminUser.getId())).willReturn(Optional.of(adminUser)); // Requester (Admin C1)
            given(clinicRepository.existsById(clinic1.getId())).willReturn(true); // Target clinic exists
            given(clinicStaffRepository.findByClinicId(clinic1.getId())).willReturn(List.of(activeStaffInClinic1, inactiveStaffInClinic1));
            given(userMapper.toClinicStaffProfileDtoList(List.of(activeStaffInClinic1, inactiveStaffInClinic1)))
                    .willReturn(List.of(activeDto, inactiveDto)); // Mock the list mapping

            List<ClinicStaffProfileDto> result = clinicStaffService.findAllStaffByClinic(clinic1.getId(), adminUser.getId());

            assertThat(result).hasSize(2).containsExactlyInAnyOrder(activeDto, inactiveDto);
            then(userRepository).should().findById(adminUser.getId());
            then(clinicRepository).should().existsById(clinic1.getId());
            then(clinicStaffRepository).should().findByClinicId(clinic1.getId());
            then(userMapper).should().toClinicStaffProfileDtoList(anyList());
        }

        @Test
        @DisplayName("findActiveStaffByClinic should return only active staff when authorized")
        void findActiveStaffByClinic_Success() {
            given(userRepository.findById(adminUser.getId())).willReturn(Optional.of(adminUser)); // Requester (Admin C1)
            given(clinicRepository.existsById(clinic1.getId())).willReturn(true); // Target clinic exists
            given(clinicStaffRepository.findByClinicIdAndIsActive(clinic1.getId(), true)).willReturn(List.of(activeStaffInClinic1));
            given(userMapper.toClinicStaffProfileDtoList(List.of(activeStaffInClinic1)))
                    .willReturn(List.of(activeDto)); // Mock list mapping

            List<ClinicStaffProfileDto> result = clinicStaffService.findActiveStaffByClinic(clinic1.getId(), adminUser.getId());

            assertThat(result).hasSize(1).containsExactly(activeDto);
            then(userRepository).should().findById(adminUser.getId());
            then(clinicRepository).should().existsById(clinic1.getId());
            then(clinicStaffRepository).should().findByClinicIdAndIsActive(clinic1.getId(), true);
            then(userMapper).should().toClinicStaffProfileDtoList(anyList());
        }

        @Test
        @DisplayName("find staff should throw AccessDeniedException if requester not Vet or Admin")
        void findStaffByClinic_Error_RequesterInvalidRole() {
            // Arrange
            Owner requesterOwner = new Owner(); requesterOwner.setId(50L);
            given(userRepository.findById(50L)).willReturn(Optional.of(requesterOwner)); // Found user, but is Owner
            given(clinicRepository.existsById(clinic1.getId())).willReturn(true);

            // Act
            Throwable thrown = Assertions.catchThrowable(() -> clinicStaffService.findAllStaffByClinic(clinic1.getId(), 50L));
            // Assert
            assertThat(thrown)
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("is not Clinic Staff");

            // Verify interactions
            then(userRepository).should().findById(50L);
            then(clinicRepository).should().existsById(clinic1.getId());
            then(clinicStaffRepository).should(never()).findByClinicId(anyLong());
        }

        @Test
        @DisplayName("find staff should throw AccessDeniedException if requester from different clinic")
        void findStaffByClinic_Error_DifferentClinic() {
            // Arrange
            Clinic clinic2 = Clinic.builder().build(); clinic2.setId(2L);
            ClinicStaff requesterOtherClinic = new ClinicStaff();
            requesterOtherClinic.setId(20L);
            requesterOtherClinic.setRoles(Set.of(adminRole)); // Es Admin
            requesterOtherClinic.setClinic(clinic2); // Pero de la clínica 2
            given(userRepository.findById(20L)).willReturn(Optional.of(requesterOtherClinic));
            given(clinicRepository.existsById(clinic1.getId())).willReturn(true);

            // Act
            Throwable thrown = Assertions.catchThrowable(() -> clinicStaffService.findAllStaffByClinic(clinic1.getId(), 20L));
            // Assert
            assertThat(thrown)
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("cannot view all staff for clinic 1");

            // Verify interactions
            then(userRepository).should().findById(20L);
            then(clinicRepository).should().existsById(clinic1.getId());
            then(clinicStaffRepository).should(never()).findByClinicId(anyLong());
        }

        @Test
        @DisplayName("find staff should throw EntityNotFoundException if requester not found")
        void findStaffByClinic_Error_RequesterNotFound() {
            given(userRepository.findById(999L)).willReturn(Optional.empty()); // Requester not found

            // Act
            Throwable thrown = Assertions.catchThrowable(() -> clinicStaffService.findAllStaffByClinic(clinic1.getId(), 999L));
            // Assert
            assertThat(thrown)
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Requesting user not found");
        }

        @Test
        @DisplayName("find staff should throw EntityNotFoundException if target clinic not found")
        void findStaffByClinic_Error_TargetClinicNotFound() {
            given(userRepository.findById(adminUser.getId())).willReturn(Optional.of(adminUser)); // Requester OK
            given(clinicRepository.existsById(99L)).willReturn(false); // Target clinic does NOT exist

            // Act
            Throwable thrown = Assertions.catchThrowable(() ->clinicStaffService.findAllStaffByClinic(99L, adminUser.getId()));
            // Assert
            assertThat(thrown)
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Target clinic not found");
            // Verify existsById was checked AFTER user/role/clinic checks passed
            then(userRepository).should().findById(adminUser.getId());
            then(clinicRepository).should().existsById(99L);
            then(clinicStaffRepository).should(never()).findByClinicId(anyLong()); // Should not reach staff lookup
        }
    }
}
