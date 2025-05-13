import { UserProfile, ClinicStaffProfile, OwnerProfile, RecordType } from '@/types/apiTypes'; 

/**
 * Formats the display name of a record creator.
 * Shows "Name Surname (Clinic Name)" for staff, or "Username (Role)" for owners.
 * @param {UserProfile | undefined | null} creator - The creator profile object.
 * @returns {string} A formatted string for display.
 */
export const formatRecordCreatorDisplay = (creator: UserProfile | undefined | null): string => {
    if (!creator) return 'Unknown';

    const staffCandidate = creator as ClinicStaffProfile;

    if (typeof (staffCandidate as ClinicStaffProfile).name === 'string' &&
        typeof (staffCandidate as ClinicStaffProfile).surname === 'string'&&
        typeof staffCandidate.clinicName === 'string') { 

        let displayName = `${staffCandidate.name} ${staffCandidate.surname}`.trim();
        if (staffCandidate.clinicName) { 
            displayName += ` (${staffCandidate.clinicName})`;
        }
        return displayName || 'Clinic Staff'; 
    }
    else if (typeof creator.username === 'string') {
        const owner = creator as OwnerProfile;
        let displayName = owner.username;
        if (owner.roles && owner.roles.length > 0) {
            const formattedRoles = owner.roles.map(r => r.charAt(0).toUpperCase() + r.slice(1).toLowerCase()).join(', ');
            displayName += ` (${formattedRoles})`;
        }
        return displayName;
    }
    return 'User';
};

/**
 * Formats a date string into a locale-specific date and time string.
 * Example: "12 May 2023, 14:30"
 * @param {string | null | undefined} dateString - The ISO date string.
 * @returns {string} The formatted date-time string or 'N/A'.
 */
export const formatDateTime = (dateString: string | null | undefined): string => {
    if (!dateString) return 'N/A';
    try {
        return new Date(dateString).toLocaleString('en-GB', {
            day: '2-digit',
            month: 'short', 
            year: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });
    } catch {
        return 'Invalid Date';
    }
};

/**
 * Gets a display-friendly string for a RecordType enum.
 * Example: ANNUAL_CHECK -> "Annual Check"
 * @param {RecordType | undefined} type - The record type.
 * @returns {string} The formatted string or 'N/A'.
 */
export const getRecordTypeDisplay = (type: RecordType | undefined): string => {
    if (!type) return 'N/A';
    return type.replace(/_/g, " ").replace(/\b\w/g, (l) => l.toUpperCase());
};
