package com.petconnect.backend.user.application.service.impl;

import com.petconnect.backend.exception.EntityNotFoundException; // Your custom exception
import com.petconnect.backend.user.application.dto.ClinicDto;
import com.petconnect.backend.user.application.dto.ClinicUpdateDto;
import com.petconnect.backend.user.application.mapper.ClinicMapper;
import com.petconnect.backend.user.domain.model.*;
import com.petconnect.backend.user.domain.repository.ClinicRepository;
import com.petconnect.backend.user.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.util.StringUtils; // Import StringUtils used in doAnswer


import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;


/**
 * Unit tests for {@link ClinicServiceImpl}.
 * Uses Mockito to mock repository and mapper dependencies, verifying business logic,
 * data retrieval, updates, and authorization checks.
 *
 * @author ibosquet
 */
@ExtendWith(MockitoExtension.class) // Initialize Mockito environment
class ClinicServiceImplTest {
    // --- Mocks ---
    @Mock
    private ClinicRepository clinicRepository; // Mock the data access layer for clinics
    @Mock
    private ClinicMapper clinicMapper;         // Mock the DTO-Entity mapping utility
    @Mock
    private UserRepository userRepository;     // Mock the data access layer for users (needed for auth checks)
    // --- Class Under Test ---
    @InjectMocks // Create an instance of ClinicServiceImpl and inject the mocks declared above
    private ClinicServiceImpl clinicService;
    // --- Argument Captors ---
    @Captor // Captures arguments passed to mocked methods for verification
    private ArgumentCaptor<Specification<Clinic>> specificationCaptor; // Captures Specification<Clinic>
    @Captor
    private ArgumentCaptor<Clinic> clinicCaptor;                 // Captures Clinic entities
    // --- Test Data (Setup before each test) ---
    private Clinic clinic1, clinic2;
    private ClinicDto clinicDto1, clinicDto2;
    private Pageable pageable;
    private ClinicUpdateDto updateDto;
    private ClinicStaff adminUser;      // Represents the admin performing actions
    private Clinic existingClinic;     // Represents the clinic being acted upon (e.g., updated)
    /**
     Sets up common test data before each test method execution.
     */
    @BeforeEach
    void setUp() {
// Simulate Clinic entities
        clinic1 = Clinic.builder().name("London Vet").city("London").country(Country.UNITED_KINGDOM).publicKey("PUB1").build();
        clinic1.setId(1L); // Set ID manually because builder doesn't handle inherited fields
        clinic2 = Clinic.builder().name("Paris Pets").city("Paris").country(Country.FRANCE).publicKey("PUB2").build();
        clinic2.setId(2L);
// Simulate corresponding Clinic DTOs
        clinicDto1 = new ClinicDto(1L, "London Vet", "Addr1", "London", Country.UNITED_KINGDOM, "111", "PUB1");
        clinicDto2 = new ClinicDto(2L, "Paris Pets", "Addr2", "Paris", Country.FRANCE, "222", "PUB2");
// Standard pagination request
        pageable = PageRequest.of(0, 10);
// Simulate a DTO used for update requests
        updateDto = new ClinicUpdateDto("Updated Name", "Updated Addr", "Updated City", null, "999");
// Simulate the existing clinic entity that will be updated in some tests
        existingClinic = Clinic.builder().name("London Vet").city("London").country(Country.UNITED_KINGDOM).publicKey("PUB1").build();
        existingClinic.setId(1L);
// Simulate an Admin user who belongs to the existingClinic (Clinic ID 1)
        adminUser = new ClinicStaff();
        adminUser.setId(10L); // Assign an ID to the admin user
        adminUser.setUsername("admin_test_user"); // Assign other relevant fields
        adminUser.setName("Admin");
        adminUser.setSurname("User");
        adminUser.setClinic(existingClinic); // Associate admin with clinic 1
        RoleEntity adminRole = RoleEntity.builder().roleEnum(RoleEnum.ADMIN).build();
        adminRole.setId(3L); // Assign an ID to the role
        adminUser.setRoles(Set.of(adminRole)); // Assign the ADMIN role
    }
// --- Tests for findClinics ---
    /**
     Test case for {@link ClinicServiceImpl#findClinics(String, String, String, Pageable)}
     when no filters are applied.
     Verifies that the repository's findAll method is called with a Specification and Pageable,
     and the resulting page of entities is correctly mapped to a page of DTOs.
     */
    @Test
    @DisplayName("findClinics should return paged DTOs when no filters are provided")
    void findClinics_shouldReturnPagedClinics_whenNoFilters() {
// Arrange: Prepare mock data and behavior
        List<Clinic> clinicList = List.of(clinic1, clinic2);
        Page<Clinic> clinicPageFromRepo = new PageImpl<>(clinicList, pageable, clinicList.size());
        given(clinicRepository.findAll(any(Specification.class), eq(pageable))).willReturn(clinicPageFromRepo);
        given(clinicMapper.toDto(clinic1)).willReturn(clinicDto1);
        given(clinicMapper.toDto(clinic2)).willReturn(clinicDto2);
// Act: Call the service method
        Page<ClinicDto> resultPage = clinicService.findClinics(null, null, null, pageable);
// Assert: Verify the results and interactions
        assertThat(resultPage).isNotNull();
        assertThat(resultPage.getTotalElements()).isEqualTo(2);
        assertThat(resultPage.getContent()).hasSize(2).containsExactly(clinicDto1, clinicDto2);
        then(clinicRepository).should().findAll(specificationCaptor.capture(), eq(pageable));
        then(clinicMapper).should().toDto(clinic1);
        then(clinicMapper).should().toDto(clinic2);
        assertThat(specificationCaptor.getValue()).isNotNull(); // Verify a spec was passed
    }
    /**
     Test case for {@link ClinicServiceImpl#findClinics(String, String, String, Pageable)}
     when filters (e.g., by city) are applied.
     Verifies that the repository is called with a Specification reflecting the filters (implicitly)
     and only the matching results are mapped and returned.
     */
    @Test
    @DisplayName("findClinics should return filtered paged DTOs when filters are provided")
    void findClinics_shouldApplyFilters_whenFiltersProvided() {
// Arrange: Prepare mock data (only clinic1 matches "London")
        List<Clinic> filteredList = List.of(clinic1);
        Page<Clinic> filteredClinicPage = new PageImpl<>(filteredList, pageable, 1);
        given(clinicRepository.findAll(any(Specification.class), eq(pageable))).willReturn(filteredClinicPage);
        given(clinicMapper.toDto(clinic1)).willReturn(clinicDto1);
// Act: Call the service method with a filter
        Page<ClinicDto> result = clinicService.findClinics(null, "London", null, pageable);
// Assert: Verify the filtered results
        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1).containsExactly(clinicDto1);
        then(clinicRepository).should().findAll(specificationCaptor.capture(), eq(pageable));
        then(clinicMapper).should().toDto(clinic1);
        then(clinicMapper).should(never()).toDto(clinic2); // Ensure non-matching clinic wasn't mapped
        assertThat(specificationCaptor.getValue()).isNotNull();
    }
