import { Country } from './apiTypes'; 

/**
 * Summary information for a Veterinarian, including their avatar, email,
 * and essential details of their primary clinic.
 * Used when listing vets associated with a pet or for selection.
 */
export interface VetSummaryDto {
    id: number | string;
    name: string | null;
    surname: string | null;
    avatar: string | null;    
    email: string | null;   
    clinicId: number | string | null; 
    clinicName: string | null;
    clinicAddress: string | null; 
    clinicCity: string | null; 
    clinicCountry: string | null; 
    clinicPhone: string | null; 
}

/**
 * Data Transfer Object representing a Clinic's details.
 */
export interface ClinicDto {
    id:  number | string; 
    name: string;
    address: string;
    city: string;
    country: Country; 
    phone: string;
    publicKey?: string | null; 
}

/**
 * Payload for updating Clinic information.
 */
export interface ClinicUpdatePayload {
    name?: string | null;
    address?: string | null;
    city?: string | null;
    country?: Country | null;
    phone?: string | null;
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