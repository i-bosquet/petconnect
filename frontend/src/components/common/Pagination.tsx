import { JSX } from 'react';
import { ChevronLeft, ChevronRight } from 'lucide-react';

/**
 * Props for the reusable Pagination component.
 */
interface PaginationProps {
    /** Current page number (0-indexed). */
    currentPage: number;
    /** Total number of pages available. */
    totalPages: number;
    /** Callback function triggered when a page change is requested. Receives the new page number (0-indexed). */
    onPageChange: (newPage: number) => void;
    /** Optional flag to disable buttons while data is loading. */
    isLoading?: boolean;
}

/**
 * Pagination - A reusable component for displaying pagination controls.
 * Shows 'Previous' and 'Next' buttons and the current page status.
 * Disables buttons appropriately based on current page, total pages, and loading state.
 *
 * @param {PaginationProps} props - Component props.
 * @returns {JSX.Element | null} The pagination controls, or null if not needed (totalPages <= 1).
 */
const Pagination = ({
    currentPage,
    totalPages,
    onPageChange,
    isLoading = false
}: PaginationProps): JSX.Element | null => {

    // Don't render pagination if there's only one page or less
    if (totalPages <= 1) {
        return null;
    }

    const handlePrevious = () => {
        if (currentPage > 0) {
            onPageChange(currentPage - 1);
        }
    };

    const handleNext = () => {
        if (currentPage < totalPages - 1) {
            onPageChange(currentPage + 1);
        }
    };

    return (
        <div className="mt-4 sm:mt-6 pt-4 border-t border-gray-700 flex justify-center items-center space-x-3">
            <button
                onClick={handlePrevious}
                disabled={currentPage === 0 || isLoading}
                className="px-3 py-1.5 rounded-md bg-gray-600 hover:bg-gray-500 text-white flex items-center disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                aria-label="Previous page"
            >
                <ChevronLeft size={18} className="mr-1" />
                Previous
            </button>
            <span className="text-gray-400 text-sm px-2">
                Page {currentPage + 1} of {totalPages}
            </span>
            <button
                onClick={handleNext}
                disabled={currentPage >= totalPages - 1 || isLoading}
                className="px-3 py-1.5 rounded-md bg-gray-600 hover:bg-gray-500 text-white flex items-center disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                aria-label="Next page"
            >
                Next
                <ChevronRight size={18} className="ml-1" />
            </button>
        </div>
    );
};

export default Pagination;