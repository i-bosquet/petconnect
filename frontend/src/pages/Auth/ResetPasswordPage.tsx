import { useState, useEffect, JSX, FormEvent, ChangeEvent  } from 'react';
import { useSearchParams, useNavigate, Link } from 'react-router-dom';
import { Lock, Eye, EyeOff, CheckCircle, AlertCircle, KeyRound } from 'lucide-react';
import { resetPassword as resetPasswordApiCall } from '../../services/authService';

/**
 * ResetPasswordPage Component - Allows users to set a new password using a reset token.
 * Reads the token from the URL query parameter, displays a form for the new password,
 * calls the backend API to reset the password, and provides feedback.
 *
 * @returns {JSX.Element} The reset password page component.
 */
const ResetPasswordPage = (): JSX.Element => {
    const [searchParams] = useSearchParams();
    const navigate = useNavigate();

    const [token, setToken] = useState<string | null>(null);
    const [newPassword, setNewPassword] = useState<string>('');
    const [confirmPassword, setConfirmPassword] = useState<string>('');
    const [showPassword, setShowPassword] = useState<boolean>(false);
    const [showConfirmPassword, setShowConfirmPassword] = useState<boolean>(false);
    const [error, setError] = useState<string>('');
    const [successMessage, setSuccessMessage] = useState<string>('');
    const [isLoading, setIsLoading] = useState<boolean>(false);

    /**
     * useEffect hook to extract the reset token from the URL query parameter 'token'
     * when the component mounts. Redirects to login if no token is found.
     */
    useEffect(() => {
        const resetToken = searchParams.get('token');
        if (resetToken) {
            setToken(resetToken);
            console.log("Reset token found in URL:", resetToken);
        } else {
            console.error("No password reset token found in URL query parameters.");
            setError("Invalid or missing password reset link.");
            setToken(''); // Set token to empty string to distinguish from initial null state
        }
    }, [searchParams]);

    /**
     * Client-side validation for the new password form.
     * @returns {boolean} True if validation passes.
     */
    const validateForm = (): boolean => {
        if (!newPassword || !confirmPassword) {
            setError("Both password fields are required.");
            return false;
        }
        if (newPassword !== confirmPassword) {
            setError("Passwords do not match.");
            return false;
        }
        if (newPassword.length < 8) {
            setError("Password must be at least 8 characters long.");
            return false;
        }
        return true;
    };

    /**
     * Handles the submission of the new password form.
     * Performs validation and calls the backend API to reset the password.
     *
     * @param {FormEvent<HTMLFormElement>} e - The form submit event.
     */
    const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        setError('');
        setSuccessMessage('');

        if (!token) {
            setError("Invalid or missing reset token.");
            return;
        }

        if (!validateForm()) {
            return;
        }

        setIsLoading(true);

        try {
            await resetPasswordApiCall({
                token: token,
                newPassword: newPassword,
                confirmPassword: confirmPassword
            });

            setSuccessMessage("Your password has been reset successfully! Redirecting to login...");
            // Redirect to login after a delay
            setTimeout(() => {
                navigate('/login');
            }, 3000);

        } catch (err) {
            console.error('Reset Password handleSubmit error:', err);
            setError(err instanceof Error ? err.message : 'An unknown error occurred while resetting the password.');
            setIsLoading(false); 
        }
    };

    // Render logic based on token presence
    if (token === null) {
        // Still checking for token or redirecting
        return <div className="min-h-screen flex items-center justify-center bg-[#090D1A] text-white">Loading reset page...</div>;
    }

    // --- JSX Rendering ---
    return (
        <div className="min-h-screen flex items-center justify-center bg-[#090D1A] p-4">
            <div className="w-full max-w-md">
                <div className="bg-[#0c1225] rounded-xl shadow-xl p-8 border border-[#FFECAB]/40">
                    <div className="text-center mb-6">
                        <KeyRound className="mx-auto h-12 w-12 text-cyan-500" /> 
                        <h2 className="mt-2 text-2xl font-semibold text-[#FFECAB]">Set New Password</h2>
                        {!token && error && ( // Show explanation only if token extraction failed
                             <p className="mt-2 text-sm text-red-400">{error}</p>
                        )}
                         {token && ( // Show instructions if token is present
                            <p className="mt-2 text-sm text-gray-400">Enter and confirm your new password below.</p>
                         )}
                    </div>

                    {/* Display Success or Error Messages */}
                    {successMessage && (
                        <div className="my-4 p-3 bg-green-900/30 text-green-300 rounded-lg text-sm flex items-center">
                             <CheckCircle size={18} className="mr-2 flex-shrink-0" />
                            <span>{successMessage}</span>
                        </div>
                    )}
                    {error && !successMessage && ( // Show error only if no success message
                         <div className="my-4 p-3 bg-red-900/30 text-red-300 rounded-lg text-sm flex items-center">
                             <AlertCircle size={18} className="mr-2 flex-shrink-0" />
                             <span>{error}</span>
                         </div>
                    )}

                    {/* Show form only if token is present and no success message */}
                    {token && !successMessage && (
                         <form onSubmit={handleSubmit} className="space-y-5"> 
                             {/* New Password Input */}
                             <div className="space-y-1.5"> 
                                 <label htmlFor="newPassword" className="block text-sm font-medium text-gray-300">New Password</label>
                                 <div className="relative">
                                      <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none"> <Lock size={18} className="text-gray-400" /> </div>
                                     <input
                                         id="newPassword"
                                         name="newPassword"
                                         type={showPassword ? "text" : "password"}
                                         autoComplete="new-password"
                                         required
                                         value={newPassword}
                                         onChange={(e: ChangeEvent<HTMLInputElement>) => setNewPassword(e.target.value)}
                                         disabled={isLoading}
                                         className="block w-full pl-10 pr-10 py-2.5 border border-gray-700 rounded-xl shadow-sm placeholder:text-gray-500 focus:outline-none focus:ring-1 focus:ring-cyan-600 focus:border-cyan-600 text-white bg-[#070913] disabled:opacity-50" 
                                         placeholder="••••••••"
                                     />
                                     <button type="button" onClick={() => setShowPassword(!showPassword)} className="absolute inset-y-0 right-0 pr-3 flex items-center text-gray-400 hover:text-gray-300 disabled:opacity-50" disabled={isLoading}>
                                         {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                                     </button>
                                 </div>
                             </div>

                             {/* Confirm Password Input */}
                             <div className="space-y-1.5"> 
                                 <label htmlFor="confirmPassword" className="block text-sm font-medium text-gray-300">Confirm New Password</label>
                                  <div className="relative">
                                      <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none"> <Lock size={18} className="text-gray-400" /> </div>
                                     <input
                                         id="confirmPassword"
                                         name="confirmPassword"
                                         type={showConfirmPassword ? "text" : "password"}
                                         autoComplete="new-password"
                                         required
                                         value={confirmPassword}
                                         onChange={(e: ChangeEvent<HTMLInputElement>) => setConfirmPassword(e.target.value)}
                                         disabled={isLoading}
                                         className="block w-full pl-10 pr-10 py-2.5 border border-gray-700 rounded-xl shadow-sm placeholder:text-gray-500 focus:outline-none focus:ring-1 focus:ring-cyan-600 focus:border-cyan-600 text-white bg-[#070913] disabled:opacity-50"
                                         placeholder="••••••••"
                                     />
                                     <button type="button" onClick={() => setShowConfirmPassword(!showConfirmPassword)} className="absolute inset-y-0 right-0 pr-3 flex items-center text-gray-400 hover:text-gray-300 disabled:opacity-50" disabled={isLoading}>
                                         {showConfirmPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                                     </button>
                                 </div>
                             </div>

                             {/* Submit Button */}
                             <button type="submit" disabled={isLoading || !token} 
                                 className="w-full flex justify-center py-3 px-4 border border-transparent rounded-xl text-[#FFECAB] bg-cyan-800 hover:bg-cyan-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-cyan-500 font-medium transition-all duration-200 disabled:opacity-50 disabled:cursor-not-allowed mt-4"> 
                                 {isLoading ? 'Resetting...' : 'Reset Password'}
                             </button>
                         </form>
                     )}

                     {/* Link to Login if there was an error AND no token */}
                     {error && !token && (
                         <div className="text-center mt-6">
                             <Link to="/login" className="text-sm font-medium text-cyan-400 hover:text-cyan-600">
                                 Back to Login
                             </Link>
                         </div>
                     )}
                </div>
            </div>
        </div>
    );
};

export default ResetPasswordPage;