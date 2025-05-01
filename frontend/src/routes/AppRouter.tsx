import { JSX } from 'react';
import { Routes, Route } from 'react-router-dom';

import LandingPage from '../pages/Landing/LandingPage';
import LoginPage from '../pages/Auth/LoginPage';
import SignupPage from '../pages/Auth/SignupPage';
import ResetPasswordPage from '../pages/Auth/ResetPasswordPage';
// import OwnerDashboardPage from '../pages/Owner/OwnerDashboardPage';
// import ClinicDashboardPage from '../pages/Clinic/ClinicDashboardPage';
// import NotFoundPage from '../pages/NotFound/NotFoundPage';

// --- Placeholders  ---
const OwnerDashboardPlaceholder = () => <div className="text-cyan-800 font-bold p-4">Owner Dashboard Placeholder</div>;
const ClinicDashboardPlaceholder = () => <div className="text-cyan-800 font-bold p-4">Clinic Dashboard Placeholder</div>;
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
      <Route path="/pet" element={<OwnerDashboardPlaceholder />} />
      <Route path="/clinic" element={<ClinicDashboardPlaceholder />} />

      {/* Catch-all Not Found Route */}
      <Route path="*" element={<NotFoundPlaceholder />} />
    </Routes>
  );
};

export default AppRouter;