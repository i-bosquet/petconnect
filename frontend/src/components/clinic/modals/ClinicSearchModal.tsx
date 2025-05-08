import { useState, useEffect, JSX, ChangeEvent, FormEvent } from 'react';
import { Search, MapPin, PhoneCall, Globe } from 'lucide-react';
import { findClinics, getClinicCountries } from '@/services/clinicService'; 
import { ClinicDto, Page, ApiErrorResponse, Country } from '@/types/apiTypes'; 
import Modal from '@/components/common/Modal'; 
import Pagination from '@/components/common/Pagination'; 
import { AxiosError } from 'axios';

// --- Component Props ---
interface ClinicSearchModalProps {
    isOpen: boolean;
    onClose: () => void;
}

const PAGE_SIZE = 5; // Number of clinics per page

/**
 * ClinicSearchModal - Encapsulates the UI and logic for searching veterinary clinics.
 * Fetches available countries and allows users to filter clinics by name, city, and country.
 * Displays results paginated within a modal overlay.
 *
 * @param {ClinicSearchModalProps} props - Component props.
 * @returns {JSX.Element | null} The modal component or null if not open.
 */
const ClinicSearchModal = ({ isOpen, onClose }: ClinicSearchModalProps): JSX.Element | null => {
    // --- State ---
    const [clinics, setClinics] = useState<ClinicDto[]>([]);
    const [isLoading, setIsLoading] = useState<boolean>(false);
    const [error, setError] = useState<string>("");
    const [totalPages, setTotalPages] = useState<number>(0);
    const [currentPage, setCurrentPage] = useState<number>(0);
    const [searchParams, setSearchParams] = useState({ name: "", city: "", country: "" });
    const [availableCountries, setAvailableCountries] = useState<Country[]>([]);
    const [countriesLoading, setCountriesLoading] = useState<boolean>(false);
    const [countriesError, setCountriesError] = useState<string>("");

    // --- Effects ---

    /**
     * useEffect hook to fetch initial data (clinics page 0 and countries)
     * when the modal becomes visible (isOpen changes to true).
     * It also resets the state when the modal opens.
     */
    useEffect(() => {
        if (isOpen) {
            // Reset State
            setClinics([]);
            setError("");
            setCurrentPage(0);
            setTotalPages(0);
            setSearchParams({ name: "", city: "", country: "" });
            setAvailableCountries([]);
            setCountriesError("");

            // Fetch initial data
            console.log("Modal opened, fetching initial clinics and countries...");
            fetchClinics(0, { name: "", city: "", country: "" });
            fetchAvailableCountries();
        }
    }, [isOpen]); // Dependency array includes isOpen

    // --- Data Fetching ---

    /**
     * Fetches the distinct list of countries with registered clinics.
     */
    const fetchAvailableCountries = async () => {
        setCountriesLoading(true);
        setCountriesError("");
        console.log("Fetching available countries...");
        try {
            const countries = await getClinicCountries();
            setAvailableCountries(countries);
            console.log("Fetched countries:", countries);
        } catch (err) {
            const errorMsg = err instanceof Error ? err.message : "Failed to load countries list.";
            console.error("fetchAvailableCountries error:", errorMsg);
            setCountriesError(errorMsg);
        } finally {
            setCountriesLoading(false);
        }
    };

    /**
     * Fetches clinics from the API based on search parameters and page number.
     */
    const fetchClinics = async (page: number, params: { name: string; city: string; country: string }) => {
        setIsLoading(true);
        setError("");
        console.log(`Fetching clinics - Page: ${page}, Params:`, params);
        try {
            const result: Page<ClinicDto> = await findClinics({
                name: params.name || undefined, // Send undefined if empty
                city: params.city || undefined,
                country: params.country as Country || undefined,
                page,
                size: PAGE_SIZE,
                sort: "name,asc",
            });
            console.log("Fetched clinics result:", result);
            setClinics(result.content);
            setTotalPages(result.totalPages);
            setCurrentPage(result.number);
        } catch (err) {
            let errorMsg = "Failed to fetch clinic data.";
            if (err instanceof AxiosError && err.response) {
                const apiError = err.response.data as ApiErrorResponse;
                errorMsg = typeof apiError.message === 'string' ? apiError.message : apiError.error || errorMsg;
            } else if (err instanceof Error) {
                errorMsg = err.message;
            }
            console.error("fetchClinics error:", errorMsg);
            setError(errorMsg);
            setClinics([]); // Clear results on error
        } finally {
            setIsLoading(false);
        }
    };

    // --- Event Handlers ---

    /** Handles changes in search inputs. */
    const handleSearchChange = (e: ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
        const { name, value } = e.target;
        setSearchParams((prev) => ({ ...prev, [name]: value }));
    };

    /** Handles search form submission. */
    const handleSearchSubmit = (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        setCurrentPage(0); // Reset to first page
        fetchClinics(0, searchParams);
    };

    /** Handles pagination page changes. */
    const handlePageChange = (newPage: number) => {
        fetchClinics(newPage, searchParams);
    };

    // --- Render Logic ---
    if (!isOpen) {
        return null;
    }

    return (
        <Modal
            title="Search Veterinary Clinics"
            onClose={onClose}
            maxWidth="max-w-2xl" // Adjust as needed
        >
            {/* Search Form */}
            <form onSubmit={handleSearchSubmit} className="mb-4 sm:mb-6">
                <div className="flex flex-col sm:flex-row gap-3 sm:gap-4">
                    {/* Name Input */}
                    <div className="relative flex-grow">
                        <input
                            type="text"
                            name="name"
                            value={searchParams.name}
                            onChange={handleSearchChange}
                             className="bg-gray-700 text-white rounded-lg pl-3 pr-4 py-2.5 w-full border border-gray-600 focus:outline-none focus:ring-2 focus:ring-cyan-500 focus:border-transparent placeholder-gray-400"
                            placeholder="Clinic Name"
                            aria-label="Clinic Name Search"
                        />
                    </div>
                    {/* City Input */}
                     <div className="relative flex-grow-[0.5] sm:flex-grow-[0.6]">
                        <input
                            type="text"
                            name="city"
                            value={searchParams.city}
                            onChange={handleSearchChange}
                             className="bg-gray-700 text-white rounded-lg pl-3 pr-4 py-2.5 w-full border border-gray-600 focus:outline-none focus:ring-2 focus:ring-cyan-500 focus:border-transparent placeholder-gray-400"
                            placeholder="City"
                            aria-label="City Search"
                        />
                    </div>
                    {/* Country Select */}
                     <div className="relative flex-grow-[0.5] sm:flex-grow-[0.4]">
                        <select
                            name="country"
                            value={searchParams.country}
                            onChange={handleSearchChange}
                            disabled={countriesLoading}
                             className="bg-gray-700 text-white rounded-lg pl-3 pr-8 py-2.5 w-full border border-gray-600 focus:outline-none focus:ring-2 focus:ring-cyan-500 focus:border-transparent appearance-none disabled:opacity-50"
                             aria-label="Country Filter"
                        >
                            <option value="">All Countries</option>
                            {availableCountries.map((countryValue) => (
                                <option key={countryValue} value={countryValue}>
                                    {countryValue.replace(/_/g, " ")}
                                </option>
                            ))}
                        </select>
                        <div className="absolute inset-y-0 right-0 pr-3 flex items-center pointer-events-none">
                            {countriesLoading ? (
                               <svg className="animate-spin h-4 w-4 text-gray-400" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                                   <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                                   <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                               </svg>
                            ) : (
                                <Globe size={18} className="text-gray-500" />
                            )}
                        </div>
                    </div>
                    {countriesError && (
                        <p className="text-xs text-red-400 col-span-full sm:col-span-1 mt-1">
                            {countriesError}
                        </p>
                    )}
                    {/* Search Button */}
                    <button
                        type="submit"
                        disabled={isLoading}
                        className="bg-cyan-800 hover:opacity-80 text-white font-medium py-2.5 px-4 rounded-lg transition duration-200 flex items-center justify-center sm:w-auto disabled:opacity-50"
                        aria-label="Submit Clinic Search"
                    >
                        <Search size={18} className="mr-1 sm:mr-2" /> Search
                    </button>
                </div>
            </form>

            {/* Results Area */}
             <div className="flex-grow overflow-y-auto pr-2 -mr-2 custom-scrollbar min-h-[200px]"> {/* Ensure minimum height */}
                {isLoading && (
                    <p className="text-center text-gray-400 py-4">Loading clinics...</p>
                )}
                {error && (
                    <p className="text-center text-red-400 py-4">Error: {error}</p>
                )}
                {!isLoading && !error && clinics.length === 0 && (
                    <p className="text-center text-gray-500 py-4">No clinics found matching your criteria.</p>
                )}
                {!isLoading && !error && clinics.length > 0 && (
                    <ul className="space-y-3">
                        {clinics.map((clinic) => (
                            <li key={clinic.id} className="bg-gray-700/50 p-3 sm:p-4 rounded-lg border border-gray-600/50 hover:border-cyan-500/60 transition-colors">
                                <h4 className="font-semibold text-[#FFECAB] text-base sm:text-lg">{clinic.name}</h4>
                                <div className="text-sm text-gray-300 mt-1 space-y-1">
                                    <p className="flex items-start">
                                        <MapPin size={14} className="mr-2 mt-0.5 flex-shrink-0 text-cyan-600" />
                                        <span>{clinic.address}, {clinic.city}, {clinic.country.replace(/_/g, " ")}</span>
                                    </p>
                                    <p className="flex items-center">
                                        <PhoneCall size={14} className="mr-2 text-cyan-600" />
                                        {clinic.phone}
                                    </p>
                                </div>
                            </li>
                        ))}
                    </ul>
                )}
            </div>

            {/* Pagination Component */}
            <Pagination
               currentPage={currentPage}
               totalPages={totalPages}
               onPageChange={handlePageChange}
               isLoading={isLoading}
           />
        </Modal>
    );
};

export default ClinicSearchModal;