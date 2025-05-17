import { useState, FormEvent, JSX, ChangeEvent } from 'react';
import Modal from '@/components/common/Modal';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { PetProfileDto, PetOwnerUpdatePayload } from '@/types/apiTypes';
import { updatePetByOwner } from '@/services/petService';
import { useAuth } from '@/hooks/useAuth';
import { Loader2, PlaneLanding, CircleX } from 'lucide-react';
import { toast } from 'sonner';

interface RegisterEuExitModalProps {
    isOpen: boolean;
    onClose: () => void;
    pet: PetProfileDto;
    onExitRegistered: () => void;
}

/**
 * RegisterEuExitModal - Modal for the owner to register their pet's exit date from the EU.
 * @param {RegisterEuExitModalProps} props - Component props.
 * @returns {JSX.Element | null} The modal component.
 */
const RegisterEuExitModal = ({ isOpen, onClose, pet, onExitRegistered }: RegisterEuExitModalProps): JSX.Element | null => {
    const { token } = useAuth();
    const [exitDate, setExitDate] = useState<string>(new Date().toISOString().split('T')[0]);
    const [isLoading, setIsLoading] = useState<boolean>(false);
    const [error, setError] = useState<string>('');

    const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        if (!token) { setError("Authentication error."); return; }
        if (!exitDate) { setError("EU Exit Date is required."); return; }

        if (!pet.lastEuEntryDate) {
            setError("Cannot register exit without a prior entry date.");
            return;
        }
        if (new Date(exitDate) < new Date(pet.lastEuEntryDate)) {
            setError("Exit date must be on or after the last registered entry date.");
            return;
        }

        setIsLoading(true);
        setError('');
        const payload: PetOwnerUpdatePayload = { newEuExitDate: exitDate };

        try {
            await updatePetByOwner(token, pet.id, payload, null);
            toast.success(`EU exit for ${pet.name} registered on ${new Date(exitDate).toLocaleDateString('en-GB')}.`);
            onExitRegistered();
            onClose();
        } catch (err) {
            const errMsg = err instanceof Error ? err.message : "Failed to register EU exit.";
            setError(errMsg);
            toast.error(errMsg);
        } finally {
            setIsLoading(false);
        }
    };

    if (!isOpen) return null;

    return (
        <Modal title={`Register EU Exit for ${pet.name}`} onClose={onClose} maxWidth="max-w-md">
            <form onSubmit={handleSubmit} className="space-y-4">
                {error && (
                    <div className="p-3 bg-red-900/30 text-red-300 rounded-lg text-sm text-center">{error}</div>
                )}
                <p className="text-sm text-gray-300">
                    Please confirm the date {pet.name} exited the EU (or returned to the UK).
                </p>
                <div className="space-y-1.5">
                    <Label htmlFor="euExitDate" className="text-gray-300">EU Exit Date *</Label>
                    <Input
                        id="euExitDate"
                        type="date"
                        value={exitDate}
                        onChange={(e: ChangeEvent<HTMLInputElement>) => setExitDate(e.target.value)}
                        required
                        disabled={isLoading}
                        min={pet.lastEuEntryDate || undefined} 
                        max={new Date().toISOString().split('T')[0]} 
                        className="bg-[#070913] border-gray-700"
                    />
                </div>
                 <div className="flex justify-end gap-3 pt-4 border-t border-[#FFECAB]/20 mt-5">
                    <Button type="button" onClick={onClose} disabled={isLoading} className="px-5 py-2.5 rounded-lg border border-[#FFECAB]/50 text-[#FFECAB] hover:bg-red-800 hover:text-[#FFECAB] focus-visible:ring-red-500 disabled:opacity-50 cursor-pointer">
                        <CircleX size={16} className="mr-2"/>Cancel
                    </Button>
                    <Button type="submit" disabled={isLoading} className="px-5 py-2.5 rounded-lg border border-[#FFECAB]/50 bg-cyan-800 text-[#FFECAB] hover:bg-cyan-600 focus-visible:ring-cyan-500 disabled:opacity-50 cursor-pointer">
                        {isLoading && <Loader2 className="animate-spin h-4 w-4 mr-2" />}
                        <PlaneLanding size={16} className="mr-2"/>Confirm Exit
                    </Button>
                </div>
            </form>
        </Modal>
    );
};
export default RegisterEuExitModal;