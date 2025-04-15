package com.petconnect.backend.pet.application.mapper;

import com.petconnect.backend.pet.application.dto.PetActivationDto;
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
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

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
    private static final String FIELD_IMAGE = "image";
    private static final String FIELD_COLOR = "color";
    private static final String FIELD_GENDER = "gender";
    private static final String FIELD_BIRTH_DATE = "birthDate";
    private static final String FIELD_MICROCHIP = "microchip";
    private static final String FIELD_BREED = "breed";

    private final UserMapper userMapper;

    /**
     * Converts a {@link Pet} entity to a detailed {@link PetProfileDto}.
     * Includes information from related Owner, Breed entities and Associated Vets. // Javadoc Actualizado
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

        return new PetProfileDto(
                pet.getId(),
                pet.getName(),
                specie,
                pet.getColor(),
                pet.getGender(),
                pet.getBirthDate(),
                pet.getMicrochip(),
                pet.getImage(),
                pet.getStatus(),
                ownerId,
                ownerUsername,
                breedId,
                breedName,
                pendingClinicId,
                vetSummaries, // Añadir el set mapeado
                pet.getCreatedAt(),
                pet.getUpdatedAt()
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
         changed |= updateStringFieldIfChanged(pet, dto.name(), pet::getName, Pet::setName, FIELD_NAME);
         log.debug("Mapper updateFromOwnerDto - Checking 'image': DTO='{}', Current='{}'", dto.image(), pet.getImage());
         changed |= updateStringFieldIfChanged(pet, dto.image(), pet::getImage, Pet::setImage, FIELD_IMAGE);
         log.debug("Mapper updateFromOwnerDto - Checking 'color': DTO='{}', Current='{}'", dto.color(), pet.getColor());
         changed |= updateStringFieldIfChanged(pet, dto.color(), pet::getColor, Pet::setColor, FIELD_COLOR);
         log.debug("Mapper updateFromOwnerDto - Checking 'microchip': DTO='{}', Current='{}'", dto.microchip(), pet.getMicrochip());
         changed |= updateStringFieldIfChanged(pet, dto.microchip(), pet::getMicrochip, Pet::setMicrochip, FIELD_MICROCHIP);
         log.debug("Mapper updateFromOwnerDto - Checking 'gender': DTO='{}', Current='{}'", dto.gender(), pet.getGender());
         changed |= updateFieldIfChanged(pet, dto.gender(), pet::getGender, Pet::setGender, FIELD_GENDER);
         log.debug("Mapper updateFromOwnerDto - Checking 'birthDate': DTO='{}', Current='{}'", dto.birthDate(), pet.getBirthDate());
         changed |= updateFieldIfChanged(pet, dto.birthDate(), pet::getBirthDate, Pet::setBirthDate, FIELD_BIRTH_DATE);
         // Update breed only if resolvedBreed is not null AND different from current
         if (resolvedBreed != null) {
             log.debug("Mapper updateFromOwnerDto - Checking 'breed': ResolvedBreedId='{}', CurrentBreedId='{}'",
                     resolvedBreed.getId(), (pet.getBreed() != null ? pet.getBreed().getId() : null));
             changed |= updateFieldIfChanged(pet, resolvedBreed, pet::getBreed, Pet::setBreed, FIELD_BREED);
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
        changed |= updateStringFieldIfChanged(pet, dto.color(), pet::getColor, Pet::setColor, FIELD_COLOR);
        changed |= updateStringFieldIfChanged(pet, dto.microchip(), pet::getMicrochip, Pet::setMicrochip, FIELD_MICROCHIP);
        changed |= updateFieldIfChanged(pet, dto.gender(), pet::getGender, Pet::setGender, FIELD_GENDER);
        changed |= updateFieldIfChanged(pet, dto.birthDate(), pet::getBirthDate, Pet::setBirthDate, FIELD_BIRTH_DATE);
        changed |= updateFieldIfChanged(pet, resolvedBreed, pet::getBreed, Pet::setBreed, FIELD_BREED);
        return changed;
    }

    /**
     * Applies updates required during Pet activation using {@link PetActivationDto}.
     * Sets clinical fields and potentially breed.
     * This assumes the service layer has already validated the input DTO fields.
     * Returns true if any field was actually modified (useful for logging/auditing).
     *
     * @param dto The activation DTO containing verified/updated data.
     * @param petToActivate The Pet entity (in PENDING state) to be updated and activated.
     * @param resolvedBreed The Breed entity corresponding to dto.breedId(), resolved by the service (can be null).
     * @return true if the entity was modified, false otherwise.
     */
    public boolean applyActivationData(PetActivationDto dto, Pet petToActivate, Breed resolvedBreed) {
        if (dto == null || petToActivate == null) return false;
        boolean changed = false;
        changed |= updateStringFieldIfChanged(petToActivate, dto.microchip(), petToActivate::getMicrochip, Pet::setMicrochip, FIELD_MICROCHIP);
        changed |= updateFieldIfChanged(petToActivate, dto.birthDate(), petToActivate::getBirthDate, Pet::setBirthDate, FIELD_BIRTH_DATE);
        changed |= updateFieldIfChanged(petToActivate, dto.gender(), petToActivate::getGender, Pet::setGender, FIELD_GENDER);
        changed |= updateStringFieldIfChanged(petToActivate, dto.color(), petToActivate::getColor, Pet::setColor, FIELD_COLOR);
        changed |= updateFieldIfChanged(petToActivate, resolvedBreed, petToActivate::getBreed, Pet::setBreed, FIELD_BREED);
        return changed;
    }

    // Helpers

    /**
     * Updates a target field using a setter if the source value is not null
     * and different from the current value obtained via a getter.
     * Specifically for updating fields on a target object.
     *
     * @param target The target object (e.g., the Pet entity).
     * @param sourceValue The new value from the DTO (can be null).
     * @param getter Supplier function to get the current value from the entity.
     * @param setter BiConsumer function to set the new value on the entity (accepts target and value).
     * @param <E> The type of the target entity.
     * @param <T> The type of the field being updated.
     * @return true if the setter was called (value was updated), false otherwise.
     */
    public static <E, T> boolean updateFieldIfChanged(E target, T sourceValue, Supplier<T> getter, BiConsumer<E, T> setter, String fieldName) {
        if (sourceValue == null) {
            log.debug("Skipping field '{}': Source value is null.", fieldName);
            return false;
        }
        T currentValue = getter.get();
        if (!Objects.equals(sourceValue, currentValue)) {
            log.debug("Updating field '{}': Current='{}', New='{}'", fieldName, currentValue, sourceValue);
            setter.accept(target, sourceValue);
            return true;
        } else {
            log.debug("Skipping field '{}': Value '{}' is the same as current.", fieldName, sourceValue);
            return false;
        }
    }

    /**
     * Updates a target String field using a setter if the source value is not null or blank,
     * and different from the current value obtained via a getter.
     * Handles blank strings by setting the target field to null.
     *
     * @param target The target object (e.g., the Pet entity).
     * @param sourceValue The new String value from the DTO (can be null or blank).
     * @param getter Supplier function to get the current String value from the entity.
     * @param setter BiConsumer function to set the new String value on the entity.
     * @param <E> The type of the target entity.
     * @return true if the setter was called (value was updated), false otherwise.
     */
    public static <E> boolean updateStringFieldIfChanged(E target, String sourceValue, Supplier<String> getter, BiConsumer<E, String> setter, String fieldName) {
        if (sourceValue == null) {
            log.debug("Field '{}' not updated: Source value is null.", fieldName);
            return false;
        }

        String effectiveSourceValue = sourceValue.isBlank() ? null : sourceValue; // Simplificado
        String currentValue = getter.get();

        if (!Objects.equals(effectiveSourceValue, currentValue)) {
            log.debug("Updating field '{}': Current='{}', New='{}' (Effective='{}')", fieldName, currentValue, sourceValue, effectiveSourceValue);
            setter.accept(target, effectiveSourceValue);
            return true;
        } else {
            log.debug("Skipping field '{}': Effective value '{}' is the same as current '{}'.", fieldName, effectiveSourceValue, currentValue);
            return false;
        }
    }
}
