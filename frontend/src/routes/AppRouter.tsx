import { JSX } from 'react';
import { Routes, Route } from 'react-router-dom';
//Layouts
import OwnerLayout from '../layouts/OwnerLayout'; 
import ClinicLayout from '../layouts/ClinicLayout';
// Pages
import LandingPage from '../pages/Landing/LandingPage';
import LoginPage from '../pages/Auth/LoginPage';
import SignupPage from '../pages/Auth/SignupPage';
import ResetPasswordPage from '../pages/Auth/ResetPasswordPage';
import OwnerDashboardPage from '../pages/Owner/OwnerDashboardPage';
import ClinicDashboardPage from '../pages/Clinic/ClinicDashboardPage';
// import NotFoundPage from '../pages/NotFound/NotFoundPage';


const NotFoundPlaceholder = () => <div className="text-cyan-800 font-bold p-4">404 Not Found Placeholder</div>;


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
      <Route element={<OwnerLayout />}> 
        <Route path="/pet" element={<OwnerDashboardPage />} />
      </Route>
      {/* Clinic Routes */}
      <Route element={<ClinicLayout />}> 
        <Route path="/clinic" element={<ClinicDashboardPage />} />
      </Route>

      {/* Catch-all Not Found Route */}
      <Route path="*" element={<NotFoundPlaceholder />} />
    </Routes>
  );
};

export default AppRouter;