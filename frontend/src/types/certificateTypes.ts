import { PetProfileDto, RecordSummaryDto, VetSummaryDto, ClinicDto } from './apiTypes'; 

export interface CertificateGenerationRequestDto {
    petId: number | string;
    certificateNumber: string;
}

export interface CertificateViewDto {
    id: number | string;
    certificateNumber: string;
    pet: PetProfileDto; 
    originatingRecord: RecordSummaryDto;
    generatorVet: VetSummaryDto;
    issuingClinic: ClinicDto;
    createdAt: string;
    initialEuEntryExpiryDate?: string | null; 
    travelValidityEndDate?: string | null; 
    isCurrentlyValidForEuTravel?: boolean | null;
    payload: string;
    hash: string;
    vetSignature: string;
    clinicSignature: string;
}