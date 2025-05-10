/**
 * Standard structure for error responses from the API.
 */
export interface ApiErrorResponse {
    timestamp: number;
    status: number;
    error: string;
    message: string | Record<string, string>;
    path: string;
}

/**
 * Represents sorting information within a Pageable object.
 */
interface Sort {
    sorted: boolean;
    unsorted: boolean;
    empty: boolean;
}

/**
 * Represents pagination information returned by the API.
 */
interface Pageable {
    pageNumber: number;
    pageSize: number;
    sort: Sort;
    offset: number;
    paged: boolean;
    unpaged: boolean;
}

/**
 * Represents a paginated response from the API.
 * @template T The type of the content items within the page.
 */
export interface Page<T> {
    content: T[];
    pageable: Pageable;
    totalPages: number;
    totalElements: number;
    last: boolean;
    size: number;
    number: number;
    sort: Sort;
    numberOfElements: number;
    first: boolean;
    empty: boolean;
}