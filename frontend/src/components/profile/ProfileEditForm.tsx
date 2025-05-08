import { useState, useRef, JSX, ChangeEvent, FormEvent } from 'react';
import { Camera, Loader2, CircleX, SaveAll } from 'lucide-react';
import {  UserProfile,
    OwnerProfile,
    ClinicStaffProfile,
    OwnerProfileUpdateDto,
    UserProfileUpdateDto,
    OwnerProfileUpdateResponseDto,
    ClinicStaffProfileUpdateResponseDto} from '@/types/apiTypes'; 
import { updateCurrentOwnerProfile, updateCurrentClinicStaffProfile } from '@/services/userService'; 
import { Button } from "@/components/ui/button";

interface ProfileEditFormProps {
    userProfile: UserProfile; // The current profile data
    onSaveSuccess: (updatedProfile: UserProfile) => void; // Callback on successful save
    onCancel: () => void; // Callback to cancel editing
}


// Helper type guard
const isClinicStaffProfile = (profile: UserProfile): profile is ClinicStaffProfile => {
    return profile && 'clinicId' in profile;
};
const isOwnerProfile = (profile: UserProfile): profile is OwnerProfile => {
    return profile && 'phone' in profile;
};

/**
 * ProfileEditForm - Form component for editing user profile details.
 * Adapts fields based on user role (Owner vs Staff) and handles API calls for updates.
 * Includes avatar upload functionality.
 *
 * @param {ProfileEditFormProps} props - Component props.
 * @returns {JSX.Element} The profile edit form component.
 */
