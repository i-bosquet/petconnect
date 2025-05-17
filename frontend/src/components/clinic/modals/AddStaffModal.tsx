import { useState, FormEvent, ChangeEvent, JSX, useRef, useEffect} from 'react'; 
import Modal from '@/components/common/Modal';
import { Lock, Mail, User as UserIcon, Briefcase, KeySquare, Upload, Loader2, Eye, EyeOff, CircleX, SaveAll, KeyRound} from 'lucide-react';
import { RoleEnum, ClinicStaffCreationPayload } from '@/types/apiTypes';
import { createClinicStaff } from '@/services/clinicStaffService';
import { useAuth } from '@/hooks/useAuth'; 
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { toast } from 'sonner';

interface AddStaffModalProps {
    isOpen: boolean;
    onClose: () => void;
    onStaffAdded: () => void;
    clinicId: number | string; 
}

interface StaffFormFields {
    username: string;
    email: string;
    password: string;
    name: string;
    surname: string;
    role: RoleEnum;
    licenseNumber: string | null; 
}

const initialFormData: StaffFormFields = {
    username: '',
    email: '',
    password: '',
    name: '',
    surname: '',
    role: RoleEnum.VET, 
    licenseNumber: '',
};

/**
 * AddStaffModal - Modal for Admins to create new Clinic Staff (Vets or Admins).
 * Includes file upload for Vet's public key PEM file.
 *
 * @param {AddStaffModalProps} props - Component props.
 * @returns {JSX.Element | null} The modal component or null if not open.
 */
