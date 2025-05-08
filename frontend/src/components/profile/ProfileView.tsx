import { JSX } from 'react';
import { UserProfile, ClinicStaffProfile } from '@/types/apiTypes'; 
import { Edit, Mail, Phone, Building, UserCheck, KeySquare, Hash } from 'lucide-react';
import { Button } from "@/components/ui/button";

interface ProfileViewProps {
    userProfile: UserProfile;
    onEdit: () => void;
}

/**
 * Displays user profile information in a read-only format.
 * Differentiates display based on user role (Owner vs Clinic Staff).
 * Provides a button to switch to edit mode.
 *
 * @param {ProfileViewProps} props - Component props.
 * @returns {JSX.Element} The profile view component.
 */
const ProfileView = ({ userProfile, onEdit }: ProfileViewProps): JSX.Element => {

    // Helper function to determine if the profile is for Clinic Staff
    const isClinicStaff = (profile: UserProfile): profile is ClinicStaffProfile => {
        return profile && 'clinicId' in profile; // Check for a field unique to ClinicStaffProfile
    };

    // Helper to format roles
    const formatRoles = (roles: string[] | undefined): string => {
         if (!roles || roles.length === 0) return 'N/A';
         return roles.map(role => role.charAt(0) + role.slice(1).toLowerCase()).join(', ');
    };

    return (
        <div className="space-y-5">
            {/* Avatar and Basic Info */}
            <div className="flex flex-col sm:flex-row items-center sm:items-start gap-4 sm:gap-6 mb-6">
                <div className="flex-shrink-0">
                    <img
                        src={userProfile.avatar || '/src/assets/images/avatars/users/default_avatar.png'}
                        alt={`${userProfile.username}'s avatar`}
                        className="w-24 h-24 sm:w-28 sm:h-28 rounded-full object-cover border-4 border-[#FFECAB]/60 bg-gray-700"
                        onError={(e) => (e.currentTarget.src = '/src/assets/images/avatars/users/default_avatar.png')}
                    />
                </div>
                <div className="text-center sm:text-left pt-2">
                     <h3 className="text-2xl font-semibold text-white">{userProfile.username}</h3>
                    <p className="text-cyan-400">{formatRoles(userProfile.roles)}</p>

                     {/* Display Name for Clinic Staff */}
                     {isClinicStaff(userProfile) && (
                        <p className="text-lg text-gray-300 mt-1">{userProfile.name} {userProfile.surname}</p>
                     )}
                     {/* Email */}
                    <div className="flex items-center justify-center sm:justify-start gap-1.5 mt-2 text-sm text-gray-400">
                         <Mail size={14} />
                         <span>{userProfile.email}</span>
                    </div>
                    {/* Phone (Owner only) */}
                    {!isClinicStaff(userProfile) && (
                          <div className="flex items-center justify-center sm:justify-start gap-1.5 mt-1 text-sm text-gray-400">
                             <Phone size={14} />
                             <span>{userProfile.phone}</span>
                         </div>
                     )}
                 </div>
            </div>

             <hr className="border-[#FFECAB]/20" />

             {/* Additional Details - Clinic Staff */}
             {isClinicStaff(userProfile) && (
                 <div className="grid grid-cols-1 md:grid-cols-2 gap-x-8 gap-y-4 text-sm">
                     <div className="flex items-start gap-2">
                         <Building size={16} className="text-gray-500 mt-0.5 flex-shrink-0" />
                         <div>
                             <span className="font-medium text-gray-400 block">Clinic:</span>
                             <span className="text-white">{userProfile.clinicName}</span>
                         </div>
                      </div>
                      <div className="flex items-start gap-2">
                           <UserCheck size={16} className="text-gray-500 mt-0.5 flex-shrink-0" />
                          <div>
                              <span className="font-medium text-gray-400 block">Status:</span>
                               <span className={`font-medium ${userProfile.isActive ? 'text-green-400' : 'text-red-400'}`}>
                                  {userProfile.isActive ? 'Active' : 'Inactive'}
                              </span>
                           </div>
                      </div>
                      {/* Vet Specific */}
                      {userProfile.licenseNumber && (
                           <div className="flex items-start gap-2">
                                <KeySquare size={16} className="text-gray-500 mt-0.5 flex-shrink-0" />
                               <div>
                                   <span className="font-medium text-gray-400 block">License No:</span>
                                   <span className="text-white break-all">{userProfile.licenseNumber}</span>
                               </div>
                            </div>
                      )}
                      {userProfile.vetPublicKey && (
                         <div className="flex items-start gap-2 md:col-span-2"> {/* Public key might be long */}
                             <Hash size={16} className="text-gray-500 mt-0.5 flex-shrink-0" />
                             <div>
                                 <span className="font-medium text-gray-400 block">Public Key:</span>
                                  <span className="text-white text-xs break-all">{userProfile.vetPublicKey}</span>
                             </div>
                          </div>
                      )}
                 </div>
             )}

            {/* Edit Button */}
            <div className="mt-6 pt-4 border-t border-[#FFECAB]/20 flex justify-end">
            <Button
                    onClick={onEdit}
                    className="px-5 py-2.5 rounded-lg border border-[#FFECAB]/50 bg-cyan-800 text-[#FFECAB] hover:bg-cyan-600 focus-visible:ring-cyan-500 disabled:opacity-50 cursor-pointer"
                >
                    <Edit size={16} className="mr-2" />
                    Edit Profile
                </Button>
            </div>
        </div>
    );
};

export default ProfileView;