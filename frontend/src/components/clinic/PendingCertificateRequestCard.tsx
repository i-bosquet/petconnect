import { JSX } from 'react';
import { PetProfileDto } from '@/types/apiTypes';
import { Button } from '@/components/ui/button';
import { PawPrint, UserCircle, FileSignature as CertificateIcon } from 'lucide-react'; 

interface PendingCertificateRequestCardProps {
    pet: PetProfileDto;
    onProcess: () => void; 
    canProcess: boolean;
}

/**
 * PendingCertificateRequestCard - Displays a summary of a pet certificate generation request.
 * Intended for use in the Clinic Dashboard.
 * @param {PendingCertificateRequestCardProps} props - Component props.
 * @returns {JSX.Element} A card displaying pet certificate request details.
 * @author ibosquet
 */
const PendingCertificateRequestCard = ({ pet, onProcess, canProcess }: PendingCertificateRequestCardProps): JSX.Element => {
    const avatarUrl = pet.image;

    return (
        <div className="bg-[#0A0F1E] border border-purple-700/50 rounded-lg p-4 shadow-lg space-y-3 transition-all hover:shadow-purple-700/30"> 
            <div className="flex items-center gap-3">
                <div className="w-12 h-12 rounded-full bg-gray-700 flex items-center justify-center overflow-hidden border-2 border-gray-600">
                    {avatarUrl ? (
                        <img src={avatarUrl} alt={pet.name} className="w-full h-full object-cover" onError={(e) => (e.currentTarget.src = `/src/assets/images/avatars/pets/${pet.specie?.toLowerCase()}.png`)} />
                    ) : (
                        <PawPrint size={24} className="text-gray-500" />
                    )}
                </div>
                <div>
                    <h3 className="font-semibold text-lg text-white">{pet.name}</h3>
                    <p className="text-xs text-gray-400 capitalize">{pet.specie?.toLowerCase()}</p>
                </div>
            </div>

            <div className="text-sm space-y-1">
                <div className="flex items-center text-gray-300">
                    <UserCircle size={14} className="mr-2 text-purple-400 flex-shrink-0" />
                    <span>Owner: <span className="font-medium text-gray-200">{pet.ownerUsername}</span></span>
                </div>
            </div>

            <Button
                onClick={onProcess}
                disabled={!canProcess || pet.status !== 'ACTIVE'} 
                className="w-full mt-2 bg-purple-800 hover:bg-purple-600 text-[#FFECAB] disabled:bg-gray-500 disabled:cursor-not-allowed cursor-pointer"
                aria-label={`Process certificate request for ${pet.name}`}
                title={pet.status !== 'ACTIVE' ? "Pet must be active to generate a certificate" : (canProcess ? "Generate Certificate" : "View Details (Vet action)")}
            >
                <CertificateIcon size={16} className="mr-2" />
                {canProcess ? "Generate Certificate" : "View Details (Vet action)"}
            </Button>
            {!canProcess && pet.status === 'ACTIVE' && ( 
                 <p className="text-xs text-yellow-400 mt-1 text-center italic">A Veterinarian must generate the certificate.</p>
            )}
             {pet.status !== 'ACTIVE' && canProcess && (
                <p className="text-xs text-red-400 mt-1 text-center italic">Pet must be in ACTIVE status.</p>
            )}
        </div>
    );
};

export default PendingCertificateRequestCard;