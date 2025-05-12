import { useState, useEffect, FormEvent, ChangeEvent, JSX } from 'react';
import Modal from '@/components/common/Modal';
import { Button } from '@/components/ui/button';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { RecordViewDto, RecordUpdatePayload, RecordType } from '@/types/apiTypes';
import { updateUnsignedRecord } from '@/services/recordService';
import { useAuth } from '@/hooks/useAuth';
import { Loader2, Save, CircleX } from 'lucide-react';
import { toast } from 'sonner';

interface EditRecordModalProps {
    isOpen: boolean;
    onClose: () => void;
    onRecordUpdated: () => void;
    recordInitialData: RecordViewDto; 
}

/**
 * EditRecordModal - Modal for an Owner to edit their own unsigned, non-vaccine medical records.
 * @param {EditRecordModalProps} props - Component props.
 * @returns {JSX.Element | null} The modal for editing a medical record.
 * @author ibosquet
 */
const EditRecordModal = ({ isOpen, onClose, onRecordUpdated, recordInitialData }: EditRecordModalProps): JSX.Element | null => {
    const { token} = useAuth();
    const [formData, setFormData] = useState<Partial<RecordUpdatePayload>>({
        type: recordInitialData.type,
        description: recordInitialData.description || '',
    });
    const [isLoading, setIsLoading] = useState<boolean>(false);
    const [error, setError] = useState<string>('');

    useEffect(() => {
        if (isOpen && recordInitialData) {
            setFormData({
                type: recordInitialData.type,
                description: recordInitialData.description || '',
            });
            setError('');
            setIsLoading(false);
        }
    }, [isOpen, recordInitialData]);

    const handleMainChange = (e: ChangeEvent<HTMLTextAreaElement>) => { 
        const { name, value } = e.target;
        setFormData(prev => ({ ...prev, [name]: value }));
    };

    const handleRecordTypeChange = (value: string) => {
        const newType = value as RecordType;
        if (newType === RecordType.VACCINE && recordInitialData.type !== RecordType.VACCINE) {
            setError("Cannot change record type to VACCINE.");
            return;
        }
        setError(''); 
        setFormData(prev => ({ ...prev, type: newType }));
    };

    const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        if (!token) {
            setError("Authentication error.");
            return;
        }
        if (!formData.type) {
            setError("Record type is inexplicably missing. Please try again.");
            return;
        }

        if (recordInitialData.type === RecordType.VACCINE && formData.type !== RecordType.VACCINE) {
            setError("Cannot change the type of a VACCINE record.");
            return;
        }
         if (formData.type === RecordType.VACCINE && recordInitialData.type !== RecordType.VACCINE) {
            setError("Cannot change record type to VACCINE.");
            return;
        }

        setIsLoading(true);
        setError('');

        const payload: RecordUpdatePayload = {
            type: formData.type,
            description: formData.description || null, 
        };

        try {
            await updateUnsignedRecord(token, recordInitialData.id, payload);
            toast.success("Medical record updated successfully!");
            onRecordUpdated();
            onClose();
        } catch (err) {
            console.error("Failed to update record:", err);
            setError(err instanceof Error ? err.message : "Could not update medical record.");
        } finally {
            setIsLoading(false);
        }
    };

    if (!isOpen || !recordInitialData) return null;

    if (recordInitialData.vetSignature || recordInitialData.type === RecordType.VACCINE) {
        console.warn("EditRecordModal opened for a signed or vaccine record. This should not happen.");
        return (
            <Modal title="Error" onClose={onClose}>
                <p className="text-red-400">This record cannot be edited.</p>
            </Modal>
        );
    }

    const formatDate = (dateString: string | null | undefined): string => {
        if (!dateString) return "N/A";
        return new Date(dateString).toLocaleString("en-GB", {
        day: "2-digit",
        month: "short",
        year: "numeric",
        hour: "2-digit",
        minute: "2-digit",
        });
    };

    return (
        <Modal title={`Edit Record (${formatDate(recordInitialData.createdAt)})`} onClose={onClose} maxWidth="max-w-lg">
            <form onSubmit={handleSubmit} className="space-y-4">
                {error && (
                    <div className="p-3 bg-red-900/30 text-red-300 rounded-lg text-sm text-center">
                        {error}
                    </div>
                )}

                {/* Record Type */}
                <div className="space-y-1.5">
                    <Label htmlFor="editRecordType" className="text-gray-300">Record Type *</Label>
                    <Select name="type" value={formData.type || ""}  onValueChange={handleRecordTypeChange} disabled={isLoading} required>
                        <SelectTrigger className="bg-[#070913] border-gray-700 focus:ring-cyan-600 focus:border-cyan-600">
                            <SelectValue placeholder="Select record type" />
                        </SelectTrigger>
                        <SelectContent className="bg-[#0c1225] border-gray-700 text-white">
                            {Object.values(RecordType)
                                .filter(type => type !== RecordType.VACCINE || recordInitialData.type === RecordType.VACCINE) 
                                .map(type => (
                                    <SelectItem key={type} value={type} className="hover:bg-cyan-800 focus:bg-cyan-700"
                                        disabled={recordInitialData.type === RecordType.VACCINE && type !== RecordType.VACCINE} 
                                    >
                                        {type.replace(/_/g, ' ').replace(/\b\w/g, l => l.toUpperCase())}
                                    </SelectItem>
                            ))}
                        </SelectContent>
                    </Select>
                </div>

                {/* Description */}
                <div className="space-y-1.5">
                    <Label htmlFor="editDescription" className="text-gray-300">Description</Label>
                    <Textarea
                        id="editDescription"
                        name="description"
                        value={formData.description || ''}
                        onChange={handleMainChange}
                        disabled={isLoading}
                        placeholder="Notes, symptoms, observations, etc."
                        rows={5}
                        maxLength={2000}
                        className="bg-[#070913] border-gray-700 focus:ring-cyan-600 focus:border-cyan-600 custom-scrollbar"
                    />
                </div>

                {/* Action Buttons */}
                <div className="flex justify-end gap-3 pt-4 border-t border-[#FFECAB]/20 mt-5">
                    <Button type="button" onClick={onClose} disabled={isLoading} className="px-5 py-2.5 rounded-lg border border-[#FFECAB]/50 text-[#FFECAB] hover:bg-red-800 hover:text-[#FFECAB] focus-visible:ring-red-500 disabled:opacity-50 cursor-pointer">
                         <CircleX size={16} className="mr-2"/>Cancel
                    </Button>
                    <Button type="submit" disabled={isLoading} className="px-5 py-2.5 rounded-lg border border-[#FFECAB]/50 bg-cyan-800 text-[#FFECAB] hover:bg-cyan-600 focus-visible:ring-cyan-500 disabled:opacity-50 cursor-pointer">
                        {isLoading && <Loader2 className="animate-spin h-4 w-4 mr-2" />}
                         <Save size={16} className="mr-2"/>Save Changes
                    </Button>
                </div>
            </form>
        </Modal>
    );
};

export default EditRecordModal;