// --- Tests for findClinicById ---
    /**
     Test case for {@link ClinicServiceImpl#findClinicById(Long)} when the clinic exists.
     Verifies that the repository's findById is called and the found entity is mapped to a DTO.
     */
    @Test
    @DisplayName("findClinicById should return ClinicDto when clinic is found")
    void findClinicById_shouldReturnClinicDto_whenFound() {
// Arrange
        given(clinicRepository.findById(1L)).willReturn(Optional.of(clinic1));
        given(clinicMapper.toDto(clinic1)).willReturn(clinicDto1);
// Act
        ClinicDto result = clinicService.findClinicById(1L);
// Assert
        assertThat(result).isNotNull().isEqualTo(clinicDto1);
        then(clinicRepository).should().findById(eq(1L));
        then(clinicMapper).should().toDto(clinic1);
    }
    /**
     Test case for {@link ClinicServiceImpl#findClinicById(Long)} when the clinic does not exist.
     Verifies that the repository's findById is called and an EntityNotFoundException is thrown.
     */
    @Test
    @DisplayName("findClinicById should throw EntityNotFoundException when clinic is not found")
    void findClinicById_shouldThrowEntityNotFoundException_whenNotFound() {
// Arrange
        given(clinicRepository.findById(99L)).willReturn(Optional.empty());
// Act & Assert
        assertThatThrownBy(() -> clinicService.findClinicById(99L))
                .isInstanceOf(EntityNotFoundException.class) // Expecting YOUR custom exception
                .hasMessageContaining("Clinic not found with id: 99");
        then(clinicRepository).should().findById(eq(99L));
        then(clinicMapper).should(never()).toDto(any()); // Mapper should not be called
    }
