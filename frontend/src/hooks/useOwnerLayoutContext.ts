import { useOutletContext } from 'react-router-dom';
import { PetProfileDto } from '@/types/apiTypes';
import React from 'react'; // Necesario para React.Dispatch

/**
 * Context type for OwnerLayout.
 */
interface OwnerLayoutContextType {
  setSelectedPetForTopBar: React.Dispatch<React.SetStateAction<PetProfileDto | null>>;
}

/**
 * Custom hook to access the context provided by OwnerLayout's Outlet.
 * Specifically, it provides a function to set the selected pet for the TopBar's chat icon.
 *
 * @returns {OwnerLayoutContextType} An object containing the setter function.
 * @throws Error if used outside of a component rendered by OwnerLayout's Outlet.
 */
export function useOwnerLayoutContext(): OwnerLayoutContextType {
    const context = useOutletContext<OwnerLayoutContextType>();
    if (context === undefined || context === null) {
        throw new Error("useOwnerLayoutContext must be used within the Outlet of OwnerLayout");
    }
    return context;
}