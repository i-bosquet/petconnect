package com.petconnect.backend.user.domain.model;

/**
 * Enumeration representing the different roles a user can have within the application.
 * @author ibosquet
 */
public enum RoleEnum {
    /**
     * Represents a pet owner user.
     */
    OWNER,

    /**
     * Represents a veterinarian user, associated with a clinic.
     */
    VET,

    /**
     * Represents an administrative staff user of a clinic, can manage clinic staff.
     */
    ADMIN,

    /**
     * Represents a system superuser (typically for initial setup or system maintenance).
     * Note: This role might be managed outside the regular user management flow.
     */
    SUPERUSER
}
