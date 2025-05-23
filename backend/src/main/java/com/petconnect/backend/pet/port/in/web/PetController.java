package com.petconnect.backend.pet.port.in.web;

import com.petconnect.backend.common.helper.UserHelper;
import com.petconnect.backend.pet.application.dto.*;
import com.petconnect.backend.pet.domain.model.PetStatus;
import com.petconnect.backend.pet.domain.model.Specie;
import com.petconnect.backend.pet.application.service.PetService;
import io.micrometer.common.lang.Nullable;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * Implementation of {@link PetControllerApi}.
 * Handles incoming HTTP requests for pet management and delegates to {@link PetService}.
 *
 * @author ibosquet
 */
@RestController
@RequestMapping("/api/pets")
@RequiredArgsConstructor
@Slf4j
public class PetController implements PetControllerApi{

    private final PetService petService;
    private final UserHelper userServiceHelper;

    /**
     * {@inheritDoc}
     */
    @Override
    @PostMapping(value = "", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PetProfileDto> registerPet(
            @RequestPart("dto") @Valid PetRegistrationDto registrationDTO,
            @RequestPart(value = "imageFile", required = false) @Nullable MultipartFile imageFile) throws IOException {
        Long ownerId = userServiceHelper.getAuthenticatedUserId();
        PetProfileDto createdPet = petService.registerPet(registrationDTO, ownerId, imageFile);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdPet);
    }

    /**
     * {@inheritDoc}
     */
    @GetMapping("")
    @Override
    public ResponseEntity<Page<PetProfileDto>> findMyPets(
            @RequestParam(required = false) List<PetStatus> statuses,
            @PageableDefault(sort = "name") Pageable pageable) {
        Long authenticatedUserId = userServiceHelper.getAuthenticatedUserId();
        Page<PetProfileDto> petPage = petService.findPetsByOwner(authenticatedUserId, statuses, pageable);
        return ResponseEntity.ok(petPage);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @PutMapping(value = "/{petId}/owner-update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PetProfileDto> updatePetByOwner(
            @PathVariable Long petId,
            @RequestPart("dto") @Valid PetOwnerUpdateDto updateDto,
            @RequestPart(value = "imageFile", required = false) @Nullable MultipartFile imageFile) throws IOException {
        Long ownerId = userServiceHelper.getAuthenticatedUserId();
        PetProfileDto updatedPet = petService.updatePetByOwner(petId, updateDto, ownerId, imageFile);
        return ResponseEntity.ok(updatedPet);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @PutMapping("/{petId}/deactivate")
    public ResponseEntity<PetProfileDto> deactivatePet(@PathVariable Long petId) {
        Long ownerId = userServiceHelper.getAuthenticatedUserId();
        PetProfileDto deactivatedPet = petService.deactivatePet(petId, ownerId);
        return ResponseEntity.ok(deactivatedPet);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @PostMapping("/{petId}/associate-clinic/{clinicId}")
    public ResponseEntity<Void> associatePetToClinicForActivation(
            @PathVariable Long petId,
            @PathVariable Long clinicId) {
        Long ownerId = userServiceHelper.getAuthenticatedUserId();
        petService.associatePetToClinicForActivation(petId, clinicId, ownerId);
        return ResponseEntity.noContent().build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @PostMapping("/{petId}/associate-vet/{vetId}")
    public ResponseEntity<Void> associateVetWithPet(
            @PathVariable Long petId,
            @PathVariable Long vetId) {
        Long ownerId = userServiceHelper.getAuthenticatedUserId();
        petService.associateVetWithPet(petId, vetId, ownerId);
        return ResponseEntity.noContent().build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @DeleteMapping("/{petId}/associate-vet/{vetId}")
    public ResponseEntity<Void> disassociateVetFromPet(
            @PathVariable Long petId,
            @PathVariable Long vetId) {
        Long ownerId = userServiceHelper.getAuthenticatedUserId();
        petService.disassociateVetFromPet(petId, vetId, ownerId);
        return ResponseEntity.noContent().build();
    }


    // --- Staff Operations ---

    /**
     * {@inheritDoc}
     */
    @Override
    @PutMapping("/{petId}/activate")
    public ResponseEntity<PetProfileDto> activatePet(
            @PathVariable Long petId,
            @Valid @RequestBody PetActivationDto activationDto) {
        Long staffId = userServiceHelper.getAuthenticatedUserId();
        PetProfileDto activatedPet = petService.activatePet(petId, activationDto, staffId);
        return ResponseEntity.ok(activatedPet);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @PutMapping("/{petId}/clinic-update")
    public ResponseEntity<PetProfileDto> updatePetByClinicStaff(
            @PathVariable Long petId,
            @Valid @RequestBody PetClinicUpdateDto updateDto) {
        Long staffId = userServiceHelper.getAuthenticatedUserId();
        PetProfileDto updatedPet = petService.updatePetByClinicStaff(petId, updateDto, staffId);
        return ResponseEntity.ok(updatedPet);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @GetMapping("/clinic")
    public ResponseEntity<Page<PetProfileDto>> findMyClinicPets(@PageableDefault(sort = "name") Pageable pageable) {
        Long requesterUserId = userServiceHelper.getAuthenticatedUserId();
        Page<PetProfileDto> petPage = petService.findPetsByClinic(requesterUserId, pageable);
        return ResponseEntity.ok(petPage);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @GetMapping("/clinic/pending")
    public ResponseEntity<List<PetProfileDto>> findMyClinicPendingPets() {
        Long requesterUserId = userServiceHelper.getAuthenticatedUserId(); // Solo ID
        List<PetProfileDto> pendingPets = petService.findPendingActivationPetsByClinic(requesterUserId);
        return ResponseEntity.ok(pendingPets);
    }

    // --- Retrieval Operations ---

    /**
     * {@inheritDoc}
     */
    @Override
    @GetMapping("/{petId}")
    public ResponseEntity<PetProfileDto> findPetById(@PathVariable Long petId) {
        Long requesterUserId = userServiceHelper.getAuthenticatedUserId();
        PetProfileDto petProfile = petService.findPetById(petId, requesterUserId);
        return ResponseEntity.ok(petProfile);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @GetMapping("/breeds/{specie}")
    public ResponseEntity<List<BreedDto>> findBreedsBySpecie(@PathVariable Specie specie) {
        List<BreedDto> breeds = petService.findBreedsBySpecie(specie);
        return ResponseEntity.ok(breeds);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @PostMapping("/{petId}/request-certificate/{clinicId}")
    public ResponseEntity<Void> requestCertificateGeneration(
            @PathVariable Long petId,
            @PathVariable Long clinicId) {
        Long ownerId = userServiceHelper.getAuthenticatedUserId();
        petService.requestCertificateGeneration(petId, clinicId, ownerId);
        return ResponseEntity.noContent().build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @GetMapping("/{clinicId}/pending-certificate-requests")
    public ResponseEntity<List<PetProfileDto>> getPetsWithPendingCertificateRequests(
            @PathVariable Long clinicId) {
        Long requesterStaffId = userServiceHelper.getAuthenticatedUserId();
        List<PetProfileDto> pets = petService.findPetsWithPendingCertRequestsForClinic(clinicId, requesterStaffId);
        return ResponseEntity.ok(pets);
    }
}
