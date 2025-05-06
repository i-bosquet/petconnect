/**
 * Structure of the response received after successful authentication (login).
 */
export interface AuthResponse {
  /** The username of the authenticated user. */
  username: string;
  /** A confirmation message from the server. */
  message: string;
  /** The JSON Web Token (JWT) for subsequent authenticated requests. */
  jwt: string;
  /** Status indicating authentication success (true). */
  status: boolean;
}

/**
 * Profile details specific to a Pet Owner user.
 */
export interface OwnerProfile {
  /** The unique ID of the user. */
  id: number | string;
  /** The user's unique username. */
  username: string;
  /** The user's unique email address. */
  email: string;
  /** An array containing the user's assigned roles (typically ["OWNER"]). */
  roles: string[];
  /** The URL path to the user's avatar image. */
  avatar: string;
  /** The owner's contact phone number. */
  phone: string;
}

/**
 * Profile details specific to a Clinic Staff user (Vet or Admin).
 */
export interface ClinicStaffProfile {
  /** The unique ID of the user. */
  id: number | string;
  /** The user's unique username. */
  username: string;
  /** The user's unique email address. */
  email: string;
  /** An array containing the user's assigned roles ( ["VET"], ["ADMIN"], ...). */
  roles: string[];
  /** The URL path to the user's avatar image. */
  avatar: string;
  /** The staff member's first name. */
  name: string;
  /** The staff member's surname. */
  surname: string;
  /** Indicates if the staff account is currently active. */
  isActive: boolean;
  /** The ID of the clinic this staff member belongs to. */
  clinicId: number | string;
  /** The name of the clinic this staff member belongs to. */
  clinicName: string;
  /** The veterinarian's license number (only applicable if role is VET). */
  licenseNumber?: string | null;
  /** The veterinarian's public cryptographic key (only applicable if role is VET). */
  vetPublicKey?: string | null;
}

/**
 * Represents a user profile which can be either an OwnerProfile or a ClinicStaffProfile.
 */
export type UserProfile = OwnerProfile | ClinicStaffProfile;

/**
 * Data Transfer Object for registering a new Pet Owner,
 * matching the backend API endpoint requirement.
 */
export interface OwnerRegistrationDto {
  /** The user's unique username. */
  username: string;
  /** The user's unique email address. */
  email: string;
  /** The user's chosen password (plain text, will be hashed by backend). */
  password: string;
  /** The owner's contact phone number. */
  phone: string;
}

/**
 * Data needed for initiating password reset (payload for the request).
 */
export interface ResetPasswordRequestData {
  email: string;
}

/**
 * Data needed for completing password reset (payload for the request).
 */
export interface ResetPasswordData {
  token: string;
  newPassword: string;
  confirmPassword: string;
}

/**
 * Simple structure for the response after successfully requesting/completing a password reset.
 */
export interface ResetPasswordResponse {
  message: string;
}

/**
 * Interface for the data expected in the login form/service call.
 */
export interface LoginCredentials {
  username: string;
  password: string;
}

/**
 * DTO for an Owner updating their specific profile information.
 */
export interface OwnerProfileUpdateDto {
    username?: string | null; 
    phone?: string | null;
}

/**
 * DTO for updating common user profile information (used by Staff).
 */
export interface UserProfileUpdateDto {
    username?: string | null;
}

/**
 * DTO for the response after a successful Owner profile update.
 */
export interface OwnerProfileUpdateResponseDto { 
  profile: OwnerProfile 
  newToken: string | null;
}

/**
 * DTO for the response after a successful ClinicStaff profile update.
 */
export interface ClinicStaffProfileUpdateResponseDto { 
  profile: ClinicStaffProfile
  newToken: string | null;
}