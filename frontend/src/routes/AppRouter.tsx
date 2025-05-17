import { JSX } from 'react';
import { Routes, Route } from 'react-router-dom';
//Layouts
import OwnerLayout from '@/layouts/OwnerLayout'; 
import ClinicLayout from '@/layouts/ClinicLayout';
// Pages
import LandingPage from '@/pages/Landing/LandingPage';
import LoginPage from '@/pages/Auth/LoginPage';
import SignupPage from '@/pages/Auth/SignupPage';
import ResetPasswordPage from '@/pages/Auth/ResetPasswordPage';
import OwnerDashboardPage from '@/pages/Owner/OwnerDashboardPage';
import ClinicDashboardPage from '@/pages/Clinic/ClinicDashboardPage';
import StaffManagementPage from '@/pages/Clinic/StaffManagementPage';
import RecordManagementPage from '@/pages/Clinic/RecordManagementPage'; 
import CertificateManagementPage from '@/pages/Clinic/CertificateManagementPage';
import ClinicManagementPage from '@/pages/Clinic/ClinicManagementPage';
import PetManagementPage from '@/pages/Clinic/PetManagementPage'; 
import VerifyRecordsPage from '@/pages/Verify/VerifyRecordsPage'; 
import NotFoundPage from '@/pages/NotFound/NotFoundPage';
// Protected routes
import OwnerProtectedRoute from '@/routes/OwnerProtectedRoute';
import ClinicProtectedRoute from '@/routes/ClinicProtectedRoute';


/**
 * AppRouter - Defines the main routing configuration for the application.
 * Maps URL paths to their corresponding page components.
 * Includes public routes, authenticated routes (later protected), and a catch-all route.
 *
 * @returns {JSX.Element} The configured Routes component.
 */
const AppRouter = (): JSX.Element => {
  return (
    <Routes>
      {/* Public Routes */}
      <Route path="/" element={<LandingPage />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/signup" element={<SignupPage />} />
      <Route path="/reset-password" element={<ResetPasswordPage />} />
      {/* This route is public and allows access to the verification page via a token */}
      <Route path="/verify-pet-records" element={<VerifyRecordsPage />} /> 

       {/* Authenticated Routes*/}
       {/* Owner Routes */}
       <Route element={<OwnerProtectedRoute />}>
        <Route path="/pet" element={<OwnerLayout />}>
          <Route path="/pet" element={<OwnerDashboardPage />} />
        </Route>
      </Route>

      {/* Clinic Routes */}
      <Route element={<ClinicProtectedRoute />}> 
        <Route path="/clinic" element={<ClinicLayout />}> 
          <Route index element={<ClinicDashboardPage />} />
          <Route path="dashboard" element={<ClinicDashboardPage />} />
          <Route path="staff" element={<StaffManagementPage />} /> 
          <Route path="pets" element={<PetManagementPage />} />
          <Route path="records" element={<RecordManagementPage />} /> 
          <Route path="certificates" element={<CertificateManagementPage />} /> 
          <Route path="settings" element={<ClinicManagementPage />} />
        </Route>
      </Route>
      
      {/* Catch-all Not Found Route */}
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  );
};

export default AppRouter;