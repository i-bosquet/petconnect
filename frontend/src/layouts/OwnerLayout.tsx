import { JSX } from 'react';
import { Outlet } from 'react-router-dom';
import TopBar from '../components/layout/TopBar'; 

/**
 * OwnerLayout - Provides the layout structure specifically for the Pet Owner dashboard sections.
 * Includes the common TopBar and the main content area for owner-specific pages.
 *
 * @returns {JSX.Element} The layout structure for owner sections.
 */
const OwnerLayout = (): JSX.Element => {
  return (
    <div className="min-h-screen bg-gradient-to-br from-[#070913] to-[#0c1225] text-[#FFECAB] sm:px-40">
      <TopBar />
      <main className="rounded-2xl shadow-xl p-8 border-2 border-[#FFECAB] transform transition-all hover:scale-[1.01]">
        {/* Owner-specific content */}
        <Outlet />
      </main>
      {/* Owner-specific footer */}
    </div>
  );
};

export default OwnerLayout;