const ProfileEditForm = ({ userProfile, onSaveSuccess, onCancel }: ProfileEditFormProps): JSX.Element => {

    interface EditFormData {
        username: string;
        phone?: string; 
    }

    // Initialize form data based on the user profile passed in
    const [formData, setFormData] = useState<EditFormData>(() => { 
        if (isOwnerProfile(userProfile)) {
            return { username: userProfile.username || '', phone: userProfile.phone || '' };
        }
        // For ClinicStaff (and fallback), only username is editable in this form
        return { username: userProfile.username || '' };
    });

    const [selectedFile, setSelectedFile] = useState<File | null>(null);
    const [previewUrl, setPreviewUrl] = useState<string | null>(userProfile.avatar);
    const [isLoading, setIsLoading] = useState<boolean>(false);
    const [error, setError] = useState<string>('');
    const fileInputRef = useRef<HTMLInputElement>(null);

    const storedUserJson = sessionStorage.getItem('user') || localStorage.getItem('user');
    const token = storedUserJson ? JSON.parse(storedUserJson).jwt : null;


    const handleChange = (e: ChangeEvent<HTMLInputElement>) => {
        const { name, value } = e.target;
        setFormData(prevState => ({ ...prevState, [name]: value }));
    };

    const handleFileChange = (e: ChangeEvent<HTMLInputElement>) => {
         if (e.target.files && e.target.files[0]) {
             const file = e.target.files[0];
             const allowedTypes = ['image/jpeg', 'image/png', 'image/gif'];
             const maxSize = 1 * 1024 * 1024; // 1MB
             if (!allowedTypes.includes(file.type)) {
                 setError('Invalid file type. Please select a JPG, PNG, or GIF image.');
                 return;
             }
             if (file.size > maxSize) {
                 setError('File size exceeds 1MB limit.');
                 return;
             }

             setError('');
             setSelectedFile(file);
             const reader = new FileReader();
             reader.onloadend = () => { setPreviewUrl(reader.result as string); };
             reader.readAsDataURL(file);
         }
     };

    const triggerFileInput = () => { fileInputRef.current?.click(); };

    const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        if (!token) { setError("Authentication error."); return; }
        setIsLoading(true);
        setError('');

        try {
            let updatedProfile: UserProfile;
            let receivedNewToken: string | null = null;

            if (isOwnerProfile(userProfile)) {
                const ownerUpdateDto: OwnerProfileUpdateDto = {
                    username: formData.username,
                    phone: formData.phone,
                };
                 // La llamada al servicio ahora devuelve OwnerProfileUpdateResponseDto
                const response: OwnerProfileUpdateResponseDto = await updateCurrentOwnerProfile(token, ownerUpdateDto, selectedFile);
                updatedProfile = response.profile;      
                receivedNewToken = response.newToken;   

             } else if (isClinicStaffProfile(userProfile)) {
                 const staffUpdateDto: UserProfileUpdateDto = {
                    username: formData.username,
                 };
                  // La llamada al servicio ahora devuelve ClinicStaffProfileUpdateResponseDto
                 const response: ClinicStaffProfileUpdateResponseDto = await updateCurrentClinicStaffProfile(token, staffUpdateDto, selectedFile);
                 updatedProfile = response.profile;      
                 receivedNewToken = response.newToken;  
             } else {
                 throw new Error("Unknown user profile type for update.");
             }

             console.log('Profile update successful. Received profile:', updatedProfile);
             if (receivedNewToken) {
                 console.log('New JWT token received.');
             }

             const storage = localStorage.getItem('user') ? localStorage : sessionStorage;
             const currentStoredUserJson = storage.getItem('user');
             if (currentStoredUserJson) {
                 try {
                    const currentStoredUser = JSON.parse(currentStoredUserJson);
                     const newStoredUser = {
                         ...currentStoredUser, 
                         username: updatedProfile.username,
                         email: updatedProfile.email, 
                         avatar: updatedProfile.avatar,
                         jwt: receivedNewToken || currentStoredUser.jwt,
                         ...(isOwnerProfile(updatedProfile) && { phone: updatedProfile.phone })
                     };
                     storage.setItem('user', JSON.stringify(newStoredUser));
                     console.log("Local/Session Storage updated successfully.");
                 } catch(storageError) {
                      console.error("Error updating storage after profile save:", storageError);
                 }
             }
            onSaveSuccess(updatedProfile); 

        } catch (err) {
            console.error('Profile update error:', err);
            setError(err instanceof Error ? err.message : 'Failed to update profile.');
        } finally {
            setIsLoading(false); 
        }
    };


    // Determine image to display (preview or existing avatar)
    const displayImage = previewUrl || '/src/assets/images/avatars/users/default_avatar.png';

    return (
        <form onSubmit={handleSubmit} className="space-y-5">
             {/* Avatar Section */}
            <div className="flex flex-col items-center mb-6">
                 <div className="relative">
                    <img
                         src={displayImage}
                         alt="Avatar Preview"
                         className="w-24 h-24 sm:w-28 sm:h-28 rounded-full object-cover border-4 border-[#FFECAB]/60 bg-gray-700"
                         onError={(e) => (e.currentTarget.src = '/src/assets/images/avatars/users/default_avatar.png')}
                     />
                     <button
                         type="button"
                         onClick={triggerFileInput}
                         className="absolute bottom-1 right-1 p-1.5 sm:p-2 bg-cyan-700 text-[#FFECAB] rounded-full hover:bg-cyan-600 transition-colors ring-2 ring-[#0c1225]"
                         aria-label="Change avatar" title="Change image"
                     >
                         <Camera size={16} className="sm:w-5 sm:h-5" /> 
                     </button>
                      <input ref={fileInputRef} type="file" accept="image/*" className="hidden" onChange={handleFileChange} disabled={isLoading} />
                 </div>
             </div>

             {/* Form Fields */}
             <div className="grid grid-cols-1 md:grid-cols-2 gap-x-6 gap-y-4">
                 {/* Username */}
                 <div className="space-y-1">
                     <label htmlFor="username" className="block text-sm font-medium text-gray-300">Username</label>
                     <input id="username" type="text" name="username" required minLength={3} maxLength={50}
                         value={formData.username} onChange={handleChange} disabled={isLoading}
                         className="block w-full px-3 py-2 border border-gray-700 rounded-lg bg-[#070913] text-white placeholder-gray-500 focus:ring-cyan-600 focus:border-cyan-600 disabled:opacity-50"
                     />
                 </div>

                {/* Email*/}
                 <div className="space-y-1">
                     <label htmlFor="email" className="block text-sm font-medium text-gray-300">Email</label>
                     <input id="email" type="email" value={userProfile.email} readOnly disabled
                         className="block w-full px-3 py-2 border border-gray-700 rounded-lg bg-gray-700 text-gray-400 cursor-not-allowed"
                    />
                    <p className="text-xs text-gray-500 mt-1">Email cannot be changed.</p>
                </div>


                 {/* OWNER Specific: Phone */}
                 {isOwnerProfile(userProfile) && (
                     <div className="space-y-1 md:col-span-2">
                          <label htmlFor="phone" className="block text-sm font-medium text-gray-300">Phone Number</label>
                         <input id="phone" type="tel" name="phone" maxLength={20}
                            value={formData.phone || ''} onChange={handleChange} disabled={isLoading} placeholder='+xx xxxx xxx'
                           className="block w-full px-3 py-2 border border-gray-700 rounded-lg bg-[#070913] text-white placeholder-gray-500 focus:ring-cyan-600 focus:border-cyan-600 disabled:opacity-50"
                        />
                    </div>
                 )}

                {/* STAFF Specific: Read-only fields */}
                {isClinicStaffProfile(userProfile) && (
                    <>
                        {/* ... campos read-only para staff ... */}
                         <div className="space-y-1">
                             <label className="block text-sm font-medium text-gray-300">First Name</label>
                             <p className="px-3 py-2 text-gray-400">{userProfile.name}</p>
                         </div>
                          <div className="space-y-1">
                             <label className="block text-sm font-medium text-gray-300">Surname</label>
                             <p className="px-3 py-2 text-gray-400">{userProfile.surname}</p>
                         </div>
                    </>
                )}

             </div>

             {/* Error Display */}
             {error && ( <p className="mt-3 text-sm text-red-400 text-center">{error}</p> )}

             {/* Action Buttons */}
            <div className="flex justify-end gap-4 pt-4 border-t border-[#FFECAB]/20 mt-6">
                <Button
                    type="button"
                    onClick={onCancel}
                    disabled={isLoading}
                    className="px-5 py-2.5 rounded-lg border border-[#FFECAB]/50 text-[#FFECAB] hover:bg-red-800 hover:text-[#FFECAB] focus-visible:ring-red-500 disabled:opacity-50 cursor-pointer">
                        <CircleX size={16} className="mr-2"  />
                    Cancel
                </Button>
                <Button
                    type="submit"
                    disabled={isLoading}
                    className="px-5 py-2.5 rounded-lg border border-[#FFECAB]/50 bg-cyan-800 text-[#FFECAB] hover:bg-cyan-600 focus-visible:ring-cyan-500 disabled:opacity-50 cursor-pointer"
                >
                    <SaveAll size={16} className="mr-2" />
                    {isLoading && <Loader2 className="animate-spin h-4 w-4 mr-2" />}
                    {isLoading ? 'Saving...' : 'Save Changes'}
                </Button>
            </div>
        </form>
    );
};

export default ProfileEditForm;