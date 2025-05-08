import { useState, useEffect, useCallback, useMemo, JSX, ChangeEvent} from "react";
import {UserRoundPlus, Users, Loader2, AlertCircle, Edit2, UserCheck, UserX, Search} from "lucide-react";
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
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from "@/components/ui/tooltip";
import { Input } from "@/components/ui/input";
import { Checkbox } from "@/components/ui/checkbox";
import { Label } from "@/components/ui/label";
import AddStaffModal from "@/components/clinic/modals/AddStaffModal";
import StaffDetailModal from "@/components/clinic/modals/StaffDetailModal";
import { ClinicStaffProfile } from "@/types/apiTypes";
import {
  getAllStaffForClinic,
  activateStaffMember,
  deactivateStaffMember,
} from "@/services/clinicStaffService";
import { useAuth } from "@/hooks/useAuth";

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
  const [error, setError] = useState<string>("");
  const [showAddStaffModal, setShowAddStaffModal] = useState<boolean>(false);
  const [selectedStaff, setSelectedStaff] = useState<ClinicStaffProfile | null>(
    null
  );
  const [showDetailModal, setShowDetailModal] = useState<boolean>(false);
  const [searchTerm, setSearchTerm] = useState<string>("");
  const [showOnlyActive, setShowOnlyActive] = useState<boolean>(true);

  const clinicId = user?.clinicId;
  const isAdmin = user?.roles?.includes("ADMIN");

  console.log(
    "StaffManagementPage - Extracted clinicId:",
    clinicId,
    "IsAdmin:",
    isAdmin
  );

  const fetchStaff = useCallback(async () => {
    if (!token || !clinicId || !isAdmin) {
      if (isAdmin && !clinicId)
        setError("Clinic information not found for your admin account.");
      setIsLoadingData(false);
      return;
    }
    setIsLoadingData(true);
    setError("");
    try {
      const data = await getAllStaffForClinic(token, clinicId);
      setStaffList(data);
    } catch (err) {
      console.error("Failed to fetch staff:", err);
      setError(
        err instanceof Error ? err.message : "Could not load staff data."
      );
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
    fetchStaff();
    setShowAddStaffModal(false);
  };

  const handleToggleStaffStatus = async (staffMember: ClinicStaffProfile) => {
    if (!token || !user || !user.id || user.id === staffMember.id) {
      setError(
        user?.id === staffMember.id
          ? "You cannot change your own status."
          : "Action not permitted."
      );
      return;
    }
    setError('');
    setIsLoadingData(true); 

    try {
      if (staffMember.isActive) {
          await deactivateStaffMember(token, staffMember.id);
      } else {
          await activateStaffMember(token, staffMember.id);
      }
      await fetchStaff(); 
  } catch (err) {
      setError(err instanceof Error ? err.message : `Failed to update status for ${staffMember.username}.`);
       setIsLoadingData(false); 
  }
  };

  const handleOpenDetailModal = (staff: ClinicStaffProfile) => {
    setSelectedStaff(staff);
    setShowDetailModal(true);
  };

  const handleCloseDetailModal = () => {
    setSelectedStaff(null);
    setShowDetailModal(false);
  };

  const handleStaffUpdateInModal = () => {
    setShowDetailModal(false);
    fetchStaff();
  };

  // Filter and Search
  const filteredStaffList = useMemo(() => {
    console.log("Recalculating filtered list:", { count: staffList.length, term: searchTerm, activeOnly: showOnlyActive });
        return staffList.filter((staff) => {
          if (showOnlyActive && !staff.isActive) {
            return false;
          }
          if (searchTerm) {
            const lowerSearchTerm = searchTerm.toLowerCase();
            if (
              !(staff.name?.toLowerCase().includes(lowerSearchTerm) ||
              staff.surname?.toLowerCase().includes(lowerSearchTerm) ||
              staff.username?.toLowerCase().includes(lowerSearchTerm) ||
              staff.email?.toLowerCase().includes(lowerSearchTerm))
            ) {
              return false; 
            }
          }
          return true;
        });
      }, [staffList, searchTerm, showOnlyActive]);

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
        <h2 className="mt-2 text-xl font-semibold text-red-300">
          Access Denied
        </h2>
        <p className="text-gray-400">
          You do not have permission to manage staff.
        </p>
      </div>
    );
  }

  if (!clinicId && isAdmin) {
    return (
      <div className="p-6 text-center bg-[#0c1225]/50 rounded-lg border border-red-500/30">
        <AlertCircle className="mx-auto h-12 w-12 text-red-400" />
        <h2 className="mt-2 text-xl font-semibold text-red-300">
          Configuration Error
        </h2>
        <p className="text-gray-400">
          {error || "Clinic information is missing for your admin account."}
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header*/}
      <div className="flex justify-between items-center">
        <h1 className="text-2xl sm:text-3xl font-bold text-[#FFECAB] flex items-center">
          <Users className="mr-3 h-7 w-7 text-cyan-400" />Staff Management</h1>
        <div className="flex items-center gap-4">
          {/* Search */}
          <div className="relative">
            <Search
              size={18}
              className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 pointer-events-none"
            />
            <Input
              type="text"
              placeholder="Search name, email..."
              value={searchTerm}
              onChange={(e: ChangeEvent<HTMLInputElement>) =>
                setSearchTerm(e.target.value)
              }
              className="pl-10 pr-3 py-2 h-9 w-40 sm:w-56 border-gray-700 bg-[#070913]/80 rounded-md focus:ring-cyan-600"
            />
          </div>
          {/* Filter*/}
          <div className="flex items-center space-x-2">
            <Checkbox
              id="active-filter"
              checked={showOnlyActive}
              onCheckedChange={(checked) => setShowOnlyActive(!!checked)}
              className="data-[state=checked]:bg-cyan-600 data-[state=checked]:text-white border-gray-500"
            />
            <Label
              htmlFor="active-filter"
              className="text-sm font-medium text-gray-300 cursor-pointer"
            >
              Active Only
            </Label>
          </div>
          {/* Add Staff Button */}
          <Button
            onClick={() => setShowAddStaffModal(true)}
            className="px-5 py-2.5 rounded-lg border border-[#FFECAB]/50 bg-cyan-800 text-[#FFECAB] hover:bg-cyan-600 focus-visible:ring-cyan-500 disabled:opacity-50 cursor-pointer"
          >
            <UserRoundPlus size={18} className="mr-2" />
            Add New Staff
          </Button>
        </div>
      </div>

      {/* Error message */}
      {error && (
        <div className="p-3 text-center text-red-400 bg-red-900/20 border border-red-500/50 rounded-lg flex items-center justify-center gap-2">
          <AlertCircle size={20} /> {error}
        </div>
      )}

      {/* Carrd */}
      <Card className="border-2 border-[#FFECAB]/30 bg-[#0c1225]/70 shadow-xl">
        <CardHeader>
          <CardTitle className="text-[#FFECAB]">Clinic Staff List</CardTitle>
        </CardHeader>

        <CardContent className="p-0">
          {isLoadingData ? (
            <div className="flex justify-center items-center py-10">
              <Loader2 className="h-8 w-8 animate-spin text-cyan-500" />
              <span className="ml-2 text-gray-400">Loading staff list...</span>
            </div>
          ) : filteredStaffList.length === 0 ? (
            <p className="text-center text-gray-400 py-8">
            {searchTerm || !showOnlyActive ? 'No staff members match your filters.' : 'No active staff members found for this clinic.'}
            </p>
          ) : (
            <Table>
              <TableHeader>
                <TableRow className="border-b-[#FFECAB]/20">
                  <TableHead className="text-[#FFECAB]/80  pl-6">Name</TableHead>
                  <TableHead className="text-[#FFECAB]/80 hidden md:table-cell">Email</TableHead>
                  <TableHead className="text-[#FFECAB]/80">Role(s)</TableHead>
                  <TableHead className="text-[#FFECAB]/80 text-center">Status</TableHead>
                  <TableHead className="text-[#FFECAB]/80 text-right pr-6">Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
              {filteredStaffList.map((staff) => (
                  <TableRow
                    key={staff.id}
                    className="border-b-[#FFECAB]/10 hover:bg-[#FFECAB]/5"
                  >
                    <TableCell className="pl-6">
                      <div className="flex items-center gap-3">
                        <img
                          src={
                            staff.avatar ||
                            "/src/assets/images/avatars/users/default_avatar.png"
                          }
                          alt={staff.username}
                          className="w-8 h-8 rounded-full object-cover"
                          onError={(e) =>
                            (e.currentTarget.src =
                              "/src/assets/images/avatars/users/default_avatar.png")
                          }
                        />
                        <div>
                          <div className="font-medium text-white">
                            {staff.name} {staff.surname}
                          </div>
                          <div className="text-xs text-gray-400">
                            @{staff.username}
                          </div>
                        </div>
                      </div>
                    </TableCell>
                    <TableCell className="text-gray-300 hidden md:table-cell">
                      {staff.email}
                    </TableCell>
                    <TableCell>
                      {staff.roles.map((role) => (
                        <Badge
                          key={role}
                          variant={role === "ADMIN" ? "default" : "secondary"}
                          className={`px-2 py-0.5 text-xs font-semibold rounded-full ${
                            role === "ADMIN"
                              ? "bg-cyan-600 text-cyan-50"
                              : "bg-purple-600 text-purple-50"
                          }`}
                        >
                          {role}
                        </Badge>
                      ))}
                    </TableCell>
                    <TableCell className="text-center">
                      <Badge
                        variant={staff.isActive ? "default" : "destructive"}
                        className={`px-2.5 py-1 text-xs font-bold rounded-md ${
                          staff.isActive
                            ? "bg-green-500/20 text-green-300 border border-green-500/50"
                            : "bg-red-500/20 text-red-300 border border-red-500/50"
                        }`}
                      >
                        {staff.isActive ? "Active" : "Inactive"}
                      </Badge>
                    </TableCell>
                    <TableCell className="text-right space-x-1 pr-6">
                      <Tooltip>
                        <TooltipTrigger asChild>
                          <Button
                            size="icon"
                            className="text-cyan-400 hover:bg-cyan-800 hover:text-[#FFECAB] h-8 w-8 cursor-pointer"
                            onClick={() => handleOpenDetailModal(staff)}
                          >
                            <Edit2 size={16} />
                          </Button>
                        </TooltipTrigger>
                        <TooltipContent>
                          <p>Edit Staff</p>
                        </TooltipContent>
                      </Tooltip>
                      <Tooltip>
                        <TooltipTrigger asChild>
                          <Button
                            size="icon"
                            className={`h-8 w-8 ${
                              staff.isActive
                                ? "text-red-400 hover:text-[#FFECAB] hover:bg-red-800"
                                : "text-cyan-400 hover:text-[#FFECAB] hover:bg-cyan-700"
                            } cursor-pointer`}
                            onClick={() => handleToggleStaffStatus(staff)}
                            disabled={user?.id === staff.id}
                            title={
                              user?.id === staff.id
                                ? "Cannot change your own status"
                                : staff.isActive
                                ? "Deactivate Staff"
                                : "Activate Staff"
                            }
                          >
                            {staff.isActive ? (
                              <UserX size={16} />
                            ) : (
                              <UserCheck size={16} />
                            )}
                          </Button>
                        </TooltipTrigger>
                        <TooltipContent>
                          <p>
                            {user?.id === staff.id
                              ? "Cannot change own status"
                              : staff.isActive
                              ? "Deactivate"
                              : "Activate"}
                          </p>
                        </TooltipContent>
                      </Tooltip>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>

      {/* Add Staff Modal */}
      {showAddStaffModal && clinicId && (
        <AddStaffModal
          isOpen={showAddStaffModal}
          onClose={() => setShowAddStaffModal(false)}
          onStaffAdded={handleStaffAdded}
          clinicId={clinicId as number | string}
        />
      )}

      {/* Staff Detail Modal */}
      {showDetailModal && selectedStaff && (
        <StaffDetailModal
          isOpen={showDetailModal}
          onClose={handleCloseDetailModal}
          staffProfileInitial={selectedStaff}
          onStaffUpdate={handleStaffUpdateInModal}
        />
      )}
    </div>
  );
};

export default StaffManagementPage;
