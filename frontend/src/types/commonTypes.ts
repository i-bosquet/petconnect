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
    /** The number of the current page. */
    number: number;
    /** Sorting details for the current response. */
    sort: Sort;
    /** The number of elements actually returned in the current page. */
    numberOfElements: number;
    /** Indicates if this is the first page. */
    first: boolean;
    /** Indicates if the current page content is empty. */
    empty: boolean;
}