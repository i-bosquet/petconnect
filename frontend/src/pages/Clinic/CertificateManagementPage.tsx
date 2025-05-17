import { useState, useEffect, useCallback, useMemo, JSX, ChangeEvent, FormEvent} from "react";
import { formatDateTime} from '@/utils/formatters';
import { ScrollText, Loader2, AlertCircle, Search, Eye} from "lucide-react"; 
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Button } from "@/components/ui/button";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip";
import { Input } from "@/components/ui/input";
import { CertificateViewDto, Page} from "@/types/apiTypes"; 
import { findCertificatesCreatedByClinic} from "@/services/certificateService";
import ViewCertificateModal from '@/components/common/ViewCertificateModal'; 
import { useAuth } from "@/hooks/useAuth";
import Pagination from "@/components/common/Pagination";

const PAGE_SIZE_CLINIC_CERTIFICATES  = 10;

/**
 * CertificateManagementPage - Displays a list of all certificates
 * created by the clinic.
 * Allows viewing details of each certificate.
 * @returns {JSX.Element} The clinic's certificate management page component.
 */
const CertificateManagementPage = (): JSX.Element => {
    const { token, user, isLoading: isLoadingAuth } = useAuth();
    const [certificates, setCertificates] = useState<CertificateViewDto[]>([]);
    const [isLoadingData, setIsLoadingData] = useState<boolean>(true);
    const [error, setError] = useState<string>("");
    const [searchTerm, setSearchTerm] = useState<string>("");
    const [currentPage, setCurrentPage] = useState<number>(0);
    const [totalPages, setTotalPages] = useState<number>(0);

    const [selectedCertificate, setSelectedCertificate] = useState<CertificateViewDto | null>(null);
    const [showViewModal, setShowViewModal] = useState<boolean>(false);

    const clinicIdFromUser = user?.clinicId;

     /**
     * Fetches the paginated list of certificates issued by the current clinic.
     * @param {number} pageToFetch - The page number to retrieve.
     */
    const fetchClinicCreatedCertificates = useCallback(async (pageToFetch: number) => { 
        if (!token || !clinicIdFromUser) {
            setIsLoadingData(false);
            if (!clinicIdFromUser) setError("Clinic information not found for your account to fetch certificates.");
            return;
        }
        setIsLoadingData(true);
        setError("");
        try {
            const data: Page<CertificateViewDto> = await findCertificatesCreatedByClinic(token, {
                clinicId: clinicIdFromUser!,
                page: pageToFetch,
                size: PAGE_SIZE_CLINIC_CERTIFICATES,
                sort: 'createdAt,desc',
            });
            setCertificates(data.content);
            setTotalPages(data.totalPages);
            setCurrentPage(data.number);
        } catch (err) {
            console.error("Failed to fetch clinic-issued certificates:", err);
            setError(err instanceof Error ? err.message : "Could not load certificates issued by the clinic.");
        } finally {
            setIsLoadingData(false);
        }
    }, [token, clinicIdFromUser]); 


    useEffect(() => {
        if (!isLoadingAuth && user && clinicIdFromUser) {
            fetchClinicCreatedCertificates(0);
        } else if (!isLoadingAuth && (!user || !clinicIdFromUser)) {
            setIsLoadingData(false);
            if (!user) setError("Authentication required to view certificates.");
            else if (!clinicIdFromUser) setError("Unable to fetch certificates: Clinic ID missing from your profile.");
        }
    }, [isLoadingAuth, user, clinicIdFromUser, fetchClinicCreatedCertificates]);

    const handlePageChange = (newPage: number) => {
        fetchClinicCreatedCertificates(newPage);
    };

    const handleSearchSubmit = (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        fetchClinicCreatedCertificates(0);
    };

     const handleOpenViewModal = (certificate: CertificateViewDto) => {
        setSelectedCertificate(certificate);
        setShowViewModal(true);
    };

    const filteredCertificates = useMemo(() => {
        if (!searchTerm) return certificates;
        const lowerSearch = searchTerm.toLowerCase();
        return certificates.filter(cert =>
            (cert.certificateNumber.toLowerCase().includes(lowerSearch)) ||
            (cert.pet?.name?.toLowerCase().includes(lowerSearch)) ||
            (cert.pet?.microchip?.toLowerCase().includes(lowerSearch)) || 
            (cert.pet?.ownerUsername?.toLowerCase().includes(lowerSearch)) || 
            (cert.generatorVet?.name?.toLowerCase().includes(lowerSearch)) ||
            (cert.generatorVet?.surname?.toLowerCase().includes(lowerSearch))
        );
    }, [certificates, searchTerm]);

    

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
            <ScrollText className="mr-3 h-7 w-7 text-cyan-400" />
                    Clinic Certificates History
          </h1>
          <form onSubmit={handleSearchSubmit} className="flex items-center gap-x-4 gap-y-2 flex-wrap w-full sm:w-auto justify-end">
            <div className="relative">
              <Search size={18} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 pointer-events-none" />
              <Input
                  type="text"
                  placeholder="Search cert #, pet, owner, vet, microchip..."
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
            <CardTitle className="text-[#FFECAB]">Certificates created by this clinic</CardTitle>
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
            ) : filteredCertificates.length === 0 ? (
              <p className="text-center text-gray-400 py-8 px-4">
                {searchTerm
                  ? "No certificates match your search criteria."
                  : "No certificates have been issued by this clinic yet."}
              </p>
            ) : (
              <Table>
                <TableHeader>
                  <TableRow className="border-b-[#FFECAB]/20">
                    <TableHead className="text-[#FFECAB]/80 pl-6 w-[150px]">Issue Date</TableHead>
                    <TableHead className="text-[#FFECAB]/80">Certificate No.</TableHead>
                    <TableHead className="text-[#FFECAB]/80">Pet</TableHead>
                    <TableHead className="text-[#FFECAB]/80 hidden md:table-cell">Owner</TableHead>
                    <TableHead className="text-[#FFECAB]/80 hidden sm:table-cell">Generated By</TableHead>
                    <TableHead className="text-[#FFECAB]/80 text-right pr-6">Actions</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {filteredCertificates.map((cert) => (
                      <TableRow key={cert.id} className="border-b-[#FFECAB]/10 hover:bg-[#FFECAB]/5">
                        <TableCell className="pl-6 text-xs text-gray-300 tabular-nums">{formatDateTime(cert.createdAt)}</TableCell>
                        <TableCell className="font-mono text-sm text-white">{cert.certificateNumber}</TableCell>
                        <TableCell><div className="font-medium text-white">{cert.pet.name}</div><div className="text-xs text-gray-400 capitalize">{cert.pet.specie.toLowerCase()}</div></TableCell>
                        <TableCell className="text-gray-300 hidden md:table-cell">{cert.pet.ownerUsername}</TableCell>
                        <TableCell className="text-gray-300 hidden sm:table-cell">  Dr. {cert.generatorVet.name} {cert.generatorVet.surname}</TableCell>
                        <TableCell className="text-right space-x-1 pr-6">
                            <Tooltip>
                              <TooltipTrigger asChild>
                                <Button
                                  size="icon"
                                  className="h-7 w-7 text-[#FFECAB] hover:text-[#090D1A]  hover:bg-[#FFECAB] cursor-pointer"
                                  onClick={() => handleOpenViewModal(cert)}
                                >
                                   <Eye size={16} />
                                </Button>
                              </TooltipTrigger>
                              <TooltipContent className="bg-gray-950 text-white border border-cyan-700">
                                <p>View Details & QR</p>
                              </TooltipContent>
                            </Tooltip>
                        </TableCell>
                      </TableRow>
                   ))}
                </TableBody>
              </Table>
            )}
          </CardContent>
          {totalPages > 1 && !isLoadingData && filteredCertificates.length > 0 && (
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
          {showViewModal && selectedCertificate && (
            <ViewCertificateModal
              isOpen={showViewModal}
              onClose={() => {setShowViewModal(false); setSelectedCertificate(null);}}
              certificate={selectedCertificate}
            />
           )}

      </div>
    );
};

export default CertificateManagementPage;