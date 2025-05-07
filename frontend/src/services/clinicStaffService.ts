import axios from 'axios';
import { API_BASE_URL } from '../config';
import {
    ClinicStaffProfile,
    ApiErrorResponse,
} from '../types/apiTypes'; 

/**
* DTO to create a new clinic staff member.
* This must match the `ClinicStaffCreationDto` in the backend.
 */
export interface ClinicStaffCreationPayload {
    username: string;
    email: string;
    password: string;
    name: string;
    surname: string;
    role: 'VET' | 'ADMIN'; 
    licenseNumber?: string | null;
    }


/**
 * Creates a new clinic staff member (Vet or Admin) for the admin's clinic.
 * Sends data as multipart/form-data if a public key file is included for a VET.
 *
 * @param {string} token - The JWT token of the authenticated Admin.
 * @param {FormData} formData - The FormData object containing 'dto' (JSON blob) and optionally 'publicKeyFile'.
 * @returns {Promise<ClinicStaffProfile>} A promise resolving to the profile of the newly created staff member.
 * @throws {Error} Throws an error if creation fails.
 */
export const createClinicStaff = async (
    token: string,
    formData: FormData
): Promise<ClinicStaffProfile> => {
    if (!token) { throw new Error("Authentication token required."); }

    try {
        const response = await axios.post<ClinicStaffProfile>(`${API_BASE_URL}/staff`, formData, {
            headers: {
                'Authorization': `Bearer ${token}`,
            }
        });
        return response.data;
    } catch (error) {
        if (axios.isAxiosError(error) && error.response) {
            const apiError = error.response.data as ApiErrorResponse;
            console.error('API Create Staff Error:', apiError);
            let errorMessage = 'Failed to create staff member.';
            if (typeof apiError.message === 'string') {
                errorMessage = apiError.message;
            } else if (typeof apiError.message === 'object' && apiError.message !== null) {
                errorMessage = Object.values(apiError.message).join(' ');
            } else if (apiError.error) {
                errorMessage = apiError.error;
            }
            if (error.response.status === 409) errorMessage = `Creation failed: ${errorMessage}`;
            throw new Error(errorMessage);
        } else {
            console.error('Network or unexpected create staff error:', error);
            throw new Error('Failed to create staff member due to network or unexpected error.');
        }
    }
};

/**
 * Fetches all staff members (active and inactive) for a specific clinic.
 * Requires ADMIN or VET role of that clinic.
 *
 * @param {string} token - The JWT token.
 * @param {number | string} clinicId - The ID of the clinic.
 * @returns {Promise<ClinicStaffProfile[]>} A promise resolving to a list of staff profiles.
 * @throws {Error} Throws an error if fetching fails.
 */
export const getAllStaffForClinic = async (
    token: string,
    clinicId: number | string
): Promise<ClinicStaffProfile[]> => {
    if (!token) { throw new Error("Authentication token required."); }
    if (!clinicId) { throw new Error("Clinic ID is required."); }

    try {
        const response = await axios.get<ClinicStaffProfile[]>(`${API_BASE_URL}/clinics/${clinicId}/staff/all`, {
            headers: {
                'Authorization': `Bearer ${token}`,
            }
        });
        return response.data;
    } catch (error) {
        if (axios.isAxiosError(error) && error.response) {
            const apiError = error.response.data as ApiErrorResponse;
            console.error('API Get All Staff Error:', apiError);
            throw new Error(typeof apiError.message === 'string' ? apiError.message : apiError.error || 'Failed to fetch staff list.');
        } else {
            console.error('Network or unexpected get all staff error:', error);
            throw new Error('Failed to fetch staff list due to network or unexpected error.');
        }
    }
};

/**
 * Activates a clinic staff member's account.
 * Requires ADMIN role of that clinic.
 *
 * @param {string} token - The JWT token of the authenticated Admin.
 * @param {number | string} staffId - The ID of the staff member to activate.
 * @returns {Promise<ClinicStaffProfile>} A promise resolving to the profile of the activated staff member.
 * @throws {Error} Throws an error if activation fails.
 */
export const activateStaffMember = async (
    token: string,
    staffId: number | string
): Promise<ClinicStaffProfile> => {
    if (!token) { throw new Error("Authentication token required."); }

    try {
        const response = await axios.put<ClinicStaffProfile>(`${API_BASE_URL}/staff/${staffId}/activate`, {}, { 
            headers: { 'Authorization': `Bearer ${token}` }
        });
        return response.data;
    } catch (error) {
        if (axios.isAxiosError(error) && error.response) {
            const apiError = error.response.data as ApiErrorResponse;
            console.error('API Activate Staff Error:', apiError);
            throw new Error(typeof apiError.message === 'string' ? apiError.message : apiError.error || 'Failed to activate staff.');
        } else {
            console.error('Network or unexpected activate staff error:', error);
            throw new Error('Failed to activate staff due to network or unexpected error.');
        }
    }
};

/**
 * Deactivates a clinic staff member's account.
 * Requires ADMIN role of that clinic. Admin cannot deactivate themselves.
 *
 * @param {string} token - The JWT token of the authenticated Admin.
 * @param {number | string} staffId - The ID of the staff member to deactivate.
 * @returns {Promise<ClinicStaffProfile>} A promise resolving to the profile of the deactivated staff member.
 * @throws {Error} Throws an error if deactivation fails.
 */
export const deactivateStaffMember = async (
    token: string,
    staffId: number | string
): Promise<ClinicStaffProfile> => {
    if (!token) { throw new Error("Authentication token required."); }

    try {
        const response = await axios.put<ClinicStaffProfile>(`${API_BASE_URL}/staff/${staffId}/deactivate`, {}, { // Empty body
            headers: { 'Authorization': `Bearer ${token}` }
        });
        return response.data;
    } catch (error) {
        if (axios.isAxiosError(error) && error.response) {
            const apiError = error.response.data as ApiErrorResponse;
            console.error('API Deactivate Staff Error:', apiError);
            throw new Error(typeof apiError.message === 'string' ? apiError.message : apiError.error || 'Failed to deactivate staff.');
        } else {
            console.error('Network or unexpected deactivate staff error:', error);
            throw new Error('Failed to deactivate staff due to network or unexpected error.');
        }
    }
};
