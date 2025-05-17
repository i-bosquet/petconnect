import axios from 'axios';
import { API_BASE_URL } from '@/config'; 
import {ApiErrorResponse,ClinicDto,ClinicUpdatePayload, Page, Country} from '../types/apiTypes';

/**
 * Fetches a paginated and filtered list of clinics from the backend API.
 *
 * @param {object} params - Filtering and pagination parameters.
 * @param {string} [params.name] - Optional filter by clinic name (partial match).
 * @param {string} [params.city] - Optional filter by city (partial match).
 * @param {Country} [params.country] - Optional filter by country.
 * @param {number} [params.page=0] - Page number (0-indexed).
 * @param {number} [params.size=10] - Number of items per page.
 * @param {string} [params.sort='name,asc'] - Sorting criteria (e.g., 'name,desc').
 * @returns {Promise<Page<ClinicDto>>} A promise resolving to the paginated clinic data.
 * @throws {Error} Throws an error if fetching fails.
 */
export const findClinics = async ({
    name,
    city,
    country, 
    page = 0,
    size = 10,
    sort = 'name,asc'
}: {
    name?: string;
    city?: string;
    country?: Country; 
    page?: number;
    size?: number;
    sort?: string;
} = {}): Promise<Page<ClinicDto>> => { 
    try {
        const response = await axios.get<Page<ClinicDto>>(`${API_BASE_URL}/clinics`, {
            params: {
                name,
                city,
                country,
                page,
                size,
                sort
            },
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            }
        });
        return response.data;
    } catch (error) {
        if (axios.isAxiosError(error) && error.response) {
            const apiError = error.response.data as ApiErrorResponse;
            console.error('API Find Clinics Error:', apiError);
            throw new Error(typeof apiError.message === 'string' ? apiError.message : apiError.error || 'Failed to fetch clinics');
        } else {
            console.error('Network or unexpected find clinics error:', error);
            throw new Error('Failed to fetch clinics due to network or unexpected error.');
        }
    }
};

/**
 * Fetches the distinct list of countries where clinics exist.
 *
 * @returns {Promise<Country[]>} A promise resolving to an array of Country enum values (as strings).
 * @throws {Error} Throws an error if fetching fails.
 */
export const getClinicCountries = async (): Promise<Country[]> => { 
    try {
        const response = await axios.get<Country[]>(`${API_BASE_URL}/clinics/countries`);
        return response.data;
    } catch (error) {
        if (axios.isAxiosError(error) && error.response) {
            const apiError = error.response.data as ApiErrorResponse;
            console.error('API Find Countries Error:', apiError);
            throw new Error(typeof apiError.message === 'string' ? apiError.message : apiError.error || 'Failed to fetch countries');
        } else {
            console.error('Network or unexpected find countries error:', error);
            throw new Error('Failed to fetch countries due to network or unexpected error.');
        }
    }
};

/**
 * Fetches the details of a specific clinic by its ID.
 * Requires authentication (token likely needed by backend, depends on endpoint security).
 *
 * @param {string} token - The JWT authentication token.
 * @param {number | string} clinicId - The ID of the clinic to fetch.
 * @returns {Promise<ClinicDto>} A promise resolving to the clinic's details.
 * @throws {Error} Throws an error if fetching fails.
 */
export const getClinicDetails = async (token: string, clinicId: number | string): Promise<ClinicDto> => {
    if (!token) throw new Error("Authentication token required.");
    if (!clinicId) throw new Error("Clinic ID required.");
    try {
        const response = await axios.get<ClinicDto>(`${API_BASE_URL}/clinics/${clinicId}`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });
        return response.data;
    } catch (error) {
        if (axios.isAxiosError(error) && error.response) {
            const apiError = error.response.data as ApiErrorResponse;
            console.error(`API Get Clinic (${clinicId}) Error:`, apiError);
            throw new Error(typeof apiError.message === 'string' ? apiError.message : apiError.error || 'Failed to fetch clinic details.');
        } else {
            console.error(`Network or unexpected get clinic (${clinicId}) error:`, error);
            throw new Error('Failed to fetch clinic details due to network or unexpected error.');
        }
    }
};

