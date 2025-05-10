import { JSX } from 'react'; 
import { Edit,  ArrowRight } from 'lucide-react';
import { Card, CardHeader, CardTitle } from "@/components/ui/card";
import { PetProfileDto } from '@/types/apiTypes';
import { Button } from '@/components/ui/button';
import { Tooltip,TooltipTrigger, TooltipContent } from '@/components/ui/tooltip';

interface PetDetailHeaderProps {
    pet: PetProfileDto; 
    onBack: () => void;
    onEdit: () => void; 
}

/**
 * PetDetailHeader - Displays the pet's profile header information
 * with actions to go back or edit pet details.
 */
const PetDetailHeader = ({ pet, onBack, onEdit }: PetDetailHeaderProps): JSX.Element => {
    if (!pet) {
        return (
            <Card className="border-2 border-[#FFECAB]/30 bg-[#070913]/50">
                <CardHeader>
                    <CardTitle className="text-center text-gray-400">Loading pet details...</CardTitle>
                </CardHeader>
            </Card>
        );
    }

    const formatDate = (dateString: string | null | undefined) => {
        if (!dateString) return 'N/A';
        try {
            const date = new Date(dateString);
            const day = String(date.getDate()).padStart(2, '0');
            const month = String(date.getMonth() + 1).padStart(2, '0'); 
            const year = date.getFullYear();
            return `${day}/${month}/${year}`;
        } catch {
            return 'Invalid Date';
        }
    };

    return (
        <Card className="border-2 border-[#FFECAB] bg-[#090D1A]">
            <CardHeader>
                <div className="flex items-center gap-4">
                    <img
                        src={pet.image || '/src/assets/images/avatars/pets/default_pet.png'} 
                        alt={pet.name}
                        className="w-20 h-20 sm:w-24 sm:h-24 rounded-full object-cover border-4 border-[#FFECAB]/50 bg-gray-700"
                        onError={(e) => (e.currentTarget.src = `/src/assets/images/avatars/pets/${pet.specie?.toLowerCase() || 'default_pet'}.png`)}
                    />

                    <div className="flex-1 text-center sm:text-left">
                        <div className="flex items-center justify-center sm:justify-start gap-2">
                            <h2 className="text-xl lg:text-2xl font-bold ">{pet.name}</h2>
                            <Tooltip>
                                 <TooltipTrigger asChild>
                                    <Button onClick={onEdit}  className="text-[#FFECAB] bg-transparent  hover:text-[#090D1A] hover:bg-[#FFECAB]  h-8 w-8 cursor-pointer m-2">
                                        <Edit />
                                    </Button>
                             </TooltipTrigger>
                             <TooltipContent className="bg-gray-950 text-white border border-cyan-700">
                                <p>Edit {pet.name}'s details</p>
                            </TooltipContent>
                            </Tooltip>
                        </div>
                        <p className="text-sm text-gray-200 capitalize">
                            {pet.specie?.toLowerCase() || 'N/A'} • {pet.breedName || 'N/A'}
                        </p>
                        <p className="text-sm text-gray-300">
                            Born: {formatDate(pet.birthDate)}
                        </p>
                    </div>

                    <Button onClick={onBack} className="px-4 py-2 bg-[#090D1A] text-[#FFECAB] hover:bg-[#FFECAB] hover:text-[#090D1A] rounded-2xl cursor-pointer ">
                        Back<ArrowRight size={16} className="mr-2" /> 
                    </Button>
                </div>
            </CardHeader>
        </Card>
    );
};

export default PetDetailHeader;