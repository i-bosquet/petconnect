import { RecordType, Specie } from './enumTypes';
import { ClinicStaffProfile } from './authTypes'; 

/**
 * Detailed view of Vaccine information within the Record.
 */
export interface VaccineViewDto {
    name: string;
    validity?: number | null; // Years
    laboratory?: string | null;
    batchNumber?: string | null;
    isRabiesVaccine: boolean;
}

/**
 * Detailed view of a Medical Record.
 */
export interface RecordViewDto {
    id: number | string;
    type: RecordType;
    description?: string | null;
    vetSignature?: string | null;
    createdAt: string; 
    createdBy?: string | null;
    updatedAt?: string | null; 
    updatedBy?: string | null;
    creator: ClinicStaffProfile; 
    vaccine?: VaccineViewDto | null;
    createdInClinicId?: number | string | null;
    createdInClinicName?: string | null;
    petId: number | string;
    petName: string | null; 
    petSpecie: Specie | null; 
    isImmutable: boolean; 
}

// --- DTOs mirroring backend API Payloads ---

/**
 * Payload for creating vaccine details, nested within RecordCreatePayload.
 */
export interface VaccineCreatePayload {
    name: string;
    validity: number;
    laboratory?: string | null;
    batchNumber: string;
    isRabiesVaccine: boolean;
}

/**
 * Payload for creating a new medical record.
 */
export interface RecordCreatePayload {
    petId: number | string;
    type: RecordType;
    description?: string | null;
    vaccine?: VaccineCreatePayload | null; 
    vetPrivateKeyPassword?: string | null;
}

/**
 * Payload for updating an unsigned medical record.
 */
export interface RecordUpdatePayload {
    type?: RecordType | null; 
    description?: string | null; 
}

/**
 * Payload for requesting temporary access.
 */
export interface TemporaryAccessRequestPayload {
    durationString: string; 
}

/**
 * Structure of the response containing the temporary access token.
 */
export interface TemporaryAccessTokenResponse {
    token: string;
}