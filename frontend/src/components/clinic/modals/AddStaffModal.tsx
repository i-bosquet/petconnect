import { useState, FormEvent, ChangeEvent, JSX } from 'react'; 
import Modal from '@/components/common/Modal';
import { Lock, Mail, User as UserIcon, Briefcase, KeySquare, Hash, Loader2 } from 'lucide-react';
import { RoleEnum } from '@/types/enumTypes';
import { ClinicStaffCreationPayload, createClinicStaff } from '@/services/clinicStaffService';
import { useAuth } from '@/hooks/useAuth'; 
import { Button } from "@/components/ui/button";

interface AddStaffModalProps {
    isOpen: boolean;
    onClose: () => void;
    onStaffAdded: () => void;
    clinicId: number | string; 
}

const initialFormData: Omit<ClinicStaffCreationPayload, 'clinicId'> = {
    username: '',
    email: '',
    password: '',
    name: '',
    surname: '',
    role: RoleEnum.VET, 
    licenseNumber: '',
    vetPublicKey: ''
};

/**
 * AddStaffModal - Modal for Admins to create new Clinic Staff (Vets or Admins).
 *
 * @param {AddStaffModalProps} props - Component props.
 * @returns {JSX.Element | null} The modal component or null if not open.
 */
const AddStaffModal = ({ isOpen, onClose, onStaffAdded }: AddStaffModalProps): JSX.Element | null => {
    const { token } = useAuth();
    const [formData, setFormData] = useState<Omit<ClinicStaffCreationPayload, 'clinicId'>>(initialFormData);
    const [confirmPassword, setConfirmPassword] = useState<string>('');
    const [error, setError] = useState<string>('');
    const [isLoading, setIsLoading] = useState<boolean>(false);

    const handleChange = (e: ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) => {
        const { name, value } = e.target;
        setFormData(prev => ({ ...prev, [name]: value }));
        if (name === "role" && value === RoleEnum.ADMIN.toString()) { 
            setFormData(prev => ({ ...prev, licenseNumber: '', vetPublicKey: ''}));
        }
    };

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
        if (formData.role === RoleEnum.VET && (!formData.licenseNumber || !formData.vetPublicKey)) {
            setError("License Number and Public Key are required for VET role.");
            return false;
        }
        return true;
    };

    const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        setError('');
        if (!validateForm()) return;
        if (!token) { setError("Authentication error."); return; }

        setIsLoading(true);
        try {
            const payload: ClinicStaffCreationPayload = {
                ...formData,
                licenseNumber: formData.role === RoleEnum.VET ? formData.licenseNumber : null,
                vetPublicKey: formData.role === RoleEnum.VET ? formData.vetPublicKey : null,
            };
            await createClinicStaff(token, payload);
            onStaffAdded(); 
        } catch (err) {
            console.error("Failed to add staff:", err);
            setError(err instanceof Error ? err.message : 'Could not add staff member.');
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
                            <input id="staffPassword" name="password" type="password" required value={formData.password} onChange={handleChange} disabled={isLoading}
                                   className="block w-full pl-10 pr-3 py-2.5 border border-gray-700 rounded-xl bg-[#070913] text-white placeholder-gray-500 focus:ring-cyan-600 focus:border-cyan-600"/>
                        </div>
                    </div>
                    {/* Confirm Password */}
                    <div className="space-y-1">
                        <label htmlFor="staffConfirmPassword" className="block text-sm font-medium text-gray-300">Confirm Password *</label>
                        <div className="relative">
                             <Lock size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
                            <input id="staffConfirmPassword" name="confirmPassword" type="password" required value={confirmPassword} onChange={(e) => setConfirmPassword(e.target.value)} disabled={isLoading}
                                   className="block w-full pl-10 pr-3 py-2.5 border border-gray-700 rounded-xl bg-[#070913] text-white placeholder-gray-500 focus:ring-cyan-600 focus:border-cyan-600"/>
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
                                <KeySquare size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
                                <input id="licenseNumber" name="licenseNumber" type="text" value={formData.licenseNumber || ''} onChange={handleChange} disabled={isLoading} required={formData.role === RoleEnum.VET}
                                       className="block w-full pl-10 pr-3 py-2.5 border border-gray-700 rounded-xl bg-[#070913] text-white placeholder-gray-500 focus:ring-cyan-600 focus:border-cyan-600"/>
                            </div>
                        </div>
                        {/* Vet Public Key */}
                        <div className="space-y-1">
                             <label htmlFor="vetPublicKey" className="block text-sm font-medium text-gray-300">Public Key * <span className="text-xs text-gray-400">(PEM format)</span></label>
                             <div className="relative">
                                 <Hash size={16} className="absolute left-3 top-3 text-gray-400" /> 
                                <textarea 
                                    id="vetPublicKey" 
                                    name="vetPublicKey" 
                                    value={formData.vetPublicKey || ''} 
                                    onChange={handleChange} 
                                    disabled={isLoading} 
                                    required={formData.role === RoleEnum.VET}
                                    rows={3}
                                    className="block w-full pl-10 pr-3 py-2.5 border border-gray-700 rounded-xl bg-[#070913] text-white placeholder-gray-500 focus:ring-cyan-600 focus:border-cyan-600 custom-scrollbar-dark"
                                    placeholder="-----BEGIN PUBLIC KEY-----...-----END PUBLIC KEY-----"
                                />
                             </div>
                        </div>
                    </div>
                )}

                {/* Action Buttons */}
                <div className="flex justify-end gap-4 pt-5">
                    <Button type="button" onClick={onClose} disabled={isLoading}
                            className="px-5 py-2.5 rounded-lg border border-[#FFECAB]/50 text-[#FFECAB] hover:bg-red-800 transition-colors disabled:opacity-50"
                            >
                                Cancel
                    </Button>
                    <Button type="submit" disabled={isLoading} className="border border-[#FFECAB]/50 bg-cyan-800 hover:bg-cyan-600 text-[#FFECAB]">
                        {isLoading && <Loader2 className="animate-spin h-4 w-4 mr-2" />}
                        {isLoading ? 'Adding Staff...' : 'Add Staff'}
                    </Button>
                </div>
            </form>
        </Modal>
    );
};

export default AddStaffModal;