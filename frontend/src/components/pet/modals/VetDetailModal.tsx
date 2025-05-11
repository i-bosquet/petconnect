import { JSX } from 'react';
import Modal from '@/components/common/Modal';
import { VetSummaryDto } from '@/types/apiTypes';
import { Mail, Phone, Building, MapPin, Globe } from 'lucide-react';

interface VetDetailModalProps {
    isOpen: boolean;
    onClose: () => void;
    vet: VetSummaryDto | null; 
}

/**
 * VetDetailModal - Displays detailed information about a veterinarian
 * and their associated clinic.
 * @param {VetDetailModalProps} props - Component props.
 * @returns {JSX.Element | null} The modal component or null if not open or no vet data.
 */
const VetDetailModal = ({ isOpen, onClose, vet }: VetDetailModalProps): JSX.Element | null => {
    if (!isOpen || !vet) {
        return null;
    }

    const formatCountry = (countryCode: string | null | undefined) => {
        if (!countryCode) return 'N/A';
        return countryCode.replace(/_/g, ' '); 
    };

    return (
        <Modal title={`Veterinarian Details: Dr. ${vet.name} ${vet.surname}`} onClose={onClose} maxWidth="max-w-xl">
            <div className="space-y-5">
                {/* Veterinarian Info */}
                <div className="pb-4 border-b border-[#FFECAB]/20">
                    <div className="flex items-center gap-4 mb-3">
                        <img
                            src={vet.avatar || '/src/assets/images/avatars/users/default_avatar.png'}
                            alt={`Dr. ${vet.name} ${vet.surname}`}
                            className="w-20 h-20 rounded-full object-cover border-2 border-cyan-500"
                            onError={(e) => (e.currentTarget.src = '/src/assets/images/avatars/users/default_avatar.png')}
                        />
                        <div>
                            <h3 className="text-xl font-semibold text-white">Dr. {vet.name} {vet.surname}</h3>
                            {vet.email && (
                                <div className="flex items-center text-sm text-gray-300 mt-1">
                                    <Mail size={14} className="mr-2 text-cyan-400" />
                                    <span>{vet.email}</span>
                                </div>
                            )}
                        </div>
                    </div>
                </div>

                {/* Clinic Info */}
                {vet.clinicName && ( 
                    <div>
                        <h4 className="text-lg font-semibold text-[#FFECAB] mb-2 flex items-center">
                            <Building size={18} className="mr-2 text-cyan-400" />
                            Clinic Information
                        </h4>
                        <div className="space-y-1.5 text-sm pl-2">
                            <p className="text-white"><strong className="text-gray-400">Name:</strong> {vet.clinicName}</p>
                            {vet.clinicAddress && (
                                <div className="flex items-start">
                                    <MapPin size={14} className="mr-2 mt-0.5 text-cyan-400 flex-shrink-0" />
                                    <span className="text-white">{vet.clinicAddress}, {vet.clinicCity || ''}</span>
                                </div>
                            )}
                             {vet.clinicCountry && (
                                <div className="flex items-center">
                                    <Globe size={14} className="mr-2 text-cyan-400" />
                                    <span className="text-white">{formatCountry(vet.clinicCountry)}</span>
                                </div>
                            )}
                            {vet.clinicPhone && (
                                <div className="flex items-center">
                                    <Phone size={14} className="mr-2 text-cyan-400" />
                                    <span className="text-white">{vet.clinicPhone}</span>
                                </div>
                            )}
                        </div>
                    </div>
                )}
                {!vet.clinicName && (
                     <p className="text-sm text-gray-400 italic">Clinic details not available for this veterinarian.</p>
                )}
            </div>
        </Modal>
    );
};

export default VetDetailModal;