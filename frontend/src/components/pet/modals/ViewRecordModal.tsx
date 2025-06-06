import { JSX } from 'react';
import { formatRecordCreatorDisplay, formatDateTime, getRecordTypeDisplay} from '@/utils/formatters';
import Modal from '@/components/common/Modal';
import { RecordViewDto, RecordType } from '@/types/apiTypes'; 
import { Button } from '@/components/ui/button';
import { CalendarDays, UserCircle, Edit3, ShieldCheck, FileText, Thermometer, Syringe, AlertTriangle, Info, BookOpenCheck } from 'lucide-react'; 

interface ViewRecordModalProps {
    isOpen: boolean;
    onClose: () => void;
    record: RecordViewDto;
    canEditRecord: boolean; 
    onEditRequest: () => void; 
    isPetActive: boolean;
}

/**
 * ViewRecordModal - Displays detailed information for a specific medical record.
 * Optionally provides an "Edit" button if the record is editable by the current user.
 * @param {ViewRecordModalProps} props - Component props.
 * @returns {JSX.Element | null} The modal component or null if not open.
 * @author ibosquet
 */
const ViewRecordModal = ({ isOpen, onClose, record, canEditRecord, onEditRequest,  isPetActive }: ViewRecordModalProps): JSX.Element | null => {

    const getRecordTypeIcon = (type: RecordType): JSX.Element => {
        switch (type) {
            case RecordType.VACCINE: return <Syringe size={18} className="text-blue-400" />;
            case RecordType.ANNUAL_CHECK: return <BookOpenCheck size={16} className="text-indigo-400 mr-1.5" />;
            case RecordType.FIRST_VISIT: return <FileText size={18} className="text-green-400" />;
            case RecordType.ILLNESS: return <Thermometer size={18} className="text-red-400" />;
            case RecordType.URGENCY: return <AlertTriangle size={18} className="text-orange-400" />;
            case RecordType.OTHER:
            default: return <Info size={18} className="text-gray-400" />;
        }
    };

    const creatorDisplay = formatRecordCreatorDisplay(record.creator);

    if (!isOpen) return null;

    return (
        <Modal title="Medical Record Details" onClose={onClose} maxWidth="max-w-2xl">
            <div className="space-y-4">
                {/* Record Type and Date */}
                <div className="pb-3 border-b border-[#FFECAB]/20">
                    <div className="flex items-center gap-2 mb-1">
                        {getRecordTypeIcon(record.type)}
                        <h3 className="text-xl font-semibold text-white">{getRecordTypeDisplay(record.type)}</h3>
                    </div>
                    <p className="text-sm text-gray-400 flex items-center gap-1.5">
                        <CalendarDays size={14}/> Created on: {formatDateTime(record.createdAt)}
                    </p>
                    <p className="text-sm text-gray-400 flex items-center gap-1.5">
                        <UserCircle size={14}/> Created by: <span className="font-medium text-gray-300">{creatorDisplay}</span>
                    </p>
                </div>

                {/* Description */}
                <div>
                    <h4 className="font-medium text-gray-300 mb-1">Description:</h4>
                    <p className="text-sm text-white bg-gray-800/50 p-3 rounded-md whitespace-pre-wrap min-h-[60px]">
                        {record.description || <span className="italic text-gray-500">No description provided.</span>}
                    </p>
                </div>

                {/* Vaccine Details (if applicable) */}
                {record.type === RecordType.VACCINE && record.vaccine && (
                    <div className="pt-3 border-t border-[#FFECAB]/20">
                        <h4 className="text-md font-semibold text-[#FFECAB] mb-2">Vaccine Information</h4>
                        <div className="grid grid-cols-1 sm:grid-cols-2 gap-x-4 gap-y-1 text-sm">
                            <p><strong className="text-gray-400">Name:</strong> <span className="text-white">{record.vaccine.name}</span></p>
                            <p><strong className="text-gray-400">Batch No:</strong> <span className="text-white">{record.vaccine.batchNumber}</span></p>
                            <p><strong className="text-gray-400">Laboratory:</strong> <span className="text-white">{record.vaccine.laboratory || 'N/A'}</span></p>
                            <p><strong className="text-gray-400">Validity:</strong> <span className="text-white">{record.vaccine.validity ?? 'N/A'} years</span></p>
                            <p className="sm:col-span-2"><strong className="text-gray-400">Rabies Vaccine:</strong> <span className="text-white">{record.vaccine.isRabiesVaccine ? 'Yes' : 'No'}</span></p>
                        </div>
                    </div>
                )}

                {/* Digital Signature (if applicable) */}
                {record.vetSignature && (
                    <div className="pt-3 border-t border-[#FFECAB]/20">
                        <h4 className="text-md font-semibold text-green-400 mb-1 flex items-center gap-2">
                            <ShieldCheck size={18}/> Digitally Signed
                        </h4>
                        <p className="text-xs text-gray-400 break-all bg-gray-800/50 p-2 rounded-md">
                            Signature: <span className="text-gray-300">{record.vetSignature}</span>
                        </p>
                    </div>
                )}

                {/* Action Buttons */}
                <div className="flex justify-end gap-3 pt-4 border-t border-[#FFECAB]/20 mt-5">
                    {canEditRecord && (
                        <Button onClick={onEditRequest} className="px-5 py-2.5 rounded-lg border border-[#FFECAB]/50 bg-cyan-800 text-[#FFECAB] hover:bg-cyan-600 focus-visible:ring-cyan-500 disabled:opacity-50 cursor-pointer" disabled={!isPetActive} 
                            title={!isPetActive ? "Cannot edit records for an inactive pet" : "Edit this record"}>
                            <Edit3 size={16} className="mr-2"/>Edit Record
                        </Button>
                    )}
                    <Button onClick={onClose} className="px-5 py-2.5 rounded-lg border border-[#FFECAB]/50 text-[#FFECAB] hover:bg-red-800 hover:text-[#FFECAB] focus-visible:ring-red-500 disabled:opacity-50 cursor-pointer">
                        Close
                    </Button>
                </div>
            </div>
        </Modal>
    );
};

export default ViewRecordModal;