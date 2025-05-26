import { useState, useEffect, useRef, JSX, useCallback } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { Bell, User, ChevronDown, UserCog, LogOut, MessageSquare } from 'lucide-react';
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip"; 
import ProfileModal from '@/components/profile/ProfileModal'; 
import { PetProfileDto } from '@/types/apiTypes';
import logoImageD from '@/assets/images/SF-Logo1-D.png';

/**
 * Represents the basic user information needed for the TopBar.
 */
interface UserData {
    id?: number | string;
    username?: string;
    avatar?: string | null;
    roles?:string[];
}

interface TopBarProps { 
    selectedPetForChat?: PetProfileDto | null;
}

/**
 * TopBar Component - Provides consistent top navigation for authenticated users.
 * Displays branding, notification/chat icons, and a user profile dropdown menu
 * with options for editing profile and logging spi.
 *
 * @returns {JSX.Element} The top navigation bar component.
 */
const TopBar = ({ selectedPetForChat }: TopBarProps): JSX.Element => {
    const navigate = useNavigate();
    const [user, setUser] = useState<UserData | null>(null);
    const [showDropdown, setShowDropdown] = useState<boolean>(false);
    const [showProfileModal, setShowProfileModal] = useState<boolean>(false);
    const dropdownRef = useRef<HTMLDivElement>(null);

    const handleLogoutCallback = useCallback((): void => {
        console.log("TopBar: Logging spi user.");
        sessionStorage.removeItem('user');
        localStorage.removeItem('user');
        setUser(null);
        setShowDropdown(false);
        setShowProfileModal(false);
        navigate('/', { replace: true });
    }, [navigate]); 
    

    /**
     * Fetches minimal user data from session/local storage on component mount
     * to display in the TopBar.
     */
    useEffect(() => {
        const storedUserJson = sessionStorage.getItem('user') || localStorage.getItem('user');
        if (storedUserJson) {
            try {
                const storedUser = JSON.parse(storedUserJson);
                setUser({
                    id: storedUser.id,
                    username: storedUser.username,
                    avatar: storedUser.avatar,
                    roles: Array.isArray(storedUser.roles) ? storedUser.roles : (storedUser.role ? [storedUser.role] : [])
                });
                console.log("TopBar: User data loaded from storage.", { id: storedUser.id, username: storedUser.username, roles: storedUser.roles }); 
            } catch (e) {
                console.error("TopBar: Failed to parse user data from storage", e);
                handleLogoutCallback();
            }
        } else {
             console.log("TopBar: No user data found in storage.");
        }
    }, [handleLogoutCallback]); 

    /**
     * Handles clicks outside the dropdown menu to close it.
     */
    useEffect(() => {
        const handleClickOutside = (event: MouseEvent) => {
            if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
                setShowDropdown(false);
            }
        };
        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, []); 


    /**
     * Toggles the visibility of the profile dropdown menu.
     */
   const handleProfileClick = useCallback((): void => { setShowDropdown(prev => !prev); },[]);

    /**
     * Opens the Profile Modal.
     */
    const handleOpenProfileModal = useCallback((): void => {
        console.log("TopBar: Opening profile modal.");
        setShowProfileModal(true);
        setShowDropdown(false); 
    },[]);

    /**
     * Closes the Profile Modal.
     */
    const handleCloseProfileModal = useCallback((): void => {
        setShowProfileModal(false);
    },[]);

    /**
      * Callback function when profile data is successfully updated in the modal.
      * Refreshes the user data displayed in the TopBar.
      *
      * @param {UserData} updatedUserData - The minimal updated user data.
      */
    const handleProfileUpdate = useCallback((updatedUserData: UserData) => {
        console.log("TopBar: Profile updated, refreshing TopBar data.");
        setUser(prevUser => ({ ...prevUser, ...updatedUserData }));
        const storage = localStorage.getItem('user') ? localStorage : sessionStorage;
        const storedUserJson = storage.getItem('user');
         if (storedUserJson) {
            try {
               const storedUser = JSON.parse(storedUserJson);
               const updatedStoredUser = { ...storedUser, ...updatedUserData };
               storage.setItem('user', JSON.stringify(updatedStoredUser));
           } catch (e) {
                console.error("TopBar: Failed to update storage after profile update", e);
            }
         }

    }, []);

    const displayName = user?.username || "Profile";
     const getHomePath = () => {
        if (user?.roles?.includes('OWNER')) return '/pet';
        if (user?.roles?.includes('ADMIN') || user?.roles?.includes('VET')) return '/clinic';
        return '/'; // Default
    };
    const homePath = getHomePath();

    const showChatButton = user?.roles?.includes('OWNER') && selectedPetForChat;

    console.log("TopBar Check - User Roles:", user?.roles, "Selected Pet:", selectedPetForChat, "Show Chat:", showChatButton);

    return (
        <> 
            <header className="flex justify-between items-center">
                {/* Logo/Brand */}
                <Link  to={homePath} className="flex items-center gap-2 ">
                    <img src={logoImageD} alt="PetConnect Logo" className="h-10 w-10" />
                    <h1 className="text-2xl font-bold text-[#FFECAB]">PetConnect</h1>
                </Link>

                <div className="flex-1"></div> 

                {/* Action Icons */}
                <div className="flex items-center gap-2 sm:gap-4">
                    <Tooltip>
                            <TooltipTrigger asChild>
                                <button className="p-2 rounded-full hover:bg-cyan-900/50 text-[#FFECAB]/80 hover:text-[#FFECAB] transition-colors" aria-label="Notifications">
                                    <Bell size={24} />
                                </button>
                            </TooltipTrigger>
                            <TooltipContent className="bg-gray-950 text-white border border-cyan-700">
                                <p>Notifications</p>
                            </TooltipContent>
                    </Tooltip>
                     {/*  Chat Button*/}
                  {showChatButton  && selectedPetForChat && ( 
                        <Tooltip>
                            <TooltipTrigger asChild>
                                <button
                                    onClick={() => alert(`Chat for ${selectedPetForChat.name} - Not implemented`)} 
                                    className="p-2 rounded-full hover:bg-cyan-900/50 text-[#FFECAB]/80 hover:text-[#FFECAB] transition-colors"
                                    aria-label={`Chat with vet for ${selectedPetForChat.name}`}
                                >
                                    <MessageSquare size={22} />
                                </button>
                            </TooltipTrigger>
                            <TooltipContent className="bg-gray-950 text-white border border-cyan-700">
                                <p>Chat with Vet (for {selectedPetForChat.name})</p>
                            </TooltipContent>
                        </Tooltip>
                    )}
                   
                    {/* Profile Dropdown */}
                    <div className="relative" ref={dropdownRef}>
                        <Tooltip>
                            <TooltipTrigger asChild>
                                <button
                                    onClick={handleProfileClick}
                                        className="flex items-center gap-2 rounded-full border border-transparent px-2 py-1.5 text-sm font-medium text-[#FFECAB]/90 hover:text-[#FFECAB] hover:bg-cyan-900/50 focus:outline-none focus:ring-2 focus:ring-cyan-600 focus:ring-offset-2 focus:ring-offset-[#090D1A] transition-colors cursor-pointer"
                                        aria-expanded={showDropdown}
                                        aria-haspopup="true"
                                        aria-label="User menu"
                                    >
                                    {/* Avatar/Icon Display Logic */}
                                    <div className="w-10 h-10 rounded-full bg-gray-700 flex items-center justify-center overflow-hidden border border-gray-600">
                                        {user?.avatar ? (
                                            <img src={user.avatar} alt="User Avatar" className="w-full h-full object-cover" />
                                        ) : (
                                            <User size={24} className="text-[#FFECAB]" />
                                        )}
                                    </div>

                                    {/* Username Display */}
                                    <span className="hidden md:inline">{displayName}</span>
                                    <ChevronDown size={16} className={`transition-transform duration-200 ${showDropdown ? 'rotate-180' : ''}`} />

                                </button>
                            </TooltipTrigger>
                            <TooltipContent className="bg-gray-950 text-white border border-cyan-700">
                                <p>User Menu</p>
                            </TooltipContent>
                        </Tooltip>

                        {/* Dropdown Menu */}
                        {showDropdown && (
                            <div className="absolute right-0 mt-2 w-48 rounded-md shadow-lg py-1 bg-[#0c1225] ring-1 ring-[#FFECAB]/30 ring-opacity-5 focus:outline-none z-40" role="menu" aria-orientation="vertical" aria-labelledby="user-menu-button">
                                <button onClick={handleOpenProfileModal} className="flex items-center w-full px-4 py-2 text-sm text-[#FFECAB]/90 hover:bg-cyan-900/50 hover:text-[#FFECAB] cursor-pointer" role="menuitem">
                                    <UserCog size={16} className="mr-2" /> Profile 
                                </button>
                                <button onClick={handleLogoutCallback} className="flex items-center w-full px-4 py-2 text-sm text-red-400 hover:bg-red-900/30 hover:text-red-300 cursor-pointer" role="menuitem">
                                    <LogOut size={16} className="mr-2" /> Logout
                                </button>
                            </div>
                        )}
                    </div>
                </div>
            </header>

            {/* Render Profile Modal */}
            {showProfileModal && user && ( 
                 <ProfileModal
                     isOpen={showProfileModal}
                     onClose={handleCloseProfileModal}
                     userId={user.id!} 
                     onProfileUpdate={handleProfileUpdate} 
                 />
             )}
        </>
    );
};

export default TopBar;