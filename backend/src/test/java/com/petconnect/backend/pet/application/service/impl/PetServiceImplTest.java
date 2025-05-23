package com.petconnect.backend.pet.application.service.impl;

import com.petconnect.backend.common.helper.AuthorizationHelper;
import com.petconnect.backend.common.service.ImageService;
import com.petconnect.backend.exception.EntityNotFoundException;
import com.petconnect.backend.exception.MicrochipAlreadyExistsException;
import com.petconnect.backend.pet.application.dto.*;
import com.petconnect.backend.pet.application.mapper.BreedMapper;
import com.petconnect.backend.pet.application.mapper.PetMapper;
import com.petconnect.backend.pet.domain.model.*;
import com.petconnect.backend.pet.domain.repository.BreedRepository;
import com.petconnect.backend.pet.domain.repository.PetRepository;
import com.petconnect.backend.user.application.dto.OwnerSummaryDto;
import com.petconnect.backend.user.application.dto.VetSummaryDto;
import com.petconnect.backend.user.application.mapper.UserMapper;
import com.petconnect.backend.user.domain.model.*;
import com.petconnect.backend.common.helper.EntityFinderHelper;

import lombok.extern.slf4j.Slf4j;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link PetServiceImpl}.
 * Verifies the business logic for managing Pet entities using Mockito.
 *
 * @author ibosquet
 */
@ExtendWith(MockitoExtension.class)
@Slf4j
class PetServiceImplTest {

    // --- Mocks ---
    @Mock private PetRepository petRepository;
    @Mock private BreedRepository breedRepository;
    @Mock private PetMapper petMapper;
    @Mock private BreedMapper breedMapper;
    @Mock private UserMapper userMapper;
    @Mock private EntityFinderHelper entityFinderHelper;
    @Mock private AuthorizationHelper authorizationHelper;
    @Mock private ImageService imageService;

    @InjectMocks
    private PetServiceImpl petService;

    // --- Captors ---
    @Captor private ArgumentCaptor<Pet> petCaptor;

    // --- Test Data ---
    private Owner owner;
    private OwnerSummaryDto ownerSummaryDto;
    private Breed dogBreedSpecific;
    private Breed mixedDogBreed;
    private PetRegistrationDto registrationDtoSpecificBreed;
    private Pet savedPet;
    private final Long ownerId = 1L;
    private final Long specificBreedId = 10L;
    private final Long savedPetId = 101L;
    private final String defaultDogImagePath = "images/avatars/pets/dog.png";


    /**
     * Sets up common test data before each test method execution.
     */
    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(petService, "defaultPetImagePathBase", "images/avatars/pets/");
        owner = new Owner();
        owner.setId(ownerId);
        owner.setUsername("testowner");
        owner.setEmail("owner@test.com");
        owner.setPhone("123456789");

        String specificDogImagePath = "images/avatars/pets/labrador.png";
        dogBreedSpecific = Breed.builder().id(specificBreedId).name("Labrador").specie(Specie.DOG).imageUrl(specificDogImagePath).build();
        Long mixedBreedId = 99L;
        mixedDogBreed = Breed.builder().id(mixedBreedId).name("Mixed/Other").specie(Specie.DOG).imageUrl(defaultDogImagePath).build();

        registrationDtoSpecificBreed = new PetRegistrationDto(
                "Buddy", Specie.DOG, LocalDate.of(2023, 1, 15),
                specificBreedId,
                null,
                "Brown", Gender.MALE, "12345"
        );

        savedPet = new Pet();
        savedPet.setId(savedPetId);
        savedPet.setOwner(owner);
        savedPet.setStatus(PetStatus.PENDING);
        savedPet.setLastEuEntryDate(null);
        savedPet.setLastEuExitDate(null);

