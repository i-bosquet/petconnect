import React, { JSX } from 'react';
import { Link, useLocation } from 'react-router-dom';
import {
  LayoutDashboard,
  User,
  Users,
  LogOut,
  Edit,
  Building,
  PawPrint,
  ClipboardList,
  ScrollText,
  X as CloseIcon 
} from 'lucide-react';
import { ClinicStaffProfile } from '@/types/apiTypes';
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip";
import { Button } from '@/components/ui/button';
import logoImageL from '@/assets/images/SF-Logo1-L.png';
import defaultAvatar from '@/assets/images/default_avatar.png';

interface ClinicSidebarProps {
    closeMobileMenu: () => void;
    currentStaff: ClinicStaffProfile;
    handleLogout: () => void;
    onOpenProfileModal: () => void;
}

interface MenuItem {
    id: string;
    label: string;
    icon: React.ElementType;
    path: string;
    adminOnly?: boolean; 
    vetOrAdmin?: boolean; 
}

/**
 * ClinicSidebar - Navigation sidebar for the clinic portal.
 * Displays menu items, user info, and logout. Triggers profile modal opening.
 *
 * @param {ClinicSidebarProps} props - Component props.
 * @returns {JSX.Element} The sidebar navigation component.
 */
const ClinicSidebar = ({
    closeMobileMenu,
    currentStaff,
    handleLogout,
    onOpenProfileModal 
}: ClinicSidebarProps): JSX.Element => {
    const location = useLocation();

    const menuItems: MenuItem[] = [
        { id: 'dashboard', label: 'Dashboard', icon: LayoutDashboard, path: '/clinic/dashboard' },
        { id: 'pet_management', label: 'Pet Management', icon: PawPrint, path: '/clinic/pets', vetOrAdmin: true },
        { id: 'record_management', label: 'Medical Records', icon: ClipboardList, path: '/clinic/records', vetOrAdmin: true },
        { id: 'certificate_management', label: 'Certificates', icon: ScrollText, path: '/clinic/certificates', vetOrAdmin: true },
        { id: 'staff_management', label: 'Staff Management', icon: Users, path: '/clinic/staff', adminOnly: true },
        { id: 'settings', label: 'Clinic Info', icon: Building, path: '/clinic/settings',  vetOrAdmin: true },
    ];

    const clinicName = currentStaff.clinicName || "Clinic Portal";

    return (
        <div className="h-full w-full bg-[#FFECAB] text-[#090D1A] p-4 flex flex-col shadow-xl">
            {/* Logo and close button */}
            <div className="flex justify-between items-center mb-6 pb-4 border-b border-[#090D1A]/20">
                <Link to="/clinic/dashboard" className="flex items-center gap-2" onClick={closeMobileMenu}>
                    <img
                        src={logoImageL}
                        alt="PetConnect"
                        className="w-10 h-10"
                    />
                    <div>
                        <h2 className="text-xl font-bold block text-[#090D1A]">PetConnect</h2>
                        <h4 className="font-semibold text-sm block text-cyan-800" title={clinicName}>
                            {clinicName}
                        </h4>
                    </div>
                </Link>
                 <Button 
                    size="icon"
                    onClick={closeMobileMenu}
                    className="lg:hidden bg-transparent font-bold text-[#090D1A] hover:text-[#FFECAB] hover:bg-[#090D1A] cursor-pointer" 
                    aria-label="Close navigation menu"
                >
                    <CloseIcon  size={24} />
                </Button>
            </div>

            {/* Navigation Items */}
            <nav className="space-y-1.5 flex-grow custom-scrollbar">
                {menuItems.map((item) => {
                    if (item.adminOnly && !currentStaff.roles.includes('ADMIN')) {
                        return null;
                    }
                    const IconComponent = item.icon;
                    const isActive = location.pathname.startsWith(item.path);
                    return (
                        <Link
                            key={item.id}
                            to={item.path}
                            onClick={closeMobileMenu}
                            className={`flex items-center gap-3 w-full p-2.5 rounded-lg transition-all duration-200 ease-in-out
                                ${isActive
                                    ? 'bg-[#090D1A] text-[#FFECAB] shadow-md scale-[1.02]'
                                    : 'text-[#090D1A]/80 hover:bg-[#090D1A]/10 hover:text-[#090D1A] hover:scale-[1.01]'
                                } `}
                        >
                            <IconComponent size={20} className={isActive ? "text-[#FFECAB]" : "text-cyan-800"}/>
                            <span>{item.label}</span>
                        </Link>
                    );
                })}
            </nav>

            {/* User info and logout */}
            <div className="mt-auto pt-4 border-t border-[#090D1A]/20">
                 <div className="group relative p-3 rounded-lg hover:bg-[#090D1A]/10 transition-colors cursor-pointer" onClick={onOpenProfileModal}> 
                     <div className="flex items-center gap-3 mb-1">
                         <div className="w-10 h-10 rounded-full bg-gray-300/70 flex items-center justify-center overflow-hidden border-2 border-[#090D1A]/50">
                             {currentStaff.avatar ? (
                                <img src={currentStaff.avatar} alt="Staff Avatar" className="w-full h-full object-cover" onError={(e) => (e.currentTarget.src = defaultAvatar)}/>
                            ) : (
                                <User size={22} className="text-[#090D1A]/70" />
                            )}
                        </div>
                        <div>
                            <p className="text-sm font-semibold text-[#090D1A] truncate" title={`${currentStaff.name} ${currentStaff.surname}`}>
                                {currentStaff.name} {currentStaff.surname}
                            </p>
                            <p className="text-xs text-cyan-800">{currentStaff.roles.join(', ')}</p>
                        </div>
                    </div>
                     <Tooltip>
                        <TooltipTrigger asChild>
                            <div className="absolute top-1 right-1 p-1 opacity-0 group-hover:opacity-100 transition-opacity"
                                 aria-label="Edit profile">
                                <Edit size={16} className="text-[#090D1A]/70" />
                            </div>
                        </TooltipTrigger>
                        <TooltipContent side="top" className="bg-gray-800 text-white border border-cyan-700">
                            <p>View/Edit Profile</p>
                        </TooltipContent>
                    </Tooltip>
                </div>

                <button
                    onClick={() => { handleLogout(); closeMobileMenu(); }} 
                    className="flex items-center justify-center gap-2 w-full mt-2 px-3 py-2 bg-cyan-800 text-[#FFECAB]  font-medium rounded-lg hover:bg-red-900 hover:text-[#FFECAB] transition-colors cursor-pointer"
                >
                    <LogOut size={16} />
                    <span>Logout</span>
                </button>
                 <p className="mt-3 text-center text-xs text-[#090D1A]/80">Version 1.0.0</p>
            </div>
        </div>
    );
};

export default ClinicSidebar;