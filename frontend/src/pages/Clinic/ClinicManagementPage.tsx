import { useState, useEffect, useCallback,  JSX } from 'react'; 
import { Building, Loader2, AlertCircle,  Search, Edit, Download  } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { useAuth } from '@/hooks/useAuth';
import { ClinicDto} from '@/types/apiTypes'; 
import { getClinicDetails, downloadClinicPublicKey} from '@/services/clinicService'; 
import EditClinicModal from '@/components/clinic/modals/EditClinicModal'; 
import ClinicSearchModal from '@/components/clinic/modals/ClinicSearchModal';

/**
 * ClinicManagementPage - Displays details of the Admin's current clinic
 * and allows editing via a modal. Also provides access to search other clinics.
 */
const ClinicManagementPage = (): JSX.Element => {
    const { user, token, isLoading: isLoadingAuth } = useAuth();
    const [clinicData, setClinicData] = useState<ClinicDto | null>(null);
    const [isLoadingData, setIsLoadingData] = useState<boolean>(true);
    const [error, setError] = useState<string>('');
    const [showSearchModal, setShowSearchModal] = useState<boolean>(false);
    const [showEditModal, setShowEditModal] = useState<boolean>(false);
    const clinicId = user?.clinicId;
    const isAdmin = user?.roles?.includes('ADMIN');
    const isVet = user?.roles?.includes('VET');


    const fetchClinicData = useCallback(async (forceRefresh = false) => {
        if (!token || !clinicId ) return;
        if (!forceRefresh && clinicData && !isLoadingData) return;

        setIsLoadingData(true);
        setError('');
        try {
            const data = await getClinicDetails(token, clinicId);
            setClinicData(data);
        } catch (err) {
            setError(err instanceof Error ? err.message : "Failed to load clinic data.");
        } finally {
            setIsLoadingData(false);
        }
    }, [token, clinicId, clinicData, isLoadingData]);

    useEffect(() => {
        if (!isLoadingAuth && clinicId) {
            fetchClinicData();
        } else if (!isLoadingAuth &&  !clinicId) {
            setError( "Clinic ID missing for your account.");
            setIsLoadingData(false);
        }
    }, [isLoadingAuth, clinicId, fetchClinicData]);

    const handleClinicUpdated = () => {
        setShowEditModal(false); 
        fetchClinicData(true); 
    };

    const handleDownloadKey = async () => {
        if (!token || !clinicId) return;
        setError('');
        try {
            await downloadClinicPublicKey(token, clinicId);
        } catch (err) {
            setError(err instanceof Error ? err.message : "Failed to download public key.");
        }
    };

    if (isLoadingAuth || isLoadingData) return <div className="flex justify-center items-center py-10"><Loader2 className="h-8 w-8 animate-spin text-cyan-500" /></div>;
    if (!isAdmin && !isVet) return <div className="p-6 text-center bg-[#0c1225]/50 rounded-lg border border-red-500/30"><AlertCircle className="mx-auto h-12 w-12 text-red-400" /><h2 className="mt-2 text-xl font-semibold text-red-300">Access Denied</h2><p className="text-gray-400">Clinic staff role required.</p></div>;
    if (error) return <div className="p-4 text-red-400 flex items-center gap-2"><AlertCircle size={20}/> {error}</div>;
    if (!clinicData && !isLoadingData) return <div className="p-4 text-gray-400">Clinic data could not be loaded or is unavailable.</div>;
    if (!clinicData) {return <div className="p-4 text-gray-400 text-center">Clinic data could not be loaded or is unavailable. Please try again later.</div>;}
     return (
        <div className="space-y-6">
            {/* Header*/}
            <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
                <h1 className="text-2xl sm:text-3xl font-bold text-[#FFECAB] flex items-center">
                    <Building className="mr-3 h-7 w-7 text-cyan-400" />
                    Clinic Information & Settings
                </h1>
                <div className="flex gap-2 w-full sm:w-auto">
                    {/* Serch button */}
                     <Button onClick={() => setShowSearchModal(true)} className="px-5 py-2.5 rounded-lg border border-[#FFECAB]/50 bg-orange-800 text-[#FFECAB] hover:bg-orange-600 focus-visible:ring-orange-500 disabled:opacity-50 cursor-pointer">
                        <Search size={16} className="mr-2" /> Search Clinics
                    </Button>
                     {/* Edit button */}
                     {isAdmin && (
                    <Button onClick={() => setShowEditModal(true)} className="px-5 py-2.5 rounded-lg border border-[#FFECAB]/50 bg-cyan-800 text-[#FFECAB] hover:bg-cyan-600 focus-visible:ring-cyan-500 disabled:opacity-50 cursor-pointer">
   <Edit size={16} className="mr-2" /> Edit Details
                    </Button>
                    )}
                </div>
            </div>

            {/* Clinic card */}
            <Card className="border-2 border-[#FFECAB]/50 bg-[#0c1225]/70 shadow-xl">
                <CardHeader>
                    <CardTitle className="text-[#FFECAB]">{clinicData.name}</CardTitle>
                </CardHeader>
                <CardContent>
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-x-8 gap-y-4 text-sm">
                        <div className="space-y-3">
                             <h4 className="font-medium text-gray-400 border-b border-gray-700 pb-1">Location</h4>
                            <p className="text-white">{clinicData.address}</p>
                            <p className="text-white">{clinicData.city}</p>
                            <p className="text-white">{clinicData.country.replace(/_/g, ' ')}</p>
                        </div>
                        <div className="space-y-3">
                             <h4 className="font-medium text-gray-400 border-b border-gray-700 pb-1">Contact</h4>
                             <p className="text-white">{clinicData.phone}</p>
                        </div>
                        {/* Publickeys */}
                        <div className="md:col-span-2 pt-3 border-t border-gray-700 mt-2">
                            <h4 className="font-medium text-gray-400 mb-1">Clinic Public Key</h4>
                            <div className="flex items-center justify-between gap-2">
                                 {/* Publickey path */}
                                 <p className="text-xs text-gray-300 break-all flex-grow mr-2">
                                    {clinicData.publicKey || 'Public key not available'}
                                 </p>
                                 {/* View publickey button */}
                                 {clinicData.publicKey && (
                                     <Button onClick={handleDownloadKey} size="sm" className="border-[#FFECAB]/50 text-sm px-3 py-1.5 text-[#FFECAB]  hover:text-cyan-800 hover:bg-gray-300 border cursor-pointer">
                                         <Download  size={14} className="mr-1.5" /> Download public-Key
                                     </Button>
                                 )}
                             </div>
                        </div>
                    </div>
                </CardContent>
            </Card>

            {showEditModal && clinicData && (
                <EditClinicModal
                    isOpen={showEditModal}
                    onClose={() => setShowEditModal(false)}
                    clinicData={clinicData} 
                    onClinicUpdated={handleClinicUpdated} 
                />
            )}
             {showSearchModal && (
                <ClinicSearchModal
                    isOpen={showSearchModal}
                    onClose={() => setShowSearchModal(false)}
                />
            )}

    
        </div>
    );
};

export default ClinicManagementPage;