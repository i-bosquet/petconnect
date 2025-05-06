import { useState, useEffect, useRef, JSX } from 'react';
import { useCallback } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { Bell, User, ChevronDown, UserCog, LogOut } from 'lucide-react';
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip"; 
import ProfileModal from '../profile/ProfileModal'; 

/**
 * Represents the basic user information needed for the TopBar.
 */
interface UserData {
    id?: number | string;
    username?: string;
    avatar?: string | null;
    role?: string;
}

/**
 * TopBar Component - Provides consistent top navigation for authenticated users.
 * Displays branding, notification/chat icons, and a user profile dropdown menu
 * with options for editing profile and logging spi.
 *
 * @returns {JSX.Element} The top navigation bar component.
 */
const TopBar = (): JSX.Element => {
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
        navigate('/login', { replace: true });
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
                    role: storedUser.role
                });
                console.log("TopBar: User data loaded from storage.", { id: storedUser.id, username: storedUser.username, role: storedUser.role }); 
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
    const handleProfileClick = (): void => {
        setShowDropdown(prev => !prev); 
    };

    /**
     * Opens the Profile Modal.
     */
    const handleOpenProfileModal = useCallback((): void => {
        console.log("TopBar: Opening profile modal.");
        setShowProfileModal(true);
        setShowDropdown(false); // Close dropdown when modal opens
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
    // Determine home path based on user role, default to '/' if role unknown or user null
    const homePath = user?.role === 'OWNER' ? '/pet' : (user?.role === 'VET' || user?.role === 'ADMIN' ? '/clinic' : '/');

    return (
        <> 
            <header className="sticky top-0 z-30 flex h-14 items-center gap-4 border-b border-[#FFECAB]/20 bg-[#090D1A]/95 px-4 backdrop-blur-sm sm:h-16 sm:px-6">
                {/* Logo/Brand */}
                <Link  to={homePath} className="flex items-center gap-2 font-semibold text-[#FFECAB]">
                    <img src="/src/assets/images/SF-Logo1-D.png" alt="PetConnect Logo" className="h-8 w-8" />
                    <span className="text-lg hidden sm:inline">PetConnect</span>
                </Link>

                <div className="flex-1"></div> 

                {/* Action Icons */}
                <div className="flex items-center gap-2 sm:gap-4">
                    <Tooltip>
                            <TooltipTrigger asChild>
                                <button className="p-2 rounded-full hover:bg-cyan-900/50 text-[#FFECAB]/80 hover:text-[#FFECAB] transition-colors" aria-label="Notifications">
                                    <Bell size={20} />
                                </button>
                            </TooltipTrigger>
                            <TooltipContent className="bg-gray-950 text-white border border-cyan-700">
                                <p>Notifications</p>
                            </TooltipContent>
                    </Tooltip>

                    {/*    Chat button temporarily removed         
                    <Tooltip>
                            <TooltipTrigger asChild>
                                <button className="p-2 rounded-full hover:bg-cyan-900/50 text-[#FFECAB]/80 hover:text-[#FFECAB] transition-colors" aria-label="Chat">
                                    <MessageCircle size={20} />
                                </button>
                            </TooltipTrigger>
                            <TooltipContent className="bg-gray-950 text-white border border-cyan-700">
                                <p>Chat</p>
                            </TooltipContent>
                    </Tooltip>
                    */}

                    {/* Profile Dropdown */}
                    <div className="relative" ref={dropdownRef}>
                        <Tooltip>
                            <TooltipTrigger asChild>
                                <button
                                    onClick={handleProfileClick}
                                        className="flex items-center gap-2 rounded-full border border-transparent px-2 py-1.5 text-sm font-medium text-[#FFECAB]/90 hover:text-[#FFECAB] hover:bg-cyan-900/50 focus:outline-none focus:ring-2 focus:ring-cyan-600 focus:ring-offset-2 focus:ring-offset-[#090D1A] transition-colors"
                                        aria-expanded={showDropdown}
                                        aria-haspopup="true"
                                        aria-label="User menu"
                                    >
                                    {/* Avatar/Icon Display Logic */}
                                    <div className="w-7 h-7 rounded-full bg-gray-700 flex items-center justify-center overflow-hidden border border-gray-600">
                                        {user?.avatar ? (
                                            <img src={user.avatar} alt="User Avatar" className="w-full h-full object-cover" />
                                        ) : (
                                            <User size={16} className="text-[#FFECAB]" />
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
                                <button onClick={handleOpenProfileModal} className="flex items-center w-full px-4 py-2 text-sm text-[#FFECAB]/90 hover:bg-cyan-900/50 hover:text-[#FFECAB]" role="menuitem">
                                    <UserCog size={16} className="mr-2" /> Profile 
                                </button>
                                <button onClick={handleLogoutCallback} className="flex items-center w-full px-4 py-2 text-sm text-red-400 hover:bg-red-900/30 hover:text-red-300" role="menuitem">
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