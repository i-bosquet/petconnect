import { useState, useEffect, FormEvent, JSX, ChangeEvent } from 'react';
import Modal from '@/components/common/Modal';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { PetProfileDto, CertificateGenerationRequestDto } from '@/types/apiTypes';
import { generateCertificate } from '@/services/certificateService'; 
import { useAuth } from '@/hooks/useAuth';
import { Loader2, FileSignature, CircleX, InfoIcon } from 'lucide-react';

interface GenerateCertificateModalProps {
    isOpen: boolean;
    onClose: () => void;
    pet: PetProfileDto;
    onCertificateGenerated: () => void;
}

/**
 * GenerateCertificateModal - Modal for a Veterinarian to input the official
 * certificate number and generate a health certificate for a pet.
 * The backend will verify prerequisites like valid rabies vaccine and health checkup.
 * @param {GenerateCertificateModalProps} props - Component props.
 * @returns {JSX.Element | null} The modal component.
 */
const GenerateCertificateModal = ({
    isOpen,
    onClose,
    pet,
    onCertificateGenerated
}: GenerateCertificateModalProps): JSX.Element | null => {
    const { token } = useAuth();
    const [certificateNumber, setCertificateNumber] = useState<string>('');
    const [isLoading, setIsLoading] = useState<boolean>(false);
    const [error, setError] = useState<string>('');

    useEffect(() => {
        if (isOpen) {
            setCertificateNumber('');
            setError('');
            setIsLoading(false);
        }
    }, [isOpen]);

    const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        if (!token) {
            setError("Authentication error. Please log in again.");
            return;
        }
        if (!certificateNumber.trim()) {
            setError("Official Certificate Number is required.");
            return;
        }

        setIsLoading(true);
        setError('');

        const payload: CertificateGenerationRequestDto = {
            petId: pet.id,
            certificateNumber: certificateNumber.trim(),
        };

        try {
            await generateCertificate(token, payload); // Usar el servicio de certificado
            // toast.success se llamará en el padre (ClinicDashboardPage) a través de onCertificateGenerated
            onCertificateGenerated(); // Esto cerrará el modal y refrescará la lista
        } catch (err) {
            const errMsg = err instanceof Error ? err.message : "Could not generate certificate.";
            console.error("Failed to generate certificate:", err);
            setError(errMsg);
            // No mostramos toast de error aquí, se muestra en el modal
        } finally {
            setIsLoading(false);
        }
    };

    if (!isOpen) return null;

    return (
        <Modal title={`Generate Certificate for ${pet.name}`} onClose={onClose} maxWidth="max-w-lg">
            <form onSubmit={handleSubmit} className="space-y-4">
                <div className="p-3 bg-gray-800/30 border border-gray-700 rounded-md text-sm">
                    <p className="text-gray-300 mb-1">
                        Pet: <strong className="text-white">{pet.name}</strong> ({pet.specie?.toLowerCase()})
                    </p>
                    <p className="text-gray-300">
                        Owner: <strong className="text-white">{pet.ownerUsername}</strong>
                    </p>
                </div>

                <div className="p-3 bg-blue-900/20 border border-blue-700 rounded-md text-xs text-blue-200">
                    <InfoIcon size={14} className="inline mr-1.5 align-text-bottom"/>
                    The system will verify that <strong className="text-white">{pet.name}</strong> has a valid, signed Rabies Vaccination
                    and a recent, signed Health Checkup on record before issuing the certificate.
                </div>


                {error && (
                    <div className="p-3 bg-red-900/30 text-red-300 rounded-lg text-sm text-center">
                        {error}
                    </div>
                )}

                <div className="space-y-1.5">
                    <Label htmlFor="certificateNumber" className="text-gray-300">
                        Official Certificate Number (AHC Number) *
                    </Label>
                    <Input
                        id="certificateNumber"
                        name="certificateNumber"
                        type="text"
                        value={certificateNumber}
                        onChange={(e: ChangeEvent<HTMLInputElement>) => setCertificateNumber(e.target.value)}
                        disabled={isLoading}
                        required
                        placeholder="Enter the official AHC number"
                        className="bg-[#070913] border-gray-700 focus:ring-cyan-600 focus:border-cyan-600"
                    />
                </div>

                <div className="flex justify-end gap-3 pt-4 border-t border-[#FFECAB]/20 mt-5">
                    <Button
                        type="button"
                        onClick={onClose}
                        disabled={isLoading}
                        className="px-5 py-2.5 rounded-lg border border-[#FFECAB]/50 text-[#FFECAB] hover:bg-red-800 hover:text-[#FFECAB] focus-visible:ring-red-500 disabled:opacity-50 cursor-pointer"
                    >
                        <CircleX size={16} className="mr-2"/>Cancel
                    </Button>
                    <Button
                        type="submit"
                        disabled={isLoading}
                        className="px-5 py-2.5 rounded-lg border border-[#FFECAB]/50 bg-cyan-800 text-[#FFECAB] hover:bg-cyan-600 focus-visible:ring-cyan-500 disabled:opacity-50 cursor-pointer"
                    >
                        {isLoading && <Loader2 className="animate-spin h-4 w-4 mr-2" />}
                        <FileSignature size={16} className="mr-2"/>Generate Certificate
                    </Button>
                </div>
            </form>
        </Modal>
    );
};

export default GenerateCertificateModal;