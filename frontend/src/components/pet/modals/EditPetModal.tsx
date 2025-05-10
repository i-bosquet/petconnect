import {
  useState,
  useEffect,
  FormEvent,
  ChangeEvent,
  JSX,
  useRef,
  useCallback,
} from "react";
import Modal from "@/components/common/Modal";
import {
  PetProfileDto,
  PetOwnerUpdatePayload,
  BreedDto,
} from "@/types/apiTypes"; // Usar PetOwnerUpdatePayload
import { Specie, Gender } from "@/types/enumTypes";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Loader2,
  Upload,
  Calendar,
  PawPrint,
  Palette,
  Sigma,
  SaveAll,
  CircleX,
} from "lucide-react";
import { updatePetByOwner, getBreedsBySpecie } from "@/services/petService";
import { useAuth } from "@/hooks/useAuth";
import { BACKEND_BASE_URL } from "@/config";
import { TooltipTrigger, TooltipContent, Tooltip } from "@/components/ui/tooltip";

interface EditPetModalProps {
  petInitialData: PetProfileDto;
  onClose: () => void;
  onPetUpdated: () => void;
}

interface PetEditFormData {
  name: string;
  color: string | null;
  gender: Gender | null | undefined;
  birthDate: string | null;
  microchip: string | null;
  breedId: number | string | null;
}

const MIXED_OTHER_BREED_ID = "MIXED_OTHER";

const defaultImages: Record<Specie, string> = {
  [Specie.DOG]: "/images/avatars/pets/dog.png",
  [Specie.CAT]: "/images/avatars/pets/cat.png",
  [Specie.RABBIT]: "/images/avatars/pets/rabbit.png",
  [Specie.FERRET]: "/images/avatars/pets/ferret.png",
};

/**
 * EditPetModal - Modal for an Owner to update their pet's information.
 */
