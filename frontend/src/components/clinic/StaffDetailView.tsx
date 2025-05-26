import { JSX } from 'react';
import { ClinicStaffProfile } from '@/types/apiTypes';
import { formatDateTime } from '@/utils/formatters';
import {  Mail, Building, UserCheck, KeySquare, Hash, Calendar, Clock, UserCog, Edit } from 'lucide-react';
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import defaultAvatar from '@/assets/images/default_avatar.png';

interface StaffDetailViewProps {
    staffProfile: ClinicStaffProfile;
    onEdit: () => void;
}

/**
 * StaffDetailView - Displays full details of a staff member for an Admin.
 */
const StaffDetailView = ({ staffProfile, onEdit }: StaffDetailViewProps): JSX.Element => {

   

     const formatRoles = (roles: string[] | undefined): JSX.Element => {
         if (!roles || roles.length === 0) return <Badge variant="outline">No Roles</Badge>;
         return (
             <div className="flex flex-wrap gap-1">
                 {roles.map(role => (
                      <Badge key={role} variant={role === 'ADMIN' ? 'default' : 'secondary'}
                             className={`px-2 py-0.5 text-xs font-semibold rounded-full ${
                                 role === 'ADMIN' ? 'bg-cyan-600 text-cyan-50' : 'bg-purple-600 text-purple-50'
                             }`}>
                         {role}
                     </Badge>
                 ))}
             </div>
         );
    };

    return (
        <div className="space-y-5">
            {/* Top Section: Avatar, Name, Roles, Status */}
             <div className="flex flex-col sm:flex-row items-center sm:items-start gap-4 sm:gap-6 mb-4 pb-4 border-b border-[#FFECAB]/20">
             <div className="flex-shrink-0">
             <img
                src={staffProfile.avatar || defaultAvatar}
                alt={`${staffProfile.username}'s avatar`}
                className="w-24 h-24 sm:w-28 sm:h-28 rounded-full object-cover border-4 border-[#FFECAB]/60 bg-gray-700"
                onError={(e) => (e.currentTarget.src = defaultAvatar)}
             />
             </div>
             <div className="text-center sm:text-left flex-grow">
                 <h3 className="text-2xl font-semibold text-white">{staffProfile.name} {staffProfile.surname}</h3>
                 <p className="text-cyan-400">@{staffProfile.username}</p>
                 <div className="mt-2">{formatRoles(staffProfile.roles)}</div>
                  <div className="flex items-center justify-center sm:justify-start gap-1.5 mt-1 text-sm">
                  <UserCheck size={16} className={staffProfile.isActive ? 'text-green-500' : 'text-red-500'} />
                  <span className={staffProfile.isActive ? 'text-green-400' : 'text-red-400'}>
                      {staffProfile.isActive ? 'Active' : 'Inactive'}
                  </span>
                  </div>
             </div>
             {/* Edit Button visible only in view mode */}
             <Button onClick={onEdit} size="sm" className=" px-5 py-2.5 rounded-lg border border-[#FFECAB]/50 bg-cyan-800 text-[#FFECAB] hover:bg-cyan-600 focus-visible:ring-cyan-500 disabled:opacity-50 cursor-pointer">
                  <Edit size={16} className="mr-2" /> Edit Staff
             </Button>
             </div>

             {/* Additional Details */}
             <div className="grid grid-cols-1 md:grid-cols-2 gap-x-8 gap-y-4 text-sm">
             {/* Left Column */}
             <div className="space-y-3">
                  <div className="flex items-start gap-2"> <Mail size={16} className="text-gray-500 mt-0.5"/> <div><span className="font-medium text-gray-400 block">Email:</span> <span className="text-white">{staffProfile.email}</span></div> </div>
                  <div className="flex items-start gap-2"> <Building size={16} className="text-gray-500 mt-0.5"/> <div><span className="font-medium text-gray-400 block">Clinic:</span> <span className="text-white">{staffProfile.clinicName}</span></div> </div>
                  {/* Vet Fields */}
                  {staffProfile.licenseNumber && ( <div className="flex items-start gap-2"> <KeySquare size={16} className="text-gray-500 mt-0.5"/> <div><span className="font-medium text-gray-400 block">License No:</span> <span className="text-white break-all">{staffProfile.licenseNumber}</span></div> </div> )}
                  {staffProfile.vetPublicKey && ( <div className="flex items-start gap-2"> <Hash size={16} className="text-gray-500 mt-0.5"/> <div><span className="font-medium text-gray-400 block">Public Key Path:</span> <span className="text-white text-xs break-all">{staffProfile.vetPublicKey}</span></div> </div> )}
             </div>
             {/* Right Column - Audit Info */}
             <div className="space-y-3 border-t md:border-t-0 md:border-l border-[#FFECAB]/20 pt-3 md:pt-0 md:pl-6">
                 <h4 className="font-medium text-gray-400 mb-2">Audit Info</h4>
                 <div className="flex items-start gap-2"> <Calendar size={16} className="text-gray-500 mt-0.5"/> <div><span className="font-medium text-gray-400 block">Created At:</span> <span className="text-white">{formatDateTime(staffProfile.createdAt)}</span></div> </div>
                 <div className="flex items-start gap-2"> <UserCog size={16} className="text-gray-500 mt-0.5"/> <div><span className="font-medium text-gray-400 block">Created By:</span> <span className="text-white">{staffProfile.createdBy || 'N/A'}</span></div> </div>
                 <div className="flex items-start gap-2"> <Clock size={16} className="text-gray-500 mt-0.5"/> <div><span className="font-medium text-gray-400 block">Last Updated:</span> <span className="text-white">{formatDateTime(staffProfile.updatedAt)}</span></div> </div>
                 <div className="flex items-start gap-2"> <UserCog size={16} className="text-gray-500 mt-0.5"/> <div><span className="font-medium text-gray-400 block">Updated By:</span> <span className="text-white">{staffProfile.updatedBy || 'N/A'}</span></div> </div>
             </div>
             </div>
        </div>
    );
};
export default StaffDetailView;