import { useState, useEffect, useCallback, useMemo, JSX, ChangeEvent } from "react";
import { PawPrint, Loader2, AlertCircle,  Search, Eye} from "lucide-react"; 
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Button } from "@/components/ui/button";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip";
import { Input } from "@/components/ui/input";
import { Checkbox } from "@/components/ui/checkbox";
import { Label } from "@/components/ui/label";
import ClinicPetDetailModal from "@/components/clinic/modals/ClinicPetDetailModal";
import { PetProfileDto, Page, PetStatus } from "@/types/apiTypes";
import { findPetsByClinic } from "@/services/petService"; 
import { useAuth } from "@/hooks/useAuth";
import Pagination from "@/components/common/Pagination"; 

/**
 * ClinicPetManagementPage - Allows Clinic Staff to view and manage pets associated with their clinic.
 * Admins and Vets see all clinic pets. Vets can filter to see only their associated pets.
 *
 * @returns {JSX.Element} The clinic pet management page component.
 */
const PetManagementPage = (): JSX.Element => {
    const { token, user, isLoading: isLoadingAuth } = useAuth();
    const [allClinicPets, setAllClinicPets] = useState<PetProfileDto[]>([]); 
    const [isLoadingData, setIsLoadingData] = useState<boolean>(true);
    const [error, setError] = useState<string>("");
    const [selectedPet, setSelectedPet] = useState<PetProfileDto | null>(null);
    const [showDetailModal, setShowDetailModal] = useState<boolean>(false);
    const [searchTerm, setSearchTerm] = useState<string>("");
    const [currentPage, setCurrentPage] = useState<number>(0);
    const [totalPages, setTotalPages] = useState<number>(0);
    const PAGE_SIZE = 10; 

    const isCurrentUserVet = user?.roles?.includes("VET") ?? false;
    const [showAllClinicPets, setShowAllClinicPets] = useState<boolean>(false);

    const clinicId = user?.clinicId;

    /**
     * Fetches the paginated list of ACTIVE pets associated with the current staff's clinic.
     * @param {number} pageToFetch - The page number to retrieve.
     */
    const fetchClinicPets = useCallback(async (pageToFetch: number) => {
        if (!token || !clinicId) {
            setIsLoadingData(false);
            if (!clinicId) setError("Clinic information not found for your account.");
            return;
        }
        setIsLoadingData(true);
        setError("");
        try {
            const data: Page<PetProfileDto> = await findPetsByClinic(token, pageToFetch, PAGE_SIZE, "name,asc");
            setAllClinicPets(data.content.filter(p => p.status === PetStatus.ACTIVE)); 
            setTotalPages(data.totalPages);
            setCurrentPage(data.number);
        } catch (err) {
            console.error("Failed to fetch clinic pets:", err);
            setError(err instanceof Error ? err.message : "Could not load pet data.");
        } finally {
            setIsLoadingData(false);
        }
    }, [token, clinicId]);

    useEffect(() => {
        if (!isLoadingAuth && user) {
            fetchClinicPets(0); 
        } else if (!isLoadingAuth && !user) {
            setIsLoadingData(false);
        }
    }, [isLoadingAuth, user, fetchClinicPets]);

    const handlePageChange = (newPage: number) => {
        fetchClinicPets(newPage);
    };

    const handleOpenDetailModal = (pet: PetProfileDto) => {
        setSelectedPet(pet);
        setShowDetailModal(true);
    };

    const handleCloseDetailModal = () => {
        setSelectedPet(null);
        setShowDetailModal(false);
    };

    const handlePetUpdateInModal = () => {
        setShowDetailModal(false);
        fetchClinicPets(currentPage); // Refrescar la página actual
    };

    /**
     * Memoized list of pets to be displayed in the table.
     * Filters the `allClinicPets` based on the `showOnlyMyAssociatedPets` toggle (for Vets)
     * and the current `searchTerm`.
     */
    const displayedPets = useMemo(() => {
        let petsToDisplay = allClinicPets;

        if (isCurrentUserVet && !showAllClinicPets && user?.id) {
            const currentVetId = user.id;
            petsToDisplay = petsToDisplay.filter(pet =>
                pet.associatedVets.some(vet => vet.id === currentVetId)
            );
        }

        if (searchTerm) {
            const lowerSearchTerm = searchTerm.toLowerCase();
            petsToDisplay = petsToDisplay.filter(pet =>
                pet.name.toLowerCase().includes(lowerSearchTerm) ||
                pet.ownerUsername.toLowerCase().includes(lowerSearchTerm) ||
                pet.specie.toLowerCase().includes(lowerSearchTerm) ||
                (pet.breedName && pet.breedName.toLowerCase().includes(lowerSearchTerm)) ||
                (pet.microchip && pet.microchip.toLowerCase().includes(lowerSearchTerm))
            );
        }
        return petsToDisplay;
    }, [allClinicPets, searchTerm, isCurrentUserVet, showAllClinicPets, user?.id]);

    if (isLoadingAuth) {
        return <div className="flex justify-center items-center py-10"><Loader2 className="h-8 w-8 animate-spin text-cyan-500" /></div>;
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
     if (!clinicId && (user.roles?.includes("VET") || user.roles?.includes("ADMIN"))) {
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
                    <PawPrint className="mr-3 h-7 w-7 text-cyan-400" />Clinic Pet Management
                </h1>
                <div className="flex items-center gap-x-4 gap-y-2 flex-wrap w-full sm:w-auto justify-end">
                    <div className="relative">
                        <Search size={18} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 pointer-events-none" />
                        <Input
                            type="text"
                            placeholder="Search pets..."
                            value={searchTerm}
                            onChange={(e: ChangeEvent<HTMLInputElement>) => setSearchTerm(e.target.value)}
                            className="pl-10 pr-3 py-2 h-9 w-full sm:w-56 border-gray-700 bg-[#070913]/80 rounded-md focus:ring-cyan-600"
                        />
                    </div>
                    {isCurrentUserVet && (
                        <div className="flex items-center space-x-2">
                            <Checkbox
                                id="show-all-clinic-pets-filter"
                                checked={showAllClinicPets} 
                                onCheckedChange={(checked) => setShowAllClinicPets(!!checked)}
                                className="data-[state=checked]:bg-cyan-600 data-[state=checked]:text-white border-gray-500"
                            />
                            <Label htmlFor="show-all-clinic-pets-filter" className="text-sm font-medium text-gray-300 cursor-pointer whitespace-nowrap">
                                Show all pets 
                            </Label>
                        </div>
                    )}
                </div>
            </div>

            {error && (
                <div className="p-3 text-center text-red-400 bg-red-900/20 border border-red-500/50 rounded-lg flex items-center justify-center gap-2">
                    <AlertCircle size={20} /> {error}
                </div>
            )}

            <Card className="border-2 border-[#FFECAB]/50 bg-[#0c1225]/70 shadow-xl">
                <CardHeader>
                    <CardTitle className="text-[#FFECAB]">Active pets in clinic</CardTitle>
                </CardHeader>
                <CardContent className="p-0">
                    {isLoadingData ? (
                        <div className="flex justify-center items-center py-10"><Loader2 className="h-8 w-8 animate-spin text-cyan-500" /><span className="ml-2 text-gray-400">Loading pets...</span></div>
                    ) : displayedPets.length === 0 ? (
                        <p className="text-center text-gray-400 py-8 px-4">
                            {searchTerm ? 'No pets match your search criteria.' : 
                             isCurrentUserVet && !showAllClinicPets  ? 'You have no pets directly associated with you yet.' :
                             'No active pets found in this clinic.'}
                        </p>
                    ) : (
                        <Table>
                            <TableHeader>
                                <TableRow className="border-b-[#FFECAB]/20">
                                    <TableHead className="text-[#FFECAB]/80 pl-6">Pet name</TableHead>
                                    <TableHead className="text-[#FFECAB]/80 hidden md:table-cell">Owner</TableHead>
                                    <TableHead className="text-[#FFECAB]/80 hidden lg:table-cell">Breed</TableHead>
                                    <TableHead className="text-[#FFECAB]/80">Associated Vet(s)</TableHead>
                                    <TableHead className="text-[#FFECAB]/80 text-right pr-6">Actions</TableHead>
                                </TableRow>
                            </TableHeader>
                            <TableBody>
                                {displayedPets.map((pet) => (
                                    <TableRow key={pet.id} className="border-b-[#FFECAB]/10 hover:bg-[#FFECAB]/5">
                                        <TableCell className="pl-6">
                                            <div className="flex items-center gap-3">
                                                <img src={pet.image || `/src/assets/images/avatars/pets/${pet.specie.toLowerCase()}.png`} alt={pet.name} className="w-9 h-9 rounded-full object-cover" onError={(e) => (e.currentTarget.src = `/src/assets/images/avatars/pets/${pet.specie.toLowerCase()}.png`)} />
                                                <div>
                                                    <div className="font-medium text-white">{pet.name}</div>
                                                    <div className="text-xs text-gray-400 capitalize">{pet.specie.toLowerCase()}</div>
                                                </div>
                                            </div>
                                        </TableCell>
                                        <TableCell className="text-gray-300 hidden md:table-cell">{pet.ownerUsername}</TableCell>
                                        <TableCell className="text-gray-300 hidden lg:table-cell">{pet.breedName || 'N/A'}</TableCell>
                                        <TableCell className="text-gray-300 text-xs">
                                            {pet.associatedVets && pet.associatedVets.length > 0
                                                ? pet.associatedVets.map(v => `${v.name} ${v.surname}`).join(', ')
                                                : <span className="italic text-gray-500">None</span>
                                            }
                                        </TableCell>
                                        <TableCell className="text-right space-x-1 pr-6">
                                            <Tooltip>
                                                <TooltipTrigger asChild>
                                                    <Button size="icon" className="h-7 w-7 text-[#FFECAB] hover:text-[#090D1A]  hover:bg-[#FFECAB] cursor-pointer"
                                                        onClick={() => handleOpenDetailModal(pet)}
                                                    >
                                                        <Eye size={16} />
                                                    </Button>
                                                </TooltipTrigger>
                                                <TooltipContent className="bg-gray-950 text-white border border-cyan-700"><p>View Details</p></TooltipContent>
                                            </Tooltip>
                                        </TableCell>
                                    </TableRow>
                                ))}
                            </TableBody>
                        </Table>
                    )}
                </CardContent>
                {totalPages > 1 && !isLoadingData && displayedPets.length > 0 && (
                    <div className="p-4 border-t border-[#FFECAB]/20">
                        <Pagination currentPage={currentPage} totalPages={totalPages} onPageChange={handlePageChange} isLoading={isLoadingData} />
                    </div>
                )}
            </Card>

            {showDetailModal && selectedPet && (
                <ClinicPetDetailModal
                    isOpen={showDetailModal}
                    onClose={handleCloseDetailModal}
                    petProfileInitial={selectedPet}
                    onPetUpdate={handlePetUpdateInModal}
                />
            )}
        </div>
    );
};

export default PetManagementPage;