const EditPetModal = ({
  petInitialData,
  onClose,
  onPetUpdated,
}: EditPetModalProps): JSX.Element | null => {
  const { token } = useAuth();
  const [formData, setFormData] = useState<PetEditFormData>({
    name: petInitialData.name || "",
    color: petInitialData.color || null,
    gender: petInitialData.gender || undefined,
    birthDate: petInitialData.birthDate || null,
    microchip: petInitialData.microchip || null,
    breedId: petInitialData.breedId?.toString() || MIXED_OTHER_BREED_ID,
  });
  const [selectedImageFile, setSelectedImageFile] = useState<File | null>(null);
  const [previewImage, setPreviewImage] = useState<string | null>(
    petInitialData.image
  );
  const imageFileRef = useRef<HTMLInputElement>(null);

  const displayImage =
    previewImage ||
    petInitialData.image ||
    `${BACKEND_BASE_URL}${defaultImages[petInitialData.specie]}` ||
    "/src/assets/images/avatars/pets/default_pet.png";

  const [breeds, setBreeds] = useState<BreedDto[]>([]);
  const [breedsLoading, setBreedsLoading] = useState<boolean>(false);

  const [isSaving, setIsSaving] = useState<boolean>(false);
  const [error, setError] = useState<string>("");

  const fetchBreeds = useCallback(
    async (specie: Specie) => {
      if (!token || !specie) {
            setBreeds([]); 
            return;
        }
      setBreedsLoading(true);
      setError('');
      try {
        const fetchedBreeds = await getBreedsBySpecie(token, specie);
        const mixedOption: BreedDto = { id: MIXED_OTHER_BREED_ID, name: 'Mixed / Other', imageUrl: null };
        
        setBreeds([
          mixedOption,
          ...fetchedBreeds.filter(
            (b) => b.id.toString() !== MIXED_OTHER_BREED_ID
          ),
        ]);
      } catch (err) {
        console.error("EditPetModal: Failed to fetch breeds", err);
        setError("Could not load breeds for this species.");
      } finally {
        setBreedsLoading(false);
      }
    },
    [token]
  );

  useEffect(() => {
    if (petInitialData.specie) {
      fetchBreeds(petInitialData.specie);
    }
  }, [petInitialData.specie, fetchBreeds]);

  useEffect(() => {
    setFormData({
      name: petInitialData.name || "",
      color: petInitialData.color || null,
      gender: petInitialData.gender || undefined,
      birthDate: petInitialData.birthDate || null,
      microchip: petInitialData.microchip || null,
      breedId: petInitialData.breedId?.toString() || MIXED_OTHER_BREED_ID,
    });
    setPreviewImage(petInitialData.image);
    setSelectedImageFile(null);
    setError("");
  }, [petInitialData]);

  const handleChange = (
    e: ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>
  ) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value === "" ? null : value }));
    setError("");
  };

  const handleBreedChange = (value: string) => {
    setFormData((prev) => ({ ...prev, breedId: value }));
    setError("");
  };

  const handleGenderChange = (value: string) => {
    setFormData((prev) => ({
      ...prev,
      gender: (value as Gender) || undefined,
    }));
    setError("");
  };

  const handleFileChange = (e: ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      const file = e.target.files[0];
      const allowedTypes = ["image/jpeg", "image/png", "image/gif"];
      const maxSizeMB = 1;
      const maxSize = maxSizeMB * 1024 * 1024;

      if (!allowedTypes.includes(file.type)) {
        setError(`Invalid file type. Allowed: ${allowedTypes.join(", ")}.`);
        return;
      }
      if (file.size > maxSize) {
        setError(`File too large. Max size: ${maxSizeMB}MB.`);
        return;
      }
      setSelectedImageFile(file);
      const reader = new FileReader();
      reader.onloadend = () => {
        setPreviewImage(reader.result as string);
      };
      reader.readAsDataURL(file);
      setError("");
    } else {
      setSelectedImageFile(null);
    }
  };
  const triggerImageInput = () => imageFileRef.current?.click();

  const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (!token) {
      setError("Authentication required.");
      return;
    }
    if (!formData.name) {
      setError("Pet name is required.");
      return;
    }

    setIsSaving(true);
    setError("");
    try {
      const payload: PetOwnerUpdatePayload = {
        name: formData.name,
        color: formData.color,
        gender: formData.gender,
        birthDate: formData.birthDate,
        microchip: formData.microchip,
        breedId:
          formData.breedId && formData.breedId !== MIXED_OTHER_BREED_ID
            ? Number(formData.breedId)
            : null,
      };
      await updatePetByOwner(
        token,
        petInitialData.id,
        payload,
        selectedImageFile
      );
      onPetUpdated();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to update pet.");
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <Modal
      title={`Edit Pet: ${petInitialData.name}`}
      onClose={onClose}
      maxWidth="max-w-3xl"
    >
      <form onSubmit={handleSubmit} className="space-y-4 md:space-y-5">
        {error && (
          <div className="p-3 bg-red-900/30 text-red-300 rounded-lg text-sm text-center">
            {error}
          </div>
        )}

        {/* Image Preview and Upload */}
        <div className="flex flex-col items-center">
          <div className="relative mb-2">
            <img
              src={displayImage}
              alt={formData.name || "Pet preview"}
              className="w-28 h-28 sm:w-32 sm:h-32 rounded-full object-cover border-4 border-[#FFECAB]/50 bg-gray-700"
              onError={(e) => {
                e.currentTarget.src = `${BACKEND_BASE_URL}${
                  defaultImages[petInitialData.specie]
                }`;
              }}
            />
             <Tooltip>
                <TooltipTrigger asChild>
                  <Button
                    type="button"
                    size="icon"   
                    onClick={triggerImageInput}
                    className="absolute bottom-0 right-0 p-2 bg-cyan-800 text-[#FFECAB] rounded-full hover:bg-cyan-600 transition-colors ring-2 ring-gray-400 h-9 w-9 sm:h-10 sm:w-10 cursor-pointer" // Ajusta clases según necesites
                    aria-label="Change pet image" 
                  >
                    <Upload className="w-4 h-4 sm:w-5 sm:h-5" />
                  </Button>
                </TooltipTrigger>
                <TooltipContent className="bg-gray-950 text-white border border-cyan-700">
                  <p>Change Pet Image</p>
                </TooltipContent>
             </Tooltip>
          </div>
          <input
            ref={imageFileRef}
            id="editPetImage"
            type="file"
            accept="image/*"
            onChange={handleFileChange}
            className="hidden"
            disabled={isSaving}
          />
          {selectedImageFile && (
            <p className="text-xs text-gray-400 mt-1">
              {selectedImageFile.name}
            </p>
          )}
        </div>

        {/* Pet Name */}
        <div className="space-y-1.5">
          <Label htmlFor="petEditName" className="text-gray-300">
            Name *
          </Label>
          <div className="relative">
            <PawPrint
              size={16}
              className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400"
            />
            <Input
              id="petEditName"
              name="name"
              type="text"
              required
              value={formData.name || ""}
              onChange={handleChange}
              disabled={isSaving}
              placeholder="Your pet's name"
              className="pl-10 bg-[#070913] border-gray-700 focus:ring-cyan-600 focus:border-cyan-600"
            />
          </div>
        </div>

        {/* Breed and Birth Date */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          {/* Breed */}
          <div className="space-y-1.5">
            <Label htmlFor="petEditBreedId" className="text-gray-300">
              Breed
            </Label>
            <Select
          name="breedId"
          value={formData.breedId?.toString() ?? MIXED_OTHER_BREED_ID} 
          onValueChange={handleBreedChange}
          disabled={isSaving || breedsLoading}
        >
          <SelectTrigger className="bg-[#070913] border-gray-700 focus:ring-cyan-600 focus:border-cyan-600">
            <SelectValue placeholder="Select breed" />
          </SelectTrigger>
          <SelectContent className="bg-[#0c1225] border-gray-700 text-white">
            {breedsLoading ? (
              <SelectItem value="loading" disabled> 
                Loading breeds...
              </SelectItem>
            ) : (
              breeds.map((breed) => (
                <SelectItem
                  key={breed.id.toString()}
                  value={breed.id.toString()} 
                  className="hover:bg-cyan-800 focus:bg-cyan-700"
                >
                  {breed.name}
                </SelectItem>
              ))
            )}
          </SelectContent>
            </Select>
          </div>

          {/* Birth Date */}
          <div className="space-y-1.5">
            <Label htmlFor="petEditBirthDate" className="text-gray-300">
              Birth Date
            </Label>
            <div className="relative">
              <Calendar
                size={16}
                className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400"
              />
              <Input
                id="petEditBirthDate"
                name="birthDate"
                type="date"
                value={formData.birthDate || ""}
                onChange={handleChange}
                disabled={isSaving}
                className="pl-10 bg-[#070913] border-gray-700 focus:ring-cyan-600 focus:border-cyan-600"
              />
            </div>
          </div>
        </div>

        {/* Gender and Color */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
           {/* Gender */}
          <div className="space-y-1.5">
            <Label className="block text-sm font-medium text-gray-300 mb-1">
              Gender
            </Label>
            <Select
            name="gender"
            value={formData.gender || ""} 
            onValueChange={handleGenderChange}
            disabled={isSaving}
        >
            <SelectTrigger className="bg-[#070913] border-gray-700 focus:ring-cyan-600 focus:border-cyan-600">
                <SelectValue placeholder="Select gender (Optional)" /> 
            </SelectTrigger>
            <SelectContent className="bg-[#0c1225] border-gray-700 text-white">
                <SelectItem value="placeholder_trigger_value_for_deselect" className="text-gray-400 italic hover:bg-cyan-800 focus:bg-cyan-700">
                    Not specified
                </SelectItem>
                {Object.values(Gender).map((g) => (
                    <SelectItem
                        key={g}
                        value={g} 
                        className="hover:bg-cyan-800 focus:bg-cyan-700"
                    >
                        {g.charAt(0) + g.slice(1).toLowerCase()}
                    </SelectItem>
                ))}
            </SelectContent>
        </Select>
          </div>
           {/* Color */}
          <div className="space-y-1.5">
            <Label htmlFor="petEditColor" className="text-gray-300">
              Color
            </Label>
            <div className="relative">
              <Palette
                size={16}
                className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400"
              />
              <Input
                id="petEditColor"
                name="color"
                type="text"
                value={formData.color || ""}
                onChange={handleChange}
                disabled={isSaving}
                placeholder="e.g., Brown, Black & White"
                className="pl-10 bg-[#070913] border-gray-700 focus:ring-cyan-600 focus:border-cyan-600"
              />
            </div>
          </div>
        </div>

        {/* Microchip Input */}
        <div className="space-y-1.5">
          <Label htmlFor="petEditMicrochip" className="text-gray-300">
            Microchip
          </Label>
          <div className="relative">
            <Sigma
              size={16}
              className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400"
            />
            <Input
              id="petEditMicrochip"
              name="microchip"
              type="text"
              maxLength={50}
              value={formData.microchip || ""}
              onChange={handleChange}
              disabled={isSaving}
              placeholder="Pet's microchip number"
              className="pl-10 bg-[#070913] border-gray-700 focus:ring-cyan-600 focus:border-cyan-600"
            />
          </div>
        </div>

        {/* Action Buttons */}
        <div className="flex justify-end gap-4 pt-5 border-t border-[#FFECAB]/20 mt-5">
          <Button
            type="button"
            onClick={onClose}
            disabled={isSaving}
            aria-label="Cancel edition"
            className="px-5 py-2.5 rounded-lg border border-[#FFECAB]/50 text-[#FFECAB] hover:bg-red-800 hover:text-[#FFECAB] focus-visible:ring-red-500 disabled:opacity-50 cursor-pointer">
            <CircleX size={16} className="mr-2"  /> Cancel
          </Button>
          <Button
            type="submit"
            disabled={isSaving}
            aria-label="Save changes"
            className="px-5 py-2.5 rounded-lg border border-[#FFECAB]/50 bg-cyan-800 text-[#FFECAB] hover:bg-cyan-600 focus-visible:ring-cyan-500 disabled:opacity-50 cursor-pointer"
          >
            <SaveAll size={16} className="mr-2" />
            {isSaving && <Loader2 className="animate-spin h-4 w-4 mr-2" />}
            {isSaving ? "Saving..." : "Save Changes"}
          </Button>
        </div>
      </form>
    </Modal>
  );
};
export default EditPetModal;
