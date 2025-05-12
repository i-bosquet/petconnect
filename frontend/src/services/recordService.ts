import axios from 'axios';
import { API_BASE_URL } from '@/config';
import {
    RecordViewDto,
    RecordCreatePayload,
    RecordUpdatePayload,
    Page,
    ApiErrorResponse
} from '@/types/apiTypes'; 

interface FindRecordsParams {
    petId: number | string;
    page: number;
    size: number;
    sort?: string; 
}

/**
 * Fetches a paginated list of medical records for a specific pet.
 *
 * @param {string} token - The JWT token for authorization.
 * @param {FindRecordsParams} params - Parameters including petId and pagination.
 * @returns {Promise<Page<RecordViewDto>>} A promise resolving to paginated record data.
 * @throws {Error} If fetching fails.
 */
export const findRecordsByPetId = async (
    token: string,
    { petId, page, size, sort = 'createdAt,desc' }: FindRecordsParams
): Promise<Page<RecordViewDto>> => {
    if (!token) throw new Error("Authentication token required.");
    try {
        const response = await axios.get<Page<RecordViewDto>>(`${API_BASE_URL}/records`, {
            headers: { 'Authorization': `Bearer ${token}` },
            params: { petId, page, size, sort }
        });
        return response.data;
    } catch (error) {
        if (axios.isAxiosError(error) && error.response) {
            const apiError = error.response.data as ApiErrorResponse;
            throw new Error(typeof apiError.message === 'string' ? apiError.message : apiError.error || 'Failed to fetch records.');
        }
        throw new Error('Failed to fetch records due to network or unexpected error.');
    }
};

/**
 * Creates a new medical record for a pet.
 *
 * @param {string} token - The JWT token.
 * @param {RecordCreatePayload} payload - The data for the new record.
 * @returns {Promise<RecordViewDto>} A promise resolving to the created record data.
 * @throws {Error} If creation fails.
 */
export const createRecord = async (
    token: string,
    payload: RecordCreatePayload
): Promise<RecordViewDto> => {
    if (!token) throw new Error("Authentication token required.");
    try {
        const response = await axios.post<RecordViewDto>(`${API_BASE_URL}/records`, payload, {
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json',
            },
        });
        return response.data;
    } catch (error) {
        if (axios.isAxiosError(error) && error.response) {
            const apiError = error.response.data as ApiErrorResponse;
            let errorMessage = 'Failed to create record.';
            if (typeof apiError.message === 'string') { errorMessage = apiError.message; }
            else if (typeof apiError.message === 'object' && apiError.message !== null) { errorMessage = Object.values(apiError.message).join(' '); }
            else if (apiError.error) { errorMessage = apiError.error; }
            throw new Error(errorMessage);
        }
        throw new Error('Failed to create record due to network or unexpected error.');
    }
};

/**
 * Updates an existing unsigned medical record.
 * Only the owner can update their own non-vaccine, unsigned records.
 *
 * @param {string} token - The JWT token.
 * @param {number | string} recordId - The ID of the record to update.
 * @param {RecordUpdatePayload} payload - The data to update the record with.
 * @returns {Promise<RecordViewDto>} A promise resolving to the updated record data.
 * @throws {Error} If update fails.
 */
export const updateUnsignedRecord = async (
    token: string,
    recordId: number | string,
    payload: RecordUpdatePayload
): Promise<RecordViewDto> => {
    if (!token) throw new Error("Authentication token required.");
    try {
        const response = await axios.put<RecordViewDto>(`${API_BASE_URL}/records/${recordId}`, payload, {
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json',
            },
        });
        return response.data;
    } catch (error) {
        if (axios.isAxiosError(error) && error.response) {
            const apiError = error.response.data as ApiErrorResponse;
             let errorMessage = 'Failed to update record.';
            if (typeof apiError.message === 'string') { errorMessage = apiError.message; }
            else if (typeof apiError.message === 'object' && apiError.message !== null) { errorMessage = Object.values(apiError.message).join(' '); }
            else if (apiError.error) { errorMessage = apiError.error; }
            throw new Error(errorMessage);
        }
        throw new Error('Failed to update record due to network or unexpected error.');
    }
};

/**
 * Deletes a medical record.
 * Restrictions apply based on who created it and if it's signed (handled by backend).
 *
 * @param {string} token - The JWT token.
 * @param {number | string} recordId - The ID of the record to delete.
 * @returns {Promise<void>} A promise that resolves when deletion is successful.
 * @throws {Error} If deletion fails.
 */
export const deleteRecord = async (
    token: string,
    recordId: number | string
): Promise<void> => {
    if (!token) throw new Error("Authentication token required.");
    try {
        await axios.delete(`${API_BASE_URL}/records/${recordId}`, {
            headers: { 'Authorization': `Bearer ${token}` },
        });
    } catch (error) {
        // ... (manejo de error estándar)
         if (axios.isAxiosError(error) && error.response) {
            const apiError = error.response.data as ApiErrorResponse;
            throw new Error(typeof apiError.message === 'string' ? apiError.message : apiError.error || 'Failed to delete record.');
        }
        throw new Error('Failed to delete record due to network or unexpected error.');
    }
};

/**
 * Fetches a single medical record by its ID.
 *
 * @param {string} token - The JWT token.
 * @param {number | string} recordId - The ID of the record.
 * @returns {Promise<RecordViewDto>} A promise resolving to the record data.
 * @throws {Error} If fetching fails.
 */
export const findRecordById = async (token: string, recordId: number | string): Promise<RecordViewDto> => {
    if (!token) throw new Error("Authentication token required.");
    try {
        const response = await axios.get<RecordViewDto>(`${API_BASE_URL}/records/${recordId}`, {
            headers: { 'Authorization': `Bearer ${token}` },
        });
        return response.data;
    } catch (error) {
        if (axios.isAxiosError(error) && error.response) {
            const apiError = error.response.data as ApiErrorResponse;
            throw new Error(typeof apiError.message === 'string' ? apiError.message : apiError.error || 'Failed to fetch record details.');
        }
        throw new Error('Failed to fetch record details due to network or unexpected error.');
    }
};