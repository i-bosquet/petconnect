import { useState, useEffect, useCallback, JSX } from 'react';
import { useAuth } from '@/hooks/useAuth';
import { PetProfileDto } from '@/types/apiTypes';
import { findMyClinicPendingPets, findPetsWithPendingCertRequestsForClinic} from '@/services/petService';
//import { generateCertificate } from '@/services/certificateService';
import { Loader2, AlertCircle, BellDot, FileSignature } from 'lucide-react';
import PendingActivationCard from '@/components/clinic/PendingActivationCard'; 
import PetActivationModal from '@/components/clinic/modals/PetActivationModal';
import PendingCertificateRequestCard from '@/components/clinic/PendingCertificateRequestCard'; 
import GenerateCertificateModal from '@/components/clinic/modals/GenerateCertificateModal';
import { toast } from "sonner"; 

/**
 * ClinicDashboardPage - Main dashboard for clinic staff.
 * Displays notifications, summaries, and pet activation requests.
 */
const ClinicDashboardPage = (): JSX.Element => {
    const { token, user, isLoading: isLoadingAuth } = useAuth();
    const clinicIdFromUser = user?.clinicId;

    const [pendingActivationPets, setPendingActivationPets] = useState<PetProfileDto[]>([]);
    const [isLoadingActivationRequests, setIsLoadingActivationRequests] = useState<boolean>(true);
    const [errorActivationRequests, setErrorActivationRequests] = useState<string>('');
    const [showPetActivationModal, setShowPetActivationModal] = useState<boolean>(false);
    const [petToActivate, setPetToActivate] = useState<PetProfileDto | null>(null);

    const [pendingCertificatePets, setPendingCertificatePets] = useState<PetProfileDto[]>([]);
    const [isLoadingCertificateRequests, setIsLoadingCertificateRequests] = useState<boolean>(true);
    const [errorCertificateRequests, setErrorCertificateRequests] = useState<string>('');
    const [showGenerateCertificateModal, setShowGenerateCertificateModal] = useState<boolean>(false);
    const [petForCertificate, setPetForCertificate] = useState<PetProfileDto | null>(null);

    
    const fetchPendingActivationRequests = useCallback(async () => {
        if (!token || !(user?.roles?.includes('VET') || user?.roles?.includes('ADMIN'))) {
            setIsLoadingActivationRequests(false);
            if (user && !(user.roles.includes('VET') || user.roles.includes('ADMIN'))) {
                setErrorActivationRequests("Access restricted for pet activation requests.");
            }
            return;
        }
        setIsLoadingActivationRequests(true);
        setErrorActivationRequests('');
        try {
            const data = await findMyClinicPendingPets(token);
            setPendingActivationPets(data);
        } catch (err) {
            setErrorActivationRequests(err instanceof Error ? err.message : "Failed to load pending activation requests.");
        } finally {
            setIsLoadingActivationRequests(false);
        }
    }, [token, user]);

    /**
     * Fetches pets with pending certificate requests for the current clinic.
     */
    const fetchPendingCertificateRequests = useCallback(async () => {
        if (!token || !clinicIdFromUser || !(user?.roles?.includes('VET') || user?.roles?.includes('ADMIN'))) {
            setIsLoadingCertificateRequests(false);
            if (user && !(user.roles.includes('VET') || user.roles.includes('ADMIN'))) {
                 setErrorCertificateRequests("Access restricted for certificate requests.");
            }
            return;
        }
        setIsLoadingCertificateRequests(true);
        setErrorCertificateRequests('');
        try {
            const data = await findPetsWithPendingCertRequestsForClinic(token, clinicIdFromUser);
            setPendingCertificatePets(data);
        } catch (err) {
            setErrorCertificateRequests(err instanceof Error ? err.message : "Failed to load pending certificate requests.");
        } finally {
            setIsLoadingCertificateRequests(false);
        }
    }, [token, user, clinicIdFromUser]);

    useEffect(() => {
        if (!isLoadingAuth && user) { 
            fetchPendingActivationRequests();
            fetchPendingCertificateRequests();
        }
    }, [isLoadingAuth, user, fetchPendingActivationRequests, fetchPendingCertificateRequests]);

    /**
     * Opens the modal to activate a specific pet.
     * Only VETs should be able to proceed with actual activation.
     * @param {PetProfileDto} pet - The pet to be considered for activation.
     */
    const handleOpenActivationModal = (pet: PetProfileDto) => { 
        if (user?.roles?.includes('VET')) {
            setPetToActivate(pet);
            setShowPetActivationModal(true);
        } else {
            toast.error("Pet activation can only be performed by a Veterinarian."); 
        }
    };

    /**
     * Callback function for when a pet has been successfully activated.
     * Closes the modal and refreshes the list of pending pets.
     */
    const handlePetActivated = () => {
       setShowPetActivationModal(false);
       setPetToActivate(null);
       fetchPendingActivationRequests(); 
       toast.success("Pet activated successfully!");
    };

    /**
     * Opens the modal for a Vet to generate a certificate for a pet.
     * @param {PetProfileDto} pet - The pet for which to generate the certificate.
     */
    const handleOpenGenerateCertificateModal = (pet: PetProfileDto) => {
        if (user?.roles?.includes('VET')) {
            setPetForCertificate(pet);
            setShowGenerateCertificateModal(true);
        } else {
            toast.error("Certificate generation can only be performed by a Veterinarian.");
        }
    };

    /**
     * Callback after a certificate has been successfully generated.
     * Closes the modal and refreshes relevant data.
     */
    const handleCertificateGenerated = () => {
        setShowGenerateCertificateModal(false);
        setPetForCertificate(null);
        fetchPendingCertificateRequests(); 
        toast.success("Certificate generated successfully!");
    };


    if (isLoadingAuth) {
        return <div className="flex justify-center items-center py-10"><Loader2 className="h-8 w-8 animate-spin text-cyan-500" /><span className="ml-2">Loading authentication...</span></div>;
    }

    if (!user || !(user.roles?.includes('VET') || user.roles?.includes('ADMIN'))) {
        return (
            <div className="p-6 text-center">
                <AlertCircle className="mx-auto h-12 w-12 text-red-400" />
                <h2 className="mt-2 text-xl font-semibold text-red-300">Access Denied</h2>
                <p className="text-gray-400">{errorActivationRequests  || "You do not have permission to view this page."}</p>
            </div>
        );
    }


    return (
        <div className="space-y-6">
            <div className="flex justify-between items-center">
                <h1 className="text-2xl font-bold text-[#FFECAB] mb-0">Clinic dashboard</h1>
            </div>
            {/* Pet Activation Requests Section */}
            <section>
                <h2 className="text-xl font-semibold text-cyan-400 mb-3 flex items-center">
                    <BellDot size={20} className="mr-2" /> Pet activation requests
                </h2>
                {isLoadingActivationRequests  && <div className="flex justify-center items-center py-6"><Loader2 className="h-6 w-6 animate-spin text-cyan-500" /><span className="ml-2 text-gray-300">Loading requests...</span></div>}
                {errorActivationRequests  && !isLoadingActivationRequests  && <div className="p-3 text-red-400 bg-red-900/20 rounded-lg"><AlertCircle size={18} className="inline mr-2"/>{errorActivationRequests }</div>}
                
                {!isLoadingActivationRequests  && !errorActivationRequests  && pendingActivationPets.length === 0 && (
                    <p className="text-gray-400 italic">No new activation requests at the moment.</p>
                )}

                {!isLoadingActivationRequests  && !errorActivationRequests  && pendingActivationPets.length > 0 && (
                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                        {pendingActivationPets.map((pet: PetProfileDto) => ( 
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

            {/* Certificate Request Section */}
            <section>
                <h2 className="text-xl font-semibold text-purple-400 mb-3 flex items-center"> 
                    <FileSignature size={20} className="mr-2" /> Certificate Generation Requests
                </h2>
                {isLoadingCertificateRequests && <div className="flex justify-center items-center py-6"><Loader2 className="h-6 w-6 animate-spin text-purple-500" /><span className="ml-2 text-gray-300">Loading certificate requests...</span></div>}
                {errorCertificateRequests && !isLoadingCertificateRequests && <div className="p-3 text-red-400 bg-red-900/20 rounded-lg">
                <AlertCircle size={18} className="inline mr-2"/>{errorCertificateRequests}</div>}
                {!isLoadingCertificateRequests && !errorCertificateRequests && pendingCertificatePets.length === 0 && (
                    <p className="text-gray-400 italic">No new certificate requests at the moment.</p>
                )}
                {!isLoadingCertificateRequests && !errorCertificateRequests && pendingCertificatePets.length > 0 && (
                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                        {pendingCertificatePets.map((pet: PetProfileDto) => (
                            <PendingCertificateRequestCard 
                                key={`cert-${pet.id}`}
                                pet={pet}
                                onProcess={() => handleOpenGenerateCertificateModal(pet)}
                                canProcess={user?.roles?.includes('VET') ?? false}
                            />
                        ))}
                    </div>
                )}
            </section>

            <div className="mt-8 p-6 border border-dashed border-gray-600 rounded-lg">
                 <h3 className="text-lg font-semibold text-gray-300 mb-2">Clinic overview</h3>
                 <p className="text-gray-400">(Next appointments will be here)</p>
            </div>

            {/* Modals */}
            {showPetActivationModal  && petToActivate && (
               <PetActivationModal
                    isOpen={showPetActivationModal }
                    onClose={() => { setShowPetActivationModal(false); setPetToActivate(null); }}
                    petToActivate={petToActivate}
                    onPetActivated={handlePetActivated}
                />
            )}

             {showGenerateCertificateModal && petForCertificate && (
               <GenerateCertificateModal
                    isOpen={showGenerateCertificateModal}
                    onClose={() => { setShowGenerateCertificateModal(false); setPetForCertificate(null); }}
                    pet={petForCertificate} 
                    onCertificateGenerated={handleCertificateGenerated}
                />
            )}
        </div>
    );
};
export default ClinicDashboardPage;