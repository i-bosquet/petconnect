import axios from 'axios';
import { API_BASE_URL } from '@/config';
import { PetProfileDto, Page, ApiErrorResponse, PetRegistrationData, BreedDto, PetOwnerUpdatePayload,Specie, PetStatus,  } from '../types/apiTypes';

interface FindMyPetsParams {
page: number;
size: number;
sort: string;
statuses?: string;
}

/**
 * Fetches the authenticated owner's pets from the backend API with pagination.
 * Requires the JWT token for authorization.
 *
 * @param {string} token - The JWT token of the authenticated owner.
 * @param {number} [page=0] - The page number to retrieve (0-indexed).
 * @param {number} [size=10] - The number of pets per page.
 * @param {string} [sort='name,asc'] - Sorting criteria (e.g., 'name,desc').
 * @param {PetStatus[]} [statuses] - Optional array of PetStatus enums to filter by.
 * @returns {Promise<Page<PetProfileDto>>} A promise resolving to the paginated pet data.
 * @throws {Error} Throws an error if fetching fails.
 */
export const findMyPets = async (
    token: string,
    page: number = 0,
    size: number = 10,
    sort: string = 'name,asc',
    statuses?: PetStatus[]
): Promise<Page<PetProfileDto>> => {
    if (!token) {
        throw new Error("Authentication token is required to fetch pets.");
    }
    try {
        const params: FindMyPetsParams = { page, size, sort };
        if (statuses && statuses.length > 0) {
        params.statuses = statuses.join(',');
    }

        const response = await axios.get<Page<PetProfileDto>>(`${API_BASE_URL}/pets`, {
        headers: { 'Authorization': `Bearer ${token}`, 'Accept': 'application/json' },
        params: params
        });
        return response.data;
    } catch (error) {
        if (axios.isAxiosError(error) && error.response) {
            const apiError = error.response.data as ApiErrorResponse;
            console.error('API Find My Pets Error:', apiError);
            const message = typeof apiError.message === 'string' ? apiError.message : apiError.error || 'Failed to fetch pets';
            throw new Error(message);
        } else {
            console.error('Network or unexpected find pets error:', error);
            throw new Error('Failed to fetch pets due to network or unexpected error.');
        }
    }
};

/**
 * Registers a new pet for the authenticated owner via the backend API.
 * Handles sending data as JSON or multipart/form-data depending on whether an image file is provided.
 *
 * @param {string} token - The JWT token of the authenticated owner.
 * @param {PetRegistrationData} petData - The data for the new pet (excluding the raw image file).
 * @param {File | null} imageFile - The optional image file to upload.
 * @returns {Promise<PetProfileDto>} A promise resolving to the profile of the newly created pet.
 * @throws {Error} Throws an error if registration fails.
 */
export const registerPet = async (
    token: string,
    petData: PetRegistrationData,
    imageFile: File | null
): Promise<PetProfileDto> => {
    if (!token) { throw new Error("Authentication token required."); }

    const formData = new FormData();

    const dtoBlob = new Blob([JSON.stringify(petData)], { type: 'application/json' });
    formData.append('dto', dtoBlob);
    if (imageFile) {
        formData.append('imageFile', imageFile, imageFile.name);
        console.log("Appending image file to FormData:", imageFile.name);
   } else {
        console.log("No image file provided, sending FormData with only 'dto' part.");
   }

   try {
       const response = await axios.post<PetProfileDto>(`${API_BASE_URL}/pets`, formData, {
           headers: {
               'Authorization': `Bearer ${token}`,
           }
       });
       return response.data;
   } catch (error) {
       if (axios.isAxiosError(error) && error.response) {
           const apiError = error.response.data as ApiErrorResponse;
           console.error('API Register Pet Error:', apiError);
           let errorMessage = 'Failed to register pet.';
           if (typeof apiError.message === 'string') { errorMessage = apiError.message; }
           else if (typeof apiError.message === 'object' && apiError.message !== null) { errorMessage = Object.values(apiError.message).join(' '); }
           else if (apiError.error) { errorMessage = apiError.error; }
           throw new Error(errorMessage);
       } else {
           console.error('Network or unexpected register pet error:', error);
           throw new Error('Failed to register pet due to network or unexpected error.');
       }
   }
};

/**
 * Updates an existing pet for the authenticated owner via the backend API.
 * Always sends data as multipart/form-data.
 *
 * @param {string} token - The JWT token.
 * @param {number | string} petId - The ID of the pet to update.
 * @param {PetOwnerUpdateDto} updateData - The DTO with updated fields (excluding image).
 * @param {File | null} imageFile - The optional new image file.
 * @returns {Promise<PetProfileDto>} Promise resolving to the updated pet profile.
 * @throws {Error} Throws an error if update fails.
 */