        ownerSummaryDto = new OwnerSummaryDto(owner.getId(), owner.getUsername(), owner.getEmail(), owner.getPhone());
    }

    /**
     * --- Tests for registerPet ---
     */
    @Nested
    @DisplayName("registerPet Tests")
    class RegisterPetTests {

        /**
         * Test case for successful pet registration with a specific breed ID and no image provided.
         * Expects the image path from the specific breed to be used.
         */
        @Test
        @DisplayName("should register pet successfully with specific breed and default image derived from breed")
        void registerPet_Success_SpecificBreed_DefaultImage() {
            // Arrange
            savedPet.setName(registrationDtoSpecificBreed.name());
            savedPet.setBreed(dogBreedSpecific);
            savedPet.setImage(dogBreedSpecific.getImageUrl());
            savedPet.setBirthDate(registrationDtoSpecificBreed.birthDate());
            savedPet.setColor(registrationDtoSpecificBreed.color());
            savedPet.setGender(registrationDtoSpecificBreed.gender());
            savedPet.setMicrochip(registrationDtoSpecificBreed.microchip());

            PetProfileDto localExpectedPetProfileDto = new PetProfileDto(
                    savedPetId, registrationDtoSpecificBreed.name(), Specie.DOG, registrationDtoSpecificBreed.color(),
                    registrationDtoSpecificBreed.gender(), registrationDtoSpecificBreed.birthDate(), registrationDtoSpecificBreed.microchip(),
                    dogBreedSpecific.getImageUrl(), //
                    PetStatus.PENDING, ownerId, owner.getUsername(), specificBreedId, dogBreedSpecific.getName(),
                    null, null,
                    Set.of(),
                    ownerSummaryDto,
                    false,
                    null,
                    null
            );

            given(entityFinderHelper.findOwnerOrFail(ownerId)).willReturn(owner);
            given(entityFinderHelper.findBreedOrFail(specificBreedId)).willReturn(dogBreedSpecific);
            given(petRepository.existsByMicrochip(registrationDtoSpecificBreed.microchip())).willReturn(false);

            given(petRepository.save(any(Pet.class))).willReturn(savedPet);
            given(petMapper.toProfileDto(savedPet)).willReturn(localExpectedPetProfileDto);

            // Act
            PetProfileDto result = petService.registerPet(registrationDtoSpecificBreed, ownerId, null);

            // Assert
            assertThat(result).isNotNull().isEqualTo(localExpectedPetProfileDto);
            assertThat(result.image()).isEqualTo(dogBreedSpecific.getImageUrl());

            then(entityFinderHelper).should().findOwnerOrFail(ownerId);
            then(entityFinderHelper).should().findBreedOrFail(specificBreedId);

            then(breedRepository).should(never()).findByNameAndSpecie(anyString(), any(Specie.class));
            then(petRepository).should().save(petCaptor.capture());
            then(petMapper).should().toProfileDto(savedPet);

            then(petRepository).should().save(petCaptor.capture());
            Pet capturedPet = petCaptor.getValue();
            assertThat(capturedPet.getImage()).isEqualTo(dogBreedSpecific.getImageUrl());
        }

        /**
         * Test case for successful pet registration with no breed ID (uses Mixed/Other fallback) and a provided image.
         * Expects the provided image path to be used.
         */
        @Test
        @DisplayName("should register pet successfully with fallback breed and provided image")
        void registerPet_Success_FallbackBreed_ProvidedImage() throws IOException {
            // Arrange
            PetRegistrationDto dtoForMixedBreedNoFile = new PetRegistrationDto(
                    "Mixy", Specie.DOG, LocalDate.of(2022, 3, 10),
                    null,
                    null,
                    "Black", Gender.FEMALE, null
            );

            Pet petToBeSaved = Pet.builder()
                    .name(dtoForMixedBreedNoFile.name())
                    .owner(owner)
                    .breed(mixedDogBreed)
                    .image(mixedDogBreed.getImageUrl())
                    .status(PetStatus.PENDING)
                    .birthDate(dtoForMixedBreedNoFile.birthDate())
                    .color(dtoForMixedBreedNoFile.color())
                    .gender(dtoForMixedBreedNoFile.gender())
                    .microchip(null)
                    .build();

            Pet petAfterSave = getPet(petToBeSaved);

            PetProfileDto localExpectedPetProfileDto = new PetProfileDto(
                    savedPetId, dtoForMixedBreedNoFile.name(), Specie.DOG, dtoForMixedBreedNoFile.color(),
                    dtoForMixedBreedNoFile.gender(), dtoForMixedBreedNoFile.birthDate(), null,
                    mixedDogBreed.getImageUrl(), // Imagen del mixedDogBreed (el mapper la construye completa)
                    PetStatus.PENDING, ownerId, owner.getUsername(), mixedDogBreed.getId(), mixedDogBreed.getName(),
                    null, null, Set.of(), ownerSummaryDto, false, null, null
            );

            given(entityFinderHelper.findOwnerOrFail(ownerId)).willReturn(owner);
            given(breedRepository.findByNameAndSpecie("Mixed/Other", Specie.DOG)).willReturn(Optional.of(mixedDogBreed));
            given(petRepository.save(any(Pet.class))).willReturn(petAfterSave);
            given(petMapper.toProfileDto(petAfterSave)).willReturn(localExpectedPetProfileDto);


            // Act
            PetProfileDto result = petService.registerPet(dtoForMixedBreedNoFile, ownerId, null);

            // Assert
            assertThat(result).isNotNull().isEqualTo(localExpectedPetProfileDto);
            assertThat(result.image()).isEqualTo(petMapper.toProfileDto(petAfterSave).image());

            then(imageService).should(never()).storeImage(any(), anyString());
            then(petRepository).should().save(petCaptor.capture());
            Pet captured = petCaptor.getValue();
            assertThat(captured.getImage()).isEqualTo(mixedDogBreed.getImageUrl());
            assertThat(captured.getBreed()).isEqualTo(mixedDogBreed);
        }

        private Pet getPet(Pet petToBeSaved) {
            return getAfterSave(petToBeSaved);
        }

        /**
         * Test case for successful pet registration with no breed ID and no image provided.
         * Expects the default species image path to be used.
         */
        @Test
        @DisplayName("should register pet successfully with fallback breed and default species image")
        void registerPet_Success_FallbackBreed_DefaultSpeciesImage() {
            // Arrange
            PetRegistrationDto dtoNoBreedNoImage = new PetRegistrationDto(
                    "Def", Specie.DOG, LocalDate.now(), null, null, null, null, null
            );

            PetProfileDto localExpectedPetProfileDto = new PetProfileDto(
                    savedPetId, dtoNoBreedNoImage.name(), Specie.DOG, null, null,
                    dtoNoBreedNoImage.birthDate(), null,
                    defaultDogImagePath, // El servicio determinará esto
                    PetStatus.PENDING, ownerId, owner.getUsername(),
                    mixedDogBreed.getId(), mixedDogBreed.getName(),
                    null, null, Set.of(), ownerSummaryDto, false, null, null
            );

            given(entityFinderHelper.findOwnerOrFail(ownerId)).willReturn(owner);
            given(breedRepository.findByNameAndSpecie("Mixed/Other", Specie.DOG)).willReturn(Optional.of(mixedDogBreed));

            given(petRepository.save(any(Pet.class))).willAnswer(invocation -> {
                Pet petPassedToSave = invocation.getArgument(0);
                petPassedToSave.setId(savedPetId);
                return petPassedToSave;
            });

            given(petMapper.toProfileDto(any(Pet.class))).willReturn(localExpectedPetProfileDto);

            // Act
            PetProfileDto result = petService.registerPet(dtoNoBreedNoImage, ownerId, null);

            // Assert
            assertThat(result).isNotNull().isEqualTo(localExpectedPetProfileDto);
            assertThat(result.image()).isEqualTo(localExpectedPetProfileDto.image());

            then(entityFinderHelper).should().findOwnerOrFail(ownerId);
            then(entityFinderHelper).should(never()).findBreedOrFail(anyLong());
            then(breedRepository).should().findByNameAndSpecie("Mixed/Other", Specie.DOG);

            then(petRepository).should().save(petCaptor.capture());
            Pet petSentToSave = petCaptor.getValue();
            then(petMapper).should().toProfileDto(eq(petSentToSave));

            assertThat(petSentToSave.getBreed()).isEqualTo(mixedDogBreed);
            assertThat(petSentToSave.getImage()).isEqualTo(defaultDogImagePath);
            assertThat(petSentToSave.getName()).isEqualTo(dtoNoBreedNoImage.name());
            assertThat(petSentToSave.getId()).isEqualTo(savedPetId);
        }

        /**
         * Test case verifying that an EntityNotFoundException is thrown if the owner ID does not exist.
         */
        @Test
        @DisplayName("should throw EntityNotFoundException if owner not found")
        void registerPet_Failure_OwnerNotFound() {
            // Arrange
            Long nonExistentOwnerId = 999L;
            given(entityFinderHelper.findOwnerOrFail(nonExistentOwnerId))
                    .willThrow(new EntityNotFoundException(Owner.class.getSimpleName(), nonExistentOwnerId));

            // Act & Assert
            assertThatThrownBy(() -> petService.registerPet(registrationDtoSpecificBreed, 999L, null))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Owner not found with id: 999");

            then(petRepository).should(never()).save(any());
        }

        /**
         * Test case verifying that an EntityNotFoundException is thrown if a specific breed ID is provided but the breed does not exist.
         */
        @Test
        @DisplayName("should throw EntityNotFoundException if specific breedId not found")
        void registerPet_Failure_BreedNotFound() {
            // Arrange
            Long nonExistentBreedId = 500L;
            PetRegistrationDto dtoWithBadBreed = new PetRegistrationDto(
                    "Buddy", Specie.DOG, LocalDate.now(),
                    nonExistentBreedId, null, null, null, null
            );
            given(entityFinderHelper.findOwnerOrFail(ownerId)).willReturn(owner);
            given(entityFinderHelper.findBreedOrFail(nonExistentBreedId))
                    .willThrow(new EntityNotFoundException(Breed.class.getSimpleName(), nonExistentBreedId));

            // Act & Assert
            assertThatThrownBy(() -> petService.registerPet(dtoWithBadBreed, ownerId, null))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Breed not found with id: " + nonExistentBreedId);

            then(entityFinderHelper).should().findOwnerOrFail(ownerId);
            then(entityFinderHelper).should().findBreedOrFail(nonExistentBreedId);
            then(petRepository).should(never()).save(any());
        }

        /**
         * Test case verifying that an IllegalStateException is thrown if the fallback 'Mixed/Other' breed is not configured for the species.
         */
        @Test
        @DisplayName("should throw IllegalStateException if fallback breed not found")
        void registerPet_Failure_FallbackBreedNotFound() {
            // Arrange
            PetRegistrationDto dtoWithoutBreedId = new PetRegistrationDto(
                    "Buddy", Specie.CAT, LocalDate.now(), null, null, null, null, null
            );
            given(entityFinderHelper.findOwnerOrFail(ownerId)).willReturn(owner);
            given(breedRepository.findByNameAndSpecie("Mixed/Other", Specie.CAT)).willReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> petService.registerPet(dtoWithoutBreedId, ownerId, null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Default breed configuration error for species CAT");

            then(entityFinderHelper).should().findOwnerOrFail(ownerId);
            then(entityFinderHelper).should(never()).findBreedOrFail(anyLong());
            then(breedRepository).should().findByNameAndSpecie("Mixed/Other", Specie.CAT);
            then(petRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("should register pet successfully using stored image when file is provided")
        void registerPet_Success_WithImageFile() throws IOException {
            // Arrange
            MockMultipartFile imageFile = new MockMultipartFile("imageFile", "test.jpg",
                    MediaType.IMAGE_JPEG_VALUE, "test image content".getBytes());
            String storedImagePathFromService = "pets/avatars/some-uuid.jpg";


            Pet petBeforeSave = Pet.builder()
                    .name(registrationDtoSpecificBreed.name())
                    .owner(owner)
                    .breed(dogBreedSpecific)
                    .image(storedImagePathFromService)
                    .status(PetStatus.PENDING)
                    .birthDate(registrationDtoSpecificBreed.birthDate())
                    .color(registrationDtoSpecificBreed.color())
                    .gender(registrationDtoSpecificBreed.gender())
                    .microchip(registrationDtoSpecificBreed.microchip())
                    .build();

            Pet petAfterSave = getAfterSave(petBeforeSave);

            PetProfileDto localExpectedPetProfileDto = new PetProfileDto(
                    savedPetId, registrationDtoSpecificBreed.name(), Specie.DOG, registrationDtoSpecificBreed.color(),
                    registrationDtoSpecificBreed.gender(), registrationDtoSpecificBreed.birthDate(), registrationDtoSpecificBreed.microchip(),
                    "http://localhost:8080/storage/" + storedImagePathFromService,
                    PetStatus.PENDING, ownerId, owner.getUsername(), specificBreedId, dogBreedSpecific.getName(),
                    null, null, Set.of(), ownerSummaryDto,
                    false, null, null
            );

            given(entityFinderHelper.findOwnerOrFail(ownerId)).willReturn(owner);
            given(entityFinderHelper.findBreedOrFail(specificBreedId)).willReturn(dogBreedSpecific);
            given(imageService.storeImage(imageFile, "pets/avatars")).willReturn(storedImagePathFromService);
            given(petRepository.existsByMicrochip(registrationDtoSpecificBreed.microchip())).willReturn(false);

            given(petRepository.save(any(Pet.class))).willReturn(petAfterSave);
            given(petMapper.toProfileDto(petAfterSave)).willReturn(localExpectedPetProfileDto);

            // Act
            PetProfileDto result = petService.registerPet(registrationDtoSpecificBreed, ownerId, imageFile);

            // Assert
            assertThat(result).isNotNull().isEqualTo(localExpectedPetProfileDto);
            assertThat(result.image()).isEqualTo("http://localhost:8080/storage/" + storedImagePathFromService);

            then(imageService).should().storeImage(imageFile, "pets/avatars");
            then(petRepository).should().save(petCaptor.capture());
            Pet captured = petCaptor.getValue();
            assertThat(captured.getImage()).isEqualTo(storedImagePathFromService);
            assertThat(captured.getName()).isEqualTo(registrationDtoSpecificBreed.name());
            then(petMapper).should().toProfileDto(petAfterSave);
        }

        private Pet getAfterSave(Pet petBeforeSave) {
            Pet petAfterSave = new Pet();
            petAfterSave.setId(savedPetId);
            petAfterSave.setName(petBeforeSave.getName());
            petAfterSave.setOwner(petBeforeSave.getOwner());
            petAfterSave.setBreed(petBeforeSave.getBreed());
            petAfterSave.setImage(petBeforeSave.getImage());
            petAfterSave.setStatus(petBeforeSave.getStatus());
            petAfterSave.setBirthDate(petBeforeSave.getBirthDate());
            petAfterSave.setColor(petBeforeSave.getColor());
            petAfterSave.setGender(petBeforeSave.getGender());
            petAfterSave.setMicrochip(petBeforeSave.getMicrochip());
            return petAfterSave;
        }

        @Test
        @DisplayName("should throw RuntimeException if image storage fails")
        void registerPet_Failure_ImageStorageError() throws IOException {
            // Arrange
            MockMultipartFile imageFile = new MockMultipartFile("imageFile", "bad.jpg", MediaType.IMAGE_JPEG_VALUE, "content".getBytes());
            given(entityFinderHelper.findOwnerOrFail(ownerId)).willReturn(owner);
            given(entityFinderHelper.findBreedOrFail(specificBreedId)).willReturn(dogBreedSpecific);
            given(imageService.storeImage(imageFile, "pets/avatars")).willThrow(new IOException("Disk full"));

            // Act & Assert
            assertThatThrownBy(() -> petService.registerPet(registrationDtoSpecificBreed, ownerId, imageFile))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to process uploaded image")
                    .hasCauseInstanceOf(IOException.class);

            then(imageService).should().storeImage(imageFile, "pets/avatars");
            then(petRepository).should(never()).save(any());
        }
    }

    /**
     * --- Tests for associatePetToClinicForActivation ---
     */
    @Nested
    @DisplayName("associatePetToClinicForActivation Tests")
    class AssociatePetToClinicTests {

        private Pet pendingPet;
        private Pet activePet; // For testing wrong status
        private Clinic targetClinic;
        private final Long clinicId = 5L;
        private final Long pendingPetId = 110L;
        private final Long activePetId = 111L;

        /**
         * Set up data specific to these tests.
         */
        @BeforeEach
        void associateSetup() {
            targetClinic = Clinic.builder().name("Target Clinic").build();
            targetClinic.setId(clinicId);

            pendingPet = new Pet();
            pendingPet.setId(pendingPetId);
            pendingPet.setName("Pending Pet");
            pendingPet.setOwner(owner);
            pendingPet.setStatus(PetStatus.PENDING);
            pendingPet.setBreed(dogBreedSpecific);
            pendingPet.setPendingActivationClinic(null);

            activePet = new Pet();
            activePet.setId(activePetId);
            activePet.setName("Active Pet");
            activePet.setOwner(owner);
            activePet.setStatus(PetStatus.ACTIVE);
            activePet.setBreed(dogBreedSpecific);
        }

        /**
         * Test successful association when the pet is PENDING and not already associated.
         * Verifies clinic lookup, pet update, and save call.
         */
        @Test
        @DisplayName("should associate pet successfully when conditions met")
        void associate_Success() {
            // Arrange
            given(entityFinderHelper.findPetByIdOrFail(pendingPetId)).willReturn(pendingPet);
            given(entityFinderHelper.findClinicOrFail(clinicId)).willReturn(targetClinic);
            given(petRepository.save(any(Pet.class))).willAnswer(inv -> inv.getArgument(0));

            // Act
            petService.associatePetToClinicForActivation(pendingPetId, clinicId, ownerId);

            // Assert
            then(entityFinderHelper).should().findPetByIdOrFail(pendingPetId);
            then(entityFinderHelper).should().findClinicOrFail(clinicId);
            then(petRepository).should().save(petCaptor.capture());

            Pet capturedAndSavedPet = petCaptor.getValue();
            assertThat(capturedAndSavedPet.getPendingActivationClinic()).isEqualTo(targetClinic);
        }

        /**
         * Test failure when the Pet ID is not found.
         */
        @Test
        @DisplayName("should throw EntityNotFoundException if pet not found")
        void associate_Failure_PetNotFound() {
            // Arrange
            given(entityFinderHelper.findPetByIdOrFail(999L))
                    .willThrow(new EntityNotFoundException(Pet.class.getSimpleName(), 999L));

            // Act & Assert
            assertThatThrownBy(() -> petService.associatePetToClinicForActivation(999L, clinicId, ownerId))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Pet not found with id: 999");

            then(entityFinderHelper).should(never()).findClinicOrFail(anyLong());
            then(petRepository).should(never()).save(any());
        }

        /**
         * Test failure when the user performing the action is not the owner of the pet.
         */
        @Test
        @DisplayName("should throw AccessDeniedException if user is not owner")
        void associate_Failure_NotOwner() {
            // Arrange
            Long otherOwnerId = 55L;
            given(entityFinderHelper.findPetByIdOrFail(pendingPetId)).willReturn(pendingPet);

            // Act & Assert
            assertThatThrownBy(() -> petService.associatePetToClinicForActivation(pendingPetId, clinicId, otherOwnerId))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("User " + otherOwnerId + " is not the owner of pet " + pendingPetId);

            then(entityFinderHelper).should(never()).findClinicOrFail(anyLong());
            then(petRepository).should(never()).save(any());
        }

        /**
         * Test failure when the pet is not in PENDING status (e.g., already ACTIVE).
         */
        @Test
        @DisplayName("should throw IllegalStateException if pet is not PENDING")
        void associate_Failure_WrongStatus() {
            // Arrange
            given(entityFinderHelper.findPetByIdOrFail(activePetId)).willReturn(activePet);

            // Act & Assert
            assertThatThrownBy(() -> petService.associatePetToClinicForActivation(activePetId, clinicId, ownerId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Pet " + activePetId + " must be in PENDING status to associate for activation, but was ACTIVE");

            then(entityFinderHelper).should(never()).findClinicOrFail(anyLong());
            then(petRepository).should(never()).save(any());
        }

        /**
         * Test failure when the pet is already pending activation at some clinic.
         */
        @Test
        @DisplayName("should throw IllegalStateException if pet already pending activation")
        void associate_Failure_AlreadyPending() {
            // Arrange
            Clinic existingPendingClinic = Clinic.builder().build();
            existingPendingClinic.setId(99L);
            pendingPet.setPendingActivationClinic(existingPendingClinic);

            given(entityFinderHelper.findPetByIdOrFail(pendingPetId)).willReturn(pendingPet);

            // Act & Assert
            assertThatThrownBy(() -> petService.associatePetToClinicForActivation(pendingPetId, clinicId, ownerId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Pet ID " + pendingPetId + " is already pending activation at clinic 99");

            then(entityFinderHelper).should(never()).findClinicOrFail(anyLong());
            then(petRepository).should(never()).save(any());
        }

        /**
         * Test failure when the target Clinic ID is not found.
         */
        @Test
        @DisplayName("should throw EntityNotFoundException if clinic not found")
        void associate_Failure_ClinicNotFound() {
            // Arrange
            Long nonExistentClinicId = 888L;
            assertThat(pendingPet.getOwner().getId()).isEqualTo(ownerId);
            given(entityFinderHelper.findPetByIdOrFail(pendingPetId)).willReturn(pendingPet);
            given(entityFinderHelper.findClinicOrFail(nonExistentClinicId))
                    .willThrow(new EntityNotFoundException("Target clinic not found with id: " + nonExistentClinicId));

            // Act & Assert
            assertThatThrownBy(() -> petService.associatePetToClinicForActivation(pendingPetId, nonExistentClinicId, ownerId))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("Target clinic not found with id: " + nonExistentClinicId);

            then(entityFinderHelper).should().findPetByIdOrFail(pendingPetId);
            then(entityFinderHelper).should().findClinicOrFail(nonExistentClinicId);
            then(petRepository).should(never()).save(any());
        }
    }

    /**
     * --- Tests for activatePet ---
     */
    @Nested
    @DisplayName("activatePet Tests")
    class ActivatePetTests {

        private Pet petToActivate;
        private Clinic pendingClinic;
        private Vet activatingVet;
        private PetProfileDto activatedPetDto;
        private PetActivationDto activationDto;
        private Breed petBreedFromDto;

        private final Long petToActivateId = 120L;
        private final Long activatingVetId = 20L;



        @BeforeEach
        void activateSetup() {
            Long clinicId = 1L;
            Long originalBreedId = 25L;
            Long breedIdFromDto = 30L;

            pendingClinic = Clinic.builder().name("Activation Clinic").build();
            pendingClinic.setId(clinicId);

            activatingVet = new Vet();
            activatingVet.setId(activatingVetId);
            activatingVet.setUsername("activator_vet");
            activatingVet.setName("Activator");
            activatingVet.setSurname("VetTest");
            activatingVet.setClinic(pendingClinic);
            activatingVet.setRoles(Set.of(RoleEntity.builder().roleEnum(RoleEnum.VET).build()));

            Breed petBreedOriginal = Breed.builder().id(originalBreedId).name("Original Poodle").specie(Specie.DOG).imageUrl("path/to/poodle.png").build();
            petBreedFromDto = Breed.builder().id(breedIdFromDto).name("Beagle").specie(Specie.DOG).imageUrl("path/to/beagle.png").build();

            petToActivate = new Pet();
            petToActivate.setId(petToActivateId);
            petToActivate.setName("Petty");
            petToActivate.setOwner(owner);
            petToActivate.setStatus(PetStatus.PENDING);
            petToActivate.setPendingActivationClinic(pendingClinic);
            petToActivate.setBreed(petBreedOriginal);
            petToActivate.setImage("path/to/original_image.png");
            petToActivate.setAssociatedVets(new HashSet<>());
            petToActivate.setLastEuEntryDate(null);
            petToActivate.setLastEuExitDate(null);


            // DTO para la activación
            activationDto = new PetActivationDto(
                    "TricolorFinalUpdated",
                    Gender.MALE,
                    LocalDate.of(2023, 6, 1),
                    "MICROCHIP123UPDATED",
                    breedIdFromDto
            );

            activatedPetDto = new PetProfileDto(
                    petToActivateId,
                    petToActivate.getName(),
                    petBreedFromDto.getSpecie(),
                    activationDto.color(),
                    activationDto.gender(),
                    activationDto.birthDate(),
                    activationDto.microchip(),
                    petToActivate.getImage(),
                    PetStatus.ACTIVE,
                    owner.getId(), owner.getUsername(),
                    activationDto.breedId(),
                    petBreedFromDto.getName(),
                    null,
                    null,
                    Set.of(new VetSummaryDto(
                            activatingVet.getId(),
                            activatingVet.getName(),
                            activatingVet.getSurname(),
                            null,
                            activatingVet.getEmail(),
                            activatingVet.getLicenseNumber(),
                            pendingClinic.getId(),
                            pendingClinic.getName(),
                            pendingClinic.getAddress(),
                            pendingClinic.getCity(),
                            pendingClinic.getCountry() != null ? pendingClinic.getCountry().name() : null,
                            pendingClinic.getPhone()
                    )),
                    ownerSummaryDto,
                    false,
                    petToActivate.getLastEuEntryDate(),
                    petToActivate.getLastEuExitDate()
            );
        }

        /**
         * Test successful activation by an authorized Vet when DTO is valid.
         */
        @Test
        @DisplayName("should activate pet successfully when called by authorized Vet with valid DTO")
        void activate_Success() {
            // Arrange
            given(entityFinderHelper.findClinicStaffOrFail(activatingVetId, "activate pet")).willReturn(activatingVet);
            given(entityFinderHelper.findPetByIdOrFail(petToActivateId)).willReturn(petToActivate);
            given(entityFinderHelper.findBreedOrFail(activationDto.breedId())).willReturn(petBreedFromDto);

            given(petRepository.existsByMicrochipAndIdNot(activationDto.microchip(), petToActivateId)).willReturn(false);
            given(petRepository.save(any(Pet.class))).willAnswer(inv -> inv.<Pet>getArgument(0));
            given(petMapper.toProfileDto(any(Pet.class))).willReturn(activatedPetDto);

            // Act
            PetProfileDto result = petService.activatePet(petToActivateId, activationDto, activatingVetId);

            // Assert
            assertThat(result).isNotNull().isEqualTo(activatedPetDto);

            then(entityFinderHelper).should().findClinicStaffOrFail(activatingVetId, "activate pet");
            then(entityFinderHelper).should().findPetByIdOrFail(petToActivateId);
            then(entityFinderHelper).should().findBreedOrFail(activationDto.breedId());
            then(petRepository).should().existsByMicrochipAndIdNot(activationDto.microchip(), petToActivateId);
            then(petRepository).should().save(petCaptor.capture());
            then(petMapper).should().toProfileDto(petCaptor.getValue());

            Pet saved = petCaptor.getValue();
            assertThat(saved.getStatus()).isEqualTo(PetStatus.ACTIVE);
            assertThat(saved.getPendingActivationClinic()).isNull();
            assertThat(saved.getAssociatedVets()).contains(activatingVet);
            assertThat(saved.getColor()).isEqualTo(activationDto.color());
            assertThat(saved.getGender()).isEqualTo(activationDto.gender());
            assertThat(saved.getBirthDate()).isEqualTo(activationDto.birthDate());
            assertThat(saved.getMicrochip()).isEqualTo(activationDto.microchip());
            assertThat(saved.getBreed().getId()).isEqualTo(activationDto.breedId());
        }

        /**
         * Test failure when the pet is not in PENDING status.
         * (Este test no necesita el DTO en el arrange, pero la llamada sí lo requiere)
         */
        @Test
        @DisplayName("should throw IllegalStateException if pet is not PENDING")
        void activate_Failure_NotPending() {
            // Arrange
            petToActivate.setStatus(PetStatus.ACTIVE);

            given(entityFinderHelper.findClinicStaffOrFail(activatingVetId, "activate pet")).willReturn(activatingVet);
            given(entityFinderHelper.findPetByIdOrFail(petToActivateId)).willReturn(petToActivate);

            // Act & Assert
            assertThatThrownBy(() -> petService.activatePet(petToActivateId, activationDto, activatingVetId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("must be in PENDING status to activate");

            then(petRepository).should(never()).save(any());
        }

        /**
         * Test failure when the staff member is not authorized (not from the pending clinic).
         */
        @Test
        @DisplayName("should throw AccessDeniedException if staff not from pending clinic")
        void activate_Failure_StaffWrongClinic() {
            // Arrange
            Clinic differentClinic = Clinic.builder().build(); differentClinic.setId(99L);
            activatingVet.setClinic(differentClinic);

            given(entityFinderHelper.findClinicStaffOrFail(activatingVetId, "activate pet")).willReturn(activatingVet);
            given(entityFinderHelper.findPetByIdOrFail(petToActivateId)).willReturn(petToActivate);

            // Act & Assert
            assertThatThrownBy(() -> petService.activatePet(petToActivateId, activationDto, activatingVetId))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("is not authorized to activate pet");

            then(petRepository).should(never()).save(any());
        }

        /**
         * Test failure when the provided microchip in DTO conflicts.
         */
        @Test
        @DisplayName("should throw MicrochipAlreadyExistsException if microchip in DTO conflicts")
        void activate_Failure_MicrochipConflict() {
            // Arrange
            given(entityFinderHelper.findClinicStaffOrFail(activatingVetId, "activate pet")).willReturn(activatingVet);
            given(entityFinderHelper.findPetByIdOrFail(petToActivateId)).willReturn(petToActivate);
            given(petRepository.existsByMicrochipAndIdNot(activationDto.microchip(), petToActivateId)).willReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> petService.activatePet(petToActivateId, activationDto, activatingVetId))
                    .isInstanceOf(MicrochipAlreadyExistsException.class)
                    .hasMessageContaining(activationDto.microchip());

            then(entityFinderHelper).should(never()).findBreedOrFail(anyLong());
            then(petRepository).should(never()).save(any());
        }

        /**
         * Test failure when the breedId provided in DTO is invalid.
         */
        @Test
        @DisplayName("should throw EntityNotFoundException if breedId in DTO not found")
        void activate_Failure_BreedNotFoundInDto() {
            // Arrange
            Long nonExistentBreedId = 999L;
            PetActivationDto dtoWithBadBreed = new PetActivationDto(
                    "SomeColor", Gender.FEMALE, LocalDate.now(), "MicrochipOKForThisTest",
                    nonExistentBreedId
            );
            given(entityFinderHelper.findClinicStaffOrFail(activatingVetId, "activate pet")).willReturn(activatingVet);
            given(entityFinderHelper.findPetByIdOrFail(petToActivateId)).willReturn(petToActivate);
            given(petRepository.existsByMicrochipAndIdNot(dtoWithBadBreed.microchip(), petToActivateId)).willReturn(false);
            given(entityFinderHelper.findBreedOrFail(nonExistentBreedId))
                    .willThrow(new EntityNotFoundException(Breed.class.getSimpleName(), nonExistentBreedId));

            // Act & Assert
            assertThatThrownBy(() -> petService.activatePet(petToActivateId, dtoWithBadBreed, activatingVetId))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Breed not found with id: " + nonExistentBreedId);

            then(petRepository).should(never()).save(any());
        }


        @Test
        @DisplayName("should throw EntityNotFoundException if activating staff not found")
        void activate_Failure_StaffNotFound() {
            // Arrange
            given(entityFinderHelper.findClinicStaffOrFail(999L, "activate pet"))
                    .willThrow(new EntityNotFoundException(UserEntity.class.getSimpleName(), 999L));

            // Act & Assert
            assertThatThrownBy(() -> petService.activatePet(petToActivateId, activationDto, 999L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("UserEntity not found with id: 999");

            then(entityFinderHelper).should(never()).findPetByIdOrFail(anyLong());
            then(petRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("should throw AccessDeniedException if activator is Admin (defensive check)")
        void activate_Failure_ActivatorIsAdmin() {
            // Arrange
            ClinicStaff activatingAdmin = new ClinicStaff();
            activatingAdmin.setId(activatingVetId);
            activatingAdmin.setClinic(pendingClinic);
            activatingAdmin.setRoles(Set.of(RoleEntity.builder().roleEnum(RoleEnum.ADMIN).build()));
            given(entityFinderHelper.findClinicStaffOrFail(activatingVetId, "activate pet")).willReturn(activatingAdmin);
            given(entityFinderHelper.findPetByIdOrFail(petToActivateId)).willReturn(petToActivate);
            given(entityFinderHelper.findBreedOrFail(activationDto.breedId())).willReturn(petBreedFromDto);
            given(petRepository.existsByMicrochipAndIdNot(activationDto.microchip(), petToActivateId)).willReturn(false);


            // Act & Assert
            assertThatThrownBy(() -> petService.activatePet(petToActivateId, activationDto, activatingVetId))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("user performing the activation is not a veterinarian");

            then(entityFinderHelper).should().findClinicStaffOrFail(activatingVetId, "activate pet");
            then(entityFinderHelper).should().findPetByIdOrFail(petToActivateId);
            then(entityFinderHelper).should().findBreedOrFail(activationDto.breedId());
            then(petRepository).should().existsByMicrochipAndIdNot(activationDto.microchip(), petToActivateId);
            then(petRepository).should(never()).save(any());
        }

    }

    /**
     * --- Tests for deactivatePet ---
     */
    @Nested
    @DisplayName("deactivatePet Tests")
    class DeactivatePetTests {

        private Pet petToDeactivate;
        private Pet alreadyInactivePet;
        private final Long petToDeactivateId = 400L;
        private final Long inactivePetId = 401L;
        private final Long ownerId = 1L;

        @BeforeEach
        void deactivateSetup() {
            petToDeactivate = new Pet();
            petToDeactivate.setId(petToDeactivateId);
            petToDeactivate.setOwner(owner);
            petToDeactivate.setStatus(PetStatus.ACTIVE);
            petToDeactivate.setBreed(dogBreedSpecific);

            alreadyInactivePet = new Pet();
            alreadyInactivePet.setId(inactivePetId);
            alreadyInactivePet.setOwner(owner);
            alreadyInactivePet.setStatus(PetStatus.INACTIVE);
            alreadyInactivePet.setBreed(dogBreedSpecific);
        }

        /**
         * Test successful deactivation when called by the owner on an active pet.
         */
        @Test
        @DisplayName("should deactivate pet successfully when called by owner")
        void deactivate_Success() {
            // Arrange
            given(entityFinderHelper.findPetByIdOrFail(petToDeactivateId)).willReturn(petToDeactivate);
            given(petRepository.save(any(Pet.class))).willAnswer(inv -> inv.getArgument(0));
            PetProfileDto inactiveDto = new PetProfileDto(petToDeactivateId,
                    petToDeactivate.getName(), petToDeactivate.getBreed().getSpecie(),
                    null,null,null,null,null,
                    PetStatus.INACTIVE, ownerId, owner.getUsername(), dogBreedSpecific.getId(),
                    dogBreedSpecific.getName(), null, null,
                    null, null, false,null,null) ;
            given(petMapper.toProfileDto(any(Pet.class))).willReturn(inactiveDto);

            // Act
            PetProfileDto result = petService.deactivatePet(petToDeactivateId, ownerId);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.status()).isEqualTo(PetStatus.INACTIVE);

            then(entityFinderHelper).should().findPetByIdOrFail(petToDeactivateId);
            then(petRepository).should().save(petCaptor.capture());
            then(petMapper).should().toProfileDto(any(Pet.class));

            Pet saved = petCaptor.getValue();
            assertThat(saved.getStatus()).isEqualTo(PetStatus.INACTIVE);
        }

        /**
         * Test failure when attempting to deactivate a pet that is already inactive.
         */
        @Test
        @DisplayName("should throw IllegalStateException if pet already inactive")
        void deactivate_Failure_AlreadyInactive() {
            // Arrange
            PetProfileDto expectedInactiveProfileDto = new PetProfileDto(
                    inactivePetId,
                    alreadyInactivePet.getName(),
                    alreadyInactivePet.getBreed().getSpecie(),
                    alreadyInactivePet.getColor(),
                    alreadyInactivePet.getGender(),
                    alreadyInactivePet.getBirthDate(),
                    alreadyInactivePet.getMicrochip(),
                    alreadyInactivePet.getImage(),
                    PetStatus.INACTIVE, // Estado esperado
                    owner.getId(),
                    owner.getUsername(),
                    alreadyInactivePet.getBreed().getId(),
                    alreadyInactivePet.getBreed().getName(),
                    null, null,
                    Collections.emptySet(),
                    ownerSummaryDto,
                    false,
                    null, null
            );
            given(entityFinderHelper.findPetByIdOrFail(inactivePetId)).willReturn(alreadyInactivePet);

            // Act & Assert
            given(petMapper.toProfileDto(alreadyInactivePet)).willReturn(expectedInactiveProfileDto);


            // Act
            PetProfileDto resultDto = petService.deactivatePet(inactivePetId, ownerId);

            // Assert
            assertThat(resultDto).isNotNull();
            assertThat(resultDto.id()).isEqualTo(inactivePetId);
            assertThat(resultDto.status()).isEqualTo(PetStatus.INACTIVE);
            assertThat(resultDto).isEqualTo(expectedInactiveProfileDto);

            then(entityFinderHelper).should().findPetByIdOrFail(inactivePetId);
            // Verifica que petRepository.save() NO fue llamado
            then(petRepository).should(never()).save(any(Pet.class));
            then(petMapper).should().toProfileDto(alreadyInactivePet);
        }

        /**
         * Test failure when the pet ID does not exist.
         */
        @Test
        @DisplayName("should throw EntityNotFoundException if pet not found")
        void deactivate_Failure_PetNotFound() {
            // Arrange
            given(entityFinderHelper.findPetByIdOrFail(999L))
                    .willThrow(new EntityNotFoundException(Pet.class.getSimpleName(), 999L));

            // Act & Assert
            assertThatThrownBy(() -> petService.deactivatePet(999L, ownerId))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Pet not found with id: 999");

            then(petRepository).should(never()).save(any());
        }

        /**
         * Test failure when the user attempting deactivation is not the owner.
         */
        @Test
        @DisplayName("should throw AccessDeniedException if user is not owner")
        void deactivate_Failure_NotOwner() {
            // Arrange
            Long otherOwnerId = 888L;
            given(entityFinderHelper.findPetByIdOrFail(petToDeactivateId)).willReturn(petToDeactivate);

            // Act & Assert
            assertThatThrownBy(() -> petService.deactivatePet(petToDeactivateId, otherOwnerId))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("User " + otherOwnerId + " is not the owner of pet " + petToDeactivateId);

            then(petRepository).should(never()).save(any());
        }
    }

    /**
     * --- Tests for updatePetByOwner ---
     */
    @Nested
    @DisplayName("updatePetByOwner Tests")
    class UpdatePetByOwnerTests {

        private Pet existingPet;
        private PetOwnerUpdateDto updateDto;
        private Breed newBreed;
        private PetProfileDto expectedUpdatedDto;
        private final Long petId = 130L;
        private final Long newBreedId = 40L;

        /**
         * Setup data: An existing pet owned by the test owner.
         */
        @BeforeEach
        void updateOwnerSetup() {
            newBreed = Breed.builder().id(newBreedId).name("Poodle").specie(Specie.DOG).build();

            existingPet = new Pet();
            existingPet.setId(petId);
            existingPet.setOwner(owner);
            existingPet.setName("BuddyOriginal");
            existingPet.setColor("Brown");
            existingPet.setBreed(dogBreedSpecific);
            existingPet.setMicrochip("11111");
            existingPet.setImage("original.jpg");
            existingPet.setStatus(PetStatus.ACTIVE);

            // DTO with some changes
            updateDto = new PetOwnerUpdateDto(
                    "BuddyUpdated",
                    null,
                    "Black and White",
                    Gender.MALE,
                    existingPet.getBirthDate(),
                    "22222",
                    newBreedId,
                    null,
                    null
            );

            expectedUpdatedDto = new PetProfileDto(
                    petId,
                    updateDto.name(),
                    newBreed.getSpecie(),
                    updateDto.color(),
                    updateDto.gender(),
                    updateDto.birthDate() !=null ? updateDto.birthDate() : existingPet.getBirthDate(),
                    updateDto.microchip(),
                    existingPet.getImage(),
                    existingPet.getStatus(),
                    owner.getId(),
                    owner.getUsername(),
                    newBreedId,
                    newBreed.getName(),
                    existingPet.getPendingActivationClinic() != null ? existingPet.getPendingActivationClinic().getId() : null,
                    existingPet.getPendingActivationClinic() != null ? existingPet.getPendingActivationClinic().getName() : null,
                    existingPet.getAssociatedVets() != null ? userMapper.toVetSummaryDtoSet(existingPet.getAssociatedVets()) : Set.of(),
                    ownerSummaryDto,
                    false,
                    updateDto.newEuEntryDate() != null ? updateDto.newEuEntryDate() : existingPet.getLastEuEntryDate(),
                    updateDto.newEuExitDate() != null ? updateDto.newEuExitDate() : existingPet.getLastEuExitDate());
        }

        /**
         * Test a successful update when called by the correct owner.
         * Verifies pet lookup, breed resolution (if changed), microchip validation,
         * mapper application, saving, and DTO mapping.
         */
        @Test
        @DisplayName("should update pet successfully when called by owner")
        void updateByOwner_Success() {
            // Arrange
            given(entityFinderHelper.findPetByIdOrFail(petId)).willReturn(existingPet);
            given(entityFinderHelper.findBreedOrFail(newBreedId)).willReturn(newBreed);
            given(petRepository.existsByMicrochipAndIdNot(updateDto.microchip(), petId)).willReturn(false);
            given(petMapper.updateFromOwnerDto(updateDto, existingPet, newBreed)).willReturn(true);
            given(petRepository.save(any(Pet.class))).willAnswer(inv -> inv.getArgument(0));
            given(petMapper.toProfileDto(any(Pet.class))).willReturn(expectedUpdatedDto);

            // Act
            PetProfileDto result = petService.updatePetByOwner(petId, updateDto, ownerId, null);

            // Assert
            assertThat(result).isNotNull().isEqualTo(expectedUpdatedDto);

            then(entityFinderHelper).should().findPetByIdOrFail(petId);
            then(entityFinderHelper).should().findBreedOrFail(newBreedId);
            then(petRepository).should().existsByMicrochipAndIdNot(updateDto.microchip(), petId);
            then(petMapper).should().updateFromOwnerDto(updateDto, existingPet, newBreed);
            then(petRepository).should().save(petCaptor.capture());
            then(petMapper).should().toProfileDto(any(Pet.class));

            Pet captured = petCaptor.getValue();
            assertThat(captured.getId()).isEqualTo(petId);
        }

        /**
         * Test successful update where no actual changes are made according to the mapper.
         * Verifies that the repository save method is NOT called.
         */
        @Test
        @DisplayName("should not save pet if mapper reports no changes")
        void updateByOwner_NoChanges_ShouldNotSave() {
            // Arrange
            PetOwnerUpdateDto noChangeDto = new PetOwnerUpdateDto(
                    existingPet.getName(),
                    null,
                    existingPet.getColor(),
                    existingPet.getGender(),
                    existingPet.getBirthDate(),
                    existingPet.getMicrochip(),
                    existingPet.getBreed().getId(),
                    existingPet.getLastEuEntryDate(),
                    existingPet.getLastEuExitDate()
            );
            given(entityFinderHelper.findPetByIdOrFail(petId)).willReturn(existingPet);
            given(petMapper.updateFromOwnerDto(noChangeDto, existingPet, existingPet.getBreed())).willReturn(false);
            PetProfileDto originalDto = new PetProfileDto(
                    existingPet.getId(),
                    existingPet.getName(),
                    existingPet.getBreed().getSpecie(),
                    existingPet.getColor(),
                    existingPet.getGender(),
                    existingPet.getBirthDate(),
                    existingPet.getMicrochip(),
                    existingPet.getImage(),
                    existingPet.getStatus(),
                    owner.getId(),
                    owner.getUsername(),
                    existingPet.getBreed().getId(),
                    existingPet.getBreed().getName(),
                    existingPet.getPendingActivationClinic() != null ? existingPet.getPendingActivationClinic().getId() : null,
                    existingPet.getPendingActivationClinic() != null ? existingPet.getPendingActivationClinic().getName() : null,
                    existingPet.getAssociatedVets() != null ? userMapper.toVetSummaryDtoSet(existingPet.getAssociatedVets()) : Set.of(),
                    ownerSummaryDto,
                    false,
                    existingPet.getLastEuEntryDate(),
                    existingPet.getLastEuExitDate()
            );

            given(petMapper.toProfileDto(existingPet)).willReturn(originalDto);

            // Act
            PetProfileDto result = petService.updatePetByOwner(petId, noChangeDto, ownerId, null);

            // Assert
            assertThat(result).isNotNull().isEqualTo(originalDto);
            then(entityFinderHelper).should().findPetByIdOrFail(petId);
            then(entityFinderHelper).should(never()).findBreedOrFail(anyLong());
            then(petRepository).should(never()).existsByMicrochipAndIdNot(anyString(), anyLong());
            then(petMapper).should().updateFromOwnerDto(noChangeDto, existingPet, existingPet.getBreed());
            then(petRepository).should(never()).save(any(Pet.class));
            then(petMapper).should().toProfileDto(existingPet);
        }

        /**
         * Test failure when the pet ID does not exist.
         */
        @Test
        @DisplayName("should throw EntityNotFoundException if pet not found")
        void updateByOwner_Failure_PetNotFound() {
            // Arrange
            given(entityFinderHelper.findPetByIdOrFail(999L))
                    .willThrow(new EntityNotFoundException(Pet.class.getSimpleName(), 999L));

            // Act & Assert
            assertThatThrownBy(() -> petService.updatePetByOwner(999L, updateDto, ownerId, null))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Pet not found with id: 999");

            then(petRepository).should(never()).save(any());
            then(petMapper).should(never()).updateFromOwnerDto(any(), any(), any());
        }

        /**
         * Test failure when the user trying to update is not the owner.
         */
        @Test
        @DisplayName("should throw AccessDeniedException if user is not owner")
        void updateByOwner_Failure_NotOwner() {
            // Arrange
            Long otherOwnerId = 55L;
            given(entityFinderHelper.findPetByIdOrFail(petId)).willReturn(existingPet);

            // Act & Assert
            assertThatThrownBy(() -> petService.updatePetByOwner(petId, updateDto, otherOwnerId, null))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("User " + otherOwnerId + " is not the owner of pet " + petId);

            then(entityFinderHelper).should().findPetByIdOrFail(petId);
            then(petRepository).should(never()).save(any());
            then(petMapper).should(never()).updateFromOwnerDto(any(), any(), any());
        }

        /**
         * Test failure when a new breed ID is provided, but the breed does not exist.
         */
        @Test
        @DisplayName("should throw EntityNotFoundException if new breedId not found")
        void updateByOwner_Failure_NewBreedNotFound() {
            // Arrange
            Long nonExistentBreedId = 888L;
            PetOwnerUpdateDto dtoWithBadBreed = new PetOwnerUpdateDto(
                    existingPet.getName(),
                    null,
                    existingPet.getColor(),
                    existingPet.getGender(),
                    existingPet.getBirthDate(),
                    existingPet.getMicrochip(),
                    nonExistentBreedId,
                    null,
                    null
            );
            given(entityFinderHelper.findPetByIdOrFail(petId)).willReturn(existingPet);
            given(entityFinderHelper.findBreedOrFail(nonExistentBreedId))
                    .willThrow(new EntityNotFoundException(Breed.class.getSimpleName(), nonExistentBreedId));

            // Act & Assert
            assertThatThrownBy(() -> petService.updatePetByOwner(petId, dtoWithBadBreed, ownerId,null))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Breed not found with id: " + nonExistentBreedId);

            then(entityFinderHelper).should().findPetByIdOrFail(petId);
            then(entityFinderHelper).should().findBreedOrFail(nonExistentBreedId);
            then(petRepository).should(never()).save(any());
            then(petMapper).should(never()).updateFromOwnerDto(any(), any(), any());
        }

        /**
         * Test failure when the new microchip number already exists for another pet.
         */
        @Test
        @DisplayName("should throw MicrochipAlreadyExistsException if new microchip conflicts")
        void updateByOwner_Failure_MicrochipConflict() {
            // Arrange
            given(entityFinderHelper.findPetByIdOrFail(petId)).willReturn(existingPet);
            given(entityFinderHelper.findBreedOrFail(newBreedId)).willReturn(newBreed);
            given(petRepository.existsByMicrochipAndIdNot(updateDto.microchip(), petId)).willReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> petService.updatePetByOwner(petId, updateDto, ownerId,null))
                    .isInstanceOf(MicrochipAlreadyExistsException.class)
                    .hasMessageContaining(updateDto.microchip());

            then(entityFinderHelper).should().findPetByIdOrFail(petId);
            then(entityFinderHelper).should().findBreedOrFail(newBreedId);
            then(petRepository).should().existsByMicrochipAndIdNot(updateDto.microchip(), petId);
            then(petRepository).should(never()).save(any());
            then(petMapper).should(never()).updateFromOwnerDto(any(), any(), any());
        }

        @Test
        @DisplayName("should update image and delete old one when image file is provided")
        void updateByOwner_Success_WithImageChange() throws IOException {
            // Arrange
            MockMultipartFile imageFile = new MockMultipartFile("imageFile", "new.png", MediaType.IMAGE_PNG_VALUE, "new image".getBytes());
            String oldImagePath = existingPet.getImage();
            String newImagePath = "pets/avatars/new-uuid.png";

            given(entityFinderHelper.findPetByIdOrFail(petId)).willReturn(existingPet);
            PetOwnerUpdateDto noOtherChangesDto = new PetOwnerUpdateDto(
                    existingPet.getName(),
                    null,
                    existingPet.getColor(),
                    existingPet.getGender(),
                    existingPet.getBirthDate(),
                    existingPet.getMicrochip(),
                    existingPet.getBreed().getId(),
                    existingPet.getLastEuEntryDate(),
                    existingPet.getLastEuExitDate()
            );

            when(petMapper.updateFromOwnerDto(noOtherChangesDto, existingPet, existingPet.getBreed())).thenReturn(false);

            given(imageService.storeImage(imageFile, "pets/avatars")).willReturn(newImagePath);
            doNothing().when(imageService).deleteImage(oldImagePath);
            when(petRepository.save(any(Pet.class))).thenAnswer(inv -> {
                Pet petToSave = inv.getArgument(0);
                petToSave.setImage(newImagePath);
                return petToSave;
            });

            expectedUpdatedDto = new PetProfileDto(
                    existingPet.getId(), existingPet.getName(), existingPet.getBreed().getSpecie(),
                    existingPet.getColor(), existingPet.getGender(), existingPet.getBirthDate(),
                    existingPet.getMicrochip(), newImagePath,
                    existingPet.getStatus(), owner.getId(), owner.getUsername(),
                    existingPet.getBreed().getId(), existingPet.getBreed().getName(),
                    existingPet.getPendingActivationClinic() != null ? existingPet.getPendingActivationClinic().getId() : null,
                    existingPet.getPendingActivationClinic() != null ? existingPet.getPendingActivationClinic().getName() : null,
                    existingPet.getAssociatedVets() != null ? userMapper.toVetSummaryDtoSet(existingPet.getAssociatedVets()) : Set.of(),
                    ownerSummaryDto,
                    false,
                    existingPet.getLastEuEntryDate(),
                    existingPet.getLastEuExitDate()
            );
            when(petMapper.toProfileDto(any(Pet.class))).thenReturn(expectedUpdatedDto);


            // Act
            PetProfileDto result = petService.updatePetByOwner(petId, noOtherChangesDto, ownerId, imageFile);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.image()).isEqualTo(newImagePath);

            then(imageService).should().storeImage(imageFile, "pets/avatars");
            then(imageService).should().deleteImage(oldImagePath);
            then(petRepository).should().save(petCaptor.capture());
            assertThat(petCaptor.getValue().getImage()).isEqualTo(newImagePath);
            then(petMapper).should().toProfileDto(any(Pet.class));
        }

        @Test
        @DisplayName("should throw RuntimeException if image update storage fails")
        void updateByOwner_Failure_ImageStorageError() throws IOException {
            // Arrange
            MockMultipartFile imageFile = new MockMultipartFile("imageFile", "fail.jpg", MediaType.IMAGE_JPEG_VALUE, "content".getBytes());
            given(entityFinderHelper.findPetByIdOrFail(petId)).willReturn(existingPet);
            given(imageService.storeImage(imageFile, "pets/avatars")).willThrow(new IOException("Disk full"));

            // Act & Assert
            assertThatThrownBy(() -> petService.updatePetByOwner(petId, updateDto, ownerId, imageFile))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to process updated image")
                    .hasCauseInstanceOf(IOException.class);

            then(imageService).should().storeImage(imageFile, "pets/avatars");
            then(imageService).should(never()).deleteImage(anyString());
            then(petRepository).should(never()).save(any());
        }

    }

    /**
     * --- Tests for updatePetByClinicStaff ---
     */
    @Nested
    @DisplayName("updatePetByClinicStaff Tests")
    class UpdatePetByClinicStaffTests {

        private Pet petToUpdate;
        private ClinicStaff authorizedStaff;
        private Owner petOwner;
        private Breed newBreed;
        private PetClinicUpdateDto updateDto;
        private PetProfileDto expectedUpdatedDto;
        private final Long petId = 140L;
        private final Long authorizedStaffId = 30L;
        private final Long unauthorizedStaffId = 31L;
        private final Long newBreedId = 45L;
        private final String actionContext = "update clinical info for";

        /**
         * Setup data: An existing pet, an authorized staff member, and an unauthorized one.
         */
        @BeforeEach
        void updateClinicStaffSetup() {
            Clinic petClinic = Clinic.builder().name("Pet's Clinic").address("123 Main St").city("Anytown").country(Country.UNITED_KINGDOM).phone("555-1234").build();
            petClinic.setId(1L);
            petOwner = new Owner();
            petOwner.setId(5L);
            petOwner.setUsername("pet_owner_for_staff_test");
            petOwner.setEmail("owner_staff@test.com");
            petOwner.setPhone("111222333");

            // Inicializa authorizedStaff (como Vet)
            authorizedStaff = new Vet();
            authorizedStaff.setId(authorizedStaffId);
            authorizedStaff.setUsername("auth_staff_vet");
            authorizedStaff.setName("Auth");
            authorizedStaff.setSurname("Vet");
            authorizedStaff.setEmail("auth_vet@clinic.com");
            authorizedStaff.setAvatar("avatars/vet_auth.png");
            ((Vet) authorizedStaff).setLicenseNumber("VET123");
            authorizedStaff.setClinic(petClinic);
            authorizedStaff.setRoles(Set.of(RoleEntity.builder().roleEnum(RoleEnum.VET).build()));

            Breed originalBreed = Breed.builder().id(20L).name("Dalmatian").specie(Specie.DOG).imageUrl("dalmatian.png").build();
            newBreed = Breed.builder().id(newBreedId).name("Boxer").specie(Specie.DOG).imageUrl("boxer.png").build();

            petToUpdate = new Pet();
            petToUpdate.setId(petId);
            petToUpdate.setOwner(petOwner);
            petToUpdate.setName("Spot");
            petToUpdate.setBreed(originalBreed);
            petToUpdate.setStatus(PetStatus.ACTIVE);
            petToUpdate.setMicrochip("CHIP-ORIGINAL");
            petToUpdate.setColor("White/Black");
            petToUpdate.setImage("spot_original.png");
            petToUpdate.setLastEuEntryDate(null);
            petToUpdate.setLastEuExitDate(null);
            petToUpdate.setPendingActivationClinic(null);
            petToUpdate.setPendingCertificateClinic(null);
            petToUpdate.setAssociatedVets(new HashSet<>());
            petToUpdate.addVet((Vet) authorizedStaff);

            updateDto = new PetClinicUpdateDto(
                    "White/Brown",
                    Gender.FEMALE,
                    LocalDate.of(2022, 10, 10),
                    "CHIP-UPDATED",
                    newBreedId
            );

            OwnerSummaryDto ownerSummaryForProfile = new OwnerSummaryDto(petOwner.getId(),
                    petOwner.getUsername(), petOwner.getEmail(), petOwner.getPhone());

            VetSummaryDto vetSummaryForProfile = getVetSummaryDto(petClinic);

            expectedUpdatedDto = new PetProfileDto(
                    petId,
                    petToUpdate.getName(),
                    newBreed.getSpecie(),
                    updateDto.color(),
                    updateDto.gender(),
                    updateDto.birthDate(),
                    updateDto.microchip(),
                    petToUpdate.getImage(),
                    petToUpdate.getStatus(),
                    petOwner.getId(),
                    petOwner.getUsername(),
                    newBreedId,
                    newBreed.getName(),
                    null,
                    null,
                    Set.of(vetSummaryForProfile),
                    ownerSummaryForProfile,
                    false,
                    petToUpdate.getLastEuEntryDate(),
                    petToUpdate.getLastEuExitDate()
            );
        }

        private VetSummaryDto getVetSummaryDto(Clinic petClinic) {
            Vet castedAuthorizedStaff = (Vet) authorizedStaff;
            return new VetSummaryDto(
                    castedAuthorizedStaff.getId(),
                    castedAuthorizedStaff.getName(),
                    castedAuthorizedStaff.getSurname(),
                    castedAuthorizedStaff.getAvatar(),
                    castedAuthorizedStaff.getEmail(),
                    castedAuthorizedStaff.getLicenseNumber(),
                    petClinic.getId(),
                    petClinic.getName(),
                    petClinic.getAddress(),
                    petClinic.getCity(),
                    petClinic.getCountry() != null ? petClinic.getCountry().name() : null,
                    petClinic.getPhone()
            );
        }

        /**
         * Test successful update when called by authorized clinic staff (Vet/Admin associated with the pet).
         */
        @Test
        @DisplayName("should update pet successfully when called by authorized staff")
        void updateByStaff_Success() {
            // Arrange
            given(entityFinderHelper.findPetByIdOrFail(petId)).willReturn(petToUpdate);
            doNothing().when(authorizationHelper).verifyUserAuthorizationForPet(authorizedStaffId, petToUpdate, actionContext);
            given(entityFinderHelper.findBreedOrFail(newBreedId)).willReturn(newBreed);
            given(petRepository.existsByMicrochipAndIdNot(updateDto.microchip(), petId)).willReturn(false);

            when(petMapper.updateFromClinicDto(eq(updateDto), any(Pet.class), eq(newBreed)))
                    .thenAnswer(invocation -> {
                        Pet petArg = invocation.getArgument(1);
                        petArg.setColor(updateDto.color());
                        petArg.setGender(updateDto.gender());
                        petArg.setBirthDate(updateDto.birthDate());
                        petArg.setMicrochip(updateDto.microchip());
                        petArg.setBreed(newBreed);
                        return true;
                    });

            when(petRepository.save(any(Pet.class))).thenAnswer(invocation -> invocation.getArgument(0));

            given(petMapper.toProfileDto(argThat(p -> p.getId().equals(petId) && p.getColor().equals(updateDto.color()) )))
                    .willReturn(expectedUpdatedDto);

            PetProfileDto result = petService.updatePetByClinicStaff(petId, updateDto, authorizedStaffId);

            assertThat(result).isNotNull().isEqualTo(expectedUpdatedDto);

            then(entityFinderHelper).should().findPetByIdOrFail(petId);
            then(authorizationHelper).should().verifyUserAuthorizationForPet(authorizedStaffId, petToUpdate, actionContext);
            then(entityFinderHelper).should().findBreedOrFail(newBreedId);
            then(petRepository).should().existsByMicrochipAndIdNot(updateDto.microchip(), petId);
            then(petMapper).should().updateFromClinicDto(eq(updateDto), any(Pet.class), eq(newBreed));
            then(petRepository).should().save(petCaptor.capture());
            then(petMapper).should().toProfileDto(petCaptor.getValue());

            Pet captured = petCaptor.getValue();
            assertThat(captured.getColor()).isEqualTo(updateDto.color());
            assertThat(captured.getGender()).isEqualTo(updateDto.gender());
            assertThat(captured.getBirthDate()).isEqualTo(updateDto.birthDate());
            assertThat(captured.getMicrochip()).isEqualTo(updateDto.microchip());
            assertThat(captured.getBreed().getId()).isEqualTo(newBreedId);
        }

        /**
         * Test successful update where no actual changes are made according to the mapper.
         * Verifies that the repository save method is NOT called.
         */
        @Test
        @DisplayName("should not save pet if mapper reports no changes")
        void updateByStaff_NoChanges_ShouldNotSave() {
            // Arrange
            PetClinicUpdateDto noChangeDto = new PetClinicUpdateDto(
                    petToUpdate.getColor(),
                    petToUpdate.getGender(),
                    petToUpdate.getBirthDate(),
                    petToUpdate.getMicrochip(),
                    petToUpdate.getBreed().getId()
            );

            given(entityFinderHelper.findPetByIdOrFail(petId)).willReturn(petToUpdate);
            doNothing().when(authorizationHelper).verifyUserAuthorizationForPet(authorizedStaffId, petToUpdate, actionContext);
            given(petMapper.updateFromClinicDto(noChangeDto, petToUpdate, petToUpdate.getBreed())).willReturn(false);

            Vet castedAuthorizedStaff = (Vet) authorizedStaff;
            PetProfileDto originalDto = getPetProfileDto(castedAuthorizedStaff);
            given(petMapper.toProfileDto(petToUpdate)).willReturn(originalDto);

            // Act
            PetProfileDto result = petService.updatePetByClinicStaff(petId, noChangeDto, authorizedStaffId);

            // Assert
            assertThat(result).isNotNull().isEqualTo(originalDto);
            then(petRepository).should(never()).save(any(Pet.class));
            then(petMapper).should().toProfileDto(petToUpdate);
        }

        private PetProfileDto getPetProfileDto(Vet castedAuthorizedStaff) {
            VetSummaryDto vetSummaryForProfile = new VetSummaryDto(
                    castedAuthorizedStaff.getId(),
                    castedAuthorizedStaff.getName(),
                    castedAuthorizedStaff.getSurname(),
                    castedAuthorizedStaff.getAvatar(),
                    castedAuthorizedStaff.getEmail(),
                    castedAuthorizedStaff.getLicenseNumber(),
                    castedAuthorizedStaff.getClinic().getId(),
                    castedAuthorizedStaff.getClinic().getName(),
                    castedAuthorizedStaff.getClinic().getAddress(),
                    castedAuthorizedStaff.getClinic().getCity(),
                    castedAuthorizedStaff.getClinic().getCountry().name(),
                    castedAuthorizedStaff.getClinic().getPhone()
            );
            OwnerSummaryDto ownerSummaryForProfile = new OwnerSummaryDto(petOwner.getId(),
                    petOwner.getUsername(), petOwner.getEmail(), petOwner.getPhone());

            return new PetProfileDto(
                    petToUpdate.getId(), petToUpdate.getName(), petToUpdate.getBreed().getSpecie(),
                    petToUpdate.getColor(), petToUpdate.getGender(), petToUpdate.getBirthDate(),
                    petToUpdate.getMicrochip(), petToUpdate.getImage(), petToUpdate.getStatus(),
                    petOwner.getId(), petOwner.getUsername(), petToUpdate.getBreed().getId(),
                    petToUpdate.getBreed().getName(),
                    null, null,
                    Set.of(vetSummaryForProfile),
                    ownerSummaryForProfile,
                    false,
                    null, null
            );
        }

        /**
         * Test failure when the pet ID does not exist.
         */
        @Test
        @DisplayName("should throw EntityNotFoundException if pet not found")
        void updateByStaff_Failure_PetNotFound() {
            // Arrange
            given(entityFinderHelper.findPetByIdOrFail(999L))
                    .willThrow(new EntityNotFoundException(Pet.class.getSimpleName(), 999L));

            // Act & Assert
            assertThatThrownBy(() -> petService.updatePetByClinicStaff(999L, updateDto, authorizedStaffId))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Pet not found with id: 999");

            then(entityFinderHelper).should(never()).findUserOrFail(anyLong());
            then(petRepository).should(never()).save(any());
        }

        /**
         * Test failure when the staff member performing the update is not found.
         */
        @Test
        @DisplayName("should throw EntityNotFoundException if staff not found")
        void updateByStaff_Failure_StaffNotFound() {
            // Arrange
            Long nonExistentStaffId = 888L;
            given(entityFinderHelper.findPetByIdOrFail(petId)).willReturn(petToUpdate);
            doThrow(new EntityNotFoundException(UserEntity.class.getSimpleName(), nonExistentStaffId))
                    .when(authorizationHelper).verifyUserAuthorizationForPet(nonExistentStaffId, petToUpdate, actionContext);

            // Act & Assert
            assertThatThrownBy(() -> petService.updatePetByClinicStaff(petId, updateDto, nonExistentStaffId))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("UserEntity not found with id: " + nonExistentStaffId);
            then(entityFinderHelper).should().findPetByIdOrFail(petId);
            then(authorizationHelper).should().verifyUserAuthorizationForPet(nonExistentStaffId, petToUpdate, actionContext);
            then(petRepository).should(never()).save(any());
        }

        /**
         * Test failure when the staff member is not authorized (e.g., from a different clinic).
         */
        @Test
        @DisplayName("should throw AccessDeniedException if staff not authorized for pet")
        void updateByStaff_Failure_StaffNotAuthorized() {
            // Arrange
            given(entityFinderHelper.findPetByIdOrFail(petId)).willReturn(petToUpdate);
            doThrow(new AccessDeniedException(String.format("User (ID: %d) is not authorized to %s pet (ID: %d).",
                    unauthorizedStaffId, actionContext, petId)))
                    .when(authorizationHelper).verifyUserAuthorizationForPet(unauthorizedStaffId, petToUpdate, actionContext);

            // Act & Assert
            assertThatThrownBy(() -> petService.updatePetByClinicStaff(petId, updateDto, unauthorizedStaffId))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining(String.format("User (ID: %d) is not authorized to %s pet (ID: %d).",
                            unauthorizedStaffId, actionContext, petId));
            then(entityFinderHelper).should().findPetByIdOrFail(petId);
            then(authorizationHelper).should().verifyUserAuthorizationForPet(unauthorizedStaffId, petToUpdate, actionContext);
            then(petRepository).should(never()).save(any());
        }

        /**
         * Test failure when a new breed ID is provided, but the breed does not exist.
         */
        @Test
        @DisplayName("should throw EntityNotFoundException if new breedId not found")
        void updateByStaff_Failure_NewBreedNotFound() {
            // Arrange
            Long nonExistentBreedId = 777L;
            PetClinicUpdateDto dtoWithBadBreed = new PetClinicUpdateDto(null,null,null,null, nonExistentBreedId);
            given(entityFinderHelper.findPetByIdOrFail(petId)).willReturn(petToUpdate);
            doNothing().when(authorizationHelper).verifyUserAuthorizationForPet(authorizedStaffId, petToUpdate, actionContext);
            given(entityFinderHelper.findBreedOrFail(nonExistentBreedId))
                    .willThrow(new EntityNotFoundException(Breed.class.getSimpleName(), nonExistentBreedId));

            // Act & Assert
            assertThatThrownBy(() -> petService.updatePetByClinicStaff(petId, dtoWithBadBreed, authorizedStaffId))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Breed not found with id: " + nonExistentBreedId);
            then(entityFinderHelper).should().findPetByIdOrFail(petId);
            then(authorizationHelper).should().verifyUserAuthorizationForPet(authorizedStaffId, petToUpdate, actionContext);
            then(entityFinderHelper).should().findBreedOrFail(nonExistentBreedId);
            then(petRepository).should(never()).save(any());
        }

        /**
         * Test failure when the new microchip number already exists for another pet.
         */
        @Test
        @DisplayName("should throw MicrochipAlreadyExistsException if new microchip conflicts")
        void updateByStaff_Failure_MicrochipConflict() {
            // Arrange
            given(entityFinderHelper.findPetByIdOrFail(petId)).willReturn(petToUpdate);
            doNothing().when(authorizationHelper).verifyUserAuthorizationForPet(authorizedStaffId, petToUpdate, actionContext);
            given(entityFinderHelper.findBreedOrFail(newBreedId)).willReturn(newBreed);
            given(petRepository.existsByMicrochipAndIdNot(updateDto.microchip(), petId)).willReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> petService.updatePetByClinicStaff(petId, updateDto, authorizedStaffId))
                    .isInstanceOf(MicrochipAlreadyExistsException.class)
                    .hasMessageContaining(updateDto.microchip());
            then(entityFinderHelper).should().findPetByIdOrFail(petId);
            then(authorizationHelper).should().verifyUserAuthorizationForPet(authorizedStaffId, petToUpdate, actionContext);
            then(entityFinderHelper).should().findBreedOrFail(newBreedId);
            then(petRepository).should().existsByMicrochipAndIdNot(updateDto.microchip(), petId);
            then(petRepository).should(never()).save(any());
        }
    }

    /**
     * --- Tests for findPetsByClinic ---
     */
    @Nested
    @DisplayName("findPetsByClinic Tests")
    class FindPetsByClinicTests {

        private ClinicStaff staffFromClinic;
        private Pet petPendingAtClinic;
        private Pet petActiveWithVetFromClinic;
        private PetProfileDto dtoPending;
        private PetProfileDto dtoActive;
        private Pageable pageable;
        private final Long clinicId = 1L;
        private final Long staffId = 30L;
        private final String actionContext = "find pets for clinic";

        @BeforeEach
        void findClinicSetup() {
            pageable = PageRequest.of(0, 5);
            Clinic clinic = Clinic.builder().build(); clinic.setId(clinicId);
            staffFromClinic = new Vet();
            staffFromClinic.setId(staffId);
            staffFromClinic.setClinic(clinic);
            staffFromClinic.setRoles(Set.of(RoleEntity.builder().roleEnum(RoleEnum.VET).build()));

            petPendingAtClinic = new Pet(); petPendingAtClinic.setId(200L); petPendingAtClinic.setName("Pending"); petPendingAtClinic.setStatus(PetStatus.PENDING); petPendingAtClinic.setPendingActivationClinic(clinic); petPendingAtClinic.setBreed(dogBreedSpecific); petPendingAtClinic.setOwner(owner);
            petActiveWithVetFromClinic = new Pet(); petActiveWithVetFromClinic.setId(201L); petActiveWithVetFromClinic.setName("Active"); petActiveWithVetFromClinic.setStatus(PetStatus.ACTIVE); petActiveWithVetFromClinic.addVet((Vet) staffFromClinic); petActiveWithVetFromClinic.setBreed(dogBreedSpecific); petActiveWithVetFromClinic.setOwner(owner);

            Pet petActiveWithVetFromOtherClinic = new Pet(); petActiveWithVetFromOtherClinic.setId(202L); petActiveWithVetFromOtherClinic.setStatus(PetStatus.ACTIVE); /* No association */ petActiveWithVetFromOtherClinic.setBreed(dogBreedSpecific); petActiveWithVetFromOtherClinic.setOwner(owner);


            dtoPending = new PetProfileDto(200L, "Pending", Specie.DOG, null,null,null,
                    null,null, PetStatus.PENDING, ownerId,
                    owner.getUsername(), dogBreedSpecific.getId(),
                    dogBreedSpecific.getName(), clinicId, null, null, null, false, null, null);
            dtoActive = new PetProfileDto(201L, "Active", Specie.DOG, null,null,null,null,null, PetStatus.ACTIVE, ownerId, owner.getUsername(),
                    dogBreedSpecific.getId(), dogBreedSpecific.getName(), null,
                    null, null, null, false, null, null  );
        }

        /**
         * Test successful retrieval of associated pets (pending and active via vet).
         */
        @Test
        @DisplayName("should return page of associated pets for authorized staff")
        void findByClinic_Success() {
            // Arrange
            given(entityFinderHelper.findClinicStaffOrFail(staffId, actionContext)).willReturn(staffFromClinic);
            List<Pet> repoResultList = List.of(petPendingAtClinic, petActiveWithVetFromClinic);
            Page<Pet> repoResultPage = new PageImpl<>(repoResultList, pageable, 2);
            given(petRepository.findPetsAssociatedWithClinic(clinicId, pageable)).willReturn(repoResultPage);
            given(petMapper.toProfileDto(petPendingAtClinic)).willReturn(dtoPending);
            given(petMapper.toProfileDto(petActiveWithVetFromClinic)).willReturn(dtoActive);

            // Act
            Page<PetProfileDto> result = petService.findPetsByClinic(staffId, pageable);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result.getContent()).hasSize(2).containsExactlyInAnyOrder(dtoPending, dtoActive);

            then(entityFinderHelper).should().findClinicStaffOrFail(staffId, actionContext);
            then(petRepository).should().findPetsAssociatedWithClinic(clinicId, pageable);
            then(petMapper).should().toProfileDto(petPendingAtClinic);
            then(petMapper).should().toProfileDto(petActiveWithVetFromClinic);
        }

        /**
         * Test failure when the requesting user is not ClinicStaff.
         */
        @Test
        @DisplayName("should throw AccessDeniedException if requester is not staff")
        void findByClinic_Failure_NotStaff() {
            // Arrange
            given(entityFinderHelper.findClinicStaffOrFail(ownerId, actionContext))
                    .willThrow(new AccessDeniedException("User " + ownerId + " is not Clinic Staff and cannot " + actionContext));

            // Act & Assert
            assertThatThrownBy(() -> petService.findPetsByClinic(ownerId, pageable))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("is not Clinic Staff");

            then(entityFinderHelper).should().findClinicStaffOrFail(ownerId, actionContext);
            then(petRepository).should(never()).findPetsAssociatedWithClinic(anyLong(), any());
        }

        /**
         * Test retrieval when a clinic has no associated pets.
         */
        @Test
        @DisplayName("should return empty page when clinic has no associated pets")
        void findByClinic_Success_NoPets() {
            // Arrange
            given(entityFinderHelper.findClinicStaffOrFail(staffId, actionContext)).willReturn(staffFromClinic);
            given(petRepository.findPetsAssociatedWithClinic(clinicId, pageable)).willReturn(Page.empty(pageable));

            // Act
            Page<PetProfileDto> result = petService.findPetsByClinic(staffId, pageable);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.isEmpty()).isTrue();

            then(entityFinderHelper).should().findClinicStaffOrFail(staffId, actionContext);
            then(petRepository).should().findPetsAssociatedWithClinic(clinicId, pageable);
            then(petMapper).should(never()).toProfileDto(any());
        }
    }

    /**
     * --- Tests for findPendingActivationPetsByClinic ---
     */
    @Nested
    @DisplayName("findPendingActivationPetsByClinic Tests")
    class FindPendingPetsByClinicTests {

        private ClinicStaff staffFromClinic;
        private Pet petPendingAtClinic;
        private PetProfileDto dtoPending;
        private final Long clinicId = 1L;
        private final Long staffId = 30L;
        private final String actionContext = "find pending pets for clinic";

        @BeforeEach
        void findPendingSetup() {
            Clinic clinic = Clinic.builder().build(); clinic.setId(clinicId);
            staffFromClinic = new Vet();
            staffFromClinic.setId(staffId);
            staffFromClinic.setClinic(clinic);
            staffFromClinic.setRoles(Set.of(RoleEntity.builder().roleEnum(RoleEnum.VET).build()));

            Pet petActiveAtClinic;
            petPendingAtClinic = new Pet(); petPendingAtClinic.setId(210L); petPendingAtClinic.setName("Pending"); petPendingAtClinic.setStatus(PetStatus.PENDING); petPendingAtClinic.setPendingActivationClinic(clinic); petPendingAtClinic.setBreed(dogBreedSpecific); petPendingAtClinic.setOwner(owner);
            petActiveAtClinic = new Pet(); petActiveAtClinic.setId(211L); petActiveAtClinic.setName("Active"); petActiveAtClinic.setStatus(PetStatus.ACTIVE); petActiveAtClinic.addVet((Vet) staffFromClinic); petActiveAtClinic.setBreed(dogBreedSpecific); petActiveAtClinic.setOwner(owner);

            dtoPending = new PetProfileDto(210L, "Pending", Specie.DOG,
                    null,null,null,null,null,
                    PetStatus.PENDING, ownerId, owner.getUsername(), dogBreedSpecific.getId(),
                    dogBreedSpecific.getName(), clinicId, null, null,
                    null, false, null, null);
        }

        /**
         * Test successful retrieval of pending pets.
         */
        @Test
        @DisplayName("should return list of PENDING pets for authorized staff")
        void findPending_Success() {
            // Arrange
            given(entityFinderHelper.findClinicStaffOrFail(staffId, actionContext)).willReturn(staffFromClinic);
            given(petRepository.findByPendingActivationClinicIdAndStatus(clinicId, PetStatus.PENDING)).willReturn(List.of(petPendingAtClinic));
            given(petMapper.toProfileDtoList(List.of(petPendingAtClinic))).willReturn(List.of(dtoPending));

            // Act
            List<PetProfileDto> result = petService.findPendingActivationPetsByClinic(staffId);

            // Assert
            assertThat(result).isNotNull().hasSize(1).containsExactly(dtoPending);
            then(entityFinderHelper).should().findClinicStaffOrFail(staffId, actionContext);
            then(petRepository).should().findByPendingActivationClinicIdAndStatus(clinicId, PetStatus.PENDING);
            then(petMapper).should().toProfileDtoList(List.of(petPendingAtClinic));
        }

        /**
         * Test retrieval when no pets are pending at the clinic.
         */
        @Test
        @DisplayName("should return empty list when no pets are pending")
        void findPending_Success_NoPets() {
            // Arrange
            given(entityFinderHelper.findClinicStaffOrFail(staffId, actionContext)).willReturn(staffFromClinic);
            given(petRepository.findByPendingActivationClinicIdAndStatus(clinicId, PetStatus.PENDING)).willReturn(Collections.emptyList());
            given(petMapper.toProfileDtoList(Collections.emptyList())).willReturn(Collections.emptyList());

            // Act
            List<PetProfileDto> result = petService.findPendingActivationPetsByClinic(staffId);

            // Assert
            assertThat(result).isNotNull().isEmpty();
            then(entityFinderHelper).should().findClinicStaffOrFail(staffId, actionContext);
            then(petRepository).should().findByPendingActivationClinicIdAndStatus(clinicId, PetStatus.PENDING);
            then(petMapper).should().toProfileDtoList(Collections.emptyList());
        }

        /**
         * Test failure when the requesting user is not ClinicStaff.
         */
        @Test
        @DisplayName("should throw AccessDeniedException if requester is not staff")
        void findPending_Failure_NotStaff() {
            // Arrange
            given(entityFinderHelper.findClinicStaffOrFail(ownerId, actionContext))
                    .willThrow(new AccessDeniedException("User " + ownerId + " is not Clinic Staff and cannot " + actionContext));

            // Act & Assert
            assertThatThrownBy(() -> petService.findPendingActivationPetsByClinic(ownerId))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("is not Clinic Staff");

            then(entityFinderHelper).should().findClinicStaffOrFail(ownerId, actionContext);
            then(petRepository).should(never()).findByPendingActivationClinicIdAndStatus(anyLong(), any());
        }
    }

    /**
     * --- Tests for findPetById ---
     */
    @Nested
    @DisplayName("findPetById Tests")
    class FindPetByIdTests {

        private Pet pet;
        private Clinic petClinic;
        private PetProfileDto petDto;
        private final Long petId = 250L;
        private final Long ownerId = 50L;
        private final Long staffSameClinicId = 52L;
        private final Long staffDifferentClinicId = 53L;
        private final Long differentOwnerId = 54L;
        private final String actionContext = "view";

        @BeforeEach
        void findByIdSetup() {
            Owner petOwner;
            Long vetId = 51L;

            petOwner = new Owner();
            petOwner.setId(ownerId);
            petOwner.setUsername("pet_owner");

            Long clinicId = 1L;
            petClinic = Clinic.builder().name("Pet's Clinic").build();
            petClinic.setId(clinicId);

            Vet associatedVet;
            associatedVet = new Vet();
            associatedVet.setId(vetId);
            associatedVet.setClinic(petClinic);

            ClinicStaff staffFromSameClinic;
            staffFromSameClinic = new ClinicStaff();
            staffFromSameClinic.setId(staffSameClinicId);
            staffFromSameClinic.setClinic(petClinic);
            staffFromSameClinic.setRoles(Set.of(RoleEntity.builder().roleEnum(RoleEnum.ADMIN).build()));

            Clinic otherClinic = Clinic.builder().name("Other Clinic").build();
            otherClinic.setId(99L);

            ClinicStaff staffFromDifferentClinic;
            staffFromDifferentClinic = new ClinicStaff();
            staffFromDifferentClinic.setId(staffDifferentClinicId);
            staffFromDifferentClinic.setUsername("unauth_staff");
            staffFromDifferentClinic.setClinic(otherClinic);
            staffFromDifferentClinic.setRoles(Set.of(RoleEntity.builder().roleEnum(RoleEnum.ADMIN).build()));

            Owner differentOwner = new Owner(); differentOwner.setId(differentOwnerId);

            pet = new Pet();
            pet.setId(petId);
            pet.setOwner(petOwner);
            pet.setName("Target Pet");
            pet.setStatus(PetStatus.ACTIVE);
            pet.addVet(associatedVet);
            pet.setBreed(dogBreedSpecific);
            pet.setImage("pet_image.png");

            associatedVet.setName("VetName");
            associatedVet.setSurname("VetSurname");
            petDto = new PetProfileDto(
                    petId, "Target Pet",
                    Specie.DOG,
                    null,
                    null,
                    null,
                    null,
                    "pet_image.png",
                    PetStatus.ACTIVE,
                    ownerId,
                    petOwner.getUsername(),
                    dogBreedSpecific.getId(),
                    dogBreedSpecific.getName(),
                    null,
                    null,null, null,
                    false, null, null);
        }

        /**
         * Test successful retrieval when the requester is the owner.
         */
        @Test
        @DisplayName("should return pet profile when requester is owner")
        void findById_Success_RequesterIsOwner() {
            // Arrange
            given(entityFinderHelper.findPetByIdOrFail(petId)).willReturn(pet);
            doNothing().when(authorizationHelper).verifyUserAuthorizationForPet(ownerId, pet, actionContext);
            given(petMapper.toProfileDto(pet)).willReturn(petDto);

            // Act
            PetProfileDto result = petService.findPetById(petId, ownerId);

            // Assert
            assertThat(result).isNotNull().isEqualTo(petDto);
            then(entityFinderHelper).should().findPetByIdOrFail(petId);
            then(authorizationHelper).should().verifyUserAuthorizationForPet(ownerId, pet, actionContext);
            then(petMapper).should().toProfileDto(pet);
            then(entityFinderHelper).should(never()).findUserOrFail(anyLong());
        }

        /**
         * Test successful retrieval when the requester is staff from a clinic associated with the pet.
         */
        @Test
        @DisplayName("should return pet profile when requester is staff from associated clinic")
        void findById_Success_RequesterIsAssociatedStaff() {
            // Arrange
            given(entityFinderHelper.findPetByIdOrFail(petId)).willReturn(pet);
            doNothing().when(authorizationHelper).verifyUserAuthorizationForPet(staffSameClinicId, pet, actionContext);
            given(petMapper.toProfileDto(pet)).willReturn(petDto);

            // Act
            PetProfileDto result = petService.findPetById(petId, staffSameClinicId);

            // Assert
            assertThat(result).isNotNull().isEqualTo(petDto);
            then(entityFinderHelper).should().findPetByIdOrFail(petId);
            then(authorizationHelper).should().verifyUserAuthorizationForPet(staffSameClinicId, pet, actionContext);
            then(petMapper).should().toProfileDto(pet);
            then(entityFinderHelper).should(never()).findUserOrFail(anyLong());
        }

        /**
         * Test successful retrieval when the requester is staff from the clinic where the pet is PENDING activation.
         */
        @Test
        @DisplayName("should return pet profile when requester is staff from pending clinic")
        void findById_Success_RequesterIsPendingClinicStaff() {
            // Arrange
            pet.setStatus(PetStatus.PENDING);
            pet.setPendingActivationClinic(petClinic);
            pet.getAssociatedVets().clear();
            given(entityFinderHelper.findPetByIdOrFail(petId)).willReturn(pet);
            doNothing().when(authorizationHelper).verifyUserAuthorizationForPet(staffSameClinicId, pet, actionContext);
            given(petMapper.toProfileDto(pet)).willReturn(petDto);

            // Act
            PetProfileDto result = petService.findPetById(petId, staffSameClinicId);

            // Assert
            assertThat(result).isNotNull().isEqualTo(petDto);
            then(entityFinderHelper).should().findPetByIdOrFail(petId);
            then(authorizationHelper).should().verifyUserAuthorizationForPet(staffSameClinicId, pet, actionContext);
            then(petMapper).should().toProfileDto(pet);
            then(entityFinderHelper).should(never()).findUserOrFail(anyLong());
        }

        /**
         * Test failure when the Pet ID does not exist.
         */
        @Test
        @DisplayName("should throw EntityNotFoundException if pet not found")
        void findById_Failure_PetNotFound() {
            // Arrange
            given(entityFinderHelper.findPetByIdOrFail(999L))
                    .willThrow(new EntityNotFoundException(Pet.class.getSimpleName(), 999L));

            // Act & Assert
            assertThatThrownBy(() -> petService.findPetById(999L, ownerId))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Pet not found with id: 999");
            then(entityFinderHelper).should().findPetByIdOrFail(999L);
            then(authorizationHelper).should(never()).verifyUserAuthorizationForPet(anyLong(), any(), anyString());
            then(petMapper).should(never()).toProfileDto(any());
        }

        /**
         * Test failure when the requester user ID does not exist.
         */
        @Test
        @DisplayName("should throw EntityNotFoundException if requester user not found")
        void findById_Failure_RequesterNotFound() {
            // Arrange
            Long nonExistentUserId = 888L;
            given(entityFinderHelper.findPetByIdOrFail(petId)).willReturn(pet);
            doThrow(new EntityNotFoundException(UserEntity.class.getSimpleName(), nonExistentUserId))
                    .when(authorizationHelper).verifyUserAuthorizationForPet(nonExistentUserId, pet, actionContext);


            // Act & Assert
            assertThatThrownBy(() -> petService.findPetById(petId, nonExistentUserId))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("UserEntity not found with id: " + nonExistentUserId);
            then(entityFinderHelper).should().findPetByIdOrFail(petId);
            then(authorizationHelper).should().verifyUserAuthorizationForPet(nonExistentUserId, pet, actionContext);
            then(petMapper).should(never()).toProfileDto(any());
        }

        /**
         * Test failure when the requester is neither the owner nor authorized staff.
         */
        @Test
        @DisplayName("should throw AccessDeniedException if requester is not owner or authorized staff")
        void findById_Failure_Unauthorized() {
            // Arrange
            given(entityFinderHelper.findPetByIdOrFail(petId)).willReturn(pet);
            doThrow(new AccessDeniedException("User (ID: " + staffDifferentClinicId + ")..."))
                    .when(authorizationHelper).verifyUserAuthorizationForPet(staffDifferentClinicId, pet, actionContext);

            // Act & Assert
            assertThatThrownBy(() -> petService.findPetById(petId, staffDifferentClinicId))
                    .isInstanceOf(AccessDeniedException.class);

            doThrow(new AccessDeniedException("User (ID: " + differentOwnerId + ")..."))
                    .when(authorizationHelper).verifyUserAuthorizationForPet(differentOwnerId, pet, actionContext);

            assertThatThrownBy(() -> petService.findPetById(petId, differentOwnerId))
                    .isInstanceOf(AccessDeniedException.class);

            then(entityFinderHelper).should(times(2)).findPetByIdOrFail(petId);
            then(authorizationHelper).should(times(1)).verifyUserAuthorizationForPet(staffDifferentClinicId, pet, actionContext);
            then(authorizationHelper).should(times(1)).verifyUserAuthorizationForPet(differentOwnerId, pet, actionContext);
            then(petMapper).should(never()).toProfileDto(any());
        }
    }

    /**
     * --- Tests for findBreedsBySpecie ---
     */
    @Nested
    @DisplayName("findBreedsBySpecie Tests")
    class FindBreedsBySpecieTests {

        private Breed catBreed1;
        private Breed catBreed2;
        private BreedDto catDto1;
        private BreedDto catDto2;

        @BeforeEach
        void findBreedsSetup() {
            catBreed1 = Breed.builder().id(201L).name("Siamese").specie(Specie.CAT).imageUrl(null).build();
            catBreed2 = Breed.builder().id(202L).name("Persian").specie(Specie.CAT).imageUrl(null).build();

            catDto1 = new BreedDto(catBreed1.getId(), catBreed1.getName(), catBreed1.getImageUrl());
            catDto2 = new BreedDto(catBreed2.getId(), catBreed2.getName(), catBreed2.getImageUrl());
        }

        /**
         * Test successful retrieval of breeds for a given species.
         */
        @Test
        @DisplayName("should return list of BreedDtos for the specified species")
        void findBreeds_Success() {
            // Arrange
            List<Breed> breedList = List.of(catBreed1, catBreed2); // Assuming sorted by repo query
            List<BreedDto> expectedDtoList = List.of(catDto1, catDto2);
            given(breedRepository.findBySpecieOrderByNameAsc(Specie.CAT)).willReturn(breedList);
            given(breedMapper.toDtoList(breedList)).willReturn(expectedDtoList);

            // Act
            List<BreedDto> result = petService.findBreedsBySpecie(Specie.CAT);

            // Assert
            assertThat(result).isNotNull().isEqualTo(expectedDtoList);
            then(breedRepository).should().findBySpecieOrderByNameAsc(Specie.CAT);
            then(breedMapper).should().toDtoList(breedList);
        }

        /**
         * Test retrieval when no breeds exist for the specified species.
         */
        @Test
        @DisplayName("should return empty list if no breeds found for species")
        void findBreeds_Success_Empty() {
            // Arrange
            given(breedRepository.findBySpecieOrderByNameAsc(Specie.FERRET)).willReturn(Collections.emptyList());
            given(breedMapper.toDtoList(Collections.emptyList())).willReturn(Collections.emptyList());

            // Act
            List<BreedDto> result = petService.findBreedsBySpecie(Specie.FERRET);

            // Assert
            assertThat(result).isNotNull().isEmpty();
            then(breedRepository).should().findBySpecieOrderByNameAsc(Specie.FERRET);
            then(breedMapper).should().toDtoList(Collections.emptyList());
        }

        /**
         * Test behavior when null is passed as species.
         */
        @Test
        @DisplayName("should return empty list when species is null")
        void findBreeds_NullSpecies() {
            // Act
            List<BreedDto> result = petService.findBreedsBySpecie(null);

            // Assert
            assertThat(result).isNotNull().isEmpty();
            then(breedRepository).should(never()).findBySpecieOrderByNameAsc(any());
            then(breedMapper).should(never()).toDtoList(any());
        }
    }

    /**
     * --- Tests for associateVetWithPet ---
     */
    @Nested
    @DisplayName("associateVetWithPet Tests")
    class AssociateVetTests {

        private Pet petToAssociate;
        private Vet vetToAssociate;
        private Vet alreadyAssociatedVet;
        private final Long petId = 300L;
        private final Long vetToAssociateId = 60L;
        private final Long alreadyAssociatedVetId = 61L;

        @BeforeEach
        void associateVetSetup() {
            Clinic clinic = Clinic.builder().build(); clinic.setId(1L); // Vet needs a clinic

            vetToAssociate = new Vet(); vetToAssociate.setId(vetToAssociateId); vetToAssociate.setClinic(clinic);
            alreadyAssociatedVet = new Vet(); alreadyAssociatedVet.setId(alreadyAssociatedVetId); alreadyAssociatedVet.setClinic(clinic);

            petToAssociate = new Pet();
            petToAssociate.setId(petId);
            petToAssociate.setOwner(owner);
            petToAssociate.setStatus(PetStatus.ACTIVE);
            petToAssociate.setBreed(dogBreedSpecific);
            petToAssociate.setAssociatedVets(new HashSet<>(Set.of(alreadyAssociatedVet)));
        }

        /**
         * Test successful association of a new Vet.
         */
        @Test
        @DisplayName("should associate vet successfully when not already associated")
        void associate_Success() {
            // Arrange
            given(entityFinderHelper.findPetByIdOrFail(petId)).willReturn(petToAssociate);
            given(entityFinderHelper.findVetOrFail(vetToAssociateId)).willReturn(vetToAssociate);
            given(petRepository.save(any(Pet.class))).willAnswer(inv -> inv.getArgument(0));

            // Act
            petService.associateVetWithPet(petId, vetToAssociateId, ownerId);

            // Assert
            then(entityFinderHelper).should().findPetByIdOrFail(petId);
            then(entityFinderHelper).should().findVetOrFail(vetToAssociateId);
            then(petRepository).should().save(petCaptor.capture());

            Pet saved = petCaptor.getValue();
            assertThat(saved.getAssociatedVets()).contains(vetToAssociate, alreadyAssociatedVet);
            assertThat(saved.getStatus()).isEqualTo(PetStatus.ACTIVE);
        }

        /**
         * Test successful association of a Vet to an INACTIVE pet, which should reactivate it.
         */
        @Test
        @DisplayName("should associate vet and reactivate pet if it was INACTIVE")
        void associate_ReactivatesInactivePet() {
            // Arrange
            petToAssociate.setStatus(PetStatus.INACTIVE);
            petToAssociate.getAssociatedVets().clear();
            given(entityFinderHelper.findPetByIdOrFail(petId)).willReturn(petToAssociate);
            given(entityFinderHelper.findVetOrFail(vetToAssociateId)).willReturn(vetToAssociate);
            given(petRepository.save(any(Pet.class))).willAnswer(inv -> inv.getArgument(0));

            // Act
            petService.associateVetWithPet(petId, vetToAssociateId, ownerId);

            // Assert
            then(entityFinderHelper).should().findPetByIdOrFail(petId);
            then(entityFinderHelper).should().findVetOrFail(vetToAssociateId);
            then(petRepository).should().save(petCaptor.capture());

            Pet saved = petCaptor.getValue();
            assertThat(saved.getAssociatedVets()).contains(vetToAssociate);
            assertThat(saved.getStatus()).isEqualTo(PetStatus.ACTIVE);
        }


        /**
         * Test failure when the association already exists.
         */
        @Test
        @DisplayName("should throw IllegalStateException if vet already associated")
        void associate_Failure_AlreadyAssociated() {
            // Arrange
            given(entityFinderHelper.findPetByIdOrFail(petId)).willReturn(petToAssociate);
            given(entityFinderHelper.findVetOrFail(alreadyAssociatedVetId)).willReturn(alreadyAssociatedVet);

            // Act & Assert
            assertThatThrownBy(() -> petService.associateVetWithPet(petId, alreadyAssociatedVetId, ownerId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("is already associated with pet");

            then(entityFinderHelper).should().findPetByIdOrFail(petId);
            then(entityFinderHelper).should().findVetOrFail(alreadyAssociatedVetId);
            then(petRepository).should(never()).save(any());
        }

        /**
         * Test failure when a pet is not found.
         */
        @Test
        @DisplayName("should throw EntityNotFoundException if pet not found")
        void associate_Failure_PetNotFound() {
            // Arrange
            given(entityFinderHelper.findPetByIdOrFail(999L))
                    .willThrow(new EntityNotFoundException(Pet.class.getSimpleName(), 999L));

            // Act & Assert
            assertThatThrownBy(() -> petService.associateVetWithPet(999L, vetToAssociateId, ownerId))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Pet not found with id: 999");

            then(entityFinderHelper).should(never()).findVetOrFail(anyLong());
            then(petRepository).should(never()).save(any());
        }

        /**
         * Test failure when a vet is not found.
         */
        @Test
        @DisplayName("should throw EntityNotFoundException if vet not found")
        void associate_Failure_VetNotFound() {
            // Arrange
            Long nonExistentVetId = 888L;
            given(entityFinderHelper.findPetByIdOrFail(petId)).willReturn(petToAssociate);
            given(entityFinderHelper.findVetOrFail(nonExistentVetId))
                    .willThrow(new EntityNotFoundException(Vet.class.getSimpleName(), nonExistentVetId));

            // Act & Assert
            assertThatThrownBy(() -> petService.associateVetWithPet(petId, nonExistentVetId, ownerId))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Vet not found with id: " + nonExistentVetId);

            then(entityFinderHelper).should().findPetByIdOrFail(petId);
            then(petRepository).should(never()).save(any());
        }

        /**
         * Test failure when a user is not the owner.
         */
        @Test
        @DisplayName("should throw AccessDeniedException if user is not owner")
        void associate_Failure_NotOwner() {
            // Arrange
            Long otherOwnerId = 777L;
            given(entityFinderHelper.findPetByIdOrFail(petId)).willReturn(petToAssociate);

            // Act & Assert
            assertThatThrownBy(() -> petService.associateVetWithPet(petId, vetToAssociateId, otherOwnerId))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("User " + otherOwnerId + " is not the owner of pet " + petId);

            then(entityFinderHelper).should().findPetByIdOrFail(petId);
            then(entityFinderHelper).should(never()).findVetOrFail(anyLong());
            then(petRepository).should(never()).save(any());
        }
    }

    /**
     * --- Tests for disassociateVetFromPet ---
     */
    @Nested
    @DisplayName("disassociateVetFromPet Tests")
    class DisassociateVetTests {

        private Pet petWithTwoVets;
        private Pet petWithOneVet;
        private Vet vetToRemove;
        private Vet vetToKeep;
        private final Long petTwoVetsId = 350L;
        private final Long petOneVetId = 351L;
        private final Long vetToRemoveId = 70L;

        @BeforeEach
        void disassociateVetSetup() {
            Clinic clinic = Clinic.builder().build(); clinic.setId(1L);
            Long vetToKeepId = 71L;

            vetToRemove = new Vet(); vetToRemove.setId(vetToRemoveId); vetToRemove.setClinic(clinic);
            vetToKeep = new Vet(); vetToKeep.setId(vetToKeepId); vetToKeep.setClinic(clinic);

            petWithTwoVets = new Pet();
            petWithTwoVets.setId(petTwoVetsId);
            petWithTwoVets.setOwner(owner);
            petWithTwoVets.setStatus(PetStatus.ACTIVE);
            petWithTwoVets.setBreed(dogBreedSpecific);
            petWithTwoVets.setAssociatedVets(new HashSet<>(Set.of(vetToRemove, vetToKeep)));

            petWithOneVet = new Pet();
            petWithOneVet.setId(petOneVetId);
            petWithOneVet.setOwner(owner);
            petWithOneVet.setStatus(PetStatus.ACTIVE);
            petWithOneVet.setBreed(dogBreedSpecific);
            petWithOneVet.setAssociatedVets(new HashSet<>(Set.of(vetToRemove)));
        }

        /**
         * Test successful disassociation when multiple vets are associated.
         * Pet status should remain ACTIVE.
         */
        @Test
        @DisplayName("should disassociate vet successfully when multiple vets exist")
        void disassociate_Success_MultipleVets() {
            // Arrange
            given(entityFinderHelper.findPetByIdOrFail(petTwoVetsId)).willReturn(petWithTwoVets);
            given(entityFinderHelper.findVetOrFail(vetToRemoveId)).willReturn(vetToRemove);

            // Act
            petService.disassociateVetFromPet(petTwoVetsId, vetToRemoveId, ownerId);

            // Assert
            then(entityFinderHelper).should().findPetByIdOrFail(petTwoVetsId);
            then(entityFinderHelper).should().findVetOrFail(vetToRemoveId);
            then(petRepository).should().save(petCaptor.capture());

            Pet saved = petCaptor.getValue();
            assertThat(saved.getAssociatedVets()).hasSize(1).containsExactly(vetToKeep);
            assertThat(saved.getStatus()).isEqualTo(PetStatus.ACTIVE);
        }

        /**
         * Test successful disassociation when it's the last vet associated.
         * Pet status should change to INACTIVE.
         */
        @Test
        @DisplayName("should disassociate last vet and set pet to INACTIVE")
        void disassociate_Success_LastVet() {
            // Arrange
            given(entityFinderHelper.findPetByIdOrFail(petOneVetId)).willReturn(petWithOneVet);
            given(entityFinderHelper.findVetOrFail(vetToRemoveId)).willReturn(vetToRemove);

            // Act
            petService.disassociateVetFromPet(petOneVetId, vetToRemoveId, ownerId);

            // Assert
            then(entityFinderHelper).should().findPetByIdOrFail(petOneVetId);
            then(entityFinderHelper).should().findVetOrFail(vetToRemoveId);
            then(petRepository).should().save(petCaptor.capture());

            Pet saved = petCaptor.getValue();
            assertThat(saved.getAssociatedVets()).isEmpty();
            assertThat(saved.getStatus()).isEqualTo(PetStatus.PENDING);
        }

        /**
         * Test the case where the vet to be disassociated is not associated.
         * Expects no changes and no save operation.
         */
        @Test
        @DisplayName("should do nothing if vet is not associated")
        void disassociate_NoOp_VetNotAssociated() {
            // Arrange
            Vet nonAssociatedVet = new Vet(); nonAssociatedVet.setId(999L);
            given(entityFinderHelper.findPetByIdOrFail(petWithTwoVets.getId())).willReturn(petWithTwoVets);
            given(entityFinderHelper.findVetOrFail(999L)).willReturn(nonAssociatedVet);

            // Act
            petService.disassociateVetFromPet(petWithTwoVets.getId(), 999L, ownerId);

            // Assert
            then(entityFinderHelper).should().findPetByIdOrFail(petWithTwoVets.getId());
            then(entityFinderHelper).should().findVetOrFail(999L);
            then(petRepository).should(never()).save(any());
            assertThat(petWithTwoVets.getAssociatedVets()).hasSize(2);
        }


        /**
         * Test failure when a pet is not found.
         */
        @Test
        @DisplayName("should throw EntityNotFoundException if pet not found")
        void disassociate_Failure_PetNotFound() {
            // Arrange
            given(entityFinderHelper.findPetByIdOrFail(999L))
                    .willThrow(new EntityNotFoundException(Pet.class.getSimpleName(), 999L));

            // Act & Assert
            assertThatThrownBy(() -> petService.disassociateVetFromPet(999L, vetToRemoveId, ownerId))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Pet not found with id: 999");
            then(entityFinderHelper).should(never()).findVetOrFail(anyLong());
            then(petRepository).should(never()).save(any());
        }

        /**
         * Test failure when a vet is not found.
         */
        @Test
        @DisplayName("should throw EntityNotFoundException if vet not found")
        void disassociate_Failure_VetNotFound() {
            // Arrange
            Long nonExistentVetId = 888L;
            given(entityFinderHelper.findPetByIdOrFail(petWithTwoVets.getId())).willReturn(petWithTwoVets);
            given(entityFinderHelper.findVetOrFail(nonExistentVetId))
                    .willThrow(new EntityNotFoundException(Vet.class.getSimpleName(), nonExistentVetId));

            // Act
            Throwable thrown = Assertions.catchThrowable(() -> petService.disassociateVetFromPet(petWithTwoVets.getId(), nonExistentVetId, ownerId));
            // Assert
            assertThat(thrown)
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Vet not found with id: " + nonExistentVetId);
            then(petRepository).should(never()).save(any());
        }

        /**
         * Test failure when a user is not the owner.
         */
        @Test
        @DisplayName("should throw AccessDeniedException if user is not owner")
        void disassociate_Failure_NotOwner() {
            // Arrange
            Long otherOwnerId = 777L;
            // Mock Pet lookup OK
            given(entityFinderHelper.findPetByIdOrFail(petWithTwoVets.getId())).willReturn(petWithTwoVets);

            // Act
            Throwable thrown = Assertions.catchThrowable(() -> petService.disassociateVetFromPet(petWithTwoVets.getId(), vetToRemoveId, otherOwnerId));
            // Assert
            assertThat(thrown)
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("User " + otherOwnerId + " is not the owner of pet " + petWithTwoVets.getId());

            then(entityFinderHelper).should().findPetByIdOrFail(petWithTwoVets.getId());
            then(entityFinderHelper).should(never()).findVetOrFail(anyLong());
            then(petRepository).should(never()).save(any());
        }
    }
}