import axios from 'axios';
import { API_BASE_URL } from '@/config';
import { PetProfileDto, Page, ApiErrorResponse, PetRegistrationData, BreedDto, PetOwnerUpdatePayload,Specie, PetStatus, PetActivationDto, PetClinicUpdatePayload, VetSummaryDto } from '@/types/apiTypes';

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

/**
 * Associates a PENDING pet with a specific clinic for activation.
 * This action is performed by the pet owner.
 *
 * @param {string} token - The JWT token of the authenticated owner.
 * @param {number | string} petId - The ID of the pet to associate.
 * @param {number | string} clinicId - The ID of the clinic to associate with.
 * @returns {Promise<void>} A promise that resolves when the association is successful.
 * @throws {Error} Throws an error if the association fails.
 */
export const associatePetToClinicForActivation = async (
    token: string,
    petId: number | string,
    clinicId: number | string
): Promise<void> => {
    if (!token) {
        throw new Error("Authentication token is required.");
    }
    if (!petId || !clinicId) {
        throw new Error("Pet ID and Clinic ID are required for association.");
    }

    try {
        await axios.post(`${API_BASE_URL}/pets/${petId}/associate-clinic/${clinicId}`, {}, {
            headers: { 'Authorization': `Bearer ${token}` },
        });
    } catch (error) {
        if (axios.isAxiosError(error) && error.response) {
            const apiError = error.response.data as ApiErrorResponse;
            console.error(`API Associate Pet (${petId}) with Clinic (${clinicId}) Error:`, apiError);
            const message = typeof apiError.message === 'string' ? apiError.message : apiError.error || 'Failed to associate pet with clinic.';
            throw new Error(message);
        } else {
            console.error(`Network or unexpected associate pet error:`, error);
            throw new Error('Failed to associate pet with clinic due to network or unexpected error.');
        }
    }
};

/**
 * Fetches pets that are pending activation at the currently authenticated staff's clinic.
 * Requires VET or ADMIN role and that the staff member is associated with a clinic.
 *
 * @param {string} token - The JWT token of the authenticated clinic staff.
 * @returns {Promise<PetProfileDto[]>} A promise resolving to a list of pets pending activation.
 * @throws {Error} Throws an error if fetching fails (e.g., not authorized, network issue).
 */
export const findMyClinicPendingPets = async (token: string): Promise<PetProfileDto[]> => {
    if (!token) {
        throw new Error("Authentication token is required to fetch pending pets.");
    }
    try {
        const response = await axios.get<PetProfileDto[]>(`${API_BASE_URL}/pets/clinic/pending`, {
            headers: { 'Authorization': `Bearer ${token}` },
        });
        return response.data;
    } catch (error) {
        if (axios.isAxiosError(error) && error.response) {
            const apiError = error.response.data as ApiErrorResponse;
            console.error('API Find My Clinic Pending Pets Error:', apiError);
            const message = typeof apiError.message === 'string' ? apiError.message : apiError.error || 'Failed to fetch pending activation requests.';
            throw new Error(message);
        } else {
            console.error('Network or unexpected find pending pets error:', error);
            throw new Error('Failed to fetch pending activation requests due to network or unexpected error.');
        }
    }
};

/**
 * Activates a PENDING pet by a clinic staff member (typically a Vet).
 * Sends all required pet details for activation.
 *
 * @param {string} token - The JWT token of the authenticated clinic staff.
 * @param {number | string} petId - The ID of the pet to activate.
 * @param {PetActivationDto} activationData - The DTO containing all necessary data for activation.
 * @returns {Promise<PetProfileDto>} A promise resolving to the profile of the activated pet.
 * @throws {Error} Throws an error if activation fails.
 */
export const activatePet = async (
    token: string,
    petId: number | string,
    activationData: PetActivationDto
): Promise<PetProfileDto> => {
    if (!token) {
        throw new Error("Authentication token is required.");
    }
    if (!petId) {
        throw new Error("Pet ID is required for activation.");
    }

    try {
        const response = await axios.put<PetProfileDto>(`${API_BASE_URL}/pets/${petId}/activate`, activationData, {
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json',
            },
        });
        return response.data;
    } catch (error) {
        if (axios.isAxiosError(error) && error.response) {
            const apiError = error.response.data as ApiErrorResponse;
            console.error(`API Activate Pet (${petId}) Error:`, apiError);
            let errorMessage = 'Failed to activate pet.';
            if (typeof apiError.message === 'string') {
                errorMessage = apiError.message;
            } else if (typeof apiError.message === 'object' && apiError.message !== null) {
                errorMessage = Object.values(apiError.message).join(' ');
            } else if (apiError.error) {
                errorMessage = apiError.error;
            }
            throw new Error(errorMessage);
        } else {
            console.error(`Network or unexpected activate pet (${petId}) error:`, error);
            throw new Error('Failed to activate pet due to network or unexpected error.');
        }
    }
};

