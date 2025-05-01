import axios from 'axios';

interface LoginCredentials {
  username: string; 
  password: string;
}

interface AuthResponse {
  username: string;
  message: string;
  jwt: string;
  status: boolean;
}

interface ApiErrorResponse {
    timestamp: number;
    status: number;
    error: string;
    message: string | Record<string, string>; 
    path: string;
}

interface UserProfile {
  id: string;
  username: string;
  email: string;
  roles: string[];
  avatar?: string | null; 
}

interface OwnerRegistrationData {
  username: string;
  email: string;
  password: string;
  phone: string;
}

interface OwnerProfile {
  id: number | string; 
  username: string;
  email: string;
  roles: string[];
  avatar?: string | null; 
  phone: string;
}


const API_BASE_URL = 'http://localhost:8080/api'; 


/**
 * Attempts to log in a user by sending credentials to the backend API.
 *
 * @param {LoginCredentials} credentials - The user's login credentials (username/email and password).
 * @returns {Promise<AuthResponse>} A promise that resolves with the authentication response data (including JWT).
 * @throws {Error | AxiosError} Throws an error if login fails ( network error, 401, 400, 500).
 *         The error object might contain details from the API response if available.
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
 * @returns {Promise<UserProfile>} A promise that resolves with the user's profile data.
 * @throws {Error | AxiosError} Throws an error if fetching fails.
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
 * @param {OwnerRegistrationData} registrationData - The owner's registration details.
 * @returns {Promise<OwnerProfile>} A promise that resolves with the profile data of the newly created owner.
 * @throws {Error | AxiosError} Throws an error if registration fails (network error, 400, 409).
 */
export const registerOwner = async (registrationData: OwnerRegistrationData): Promise<OwnerProfile> => {
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
 * The backend should handle validating the email and sending the reset link.
 *
 * @param {string} email - The email address for which to request a password reset.
 * @returns {Promise<void>} A promise that resolves when the request is successfully sent (backend returns success).
 * @throws {Error | AxiosError} Throws an error if the request fails (e.g., email not found, server error).
 */
export const requestPasswordReset = async (email: string): Promise<void> => {
    try {
        await axios.post(`${API_BASE_URL}/auth/forgot-password`, { email }, { 
            headers: {
                'Content-Type': 'application/json',
            }
        });
        return; 
    } catch (error) {
        if (axios.isAxiosError(error) && error.response) {
            const apiError = error.response.data as ApiErrorResponse;
            console.error('API Forgot Password Error:', apiError);
             let errorMessage = 'Failed to send password reset request.';
             if (error.response.status === 404) {
                 errorMessage = 'No account found with that email address.'; 
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