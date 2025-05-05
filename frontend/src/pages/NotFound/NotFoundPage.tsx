import { JSX } from 'react';
import { Link } from 'react-router-dom';
import { AlertTriangle, Home } from 'lucide-react';
import Footer from "@/components/layout/Footer";

/**
 * NotFoundPage Component - Displays a user-friendly 404 error message
 * when a requested route does not exist. Provides a link back to the homepage.
 * Uses the application's color scheme.
 *
 * @returns {JSX.Element} The 404 Not Found page component.
 */
const NotFoundPage = (): JSX.Element => {
  return (
    <div className="min-h-screen flex flex-col items-center justify-center bg-gradient-to-br from-[#070913] to-[#0c1225] text-center p-4">
      <AlertTriangle size={64} className="text-red-500 mb-4" /> 
      <h1 className="text-4xl sm:text-5xl font-bold text-[#FFECAB] mb-2">
        404 - Page Not Found
      </h1>
      <p className="text-lg text-gray-300 mb-8 max-w-md">
        Oops! The page you are looking for seems to have gone astray.
        It might have been moved, deleted, or maybe it never existed.
      </p>
      <Link
        to="/" // Link to the landing page
        className="inline-flex items-center py-3 px-6 text-[#090D1A] bg-[#FFECAB] rounded-xl font-medium hover:opacity-90 transition-opacity"
      >
        <Home size={18} className="mr-2" /> 
        Go Back Home
      </Link>

       {/* Footer */}
       <Footer className="absolute bottom-0 w-full" />
      
    </div>
  );
};

export default NotFoundPage;