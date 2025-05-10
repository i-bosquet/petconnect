import { JSX } from 'react'; 
import { PetProfileDto } from '@/types/apiTypes';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Stethoscope, Scale, Sigma,  Activity } from 'lucide-react';

interface PetHomeTabProps { pet: PetProfileDto}

/**
 * PetHomeTab - Displays the general overview and key details of a selected pet.
 */
const PetHomeTab = ({ pet }: PetHomeTabProps): JSX.Element => {  

   return (
        <div className="space-y-6">
            <Card className="border-2 border-[#FFECAB] bg-[#070913]/50 shadow-md">
                <CardHeader>
                    <CardTitle className="text-[#FFECAB] text-xl text-center">General Information</CardTitle>
                </CardHeader>
                <CardContent className="space-y-4">
                    {/* Basic Information Section - Shows identification and weight data */}
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                        {/* Left panel - Pet identification details */}
                        <div className="p-4 bg-gray-800 rounded-lg">
                            <h3 className=" text-lg mb-2">Identification Data</h3>
                            <div className="flex items-center">
                                <Sigma size={16} className="mr-2 text-cyan-400 flex-shrink-0" />
                                <span className="font-medium text-gray-300 w-30">Microchip:</span>
                                <span className="text-white break-all">{pet.microchip || 'Not registered'}</span>
                            </div>
                            <div className="flex items-center">
                                <Stethoscope size={16} className="mr-2 text-cyan-400 flex-shrink-0" />
                                <span className="font-medium text-gray-300 w-30">Veterinarians:</span>
                                <ul className="text-white break-all">
                                    {pet.associatedVets.length > 0
                                        ? pet.associatedVets.map((vet, index) => (
                                              <li key={index}>{vet.name} {vet.surname}{index < pet.associatedVets.length - 1 ? ', ' : ''}</li>
                                          ))
                                        : 'Not registered'}
                                </ul>
                            </div>
                        </div>
                        
                        {/* Right panel */}
                        <div className="p-4 bg-gray-800 rounded-lg">
                            <h3 className=" text-lg mb-2">Weight Control</h3>
                            <div className="flex items-center">
                                <Scale size={16} className="mr-2 text-cyan-400 flex-shrink-0" />
                                <span className="font-medium text-gray-300 w-30">Current weight:</span>
                                <span className="text-white break-all">{'Not registered'}</span>
                            </div>
                            <div className="flex items-center">
                                <Activity  size={16} className="mr-2 text-cyan-400 flex-shrink-0" />
                                <span className="font-medium text-gray-300 w-30">Variation:</span>
                                <span className="text-white break-all">{'Not registered'}</span>
                            </div>
                        </div>
                    </div>
                    {/* Urgent Events Section - Shows upcoming appointments and vaccines */}
                    <h3 className="font-medium text-lg mt-6 mb-2 text-center">Urgent Events</h3>
                    <div className="space-y-4">
                        {/* Next appointment card - Shows only if there are upcoming appointments */}
                        <div className="p-4 bg-gray-50 rounded-lg shadow-md flex flex-col sm:flex-row justify-between items-center">
                            <div className="text-center sm:text-left">
                            <h3 className="font-medium text-lg text-[#090D1A]">
                                Next Appointment
                            </h3>
                            {/* IIFE to find and display the nearest upcoming vaccine */}
                                <p className="text-sm text-gray-600">
                                Not registered
                                </p>
                            </div>
                            <button className="mt-2 sm:mt-0 px-4 py-2 bg-cyan-800 text-[#FFECAB] rounded-2xl">
                            Edit Appointment
                            </button>
                        </div>
                        
                        {/* Upcoming vaccine card - Shows only if there are upcoming vaccines */}
                        <div className="p-4 bg-gray-50 rounded-lg shadow-md flex flex-col sm:flex-row justify-between items-center">
                            <div className="text-center sm:text-left">
                            <h3 className="font-medium text-lg text-[#090D1A]">
                                Upcoming Vaccine
                            </h3>
                            {/* IIFE to find and display the nearest upcoming vaccine */}
                                <p className="text-sm text-gray-600">
                                Not registered
                                </p>
                            </div>
                            <button className="mt-2 sm:mt-0 px-4 py-2 bg-red-800 text-[#FFECAB] rounded-2xl">
                            Schedule Appointment
                            </button>
                        </div>
                    </div>
                   
                </CardContent>
            </Card>
        </div>
    );
};
export default PetHomeTab;