export const updatePetByOwner = async (
    token: string,
    petId: number | string,
    updateData: PetOwnerUpdatePayload, 
    imageFile: File | null
): Promise<PetProfileDto> => {
     if (!token) { throw new Error("Authentication token required."); }

     const formData = new FormData();
     const dtoBlob = new Blob([JSON.stringify(updateData)], { type: 'application/json' });
     formData.append('dto', dtoBlob);

     if (imageFile) {
         formData.append('imageFile', imageFile, imageFile.name);
         console.log("Updating pet with image file using FormData.");
     } else {
          console.log("Updating pet without image file using FormData.");
     }

      try {
         const response = await axios.put<PetProfileDto>(`${API_BASE_URL}/pets/${petId}/owner-update`, formData, { 
             headers: {
                 'Authorization': `Bearer ${token}`,
             }
         });
         return response.data;
     } catch (error) {
        if (axios.isAxiosError(error) && error.response) {
            const apiError = error.response.data as ApiErrorResponse;
            console.error('API Update Pet Error:', apiError);
            let errorMessage = 'Failed to update pet.';
            if (typeof apiError.message === 'string') { errorMessage = apiError.message; }
            else if (typeof apiError.message === 'object' && apiError.message !== null) { errorMessage = Object.values(apiError.message).join(' '); }
            else if (apiError.error) { errorMessage = apiError.error; }
            throw new Error(errorMessage);
        } else {
            console.error('Network or unexpected update pet error:', error);
            throw new Error('Failed to update pet due to network or unexpected error.');
        }
    }
};
    

/**
 * Fetches available breeds for a given species.
 *
 * @param {string} token - The JWT token for authorization.
 * @param {Specie} specie - The species to filter breeds by.
 * @returns {Promise<BreedDto[]>} A promise resolving to an array of breeds.
 * @throws {Error} Throws an error if fetching fails.
 */
 export const getBreedsBySpecie = async (token: string, specie: Specie): Promise<BreedDto[]> => {
    if (!token) { throw new Error("Authentication token is required."); }
    if (!specie) { return []; }

    try {
        const response = await axios.get<BreedDto[]>(`${API_BASE_URL}/pets/breeds/${specie}`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });
        return response.data;
    } catch (error) {
         if (axios.isAxiosError(error) && error.response) {
            const apiError = error.response.data as ApiErrorResponse;
            console.error('API Get Breeds Error:', apiError);
            throw new Error(typeof apiError.message === 'string' ? apiError.message : apiError.error || 'Failed to fetch breeds');
        } else {
            console.error('Network or unexpected get breeds error:', error);
            throw new Error('Failed to fetch breeds due to network or unexpected error.');
        }
    }
};

/**
 * Fetches the detailed profile of a specific pet by its ID.
 * Requires authentication.
 *
 * @param {string} token - The JWT token.
 * @param {number | string} petId - The ID of the pet to retrieve.
 * @returns {Promise<PetProfileDto>} A promise resolving to the pet's detailed profile.
 * @throws {Error} Throws an error if fetching fails.
 */
export const getPetDetailsById = async (token: string, petId: number | string): Promise<PetProfileDto> => {
    if (!token) throw new Error("Authentication token required.");
    if (!petId) throw new Error("Pet ID required.");
    try {
        const response = await axios.get<PetProfileDto>(`${API_BASE_URL}/pets/${petId}`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });
        return response.data;
    } catch (error) {
        if (axios.isAxiosError(error) && error.response) {
            const apiError = error.response.data as ApiErrorResponse;
            console.error(`API Get Pet Details (${petId}) Error:`, apiError);
            throw new Error(typeof apiError.message === 'string' ? apiError.message : apiError.error || 'Failed to fetch pet details.');
        } else {
            console.error(`Network or unexpected get pet details (${petId}) error:`, error);
            throw new Error('Failed to fetch pet details due to network or unexpected error.');
        }
    }
};

/**
 * Deactivates a specific pet for the authenticated owner.
 * This action sets the pet's status to INACTIVE.
 *
 * @param {string} token - The JWT token of the authenticated owner.
 * @param {number | string} petId - The ID of the pet to deactivate.
 * @returns {Promise<PetProfileDto>} A promise resolving to the updated pet profile (now inactive).
 * @throws {Error} Throws an error if deactivation fails.
 * @author ibosquet
 */
export const deactivatePet = async (
    token: string,
    petId: number | string
): Promise<PetProfileDto> => {
    if (!token) {
        throw new Error("Authentication token is required.");
    }
    if (!petId) {
        throw new Error("Pet ID is required for deactivation.");
    }

    try {
        const response = await axios.put<PetProfileDto>(`${API_BASE_URL}/pets/${petId}/deactivate`, {}, {
            headers: { 'Authorization': `Bearer ${token}` },
        });
        return response.data;
    } catch (error) {
        if (axios.isAxiosError(error) && error.response) {
            const apiError = error.response.data as ApiErrorResponse;
            console.error(`API Deactivate Pet (${petId}) Error:`, apiError);
            const message = typeof apiError.message === 'string' ? apiError.message : apiError.error || 'Failed to deactivate pet.';
            throw new Error(message);
        } else {
            console.error(`Network or unexpected deactivate pet (${petId}) error:`, error);
            throw new Error('Failed to deactivate pet due to network or unexpected error.');
        }
    }
};