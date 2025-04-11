package com.petconnect.backend.pet.application.mapper;

import com.petconnect.backend.pet.application.dto.PetActivationDto;
import com.petconnect.backend.pet.application.dto.PetClinicUpdateDto;
import com.petconnect.backend.pet.application.dto.PetOwnerUpdateDto;
import com.petconnect.backend.pet.application.dto.PetProfileDto;
import com.petconnect.backend.pet.domain.model.Breed;
import com.petconnect.backend.pet.domain.model.Pet;
import com.petconnect.backend.pet.domain.model.Specie;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * Mapper component for converting between {@link Pet} entities and related DTOs.
 * Handles mapping for registration, profile views, and updates.
 *
 * @author ibosquet
 */
@Component
public class PetMapper {
    /**
     * Converts a {@link Pet} entity to a detailed {@link PetProfileDto}.
     * Includes information from related Owner and Breed entities.
     * Returns null if the input entity is null.
     *
     * @param pet The Pet entity to convert.
     * @return The corresponding PetProfileDto, or null if the input was null.
     */
    public PetProfileDto toProfileDto(Pet pet) {
        if (pet == null) {
            return null;
        }

        // Extract owner info safely
        Long ownerId = (pet.getOwner() != null) ? pet.getOwner().getId() : null;
        String ownerUsername = (pet.getOwner() != null) ? pet.getOwner().getUsername() : null;

        // Extract breed info (Breed is @NotNull on Pet entity)
        Long breedId = pet.getBreed().getId();
        String breedName = pet.getBreed().getName();
        Specie specie = pet.getBreed().getSpecie();  // Get species from Breed

        // Extract pending clinic ID safely
        Long pendingClinicId = (pet.getPendingActivationClinic() != null) ? pet.getPendingActivationClinic().getId() : null;

        // Construct the DTO
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
         changed |= updateStringFieldIfChanged(pet, dto.name(), pet::getName, Pet::setName);
         changed |= updateStringFieldIfChanged(pet, dto.image(), pet::getImage, Pet::setImage);
         changed |= updateStringFieldIfChanged(pet, dto.color(), pet::getColor, Pet::setColor);
         changed |= updateStringFieldIfChanged(pet, dto.microchip(), pet::getMicrochip, Pet::setMicrochip);
         changed |= updateFieldIfChanged(pet, dto.gender(), pet::getGender, Pet::setGender);
         changed |= updateFieldIfChanged(pet, dto.birthDate(), pet::getBirthDate, Pet::setBirthDate);
         // Update breed only if resolvedBreed is not null AND different from current
         if (resolvedBreed != null) {
             changed |= updateFieldIfChanged(pet, resolvedBreed, pet::getBreed, Pet::setBreed);
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
        changed |= updateStringFieldIfChanged(pet, dto.color(), pet::getColor, Pet::setColor);
        changed |= updateStringFieldIfChanged(pet, dto.microchip(), pet::getMicrochip, Pet::setMicrochip);
        changed |= updateFieldIfChanged(pet, dto.gender(), pet::getGender, Pet::setGender);
        changed |= updateFieldIfChanged(pet, dto.birthDate(), pet::getBirthDate, Pet::setBirthDate);
        changed |= updateFieldIfChanged(pet, resolvedBreed, pet::getBreed, Pet::setBreed);
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
        changed |= updateStringFieldIfChanged(petToActivate, dto.microchip(), petToActivate::getMicrochip, Pet::setMicrochip);
        changed |= updateFieldIfChanged(petToActivate, dto.birthDate(), petToActivate::getBirthDate, Pet::setBirthDate);
        changed |= updateFieldIfChanged(petToActivate, dto.gender(), petToActivate::getGender, Pet::setGender);
        changed |= updateStringFieldIfChanged(petToActivate, dto.color(), petToActivate::getColor, Pet::setColor);
        changed |= updateFieldIfChanged(petToActivate, resolvedBreed, petToActivate::getBreed, Pet::setBreed);
        return changed;
    }

    // Helpers
    /**
     * Updates a target field using a setter if the source value is not null
     * and different from the current value obtained via a getter.
     *
     * @param sourceValue The new value from the DTO (can be null).
     * @param getter Supplier function to get the current value from the entity.
     * @param setter BiConsumer function to set the new value on the entity.
     * @param <T> The type of the field being updated.
     * @return true if the setter was called (value was updated), false otherwise.
     */
    public static <T> boolean updateIfChanged(T sourceValue, Supplier<T> getter, BiConsumer<T, T> setter) {
        if (sourceValue != null && !Objects.equals(sourceValue, getter.get())) {
            setter.accept(null, sourceValue); // Passing null as first arg, setter should ignore it
            return true;
        }
        return false;
    }

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
    public static <E, T> boolean updateFieldIfChanged(E target, T sourceValue, Supplier<T> getter, BiConsumer<E, T> setter) {
        if (sourceValue != null && !Objects.equals(sourceValue, getter.get())) {
            setter.accept(target, sourceValue); // Call the setter with target and new value
            return true;
        }
        return false;
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
    public static <E> boolean updateStringFieldIfChanged(E target, String sourceValue, Supplier<String> getter, BiConsumer<E, String> setter) {
        // Treat blank input string as intent to clear (set to null), unless already null
        String effectiveSourceValue = (sourceValue != null && sourceValue.isBlank()) ? null : sourceValue;
        // Update only if the effective source value is different from the current value
        if (!Objects.equals(effectiveSourceValue, getter.get())) {
            setter.accept(target, effectiveSourceValue);
            return true;
        }
        return false;
    }
}
