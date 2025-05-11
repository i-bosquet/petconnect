import { useState, useEffect, JSX, ChangeEvent, FormEvent, useCallback } from 'react';
import { Search, MapPin, PhoneCall, Globe, CheckSquare, Loader2 } from 'lucide-react';
import { findClinics, getClinicCountries } from '@/services/clinicService';
import { ClinicDto, Page, ApiErrorResponse, Country } from '@/types/apiTypes';
import Modal from '@/components/common/Modal';
import Pagination from '@/components/common/Pagination';
import { AxiosError } from 'axios';
import { Button } from '@/components/ui/button'; 

interface ClinicSelectionForAssociationModalProps {
    isOpen: boolean;
    onClose: () => void;
    onClinicSelected: (clinic: ClinicDto) => void;
}

const PAGE_SIZE = 5;

/**
 * ClinicSelectionForAssociationModal - Modal for searching and selecting a clinic
 * when an owner wants to associate a new veterinarian with their pet.
 * @param {ClinicSelectionForAssociationModalProps} props - Component props.
 * @returns {JSX.Element | null} The modal component or null if not open.
 */
const ClinicSelectionForAssociationModal = ({ isOpen, onClose, onClinicSelected }: ClinicSelectionForAssociationModalProps): JSX.Element | null => {
    const [clinics, setClinics] = useState<ClinicDto[]>([]);
    const [isLoading, setIsLoading] = useState<boolean>(false);
    const [error, setError] = useState<string>("");
    const [totalPages, setTotalPages] = useState<number>(0);
    const [currentPage, setCurrentPage] = useState<number>(0);
    const [searchParams, setSearchParams] = useState({ name: "", city: "", country: "" });
    const [availableCountries, setAvailableCountries] = useState<Country[]>([]);
    const [countriesLoading, setCountriesLoading] = useState<boolean>(false);

    const fetchClinicsCallback = useCallback(async (page: number, params: { name: string; city: string; country: string }) => {
        setIsLoading(true);
        setError("");
        try {
            const result: Page<ClinicDto> = await findClinics({
                name: params.name || undefined,
                city: params.city || undefined,
                country: params.country as Country || undefined,
                page,
                size: PAGE_SIZE,
                sort: "name,asc",
            });
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
            setError(errorMsg);
            setClinics([]);
        } finally {
            setIsLoading(false);
        }
    }, []);

    const fetchAvailableCountriesCallback = useCallback(async () => {
        setCountriesLoading(true);
        try {
            const countries = await getClinicCountries();
            setAvailableCountries(countries);
        } catch (err) {
            let errorMsg = "Failed to fetch countries list.";
             if (err instanceof AxiosError && err.response) {
                const apiError = err.response.data as ApiErrorResponse;
                errorMsg = typeof apiError.message === 'string' ? apiError.message : apiError.error || errorMsg;
            } else if (err instanceof Error) {
                errorMsg = err.message;
            }
            setError(errorMsg); 
        } finally {
            setCountriesLoading(false);
        }
    }, []);

    useEffect(() => {
        if (isOpen) {
            setSearchParams({ name: "", city: "", country: "" });
            fetchClinicsCallback(0, { name: "", city: "", country: "" });
            fetchAvailableCountriesCallback();
        }
    }, [isOpen, fetchClinicsCallback, fetchAvailableCountriesCallback]);

    const handleSearchChange = (e: ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
        setSearchParams((prev) => ({ ...prev, [e.target.name]: e.target.value }));
    };

    const handleSearchSubmit = (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        fetchClinicsCallback(0, searchParams);
    };

    const handlePageChange = (newPage: number) => {
        fetchClinicsCallback(newPage, searchParams);
    };

    const handleSelectClinic = (clinic: ClinicDto) => {
        onClinicSelected(clinic); 
    };

    if (!isOpen) return null; 

    return (
        <Modal title="Select Clinic for veterinarian association" onClose={onClose} maxWidth="max-w-2xl">
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
                        >
                            <option value="">All Countries</option>
                            {availableCountries.map((countryValue) => (
                                <option key={countryValue} value={countryValue}>
                                    {countryValue.replace(/_/g, " ")}
                                </option>
                            ))}
                        </select>
                         <div className="absolute inset-y-0 right-0 pr-3 flex items-center pointer-events-none">
                            {countriesLoading ? ( <Loader2 size={16} className="text-gray-400 animate-spin" /> ) : ( <Globe size={18} className="text-gray-500" /> )}
                        </div>
                    </div>
                    <Button type="submit" disabled={isLoading} 
                    className="bg-cyan-800 hover:bg-cyan-600 text-[#FFECAB] font-medium py-2.5 px-4 rounded-lg transition duration-200 flex items-center justify-center sm:w-auto disabled:opacity-50 cursor-pointer">
                        <Search size={18} className="text-[#FFECAB] mr-2" /> Search
                    </Button>
                </div>
            </form>

            <div className="flex-grow overflow-y-auto pr-2 -mr-2 custom-scrollbar min-h-[200px]">
                {isLoading && <p className="text-center text-gray-400 py-4">Loading...</p>}
                {error && <p className="text-center text-red-400 py-4">Error: {error}</p>}
                {!isLoading && !error && clinics.length === 0 && <p className="text-center text-gray-500 py-4">No clinics found.</p>}
                {!isLoading && !error && clinics.length > 0 && (
                    <ul className="space-y-3">
                        {clinics.map((clinic) => (
                            <li key={clinic.id} className="bg-gray-700/50 p-3 rounded-lg border border-gray-600/50 hover:border-cyan-500/60 flex justify-between items-center">
                                <div>
                                    <h4 className="font-semibold text-[#FFECAB]">{clinic.name}</h4>
                                    <div className="text-sm text-gray-300 mt-1">
                                        <p className="flex items-start"><MapPin size={14} className="mr-2 mt-0.5 flex-shrink-0 text-cyan-600" /> {clinic.address}, {clinic.city}</p>
                                        <p className="flex items-center"><PhoneCall size={14} className="mr-2 text-cyan-600" /> {clinic.phone}</p>
                                    </div>
                                </div>
                                <Button onClick={() => handleSelectClinic(clinic)} size="sm" className="bg-[#FFECAB] hover:bg-cyan-700 text-[#090D1A] hover:text-[#FFECAB] cursor-pointer ">
                                    <CheckSquare size={16} className="mr-2"/> Select
                                </Button>
                            </li>
                        ))}
                    </ul>
                )}
            </div>
            <Pagination currentPage={currentPage} totalPages={totalPages} onPageChange={handlePageChange} isLoading={isLoading} />
        </Modal>
    );
};
export default ClinicSelectionForAssociationModal;