import { Gender, PetStatus, Specie, RecordType } from './enumTypes'; 

// --- General ---

/**
 * Standard structure for error responses from the API.
 */
export interface ApiErrorResponse {
    /** Timestamp when the error occurred (epoch milliseconds). */
    timestamp: number;
    /** HTTP status code. */
    status: number;
    /** General error category ("Not Found", "Bad Request", ...). */
    error: string;
    /** Detailed error message, or a map of field-specific validation errors. */
    message: string | Record<string, string>;
    /** The API path where the error occurred. */
    path: string;
}

/**
 * Represents sorting information within a Pageable object.
 */
interface Sort {
    /** Indicates if the results are actively sorted. */
    sorted: boolean;
    /** Indicates if the results are not sorted. */
    unsorted: boolean;
    /** Indicates if sorting information is empty (usually means unsorted). */
    empty: boolean;
}

/**
 * Represents pagination information returned by the API.
 */
interface Pageable {
    /** The number of the current page (0-indexed). */
    pageNumber: number;
    /** The number of items requested per page. */
    pageSize: number;
    /** Sorting details for the current page. */
    sort: Sort;
    /** The offset of the first element in the current page. */
    offset: number;
    /** Indicates if the request was paged. */
    paged: boolean;
    /** Indicates if the request was not paged. */
    unpaged: boolean;
}

/**
 * Represents a paginated response from the API.
 * @template T The type of the content items within the page.
 */
export interface Page<T> {
    /** The array of items for the current page. */
    content: T[];
    /** Pagination details. */
    pageable: Pageable;
    /** The total number of pages available. */
    totalPages: number;
    /** The total number of items across all pages. */
    totalElements: number;
    /** Indicates if this is the last page. */
    last: boolean;
    /** The number of items on the current page. */
    size: number;
    /** The number of the current page (same as pageable.pageNumber). */
    number: number;
    /** Sorting details for the current response (same as pageable.sort). */
    sort: Sort;
    /** The number of elements actually returned in the current page. */
    numberOfElements: number;
    /** Indicates if this is the first page. */
    first: boolean;
    /** Indicates if the current page content is empty. */
    empty: boolean;
}

// --- Auth ---
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
    /** The staff member's surname (last name). */
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
export interface OwnerRegistrationDto { // <<<--- AÑADIR ESTA INTERFAZ
    /** The user's unique username. */
    username: string;
    /** The user's unique email address. */
    email: string;
    /** The user's chosen password (plain text, will be hashed by backend). */
    password: string;
    /** The owner's contact phone number. */
    phone: string;
}

// --- Clinic ---
/**
 * Data Transfer Object representing a Clinic's details.
 */
export interface ClinicDto {
    /** The unique ID of the clinic. */
    id: number;
    /** The official name of the clinic. */
    name: string;
    /** The full street address of the clinic. */
    address: string;
    /** The city where the clinic is located. */
    city: string;
    /** The country where the clinic is located (as an enum string value). */
    country: string; // Matches Country enum string value from backend
    /** The clinic's primary contact phone number. */
    phone: string;
    /* Note: publicKey is excluded as it's likely not needed often in UI lists/summaries */
}

// --- Pet / Breed ---
/**
 * Summary information for a Veterinarian, often used when listing vets associated with a Pet.
 */
export interface VetSummaryDto {
    /** The unique ID of the Vet user. */
    id: number | string;
    /** The Vet's first name. */
    name: string | null; // Name might be null if data is inconsistent
    /** The Vet's surname (last name). */
    surname: string | null; // Surname might be null
}

/**
 * Detailed profile information for a Pet.
 */
export interface PetProfileDto {
    /** The unique ID of the pet. */
    id: number | string;
    /** The pet's given name. */
    name: string;
    /** The species of the pet (DOG, CAT, ...). */
    specie: Specie;
    /** Description of the pet's color or coat. */
    color?: string | null;
    /** The gender of the pet. */
    gender?: Gender | null;
    /** The pet's date of birth (ISO string format, "YYYY-MM-DD"). */
    birthDate?: string | null;
    /** The pet's unique microchip number. */
    microchip?: string | null;
    /** The URL path to the pet's image (guaranteed to have a value, possibly default). */
    image: string;
    /** The current status of the pet (PENDING, ACTIVE, INACTIVE). */
    status: PetStatus;
    /** The ID of the user who owns this pet. */
    ownerId: number | string;
    /** The username of the user who owns this pet. */
    ownerUsername: string;
    /** The ID of the pet's assigned breed. */
    breedId?: number | string | null;
    /** The name of the pet's assigned breed. */
    breedName?: string | null;
    /** If status is PENDING, the ID of the clinic awaiting activation. */
    pendingActivationClinicId?: number | string | null;
    /** A list of veterinarians associated with providing care for this pet. */
    associatedVets: VetSummaryDto[]; // Changed from Set to Array for easier handling in JS/TS
    /** Timestamp when the pet record was created (ISO string format) */
    createdAt?: string | null;
    /** Timestamp when the pet record was last updated (ISO string format) */
    updatedAt?: string | null;
}

/**
 * Simplified view of a Breed, used for selection lists.
 */
export interface BreedDto {
    /** The unique ID of the breed. */
    id: number | string;
    /** The common name of the breed. */
    name: string;
    /** Optional URL to a representative image of the breed. */
    imageUrl?: string | null;
}

/**
 * Represents the data collected from the frontend form for registering a new pet.
 * This differs slightly from the backend DTO as it reflects form state.
 */
export interface PetRegistrationData {
    /** The chosen name for the pet. */
    name: string;
    /** The species of the pet. */
    specie: Specie;
    /** The pet's date of birth, as a string ("YYYY-MM-DD"). */
    birthDate: string;
    /** Optional ID of the selected breed. */
    breedId?: number | string | null;
    /** Optional image data (might be File object before upload, or null). */
    image?: File | null | string; // Allow File for upload, string if path already known? Or handle separately. Usually just File or null.
    /** Optional color description. */
    color?: string | null;
    /** Optional gender of the pet. */
    gender?: Gender | null;
    /** Optional microchip number. */
    microchip?: string | null;
  }

// --- Record / Vaccine ---
/**
 * Summary information for a medical Record, typically used within certificate details.
 */
export interface RecordSummaryDto {
    /** The unique ID of the medical record. */
    id: number | string;
    /** The type of the medical record ("VACCINE", "ANNUAL_CHECK", ...). */
    type: string; // RecordType enum as string
    /** A truncated description from the record. */
    description?: string | null;
    /** Timestamp when the record was created (ISO string format). */
    createdAt?: string | null;
}

/**
 * Detailed view of a Medical Record.
 */
export interface RecordViewDto {
    id: number | string;
    type: RecordType;
    description?: string | null;
    vetSignature?: string | null; 
    createdAt: string; 
    creator: UserProfile; 
    vaccine?: VaccineViewDto | null;
  }

/**
 * Detailed view of Vaccine information within a Record.
 */
 export interface VaccineViewDto {
    name: string;
    validity?: number | null; // Years
    laboratory?: string | null;
    batchNumber?: string | null;
    isRabiesVaccine: boolean;
  }

  /**
   * Data needed for requesting temporary access.
   */
  export interface TemporaryAccessRequestData {
      durationString: string; 
  }

  /**
   * Response containing the temporary access token.
   */
  export interface TemporaryAccessTokenResponse {
      token: string;
  }