// --- Tests for updateClinic ---
    /**
     Test case for {@link ClinicServiceImpl#updateClinic(Long, ClinicUpdateDto, Long)}
     when the performing user is an authorized Admin of the target clinic.
     Verifies authorization checks pass, the mapper's update method is called,
     the entity is saved, and the updated DTO is returned.
     */
    @Test
    @DisplayName("updateClinic should update and return DTO when admin is authorized")
    void updateClinic_shouldUpdateAndReturnDto_whenAdminIsAuthorized() {
// Arrange
        Long clinicId = 1L;
        Long adminId = 10L;
        ClinicDto dtoResultadoEsperado = new ClinicDto(clinicId, "Updated Name", "Updated Addr", "Updated City", null, "999", "PUB1");
        given(userRepository.findById(adminId)).willReturn(Optional.of(adminUser)); // Admin found
        given(clinicRepository.findById(clinicId)).willReturn(Optional.of(existingClinic)); // Clinic found
        given(clinicRepository.save(any(Clinic.class))).willAnswer(invocation -> invocation.getArgument(0)); // Save returns input
        given(clinicMapper.toDto(any(Clinic.class))).willReturn(dtoResultadoEsperado); // Map returns final DTO
// Use doAnswer to simulate and verify the side effect of updateFromDto
        doAnswer(invocation -> {
            Clinic clinicArg = invocation.getArgument(1);
            ClinicUpdateDto dtoArg = invocation.getArgument(0);
// Simulate the actual mapping logic
            if (StringUtils.hasText(dtoArg.name())) clinicArg.setName(dtoArg.name());
            if (StringUtils.hasText(dtoArg.address())) clinicArg.setAddress(dtoArg.address());
            if (StringUtils.hasText(dtoArg.city())) clinicArg.setCity(dtoArg.city());
            if (dtoArg.country() != null) clinicArg.setCountry(dtoArg.country());
            if (StringUtils.hasText(dtoArg.phone())) clinicArg.setPhone(dtoArg.phone());
// Assert state immediately after simulated update
            assertThat(clinicArg.getName()).isEqualTo("Updated Name");
            return null; // void method
        }).when(clinicMapper).updateFromDto(updateDto, existingClinic);
// Act
        ClinicDto result = clinicService.updateClinic(clinicId, updateDto, adminId);
// Assert
        assertThat(result).isNotNull().isEqualTo(dtoResultadoEsperado);
// Verify interactions
        then(userRepository).should().findById(eq(adminId));
        then(clinicRepository).should().findById(eq(clinicId));
        then(clinicMapper).should().updateFromDto(eq(updateDto), eq(existingClinic)); // Verify call
        then(clinicRepository).should().save(clinicCaptor.capture());          // Verify save and capture
        then(clinicMapper).should().toDto(any(Clinic.class));                    // Verify final mapping
// Verify captured entity state
        Clinic savedClinic = clinicCaptor.getValue();
        assertThat(savedClinic.getName()).isEqualTo("Updated Name");
        assertThat(savedClinic.getAddress()).isEqualTo("Updated Addr");
        assertThat(savedClinic.getId()).isEqualTo(clinicId);
    }
    /**
     Test case for {@link ClinicServiceImpl#updateClinic(Long, ClinicUpdateDto, Long)}
     when the admin user performing the action is not found.
     Verifies that an EntityNotFoundException is thrown before accessing clinic data.
     */
    @Test
    @DisplayName("updateClinic should throw EntityNotFoundException when admin user not found")
    void updateClinic_shouldThrowEntityNotFoundException_whenAdminUserNotFound() {
// Arrange
        Long clinicId = 1L;
        Long nonExistentAdminId = 99L;
        given(userRepository.findById(nonExistentAdminId)).willReturn(Optional.empty());
// Act & Assert
        assertThatThrownBy(() -> clinicService.updateClinic(clinicId, updateDto, nonExistentAdminId))
                .isInstanceOf(EntityNotFoundException.class) // Expecting YOUR custom exception
                .hasMessageContaining("Admin user not found with id: 99");
// Verify no further interactions occurred
        then(clinicRepository).should(never()).findById(anyLong());
        then(clinicRepository).should(never()).save(any());
        then(clinicMapper).should(never()).updateFromDto(any(), any());
    }
    /**
     Test case for {@link ClinicServiceImpl#updateClinic(Long, ClinicUpdateDto, Long)}
     when the target clinic to be updated is not found.
     Verifies that an EntityNotFoundException is thrown after the admin user is found.
     */
    @Test
    @DisplayName("updateClinic should throw EntityNotFoundException when clinic not found")
    void updateClinic_shouldThrowEntityNotFoundException_whenClinicNotFound() {
// Arrange
        Long nonExistentClinicId = 99L;
        Long adminId = 10L;
        given(userRepository.findById(adminId)).willReturn(Optional.of(adminUser)); // Admin exists
        given(clinicRepository.findById(nonExistentClinicId)).willReturn(Optional.empty()); // Clinic doesn't
// Act & Assert
        assertThatThrownBy(() -> clinicService.updateClinic(nonExistentClinicId, updateDto, adminId))
                .isInstanceOf(EntityNotFoundException.class) // Expecting YOUR custom exception
                .hasMessageContaining("Clinic not found with id: 99");
// Verify interactions
        then(userRepository).should().findById(eq(adminId)); // Admin lookup happened
        then(clinicRepository).should().findById(eq(nonExistentClinicId)); // Clinic lookup happened
        then(clinicRepository).should(never()).save(any());
        then(clinicMapper).should(never()).updateFromDto(any(), any());
    }
    /**
     Test case for {@link ClinicServiceImpl#updateClinic(Long, ClinicUpdateDto, Long)}
     when the user attempting the update is not an Admin.
     Verifies that an AccessDeniedException is thrown during the authorization check.
     */
    @Test
    @DisplayName("updateClinic should throw AccessDeniedException when user is not an Admin")
    void updateClinic_shouldThrowAccessDeniedException_whenUserIsNotAdmin() {
// Arrange: Simulate a user who exists but has the VET role, not ADMIN
        Long clinicId = 1L;
        Long vetUserId = 11L;
        ClinicStaff vetUser = new ClinicStaff();
        vetUser.setId(vetUserId);
        vetUser.setClinic(existingClinic); // Belongs to the correct clinic
        RoleEntity vetRole = RoleEntity.builder().id(2L).roleEnum(RoleEnum.VET).build();
        vetUser.setRoles(Set.of(vetRole)); // Assign VET role
        given(userRepository.findById(vetUserId)).willReturn(Optional.of(vetUser)); // User found
// clinicRepository.findById is not needed here as the check fails earlier
// Act & Assert
        assertThatThrownBy(() -> clinicService.updateClinic(clinicId, updateDto, vetUserId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("is not authorized to update clinic information");
// Verify interactions
        then(userRepository).should().findById(eq(vetUserId)); // User lookup happened
        then(clinicRepository).should(never()).findById(anyLong()); // Clinic lookup should not happen
        then(clinicRepository).should(never()).save(any());
        then(clinicMapper).should(never()).updateFromDto(any(), any());
    }
    /**
     Test case for {@link ClinicServiceImpl#updateClinic(Long, ClinicUpdateDto, Long)}
     when the Admin user exists and has the ADMIN role, but attempts to update a clinic
     different from their own.
     Verifies that an AccessDeniedException is thrown during the clinic ownership check.
     */
    @Test
    @DisplayName("updateClinic should throw AccessDeniedException when Admin is from a different clinic")
    void updateClinic_shouldThrowAccessDeniedException_whenAdminIsFromDifferentClinic() {
// Arrange: Simulate an admin from a different clinic
        Long clinicIdToUpdate = 1L; // Target: London clinic
        Long adminFromAnotherClinicId = 12L;
        Clinic anotherClinic = Clinic.builder().name("Paris").build();
        anotherClinic.setId(2L); // Admin's clinic is ID 2
        ClinicStaff adminFromAnotherClinic = new ClinicStaff();
        adminFromAnotherClinic.setId(adminFromAnotherClinicId);
        adminFromAnotherClinic.setClinic(anotherClinic); // Associate admin with clinic 2
        RoleEntity adminRole = RoleEntity.builder().id(3L).roleEnum(RoleEnum.ADMIN).build();
        adminFromAnotherClinic.setRoles(Set.of(adminRole));
        given(userRepository.findById(adminFromAnotherClinicId)).willReturn(Optional.of(adminFromAnotherClinic)); // Find this admin
        given(clinicRepository.findById(clinicIdToUpdate)).willReturn(Optional.of(existingClinic)); // Target clinic (ID 1) found
// Act & Assert
        assertThatThrownBy(() -> clinicService.updateClinic(clinicIdToUpdate, updateDto, adminFromAnotherClinicId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("is not authorized to update clinic 1"); // Verify message
// Verify interactions
        then(userRepository).should().findById(eq(adminFromAnotherClinicId)); // Admin lookup happened
        then(clinicRepository).should().findById(eq(clinicIdToUpdate)); // Target clinic lookup happened
        then(clinicRepository).should(never()).save(any()); // Save should not be called
        then(clinicMapper).should(never()).updateFromDto(any(), any()); // Update mapper not called
    }
}