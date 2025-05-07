import { useState, useEffect } from 'react';
import { UserProfile } from '@/types/apiTypes'

export interface StoredUserDataType extends Omit<UserProfile, 'roles'> { 
    id: number | string;
    username: string;
    email: string;
    roles: string[];
    avatar: string | null;
    jwt?: string; 
    //Additional fields 
    phone?: string;
    name?: string;
    surname?: string;
    isActive?: boolean;
    clinicId?: number | string;
    clinicName?: string;
    licenseNumber?: string | null;
    vetPublicKey?: string | null;
}

interface AuthData {
    token: string | null;
    user: StoredUserDataType | null; 
    isLoading: boolean;
}

export const useAuth = (): AuthData => {
    const [authData, setAuthData] = useState<AuthData>({ token: null, user: null, isLoading: true });

    useEffect(() => {
        const storedUserJson = sessionStorage.getItem('user') || localStorage.getItem('user');
        if (storedUserJson) {
            try {
                const storedUserFromStorage = JSON.parse(storedUserJson);
                let rolesArray: string[] = [];
                if (storedUserFromStorage.roles && Array.isArray(storedUserFromStorage.roles)) {
                    rolesArray = storedUserFromStorage.roles;
                } else if (typeof storedUserFromStorage.role === 'string') { 
                    rolesArray = [storedUserFromStorage.role];
                }

                const userForState: StoredUserDataType = {
                    id: storedUserFromStorage.id,
                    username: storedUserFromStorage.username,
                    email: storedUserFromStorage.email,
                    avatar: storedUserFromStorage.avatar,
                    roles: rolesArray,
                     ...(storedUserFromStorage.phone && { phone: storedUserFromStorage.phone }),
                     ...(storedUserFromStorage.name && { name: storedUserFromStorage.name }),
                     ...(storedUserFromStorage.surname && { surname: storedUserFromStorage.surname }),
                     ...(typeof storedUserFromStorage.isActive === 'boolean' && { isActive: storedUserFromStorage.isActive }),
                     ...(storedUserFromStorage.clinicId && { clinicId: storedUserFromStorage.clinicId }),
                     ...(storedUserFromStorage.clinicName && { clinicName: storedUserFromStorage.clinicName }),
                     ...(storedUserFromStorage.licenseNumber && { licenseNumber: storedUserFromStorage.licenseNumber }),
                     ...(storedUserFromStorage.vetPublicKey && { vetPublicKey: storedUserFromStorage.vetPublicKey }),
                };

                setAuthData({
                    token: storedUserFromStorage.jwt, 
                    user: userForState,
                    isLoading: false
                });
            } catch (e) {
                console.error("useAuth: Failed to parse user from storage", e);
                sessionStorage.removeItem('user');
                localStorage.removeItem('user');
                setAuthData({ token: null, user: null, isLoading: false });
            }
        } else {
            setAuthData({ token: null, user: null, isLoading: false });
        }
    }, []); 

    return authData;
};