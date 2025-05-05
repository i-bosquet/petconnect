import { useState, useEffect, JSX } from 'react';
import { findMyPets } from '../../services/petService';
import { PetProfileDto, Page } from '../../types/apiTypes';
import PetList from '../../components/pet/PetList'; 
import AddPetModal from '../../components/pet/modals/AddPetModal';


const PetDetailPlaceholder = ({ petId, onBack }: { petId: number | string, onBack: () => void }) => (
    <div className="p-4 border rounded-lg border-[#FFECAB]/30 bg-[#070913]/50 mt-6">
        <button onClick={onBack} className="mb-4 text-cyan-400 hover:text-cyan-300">← Back to List</button>
        <h2 className="text-xl font-semibold text-[#FFECAB]">Pet Details Placeholder (ID: {petId})</h2>
        <p className="text-gray-400">Full details and tabs will be shown here.</p>
    </div>
);


/**
 * OwnerDashboardPage - Main container for the owner's pet management view.
 * Fetches and displays the list of pets, handles pet selection,
 * and manages the visibility of the AddPetModal.
 *
 * @returns {JSX.Element} The owner dashboard page component.
 */
const OwnerDashboardPage = (): JSX.Element => {
    const [pets, setPets] = useState<PetProfileDto[]>([]);
    const [isLoading, setIsLoading] = useState<boolean>(true);
    const [error, setError] = useState<string>('');
    const [selectedPetId, setSelectedPetId] = useState<number | string | null>(null); 
    const [showAddModal, setShowAddModal] = useState<boolean>(false); 


    /**
     * Fetches the owner's pets from the API.
     * Can be called on mount and after adding/deleting a pet.
     */
    const fetchOwnerPets = async () => {
        setIsLoading(true);
        setError('');
        const storedUserJson = sessionStorage.getItem('user') || localStorage.getItem('user');
        if (!storedUserJson) {
            setError("User not authenticated.");
            setIsLoading(false);
            return;
        }
        try {
            const storedUser = JSON.parse(storedUserJson);
            const token = storedUser.jwt;
            if (!token) {
                 setError("Authentication token not found.");
                 setIsLoading(false);
                 return;
            }

            const petsPage: Page<PetProfileDto> = await findMyPets(token, 0, 50); // Fetch up to 50 pets
            setPets(petsPage.content);

        } catch (err) {
            console.error("Failed to fetch pets:", err);
            setError(err instanceof Error ? err.message : 'Could not load pet data.');
        } finally {
            setIsLoading(false);
        }
    };

    /**
     * Fetch pets when the component mounts.
     */
    useEffect(() => {
        fetchOwnerPets();
    }, []); // Ejecutar solo al montar

    /**
     * Callback function passed to AddPetModal. Refreshes the pet list after addition.
     */
    const handlePetAdded = () => {
        fetchOwnerPets(); 
    };

    /**
     * Handles selecting a pet from the list or returning to the list.
     * @param {number | string | null} petId - The ID of the selected pet, or null to show the list.
     */
    const handleSelectPet = (petId: number | string | null) => {
        setSelectedPetId(petId);
    };

    // --- Renderizado ---
    return (
        <div className="space-y-6"> 
            {isLoading && (
                <div className="text-center py-10 text-gray-400">Loading pets...</div>
            )}
            {error && (
                <div className="p-4 text-center text-red-400 bg-red-900/20 border border-red-500/50 rounded-lg">{error}</div>
            )}

            {!isLoading && !error && selectedPetId === null && (
                <PetList
                    pets={pets}
                    onSelectPet={handleSelectPet}
                    onAddPet={() => setShowAddModal(true)} 
                />
            )}

            {!isLoading && !error && selectedPetId !== null && (
                 <PetDetailPlaceholder petId={selectedPetId} onBack={() => handleSelectPet(null)} />
            )}

            {/* Renderizar Modal para Añadir Pet */}
            {showAddModal && (
                <AddPetModal
                    onClose={() => setShowAddModal(false)}
                    onPetAdded={handlePetAdded}
                />
            )}
        </div>
    );
};

export default OwnerDashboardPage;