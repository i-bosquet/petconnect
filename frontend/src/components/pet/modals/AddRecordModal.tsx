import { useState, useEffect, FormEvent, ChangeEvent, JSX } from "react";
import Modal from "@/components/common/Modal";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Checkbox } from "@/components/ui/checkbox";
import {
  RecordCreatePayload,
  RecordType,
  VaccineCreatePayload ,
  RoleEnum
} from "@/types/apiTypes";
import { createRecord } from "@/services/recordService";
import { useAuth } from "@/hooks/useAuth";
import { Loader2, Save, CircleX, ShieldPlus, LockKeyhole,Eye, EyeOff } from "lucide-react";
import { toast } from "sonner";

interface AddRecordModalProps {
  isOpen: boolean;
  onClose: () => void;
  onRecordAdded: () => void; // Callback para refrescar la lista
  petId: number | string;
}

const initialFormData: Partial<RecordCreatePayload> = {
  type: RecordType.OTHER, // Default  OTHER
  description: "",
  vaccine: undefined,
  vetPrivateKeyPassword: '',
};

const initialVaccineData: Partial<VaccineCreatePayload > = {
  name: "",
  validity: 1, // Default 1 year
  laboratory: "",
  batchNumber: "",
  isRabiesVaccine: false,
};

/**
 * AddRecordModal - Modal for creating a new medical record for a pet.
 * Includes specific fields if the record type is VACCINE.
 * @param {AddRecordModalProps} props - Component props.
 * @returns {JSX.Element | null} The modal for adding a medical record.
 */
