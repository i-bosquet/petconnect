import { JSX } from 'react';
import Modal from './Modal'; 
import { Button } from "@/components/ui/button";
import { AlertTriangle } from 'lucide-react';

interface ConfirmationModalProps {
    isOpen: boolean;
    onClose: () => void;
    onConfirm: () => void;
    title: string;
    message: string | JSX.Element; 
    confirmButtonText?: string;
    cancelButtonText?: string;
    isLoading?: boolean;
}

/**
 * ConfirmationModal - A reusable modal for confirming user actions.
 * @param {ConfirmationModalProps} props - Component props.
 * @returns {JSX.Element | null} The confirmation modal or null if not open.
 * @author ibosquet
 */
const ConfirmationModal = ({
    isOpen,
    onClose,
    onConfirm,
    title,
    message,
    confirmButtonText = "Confirm",
    cancelButtonText = "Cancel",
    isLoading = false
}: ConfirmationModalProps): JSX.Element | null => {
    if (!isOpen) {
        return null;
    }

    return (
        <Modal title={title} onClose={onClose} maxWidth="max-w-md">
            <div className="text-center">
                <AlertTriangle className="mx-auto h-12 w-12 text-red-600 mb-4" />
                <p className="text-sm text-gray-300 mb-6 whitespace-pre-wrap">{message}</p>
            </div>
            <div className="flex justify-end gap-3 mt-6 pt-4 border-t border-[#FFECAB]/20">
                <Button
                    onClick={onClose}
                    disabled={isLoading}
                    className="border-[#FFECAB]/50 text-[#FFECAB] bg-cyan-800 hover:bg-cyan-600 cursor-pointer"
                >
                    {cancelButtonText}
                </Button>
                <Button
                    onClick={onConfirm}
                    disabled={isLoading}
                    className="bg-red-800 hover:bg-red-500 text-white cursor-pointer"
                >
                    {isLoading ? "Processing..." : confirmButtonText}
                </Button>
            </div>
        </Modal>
    );
};

export default ConfirmationModal;