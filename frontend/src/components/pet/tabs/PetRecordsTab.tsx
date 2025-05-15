import { useState, useEffect, useCallback, JSX } from "react";
import { formatRecordCreatorDisplay, formatDateTime, getRecordTypeDisplay} from '@/utils/formatters';
import {
  PetProfileDto,
  RecordViewDto,
  Page,
  RecordType,
} from "@/types/apiTypes";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import {
  PlusCircle,
  FileText,
  Trash,
  Eye,
  Loader2,
  AlertCircle,
  ShieldCheck,
  ShieldAlert,
  Thermometer,
  Syringe,
  AlertTriangle,
  Info,
  BookOpenCheck,
  Share2
} from "lucide-react";
import { useAuth } from "@/hooks/useAuth";
import { findRecordsByPetId, deleteRecord} from "@/services/recordService";
import Pagination from "@/components/common/Pagination";
import AddRecordModal from "@/components/pet/modals/AddRecordModal";
import ViewRecordModal from "@/components/pet/modals/ViewRecordModal";
import EditRecordModal from "@/components/pet/modals/EditRecordModal";
import ConfirmationModal from "@/components/common/ConfirmationModal";
import RequestTempAccessModal from '../modals/RequestTempAccessModal'; 
import ShowTempAccessModal from '../modals/ShowTempAccessModal'; 
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from "@/components/ui/tooltip";
import { toast } from 'sonner';

interface PetRecordsTabProps {
  pet: PetProfileDto;
}

const PAGE_SIZE_RECORDS = 5;

/**
 * PetRecordsTab - Displays a list of medical records for the selected pet.
 * Allows Owners to view, add, edit (own, unsigned), and delete (own) records.
 * @param {PetRecordsTabProps} props - Component props.
 * @returns {JSX.Element} The Pet Records Tab content.
 */
