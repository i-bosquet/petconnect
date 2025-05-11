import { useState, useCallback, JSX, useEffect } from 'react';
import Modal from '@/components/common/Modal';
import ClinicPetDetailView from '@/components/clinic/ClinicPetDetailView';
import ClinicPetEditForm from '@/components/clinic/ClinicPetEditForm';  
import { PetProfileDto} from '@/types/apiTypes'; 
import { useAuth } from '@/hooks/useAuth';

interface ClinicPetDetailModalProps {
    isOpen: boolean;
    onClose: () => void;
    petProfileInitial: PetProfileDto;
    onPetUpdate: () => void; 
}

/**
 * ClinicPetDetailModal - Modal container for viewing and editing Pet details
 * from the clinic staff's perspective. Toggles between view and edit forms.
 * @param {ClinicPetDetailModalProps} props - Component props.
 * @returns {JSX.Element | null} The modal component or null if not open.
 */
const ClinicPetDetailModal = ({
    isOpen,
    onClose,
    petProfileInitial,
    onPetUpdate
}: ClinicPetDetailModalProps): JSX.Element | null => {
    const { user } = useAuth(); 
    const [isEditing, setIsEditing] = useState<boolean>(false);
    const [currentPetData, setCurrentPetData] = useState<PetProfileDto>(petProfileInitial);

    useEffect(() => {
        if (isOpen) {
            setCurrentPetData(petProfileInitial);
            setIsEditing(false); 
        }
    }, [isOpen, petProfileInitial]);

    const handleStartEditing = useCallback(() => {
        setIsEditing(true);
    }, []);

    const handleCancelEditing = useCallback(() => {
        setIsEditing(false);
        setCurrentPetData(petProfileInitial); 
    }, [petProfileInitial]);

    const handleSaveSuccess = useCallback((updatedPet: PetProfileDto) => {
        setCurrentPetData(updatedPet); 
        setIsEditing(false);
        onPetUpdate(); 
    }, [onPetUpdate]);

    if (!isOpen) return null;

    const canEditPet = user?.roles?.includes('VET') || user?.roles?.includes('ADMIN');

    const modalTitle = isEditing
        ? `Edit Pet: ${currentPetData.name}`
        : `Pet Details: ${currentPetData.name}`;

    return (
        <Modal title={modalTitle} onClose={onClose} maxWidth="max-w-4xl"> 
            {isEditing ? (
                <ClinicPetEditForm
                    petInitialData={currentPetData}
                    onSaveSuccess={handleSaveSuccess}
                    onCancel={handleCancelEditing}
                />
            ) : (
                <ClinicPetDetailView
                    petProfile={currentPetData}
                    onEdit={canEditPet ? handleStartEditing : undefined} 
                    currentUserRoles={user?.roles || []} 
                />
            )}
        </Modal>
    );
};

export default ClinicPetDetailModal;