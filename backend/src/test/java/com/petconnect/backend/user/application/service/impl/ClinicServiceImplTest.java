package com.petconnect.backend.user.application.service.impl;

import com.petconnect.backend.common.helper.EntityFinderHelper;
import com.petconnect.backend.exception.EntityNotFoundException;
import com.petconnect.backend.user.application.dto.ClinicDto;
import com.petconnect.backend.user.application.dto.ClinicUpdateDto;
import com.petconnect.backend.user.application.mapper.ClinicMapper;
import com.petconnect.backend.user.domain.model.*;
import com.petconnect.backend.user.domain.repository.ClinicRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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


import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ClinicServiceImpl}.
 * Uses Mockito to mock repository, mapper and helper dependencies.
 *
 * @author ibosquet
 */
@ExtendWith(MockitoExtension.class)
class ClinicServiceImplTest {
    // --- Mocks ---
    @Mock private ClinicRepository clinicRepository;
    @Mock private ClinicMapper clinicMapper;
    @Mock private EntityFinderHelper entityFinderHelper;

    @InjectMocks private ClinicServiceImpl clinicService;

    // --- Captors ---
    @Captor private ArgumentCaptor<Specification<Clinic>> specificationCaptor;
    @Captor private ArgumentCaptor<Clinic> clinicCaptor;


    // --- Test Data ---
    private Clinic clinic1, clinic2;
    private ClinicDto clinicDto1, clinicDto2;
    private Pageable pageable;
    private ClinicUpdateDto updateDto;
    private ClinicStaff adminUser;
    private Clinic existingClinic;
    private final Long clinicId = 1L;
    private final Long adminId = 10L;

    /**
     * Sets up common test data before each test method execution.
     */
    @BeforeEach
    void setUp() {
        // Clinic Entities
        clinic1 = Clinic.builder().name("London Vet").city("London").country(Country.UNITED_KINGDOM).publicKey("PUB1").build();
        clinic1.setId(clinicId);
        clinic2 = Clinic.builder().name("Paris Pets").city("Paris").country(Country.FRANCE).publicKey("PUB2").build();
        clinic2.setId(2L);

        // Clinic DTOs
        clinicDto1 = new ClinicDto(clinicId, "London Vet", "Addr1", "London", Country.UNITED_KINGDOM, "111", "PUB1");
        clinicDto2 = new ClinicDto(2L, "Paris Pets", "Addr2", "Paris", Country.FRANCE, "222", "PUB2");

        // Pageable
        pageable = PageRequest.of(0, 10);

        // Update DTO
        updateDto = new ClinicUpdateDto("Updated Name", "Updated Addr", "Updated City", Country.SPAIN, "999");

        // Existing Clinic for Update Tests
        existingClinic = new Clinic();
        existingClinic.setId(clinicId);
        existingClinic.setName("London Vet");
        existingClinic.setCity("London");
        existingClinic.setCountry(Country.UNITED_KINGDOM);
        existingClinic.setPublicKey("PUB1");

        // Admin User belonging to existingClinic
        adminUser = new ClinicStaff();
        adminUser.setId(adminId);
        adminUser.setUsername("admin_test");
        adminUser.setClinic(existingClinic);
        RoleEntity adminRole = RoleEntity.builder().roleEnum(RoleEnum.ADMIN).build();
        adminUser.setRoles(Set.of(adminRole));
    }

    /**
     * --- Tests for findClinics ---
     */
    @Nested
    @DisplayName("findClinics Tests")
    class FindClinicsTests {
        @Test
        @DisplayName("findClinics should return paged DTOs when no filters are provided")
        void findClinics_shouldReturnPagedClinics_whenNoFilters() {
            // Arrange
            List<Clinic> clinicList = List.of(clinic1, clinic2);
            Page<Clinic> clinicPageFromRepo = new PageImpl<>(clinicList, pageable, clinicList.size());
            given(clinicRepository.findAll(any(Specification.class), eq(pageable))).willReturn(clinicPageFromRepo);
            given(clinicMapper.toDto(clinic1)).willReturn(clinicDto1);
            given(clinicMapper.toDto(clinic2)).willReturn(clinicDto2);

            // Act
            Page<ClinicDto> resultPage = clinicService.findClinics(null, null, null, pageable);

            // Assert
            assertThat(resultPage).isNotNull();
            assertThat(resultPage.getTotalElements()).isEqualTo(2);
            assertThat(resultPage.getContent()).hasSize(2).containsExactly(clinicDto1, clinicDto2);
            then(clinicRepository).should().findAll(specificationCaptor.capture(), eq(pageable));
            then(clinicMapper).should().toDto(clinic1);
            then(clinicMapper).should().toDto(clinic2);
            assertThat(specificationCaptor.getValue()).isNotNull();
        }

