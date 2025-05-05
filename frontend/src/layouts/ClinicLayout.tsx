import { JSX } from 'react';
import { Outlet } from 'react-router-dom';
import TopBar from '../components/layout/TopBar'; 

/**
 * ClinicLayout - Provides the layout structure specifically for the Clinic Staff (Vet/Admin) dashboard sections.
 * Includes the common TopBar and the main content area for clinic-specific pages.
 * Could potentially include a sidebar navigation in the future.
 *
 * @returns {JSX.Element} The layout structure for clinic sections.
 */
const ClinicLayout = (): JSX.Element => {
  return (
    <div className="min-h-screen bg-gradient-to-br from-[#070913] to-[#0c1225] text-[#FFECAB]">
      <TopBar />
      <main className="p-4 sm:p-6">
         {/* Clinic-specific content*/}
        <Outlet />
      </main>
       {/* Clinic-specific footer */}
    </div>
  );
};

export default ClinicLayout;