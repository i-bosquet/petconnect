import { useState, useEffect, useRef, JSX, ChangeEvent, FormEvent, useCallback } from 'react';
import Modal from '../../common/Modal';
import { Upload, Calendar, PawPrint, VenetianMask, Palette, Sigma, Loader2 } from 'lucide-react';
import { Specie, Gender } from '../../../types/enumTypes';
import { PetRegistrationData, BreedDto } from '../../../types/apiTypes';
import { registerPet, getBreedsBySpecie } from '../../../services/petService';
import { BACKEND_BASE_URL } from '../../../config';

interface AddPetModalProps {
    onClose: () => void;
    onPetAdded: () => void;
}

const MIXED_OTHER_VALUE = 'MIXED_OTHER';

const initialPetData: Partial<PetRegistrationData> = {
    name: '',
    specie: Specie.DOG,
    birthDate: '',
    breedId: MIXED_OTHER_VALUE, // Use constant for default value
    image: null, // Image path/URL is set by backend or separate upload
    color: '',
    gender: undefined,
    microchip: ''
};

const defaultImages: Record<Specie, string> = {
    [Specie.DOG]: '/images/avatars/pets/dog.png',
    [Specie.CAT]: '/images/avatars/pets/cat.png',
    [Specie.RABBIT]: '/images/avatars/pets/rabbit.png',
    [Specie.FERRET]: '/images/avatars/pets/ferret.png'
};

/**
 * AddPetModal - Modal component for registering a new pet.
 * Contains the form to input pet details and calls the API on submit.
 * Handles image preview and prepares data for multipart submission if an image is selected.
 *
 * @param {AddPetModalProps} props - Component props.
 * @returns {JSX.Element} The modal component for adding a pet.
 */
