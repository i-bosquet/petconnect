import { RecordType } from './enumTypes';
import { UserProfile } from './authTypes'; 

/**
 * Detailed view of Vaccine information within a Record.
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
    creator: UserProfile; 
    vaccine?: VaccineViewDto | null;
}

/**
 * Summary information for a medical Record, typically used within certificate details.
 */
export interface RecordSummaryDto {
    id: number | string;
    type: RecordType;
    description?: string | null;
    createdAt?: string | null; 
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