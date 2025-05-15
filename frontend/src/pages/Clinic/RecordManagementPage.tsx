import { useState, useEffect, useCallback, useMemo, JSX, ChangeEvent, FormEvent} from "react";
import { formatRecordCreatorDisplay, formatDateTime, getRecordTypeDisplay} from '@/utils/formatters';
import { ClipboardList, Loader2, AlertCircle, Search, Eye, FileText, ShieldCheck, ShieldAlert, Thermometer, Syringe, AlertTriangle, Info, BookOpenCheck,Trash } from "lucide-react"; 
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Button } from "@/components/ui/button";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip";
import { Input } from "@/components/ui/input";
import { RecordViewDto, Page, RecordType} from "@/types/apiTypes"; 
import { findRecordsCreatedByClinic, deleteRecord } from "@/services/recordService";
import { useAuth } from "@/hooks/useAuth";
import Pagination from "@/components/common/Pagination";
import ViewRecordModal from '@/components/pet/modals/ViewRecordModal'; 
import EditRecordModal from '@/components/pet/modals/EditRecordModal';   
import ConfirmationModal from '@/components/common/ConfirmationModal';
import { toast } from 'sonner';

const PAGE_SIZE_CLINIC_RECORDS = 10;

/**
 * RecordManagementPage - Displays a historical list of all medical records
 * created by the clinic's staff.
 * Allows viewing details of each record.
 *
 * @returns {JSX.Element} The clinic's record management page component.
 */
