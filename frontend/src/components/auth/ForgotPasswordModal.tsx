import { useState, JSX, FormEvent, ChangeEvent } from 'react';
import { Mail, X } from 'lucide-react';
import { requestPasswordReset } from '@/services/authService'; 

/**
 * Props for the ForgotPasswordModal component.
 */
interface ForgotPasswordModalProps {
  /** Controls whether the modal is visible. */
  isOpen: boolean;
  /** Callback function to close the modal. */
  onClose: () => void;
}

/**
 * ForgotPasswordModal Component - Provides a modal dialog for users
 * to request a password reset link by entering their email address.
 * Handles the API call and displays success or error messages.
 *
 * @param {ForgotPasswordModalProps} props - The component props.
 * @returns {JSX.Element | null} The modal component or null if not open.
 */
const ForgotPasswordModal = ({ isOpen, onClose }: ForgotPasswordModalProps): JSX.Element | null => {
  // --- State specific to this modal ---
  const [email, setEmail] = useState<string>('');
  const [message, setMessage] = useState<string>('');
  const [error, setError] = useState<string>('');
  const [isLoading, setIsLoading] = useState<boolean>(false);

  /**
   * Handles the submission of the forgot password email form within the modal.
   * Calls the backend API to request a password reset link.
   *
   * @param {FormEvent<HTMLFormElement>} e - The form submit event.
   */
  const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setError('');
    setMessage('');
    setIsLoading(true);

    try {
      await requestPasswordReset({ email: email });
      setMessage("If an account exists for this email, a password reset link has been sent.");
    } catch (err) {
      setError(err instanceof Error ? err.message : 'An unknown error occurred sending the reset link.');
    } finally {
      setIsLoading(false);
    }
  };

     // --- Render Logic ---

  if (!isOpen) {
    return null;
  }

  return (
    <div className="fixed inset-0 bg-black/80 flex items-center justify-center p-4 z-50 animate-fade-in backdrop-blur-sm">
      {/* Modal Content Box */}
      <div className="bg-[#0c1225] rounded-xl shadow-xl w-full max-w-md p-6 sm:p-8 border border-[#FFECAB]/40 relative">

        {/* Close Button */}
        <button
          onClick={onClose} // Use the onClose prop passed from the parent
          className="absolute top-3 right-3 text-gray-400 hover:text-[#FFECAB] p-1 rounded-full hover:bg-cyan-900/50 transition-colors"
          aria-label="Close modal"
        >
          <X size={20} />
        </button>

        {/* Modal Header */}
        <div className="mb-6 text-center">
          <h3 className="text-xl font-semibold text-[#FFECAB]">
            Reset Password
          </h3>
        </div>

        {/* Modal Body - Form */}
        <form onSubmit={handleSubmit} className="space-y-6">
          <p className="text-sm text-gray-300 text-center">
            Enter the email address associated with your account.
          </p>

          {/* Success/Error Message Area */}
          {message && !error && (
            <div className="p-3 bg-green-900/30 text-green-300 rounded-lg text-sm text-center">
              {message}
            </div>
          )}
          {error && (
            <div className="p-3 bg-red-900/30 text-red-300 rounded-lg text-sm text-center">
              {error}
            </div>
          )}

          {/* Email Input */}
          <div className="space-y-2">
            <label
              htmlFor="forgot-email-modal" // Use a unique ID for the modal input
              className="sr-only" // Hide label visually, but keep for accessibility
            >
              Email Address
            </label>
            <div className="relative">
              <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                <Mail size={18} className="text-gray-400" />
              </div>
              <input
                id="forgot-email-modal" 
                name="forgot-email"
                type="email"
                required
                autoComplete="email"
                value={email}
                onChange={(e: ChangeEvent<HTMLInputElement>) => setEmail(e.target.value)}
                className="block w-full pl-10 pr-3 py-3 border border-gray-700 rounded-xl shadow-sm placeholder:text-gray-400 focus:outline-none focus:ring-2 focus:ring-cyan-700 focus:border-cyan-500 text-white bg-gray-800 disabled:opacity-50"
                placeholder="your.email@example.com"
                disabled={isLoading}
              />
            </div>
          </div>

          {/* Submit Button */}
          <button
            type="submit"
            disabled={isLoading}
            className="w-full flex justify-center py-3 px-4 border border-transparent rounded-xl text-[#FFECAB] bg-cyan-800 hover:bg-cyan-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-offset-[#0c1225] focus:ring-cyan-500 font-medium transition-all duration-200 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {isLoading ? "Sending Link..." : "Send Reset Link"}
          </button>
        </form>
      </div>
    </div>
  );
};

export default ForgotPasswordModal;

