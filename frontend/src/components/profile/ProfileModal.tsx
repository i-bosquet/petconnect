import { useState, useEffect, useCallback, JSX } from 'react';
import Modal from '../common/Modal'; 
import ProfileView from './ProfileView'; 
import ProfileEditForm from './ProfileEditForm';
import { getCurrentUserProfile } from '@/services/authService';
import { UserProfile} from '@/types/apiTypes';
import { Loader2, AlertCircle } from 'lucide-react';

interface ProfileModalProps {
    isOpen: boolean;
    onClose: () => void;
    userId: number | string; 
    onProfileUpdate: (updatedData: { username?: string, avatar?: string | null }) => void; 
}

/**
 * ProfileModal - Container modal for displaying user profile details
 * and allowing editing. Fetches full profile data and toggles between
 * view and edit modes.
 *
 * @param {ProfileModalProps} props - Component props.
 * @returns {JSX.Element | null} The profile modal or null if not open.
 */
const ProfileModal = ({ isOpen, onClose, userId, onProfileUpdate }: ProfileModalProps): JSX.Element | null => {
    const [isEditing, setIsEditing] = useState<boolean>(false);
    const [profileData, setProfileData] = useState<UserProfile | null>(null);
    const [isLoading, setIsLoading] = useState<boolean>(true);
    const [error, setError] = useState<string>('');

    const storedUserJson = sessionStorage.getItem('user') || localStorage.getItem('user');
    const token = storedUserJson ? JSON.parse(storedUserJson).jwt : null;

    // Fetch full profile data when the modal opens or userId changes
    const fetchProfile = useCallback(async () => {
        if (!isOpen || !token || !userId) return; // Don't fetch if not open or no token/userId

        setIsLoading(true);
        setError('');
        try {
            console.log(`ProfileModal: Fetching profile for user ID: ${userId}`);
            if (!token) {
                 throw new Error("Authentication token is missing.");
            }
            const data = await getCurrentUserProfile(token);
            console.log("ProfileModal: Profile data fetched:", data);
            setProfileData(data);
        } catch (err) {
            console.error("ProfileModal: Failed to fetch profile:", err);
            setError(err instanceof Error ? err.message : 'Could not load profile data.');
             if (err instanceof Error && err.message.includes('401')) { 
                 onClose(); // Close modal if auth fails (token might be expired)
             }
        } finally {
            setIsLoading(false);
        }
    }, [isOpen, token, userId, onClose]);

    useEffect(() => {
        fetchProfile();
    }, [fetchProfile]); // Fetch on open/userId change


    const handleStartEditing = () => {
        setIsEditing(true);
    };

    const handleCancelEditing = () => {
        setIsEditing(false);
        fetchProfile();
    };

    // Callback from ProfileEditForm on successful save
    const handleSaveSuccess = (updatedProfile: UserProfile) => {
        setProfileData(updatedProfile); // Update local state
        setIsEditing(false); // Go back to view mode
        onProfileUpdate({
            username: updatedProfile.username,
            avatar: updatedProfile.avatar
        });
    };

    if (!isOpen) {
        return null;
    }

    const modalTitle = isEditing ? "Edit Profile" : "My Profile";

    return (
        <Modal title={modalTitle} onClose={onClose} maxWidth="max-w-4xl">
            {isLoading && (
                <div className="flex justify-center items-center py-10">
                    <Loader2 className="h-8 w-8 animate-spin text-cyan-500" />
                    <span className="ml-2 text-gray-400">Loading profile...</span>
                </div>
            )}
            {error && !isLoading && (
                 <div className="flex items-center justify-center p-4 bg-red-900/20 text-red-400 border border-red-500/50 rounded-lg">
                    <AlertCircle size={20} className="mr-2"/>
                    <span>{error}</span>
                </div>
            )}
            {!isLoading && !error && profileData && (
                <>
                    {isEditing ? (
                        <ProfileEditForm
                            userProfile={profileData} 
                            onSaveSuccess={handleSaveSuccess}
                            onCancel={handleCancelEditing}
                        />
                    ) : (
                        <ProfileView
                            userProfile={profileData}
                            onEdit={handleStartEditing}
                        />
                    )}
                </>
            )}
             {!isLoading && !error && !profileData && (
                 <div className="py-10 text-center text-gray-500">Could not load profile details.</div>
             )}

        </Modal>
    );
};

export default ProfileModal;