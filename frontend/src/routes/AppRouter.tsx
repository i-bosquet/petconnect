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
import ClinicManagementPage from '@/pages/Clinic/ClinicManagementPage';
import PetManagementPage from '@/pages/Clinic/PetManagementPage'; 
import NotFoundPage from '@/pages/NotFound/NotFoundPage';


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

       {/* Authenticated Routes*/}
       {/* Owner Routes */}
       <Route path="/pet" element={<OwnerLayout />}>
        <Route path="/pet" element={<OwnerDashboardPage />} />
      </Route>
      {/* Clinic Routes */}
      <Route path="/clinic" element={<ClinicLayout />}> 
        <Route index element={<ClinicDashboardPage />} />
        <Route path="dashboard" element={<ClinicDashboardPage />} />
        <Route path="staff" element={<StaffManagementPage />} /> 
        <Route path="pets" element={<PetManagementPage />} /> 
        <Route path="settings" element={<ClinicManagementPage />} />
      </Route>

      {/* Catch-all Not Found Route */}
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  );
};

export default AppRouter;