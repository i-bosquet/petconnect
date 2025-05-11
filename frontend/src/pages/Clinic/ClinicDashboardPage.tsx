import { useState, useEffect, useCallback, JSX } from 'react';
import { useAuth } from '@/hooks/useAuth';
import { PetProfileDto } from '@/types/apiTypes';
import { findMyClinicPendingPets } from '@/services/petService';
import { Loader2, AlertCircle, BellDot } from 'lucide-react';
import PendingActivationCard from '@/components/clinic/PendingActivationCard'; 
import PetActivationModal from '@/components/clinic/modals/PetActivationModal';
import { toast } from "sonner"; 

/**
 * ClinicDashboardPage - Main dashboard for clinic staff.
 * Displays notifications, summaries, and pet activation requests.
 */
const ClinicDashboardPage = (): JSX.Element => {
    const { token, user, isLoading: isLoadingAuth } = useAuth(); 
    const [pendingPets, setPendingPets] = useState<PetProfileDto[]>([]); 
    const [isLoadingPending, setIsLoadingPending] = useState<boolean>(true);
    const [errorPending, setErrorPending] = useState<string>('');

    const [showActivationModal, setShowActivationModal] = useState<boolean>(false);
    const [petToActivate, setPetToActivate] = useState<PetProfileDto | null>(null);

    const fetchPendingPets = useCallback(async () => {
        if (!token || !(user?.roles?.includes('VET') || user?.roles?.includes('ADMIN'))) {
            setIsLoadingPending(false);
            if (user && !(user.roles.includes('VET') || user.roles.includes('ADMIN'))) {
                setErrorPending("Access restricted to clinic staff.");
            }
            return;
        }
        setIsLoadingPending(true);
        setErrorPending('');
        try {
            const data = await findMyClinicPendingPets(token);
            setPendingPets(data);
        } catch (err) {
            setErrorPending(err instanceof Error ? err.message : "Failed to load pending activation requests.");
        } finally {
            setIsLoadingPending(false);
        }
    }, [token, user]);

    useEffect(() => {
        if (!isLoadingAuth) { 
             fetchPendingPets();
        }
    }, [isLoadingAuth, fetchPendingPets]); 

    /**
     * Opens the modal to activate a specific pet.
     * Only VETs should be able to proceed with actual activation.
     * @param {PetProfileDto} pet - The pet to be considered for activation.
     */
    const handleOpenActivationModal = (pet: PetProfileDto) => { 
        if (user?.roles?.includes('VET')) {
            setPetToActivate(pet);
            setShowActivationModal(true);
        } else {
            toast.error("Pet activation can only be performed by a Veterinarian."); 
        }
    };

    /**
     * Callback function for when a pet has been successfully activated.
     * Closes the modal and refreshes the list of pending pets.
     */
    const handlePetActivated = () => {
       setShowActivationModal(false);
       setPetToActivate(null);
       fetchPendingPets(); 
       toast.success("Pet activated successfully!");
    };


    if (isLoadingAuth) {
        return <div className="flex justify-center items-center py-10"><Loader2 className="h-8 w-8 animate-spin text-cyan-500" /><span className="ml-2">Loading authentication...</span></div>;
    }
    if (!user || !(user.roles?.includes('VET') || user.roles?.includes('ADMIN'))) {
        return (
            <div className="p-6 text-center">
                <AlertCircle className="mx-auto h-12 w-12 text-red-400" />
                <h2 className="mt-2 text-xl font-semibold text-red-300">Access Denied</h2>
                <p className="text-gray-400">{errorPending || "You do not have permission to view this page."}</p>
            </div>
        );
    }


    return (
        <div className="space-y-6">
            <div className="flex justify-between items-center">
                <h1 className="text-2xl font-bold text-[#FFECAB] mb-0">Clinic Dashboard</h1>
            </div>

            <section>
                <h2 className="text-xl font-semibold text-cyan-400 mb-3 flex items-center">
                    <BellDot size={20} className="mr-2" /> Pet Activation Requests
                </h2>
                {isLoadingPending && <div className="flex justify-center items-center py-6"><Loader2 className="h-6 w-6 animate-spin text-cyan-500" /><span className="ml-2 text-gray-300">Loading requests...</span></div>}
                {errorPending && !isLoadingPending && <div className="p-3 text-red-400 bg-red-900/20 rounded-lg"><AlertCircle size={18} className="inline mr-2"/>{errorPending}</div>}
                
                {!isLoadingPending && !errorPending && pendingPets.length === 0 && (
                    <p className="text-gray-400 italic">No new activation requests at the moment.</p>
                )}

                {!isLoadingPending && !errorPending && pendingPets.length > 0 && (
                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                        {pendingPets.map((pet: PetProfileDto) => ( 
                            <PendingActivationCard
                                key={pet.id}
                                pet={pet}
                                onProcess={() => handleOpenActivationModal(pet)}
                                canProcess={user?.roles?.includes('VET') ?? false}
                            />
                        ))}
                    </div>
                )}
            </section>

            <div className="mt-8 p-6 border border-dashed border-gray-600 rounded-lg">
                 <h3 className="text-lg font-semibold text-gray-300 mb-2">Clinic Overview</h3>
                 <p className="text-gray-400">(More clinic stats, quick links, etc. will be here)</p>
            </div>

            {showActivationModal && petToActivate && (
               <PetActivationModal
                    isOpen={showActivationModal}
                    onClose={() => { setShowActivationModal(false); setPetToActivate(null); }}
                    petToActivate={petToActivate}
                    onPetActivated={handlePetActivated}
                />
            )}
        </div>
    );
};
export default ClinicDashboardPage;