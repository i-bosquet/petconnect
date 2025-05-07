import { Country } from './enumTypes'; 
export type { ClinicStaffProfile } from './authTypes'; 

/**
 * Summary information for a Veterinarian, often used when listing keys.vets associated with a Pet or Clinic.
 */
export interface VetSummaryDto {
    /** The unique ID of the Vet user. */
    id: number | string;
    /** The Vet's first name. */
    name: string | null; 
    /** The Vet's surname. */
    surname: string | null; 
}


/**
 * Data Transfer Object representing a Clinic's details.
 */
export interface ClinicDto {
    /** The unique ID of the clinic. */
    id: number;
    /** The official name of the clinic. */
    name: string;
    /** The full street address of the clinic. */
    address: string;
    /** The city where the clinic is located. */
    city: string;
    /** The country where the clinic is located. */
    country: Country; 
    /** The clinic's primary contact phone number. */
    phone: string;
    /* Note: publicKey is excluded as it's likely not needed often in UI lists/summaries */
}

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