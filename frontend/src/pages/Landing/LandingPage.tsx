import { useState, JSX } from "react"; 
import { Link } from "react-router-dom";
import { Hospital } from "lucide-react";
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from "@/components/ui/tooltip"; 
import ClinicSearchModal from "@/components/clinic/ClinicSearchModal"; 
import Footer from "@/components/layout/Footer";

/**
 * LandingPage - The main entry point and public face of the PetConnect application
 * This component serves as the website's homepage with brand messaging and access options
 * Provides buttons for login/signup and access to the clinic search modal.
 *
 * @returns {JSX.Element} The landing page with login and signup options
 */
const LandingPage = (): JSX.Element => {
  const [isModalOpen, setIsModalOpen] = useState<boolean>(false);

  /** Opens the clinic search modal. */
  const openModal = () => {
    setIsModalOpen(true);
  };

  /** Closes the clinic search modal. */
  const closeModal = () => {
    setIsModalOpen(false);
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-[#070913] to-[#0c1225] flex flex-col relative">
      {/* Clinic Search Button */}
      <Tooltip>
        <TooltipTrigger asChild>
          <button
            onClick={openModal}
            className="absolute top-6 right-6 sm:mr-40 bg-cyan-800 hover:opacity-80 w-10 h-10 sm:w-12 sm:h-12 rounded-full flex items-center justify-center transition-colors duration-200 shadow-lg z-10"
            aria-label="Search clinics"
          >
            <Hospital size={24} className="text-[#FFECAB] cursor-pointer" />
          </button>
        </TooltipTrigger>
        <TooltipContent className="bg-gray-800 text-white border border-cyan-700">
          <p>Search Clinics</p>
        </TooltipContent>
      </Tooltip>

      {/* Hero section */}
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

        {/* Buttons*/}
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
      <Footer />

      {/* Clicincs Modal */}
      <ClinicSearchModal isOpen={isModalOpen} onClose={closeModal} />
    </div>
  );
};

export default LandingPage;