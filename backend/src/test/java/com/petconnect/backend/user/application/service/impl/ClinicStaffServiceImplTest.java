package com.petconnect.backend.user.application.service.impl;

import com.petconnect.backend.common.helper.AuthorizationHelper;
import com.petconnect.backend.common.helper.ClinicStaffHelper;
import com.petconnect.backend.common.helper.EntityFinderHelper;
import com.petconnect.backend.common.helper.ValidateHelper;
import com.petconnect.backend.common.service.KeyStorageService;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;
import org.springframework.mock.web.MockMultipartFile;
import static org.mockito.ArgumentMatchers.nullable;

/**
 * Unit tests for {@link ClinicStaffServiceImpl}.
 * Verifies the business logic for creating, updating, activating, deactivating,
 * and retrieving clinic staff members, including authorization checks.
 *
 * @author ibosquet
 */
@ExtendWith(MockitoExtension.class)
class ClinicStaffServiceImplTest {
    // --- Mocks ---
    @Mock private ClinicStaffRepository clinicStaffRepository;
    @Mock private UserMapper userMapper;
    @Mock private EntityFinderHelper entityFinderHelper;
    @Mock private ValidateHelper validateHelper;
    @Mock private ClinicStaffHelper clinicStaffHelper;
    @Mock private AuthorizationHelper authorizationHelper;
    @Mock private KeyStorageService keyStorageService;

    // --- Class Under Test ---
    @InjectMocks
    private ClinicStaffServiceImpl clinicStaffService;

    // --- Captors ---
    @Captor private ArgumentCaptor<ClinicStaff> clinicStaffCaptor;

    // --- Test Data ---
    private Clinic clinic1;
    private ClinicStaff adminUser;
    private RoleEntity adminRole, vetRole;
    private ClinicStaff existingStaffMember;
    private Vet existingVetMember;


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

        existingStaffMember = new ClinicStaff();
        existingStaffMember.setId(11L);
        existingStaffMember.setUsername("existing_admin");
        existingStaffMember.setName("Existing");
        existingStaffMember.setSurname("Admin");
        existingStaffMember.setEmail("existing.admin@test.com");
        existingStaffMember.setClinic(clinic1);
        existingStaffMember.setRoles(Set.of(adminRole));
        existingStaffMember.setActive(true);
        existingStaffMember.setAvatar("images/avatars/users/admin.png");

        existingVetMember = new Vet();
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

    /**
     * --- Tests for createClinicStaff ---
     */
    @Nested
    @DisplayName("createClinicStaff Tests")
    class CreateClinicStaffTests {

        private ClinicStaffCreationDto vetCreationDto;
        private ClinicStaffCreationDto adminCreationDto;
        private Vet savedVet;
        private ClinicStaff savedAdmin;
        private ClinicStaffProfileDto vetProfileDto;
        private ClinicStaffProfileDto adminProfileDto;
        private final String actionContext = "create clinic staff";
        private MockMultipartFile mockPublicKeyFile; // for VET
        private MockMultipartFile mockPrivateKeyFile; // for VET


        @BeforeEach
        void createDtoSetup() {
            String defaultUserImagePathBase = "images/avatars/users/";
            vetCreationDto = new ClinicStaffCreationDto(
                    "newvet", "new.vet@test.com", "password123",
                    "New", "Vet", RoleEnum.VET, "VETLIC123");

            adminCreationDto = new ClinicStaffCreationDto(
                    "newadmin", "new.admin@test.com", "password123",
                    "New", "Admin", RoleEnum.ADMIN,
                    null); // No vet fields

            // Simulate saved entities (IDs assigned, password hashed)
            savedVet = new Vet();
            savedVet.setId(100L);
            savedVet.setUsername(vetCreationDto.username());
            savedVet.setEmail(vetCreationDto.email());
            savedVet.setPassword("hashedPassword");
            savedVet.setName(vetCreationDto.name());
            savedVet.setSurname(vetCreationDto.surname());
            savedVet.setLicenseNumber(vetCreationDto.licenseNumber());
            savedVet.setClinic(clinic1);
            savedVet.setRoles(Set.of(vetRole));
            savedVet.setActive(true);
            savedVet.setAvatar(defaultUserImagePathBase+ "vet.png");

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
            savedAdmin.setAvatar(defaultUserImagePathBase+ "admin.png");

            vetProfileDto = new ClinicStaffProfileDto(
                    100L, savedVet.getUsername(), savedVet.getEmail(),
                    Set.of(RoleEnum.VET.name()), savedVet.getAvatar(),
                    savedVet.getName(), savedVet.getSurname(), savedVet.isActive(),
                    savedVet.getClinic().getId(), savedVet.getClinic().getName(),
                    savedVet.getLicenseNumber(), savedVet.getVetPublicKey(), true,
                    savedVet.getCreatedAt(), savedVet.getCreatedBy(),savedVet.getUpdatedAt(), savedVet.getUpdatedBy()
            );
            adminProfileDto = new ClinicStaffProfileDto(
                    101L, savedAdmin.getUsername(), savedAdmin.getEmail(),
                    Set.of(RoleEnum.ADMIN.name()), savedAdmin.getAvatar(),
                    savedAdmin.getName(), savedAdmin.getSurname(), savedAdmin.isActive(),
                    savedAdmin.getClinic().getId(), savedAdmin.getClinic().getName(),
                    null, null, false,savedAdmin.getCreatedAt(),
                    savedAdmin.getCreatedBy(),savedAdmin.getUpdatedAt(), savedAdmin.getUpdatedBy()
            );

            mockPublicKeyFile = new MockMultipartFile("publicKeyFile", "vet_pub.pem", "application/x-x509-ca-cert", "public key content".getBytes());
            mockPrivateKeyFile = new MockMultipartFile("privateKeyFile", "vet_pri.pem", "application/octet-stream", "private key content".getBytes());
        }

