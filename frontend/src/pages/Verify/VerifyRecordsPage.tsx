import { useState, useEffect, JSX } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import { getRecordsByTemporaryToken } from '@/services/recordService';
import { RecordViewDto, RecordType } from '@/types/apiTypes'; 
import { formatRecordCreatorDisplay, formatDateTime, getRecordTypeDisplay } from '@/utils/formatters'; 
import { Loader2, AlertCircle, ShieldCheck, Syringe, BookOpenCheck, FileText, Thermometer, AlertTriangle, Info} from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import Footer from '@/components/layout/Footer';


/**
 * VerifyRecordsPage - Displays signed medical records for a pet, accessed via a temporary token.
 * This page is intended to be publicly accessible via a shared link.
 * @returns {JSX.Element} The verification page.
 * @author ibosquet
 */
const VerifyRecordsPage = (): JSX.Element => {
    const [searchParams] = useSearchParams();
    const [records, setRecords] = useState<RecordViewDto[] | null>(null);
    const [isLoading, setIsLoading] = useState<boolean>(true);
    const [error, setError] = useState<string | null>(null);
    const [petName, setPetName] = useState<string>(''); 

    useEffect(() => {
        const token = searchParams.get('token');
        if (token) {
            setIsLoading(true);
            setError(null);
            getRecordsByTemporaryToken(token)
                .then(data => {
                    setRecords(data);
                    if (data.length > 0) {
                        setPetName(data[0].petName || 'this pet'); // Asume que todos los records son del mismo pet
                    } else {
                        setPetName('this pet');
                    }
                })
                .catch(err => {
                    setError(err instanceof Error ? err.message : "An unknown error occurred.");
                    setRecords([]); // Limpia records en caso de error para no mostrar datos viejos
                })
                .finally(() => setIsLoading(false));
        } else {
            setError("No access token provided in the link.");
            setIsLoading(false);
        }
    }, [searchParams]);

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

    if (isLoading) {
        return (
            <div className="min-h-screen flex flex-col items-center justify-center bg-gradient-to-br from-[#070913] to-[#0c1225] p-4">
                <Loader2 className="h-12 w-12 animate-spin text-cyan-400" />
                <p className="mt-4 text-lg text-gray-300">Verifying access and loading records...</p>
            </div>
        );
    }

    return (
        <div className="min-h-screen flex flex-col bg-gradient-to-br from-[#070913] to-[#0c1225] text-white">
            <header className="p-4 sm:p-6 text-center border-b border-[#FFECAB]/20">
                 <Link to="/" className="inline-flex items-center gap-2 mb-2">
                    <img src="/src/assets/images/SF-Logo1-D.png" alt="PetConnect Logo" className="h-10 w-10" />
                    <h1 className="text-2xl font-bold text-[#FFECAB]">PetConnect</h1>
                </Link>
                <p className="text-sm text-gray-400">Securely shared medical history</p>
            </header>

            <main className="flex-grow container mx-auto px-4 py-8">
                {error && (
                    <Card className="border-red-500/50 bg-red-900/30 text-red-300">
                        <CardHeader><CardTitle className="flex items-center"><AlertCircle className="mr-2"/> Access Error</CardTitle></CardHeader>
                        <CardContent><p>{error}</p></CardContent>
                    </Card>
                )}

                {!error && records && (
                    <>
                        <div className="mb-6 text-center">
                            <h2 className="text-2xl font-semibold text-[#FFECAB]">
                                Signed medical records for <span className="text-cyan-400">{petName}</span>
                            </h2>
                            <p className="text-sm text-gray-400">This is a read-only temporary view.</p>
                        </div>

                        {records.length === 0 && !isLoading && (
                            <p className="text-center text-gray-400 italic py-6">No signed medical records found for this pet with the provided access.</p>
                        )}

                        {records.length > 0 && (
                            <div className="space-y-4">
                                {records.map(record => (
                                    <Card key={record.id} className="bg-[#0A0F1E]/70 border border-gray-700 shadow-md">
                                        <CardHeader className="pb-3">
                                            <CardTitle className="text-lg text-cyan-300 flex items-center gap-2">
                                                {getRecordTypeIcon(record.type)}
                                                {getRecordTypeDisplay(record.type)}
                                            </CardTitle>
                                            <div className="text-xs text-gray-400 space-x-3">
                                                <span>Date: {formatDateTime(record.createdAt)}</span>
                                                <span>By: {formatRecordCreatorDisplay(record.creator)}</span> 
                                            </div>
                                        </CardHeader>
                                        <CardContent className="text-sm">
                                            {record.description && (
                                                <p className="text-gray-200 whitespace-pre-wrap mb-2">{record.description}</p>
                                            )}
                                            {record.type === RecordType.VACCINE && record.vaccine && (
                                                <div className="mt-1 pt-2 border-t border-gray-700/50 text-xs">
                                                    <p className="font-medium text-blue-300">Vaccine: {record.vaccine.name}</p>
                                                    <p className="text-gray-400">Batch: {record.vaccine.batchNumber}, Lab: {record.vaccine.laboratory || 'N/A'}, Validity: {record.vaccine.validity ?? 'N/A'} yrs</p>
                                                </div>
                                            )}
                                            {record.vetSignature && (
                                                <div className="mt-2 pt-2 border-t border-green-700/30">
                                                    <p className="text-xs text-green-400 flex items-center gap-1">
                                                        <ShieldCheck size={14}/> Digitally Signed.
                                                    </p>
                                                </div>
                                            )}
                                        </CardContent>
                                    </Card>
                                ))}
                            </div>
                        )}
                    </>
                )}
            </main>
            <Footer className="bg-transparent text-gray-600" />
        </div>
    );
};
export default VerifyRecordsPage;