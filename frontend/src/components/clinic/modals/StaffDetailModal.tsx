import { useState, useCallback, JSX, useEffect } from 'react'; // Import React
import Modal from '@/components/common/Modal';
import StaffDetailView from '../StaffDetailView';
import StaffEditForm from '../StaffEditForm';
import { ClinicStaffProfile } from '@/types/apiTypes';

interface StaffDetailModalProps {
    isOpen: boolean;
    onClose: () => void;
    staffProfileInitial: ClinicStaffProfile; 
    onStaffUpdate: () => void;
}

/**
 * StaffDetailModal - Modal container for viewing and editing Clinic Staff details by an Admin.
 * Toggles between StaffDetailView and StaffEditForm.
 */
const StaffDetailModal = ({ isOpen, onClose, staffProfileInitial, onStaffUpdate }: StaffDetailModalProps): JSX.Element | null => {
    const [isEditing, setIsEditing] = useState<boolean>(false);
    const [currentStaffData, setCurrentStaffData] = useState<ClinicStaffProfile>(staffProfileInitial);

    const handleStartEditing = useCallback(() => { setIsEditing(true); }, []);
    const handleCancelEditing = useCallback(() => { setIsEditing(false); }, []);

    const handleSaveSuccess = useCallback((updatedStaff: ClinicStaffProfile) => {
        setCurrentStaffData(updatedStaff);
        setIsEditing(false);           
        onStaffUpdate();                 
    }, [onStaffUpdate]);

    useEffect(() => {
        if (isOpen) {
            setCurrentStaffData(staffProfileInitial);
            setIsEditing(false); 
        }
    }, [isOpen, staffProfileInitial]);


    if (!isOpen) return null;
    const modalTitle = isEditing ? `Edit Staff: ${currentStaffData.username}` : `Staff Details: ${currentStaffData.username}`;

    return (
        <Modal title={modalTitle} onClose={onClose} maxWidth="max-w-4xl">
            {isEditing ? (
                <StaffEditForm
                    staffProfile={currentStaffData} 
                    onSaveSuccess={handleSaveSuccess}
                    onCancel={handleCancelEditing}
                />
            ) : (
                <StaffDetailView
                    staffProfile={currentStaffData} 
                     onEdit={currentStaffData.isActive ? handleStartEditing : undefined}    
                />
            )}
        </Modal>
    );
};
export default StaffDetailModal;