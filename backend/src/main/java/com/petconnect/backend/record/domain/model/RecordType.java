package com.petconnect.backend.record.domain.model;

/**
 * Enumeration representing the different types of records that can be added
 * to a pet's medical history.
 *
 * @author ibosquet
 */
public enum RecordType {
    /** Represents the initial registration visit or check-up. */
    FIRST_VISIT,

    /** Represents a routine annual check-up or vaccination update visit. */
    ANNUAL_CHECK,

    /** Represents a record specifically detailing a vaccination event. */
    VACCINE,

    /** Represents a visit due to a specific illness or symptoms. */
    ILLNESS,

    /** Represents an emergency visit or urgent care event. */
    URGENCY,

    /** Represents any other type of record not covered by the specific types. */
    OTHER
}
