import { useState, useEffect, useCallback, JSX, useMemo } from 'react';
import { PetProfileDto, CertificateViewDto, Country, ClinicDto, VetSummaryDto } from '@/types/apiTypes';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { FileText,  Loader2, AlertCircle, QrCode, Send, ShieldCheck, InfoIcon } from 'lucide-react';
import { useAuth } from '@/hooks/useAuth';
import { findCertificatesByPet, getCertificateQrData } from '@/services/certificateService';
import { requestCertificateGeneration } from '@/services/petService'; 
import { toast } from 'sonner';
import { formatDateTime, getRecordTypeDisplay } from '@/utils/formatters';
import { Tooltip, TooltipTrigger, TooltipContent } from '@/components/ui/tooltip';
import { Badge } from '@/components/ui/badge';
import SelectClinicForCertificateRequestModal from '@/components/pet/modals/SelectClinicForCertificateRequestModal';
import ShowQrModal from '@/components/common/ShowQrModal'; 

interface QrModalState {
    isOpen: boolean;
    qrDataString: string | null;
    title: string;
    infoText?: string; 
}

/**
 * PetCertificatesTab - Displays a list of certificates for the pet.
 * Allows Owners to request new certificates.
 * Allows viewing details and QR code for each certificate.
 * @param {PetCertificatesTabProps} props - Component props.
 * @returns {JSX.Element} The Pet Certificates Tab content.
 */
