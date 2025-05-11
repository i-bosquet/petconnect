import { useState, useEffect, FormEvent, ChangeEvent, JSX, useCallback } from 'react';
import { PetProfileDto, BreedDto, PetClinicUpdatePayload, Specie, Gender } from '@/types/apiTypes';
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Loader2, SaveAll, CircleX, Palette, Sigma, CalendarDays } from 'lucide-react';
import { updatePetByClinicStaff, getBreedsBySpecie } from '@/services/petService';
import { useAuth } from '@/hooks/useAuth';

interface ClinicPetEditFormProps {
    petInitialData: PetProfileDto;
    onSaveSuccess: (updatedPet: PetProfileDto) => void;
    onCancel: () => void;
}

const MIXED_OTHER_BREED_ID_CLINIC_EDIT = "MIXED_OTHER_CLINIC_EDIT";

/**
 * ClinicPetEditForm - Form for clinic staff to edit specific details of a pet.
 * Restricted fields like name and image are not editable here.
 * @param {ClinicPetEditFormProps} props - Component props.
 * @returns {JSX.Element} The pet edit form for clinic staff.
 * @author ibosquet
 */
const ClinicPetEditForm = ({ petInitialData, onSaveSuccess, onCancel }: ClinicPetEditFormProps): JSX.Element => {
    const { token } = useAuth();
    const [formData, setFormData] = useState<Partial<PetClinicUpdatePayload>>({});
    const [breeds, setBreeds] = useState<BreedDto[]>([]);
    const [breedsLoading, setBreedsLoading] = useState<boolean>(false);
    const [isSaving, setIsSaving] = useState<boolean>(false);
    const [error, setError] = useState<string>('');

    const fetchBreeds = useCallback(async (specie: Specie) => {
        if (!token) return;
        setBreedsLoading(true);
        try {
            const fetchedBreeds = await getBreedsBySpecie(token, specie);
            const mixedOption: BreedDto = { id: MIXED_OTHER_BREED_ID_CLINIC_EDIT, name: "Mixed / Other", imageUrl: null };
            setBreeds([mixedOption, ...fetchedBreeds.filter(b => b.id.toString() !== MIXED_OTHER_BREED_ID_CLINIC_EDIT)]);
            if (!petInitialData.breedId || !fetchedBreeds.some(b => b.id === petInitialData.breedId)) {
                 setFormData(prev => ({ ...prev, breedId: MIXED_OTHER_BREED_ID_CLINIC_EDIT }));
            } else {
                 setFormData(prev => ({ ...prev, breedId: petInitialData.breedId }));
            }
        } catch (err) {
            console.error("ClinicPetEditForm: Failed to fetch breeds in edit form", err); 
            setError(err instanceof Error ? err.message : "Could not load breeds for this species.");
        } finally {
            setBreedsLoading(false);
        }
    }, [token, petInitialData.breedId]);

    useEffect(() => {
        setFormData({
            color: petInitialData.color || '',
            gender: petInitialData.gender || undefined, 
            birthDate: petInitialData.birthDate || '',
            microchip: petInitialData.microchip || '',
             breedId: petInitialData.breedId ? Number(petInitialData.breedId) : undefined,
        });
        setError('');
        if (petInitialData.specie && token) {
            fetchBreeds(petInitialData.specie);
        }
    }, [petInitialData, token, fetchBreeds]); 

    const handleChange = (e: ChangeEvent<HTMLInputElement>) => {
        const { name, value } = e.target;
        setFormData(prev => ({ ...prev, [name]: value === "" ? null : value }));
    };

    const handleSelectChange = (name: keyof PetClinicUpdatePayload, value: string) => {
        if (name === 'gender') {
            setFormData(prev => ({ ...prev, [name]: value === "placeholder_gender_deselect" ? undefined : (value as Gender) }));
        } else if (name === 'breedId') {
            setFormData(prev => ({ ...prev, [name]: value === MIXED_OTHER_BREED_ID_CLINIC_EDIT ? null : Number(value) }));
        }
    };

    const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        if (!token) { setError("Authentication required."); return; }

        setIsSaving(true);
        setError('');
        try {
            const payload: PetClinicUpdatePayload = {
                color: formData.color || null,
                gender: formData.gender || null,
                birthDate: formData.birthDate || null,
                microchip: formData.microchip || null,
                breedId: formData.breedId ? Number(formData.breedId) : null,
            };
            const updatedPet = await updatePetByClinicStaff(token, petInitialData.id, payload);
            onSaveSuccess(updatedPet);
        } catch (err) {
            setError(err instanceof Error ? err.message : "Failed to update pet information.");
        } finally {
            setIsSaving(false);
        }
    };

    return (
        <form onSubmit={handleSubmit} className="space-y-5">
            {error && <div className="p-3 bg-red-900/30 text-red-300 rounded-lg text-sm text-center">{error}</div>}

            <div className="p-4 bg-gray-800/50 rounded-lg border border-gray-700">
                <h4 className="text-md font-semibold text-[#FFECAB] mb-2">Pet Identity</h4>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-sm">
                    <div><span className="font-medium text-gray-400">Name:</span> <span className="text-white">{petInitialData.name}</span></div>
                    <div><span className="font-medium text-gray-400">Species:</span> <span className="text-white capitalize">{petInitialData.specie.toLowerCase()}</span></div>
                </div>
                <div className="mt-2">
                    <span className="font-medium text-gray-400 text-sm">Current Image:</span>
                    <img src={petInitialData.image} alt={petInitialData.name} className="mt-1 w-20 h-20 rounded-md object-cover bg-gray-700" />
                </div>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-x-6 gap-y-4">
                {/* Color */}
                <div className="space-y-1.5">
                    <Label htmlFor="clinicEditPetColor" className="text-gray-300">Color</Label>
                    <div className="relative"><Palette size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
                    <Input id="clinicEditPetColor" name="color" value={formData.color || ''} onChange={handleChange} disabled={isSaving}
                           className="pl-10 bg-[#070913] border-gray-700"/></div>
                </div>
                {/* Gender */}
                <div className="space-y-1.5">
                    <Label htmlFor="clinicEditPetGender" className="text-gray-300">Gender</Label>
                    <Select name="gender" value={formData.gender || ""} onValueChange={(val) => handleSelectChange('gender', val)} disabled={isSaving}>
                        <SelectTrigger className="bg-[#070913] border-gray-700"><SelectValue placeholder="Select gender" /></SelectTrigger>
                        <SelectContent className="bg-[#0c1225] border-gray-700 text-white">
                            <SelectItem value="placeholder_gender_deselect" className="text-gray-400 italic">Not Specified</SelectItem>
                            {Object.values(Gender).map(g => <SelectItem key={g} value={g}>{g.charAt(0) + g.slice(1).toLowerCase()}</SelectItem>)}
                        </SelectContent>
                    </Select>
                </div>
                {/* Birth Date */}
                <div className="space-y-1.5">
                    <Label htmlFor="clinicEditPetBirthDate" className="text-gray-300">Birth Date</Label>
                     <div className="relative"><CalendarDays size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
                    <Input id="clinicEditPetBirthDate" name="birthDate" type="date" value={formData.birthDate || ''} onChange={handleChange} disabled={isSaving}
                           className="pl-10 bg-[#070913] border-gray-700"/></div>
                </div>
                {/* Microchip */}
                <div className="space-y-1.5">
                    <Label htmlFor="clinicEditPetMicrochip" className="text-gray-300">Microchip</Label>
                     <div className="relative"><Sigma size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
                    <Input id="clinicEditPetMicrochip" name="microchip" value={formData.microchip || ''} onChange={handleChange} disabled={isSaving}
                           className="pl-10 bg-[#070913] border-gray-700"/></div>
                </div>
                {/* Breed */}
                <div className="space-y-1.5 sm:col-span-2"> {/* Breed puede ocupar más espacio si quieres */}
                    <Label htmlFor="clinicEditPetBreedId" className="text-gray-300">Breed</Label>
                    <Select name="breedId" value={formData.breedId?.toString() || MIXED_OTHER_BREED_ID_CLINIC_EDIT} onValueChange={(val) => handleSelectChange('breedId', val)} disabled={isSaving || breedsLoading}>
                        <SelectTrigger className="bg-[#070913] border-gray-700"><SelectValue placeholder="Select breed" /></SelectTrigger>
                        <SelectContent className="bg-[#0c1225] border-gray-700 text-white">
                            {breedsLoading ? <SelectItem value="loading" disabled>Loading breeds...</SelectItem> :
                                breeds.map(breed => <SelectItem key={breed.id.toString()} value={breed.id.toString()}>{breed.name}</SelectItem>)
                            }
                        </SelectContent>
                    </Select>
                </div>
            </div>

            <div className="flex justify-end gap-4 pt-5 border-t border-[#FFECAB]/20 mt-6">
                <Button type="button" onClick={onCancel} disabled={isSaving} className="px-5 py-2.5 rounded-lg border border-[#FFECAB]/50 text-[#FFECAB] hover:bg-red-800 hover:text-[#FFECAB] focus-visible:ring-red-500 disabled:opacity-50 cursor-pointer">
    <CircleX size={16} className="mr-2"  />Cancel
                </Button>
                <Button type="submit" disabled={isSaving} className="px-5 py-2.5 rounded-lg border border-[#FFECAB]/50 bg-cyan-800 text-[#FFECAB] hover:bg-cyan-600 focus-visible:ring-cyan-500 disabled:opacity-50 cursor-pointer">
                    {isSaving && <Loader2 className="animate-spin h-4 w-4 mr-2" />}<SaveAll size={16} className="mr-2"/>Save Changes
                </Button>
            </div>
        </form>
    );
};

export default ClinicPetEditForm;