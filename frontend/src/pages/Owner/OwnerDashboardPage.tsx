import {  useState, useEffect, useCallback, JSX } from 'react';
import { findMyPets, getPetDetailsById } from '@/services/petService';
import { PetProfileDto, Page } from '@/types/apiTypes';
import { useAuth } from '@/hooks/useAuth';
import PetList from '@/components/pet/PetList'; 
import AddPetModal from '@/components/pet/modals/AddPetModal';
import EditPetModal from '@/components/pet/modals/EditPetModal';
import PetDetailHeader from '@/components/pet/PetDetailHeader';
import PetDetailTabs from '@/components/pet/PetDetailTabs';
import { useOwnerLayoutContext } from '@/hooks/useOwnerLayoutContext';
import { Loader2 } from 'lucide-react';


/**
 * OwnerDashboardPage - Main container for the owner's pet management view.
 * Fetches and displays the list of pets, handles pet selection,
 * and manages the visibility of the AddPetModal.
 *
 * @returns {JSX.Element} The owner dashboard page component.
 */
const OwnerDashboardPage = (): JSX.Element => {
    const { token, isLoading: isLoadingAuth } = useAuth();
    const [pets, setPets] = useState<PetProfileDto[]>([]);
    const [selectedPet, setSelectedPet] = useState<PetProfileDto | null>(null); 
    const [isLoadingPets, setIsLoadingPets] = useState<boolean>(true);
    const [isLoadingDetail, setIsLoadingDetail] = useState<boolean>(false);
    const [error, setError] = useState<string>('');
    const [showAddModal, setShowAddModal] = useState<boolean>(false);
    const [showEditModal, setShowEditModal] = useState<boolean>(false);
    const { setSelectedPetForTopBar } = useOwnerLayoutContext(); 


    /**
     * Fetches the owner's pets from the API.
     * Can be called on mount and after adding/deleting a pet.
     */
    const fetchOwnerPets = useCallback(async () => {
        if (!token) return;
        setIsLoadingPets(true);
        setError('');
        try {
            const petsPage: Page<PetProfileDto> = await findMyPets(token, 0, 50);
            setPets(petsPage.content);
        } catch (err) {
            setError(err instanceof Error ? err.message : 'Could not load pet data.');
        } finally {
            setIsLoadingPets(false);
        }
    }, [token]);


    /**
     * Fetch pets when the component mounts.
     */
    useEffect(() => {
        if (token) {
            fetchOwnerPets();
        } else if (!isLoadingAuth && !token) {
             setError("User not authenticated.");
             setIsLoadingPets(false);
        }
    }, [token, isLoadingAuth, fetchOwnerPets]);

    const handleSelectPet = useCallback(async (petId: number | string | null) => {
        if (petId === null) {
            setSelectedPet(null);
            setSelectedPetForTopBar(null); 
            return;
        }
        if (!token) { setError("Authentication token missing."); return; }
        setIsLoadingDetail(true);
        setError('');
        try {
            const petDetails = await getPetDetailsById(token, petId);
             setSelectedPet(petDetails);
            setSelectedPetForTopBar(petDetails); 
        } catch (err) {
            setError(err instanceof Error ? err.message : 'Could not load pet details.');
            setSelectedPet(null);
            setSelectedPetForTopBar(null);
        } finally {
            setIsLoadingDetail(false);
        }
    }, [token, setSelectedPetForTopBar]);


    const handlePetAddedOrUpdated = useCallback(() => {
        fetchOwnerPets(); 
        if (selectedPet) {
            handleSelectPet(selectedPet.id); 
        }
    }, [fetchOwnerPets, selectedPet, handleSelectPet]);

    
    if (isLoadingAuth) return <div className="flex justify-center items-center py-10"><Loader2 className="h-8 w-8 animate-spin text-cyan-500" /> 
    <span className="ml-2">Loading authentication...</span></div>;
    if (error && pets.length === 0 && !selectedPet) 
        return <div className="p-4 text-center text-red-400 bg-red-900/20 border border-red-500/50 rounded-lg">{error}</div>;
    
    return (
        <div> 
            {isLoadingPets && !selectedPet && (
                <div className="text-center py-10 text-gray-400">Loading pets...</div>
            )}

            {error && !selectedPet && ( 
                 <div className="p-4 text-center text-red-400 bg-red-900/20 border border-red-500/50 rounded-lg">{error}</div>
            )}

            {!isLoadingPets && !selectedPet && (
                <PetList
                    pets={pets}
                    onSelectPet={(petId) => handleSelectPet(petId)} 
                    onAddPet={() => setShowAddModal(true)}
                />
            )}

            {isLoadingDetail && selectedPet && (
                 <div className="text-center py-10 text-gray-400">Loading pet details...</div>
            )}
            
            {!isLoadingDetail && selectedPet && (
                <div>
                    <PetDetailHeader
                        pet={selectedPet}
                        onBack={() => handleSelectPet(null)}
                        onEdit={() => setShowEditModal(true)}
                    />
                    <PetDetailTabs pet={selectedPet} />
                </div>
            )}

            {error && selectedPet && ( 
                 <div className="p-4 text-center text-red-400 bg-red-900/20 border border-red-500/50 rounded-lg">{error}</div>
            )}

            {/* Modales */}
            {showAddModal && (
                <AddPetModal
                    onClose={() => setShowAddModal(false)}
                    onPetAdded={() => {
                        setShowAddModal(false);
                        handlePetAddedOrUpdated();
                    }}
                />
            )}

            {showEditModal && selectedPet && (
                 <EditPetModal
                     petInitialData={selectedPet} 
                     onClose={() => {
                         setShowEditModal(false);
                     }}
                     onPetUpdated={() => {
                         setShowEditModal(false);
                         handlePetAddedOrUpdated(); 
                     }}
                 />
            )}
        </div>
    );
};

export default OwnerDashboardPage;