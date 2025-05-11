import { useState, JSX } from 'react';
import { PetProfileDto, VetSummaryDto, ClinicDto } from '@/types/apiTypes'; 
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { UserPlus, Trash2, Info, } from 'lucide-react';
import { useAuth } from '@/hooks/useAuth';
import { associateVetWithPet, disassociateVetFromPet } from '@/services/petService';
import ConfirmationModal from '@/components/common/ConfirmationModal';
import ClinicSelectionForAssociationModal from '@/components/pet/modals/ClinicSelectionForAssociationModal';
import VetSelectionModal from '@/components/pet/modals/VetSelectionModal';
import VetDetailModal from '@/components/pet/modals/VetDetailModal'; 
import { Tooltip, TooltipContent, TooltipTrigger } from '@/components/ui/tooltip';
import { toast } from 'sonner';


interface PetHomeTabProps {
  pet: PetProfileDto;
  onPetAssociationChange: () => void;
}

/**
 * PetHomeTab - Displays general pet information and allows managing associated veterinarians.
 * @param {PetHomeTabProps} props - Component props.
 * @returns {JSX.Element} The Pet Home Tab content.
 */
const PetHomeTab = ({ pet, onPetAssociationChange }: PetHomeTabProps): JSX.Element => {
    const { token } = useAuth();
    const [isLoadingAction, setIsLoadingAction] = useState<boolean>(false); 
    const [showConfirmDisassociateModal, setShowConfirmDisassociateModal] = useState<boolean>(false);
    const [vetToDisassociate, setVetToDisassociate] = useState<VetSummaryDto | null>(null);
    const [showClinicSelectionModal, setShowClinicSelectionModal] = useState<boolean>(false);
    const [selectedClinicForAssociation, setSelectedClinicForAssociation] = useState<ClinicDto | null>(null);
    const [showVetSelectionModal, setShowVetSelectionModal] = useState<boolean>(false);
    const [showVetDetailModal, setShowVetDetailModal] = useState<boolean>(false);
    const [vetForDetailView, setVetForDetailView] = useState<VetSummaryDto | null>(null);


    const handleOpenDisassociateConfirm = (vet: VetSummaryDto) => {
        setVetToDisassociate(vet);
        setShowConfirmDisassociateModal(true);
    };

    const handleConfirmDisassociate = async () => {
        if (!token || !vetToDisassociate || !pet) return;
        setIsLoadingAction(true);
        try {
            await disassociateVetFromPet(token, pet.id, vetToDisassociate.id);
            toast.success(`Dr. ${vetToDisassociate.name} ${vetToDisassociate.surname} has been disassociated from ${pet.name}.`);
            onPetAssociationChange();
        } catch (error) {
            console.error("Error disassociating vet:", error);
            toast.error(error instanceof Error ? error.message : "Failed to disassociate veterinarian.");
        } finally {
            setIsLoadingAction(false);
            setShowConfirmDisassociateModal(false);
            setVetToDisassociate(null);
        }
    };

    const handleOpenClinicSelection = () => {
        setSelectedClinicForAssociation(null);
        setShowClinicSelectionModal(true);
    };

    const handleClinicSelectedForAssociation = (clinic: ClinicDto) => {
        setSelectedClinicForAssociation(clinic);
        setShowClinicSelectionModal(false);
        setShowVetSelectionModal(true);
    };

    const handleVetSelectedForAssociation = async (vetId: number | string) => {
        if (!token || !pet) return;
        setIsLoadingAction(true);
        try {
            await associateVetWithPet(token, pet.id, vetId);
            toast.success(`Veterinarian has been associated with ${pet.name}.`);
            onPetAssociationChange();
        } catch (error) {
            console.error("Error associating vet:", error);
            toast.error(error instanceof Error ? error.message : "Failed to associate veterinarian.");
        } finally {
            setIsLoadingAction(false);
            setShowVetSelectionModal(false);
            setSelectedClinicForAssociation(null);
        }
    };

    const handleViewVetDetails = (vet: VetSummaryDto) => {
        setVetForDetailView(vet);
        setShowVetDetailModal(true);
    };

  return (
        <div className="space-y-6">
            <Card className="border-2 border-[#FFECAB]/50 bg-[#0c1225]/70 shadow-xl">
                <CardHeader>
                    <div className="flex justify-between items-center">
                        <CardTitle className="text-[#FFECAB] text-xl">Associated Veterinarians</CardTitle>
                        <Button onClick={handleOpenClinicSelection} size="sm" className="bg-cyan-800 hover:bg-cyan-600 text-[#FFECAB] border border-[#FFECAB] cursor-pointer">
                            <UserPlus size={16} className="mr-2" />
                            Associate New Vet
                        </Button>
                    </div>
                </CardHeader>
                <CardContent>
                    {pet.associatedVets && pet.associatedVets.length > 0 ? (
                        <ul className="space-y-3">
                            {pet.associatedVets.map((vet) => (
                                <li key={vet.id} className="flex flex-col sm:flex-row justify-between items-start sm:items-center p-3 bg-gray-800/70 rounded-lg hover:bg-gray-700/90 gap-2">
                                    <div className="flex items-center gap-3 flex-grow">
                                        <img 
                                            src={vet.avatar || '/src/assets/images/avatars/users/default_avatar.png'} 
                                            alt={`${vet.name} ${vet.surname}`} 
                                            className="w-10 h-10 rounded-full object-cover border border-cyan-700"
                                            onError={(e) => (e.currentTarget.src = '/src/assets/images/avatars/users/default_avatar.png')}
                                        />
                                        <div>
                                            <p className="font-semibold text-white">{vet.name} {vet.surname}</p>
                                            <p className="text-xs text-cyan-400">{vet.email}</p>
                                            {vet.clinicName && <p className="text-xs text-gray-400">Clinic: {vet.clinicName}</p>}
                                        </div>
                                    </div>
                                    <div className="flex items-center gap-2 mt-2 sm:mt-0 self-end sm:self-center">
                                        <Button size="sm" onClick={() => handleViewVetDetails(vet)} className="text-[#090D1A] bg-[#FFECAB] hover:text-[#FFECAB] hover:bg-[#090D1A] border hover:border-[#FFECAB] border-[#090D1A] px-2 py-1 h-auto cursor-pointer">
                                            <Info size={14} className="mr-1 sm:mr-1.5"/> Details
                                        </Button>
                                        <Tooltip>
                                            <TooltipTrigger asChild>
                                                <Button variant="ghost" size="icon" onClick={() => handleOpenDisassociateConfirm(vet)} className="text-red-500  hover:bg-red-900 hover:text-[#FFECAB] h-8 w-8 p-1.5  cursor-pointer">
                                                    <Trash2 size={16} />
                                                </Button>
                                            </TooltipTrigger>
                                            <TooltipContent className="bg-red-700 text-white border-none"><p>Disassociate Vet</p></TooltipContent>
                                        </Tooltip>
                                    </div>
                                </li>
                            ))}
                        </ul>
                    ) : (
                        <p className="text-gray-400 italic text-center py-4">No veterinarians are currently associated with {pet.name}.</p>
                    )}
                </CardContent>
            </Card>
      {/* Urgent Events Section - Shows upcoming appointments and vaccines */}
      <h3 className="font-medium text-lg mt-6 mb-2 text-center">
        Urgent Events
      </h3>
      <div className="space-y-4">
        {/* Next appointment card - Shows only if there are upcoming appointments */}
        <div className="p-4 bg-gray-50 rounded-lg shadow-md flex flex-col sm:flex-row justify-between items-center">
          <div className="text-center sm:text-left">
            <h3 className="font-medium text-lg text-[#090D1A]">
              Next Appointment
            </h3>
            {/* IIFE to find and display the nearest upcoming vaccine */}
            <p className="text-sm text-gray-600">Not registered</p>
          </div>
          <button className="mt-2 sm:mt-0 px-4 py-2 bg-cyan-800 text-[#FFECAB] rounded-2xl">
            Edit Appointment
          </button>
        </div>

        {/* Upcoming vaccine card - Shows only if there are upcoming vaccines */}
        <div className="p-4 bg-gray-50 rounded-lg shadow-md flex flex-col sm:flex-row justify-between items-center">
          <div className="text-center sm:text-left">
            <h3 className="font-medium text-lg text-[#090D1A]">
              Upcoming Vaccine
            </h3>
            {/* IIFE to find and display the nearest upcoming vaccine */}
            <p className="text-sm text-gray-600">Not registered</p>
          </div>
          <button className="mt-2 sm:mt-0 px-4 py-2 bg-red-800 text-[#FFECAB] rounded-2xl">
            Schedule Appointment
          </button>
        </div>
      </div>
      {/* Modals */}
      {showConfirmDisassociateModal && vetToDisassociate && (
                <ConfirmationModal
                    isOpen={showConfirmDisassociateModal}
                    onClose={() => setShowConfirmDisassociateModal(false)}
                    onConfirm={handleConfirmDisassociate}
                    title="Confirm Disassociation"
                    message={<>Are you sure you want to disassociate Dr. <strong className="text-[#FFECAB]">{vetToDisassociate.name} {vetToDisassociate.surname}</strong> from {pet.name}?</>}
                    confirmButtonText="Yes, Disassociate"
                    isLoading={isLoadingAction}
                />
            )}

            {showClinicSelectionModal && (
                <ClinicSelectionForAssociationModal
                    isOpen={showClinicSelectionModal}
                    onClose={() => setShowClinicSelectionModal(false)}
                    onClinicSelected={handleClinicSelectedForAssociation}
                />
            )}

            {showVetSelectionModal && selectedClinicForAssociation && (
                <VetSelectionModal
                    isOpen={showVetSelectionModal}
                    onClose={() => { setShowVetSelectionModal(false); setSelectedClinicForAssociation(null); }}
                    clinic={selectedClinicForAssociation}
                    onVetSelected={handleVetSelectedForAssociation}
                    isLoading={isLoadingAction}
                />
            )}

            {showVetDetailModal && vetForDetailView && (
                <VetDetailModal
                    isOpen={showVetDetailModal}
                    onClose={() => setShowVetDetailModal(false)}
                    vet={vetForDetailView} 
                />
            )}
        </div>
    );
};
export default PetHomeTab;