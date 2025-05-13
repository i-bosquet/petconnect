import { useState, FormEvent, JSX } from 'react';
import Modal from '@/components/common/Modal';
import { Button } from '@/components/ui/button';
import { Label } from '@/components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Loader2, CheckCircle, CircleX, Clock } from 'lucide-react';
import { TemporaryAccessRequestPayload, TemporaryAccessTokenResponse } from '@/types/apiTypes';
import { generateTemporaryRecordAccessToken } from '@/services/recordService';
import { useAuth } from '@/hooks/useAuth';
import { toast } from 'sonner';

interface RequestTempAccessModalProps {
    isOpen: boolean;
    onClose: () => void;
    petId: number | string;
    onTokenGenerated: (accessToken: string) => void; // Callback con el token
}

const durationOptions = [
    { value: "PT1H", label: "1 Hour" },
    { value: "PT6H", label: "6 Hours" },
    { value: "P1D", label: "1 Day (24 Hours)" },
    { value: "P3D", label: "3 Days" },
    { value: "P7D", label: "1 Week" },
];

/**
 * RequestTempAccessModal - Modal for the owner to select a duration and generate
 * a temporary access token for their pet's signed medical records.
 * @param {RequestTempAccessModalProps} props - Component props.
 * @returns {JSX.Element | null} The modal component.
 */
const RequestTempAccessModal = ({ isOpen, onClose, petId, onTokenGenerated }: RequestTempAccessModalProps): JSX.Element | null => {
    const { token } = useAuth();
    const [selectedDuration, setSelectedDuration] = useState<string>(durationOptions[0].value); // Default a 1 hour
    const [isLoading, setIsLoading] = useState<boolean>(false);
    const [error, setError] = useState<string>('');

    const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        if (!token) {
            setError("Authentication error.");
            return;
        }
        setIsLoading(true);
        setError('');

        const payload: TemporaryAccessRequestPayload = {
            durationString: selectedDuration,
        };

        try {
            const response: TemporaryAccessTokenResponse = await generateTemporaryRecordAccessToken(token, petId, payload);
            onTokenGenerated(response.token); // Pass the token to the parent
        } catch (err) {
            console.error("Failed to generate temporary token:", err);
            const errMsg = err instanceof Error ? err.message : "Could not generate temporary access token.";
            setError(errMsg);
            toast.error(errMsg);
        } finally {
            setIsLoading(false);
        }
    };

    if (!isOpen) return null;

    return (
        <Modal title="Share Pet's Medical History" onClose={onClose} maxWidth="max-w-md">
            <form onSubmit={handleSubmit} className="space-y-4">
                <p className="text-sm text-gray-300">
                    Generate a temporary, secure link to share your pet's signed medical records.
                    Select the duration for which the link will be active.
                </p>

                {error && (
                    <div className="p-3 bg-red-900/30 text-red-300 rounded-lg text-sm text-center">
                        {error}
                    </div>
                )}

                <div className="space-y-1.5">
                    <Label htmlFor="durationSelect" className="text-gray-300 flex items-center">
                        <Clock size={14} className="mr-2 text-cyan-400"/> Access Duration *
                    </Label>
                    <Select value={selectedDuration} onValueChange={setSelectedDuration} disabled={isLoading} required>
                        <SelectTrigger className="bg-[#070913] border-gray-700 focus:ring-cyan-600 focus:border-cyan-600">
                            <SelectValue placeholder="Select duration" />
                        </SelectTrigger>
                        <SelectContent className="bg-[#0c1225] border-gray-700 text-white">
                            {durationOptions.map(opt => (
                                <SelectItem key={opt.value} value={opt.value} className="hover:bg-cyan-800 focus:bg-cyan-700">
                                    {opt.label}
                                </SelectItem>
                            ))}
                        </SelectContent>
                    </Select>
                </div>

                <div className="flex justify-end gap-3 pt-4 border-t border-[#FFECAB]/20 mt-5">
                    <Button type="button"  onClick={onClose} disabled={isLoading} className="px-5 py-2.5 rounded-lg border border-[#FFECAB]/50 text-[#FFECAB] hover:bg-red-800 hover:text-[#FFECAB] focus-visible:ring-red-500 disabled:opacity-50 cursor-pointer">
                         <CircleX size={16} className="mr-2"/>Cancel
                    </Button>
                    <Button type="submit" disabled={isLoading} className="px-5 py-2.5 rounded-lg border border-[#FFECAB]/50 bg-cyan-800 text-[#FFECAB] hover:bg-cyan-600 focus-visible:ring-cyan-500 disabled:opacity-50 cursor-pointer">
                        {isLoading && <Loader2 className="animate-spin h-4 w-4 mr-2" />}
                         <CheckCircle size={16} className="mr-2"/>Generate Link
                    </Button>
                </div>
            </form>
        </Modal>
    );
};

export default RequestTempAccessModal;