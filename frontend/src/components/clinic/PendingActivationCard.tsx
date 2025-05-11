import { JSX } from 'react';
import { PetProfileDto } from '@/types/apiTypes';
import { Button } from '@/components/ui/button';
import { PawPrint, UserCircle, CalendarCheck2 } from 'lucide-react'; 

interface PendingActivationCardProps {
    pet: PetProfileDto;
    onProcess: () => void; 
    canProcess: boolean;  
}

/**
 * PendingActivationCard - Displays a summary of a pet activation request.
 * @param {PendingActivationCardProps} props - Component props.
 * @returns {JSX.Element} A card displaying pet activation request details.
 * @author ibosquet
 */
const PendingActivationCard = ({ pet, onProcess, canProcess }: PendingActivationCardProps): JSX.Element => {
    const avatarUrl = pet.image; 

    return (
        <div className="bg-[#0A0F1E] border border-cyan-700/50 rounded-lg p-4 shadow-lg space-y-3 transition-all hover:shadow-cyan-700/30">
            <div className="flex items-center gap-3">
                <div className="w-12 h-12 rounded-full bg-gray-700 flex items-center justify-center overflow-hidden border-2 border-gray-600">
                    {avatarUrl ? (
                        <img src={avatarUrl} alt={pet.name} className="w-full h-full object-cover" />
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
                    <UserCircle size={14} className="mr-2 text-cyan-400 flex-shrink-0" />
                    <span>Owner: <span className="font-medium text-gray-200">{pet.ownerUsername}</span></span>
                </div>
            </div>

            <Button
                onClick={onProcess}
                disabled={!canProcess || pet.status !== 'PENDING'} 
                className="w-full mt-2 bg-cyan-800 hover:bg-cyan-600 text-[#FFECAB] disabled:bg-gray-500 disabled:cursor-not-allowed cursor-pointer"
                aria-label={`Process activation for ${pet.name}`}
            >
                <CalendarCheck2 size={16} className="mr-2" />
                {canProcess ? "Process Activation" : "View Details (Vet action)"}
            </Button>
            {!canProcess && pet.status === 'PENDING' && (
                 <p className="text-xs text-yellow-400 mt-1 text-center italic">A Veterinarian must process this request.</p>
            )}
        </div>
    );
};

export default PendingActivationCard;