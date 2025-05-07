import React, { useState, JSX, FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";
import { Eye, EyeOff, User, Lock, LogIn } from "lucide-react";
import { loginUser, getCurrentUserProfile } from "../../services/authService";
import {
  UserProfile,
  ClinicStaffProfile,
  OwnerProfile,
} from "@/types/apiTypes";
import ForgotPasswordModal from "../../components/auth/ForgotPasswordModal";

/**
 * Represents the user data stored in session/local storage after successful login.
 * Should align with StoredUserDataType from useAuth.
 */
interface StoredUserForStorage {
  id: number | string;
  username: string;
  email: string;
  roles: string[];
  avatar: string | null;
  jwt: string;

  // Owner
  phone?: string;

  // ClinicStaff
  name?: string;
  surname?: string;
  isActive?: boolean;
  clinicId?: number | string;
  clinicName?: string;
  licenseNumber?: string | null;
  vetPublicKey?: string | null;
}

/**
 * LoginPage Component - Handles user authentication using username and password
 * via the backend API.
 * Connects to the backend API '/api/auth/login' endpoint.
 * Stores authentication token and user info on success, then redirects.
 *
 * @returns {JSX.Element} The login page component.
 */
const LoginPage = (): JSX.Element => {
  const [username, setUsername] = useState<string>("");
  const [password, setPassword] = useState<string>("");
  const [showPassword, setShowPassword] = useState<boolean>(false);
  const [error, setError] = useState<string>("");
  const [rememberMe, setRememberMe] = useState(false);
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [isForgotPasswordModalOpen, setIsForgotPasswordModalOpen] =
    useState<boolean>(false);

  const navigate = useNavigate();

  /**
   * Handles the form submission for login.
   * Calls the backend API to authenticate the user. Stores user data and JWT
   * in either sessionStorage or localStorage based on the 'Remember me' checkbox,
   * then redirects based on role. Sets error message on failure.
   *
   * @param {FormEvent<HTMLFormElement>} e - The form submit event.
   */
  const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setError("");
    setIsLoading(true);

    try {
      const loginResponse = await loginUser({ username, password });
      console.log("Login API Response:", loginResponse);

      if (loginResponse?.jwt && loginResponse.status) {
        const token = loginResponse.jwt;
        // userProfile is OwnerProfile or ClinicStaffProfile
        const userProfile: UserProfile = await getCurrentUserProfile(token);
        console.log("Get User Profile Response:", userProfile);

        if (userProfile?.roles && userProfile.roles.length > 0) {
          const primaryRole = userProfile.roles[0]; // Take the first role for redirection logic

          // Build the object to be saved with all relevant fields
          const userDataToStore: StoredUserForStorage = {
            id: userProfile.id,
            username: userProfile.username,
            email: userProfile.email,
            avatar: userProfile.avatar,
            roles: userProfile.roles, 
            jwt: token,
          };

          // Add Owner-specific fields
          if ("phone" in userProfile && (userProfile as OwnerProfile).phone) {
            userDataToStore.phone = (userProfile as OwnerProfile).phone;
          }

          // Add ClinicStaff Specific Fields
          if (
            "clinicId" in userProfile &&
            (userProfile as ClinicStaffProfile).clinicId
          ) {
            const staffProfile = userProfile as ClinicStaffProfile;
            userDataToStore.name = staffProfile.name;
            userDataToStore.surname = staffProfile.surname;
            userDataToStore.isActive = staffProfile.isActive;
            userDataToStore.clinicId = staffProfile.clinicId;
            userDataToStore.clinicName = staffProfile.clinicName;
            if (staffProfile.licenseNumber)
              userDataToStore.licenseNumber = staffProfile.licenseNumber;
            if (staffProfile.vetPublicKey)
              userDataToStore.vetPublicKey = staffProfile.vetPublicKey;
          }

          const storage = rememberMe ? localStorage : sessionStorage;
          storage.setItem("user", JSON.stringify(userDataToStore));
          console.log(
            `User data (including specific profile fields) stored in ${
              rememberMe ? "localStorage" : "sessionStorage"
            }:`,
            userDataToStore
          );

          // Redirection based on the primary role
          if (primaryRole === "OWNER") {
            navigate("/pet", { replace: true });
          } else if (primaryRole === "VET" || primaryRole === "ADMIN") {
            navigate("/clinic", { replace: true });
          } else {
            setError(
              "Login successful, but user role is unrecognized for redirection."
            );
          }
        } else {
          console.error(
            "Could not determine user roles from profile API response:",
            userProfile
          );
          setError(
            "Login successful, but failed to retrieve user role information."
          );
        }
      } else {
        setError(
          loginResponse?.message ||
            "Login failed. Please check your credentials."
        );
      }
    } catch (err) {
      console.error("Login handleSubmit error:", err);
      setError(
        err instanceof Error ? err.message : "An unknown login error occurred."
      );
    } finally {
      setIsLoading(false);
    }
  };

  /**
   * Opens the Forgot Password modal and resets its state.
   */
  const openForgotPasswordModal = () => {
    setIsForgotPasswordModalOpen(true);
    setError("");
  };

  /**
   * Closes the Forgot Password modal.
   */
  const closeForgotPasswordModal = () => {
    setIsForgotPasswordModalOpen(false);
  };

  // --- JSX Rendering ---
  return (
    <div className="min-h-screen flex items-center justify-center bg-[#090D1A] p-4">
      <div className="w-full max-w-6xl flex flex-col lg:flex-row lg:items-center lg:justify-around">
        {/* Left Branding Section */}
        <div className="text-center mx-8 lg:text-left mb-8 lg:mb-0 lg:flex-1 lg:pr-12">
          <div className="flex justify-center lg:justify-start mb-3">
            <img
              src="/src/assets/images/SF-Logo1-D.png"
              alt="PetConnect"
              className="w-16 h-16 lg:w-24 lg:h-24"
            />
          </div>
          <h1 className="text-3xl lg:text-5xl font-bold text-[#FFECAB] mt-6">
            PetConnect
          </h1>
          <p className="text-gray-300 mt-2 lg:mt-4 lg:text-xl">
            Connect pets with the world
          </p>
          {/* Additional content for large screens */}
          <div className="hidden lg:block mt-8 space-y-4">
            <p className="text-gray-300">Welcome to PetConnect!</p>
            <ul className="text-gray-300 space-y-2">
              <li className="flex items-center">
                <span className="text-[#FFECAB] mr-2">✓</span> Access your
                personalized dashboard
              </li>
              <li className="flex items-center">
                <span className="text-[#FFECAB] mr-2">✓</span> Use secure
                messaging and communication tools
              </li>
              <li className="flex items-center">
                <span className="text-[#FFECAB] mr-2">✓</span> Manage records,
                appointments and certificates
              </li>
            </ul>
          </div>
        </div>

        {/* Right Login Form Section */}
        <div className="lg:flex-1 w-full max-w-lg mx-auto lg:mx-4">
          <div className="bg-[#090D1A] rounded-2xl shadow-xl p-8 border-2 border-[#FFECAB] transform transition-all hover:scale-[1.01]">
            <h2 className="text-2xl font-semibold text-[#FFECAB] mb-6">
              Sign In
            </h2>

            {/* Error message display */}
            {error && (
              <div className="mb-4 p-3 bg-red-900/30 text-red-300 rounded-lg text-sm">
                {error}
              </div>
            )}

            <form onSubmit={handleSubmit} className="space-y-6">
              {/* Username Input */}
              <div className="space-y-2">
                <label
                  htmlFor="username"
                  className="block text-sm font-medium text-gray-300"
                >
                  Username
                </label>
                <div className="relative">
                  <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                    <User size={18} className="text-gray-400" />{" "}
                    {/* User Icon */}
                  </div>
                  <input
                    id="username"
                    name="username"
                    type="text"
                    autoComplete="username"
                    required
                    value={username}
                    onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
                      setUsername(e.target.value)
                    }
                    className="block w-full pl-10 pr-3 py-3 border border-gray-700 rounded-xl shadow-sm placeholder:text-gray-400 focus:outline-none focus:ring-2 focus:ring-cyan-700 focus:border-cyan-500 text-white bg-gray-800 disabled:opacity-50"
                    placeholder="Your username"
                    disabled={isLoading}
                  />
                </div>
              </div>

              {/* Password Input */}
              <div className="space-y-2">
                <div className="flex justify-between items-center">
                  <label
                    htmlFor="password"
                    className="block text-sm font-medium text-gray-300"
                  >
                    Password
                  </label>
                  {/* Forgot Password Link */}
                  <div className="flex justify-between items-center">
                    <label
                      htmlFor="password"
                      className="block text-sm font-medium text-gray-300"
                    ></label>
                    <button
                      type="button"
                      onClick={openForgotPasswordModal}
                      className="text-sm font-medium text-cyan-400 hover:text-cyan-600 focus:outline-none cursor-pointer"
                    >
                      Forgot password?
                    </button>
                  </div>
                </div>
                {/* Input Password eye */}
                <div className="relative">
                  <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                    <Lock size={18} className="text-gray-400" />
                  </div>
                  <input
                    id="password"
                    name="password"
                    type={showPassword ? "text" : "password"}
                    autoComplete="current-password"
                    required
                    value={password}
                    onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
                      setPassword(e.target.value)
                    }
                    className="block w-full pl-10 pr-10 py-3 border border-gray-700 rounded-xl shadow-sm placeholder:text-gray-400 focus:outline-none focus:ring-2 focus:ring-cyan-700 focus:border-cyan-500 text-white bg-gray-800 disabled:opacity-50"
                    placeholder="••••••••"
                    disabled={isLoading}
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    className="absolute inset-y-0 right-0 pr-3 flex items-center text-gray-400 hover:text-gray-300 disabled:opacity-50"
                    disabled={isLoading}
                  >
                    {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                  </button>
                </div>
              </div>

              {/* Remember me checkbox option */}
              <div className="flex items-center justify-between">
                <div className="flex items-center">
                  <input
                    id="remember-me"
                    name="remember-me"
                    type="checkbox"
                    checked={rememberMe}
                    onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
                      setRememberMe(e.target.checked)
                    }
                    className="h-4 w-4 rounded border-gray-500 text-cyan-600 focus:ring-cyan-500 focus:ring-offset-gray-800 bg-gray-700 cursor-pointer"
                  />
                  <label
                    htmlFor="remember-me"
                    className="ml-2 block text-sm text-gray-300 cursor-pointer"
                  >
                    Remember me
                  </label>
                </div>
              </div>

              {/* Submit Button */}
              <div>
                <button
                  type="submit"
                  disabled={isLoading}
                  className="group relative w-full flex justify-center py-3 px-4 border border-transparent rounded-xl text-[#FFECAB] bg-cyan-800 hover:bg-cyan-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-cyan-500 font-medium transition-all duration-200 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  <span className="absolute left-0 inset-y-0 flex items-center pl-3">
                    {isLoading ? (
                      <svg
                        className="animate-spin h-5 w-5 text-[#FFECAB]" /* ... spinner svg ... */
                      ></svg>
                    ) : (
                      <LogIn size={18} className="group-hover:opacity-80" />
                    )}
                  </span>
                  {isLoading ? "Signing In..." : "Sign in"}
                </button>
              </div>
            </form>

            {/* Link to Signup */}
            <div className="text-center mt-8">
              <p className="text-sm text-gray-400">
                Don't have an account?{" "}
                <Link
                  to="/signup"
                  className="font-medium text-cyan-400 hover:text-cyan-600"
                >
                  Sign up
                </Link>
              </p>
            </div>
          </div>
        </div>
      </div>

      {/* Forgot Password Modal */}
      {isForgotPasswordModalOpen && (
        <ForgotPasswordModal
          isOpen={isForgotPasswordModalOpen}
          onClose={closeForgotPasswordModal}
        />
      )}
    </div>
  );
};

export default LoginPage;