        @Test
        @DisplayName("should create VET successfully when data is valid and admin authorized")
        void createClinicStaff_Success_Vet() throws Exception {
                // Arrange
                String expectedPubKeyPath = "vets/newvet_pub.pem";
                String expectedPrivKeyPath = "vets/newvet_priv.pem";

                given(entityFinderHelper.findAdminStaffOrFail(adminUser.getId(), actionContext)).willReturn(adminUser);
                doNothing().when(validateHelper).validateStaffRole(RoleEnum.VET);
                doNothing().when(validateHelper).validateNewStaffUniqueness(vetCreationDto.email(), vetCreationDto.username());
                doNothing().when(validateHelper).validateVetLicenseNumber(vetCreationDto.licenseNumber()); // Mockear esta validación

                given(keyStorageService.storePublicKey(eq(mockPublicKeyFile), eq("vets"), anyString())).willReturn(expectedPubKeyPath);
                given(keyStorageService.storeEncryptedPrivateKey(eq(mockPrivateKeyFile), eq("vets"), anyString())).willReturn(expectedPrivKeyPath);

                given(clinicStaffHelper.buildNewStaffEntity((vetCreationDto), (expectedPubKeyPath), (expectedPrivKeyPath), (clinic1))).willReturn(savedVet);

                given(clinicStaffRepository.save(any(Vet.class))).willAnswer(invocation -> {
                    Vet vetToSave = invocation.getArgument(0);
                    vetToSave.setId(100L);
                    return vetToSave;
                });
                given(userMapper.toClinicStaffProfileDto(any(Vet.class))).willReturn(vetProfileDto);

                // Act
                ClinicStaffProfileDto result = clinicStaffService.createClinicStaff(vetCreationDto, mockPublicKeyFile, mockPrivateKeyFile, adminUser.getId());

                // Assert
                assertThat(result).isNotNull().isEqualTo(vetProfileDto);

                then(entityFinderHelper).should().findAdminStaffOrFail(adminUser.getId(), actionContext);
                then(validateHelper).should().validateStaffRole(RoleEnum.VET);
                then(validateHelper).should().validateNewStaffUniqueness(vetCreationDto.email(), vetCreationDto.username());
                then(validateHelper).should().validateVetLicenseNumber(vetCreationDto.licenseNumber());
                then(keyStorageService).should().storePublicKey(eq(mockPublicKeyFile), eq("vets"), anyString());
                then(keyStorageService).should().storeEncryptedPrivateKey(eq(mockPrivateKeyFile), eq("vets"), anyString());
                then(clinicStaffHelper).should().buildNewStaffEntity(eq(vetCreationDto), eq(expectedPubKeyPath), eq(expectedPrivKeyPath), eq(clinic1));
                then(clinicStaffRepository).should().save(any(Vet.class));
                then(userMapper).should().toClinicStaffProfileDto(any(Vet.class));
        }

        @Test
        @DisplayName("should create ADMIN successfully when data is valid and admin authorized")
        void createClinicStaff_Success_Admin() {
            // Arrange
            given(entityFinderHelper.findAdminStaffOrFail(adminUser.getId(), actionContext)).willReturn(adminUser);
            doNothing().when(validateHelper).validateStaffRole(RoleEnum.ADMIN);
            doNothing().when(validateHelper).validateNewStaffUniqueness(adminCreationDto.email(), adminCreationDto.username());
            given(clinicStaffHelper.buildNewStaffEntity(adminCreationDto, null, null,clinic1)).willReturn(savedAdmin);
            given(clinicStaffRepository.save(savedAdmin)).willAnswer(invocation -> {
                ClinicStaff adminToSave = invocation.getArgument(0);
                adminToSave.setId(101L);
                return adminToSave;
            });
            given(userMapper.toClinicStaffProfileDto(any(ClinicStaff.class))).willReturn(adminProfileDto);

            // Act
            ClinicStaffProfileDto result = clinicStaffService.createClinicStaff(adminCreationDto,
                    null, null,adminUser.getId());

            // Assert
            assertThat(result).isNotNull().isEqualTo(adminProfileDto);

            then(entityFinderHelper).should().findAdminStaffOrFail(adminUser.getId(), actionContext);
            then(validateHelper).should().validateStaffRole(RoleEnum.ADMIN);
            then(validateHelper).should().validateNewStaffUniqueness(adminCreationDto.email(), adminCreationDto.username());
            then(clinicStaffHelper).should().buildNewStaffEntity(adminCreationDto, null, null,clinic1);
            then(clinicStaffRepository).should().save(savedAdmin);
            then(userMapper).should().toClinicStaffProfileDto(any(ClinicStaff.class));
        }

        @Test
        @DisplayName("should throw IllegalArgumentException for invalid role")
        void createClinicStaff_Error_InvalidRole() {
            ClinicStaffCreationDto dto = new ClinicStaffCreationDto("u", "e@e.c",
                    "p", "N", "S", RoleEnum.OWNER,  null);
            given(entityFinderHelper.findAdminStaffOrFail(adminUser.getId(), actionContext)).willReturn(adminUser);
            doThrow(new IllegalArgumentException("Invalid role specified"))
                    .when(validateHelper).validateStaffRole(RoleEnum.OWNER);

            // Act
            Throwable thrown = Assertions.catchThrowable(() -> clinicStaffService.createClinicStaff(dto, null,
                    null, adminUser.getId()));
            // Assert
            assertThat(thrown)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid role specified");

            then(validateHelper).should().validateStaffRole(RoleEnum.OWNER);
            then(validateHelper).should(never()).validateNewStaffUniqueness(anyString(), anyString());

        }

        @Test
        @DisplayName("should throw EmailAlreadyExistsException")
        void createClinicStaff_Error_EmailExists() {
            given(entityFinderHelper.findAdminStaffOrFail(adminUser.getId(), actionContext)).willReturn(adminUser);
            doNothing().when(validateHelper).validateStaffRole(vetCreationDto.role());
            doThrow(new EmailAlreadyExistsException(vetCreationDto.email()))
                    .when(validateHelper).validateNewStaffUniqueness(vetCreationDto.email(), vetCreationDto.username());

            // Act
            Throwable thrown = Assertions.catchThrowable(() -> clinicStaffService.createClinicStaff(vetCreationDto, mockPublicKeyFile, mockPrivateKeyFile, adminUser.getId()));

            // Assert
            assertThat(thrown)
                    .isInstanceOf(EmailAlreadyExistsException.class);

            then(validateHelper).should().validateNewStaffUniqueness(vetCreationDto.email(), vetCreationDto.username());
            then(validateHelper).should(never()).validateVetLicenseNumber(anyString());
        }

        @Test
        @DisplayName("should throw UsernameAlreadyExistsException")
        void createClinicStaff_Error_UsernameExists() {
            // Arrange
            given(entityFinderHelper.findAdminStaffOrFail(adminUser.getId(), actionContext)).willReturn(adminUser);
            doNothing().when(validateHelper).validateStaffRole(vetCreationDto.role());
            doThrow(new UsernameAlreadyExistsException(vetCreationDto.username()))
                    .when(validateHelper).validateNewStaffUniqueness(vetCreationDto.email(), vetCreationDto.username());

            // Act & Assert
            Throwable thrown = Assertions.catchThrowable(() -> clinicStaffService.createClinicStaff(vetCreationDto, mockPublicKeyFile, mockPrivateKeyFile, adminUser.getId()));

            // Assert
            assertThat(thrown)
                    .isInstanceOf(UsernameAlreadyExistsException.class);
        }

