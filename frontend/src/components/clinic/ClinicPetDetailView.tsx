import { useState, useEffect, useCallback, JSX } from 'react';
import { formatRecordCreatorDisplay, formatDateTime, getRecordTypeDisplay  } from '@/utils/formatters';
import { PetProfileDto,  VetSummaryDto, RecordViewDto, RecordType, } from '@/types/apiTypes';
import { UserCircle, Edit, Mail, Phone, FileText, PlusCircle, Trash, Eye as EyeIcon, Loader2, AlertCircle, ShieldCheck,  Thermometer, Syringe, AlertTriangle, Info, BookOpenCheck } from 'lucide-react'; 
import { Button } from "@/components/ui/button";
import { Card, CardHeader, CardTitle, CardContent } from "@/components/ui/card";
import { useAuth } from '@/hooks/useAuth'; 
import { findRecordsByPetId } from '@/services/recordService'; 
import Pagination from '@/components/common/Pagination';
import AddRecordModal from '@/components/pet/modals/AddRecordModal'; 
import ViewRecordModal from '@/components/pet/modals/ViewRecordModal';
import EditRecordModal from '@/components/pet/modals/EditRecordModal';
import ConfirmationModal from '@/components/common/ConfirmationModal';
import { Tooltip, TooltipTrigger, TooltipContent } from "@/components/ui/tooltip";
import { toast } from 'sonner';

interface ClinicPetDetailViewProps {
    petProfile: PetProfileDto;
    onEdit?: () => void; 
    currentUserRoles: string[];
    onRecordDataChange?: () => void;
}

const PAGE_SIZE_PET_RECORDS_IN_DETAIL = 3;

/**
  * ClinicPetDetailView - Displays full details of a pet from the clinic staff's perspective.
 * Includes an "Edit" button for pet info if staff has permission.
 * Also includes a section for managing the pet's medical records.
 * @param {ClinicPetDetailViewProps} props - Component props.
 * @returns {JSX.Element} The pet detail view component.
 */
