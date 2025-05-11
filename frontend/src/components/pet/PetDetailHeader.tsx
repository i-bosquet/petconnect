import { JSX } from 'react'; 
import { Edit,  ArrowRight, ShieldX, Send   } from 'lucide-react';
import { Card, CardHeader, CardTitle } from "@/components/ui/card";
import { PetProfileDto, PetStatus } from '@/types/apiTypes';
import { Button } from '@/components/ui/button';
import { Tooltip,TooltipTrigger, TooltipContent } from '@/components/ui/tooltip';

interface PetDetailHeaderProps {
    pet: PetProfileDto; 
    onBack: () => void;
    onEdit: () => void;
    onDeactivate: () => void;  
    onRequestActivation: () => void;
}

/**
 * PetDetailHeader - Displays the pet's profile header information
 * with actions to go back or edit pet details.
 */
const PetDetailHeader = ({pet, onBack, onEdit, onDeactivate, onRequestActivation }: PetDetailHeaderProps): JSX.Element => {
    if (!pet) {
        return (
            <Card >
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

    const isPetActiveOrPending = pet.status === PetStatus.ACTIVE || pet.status === PetStatus.PENDING
    const canRequestActivation = pet.status === PetStatus.PENDING && !pet.pendingActivationClinicId;

    return (
      <Card className="border-2 border-[#FFECAB]/50 bg-[#0c1225]/70 shadow-xl">
        <CardHeader>
          <div className="flex items-center gap-4">
            <img
              src={
                pet.image || "/src/assets/images/avatars/pets/default_pet.png"
              }
              alt={pet.name}
              className="w-20 h-20 sm:w-24 sm:h-24 rounded-full object-cover border-4 border-[#FFECAB]/50 bg-gray-700"
              onError={(e) =>
                (e.currentTarget.src = `/src/assets/images/avatars/pets/${pet.specie?.toLowerCase()}.png`)
              }
            />
            <div className="flex-1 text-center sm:text-left">
              <div className="flex items-center justify-center sm:justify-start gap-2">
                <h2 className="text-xl lg:text-2xl font-bold ">{pet.name}</h2>
                {/* Edit button */}
                <Tooltip>
                  <TooltipTrigger asChild>
                    <Button
                      onClick={isPetActiveOrPending ? onEdit : undefined} 
                                        className={`text-[#FFECAB] bg-transparent hover:bg-cyan-800 h-8 w-8 cursor-pointer m-2 ${
                                            !isPetActiveOrPending ? 'hidden' : ''
                                        }`}
                    >
                      <Edit size={18} />
                    </Button>
                  </TooltipTrigger>
                  <TooltipContent className="bg-gray-950 text-white border border-cyan-700">
                     <p>{isPetActiveOrPending ? `Edit ${pet.name}'s details` : `${pet.name} is inactive, details cannot be edited.`}</p>
                  </TooltipContent>
                </Tooltip>
                {/* deactive button */}
              {isPetActiveOrPending  && (
                <Tooltip>
                  <TooltipTrigger asChild>
                    <Button
                      onClick={onDeactivate}
                      className=" text-red-800 hover:bg-red-800 hover:text-white  h-8 w-8 cursor-pointer m-2"
                      aria-label={`Deactivate ${pet.name}`}
                    >
                      <ShieldX size={18} />
                    </Button>
                  </TooltipTrigger>
                  <TooltipContent className="bg-gray-950 text-white border border-red-700">
                    <p>Deactivate {pet.name}</p>
                  </TooltipContent>
                </Tooltip>
              )}
              </div>
              <p className="text-sm text-gray-200 capitalize">
                {pet.specie?.toLowerCase() || "N/A"} • {pet.breedName || "N/A"}
              </p>
              <p className="text-sm text-gray-300">
                Born: {formatDate(pet.birthDate)}
              </p>
              <div className="text-xs text-gray-400 mt-1">Status: <span className={
                                pet.status === PetStatus.ACTIVE ? "text-green-400 font-semibold" :
                                pet.status === PetStatus.PENDING ? "text-yellow-400 font-semibold" :
                                "text-red-400 font-semibold"
                            }>{pet.status}</span>
               {canRequestActivation && (
                    <Tooltip>
                        <TooltipTrigger asChild>
                            <Button
                                onClick={onRequestActivation}
                                size="sm"
                                className="text-cyan-400 bg-transparent hover:bg-cyan-800 hover:text-[#FFECAB] h-7 px-2 py-1 text-xs ml-4 border border-cyan-600 hover:border-cyan-400 mt-2.5"
                                aria-label={`Request activation for ${pet.name}`}
                            >
                                <Send size={14} className="mr-1 sm:mr-1.5" />
                                <span className="hidden sm:inline">Request Activation</span>
                                <span className="sm:hidden">Activate</span>
                            </Button>
                        </TooltipTrigger>
                        <TooltipContent className="bg-gray-950 text-white border border-cyan-700">
                            <p>Request Activation at a Clinic</p>
                        </TooltipContent>
                    </Tooltip>
                )}
                {pet.status === PetStatus.PENDING && pet.pendingActivationClinicId && (
                    <span className="text-xs text-cyan-400 ml-2 italic">(Pending at: {pet.pendingActivationClinicName || `Clinic ID ${pet.pendingActivationClinicId}`})</span>
                )}
             </div>
            </div>
              {/* back button */}
              <Tooltip>
                <TooltipTrigger asChild>
                  <Button
                    onClick={onBack}
                    className="px-4 py-2 bg-[#090D1A] text-[#FFECAB] hover:bg-[#FFECAB] hover:text-[#090D1A] rounded-2xl cursor-pointer"
                    aria-label="Go back to pet list"
                  >
                    <span className='hidden sm:block'>Back</span>
                    <ArrowRight size={16} className="sm:mr-2" />
                  </Button>
                </TooltipTrigger>
                <TooltipContent className="bg-gray-950 text-white border border-cyan-700">
                  <p>Go back to pet list</p>
                </TooltipContent>
              </Tooltip>
          </div>
        </CardHeader>
      </Card>
    );
};

export default PetDetailHeader;