import { useState, useEffect, JSX, useCallback } from 'react';
import Modal from '@/components/common/Modal';
import { ClinicDto, VetSummaryDto } from '@/types/apiTypes';
import { getVetsByClinicId } from '@/services/petService';
import { useAuth } from '@/hooks/useAuth';
import { Button } from '@/components/ui/button';
import { Loader2, CheckSquare, AlertCircle } from 'lucide-react';
import defaultAvatar from '@/assets/images/default_avatar.png';

interface VetSelectionModalProps {
    isOpen: boolean;
    onClose: () => void;
    clinic: ClinicDto; 
    onVetSelected: (vetId: number | string) => void;
    isLoading?: boolean; 
}

/**
 * VetSelectionModal - Allows an owner to select a veterinarian from a specific clinic
 * to associate with their pet.
 * @param {VetSelectionModalProps} props - Component props.
 * @returns {JSX.Element | null} The modal component or null if not open.
 * @author ibosquet
 */
const VetSelectionModal = ({ isOpen, onClose, clinic, onVetSelected, isLoading: isLoadingAssociation }: VetSelectionModalProps): JSX.Element | null => {
    const { token } = useAuth();
    const [vets, setVets] = useState<VetSummaryDto[]>([]);
    const [isLoadingVets, setIsLoadingVets] = useState<boolean>(false);
    const [error, setError] = useState<string>('');

    const fetchVets = useCallback(async () => {
        if (!isOpen || !token || !clinic?.id) return;
        setIsLoadingVets(true);
        setError('');
        try {
            const data = await getVetsByClinicId(token, clinic.id);
            setVets(data);
        } catch (err) {
            setError(err instanceof Error ? err.message : "Failed to load veterinarians for this clinic.");
        } finally {
            setIsLoadingVets(false);
        }
    }, [isOpen, token, clinic?.id]);

    useEffect(() => {
        fetchVets();
    }, [fetchVets]);


    if (!isOpen) return null;

    return (
        <Modal title={`Select Veterinarian from ${clinic.name}`} onClose={onClose} maxWidth="max-w-lg">
            {isLoadingVets && (
                <div className="flex justify-center items-center py-10">
                    <Loader2 className="h-8 w-8 animate-spin text-cyan-500" />
                    <span className="ml-2 text-gray-400">Loading veterinarians...</span>
                </div>
            )}
            {error && !isLoadingVets && (
                <div className="p-3 my-4 bg-red-900/30 text-red-300 rounded-lg text-sm text-center flex items-center justify-center gap-2">
                    <AlertCircle size={18} /> {error}
                </div>
            )}
            {!isLoadingVets && !error && vets.length === 0 && (
                <p className="text-gray-400 italic text-center py-6">No veterinarians found for this clinic.</p>
            )}
            {!isLoadingVets && !error && vets.length > 0 && (
                <ul className="space-y-3 max-h-[60vh] overflow-y-auto custom-scrollbar pr-1">
                    {vets.map((vet) => (
                        <li key={vet.id} className="flex justify-between items-center p-3 bg-gray-700/50 rounded-lg hover:bg-gray-600/70">
                            <div className="flex items-center gap-3">
                                <img
                                    src={vet.avatar || defaultAvatar}
                                    alt={`Dr. ${vet.name} ${vet.surname}`}
                                    className="w-10 h-10 rounded-full object-cover border border-cyan-600"
                                />
                                <div>
                                    <p className="font-semibold text-white">Dr. {vet.name} {vet.surname}</p>
                                    <p className="text-xs text-cyan-400">{vet.email}</p>
                                </div>
                            </div>
                            <Button onClick={() => onVetSelected(vet.id)} size="sm" disabled={isLoadingAssociation} className="px-5 py-2.5 rounded-lg border border-[#FFECAB]/50 bg-cyan-800 text-[#FFECAB] hover:bg-cyan-600 focus-visible:ring-cyan-500 disabled:opacity-50 cursor-pointer">
                                <CheckSquare size={16} className="mr-2"/> Select
                            </Button>
                        </li>
                    ))}
                </ul>
            )}
            <div className="mt-6 flex justify-end">
                <Button onClick={onClose} disabled={isLoadingAssociation} className="px-5 py-2.5 rounded-lg border border-[#FFECAB]/50 text-[#FFECAB] hover:bg-red-800 hover:text-[#FFECAB] focus-visible:ring-red-500 disabled:opacity-50 cursor-pointer">
                    Cancel
                </Button>
            </div>
        </Modal>
    );
};

export default VetSelectionModal;