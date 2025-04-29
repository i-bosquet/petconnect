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

type UserProfile = any;

const API_BASE_URL = 'http://localhost:8080/api'; 

/**
 * Attempts to log in a user by sending credentials to the backend API.
 *
 * @param {LoginCredentials} credentials - The user's login credentials (username/email and password).
 * @returns {Promise<AuthResponse>} A promise that resolves with the authentication response data (including JWT).
 * @throws {Error | AxiosError} Throws an error if login fails (e.g., network error, 401, 400, 500).
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