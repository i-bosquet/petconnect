import { JSX } from 'react';
import { QrCode, FileText, History, Home } from "lucide-react";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { PetProfileDto } from '@/types/apiTypes';
import PetHomeTab from "./tabs/PetHomeTab";
import PetRecordsTab from './tabs/PetRecordsTab';
import PetDocumentsTab from './tabs/PetCertificatesTab';
import PetQrTab from './tabs/PetQrTab';
import { cn } from '@/lib/utils'; 

interface PetDetailTabsProps {
    pet: PetProfileDto;
}

const PetDetailTabs = ({ pet }: PetDetailTabsProps): JSX.Element => {
 const baseTriggerClasses = "flex-1 min-w-[70px] sm:min-w-[100px] justify-center items-center gap-1.5 px-2 py-2.5 h-auto text-sm font-medium cursor-pointer";
    const inactiveTriggerClasses = "text-gray-400 hover:text-[#FFECAB] hover:bg-[#090D1A]/30";

    return (
        <Tabs defaultValue="home" className="w-full mt-4">
            <TabsList className="flex w-full justify-around gap-1 bg-transparent ">
                <TabsTrigger
                    value="home"
                    className={cn(
                        baseTriggerClasses,
                        inactiveTriggerClasses, 
                        "data-[state=active]:bg-transparent data-[state=active]:text-[#FFECAB] data-[state=active]:border-b-2 data-[state=active]:border-[#FFECAB] data-[state=active]:shadow-none" 
                    )}
                >
                    <Home size={16} className="inline-block sm:mr-2" />
                    <span className="hidden sm:inline">Home</span>
                </TabsTrigger>
                <TabsTrigger
                    value="Record"
                    className={cn(
                        baseTriggerClasses,
                        inactiveTriggerClasses,
                        "data-[state=active]:bg-transparent data-[state=active]:text-[#FFECAB] data-[state=active]:border-b-2 data-[state=active]:border-[#FFECAB] data-[state=active]:shadow-none"
                    )}
                >
                    <History size={16} className="inline-block sm:mr-2" />
                    <span className="hidden sm:inline">Records</span>
                </TabsTrigger>
                <TabsTrigger
                    value="certificates"
                    className={cn(
                        baseTriggerClasses,
                        inactiveTriggerClasses,
                        "data-[state=active]:bg-transparent data-[state=active]:text-[#FFECAB] data-[state=active]:border-b-2 data-[state=active]:border-[#FFECAB] data-[state=active]:shadow-none"
                    )}
                >
                    <FileText size={16} className="inline-block sm:mr-2" />
                    <span className="hidden sm:inline">Cretificates</span>
                </TabsTrigger>
                <TabsTrigger
                    value="qr"
                    className={cn(
                        baseTriggerClasses,
                        inactiveTriggerClasses,
                        "data-[state=active]:bg-transparent data-[state=active]:text-[#FFECAB] data-[state=active]:border-b-2 data-[state=active]:border-[#FFECAB] data-[state=active]:shadow-none"
                    )}
                >
                    <QrCode size={16} className="inline-block sm:mr-2" />
                    <span className="hidden sm:inline">QR</span>
                </TabsTrigger>
            </TabsList>

            <TabsContent value="home" className="mt-2"><PetHomeTab pet={pet} /></TabsContent>
            <TabsContent value="records" className="mt-2"><PetRecordsTab pet={pet} /></TabsContent>
            <TabsContent value="certificates" className="mt-2"><PetDocumentsTab pet={pet} /></TabsContent>
            <TabsContent value="qr" className="mt-2"><PetQrTab pet={pet} /></TabsContent>
        </Tabs>
    );
}
export default PetDetailTabs;