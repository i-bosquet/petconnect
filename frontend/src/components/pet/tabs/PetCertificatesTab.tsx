import { JSX } from 'react'; 
import { PetProfileDto } from '@/types/apiTypes';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';

interface PetCtertificatesTabProps { pet: PetProfileDto; }

const PetCtertificatesTab = ({ pet }: PetCtertificatesTabProps): JSX.Element => (
    <Card className="border-2 border-[#FFECAB]/50 bg-[#0c1225]/70 shadow-xl">
        <CardHeader><CardTitle className="text-[#FFECAB]">Pet Certificates - {pet.name}</CardTitle></CardHeader>
        <CardContent><p className="text-gray-400">(List of documents, option to schedule new...)</p></CardContent>
    </Card>
);
export default PetCtertificatesTab;