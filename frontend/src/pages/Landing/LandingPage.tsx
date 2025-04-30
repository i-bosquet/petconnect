import { useState, JSX, ChangeEvent, FormEvent } from "react";
import { Link } from "react-router-dom";
import { Search, X, Hospital, MapPin, PhoneCall, Globe } from "lucide-react";
import {
  findClinics,
  getClinicCountries,
  ClinicDto,
  Page,
} from "../../services/clinicService";
import { Country } from "../../types/enums";
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "../../components/ui/tooltip";

/**
 * LandingPage - The main entry point and public face of the PetConnect application
 * This component serves as the website's homepage with brand messaging and access options
 * Includes branding, login/signup options, and a modal to search for clinics.
 *
 * @returns {JSX.Element} The landing page with login and signup options
 */
const LandingPage = (): JSX.Element => {
  const [isModalOpen, setIsModalOpen] = useState<boolean>(false);
  const [clinics, setClinics] = useState<ClinicDto[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [error, setError] = useState<string>("");
  const [totalPages, setTotalPages] = useState<number>(0);
  const [currentPage, setCurrentPage] = useState<number>(0);
  const [searchParams, setSearchParams] = useState({
    name: "",
    city: "",
    country: "",
  });
  const [availableCountries, setAvailableCountries] = useState<Country[]>([]);
  const [countriesLoading, setCountriesLoading] = useState<boolean>(false);
  const [countriesError, setCountriesError] = useState<string>("");

  const PAGE_SIZE = 5; // Number of clinics per page in the modal

  /**
   * Opens the clinic search modal.
   * Optionally triggers an initial search.
   */
  const openModal = () => {
    setIsModalOpen(true);
    // Reset state when opening modal
    setClinics([]);
    setError("");
    setCurrentPage(0);
    setTotalPages(0);
    setSearchParams({ name: "", city: "", country: "" });
    setAvailableCountries([]); // Reset countries
    setCountriesError("");
    // Fetch initial clinics and available countries
    fetchClinics(0, { name: "", city: "", country: "" });
    fetchAvailableCountries();
  };

  /**
   * Fetches the distinct list of countries with registered clinics.
   */
  const fetchAvailableCountries = async () => {
    setCountriesLoading(true);
    setCountriesError("");
    try {
      const countries = await getClinicCountries();
      setAvailableCountries(countries);
    } catch (err) {
      setCountriesError(
        err instanceof Error ? err.message : "Failed to load countries."
      );
    } finally {
      setCountriesLoading(false);
    }
  };

  /**
   * Closes the clinic search modal.
   */
  const closeModal = () => {
    setIsModalOpen(false);
  };

  /**
   * Fetches clinics from the API based on current search parameters and page number.
   *
   * @param {number} page - The page number to fetch (0-indexed).
   * @param {object} params - The current search filter parameters.
   */
  const fetchClinics = async (
    page: number,
    params: { name: string; city: string; country: string }
  ) => {
    setIsLoading(true);
    setError("");
    try {
      const result: Page<ClinicDto> = await findClinics({
        ...params,
        country: params.country as Country,
        page,
        size: PAGE_SIZE,
        sort: "name,asc",
      });
      setClinics(result.content);
      setTotalPages(result.totalPages);
      setCurrentPage(result.number);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load clinics.");
      setClinics([]); // Clear previous results on error
    } finally {
      setIsLoading(false);
    }
  };

  /**
   * Handles changes in the search input fields.
   * Updates the searchParams state.
   *
   * @param {ChangeEvent<HTMLInputElement | HTMLSelectElement>} e - The input change event.
   */
  const handleSearchChange = (
    e: ChangeEvent<HTMLInputElement | HTMLSelectElement>
  ) => {
    const { name, value } = e.target;
    setSearchParams((prev) => ({ ...prev, [name]: value }));
  };

  /**
   * Handles the submission of the search form.
   * Triggers fetching clinics with the current search parameters, resetting to page 0.
   *
   * @param {FormEvent<HTMLFormElement>} e - The form submit event.
   */
  const handleSearchSubmit = (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setCurrentPage(0); // Reset to first page on new search
    fetchClinics(0, searchParams);
  };

  /**
   * Handles changing the page in the pagination.
   * @param {number} newPage - The new page number (0-indexed).
   */
  const handlePageChange = (newPage: number) => {
    if (newPage >= 0 && newPage < totalPages) {
      fetchClinics(newPage, searchParams);
    }
  };

  return (
    <TooltipProvider>
      <div className="min-h-screen bg-gradient-to-br from-[#070913] to-[#0c1225] flex flex-col relative">
        {/* Clinic Search Button */}
        <Tooltip>
          <TooltipTrigger asChild>
            <button
              onClick={openModal}
              className="absolute top-6 right-6 bg-cyan-800 hover:opacity-80 w-10 h-10 sm:w-12 sm:h-12 rounded-full flex items-center justify-center transition-colors duration-200 shadow-lg z-10"
              aria-label="Search clinics"
            >
              <Hospital size={24} className="text-[#FFECAB]" />
            </button>
          </TooltipTrigger>
          <TooltipContent className="bg-gray-800 text-white border border-cyan-700">
            <p>Search Clinics</p>
          </TooltipContent>
        </Tooltip>

        {/* Hero section with logo and tagline */}
        <div className="flex-1 flex flex-col items-center justify-center px-4 text-center pt-16 sm:pt-0">
          {/* Logo and title */}
          <div className="mb-8 flex flex-col items-center">
            <img
              src="/src/assets/images/SF-Logo1-D.png"
              alt="PetConnect Logo"
              className="w-24 h-24 sm:w-32 sm:h-32"
            />
            <h1 className="text-4xl sm:text-5xl font-bold text-[#FFECAB] mt-4 sm:mt-6">
              PetConnect
            </h1>
            <p className="text-lg sm:text-xl text-gray-300 mt-3 sm:mt-4 max-w-md">
              Connecting pets to the world
            </p>
          </div>

          {/* Call to action buttons */}
          <div className="space-y-4 w-full max-w-xs">
            <Link
              to="/login"
              className="block w-full py-3 px-4 bg-cyan-800 text-[#FFECAB] rounded-xl font-medium text-center hover:opacity-80 transition-colors"
            >
              Log In
            </Link>
            <Link
              to="/signup"
              className="block w-full py-3 px-4 text-cyan-800 bg-[#FFECAB] rounded-xl font-medium text-center hover:opacity-80 transition-colors"
            >
              Sign Up
            </Link>
          </div>
        </div>

        {/* Footer */}
        <footer className="py-6 text-center text-gray-500">
          <p>© 2025 PetConnect. All rights reserved.</p>
        </footer>

        {/* Clinic Search Modal */}
        {isModalOpen && (
          <div className="fixed inset-0 bg-black bg-opacity-80 flex items-start justify-center p-4 z-50 transition-opacity duration-300">
            <div className="bg-gradient-to-b from-gray-800 to-gray-900 rounded-xl shadow-xl w-full max-w-2xl p-6 sm:p-8 border border-cyan-500/50 max-h-[95vh] flex flex-col">
              {/* Modal Header */}
              <div className="flex justify-between items-center mb-4 sm:mb-6 pb-4 border-b border-gray-700">
                <h3 className="text-lg sm:text-xl font-semibold text-[#FFECAB] flex items-center">
                  <Hospital size={24} className="mr-2 text-cyan-600" />
                  Search Veterinary Clinics
                </h3>
                <button
                  onClick={closeModal}
                  className="text-gray-400 hover:text-white p-1 rounded-full hover:bg-gray-700"
                >
                  <X size={20} />
                </button>
              </div>

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
                      className="bg-gray-700 text-white rounded-lg pl-3 pr-4 py-2.5 w-full border border-gray-600 focus:outline-none focus:ring-2 focus:ring-cyan-500 focus:border-transparent"
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
                      className="bg-gray-700 text-white rounded-lg pl-3 pr-4 py-2.5 w-full border border-gray-600 focus:outline-none focus:ring-2 focus:ring-cyan-500 focus:border-transparent"
                      placeholder="City"
                    />
                  </div>
                  {/* Country Select */}
                  <div className="relative flex-grow-[0.5] sm:flex-grow-[0.4]">
                    <select
                      name="country"
                      value={searchParams.country}
                      onChange={handleSearchChange}
                      disabled={countriesLoading} // Deshabilitar mientras carga
                      className="bg-gray-700 text-white rounded-lg pl-3 pr-8 py-2.5 w-full border border-gray-600 focus:outline-none focus:ring-2 focus:ring-teal-500 focus:border-transparent appearance-none disabled:opacity-50"
                    >
                      <option value="">All Countries</option>
                      {/* Iterar sobre los países obtenidos de la API */}
                      {availableCountries.map((countryValue) => (
                        <option key={countryValue} value={countryValue}>
                          {countryValue.replace(/_/g, " ")}{" "}
                          {/* Formatear nombre */}
                        </option>
                      ))}
                    </select>
                    <div className="absolute inset-y-0 right-0 pr-3 flex items-center pointer-events-none">
                      {countriesLoading ? (
                        <svg
                          className="animate-spin h-4 w-4 text-gray-400" /* spinner */
                        ></svg>
                      ) : (
                        <Globe size={18} className="text-gray-500" />
                      )}
                    </div>
                  </div>
                  {countriesError && (
                    <p className="text-xs text-red-400 col-span-full sm:col-span-1">
                      {countriesError}
                    </p>
                  )}

                  {/* Search Button */}
                  <button
                    type="submit"
                    disabled={isLoading}
                    className="bg-cyan-800 hover:opacity-80 text-white font-medium py-2.5 px-4 rounded-lg transition duration-200 flex items-center justify-center sm:w-auto disabled:opacity-50"
                  >
                    <Search size={18} className="mr-1 sm:mr-2" /> Search
                  </button>
                </div>
              </form>

              {/* Results Area */}
              <div className="flex-grow overflow-y-auto pr-2 -mr-2">
                {isLoading && (
                  <p className="text-center text-gray-400 py-4">
                    Loading clinics...
                  </p>
                )}
                {error && (
                  <p className="text-center text-red-400 py-4">
                    Error: {error}
                  </p>
                )}
                {!isLoading && !error && clinics.length === 0 && (
                  <p className="text-center text-gray-500 py-4">
                    No clinics found matching your criteria. Try broadening your
                    search.
                  </p>
                )}
                {!isLoading && !error && clinics.length > 0 && (
                  <ul className="space-y-3">
                    {clinics.map((clinic) => (
                      <li
                        key={clinic.id}
                        className="bg-gray-700/50 p-3 sm:p-4 rounded-lg border border-gray-600/50"
                      >
                        <h4 className="font-semibold text-[#FFECAB] text-base sm:text-lg">
                          {clinic.name}
                        </h4>
                        <div className="text-sm text-gray-300 mt-1 space-y-1">
                          <p className="flex items-center">
                            <MapPin size={14} className="mr-2 text-cyan-600" />{" "}
                            {clinic.address}, {clinic.city},{" "}
                            {clinic.country.replace(/_/g, " ")}
                          </p>
                          <p className="flex items-center">
                            <PhoneCall
                              size={14}
                              className="mr-2 text-cyan-600"
                            />{" "}
                            {clinic.phone}
                          </p>
                        </div>
                      </li>
                    ))}
                  </ul>
                )}
              </div>

              {/* Pagination */}
              {!isLoading && totalPages > 1 && (
                <div className="mt-4 sm:mt-6 pt-4 border-t border-gray-700 flex justify-center items-center space-x-2">
                  <button
                    onClick={() => handlePageChange(currentPage - 1)}
                    disabled={currentPage === 0}
                    className="px-3 py-1 rounded bg-gray-600 hover:bg-gray-500 text-white disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    Previous
                  </button>
                  <span className="text-gray-400 text-sm">
                    Page {currentPage + 1} of {totalPages}
                  </span>
                  <button
                    onClick={() => handlePageChange(currentPage + 1)}
                    disabled={currentPage >= totalPages - 1}
                    className="px-3 py-1 rounded bg-gray-600 hover:bg-gray-500 text-white disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    Next
                  </button>
                </div>
              )}
            </div>
          </div>
        )}
      </div>
    </TooltipProvider>
  );
};

export default LandingPage;
