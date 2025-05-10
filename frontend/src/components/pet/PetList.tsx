import { JSX } from 'react';
import { PetProfileDto } from '../../types/apiTypes';
import PetCard from './PetCard'; 
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"; 
import { Dog, Cat, PlusCircle } from 'lucide-react'; 


interface PetListProps {
    pets: PetProfileDto[]; 
    onSelectPet: (petId: number | string) => void; 
    onAddPet: () => void; 
}

/**
 * PetList - Displays a grid of PetCard components for the owner's pets.
 * Includes a button to add a new pet.
 *
 * @param {PetListProps} props - Component properties.
 * @returns {JSX.Element} A Card component containing a grid of pet cards and an add button.
 */
const PetList = ({ pets, onSelectPet, onAddPet }: PetListProps): JSX.Element => {

    return (
        <Card className="border-2 border-[#FFECAB]"> 
            <CardHeader>
                    <CardTitle className="flex items-center justify-between px-6">
                        <div className="flex justify-between gap-2">
                        <Dog size={20} /> 
                        <span>My Pets</span>
                        <Cat size={20} /> 
                        </div>
                         <button
                        onClick={onAddPet}
                        className="flex items-center gap-1 sm:gap-2 px-3 py-1.5 sm:px-4 sm:py-2 bg-cyan-700 text-[#FFECAB] rounded-lg text-sm font-medium hover:bg-cyan-600 transition-colors"
                    >
                        <PlusCircle size={18} />
                        <span className="hidden sm:inline">Add Pet</span>
                        <span className="sm:hidden"></span> 
                    </button>
                    </CardTitle>
                   
            </CardHeader>
            {/* Grid of pet cards */}
            <CardContent>
                {pets.length > 0 ? (
                    <div className="flex flex-wrap justify-evenly gap-3"> 
                        {pets.map((pet) => (
                            <PetCard
                                key={pet.id}
                                pet={pet}
                                onSelect={() => onSelectPet(pet.id)} 
                            />
                        ))}
                    </div>
                ) : (
                    <div className="text-center py-10 px-6 bg-[#070913]/40 rounded-lg border border-dashed border-gray-600">
                         <p className="text-gray-400">You haven't added any pets yet.</p>
                         <button
                            onClick={onAddPet}
                            className="mt-4 inline-flex items-center gap-2 px-4 py-2 bg-cyan-700 text-[#FFECAB] rounded-lg text-sm font-medium hover:bg-cyan-600 transition-colors"
                        >
                             <PlusCircle size={18} />
                            Add Your First Pet
                         </button>
                     </div>
                )}
            </CardContent>
        </Card>
    );
};

export default PetList;