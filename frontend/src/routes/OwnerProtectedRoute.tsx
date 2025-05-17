import { JSX, ReactNode } from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '@/hooks/useAuth';
import { RoleEnum } from '@/types/apiTypes'; 
import { Loader2 } from 'lucide-react';

interface OwnerProtectedRouteProps {
    children?: ReactNode; 
}

/**
 * OwnerProtectedRoute - A component that protects routes intended only for authenticated users
 * with the 'OWNER' role. Redirects to /login if not authenticated or not an owner.
 * Displays a loading spinner while authentication status is being determined.
 *
 * @param {OwnerProtectedRouteProps} props - Component props.
 * @returns {JSX.Element} The protected content (Outlet or children) or a redirect.
 */
const OwnerProtectedRoute = ({ children }: OwnerProtectedRouteProps): JSX.Element => {
    const { token, user, isLoading } = useAuth();

    if (isLoading) {
        return (
            <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-[#070913] to-[#0c1225]">
                <Loader2 className="h-12 w-12 animate-spin text-cyan-400" />
            </div>
        );
    }

    if (!token || !user) {
        return <Navigate to="/login" replace />;
    }

    if (!user.roles?.includes(RoleEnum.OWNER)) {
        console.warn("Access denied: User is not an OWNER. Redirecting to login.");
        return <Navigate to="/login" replace />; 
    }

    return children ? <>{children}</> : <Outlet />;
};

export default OwnerProtectedRoute;