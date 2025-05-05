import { JSX } from 'react';

const ClinicDashboardPage = (): JSX.Element => {
    return (
        <div>
            <h1 className="text-2xl font-bold text-[#FFECAB] mb-4">Clinic Management Dashboard</h1>
             <p className="text-gray-400">(Clinic Info / Staff List / Pet List will be here)</p>
             <div className="mt-6 p-6 border border-dashed border-gray-600 rounded-lg">
                 Content Area for Clinic Management
             </div>
        </div>
    );
};
export default ClinicDashboardPage;