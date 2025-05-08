// frontend/src/components/clinic/staff/StaffEditForm.tsx
import { useState, useRef, useEffect, ChangeEvent, FormEvent, JSX } from 'react';
import { Loader2, User as KeySquare, Upload, CircleX, SaveAll } from 'lucide-react';
import { ClinicStaffProfile, ClinicStaffUpdatePayload } from '@/types/apiTypes'; 
import { RoleEnum } from '@/types/enumTypes';
import { updateClinicStaff } from '@/services/clinicStaffService';
import { useAuth } from '@/hooks/useAuth';
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { Label } from "@/components/ui/label";

interface StaffEditFormProps {
    staffProfile: ClinicStaffProfile;
    onSaveSuccess: (updatedProfile: ClinicStaffProfile) => void;
    onCancel: () => void;
}

interface StaffEditFormData {
    name: string;
    surname: string;
    roles: RoleEnum[]; 
    licenseNumber: string | null;
}

/**
 * StaffEditForm - Form component for Admins to edit Clinic Staff details.
 * Allows editing name, surname, roles, and Vet-specific fields.
 */
const StaffEditForm = ({ staffProfile, onSaveSuccess, onCancel }: StaffEditFormProps): JSX.Element => {
    const { token } = useAuth();
    const [formData, setFormData] = useState<StaffEditFormData>({
        name: staffProfile.name || '',
        surname: staffProfile.surname || '',
        roles: staffProfile.roles.map(roleStr => RoleEnum[roleStr as keyof typeof RoleEnum]).filter(Boolean),
        licenseNumber: staffProfile.licenseNumber || '',
    });
    const [selectedPublicKeyFile, setSelectedPublicKeyFile] = useState<File | null>(null);
    const publicKeyFileInputRef = useRef<HTMLInputElement>(null);
    const [isLoading, setIsLoading] = useState<boolean>(false);
    const [error, setError] = useState<string>('');

    // Reset form when the initial profile changes
    useEffect(() => {
        setFormData({
            name: staffProfile.name || '',
            surname: staffProfile.surname || '',
            roles: staffProfile.roles.map(roleStr => RoleEnum[roleStr as keyof typeof RoleEnum]).filter(Boolean),
            licenseNumber: staffProfile.licenseNumber || '',
        });
        setSelectedPublicKeyFile(null);
        setError('');
    }, [staffProfile]);

    const handleChange = (e: ChangeEvent<HTMLInputElement>) => {
        const { name, value } = e.target;
        setFormData(prev => ({ ...prev, [name]: value }));
    };

    const handleRoleChange = (role: RoleEnum, checked: boolean) => {
        setFormData(prev => {
            const currentRoles = new Set(prev.roles);
            if (checked) {
                currentRoles.add(role);
            } else {
                if (currentRoles.size > 1) {
                    currentRoles.delete(role);
                } else {
                    console.warn("Cannot remove the last role.");
                    return prev;
                }
            }
            const newRoles = Array.from(currentRoles);
            // If we remove the VET role, clear the license from the form status
            if (role === RoleEnum.VET && !newRoles.includes(RoleEnum.VET)) {
                return { ...prev, roles: newRoles, licenseNumber: '' };
            }
            return { ...prev, roles: newRoles };
        });
         // If we remove the VET role, reset selected file
        if (role === RoleEnum.VET && !formData.roles.includes(RoleEnum.VET)) {
            setSelectedPublicKeyFile(null);
            if (publicKeyFileInputRef.current) {
                publicKeyFileInputRef.current.value = '';
            }
        }
    };

    const handlePublicKeyFileChange = (e: ChangeEvent<HTMLInputElement>) => {
        if (e.target.files && e.target.files[0]) {
            const file = e.target.files[0];
            const allowedTypes = ['application/x-x509-ca-cert', 'application/pkix-cert', 'application/x-pem-file', 'text/plain'];
            const allowedExtensions = ['.pem', '.crt'];
            const fileExtension = file.name.substring(file.name.lastIndexOf('.')).toLowerCase();

            if (allowedTypes.includes(file.type) || allowedExtensions.includes(fileExtension)) {
                setSelectedPublicKeyFile(file);
                setError('');
            } else {
                setSelectedPublicKeyFile(null);
                e.target.value = '';
                setError('Invalid file type. Please select a .pem or .crt file.');
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
        setError('');
        if (!token) { setError("Authentication error."); return; }

        const isVet = formData.roles.includes(RoleEnum.VET);
        if (isVet && !formData.licenseNumber) {
            setError("License Number is required if VET role is selected.");
            return;
        }
        // If it is VET and there is NO new file and there was NOT one before EITHER -> Error
        if (isVet && !selectedPublicKeyFile && !staffProfile.vetPublicKey) {
            setError("A public key file must be provided or already exist for VET role.");
            return;
        }

        setIsLoading(true);
        try {
            const payload: ClinicStaffUpdatePayload = {
                name: formData.name,
                surname: formData.surname,
                roles: formData.roles.map(role => role.toString()), 
                licenseNumber: isVet ? formData.licenseNumber : null,
            };

            const updatedProfile = await updateClinicStaff(token, staffProfile.id, payload, selectedPublicKeyFile);
            onSaveSuccess(updatedProfile);

        } catch (err) {
            console.error("Failed to update staff:", err);
            setError(err instanceof Error ? err.message : 'Could not update staff member.');
        } finally {
            setIsLoading(false);
        }
    };

    const isCurrentRoleVet = formData.roles.includes(RoleEnum.VET);

    return (
        <form onSubmit={handleSubmit} className="space-y-5">
            {error && ( <div className="p-3 bg-red-900/30 text-red-300 rounded-lg text-sm text-center">{error}</div> )}

            {/* Avatar and non-editable data section */}
            <div className="flex items-center gap-4 mb-4 pb-4 border-b border-[#FFECAB]/20">
                <img
                    src={staffProfile.avatar || '/src/assets/images/avatars/users/default_avatar.png'}
                    alt={`${staffProfile.username}'s avatar`}
                    className="w-16 h-16 rounded-full object-cover border-2 border-[#FFECAB]/50 bg-gray-700"
                    onError={(e) => (e.currentTarget.src = '/src/assets/images/avatars/users/default_avatar.png')}
                />
                <div>
                    <p className="font-semibold text-white">{staffProfile.username}</p>
                    <p className="text-sm text-gray-400">{staffProfile.email}</p>
                </div>
            </div>

            {/* Editable Fields */}
             <div className="grid grid-cols-1 md:grid-cols-2 gap-x-6 gap-y-4">
                 {/* Name */}
                 <div className="space-y-1">
                     <Label htmlFor="staffEditName" className="text-gray-300">First Name *</Label>
                     <input id="staffEditName" name="name" type="text" required value={formData.name} onChange={handleChange} disabled={isLoading}
                            className="block w-full px-3 py-2 border border-gray-700 rounded-lg bg-[#070913] text-white placeholder-gray-500 focus:ring-cyan-600 focus:border-cyan-600 disabled:opacity-50"/>
                 </div>

                 {/* Surname */}
                 <div className="space-y-1">
                     <Label htmlFor="staffEditSurname" className="text-gray-300">Surname *</Label>
                     <input id="staffEditSurname" name="surname" type="text" required value={formData.surname} onChange={handleChange} disabled={isLoading}
                            className="block w-full px-3 py-2 border border-gray-700 rounded-lg bg-[#070913] text-white placeholder-gray-500 focus:ring-cyan-600 focus:border-cyan-600 disabled:opacity-50"/>
                 </div>

                {/* Roles Checkboxes */}
                <div className="space-y-2 md:col-span-2">
                      <Label className="block text-sm font-medium text-gray-300">Roles *</Label>
                      <div className="flex items-center flex-wrap gap-x-6 gap-y-2 pt-1">
                          {(Object.keys(RoleEnum) as Array<keyof typeof RoleEnum>)
                             .filter(key => RoleEnum[key] === RoleEnum.ADMIN || RoleEnum[key] === RoleEnum.VET)
                             .map((key) => {
                                 const currentRoleEnum = RoleEnum[key];
                                 return (
                                     <div key={key} className="flex items-center">
                                         <Checkbox
                                             id={`edit-role-${key}`}
                                             checked={formData.roles.includes(currentRoleEnum)}
                                             onCheckedChange={(checked) => handleRoleChange(currentRoleEnum, !!checked)}
                                             disabled={isLoading || (formData.roles.length === 1 && formData.roles[0] === currentRoleEnum)}
                                             className="data-[state=checked]:bg-cyan-600 data-[state=checked]:text-white border-gray-500 mr-2"
                                         />
                                         <Label htmlFor={`edit-role-${key}`} className="text-sm font-medium text-gray-300 cursor-pointer">
                                             {key}
                                         </Label>
                                      </div>
                                 );
                             })}
                      </div>
                       {formData.roles.length === 0 && <p className="text-xs text-red-400">At least one role (ADMIN or VET) must be selected.</p>}
                       {formData.roles.length === 1 && <p className="text-xs text-gray-500">Cannot remove the last role.</p>}
                </div>
              </div>
    
              {/* VET specific fields */} 
              {isCurrentRoleVet && (
                  <div className="mt-4 pt-4 border-t border-[#FFECAB]/20 space-y-4">
                      <h4 className="text-md font-semibold text-[#FFECAB]">Veterinarian Details (Edit)</h4>
                      <div className="space-y-1">
                           <Label htmlFor="editLicenseNumber" className="text-gray-300">License Number *</Label>
                          <div className="relative">
                              <KeySquare size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
                               <input id="editLicenseNumber" name="licenseNumber" type="text" value={formData.licenseNumber || ''} onChange={handleChange} disabled={isLoading} required={isCurrentRoleVet}
                                     className="block w-full pl-10 pr-3 py-2.5 border border-gray-700 rounded-xl bg-[#070913] text-white placeholder-gray-500 focus:ring-cyan-600 focus:border-cyan-600 disabled:opacity-50"/>
                          </div>
                      </div>
                       <div className="space-y-1">
                           <Label htmlFor="editVetPublicKeyFile" className="text-gray-300">Public Key File (.pem/.crt) {staffProfile.vetPublicKey ? '(Optional: Upload to replace)' : '*'}</Label>
                            <p className="text-xs text-gray-400 mb-1">Current Path: {staffProfile.vetPublicKey || 'None'}</p>
                           <div className="mt-1 flex items-center gap-2">
                              <Button type="button"  onClick={triggerPublicKeyFileInput} disabled={isLoading} size="sm" className="border-[#FFECAB]/50 text-sm px-3 py-1.5 text-[#FFECAB]  hover:text-cyan-800 hover:bg-gray-300 border cursor-pointer">
                              <Upload size={16} className="mr-2"/>
                                  {selectedPublicKeyFile ? "Change File" : "Select New File"}
                               </Button>
                              <input id="editVetPublicKeyFile" type="file" ref={publicKeyFileInputRef} className="hidden" accept=".pem,.crt,application/x-x509-ca-cert,application/pkix-cert" onChange={handlePublicKeyFileChange} disabled={isLoading}/>
                               {selectedPublicKeyFile && <span className="text-xs text-gray-400 truncate max-w-xs">{selectedPublicKeyFile.name}</span>}
                               {!selectedPublicKeyFile && staffProfile.vetPublicKey && <span className="text-xs text-gray-500">Keeping existing key.</span>}
                               {!selectedPublicKeyFile && !staffProfile.vetPublicKey && <span className="text-xs text-red-400">New file required if VET role selected.</span>}
                           </div>
                       </div>
                  </div>
              )}

    
              {/* Action Buttons */}
              <div className="flex justify-end gap-4 pt-5 border-t border-[#FFECAB]/20 mt-6">
                   <Button type="button" onClick={onCancel} disabled={isLoading} className="px-5 py-2.5 rounded-lg border border-[#FFECAB]/50 text-[#FFECAB] hover:bg-red-800 hover:text-[#FFECAB] focus-visible:ring-red-500 disabled:opacity-50 cursor-pointer">
                   <CircleX size={16} className="mr-2"  />
                      Cancel
                  </Button>
                  <Button type="submit" disabled={isLoading} className="px-5 py-2.5 rounded-lg border border-[#FFECAB]/50 bg-cyan-800 text-[#FFECAB] hover:bg-cyan-600 focus-visible:ring-cyan-500 disabled:opacity-50 cursor-pointer">
                  <SaveAll size={16} className="mr-2" />
                      {isLoading && <Loader2 className="animate-spin h-4 w-4 mr-2" />}
                      {isLoading ? 'Saving...' : 'Save Changes'}
                  </Button>
              </div>
         </form>
     );
 };
 export default StaffEditForm;