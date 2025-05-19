import { JSX } from 'react';
import { PetProfileDto, PetStatus  } from '@/types/apiTypes'; 
import { PawPrint } from 'lucide-react'; 

interface PetCardProps {
    pet: PetProfileDto;
    onSelect: () => void;
}

/**
 * PetCard - A clickable component displaying basic pet information (image, name, species).
 * Used within the PetList component.
 *
 * @param {PetCardProps} props - Component properties.
 * @returns {JSX.Element} A button styled as a pet profile card.
 */
const PetCard = ({ pet, onSelect }: PetCardProps): JSX.Element => {

    const avatarUrl = pet.image
    const isInactive = pet.status === PetStatus.INACTIVE;

    return (
        <button
            onClick={onSelect}
            className={`flex flex-col items-center p-3 rounded-xl transition-all border border-[#FFECAB] hover:opacity-80 min-w-28 cursor-pointer ${
                isInactive ? 'opacity-50 filter grayscale' : '' 
            }`}
        >
            {/* Pet avatar image*/}
            <div className="relative mb-2">
                <div className="w-16 h-16 rounded-full bg-gray-700 flex items-center justify-center overflow-hidden border-2 border-gray-600 group-hover:border-cyan-600 transition-colors">
                     {avatarUrl ? (
                         <img
                             src={avatarUrl}
                             alt={pet.name}
                             className="w-full h-full object-cover"
                         />
                     ) : (
                         <PawPrint size={32} className="text-gray-500" /> 
                     )}
                </div>
            </div>
            {/* Pet name */}
            <span className="font-medium mt-1 text-sm text-[#FFECAB] truncate w-full">{pet.name}</span> 
            {/* Pet species */}
            <span className="text-xs text-gray-400 capitalize">
                {pet.specie?.toLowerCase() || 'Unknown'} 
            </span>
            {isInactive && (
                <span className="text-xs text-red-400 font-semibold mt-1">(Inactive)</span>
            )}
        </button>
    );
};

export default PetCard;