const AddPetModal = ({ onClose, onPetAdded }: AddPetModalProps): JSX.Element => {
    const [petData, setPetData] = useState<Partial<PetRegistrationData>>(initialPetData);
    const [breeds, setBreeds] = useState<BreedDto[]>([]);
    const [breedsLoading, setBreedsLoading] = useState<boolean>(false);
    const [previewImage, setPreviewImage] = useState<string | null>(`${BACKEND_BASE_URL}${defaultImages[initialPetData.specie!]}`);
    const [selectedFile, setSelectedFile] = useState<File | null>(null); // State for the actual file object
    const [error, setError] = useState<string>('');
    const [isLoading, setIsLoading] = useState<boolean>(false);
    const fileInputRef = useRef<HTMLInputElement>(null);

    const storedUserJson = sessionStorage.getItem('user') || localStorage.getItem('user');
    const token = storedUserJson ? JSON.parse(storedUserJson).jwt : null;

    const fetchBreeds = useCallback(async (specie: Specie) => {
        if (!token || !specie) return;
        setBreedsLoading(true);
        setError('');
        setBreeds([]);
        try {
            const fetchedBreeds = await getBreedsBySpecie(token, specie);
            const mixedOption: BreedDto = { id: MIXED_OTHER_VALUE, name: 'Mixed / Other', imageUrl: null }; 
            setBreeds([mixedOption, ...fetchedBreeds]);
            setPetData(prev => ({ ...prev, breedId: MIXED_OTHER_VALUE }));
        } catch (err) {
            console.error("Failed to fetch breeds:", err);
            setError("Could not load breeds for the selected species.");
        } finally {
            setBreedsLoading(false);
        }
    }, [token]);

    useEffect(() => {
        if (initialPetData.specie) {
             fetchBreeds(initialPetData.specie);
        }
    }, [fetchBreeds]);


    const handleChange = (e: ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
        const { name, value } = e.target;

        setPetData(prevData => {
            const newData = { ...prevData, [name]: value };

            if (name === 'specie') {
                 const newSpecie = value as Specie;
                 fetchBreeds(newSpecie);
                 // Reset preview only if user hasn't uploaded one
                 if (!selectedFile) { 
                    setPreviewImage(`${BACKEND_BASE_URL}${defaultImages[newSpecie]}`);
                 }
            }

            if (name === 'breedId') {
                 // Update preview only if user hasn't uploaded one
                 if (!selectedFile) {
                     const selectedBreed = breeds.find(b => b.id?.toString() === value);
                     if (selectedBreed && selectedBreed.id !== MIXED_OTHER_VALUE && selectedBreed.imageUrl) {
                          const breedImageUrl = `${BACKEND_BASE_URL}/${selectedBreed.imageUrl.replace(/^\//, '')}`;
                          setPreviewImage(breedImageUrl);
                     } else {
                         setPreviewImage(`${BACKEND_BASE_URL}${defaultImages[newData.specie!]}`);
                     }
                 }
            }
            return newData;
        });
    };

     const handleFileChange = (e: ChangeEvent<HTMLInputElement>) => {
        if (e.target.files && e.target.files[0]) {
            const file = e.target.files[0];
            setSelectedFile(file); // Store the File object
            const reader = new FileReader();
            reader.onloadend = () => {
                setPreviewImage(reader.result as string); 
            };
            reader.readAsDataURL(file);
        }
    };

    const triggerFileInput = () => { fileInputRef.current?.click(); };

    const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        setError('');
        if (!token) { setError("Authentication error."); return; }
        if (!petData.name || !petData.specie || !petData.birthDate) {
            setError("Pet Name, Species, and Birth Date are required."); return;
        }
        setIsLoading(true);

        // Prepare data DTO *without* the image field
        const dataToSend: PetRegistrationData = {
            name: petData.name!,
            specie: petData.specie!,
            birthDate: petData.birthDate!,
            breedId: (petData.breedId && petData.breedId !== MIXED_OTHER_VALUE) ? Number(petData.breedId) : null,
            image: null, // Image path will be set by backend if file is uploaded successfully
            color: petData.color || null,
            gender: petData.gender || null,
            microchip: petData.microchip || null,
        };

        try {
            // Pass the DTO and the File object (or null) to the service
            await registerPet(token, dataToSend, selectedFile);
            onPetAdded();
            onClose();
        } catch (err) {
            console.error("Failed to add pet:", err);
            setError(err instanceof Error ? err.message : 'Could not add pet.');
            setIsLoading(false);
        }
    };

    return (
        <Modal title="Add New Pet" onClose={onClose} maxWidth="max-w-3xl">
            <form onSubmit={handleSubmit} className="space-y-4 md:space-y-5">
                {/* Image Preview and Upload */}
                 <div className="flex flex-col items-center">
                     <div className="relative mb-2">
                        <img src={previewImage || ''} alt={petData.name || 'Pet preview'}
                            className="w-28 h-28 sm:w-32 sm:h-32 rounded-full object-cover border-4 border-[#FFECAB]/50 bg-gray-700"
                            onError={(e) => { if (petData.specie) e.currentTarget.src = `${BACKEND_BASE_URL}${defaultImages[petData.specie]}`; }} />
                         <button type="button" onClick={triggerFileInput}
                            className="absolute bottom-0 right-0 p-2 bg-cyan-700 text-[#FFECAB] rounded-full hover:bg-cyan-600 transition-colors ring-2 ring-[#0c1225]"
                            aria-label="Upload pet image" title="Upload image">
                             <Upload className="w-4 h-4 sm:w-5 sm:h-5" />
                         </button>
                     </div>
                     <input ref={fileInputRef} id="addPetImage" type="file" accept="image/*" onChange={handleFileChange} className="hidden" />
                 </div>

                {/* Name and Species */}
                 <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                     {/* Pet Name */}
                    <div className="space-y-1.5">
                        <label htmlFor="petName" className="block text-sm font-medium text-gray-300">Name *</label>
                        <div className="relative">
                             <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none"><PawPrint size={16} className="text-gray-400" /></div>
                             <input id="petName" name="name" type="text" required value={petData.name} onChange={handleChange} disabled={isLoading} placeholder="Your pet's name"
                                   className="block w-full pl-10 pr-3 py-2.5 border border-gray-700 rounded-xl shadow-sm placeholder:text-gray-500 focus:outline-none focus:ring-1 focus:ring-cyan-600 focus:border-cyan-600 text-white bg-[#070913] disabled:opacity-50"/>
                        </div>
                    </div>
                    {/* Species Selection */}
                     <div className="space-y-1.5">
                          <label className="block text-sm font-medium text-gray-300 mb-1">Species *</label>
                          <div className="flex flex-wrap gap-x-4 gap-y-2 pt-1">
                              {(Object.keys(Specie) as Array<keyof typeof Specie>).map((key) => (
                                 <label key={key} className="flex items-center cursor-pointer text-sm">
                                     <input type="radio" name="specie" value={Specie[key]} checked={petData.specie === Specie[key]} onChange={handleChange} disabled={isLoading} required className="h-4 w-4 mr-1.5 border-gray-500 text-cyan-600 focus:ring-cyan-500 focus:ring-offset-gray-800 bg-gray-700"/>
                                     {key}
                                 </label>
                              ))}
                          </div>
                      </div>
                 </div>

                 {/* Breed and Birth Date */}
                 <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                     {/* Breed Selection */}
                     <div className="space-y-1.5">
                         <label htmlFor="breedId" className="block text-sm font-medium text-gray-300">Breed</label>
                         <div className="relative">
                            <select id="breedId" name="breedId" value={petData.breedId ?? MIXED_OTHER_VALUE} onChange={handleChange} disabled={isLoading || breedsLoading}
                                className="block appearance-none w-full pl-3 pr-10 py-2.5 border border-gray-700 rounded-xl shadow-sm focus:outline-none focus:ring-1 focus:ring-cyan-600 focus:border-cyan-600 text-white bg-[#070913] disabled:opacity-50" >
                                <option value={MIXED_OTHER_VALUE} >Mixed / Other</option> {/* Explicit default/mixed option */}
                                {breeds.filter(b => b.id !== MIXED_OTHER_VALUE).map(breed => ( // Filter spi our placeholder
                                     <option key={breed.id.toString()} value={breed.id.toString()}>{breed.name}</option>
                                 ))}
                            </select>
                            <div className="absolute inset-y-0 right-0 pr-3 flex items-center pointer-events-none">
                                {breedsLoading ? <Loader2 size={16} className="text-gray-400 animate-spin"/> : <VenetianMask size={16} className="text-gray-400"/>}
                            </div>
                         </div>
                     </div>
                     {/* Birth Date */}
                     <div className="space-y-1.5">
                          <label htmlFor="birthDate" className="block text-sm font-medium text-gray-300">Birth Date *</label>
                          <div className="relative">
                              <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none"><Calendar size={16} className="text-gray-400" /></div>
                              <input id="birthDate" name="birthDate" type="date" required value={petData.birthDate} onChange={handleChange} disabled={isLoading}
                                     className="block w-full pl-10 pr-3 py-2.5 border border-gray-700 rounded-xl shadow-sm focus:outline-none focus:ring-1 focus:ring-cyan-600 focus:border-cyan-600 text-white bg-[#070913] disabled:opacity-50"/>
                          </div>
                     </div>
                 </div>

                 {/* Gender and Color */}
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                    {/* Gender Selection */}
                    <div className="space-y-1.5">
                       <label className="block text-sm font-medium text-gray-300 mb-1">Gender (Optional)</label>
                        <div className="flex gap-x-6 gap-y-2 pt-1">
                            {(Object.keys(Gender) as Array<keyof typeof Gender>).map(key => (
                                <label key={key} className="flex items-center cursor-pointer text-sm">
                                    <input type="radio" name="gender" value={Gender[key]} checked={petData.gender === Gender[key]} onChange={handleChange} disabled={isLoading}
                                           className="h-4 w-4 mr-1.5 border-gray-500 text-cyan-600 focus:ring-cyan-500 focus:ring-offset-gray-800 bg-gray-700"/> {key}
                                </label>
                            ))}
                        </div>
                    </div>
                     {/* Color Input */}
                    <div className="space-y-1.5">
                         <label htmlFor="color" className="block text-sm font-medium text-gray-300">Color (Optional)</label>
                          <div className="relative">
                             <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none"><Palette size={16} className="text-gray-400" /></div>
                            <input id="color" name="color" type="text" value={petData.color ?? ''} onChange={handleChange} disabled={isLoading} placeholder="e.g., Brown, Black & White"
                                className="block w-full pl-10 pr-3 py-2.5 border border-gray-700 rounded-xl shadow-sm placeholder:text-gray-500 focus:outline-none focus:ring-1 focus:ring-cyan-600 focus:border-cyan-600 text-white bg-[#070913] disabled:opacity-50"/>
                         </div>
                    </div>
                </div>

                {/* Microchip Input */}
                <div className="space-y-1.5">
                    <label htmlFor="microchip" className="block text-sm font-medium text-gray-300">Microchip (Optional)</label>
                     <div className="relative">
                        <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none"><Sigma size={16} className="text-gray-400" /></div>
                        <input id="microchip" name="microchip" type="text" maxLength={50}
                           value={petData.microchip ?? ''} onChange={handleChange} disabled={isLoading} placeholder="Pet's microchip number"
                           className="block w-full pl-10 pr-3 py-2.5 border border-gray-700 rounded-xl shadow-sm placeholder:text-gray-500 focus:outline-none focus:ring-1 focus:ring-cyan-600 focus:border-cyan-600 text-white bg-[#070913] disabled:opacity-50"/>
                    </div>
                </div>

                 {/* Error Display */}
                 {error && ( <p className="text-sm text-red-400 text-center">{error}</p> )}

                {/* Action Buttons */}
                <div className="flex justify-end gap-4 pt-4">
                    <button type="button" onClick={onClose} disabled={isLoading}
                            className="px-5 py-2.5 rounded-lg border border-[#FFECAB]/50 text-[#FFECAB] hover:bg-[#FFECAB]/10 transition-colors disabled:opacity-50">
                        Cancel
                    </button>
                    <button type="submit" disabled={isLoading}
                            className="px-5 py-2.5 rounded-lg bg-cyan-700 text-white hover:bg-cyan-600 transition-colors disabled:opacity-50 flex items-center">
                        {isLoading && <Loader2 className="animate-spin h-4 w-4 mr-2" />}
                        {isLoading ? 'Adding Pet...' : 'Add Pet'}
                    </button>
                </div>
            </form>
        </Modal>
    );
};

export default AddPetModal;