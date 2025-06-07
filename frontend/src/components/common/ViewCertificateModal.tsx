import { useState, useEffect, JSX } from 'react';
import Modal from '@/components/common/Modal';
import { Button } from '@/components/ui/button';
import { CertificateViewDto } from '@/types/apiTypes';
import { getCertificateQrData } from '@/services/certificateService';
import { useAuth } from '@/hooks/useAuth';
import { QRCodeCanvas } from 'qrcode.react';
import { Loader2, AlertCircle, Copy, Check, CircleX, ShieldCheck, CalendarCheck2, Globe } from 'lucide-react';
import { toast } from 'sonner';
import { formatDateTime, getRecordTypeDisplay, getAhcValidityInfo  } from '@/utils/formatters';

interface ViewCertificateModalProps {
    isOpen: boolean;
    onClose: () => void;
    certificate: CertificateViewDto | null;
}

/**
 * ViewCertificateModal - Displays detailed information for a specific certificate
 * and its corresponding QR code.
 * @param {ViewCertificateModalProps} props - Component props.
 * @returns {JSX.Element | null} The modal component.
 */
const ViewCertificateModal = ({ isOpen, onClose, certificate }: ViewCertificateModalProps): JSX.Element | null => {
    const { token } = useAuth();
    const [qrData, setQrData] = useState<string | null>(null);
    const [isLoadingQr, setIsLoadingQr] = useState<boolean>(false);
    const [qrError, setQrError] = useState<string | null>(null);
    const [copied, setCopied] = useState(false);

    useEffect(() => {
        if (isOpen && certificate && token && !isLoadingQr && qrData === null && qrError === null) { 
            setIsLoadingQr(true);
            setQrError(null);
            setQrData(null); 
            getCertificateQrData(token, certificate.id)
                .then(data => setQrData(data))
                .catch(err => {
                    const errMsg = err instanceof Error ? err.message : "Failed to load QR data.";
                    setQrError(errMsg);
                })
                .finally(() => setIsLoadingQr(false));
        } else if (!isOpen) {
            setQrData(null); 
            setQrError(null);
            setCopied(false);
        }
    }, [isOpen, certificate, token, qrData, isLoadingQr, qrError]); 

    const handleCopyToClipboard = async () => {
        if (!qrData) return;
        try {
            await navigator.clipboard.writeText(qrData);
            setCopied(true);
            toast.success("QR data (HC1 string) copied to clipboard!");
            setTimeout(() => setCopied(false), 2500);
        } catch (err) {
            console.error("Failed to copy QR data:", err);
            toast.error("Failed to copy QR data.");
        }
    };

    if (!isOpen || !certificate) return null;

    const validityInfo = getAhcValidityInfo(
        certificate.createdAt, 
        certificate.pet.lastEuEntryDate, 
        certificate.travelValidityEndDate 
    );

    const formatDate = (dateString: string | null | undefined): string => {
        if (!dateString) return 'N/A';
        return new Date(dateString).toLocaleDateString('en-GB', { day: '2-digit', month: 'long', year: 'numeric' });
    };
    
    return (
        <Modal title={`Certificate Details: #${certificate.certificateNumber}`} onClose={onClose} maxWidth="max-w-2xl lg:max-w-3xl">
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">

                <div className="space-y-3 pr-0 lg:pr-4 lg:border-r border-gray-700">
                     {/* Pet Information */}
                    <h4 className="text-lg font-semibold text-cyan-400">Pet Information</h4>
                    <p className="text-sm"><strong className="text-gray-400">Name:</strong> <span className="text-white">{certificate.pet.name}</span></p>
                    <p className="text-sm"><strong className="text-gray-400">Species:</strong> <span className="text-white capitalize">{certificate.pet.specie?.toLowerCase()}</span></p>
                    <p className="text-sm"><strong className="text-gray-400">Breed:</strong> <span className="text-white">{certificate.pet.breedName || 'N/A'}</span></p>
                    <p className="text-sm"><strong className="text-gray-400">Microchip:</strong> <span className="text-white">{certificate.pet.microchip || 'N/A'}</span></p>

                    <h4 className="text-lg font-semibold text-cyan-400 pt-2 mt-3 border-t border-gray-700">Issuing Authority</h4>
                    <p className="text-sm"><strong className="text-gray-400">Clinic:</strong> <span className="text-white">{certificate.generatorVet.clinicName} - {certificate.generatorVet.clinicCity}</span></p>
                    <p className="text-xs text-gray-300"><Globe size={12} className="inline mr-1.5 text-[#FFECAB]"/> {certificate.generatorVet.clinicCountry?.replace(/_/g, ' ') || 'N/A'}</p>
                    <p className="text-sm"><strong className="text-gray-400">Vet:</strong> <span className="text-white">Dr. {certificate.generatorVet.name} {certificate.generatorVet.surname} </span> </p>
                    <p className="text-xs text-gray-300">License: {certificate.generatorVet.licenseNumber}</p>
                    
                    <h4 className="text-lg font-semibold text-cyan-400 pt-2 mt-3 border-t border-gray-700">Certificate Validity</h4>
                    <p className="text-sm"><strong className="text-gray-400">Issued On:</strong> <span className="text-white">{formatDateTime(certificate.createdAt)}</span></p>

                    {/* Validity Information */}
                    <div className="p-2 rounded-md text-sm" style={{ backgroundColor: validityInfo.overallStatus === 'EXPIRED' ? 'rgba(239, 68, 68, 0.1)' : validityInfo.overallStatus === 'VALID_FOR_ENTRY' ? 'rgba(234, 179, 8, 0.1)' : 'rgba(34, 197, 94, 0.1)'}}>
                        <div className="flex items-center font-semibold" style={{ color: validityInfo.overallStatus === 'EXPIRED' ? '#f87171' : validityInfo.overallStatus === 'VALID_FOR_ENTRY' ? '#facc15' : '#4ade80'}}>
                            {validityInfo.overallStatus === 'VALID_FOR_TRAVEL' && <ShieldCheck size={16} className="mr-2"/>}
                            {validityInfo.overallStatus === 'VALID_FOR_ENTRY' && <CalendarCheck2 size={16} className="mr-2"/>}
                            {validityInfo.overallStatus === 'EXPIRED' && <AlertCircle size={16} className="mr-2"/>}
                            {validityInfo.overallStatus.replace(/_/g, ' ')}
                        </div>
                        <p className="text-gray-300 text-xs mt-1">{validityInfo.statusMessage}</p>
                    </div>
                    <p className="text-xs text-gray-500">EU Entry by: {formatDate(certificate.initialEuEntryExpiryDate)} | Travel within EU by: {formatDate(certificate.travelValidityEndDate)}</p>

                    
                </div>

                {/* QR Code */}
                <div className="flex flex-col items-center justify-center space-y-3">
                    <h4 className="text-lg font-semibold text-cyan-400">Digital Certificate QR Code</h4>
                    {isLoadingQr && <div className="h-48 flex items-center"><Loader2 className="h-8 w-8 animate-spin text-cyan-500" /></div>}
                    {qrError && !isLoadingQr && <div className="h-48 flex flex-col items-center justify-center text-red-400"><AlertCircle size={24} className="mb-2"/> <p>{qrError}</p></div>}
                    {!isLoadingQr && !qrError && qrData && (
                        <div className="p-2 bg-white rounded-md inline-block shadow-lg">
                            <QRCodeCanvas value={qrData} size={200} bgColor="#FFFFFF" fgColor="#090D1A" level="M" />
                        </div>
                    )}
                    {!isLoadingQr && qrData && (
                         <Button onClick={handleCopyToClipboard} size="sm" className="px-5 py-2.5 rounded-lg border border-[#FFECAB]/50 bg-cyan-800 text-[#FFECAB] hover:bg-cyan-600 focus-visible:ring-cyan-500 disabled:opacity-50 cursor-pointer">
                            {copied ? <Check size={16} className="mr-2 text-green-400" /> : <Copy size={16} className="mr-2" />}
                            {copied ? 'Copied!' : 'Copy HC1 Data'}
                        </Button>
                    )}
                    <p className="text-xs text-gray-500 text-center max-w-xs">
                        This QR code contains the digitally signed health certificate information compliant with EU DCC standards.
                    </p>
                    <div className="flex flex-col items-center justify-center space-y-3">
                    <h4 className="text-lg font-semibold text-cyan-400 pt-2 mt-3 border-t border-gray-700">Basis of Certificate</h4>
                    <p className="text-sm"><strong className="text-gray-400">Originating Record:</strong> <span className="text-white">{getRecordTypeDisplay(certificate.originatingRecord.type)}</span></p>
                    <div>
                        <p className="text-xs text-center">Date:<span className="text-white">{formatDate(certificate.originatingRecord.createdAt)}</span></p>
                        <p className="text-xs">
                            Vaccine: <span className="text-white">{certificate.originatingRecord.vaccine?.name} - </span>
                            Laboratory: <span className="text-white">{certificate.originatingRecord.vaccine?.laboratory}</span>    
                        </p>
                        <p className="text-xs">
                            Batch: <span className="text-white">{certificate.originatingRecord.vaccine?.batchNumber} - </span>
                            Validity: <span className="text-white"> {certificate.originatingRecord.vaccine?.validity} year</span>
                        </p>
                        <p className="text-xs">
                            Vet: <span className="text-white">Dr. {certificate.originatingRecord.creator.name} {certificate.originatingRecord.creator.surname} ({certificate.originatingRecord.creator.clinicName})</span> 
                        </p>
                    </div>
                </div>
                </div>
                
            </div>

            <div className="mt-6 pt-4 border-t border-[#FFECAB]/20 flex justify-end">
                <Button onClick={onClose} className="px-5 py-2.5 rounded-lg border border-[#FFECAB]/50 text-[#FFECAB] hover:bg-red-800 hover:text-[#FFECAB] focus-visible:ring-red-500 disabled:opacity-50 cursor-pointer">
                    <CircleX size={16} className="mr-2"  /> Close</Button>
            </div>
        </Modal>
    );
};
export default ViewCertificateModal;