const PetCertificatesTab = ({ pet }: { pet: PetProfileDto }): JSX.Element => {
    const { token, user } = useAuth();
    const [certificates, setCertificates] = useState<CertificateViewDto[]>([]);
    const [isLoading, setIsLoading] = useState<boolean>(true);
    const [error, setError] = useState<string>('');
    const [showSelectClinicModal, setShowSelectClinicModal] = useState<boolean>(false);
    const [isRequesting, setIsRequesting] = useState<boolean>(false);
    const [qrModal, setQrModal] = useState<QrModalState>({ isOpen: false, qrDataString: null, title: '' });
    const [isLoadingQr, setIsLoadingQr] = useState<boolean>(false);

    const isOwner = user?.id === pet.ownerId;

    const uniqueAssociatedClinics = useMemo((): ClinicDto[] => {
        if (!pet.associatedVets || pet.associatedVets.length === 0) {
            return [];
        }
        const clinicsMap = new Map<string | number, ClinicDto>();
        pet.associatedVets.forEach((vet: VetSummaryDto) => {
            if (vet.clinicId && vet.clinicName) {
                if (!clinicsMap.has(vet.clinicId)) {
                    clinicsMap.set(vet.clinicId, {
                        id: vet.clinicId,
                        name: vet.clinicName,
                        address: vet.clinicAddress || '',
                        city: vet.clinicCity || '',
                        country: (vet.clinicCountry as Country) || Country.UNITED_KINGDOM,
                        phone: vet.clinicPhone || '',
                    });
                }
            }
        });
        return Array.from(clinicsMap.values());
    }, [pet.associatedVets]);

    const canMeetPreRequisites = pet.canRequestAhcCertificate === true;
    const hasAssociatedVets = uniqueAssociatedClinics.length > 0;
    const hasPendingCertificateAtThisClinic = (clinicIdToCheck: string | number): boolean => {
        return pet.pendingCertificateClinicId === clinicIdToCheck;
    };

    const canRequestFromAnyClinic = isOwner && canMeetPreRequisites && hasAssociatedVets && !pet.pendingCertificateClinicId;

    let requestButtonTooltip = "";

    if (isOwner) {
        if (pet.pendingCertificateClinicId) {
            requestButtonTooltip = `A certificate request is already pending at ${pet.pendingCertificateClinicName || 'a clinic'}.`;
        } else if (!hasAssociatedVets) {
            requestButtonTooltip = "Associate a veterinarian with your pet to request a certificate.";
        }else if (!canMeetPreRequisites) {
            requestButtonTooltip = "Pet does not meet AHC requirements (missing valid rabies vaccine or recent checkup).";
        } 
}

    const handleRequestCertificateClick = () => {
        if (!canRequestFromAnyClinic) return; // Doble chequeo

        if (uniqueAssociatedClinics.length === 1) {
            const singleClinicId = uniqueAssociatedClinics[0].id;
            if (hasPendingCertificateAtThisClinic(singleClinicId)) {
                 toast.info(`A request is already pending at ${uniqueAssociatedClinics[0].name}.`);
                 return;
            }
            handleClinicSelectedForCertificate(singleClinicId);
        } else if (uniqueAssociatedClinics.length > 1) {
            setShowSelectClinicModal(true);
        }
    };

    const handleClinicSelectedForCertificate = async (clinicId: number | string) => {
        if (!token || !pet) return;
        if (hasPendingCertificateAtThisClinic(clinicId)) {
            const clinicName = uniqueAssociatedClinics.find(c=>c.id === clinicId)?.name || "this clinic";
            toast.info(`A request is already pending at ${clinicName}.`);
            setShowSelectClinicModal(false);
            return;
        }

        setIsRequesting(true);
        setShowSelectClinicModal(false);
        try {
            await requestCertificateGeneration(token, pet.id, clinicId);
            const selectedClinic = uniqueAssociatedClinics.find(c => c.id === clinicId);
            toast.success(`Certificate request sent successfully to ${selectedClinic?.name || 'the clinic'}!`);
            if (onPetProfileShouldRefresh) onPetProfileShouldRefresh(); 
        } catch (err) {
            toast.error(err instanceof Error ? err.message : "Failed to send certificate request.");
        } finally {
            setIsRequesting(false);
        }
    };

    const fetchCertificates = useCallback(async () => {
        if (!token || !pet?.id) { setIsLoading(false); return; }
        setIsLoading(true);
        setError('');
        try {
            const data = await findCertificatesByPet(token, pet.id);
            setCertificates(data);
        } catch (err) {
            setError(err instanceof Error ? err.message : "Failed to load certificates.");
        } finally {
            setIsLoading(false);
        }
    }, [token, pet?.id]);

  useEffect(() => {
    if (token && pet?.id) {
        fetchCertificates();
    } else {
        setCertificates([]); 
        setIsLoading(false); 
        setError('');      
    }
}, [fetchCertificates, pet?.id, token]);

    const handleViewQr = async (certificate: CertificateViewDto) => {
        if (!token) {
            toast.error("Authentication required.");
            return;
        }
        setIsLoadingQr(true);
        try {
            const qrData = await getCertificateQrData(token, certificate.id);
            setQrModal({
                isOpen: true,
                qrDataString: qrData,
                title: "Certificate",
                infoText: `Certificate No: ${certificate.certificateNumber} - Issued on: ${formatDateTime(certificate.createdAt)}`,
            });
        } catch (err) {
            toast.error(err instanceof Error ? err.message : "Failed to load QR data.");
        } finally {
            setIsLoadingQr(false);
        }
    };
    
    const getAhcValidityInfo = (issueDateStr: string): {entryEuExpiry: string, travelEuExpiry: string, isExpiredForEntry: boolean } => {
             const issueDate = new Date(issueDateStr);
        const entryEuExpiryDate = new Date(issueDate);
        entryEuExpiryDate.setDate(issueDate.getDate() + 10);
        const travelEuExpiryDate = new Date(issueDate);
        travelEuExpiryDate.setMonth(issueDate.getMonth() + 4);
        const today = new Date();
        today.setHours(0,0,0,0);
        return {
            entryEuExpiry: entryEuExpiryDate.toLocaleDateString('en-GB'),
            travelEuExpiry: travelEuExpiryDate.toLocaleDateString('en-GB'),
            isExpiredForEntry: today > entryEuExpiryDate
        };
    };

    const onPetProfileShouldRefresh = () => {
        console.log("PetCertificatesTab: Requesting parent to refresh pet profile.");
    };


    return (
        <Card className="border-2 border-[#FFECAB]/50 bg-[#0c1225]/70 shadow-xl">
            <CardHeader>
                <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-2">
                    <CardTitle className="text-[#FFECAB] text-xl flex items-center">
                        <FileText size={24} className="mr-2 text-cyan-400" />
                        Health Certificates for {pet.name}
                    </CardTitle>
                     {isOwner && (
                        <Tooltip open={!canRequestFromAnyClinic && !!requestButtonTooltip ? undefined : false} >
                            <TooltipTrigger asChild>
                                <div className="inline-block self-start sm:self-center">
                                    <Button
                                        onClick={handleRequestCertificateClick}
                                        size="sm"
                                        className="px-5 py-2.5 rounded-lg border border-[#FFECAB]/50 bg-cyan-800 text-[#FFECAB] hover:bg-cyan-600 cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
                                        disabled={!canRequestFromAnyClinic || isRequesting}
                                        aria-disabled={!canRequestFromAnyClinic}
                                    >
                                        {isRequesting ? <Loader2 className="h-4 w-4 animate-spin mr-2"/> : <Send size={16} className="mr-2" />}
                                        {isRequesting ? "Sending..." : "Request New Certificate"}
                                    </Button>
                                </div>
                            </TooltipTrigger>
                            {!canRequestFromAnyClinic && requestButtonTooltip && (
                                <TooltipContent className="bg-gray-950 text-white border  border-red-700">
                                    <p>{requestButtonTooltip}</p>
                                </TooltipContent>
                            )}
                        </Tooltip>
                    )}
                </div>
                <div className="mt-3 p-3 bg-gray-800/50 border border-gray-700 rounded-lg text-sm text-gray-300 space-y-1">
                    <h4 className="font-semibold text-gray-200 flex items-center"><InfoIcon size={20} className="mr-1.5 text-blue-400"/>Animal Health Certificate (AHC) Info:</h4>
                    <ul className="list-disc list-inside pl-1 space-y-0.5">
                        <li>For travel from Great Britain (UK except NI) to the EU or NI.</li>
                        <li>Valid for EU entry for <strong className="text-[#FFECAB]">10 days</strong> from issue.</li>
                        <li>Valid for EU travel for <strong className="text-[#FFECAB]">4 months</strong> (or rabies expiry).</li>
                        <li>Valid for GB re-entry for <strong className="text-[#FFECAB]">4 months</strong> from issue.</li>
                        <li>A new AHC is needed for each trip.</li>
                        <li>Requires a valid, signed rabies vaccine and a recent, signed health checkup.</li>
                    </ul>
                </div>
            </CardHeader>
            <CardContent>
                {isLoading && <div className="text-center py-6"><Loader2 className="h-6 w-6 animate-spin text-cyan-500 inline-flex mr-2"/>Loading certificates...</div>}
                {error && <div className="text-center py-6 text-red-400"><AlertCircle className="inline mr-2"/>{error}</div>}
                {!isLoading && !error && certificates.length === 0 && (
                    <p className="text-gray-400 italic text-center py-8">No certificate found for {pet.name}.</p>
                )}
                {!isLoading && !error && certificates.length > 0 && (
                    <div className="space-y-3">
                        {certificates.map(cert => {
                            const validityInfo = getAhcValidityInfo(cert.createdAt);
                            return (
                                <div key={cert.id} className={`bg-gray-800/60 p-3 sm:p-4 rounded-lg border hover:border-cyan-600/50 ${validityInfo.isExpiredForEntry ? 'border-red-700/50 opacity-70' : 'border-gray-700'}`}>
                                    <div className="flex flex-col sm:flex-row justify-between items-start">
                                        <div>
                                            <h4 className="font-semibold text-base text-white mb-0.5 flex items-center gap-1.5">
                                                <ShieldCheck size={16} className={validityInfo.isExpiredForEntry ? "text-red-400" : "text-green-400"}/>
                                                Certificate No: {cert.certificateNumber}
                                                {validityInfo.isExpiredForEntry && <Badge variant="destructive" className="ml-2 text-xs">Expired for EU Entry</Badge>}
                                            </h4>
                                            <p className="text-xs text-gray-400">Issued: {formatDateTime(cert.createdAt)}</p>
                                            <p className="text-xs text-gray-400">By: Dr. {cert.generatorVet.name} {cert.generatorVet.surname} ({cert.issuingClinic.name})</p>
                                            <p className="text-xs text-gray-400 mt-1">Based on Record: {getRecordTypeDisplay(cert.originatingRecord.type)} ({formatDateTime(cert.originatingRecord.createdAt)})</p>
                                            <p className="text-xs text-amber-400 mt-1">EU Entry Valid Until: {validityInfo.entryEuExpiry}</p>
                                        </div>
                                        <div className="flex gap-1.5 mt-3 sm:mt-0 self-start sm:self-center">
                                            <Button
                                                className="px-2 py-1 h-auto text-sm bg-[#FFECAB] text-[#090D1A] border-2 border-[#FFECAB] hover:bg-cyan-800 hover:text-[#FFECAB] cursor-pointer"
                                                onClick={() => handleViewQr(cert)}
                                                disabled={isLoadingQr}
                                            >
                                                {isLoadingQr && qrModal.title.includes(cert.certificateNumber) ? <Loader2 className="h-4 w-4 animate-spin"/> : <QrCode size={14} className="mr-1"/>}
                                                View QR
                                            </Button>
                                        </div>
                                    </div>
                                </div>
                            );
                        })}
                    </div>
                )}
            </CardContent>
            {showSelectClinicModal && isOwner && (
                 <SelectClinicForCertificateRequestModal
                    isOpen={showSelectClinicModal}
                    onClose={() => setShowSelectClinicModal(false)}
                    availableClinics={uniqueAssociatedClinics}
                    onClinicSelected={handleClinicSelectedForCertificate}
                    isLoading={isRequesting}
                    petPendingCertificateClinicId={pet.pendingCertificateClinicId}
                />
            )}

            {qrModal.isOpen && qrModal.qrDataString && (
                <ShowQrModal
                    isOpen={qrModal.isOpen}
                    onClose={() => setQrModal({ isOpen: false, qrDataString: null, title: '' })}
                    qrData={qrModal.qrDataString}
                    title={qrModal.title}
                    infoText={qrModal.infoText}
                />
            )}
        </Card>
    );
};

export default PetCertificatesTab;