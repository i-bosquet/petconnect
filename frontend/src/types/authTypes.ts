/**
 * Structure of the response received after successful authentication (login).
 */
export interface AuthResponse {
  username: string;
  message: string;
  jwt: string;
  status: boolean;
}

/**
 * Profile details specific to a Pet Owner user.
 */
export interface OwnerProfile {
  id: number | string;
  username: string;
  email: string;
  roles: string[];
  avatar: string | null;
  phone: string | null;
}

/**
 * Profile details specific to a Clinic Staff user (Vet or Admin).
 */
export interface ClinicStaffProfile {
  id: number | string;
  username: string;
  email: string;
  roles: string[];
  avatar: string | null;
  name: string;
  surname: string;
  isActive: boolean;
  clinicId: number | string;
  clinicName: string;
  licenseNumber?: string | null;
  vetPublicKey?: string | null;
  createdAt?: string | null;
  createdBy?: string | null;
  updatedAt?: string | null;
  updatedBy?: string | null;
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
  username: string;
  email: string;
  password: string;
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

/**
 * Summary information for a Pet Owner.
 * Used to embed owner details within other DTOs like PetProfileDto.
 */
export interface OwnerSummaryDto {
    id: number | string;
    username: string;
    email: string;
    phone: string | null; 
}