const AddStaffModal = ({ isOpen, onClose, onStaffAdded, clinicId  }: AddStaffModalProps): JSX.Element | null => {
    const { token } = useAuth();
    const [formData, setFormData] = useState<StaffFormFields>(initialFormData);
    const [confirmPassword, setConfirmPassword] = useState<string>('');
    const [showPassword, setShowPassword] = useState<boolean>(false); 
    const [showConfirmPassword, setShowConfirmPassword] = useState<boolean>(false); 

    const [selectedPublicKeyFile, setSelectedPublicKeyFile] = useState<File | null>(null);
    const publicKeyFileInputRef = useRef<HTMLInputElement>(null);
    const [selectedPrivateKeyFile, setSelectedPrivateKeyFile] = useState<File | null>(null); 
    const privateKeyFileInputRef = useRef<HTMLInputElement>(null);

    const [error, setError] = useState<string>('');
    const [fileError, setFileError] = useState<string>('');
    const [isLoading, setIsLoading] = useState<boolean>(false)

    useEffect(() => {
        if (isOpen) {
            setFormData(initialFormData);
            setConfirmPassword('');
            setShowPassword(false);
            setShowConfirmPassword(false);
            setSelectedPublicKeyFile(null);
            setSelectedPrivateKeyFile(null);
            if (publicKeyFileInputRef.current) publicKeyFileInputRef.current.value = '';
            if (privateKeyFileInputRef.current) privateKeyFileInputRef.current.value = '';
            setError('');
            setFileError('');
            setIsLoading(false);
        }
    }, [isOpen, clinicId]);

    const handleChange = (e: ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
        const { name, value } = e.target;
        setFormData(prev => ({ ...prev, [name]: value }));
        if (name === "role" && value === RoleEnum.ADMIN.toString()) {
            setFormData(prev => ({ ...prev, licenseNumber: ''}));
            setSelectedPublicKeyFile(null); 
        }
    };

    const handleKeyFileChange = ( e: ChangeEvent<HTMLInputElement>, 
        setFileState: React.Dispatch<React.SetStateAction<File | null>>,
        keyType: 'Public' | 'Encrypted Private') => {
        setFileError('');
        if (e.target.files && e.target.files[0]) {
            const file = e.target.files[0];
            // Permitir .pem y .crt (común para claves)
            const allowedExtensions = [".pem", ".crt"]; // Puedes añadir .p12, .pfx si son formatos encriptados comunes que manejas
            const fileExtension = file.name.substring(file.name.lastIndexOf(".")).toLowerCase();

            if (!allowedExtensions.includes(fileExtension)) {
                setFileState(null);
                e.target.value = "";
                setFileError(`Invalid ${keyType} Key file. Allowed: ${allowedExtensions.join(", ")}.`);
                return;
            }
            setFileState(file);
        } else {
            setFileState(null);
        }
    };

    const triggerPublicKeyFileInput = () => {publicKeyFileInputRef.current?.click();};

    const triggerPrivateKeyFileInput = () => privateKeyFileInputRef.current?.click(); 

    const validateForm = (): boolean => {
        if (!formData.username || !formData.email || !formData.password || !confirmPassword || !formData.name || !formData.surname) {
            setError("All fields marked with * are required.");
            return false;
        }
        if (formData.password !== confirmPassword) {
            setError("Passwords do not match.");
            return false;
        }
        if (formData.password.length < 8) {
            setError("Password must be at least 8 characters long.");
            return false;
        }

        if (formData.role === RoleEnum.VET) {
            if (!formData.licenseNumber) {
                 setError("License Number is required for VET role.");
                 return false;
            }
            if (!selectedPublicKeyFile) { 
                setError("Public Key file (.pem) is required for VET role.");
                return false;
            }
            if (!selectedPrivateKeyFile) { 
                setError("Encrypted Private Key file is required for VET role."); 
                return false; 
            }
        }
        return true;
    };

    const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        setError('');
        setFileError('');
        if (!validateForm()) return;
        if (!token) { setError("Authentication error."); return; }

        setIsLoading(true);

        const submissionFormData = new FormData();

        // Ensure the role is VET or ADMIN before passing it
        const roleToSend: 'VET' | 'ADMIN' = formData.role === RoleEnum.VET ? 'VET' : 'ADMIN';

        const staffDtoPayload: ClinicStaffCreationPayload = {
            username: formData.username,
            email: formData.email,
            password: formData.password,
            name: formData.name,
            surname: formData.surname,
            role: roleToSend,
            licenseNumber: roleToSend === 'VET' ? formData.licenseNumber : null,
        };

        submissionFormData.append('dto', new Blob([JSON.stringify(staffDtoPayload)], { type: 'application/json' }));

        if (formData.role === RoleEnum.VET) {
            if (selectedPublicKeyFile) {
                submissionFormData.append('publicKeyFile', selectedPublicKeyFile, selectedPublicKeyFile.name);
            }
            if (selectedPrivateKeyFile) { 
                submissionFormData.append('privateKeyFile', selectedPrivateKeyFile, selectedPrivateKeyFile.name);
            }
        }

        try {
            await createClinicStaff(token, submissionFormData);
            toast.success(`Staff member ${formData.username} created successfully!`);
            onStaffAdded();
            console.log("Adding staff for clinic ID:", clinicId);
            onClose();
        } catch (err) {
            console.error("Failed to add staff:", err);
            const errMsg = err instanceof Error ? err.message : 'Could not add staff member.';
            setError(errMsg);
            toast.error(errMsg);
        } finally {
            setIsLoading(false);
        } 
    };

    if (!isOpen) return null;

    return (
        <Modal title="Add New Clinic Staff" onClose={onClose} maxWidth="max-w-2xl">
            <form onSubmit={handleSubmit} className="space-y-4">
                {/* Error Display */}
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

                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    {/* Username */}
                    <div className="space-y-1">
                        <label htmlFor="staffUsername" className="block text-sm font-medium text-gray-300">Username *</label>
                        <div className="relative">
                            <UserIcon size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
                            <input id="staffUsername" name="username" type="text" required value={formData.username} onChange={handleChange} disabled={isLoading}
                                   className="block w-full pl-10 pr-3 py-2.5 border border-gray-700 rounded-xl bg-[#070913] text-white placeholder-gray-500 focus:ring-cyan-600 focus:border-cyan-600"/>
                        </div>
                    </div>
                    {/* Email */}
                    <div className="space-y-1">
                        <label htmlFor="staffEmail" className="block text-sm font-medium text-gray-300">Email *</label>
                        <div className="relative">
                             <Mail size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
                             <input id="staffEmail" name="email" type="email" required value={formData.email} onChange={handleChange} disabled={isLoading}
                                   className="block w-full pl-10 pr-3 py-2.5 border border-gray-700 rounded-xl bg-[#070913] text-white placeholder-gray-500 focus:ring-cyan-600 focus:border-cyan-600"/>
                        </div>
                    </div>
                     {/* Name */}
                     <div className="space-y-1">
                        <label htmlFor="staffName" className="block text-sm font-medium text-gray-300">First Name *</label>
                         <input id="staffName" name="name" type="text" required value={formData.name} onChange={handleChange} disabled={isLoading}
                               className="block w-full px-3 py-2.5 border border-gray-700 rounded-xl bg-[#070913] text-white placeholder-gray-500 focus:ring-cyan-600 focus:border-cyan-600"/>
                    </div>
                    {/* Surname */}
                    <div className="space-y-1">
                        <label htmlFor="staffSurname" className="block text-sm font-medium text-gray-300">Surname *</label>
                        <input id="staffSurname" name="surname" type="text" required value={formData.surname} onChange={handleChange} disabled={isLoading}
                               className="block w-full px-3 py-2.5 border border-gray-700 rounded-xl bg-[#070913] text-white placeholder-gray-500 focus:ring-cyan-600 focus:border-cyan-600"/>
                    </div>
                    {/* Password */}
                    <div className="space-y-1">
                        <label htmlFor="staffPassword" className="block text-sm font-medium text-gray-300">Password *</label>
                        <div className="relative">
                             <Lock size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
                            <input 
                                id="staffPassword" 
                                name="password" 
                                type={showPassword ? "text" : "password"}  
                                required value={formData.password} 
                                onChange={handleChange} 
                                disabled={isLoading}
                                className="block w-full px-10 py-2.5 border border-gray-700 rounded-xl bg-[#070913] text-white placeholder-gray-500 focus:ring-cyan-600 focus:border-cyan-600"
                                placeholder="••••••••"
                            />
                            <button type="button" onClick={() => setShowPassword(!showPassword)} className="absolute inset-y-0 right-0 pr-3 flex items-center text-gray-400 hover:text-gray-300 disabled:opacity-50" disabled={isLoading}>
                                 {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                             </button>
                        </div>
                    </div>
                    {/* Confirm Password */}
                    <div className="space-y-1">
                        <label htmlFor="staffConfirmPassword" className="block text-sm font-medium text-gray-300">Confirm Password *</label>
                        <div className="relative">
                             <Lock size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
                            <input 
                                id="staffConfirmPassword" 
                                name="confirmPassword" 
                                type={showConfirmPassword ? "text" : "password"} 
                                required value={confirmPassword} 
                                onChange={(e) => setConfirmPassword(e.target.value)} disabled={isLoading} 
                                className="block w-full px-10 py-2.5 border border-gray-700 rounded-xl bg-[#070913] text-white placeholder-gray-500 focus:ring-cyan-600 focus:border-cyan-600"
                                placeholder="••••••••"
                            />
                            <button type="button" onClick={() => setShowConfirmPassword(!showConfirmPassword)} className="absolute inset-y-0 right-0 pr-3 flex items-center text-gray-400 hover:text-gray-300 disabled:opacity-50" disabled={isLoading}>
                                  {showConfirmPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                              </button>
                        </div>
                    </div>
                     {/* Role Selection */}
                    <div className="space-y-1 md:col-span-2">
                        <label htmlFor="staffRole" className="block text-sm font-medium text-gray-300">Role *</label>
                         <div className="relative">
                             <Briefcase size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
                            <select id="staffRole" name="role" value={formData.role} onChange={handleChange} disabled={isLoading} required
                                className="block appearance-none w-full pl-10 pr-3 py-2.5 border border-gray-700 rounded-xl bg-[#070913] text-white focus:ring-cyan-600 focus:border-cyan-600">
                                <option value={RoleEnum.VET}>Veterinarian (VET)</option>
                                <option value={RoleEnum.ADMIN}>Administrator (ADMIN)</option>
                            </select>
                         </div>
                    </div>
                </div>

                {/* VET Specific Fields */}
                {formData.role === RoleEnum.VET && (
                    <div className="mt-4 pt-4 border-t border-[#FFECAB]/20 space-y-4">
                        <h4 className="text-md font-semibold text-[#FFECAB]">Veterinarian Details</h4>
                        {/* License Number */}
                        <div className="space-y-1">
                            <label htmlFor="licenseNumber" className="block text-sm font-medium text-gray-300">License Number *</label>
                            <div className="relative">
                                {/* licenseNumber input*/}
                                 <KeySquare size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
                                 <input id="licenseNumber" name="licenseNumber" type="text" value={formData.licenseNumber || ''} onChange={handleChange} disabled={isLoading} required={formData.role === RoleEnum.VET}
                                       className="block w-full pl-10 pr-3 py-2.5 border border-gray-700 rounded-xl bg-[#070913] text-white placeholder-gray-500 focus:ring-cyan-600 focus:border-cyan-600"/>
                            </div>
                        </div>
                        {/* Vet Public Key File Upload */}
                        <div className="space-y-1">
                             <Label htmlFor="vetPublicKeyFile" className="block text-sm font-medium text-gray-300">Public Key File (.pem/.crt) *</Label>
                             <div className="mt-1 flex items-center gap-2">
                                <Button type="button"  onClick={triggerPublicKeyFileInput} disabled={isLoading}
                                        className="border-[#FFECAB]/50 text-sm px-3 py-1.5 text-[#FFECAB]  hover:text-cyan-800 hover:bg-gray-300 border cursor-pointer">
                                    <Upload size={16} className="mr-2"/>
                                    {selectedPublicKeyFile ? "Change File" : "Select File"}
                                </Button>
                                <input
                                    id="vetPublicKeyFile"
                                    name="vetPublicKeyFile"
                                    type="file"
                                    ref={publicKeyFileInputRef}
                                    className="hidden"
                                    accept=".pem,.crt,application/x-x509-ca-cert,application/pkix-cert"
                                    onChange={(e) => handleKeyFileChange(e, setSelectedPublicKeyFile, 'Encrypted Private')}
                                    disabled={isLoading}
                                />
                                 {selectedPublicKeyFile && <span className="text-xs text-gray-400 truncate max-w-xs">{selectedPublicKeyFile.name}</span>}
                                 {!selectedPublicKeyFile && <span className="text-xs text-gray-500">No file selected</span>}
                             </div>
                        </div>
                        {/* Encrypted Private Key File Upload */}
                         <div className="space-y-1">
                            <Label htmlFor="vetPrivateKeyFileEnc" className="text-gray-300 flex items-center gap-1.5">
                                <KeyRound size={16} className="text-orange-400" /> Encrypted Private Key File (.pem/.crt) *
                            </Label>
                            <div className="mt-1 flex items-center gap-2">
                                <Button type="button" onClick={triggerPrivateKeyFileInput} disabled={isLoading}
                                    className="border-[#FFECAB]/50 text-sm px-3 py-1.5 text-[#FFECAB]  hover:text-cyan-800 hover:bg-gray-300 border cursor-pointer">
                                    <Upload size={16} className="mr-2"/>
                                    {selectedPrivateKeyFile ? "Change Encrypted Key" : "Select Encrypted Key"}
                                </Button>
                                <input
                                    id="vetPrivateKeyFileEnc"
                                    type="file"
                                    ref={privateKeyFileInputRef}
                                    className="hidden"
                                    accept=".pem,.crt" // Ajusta según los tipos de archivo que esperas
                                    onChange={(e) => handleKeyFileChange(e, setSelectedPrivateKeyFile, 'Encrypted Private')}
                                    disabled={isLoading}
                                />
                                {selectedPrivateKeyFile && <span className="text-xs text-gray-400 truncate max-w-xs">{selectedPrivateKeyFile.name}</span>}
                                {!selectedPrivateKeyFile && <span className="text-xs text-gray-500">No file selected</span>}
                            </div>
                        </div>
                    </div>
                )}

                {/* Action Buttons */}
                <div className="flex justify-end gap-4 pt-5">
                    <Button type="button" onClick={onClose} disabled={isLoading}
                            className="px-5 py-2.5 rounded-lg border border-[#FFECAB]/50 text-[#FFECAB] hover:bg-red-800 hover:text-[#FFECAB] focus-visible:ring-red-500 disabled:opacity-50 cursor-pointer">
    <CircleX size={16} className="mr-2"  />
                                Cancel
                    </Button>
                    <Button type="submit" disabled={isLoading} 
                    className="px-5 py-2.5 rounded-lg border border-[#FFECAB]/50 bg-cyan-800 text-[#FFECAB] hover:bg-cyan-600 focus-visible:ring-cyan-500 disabled:opacity-50 cursor-pointer">
                        <SaveAll size={16} className="mr-2" />
                        {isLoading && <Loader2 className="animate-spin h-4 w-4 mr-2" />}
                        {isLoading ? 'Adding Staff...' : 'Save Staff'}
                    </Button>
                </div>
            </form>
        </Modal>
    );
};

export default AddStaffModal;