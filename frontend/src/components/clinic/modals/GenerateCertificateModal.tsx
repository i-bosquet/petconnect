import { useState, useEffect, FormEvent, JSX, ChangeEvent } from 'react';
import Modal from '@/components/common/Modal';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { PetProfileDto, CertificateGenerationRequestDto } from '@/types/apiTypes';
import { generateCertificate } from '@/services/certificateService'; 
import { useAuth } from '@/hooks/useAuth';
import { Loader2, FileSignature, CircleX, InfoIcon, LockKeyhole, Eye, EyeOff } from 'lucide-react';
import { toast } from 'sonner';

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
    const { token, user} = useAuth();
    const [certificateNumber, setCertificateNumber] = useState<string>('');
    const [vetPassword, setVetPassword] = useState<string>(''); 
    const [showVetPassword, setShowVetPassword] = useState<boolean>(false); 
    const [clinicPassword, setClinicPassword] = useState<string>('');
    const [showClinicPassword, setShowClinicPassword] = useState<boolean>(false); 

    const [isLoading, setIsLoading] = useState<boolean>(false);
    const [error, setError] = useState<string>('');

    useEffect(() => {
        if (isOpen) {
            setCertificateNumber('');
            setVetPassword('');
            setShowVetPassword(false);
            setClinicPassword('');
            setShowClinicPassword(false);
            setError('');
            setIsLoading(false);
        }
    }, [isOpen]);

    const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        if (!token) { setError("Authentication error. Please log in again.");return;}
        if (!user?.roles?.includes('VET')) { setError("Only veterinarians can generate certificates."); return;}
        if (!certificateNumber.trim()) {setError("Official Certificate Number is required.");return;}
        if (!vetPassword.trim()) { setError("Your signing password is required."); return; }
        if (!clinicPassword.trim()) { setError("Clinic's signing password is required."); return;}

        setIsLoading(true);
        setError('');

        const payload: CertificateGenerationRequestDto = {
            petId: pet.id,
            certificateNumber: certificateNumber.trim(),
            vetPrivateKeyPassword: vetPassword,
            clinicPrivateKeyPassword: clinicPassword,
        };

        try {
            await generateCertificate(token, payload); 
            toast.success("Certificate generated successfully.");
            onCertificateGenerated(); 
        } catch (err) {
            const errMsg = err instanceof Error ? err.message : "Could not generate certificate.";
            console.error("Failed to generate certificate:", err);
            setError(errMsg);
            toast.error(errMsg); 
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

                {error && (<div className="p-3 bg-red-900/30 text-red-300 rounded-lg text-sm text-center">{error}</div>)}

                {/* Certificate Number */}
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

                 {/* Vet password */}
                <div className="space-y-1.5">
                    <Label htmlFor="vetCertPassword" className="text-gray-300 flex items-center">
                        <LockKeyhole size={16} className="mr-2 text-orange-400" /> Your Signing Password *
                    </Label>
                    <div className="relative">
                        <Input id="vetCertPassword" type={showVetPassword ? "text" : "password"} value={vetPassword}
                               onChange={(e) => setVetPassword(e.target.value)}
                               placeholder="Your private key password" required disabled={isLoading}
                               className="bg-[#070913] border-gray-700 pr-10"/>
                        <button type="button" onClick={() => setShowVetPassword(!showVetPassword)}
                                className="absolute inset-y-0 right-0 pr-3 flex items-center text-gray-400 hover:text-gray-200">
                            {showVetPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                        </button>
                    </div>
                </div>

                {/* Clinic password */}
                <div className="space-y-1.5">
                    <Label htmlFor="clinicCertPassword" className="text-gray-300 flex items-center">
                        <LockKeyhole size={16} className="mr-2 text-red-400" /> Clinic's Signing Password *
                    </Label>
                     <p className="text-xs text-gray-400">
                        Enter the password for the clinic's private signing key.
                    </p>
                    <div className="relative">
                        <Input id="clinicCertPassword" type={showClinicPassword ? "text" : "password"} value={clinicPassword}
                               onChange={(e) => setClinicPassword(e.target.value)}
                               placeholder="Clinic's private key password" required disabled={isLoading}
                               className="bg-[#070913] border-gray-700 pr-10"/>
                        <button type="button" onClick={() => setShowClinicPassword(!showClinicPassword)}
                                className="absolute inset-y-0 right-0 pr-3 flex items-center text-gray-400 hover:text-gray-200">
                            {showClinicPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                        </button>
                    </div>
                </div>

                {/* Action buttons*/}
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