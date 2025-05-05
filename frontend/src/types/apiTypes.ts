import { Gender, PetStatus, Specie } from './enumTypes'; 

// --- General ---
export interface ApiErrorResponse {
    timestamp: number;
    status: number;
    error: string;
    message: string | Record<string, string>;
    path: string;
}

interface Sort {
    sorted: boolean;
    unsorted: boolean;
    empty: boolean;
}

interface Pageable {
    pageNumber: number;
    pageSize: number;
    sort: Sort; 
    offset: number;
    paged: boolean;
    unpaged: boolean;
}

export interface Page<T> {
    content: T[];
    pageable: Pageable; 
    totalPages: number;
    totalElements: number;
    last: boolean;
    size: number;
    number: number; 
    sort: Sort; 
    numberOfElements: number;
    first: boolean;
    empty: boolean;
}

// --- Auth ---
export interface AuthResponse {
  username: string;
  message: string;
  jwt: string;
  status: boolean;
}

export interface OwnerProfile {
  id: number | string;
  username: string;
  email: string;
  roles: string[];
  avatar: string;
  phone: string;
}

export interface ClinicStaffProfile {
    id: number | string;
    username: string;
    email: string;
    roles: string[];
    avatar: string;
    name: string;
    surname: string;
    isActive: boolean;
    clinicId: number | string;
    clinicName: string;
    licenseNumber?: string | null; 
    vetPublicKey?: string | null; 
}

export type UserProfile = OwnerProfile | ClinicStaffProfile; 

// --- Clinic ---
export interface ClinicDto {
    id: number;
    name: string;
    address: string;
    city: string;
    country: string; 
    phone: string;
}

// --- Pet / Breed ---
export interface VetSummaryDto {
    id: number | string;
    name: string | null; 
    surname: string | null; 
}

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
    associatedVets: VetSummaryDto[]; 
    createdAt?: string | null; 
    updatedAt?: string | null;
}

export interface BreedDto {
    id: number | string;
    name: string;
    imageUrl?: string | null;
}

export interface PetRegistrationData {
    name: string;
    specie: Specie;
    birthDate: string; 
    breedId?: number | string | null;
    image?: string | null;
    color?: string | null;
    gender?: Gender | null;
    microchip?: string | null;
  }

// --- Record / Vaccine ---
export interface RecordSummaryDto {
    id: number | string;
    type: string; 
    description?: string | null;
    createdAt?: string | null;
}