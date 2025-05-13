import { JSX, useState } from 'react';
import Modal from '@/components/common/Modal';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { QRCodeCanvas } from 'qrcode.react'; 
import { Copy, Check, ExternalLink } from 'lucide-react';
import { toast } from 'sonner';

interface ShowTempAccessModalProps {
    isOpen: boolean;
    onClose: () => void;
    accessToken: string; 
    petName: string;
}

/**
 * ShowTempAccessModal - Displays the generated temporary access token/link
 * and its corresponding QR code for sharing.
 * @param {ShowTempAccessModalProps} props - Component props.
 * @returns {JSX.Element | null} The modal component.
 */
const ShowTempAccessModal = ({ isOpen, onClose, accessToken, petName }: ShowTempAccessModalProps): JSX.Element | null => {
    const [copied, setCopied] = useState(false);
    const qrValue = accessToken; 
    const shareableLink = `${window.location.origin}/verify-pet-records?token=${accessToken}`; 

    const handleCopyToClipboard = async () => {
        try {
            await navigator.clipboard.writeText(shareableLink); 
            setCopied(true);
            toast.success("Link copied to clipboard!");
            setTimeout(() => setCopied(false), 2000);
        } catch (err) {
            console.error('Failed to copy link: ', err);
            toast.error("Failed to copy link.");
        }
    };

    if (!isOpen) return null;

    return (
        <Modal title={`Share Access for ${petName}`} onClose={onClose} maxWidth="max-w-md">
            <div className="space-y-4 text-center">
                <p className="text-sm text-gray-300">
                    Share the QR code or the link below to provide temporary read-only access
                    to <strong className="text-[#FFECAB]">{petName}'s</strong> signed medical records.
                </p>

                <div className="p-4 bg-white rounded-lg inline-block my-4">
                    <QRCodeCanvas value={qrValue} size={180} bgColor="#ffffff" fgColor="#090D1A" level="M" />
                </div>

                <div className="space-y-2">
                    <Label htmlFor="shareableLink" className="text-gray-300 sr-only">Shareable Link</Label>
                    <div className="flex items-center gap-2">
                        <Input
                            id="shareableLink"
                            type="text"
                            value={shareableLink} 
                            readOnly
                            className="bg-gray-700 border-gray-600 text-gray-200 flex-grow"
                        />
                        <Button onClick={handleCopyToClipboard} size="icon" className=" border border-cyan-500 text-cyan-400 hover:bg-cyan-700/30 cursor-pointer">
                            {copied ? <Check size={18} /> : <Copy size={18} />}
                        </Button>
                    </div>
                     <a href={shareableLink} target="_blank" rel="noopener noreferrer" className="text-xs text-cyan-400 hover:underline inline-flex items-center">
                        Open link in new tab <ExternalLink size={12} className="ml-1"/>
                    </a>
                </div>
                
                <p className="text-xs text-gray-500 mt-3">
                    Access will expire based on the duration you selected.
                </p>

                <div className="pt-4">
                    <Button onClick={onClose} className="px-5 py-2.5 rounded-lg border border-[#FFECAB]/50 text-[#FFECAB] hover:bg-cyan-800 hover:text-[#FFECAB] focus-visible:ring-cyan-500 disabled:opacity-50 cursor-pointer">
                        Done
                    </Button>
                </div>
            </div>
        </Modal>
    );
};

export default ShowTempAccessModal;