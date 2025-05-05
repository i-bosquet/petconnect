package com.petconnect.backend.common.helper;

import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * General utility methods for common tasks.
 * Provides helpers for conditional updates of entity fields.
 * This class cannot be instantiated.
 *
 * @author ibosquet
 */
@Slf4j
public class Utils {

    /**
     * Private constructor to prevent instantiation.
     */
    private Utils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
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
     * Updates a target String field using a setter if the source value is not null or blank
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

        // Treat blank source string as null for comparison and setting
        String effectiveSourceValue = sourceValue.isBlank() ? null : sourceValue;
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
