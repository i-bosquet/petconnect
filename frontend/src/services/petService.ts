import axios from 'axios';
import { API_BASE_URL } from '../config';
import { PetProfileDto, Page, ApiErrorResponse, PetRegistrationData, BreedDto } from '../types/apiTypes';
import { Specie } from '../types/enumTypes';

/**
 * Fetches the authenticated owner's pets from the backend API with pagination.
 * Requires the JWT token for authorization.
 *
 * @param {string} token - The JWT token of the authenticated owner.
 * @param {number} [page=0] - The page number to retrieve (0-indexed).
 * @param {number} [size=10] - The number of pets per page.
 * @param {string} [sort='name,asc'] - Sorting criteria (e.g., 'name,desc').
 * @returns {Promise<Page<PetProfileDto>>} A promise resolving to the paginated pet data.
 * @throws {Error} Throws an error if fetching fails.
 */
export const findMyPets = async (
    token: string,
    page: number = 0,
    size: number = 10,
    sort: string = 'name,asc'
): Promise<Page<PetProfileDto>> => {
    if (!token) {
        throw new Error("Authentication token is required to fetch pets.");
    }
    try {
        const response = await axios.get<Page<PetProfileDto>>(`${API_BASE_URL}/pets`, {
            headers: { 'Authorization': `Bearer ${token}`, 'Accept': 'application/json' },
            params: { page, size, sort }
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
    updateData: Omit<PetProfileDto, 'image'>, 
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