const RecordManagementPage = (): JSX.Element => {
    const { token, user, isLoading: isLoadingAuth } = useAuth(); 
    const [records, setRecords] = useState<RecordViewDto[]>([]);
    const [isLoadingData, setIsLoadingData] = useState<boolean>(true);
    const [error, setError] = useState<string>("");
    const [searchTerm, setSearchTerm] = useState<string>("");
    const [currentPage, setCurrentPage] = useState<number>(0);
    const [totalPages, setTotalPages] = useState<number>(0);

    const [selectedRecord, setSelectedRecord] = useState<RecordViewDto | null>(null);
    const [showViewModal, setShowViewModal] = useState<boolean>(false);
    const [showEditModal, setShowEditModal] = useState<boolean>(false);
    const [showDeleteConfirmModal, setShowDeleteConfirmModal] = useState<boolean>(false);
    const [isActionLoading, setIsActionLoading] = useState<boolean>(false);

    const clinicIdFromUser = user?.clinicId;

    /**
     * Fetches the paginated list of records created by the current clinic.
     * @param {number} pageToFetch - The page number to retrieve.
     */
    const fetchClinicCreatedRecords = useCallback(async (pageToFetch: number) => {
        if (!token || !clinicIdFromUser) {
            setIsLoadingData(false);
            if (!clinicIdFromUser) setError("Clinic information not found for your account to fetch records.");
            return;
        }
        setIsLoadingData(true);
        setError("");
        try {
            const data: Page<RecordViewDto> = await findRecordsCreatedByClinic(token, {
                clinicId: clinicIdFromUser,
                page: pageToFetch,
                size: PAGE_SIZE_CLINIC_RECORDS,
                sort: 'createdAt,desc',
            });
            setRecords(data.content);
            setTotalPages(data.totalPages);
            setCurrentPage(data.number);
        } catch (err) {
            console.error("Failed to fetch clinic-created records:", err);
            setError(err instanceof Error ? err.message : "Could not load records created by the clinic.");
        } finally {
            setIsLoadingData(false);
        }
    }, [token, clinicIdFromUser ]); 

    useEffect(() => {
        if (!isLoadingAuth && user && clinicIdFromUser) {
             fetchClinicCreatedRecords(0); 
       } else if (!isLoadingAuth && (!user || !clinicIdFromUser)) {
            setIsLoadingData(false);
            if (!user) setError("Authentication required to view records.");
            else if (!clinicIdFromUser) setError("Unable to fetch records: Clinic ID missing from your profile.");
        }
    },  [isLoadingAuth, user, clinicIdFromUser, fetchClinicCreatedRecords]);

    const handlePageChange = (newPage: number) => {
        fetchClinicCreatedRecords(newPage);
    };

    const handleSearchSubmit = (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        fetchClinicCreatedRecords(0);
    };
    
    const getRecordTypeIcon = (type: RecordType): JSX.Element => { 
        switch (type) {
            case RecordType.VACCINE: return <Syringe size={16} className="text-blue-400 mr-1.5" />;
            case RecordType.ANNUAL_CHECK: return <BookOpenCheck size={16} className="text-indigo-400 mr-1.5" />;
            case RecordType.FIRST_VISIT: return <FileText size={16} className="text-green-400 mr-1.5" />;
            case RecordType.ILLNESS: return <Thermometer size={16} className="text-red-400 mr-1.5" />;
            case RecordType.URGENCY: return <AlertTriangle size={16} className="text-orange-400 mr-1.5" />;
            default: return <Info size={16} className="text-gray-400 mr-1.5" />;
        }
    };

    const filteredRecords = useMemo(() => {
    if (!searchTerm) return records; 
    const lowerSearch = searchTerm.toLowerCase();
    return records.filter(record => 
        (record.petName?.toLowerCase().includes(lowerSearch)) ||
        (record.creator?.username?.toLowerCase().includes(lowerSearch)) ||
        (record.description?.toLowerCase().includes(lowerSearch)) ||
        (getRecordTypeDisplay(record.type).toLowerCase().includes(lowerSearch))
    );
    }, [records, searchTerm]);

     const handleOpenViewModal = (record: RecordViewDto) => {
        setSelectedRecord(record);
        setShowViewModal(true);
    };

    const handleOpenEditModalFromView = (recordToEdit: RecordViewDto) => {
        setShowViewModal(false); 
        setSelectedRecord(recordToEdit);
        setShowEditModal(true);   
    };
    
    const handleOpenDeleteConfirmModal = (record: RecordViewDto) => {
        setSelectedRecord(record);
        setShowDeleteConfirmModal(true);
    };
    
    const handleCloseAllModals = () => {
        setShowViewModal(false);
        setShowEditModal(false);
        setShowDeleteConfirmModal(false);
        setSelectedRecord(null);
    };

    const handleRecordUpdated = () => { 
        handleCloseAllModals();
        fetchClinicCreatedRecords(currentPage); 
    };

    const handleConfirmDeleteRecord = async () => {
        if (!token || !selectedRecord) return;
        setIsActionLoading(true);
        try {
            await deleteRecord(token, selectedRecord.id);
            toast.success("Medical record deleted successfully.");
            handleCloseAllModals();
            if (records.length === 1 && currentPage > 0) {
                fetchClinicCreatedRecords(currentPage - 1);
            } else {
                fetchClinicCreatedRecords(currentPage);
            }
        } catch (err) {
            toast.error(err instanceof Error ? err.message : "Could not delete medical record.");
        } finally {
            setIsActionLoading(false);
        }
    };

    if (isLoadingAuth) {
        return <div className="flex justify-center items-center py-10"><Loader2 className="h-8 w-8 animate-spin text-cyan-500" /> <span className="ml-2">Loading...</span></div>;
    }
    if (!user || !(user.roles?.includes("VET") || user.roles?.includes("ADMIN"))) {
        return (
            <div className="p-6 text-center bg-[#0c1225]/50 rounded-lg border border-red-500/30">
                <AlertCircle className="mx-auto h-12 w-12 text-red-400" />
                <h2 className="mt-2 text-xl font-semibold text-red-300">Access Denied</h2>
                <p className="text-gray-400">You do not have permission to view this page.</p>
            </div>
        );
    }
    if (!clinicIdFromUser) { 
        return (
            <div className="p-6 text-center bg-[#0c1225]/50 rounded-lg border border-red-500/30">
                <AlertCircle className="mx-auto h-12 w-12 text-red-400" />
                <h2 className="mt-2 text-xl font-semibold text-red-300">Configuration Error</h2>
                <p className="text-gray-400">{error || "Clinic information is missing for your account."}</p>
            </div>
        );
    }

    return (
      <div className="space-y-6">
        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
          <h1 className="text-2xl sm:text-3xl font-bold text-[#FFECAB] flex items-center">
            <ClipboardList className="mr-3 h-7 w-7 text-cyan-400" />
            Clinic record history
          </h1>
          <form onSubmit={handleSearchSubmit} className="flex items-center gap-x-4 gap-y-2 flex-wrap w-full sm:w-auto justify-end">
            <div className="relative">
              <Search size={18} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 pointer-events-none" />
              <Input
                  type="text"
                  placeholder="Search records..."
                  value={searchTerm}
                  onChange={(e: ChangeEvent<HTMLInputElement>) => setSearchTerm(e.target.value)}
                  className="pl-10 pr-3 py-2 h-9 w-full sm:w-64 border-gray-700 bg-[#070913]/80 rounded-md focus:ring-cyan-600"
              />
            </div>
          </form>
        </div>

        {error && (
          <div className="p-3 text-center text-red-400 bg-red-900/20 border border-red-500/50 rounded-lg flex items-center justify-center gap-2">
            <AlertCircle size={20} /> {error}
          </div>
        )}

        <Card className="border-2 border-[#FFECAB]/30 bg-[#0c1225]/70 shadow-xl">
          <CardHeader>
            <CardTitle className="text-[#FFECAB]">Records created by this clinic</CardTitle>
          </CardHeader>

          <CardContent className="p-0">
            {isLoadingData ? (
              <div className="flex justify-center items-center py-10">
                <Loader2 className="h-8 w-8 animate-spin text-cyan-500" />
                <span className="ml-2 text-gray-400">Loading records...</span>
              </div>
            ) : error ? (
              <div className="p-4 my-4 text-center text-red-400 bg-red-900/20 border border-red-500/50 rounded-lg">
                <AlertCircle size={20} className="inline mr-2" />
                {error}
              </div>
            ) : filteredRecords.length === 0 ? (
              <p className="text-center text-gray-400 py-8 px-4">
                {searchTerm
                  ? "No records match your search criteria."
                  : "No medical records have been created by this clinic yet."}
              </p>
            ) : (
              <Table>
                <TableHeader>
                  <TableRow className="border-b-[#FFECAB]/20">
                    <TableHead className="text-[#FFECAB]/80 pl-6">Date</TableHead>
                    <TableHead className="text-[#FFECAB]/80">Pet Name</TableHead>
                    <TableHead className="text-[#FFECAB]/80">Type</TableHead>
                    <TableHead className="text-[#FFECAB]/80 hidden sm:table-cell">Created By</TableHead>
                    <TableHead className="text-[#FFECAB]/80 text-center">Signed</TableHead>
                    <TableHead className="text-[#FFECAB]/80 text-right pr-6">Actions</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {filteredRecords.map((record) => {
                    const isRecordCreatorVet = record.creator?.roles?.includes('VET');
                    const isCurrentUserCreator = record.creator?.id === user?.id;
                    let canStaffDeleteThisRecord = false;
                    if (user?.roles?.includes('ADMIN') || user?.roles?.includes('VET')) { 
                                        if (!record.vetSignature) {
                                            canStaffDeleteThisRecord = true;
                                        } else if (isRecordCreatorVet && isCurrentUserCreator) {
                                            canStaffDeleteThisRecord = true;
                                        }
                                    }
                    return (
                      <TableRow key={record.id} className="border-b-[#FFECAB]/10 hover:bg-[#FFECAB]/5">
                        <TableCell className="pl-6 text-xs text-gray-300 tabular-nums">{formatDateTime(record.createdAt)}</TableCell>
                         <TableCell>
                          <div className="font-medium text-white">{record.petName || `Pet ID: ${record.petId}`}</div>
                          {record.petSpecie && <div className="text-xs text-gray-400 capitalize">{record.petSpecie.toLowerCase()}</div>}
                        </TableCell>
                        <TableCell className="text-gray-300">
                          <div className="flex items-center gap-1.5">
                            {getRecordTypeIcon(record.type)}{" "}
                            {getRecordTypeDisplay(record.type)}
                          </div>
                        </TableCell>
                        <TableCell className="text-gray-300 hidden sm:table-cell">
                          {formatRecordCreatorDisplay(record.creator)}
                        </TableCell>
                        <TableCell className="text-center">
                          {record.vetSignature ? (
                            <Tooltip>
                              <TooltipTrigger asChild>
                                <ShieldCheck
                                    size={18}
                                    className="text-green-400 mx-auto"
                                />
                              </TooltipTrigger>
                                <TooltipContent className="bg-gray-950 text-white border border-cyan-700">
                                    <p>Signed</p>
                                </TooltipContent>
                            </Tooltip> 
                          ) : (
                            <Tooltip>
                                <TooltipTrigger asChild>
                                  <ShieldAlert
                                      size={18}
                                      className="text-red-800 mx-auto"
                                  />
                                </TooltipTrigger>
                                <TooltipContent className="bg-gray-950 text-white border border-red-700">
                                    <p>Unsigned</p>
                                </TooltipContent>
                          </Tooltip> 
                          )}
                        </TableCell>
                        <TableCell className="text-right space-x-1 pr-6">
                          <Tooltip>
                            <TooltipTrigger asChild>
                              <Button
                                size="icon"
                                className="h-7 w-7 text-[#FFECAB] hover:text-[#090D1A]  hover:bg-[#FFECAB] cursor-pointer"
                                onClick={() => handleOpenViewModal(record)}
                              >
                                <Eye size={16} />
                              </Button>
                            </TooltipTrigger>
                            <TooltipContent className="bg-gray-950 text-white border border-cyan-700">
                              <p>View Details</p>
                            </TooltipContent>
                          </Tooltip>
                           {canStaffDeleteThisRecord && ( 
                           <Tooltip>
                              <TooltipTrigger asChild>
                                <Button
                                  size="icon"
                                  className="h-7 w-7 text-red-400 hover:text-gray-800 hover:bg-red-400 cursor-pointer"
                                  onClick={() => handleOpenDeleteConfirmModal(record)}
                                  disabled={isActionLoading}
                                >
                                  <Trash size={16} />
                                </Button>
                              </TooltipTrigger>
                            <TooltipContent className="bg-gray-950 text-white border border-red-700"><p>Delete Record</p></TooltipContent>
                            </Tooltip>
                            )}
                          </TableCell>
                      </TableRow>
                    );
                  })}
                </TableBody>
              </Table>
            )}
          </CardContent>
          {totalPages > 1 && !isLoadingData && filteredRecords.length > 0 && (
            <div className="p-4 border-t border-[#FFECAB]/20">
              <Pagination
                currentPage={currentPage}
                totalPages={totalPages}
                onPageChange={handlePageChange}
                isLoading={isLoadingData}
              />
            </div>
          )}
        </Card>
        {/* Modals */}
            {showViewModal && selectedRecord && user && (
                <ViewRecordModal
                    isOpen={showViewModal}
                    onClose={handleCloseAllModals}
                    record={selectedRecord}
                    canEditRecord={ 
                        !selectedRecord.vetSignature &&
                        selectedRecord.type !== RecordType.VACCINE &&
                        selectedRecord.createdInClinicId === user.clinicId 
                    }
                    onEditRequest={() => {
                        if(selectedRecord) handleOpenEditModalFromView(selectedRecord);
                    }}
                />
            )}

            {showEditModal && selectedRecord && user &&(
                 <EditRecordModal 
                    isOpen={showEditModal}
                    onClose={handleCloseAllModals}
                    recordInitialData={selectedRecord}
                    onRecordUpdated={handleRecordUpdated}
                />
            )}

            {showDeleteConfirmModal && selectedRecord && (
                <ConfirmationModal
                    isOpen={showDeleteConfirmModal}
                    onClose={handleCloseAllModals}
                    onConfirm={handleConfirmDeleteRecord}
                    title="Confirm Delete Record"
                    message={
                        <>Are you sure you want to delete this medical record?
                            <br/>(Type: <strong className="text-[#FFECAB]">{getRecordTypeDisplay(selectedRecord.type)}</strong>, Pet: <strong className="text-[#FFECAB]">{selectedRecord.petName || `ID ${selectedRecord.petId}`}</strong>)
                            <br/><br/>This action cannot be undone.
                        </>
                    }
                    confirmButtonText="Yes, Delete"
                    isLoading={isActionLoading}
                />
            )}

      </div>
    );
};

export default RecordManagementPage;