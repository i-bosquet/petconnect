import { Gender, PetStatus, Specie } from './enumTypes';
import { VetSummaryDto } from './clinicTypes';

/**
 * Detailed profile information for a Pet.
 */
export interface PetProfileDto {
    /** The unique ID of the pet. */
    id: number | string;
    /** The pet's given name. */
    name: string;
    /** The species of the pet (DOG, CAT, ...). */
    specie: Specie;
    /** Description of the pet's color or coat. */
    color?: string | null;
    /** The gender of the pet. */
    gender?: Gender | null;
    /** The pet's date of birth (ISO string format). */
    birthDate?: string | null; 
    /** The pet's unique microchip number. */
    microchip?: string | null;
    /** The URL path to the pet's image. */
    image: string;
    /** The current status of the pet (PENDING, ACTIVE, INACTIVE). */
    status: PetStatus;
    /** The ID of the user who owns this pet. */
    ownerId: number | string;
    /** The username of the user who owns this pet. */
    ownerUsername: string;
    /** The ID of the pet's assigned breed. */
    breedId?: number | string | null;
    /** The name of the pet's assigned breed. */
    breedName?: string | null;
    /** If status is PENDING, the ID of the clinic awaiting activation. */
    pendingActivationClinicId?: number | string | null;
    /** A list of veterinarians associated with providing care for this pet. */
    associatedVets: VetSummaryDto[];
    /** Timestamp when the pet record was created (ISO string format) */
    createdAt?: string | null; 
    /** Timestamp when the pet record was last updated (ISO string format) */
    updatedAt?: string | null;
}

/**
 * Simplified view of a Breed, used for selection lists.
 */
export interface BreedDto {
    /** The unique ID of the breed. */
    id: number | string;
    /** The common name of the breed. */
    name: string;
    /** Optional URL to a representative image of the breed. */
    imageUrl?: string | null;
}

/**
 * Represents the data collected from the frontend form for registering a new pet.
 * This differs slightly from the backend DTO as it reflects form state.
 */
export interface PetRegistrationData { 
    /** The chosen name for the pet. */
    name: string;
    /** The species of the pet. */
    specie: Specie;
    /** The pet's date of birth, as a string. */
    birthDate: string;
    /** Optional ID of the selected breed (can be string like 'MIXED_OTHER' initially). */
    breedId?: number | string | null;
    /** Optional image data (File object for upload). */
    image?: File | null; // Keep as File or null for form handling
    /** Optional color description. */
    color?: string | null;
    /** Optional gender of the pet. */
    gender?: Gender | null;
    /** Optional microchip number. */
    microchip?: string | null;
  }

// --- DTOs mirroring backend payloads ---

/**
 * Payload for updating Pet info specifically by Clinic Staff.
 */
 export interface PetClinicUpdatePayload {
    color?: string | null;
    gender?: Gender | null;
    birthDate?: string | null; 
    microchip?: string | null;
    breedId?: number | string | null;
}

/**
 * Payload for updating Pet info specifically by the Owner.
 */
 export interface PetOwnerUpdatePayload {
    name?: string | null;
    // image is handled via multipart, not in this DTO payload
    color?: string | null;
    gender?: Gender | null;
    birthDate?: string | null; 
    microchip?: string | null;
    breedId?: number | string | null;
}

/**
 * DTO mirroring the backend API payload expectation for registering a pet.
 */
 export interface PetRegistrationPayload {
    name: string;
    specie: Specie;
    birthDate: string; 
    breedId?: number | string | null;
    // image path is set by backend, image File sent via multipart
    color?: string | null;
    gender?: Gender | null;
    microchip?: string | null;
}