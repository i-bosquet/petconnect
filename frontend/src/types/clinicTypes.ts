import { Country } from './enumTypes'; 
export type { ClinicStaffProfile } from './authTypes'; 

/**
 * Summary information for a Veterinarian, often used when listing keys.vets associated with a Pet or Clinic.
 */
export interface VetSummaryDto {
    id: number | string;
    name: string | null; 
    surname: string | null; 
}


/**
 * Data Transfer Object representing a Clinic's details.
 */
export interface ClinicDto {
    id: number;
    name: string;
    address: string;
    city: string;
    country: Country; 
    phone: string;
}

/**
 * Represents the payload required to create a new clinic staff member.
 */
export interface ClinicStaffCreationPayload {
    username: string;
    email: string;
    password: string;
    name: string;
    surname: string;
    role: 'VET' | 'ADMIN';
    licenseNumber?: string | null;
    vetPublicKey?: string | null;
}

/**
 * Payload for updating Clinic Staff information by an Admin.
 * Matches ClinicStaffUpdateDto from backend excluding vetPublicKey (handled by file).
 */
export interface ClinicStaffUpdatePayload {
    name?: string | null;
    surname?: string | null;
    roles?: string[] | null;
    licenseNumber?: string | null; // Only VET
}