        @Test
        @DisplayName("findClinics should return filtered paged DTOs when filters are provided")
        void findClinics_shouldApplyFilters_whenFiltersProvided() {
            // Arrange
            List<Clinic> filteredList = List.of(clinic1);
            Page<Clinic> filteredClinicPage = new PageImpl<>(filteredList, pageable, 1);
            given(clinicRepository.findAll(any(Specification.class), eq(pageable))).willReturn(filteredClinicPage);
            given(clinicMapper.toDto(clinic1)).willReturn(clinicDto1);

            // Act
            Page<ClinicDto> result = clinicService.findClinics(null, null, Country.UNITED_KINGDOM.name(), pageable);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent()).containsExactly(clinicDto1);

            then(clinicRepository).should().findAll(specificationCaptor.capture(), eq(pageable));
            then(clinicMapper).should().toDto(clinic1);
            then(clinicMapper).should(never()).toDto(clinic2);
            assertThat(specificationCaptor.getValue()).isNotNull();
        }
    }

    /**
     * --- Tests for findClinicById ---
     */
    @Nested
    @DisplayName("findClinicById Tests")
    class FindClinicByIdTests { // Renombrado
        @Test
        @DisplayName("should return ClinicDto when clinic is found")
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
         * Test case for {@link ClinicServiceImpl#findClinicById(Long)} when the clinic does not exist.
         * Verifies that the repository's findById is called and an EntityNotFoundException is thrown.
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
    }

    /**
     * --- Tests for updateClinic ---
     */
    @Nested
    @DisplayName("updateClinic Tests")
    class UpdateClinicTests { // Renombrado
        @Test
        @DisplayName("should update and return DTO when admin is authorized")
        void updateClinic_shouldUpdateAndReturnDto_whenAdminIsAuthorized() {
            // Arrange
            ClinicDto dtoResultadoEsperado = new ClinicDto(clinicId, "Updated Name", "Updated Addr", "Updated City", Country.SPAIN, "999", "PUB1");
            given(entityFinderHelper.findUserOrFail(adminId)).willReturn(adminUser);
            given(entityFinderHelper.findClinicOrFail(clinicId)).willReturn(existingClinic);
            given(clinicRepository.save(any(Clinic.class))).willAnswer(invocation -> invocation.getArgument(0));
            given(clinicMapper.toDto(any(Clinic.class))).willReturn(dtoResultadoEsperado);
            doNothing().when(clinicMapper).updateFromDto((updateDto), (existingClinic));

            // Act
            ClinicDto result = clinicService.updateClinic(clinicId, updateDto, adminId);

            // Assert
            assertThat(result).isNotNull().isEqualTo(dtoResultadoEsperado);

            then(entityFinderHelper).should().findUserOrFail(eq(adminId));
            then(entityFinderHelper).should().findClinicOrFail(eq(clinicId));
            then(clinicMapper).should().updateFromDto((updateDto), (existingClinic));
            then(clinicRepository).should().save(clinicCaptor.capture());
            then(clinicMapper).should().toDto(any(Clinic.class));

            Clinic savedClinic = clinicCaptor.getValue();
            assertThat(savedClinic.getId()).isEqualTo(clinicId);
        }

        @Test
        @DisplayName("should throw EntityNotFoundException when admin user not found")
        void updateClinic_shouldThrowEntityNotFoundException_whenAdminUserNotFound() {
            // Arrange
            Long nonExistentAdminId = 99L;
            given(entityFinderHelper.findUserOrFail(nonExistentAdminId))
                    .willThrow(new EntityNotFoundException(UserEntity.class.getSimpleName(), nonExistentAdminId));

            // Act & Assert
            assertThatThrownBy(() -> clinicService.updateClinic(clinicId, updateDto, nonExistentAdminId))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("UserEntity not found with id: 99");

            then(entityFinderHelper).should(never()).findClinicOrFail(anyLong());
            then(clinicRepository).should(never()).save(any());
            then(clinicMapper).should(never()).updateFromDto(any(), any());
        }

        @Test
        @DisplayName("should throw EntityNotFoundException when clinic not found")
        void updateClinic_shouldThrowEntityNotFoundException_whenClinicNotFound() {
            // Arrange
            Long nonExistentClinicId = 99L;
            given(entityFinderHelper.findUserOrFail(adminId)).willReturn(adminUser);
            given(entityFinderHelper.findClinicOrFail(nonExistentClinicId))
                    .willThrow(new EntityNotFoundException(Clinic.class.getSimpleName(), nonExistentClinicId));

            // Act & Assert
            assertThatThrownBy(() -> clinicService.updateClinic(nonExistentClinicId, updateDto, adminId))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Clinic not found with id: 99");

            then(entityFinderHelper).should().findUserOrFail(eq(adminId));
            then(entityFinderHelper).should().findClinicOrFail(eq(nonExistentClinicId));
            then(clinicRepository).should(never()).save(any());
            then(clinicMapper).should(never()).updateFromDto(any(), any());
        }

        @Test
        @DisplayName("should throw AccessDeniedException when user is not an Admin")
        void updateClinic_shouldThrowAccessDeniedException_whenUserIsNotAdmin() {
            // Arrange
            Long vetUserId = 11L;
            ClinicStaff vetUser = new Vet();
            vetUser.setId(vetUserId);
            vetUser.setClinic(existingClinic);
            RoleEntity vetRole = RoleEntity.builder().id(2L).roleEnum(RoleEnum.VET).build();
            vetUser.setRoles(Set.of(vetRole));
            given(entityFinderHelper.findUserOrFail(vetUserId)).willReturn(vetUser);

            // Act & Assert
            assertThatThrownBy(() -> clinicService.updateClinic(clinicId, updateDto, vetUserId))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("is not authorized to update clinic information");

            then(entityFinderHelper).should().findUserOrFail(eq(vetUserId));
            then(entityFinderHelper).should(never()).findClinicOrFail(anyLong());
            then(clinicRepository).should(never()).save(any());
            then(clinicMapper).should(never()).updateFromDto(any(), any());
        }

        @Test
        @DisplayName("should throw AccessDeniedException when Admin is from a different clinic")
        void updateClinic_shouldThrowAccessDeniedException_whenAdminIsFromDifferentClinic() {
            // Arrange
            Long clinicIdToUpdate = 1L;
            Long adminFromAnotherClinicId = 12L;
            Clinic anotherClinic = Clinic.builder().name("Paris").build(); anotherClinic.setId(2L);
            ClinicStaff adminFromAnotherClinic = new ClinicStaff();
            adminFromAnotherClinic.setId(adminFromAnotherClinicId);
            adminFromAnotherClinic.setClinic(anotherClinic);
            RoleEntity adminRole = RoleEntity.builder().id(3L).roleEnum(RoleEnum.ADMIN).build();
            adminFromAnotherClinic.setRoles(Set.of(adminRole));
            given(entityFinderHelper.findUserOrFail(adminFromAnotherClinicId)).willReturn(adminFromAnotherClinic);
            given(entityFinderHelper.findClinicOrFail(clinicIdToUpdate)).willReturn(existingClinic);

            // Act & Assert
            assertThatThrownBy(() -> clinicService.updateClinic(clinicIdToUpdate, updateDto, adminFromAnotherClinicId))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("is not authorized to update clinic 1");

            then(entityFinderHelper).should().findUserOrFail(eq(adminFromAnotherClinicId));
            then(entityFinderHelper).should().findClinicOrFail(eq(clinicIdToUpdate));
            then(clinicRepository).should(never()).save(any());
            then(clinicMapper).should(never()).updateFromDto(any(), any());
        }
    }

    /**
     * --- Tests for getDistinctClinicCountries ---
     */
    @Nested
    @DisplayName("getDistinctClinicCountries Tests")
    class GetDistinctClinicCountriesTests {

        @Test
        @DisplayName("should return list of distinct countries from repository")
        void shouldReturnDistinctCountries() {
            // Arrange
            List<Country> expectedCountries = List.of(Country.FRANCE, Country.SPAIN, Country.UNITED_KINGDOM);
            given(clinicRepository.findDistinctCountries()).willReturn(expectedCountries);

            // Act
            List<Country> result = clinicService.getDistinctClinicCountries();

            // Assert
            assertThat(result)
                    .isNotNull()
                    .hasSize(3)
                    .containsExactly(Country.FRANCE, Country.SPAIN, Country.UNITED_KINGDOM);

            then(clinicRepository).should().findDistinctCountries();
        }

        @Test
        @DisplayName("should return empty list when repository returns empty list")
        void shouldReturnEmptyListWhenRepoIsEmpty() {
            // Arrange
            given(clinicRepository.findDistinctCountries()).willReturn(Collections.emptyList());

            // Act
            List<Country> result = clinicService.getDistinctClinicCountries();

            // Assert
            assertThat(result).isNotNull().isEmpty();
            then(clinicRepository).should().findDistinctCountries();
        }
    }
}