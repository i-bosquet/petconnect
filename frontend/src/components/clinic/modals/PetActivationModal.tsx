import {
  useState,
  useEffect,
  FormEvent,
  ChangeEvent,
  JSX,
  useCallback,
} from "react";
import Modal from "@/components/common/Modal";
import {
  PetProfileDto,
  BreedDto,
  PetActivationDto,
  Specie,
  Gender,
} from "@/types/apiTypes";
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
import { Loader2, CheckCircle, CircleX } from "lucide-react";
import {
  activatePet as activatePetService,
  getBreedsBySpecie,
} from "@/services/petService";
import { useAuth } from "@/hooks/useAuth";
import { BACKEND_BASE_URL } from "@/config";

interface PetActivationModalProps {
  isOpen: boolean;
  onClose: () => void;
  petToActivate: PetProfileDto;
  onPetActivated: () => void;
}

const MIXED_OTHER_BREED_ID_ACTIVATION = "MIXED_OTHER_ACTIVATION";

const defaultImagesActivation: Record<Specie, string> = {
  [Specie.DOG]: "/images/avatars/pets/dog.png",
  [Specie.CAT]: "/images/avatars/pets/cat.png",
  [Specie.RABBIT]: "/images/avatars/pets/rabbit.png",
  [Specie.FERRET]: "/images/avatars/pets/ferret.png",
};

/**
 * PetActivationModal - Modal for a Veterinarian to review, complete,
 * and activate a pet that is in PENDING status.
 * @param {PetActivationModalProps} props - Component props.
 * @returns {JSX.Element | null} The modal for pet activation.
 */