const PetRecordsTab = ({ pet }: PetRecordsTabProps): JSX.Element => {
  const { token, user } = useAuth();
  const [records, setRecords] = useState<RecordViewDto[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [isActionLoading, setIsActionLoading] = useState<boolean>(false);
  const [error, setError] = useState<string>("");
  const [currentPage, setCurrentPage] = useState<number>(0);
  const [totalPages, setTotalPages] = useState<number>(0);

  const [showAddModal, setShowAddModal] = useState<boolean>(false);
  const [showViewModal, setShowViewModal] = useState<boolean>(false);
  const [showEditModal, setShowEditModal] = useState<boolean>(false);
  const [showDeleteConfirmModal, setShowDeleteConfirmModal] = useState<boolean>(false);
  const [selectedRecord, setSelectedRecord] = useState<RecordViewDto | null>(null);

  const [showRequestTempAccessModal, setShowRequestTempAccessModal] = useState<boolean>(false);
  const [generatedTempAccessToken, setGeneratedTempAccessToken] = useState<string | null>(null);
  const [showShowTempAccessModal, setShowShowTempAccessModal] = useState<boolean>(false);

  const fetchRecords = useCallback(
    async (pageToFetch: number) => {
      if (!token || !pet?.id) {
        setIsLoading(false);
        return;
      }
      setIsLoading(true);
      setError("");
      try {
        const data: Page<RecordViewDto> = await findRecordsByPetId(token, {
          petId: pet.id,
          page: pageToFetch,
          size: PAGE_SIZE_RECORDS,
          sort: "createdAt,desc",
        });
        setRecords(data.content);
        setTotalPages(data.totalPages);
        setCurrentPage(data.number);
      } catch (err) {
        setError(
          err instanceof Error ? err.message : "Failed to load medical records."
        );
      } finally {
        setIsLoading(false);
      }
    },
    [token, pet?.id]
  );

  useEffect(() => {
    if (pet?.id) {
      fetchRecords(0);
    }
  }, [fetchRecords, pet?.id]);

  const handlePageChange = (newPage: number) => {
    fetchRecords(newPage);
  };

  const handleOpenAddModal = () => setShowAddModal(true);

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

   const handleRecordAdded = () => {
    setShowAddModal(false);
    fetchRecords(currentPage); 
  };

   const handleConfirmDeleteRecord = async () => {
    if (!token || !selectedRecord) return;
    setIsActionLoading(true);
    try {
        await deleteRecord(token, selectedRecord.id);
        toast.success("Medical record deleted successfully.");
        setShowDeleteConfirmModal(false);
        setSelectedRecord(null);
        if (records.length === 1 && currentPage > 0) {
            fetchRecords(currentPage - 1);
        } else {
            fetchRecords(currentPage);
        }
    } catch (err) {
        console.error("Failed to delete record:", err);
        toast.error(err instanceof Error ? err.message : "Could not delete medical record.");
    } finally {
        setIsActionLoading(false);
    }
  };

  const handleOpenRequestTempAccessModal = () => {
        if (user?.id === pet.ownerId) {
            setShowRequestTempAccessModal(true);
        } else {
            toast.error("Only the pet owner can share access to the medical history.");
        }
    };

  /**
  * Called when the temporary access token is successfully generated.
  * Closes the request modal and opens the modal to display the token/QR.
  * @param {string} accessToken - The generated temporary access token.
  */
  const handleTempTokenGenerated = (accessToken: string) => {
        setShowRequestTempAccessModal(false);
        setGeneratedTempAccessToken(accessToken);
        setShowShowTempAccessModal(true); // Open the modal to show the token and QR
  };

  const getRecordTypeIcon = (type: RecordType): JSX.Element => {
    switch (type) {
      case RecordType.VACCINE:
        return <Syringe size={16} className="text-blue-400 mr-1.5" />;
      case RecordType.ANNUAL_CHECK:
        return <BookOpenCheck size={16} className="text-indigo-400 mr-1.5" />;
      case RecordType.FIRST_VISIT:
        return <FileText size={16} className="text-green-400 mr-1.5" />;
      case RecordType.ILLNESS:
        return <Thermometer size={16} className="text-red-400 mr-1.5" />;
      case RecordType.URGENCY:
        return <AlertTriangle size={16} className="text-orange-400 mr-1.5" />;
      case RecordType.OTHER:
      default:
        return <Info size={16} className="text-gray-400 mr-1.5" />;
    }
  };

  return (
    <Card className="border-2 border-[#FFECAB]/50 bg-[#0c1225]/70 shadow-xl">
      <CardHeader>
        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-2">
          <CardTitle className="text-[#FFECAB] text-xl flex items-center">
            <FileText size={24} className="mr-2 text-cyan-400" />
            Medical Records for {pet.name}
          </CardTitle>
          <div className="sm:items-end mx-auto">
            {user?.id === pet.ownerId &&
                records.some((r) => r.vetSignature) && ( // Only show owner and Show only if there are signed records
                <Tooltip>
                  <TooltipTrigger asChild>
                    <Button
                      onClick={handleOpenRequestTempAccessModal}
                      size="sm"
                      className="text-purple-300 hover:bg-purple-700/50 hover:text-purple-200 border border-purple-400 cursor-pointer px-3 mr-2"
                    >
                      <Share2 size={16} className="sm:mr-2" />
                      <p>Share <span className="hidden sm:inline">Signed History</span></p>
                    </Button>
                  </TooltipTrigger>
                  <TooltipContent className="bg-gray-950 text-white border border-purple-700">
                    <p>Share Signed History</p>
                  </TooltipContent>
                </Tooltip>
              )}
            <Tooltip>
              <TooltipTrigger asChild>
                <Button
                  onClick={handleOpenAddModal}
                  size="sm"
                  className="px-5 py-2.5 rounded-lg border border-[#FFECAB]/50 bg-cyan-800 text-[#FFECAB] hover:bg-cyan-600 focus-visible:ring-cyan-500 disabled:opacity-50 cursor-pointer"
                >
                  <PlusCircle size={16} className="sm:mr-2" />
                  <p>Add <span className="hidden sm:inline">New Record</span></p>
                </Button>
              </TooltipTrigger>
              <TooltipContent className="bg-gray-950 text-white border border-cyan-700">
                <p>Add New Record</p>
              </TooltipContent>
            </Tooltip>
          </div>
        </div>
      </CardHeader>
      <CardContent>
        {isLoading && (
          <div className="flex justify-center items-center py-10">
            <Loader2 className="h-8 w-8 animate-spin text-cyan-500" />
            <span className="ml-2 text-gray-300">Loading records...</span>
          </div>
        )}
        {error && !isLoading && (
          <div className="p-4 my-4 text-center text-red-400 bg-red-900/20 border border-red-500/50 rounded-lg">
            <AlertCircle size={20} className="inline mr-2" />
            {error}
          </div>
        )}
        {!isLoading && !error && records.length === 0 && (
          <p className="text-gray-400 italic text-center py-8">
            No medical records found for {pet.name}.
          </p>
        )}
        {!isLoading && !error && records.length > 0 && (
          <div className="space-y-3">
            {records.map((record) => {
              const isOwnerRecord = record.creator?.id === user?.id;
              const canDeleteThisRecord = isOwnerRecord;
              return (
                <div
                  key={record.id}
                  className="bg-gray-800/60 p-3 sm:p-4 rounded-lg border border-gray-700 hover:border-cyan-600/50 transition-all"
                >
                  <div className="flex flex-col sm:flex-row justify-between items-start">
                    <div>
                      <h4 className="font-semibold text-base text-white mb-0.5 flex items-center gap-1.5">
                        {getRecordTypeIcon(record.type)}{" "}
                        {getRecordTypeDisplay(record.type)}
                        {record.vetSignature && (
                          <Tooltip>
                            <TooltipTrigger asChild>
                              <ShieldCheck
                                size={14}
                                className="inline ml-2 text-green-400 cursor-help"
                              />
                            </TooltipTrigger>
                            <TooltipContent className="bg-gray-950 text-white border border-green-700">
                              <p>Digitally Signed</p>
                            </TooltipContent>
                          </Tooltip>
                        )}
                        {!record.vetSignature &&
                          record.creator?.roles?.includes("VET") && (
                            <Tooltip>
                              <TooltipTrigger asChild>
                                <ShieldAlert
                                  size={14}
                                  className="inline ml-2 text-yellow-400 cursor-help"
                                />
                              </TooltipTrigger>
                              <TooltipContent className="bg-gray-950 text-white border border-yellow-700">
                                <p>Created by Vet, Not Signed</p>
                              </TooltipContent>
                            </Tooltip>
                          )}
                      </h4>
                      <p className="text-xs text-gray-400">
                        On: {formatDateTime(record.createdAt)} by{" "}
                        {formatRecordCreatorDisplay(record.creator)}
                      </p>
                    </div>
                    <div className="flex gap-1.5 mt-2 sm:mt-0 self-start sm:self-center">
                      <Button
                        size="sm"
                        className="px-5 py-2.5 rounded-lg border border-[#FFECAB]/50 text-[#090D1A] bg-[#FFECAB] hover:text-[#FFECAB] hover:bg-[#090D1A] focus-visible:ring-cyan-500 disabled:opacity-50 cursor-pointer"
                        onClick={() => handleOpenViewModal(record)}
                      >
                        <Eye size={14} className="mr-1" />
                        View
                      </Button>
                      {canDeleteThisRecord && (
                        <Button
                          size="sm"
                          className="px-5 py-2.5 rounded-lg border border-[#FFECAB]/50 text-[#FFECAB] hover:bg-red-800 hover:text-[#FFECAB] focus-visible:ring-red-500 disabled:opacity-50 cursor-pointer"
                          onClick={() => handleOpenDeleteConfirmModal(record)}
                        >
                          <Trash size={14} className="mr-1" />
                          Delete
                        </Button>
                      )}
                    </div>
                  </div>

                  {record.type === RecordType.VACCINE && record.vaccine && (
                    <div className="mt-2 pt-2 border-t border-gray-700/50 text-xs">
                      <p className="font-medium text-cyan-400">
                        Vaccine: {record.vaccine.name}
                      </p>
                      <p className="text-gray-400">
                        Batch: {record.vaccine.batchNumber}, Lab:{" "}
                        {record.vaccine.laboratory || "N/A"}, Validity:{" "}
                        {record.vaccine.validity ?? "N/A"} yrs
                      </p>
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        )}

        {totalPages > 0 && (
          <Pagination
            currentPage={currentPage}
            totalPages={totalPages}
            onPageChange={handlePageChange}
            isLoading={isLoading}
          />
        )}
      </CardContent>

      {showAddModal && pet?.id && (
        <AddRecordModal
          isOpen={showAddModal}
          onClose={() => setShowAddModal(false)}
          onRecordAdded={handleRecordAdded}
          petId={pet.id}
        />
      )}

      {showViewModal && selectedRecord && user && (
        <ViewRecordModal
          isOpen={showViewModal}
          onClose={() => {
            setShowViewModal(false);
            setSelectedRecord(null);
          }}
          record={selectedRecord}
          canEditRecord={
            !!user &&
            selectedRecord.creator?.id === user.id &&
            selectedRecord.type !== RecordType.VACCINE &&
            !selectedRecord.vetSignature
          }
          onEditRequest={() => {
            setShowViewModal(false);
            if (selectedRecord) {
              handleOpenEditModalFromView(selectedRecord);
            }
          }}
        />
      )}

      {showEditModal && selectedRecord && user && (
        <EditRecordModal
          isOpen={showEditModal}
          onClose={() => {
            setShowEditModal(false);
            setSelectedRecord(null);
          }}
          recordInitialData={selectedRecord}
          onRecordUpdated={() => {
            setShowEditModal(false);
            setSelectedRecord(null);
            fetchRecords(currentPage);
          }}
        />
      )}

      {showDeleteConfirmModal && selectedRecord && (
        <ConfirmationModal
          isOpen={showDeleteConfirmModal}
          onClose={() => {
            setShowDeleteConfirmModal(false);
            setSelectedRecord(null);
          }}
          onConfirm={handleConfirmDeleteRecord}
          title="Confirm Delete Record"
          message={
            <>
              Are you sure you want to delete this medical record?
              <br />
              (Type:{" "}
              <strong className="text-[#FFECAB]">
                {getRecordTypeDisplay(selectedRecord.type)}
              </strong>
              , Created:{" "}
              <strong className="text-[#FFECAB]">
                {formatDateTime(selectedRecord.createdAt)}
              </strong>
              )
              <br />
              <br />
              This action cannot be undone.
            </>
          }
          confirmButtonText="Yes, Delete"
          isLoading={isActionLoading}
        />
      )}

      {showRequestTempAccessModal && pet?.id && user?.id === pet.ownerId && (
        <RequestTempAccessModal
          isOpen={showRequestTempAccessModal}
          onClose={() => setShowRequestTempAccessModal(false)}
          petId={pet.id}
          onTokenGenerated={handleTempTokenGenerated}
        />
      )}

      {showShowTempAccessModal && generatedTempAccessToken && (
        <ShowTempAccessModal
          isOpen={showShowTempAccessModal}
          onClose={() => {
            setShowShowTempAccessModal(false);
            setGeneratedTempAccessToken(null);
          }}
          accessToken={generatedTempAccessToken}
          petName={pet.name}
        />
      )}
    </Card>
  );
};

export default PetRecordsTab;
