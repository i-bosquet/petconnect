import { useState, useEffect, useCallback, JSX } from 'react';
import { PetProfileDto, CertificateViewDto, PetStatus } from '@/types/apiTypes';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { PlaneTakeoff, PlaneLanding, CalendarClock, AlertTriangleIcon,Badge, Loader2 } from 'lucide-react';
import { useAuth } from '@/hooks/useAuth';
import { findCertificatesByPet } from '@/services/certificateService';
import RegisterEuEntryModal from '@/components/pet/modals/RegisterEuEntryModal';
import RegisterEuExitModal from '@/components/pet/modals/RegisterEuExitModal';

interface PetTravelStatusCardProps {
    pet: PetProfileDto;
    onNavigateToCertificates: () => void; 
    onTravelStatusUpdated: () => void; 
}

/**
 * PetTravelStatusCard - Displays travel status and AHC validity for a pet.
 * Allows owner to register EU entry/exit.
 * @param {PetTravelStatusCardProps} props - Component props.
 * @returns {JSX.Element} The travel status card component.
 */
const PetTravelStatusCard = ({ pet, onNavigateToCertificates, onTravelStatusUpdated }: PetTravelStatusCardProps): JSX.Element => {
    const { token } = useAuth();
    const [latestAhc, setLatestAhc] = useState<CertificateViewDto | null>(null);
    const [isLoadingCert, setIsLoadingCert] = useState<boolean>(false);
    const [certError, setCertError] = useState<string | null>(null);

    const [showTravelModal, setShowTravelModal] = useState<null | { type: 'entry' | 'exit', petForModal: PetProfileDto }>(null);

    const fetchLatestAhc = useCallback(async () => {
        if (!token || !pet || pet.status !== PetStatus.ACTIVE) { 
            setLatestAhc(null);
            setIsLoadingCert(false);
            return;
        }
        setIsLoadingCert(true);
        setCertError(null);
        try {
            const certificates = await findCertificatesByPet(token, pet.id);
            const sortedCerts = certificates.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
            setLatestAhc(sortedCerts.length > 0 ? sortedCerts[0] : null);
        } catch (err) {
            setCertError(err instanceof Error ? err.message : "Failed to load AHC data.");
        } finally {
            setIsLoadingCert(false);
        }
    }, [token, pet]);

    useEffect(() => {
        fetchLatestAhc();
    }, [fetchLatestAhc]);

    const handleOpenTravelModal = (type: 'entry' | 'exit') => {
        setShowTravelModal({ type, petForModal: pet });
    };
    
    const handleTravelDateRegistered = () => {
        setShowTravelModal(null);
        onTravelStatusUpdated(); 
        fetchLatestAhc(); 
    };


    const entryDate = pet.lastEuEntryDate ? new Date(pet.lastEuEntryDate) : null;
    const exitDate = pet.lastEuExitDate ? new Date(pet.lastEuExitDate) : null;
    let petLocationStatus: 'IN_UK' | 'IN_EU' = 'IN_UK'; 
    if (entryDate && (!exitDate || entryDate.getTime() > exitDate.getTime())) {
        petLocationStatus = "IN_EU";
    }

    let validityMessage = <span className="text-gray-500 italic">No active AHC found for travel.</span>;
    let actionButton = null;
    const today = new Date();
    today.setHours(0,0,0,0); 

    if (isLoadingCert) {
        validityMessage = <span className="text-gray-400 flex items-center"><Loader2 className="h-4 w-4 animate-spin mr-2"/>Loading certificate info...</span>;
    } else if (certError) {
        validityMessage = <span className="text-red-400 flex items-center"><AlertTriangleIcon className="h-4 w-4 mr-2"/>{certError}</span>;
    } else if (latestAhc) {
        const issueDate = new Date(latestAhc.createdAt);
        const initialExpiry = latestAhc.initialEuEntryExpiryDate ? new Date(latestAhc.initialEuEntryExpiryDate) : null;
        const travelExpiry = latestAhc.travelValidityEndDate ? new Date(latestAhc.travelValidityEndDate) : null;
        
        issueDate.setHours(0,0,0,0);
        initialExpiry?.setHours(0,0,0,0);
        travelExpiry?.setHours(0,0,0,0);

        if (petLocationStatus === 'IN_UK') {
            if (initialExpiry && today <= initialExpiry) {
                validityMessage = <span className="text-green-400">AHC #{latestAhc.certificateNumber.slice(0,8)}... valid for EU entry until {initialExpiry.toLocaleDateString('en-GB')}.</span>;
                actionButton = <Button size="sm" className="mt-2 bg-blue-600 hover:bg-blue-500 w-full sm:w-auto" onClick={() => handleOpenTravelModal('entry')}><PlaneTakeoff className="mr-2 h-4 w-4"/>Register EU Entry</Button>;
            } else {
                validityMessage = <span className="text-red-500">AHC #{latestAhc.certificateNumber.slice(0,8)}... (Issued: {issueDate.toLocaleDateString('en-GB')}) has expired for EU entry.</span>;
                actionButton = <Button size="sm" className="mt-2 w-full sm:w-auto bg-[#090D1A] text-[#FFECAB] hover:bg-[#FFECAB] hover:text-[#090D1A]  cursor-pointer" onClick={onNavigateToCertificates}>Request/View Certificates</Button>;
            }
        } else if (petLocationStatus === 'IN_EU') {
            // For the AHC to be valid for travel within the EU, entry must have occurred within the 10 days of initial validity.
            const entryWasValid = entryDate && initialExpiry && entryDate <= initialExpiry;

            if (entryWasValid && travelExpiry && today <= travelExpiry) {
                validityMessage = <span className="text-green-400">AHC #{latestAhc.certificateNumber.slice(0,8)}... valid for EU travel until {travelExpiry.toLocaleDateString('en-GB')}.</span>;
                actionButton = <Button size="sm" className="mt-2 bg-amber-900 hover:bg-[#FFECAB] hover:text-[#090D1A] w-full sm:w-auto cursor-pointer" onClick={() => handleOpenTravelModal('exit')}><PlaneLanding className="mr-2 h-4 w-4"/>Register EU Exit</Button>;
            } else {
                validityMessage = <span className="text-red-500">AHC #{latestAhc.certificateNumber.slice(0,8)}... travel validity in EU has expired or entry was not within its validity.</span>;
                actionButton = (
                    <div className="flex flex-col sm:flex-row gap-2 mt-2">
                        <Button size="sm" variant="outline" className="w-full sm:w-auto" onClick={() => handleOpenTravelModal('exit')}>Register EU Exit (Past)</Button>
                        <Button size="sm" variant="outline" className="w-full sm:w-auto" onClick={onNavigateToCertificates}>Request New AHC</Button>
                    </div>
                );
            }
        }
    } else if (pet.canRequestAhcCertificate && !isLoadingCert && !certError) {
        if (pet.pendingCertificateClinicId) { 
        validityMessage = (
            <span className="text-yellow-400 italic">
                AHC request pending at: {pet.pendingCertificateClinicName || `Clinic ID ${pet.pendingCertificateClinicId}`}.
            </span>
        );
        actionButton = null; 
        } else {
            validityMessage = <span className="text-yellow-500 italic">No AHC found. Pet may be eligible.</span>;
            actionButton = <Button size="sm" className="px-5 py-2.5 rounded-lg border border-[#FFECAB]/50 bg-cyan-800 text-[#FFECAB] hover:bg-cyan-600 focus-visible:ring-cyan-500 disabled:opacity-50 cursor-pointer" onClick={onNavigateToCertificates}>Request AHC</Button>;
        }
    }

    return (
        <Card className="bg-[#0A0F1E] border-gray-700/80 shadow-md hover:shadow-cyan-700/20 transition-shadow">
            <CardHeader className="pb-3 pt-4">
                <CardTitle className="text-md text-[#FFECAB] flex items-center justify-between">
                    <div className="flex items-center">
                        <img src={pet.image} alt={pet.name} className="w-9 h-9 rounded-full mr-2.5 object-cover border-2 border-gray-600"/>
                        {pet.name}
                    </div>
                     <Badge className={petLocationStatus === 'IN_EU' ? 'bg-sky-600 text-sky-100' : 'bg-slate-600 text-slate-200'}>
                        {petLocationStatus === 'IN_EU' ? 'In EU' : 'In UK / Other'}
                    </Badge>
                </CardTitle>
            </CardHeader>
            <CardContent className="text-sm space-y-2">
                <div className="p-3 rounded-md bg-gray-800 min-h-[60px] flex flex-col justify-center">
                    <div className="flex items-start">
                        <CalendarClock size={16} className="inline mr-2 mt-0.5 text-cyan-400 flex-shrink-0"/>
                        <div>{validityMessage}</div>
                    </div>
                </div>
                {actionButton && <div className="mt-3 flex justify-end">{actionButton}</div>}
                {!actionButton && !latestAhc && !isLoadingCert && !certError && (
                     <p className="text-xs text-gray-500 mt-2 italic text-center">No travel actions available.</p>
                )}
            </CardContent>

            {showTravelModal && (
                showTravelModal.type === 'entry' ?
                <RegisterEuEntryModal
                    isOpen={true}
                    onClose={() => setShowTravelModal(null)}
                    pet={showTravelModal.petForModal} 
                    onEntryRegistered={handleTravelDateRegistered}
                /> :
                <RegisterEuExitModal
                    isOpen={true}
                    onClose={() => setShowTravelModal(null)}
                    pet={showTravelModal.petForModal}
                    onExitRegistered={handleTravelDateRegistered}
                />
            )}
        </Card>
    );
};

export default PetTravelStatusCard;