package com.petconnect.backend.pet.application.service.impl;

import com.petconnect.backend.exception.EntityNotFoundException;
import com.petconnect.backend.exception.MicrochipAlreadyExistsException;
import com.petconnect.backend.pet.application.dto.*;
import com.petconnect.backend.pet.application.mapper.BreedMapper;
import com.petconnect.backend.pet.application.mapper.PetMapper;
import com.petconnect.backend.pet.domain.model.*;
import com.petconnect.backend.pet.domain.repository.BreedRepository;
import com.petconnect.backend.pet.domain.repository.PetRepository;
import com.petconnect.backend.user.application.dto.VetSummaryDto;
import com.petconnect.backend.user.domain.model.*;
import com.petconnect.backend.user.domain.repository.ClinicRepository;
import com.petconnect.backend.user.domain.repository.UserRepository;
import com.petconnect.backend.user.domain.repository.VetRepository;

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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/**
 * Unit tests for {@link PetServiceImpl}.
 * Verifies the business logic for managing Pet entities using Mockito.
 *
 * @author ibosquet
 */
@ExtendWith(MockitoExtension.class)
class PetServiceImplTest {

    // --- Mocks ---
    @Mock private PetRepository petRepository;
    @Mock private BreedRepository breedRepository;
    @Mock private UserRepository userRepository;
    @Mock private ClinicRepository clinicRepository;
    @Mock private VetRepository vetRepository;
    @Mock private PetMapper petMapper;
    @Mock private BreedMapper breedMapper;

    // --- Class Under Test ---
    @InjectMocks
    private PetServiceImpl petService;

    // --- Captors ---
    @Captor private ArgumentCaptor<Pet> petCaptor;

    // --- Test Data ---
    private Owner owner;
    private Breed dogBreedSpecific;
    private Breed mixedDogBreed;
    private PetRegistrationDto registrationDtoSpecificBreed;
    private PetRegistrationDto registrationDtoNoBreed;
    private Pet savedPet;
    private PetProfileDto expectedPetProfileDto;
    private final Long ownerId = 1L;
    private final Long specificBreedId = 10L;
    private final Long mixedBreedId = 99L;
    private final Long savedPetId = 101L;
    private final String defaultDogImagePath = "images/avatars/pets/dog.png";
    private final String specificDogImagePath = "images/avatars/pets/labrador.png";
    private final String providedImagePath = "user/provided/dog.jpg";


    /**
     * Sets up common test data before each test method execution.
     */
    @BeforeEach
    void setUp() {
        // --- Mock @Value field ---
        ReflectionTestUtils.setField(petService, "defaultPetImagePathBase", "images/avatars/pets/");

        // --- Entities ---
        owner = new Owner();
        owner.setId(ownerId);
        owner.setUsername("testowner");

        dogBreedSpecific = Breed.builder().id(specificBreedId).name("Labrador").specie(Specie.DOG).imageUrl(specificDogImagePath).build();
        mixedDogBreed = Breed.builder().id(mixedBreedId).name("Mixed/Other").specie(Specie.DOG).imageUrl(defaultDogImagePath).build();

        // --- DTOs ---
        registrationDtoSpecificBreed = new PetRegistrationDto(
                "Buddy", Specie.DOG, LocalDate.of(2023, 1, 15),
                specificBreedId, // Specific breed ID
                null,            // No image provided
                "Brown", Gender.MALE, "12345"
        );
        registrationDtoNoBreed = new PetRegistrationDto(
                "Mixy", Specie.DOG, LocalDate.of(2022, 3, 10),
                null,             // No breed ID provided
                providedImagePath,// Image IS provided
                "Black", Gender.FEMALE, null
        );

        // --- Simulated results  ---
        savedPet = new Pet();
        savedPet.setId(savedPetId);
        savedPet.setOwner(owner);
        savedPet.setStatus(PetStatus.PENDING);
        // We will set name, breed, image, etc. within the specific tests based on the input DTO

        // Expected DTO also needs specific setup per test
        expectedPetProfileDto = new PetProfileDto(
                savedPetId, "Buddy", Specie.DOG, "Brown", Gender.MALE,
                LocalDate.of(2023, 1, 15), "12345", specificDogImagePath, // Expect specific breed image
                PetStatus.PENDING, ownerId, "testowner", specificBreedId, "Labrador",
                null, Set.of(),
                LocalDateTime.now(), LocalDateTime.now()
        );
    }

    // --- Tests for registerPet ---
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
            savedPet.setImage(dogBreedSpecific.getImageUrl()); // Expecting image from specific breed
            savedPet.setBirthDate(registrationDtoSpecificBreed.birthDate());
            savedPet.setColor(registrationDtoSpecificBreed.color());
            savedPet.setGender(registrationDtoSpecificBreed.gender());
            savedPet.setMicrochip(registrationDtoSpecificBreed.microchip());

            // Prepare expected DTO result matching the savedPet state above
            expectedPetProfileDto = new PetProfileDto(
                    savedPetId, savedPet.getName(), savedPet.getBreed().getSpecie(), savedPet.getColor(),
                    savedPet.getGender(), savedPet.getBirthDate(), savedPet.getMicrochip(),
                    savedPet.getImage(),
                    savedPet.getStatus(), ownerId, owner.getUsername(), specificBreedId, dogBreedSpecific.getName(),
                    null, Set.of(), savedPet.getCreatedAt(), savedPet.getUpdatedAt()
            );

            given(userRepository.findById(ownerId)).willReturn(Optional.of(owner));
            given(breedRepository.findById(specificBreedId)).willReturn(Optional.of(dogBreedSpecific));
            given(petRepository.save(any(Pet.class))).willReturn(savedPet);
            given(petMapper.toProfileDto(savedPet)).willReturn(expectedPetProfileDto);

            // Act
            PetProfileDto result = petService.registerPet(registrationDtoSpecificBreed, ownerId);

            // Assert
            assertThat(result).isNotNull().isEqualTo(expectedPetProfileDto);
            assertThat(result.image()).isEqualTo(specificDogImagePath); // Verify correct image used

            then(userRepository).should().findById(ownerId);
            then(breedRepository).should().findById(specificBreedId);
            then(breedRepository).should(never()).findByNameAndSpecie(anyString(), any(Specie.class));
            then(petRepository).should().save(petCaptor.capture());
            then(petMapper).should().toProfileDto(savedPet);