const ClinicPetDetailView = ({ petProfile, onEdit, currentUserRoles, onRecordDataChange }: ClinicPetDetailViewProps): JSX.Element => {
    const { token, user } = useAuth(); 

    const [petRecords, setPetRecords] = useState<RecordViewDto[]>([]);
    const [isLoadingRecords, setIsLoadingRecords] = useState<boolean>(true);
    const [recordsError, setRecordsError] = useState<string>('');
    const [recordsCurrentPage, setRecordsCurrentPage] = useState<number>(0);
    const [recordsTotalPages, setRecordsTotalPages] = useState<number>(0);

    const [showAddRecordModal, setShowAddRecordModal] = useState<boolean>(false);
    const [showViewRecordModal, setShowViewRecordModal] = useState<boolean>(false);
    const [showEditRecordModal, setShowEditRecordModal] = useState<boolean>(false);
    const [showDeleteRecordModal, setShowDeleteRecordModal] = useState<boolean>(false);
    const [selectedRecord, setSelectedRecord] = useState<RecordViewDto | null>(null);
    const [isRecordActionLoading, setIsRecordActionLoading] = useState<boolean>(false);


    const formatVetList = (vets: VetSummaryDto[] | undefined): string => {
        if (!vets || vets.length === 0) return 'None assigned';
        return vets.map(vet => `${vet.name || ''} ${vet.surname || ''}`.trim()).join(', ');
    };
    
    const getRecordTypeIcon = (type: RecordType | undefined): JSX.Element => {
        if (!type) return <Info size={16} className="text-gray-400 mr-1.5" />;
        switch (type) {
            case RecordType.VACCINE: return <Syringe size={16} className="text-blue-400 mr-1.5" />;
            case RecordType.ANNUAL_CHECK: return <BookOpenCheck size={16} className="text-indigo-400 mr-1.5" />;
            case RecordType.FIRST_VISIT: return <FileText size={16} className="text-green-400 mr-1.5" />;
            case RecordType.ILLNESS: return <Thermometer size={16} className="text-red-400 mr-1.5" />;
            case RecordType.URGENCY: return <AlertTriangle size={16} className="text-orange-400 mr-1.5" />;
            default: return <Info size={16} className="text-gray-400 mr-1.5" />;
        }
    };

const canEdit = onEdit && (currentUserRoles.includes('VET') || currentUserRoles.includes('ADMIN'));

const fetchPetRecords = useCallback(async (page: number) => {
        if (!token || !petProfile?.id) return;
        setIsLoadingRecords(true);
        setRecordsError('');
        try {
            const data = await findRecordsByPetId(token, {
                petId: petProfile.id,
                page,
                size: PAGE_SIZE_PET_RECORDS_IN_DETAIL,
                sort: 'createdAt,desc'
            });
            setPetRecords(data.content);
            setRecordsTotalPages(data.totalPages);
            setRecordsCurrentPage(data.number);
        } catch (err) {
            setRecordsError(err instanceof Error ? err.message : "Failed to load pet's medical records.");
        } finally {
            setIsLoadingRecords(false);
        }
    }, [token, petProfile?.id]);

    useEffect(() => {
        if (petProfile?.id) {
            fetchPetRecords(0);
        }
    }, [petProfile, fetchPetRecords]); 

    const handleRecordsPageChange = (newPage: number) => {
        fetchPetRecords(newPage);
    };

    const handleOpenAddRecordModal = () => setShowAddRecordModal(true);
    const handleOpenViewRecordModal = (record: RecordViewDto) => { setSelectedRecord(record); setShowViewRecordModal(true); };
    const handleOpenDeleteRecordModal = (record: RecordViewDto) => { setSelectedRecord(record); setShowDeleteRecordModal(true); };
    
    const handleCloseAllRecordModals = () => {
        setShowAddRecordModal(false);
        setShowViewRecordModal(false);
        setShowEditRecordModal(false);
        setShowDeleteRecordModal(false);
        setSelectedRecord(null);
    };

    const handleRecordActionSuccess = () => {
        handleCloseAllRecordModals();
        fetchPetRecords(recordsCurrentPage); 
        if (onRecordDataChange) onRecordDataChange(); 
    };
    
    const handleConfirmDeleteRecord = async () => {
        if (!token || !selectedRecord) return;
        setIsRecordActionLoading(true);
        try {
            // await deleteRecord(token, selectedRecord.id); 
            toast.success(`Record (ID: ${selectedRecord.id}) deleted successfully.`); 
            handleRecordActionSuccess();
        } catch (err) {
            toast.error(err instanceof Error ? err.message : "Failed to delete record.");
        } finally {
            setIsRecordActionLoading(false);
        }
    };

    return (
        <div className="space-y-5">
            <div className="flex flex-col sm:flex-row items-center sm:items-start gap-4 sm:gap-6 pb-4 border-b border-[#FFECAB]/20">
                <div className="flex-shrink-0">
                    <img
                        src={petProfile.image || `/src/assets/images/avatars/pets/${petProfile.specie.toLowerCase()}.png`}
                        alt={`${petProfile.name}'s avatar`}
                        className="w-24 h-24 sm:w-32 sm:h-32 rounded-full object-cover border-4 border-[#FFECAB]/60 bg-gray-700"
                        onError={(e) => (e.currentTarget.src = `/src/assets/images/avatars/pets/${petProfile.specie.toLowerCase()}.png`)}
                    />
                </div>
                <div className="text-center sm:text-left flex-grow">
                    <h3 className="text-2xl font-semibold text-white">{petProfile.name}</h3>
                    <p className="text-cyan-400 capitalize">{petProfile.specie.toLowerCase()} · {petProfile.breedName || 'N/A'}</p>
                    <div className="mt-1 text-sm text-gray-300">
                        <p className="font-medium text-gray-200"><UserCircle size={14} className="text-[#FFECAB] inline mr-1.5 align-middle" /> {petProfile.ownerUsername}</p>
                        <p className="font-medium text-gray-200"><Mail size={14}  className="text-[#FFECAB] inline mr-1.5 align-middle"  /> {petProfile.ownerDetails?.email}</p>
                        <p className="font-medium text-gray-200"><Phone size={14}  className="text-[#FFECAB] inline mr-1.5 align-middle"  /> {petProfile.ownerDetails?.phone}</p>
                    </div>
                </div>
                {canEdit && (
                    <Button onClick={onEdit} size="sm" className="px-5 py-2.5 rounded-lg border border-[#FFECAB]/50 bg-cyan-800 text-[#FFECAB] hover:bg-cyan-600 focus-visible:ring-cyan-500 disabled:opacity-50 cursor-pointer">
                        <Edit size={16} className="mr-2" /> Edit Pet Info
                    </Button>
                )}
            </div>
            <div className="grid grid-cols-1 gap-2 sm:flex sm:gap-10 text-sm font-medium text-gray-400">
                <p>Gender:<span className="text-white capitalize ml-2">{petProfile.gender?.toLowerCase() || 'N/A'}</span></p>
                <p>Birth Date:<span className="text-white ml-2">{formatDateTime(petProfile.birthDate)}</span></p>  
                <p>Microchip:<span className="text-white ml-2">{petProfile.microchip || 'N/A'}</span></p>                  
                <p>Color:<span className="text-white ml-2">{petProfile.color || 'N/A'}</span></p>                
            </div>
            
            {/* Vets*/}
            <div className="pt-3 border-t border-[#FFECAB]/20">
                <h4 className="font-medium text-gray-300">Associated Veterinarian(s):</h4>
                <p className="text-white text-sm">{formatVetList(petProfile.associatedVets)}</p>
            </div>

            {/* Records */}
            <div className="mt-0 border-t border-[#FFECAB]/20"> 
                <Card className="border-none shadow-none bg-transparent py-3">
                    <CardHeader className="px-0"> 
                        <div className="flex justify-between items-center">
                            <CardTitle className="text-xl text-[#FFECAB] flex items-center">
                                <FileText size={22} className="mr-2 text-cyan-400"/>Medical Records
                            </CardTitle>
                            <Button onClick={handleOpenAddRecordModal} size="sm" className="px-5 py-2.5 rounded-lg border border-[#FFECAB]/50 bg-cyan-800 text-[#FFECAB] hover:bg-cyan-600 focus-visible:ring-cyan-500 disabled:opacity-50 cursor-pointer">
                                <PlusCircle size={16} className="mr-2"/>Add Record
                            </Button>
                        </div>
                    </CardHeader>
                    <CardContent className="px-0">
                        {isLoadingRecords && <div className="text-center py-6"><Loader2 className="h-6 w-6 animate-spin text-cyan-500 inline-flex"/></div>}
                        {recordsError && <div className="text-center py-6 text-red-400"><AlertCircle className="inline mr-2"/>{recordsError}</div>}
                        {!isLoadingRecords && !recordsError && petRecords.length === 0 && (
                            <p className="text-gray-400 italic text-center py-6">No medical records found for {petProfile.name}.</p>
                        )}
                        {!isLoadingRecords && !recordsError && petRecords.length > 0 && (
                            <div className="space-y-2">
                                {petRecords.map(record => {
                                    const isRecordCreatorVet = record.creator?.roles?.includes('VET');
                                    const isCurrentUserCreator = record.creator?.id === user?.id;
                                    const isRecordFromThisClinic = record.createdInClinicId === user?.clinicId;

                                    let canStaffDelete = false;

                                    if (user?.roles?.includes('ADMIN') || user?.roles?.includes('VET')) {
                                        if (!record.vetSignature && isRecordFromThisClinic) {
                                            canStaffDelete = true; // Admins and Vets can delete unsigned from their clinic
                                        }
                                        if (record.vetSignature && isCurrentUserCreator && isRecordCreatorVet) {
                                            // Only the VET who signed can delete his own signed record
                                            canStaffDelete = true;
                                        }
                                    }

                                    return (
                                        <div key={record.id} className="bg-gray-800/70 p-3 rounded-md border border-gray-700">
                                            <div className="flex justify-between items-start">
                                                <div>
                                                    <h5 className="font-semibold text-sm text-white flex items-center gap-1.5">
                                                        {getRecordTypeIcon(record.type)} {getRecordTypeDisplay(record.type)}
                                                        {record.vetSignature && <Tooltip><TooltipTrigger asChild><ShieldCheck size={12} className="text-green-400"/></TooltipTrigger><TooltipContent className="bg-gray-950 text-white border  border-green-400"><p>Signed</p></TooltipContent></Tooltip>}
                                                    </h5>
                                                    <p className="text-xs text-gray-400">
                                                        {formatDateTime(record.createdAt)} by {formatRecordCreatorDisplay(record.creator)}
                                                    </p>
                                                </div>
                                                <div className="flex gap-1">
                                                    <Tooltip>
                                                        <TooltipTrigger asChild>
                                                            <Button size="icon" className="h-7 w-7 text-[#FFECAB] hover:text-[#090D1A]  hover:bg-[#FFECAB] cursor-pointer" onClick={() => handleOpenViewRecordModal(record)}>
                                                                <EyeIcon size={15}/>
                                                            </Button>
                                                        </TooltipTrigger>
                                                    <TooltipContent className="bg-gray-950 text-white border border-cyan-700"><p>View Details</p></TooltipContent>
                                                </Tooltip>    
                                                    {(canStaffDelete) && (
                                                         <Tooltip>
                                                        <TooltipTrigger asChild>
                                                        <Button size="icon" className="h-7 w-7 text-red-400 hover:text-gray-800 hover:bg-red-400 cursor-pointer"
                                                            onClick={() => handleOpenDeleteRecordModal(record)}
                                                        ><Trash size={15}/></Button>
                                                         </TooltipTrigger>
                                                    <TooltipContent className="bg-gray-950 text-white border border-red-700"><p>Delete Record</p></TooltipContent>
                                                </Tooltip> 
                                                    )}
                                                </div>
                                            </div>
                                            {record.type === RecordType.VACCINE && record.vaccine && (
                                                 <div className="mt-1.5 pt-1.5 border-t border-gray-700/50 text-xs text-gray-300 ">
                                                    Vaccine: {record.vaccine.name} (Batch: {record.vaccine.batchNumber})
                                                 </div>
                                            )}
                                        </div>
                                    );
                                })}
                            </div>
                        )}
                        {recordsTotalPages > 1 && (
                            <Pagination currentPage={recordsCurrentPage} totalPages={recordsTotalPages} onPageChange={handleRecordsPageChange} isLoading={isLoadingRecords} />
                        )}
                    </CardContent>
                </Card>
            </div>

            {(petProfile.createdAt || petProfile.updatedAt) && (
                <div className="pt-3 border-t border-[#FFECAB]/20 mt-4 text-xs text-gray-500">
                    {petProfile.createdAt && <p>Registered on: {formatDateTime(petProfile.createdAt)}</p>}
                    {petProfile.updatedAt && <p>Last updated: {formatDateTime(petProfile.updatedAt)}</p>}
                </div>
            )}
            {/* Modals */}
            {showAddRecordModal && petProfile?.id && (
                <AddRecordModal
                    isOpen={showAddRecordModal}
                    onClose={handleCloseAllRecordModals}
                    onRecordAdded={() => { handleRecordActionSuccess(); toast.success("Record added!");}}
                    petId={petProfile.id}
                />
            )}
            {showViewRecordModal && selectedRecord && user &&(
                <ViewRecordModal
                    isOpen={showViewRecordModal}
                    onClose={handleCloseAllRecordModals}
                    record={selectedRecord}
                    canEditRecord={ 
                        !selectedRecord.vetSignature &&
                        selectedRecord.type !== RecordType.VACCINE &&
                        ( (selectedRecord.creator.id === user.id && !user.roles?.includes('VET') && !user.roles?.includes('ADMIN') ) || 
                          ( (user.roles?.includes('ADMIN') || user.roles?.includes('VET')) && selectedRecord.createdInClinicId === user.clinicId )
                        )
                    }
                    onEditRequest={() => {
                        setShowViewRecordModal(false); 
                        setShowEditRecordModal(true); 
                    }}
                />
            )}
            {showEditRecordModal && selectedRecord && (
                 <EditRecordModal
                    isOpen={showEditRecordModal}
                    onClose={handleCloseAllRecordModals}
                    recordInitialData={selectedRecord}
                    onRecordUpdated={() => {handleRecordActionSuccess(); toast.success("Record updated!");}}
                />
            )}
            {showDeleteRecordModal && selectedRecord && (
                <ConfirmationModal
                    isOpen={showDeleteRecordModal}
                    onClose={handleCloseAllRecordModals}
                    onConfirm={handleConfirmDeleteRecord} 
                    title="Confirm Delete Record"
                    message={`Are you sure you want to delete this record (Type: ${selectedRecord.type})? This action cannot be undone.`}
                    confirmButtonText="Yes, Delete"
                    isLoading={isRecordActionLoading}
                />
            )}
        </div>
    );
};

export default ClinicPetDetailView;