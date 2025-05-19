import { useState, useEffect, useCallback, JSX } from 'react';
import { Outlet, useNavigate } from 'react-router-dom';
import ClinicSidebar from '@/components/layout/ClinicSidebar';
import ProfileModal from '@/components/profile/ProfileModal'; 
import { ClinicStaffProfile, UserProfile } from '@/types/apiTypes';
import { getCurrentUserProfile } from '@/services/authService';
import { Menu }from 'lucide-react';
import { Button } from '@/components/ui/button';

interface UserDisplayData {
    username?: string;
    avatar?: string | null;
}

/**
 * ClinicLayout - Provides the layout structure for the Clinic Staff dashboard.
 * Includes a collapsible ClinicSidebar and the main content area.
 * Manages the visibility and data for the ProfileModal.
 *
 * @returns {JSX.Element} The layout structure for clinic sections.
 */
const ClinicLayout = (): JSX.Element => {
    const [currentStaff, setCurrentStaff] = useState<ClinicStaffProfile | null>(null);
    const [isLoadingProfile, setIsLoadingProfile] = useState<boolean>(true);
    const [showProfileModal, setShowProfileModal] = useState<boolean>(false);
    const [isSidebarOpenOnSmallScreens, setIsSidebarOpenOnSmallScreens] = useState(false);
    const navigate = useNavigate();

    const fetchStaffProfile = useCallback(async (forceRefresh = false) => {
        if (!forceRefresh && currentStaff && !isLoadingProfile) {
            return;
        }
        setIsLoadingProfile(true);
        const storedUserJson = sessionStorage.getItem('user') || localStorage.getItem('user');
        if (!storedUserJson) {
            navigate('/login', { replace: true });
            setIsLoadingProfile(false);
            return;
        }
        try {
            const basicStoredUser = JSON.parse(storedUserJson);
            const userRoles = Array.isArray(basicStoredUser.roles) ? basicStoredUser.roles : [];

            if (!basicStoredUser.jwt || (!userRoles.includes('VET') && !userRoles.includes('ADMIN'))) {
                console.error("ClinicLayout: User not authorized (VET/ADMIN role not found in storage) or token missing.", basicStoredUser);
                navigate('/login', { replace: true });
                setIsLoadingProfile(false);
                return;
            }
            
            const fullProfile: UserProfile = await getCurrentUserProfile(basicStoredUser.jwt);

            if ('clinicId' in fullProfile) {
                 setCurrentStaff(fullProfile as ClinicStaffProfile);
            } else {
                console.error("ClinicLayout: Fetched profile is not ClinicStaffProfile.");
                navigate('/login', { replace: true });
            }
        } catch (error) {
            console.error("ClinicLayout: Failed to fetch staff profile", error);
            navigate('/login', { replace: true });
        } finally {
            setIsLoadingProfile(false);
        }
    }, [navigate, currentStaff, isLoadingProfile]);

    useEffect(() => {
        fetchStaffProfile();
    }, [fetchStaffProfile]);

    const handleLogout = useCallback(() => {
        sessionStorage.removeItem('user');
        localStorage.removeItem('user');
        setCurrentStaff(null);
        setShowProfileModal(false);
        setIsSidebarOpenOnSmallScreens(false); 
        navigate('/login', { replace: true });
    }, [navigate]);

    const handleOpenProfileModal = useCallback(() => {
        setShowProfileModal(true);
        setIsSidebarOpenOnSmallScreens(false);
    }, []);

    const handleCloseProfileModal = useCallback(() => {
        setShowProfileModal(false);
    }, []);

    const handleProfileUpdateLayout = useCallback((updatedData: UserDisplayData) => {
        setCurrentStaff(prev => {
            if (!prev) return null;
            return {
                ...prev,
                username: updatedData.username !== undefined ? updatedData.username : prev.username,
                avatar: updatedData.avatar !== undefined ? updatedData.avatar : prev.avatar,
            };
        });
    const storage = localStorage.getItem('user') ? localStorage : sessionStorage;

    const storedUserJson = storage.getItem('user');

    if (storedUserJson) {
            try {
                const storedUser = JSON.parse(storedUserJson);
                const updatedStoredUser = {
                     ...storedUser,
                     username: updatedData.username !== undefined ? updatedData.username : storedUser.username,
                     avatar: updatedData.avatar !== undefined ? updatedData.avatar : storedUser.avatar,
                };
                storage.setItem('user', JSON.stringify(updatedStoredUser));
                 if (updatedData.username && updatedData.username !== storedUser.username) {
                    fetchStaffProfile(true);
                }
            } catch (e) {
                console.error("ClinicLayout: Failed to update storage after profile update callback", e);
            }
        }
    }, [fetchStaffProfile]);

    if (isLoadingProfile || !currentStaff) {
        return (
            <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-[#070913] to-[#0c1225]">
                <p className="text-[#FFECAB]">Loading Clinic Portal...</p>
            </div>
        );
    }

    return (
        <>
            <div className="min-h-screen bg-gradient-to-br from-[#070913] to-[#0c1225] text-[#FFECAB] flex relative">
                {/* Sidebar */}
                <div className={`
                    fixed inset-y-0 left-0 z-40 w-72 bg-[#FFECAB] text-[#090D1A] p-4 flex flex-col shadow-xl transition-transform duration-300 ease-in-out
                    lg:static lg:translate-x-0 lg:shadow-none 
                    ${isSidebarOpenOnSmallScreens ? 'translate-x-0' : '-translate-x-full'}
                `}>
                     {currentStaff && (
                    <ClinicSidebar
                        closeMobileMenu={() => setIsSidebarOpenOnSmallScreens(false)}
                        currentStaff={currentStaff}
                        handleLogout={handleLogout}
                        onOpenProfileModal={handleOpenProfileModal}
                    />
                    )}
                </div>

                {/* Main */}
                <div className="flex-1 flex flex-col overflow-x-hidden">
                    <div className="lg:hidden p-3 sticky top-0 bg-[#070913]/90 backdrop-blur-sm z-20 border-b border-[#FFECAB]/10 flex items-center">
                        <Button
                            size="icon"
                            onClick={() => setIsSidebarOpenOnSmallScreens(true)}
                            className="text-[#FFECAB] bg-cyan-800 hover:bg-cyan-500  cursor-pointer"
                            aria-label="Open navigation menu"
                        >
                            <Menu size={24} />
                        </Button>
                    </div>
                    <main className="flex-1 p-4 sm:p-6 overflow-y-auto">
                        <Outlet />
                    </main>
                </div>
            </div>

             {isSidebarOpenOnSmallScreens && (
                <div 
                    className="lg:hidden fixed inset-0 bg-black/60 z-30"
                    onClick={() => setIsSidebarOpenOnSmallScreens(false)}
                    aria-hidden="true"
                ></div>
            )}

            {showProfileModal && currentStaff && (
                <ProfileModal
                    isOpen={showProfileModal}
                    onClose={handleCloseProfileModal}
                    userId={currentStaff.id}
                    onProfileUpdate={handleProfileUpdateLayout}
                />
            )}
        </>
    );
};

export default ClinicLayout;