/**
 * Fetches pets associated with the currently authenticated staff's clinic.
 * Supports pagination.
 *
 * @param {string} token - JWT token of clinic staff.
 * @param {number} [page=0] - Page number.
 * @param {number} [size=10] - Page size.
 * @param {string} [sort='name,asc'] - Sort criteria.
 * @returns {Promise<Page<PetProfileDto>>} Paginated list of clinic's pets.
 */
export const findPetsByClinic = async (
    token: string,
    page: number = 0,
    size: number = 10,
    sort: string = 'name,asc'
): Promise<Page<PetProfileDto>> => {
    if (!token) throw new Error("Authentication token required.");
    try {
        const response = await axios.get<Page<PetProfileDto>>(`${API_BASE_URL}/pets/clinic`, {
            headers: { 'Authorization': `Bearer ${token}` },
            params: { page, size, sort }
        });
        return response.data;
    } catch (error) {
        // ... manejo de error ...
        if (axios.isAxiosError(error) && error.response) {
            const apiError = error.response.data as ApiErrorResponse;
            throw new Error(typeof apiError.message === 'string' ? apiError.message : apiError.error || 'Failed to fetch clinic pets.');
        }
        throw new Error('Failed to fetch clinic pets due to network or unexpected error.');
    }
};

/**
 * Updates an existing pet's clinical information by clinic staff.
 *
 * @param {string} token - JWT token of the authenticated clinic staff.
 * @param {number | string} petId - The ID of the pet to update.
 * @param {PetClinicUpdatePayload} updateData - DTO with updatable clinical fields.
 * @returns {Promise<PetProfileDto>} Promise resolving to the updated pet profile.
 * @throws {Error} Throws an error if update fails.
 */
export const updatePetByClinicStaff = async (
    token: string,
    petId: number | string,
    updateData: PetClinicUpdatePayload
): Promise<PetProfileDto> => {
    if (!token) { throw new Error("Authentication token required."); }
    if (!petId) { throw new Error("Pet ID required."); }

    try {
        const response = await axios.put<PetProfileDto>(
            `${API_BASE_URL}/pets/${petId}/clinic-update`,
            updateData, 
            {
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json', 
                }
            }
        );
        return response.data;
    } catch (error) {
        if (axios.isAxiosError(error) && error.response) {
            const apiError = error.response.data as ApiErrorResponse;
            console.error(`API Update Pet by Clinic Staff (${petId}) Error:`, apiError);
            let errorMessage = 'Failed to update pet by clinic staff.';
            // ... (manejo de errorMessage similar a otros) ...
            if (typeof apiError.message === 'string') { errorMessage = apiError.message; }
            else if (typeof apiError.message === 'object' && apiError.message !== null) { errorMessage = Object.values(apiError.message).join(' '); }
            else if (apiError.error) { errorMessage = apiError.error; }
            throw new Error(errorMessage);
        } else {
            console.error(`Network or unexpected update pet by clinic staff (${petId}) error:`, error);
            throw new Error('Failed to update pet by clinic staff due to network or unexpected error.');
        }
    }
};

/**
 * Associates a veterinarian with a specific pet for the authenticated owner.
 *
 * @param {string} token - The JWT token of the authenticated owner.
 * @param {number | string} petId - The ID of the pet.
 * @param {number | string} vetId - The ID of the veterinarian to associate.
 * @returns {Promise<void>} A promise that resolves when the association is successful.
 * @throws {Error} Throws an error if association fails.
 */
export const associateVetWithPet = async (
    token: string,
    petId: number | string,
    vetId: number | string
): Promise<void> => {
    if (!token) throw new Error("Authentication token required.");
    if (!petId || !vetId) throw new Error("Pet ID and Veterinarian ID are required.");

    try {
        await axios.post(`${API_BASE_URL}/pets/${petId}/associate-vet/${vetId}`, {}, {
            headers: { 'Authorization': `Bearer ${token}` },
        });
    } catch (error) {
        // ... (manejo de error similar a otras funciones)
        if (axios.isAxiosError(error) && error.response) {
            const apiError = error.response.data as ApiErrorResponse;
            throw new Error(typeof apiError.message === 'string' ? apiError.message : apiError.error || 'Failed to associate veterinarian.');
        }
        throw new Error('Failed to associate veterinarian due to network or unexpected error.');
    }
};

/**
 * Disassociates a veterinarian from a specific pet for the authenticated owner.
 *
 * @param {string} token - The JWT token of the authenticated owner.
 * @param {number | string} petId - The ID of the pet.
 * @param {number | string} vetId - The ID of the veterinarian to disassociate.
 * @returns {Promise<void>} A promise that resolves when the disassociation is successful.
 * @throws {Error} Throws an error if disassociation fails.
 */
