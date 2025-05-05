import axios from 'axios';
import { API_BASE_URL } from '../config'; 
import { Country } from '../types/enumTypes'; 
import { 
    ApiErrorResponse,
    ClinicDto,
    Page
} from '../types/apiTypes';

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