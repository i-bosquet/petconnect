import axios from 'axios';
import { API_BASE_URL } from '@/config';
import {
    CertificateViewDto,
    CertificateGenerationRequestDto,
    ApiErrorResponse,
    Page
} from '@/types/apiTypes';

interface FindClinicCertificatesParams {
    clinicId: number | string;
    page: number;
    size: number;
    sort?: string; 
    certificateNumber?: string; 
    petName?: string; 
}

/**
 * Fetches all certificates for a specific pet.
 *
 * @param {string} token - The JWT token for authorization.
 * @param {number | string} petId - The ID of the pet.
 * @returns {Promise<CertificateViewDto[]>} A list of certificates.
 * @throws {Error} If fetching fails.
 */
export const findCertificatesByPet = async (token: string, petId: number | string): Promise<CertificateViewDto[]> => {
    if (!token) throw new Error("Authentication token required.");
    try {
        const response = await axios.get<CertificateViewDto[]>(`${API_BASE_URL}/certificates`, {
            headers: { 'Authorization': `Bearer ${token}` },
            params: { petId }
        });
        return response.data;
    } catch (error) {
        if (axios.isAxiosError(error) && error.response) {
            const apiError = error.response.data as ApiErrorResponse;
            throw new Error(typeof apiError.message === 'string' ? apiError.message : apiError.error || 'Failed to fetch certificates.');
        }
        throw new Error('Failed to fetch certificates due to network or unexpected error.');
    }
};

/**
 * Generates a new certificate for a pet. (Action performed by a VET)
 *
 * @param {string} token - The JWT token of the VET.
 * @param {CertificateGenerationRequestDto} payload - Pet ID and official certificate number.
 * @returns {Promise<CertificateViewDto>} The generated certificate.
 * @throws {Error} If generation fails (e.g., missing prerequisites).
 */
export const generateCertificate = async (token: string, payload: CertificateGenerationRequestDto): Promise<CertificateViewDto> => {
    if (!token) throw new Error("Authentication token required.");
    try {
        const response = await axios.post<CertificateViewDto>(`${API_BASE_URL}/certificates`, payload, {
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json',
            },
        });
        return response.data;
    } catch (error) {
        if (axios.isAxiosError(error) && error.response) {
            const apiError = error.response.data as ApiErrorResponse;
            let errMsg = 'Failed to generate certificate.';
            if (typeof apiError.message === 'string') errMsg = apiError.message;
            else if (typeof apiError.message === 'object' && apiError.message !== null) errMsg = Object.values(apiError.message).join(' ');
            else if (apiError.error) errMsg = apiError.error;
            if (error.response.status === 409 || error.response.status === 400) errMsg = apiError.message as string || errMsg; 
            throw new Error(errMsg);
        }
        throw new Error('Failed to generate certificate due to network or unexpected error.');
    }
};

/**
 * Fetches the QR code data string for a specific certificate.
 *
 * @param {string} token - The JWT token.
 * @param {number | string} certificateId - The ID of the certificate.
 * @returns {Promise<string>} The Base45 encoded string for the QR code.
 * @throws {Error} If fetching fails.
 */
export const getCertificateQrData = async (token: string, certificateId: number | string): Promise<string> => {
    if (!token) throw new Error("Authentication token required.");
    try {
        const response = await axios.get<string>(`${API_BASE_URL}/certificates/${certificateId}/qr-data`, {
            headers: { 'Authorization': `Bearer ${token}` },
            responseType: 'text' 
        });
        return response.data;
    } catch (error) {
        if (axios.isAxiosError(error) && error.response) {
             try {
                const errorObj = typeof error.response.data === "string" ? JSON.parse(error.response.data) : error.response.data;
                const message = errorObj.message || errorObj.error || "Failed to fetch QR data.";
                throw new Error(message);
            } catch {
                 throw new Error( "Failed to fetch QR data or parse error response.");
            }
        }
        throw new Error('Failed to fetch QR data due to network or unexpected error.');
    }
};

/**
 * Fetches a paginated list of all certificates issued by a specific clinic.
 *
 * @param {string} token - The JWT token for authorization.
 * @param {FindClinicCertificatesParams} params - Parameters including clinicId and pagination.
 * @returns {Promise<Page<CertificateViewDto>>} A promise resolving to paginated certificate data.
 * @throws {Error} If fetching fails.
 */
export const findCertificatesCreatedByClinic = async (
    token: string,
     { clinicId, page, size, sort = 'createdAt,desc', certificateNumber, petName }: FindClinicCertificatesParams
): Promise<Page<CertificateViewDto>> => {
    if (!token) throw new Error("Authentication token required.");
    if (!clinicId) throw new Error("Clinic ID is required.");

    try {
        const queryParams: Record<string, string | number> = {page, size, sort};

        if (certificateNumber) queryParams.certificateNumber = certificateNumber;
        if (petName) queryParams.petName = petName;

          const response = await axios.get<Page<CertificateViewDto>>(`${API_BASE_URL}/certificates/clinic/${clinicId}`, {
            headers: { 'Authorization': `Bearer ${token}` },
            params: queryParams
        });
        return response.data;
    } catch (error) {
        if (axios.isAxiosError(error) && error.response) {
            const apiError = error.response.data as ApiErrorResponse;
            throw new Error(typeof apiError.message === 'string' ? apiError.message : apiError.error || 'Failed to fetch issued certificates.');
        }
        throw new Error('Failed to fetch issued certificates due to network or unexpected error.');
    }
};