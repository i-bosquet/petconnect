import { JSX } from 'react'; // Import React
import { PetProfileDto } from '@/types/apiTypes';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';

interface PetRecordsTabProps { pet: PetProfileDto; }

const PetRecordsTab = ({ pet }: PetRecordsTabProps): JSX.Element => (
    <Card className="border-2 border-[#FFECAB]/50 bg-[#0c1225]/70 shadow-xl">
        <CardHeader><CardTitle className="text-[#FFECAB]">Pet Records - {pet.name}</CardTitle></CardHeader>
        <CardContent><p className="text-gray-400">(history records, option to schedule new...)</p></CardContent>
    </Card>
);
export default PetRecordsTab;