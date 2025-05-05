import axios from 'axios';
import { API_BASE_URL } from '../config';
import {
    AuthResponse,
    ApiErrorResponse,
    UserProfile,
    OwnerRegistrationDto, 
    OwnerProfile,
} from '../types/apiTypes';

interface LoginCredentials {
  username: string; 
  password: string;
}

// Data needed for initiating password reset
interface ResetPasswordRequestData {
    email: string;
  }

  // Data needed for completing password reset
interface ResetPasswordData {
    token: string;
    newPassword: string;
    confirmPassword: string; 
}

// Simple response for password reset confirmation
interface ResetPasswordResponse {
    message: string;
}

/**
 * Attempts to log in a user by sending credentials to the backend API.
 *
 * @param {LoginCredentials} credentials - The user's login credentials (username/email and password).
 * @returns {Promise<AuthResponse>} A promise that resolves with the authentication response data (including JWT).
 * @throws {Error} Throws an error if login fails.
 */
export const loginUser = async (credentials: LoginCredentials): Promise<AuthResponse> => {
    try {
      const response = await axios.post<AuthResponse>(`${API_BASE_URL}/auth/login`, credentials, {
          headers: {
              'Content-Type': 'application/json',
          }
      });
      return response.data;
    } catch (error) {
      if (axios.isAxiosError(error) && error.response) {
          const apiError = error.response.data as ApiErrorResponse;
          console.error('API Login Error:', apiError);
          throw new Error(typeof apiError.message === 'string' ? apiError.message : apiError.error || 'Login failed');
      } else {
        console.error('Network or unexpected login error:', error);
        throw new Error('Login failed due to network or unexpected error.');
      }
    }
};

/**
 * Fetches the profile of the currently authenticated user using the provided JWT token.
 *
 * @param {string} token - The JWT token for authorization.
 * @returns {Promise<UserProfile>} A promise that resolves with the user's profile data (OwnerProfile or ClinicStaffProfile).
 * @throws {Error} Throws an error if fetching fails.
 */
export const getCurrentUserProfile = async (token: string): Promise<UserProfile> => {
    if (!token) {
        throw new Error("No authentication token provided.");
    }
    try {
        const response = await axios.get<UserProfile>(`${API_BASE_URL}/users/me`, {
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json',
            }
        });
        return response.data;
    } catch (error) {
        if (axios.isAxiosError(error) && error.response) {
            const apiError = error.response.data as ApiErrorResponse;
            console.error('API Get Profile Error:', apiError);
            throw new Error(typeof apiError.message === 'string' ? apiError.message : apiError.error || 'Failed to fetch user profile');
        } else {
            console.error('Network or unexpected get profile error:', error);
            throw new Error('Failed to fetch user profile due to network or unexpected error.');
        }
    }
};

/**
 * Registers a new Owner user by sending registration data to the backend API.
 *
 * @param {OwnerRegistrationDto} registrationData - The owner's registration details matching the backend DTO.
 * @returns {Promise<OwnerProfile>} A promise that resolves with the profile data of the newly created owner.
 * @throws {Error} Throws an error if registration fails.
 */
export const registerOwner = async (registrationData: OwnerRegistrationDto): Promise<OwnerProfile> => {
  try {
      const response = await axios.post<OwnerProfile>(`${API_BASE_URL}/auth/register`, registrationData, {
          headers: {
              'Content-Type': 'application/json',
          }
      });
      return response.data;
  } catch (error) {
      if (axios.isAxiosError(error) && error.response) {
          const apiError = error.response.data as ApiErrorResponse;
          console.error('API Registration Error:', apiError);
           let errorMessage = 'Registration failed.';
           if (typeof apiError.message === 'string') {
               errorMessage = apiError.message;
           } else if (typeof apiError.message === 'object' && apiError.message !== null) {
               errorMessage = Object.values(apiError.message).join(' ');
           } else if (apiError.error) {
               errorMessage = apiError.error;
           }
          throw new Error(errorMessage);
      } else {
          console.error('Network or unexpected registration error:', error);
          throw new Error('Registration failed due to network or unexpected error.');
      }
  }
};

/**
 * Sends a request to the backend to initiate the password reset process for the given email.
 *
 * @param {string} email - The email address for which to request a password reset.
 * @returns {Promise<void>} A promise that resolves when the request is successfully processed by the backend.
 * @throws {Error} Throws an error if the request fails.
 */
export const requestPasswordReset = async (requestData: ResetPasswordRequestData): Promise<void> => {
    try {
        await axios.post(`${API_BASE_URL}/auth/forgot-password`, requestData, {
            headers: {
                'Content-Type': 'application/json',
            }
        });
    } catch (error) {
        if (axios.isAxiosError(error) && error.response) {
            const apiError = error.response.data as ApiErrorResponse;
            console.error('API Forgot Password Error:', apiError);
             let errorMessage = 'Failed to send password reset request.';
              if (error.response.status === 400) {
                 errorMessage = 'Invalid email format provided.';
              } else if (typeof apiError.message === 'string') {
                 errorMessage = apiError.message;
             } else if (apiError.error) {
                errorMessage = apiError.error;
             }
            throw new Error(errorMessage);
        } else {
            console.error('Network or unexpected forgot password error:', error);
            throw new Error('Password reset request failed due to network or unexpected error.');
        }
    }
};

/**
 * Resets the user's password using the provided token and new password.
 *
 * @param {ResetPasswordData} resetData - Includes token, newPassword, confirmPassword.
 * @returns {Promise<ResetPasswordResponse>} A promise resolving with a success message interface.
 * @throws {Error} Throws an error if reset fails.
 */
export const resetPassword = async (resetData: ResetPasswordData): Promise<ResetPasswordResponse> => {
    if (!resetData.token) {
        throw new Error("Reset token is missing.");
    }
    if (resetData.newPassword !== resetData.confirmPassword) {
         throw new Error("Passwords do not match.");
    }

    try {
         const response = await axios.post<ResetPasswordResponse>(`${API_BASE_URL}/auth/reset-password`, {
             token: resetData.token,
             newPassword: resetData.newPassword,
             confirmPassword: resetData.confirmPassword // Send confirmation to backend if API expects it
         }, {
             headers: {
                 'Content-Type': 'application/json',
             }
         });
         return response.data;
    } catch (error) {
        if (axios.isAxiosError(error) && error.response) {
            const apiError = error.response.data as ApiErrorResponse;
            console.error('API Reset Password Error:', apiError);
             let errorMessage = 'Failed to reset password.';
             if (error.response.status === 400 && typeof apiError.message === 'string') {
                errorMessage = apiError.message; 
             } else if (apiError.error) {
                errorMessage = apiError.error;
             }
            throw new Error(errorMessage);
        } else {
            console.error('Network or unexpected reset password error:', error);
            throw new Error('Password reset failed due to network or unexpected error.');
        }
    }
};