import { JSX } from 'react';
import { Routes, Route } from 'react-router-dom';
import LandingPage from './pages/Landing/LandingPage';
import LoginPage from './pages/Auth/LoginPage';

const SignupPagePlaceholder = () => <div className="text-cyan-800 font-bold p-4">Signup Page Placeholder</div>;
const OwnerDashboardPlaceholder = () => <div className="text-cyan-800 font-bold p-4">Owner Dashboard Placeholder</div>;
const ClinicDashboardPlaceholder = () => <div className="text-cyan-800 font-bold p-4">Clinic Dashboard Placeholder</div>;
const NotFoundPlaceholder = () => <div className="text-cyan-800 font-bold p-4">404 Not Found Placeholder</div>;


/**
 * App - Configures the main application routes.
 * @returns {JSX.Element} The application component with routing setup.
 */
function App(): JSX.Element {
  return (
    <Routes>
      <Route path="/" element={<LandingPage />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/signup" element={<SignupPagePlaceholder />} />
      <Route path="/pet" element={<OwnerDashboardPlaceholder />} /> 
      <Route path="/clinic" element={<ClinicDashboardPlaceholder />} /> 
      <Route path="*" element={<NotFoundPlaceholder />} /> 
    </Routes>
  );
}

export default App;