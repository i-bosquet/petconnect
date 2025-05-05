package com.petconnect.backend.pet.application.mapper;

import com.petconnect.backend.common.helper.Utils;
import com.petconnect.backend.pet.application.dto.PetClinicUpdateDto;
import com.petconnect.backend.pet.application.dto.PetOwnerUpdateDto;
import com.petconnect.backend.pet.application.dto.PetProfileDto;
import com.petconnect.backend.pet.domain.model.Breed;
import com.petconnect.backend.pet.domain.model.Pet;
import com.petconnect.backend.pet.domain.model.Specie;
import com.petconnect.backend.user.application.dto.VetSummaryDto;
import com.petconnect.backend.user.application.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Mapper component for converting between {@link Pet} entities and related DTOs.
 * Handles mapping for registration, profile views, and updates.
 *
 * @author ibosquet
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PetMapper {

    private static final String FIELD_NAME = "name";
    private static final String FIELD_COLOR = "color";
    private static final String FIELD_GENDER = "gender";
    private static final String FIELD_BIRTH_DATE = "birthDate";
    private static final String FIELD_MICROCHIP = "microchip";
    private static final String FIELD_BREED = "breed";

    private final UserMapper userMapper;

    @Value("${app.backend.base-url}")
    private String backendBaseUrl;

    @Value("${app.default.pet.image.path}")
    private String defaultImageDbPrefix;

    // --- Default image URL prefix ---
    private static final String DEFAULT_IMAGE_URL_PREFIX = "/images/avatars/pets/";
    // --- Prefix for uploaded image URLs (from external storage) ---
    private static final String UPLOADED_IMAGE_URL_PREFIX = "/storage/pets/avatars/";

    /**
     * Converts a {@link Pet} entity to a detailed {@link PetProfileDto}.
     * Includes information from related Owners, Breed entities, and Associated Vets.
     * Returns null if the input entity is null.
     *
     * @param pet The Pet entity to convert.
     * @return The corresponding PetProfileDto, or null if the input was null.
     */
    public PetProfileDto toProfileDto(Pet pet) {
        if (pet == null) {
            return null;
        }

        Long ownerId = (pet.getOwner() != null) ? pet.getOwner().getId() : null;
        String ownerUsername = (pet.getOwner() != null) ? pet.getOwner().getUsername() : null;
        Long breedId = pet.getBreed().getId();
        String breedName = pet.getBreed().getName();
        Specie specie = pet.getBreed().getSpecie();
        Long pendingClinicId = (pet.getPendingActivationClinic() != null) ? pet.getPendingActivationClinic().getId() : null;
        Set<VetSummaryDto> vetSummaries = userMapper.toVetSummaryDtoSet(pet.getAssociatedVets());

        String fullImageUrl = null;
        if (StringUtils.hasText(pet.getImage())) {
            String imagePathInDb = pet.getImage();
            String baseUrl = backendBaseUrl.endsWith("/") ? backendBaseUrl : backendBaseUrl + "/";
            String relativePathForUrl;

            // Decide which URL prefix to use based on how the saved path starts
            if (imagePathInDb.startsWith(defaultImageDbPrefix)) {
                // It is a default image, use prefix /images/
                relativePathForUrl = DEFAULT_IMAGE_URL_PREFIX + imagePathInDb.substring(defaultImageDbPrefix.length());
                log.trace("Mapping default image path '{}' to URL prefix '{}'", imagePathInDb, DEFAULT_IMAGE_URL_PREFIX);
            } else {
                // It is an uploaded image, use prefix /storage/
                relativePathForUrl = UPLOADED_IMAGE_URL_PREFIX + imagePathInDb.substring("pets/avatars/".length());
                log.trace("Mapping uploaded image path '{}' to URL prefix '{}'", imagePathInDb, UPLOADED_IMAGE_URL_PREFIX);
            }
            relativePathForUrl = relativePathForUrl.startsWith("/") ? relativePathForUrl.substring(1) : relativePathForUrl;
            fullImageUrl = baseUrl + relativePathForUrl;
        } else {
            log.warn("Pet ID {} has null or empty image path in database.", pet.getId());
        }

        return new PetProfileDto(
                pet.getId(),
                pet.getName(),
                specie,
                pet.getColor(),
                pet.getGender(),
                pet.getBirthDate(),
                pet.getMicrochip(),
                fullImageUrl,
                pet.getStatus(),
                ownerId,
                ownerUsername,
                breedId,
                breedName,
                pendingClinicId,
                vetSummaries
        );
    }

    /**
     * Converts a list of {@link Pet} entities to an unmodifiable list of {@link PetProfileDto}.
     * Returns an empty list if the input list is null or empty.
     *
     * @param pets The list of Pet entities.
     * @return An unmodifiable list of corresponding PetProfileDto objects.
     */
    public List<PetProfileDto> toProfileDtoList(List<Pet> pets) {
        if (pets == null || pets.isEmpty()) {
            return Collections.emptyList();
        }
        return pets.stream()
                .map(this::toProfileDto)
                .toList();
    }

    /**
     * Updates an existing {@link Pet} entity from a {@link PetOwnerUpdateDto} (Placeholder).
     * This method would apply changes allowed for an owner (e.g., name, image).
     *
     * @param dto The DTO containing owner-updatable fields.
     * @param pet The Pet entity to update.
     */
     public boolean updateFromOwnerDto(PetOwnerUpdateDto dto, Pet pet, Breed resolvedBreed) {
         if (dto == null || pet == null) return false;
         boolean changed = false;
         log.debug("Mapper updateFromOwnerDto - Checking 'name': DTO='{}', Current='{}'", dto.name(), pet.getName());
         changed |= Utils.updateStringFieldIfChanged(pet, dto.name(), pet::getName, Pet::setName, FIELD_NAME);
         log.debug("Mapper updateFromOwnerDto - Checking 'color': DTO='{}', Current='{}'", dto.color(), pet.getColor());
         changed |= Utils.updateStringFieldIfChanged(pet, dto.color(), pet::getColor, Pet::setColor, FIELD_COLOR);
         log.debug("Mapper updateFromOwnerDto - Checking 'microchip': DTO='{}', Current='{}'", dto.microchip(), pet.getMicrochip());
         changed |= Utils.updateStringFieldIfChanged(pet, dto.microchip(), pet::getMicrochip, Pet::setMicrochip, FIELD_MICROCHIP);
         log.debug("Mapper updateFromOwnerDto - Checking 'gender': DTO='{}', Current='{}'", dto.gender(), pet.getGender());
         changed |= Utils.updateFieldIfChanged(pet, dto.gender(), pet::getGender, Pet::setGender, FIELD_GENDER);
         log.debug("Mapper updateFromOwnerDto - Checking 'birthDate': DTO='{}', Current='{}'", dto.birthDate(), pet.getBirthDate());
         changed |= Utils.updateFieldIfChanged(pet, dto.birthDate(), pet::getBirthDate, Pet::setBirthDate, FIELD_BIRTH_DATE);

         if (resolvedBreed != null) {
             log.debug("Mapper updateFromOwnerDto - Checking 'breed': ResolvedBreedId='{}', CurrentBreedId='{}'",
                     resolvedBreed.getId(), (pet.getBreed() != null ? pet.getBreed().getId() : null));
             changed |= Utils.updateFieldIfChanged(pet, resolvedBreed, pet::getBreed, Pet::setBreed, FIELD_BREED);
         }
         return changed;
     }

    /**
     * Updates an existing {@link Pet} entity from a {@link PetClinicUpdateDto}.
     * Applies changes allowed for clinic staff (microchip, birthDate, gender, color, breed).
     * Checks if values are provided and different from existing ones.
     * Returns true if any changes were applied, false otherwise.
     *
     * @param dto The DTO containing clinic-updatable fields.
     * @param pet The Pet entity to update.
     * @param resolvedBreed The Breed entity corresponding to dto.breedId(), resolved by the service (can be null).
     * @return true if the entity was modified, false otherwise.
     */
    public boolean updateFromClinicDto(PetClinicUpdateDto dto, Pet pet, Breed resolvedBreed) {
        if (dto == null || pet == null) return false;
        boolean changed = false;
        changed |= Utils.updateStringFieldIfChanged(pet, dto.color(), pet::getColor, Pet::setColor, FIELD_COLOR);
        changed |= Utils.updateStringFieldIfChanged(pet, dto.microchip(), pet::getMicrochip, Pet::setMicrochip, FIELD_MICROCHIP);
        changed |= Utils.updateFieldIfChanged(pet, dto.gender(), pet::getGender, Pet::setGender, FIELD_GENDER);
        changed |= Utils.updateFieldIfChanged(pet, dto.birthDate(), pet::getBirthDate, Pet::setBirthDate, FIELD_BIRTH_DATE);
        changed |= Utils.updateFieldIfChanged(pet, resolvedBreed, pet::getBreed, Pet::setBreed, FIELD_BREED);
        return changed;
    }
}
