import { Gender, PetStatus, Specie } from './enumTypes';
import { VetSummaryDto } from './clinicTypes';

/**
 * Detailed profile information for a Pet.
 */
export interface PetProfileDto {
    id: number | string;
    name: string;
    specie: Specie;
    color?: string | null;
    gender?: Gender | null;
    birthDate?: string | null; 
    microchip?: string | null;
    image: string;
    status: PetStatus;
    ownerId: number | string;
    ownerUsername: string;
    breedId?: number | string | null;
    breedName?: string | null;
    pendingActivationClinicId?: number | string | null;
    pendingActivationClinicName?: string | null;
    associatedVets: VetSummaryDto[];
    createdAt?: string | null; 
    updatedAt?: string | null;
}

/**
 * Simplified view of a Breed, used for selection lists.
 */
export interface BreedDto {
    id: number | string;
    name: string;
    imageUrl?: string | null;
}

/**
 * Represents the data collected from the frontend form for registering a new pet.
 * This differs slightly from the backend DTO as it reflects form state.
 */
export interface PetRegistrationData { 
    name: string;
    specie: Specie;
    birthDate: string;
    breedId?: number | string | null;
    image?: File | null; 
    color?: string | null;
    gender?: Gender | null;
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

/**
 * Data Transfer Object used by clinic staff to activate a pet.
 * Must contain all required fields for an active pet.
 */
export interface PetActivationDto {
    color: string; 
    gender: Gender; 
    birthDate: string;
    microchip: string;
    breedId: number; 
}