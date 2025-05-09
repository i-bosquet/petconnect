// frontend/src/components/clinic/modals/EditClinicModal.tsx
import {
  useState,
  useEffect,
  FormEvent,
  ChangeEvent,
  JSX,
  useRef,
} from "react";
import Modal from "@/components/common/Modal";
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
import { Button } from "@/components/ui/button";
import { Loader2, SaveAll, CircleX, Upload } from "lucide-react";
import { ClinicDto, ClinicUpdatePayload, Country } from "@/types/apiTypes";
import { updateClinic } from "@/services/clinicService";
import { useAuth } from "@/hooks/useAuth";

interface EditClinicModalProps {
  isOpen: boolean;
  onClose: () => void;
  clinicData: ClinicDto;
  onClinicUpdated: () => void;
}

/**
 * EditClinicModal - Modal form for Admins to update their clinic's details.
 */
const EditClinicModal = ({
  isOpen,
  onClose,
  clinicData,
  onClinicUpdated,
}: EditClinicModalProps): JSX.Element | null => {
  const { token } = useAuth();
  const [formData, setFormData] = useState<Partial<ClinicUpdatePayload>>({
    name: clinicData.name,
    address: clinicData.address,
    city: clinicData.city,
    country: clinicData.country,
    phone: clinicData.phone,
  });
  const [selectedPublicKeyFile, setSelectedPublicKeyFile] =
    useState<File | null>(null);
  const publicKeyFileInputRef = useRef<HTMLInputElement>(null);
  const [isSaving, setIsSaving] = useState<boolean>(false);
  const [error, setError] = useState<string>("");

  useEffect(() => {
    if (isOpen) {

      setFormData({
        name: clinicData.name,
        address: clinicData.address,
        city: clinicData.city,
        country: clinicData.country,
        phone: clinicData.phone,
      });
      setError('');
      setSelectedPublicKeyFile(null);
      if (publicKeyFileInputRef.current) {
        publicKeyFileInputRef.current.value = "";
      }
      setError("");
    }
  }, [isOpen, clinicData]);

  const handleChange = (
    e: ChangeEvent<HTMLInputElement | HTMLTextAreaElement>
  ) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
    setError("");
  };

  const handleCountryChange = (value: string) => {
    setFormData((prev) => ({ ...prev, country: value as Country }));
    setError("");
  };

  const handlePublicKeyFileChange = (e: ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      const file = e.target.files[0];
      const allowedExtensions = [".pem", ".crt"];
      const fileExtension = file.name
        .substring(file.name.lastIndexOf("."))
        .toLowerCase();

      if (allowedExtensions.includes(fileExtension)) {
        setSelectedPublicKeyFile(file);
        setError("");
      } else {
        setSelectedPublicKeyFile(null);
        e.target.value = "";
        setError("Invalid file type. Please select a .pem or .crt file.");
      }
    } else {
      setSelectedPublicKeyFile(null);
    }
  };

  const triggerPublicKeyFileInput = () => {
    publicKeyFileInputRef.current?.click();
  };

  const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (!token) {
      setError("Authentication error.");
      return;
    }
    if (
      !formData.name ||
      !formData.address ||
      !formData.city ||
      !formData.country ||
      !formData.phone
    ) {
      setError("All fields are required.");
      return;
    }
    setIsSaving(true);
    setError("");

    try {
      const payload: ClinicUpdatePayload = {
        name: formData.name,
        address: formData.address,
        city: formData.city,
        country: formData.country,
        phone: formData.phone,
      };
      await updateClinic(token, clinicData.id, payload, selectedPublicKeyFile);
      onClinicUpdated();
    } catch (err) {
      setError(
        err instanceof Error ? err.message : "Failed to update clinic details."
      );
    } finally {
      setIsSaving(false);
    }
  };

  if (!isOpen) return null;

  return (
    <Modal
      title={`Edit Clinic: ${clinicData.name}`}
      onClose={onClose}
      maxWidth="max-w-2xl"
    >
      <form onSubmit={handleSubmit} className="space-y-4">
        {error && (
          <div className="p-3 bg-red-900/30 text-red-300 rounded-lg text-sm text-center">
            {error}
          </div>
        )}

        {/* Name */}
        <div className="space-y-1.5">
          <Label htmlFor="editClinicName" className="text-gray-300">
            Clinic Name *
          </Label>
          <Input
            id="editClinicName"
            name="name"
            required
            value={formData.name || ""}
            onChange={handleChange}
            disabled={isSaving}
            className="bg-[#070913] border-gray-700 focus:ring-cyan-600 focus:border-cyan-600"
          />
        </div>
        {/* Address */}
        <div className="space-y-1.5">
          <Label htmlFor="editClinicAddress" className="text-gray-300">
            Address *
          </Label>
          <Textarea
            id="editClinicAddress"
            name="address"
            required
            value={formData.address || ""}
            onChange={handleChange}
            disabled={isSaving}
            rows={3}
            className="bg-[#070913] border-gray-700 focus:ring-cyan-600 focus:border-cyan-600 custom-scrollbar"
          />
        </div>
        {/* City & Country */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div className="space-y-1.5">
            <Label htmlFor="editClinicCity" className="text-gray-300">
              City *
            </Label>
            <Input
              id="editClinicCity"
              name="city"
              required
              value={formData.city || ""}
              onChange={handleChange}
              disabled={isSaving}
              className="bg-[#070913] border-gray-700 focus:ring-cyan-600 focus:border-cyan-600"
            />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="editClinicCountry" className="text-gray-300">
              Country *
            </Label>
            <Select
              name="country"
              required
              value={formData.country || ""}
              onValueChange={handleCountryChange}
              disabled={isSaving}
            >
              <SelectTrigger className="bg-[#070913] border-gray-700 focus:ring-cyan-600 focus:border-cyan-600">
                <SelectValue placeholder="Select country" />
              </SelectTrigger>
              <SelectContent className="bg-[#0c1225] border-gray-700 text-white">
                {Object.values(Country).map((c) => (
                    <SelectItem
                      key={c}
                      value={c}
                      className="hover:bg-cyan-800 focus:bg-cyan-700"
                    >
                      {c.replace(/_/g, " ")}
                    </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
        </div>
        {/* Phone */}
        <div className="space-y-1.5">
          <Label htmlFor="editClinicPhone" className="text-gray-300">
            Phone *
          </Label>
          <Input
            id="editClinicPhone"
            name="phone"
            type="tel"
            required
            value={formData.phone || ""}
            onChange={handleChange}
            disabled={isSaving}
            className="bg-[#070913] border-gray-700 focus:ring-cyan-600 focus:border-cyan-600"
          />
        </div>

        {/* Public key */}
        <div className="mt-4 pt-4 border-t border-[#FFECAB]/20 space-y-2">
          <Label
            htmlFor="clinicPublicKeyFile"
            className="block text-sm font-medium text-gray-300"
          >
            Update Public Key File (.pem/.crt)
          </Label>
          <p className="text-xs text-gray-400 mb-1">
            Current Path: {clinicData.publicKey || "None"}
          </p>
          <div className="mt-1 flex items-center gap-2">
            <Button
              type="button"
              onClick={triggerPublicKeyFileInput}
              disabled={isSaving}
              size="sm"
              className="border-[#FFECAB]/50 text-sm px-3 py-1.5 text-[#FFECAB]  hover:text-cyan-800 hover:bg-gray-300 border cursor-pointer"
            >
              <Upload size={16} className="mr-2" />
              {selectedPublicKeyFile ? "Change File" : "Select New Key File"}
            </Button>
            <input
              id="clinicPublicKeyFile"
              name="clinicPublicKeyFile"
              type="file"
              ref={publicKeyFileInputRef}
              className="hidden"
              accept=".pem,.crt,application/x-x509-ca-cert,application/pkix-cert"
              onChange={handlePublicKeyFileChange}
              disabled={isSaving}
            />
            {selectedPublicKeyFile && (
              <span className="text-xs text-gray-400 truncate max-w-xs">
                {selectedPublicKeyFile.name}
              </span>
            )}
            {!selectedPublicKeyFile && (
              <span className="text-xs text-gray-500">
                Leave empty to keep current key.
              </span>
            )}
          </div>
          {error.includes("key file type") && (
            <p className="text-xs text-red-400 mt-1">{error}</p>
          )}
        </div>

        {/* Action Buttons */}
        <div className="flex justify-end gap-4 pt-5 border-t border-[#FFECAB]/20 mt-5">
          <Button
            type="button"
            onClick={onClose}
            disabled={isSaving}
            className="px-5 py-2.5 rounded-lg border border-[#FFECAB]/50 text-[#FFECAB] hover:bg-red-800 hover:text-[#FFECAB] focus-visible:ring-red-500 disabled:opacity-50 cursor-pointer"
          >
            <CircleX size={16} className="mr-2" />
            Cancel
          </Button>
          <Button
            type="submit"
            disabled={isSaving}
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
export default EditClinicModal;
