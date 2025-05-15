import { useState, JSX, useEffect } from 'react';
import Modal from '@/components/common/Modal';
import { Button } from '@/components/ui/button';
import { ClinicDto } from '@/types/apiTypes'; 
import { CheckSquare, CircleX, Building, Loader2 } from 'lucide-react';
import { toast } from 'sonner';

interface SelectClinicForCertificateRequestModalProps {
    isOpen: boolean;
    onClose: () => void;
    availableClinics: ClinicDto[];
    onClinicSelected: (clinicId: number | string) => void;
    isLoading?: boolean;
    petPendingCertificateClinicId?: number | string | null;
}

/**
 * SelectClinicForCertificateRequestModal - Allows an owner to select one of their pet's
 * associated clinics to send a certificate request to.
 * @param {SelectClinicForCertificateRequestModalProps} props - Component props.
 * @returns {JSX.Element | null} The modal component.
 */
const SelectClinicForCertificateRequestModal = ({
    isOpen,
    onClose,
    availableClinics,
    onClinicSelected,
    isLoading,
    petPendingCertificateClinicId 
}: SelectClinicForCertificateRequestModalProps): JSX.Element | null => {
    const [selectedClinicId, setSelectedClinicId] = useState<string | number | null>(null);

    useEffect(() => {
        if (isOpen) {
            setSelectedClinicId(null);
        }
    }, [isOpen]);

    const handleSubmit = () => {
        if (selectedClinicId) {
            if (petPendingCertificateClinicId && petPendingCertificateClinicId === selectedClinicId) {
                toast.info(`A request is already pending at this clinic.`);
                return;
            }
            onClinicSelected(selectedClinicId);
        }
    };

    if (!isOpen) return null;

    return (
        <Modal title="Select Clinic for Certificate Request" onClose={onClose} maxWidth="max-w-md">
            <div className="space-y-4">
                <p className="text-sm text-gray-300">
                    Your pet is associated with veterinarians from multiple clinics.
                    Please select the clinic you'd like to request the certificate from.
                </p>
                <div className="space-y-2 max-h-60 overflow-y-auto custom-scrollbar pr-1">
                    {availableClinics.map((clinic) => {
                        const isPendingAtThisClinic = petPendingCertificateClinicId === clinic.id;

                        return (
                            <Button
                                key={clinic.id}
                                className={`w-full justify-start text-left h-auto p-3 transition-all cursor-pointer ${
                                    selectedClinicId === clinic.id
                                        ? 'bg-cyan-800 text-white' 
                                    : isPendingAtThisClinic
                                        ? 'opacity-60 cursor-not-allowed border-yellow-600 bg-yellow-900/20 text-yellow-300 hover:bg-yellow-900/30'
                                        : 'border-gray-700 hover:bg-gray-700/70 text-gray-200 hover:text-white' 
                                }`}
                                onClick={() => {
                                    if (!isPendingAtThisClinic) { 
                                        setSelectedClinicId(clinic.id);
                                    } else {
                                        toast.info("A certificate request is already pending at this clinic.");
                                    }
                                }}
                                disabled={isPendingAtThisClinic || isLoading} 
                                title={
                                    isPendingAtThisClinic
                                        ? "A certificate request is already pending at this clinic."
                                        : `Select ${clinic.name}`
                                }
                            >
                                <Building size={20} className={`mr-3 flex-shrink-0 ${selectedClinicId === clinic.id ? 'text-white' : isPendingAtThisClinic ? 'text-yellow-500' : 'text-cyan-400'}`} />
                                <div className="flex-grow">
                                    <p className={`font-medium ${selectedClinicId === clinic.id ? 'text-white' : 'text-gray-100'}`}>{clinic.name}</p>
                                    <p className={`text-xs ${selectedClinicId === clinic.id ? 'text-cyan-200' : 'text-gray-400'}`}>
                                        {clinic.city}, {clinic.country.replace(/_/g, " ")}
                                    </p>
                                    {isPendingAtThisClinic && (
                                        <p className="text-xs text-yellow-400 mt-1 italic">(Request Pending)</p>
                                    )}
                                </div>
                                {selectedClinicId === clinic.id && <CheckSquare size={18} className="text-white ml-2 flex-shrink-0"/>}
                            </Button>
                        );
                    })}
                </div>
                <div className="flex justify-end gap-3 pt-4 border-t border-[#FFECAB]/20 mt-5">
                    <Button type="button" onClick={onClose} disabled={isLoading} className="x-5 py-2.5 rounded-lg border border-[#FFECAB]/50 text-[#FFECAB] hover:bg-red-800 hover:text-[#FFECAB] focus-visible:ring-red-500 disabled:opacity-50 cursor-pointer">
                         <CircleX size={16} className="mr-2"/>Cancel
                    </Button>
                    <Button onClick={handleSubmit} disabled={!selectedClinicId || isLoading} className="px-5 py-2.5 rounded-lg border border-[#FFECAB]/50 bg-cyan-800 text-[#FFECAB] hover:bg-cyan-600 focus-visible:ring-cyan-500 disabled:opacity-50 cursor-pointer">
                        {isLoading && <Loader2 className="animate-spin h-4 w-4 mr-2" />}
                         <CheckSquare size={16} className="mr-2"/>Confirm Clinic
                    </Button>
                </div>
            </div>
        </Modal>
    );
};

export default SelectClinicForCertificateRequestModal;