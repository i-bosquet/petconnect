import { JSX } from 'react';
import { PetProfileDto } from '@/types/apiTypes';
import PetCard from './PetCard'; 
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"; 
import { Checkbox } from "@/components/ui/checkbox";
import { Label  } from "@/components/ui/label";
import { Button } from "@/components/ui/button";
import { Dog, Cat, PlusCircle } from 'lucide-react'; 


interface PetListProps {
    pets: PetProfileDto[]; 
    onSelectPet: (petId: number | string) => void; 
    onAddPet: () => void; 
    showInactive: boolean; 
     onToggleShowInactive: (checked: boolean) => void;
}

/**
 * PetList - Displays a grid of PetCard components for the owner's pets.
 * Includes a button to add a new pet.
 *
 * @param {PetListProps} props - Component properties.
 * @returns {JSX.Element} A Card component containing a grid of pet cards and an add button.
 */
const PetList = ({ pets, onSelectPet, onAddPet, showInactive, onToggleShowInactive}: PetListProps): JSX.Element => {

    return (
      <Card className="border-2 border-[#FFECAB]/30 bg-[#0c1225]/70 shadow-xl">
        <CardHeader>
          <CardTitle className="flex items-center justify-between px-6">
            <div className="flex justify-between gap-2">
              <Dog size={20} />
              <span>My Pets</span>
              <Cat size={20} />
            </div>
            <div className="flex items-center gap-4">
              {/* Button to add a new pet */}
              <Button
                onClick={onAddPet}
                className="flex items-center gap-1 sm:gap-2 px-3 py-1.5 sm:px-4 sm:py-2 bg-cyan-700 text-[#FFECAB] rounded-lg text-sm font-medium hover:bg-cyan-600 transition-colors cursor-pointer"
              >
                <PlusCircle size={18} />
                <span className="hidden sm:inline">Add Pet</span>
                <span className="sm:hidden"></span>
              </Button>
            </div>
          </CardTitle>
        </CardHeader>
        {/* Grid of pet cards */}
        <CardContent>
          {pets.length > 0 ? (
            <div className="flex flex-wrap justify-center gap-8">
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
              <p className="text-gray-400">
                {showInactive ? "No pets found (including inactive)." : "You haven't added any active or pending pets yet."}
                </p>
            </div>
          )}

          {/* Checkbox to show/hide inactive pets */}
              <div className="flex items-center space-x-2 justify-end mt-2">
                <Checkbox
                  id="show-inactive-pets"
                  checked={showInactive}
                  onCheckedChange={onToggleShowInactive}
                  className="data-[state=checked]:bg-cyan-600 data-[state=checked]:text-white border-gray-500 cursor-pointer"
                />
                <Label
                  htmlFor="show-inactive-pets"
                  className="text-sm font-medium text-gray-300 cursor-pointer"
                >
                  Show Inactive
                </Label>
              </div>
        </CardContent>
      </Card>
    );
};

export default PetList;