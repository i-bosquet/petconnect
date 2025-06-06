import { useState, JSX } from 'react';
import { PetProfileDto, VetSummaryDto, ClinicDto, PetStatus } from '@/types/apiTypes'; 
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
import defaultAvatar from '@/assets/images/default_avatar.png';


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

    const isPetInactive = pet.status === PetStatus.INACTIVE;

  return (
        <div className="space-y-6">
            <Card className="border-2 border-[#FFECAB]/50 bg-[#0c1225]/70 shadow-xl">
                <CardHeader>
                    <div className="flex justify-between items-center">
                        <CardTitle className="text-[#FFECAB] text-xl">Associated Veterinarians</CardTitle>
                        <Button onClick={handleOpenClinicSelection} size="sm" className="bg-cyan-800 hover:bg-cyan-600 text-[#FFECAB] border border-[#FFECAB] cursor-pointer" disabled={isPetInactive} title={isPetInactive ? "Cannot associate vets to an inactive pet" : "Associate New Vet"}>
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
                                            src={vet.avatar || defaultAvatar} 
                                            alt={`${vet.name} ${vet.surname}`} 
                                            className="w-10 h-10 rounded-full object-cover border border-cyan-700"
                                            onError={(e) => (e.currentTarget.src = defaultAvatar)}
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
                                                <Button size="icon" onClick={() => handleOpenDisassociateConfirm(vet)} className=" text-red-800 hover:bg-red-800 hover:text-white  h-8 w-8 cursor-pointer m-2">
                                                    <Trash2 size={18} />
                                                </Button>
                                            </TooltipTrigger>
                                            <TooltipContent className="bg-gray-950 text-white border border-red-700"><p>Disassociate Vet</p></TooltipContent>
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
      <h3 className="font-medium text-xl mt-8 mb-4 text-center text-[#FFECAB] opacity-90">
        Upcoming Reminders
      </h3>
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Next Recommended Checkup Card */}
        <div className="p-5 bg-[#0A0F1E]/70 border border-gray-700/80 rounded-xl shadow-lg flex flex-col justify-between min-h-[120px]">
          <div>
            <h4 className="font-semibold text-lg text-cyan-400 mb-1.5">
              Next Recommended Checkup
            </h4>
            {/* Aquí iría la lógica para mostrar la fecha del próximo chequeo */}
            <p className="text-sm text-gray-300" id="next-checkup-info">
              Calculating...
            </p>
          </div>
          <Button variant="outline" size="sm" disabled className="mt-3 self-start border-cyan-600 text-cyan-400 hover:bg-cyan-700/30 hover:text-cyan-300 cursor-not-allowed">
            Schedule Checkup
          </Button>
        </div>

        {/* Upcoming Vaccine(s) Card */}
        <div className="p-5 bg-[#0A0F1E]/70 border border-gray-700/80 rounded-xl shadow-lg flex flex-col justify-between min-h-[120px]">
          <div>
            <h4 className="font-semibold text-lg text-purple-400 mb-1.5">
              Next Due Vaccine(s)
            </h4>
            {/* Aquí iría la lógica para mostrar la próxima vacuna */}
            <p className="text-sm text-gray-300" id="next-vaccine-info">
              Calculating...
            </p>
          </div>
          <Button variant="outline" size="sm" disabled className="mt-3 self-start border-purple-600 text-purple-400 hover:bg-purple-700/30 hover:text-purple-300 cursor-not-allowed">
            View Vaccine Schedule
          </Button>
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