const AddRecordModal = ({
  isOpen,
  onClose,
  onRecordAdded,
  petId,
}: AddRecordModalProps): JSX.Element | null => {
  const { token, user } = useAuth();
  const [formData, setFormData] =useState<Partial<RecordCreatePayload>>(initialFormData);
  const [vaccineData, setVaccineData] =useState<Partial<VaccineCreatePayload >>(initialVaccineData);
  const [vetPassword, setVetPassword] = useState<string>(''); 
  const [showVetPassword, setShowVetPassword] = useState<boolean>(false);
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [error, setError] = useState<string>("");

  const isCurrentUserVet = user?.roles?.includes(RoleEnum.VET) ?? false;

  // Reset form when modal opens
  useEffect(() => {
    if (isOpen) {
      setFormData(initialFormData);
      setVaccineData(initialVaccineData);
      setVetPassword(''); 
      setError("");
      setIsLoading(false);
    }
  }, [isOpen]);

  const handleMainChange = (
    e: ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>
  ) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

 const handleRecordTypeChange = (value: string) => {
    const newType = value as RecordType;
    setFormData((prev: Partial<RecordCreatePayload>) => { 
        let vaccinePayload: VaccineCreatePayload | null | undefined = undefined;

        if (newType === RecordType.VACCINE) {
            const existingOrInitialVaccine = prev.vaccine || initialVaccineData;
            vaccinePayload = {
                name: existingOrInitialVaccine.name || '', 
                validity: existingOrInitialVaccine.validity ?? 1, 
                laboratory: existingOrInitialVaccine.laboratory || null,
                batchNumber: existingOrInitialVaccine.batchNumber || '', 
                isRabiesVaccine: existingOrInitialVaccine.isRabiesVaccine || false,
            };
        }

        return {
            ...prev,
            type: newType,
            vaccine: vaccinePayload, 
        };
    });

    if (newType !== RecordType.VACCINE) {
        setVaccineData(initialVaccineData); 
    } else {
        const currentVaccineState = formData.vaccine || initialVaccineData;
        setVaccineData({
            name: currentVaccineState.name || '',
            validity: currentVaccineState.validity ?? 1,
            laboratory: currentVaccineState.laboratory || '',
            batchNumber: currentVaccineState.batchNumber || '',
            isRabiesVaccine: currentVaccineState.isRabiesVaccine || false,
        });
    }
};

  const handleVaccineChange = (e: ChangeEvent<HTMLInputElement>) => {
    const { name, value, type, checked } = e.target;
    setVaccineData((prev: Partial<VaccineCreatePayload>) => ({
      ...prev,
      [name]:
        type === "checkbox"
          ? checked
          : type === "number"
          ? value === ""
            ? null
            : Number(value)
          : value,
    }));
  };

  const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (!token) {setError("Authentication error. Please log in again."); return;}
    if (!formData.type) {setError("Record type is required."); return;}
    if (formData.type === RecordType.VACCINE) {
      if (
        !vaccineData.name ||
        !vaccineData.batchNumber ||
        vaccineData.validity === null ||
        vaccineData.validity === undefined
      ) {
        setError(
          "For vaccine records, vaccine name, batch number, and validity are required."
        );
        return;
      }
      if (vaccineData.validity < 0) {
        setError("Vaccine validity cannot be negative.");
        return;
      }
    }
     if (isCurrentUserVet && !vetPassword.trim()) {
            setError("Your private key password is required to sign this record.");
            return;
        }

    setIsLoading(true);
    setError("");

    const payload: RecordCreatePayload = {
      petId: petId,
      type: formData.type!,
      description: formData.description || null,
      vaccine:
        formData.type === RecordType.VACCINE
          ? {
              name: vaccineData.name!,
              validity: vaccineData.validity!,
              laboratory: vaccineData.laboratory || null,
              batchNumber: vaccineData.batchNumber!,
              isRabiesVaccine: vaccineData.isRabiesVaccine || false,
            }
          : null,
      vetPrivateKeyPassword: isCurrentUserVet ? vetPassword : null, 
    };

    try {
      await createRecord(token, payload);
      toast.success("Medical record added successfully!");
      onRecordAdded();
      onClose();
    } catch (err) {
      console.error("Failed to add record:", err);
      setError(
        err instanceof Error ? err.message : "Could not add medical record."
        
      );
      toast.error(err instanceof Error ? err.message : 'Could not add medical record.');
    } finally {
      setIsLoading(false);
    }
  };

  if (!isOpen) return null;

  return (
    <Modal
      title="Add New Medical Record"
      onClose={onClose}
      maxWidth="max-w-2xl"
    >
      <form onSubmit={handleSubmit} className="space-y-4">
        {error && (
          <div className="p-3 bg-red-900/30 text-red-300 rounded-lg text-sm text-center">
            {error}
          </div>
        )}

        {/* Record Type */}
        <div className="space-y-1.5">
          <Label htmlFor="recordType" className="text-gray-300">
            Record Type *
          </Label>
          <Select
            name="type"
            value={formData.type}
            onValueChange={handleRecordTypeChange}
            disabled={isLoading}
            required
          >
            <SelectTrigger className="bg-[#070913] border-gray-700 focus:ring-cyan-600 focus:border-cyan-600">
              <SelectValue placeholder="Select record type" />
            </SelectTrigger>
            <SelectContent className="bg-[#0c1225] border-gray-700 text-white">
              {Object.values(RecordType).map((type) => (
                <SelectItem
                  key={type}
                  value={type}
                  className="hover:bg-cyan-800 focus:bg-cyan-700"
                >
                  {type
                    .replace(/_/g, " ")
                    .replace(/\b\w/g, (l) => l.toUpperCase())}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>

        {/* Description */}
        <div className="space-y-1.5">
          <Label htmlFor="description" className="text-gray-300">
            Description
          </Label>
          <Textarea
            id="description"
            name="description"
            value={formData.description || ""}
            onChange={handleMainChange}
            disabled={isLoading}
            placeholder="Notes, symptoms, observations, etc."
            rows={4}
            maxLength={2000}
            className="bg-[#070913] border-gray-700 focus:ring-cyan-600 focus:border-cyan-600 custom-scrollbar"
          />
        </div>

        {/* Vaccine Details */}
        {formData.type === RecordType.VACCINE && (
          <div className="mt-4 pt-4 border-t border-[#FFECAB]/20 space-y-4">
            <h4 className="text-md font-semibold text-[#FFECAB] flex items-center">
              <ShieldPlus size={18} className="mr-2 text-cyan-400" />
              Vaccine Details
            </h4>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div className="space-y-1.5">
                <Label htmlFor="vaccineName" className="text-gray-300">
                  Vaccine Name *
                </Label>
                <Input
                  id="vaccineName"
                  name="name"
                  value={vaccineData.name || ""}
                  onChange={handleVaccineChange}
                  disabled={isLoading}
                  required
                  className="bg-[#070913] border-gray-700"
                />
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="batchNumber" className="text-gray-300">
                  Batch Number *
                </Label>
                <Input
                  id="batchNumber"
                  name="batchNumber"
                  value={vaccineData.batchNumber || ""}
                  onChange={handleVaccineChange}
                  disabled={isLoading}
                  required
                  className="bg-[#070913] border-gray-700"
                />
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="laboratory" className="text-gray-300">
                  Laboratory *
                </Label>
                <Input
                  id="laboratory"
                  name="laboratory"
                  value={vaccineData.laboratory || ""}
                  onChange={handleVaccineChange}
                  disabled={isLoading}
                  className="bg-[#070913] border-gray-700"
                />
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="validity" className="text-gray-300">
                  Validity (years) *
                </Label>
                <Input
                  id="validity"
                  name="validity"
                  type="number"
                  min="0"
                  step="1"
                  value={vaccineData.validity ?? ""}
                  onChange={handleVaccineChange}
                  disabled={isLoading}
                  required
                  className="bg-[#070913] border-gray-700"
                />
              </div>
              <div className="sm:col-span-2 flex items-center space-x-2 mt-2">
                <Checkbox
                  id="isRabiesVaccine"
                  name="isRabiesVaccine"
                  checked={vaccineData.isRabiesVaccine || false}
                  onCheckedChange={(checkedState) => {
                    const isChecked = !!checkedState; 
                    setVaccineData((prev: Partial<VaccineCreatePayload>) => ({
                      ...prev,
                      isRabiesVaccine: isChecked,
                    }));
                  }}
                  disabled={isLoading}
                  className="data-[state=checked]:bg-cyan-600 border-gray-500"
                />
                <Label
                  htmlFor="isRabiesVaccine"
                  className="text-sm font-medium text-gray-300 cursor-pointer"
                >
                  Is this a Rabies Vaccine?
                </Label>
              </div>
            </div>
          </div>
        )}

        {/* Password for Vet */}
        {isCurrentUserVet && (
        <div className="mt-4 pt-4 border-t border-[#FFECAB]/20 space-y-2">
          <Label htmlFor="vetPasswordForSign" className="text-gray-300 flex items-center">
            <LockKeyhole size={16} className="mr-2 text-orange-400" />Your Signing Password * </Label>
          <p className="text-xs text-gray-400">Enter the password for your private signing key to digitally sign this record.</p>
          <div className="relative">
            <Input
              id="vetPasswordForSign"
              type={showVetPassword ? "text" : "password"}
              value={vetPassword}
              onChange={(e) => setVetPassword(e.target.value)}
              placeholder="Enter your signing password"
              required
              disabled={isLoading}
              className="bg-[#070913] border-gray-700 pr-10"
             />
            <button
              type="button"
              onClick={() => setShowVetPassword(!showVetPassword)}
              className="absolute inset-y-0 right-0 pr-3 flex items-center text-gray-400 hover:text-gray-200"
              aria-label={showVetPassword ? "Hide password" : "Show password"}
            >
              {showVetPassword ? <EyeOff size={18} /> : <Eye size={18} />}
            </button>
          </div>
        </div>          
        )}        

        {/* Action Buttons */}
        <div className="flex justify-end gap-3 pt-4 border-t border-[#FFECAB]/20 mt-5">
          <Button
            type="button"
            onClick={onClose}
            disabled={isLoading}
            className="px-5 py-2.5 rounded-lg border border-[#FFECAB]/50 text-[#FFECAB] hover:bg-red-800 hover:text-[#FFECAB] focus-visible:ring-red-500 disabled:opacity-50 cursor-pointer">
            <CircleX size={16} className="mr-2" />
            Cancel
          </Button>
          <Button
            type="submit"
            disabled={isLoading}
            className="px-5 py-2.5 rounded-lg border border-[#FFECAB]/50 bg-cyan-800 text-[#FFECAB] hover:bg-cyan-600 focus-visible:ring-cyan-500 disabled:opacity-50 cursor-pointer">
            {isLoading && <Loader2 className="animate-spin h-4 w-4 mr-2" />}
            <Save size={16} className="mr-2" />
            Save Record
          </Button>
        </div>
      </form>
    </Modal>
  );
};

export default AddRecordModal;