        @Test
        @DisplayName("should throw EntityNotFoundException if creating admin not found")
        void createClinicStaff_Error_AdminNotFound() {
            // Arrange
            given(entityFinderHelper.findAdminStaffOrFail(999L, actionContext))
                    .willThrow(new EntityNotFoundException("User performing action..."));

            Throwable thrown = Assertions.catchThrowable(() -> clinicStaffService.createClinicStaff(vetCreationDto, null, null,999L));
            assertThat(thrown)
                    .isInstanceOf(EntityNotFoundException.class);
            then(validateHelper).should(never()).validateStaffRole(any());
            then(clinicStaffRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("should throw AccessDeniedException if creator is not Admin")
        void createClinicStaff_Error_CreatorNotAdmin() {
            // Arrange
            given(entityFinderHelper.findAdminStaffOrFail(15L, actionContext))
                    .willThrow(new AccessDeniedException("User 15 is not an authorized Admin..."));

            // Act & Assert
            assertThatThrownBy(() -> clinicStaffService.createClinicStaff(vetCreationDto, null, null,15L))
                    .isInstanceOf(AccessDeniedException.class);
            then(clinicStaffRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("should throw IllegalStateException if creator has no clinic")
        void createClinicStaff_Error_CreatorNoClinic() {
            // Arrange
            given(entityFinderHelper.findAdminStaffOrFail(adminUser.getId(), actionContext))
                    .willThrow(new IllegalStateException("Admin user " + adminUser.getId() + " is not associated with any clinic."));

            // Act
            Throwable thrown = Assertions.catchThrowable(() -> clinicStaffService.createClinicStaff(vetCreationDto, null, null,adminUser.getId()));

            // Assert
            assertThat(thrown)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("is not associated with any clinic");

            then(entityFinderHelper).should().findAdminStaffOrFail(adminUser.getId(), actionContext);
            then(validateHelper).should(never()).validateStaffRole(any());
            then(clinicStaffRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("should throw IllegalArgumentException if Vet license is missing")
        void createClinicStaff_Error_VetLicenseMissing() throws IOException {
            // Arrange
            ClinicStaffCreationDto missingLicenseDto = new ClinicStaffCreationDto("v","e@v.c",
                    "p","N","S",RoleEnum.VET, null);
            given(entityFinderHelper.findAdminStaffOrFail(adminUser.getId(), actionContext)).willReturn(adminUser);
            doNothing().when(validateHelper).validateStaffRole(RoleEnum.VET);
            doNothing().when(validateHelper).validateNewStaffUniqueness(missingLicenseDto.email(), missingLicenseDto.username());
            // Hacemos que validateVetLicenseNumber lance la excepción
            doThrow(new IllegalArgumentException("License number is required for VET role."))
                    .when(validateHelper).validateVetLicenseNumber(null);


            // Act & Assert
            Throwable thrown = Assertions.catchThrowable(()->clinicStaffService.createClinicStaff(missingLicenseDto,
                    mockPublicKeyFile, mockPrivateKeyFile, adminUser.getId()));
            assertThat(thrown)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("License number is required for VET role.");

            then(validateHelper).should().validateVetLicenseNumber(null);
            then(keyStorageService).should(never()).storePublicKey(any(), anyString(), anyString());
        }

        @Test
        @DisplayName("should throw IllegalArgumentException if Vet public key is missing")
        void createClinicStaff_Error_VetKeyMissing() throws IOException {
            // Arrange
            given(entityFinderHelper.findAdminStaffOrFail(adminUser.getId(), actionContext)).willReturn(adminUser);
            doNothing().when(validateHelper).validateStaffRole(RoleEnum.VET);
            doNothing().when(validateHelper).validateNewStaffUniqueness(vetCreationDto.email(), vetCreationDto.username());
            doNothing().when(validateHelper).validateVetLicenseNumber(vetCreationDto.licenseNumber());

            // Act & Assert:

            Throwable thrown = Assertions.catchThrowable(()->clinicStaffService.createClinicStaff(vetCreationDto, null, mockPrivateKeyFile, adminUser.getId()));
            assertThat(thrown)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Public Key file (.pem) is required for VET role.");

            then(keyStorageService).should(never()).storePublicKey(any(), anyString(), anyString());
        }

        @Test
        @DisplayName("should throw LicenseNumberAlreadyExistsException")
        void createClinicStaff_Error_VetLicenseExists() throws IOException {
            // Arrange
            given(entityFinderHelper.findAdminStaffOrFail(adminUser.getId(), actionContext)).willReturn(adminUser);
            doNothing().when(validateHelper).validateStaffRole(RoleEnum.VET);
            doNothing().when(validateHelper).validateNewStaffUniqueness(vetCreationDto.email(), vetCreationDto.username());
            // Hacemos que validateVetLicenseNumber lance la excepción de existencia
            doThrow(new LicenseNumberAlreadyExistsException(vetCreationDto.licenseNumber()))
                    .when(validateHelper).validateVetLicenseNumber(vetCreationDto.licenseNumber());

            // Act
            Throwable thrown = Assertions.catchThrowable(()->clinicStaffService.createClinicStaff(vetCreationDto, mockPublicKeyFile, mockPrivateKeyFile, adminUser.getId()));
            // Assert
            assertThat(thrown)
                    .isInstanceOf(LicenseNumberAlreadyExistsException.class);

            then(validateHelper).should().validateVetLicenseNumber(vetCreationDto.licenseNumber());
            then(keyStorageService).should(never()).storePublicKey(any(), anyString(), anyString());
        }

        @Test
        @DisplayName("should throw VetPublicKeyAlreadyExistsException")
        void createClinicStaff_Error_VetPublicKeyExists() throws IOException {
            // Arrange
            given(entityFinderHelper.findAdminStaffOrFail(adminUser.getId(), actionContext)).willReturn(adminUser);
            doNothing().when(validateHelper).validateStaffRole(RoleEnum.VET);
            doNothing().when(validateHelper).validateNewStaffUniqueness(vetCreationDto.email(), vetCreationDto.username());
            doNothing().when(validateHelper).validateVetLicenseNumber(vetCreationDto.licenseNumber());
            given(keyStorageService.storePublicKey(eq(mockPublicKeyFile), eq("vets"), anyString()))
                    .willThrow(new VetPublicKeyAlreadyExistsException());

            // Act
            Throwable thrown = Assertions.catchThrowable(()->clinicStaffService.createClinicStaff(vetCreationDto, mockPublicKeyFile, mockPrivateKeyFile, adminUser.getId()));
            // Assert
            assertThat(thrown)
                    .isInstanceOf(VetPublicKeyAlreadyExistsException.class);
            // Verify...
            then(entityFinderHelper).should().findAdminStaffOrFail(adminUser.getId(), actionContext);
            then(validateHelper).should().validateStaffRole(RoleEnum.VET);
            then(validateHelper).should().validateNewStaffUniqueness(vetCreationDto.email(), vetCreationDto.username());
            then(validateHelper).should().validateVetLicenseNumber(vetCreationDto.licenseNumber());
            then(keyStorageService).should().storePublicKey(eq(mockPublicKeyFile), eq("vets"), anyString());
            then(keyStorageService).should(never()).storeEncryptedPrivateKey(any(), anyString(), anyString());
            then(clinicStaffHelper).should(never()).buildNewStaffEntity(any(), any(), any(), any());
            then(clinicStaffRepository).should(never()).save(any());
        }
    }

    /**
     * --- Tests for updateClinicStaff ---
     */
    @Nested
    @DisplayName("updateClinicStaff Tests")
    class UpdateClinicStaffTests {
        private ClinicStaffUpdateDto adminUpdateDto;
        private ClinicStaffUpdateDto vetUpdateDto;
        private ClinicStaffProfileDto updatedAdminProfileDto;
        private final String actionContext = "update";
        private MockMultipartFile mockNewPublicKeyFile;

        @BeforeEach
        void updateDtoSetup() {
            adminUpdateDto = new ClinicStaffUpdateDto("UpdatedAdminName", "UpdatedAdminSurname", null, null);
            vetUpdateDto = new ClinicStaffUpdateDto("UpdatedVetName", "UpdatedVetSurname", null, "VETKEY999UPD");
            updatedAdminProfileDto = new ClinicStaffProfileDto(
                    existingStaffMember.getId(),
                    "updatedUser",
                    existingStaffMember.getEmail(),
                    Set.of(RoleEnum.ADMIN.name()),
                    existingStaffMember.getAvatar(),
                    adminUpdateDto.name(),
                    adminUpdateDto.surname(),
                    true,
                    clinic1.getId(),
                    clinic1.getName(),
                    null,
                    null,
                    false,
                    null,
                    null,
                    null,
                    null
            );

            // Simulate expected DTOs after update
            mockNewPublicKeyFile = new MockMultipartFile("newPubKey", "new_pub.pem", "application/x-x509-ca-cert", "new public".getBytes());
            new MockMultipartFile("newPrivKey", "new_priv.pem", "application/octet-stream", "new private".getBytes());
        }

        @Test
        @DisplayName("should not save if applyStaffUpdates returns false")
        void updateClinicStaff_NoChanges_ShouldNotSave() {
            // Arrange
            ClinicStaffUpdateDto noChangeAdminDto = new ClinicStaffUpdateDto(
                    existingStaffMember.getName(), existingStaffMember.getSurname(), null, null
            );
            given(entityFinderHelper.findClinicStaffOrFail(existingStaffMember.getId(), actionContext)).willReturn(existingStaffMember);
            doNothing().when(authorizationHelper).verifyAdminActionOnStaff(adminUser.getId(), existingStaffMember, actionContext);
            given(clinicStaffHelper.applyStaffUpdates(existingStaffMember, noChangeAdminDto, null, null)).willReturn(false);
            ClinicStaffProfileDto originalDto = new ClinicStaffProfileDto(
                    existingStaffMember.getId(), existingStaffMember.getUsername(), existingStaffMember.getEmail(),
                    Set.of(RoleEnum.ADMIN.name()),
                    existingStaffMember.getAvatar(),
                    existingStaffMember.getName(),
                    existingStaffMember.getSurname(),
                    existingStaffMember.isActive(),
                    existingStaffMember.getClinic().getId(),
                    existingStaffMember.getClinic().getName(),
                    null, null,null, null, null, null,null
            );
            given(userMapper.toClinicStaffProfileDto(existingStaffMember)).willReturn(originalDto);

            // Act
            ClinicStaffProfileDto result = clinicStaffService.updateClinicStaff(existingStaffMember.getId(), noChangeAdminDto, null, null,adminUser.getId());

            // Assert
            assertThat(result).isEqualTo(originalDto);
            // Verify helpers called
            then(entityFinderHelper).should().findClinicStaffOrFail(existingStaffMember.getId(), actionContext);
            then(authorizationHelper).should().verifyAdminActionOnStaff(adminUser.getId(), existingStaffMember, actionContext);
            then(clinicStaffHelper).should().applyStaffUpdates(existingStaffMember, noChangeAdminDto, null, null);
            // Verify save NOT called
            then(clinicStaffRepository).should(never()).save(any());
            then(userMapper).should().toClinicStaffProfileDto(existingStaffMember);
        }

        @Test
        @DisplayName("should update ADMIN successfully when authorized")
        void updateClinicStaff_Success_Admin() {
            // Arrange
            given(entityFinderHelper.findClinicStaffOrFail(existingStaffMember.getId(), actionContext)).willReturn(existingStaffMember);
            doNothing().when(authorizationHelper).verifyAdminActionOnStaff(adminUser.getId(), existingStaffMember, actionContext);
            given(clinicStaffHelper.applyStaffUpdates(existingStaffMember, adminUpdateDto, null, null)).willReturn(true);
            given(clinicStaffRepository.save(existingStaffMember)).willReturn(existingStaffMember);
            given(userMapper.toClinicStaffProfileDto(existingStaffMember)).willReturn(updatedAdminProfileDto);

            // Act
            ClinicStaffProfileDto result = clinicStaffService.updateClinicStaff(existingStaffMember.getId(), adminUpdateDto,null, null, adminUser.getId());

            // Assert
            assertThat(result).isEqualTo(updatedAdminProfileDto);
            then(entityFinderHelper).should().findClinicStaffOrFail(existingStaffMember.getId(), actionContext);
            then(authorizationHelper).should().verifyAdminActionOnStaff(adminUser.getId(), existingStaffMember, actionContext);
            then(clinicStaffHelper).should().applyStaffUpdates(existingStaffMember, adminUpdateDto,null, null);
            then(clinicStaffRepository).should().save(existingStaffMember);
            then(userMapper).should().toClinicStaffProfileDto(existingStaffMember);
        }

        @Test
        @DisplayName("should update VET successfully when authorized")
        void updateClinicStaff_Success_Vet() {
            // Arrange
            given(entityFinderHelper.findClinicStaffOrFail(existingVetMember.getId(), actionContext)).willReturn(existingVetMember);
            doNothing().when(authorizationHelper).verifyAdminActionOnStaff(adminUser.getId(), existingVetMember, actionContext);

            given(clinicStaffHelper.applyStaffUpdates(
                    (existingVetMember),
                    (vetUpdateDto),
                    (existingVetMember.getVetPublicKey()),
                    (existingVetMember.getVetPrivateKey())
            )).willReturn(true);

            given(clinicStaffRepository.save(existingVetMember)).willReturn(existingVetMember);
            ClinicStaffProfileDto expectedDto = new ClinicStaffProfileDto(
                    existingVetMember.getId(), existingVetMember.getUsername(), existingVetMember.getEmail(),
                    Set.of(RoleEnum.VET.name()), existingVetMember.getAvatar(),
                    vetUpdateDto.name(), vetUpdateDto.surname(), true, clinic1.getId(), clinic1.getName(),
                    vetUpdateDto.licenseNumber(), existingVetMember.getVetPublicKey(), null,
                    null,null, null, null
            );
            given(userMapper.toClinicStaffProfileDto(existingVetMember)).willReturn(expectedDto);

            // Act
            ClinicStaffProfileDto result = clinicStaffService.updateClinicStaff(existingVetMember.getId(),
                    vetUpdateDto, null, null, adminUser.getId());

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.name()).isEqualTo(vetUpdateDto.name());
            assertThat(result.licenseNumber()).isEqualTo(vetUpdateDto.licenseNumber());
            then(clinicStaffHelper).should().applyStaffUpdates(eq(existingVetMember), eq(vetUpdateDto),
                    eq(existingVetMember.getVetPublicKey()), eq(existingVetMember.getVetPrivateKey()));
            then(clinicStaffRepository).should().save(existingVetMember);
        }

        @Test
        @DisplayName("should throw EntityNotFoundException if staff to update not found")
        void updateClinicStaff_Error_StaffNotFound() {
            // Arrange
            given(entityFinderHelper.findClinicStaffOrFail(999L, actionContext))
                    .willThrow(new EntityNotFoundException(ClinicStaff.class.getSimpleName(), 999L));

            // Act
            Throwable thrown = Assertions.catchThrowable(() -> clinicStaffService.updateClinicStaff(999L, adminUpdateDto,null, null, adminUser.getId()));
            //Assert
            assertThat(thrown)
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("ClinicStaff not found with id: 999");
            // Verify helpers/repos not called after failure
            then(authorizationHelper).should(never()).verifyAdminActionOnStaff(anyLong(), any(), anyString());
            then(clinicStaffRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("should throw AccessDeniedException if updater not authorized Admin")
        void updateClinicStaff_Error_UpdaterNotAuthAdmin() {
            // Arrange
            given(entityFinderHelper.findClinicStaffOrFail(existingStaffMember.getId(), actionContext)).willReturn(existingStaffMember);
            doThrow(new AccessDeniedException("User 15 is not an authorized Admin..."))
                    .when(authorizationHelper).verifyAdminActionOnStaff(15L, existingStaffMember, actionContext);

            // Act
            Throwable thrown = Assertions.catchThrowable(() -> clinicStaffService.updateClinicStaff(existingStaffMember.getId(), adminUpdateDto, null, null,15L));
            // Assert
            assertThat(thrown)
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("is not an authorized Admin");

            then(entityFinderHelper).should().findClinicStaffOrFail(existingStaffMember.getId(), actionContext);
            then(authorizationHelper).should().verifyAdminActionOnStaff(15L, existingStaffMember, actionContext);
            then(clinicStaffRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("should throw AccessDeniedException if Admin from different clinic")
        void updateClinicStaff_Error_DifferentClinic() {
            // Arrange
            given(entityFinderHelper.findClinicStaffOrFail(existingStaffMember.getId(), actionContext)).willReturn(existingStaffMember);
            doThrow(new AccessDeniedException("Admin (ID: 20, Clinic: 2) cannot update staff..."))
                    .when(authorizationHelper).verifyAdminActionOnStaff(20L, existingStaffMember, actionContext);

            // Act
            Throwable thrown = Assertions.catchThrowable(() -> clinicStaffService.updateClinicStaff(existingStaffMember.getId(), adminUpdateDto, null, null,20L));
            // Assert
            assertThat(thrown)
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("cannot update staff");
            then(entityFinderHelper).should().findClinicStaffOrFail(existingStaffMember.getId(), actionContext);
            then(authorizationHelper).should().verifyAdminActionOnStaff(20L, existingStaffMember, actionContext);
            then(clinicStaffRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("should throw LicenseNumberAlreadyExistsException on update")
        void updateClinicStaff_Error_DuplicateLicense() {
            // Arrange
            given(entityFinderHelper.findClinicStaffOrFail(existingVetMember.getId(), actionContext)).willReturn(existingVetMember);
            doNothing().when(authorizationHelper).verifyAdminActionOnStaff(adminUser.getId(), existingVetMember, actionContext);
            given(clinicStaffHelper.applyStaffUpdates(
                    (existingVetMember),
                    (vetUpdateDto),
                    (existingVetMember.getVetPublicKey()),
                    (existingVetMember.getVetPrivateKey())
            )).willThrow(new LicenseNumberAlreadyExistsException(vetUpdateDto.licenseNumber()));

            // Act & Assert
            Throwable thrown = Assertions.catchThrowable(() -> clinicStaffService.updateClinicStaff(existingVetMember.getId(),
                            vetUpdateDto, null, null, adminUser.getId()));
            assertThat(thrown)
                    .isInstanceOf(LicenseNumberAlreadyExistsException.class);
        }

        @Test
        @DisplayName("should throw VetPublicKeyAlreadyExistsException on update")
        void updateClinicStaff_Error_DuplicatePublicKey() throws IOException {
            // Arrange
            given(entityFinderHelper.findClinicStaffOrFail(existingVetMember.getId(), actionContext)).willReturn(existingVetMember);
            doNothing().when(authorizationHelper).verifyAdminActionOnStaff(adminUser.getId(), existingVetMember, actionContext);
            given(keyStorageService.storePublicKey(eq(mockNewPublicKeyFile), eq("vets"), anyString()))
                    .willThrow(new VetPublicKeyAlreadyExistsException());

            // Act & Assert
            Throwable thrown = Assertions.catchThrowable(() -> clinicStaffService.updateClinicStaff(existingVetMember.getId(),
                    vetUpdateDto, mockNewPublicKeyFile, null, adminUser.getId()));
            assertThat(thrown)
                    .isInstanceOf(VetPublicKeyAlreadyExistsException.class);
        }

        @Test
        @DisplayName("should log warning when attempting to update Vet fields on Admin")
        void updateClinicStaff_Warns_UpdatingVetFieldsOnAdmin() {
            // Arrange
            ClinicStaffUpdateDto vetFieldsOnAdminDto = new ClinicStaffUpdateDto(
                    "AdminNewName", "AdminNewSurname", Set.of(RoleEnum.ADMIN), "SHOULD_BE_IGNORED_LICENSE"
            );
            given(entityFinderHelper.findClinicStaffOrFail(existingStaffMember.getId(), actionContext)).willReturn(existingStaffMember);
            doNothing().when(authorizationHelper).verifyAdminActionOnStaff(adminUser.getId(), existingStaffMember, actionContext);

            given(clinicStaffHelper.applyStaffUpdates(eq(existingStaffMember), eq(vetFieldsOnAdminDto),
                    nullable(String.class), nullable(String.class))).willReturn(true);
            given(clinicStaffRepository.save(existingStaffMember)).willReturn(existingStaffMember);
            ClinicStaffProfileDto expectedDto = new ClinicStaffProfileDto(
                    existingStaffMember.getId(), existingStaffMember.getUsername(), existingStaffMember.getEmail(),
                    Set.of(RoleEnum.ADMIN.name()), existingStaffMember.getAvatar(),
                    vetFieldsOnAdminDto.name(), vetFieldsOnAdminDto.surname(), true, clinic1.getId(), clinic1.getName(),
                    null, null, null, null,null, null, null
            );
            given(userMapper.toClinicStaffProfileDto(existingStaffMember)).willReturn(expectedDto);


            // Act
            ClinicStaffProfileDto result = clinicStaffService.updateClinicStaff(existingStaffMember.getId(),
                    vetFieldsOnAdminDto, null, null, adminUser.getId());

            // Assert
            assertThat(result.name()).isEqualTo("AdminNewName");
            assertThat(result.licenseNumber()).isNull();

            // Verifica que applyStaffUpdates fue llamado
            then(clinicStaffHelper).should().applyStaffUpdates(eq(existingStaffMember),
                    eq(vetFieldsOnAdminDto), nullable(String.class), nullable(String.class));
            then(clinicStaffRepository).should().save(existingStaffMember);
        }

    }

    /**
     * --- Tests for activateStaff ---
     */
    @Nested
    @DisplayName("activateStaff Tests")
    class ActivateStaffTests {
        private ClinicStaff inactiveStaff;
        private ClinicStaffProfileDto activeProfileDto;
        private final Long inactiveStaffId = 11L;
        private final Long activatingAdminId = 10L;
        private final String actionContext = "activate";

        @BeforeEach
        void activateSetup() {
            inactiveStaff = new ClinicStaff();
            inactiveStaff.setId(inactiveStaffId);
            inactiveStaff.setUsername(existingStaffMember.getUsername());
            inactiveStaff.setName(existingStaffMember.getName());
            inactiveStaff.setSurname(existingStaffMember.getSurname());
            inactiveStaff.setEmail(existingStaffMember.getEmail());
            inactiveStaff.setAvatar(existingStaffMember.getAvatar());
            inactiveStaff.setClinic(clinic1);
            inactiveStaff.setRoles(Set.of(adminRole));
            inactiveStaff.setActive(false);

            activeProfileDto = new ClinicStaffProfileDto(
                    inactiveStaffId,
                    inactiveStaff.getUsername(),
                    inactiveStaff.getEmail(),
                    Set.of(RoleEnum.ADMIN.name()),
                    inactiveStaff.getAvatar(),
                    inactiveStaff.getName(),
                    inactiveStaff.getSurname(),
                    true,
                    clinic1.getId(),
                    clinic1.getName(),
                    null,
                    null,
                    false,
                    null,
                    null,
                    null,
                    null
            );
        }

        @Test
        @DisplayName("should activate staff successfully")
        void activateStaff_Success() {
            // Arrange
            given(entityFinderHelper.findClinicStaffOrFail(inactiveStaffId, actionContext)).willReturn(inactiveStaff);
            doNothing().when(authorizationHelper).verifyAdminActionOnStaff(activatingAdminId, inactiveStaff, actionContext);
            given(clinicStaffRepository.save(any(ClinicStaff.class))).willAnswer(i -> i.getArgument(0));
            given(userMapper.toClinicStaffProfileDto(any(ClinicStaff.class))).willReturn(activeProfileDto);

            // Act
            ClinicStaffProfileDto result = clinicStaffService.activateStaff(inactiveStaffId, activatingAdminId);

            // Assert
            assertThat(result).isEqualTo(activeProfileDto);
            then(entityFinderHelper).should().findClinicStaffOrFail(inactiveStaffId, actionContext);
            then(authorizationHelper).should().verifyAdminActionOnStaff(activatingAdminId, inactiveStaff, actionContext);
            then(clinicStaffRepository).should().save(clinicStaffCaptor.capture());
            then(userMapper).should().toClinicStaffProfileDto(any(ClinicStaff.class));

            assertThat(clinicStaffCaptor.getValue().isActive()).isTrue();
        }

        @Test
        @DisplayName("should throw IllegalStateException if staff already active")
        void activateStaff_Error_AlreadyActive() {
            // Arrange
            given(entityFinderHelper.findClinicStaffOrFail(existingStaffMember.getId(), actionContext)).willReturn(existingStaffMember);
            doNothing().when(authorizationHelper).verifyAdminActionOnStaff(activatingAdminId, existingStaffMember, actionContext);

            // Act
            Throwable thrown = Assertions.catchThrowable(() -> clinicStaffService.activateStaff(existingStaffMember.getId(), activatingAdminId));
            //Assert
            assertThat(thrown)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("is already active");

            then(entityFinderHelper).should().findClinicStaffOrFail(existingStaffMember.getId(), actionContext);
            then(authorizationHelper).should().verifyAdminActionOnStaff(activatingAdminId, existingStaffMember, actionContext);
            then(clinicStaffRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("should throw EntityNotFoundException if staff to activate not found")
        void activateStaff_Error_StaffNotFound() {
            // Arrange
            given(entityFinderHelper.findClinicStaffOrFail(999L, actionContext))
                    .willThrow(new EntityNotFoundException(ClinicStaff.class.getSimpleName(), 999L));
            // Act
            Throwable thrown = Assertions.catchThrowable(() -> clinicStaffService.activateStaff(999L, adminUser.getId()));
            // Assert
            assertThat(thrown)
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("ClinicStaff not found with id: 999");
            then(authorizationHelper).should(never()).verifyAdminActionOnStaff(anyLong(), any(), anyString());
            then(clinicStaffRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("should throw AccessDeniedException if activator not Admin")
        void activateStaff_Error_ActivatorNotAdmin() {
            // Arrange
            Long activatorVetId = 15L;
            given(entityFinderHelper.findClinicStaffOrFail(inactiveStaffId, actionContext)).willReturn(inactiveStaff);
            doThrow(new AccessDeniedException("User " + activatorVetId + " is not an authorized Admin..."))
                    .when(authorizationHelper).verifyAdminActionOnStaff(activatorVetId, inactiveStaff, actionContext);


            // Act & Assert
            assertThatThrownBy(() -> clinicStaffService.activateStaff(inactiveStaffId, activatorVetId))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("is not an authorized Admin");
            then(entityFinderHelper).should().findClinicStaffOrFail(inactiveStaffId, actionContext);
            then(authorizationHelper).should().verifyAdminActionOnStaff(activatorVetId, inactiveStaff, actionContext);
            then(clinicStaffRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("should throw AccessDeniedException if Admin from different clinic")
        void activateStaff_Error_DifferentClinic() {
            // Arrange
            Long adminOtherClinicId = 20L;
            given(entityFinderHelper.findClinicStaffOrFail(inactiveStaffId, actionContext)).willReturn(inactiveStaff);
            doThrow(new AccessDeniedException("Admin (ID: "+adminOtherClinicId+"...) cannot activate staff..."))
                    .when(authorizationHelper).verifyAdminActionOnStaff(adminOtherClinicId, inactiveStaff, actionContext);

            // Act & Assert
            assertThatThrownBy(() -> clinicStaffService.activateStaff(inactiveStaffId, adminOtherClinicId))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("cannot activate staff");
            then(entityFinderHelper).should().findClinicStaffOrFail(inactiveStaffId, actionContext);
            then(authorizationHelper).should().verifyAdminActionOnStaff(adminOtherClinicId, inactiveStaff, actionContext);
            then(clinicStaffRepository).should(never()).save(any());
        }

    }

    /**
     * --- Tests for deactivateStaff ---
     */
    @Nested
    @DisplayName("deactivateStaff Tests")
    class DeactivateStaffTests {
        private ClinicStaff activeStaff;
        private ClinicStaffProfileDto inactiveProfileDto;
        private final Long activeStaffId = 11L;
        private final Long deactivatingAdminId = 10L;
        private final String actionContext = "deactivate";

        @BeforeEach
        void deactivateSetup() {
            // Create an active version based on an existingStaffMember
            activeStaff = new ClinicStaff();
            activeStaff.setId(activeStaffId);
            activeStaff.setUsername(existingStaffMember.getUsername()); // Use data from existingStaffMember
            activeStaff.setName(existingStaffMember.getName());
            activeStaff.setSurname(existingStaffMember.getSurname());
            activeStaff.setEmail(existingStaffMember.getEmail());
            activeStaff.setAvatar(existingStaffMember.getAvatar());
            activeStaff.setClinic(clinic1);
            activeStaff.setRoles(Set.of(adminRole));
            activeStaff.setActive(true);

            inactiveProfileDto = new ClinicStaffProfileDto(
                    activeStaffId, activeStaff.getUsername(), activeStaff.getEmail(),
                    Set.of(RoleEnum.ADMIN.name()), activeStaff.getAvatar(),
                    activeStaff.getName(), activeStaff.getSurname(),
                    false,
                    clinic1.getId(), clinic1.getName(),
                    null, null, null, null, null, null, null
            );
        }

        @Test
        @DisplayName("should deactivate staff successfully")
        void deactivateStaff_Success() {
            // Arrange
            given(entityFinderHelper.findClinicStaffOrFail(activeStaffId, actionContext)).willReturn(activeStaff);
            doNothing().when(authorizationHelper).verifyAdminActionOnStaff(deactivatingAdminId, activeStaff, actionContext);
            given(clinicStaffRepository.save(any(ClinicStaff.class))).willAnswer(i -> i.getArgument(0));
            given(userMapper.toClinicStaffProfileDto(any(ClinicStaff.class))).willReturn(inactiveProfileDto);

            // Act
            ClinicStaffProfileDto result = clinicStaffService.deactivateStaff(activeStaffId, deactivatingAdminId);

            // Assert
            assertThat(result).isEqualTo(inactiveProfileDto);
            then(entityFinderHelper).should().findClinicStaffOrFail(activeStaffId, actionContext);
            then(authorizationHelper).should().verifyAdminActionOnStaff(deactivatingAdminId, activeStaff, actionContext);
            then(clinicStaffRepository).should().save(clinicStaffCaptor.capture());
            then(userMapper).should().toClinicStaffProfileDto(any(ClinicStaff.class));

            assertThat(clinicStaffCaptor.getValue().isActive()).isFalse();
        }

        @Test
        @DisplayName("should throw IllegalStateException if staff already inactive")
        void deactivateStaff_Error_AlreadyInactive() {
            // Arrange
            activeStaff.setActive(false);
            given(entityFinderHelper.findClinicStaffOrFail(activeStaffId, actionContext)).willReturn(activeStaff);
            doNothing().when(authorizationHelper).verifyAdminActionOnStaff(deactivatingAdminId, activeStaff, actionContext);


            // Act & Assert
            assertThatThrownBy(() -> clinicStaffService.deactivateStaff(activeStaffId, deactivatingAdminId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("is already inactive");
            then(entityFinderHelper).should().findClinicStaffOrFail(activeStaffId, actionContext);
            then(authorizationHelper).should().verifyAdminActionOnStaff(deactivatingAdminId, activeStaff, actionContext); // Auth still checked first
            then(clinicStaffRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("should throw IllegalArgumentException if admin tries to deactivate self")
        void deactivateStaff_Error_SelfDeactivation() {
            // Arrange
            given(entityFinderHelper.findClinicStaffOrFail(adminUser.getId(), actionContext)).willReturn(adminUser);
            doNothing().when(authorizationHelper).verifyAdminActionOnStaff(adminUser.getId(), adminUser, actionContext);

            // Act
            Throwable thrown = Assertions.catchThrowable(() ->clinicStaffService.deactivateStaff(adminUser.getId(), adminUser.getId()));
            // Assert
            assertThat(thrown)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot deactivate their own account");
            then(entityFinderHelper).should().findClinicStaffOrFail(adminUser.getId(), actionContext);
            then(authorizationHelper).should().verifyAdminActionOnStaff(adminUser.getId(), adminUser, actionContext);
            then(clinicStaffRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("should throw EntityNotFoundException if staff to deactivate not found")
        void deactivateStaff_Error_StaffNotFound() {
            // Arrange
            given(entityFinderHelper.findClinicStaffOrFail(999L, actionContext))
                    .willThrow(new EntityNotFoundException(ClinicStaff.class.getSimpleName(), 999L));

            // Act
            Throwable thrown = Assertions.catchThrowable(() -> clinicStaffService.deactivateStaff(999L, deactivatingAdminId));
            // Assert
            assertThat(thrown)
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("ClinicStaff not found with id: 999");
            then(authorizationHelper).should(never()).verifyAdminActionOnStaff(anyLong(), any(), anyString());
            then(clinicStaffRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("should throw AccessDeniedException if deactivator not Admin")
        void deactivateStaff_Error_DeactivatorNotAdmin() {
            // Arrange
            Long deactivatorVetId = 15L;
            given(entityFinderHelper.findClinicStaffOrFail(activeStaffId, actionContext)).willReturn(activeStaff);
            doThrow(new AccessDeniedException("User " + deactivatorVetId + " is not an authorized Admin..."))
                    .when(authorizationHelper).verifyAdminActionOnStaff(deactivatorVetId, activeStaff, actionContext);

            // Act
            Throwable thrown = Assertions.catchThrowable(() ->  clinicStaffService.deactivateStaff(activeStaffId, deactivatorVetId));
            // Assert
            assertThat(thrown)
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("is not an authorized Admin");
            then(entityFinderHelper).should().findClinicStaffOrFail(activeStaffId, actionContext);
            then(authorizationHelper).should().verifyAdminActionOnStaff(deactivatorVetId, activeStaff, actionContext);
            then(clinicStaffRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("should throw AccessDeniedException if Admin from different clinic")
        void deactivateStaff_Error_DifferentClinic() {
            // Arrange
            Long adminOtherClinicId = 20L;
            // Mock helper finds Staff OK
            given(entityFinderHelper.findClinicStaffOrFail(activeStaffId, actionContext)).willReturn(activeStaff);
            // Mock helper authorization check to fail
            doThrow(new AccessDeniedException("Admin (ID: " + adminOtherClinicId + "...) cannot deactivate staff..."))
                    .when(authorizationHelper).verifyAdminActionOnStaff(adminOtherClinicId, activeStaff, actionContext);

            // Act
            Throwable thrown = Assertions.catchThrowable(() -> clinicStaffService.deactivateStaff(activeStaffId, adminOtherClinicId));
            // Assert
            assertThat(thrown)
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("cannot deactivate staff");
            then(entityFinderHelper).should().findClinicStaffOrFail(activeStaffId, actionContext);
            then(authorizationHelper).should().verifyAdminActionOnStaff(adminOtherClinicId, activeStaff, actionContext);
            then(clinicStaffRepository).should(never()).save(any());
        }
    }


    /**
     * --- Tests for findActiveStaffByClinic / findAllStaffByClinic ---
     */
    @Nested
    @DisplayName("find Staff By Clinic Tests")
    class FindStaffByClinicTests {
        private ClinicStaff activeStaffInClinic1; // Could be Vet or Admin
        private ClinicStaff inactiveStaffInClinic1; // Could be Vet or Admin
        private ClinicStaffProfileDto activeDto;
        private ClinicStaffProfileDto inactiveDto;
        private final String actionContextAll = "view all staff for";


        @BeforeEach
        void findSetup() {
            activeStaffInClinic1 = existingVetMember;
            activeStaffInClinic1.setActive(true);

            inactiveStaffInClinic1 = existingStaffMember;
            inactiveStaffInClinic1.setActive(false);

            activeDto = new ClinicStaffProfileDto(
                    activeStaffInClinic1.getId(), activeStaffInClinic1.getUsername(), activeStaffInClinic1.getEmail(),
                    Set.of(RoleEnum.VET.name()), activeStaffInClinic1.getAvatar(),
                    activeStaffInClinic1.getName(), activeStaffInClinic1.getSurname(),
                    true, // Active
                    activeStaffInClinic1.getClinic().getId(), activeStaffInClinic1.getClinic().getName(),
                    // --- CASTE TO VET TO ACCESS VET METHODS ---
                    ((Vet) activeStaffInClinic1).getLicenseNumber(),
                    ((Vet) activeStaffInClinic1).getVetPublicKey(), null, null, null, null, null
            );
            inactiveDto = new ClinicStaffProfileDto(
                    inactiveStaffInClinic1.getId(), inactiveStaffInClinic1.getUsername(), inactiveStaffInClinic1.getEmail(),
                    Set.of(RoleEnum.ADMIN.name()), inactiveStaffInClinic1.getAvatar(),
                    inactiveStaffInClinic1.getName(), inactiveStaffInClinic1.getSurname(),
                    false, // Inactive
                    inactiveStaffInClinic1.getClinic().getId(), inactiveStaffInClinic1.getClinic().getName(),
                    null, null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
        }

        @Test
        @DisplayName("findAllStaffByClinic should return all staff when authorized")
        void findAllStaffByClinic_Success() {
            // Arrange
            doNothing().when(authorizationHelper).verifyClinicStaffAccess(adminUser.getId(), clinic1.getId(), actionContextAll);
            given(clinicStaffRepository.findByClinicId(clinic1.getId())).willReturn(List.of(activeStaffInClinic1, inactiveStaffInClinic1));
            given(userMapper.toClinicStaffProfileDtoList(anyList())).willReturn(List.of(activeDto, inactiveDto));

            // Act
            List<ClinicStaffProfileDto> result = clinicStaffService.findAllStaffByClinic(clinic1.getId(), adminUser.getId());

            // Assert
            assertThat(result).hasSize(2).containsExactlyInAnyOrder(activeDto, inactiveDto);
            then(authorizationHelper).should().verifyClinicStaffAccess(adminUser.getId(), clinic1.getId(), actionContextAll);
            then(clinicStaffRepository).should().findByClinicId(clinic1.getId());
            then(userMapper).should().toClinicStaffProfileDtoList(anyList());
        }

        @Test
        @DisplayName("findActiveStaffByClinic should return only active staff when authorized")
        void findActiveStaffByClinic_Success() {
            // Arrange
            String actionContextActive = "view active staff for";
            doNothing().when(authorizationHelper).verifyClinicStaffAccess(adminUser.getId(), clinic1.getId(), actionContextActive);
            given(clinicStaffRepository.findByClinicIdAndIsActive(clinic1.getId(), true)).willReturn(List.of(activeStaffInClinic1));
            given(userMapper.toClinicStaffProfileDtoList(List.of(activeStaffInClinic1))).willReturn(List.of(activeDto));

            // Act
            List<ClinicStaffProfileDto> result = clinicStaffService.findActiveStaffByClinic(clinic1.getId(), adminUser.getId());

            // Assert
            assertThat(result).hasSize(1).containsExactly(activeDto);
            then(authorizationHelper).should().verifyClinicStaffAccess(adminUser.getId(), clinic1.getId(), actionContextActive);
            then(clinicStaffRepository).should().findByClinicIdAndIsActive(clinic1.getId(), true);
            then(userMapper).should().toClinicStaffProfileDtoList(anyList());
        }

        @Test
        @DisplayName("find staff should throw AccessDeniedException if requester not Vet or Admin")
        void findStaffByClinic_Error_RequesterInvalidRole() {
            // Arrange
            doThrow(new AccessDeniedException("User " + 200L + " is not Clinic Staff..."))
                    .when(authorizationHelper).verifyClinicStaffAccess(200L, clinic1.getId(), actionContextAll);

            // Act
            Throwable thrown = Assertions.catchThrowable(() -> clinicStaffService.findAllStaffByClinic(clinic1.getId(), 200L));
            // Assert
            assertThat(thrown)
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("is not Clinic Staff");

            // Verify interactions
            then(authorizationHelper).should().verifyClinicStaffAccess(200L, clinic1.getId(), actionContextAll);
            then(clinicStaffRepository).should(never()).findByClinicId(anyLong());
        }

        @Test
        @DisplayName("find staff should throw AccessDeniedException if requester from different clinic")
        void findStaffByClinic_Error_DifferentClinic() {
            // Arrange
            Long requesterOtherClinicId = 20L;
            Long targetClinicId = clinic1.getId();
            doThrow(new AccessDeniedException(String.format("User (ID: %d...) cannot %s clinic %d.", requesterOtherClinicId, actionContextAll, targetClinicId))) // Mensaje más preciso
                    .when(authorizationHelper).verifyClinicStaffAccess(requesterOtherClinicId, targetClinicId, actionContextAll);

            // Act
            Throwable thrown = Assertions.catchThrowable(() ->clinicStaffService.findAllStaffByClinic(targetClinicId, requesterOtherClinicId));
            // Assert
            assertThat(thrown)
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining(String.format("cannot %s clinic %d", actionContextAll, targetClinicId));
            then(authorizationHelper).should().verifyClinicStaffAccess(requesterOtherClinicId, targetClinicId, actionContextAll);
            then(clinicStaffRepository).should(never()).findByClinicId(anyLong());
        }

        @Test
        @DisplayName("find staff should throw EntityNotFoundException if requester not found")
        void findStaffByClinic_Error_RequesterNotFound() {
            // Arrange
            Long nonExistentUserId = 999L;
            doThrow(new EntityNotFoundException("Requesting user not found..."))
                    .when(authorizationHelper).verifyClinicStaffAccess(nonExistentUserId, clinic1.getId(), actionContextAll);

            // Act
            Throwable thrown = Assertions.catchThrowable(() -> clinicStaffService.findAllStaffByClinic(clinic1.getId(), nonExistentUserId));
            // Assert
            assertThat(thrown)
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Requesting user not found");
            then(authorizationHelper).should().verifyClinicStaffAccess(nonExistentUserId, clinic1.getId(), actionContextAll);
            then(clinicStaffRepository).should(never()).findByClinicId(anyLong());
        }

        @Test
        @DisplayName("find staff should throw EntityNotFoundException if target clinic not found")
        void findStaffByClinic_Error_TargetClinicNotFound() {
            // Arrange
            Long nonExistentClinicId = 99L;
            doThrow(new EntityNotFoundException("Target clinic not found..."))
                    .when(authorizationHelper).verifyClinicStaffAccess(adminUser.getId(), nonExistentClinicId, actionContextAll);

            // Act
            Throwable thrown = Assertions.catchThrowable(() ->clinicStaffService.findAllStaffByClinic(nonExistentClinicId, adminUser.getId()));
            // Assert
            assertThat(thrown)
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Target clinic not found");
            then(authorizationHelper).should().verifyClinicStaffAccess(adminUser.getId(), nonExistentClinicId, actionContextAll);
            then(clinicStaffRepository).should(never()).findByClinicId(anyLong());
        }
    }
}
