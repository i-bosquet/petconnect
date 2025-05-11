import { JSX } from 'react';
import { PetProfileDto, PetStatus, VetSummaryDto } from '@/types/apiTypes';
import { UserCircle, Edit, Mail, Phone } from 'lucide-react'; 
import { Button } from "@/components/ui/button";
import { Badge } from '@/components/ui/badge';

interface ClinicPetDetailViewProps {
    petProfile: PetProfileDto;
    onEdit?: () => void; 
    currentUserRoles: string[]; 
}

/**
 * ClinicPetDetailView - Displays full details of a pet from the clinic staff's perspective.
 * Includes an "Edit" button if the staff member has permission.
 * @param {ClinicPetDetailViewProps} props - Component props.
 * @returns {JSX.Element} The pet detail view component.
 * @author ibosquet
 */
const ClinicPetDetailView = ({ petProfile, onEdit, currentUserRoles }: ClinicPetDetailViewProps): JSX.Element => {

    const formatDate = (dateString: string | null | undefined): string => {
        if (!dateString) return 'N/A';
        return new Date(dateString).toLocaleDateString('en-GB', { day: '2-digit', month: '2-digit', year: 'numeric' });
    };

    const formatVetList = (vets: VetSummaryDto[] | undefined): string => {
        if (!vets || vets.length === 0) return 'None assigned';
        return vets.map(vet => `${vet.name || ''} ${vet.surname || ''}`.trim()).join(', ');
    };

    const canEdit = onEdit && (currentUserRoles.includes('VET') || currentUserRoles.includes('ADMIN'));

    return (
        <div className="space-y-5">
            <div className="flex flex-col sm:flex-row items-center sm:items-start gap-4 sm:gap-6 pb-4 border-b border-[#FFECAB]/20">
                <div className="flex-shrink-0">
                    <img
                        src={petProfile.image || `/src/assets/images/avatars/pets/${petProfile.specie.toLowerCase()}.png`}
                        alt={`${petProfile.name}'s avatar`}
                        className="w-24 h-24 sm:w-32 sm:h-32 rounded-full object-cover border-4 border-[#FFECAB]/60 bg-gray-700"
                        onError={(e) => (e.currentTarget.src = `/src/assets/images/avatars/pets/${petProfile.specie.toLowerCase()}.png`)}
                    />
                </div>
                <div className="text-center sm:text-left flex-grow">
                    <h3 className="text-2xl font-semibold text-white">{petProfile.name}</h3>
                    <p className="text-cyan-400 capitalize">{petProfile.specie.toLowerCase()}</p>
                    <div className="mt-1 text-sm text-gray-300">
                        <p className="font-medium text-gray-200"><UserCircle size={14} className="text-[#FFECAB] inline mr-1.5 align-middle" /> {petProfile.ownerUsername}</p>
                        <p className="font-medium text-gray-200"><Mail size={14}  className="text-[#FFECAB] inline mr-1.5 align-middle"  /> {petProfile.ownerDetails?.email}</p>
                        <p className="font-medium text-gray-200"><Phone size={14}  className="text-[#FFECAB] inline mr-1.5 align-middle"  /> {petProfile.ownerDetails?.phone}</p>
                    </div>
                </div>
                {canEdit && (
                    <Button onClick={onEdit} size="sm" className="px-5 py-2.5 rounded-lg border border-[#FFECAB]/50 bg-cyan-800 text-[#FFECAB] hover:bg-cyan-600 focus-visible:ring-cyan-500 disabled:opacity-50 cursor-pointer">
                        <Edit size={16} className="mr-2" /> Edit Pet Info
                    </Button>
                )}
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-x-8 gap-y-4 text-sm">
                {/* Left Side */}
                <div className="space-y-3">
                    <div><span className="font-medium text-gray-400 block">Status:</span> <Badge variant={petProfile.status === PetStatus.ACTIVE ? "default" : petProfile.status === PetStatus.PENDING ? "outline" : "destructive"} className={petProfile.status === PetStatus.ACTIVE ? "bg-green-600/70 text-green-50" : petProfile.status === PetStatus.PENDING ? "border-yellow-500 text-yellow-300" : ""}>{petProfile.status}</Badge></div>
                    <div><span className="font-medium text-gray-400 block">Breed:</span> <span className="text-white">{petProfile.breedName || 'N/A'}</span></div>
                    <div><span className="font-medium text-gray-400 block">Gender:</span> <span className="text-white capitalize">{petProfile.gender?.toLowerCase() || 'N/A'}</span></div>
                </div>
                {/* Right side */}
                <div className="space-y-3">
                    <div><span className="font-medium text-gray-400 block">Birth Date:</span> <span className="text-white">{formatDate(petProfile.birthDate)}</span></div>
                    <div><span className="font-medium text-gray-400 block">Color:</span> <span className="text-white">{petProfile.color || 'N/A'}</span></div>
                    <div><span className="font-medium text-gray-400 block">Microchip:</span> <span className="text-white break-all">{petProfile.microchip || 'N/A'}</span></div>
                </div>
            </div>
            
            {/* Vets*/}
            <div className="pt-3 border-t border-[#FFECAB]/20 mt-4">
                <h4 className="font-medium text-gray-300 mb-1">Associated Veterinarian(s):</h4>
                <p className="text-white text-sm">{formatVetList(petProfile.associatedVets)}</p>
            </div>

            {(petProfile.createdAt || petProfile.updatedAt) && (
                <div className="pt-3 border-t border-[#FFECAB]/20 mt-4 text-xs text-gray-500">
                    {petProfile.createdAt && <p>Registered on: {formatDate(petProfile.createdAt)}</p>}
                    {petProfile.updatedAt && <p>Last updated: {formatDate(petProfile.updatedAt)}</p>}
                </div>
            )}
        </div>
    );
};

export default ClinicPetDetailView;