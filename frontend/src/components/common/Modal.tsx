import { JSX, ReactNode } from 'react';
import { X } from 'lucide-react';

// Definir las props usando una interfaz TypeScript
interface ModalProps {
    title: string;         
    children: ReactNode;   
    onClose: () => void;   
    maxWidth?: string;     
}

/**
 * Modal - A reusable overlay component for displaying content.
 * Creates a fixed overlay with a customizable title, content area,
 * and close button. Aligns content towards the top by default.
 *
 * @param {ModalProps} props - Component props.
 * @returns {JSX.Element} The modal component.
 */
const Modal = ({ title, children, onClose, maxWidth = 'max-w-lg' }: ModalProps): JSX.Element => {

    // Prevent click inside the modal content from closing it
    const handleContentClick = (e: React.MouseEvent<HTMLDivElement>) => {
        e.stopPropagation();
    };

    return (
        // Full screen overlay with semi-transparent background, aligned top
        <div
            className="fixed inset-0 bg-black/80 flex items-start justify-center p-4 pt-12 sm:pt-16 z-50 animate-fade-in backdrop-blur-sm"
            onClick={onClose} // Close modal if backdrop is clicked
            role="dialog"
            aria-modal="true"
            aria-labelledby="modal-title"
        >
            {/* Modal container */}
            <div
                 className={`bg-[#0c1225] rounded-xl shadow-xl w-full ${maxWidth} border border-[#FFECAB]/40 max-h-[90vh] flex flex-col overflow-hidden animate-fade-in`}
                onClick={handleContentClick} // Prevent closing when clicking inside content
            >
                {/* Modal header */}
                <div className="flex justify-between items-center p-4 sm:p-5 border-b border-[#FFECAB]/30 flex-shrink-0">
                    <h2 id="modal-title" className="text-lg sm:text-xl font-semibold text-[#FFECAB]">
                        {title}
                    </h2>
                    <button
                        onClick={onClose}
                        className="text-gray-400 hover:text-[#FFECAB] p-1 rounded-full hover:bg-cyan-900/50 cursor-pointer"
                        aria-label="Close modal"
                    >
                        <X size={24} />
                    </button>
                </div>

                {/* Modal content area - scrollable */}
                <div className="p-4 sm:p-6 overflow-y-auto flex-grow custom-scrollbar"> 
                    {children}
                </div>
            </div>
        </div>
    );
};

export default Modal;