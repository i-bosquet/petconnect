import { useState, FormEvent, JSX, ChangeEvent } from 'react';
import Modal from '@/components/common/Modal';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { PetProfileDto, PetOwnerUpdatePayload } from '@/types/apiTypes';
import { updatePetByOwner } from '@/services/petService';
import { useAuth } from '@/hooks/useAuth';
import { Loader2, PlaneTakeoff, CircleX } from 'lucide-react';
import { toast } from 'sonner';

interface RegisterEuEntryModalProps {
    isOpen: boolean;
    onClose: () => void;
    pet: PetProfileDto;
    onEntryRegistered: () => void;
}

/**
 * RegisterEuEntryModal - Modal for the owner to register their pet's entry date into the EU.
 * @param {RegisterEuEntryModalProps} props - Component props.
 * @returns {JSX.Element | null} The modal component.
 */
const RegisterEuEntryModal = ({ isOpen, onClose, pet, onEntryRegistered }: RegisterEuEntryModalProps): JSX.Element | null => {
    console.log("RegisterEuEntryModal received pet:", pet);
    const { token } = useAuth();
    const [entryDate, setEntryDate] = useState<string>(new Date().toISOString().split('T')[0]); // Default today's date
    const [isLoading, setIsLoading] = useState<boolean>(false);
    const [error, setError] = useState<string>('');

    const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        if (!token) { setError("Authentication error."); return; }
        if (!entryDate) { setError("EU Entry Date is required."); return; }

        if (pet.lastEuExitDate && new Date(entryDate) <= new Date(pet.lastEuExitDate)) {
            setError("Entry date must be after the last registered exit date.");
            return;
        }

        setIsLoading(true);
        setError('');

        const payload: PetOwnerUpdatePayload = {
            newEuEntryDate: entryDate,
        };

        try {
            await updatePetByOwner(token, pet.id, payload, null); // No se actualiza imagen aquí
            toast.success(`EU entry for ${pet.name} registered on ${new Date(entryDate).toLocaleDateString('en-GB')}.`);
            onEntryRegistered();
            onClose();
        } catch (err) {
            const errMsg = err instanceof Error ? err.message : "Failed to register EU entry.";
            setError(errMsg);
            toast.error(errMsg);
        } finally {
            setIsLoading(false);
        }
    };

    if (!isOpen) return null;

    return (
        <Modal title={`Register EU Entry for ${pet.name}`} onClose={onClose} maxWidth="max-w-md">
            <form onSubmit={handleSubmit} className="space-y-4">
                {error && (
                    <div className="p-3 bg-red-900/30 text-red-300 rounded-lg text-sm text-center">{error}</div>
                )}
                <p className="text-sm text-gray-300">
                    Please confirm the date {pet.name} entered the EU.
                    This will affect the validity calculation of travel certificates.
                </p>
                <div className="space-y-1.5">
                    <Label htmlFor="euEntryDate" className="text-gray-300">EU Entry Date *</Label>
                    <Input
                        id="euEntryDate"
                        type="date"
                        value={entryDate}
                        onChange={(e: ChangeEvent<HTMLInputElement>) => setEntryDate(e.target.value)}
                        required
                        disabled={isLoading}
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
                        <PlaneTakeoff size={16} className="mr-2"/>Confirm Entry
                    </Button>
                </div>
            </form>
        </Modal>
    );
};
export default RegisterEuEntryModal;