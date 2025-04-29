import { JSX } from 'react';
import { Link } from 'react-router-dom';

/**
 * LandingPage - The main entry point and public face of the PetConnect application
 * This component serves as the website's homepage with brand messaging and access options
 * 
 * @returns {JSX.Element} The landing page with login and signup options
 */
const LandingPage = (): JSX.Element => {
  return (
    <div className="min-h-screen bg-gradient-to-br from-[#070913] to-[#0c1225] flex flex-col">
      {/* Hero section with logo and tagline */}
      <div className="flex-1 flex flex-col items-center justify-center px-4 text-center">
        <div className="mb-8 flex flex-col items-center">
          <img 
            src="/src/assets/images/SF-Logo1-D.png" 
            alt="PetConnect Logo" 
            className="w-32 h-32" 
          />
          <h1 className="text-5xl font-bold text-[#FFECAB] mt-6">
            PetConnect
          </h1>
          <p className="text-xl text-gray-300 mt-4 max-w-md">
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
      
      {/* Footer with brief information */}
      <footer className="py-6 text-center text-gray-500">
        <p>© 2025 PetConnect. All rights reserved.</p>
      </footer>
    </div>
  );
};

export default LandingPage;