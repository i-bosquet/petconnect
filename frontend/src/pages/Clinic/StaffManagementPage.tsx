import { useState, useEffect, useCallback, JSX } from 'react'; 
import { PlusCircle, Users, Loader2, AlertCircle, Edit2, UserCheck, UserX } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from "@/components/ui/table";
import { Badge } from "@/components/ui/badge"; 
import { Button } from "@/components/ui/button";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip";
import AddStaffModal from '@/components/clinic/modals/AddStaffModal'; 
import { ClinicStaffProfile } from '@/types/apiTypes';
import { getAllStaffForClinic, activateStaffMember, deactivateStaffMember } from '@/services/clinicStaffService'; 
import { useAuth } from '@/hooks/useAuth'; 

/**
 * StaffManagementPage - Allows Admins to manage clinic staff members.
 * Features listing, adding, (later: editing, activating/deactivating) staff.
 *
 * @returns {JSX.Element} The staff management page component.
 */
const StaffManagementPage = (): JSX.Element => {
    const { token, user, isLoading: isLoadingAuth } = useAuth(); 
    console.log("StaffManagementPage - User from useAuth:", user); 
    const [staffList, setStaffList] = useState<ClinicStaffProfile[]>([]);
    const [isLoadingData, setIsLoadingData] = useState<boolean>(true); 
    const [error, setError] = useState<string>('');
    const [showAddStaffModal, setShowAddStaffModal] = useState<boolean>(false);

    const clinicId = user?.clinicId;
    const isAdmin = user?.roles?.includes('ADMIN');

    console.log("StaffManagementPage - Extracted clinicId:", clinicId, "IsAdmin:", isAdmin);

    const fetchStaff = useCallback(async () => {
        if (!token || !clinicId || !isAdmin) { 
            if (isAdmin && !clinicId) setError("Clinic information not found for your admin account.");
            setIsLoadingData(false);
            return;
        }
        setIsLoadingData(true);
        setError('');
        try {
            const data = await getAllStaffForClinic(token, clinicId);
            setStaffList(data);
        } catch (err) {
            console.error("Failed to fetch staff:", err);
            setError(err instanceof Error ? err.message : "Could not load staff data.");
        } finally {
            setIsLoadingData(false);
        }
    }, [token, clinicId, isAdmin]);

    useEffect(() => {
        if (!isLoadingAuth && user) {
            if (isAdmin) {
                fetchStaff();
            } else {
                setIsLoadingData(false);
            }
        } else if (!isLoadingAuth && !user) {
             setIsLoadingData(false);
        }
    }, [fetchStaff, isAdmin, user, isLoadingAuth]);

    const handleStaffAdded = () => {
        fetchStaff(); // Refrescar lista
        setShowAddStaffModal(false);
    };

    const handleToggleStaffStatus = async (staffMember: ClinicStaffProfile) => {
        if (!token || !user || !user.id || user.id === staffMember.id) {
            setError(user?.id === staffMember.id ? "You cannot change your own status." : "Action not permitted.");
            return;
        }
        const originalStaffList = [...staffList];
        setStaffList(prevList => prevList.map(s => s.id === staffMember.id ? {...s, isActive: !s.isActive} : s));
        setError('');

        try {
            if (staffMember.isActive) {
                await deactivateStaffMember(token, staffMember.id);
            } else {
                await activateStaffMember(token, staffMember.id);
            }
            await fetchStaff();
        } catch (err) {
            setError(err instanceof Error ? err.message : `Failed to update status for ${staffMember.username}.`);
            setStaffList(originalStaffList); 
        }
    };

    if (isLoadingAuth) { 
        return (
            <div className="flex justify-center items-center py-10">
                <Loader2 className="h-8 w-8 animate-spin text-cyan-500" />
            </div>
        );
    }

    if (!isAdmin) {
        return (
            <div className="p-6 text-center bg-[#0c1225]/50 rounded-lg border border-red-500/30">
                <AlertCircle className="mx-auto h-12 w-12 text-red-400" />
                <h2 className="mt-2 text-xl font-semibold text-red-300">Access Denied</h2>
                <p className="text-gray-400">You do not have permission to manage staff.</p>
            </div>
        );
    }
    if (!clinicId && isAdmin) { 
        return (
             <div className="p-6 text-center bg-[#0c1225]/50 rounded-lg border border-red-500/30">
                <AlertCircle className="mx-auto h-12 w-12 text-red-400" />
                <h2 className="mt-2 text-xl font-semibold text-red-300">Configuration Error</h2>
                <p className="text-gray-400">{error || "Clinic information is missing for your admin account."}</p>
            </div>
        );
    }


    return (
        <div className="space-y-6">
            <div className="flex justify-between items-center">
                <h1 className="text-2xl sm:text-3xl font-bold text-[#FFECAB] flex items-center">
                    <Users className="mr-3 h-7 w-7 text-cyan-400" />
                    Staff Management
                </h1>
                <Button onClick={() => setShowAddStaffModal(true)} variant="outline" className="bg-cyan-700 hover:bg-cyan-600 text-[#FFECAB] border-cyan-600 hover:border-cyan-500">
                    <PlusCircle size={18} className="mr-2" />
                    Add New Staff
                </Button>
            </div>

            {error && (
                <div className="p-3 text-center text-red-400 bg-red-900/20 border border-red-500/50 rounded-lg flex items-center justify-center gap-2">
                    <AlertCircle size={20}/> {error}
                </div>
            )}

            <Card className="border-2 border-[#FFECAB]/30 bg-[#0c1225]/70 shadow-xl">
                <CardHeader>
                    <CardTitle className="text-[#FFECAB]">Clinic Staff List</CardTitle>
                </CardHeader>
                <CardContent>
                    {isLoadingData ? (
                        <div className="flex justify-center items-center py-10">
                            <Loader2 className="h-8 w-8 animate-spin text-cyan-500" />
                            <span className="ml-2 text-gray-400">Loading staff list...</span>
                        </div>
                    ) : staffList.length === 0 && !error ? (
                        <p className="text-center text-gray-400 py-8">No staff members found for this clinic.</p>
                    ) : staffList.length > 0 ? (
                        <Table>
                            <TableHeader>
                                <TableRow className="border-b-[#FFECAB]/20">
                                    <TableHead className="text-[#FFECAB]/80">Name</TableHead>
                                    <TableHead className="text-[#FFECAB]/80 hidden md:table-cell">Email</TableHead>
                                    <TableHead className="text-[#FFECAB]/80">Role(s)</TableHead>
                                    <TableHead className="text-[#FFECAB]/80 text-center">Status</TableHead>
                                    <TableHead className="text-[#FFECAB]/80 text-right">Actions</TableHead>
                                </TableRow>
                            </TableHeader>
                            <TableBody>
                                {staffList.map((staff) => (
                                    <TableRow key={staff.id} className="border-b-[#FFECAB]/10 hover:bg-[#FFECAB]/5">
                                        <TableCell>
                                            <div className="flex items-center gap-3">
                                                <img src={staff.avatar || '/src/assets/images/avatars/users/default_avatar.png'}
                                                     alt={staff.username}
                                                     className="w-8 h-8 rounded-full object-cover"
                                                     onError={(e) => (e.currentTarget.src = '/src/assets/images/avatars/users/default_avatar.png')}
                                                />
                                                <div>
                                                    <div className="font-medium text-white">{staff.name} {staff.surname}</div>
                                                    <div className="text-xs text-gray-400">@{staff.username}</div>
                                                </div>
                                            </div>
                                        </TableCell>
                                        <TableCell className="text-gray-300 hidden md:table-cell">{staff.email}</TableCell>
                                        <TableCell>
                                            {staff.roles.map(role => (
                                                <Badge key={role} variant={role === 'ADMIN' ? 'default' : 'secondary'}
                                                       className={`px-2 py-0.5 text-xs font-semibold rounded-full ${
                                                           role === 'ADMIN' ? 'bg-cyan-600 text-cyan-50' : 'bg-purple-600 text-purple-50'
                                                       }`}>
                                                    {role}
                                                </Badge>
                                            ))}
                                        </TableCell>
                                        <TableCell className="text-center">
                                            <Badge variant={staff.isActive ? 'default' : 'destructive'}
                                                   className={`px-2.5 py-1 text-xs font-bold rounded-md ${
                                                       staff.isActive ? 'bg-green-500/20 text-green-300 border border-green-500/50' : 'bg-red-500/20 text-red-300 border border-red-500/50'
                                                   }`}>
                                                {staff.isActive ? 'Active' : 'Inactive'}
                                            </Badge>
                                        </TableCell>
                                        <TableCell className="text-right space-x-1">
                                             <Tooltip>
                                                 <TooltipTrigger asChild>
                                                     <Button  size="icon" className="text-cyan-400 hover:bg-cyan-800 hover:text-[#FFECAB] h-8 w-8" onClick={() => { console.log("Edit staff:", staff.id)}}>
                                                         <Edit2 size={16} />
                                                     </Button>
                                                 </TooltipTrigger>
                                                 <TooltipContent><p>Edit Staff</p></TooltipContent>
                                             </Tooltip>
                                             <Tooltip>
                                                 <TooltipTrigger asChild>
                                                     <Button variant="ghost" size="icon"
                                                             className={`h-8 w-8 ${staff.isActive ? "text-red-400 hover:text-red-300" : "text-green-400 hover:text-green-300"}`}
                                                             onClick={() => handleToggleStaffStatus(staff)}
                                                             disabled={user?.id === staff.id }
                                                             title={user?.id === staff.id ? "Cannot change your own status" : (staff.isActive ? "Deactivate Staff" : "Activate Staff")}>
                                                         {staff.isActive ? <UserX size={16} /> : <UserCheck size={16} />}
                                                     </Button>
                                                 </TooltipTrigger>
                                                 <TooltipContent><p>{user?.id === staff.id ? "Cannot change own status" : (staff.isActive ? "Deactivate" : "Activate")}</p></TooltipContent>
                                             </Tooltip>
                                        </TableCell>
                                    </TableRow>
                                ))}
                            </TableBody>
                        </Table>
                    ) : null }
                     {!isLoadingData && !error && staffList.length === 0 && (
                        <p className="text-center text-gray-400 py-8">No staff members found for this clinic.</p>
                    )}

                </CardContent>
            </Card>

            {showAddStaffModal && clinicId && (
                <AddStaffModal
                    isOpen={showAddStaffModal}
                    onClose={() => setShowAddStaffModal(false)}
                    onStaffAdded={handleStaffAdded}
                    clinicId={clinicId as number | string}
                />
            )}
        </div>
    );
};

export default StaffManagementPage;