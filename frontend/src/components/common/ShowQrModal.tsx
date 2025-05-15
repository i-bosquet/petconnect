import { JSX, useState } from 'react';
import Modal from '@/components/common/Modal';
import { Button } from '@/components/ui/button';
import { QRCodeCanvas } from 'qrcode.react';
import { Copy, Check} from 'lucide-react';
import { toast } from 'sonner';
import { Label } from '@/components/ui/label';
import { Tooltip, TooltipContent, TooltipTrigger } from '@/components/ui/tooltip';

interface ShowQrModalProps {
    isOpen: boolean;
    onClose: () => void;
    qrData: string; 
    title: string;
    infoText?: string;
}

/**
 * ShowQrModal - A generic modal to display a QR code generated from a string,
 * an optional shareable link, and informational text.
 * @param {ShowQrModalProps} props - Component props.
 * @returns {JSX.Element | null} The modal component.
 */
const ShowQrModal = ({
     isOpen,
    onClose,
    qrData,
    title,
    infoText,
}: ShowQrModalProps): JSX.Element | null => {
    const [copiedQrData, setCopiedQrData] = useState(false);

    const handleCopyQrData = async () => {
        try {
            await navigator.clipboard.writeText(qrData);
            setCopiedQrData(true);
            toast.success("QR data copied to clipboard!");
            setTimeout(() => setCopiedQrData(false), 2000);
        } catch (err) {
            console.error("Failed to copy QR data:", err);
            toast.error("Failed to copy QR data.");
        }
    };

    if (!isOpen) return null;

    return (
        <Modal title={title} onClose={onClose}  maxWidth="max-w-4xl" >
            <div className="space-y-4 text-center ">
                {infoText && <p className="text-sm text-gray-300">{infoText}</p>}
                <div className="p-3 bg-white rounded-lg inline-block my-3 border-4 border-gray-300">
                    <QRCodeCanvas value={qrData} size={192} bgColor="#ffffff" fgColor="#090D1A" level="M" />
                </div>
                <div className="space-y-1 text-left">
                    <Label htmlFor="qrDataDisplay" className="text-gray-300 mb-2">
                        <Tooltip>
                            <TooltipTrigger asChild>
                                <Button 
                                    onClick={handleCopyQrData}
                                    size="icon" 
                                    className="text-[#FFECAB] hover:bg-cyan-700 cursor-pointer bg-transparent"
                                    aria-label="Copy QR Data"
                                >
                                    {copiedQrData ? <Check size={16} /> : <Copy size={16} />}
                                </Button>
                            </TooltipTrigger>
                            <TooltipContent className="bg-gray-950 text-white border border-cyan-700">
                                <p>Copy Raw Data</p>
                            </TooltipContent>
                        </Tooltip>
                        Raw QR Data
                    </Label>
                    <div className="flex items-start justify-between  gap-2"> 
                        <div 
                            id="qrDataDisplay"
                            className="flex-grow bg-gray-700 border border-gray-600 text-gray-300 text-xs p-2 rounded-md custom-scrollbar h-auto min-h-[60px] max-h-lvh overflow-y-auto whitespace-pre-wrap break-all relative"
                        >
                            
                        <div className='mt-2'>
                            {qrData}
                            </div>
                        </div>
                        
                    </div>
                </div>
            </div>
        </Modal>
    );
};
export default ShowQrModal;