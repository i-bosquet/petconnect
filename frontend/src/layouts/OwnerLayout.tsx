import { useState, JSX} from 'react';
import { Outlet } from 'react-router-dom';
import TopBar from '../components/layout/TopBar';
import { PetProfileDto } from '@/types/apiTypes';

/**
 * OwnerLayout - Provides the layout structure specifically for the Pet Owner dashboard sections.
 * Includes the common TopBar and the main content area for owner-specific pages.
 * Manage the selected pet state to inform TopBar about chat availability.
 *
 * @returns {JSX.Element} The layout structure for owner sections.
 */
const OwnerLayout = (): JSX.Element => {
    const [selectedPetForTopBar, setSelectedPetForTopBar] = useState<PetProfileDto | null>(null);

    return (
        <div className="min-h-screen bg-gradient-to-br from-[#070913] to-[#0c1225] text-[#FFECAB]">
            <div className="sticky top-0 z-30 px-4 sm:px-6 lg:px-8 bg-[#090D1A]/95 backdrop-blur-sm border-b border-[#FFECAB]/20"> 
                <TopBar selectedPetForChat={selectedPetForTopBar} />
            </div>
            <main className="p-4 sm:p-6 lg:p-8 lg:max-w-6xl lg:mx-auto">
                <Outlet context={{ setSelectedPetForTopBar }} />
            </main>
        </div>
    );
};
export default OwnerLayout;