            Pet capturedPet = petCaptor.getValue();
            assertThat(capturedPet.getBreed()).isEqualTo(dogBreedSpecific);
            assertThat(capturedPet.getImage()).isEqualTo(specificDogImagePath); // Verify image in captured entity
        }

        /**
         * Test case for successful pet registration with no breed ID (uses Mixed/Other fallback) and a provided image.
         * Expects the provided image path to be used.
         */
        @Test
        @DisplayName("should register pet successfully with fallback breed and provided image")
        void registerPet_Success_FallbackBreed_ProvidedImage() {
            // Arrange
            savedPet.setName(registrationDtoNoBreed.name());
            savedPet.setBreed(mixedDogBreed); // Fallback breed
            savedPet.setImage(registrationDtoNoBreed.image()); // Provided image
            savedPet.setBirthDate(registrationDtoNoBreed.birthDate());
            savedPet.setColor(registrationDtoNoBreed.color());
            savedPet.setGender(registrationDtoNoBreed.gender());
            savedPet.setMicrochip(registrationDtoNoBreed.microchip());

            // Prepare expected DTO result matching the savedPet state above
            expectedPetProfileDto = new PetProfileDto(
                    savedPetId, savedPet.getName(), Specie.DOG, savedPet.getColor(), savedPet.getGender(),
                    savedPet.getBirthDate(), savedPet.getMicrochip(), savedPet.getImage(), // Should be providedImagePath
                    PetStatus.PENDING, ownerId, owner.getUsername(), mixedBreedId, mixedDogBreed.getName(),
                    null, Set.of(), savedPet.getCreatedAt(), savedPet.getUpdatedAt()
            );

            given(userRepository.findById(ownerId)).willReturn(Optional.of(owner));
            given(breedRepository.findByNameAndSpecie("Mixed/Other", Specie.DOG)).willReturn(Optional.of(mixedDogBreed));
            given(petRepository.save(any(Pet.class))).willReturn(savedPet);
            given(petMapper.toProfileDto(savedPet)).willReturn(expectedPetProfileDto);

            // Act
            PetProfileDto result = petService.registerPet(registrationDtoNoBreed, ownerId);

            // Assert
            assertThat(result).isNotNull().isEqualTo(expectedPetProfileDto);
            assertThat(result.image()).isEqualTo(providedImagePath); // Verify correct image used

            then(userRepository).should().findById(ownerId);
            then(breedRepository).should(never()).findById(anyLong());
            then(breedRepository).should().findByNameAndSpecie("Mixed/Other", Specie.DOG);
            then(petRepository).should().save(petCaptor.capture());
            then(petMapper).should().toProfileDto(savedPet);

            Pet capturedPet = petCaptor.getValue();
            assertThat(capturedPet.getBreed()).isEqualTo(mixedDogBreed);
            assertThat(capturedPet.getImage()).isEqualTo(providedImagePath); // Verify image in captured entity
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
            // Prepare expected savedPet state
            savedPet.setName(dtoNoBreedNoImage.name());
            savedPet.setBreed(mixedDogBreed); // Fallback breed
            savedPet.setImage(defaultDogImagePath); // Default species image
            savedPet.setBirthDate(dtoNoBreedNoImage.birthDate());
            // ... reset other optional fields ...
            savedPet.setColor(null);
            savedPet.setGender(null);
            savedPet.setMicrochip(null);

            // Prepare expected DTO result
            expectedPetProfileDto = new PetProfileDto(
                    savedPetId, savedPet.getName(), Specie.DOG, null, null,
                    savedPet.getBirthDate(), null, defaultDogImagePath, // Expect default image
                    PetStatus.PENDING, ownerId, owner.getUsername(), mixedBreedId, mixedDogBreed.getName(),
                    null, Set.of(), savedPet.getCreatedAt(), savedPet.getUpdatedAt()
            );

            given(userRepository.findById(ownerId)).willReturn(Optional.of(owner));
            given(breedRepository.findByNameAndSpecie("Mixed/Other", Specie.DOG)).willReturn(Optional.of(mixedDogBreed));
            given(petRepository.save(any(Pet.class))).willReturn(savedPet);
            given(petMapper.toProfileDto(savedPet)).willReturn(expectedPetProfileDto);

            // Act
            PetProfileDto result = petService.registerPet(dtoNoBreedNoImage, ownerId);

            // Assert
            assertThat(result).isNotNull().isEqualTo(expectedPetProfileDto);
            assertThat(result.image()).isEqualTo(defaultDogImagePath);

            then(userRepository).should().findById(ownerId);
            then(breedRepository).should(never()).findById(anyLong());
            then(breedRepository).should().findByNameAndSpecie("Mixed/Other", Specie.DOG);
            then(petRepository).should().save(petCaptor.capture());
            then(petMapper).should().toProfileDto(savedPet);

            assertThat(petCaptor.getValue().getImage()).isEqualTo(defaultDogImagePath);
        }

        /**
         * Test case verifying that an EntityNotFoundException is thrown if the owner ID does not exist.
         */
        @Test
        @DisplayName("should throw EntityNotFoundException if owner not found")
        void registerPet_Failure_OwnerNotFound() {
            // Arrange
            given(userRepository.findById(999L)).willReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> petService.registerPet(registrationDtoSpecificBreed, 999L))
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
            given(userRepository.findById(ownerId)).willReturn(Optional.of(owner));
            given(breedRepository.findById(nonExistentBreedId)).willReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> petService.registerPet(dtoWithBadBreed, ownerId))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Breed not found with id: " + nonExistentBreedId);

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
            given(userRepository.findById(ownerId)).willReturn(Optional.of(owner));
            given(breedRepository.findByNameAndSpecie("Mixed/Other", Specie.CAT)).willReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> petService.registerPet(dtoWithoutBreedId, ownerId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Default breed configuration error for species CAT");

            then(petRepository).should(never()).save(any());
        }
    }

    // --- Tests for associatePetToClinicForActivation ---
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
         * Setup data specific to these tests.
         */
        @BeforeEach
        void associateSetup() {
            targetClinic = Clinic.builder().name("Target Clinic").build();
            targetClinic.setId(clinicId);

            pendingPet = new Pet();
            pendingPet.setId(pendingPetId);
            pendingPet.setName("Pending Pet");
            pendingPet.setOwner(owner); // Owner from main setup
            pendingPet.setStatus(PetStatus.PENDING); // Correct status
            pendingPet.setBreed(dogBreedSpecific); // Need a breed
            pendingPet.setPendingActivationClinic(null); // Not currently pending

            activePet = new Pet();
            activePet.setId(activePetId);
            activePet.setName("Active Pet");
            activePet.setOwner(owner);
            activePet.setStatus(PetStatus.ACTIVE); // Incorrect status for this operation
            activePet.setBreed(dogBreedSpecific);
        }

        /**
         * Test successful association when pet is PENDING and not already associated.
         * Verifies clinic lookup, pet update, and save call.
         */
        @Test
        @DisplayName("should associate pet successfully when conditions met")
        void associate_Success() {
            // Arrange
            given(petRepository.findById(pendingPetId)).willReturn(Optional.of(pendingPet));
            given(clinicRepository.findById(clinicId)).willReturn(Optional.of(targetClinic));
            // No need to mock save specifically unless verifying return, void method here

            // Act
            petService.associatePetToClinicForActivation(pendingPetId, clinicId, ownerId);

            // Assert
            then(petRepository).should().findById(pendingPetId);
            then(clinicRepository).should().findById(clinicId);
            then(petRepository).should().save(petCaptor.capture()); // Verify save and capture

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
            given(petRepository.findById(999L)).willReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> petService.associatePetToClinicForActivation(999L, clinicId, ownerId))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Pet not found with id: 999");

            then(clinicRepository).should(never()).findById(anyLong());
            then(petRepository).should(never()).save(any());
        }

        /**
         * Test failure when the user performing the action is not the owner of the pet.
         */
        @Test
        @DisplayName("should throw AccessDeniedException if user is not owner")
        void associate_Failure_NotOwner() {
            // Arrange
            Long otherOwnerId = 55L; // Different owner ID
            given(petRepository.findById(pendingPetId)).willReturn(Optional.of(pendingPet)); // Pet found

            // Act & Assert
            assertThatThrownBy(() -> petService.associatePetToClinicForActivation(pendingPetId, clinicId, otherOwnerId))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("User " + otherOwnerId + " is not the owner of pet " + pendingPetId);

            then(clinicRepository).should(never()).findById(anyLong());
            then(petRepository).should(never()).save(any());
        }

        /**
         * Test failure when the pet is not in PENDING status (e.g., already ACTIVE).
         */
        @Test
        @DisplayName("should throw IllegalStateException if pet is not PENDING")
        void associate_Failure_WrongStatus() {
            // Arrange
            given(petRepository.findById(activePetId)).willReturn(Optional.of(activePet)); // Return the ACTIVE pet

            // Act & Assert
            assertThatThrownBy(() -> petService.associatePetToClinicForActivation(activePetId, clinicId, ownerId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Pet " + activePetId + " must be in PENDING status to associate for activation, but was ACTIVE");

            then(clinicRepository).should(never()).findById(anyLong());
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
            existingPendingClinic.setId(99L); // Some other clinic ID
            pendingPet.setPendingActivationClinic(existingPendingClinic); // Set it as already pending

            given(petRepository.findById(pendingPetId)).willReturn(Optional.of(pendingPet));

            // Act & Assert
            assertThatThrownBy(() -> petService.associatePetToClinicForActivation(pendingPetId, clinicId, ownerId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Pet ID " + pendingPetId + " is already pending activation at clinic 99");

            then(clinicRepository).should(never()).findById(anyLong());
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
            given(petRepository.findById(pendingPetId)).willReturn(Optional.of(pendingPet));
            given(clinicRepository.findById(nonExistentClinicId)).willReturn(Optional.empty()); // Clinic not found

            // Act & Assert
            assertThatThrownBy(() -> petService.associatePetToClinicForActivation(pendingPetId, nonExistentClinicId, ownerId))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Target clinic not found with id: " + nonExistentClinicId);

            then(petRepository).should(never()).save(any());
        }
    }

    // --- Tests for activatePet ---
    @Nested
    @DisplayName("activatePet Tests")
    class ActivatePetTests {

        private Pet petToActivate;
        private Clinic pendingClinic;
        private Vet activatingVet;
        private PetProfileDto activatedPetDto;
        private PetActivationDto activationDto;
        private Breed petBreed;
        private final Long petToActivateId = 120L;
        private final Long activatingVetId = 20L;


        @BeforeEach
        void activateSetup() {
            Long clinicId = 1L;
            Long breedId = 30L;
            pendingClinic = Clinic.builder().name("Activation Clinic").build();
            pendingClinic.setId(clinicId);

            activatingVet = new Vet();
            activatingVet.setId(activatingVetId);
            activatingVet.setUsername("activator_vet");
            activatingVet.setClinic(pendingClinic);
            activatingVet.setRoles(Set.of(RoleEntity.builder().roleEnum(RoleEnum.VET).build()));

            petBreed = Breed.builder().id(breedId).name("Beagle").specie(Specie.DOG).build();

            petToActivate = new Pet();
            petToActivate.setId(petToActivateId);
            petToActivate.setName("ActivoOriginal");
            petToActivate.setStatus(PetStatus.PENDING);
            petToActivate.setOwner(owner);
            petToActivate.setBreed(petBreed);
            petToActivate.setPendingActivationClinic(pendingClinic);
            petToActivate.setBirthDate(null);
            petToActivate.setGender(null);
            petToActivate.setMicrochip(null);
            petToActivate.setImage("original.jpg");
            petToActivate.setColor("OriginalColor");

            activationDto = new PetActivationDto(
                    "ActivoFinal",
                    "TricolorFinal",
                    Gender.MALE,
                    LocalDate.of(2023, 6, 1),
                    "MICROCHIP123FINAL",
                    breedId,
                    "final_image.jpg"
            );


            activatedPetDto = new PetProfileDto(
                    petToActivateId, activationDto.name(), Specie.DOG, activationDto.color(), activationDto.gender(),
                    activationDto.birthDate(), activationDto.microchip(), activationDto.image(),
                    PetStatus.ACTIVE,
                    owner.getId(), owner.getUsername(), breedId, petBreed.getName(),
                    null,
                    Set.of(new VetSummaryDto(activatingVet.getId(), activatingVet.getName(), activatingVet.getSurname())),
                    LocalDateTime.now(), LocalDateTime.now()
            );
        }

        /**
         * Test successful activation by an authorized Vet when DTO is valid.
         */
        @Test
        @DisplayName("should activate pet successfully when called by authorized Vet with valid DTO")
        void activate_Success() {
            // Arrange
            given(userRepository.findById(activatingVetId)).willReturn(Optional.of(activatingVet));
            given(petRepository.findById(petToActivateId)).willReturn(Optional.of(petToActivate));
            given(petRepository.existsByMicrochipAndIdNot(activationDto.microchip(), petToActivateId)).willReturn(false);
            given(breedRepository.findById(activationDto.breedId())).willReturn(Optional.of(petBreed));
            given(petRepository.save(any(Pet.class))).willAnswer(inv -> inv.getArgument(0));
            given(petMapper.toProfileDto(any(Pet.class))).willReturn(activatedPetDto);

            // Act
            PetProfileDto result = petService.activatePet(petToActivateId, activationDto, activatingVetId);

            // Assert
            assertThat(result).isNotNull().isEqualTo(activatedPetDto);

            // Verify interactions
            then(userRepository).should().findById(activatingVetId);
            then(petRepository).should().findById(petToActivateId);
            then(petRepository).should().existsByMicrochipAndIdNot(activationDto.microchip(), petToActivateId);
            then(breedRepository).should().findById(activationDto.breedId());
            then(petRepository).should().save(petCaptor.capture());
            then(petMapper).should().toProfileDto(any(Pet.class));

            Pet saved = petCaptor.getValue();
            assertThat(saved.getStatus()).isEqualTo(PetStatus.ACTIVE);
            assertThat(saved.getPendingActivationClinic()).isNull();
            assertThat(saved.getAssociatedVets()).contains(activatingVet);
            assertThat(saved.getName()).isEqualTo(activationDto.name());
            assertThat(saved.getColor()).isEqualTo(activationDto.color());
            assertThat(saved.getGender()).isEqualTo(activationDto.gender());
            assertThat(saved.getBirthDate()).isEqualTo(activationDto.birthDate());
            assertThat(saved.getMicrochip()).isEqualTo(activationDto.microchip());
            assertThat(saved.getBreed().getId()).isEqualTo(activationDto.breedId());
            assertThat(saved.getImage()).isEqualTo(activationDto.image());
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
            given(userRepository.findById(activatingVetId)).willReturn(Optional.of(activatingVet));
            given(petRepository.findById(petToActivateId)).willReturn(Optional.of(petToActivate));

            // Act & Assert
            assertThatThrownBy(() -> petService.activatePet(petToActivateId, activationDto, activatingVetId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("must be in PENDING status to activate");

            then(petRepository).should(never()).save(any());
        }

        /**
         * Test failure when the staff member is not authorized (not from the pending clinic).
         * (Este test no necesita el DTO en el arrange, pero la llamada sí lo requiere)
         */
        @Test
        @DisplayName("should throw AccessDeniedException if staff not from pending clinic")
        void activate_Failure_StaffWrongClinic() {
            // Arrange
            Clinic differentClinic = Clinic.builder().build(); differentClinic.setId(99L);
            activatingVet.setClinic(differentClinic);
            given(userRepository.findById(activatingVetId)).willReturn(Optional.of(activatingVet));
            given(petRepository.findById(petToActivateId)).willReturn(Optional.of(petToActivate));

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
            given(userRepository.findById(activatingVetId)).willReturn(Optional.of(activatingVet));
            given(petRepository.findById(petToActivateId)).willReturn(Optional.of(petToActivate));
            given(petRepository.existsByMicrochipAndIdNot(activationDto.microchip(), petToActivateId)).willReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> petService.activatePet(petToActivateId, activationDto, activatingVetId))
                    .isInstanceOf(MicrochipAlreadyExistsException.class)
                    .hasMessageContaining(activationDto.microchip());

            then(breedRepository).should(never()).findById(anyLong());
            then(petRepository).should(never()).save(any());
        }

        /**
         * Test failure when the breedId provided in DTO is invalid.
         */
        @Test
        @DisplayName("should throw EntityNotFoundException if breedId in DTO not found")
        void activate_Failure_BreedNotFoundInDto() {
            // Arrange
            PetActivationDto dtoWithBadBreed = new PetActivationDto(
                    "Name", "Color", Gender.FEMALE, LocalDate.now(), "MicrochipOK",
                    999L, // ID de Raza Inexistente
                    "image.jpg"
            );
            given(userRepository.findById(activatingVetId)).willReturn(Optional.of(activatingVet));
            given(petRepository.findById(petToActivateId)).willReturn(Optional.of(petToActivate));
            given(petRepository.existsByMicrochipAndIdNot(dtoWithBadBreed.microchip(), petToActivateId)).willReturn(false);
            given(breedRepository.findById(999L)).willReturn(Optional.empty());


            // Act & Assert
            assertThatThrownBy(() -> petService.activatePet(petToActivateId, dtoWithBadBreed, activatingVetId))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Breed not found with id: 999");

            then(petRepository).should(never()).save(any());
        }


        @Test
        @DisplayName("should throw EntityNotFoundException if activating staff not found")
        void activate_Failure_StaffNotFound() {
            // Arrange
            given(userRepository.findById(999L)).willReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> petService.activatePet(petToActivateId, activationDto, 999L)) // Pasar DTO
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("UserEntity not found with id: 999");

            then(petRepository).should(never()).findById(anyLong());
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

            given(userRepository.findById(activatingVetId)).willReturn(Optional.of(activatingAdmin));
            given(petRepository.findById(petToActivateId)).willReturn(Optional.of(petToActivate));
            given(breedRepository.findById(activationDto.breedId())).willReturn(Optional.of(petBreed));
            given(petRepository.existsByMicrochipAndIdNot(activationDto.microchip(), petToActivateId)).willReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> petService.activatePet(petToActivateId, activationDto, activatingVetId))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("user performing the activation is not a veterinarian");

            then(petRepository).should(never()).save(any());
            then(userRepository).should().findById(activatingVetId);
            then(petRepository).should().findById(petToActivateId);
            then(breedRepository).should().findById(activationDto.breedId());
            then(petRepository).should().existsByMicrochipAndIdNot(activationDto.microchip(), petToActivateId);
        }

    }

    // --- Tests for deactivatePet ---
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
            given(petRepository.findById(petToDeactivateId)).willReturn(Optional.of(petToDeactivate));

            given(petRepository.save(any(Pet.class))).willAnswer(invocation -> {
                Pet petToSave = invocation.getArgument(0);
                assertThat(petToSave.getStatus()).isEqualTo(PetStatus.INACTIVE);
                return petToSave;
            });

            PetProfileDto inactiveDto = new PetProfileDto(petToDeactivateId, petToDeactivate.getName(), petToDeactivate.getBreed().getSpecie(), null,null,null,null,null, PetStatus.INACTIVE, ownerId, owner.getUsername(), dogBreedSpecific.getId(), dogBreedSpecific.getName(), null, Set.of(), null, null);
            given(petMapper.toProfileDto(any(Pet.class))).willReturn(inactiveDto);


            // Act
            PetProfileDto result = petService.deactivatePet(petToDeactivateId, ownerId);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.status()).isEqualTo(PetStatus.INACTIVE);

            then(petRepository).should().findById(petToDeactivateId);
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
            given(petRepository.findById(inactivePetId)).willReturn(Optional.of(alreadyInactivePet));

            // Act & Assert
            assertThatThrownBy(() -> petService.deactivatePet(inactivePetId, ownerId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot deactivate pet " + inactivePetId + " because it is already in INACTIVE status.");

            then(petRepository).should(never()).save(any());
        }

        /**
         * Test failure when the pet ID does not exist.
         */
        @Test
        @DisplayName("should throw EntityNotFoundException if pet not found")
        void deactivate_Failure_PetNotFound() {
            // Arrange
            given(petRepository.findById(999L)).willReturn(Optional.empty());

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
            given(petRepository.findById(petToDeactivateId)).willReturn(Optional.of(petToDeactivate));

            // Act & Assert
            assertThatThrownBy(() -> petService.deactivatePet(petToDeactivateId, otherOwnerId))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("User " + otherOwnerId + " is not the owner of pet " + petToDeactivateId);

            then(petRepository).should(never()).save(any());
        }
    }

    // --- Tests for updatePetByOwner ---
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
                    "new_image.png",
                    "Black and White",
                    Gender.MALE,
                    null,
                    "22222",
                    newBreedId
            );

            // Expected DTO after update
            expectedUpdatedDto = new PetProfileDto(
                    petId, updateDto.name(), Specie.DOG, updateDto.color(), updateDto.gender(),
                    existingPet.getBirthDate(),
                    updateDto.microchip(), updateDto.image(), existingPet.getStatus(), owner.getId(),
                    owner.getUsername(), newBreedId, newBreed.getName(),
                    null, Set.of(),
                    LocalDateTime.now(), LocalDateTime.now().plusMinutes(1)
            );
        }

        /**
         * Test successful update when called by the correct owner.
         * Verifies pet lookup, breed resolution (if changed), microchip validation,
         * mapper application, saving, and DTO mapping.
         */
        @Test
        @DisplayName("should update pet successfully when called by owner")
        void updateByOwner_Success() {
            // Arrange
            given(petRepository.findById(petId)).willReturn(Optional.of(existingPet));
            given(breedRepository.findById(newBreedId)).willReturn(Optional.of(newBreed));
            given(petRepository.existsByMicrochipAndIdNot(updateDto.microchip(), petId)).willReturn(false);
            given(petMapper.updateFromOwnerDto(updateDto, existingPet, newBreed)).willReturn(true);
            given(petRepository.save(any(Pet.class))).willAnswer(inv -> inv.getArgument(0));
            given(petMapper.toProfileDto(any(Pet.class))).willReturn(expectedUpdatedDto);


            // Act
            PetProfileDto result = petService.updatePetByOwner(petId, updateDto, ownerId);

            // Assert
            assertThat(result).isNotNull().isEqualTo(expectedUpdatedDto);

            // Verify interactions
            then(petRepository).should().findById(petId);
            then(breedRepository).should().findById(newBreedId);
            then(petRepository).should().existsByMicrochipAndIdNot(updateDto.microchip(), petId);
            then(petMapper).should().updateFromOwnerDto(updateDto, existingPet, newBreed);
            then(petRepository).should().save(petCaptor.capture());
            then(petMapper).should().toProfileDto(any(Pet.class));

            // Verify captured entity state (
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
            // Create a DTO with the *same* values as the existingPet
            PetOwnerUpdateDto noChangeDto = new PetOwnerUpdateDto(
                    existingPet.getName(), existingPet.getImage(), existingPet.getColor(),
                    existingPet.getGender(), existingPet.getBirthDate(), existingPet.getMicrochip(),
                    existingPet.getBreed().getId()
            );
            // Mock pet lookup
            given(petRepository.findById(petId)).willReturn(Optional.of(existingPet));
            given(petMapper.updateFromOwnerDto(noChangeDto, existingPet, existingPet.getBreed())).willReturn(false);
            // Mock the final DTO mapping
            given(petMapper.toProfileDto(existingPet)).willReturn(
                    new PetProfileDto( // Create an expected DTO based on original data
                            existingPet.getId(), existingPet.getName(), existingPet.getBreed().getSpecie(), existingPet.getColor(),
                            existingPet.getGender(), existingPet.getBirthDate(), existingPet.getMicrochip(),
                            existingPet.getImage(), existingPet.getStatus(), owner.getId(), owner.getUsername(),
                            existingPet.getBreed().getId(), existingPet.getBreed().getName(), null, Set.of(),
                            existingPet.getCreatedAt(), existingPet.getUpdatedAt()
                    )
            );

            // Act
            PetProfileDto result = petService.updatePetByOwner(petId, noChangeDto, ownerId);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.name()).isEqualTo(existingPet.getName());

            then(petRepository).should().findById(petId);
            // Breed lookup NOT called because ID didn't change
            then(breedRepository).should(never()).findById(anyLong());
            // Microchip check NOT called because value didn't change
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
            given(petRepository.findById(999L)).willReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> petService.updatePetByOwner(999L, updateDto, ownerId))
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
            given(petRepository.findById(petId)).willReturn(Optional.of(existingPet)); // Pet found

            // Act & Assert
            assertThatThrownBy(() -> petService.updatePetByOwner(petId, updateDto, otherOwnerId))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("User " + otherOwnerId + " is not the owner of pet " + petId);

            then(petRepository).should(never()).save(any());
            then(petMapper).should(never()).updateFromOwnerDto(any(), any(), any());
        }

        /**
         * Test failure when a new breed ID is provided but the breed does not exist.
         */
        @Test
        @DisplayName("should throw EntityNotFoundException if new breedId not found")
        void updateByOwner_Failure_NewBreedNotFound() {
            // Arrange
            Long nonExistentBreedId = 888L;
            PetOwnerUpdateDto dtoWithBadBreed = new PetOwnerUpdateDto(null,null,null,null,null,null, nonExistentBreedId);
            given(petRepository.findById(petId)).willReturn(Optional.of(existingPet));
            given(breedRepository.findById(nonExistentBreedId)).willReturn(Optional.empty()); // Breed not found

            // Act & Assert
            assertThatThrownBy(() -> petService.updatePetByOwner(petId, dtoWithBadBreed, ownerId))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Breed not found with id: " + nonExistentBreedId);

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
            given(petRepository.findById(petId)).willReturn(Optional.of(existingPet));
            given(breedRepository.findById(newBreedId)).willReturn(Optional.of(newBreed));
            given(petRepository.existsByMicrochipAndIdNot(updateDto.microchip(), petId)).willReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> petService.updatePetByOwner(petId, updateDto, ownerId))
                    .isInstanceOf(MicrochipAlreadyExistsException.class)
                    .hasMessageContaining(updateDto.microchip());

            then(petRepository).should(never()).save(any());
            then(petMapper).should(never()).updateFromOwnerDto(any(), any(), any());
        }
    }

    // --- Tests for updatePetByClinicStaff ---
    @Nested
    @DisplayName("updatePetByClinicStaff Tests")
    class UpdatePetByClinicStaffTests {

        private Pet petToUpdate;
        private ClinicStaff authorizedStaff; // Staff authorized for the pet
        private ClinicStaff unauthorizedStaff; // Staff from a different clinic or not associated
        private Owner petOwner; // Need the owner to verify association isn't broken
        private Breed newBreed;
        private PetClinicUpdateDto updateDto;
        private PetProfileDto expectedUpdatedDto;
        private final Long petId = 140L;
        private final Long authorizedStaffId = 30L;
        private final Long unauthorizedStaffId = 31L;
        private final Long newBreedId = 45L;

        /**
         * Setup data: An existing pet, an authorized staff member, and an unauthorized one.
         */
        @BeforeEach
        void updateClinicStaffSetup() {
            Clinic petClinic = Clinic.builder().name("Pet's Clinic").build();
            Long clinicId = 1L;
            petClinic.setId(clinicId);

            // Authorized staff (Vet from clinic 1)
            authorizedStaff = new Vet(); // Using Vet for variety
            authorizedStaff.setId(authorizedStaffId);
            authorizedStaff.setUsername("auth_staff");
            authorizedStaff.setClinic(petClinic);
            authorizedStaff.setRoles(Set.of(RoleEntity.builder().roleEnum(RoleEnum.VET).build()));

            // Unauthorized staff (e.g., Admin from another clinic)
            Clinic otherClinic = Clinic.builder().name("Other Clinic").build();
            otherClinic.setId(99L);
            unauthorizedStaff = new ClinicStaff();
            unauthorizedStaff.setId(unauthorizedStaffId);
            unauthorizedStaff.setUsername("unauth_staff");
            unauthorizedStaff.setClinic(otherClinic); // Different clinic
            unauthorizedStaff.setRoles(Set.of(RoleEntity.builder().roleEnum(RoleEnum.ADMIN).build()));

            // Pet's Owner
            Long ownerIdForPet = 5L;
            petOwner = new Owner();
            petOwner.setId(ownerIdForPet);
            petOwner.setUsername("pet_owner_" + petId);

            // Breeds
            Breed originalBreed = Breed.builder().id(20L).name("Dalmatian").specie(Specie.DOG).build();
            newBreed = Breed.builder().id(newBreedId).name("Boxer").specie(Specie.DOG).build();

            // Pet to be updated (ACTIVE, owned by petOwner, associated with clinic 1 via the authorizedStaff)
            petToUpdate = new Pet();
            petToUpdate.setId(petId);
            petToUpdate.setOwner(petOwner);
            petToUpdate.setName("Spot");
            petToUpdate.setBreed(originalBreed);
            petToUpdate.setStatus(PetStatus.ACTIVE); // Staff updates typically happen on ACTIVE pets
            petToUpdate.setMicrochip("CHIP-ORIGINAL");
            petToUpdate.setColor("White/Black");
            petToUpdate.addVet((Vet) authorizedStaff); // Associate the authorized vet

            // DTO with changes clinic staff can make
            updateDto = new PetClinicUpdateDto(
                    "White/Brown",                // Color changed
                    Gender.FEMALE,                // Gender changed
                    LocalDate.of(2022, 10, 10),
                    "CHIP-UPDATED",               // Microchip changed
                    newBreedId                    // Breed changed
            );

            // Expected DTO after update
            expectedUpdatedDto = new PetProfileDto(
                    petId, petToUpdate.getName(), // Name not changed by this DTO
                    Specie.DOG, updateDto.color(), updateDto.gender(), updateDto.birthDate(),
                    updateDto.microchip(), petToUpdate.getImage(), // Image not changed
                    petToUpdate.getStatus(), ownerIdForPet, petOwner.getUsername(),
                    newBreedId, newBreed.getName(), null,
                    Set.of(new VetSummaryDto(authorizedStaffId, authorizedStaff.getName(), authorizedStaff.getSurname())), // Vets associated
                    LocalDateTime.now(), LocalDateTime.now().plusMinutes(1)
            );
        }

        /**
         * Test successful update when called by authorized clinic staff (Vet/Admin associated with the pet).
         */
        @Test
        @DisplayName("should update pet successfully when called by authorized staff")
        void updateByStaff_Success() {
            // Arrange
            given(petRepository.findById(petId)).willReturn(Optional.of(petToUpdate));
            // Mock staff lookup (needed for authorization)
            given(userRepository.findById(authorizedStaffId)).willReturn(Optional.of(authorizedStaff));
            // Mock breed lookup (because breedId is changing)
            given(breedRepository.findById(newBreedId)).willReturn(Optional.of(newBreed));
            // Mock microchip uniqueness check for the new microchip
            given(petRepository.existsByMicrochipAndIdNot(updateDto.microchip(), petId)).willReturn(false);
            // Mock mapper update call to return true (changes were applied)
            given(petMapper.updateFromClinicDto(updateDto, petToUpdate, newBreed)).willReturn(true);
            // Mock save and final DTO mapping
            given(petRepository.save(any(Pet.class))).willAnswer(inv -> inv.getArgument(0));
            given(petMapper.toProfileDto(any(Pet.class))).willReturn(expectedUpdatedDto);

            // Act
            PetProfileDto result = petService.updatePetByClinicStaff(petId, updateDto, authorizedStaffId);

            // Assert
            assertThat(result).isNotNull().isEqualTo(expectedUpdatedDto);

            // Verify interactions
            then(petRepository).should().findById(petId);
            then(userRepository).should().findById(authorizedStaffId); // Verify auth check happened
            then(breedRepository).should().findById(newBreedId);
            then(petRepository).should().existsByMicrochipAndIdNot(updateDto.microchip(), petId);
            then(petMapper).should().updateFromClinicDto(updateDto, petToUpdate, newBreed);
            then(petRepository).should().save(any(Pet.class));
            then(petMapper).should().toProfileDto(any(Pet.class));
        }

        /**
         * Test successful update where no actual changes are made according to the mapper.
         * Verifies that the repository save method is NOT called.
         */
        @Test
        @DisplayName("should not save pet if mapper reports no changes")
        void updateByStaff_NoChanges_ShouldNotSave() {
            // Arrange
            // Create a DTO with the *same* values as the existingPet for clinic-updatable fields
            PetClinicUpdateDto noChangeDto = new PetClinicUpdateDto(
                    petToUpdate.getColor(), petToUpdate.getGender(), petToUpdate.getBirthDate(),
                    petToUpdate.getMicrochip(), petToUpdate.getBreed().getId()
            );
            given(petRepository.findById(petId)).willReturn(Optional.of(petToUpdate));
            given(userRepository.findById(authorizedStaffId)).willReturn(Optional.of(authorizedStaff));
            // Breed/Microchip lookups/checks should not happen if values are the same
            // Mock mapper update call to return FALSE
            given(petMapper.updateFromClinicDto(noChangeDto, petToUpdate, petToUpdate.getBreed())).willReturn(false);
            // Mock final DTO mapping (will use original petToUpdate)
            given(petMapper.toProfileDto(petToUpdate)).willReturn(
                    new PetProfileDto( // DTO based on original data
                            petToUpdate.getId(), petToUpdate.getName(), petToUpdate.getBreed().getSpecie(), petToUpdate.getColor(),
                            petToUpdate.getGender(), petToUpdate.getBirthDate(), petToUpdate.getMicrochip(),
                            petToUpdate.getImage(), petToUpdate.getStatus(), petOwner.getId(), petOwner.getUsername(),
                            petToUpdate.getBreed().getId(), petToUpdate.getBreed().getName(), null,
                            Set.of(new VetSummaryDto(authorizedStaffId, authorizedStaff.getName(), authorizedStaff.getSurname())),
                            petToUpdate.getCreatedAt(), petToUpdate.getUpdatedAt()
                    )
            );

            // Act
            PetProfileDto result = petService.updatePetByClinicStaff(petId, noChangeDto, authorizedStaffId);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.color()).isEqualTo(petToUpdate.getColor()); // Verify original data

            then(petRepository).should().findById(petId);
            then(userRepository).should().findById(authorizedStaffId);
            then(breedRepository).should(never()).findById(anyLong()); // Not called
            then(petRepository).should(never()).existsByMicrochipAndIdNot(anyString(), anyLong()); // Not called
            then(petMapper).should().updateFromClinicDto(noChangeDto, petToUpdate, petToUpdate.getBreed());
            then(petRepository).should(never()).save(any(Pet.class)); // Save NOT called
            then(petMapper).should().toProfileDto(petToUpdate);
        }

        /**
         * Test failure when the pet ID does not exist.
         */
        @Test
        @DisplayName("should throw EntityNotFoundException if pet not found")
        void updateByStaff_Failure_PetNotFound() {
            // Arrange
            given(petRepository.findById(999L)).willReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> petService.updatePetByClinicStaff(999L, updateDto, authorizedStaffId))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Pet not found with id: 999");

            then(userRepository).should(never()).findById(anyLong()); // Should fail before auth check
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
            given(petRepository.findById(petId)).willReturn(Optional.of(petToUpdate)); // Pet found
            given(userRepository.findById(nonExistentStaffId)).willReturn(Optional.empty()); // Staff not found

            // Act & Assert
            assertThatThrownBy(() -> petService.updatePetByClinicStaff(petId, updateDto, nonExistentStaffId))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("UserEntity not found with id: " + nonExistentStaffId); // Check helper message

            then(petRepository).should(never()).save(any());
        }

        /**
         * Test failure when the staff member is not authorized (e.g., from a different clinic).
         */
        @Test
        @DisplayName("should throw AccessDeniedException if staff not authorized for pet")
        void updateByStaff_Failure_StaffNotAuthorized() {
            // Arrange
            given(petRepository.findById(petId)).willReturn(Optional.of(petToUpdate));
            given(userRepository.findById(unauthorizedStaffId)).willReturn(Optional.of(unauthorizedStaff)); // Unauthorized staff found

            // Act & Assert
            // Note: The exact message depends on verifyUserAuthorizationForPet logic
            assertThatThrownBy(() -> petService.updatePetByClinicStaff(petId, updateDto, unauthorizedStaffId))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("is not authorized to update clinical info for pet");

            then(petRepository).should(never()).save(any());
        }

        /**
         * Test failure when a new breed ID is provided but the breed does not exist.
         */
        @Test
        @DisplayName("should throw EntityNotFoundException if new breedId not found")
        void updateByStaff_Failure_NewBreedNotFound() {
            // Arrange
            Long nonExistentBreedId = 777L;
            PetClinicUpdateDto dtoWithBadBreed = new PetClinicUpdateDto(null,null,null,null, nonExistentBreedId);
            given(petRepository.findById(petId)).willReturn(Optional.of(petToUpdate));
            given(userRepository.findById(authorizedStaffId)).willReturn(Optional.of(authorizedStaff)); // Staff authorized
            given(breedRepository.findById(nonExistentBreedId)).willReturn(Optional.empty()); // New breed not found

            // Act & Assert
            assertThatThrownBy(() -> petService.updatePetByClinicStaff(petId, dtoWithBadBreed, authorizedStaffId))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Breed not found with id: " + nonExistentBreedId);

            then(petRepository).should(never()).save(any());
        }

        /**
         * Test failure when the new microchip number already exists for another pet.
         */
        @Test
        @DisplayName("should throw MicrochipAlreadyExistsException if new microchip conflicts")
        void updateByStaff_Failure_MicrochipConflict() {
            // Arrange
            given(petRepository.findById(petId)).willReturn(Optional.of(petToUpdate));
            given(userRepository.findById(authorizedStaffId)).willReturn(Optional.of(authorizedStaff));
            // Mock breed lookup if needed (breedId changes in updateDto)
            given(breedRepository.findById(newBreedId)).willReturn(Optional.of(newBreed));
            // Mock microchip check to return TRUE (conflict found)
            given(petRepository.existsByMicrochipAndIdNot(updateDto.microchip(), petId)).willReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> petService.updatePetByClinicStaff(petId, updateDto, authorizedStaffId))
                    .isInstanceOf(MicrochipAlreadyExistsException.class)
                    .hasMessageContaining(updateDto.microchip());

            then(petRepository).should(never()).save(any());
        }
    }

    // --- Tests for findPetsByClinic ---
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

        @BeforeEach
        void findClinicSetup() {
            pageable = PageRequest.of(0, 5);
            Clinic clinic = Clinic.builder().build(); clinic.setId(clinicId);
            staffFromClinic = new Vet(); // Use Vet, could be Admin too
            staffFromClinic.setId(staffId);
            staffFromClinic.setClinic(clinic);
            staffFromClinic.setRoles(Set.of(RoleEntity.builder().roleEnum(RoleEnum.VET).build()));

            petPendingAtClinic = new Pet(); petPendingAtClinic.setId(200L); petPendingAtClinic.setName("Pending"); petPendingAtClinic.setStatus(PetStatus.PENDING); petPendingAtClinic.setPendingActivationClinic(clinic); petPendingAtClinic.setBreed(dogBreedSpecific); petPendingAtClinic.setOwner(owner);
            petActiveWithVetFromClinic = new Pet(); petActiveWithVetFromClinic.setId(201L); petActiveWithVetFromClinic.setName("Active"); petActiveWithVetFromClinic.setStatus(PetStatus.ACTIVE); petActiveWithVetFromClinic.addVet((Vet) staffFromClinic); petActiveWithVetFromClinic.setBreed(dogBreedSpecific); petActiveWithVetFromClinic.setOwner(owner);

            // Pet not associated with this clinic
            Pet petActiveWithVetFromOtherClinic = new Pet(); petActiveWithVetFromOtherClinic.setId(202L); petActiveWithVetFromOtherClinic.setStatus(PetStatus.ACTIVE); /* No association */ petActiveWithVetFromOtherClinic.setBreed(dogBreedSpecific); petActiveWithVetFromOtherClinic.setOwner(owner);


            dtoPending = new PetProfileDto(200L, "Pending", Specie.DOG, null,null,null,null,null, PetStatus.PENDING, ownerId, owner.getUsername(), dogBreedSpecific.getId(), dogBreedSpecific.getName(), clinicId, Set.of(), null, null);
            dtoActive = new PetProfileDto(201L, "Active", Specie.DOG, null,null,null,null,null, PetStatus.ACTIVE, ownerId, owner.getUsername(), dogBreedSpecific.getId(), dogBreedSpecific.getName(), null, Set.of(new VetSummaryDto(staffId, staffFromClinic.getName(), staffFromClinic.getSurname())), null, null);
        }

        /**
         * Test successful retrieval of associated pets (pending and active via vet).
         */
        @Test
        @DisplayName("should return page of associated pets for authorized staff")
        void findByClinic_Success() {
            // Arrange
            given(userRepository.findById(staffId)).willReturn(Optional.of(staffFromClinic)); // Find staff
            List<Pet> repoResultList = List.of(petPendingAtClinic, petActiveWithVetFromClinic);
            Page<Pet> repoResultPage = new PageImpl<>(repoResultList, pageable, 2);
            given(petRepository.findPetsAssociatedWithClinic(clinicId, pageable)).willReturn(repoResultPage); // Mock repo call
            given(petMapper.toProfileDto(petPendingAtClinic)).willReturn(dtoPending);
            given(petMapper.toProfileDto(petActiveWithVetFromClinic)).willReturn(dtoActive);

            // Act
            Page<PetProfileDto> result = petService.findPetsByClinic(staffId, pageable);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result.getContent()).hasSize(2).containsExactlyInAnyOrder(dtoPending, dtoActive);

            then(userRepository).should().findById(staffId);
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
            given(userRepository.findById(ownerId)).willReturn(Optional.of(owner)); // Requester is Owner

            // Act & Assert
            assertThatThrownBy(() -> petService.findPetsByClinic(ownerId, pageable))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("is not Clinic Staff");

            then(petRepository).should(never()).findPetsAssociatedWithClinic(anyLong(), any());
        }

        /**
         * Test retrieval when clinic has no associated pets.
         */
        @Test
        @DisplayName("should return empty page when clinic has no associated pets")
        void findByClinic_Success_NoPets() {
            // Arrange
            given(userRepository.findById(staffId)).willReturn(Optional.of(staffFromClinic));
            given(petRepository.findPetsAssociatedWithClinic(clinicId, pageable)).willReturn(Page.empty(pageable));

            // Act
            Page<PetProfileDto> result = petService.findPetsByClinic(staffId, pageable);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.isEmpty()).isTrue();

            then(userRepository).should().findById(staffId);
            then(petRepository).should().findPetsAssociatedWithClinic(clinicId, pageable);
            then(petMapper).should(never()).toProfileDto(any());
        }
    }

    // --- Tests for findPendingActivationPetsByClinic ---
    @Nested
    @DisplayName("findPendingActivationPetsByClinic Tests")
    class FindPendingPetsByClinicTests {

        private ClinicStaff staffFromClinic;
        private Pet petPendingAtClinic;
        private PetProfileDto dtoPending;
        private final Long clinicId = 1L;
        private final Long staffId = 30L;

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

            dtoPending = new PetProfileDto(210L, "Pending", Specie.DOG, null,null,null,null,null, PetStatus.PENDING, ownerId, owner.getUsername(), dogBreedSpecific.getId(), dogBreedSpecific.getName(), clinicId, Set.of(), null, null);
        }

        /**
         * Test successful retrieval of pending pets.
         */
        @Test
        @DisplayName("should return list of PENDING pets for authorized staff")
        void findPending_Success() {
            // Arrange
            given(userRepository.findById(staffId)).willReturn(Optional.of(staffFromClinic));
            given(petRepository.findByPendingActivationClinicIdAndStatus(clinicId, PetStatus.PENDING)).willReturn(List.of(petPendingAtClinic));
            given(petMapper.toProfileDtoList(List.of(petPendingAtClinic))).willReturn(List.of(dtoPending));

            // Act
            List<PetProfileDto> result = petService.findPendingActivationPetsByClinic(staffId);

            // Assert
            assertThat(result).isNotNull().hasSize(1).containsExactly(dtoPending);
            then(userRepository).should().findById(staffId);
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
            given(userRepository.findById(staffId)).willReturn(Optional.of(staffFromClinic));
            given(petRepository.findByPendingActivationClinicIdAndStatus(clinicId, PetStatus.PENDING)).willReturn(Collections.emptyList());
            given(petMapper.toProfileDtoList(Collections.emptyList())).willReturn(Collections.emptyList());

            // Act
            List<PetProfileDto> result = petService.findPendingActivationPetsByClinic(staffId);

            // Assert
            assertThat(result).isNotNull().isEmpty();
            then(userRepository).should().findById(staffId);
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
            given(userRepository.findById(ownerId)).willReturn(Optional.of(owner));

            // Act & Assert
            assertThatThrownBy(() -> petService.findPendingActivationPetsByClinic(ownerId))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("is not Clinic Staff");

            then(petRepository).should(never()).findByPendingActivationClinicIdAndStatus(anyLong(), any());
        }
    }

    // --- Tests for findPetById ---
    @Nested
    @DisplayName("findPetById Tests")
    class FindPetByIdTests {

        private Pet pet;
        private Owner petOwner;
        private Clinic petClinic;
        private ClinicStaff staffFromSameClinic;
        private ClinicStaff staffFromDifferentClinic;
        private Owner differentOwner;
        private PetProfileDto petDto;
        private final Long petId = 250L;
        private final Long ownerId = 50L;
        private final Long staffSameClinicId = 52L;
        private final Long staffDifferentClinicId = 53L;
        private final Long differentOwnerId = 54L;

        @BeforeEach
        void findByIdSetup() {
            Long vetId = 51L;
            Long clinicId = 1L;

            petOwner = new Owner(); petOwner.setId(ownerId);
            petClinic = Clinic.builder().name("Pet's Clinic").build();
            petClinic.setId(clinicId);
            Vet associatedVet = new Vet(); associatedVet.setId(vetId); associatedVet.setClinic(petClinic);
            staffFromSameClinic = new ClinicStaff(); staffFromSameClinic.setId(staffSameClinicId); staffFromSameClinic.setClinic(petClinic); staffFromSameClinic.setRoles(Set.of(RoleEntity.builder().roleEnum(RoleEnum.ADMIN).build())); // Example Role

            Clinic otherClinic = Clinic.builder().name("Other Clinic").build();
            otherClinic.setId(99L);
            staffFromDifferentClinic = new ClinicStaff();
            staffFromDifferentClinic.setId(staffDifferentClinicId);
            staffFromDifferentClinic.setUsername("unauth_staff");
            staffFromDifferentClinic.setClinic(otherClinic);
            staffFromDifferentClinic.setRoles(Set.of(RoleEntity.builder().roleEnum(RoleEnum.ADMIN).build()));

            differentOwner = new Owner(); differentOwner.setId(differentOwnerId);

            pet = new Pet();
            pet.setId(petId);
            pet.setOwner(petOwner);
            pet.setName("Target Pet");
            pet.setStatus(PetStatus.ACTIVE);
            pet.addVet(associatedVet);
            pet.setBreed(dogBreedSpecific);

            petDto = new PetProfileDto(petId, "Target Pet", Specie.DOG, null, null, null, null, null, PetStatus.ACTIVE, ownerId, petOwner.getUsername(), dogBreedSpecific.getId(), dogBreedSpecific.getName(), null, Set.of(new VetSummaryDto(vetId, associatedVet.getName(), associatedVet.getSurname())), null, null);
        }

        /**
         * Test successful retrieval when the requester is the owner.
         */
        @Test
        @DisplayName("should return pet profile when requester is owner")
        void findById_Success_RequesterIsOwner() {
            // Arrange
            given(petRepository.findById(petId)).willReturn(Optional.of(pet));
            given(userRepository.findById(ownerId)).willReturn(Optional.of(petOwner));
            given(petMapper.toProfileDto(pet)).willReturn(petDto);

            // Act
            PetProfileDto result = petService.findPetById(petId, ownerId);

            // Assert
            assertThat(result).isNotNull().isEqualTo(petDto);
            then(petRepository).should().findById(petId);
            then(userRepository).should().findById(ownerId);
            then(petMapper).should().toProfileDto(pet);
        }

        /**
         * Test successful retrieval when the requester is staff from a clinic associated with the pet.
         */
        @Test
        @DisplayName("should return pet profile when requester is staff from associated clinic")
        void findById_Success_RequesterIsAssociatedStaff() {
            // Arrange
            given(petRepository.findById(petId)).willReturn(Optional.of(pet));
            given(userRepository.findById(staffSameClinicId)).willReturn(Optional.of(staffFromSameClinic));
            given(petMapper.toProfileDto(pet)).willReturn(petDto);


            // Act
            PetProfileDto result = petService.findPetById(petId, staffSameClinicId);

            // Assert
            assertThat(result).isNotNull().isEqualTo(petDto);
            then(petRepository).should().findById(petId);
            then(userRepository).should().findById(staffSameClinicId);
            then(petMapper).should().toProfileDto(pet);
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

            given(petRepository.findById(petId)).willReturn(Optional.of(pet));
            given(userRepository.findById(staffSameClinicId)).willReturn(Optional.of(staffFromSameClinic));
            given(petMapper.toProfileDto(pet)).willReturn(petDto);

            // Act
            PetProfileDto result = petService.findPetById(petId, staffSameClinicId);

            // Assert
            assertThat(result).isNotNull().isEqualTo(petDto);
            then(petRepository).should().findById(petId);
            then(userRepository).should().findById(staffSameClinicId);
            then(petMapper).should().toProfileDto(pet);
        }

        /**
         * Test failure when the Pet ID does not exist.
         */
        @Test
        @DisplayName("should throw EntityNotFoundException if pet not found")
        void findById_Failure_PetNotFound() {
            // Arrange
            given(petRepository.findById(999L)).willReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> petService.findPetById(999L, ownerId)) // Requester ID doesn't matter here
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Pet not found with id: 999");

            then(userRepository).should(never()).findById(anyLong()); // Fails before user lookup
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
            given(petRepository.findById(petId)).willReturn(Optional.of(pet));
            given(userRepository.findById(nonExistentUserId)).willReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> petService.findPetById(petId, nonExistentUserId))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("UserEntity not found with id: " + nonExistentUserId);

            then(petMapper).should(never()).toProfileDto(any());
        }

        /**
         * Test failure when the requester is neither the owner nor authorized staff.
         */
        @Test
        @DisplayName("should throw AccessDeniedException if requester is not owner or authorized staff")
        void findById_Failure_Unauthorized() {
            // Arrange: Use staff from a different clinic AND a different owner
            given(petRepository.findById(petId)).willReturn(Optional.of(pet));
            given(userRepository.findById(staffDifferentClinicId)).willReturn(Optional.of(staffFromDifferentClinic));

            // Act & Assert for unauthorized staff
            assertThatThrownBy(() -> petService.findPetById(petId, staffDifferentClinicId))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("is not authorized to view pet");

            // Arrange for different owner
            given(userRepository.findById(differentOwnerId)).willReturn(Optional.of(differentOwner));

            // Act & Assert for different owner
            assertThatThrownBy(() -> petService.findPetById(petId, differentOwnerId))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("is not authorized to view pet");


            then(petMapper).should(never()).toProfileDto(any());
        }
    }

    // --- Tests for findBreedsBySpecie ---
    @Nested
    @DisplayName("findBreedsBySpecie Tests")
    class FindBreedsBySpecieTests {

        private Breed catBreed1;
        private Breed catBreed2;
        private BreedDto catDto1;
        private BreedDto catDto2;

        @BeforeEach
        void findBreedsSetup() {
            catBreed1 = Breed.builder().id(201L).name("Siamese").specie(Specie.CAT).build();
            catBreed2 = Breed.builder().id(202L).name("Persian").specie(Specie.CAT).build();
            catDto1 = new BreedDto(catBreed1.getId(), catBreed1.getName());
            catDto2 = new BreedDto(catBreed2.getId(), catBreed2.getName());
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
            // Repository and mapper should not be called
            then(breedRepository).should(never()).findBySpecieOrderByNameAsc(any());
            then(breedMapper).should(never()).toDtoList(any());
        }
    }

    // --- Tests for associateVetWithPet ---
    @Nested
    @DisplayName("associateVetWithPet Tests")
    class AssociateVetTests {

        private Pet petToAssociate;
        private Vet vetToAssociate;
        private Vet alreadyAssociatedVet;
        private final Long petId = 300L;
        private final Long vetToAssociateId = 60L;
        private final Long alreadyAssociatedVetId = 61L;
        private final Long ownerId = 1L; // Use owner from main setup

        @BeforeEach
        void associateVetSetup() {
            Clinic clinic = Clinic.builder().build(); clinic.setId(1L); // Vet needs a clinic

            vetToAssociate = new Vet(); vetToAssociate.setId(vetToAssociateId); vetToAssociate.setClinic(clinic);
            alreadyAssociatedVet = new Vet(); alreadyAssociatedVet.setId(alreadyAssociatedVetId); alreadyAssociatedVet.setClinic(clinic);

            petToAssociate = new Pet();
            petToAssociate.setId(petId);
            petToAssociate.setOwner(owner); // Correct owner
            petToAssociate.setStatus(PetStatus.ACTIVE); // Assume active
            petToAssociate.setBreed(dogBreedSpecific);
            // Start with ONLY alreadyAssociatedVet in the set
            petToAssociate.getAssociatedVets().add(alreadyAssociatedVet);
        }

        /**
         * Test successful association of a new Vet.
         */
        @Test
        @DisplayName("should associate vet successfully when not already associated")
        void associate_Success() {
            // Arrange
            given(petRepository.findById(petId)).willReturn(Optional.of(petToAssociate));
            given(vetRepository.findById(vetToAssociateId)).willReturn(Optional.of(vetToAssociate));
            // No need to mock save unless verifying return (void method)

            // Act
            petService.associateVetWithPet(petId, vetToAssociateId, ownerId);

            // Assert
            then(petRepository).should().findById(petId);
            then(vetRepository).should().findById(vetToAssociateId);
            then(petRepository).should().save(petCaptor.capture());

            Pet saved = petCaptor.getValue();
            assertThat(saved.getAssociatedVets()).contains(vetToAssociate, alreadyAssociatedVet); // Verify both are present
            assertThat(saved.getStatus()).isEqualTo(PetStatus.ACTIVE); // Status shouldn't change
        }

        /**
         * Test successful association of a Vet to an INACTIVE pet, which should reactivate it.
         */
        @Test
        @DisplayName("should associate vet and reactivate pet if it was INACTIVE")
        void associate_ReactivatesInactivePet() {
            // Arrange
            petToAssociate.setStatus(PetStatus.INACTIVE); // Make pet inactive
            petToAssociate.getAssociatedVets().clear(); // Remove existing vets
            given(petRepository.findById(petId)).willReturn(Optional.of(petToAssociate));
            given(vetRepository.findById(vetToAssociateId)).willReturn(Optional.of(vetToAssociate));

            // Act
            petService.associateVetWithPet(petId, vetToAssociateId, ownerId);

            // Assert
            then(petRepository).should().save(petCaptor.capture());
            Pet saved = petCaptor.getValue();
            assertThat(saved.getAssociatedVets()).contains(vetToAssociate);
            assertThat(saved.getStatus()).isEqualTo(PetStatus.ACTIVE); // Verify status changed to ACTIVE
        }


        /**
         * Test failure when the association already exists.
         */
        @Test
        @DisplayName("should throw IllegalStateException if vet already associated")
        void associate_Failure_AlreadyAssociated() {
            // Arrange
            given(petRepository.findById(petId)).willReturn(Optional.of(petToAssociate));
            given(vetRepository.findById(alreadyAssociatedVetId)).willReturn(Optional.of(alreadyAssociatedVet)); // Vet to associate is already in the list

            // Act & Assert
            assertThatThrownBy(() -> petService.associateVetWithPet(petId, alreadyAssociatedVetId, ownerId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("is already associated with pet");

            then(petRepository).should(never()).save(any());
        }

        /**
         * Test failure when pet is not found.
         */
        @Test
        @DisplayName("should throw EntityNotFoundException if pet not found")
        void associate_Failure_PetNotFound() {
            // Arrange
            given(petRepository.findById(999L)).willReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> petService.associateVetWithPet(999L, vetToAssociateId, ownerId))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Pet not found with id: 999");
            then(vetRepository).should(never()).findById(anyLong());
            then(petRepository).should(never()).save(any());
        }

        /**
         * Test failure when vet is not found.
         */
        @Test
        @DisplayName("should throw EntityNotFoundException if vet not found")
        void associate_Failure_VetNotFound() {
            // Arrange
            Long nonExistentVetId = 888L;
            given(petRepository.findById(petId)).willReturn(Optional.of(petToAssociate));
            given(vetRepository.findById(nonExistentVetId)).willReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> petService.associateVetWithPet(petId, nonExistentVetId, ownerId))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Vet not found with id: " + nonExistentVetId);
            then(petRepository).should(never()).save(any());
        }

        /**
         * Test failure when user is not the owner.
         */
        @Test
        @DisplayName("should throw AccessDeniedException if user is not owner")
        void associate_Failure_NotOwner() {
            // Arrange
            Long otherOwnerId = 777L;
            given(petRepository.findById(petId)).willReturn(Optional.of(petToAssociate));

            // Act & Assert
            assertThatThrownBy(() -> petService.associateVetWithPet(petId, vetToAssociateId, otherOwnerId))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("User " + otherOwnerId + " is not the owner of pet " + petId);
            then(petRepository).should(never()).save(any());
        }
    }

    // --- Tests for disassociateVetFromPet ---
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
        private final Long ownerId = 1L; // Use owner from main setup

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
            petWithTwoVets.getAssociatedVets().add(vetToRemove);
            petWithTwoVets.getAssociatedVets().add(vetToKeep);

            petWithOneVet = new Pet();
            petWithOneVet.setId(petOneVetId);
            petWithOneVet.setOwner(owner);
            petWithOneVet.setStatus(PetStatus.ACTIVE);
            petWithOneVet.setBreed(dogBreedSpecific);
            petWithOneVet.getAssociatedVets().add(vetToRemove); // Only has the vet to remove
        }

        /**
         * Test successful disassociation when multiple vets are associated.
         * Pet status should remain ACTIVE.
         */
        @Test
        @DisplayName("should disassociate vet successfully when multiple vets exist")
        void disassociate_Success_MultipleVets() {
            // Arrange
            given(petRepository.findById(petTwoVetsId)).willReturn(Optional.of(petWithTwoVets));
            given(vetRepository.findById(vetToRemoveId)).willReturn(Optional.of(vetToRemove));

            // Act
            petService.disassociateVetFromPet(petTwoVetsId, vetToRemoveId, ownerId);

            // Assert
            then(petRepository).should().findById(petTwoVetsId);
            then(vetRepository).should().findById(vetToRemoveId);
            then(petRepository).should().save(petCaptor.capture());

            Pet saved = petCaptor.getValue();
            assertThat(saved.getAssociatedVets()).hasSize(1).containsExactly(vetToKeep); // Only vetToKeep remains
            assertThat(saved.getStatus()).isEqualTo(PetStatus.ACTIVE); // Status remains ACTIVE
        }

        /**
         * Test successful disassociation when it's the last vet associated.
         * Pet status should change to INACTIVE.
         */
        @Test
        @DisplayName("should disassociate last vet and set pet to INACTIVE")
        void disassociate_Success_LastVet() {
            // Arrange
            given(petRepository.findById(petOneVetId)).willReturn(Optional.of(petWithOneVet));
            given(vetRepository.findById(vetToRemoveId)).willReturn(Optional.of(vetToRemove));

            // Act
            petService.disassociateVetFromPet(petOneVetId, vetToRemoveId, ownerId);

            // Assert
            then(petRepository).should().findById(petOneVetId);
            then(vetRepository).should().findById(vetToRemoveId);
            then(petRepository).should().save(petCaptor.capture());

            Pet saved = petCaptor.getValue();
            assertThat(saved.getAssociatedVets()).isEmpty();
            assertThat(saved.getStatus()).isEqualTo(PetStatus.INACTIVE);
        }

        /**
         * Test the case where the vet to be disassociated is not actually associated.
         * Expects no changes and no save operation.
         */
        @Test
        @DisplayName("should do nothing if vet is not associated")
        void disassociate_NoOp_VetNotAssociated() {
            // Arrange
            Vet nonAssociatedVet = new Vet(); nonAssociatedVet.setId(999L);
            given(petRepository.findById(petTwoVetsId)).willReturn(Optional.of(petWithTwoVets));
            given(vetRepository.findById(999L)).willReturn(Optional.of(nonAssociatedVet));

            // Act
            petService.disassociateVetFromPet(petTwoVetsId, 999L, ownerId);

            // Assert
            then(petRepository).should().findById(petTwoVetsId);
            then(vetRepository).should().findById(999L);
            // Verify save was NOT called
            then(petRepository).should(never()).save(any());
            // Verify the original pet's associations were not modified (optional deep check)
            assertThat(petWithTwoVets.getAssociatedVets()).hasSize(2).contains(vetToRemove, vetToKeep);
        }


        /**
         * Test failure when pet is not found.
         */
        @Test
        @DisplayName("should throw EntityNotFoundException if pet not found")
        void disassociate_Failure_PetNotFound() {
            // Arrange
            given(petRepository.findById(999L)).willReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> petService.disassociateVetFromPet(999L, vetToRemoveId, ownerId))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Pet not found with id: 999");
            then(vetRepository).should(never()).findById(anyLong());
            then(petRepository).should(never()).save(any());
        }

        /**
         * Test failure when vet is not found.
         */
        @Test
        @DisplayName("should throw EntityNotFoundException if vet not found")
        void disassociate_Failure_VetNotFound() {
            // Arrange
            Long nonExistentVetId = 888L;
            given(petRepository.findById(petTwoVetsId)).willReturn(Optional.of(petWithTwoVets));
            given(vetRepository.findById(nonExistentVetId)).willReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> petService.disassociateVetFromPet(petTwoVetsId, nonExistentVetId, ownerId))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Vet not found with id: " + nonExistentVetId);
            then(petRepository).should(never()).save(any());
        }

        /**
         * Test failure when user is not the owner.
         */
        @Test
        @DisplayName("should throw AccessDeniedException if user is not owner")
        void disassociate_Failure_NotOwner() {
            // Arrange
            Long otherOwnerId = 777L;
            given(petRepository.findById(petTwoVetsId)).willReturn(Optional.of(petWithTwoVets));

            // Act & Assert
            assertThatThrownBy(() -> petService.disassociateVetFromPet(petTwoVetsId, vetToRemoveId, otherOwnerId))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("User " + otherOwnerId + " is not the owner of pet " + petTwoVetsId);
            then(petRepository).should(never()).save(any());
        }
    }
}
