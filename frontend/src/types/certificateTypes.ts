import { PetProfileDto, RecordViewDto, VetSummaryDto} from './apiTypes'; 

export interface CertificateGenerationRequestDto {
    petId: number | string;
    certificateNumber: string;
}

export interface CertificateViewDto {
    id: number | string;
    certificateNumber: string;
    pet: PetProfileDto; 
    originatingRecord: RecordViewDto;
    generatorVet: VetSummaryDto;
    createdAt: string;
    initialEuEntryExpiryDate?: string | null; 
    travelValidityEndDate?: string | null; 
    isCurrentlyValidForEuTravel?: boolean | null;
    payload: string;
    hash: string;
    vetSignature: string;
    clinicSignature: string;
}

export interface CertificateGenerationRequestDto {
    petId: number | string;
    certificateNumber: string;
    vetPrivateKeyPassword?: string | null; 
    clinicPrivateKeyPassword?: string | null; 
}