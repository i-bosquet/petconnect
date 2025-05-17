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

export const getAhcValidityInfo = (
    issueDateStr: string,
    petLastEuEntryDateStr?: string | null,
    certificateTravelExpiryDateStr?: string | null 
): {
    entryEuExpiry: string;
    travelEuExpiry: string; 
    isStillValidForEntry: boolean;
    isCurrentlyValidForEuTravel: boolean;
    overallStatus: 'VALID_FOR_TRAVEL' | 'VALID_FOR_ENTRY' | 'EXPIRED' | 'UNKNOWN';
    statusMessage: string;
} => {
    const issueDate = new Date(issueDateStr);
    issueDate.setHours(0, 0, 0, 0);

    const entryEuExpiryDate = new Date(issueDate);
    entryEuExpiryDate.setDate(issueDate.getDate() + 10);
    const travelEuExpiryDateFromCert = certificateTravelExpiryDateStr ? new Date(certificateTravelExpiryDateStr) : null;
    if (travelEuExpiryDateFromCert) travelEuExpiryDateFromCert.setHours(0,0,0,0);


    const today = new Date();
    today.setHours(0, 0, 0, 0);

    const isStillValidForEntry = today <= entryEuExpiryDate;
    let isCurrentlyValidForEuTravel = false;
    let statusMessage = `Valid for EU entry until ${entryEuExpiryDate.toLocaleDateString('en-GB')}.`;
    let overallStatus: 'VALID_FOR_TRAVEL' | 'VALID_FOR_ENTRY' | 'EXPIRED' | 'UNKNOWN' = 'UNKNOWN';

    const petEuEntryDate = petLastEuEntryDateStr ? new Date(petLastEuEntryDateStr) : null;
    if (petEuEntryDate) petEuEntryDate.setHours(0, 0, 0, 0);

    if (petEuEntryDate && petEuEntryDate >= issueDate && petEuEntryDate <= entryEuExpiryDate) {
        if (travelEuExpiryDateFromCert && today <= travelEuExpiryDateFromCert) {
            isCurrentlyValidForEuTravel = true;
            statusMessage = `Active for EU travel. Valid until ${travelEuExpiryDateFromCert.toLocaleDateString('en-GB')}.`;
            overallStatus = 'VALID_FOR_TRAVEL';
        } else if (travelEuExpiryDateFromCert) {
            statusMessage = `EU travel period expired on ${travelEuExpiryDateFromCert.toLocaleDateString('en-GB')}.`;
            overallStatus = 'EXPIRED';
        } else { 
            statusMessage = `EU travel period status unclear (missing travel expiry date from certificate).`;
            overallStatus = 'EXPIRED'; 
        }
    } else if (isStillValidForEntry) {
        statusMessage = `Valid for EU entry until ${entryEuExpiryDate.toLocaleDateString('en-GB')}. Register entry to activate 4-month travel period.`;
        overallStatus = 'VALID_FOR_ENTRY';
    } else {
        statusMessage = `Expired for EU entry (was valid until ${entryEuExpiryDate.toLocaleDateString('en-GB')}).`;
        overallStatus = 'EXPIRED';
    }

    return {
        entryEuExpiry: entryEuExpiryDate.toLocaleDateString('en-GB'),
        travelEuExpiry: travelEuExpiryDateFromCert ? travelEuExpiryDateFromCert.toLocaleDateString('en-GB') : 'N/A',
        isStillValidForEntry,
        isCurrentlyValidForEuTravel,
        overallStatus,
        statusMessage,
    };
};
