import { JSX, ReactNode } from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '@/hooks/useAuth';
import { RoleEnum } from '@/types/apiTypes'; 
import { Loader2 } from 'lucide-react';

interface ClinicProtectedRouteProps {
    children?: ReactNode;
}
/**
 * ClinicProtectedRoute - A component that protects routes intended only for authenticated users
 * with 'VET' or 'ADMIN' roles. Redirects to /login if not authorized.
 * Displays a loading spinner while authentication status is being determined.
 *
 * @param {ClinicProtectedRouteProps} props - Component props.
 * @returns {JSX.Element} The protected content (Outlet or children) or a redirect.
 */
const ClinicProtectedRoute = ({ children }: ClinicProtectedRouteProps): JSX.Element => {
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

    const isClinicStaff = user.roles?.includes(RoleEnum.VET) || user.roles?.includes(RoleEnum.ADMIN);

    if (!isClinicStaff) {
        console.warn("Access denied: User is not VET or ADMIN. Redirecting to login.");
        return <Navigate to="/login" replace />; 
    }

    if (!user.clinicId) {
         console.warn("Access denied: Clinic staff user does not have an associated clinicId. Redirecting to login.");
         return <Navigate to="/login" replace />;
    }


    return children ? <>{children}</> : <Outlet />;
};

export default ClinicProtectedRoute;