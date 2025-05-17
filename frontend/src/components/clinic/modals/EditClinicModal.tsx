import {
  useState,
  useEffect,
  FormEvent,
  ChangeEvent,
  JSX,
  useRef,
} from "react";
import Modal  from "@/components/common/Modal";
import ConfirmationModal from "@/components/common/ConfirmationModal";
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
import { Loader2, SaveAll, CircleX, Upload, KeyRound } from "lucide-react";
import { ClinicDto, ClinicUpdatePayload, Country } from "@/types/apiTypes";
import { updateClinic } from "@/services/clinicService";
import { useAuth } from "@/hooks/useAuth";
import { toast } from "sonner";

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
  const [selectedPublicKeyFile, setSelectedPublicKeyFile] = useState<File | null>(null);
  const publicKeyFileInputRef = useRef<HTMLInputElement>(null);
  const [selectedPrivateKeyFile, setSelectedPrivateKeyFile] = useState<File | null>(null);
  const privateKeyFileInputRef = useRef<HTMLInputElement>(null);
  const [showKeyChangeConfirmModal, setShowKeyChangeConfirmModal] = useState<boolean>(false);

  const [isSaving, setIsSaving] = useState<boolean>(false);
  const [error, setError] = useState<string>("");
  const [fileError, setFileError] = useState<string>("");

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
      setFileError('');
      setSelectedPublicKeyFile(null);
      setSelectedPrivateKeyFile(null);
      if (publicKeyFileInputRef.current) publicKeyFileInputRef.current.value = "";
      if (privateKeyFileInputRef.current) privateKeyFileInputRef.current.value = "";
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

  const handleKeyFileChange  = (
    e: ChangeEvent<HTMLInputElement>,
    setFileState: React.Dispatch<React.SetStateAction<File | null>>,
    keyType: 'Public' | 'Private Encrypted'
  ) => {
     setFileError('');
    if (e.target.files && e.target.files[0]) {
      const file = e.target.files[0];
      const allowedExtensions = [".pem", ".crt"];
      const fileExtension = file.name
        .substring(file.name.lastIndexOf("."))
        .toLowerCase();

      if (!allowedExtensions.includes(fileExtension)) {
            setFileState(null);
            e.target.value = ""; // Limpiar el input
            setFileError(`Invalid ${keyType} Key file type. Allowed: ${allowedExtensions.join(", ")}.`);
            return;
        }
         setFileState(file);
    } else {
      setFileState(null);
    }
  };

  const triggerPublicKeyFileInput = () => publicKeyFileInputRef.current?.click();
  const triggerPrivateKeyFileInput = () => privateKeyFileInputRef.current?.click();

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

    if (selectedPublicKeyFile || selectedPrivateKeyFile) {setShowKeyChangeConfirmModal(true); return;}

    setIsSaving(true);
    try {
      const payload: ClinicUpdatePayload = {
        name: formData.name,
        address: formData.address,
        city: formData.city,
        country: formData.country,
        phone: formData.phone,
      };
      await updateClinic(
        token,
        clinicData.id,
        payload,
        selectedPublicKeyFile,
        selectedPrivateKeyFile
      );
      toast.success("Clinic details updated successfully!");
      onClinicUpdated();
      onClose();
    } catch (err) {
      const errMsg =
        err instanceof Error ? err.message : "Failed to update clinic details.";
      setError(errMsg);
      toast.error(errMsg);
    } finally {
      setIsSaving(false);
    }
  };

  const handleConfirmKeyChangeAndSaveChanges = async () => {
        setShowKeyChangeConfirmModal(false); 
        if (!token) { setError("Authentication error."); return; } 

        setIsSaving(true);
        setError('');
        setFileError('');
        try {
            const payload: ClinicUpdatePayload = {
                name: formData.name,
                address: formData.address,
                city: formData.city,
                country: formData.country,
                phone: formData.phone,
            };
            await updateClinic(
                token,
                clinicData.id,
                payload,
                selectedPublicKeyFile, 
                selectedPrivateKeyFile
            );
            toast.success("Clinic details and key(s) updated successfully!");
            onClinicUpdated();
            onClose(); 
        } catch (err) {
            const errMsg = err instanceof Error ? err.message : "Failed to update clinic details and/or keys.";
            setError(errMsg); 
            toast.error(errMsg);
        } finally {
            setIsSaving(false);
        }
  };

  if (!isOpen) return null;

  return (
    <>
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
          {fileError && (
            <div className="p-2 mt-2 bg-orange-800/40 text-orange-300 rounded-md text-xs text-center">
              {fileError}
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

          {/* Keys */}
          <div className="mt-4 pt-4 border-t border-[#FFECAB]/20 space-y-3">
            <h4 className="text-md font-semibold text-[#FFECAB]">
              Cryptographic Keys
            </h4>
            {/* Public Key Upload */}
            <div className="space-y-1">
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
                  {selectedPublicKeyFile
                    ? "Change File"
                    : "Upload New Key File"}
                </Button>
                <input
                  id="clinicPublicKeyFile"
                  name="clinicPublicKeyFile"
                  type="file"
                  ref={publicKeyFileInputRef}
                  className="hidden"
                  accept=".pem,.crt,application/x-x509-ca-cert,application/pkix-cert"
                  onChange={(e) =>
                    handleKeyFileChange(e, setSelectedPublicKeyFile, "Public")
                  }
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

            {/* Private key */}
            <div className="space-y-1">
              <Label
                htmlFor="clinicPrivateKeyFile"
                className="text-gray-300 flex items-center gap-1.5"
              >
                <KeyRound size={16} className="text-orange-400" /> Encrypted
                Private Key File (.pem/.crt)
              </Label>
              <p className="text-xs text-gray-400">
                Upload a new encrypted private key file to replace the existing
                one, or to set it if none exists. This key is used by the clinic
                for signing.
              </p>
              <div className="flex items-center gap-2">
                <Button
                  type="button"
                  onClick={triggerPrivateKeyFileInput}
                  disabled={isSaving}
                  size="sm"
                  className="border-[#FFECAB]/50 text-sm px-3 py-1.5 text-[#FFECAB]  hover:text-cyan-800 hover:bg-gray-300 border cursor-pointer"
                >
                  <Upload size={16} className="mr-2" />{" "}
                  {selectedPrivateKeyFile
                    ? "Change Private Key"
                    : "Upload New Private Key"}
                </Button>
                <input
                  id="clinicPrivateKeyFile"
                  ref={privateKeyFileInputRef}
                  type="file"
                  className="hidden"
                  accept=".pem,.crt"
                  onChange={(e) =>
                    handleKeyFileChange(
                      e,
                      setSelectedPrivateKeyFile,
                      "Private Encrypted"
                    )
                  }
                  disabled={isSaving}
                />
                {selectedPrivateKeyFile && (
                  <span className="text-xs text-gray-400 truncate max-w-[calc(100%-150px)]">
                    {selectedPrivateKeyFile.name}
                  </span>
                )}
              </div>
            </div>
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

      {/* Confirmation Modal for Key Change */}
      {showKeyChangeConfirmModal && (
        <ConfirmationModal
          isOpen={showKeyChangeConfirmModal}
          onClose={() => setShowKeyChangeConfirmModal(false)}
          onConfirm={handleConfirmKeyChangeAndSaveChanges} 
          title="Confirm Cryptographic Key Change"
          isLoading={isSaving}
          confirmButtonText="Yes, Update Key(s) & Save"
          message={
            <div className="text-sm text-left space-y-2">
              <p>
                You are about to update the cryptographic key(s) for clinic{" "}
                <strong className="text-[#FFECAB]">{clinicData.name}</strong>.
              </p>
              <p className="font-semibold mt-2">Important Consequences:</p>
              <ul className="list-disc list-inside text-xs text-gray-300 space-y-1 pl-4">
                <li><strong>New Signatures:</strong> Any new documents signed by this clinic will use the new private key (if updated).</li>
                <li><strong>Old Document Verification:</strong> Documents signed with the old key will remain verifiable with the old public key.</li>
                <li><strong>Security:</strong> Ensure new key files are correct and securely obtained.</li>
                <li><strong>Private Key Custody:</strong> If updating the encrypted private key, its password must be kept extremely secure.</li>
              </ul>
              <p className="mt-2">An email notification will be sent to the clinic administrator about this change.</p>
              <p className="font-bold mt-3">Are you sure you want to proceed?</p>
            </div>
          }
        />
      )}
    </>
  );
};
export default EditClinicModal;