const PetActivationModal = ({
  isOpen,
  onClose,
  petToActivate,
  onPetActivated,
}: PetActivationModalProps): JSX.Element | null => {
  const { token } = useAuth();
  const [formData, setFormData] = useState<Partial<PetActivationDto>>({});
  const [previewImage, setPreviewImage] = useState<string | null>(null);
  const [breeds, setBreeds] = useState<BreedDto[]>([]);
  const [breedsLoading, setBreedsLoading] = useState<boolean>(false);
  const [isActivating, setIsActivating] = useState<boolean>(false);
  const [error, setError] = useState<string>("");

  const fetchBreeds = useCallback(
    async (specie: Specie) => {
      if (!token || !specie) {
        setBreeds([]);
        return;
      }
      setBreedsLoading(true);
      try {
        const fetchedBreeds = await getBreedsBySpecie(token, specie);
        const mixedOption: BreedDto = {
          id: MIXED_OTHER_BREED_ID_ACTIVATION,
          name: "Mixed / Other",
          imageUrl: null,
        };
        setBreeds([
          mixedOption,
          ...fetchedBreeds.filter(
            (b) => b.id.toString() !== MIXED_OTHER_BREED_ID_ACTIVATION
          ),
        ]);
      }  catch (err) { 
        console.error("PetActivationModal: Failed to fetch breeds", err); 
        setError(err instanceof Error ? err.message : "Could not load breeds.");
      } finally {
        setBreedsLoading(false);
      }
    },
    [token]
  );
  
  useEffect(() => {
    if (isOpen && petToActivate) {
      setFormData({
        color: petToActivate.color || '',
        gender: petToActivate.gender,
        birthDate: petToActivate.birthDate || '',
        microchip: petToActivate.microchip || '',
        breedId: petToActivate.breedId ? Number(petToActivate.breedId) : undefined,
      });
      setPreviewImage(petToActivate.image || `${BACKEND_BASE_URL}${defaultImagesActivation[petToActivate.specie]}`);
      setError("");
       if (petToActivate.specie) {
            fetchBreeds(petToActivate.specie);
        }
    }
  }, [isOpen, petToActivate, fetchBreeds]);


  const handleChange = (
    e: ChangeEvent<HTMLInputElement | HTMLSelectElement>
  ) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value === "" ? null : value }));
  };

  const handleBreedChange = (value: string) => {
     setFormData((prev) => ({ ...prev, breedId: value }));
  };

  const handleGenderChange = (value: string) => {
    setFormData((prev) => ({
      ...prev,
      gender:
        value === "placeholder_gender_deselect" ? undefined : (value as Gender),
    }));
  };

  const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (!token) {
      setError("Authentication error.");
      return;
    }

    if (
      !formData.color ||
        formData.gender === undefined || formData.gender === null ||
        !formData.birthDate ||
        !formData.microchip ||
        formData.breedId === undefined || formData.breedId === null
    ) {
      setError(
        "All fields: Color, Gender, Birth Date, Microchip, and Breed are required for activation."
      );
      return;
    }

    setIsActivating(true);
    setError("");

    const activationPayload: PetActivationDto = {
      color: formData.color!,
      gender: formData.gender!,
      birthDate: formData.birthDate!,
      microchip: formData.microchip!,
      breedId: Number(formData.breedId!),
    };

    try {
      await activatePetService(token, petToActivate.id, activationPayload);
      onPetActivated();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to activate pet.");
    } finally {
      setIsActivating(false);
    }
  };


  if (!isOpen) return null;

  return (
    <Modal
      title={`Activate Pet: ${petToActivate.name}`}
      onClose={onClose}
      maxWidth="max-w-3xl"
    >
      <form onSubmit={handleSubmit} className="space-y-4 md:space-y-5">
        {error && (
          <div className="p-3 bg-red-900/30 text-red-300 rounded-lg text-sm text-center">
            {error}
          </div>
        )}

        <div className="flex flex-col items-center">
          <div className="relative mb-2">
            <img
              src={previewImage || undefined} 
              alt="Pet Preview"
              className="w-28 h-28 sm:w-32 sm:h-32 rounded-full object-cover border-4 border-[#FFECAB]/50 bg-gray-700"
            />
          </div>
          <p className="text-xs text-gray-400 mt-1">Current pet image.</p>
        </div>

        {/* Breed */}
        <div className="space-y-1.5">
          <Label htmlFor="activatePetBreedId" className="text-gray-300">
            Breed 
          </Label>
          <Select
            name="breedId"
            value={formData.breedId?.toString() || ""}
            onValueChange={handleBreedChange}
            disabled={isActivating || breedsLoading}
          >
            <SelectTrigger className="bg-[#070913] border-gray-700">
              <SelectValue placeholder="Select breed" />
            </SelectTrigger>
            <SelectContent className="bg-[#0c1225] border-gray-700 text-white">
              {breedsLoading ? (
                <SelectItem value="loadingBreed" disabled>
                  Loading...
                </SelectItem>
              ) : (
                breeds.map((breed) => (
                  <SelectItem
                    key={breed.id.toString()}
                    value={breed.id.toString()}
                    disabled={breed.id === MIXED_OTHER_BREED_ID_ACTIVATION}
                  >
                    {breed.name}
                  </SelectItem>
                ))
              )}
            </SelectContent>
          </Select>
        </div>

        {/* BirthDate */}
        <div className="space-y-1.5">
          <Label htmlFor="activatePetBirthDate" className="text-gray-300">
            Birth Date *
          </Label>
          <Input
            id="activatePetBirthDate"
            name="birthDate"
            type="date"
            required
            value={formData.birthDate || ""}
            onChange={handleChange}
            disabled={isActivating}
            className="bg-[#070913] border-gray-700"
          />
        </div>

        {/* Microchip */}
        <div className="space-y-1.5">
          <Label htmlFor="activatePetMicrochip" className="text-gray-300">
            Microchip *
          </Label>
          <Input
            id="activatePetMicrochip"
            name="microchip"
            required
            value={formData.microchip || ""}
            onChange={handleChange}
            disabled={isActivating}
            className="bg-[#070913] border-gray-700"
          />
        </div>

        {/* Gender */}
        <div className="space-y-1.5">
          <Label className="text-gray-300">Gender</Label>
          <Select
            name="gender"
            required
            value={formData.gender || ""}
            onValueChange={handleGenderChange}
            disabled={isActivating}
          >
            <SelectTrigger className="bg-[#070913] border-gray-700">
              <SelectValue placeholder="Select gender *" />
            </SelectTrigger>
            <SelectContent className="bg-[#0c1225] border-gray-700 text-white">
              {Object.values(Gender).map((g) => (
                <SelectItem key={g} value={g}>
                  {g.charAt(0) + g.slice(1).toLowerCase()}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>

        {/* Color */}
        <div className="space-y-1.5">
          <Label htmlFor="activatePetColor" className="text-gray-300">
            Color
          </Label>
          <Input
            id="activatePetColor"
            name="color"
            value={formData.color || ""}
            onChange={handleChange}
            disabled={isActivating}
            className="bg-[#070913] border-gray-700"
          />
        </div>

        {/* Action Buttons*/}
        <div className="flex justify-end gap-3 pt-4 border-t border-[#FFECAB]/20 mt-5">
          <Button
            type="button"
            onClick={onClose}
            disabled={isActivating}
            className="px-5 py-2.5 rounded-lg border border-[#FFECAB]/50 text-[#FFECAB] hover:bg-red-800 hover:text-[#FFECAB] focus-visible:ring-red-500 disabled:opacity-50 cursor-pointer">
    <CircleX size={16} className="mr-2"  />
            Cancel
          </Button>
          <Button
            type="submit"
            disabled={isActivating}
            className="px-5 py-2.5 rounded-lg border border-[#FFECAB]/50 bg-cyan-800 text-[#FFECAB] hover:bg-cyan-600 focus-visible:ring-cyan-500 disabled:opacity-50 cursor-pointer">
            {isActivating && <Loader2 className="animate-spin h-4 w-4 mr-2" />}
            <CheckCircle size={16} className="mr-2" />
            Activate Pet
          </Button>
        </div>
      </form>
    </Modal>
  );
};
export default PetActivationModal;