/**
 * Updates the details of a specific clinic.
 * Requires ADMIN role for that clinic (enforced by backend).
 * Handles optional public key file upload.
 *
 * @param {string} token - The JWT token of the authenticated Admin.
 * @param {number | string} clinicId - The ID of the clinic to update.
 * @param {ClinicUpdatePayload} updateData - The data to update (JSON part).
 * @param {File | null} publicKeyFile - Optional new public key file (.pem/.crt).
 * @returns {Promise<ClinicDto>} A promise resolving to the updated clinic details.
 * @throws {Error} Throws an error if updating fails.
 */
export const updateClinic = async (
    token: string,
    clinicId: number | string,
    updateData: ClinicUpdatePayload,
    publicKeyFile: File | null,
    privateKeyFile: File | null
): Promise<ClinicDto> => {
    if (!token) throw new Error("Authentication token required.");
    if (!clinicId) throw new Error("Clinic ID required.");

    const formData = new FormData();
    const dtoBlob = new Blob([JSON.stringify(updateData)], { type: 'application/json' });
    formData.append('dto', dtoBlob);

    if (publicKeyFile) {
        formData.append('publicKeyFile', publicKeyFile, publicKeyFile.name);
    }

    if (privateKeyFile) { 
        formData.append('privateKeyFile', privateKeyFile, privateKeyFile.name);
    }

    try {
        const response = await axios.put<ClinicDto>(`${API_BASE_URL}/clinics/${clinicId}`, formData, { 
            headers: {
                'Authorization': `Bearer ${token}`,
            }
        });
        return response.data;
    } catch (error) {
         if (axios.isAxiosError(error) && error.response) {
             const apiError = error.response.data as ApiErrorResponse;
             console.error(`API Update Clinic (${clinicId}) Error:`, apiError);
              let errorMessage = 'Failed to update clinic details.';
              if (typeof apiError.message === 'string') { errorMessage = apiError.message; }
              else if (typeof apiError.message === 'object' && apiError.message !== null) { errorMessage = Object.values(apiError.message).join(' '); }
              else if (apiError.error) { errorMessage = apiError.error; }
             throw new Error(errorMessage);
         } else {
             console.error(`Network or unexpected update clinic (${clinicId}) error:`, error);
             throw new Error('Failed to update clinic details due to network or unexpected error.');
         }
    }
};
/**
 * Initiates the download of the clinic's public key file.
 *
 * @param {string} token - The JWT token.
 * @param {number | string} clinicId - The ID of the clinic.
 * @throws {Error} Throws an error if the download fails.
 */
export const downloadClinicPublicKey = async (token: string, clinicId: number | string): Promise<void> => {
    if (!token) throw new Error("Authentication token required.");
    if (!clinicId) throw new Error("Clinic ID required.");
    try {
        const response = await axios.get(`${API_BASE_URL}/clinics/${clinicId}/public-key/download`, {
            headers: { 'Authorization': `Bearer ${token}` },
            responseType: 'blob', 
        });

        // Extract file name from Content-Disposition header
        const contentDisposition = response.headers['content-disposition'];
        let filename = `clinic_${clinicId}_pub.pem`; // Fallback filename
        if (contentDisposition) {
            const filenameMatch = contentDisposition.match(/filename="?(.+)"?/i);
            if (filenameMatch && filenameMatch.length === 2) {
                filename = filenameMatch[1];
            }
        }

        // Create a temporary link to start the download
        const url = window.URL.createObjectURL(new Blob([response.data]));
        const link = document.createElement('a');
        link.href = url;
        link.setAttribute('download', filename);
        document.body.appendChild(link);
        link.click();
        link.parentNode?.removeChild(link);
        window.URL.revokeObjectURL(url);

    } catch (error) {
         if (axios.isAxiosError(error) && error.response) {
             try {
                 const errorText = await (error.response.data as Blob).text();
                 const apiError = JSON.parse(errorText) as ApiErrorResponse;
                 console.error(`API Download Key (${clinicId}) Error:`, apiError);
                 throw new Error(typeof apiError.message === 'string' ? apiError.message : apiError.error || 'Failed to download key.');
             } catch {
                  console.error(`API Download Key (${clinicId}) Error: Status ${error.response.status}`, error);
                  throw new Error(`Failed to download key (Status: ${error.response.status}).`);
             }
         } else {
             console.error(`Network or unexpected download key (${clinicId}) error:`, error);
             throw new Error('Failed to download key due to network or unexpected error.');
         }
    }
};