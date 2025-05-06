import axios from 'axios';
import { API_BASE_URL } from '../config';
import {
    OwnerProfileUpdateDto,
    UserProfileUpdateDto,
    OwnerProfileUpdateResponseDto,      
    ClinicStaffProfileUpdateResponseDto, 
    ApiErrorResponse
} from '../types/apiTypes'; 

/**
 * Updates the currently authenticated owner's profile.
 * Handles sending data as multipart/form-data.
 *
 * @param {string} token - The JWT authentication token.
 * @param {OwnerProfileUpdateDto} updateDTO - The data transfer object for the owner update.
 * @param {File | null} imageFile - The optional new avatar image file.
 * @returns {Promise<OwnerProfileUpdateResponseDto>} A promise resolving to the update response (profile + new token).
 * @throws {Error} Throws an error if the update fails.
 */
export const updateCurrentOwnerProfile = async (
    token: string,
    updateDTO: OwnerProfileUpdateDto,
    imageFile: File | null
): Promise<OwnerProfileUpdateResponseDto> => {
    if (!token) { throw new Error("Authentication token required."); }

    const formData = new FormData();
    const dtoBlob = new Blob([JSON.stringify(updateDTO)], { type: 'application/json' });
    formData.append('dto', dtoBlob);
    if (imageFile) {
        formData.append('imageFile', imageFile, imageFile.name);
    }

    try {
        const response = await axios.put<OwnerProfileUpdateResponseDto>(`${API_BASE_URL}/users/me`, formData, {
            headers: {
                'Authorization': `Bearer ${token}`,
            }
        });
        return response.data;
    } catch (error) {
        if (axios.isAxiosError(error) && error.response) {
            const apiError = error.response.data as ApiErrorResponse;
            console.error('API Update Owner Profile Error:', apiError);
             let errorMessage = 'Failed to update profile.';
             if (typeof apiError.message === 'string') { errorMessage = apiError.message; }
             else if (typeof apiError.message === 'object' && apiError.message !== null) { errorMessage = Object.values(apiError.message).join(' '); }
             else if (apiError.error) { errorMessage = apiError.error; }
             if (error.response.status === 409) errorMessage = `Update failed: ${errorMessage}`; 
            throw new Error(errorMessage);
        } else {
            console.error('Network or unexpected update owner profile error:', error);
            throw new Error('Failed to update profile due to network or unexpected error.');
        }
    }
};

/**
 * Updates the currently authenticated clinic staff's common profile information (username/avatar).
 * Handles sending data as multipart/form-data.
 *
 * @param {string} token - The JWT authentication token.
 * @param {UserProfileUpdateDto} updateDTO - The data transfer object for the staff update (username).
 * @param {File | null} imageFile - The optional new avatar image file.
 * @returns {Promise<ClinicStaffProfileUpdateResponseDto>}  A promise resolving to the update response (profile + new token).
 * @throws {Error} Throws an error if the update fails.
 */
export const updateCurrentClinicStaffProfile = async (
    token: string,
    updateDTO: UserProfileUpdateDto,
    imageFile: File | null
): Promise<ClinicStaffProfileUpdateResponseDto> => {
     if (!token) { throw new Error("Authentication token required."); }

     const formData = new FormData();
     const dtoBlob = new Blob([JSON.stringify(updateDTO)], { type: 'application/json' });
     formData.append('dto', dtoBlob);

     if (imageFile) {
         formData.append('imageFile', imageFile, imageFile.name);
     }

     try {
         const response = await axios.put<ClinicStaffProfileUpdateResponseDto>(`${API_BASE_URL}/users/me/staff`, formData, {
             headers: {
                 'Authorization': `Bearer ${token}`,
             }
         });
         return response.data;
     } catch (error) {
         if (axios.isAxiosError(error) && error.response) {
              const apiError = error.response.data as ApiErrorResponse;
              console.error('API Update Staff Profile Error:', apiError);
             let errorMessage = 'Failed to update profile.';
              if (typeof apiError.message === 'string') { errorMessage = apiError.message; }
              else if (typeof apiError.message === 'object' && apiError.message !== null) { errorMessage = Object.values(apiError.message).join(' '); }
              else if (apiError.error) { errorMessage = apiError.error; }
               if (error.response.status === 409) errorMessage = `Update failed: ${errorMessage}`;
              throw new Error(errorMessage);
          } else {
              console.error('Network or unexpected update staff profile error:', error);
              throw new Error('Failed to update profile due to network or unexpected error.');
          }
     }
};
