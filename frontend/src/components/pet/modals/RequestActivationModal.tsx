import { useState, useEffect, JSX } from 'react';
import Modal from '@/components/common/Modal';
import { Button } from '@/components/ui/button';
import { PetProfileDto, ClinicDto } from '@/types/apiTypes';
import { Loader2,  Search, CircleX, CheckCircle } from 'lucide-react';
import ClinicSelectionForActivation from '@/components/pet/ClinicSelectionForActivation'; 

interface RequestActivationModalProps {
    isOpen: boolean;
    onClose: () => void;
    pet: PetProfileDto;
    onActivationRequested: (clinicId: number | string) => void; 
    isLoadingRequest: boolean;
}

/**
 * RequestActivationModal - Modal for an Owner to select a clinic
 * to request activation for their PENDING pet.
 * @param {RequestActivationModalProps} props - Component props.
 * @returns {JSX.Element | null} The modal component or null if not open.
 * @author ibosquet
 */
const RequestActivationModal = ({
    isOpen,
    onClose,
    pet,
    onActivationRequested,
    isLoadingRequest
}: RequestActivationModalProps): JSX.Element | null => {
    const [selectedClinic, setSelectedClinic] = useState<ClinicDto | null>(null);
    const [showClinicSearch, setShowClinicSearch] = useState<boolean>(false);
    const [error, setError] = useState<string>('');
    
    useEffect(() => {
        if (isOpen) {
            setSelectedClinic(null);
            setShowClinicSearch(false);
            setError('');
        }
    }, [isOpen]);

    const handleClinicSelected = (clinic: ClinicDto) => {
        setSelectedClinic(clinic);
        setShowClinicSearch(false); 
        setError('');
    };

    const handleSubmit = () => {
        if (!selectedClinic || !selectedClinic.id) {
            setError("Please select a clinic.");
            return;
        }
        setError('');
        onActivationRequested(selectedClinic.id);
    };

    if (!isOpen) return null;

    return (
        <Modal title={`Request Activation for ${pet.name}`} onClose={onClose} maxWidth="max-w-lg">
            <div className="space-y-4">
                <p className="text-sm text-gray-300">
                    Select the clinic where you will take <strong className='text-[#FFECAB]'>{pet.name}</strong> to complete the activation process.
                </p>

                {error && <p className="text-sm text-red-400 text-center">{error}</p>}

                <div className="space-y-2">
                    <label className="block text-sm font-medium text-gray-300">
                        Clinic selected:
                    </label>
                    {selectedClinic  ? (
                        <div className="flex items-center justify-between p-3 bg-gray-700 rounded-md">
                            <span className="text-white truncate" title={selectedClinic.name}>
                                {selectedClinic.name} ( {selectedClinic.city}   )
                            </span>
                            <Button variant="link" onClick={() => setShowClinicSearch(true)} className="text-cyan-400 h-auto p-0 cursor-pointer">
                                Change Clinic
                            </Button>
                        </div>
                    ) : (
                        <Button onClick={() => setShowClinicSearch(true)} className="w-full justify-start text-[#FFECAB] bg-cyan-800 hover:bg-cyan-500 border border-[#FFECAB] cursor-pointer">
                            <Search size={16} className="mr-2 text-[#FFECAB]" />
                            Select a Clinic...
                        </Button>
                    )}
                </div>

                <div className="flex justify-end gap-3 pt-4 border-t border-[#FFECAB]/20 mt-5">
                    <Button
                        onClick={onClose}
                        disabled={isLoadingRequest}
                        className="px-5 py-2.5 rounded-lg border border-[#FFECAB]/50 text-[#FFECAB] hover:bg-red-800 hover:text-[#FFECAB] focus-visible:ring-red-500 disabled:opacity-50 cursor-pointer">
    <CircleX size={16} className="mr-2"  />
                        Cancel
                    </Button>
                    <Button
                        onClick={handleSubmit}
                        disabled={!selectedClinic  || isLoadingRequest}
                        className="px-5 py-2.5 rounded-lg border border-[#FFECAB]/50 bg-cyan-800 text-[#FFECAB] hover:bg-cyan-600 focus-visible:ring-cyan-500 disabled:opacity-50 cursor-pointer">
                        {isLoadingRequest && <Loader2 className="animate-spin h-4 w-4 mr-2" />}
                        <CheckCircle size={16} className="mr-2" />
                        Confirm Request
                    </Button>
                </div>
            </div>

            {showClinicSearch && (
                <ClinicSelectionForActivation
                    isOpen={showClinicSearch}
                    onClose={() => setShowClinicSearch(false)}
                    onClinicSelected={handleClinicSelected}
                />
            )}
        </Modal>
    );
};

export default RequestActivationModal;