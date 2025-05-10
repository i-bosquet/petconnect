import { JSX } from 'react'; 
import { PetProfileDto } from '@/types/apiTypes';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';

interface PetQrTabProps { pet: PetProfileDto; }

const PetQrTab = ({ pet }: PetQrTabProps): JSX.Element => (
    <Card className="border-2 border-[#FFECAB]/20 bg-[#070913]/30">
        <CardHeader><CardTitle className="text-[#FFECAB]">Pet Qr - {pet.name}</CardTitle></CardHeader>
        <CardContent><p className="text-gray-400">(Qr, option to schedule new...)</p></CardContent>
    </Card>
);
export default PetQrTab;