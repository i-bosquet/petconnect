import { useState, useEffect, useCallback, JSX } from "react";
import {
  findMyPets,
  getPetDetailsById,
  deactivatePet,
} from "@/services/petService";
import { PetProfileDto, Page, PetStatus } from "@/types/apiTypes";
import PetList from "@/components/pet/PetList";
import AddPetModal from "@/components/pet/modals/AddPetModal";
import EditPetModal from "@/components/pet/modals/EditPetModal";
import ConfirmationModal from "@/components/common/ConfirmationModal";
import PetDetailHeader from "@/components/pet/PetDetailHeader";
import PetDetailTabs from "@/components/pet/PetDetailTabs";
import { useAuth } from "@/hooks/useAuth";
import { useOwnerLayoutContext } from "@/hooks/useOwnerLayoutContext";
import { Loader2 } from "lucide-react";

/**
 * OwnerDashboardPage - Main container for the owner's pet management view.
 * Fetches and displays the list of pets, handles pet selection,
 * and manages the visibility of the AddPetModal.
 *
 * @returns {JSX.Element} The owner dashboard page component.
 */
const OwnerDashboardPage = (): JSX.Element => {
  const { token, isLoading: isLoadingAuth } = useAuth();
  const [pets, setPets] = useState<PetProfileDto[]>([]);
  const [selectedPet, setSelectedPet] = useState<PetProfileDto | null>(null);
  const [isLoadingPets, setIsLoadingPets] = useState<boolean>(true);
  const [isLoadingDetail, setIsLoadingDetail] = useState<boolean>(false);
  const [isDeactivating, setIsDeactivating] = useState<boolean>(false);
  const [error, setError] = useState<string>("");
  const [showAddModal, setShowAddModal] = useState<boolean>(false);
  const [showEditModal, setShowEditModal] = useState<boolean>(false);
  const [showDeactivateConfirmModal, setShowDeactivateConfirmModal] =
    useState<boolean>(false);
  const [petToDeactivate, setPetToDeactivate] = useState<PetProfileDto | null>(
    null
  );
  const [showInactivePets, setShowInactivePets] = useState<boolean>(false);
  const [everLoadedInactive, setEverLoadedInactive] = useState<boolean>(false);
  const { setSelectedPetForTopBar } = useOwnerLayoutContext();

  /**
   * Fetches the owner's pets from the API.
   * Can be called on mount and after adding/deleting a pet.
   */
  const fetchOwnerPets = useCallback(async () => {
    if (!token) return;
    setIsLoadingPets(true);
    setError('');
    try {
        const statusesToFetch: PetStatus[] = [PetStatus.ACTIVE, PetStatus.PENDING];
        if (showInactivePets) {
            statusesToFetch.push(PetStatus.INACTIVE);
        }
        const petsPage: Page<PetProfileDto> = await findMyPets(token, 0, 50, 'name,asc', statusesToFetch);
        setPets(petsPage.content);

        const hasAnyInactiveInResponse = petsPage.content.some(p => p.status === PetStatus.INACTIVE);

        if (showInactivePets && hasAnyInactiveInResponse) {
            setEverLoadedInactive(true);
        }
    } catch (err) {
        setError(err instanceof Error ? err.message : 'Could not load pet data.');
    } finally {
        setIsLoadingPets(false);
    }
}, [token, showInactivePets]);

useEffect(() => {
    if (selectedPet) {
        const petInCurrentList = pets.find(p => p.id === selectedPet.id);

        if (petInCurrentList) {
            if (petInCurrentList.status === PetStatus.INACTIVE && !showInactivePets) {
                setSelectedPet(null);
                setSelectedPetForTopBar(null);
            }
        } else {
            setSelectedPet(null);
            setSelectedPetForTopBar(null);
        }
    }
}, [pets, selectedPet, showInactivePets, setSelectedPet, setSelectedPetForTopBar]);

  /**
   * Fetch pets when the component mounts.
   */
  useEffect(() => {
    if (token) {
      console.log("[useEffect fetch initial/filter] Disparando fetchOwnerPets. showInactivePets:", showInactivePets);
      fetchOwnerPets();
    } else if (!isLoadingAuth && !token) {
      setError("User not authenticated.");
      setIsLoadingPets(false);
    }
  }, [token, isLoadingAuth, fetchOwnerPets, showInactivePets]);

  /**
   * Handles the selection of a pet, fetching its details and updating the state accordingly.
   * @param petId - The ID of the pet to select, or null to clear the selection.
   */
  const handleSelectPet = useCallback(
    async (petId: number | string | null) => {
      if (petId === null) {
        setSelectedPet(null);
        setSelectedPetForTopBar(null);
        return;
      }
      if (!token) {
        setError("Authentication token missing.");
        return;
      }
      setIsLoadingDetail(true);
      setError("");
      try {
        const petDetails = await getPetDetailsById(token, petId);
          setSelectedPet(petDetails);
          setSelectedPetForTopBar(petDetails);
      } catch (err) {
        setError(
          err instanceof Error ? err.message : "Could not load pet details."
        );
        setSelectedPet(null);
        setSelectedPetForTopBar(null);
      } finally {
        setIsLoadingDetail(false);
      }
    },
    [token, setSelectedPetForTopBar]
  );

  /**
   * Handles the addition or update of a pet by refreshing the owner's pets
   * and re-selecting the currently selected pet if applicable.
   */
  const handlePetAddedOrUpdated = useCallback(() => {
    fetchOwnerPets();
    if (selectedPet) {
      handleSelectPet(selectedPet.id);
    }
  }, [fetchOwnerPets, selectedPet, handleSelectPet]);

  /**
   * Opens the confirmation modal for deactivating a pet.
   * @param {PetProfileDto} pet - The pet to be deactivated.
   */
  const handleOpenDeactivateModal = (pet: PetProfileDto) => {
    setPetToDeactivate(pet);
    setShowDeactivateConfirmModal(true);
  };

  /**
   * Closes the deactivation confirmation modal.
   */
  const handleCloseDeactivateModal = () => {
    setShowDeactivateConfirmModal(false);
    setPetToDeactivate(null);
  };

  /**
   * Handles the confirmed deactivation of a pet.
   * Calls the service, updates state, and closes the modal.
   */
  const handleConfirmDeactivatePet = async () => {
    if (!petToDeactivate || !token) return;
    setIsDeactivating(true);
    setError("");
    try {
      await deactivatePet(token, petToDeactivate.id);
      console.log(`Pet ${petToDeactivate.name} deactivated successfully.`);
      handleCloseDeactivateModal();
      setSelectedPet(null);
      setSelectedPetForTopBar(null);
      fetchOwnerPets();
    } catch (err) {
      setError(
        err instanceof Error ? err.message : "Failed to deactivate pet."
      );
    } finally {
      setIsDeactivating(false);
    }
  };

    if (isLoadingAuth)
    if (error && pets.length === 0 && !selectedPet) 
        
    return (
      <div className="flex justify-center items-center py-10">
        <Loader2 className="h-8 w-8 animate-spin text-cyan-500" />
        <span className="ml-2">Loading authentication...</span>
      </div>
    );
  if (error && pets.length === 0 && !selectedPet)
    return (
      <div className="p-4 text-center text-red-400 bg-red-900/20 border border-red-500/50 rounded-lg">
        {error}
      </div>
    );

    const calculatedHasInactivePets = everLoadedInactive || (pets.some(p => p.status === PetStatus.INACTIVE && !showInactivePets));
console.log("[OwnerDashboardPage] Props to PetList - showInactive:", showInactivePets, "everLoadedInactive:", everLoadedInactive, "calculatedHasInactivePets:", calculatedHasInactivePets, "pets array:", pets); // LOG 5

  return (
    <div>
      {isLoadingPets && !selectedPet && (
        <div className="text-center py-10 text-gray-400">Loading pets...</div>
      )}

      {error && !selectedPet && (
        <div className="p-4 text-center text-red-400 bg-red-900/20 border border-red-500/50 rounded-lg">
          {error}
        </div>
      )}

      {/* Pet List */}
      {!isLoadingPets && !selectedPet && (
        <PetList
          pets={pets}
          onSelectPet={(petId) => handleSelectPet(petId)}
          onAddPet={() => setShowAddModal(true)}
          showInactive={showInactivePets}
         onToggleShowInactive={(checked) => {
            setShowInactivePets(checked);
       }}
        />
      )}

      {isLoadingDetail && selectedPet && (
        <div className="text-center py-10 text-gray-400">
          Loading pet details...
        </div>
      )}

      {/* Pet Detail */}
      {!isLoadingDetail && selectedPet && (
        <div>
          <PetDetailHeader
            pet={selectedPet}
            onBack={() => handleSelectPet(null)}
            onEdit={() => setShowEditModal(true)}
            onDeactivate={() => handleOpenDeactivateModal(selectedPet)}
          />
          <PetDetailTabs pet={selectedPet} />
        </div>
      )}

      {error && selectedPet && (
        <div className="p-4 text-center text-red-400 bg-red-900/20 border border-red-500/50 rounded-lg">
          {error}
        </div>
      )}

      {/* Modals */}
      {showAddModal && (
        <AddPetModal
          onClose={() => setShowAddModal(false)}
          onPetAdded={() => {
            setShowAddModal(false);
            handlePetAddedOrUpdated();
          }}
        />
      )}

      {showEditModal && selectedPet && (
        <EditPetModal
          petInitialData={selectedPet}
          onClose={() => {
            setShowEditModal(false);
          }}
          onPetUpdated={() => {
            setShowEditModal(false);
            handlePetAddedOrUpdated();
          }}
        />
      )}

      {petToDeactivate && (
        <ConfirmationModal
          isOpen={showDeactivateConfirmModal}
          onClose={handleCloseDeactivateModal}
          onConfirm={handleConfirmDeactivatePet}
          title="Confirm Deactivation"
          message={
            <>
              Are you sure you want to deactivate{" "}
              <strong className="text-[#FFECAB]">{petToDeactivate.name}</strong>
              ?
              <br />
              <br />
              This action will mark the pet as inactive, and all associated
              veterinarians will be unlinked. The pet will no longer appear in
              your active list by default.
            </>
          }
          confirmButtonText="Yes, Deactivate"
          isLoading={isDeactivating}
        />
      )}
    </div>
  );
};

export default OwnerDashboardPage;