export const disassociateVetFromPet = async (
    token: string,
    petId: number | string,
    vetId: number | string
): Promise<void> => {
    if (!token) throw new Error("Authentication token required.");
    if (!petId || !vetId) throw new Error("Pet ID and Veterinarian ID are required.");

    try {
        await axios.delete(`${API_BASE_URL}/pets/${petId}/associate-vet/${vetId}`, {
            headers: { 'Authorization': `Bearer ${token}` },
        });
    } catch (error) {
        if (axios.isAxiosError(error) && error.response) {
            const apiError = error.response.data as ApiErrorResponse;
            throw new Error(typeof apiError.message === 'string' ? apiError.message : apiError.error || 'Failed to disassociate veterinarian.');
        }
        throw new Error('Failed to disassociate veterinarian due to network or unexpected error.');
    }
};

/**
 * Fetches a list of active veterinarians for a given clinic ID, suitable for selection.
 *
 * @param {string} token - The JWT token for authorization.
 * @param {number | string} clinicId - The ID of the clinic.
 * @returns {Promise<VetSummaryForSelectionDto[]>} A promise resolving to a list of vet summaries.
 *                                                 (Asegúrate que el tipo coincida con lo que devuelve el backend)
 * @throws {Error} Throws an error if fetching fails.
 */
export const getVetsByClinicId = async (token: string, clinicId: number | string): Promise<VetSummaryDto[]> => {
    if (!token) throw new Error("Authentication token is required.");
    if (!clinicId) throw new Error("Clinic ID is required.");

    try {
        const response = await axios.get<VetSummaryDto[]>(`${API_BASE_URL}/clinics/${clinicId}/vets-for-selection`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });
        return response.data;
    } catch (error) {
        if (axios.isAxiosError(error) && error.response) {
            const apiError = error.response.data as ApiErrorResponse;
            throw new Error(typeof apiError.message === 'string' ? apiError.message : apiError.error || 'Failed to fetch veterinarians for clinic.');
        }
        throw new Error('Failed to fetch veterinarians for clinic due to network or unexpected error.');
    }
};

/**
 * Allows a pet owner to request certificate generation from a specific clinic
 * for one of their pets. The pet must be associated with a vet from that clinic.
 *
 * @param {string} token - The JWT token of the authenticated owner.
 * @param {number | string} petId - The ID of the pet for which the certificate is requested.
 * @param {number | string} clinicId - The ID of the clinic to which the request is directed.
 * @returns {Promise<void>} A promise that resolves when the request is successfully sent.
 * @throws {Error} Throws an error if the request fails.
 */
export const requestCertificateGeneration = async (
    token: string,
    petId: number | string,
    clinicId: number | string
): Promise<void> => {
    if (!token) throw new Error("Authentication token required.");
    if (!petId || !clinicId) throw new Error("Pet ID and Clinic ID are required.");

    try {
        await axios.post(`${API_BASE_URL}/pets/${petId}/request-certificate/${clinicId}`, {}, {
            headers: { 'Authorization': `Bearer ${token}` },
        });
    } catch (error) {
        if (axios.isAxiosError(error) && error.response) {
            const apiError = error.response.data as ApiErrorResponse;
            console.error(`API Request Certificate (Pet: ${petId}, Clinic: ${clinicId}) Error:`, apiError);
            throw new Error(typeof apiError.message === 'string' ? apiError.message : apiError.error || 'Failed to request certificate.');
        }
        throw new Error('Failed to request certificate due to network or unexpected error.');
    }
};

/**
 * Fetches pets that have a pending certificate request for a specific clinic.
 * Requires VET or ADMIN role of that clinic.
 *
 * @param {string} token - The JWT token of the authenticated clinic staff.
 * @param {number | string} clinicId - The ID of the clinic.
 * @returns {Promise<PetProfileDto[]>} A list of pets with pending certificate requests.
 */
export const findPetsWithPendingCertRequestsForClinic = async (
    token: string,
    clinicId: number | string
): Promise<PetProfileDto[]> => {
    if (!token) throw new Error("Authentication token required.");
    if (!clinicId) throw new Error("Clinic ID required.");
    try {
        const response = await axios.get<PetProfileDto[]>(`${API_BASE_URL}/pets/${clinicId}/pending-certificate-requests`, {
            headers: { 'Authorization': `Bearer ${token}` },
        });
        return response.data;
    } catch (error) {
       if (axios.isAxiosError(error) && error.response) {
            const apiError = error.response.data as ApiErrorResponse;
            console.error(`API Error Fetching Pending Certificate Requests for Clinic ${clinicId}:`, apiError);
            let errorMessage = 'Failed to fetch pending certificate requests.';
            if (typeof apiError.message === 'string') {
                errorMessage = apiError.message;
            } else if (apiError.error) { 
                errorMessage = apiError.error;
            }
            throw new Error(errorMessage);
        } else {
            console.error('Network or other error fetching pending certificate requests:', error);
            throw new Error('Failed to fetch pending certificate requests due to network or unexpected error.');
        }
    }
};
