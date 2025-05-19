import { useState, JSX, FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Eye, EyeOff, Mail, Lock, User, Phone } from 'lucide-react'; 
import { registerOwner } from '@/services/authService';

/**
 * SignupPage Component - Handles new Owner user registration.
 * Provides a form for users to create an Owner account, validates input,
 * calls the backend API, and handles success or error responses.
 *
 * @returns {JSX.Element} The signup page component.
 */
const SignupPage = (): JSX.Element => {
    const [username, setUsername] = useState<string>('');
    const [email, setEmail] = useState<string>('');
    const [phone, setPhone] = useState<string>(''); 
    const [password, setPassword] = useState<string>('');
    const [confirmPassword, setConfirmPassword] = useState<string>('');
    const [showPassword, setShowPassword] = useState<boolean>(false);
    const [showConfirmPassword, setShowConfirmPassword] = useState<boolean>(false);
    const [error, setError] = useState<string>('');
    const [successMessage, setSuccessMessage] = useState<string>(''); 
    const [isLoading, setIsLoading] = useState<boolean>(false);
    const navigate = useNavigate();

    /**
     * Client-side validation logic before submitting to the API.
     * Checks for empty fields, password match, and minimum password length.
     * Note: Email format and uniqueness are primarily validated by the backend.
     *
     * @returns {boolean} - True if basic client-side validation passes, false otherwise.
     */
    const validateForm = (): boolean => {
        if (!username || !email || !phone || !password || !confirmPassword) {
            setError("All fields are required.");
            return false;
        }
        if (password !== confirmPassword) {
            setError("Passwords do not match.");
            return false;
        }
        if (password.length < 8) {
            setError("Password must be at least 8 characters long.");
            return false;
        }
        return true;
    };

    /**
     * Handles the form submission for registration.
     * Performs client-side validation, calls the backend registration API,
     * shows success/error messages, and redirects to login on success.
     *
     * @param {FormEvent<HTMLFormElement>} e - The form submit event.
     */
    const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        setError('');
        setSuccessMessage('');

        // Perform client-side validation first
        if (!validateForm()) {
            return;
        }

        setIsLoading(true);

        try {
            const registrationData = { username, email, password, phone };
            const createdOwnerProfile = await registerOwner(registrationData);

            console.log('Registration successful:', createdOwnerProfile);
            setSuccessMessage(`Account created successfully for ${createdOwnerProfile.username}! Redirecting to login...`);

            // Redirect to login page after a short delay to show the success message
            setTimeout(() => {
                navigate('/login');
            }, 2000); 

        } catch (err) {
            console.error('Registration handleSubmit error:', err);
            setError(err instanceof Error ? err.message : 'An unknown registration error occurred.');
            setIsLoading(false); 
        }
    };

    // --- JSX Rendering ---
    return (
      <div className="min-h-screen flex items-center justify-center bg-[#090D1A] p-4">
        <div className="w-full max-w-6xl flex flex-col lg:flex-row lg:items-center lg:justify-between">
          {/* Left Branding Section */}
          <div className="text-center lg:text-left mb-8 lg:mb-0 lg:flex-1 lg:pr-12">
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
              <p className="text-gray-300">
                Join our community of pet owners and veterinarians.
                <br />
                Create your account and start managing the pet&apos;s health
                journey.
              </p>
              <ul className="text-gray-300 space-y-2">
                <li className="flex items-center">
                  <span className="text-[#FFECAB] mr-2">✓</span> Access
                  veterinary records anytime
                </li>
                <li className="flex items-center">
                  <span className="text-[#FFECAB] mr-2">✓</span> Schedule
                  appointments online
                </li>
                <li className="flex items-center">
                  <span className="text-[#FFECAB] mr-2">✓</span> Receive health
                  reminders and notifications
                </li>
              </ul>
            </div>
          </div>

          {/* Right Registration Form Section */}
          <div className="lg:flex-1 w-full max-w-lg mx-auto lg:mx-0">
            <div className="rounded-2xl shadow-xl p-8 border-2 border-[#FFECAB] transform transition-all hover:scale-[1.01]">
              <h2 className="text-2xl font-semibold text-[#FFECAB] mb-6">
                Create Account
              </h2>

              {/* Error message display */}
              {error && (
                <div className="mb-4 p-3 bg-red-900/30 text-red-300 rounded-lg text-sm">
                  {error}
                </div>
              )}

              {/* Success Message */}
              {successMessage && (
                <div className="mb-4 p-3 bg-green-900/30 text-green-300 rounded-lg text-sm">
                  {successMessage}
                </div>
              )}

              {/* Registration Form */}
              <form onSubmit={handleSubmit}>
                <div className="space-y-5 lg:space-y-6">

                    {/* Row 1: Username and Email */}
                    <div className="grid grid-cols-1 lg:grid-cols-2 gap-5">
                        {/* Username Input */}
                        <div className="space-y-2">
                            <label htmlFor="username" className="block text-sm font-medium text-gray-300">Username</label>
                            <div className="relative">
                            <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                                <User size={18} className="text-gray-400" />
                            </div>            
                            <input
                            id="username"
                            name="username"
                            type="text"
                            autoComplete="name"
                            value={username}
                            onChange={(e) => setUsername(e.target.value)}
                            disabled={isLoading}
                            placeholder="Your username"
                            className="block w-full pl-10 pr-3 py-3 border border-gray-700 rounded-xl shadow-sm placeholder:text-gray-400 focus:outline-none focus:ring-2 focus:ring-cyan-700 focus:border-cyan-500 text-white bg-gray-800"
                            required
                            />
                        </div>
                        </div>
                        {/* Email Input */}
                        <div className="space-y-2">
                        <label htmlFor="email" className="block text-sm font-medium text-gray-300">Email address</label>
                            <div className="relative">
                                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                                    <Mail size={18} className="text-gray-400" />
                                </div>
                                <input
                                    id="email"
                                    name="email"
                                    type="email"
                                    autoComplete="email"
                                    value={email}
                                    onChange={(e) => setEmail(e.target.value)}
                                    disabled={isLoading}
                                    placeholder="name@email.com"
                                    className="block w-full pl-10 pr-3 py-3 border border-gray-700 rounded-xl shadow-sm placeholder:text-gray-400 focus:outline-none focus:ring-2 focus:ring-cyan-700 focus:border-cyan-500 text-white bg-gray-800"
                                    required
                                    />
                                </div>
                        </div>
                    </div>

                    {/* Row 2: Phone */}
                    <div className="grid grid-cols-1 lg:grid-cols-2 gap-5">
                        <div className="space-y-2">
                            <label htmlFor="phone" className="block text-sm font-medium text-gray-300">Phone Number</label>
                            <div className="relative">
                                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                                    <Phone size={18} className="text-gray-400" />
                                </div>
                            <input
                                id="phone"
                                name="phone"
                                type="tel"
                                autoComplete="tel"
                                value={phone}
                                onChange={(e) => setPhone(e.target.value)}
                                className="block w-full pl-10 pr-3 py-3 border border-gray-700 rounded-xl shadow-sm placeholder:text-gray-400 focus:outline-none focus:ring-2 focus:ring-cyan-700 focus:border-cyan-500 text-white bg-gray-800"
                                placeholder="Your phone number"
                                disabled={isLoading}
                                required
                            />
                            </div>
                        </div>
                    </div>

                    {/* Row 3: Password and Confirm Password */}
                    <div className="grid grid-cols-1 lg:grid-cols-2 gap-5">
                         {/* Password */}
                        <div className="space-y-2">
                            <label htmlFor="password" className="block text-sm font-medium text-gray-300">Password</label>
                            <div className="relative">
                                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                                    <Lock size={18} className="text-gray-400" />
                                </div>
                                <input
                                    id="password"
                                    name="password"
                                    type={showPassword ? "text" : "password"}
                                    autoComplete='new-password'
                                    value={password}
                                    onChange={(e) => setPassword(e.target.value)}
                                    disabled={isLoading}
                                    className="block w-full pl-10 pr-10 py-3 border border-gray-700 rounded-xl shadow-sm placeholder:text-gray-400 focus:outline-none focus:ring-2 focus:ring-cyan-700 focus:border-cyan-500 text-white bg-gray-800"
                                    placeholder="••••••••"
                                    required
                                />
                                <div className="absolute inset-y-0 right-0 pr-3 flex items-center">
                                    <button
                                    type="button"
                                    onClick={() => setShowPassword(!showPassword)}
                                    className="text-gray-400 hover:text-gray-300"
                                    >
                                    {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                                    </button>
                                </div>
                            </div>
                        </div>
                        {/* Confirm Password Input */}
                        <div className="space-y-2">
                            <label htmlFor="confirmPassword" className="block text-sm font-medium text-gray-300">Confirm Password</label>
                            <div className="relative">
                                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                                    <Lock size={18} className="text-gray-400" />
                                </div>
                                <input
                                    id="confirmPassword"
                                    name="confirmPassword"
                                    type={showConfirmPassword ? "text" : "password"}
                                    autoComplete="new-password"
                                    required
                                    value={confirmPassword}
                                    onChange={(e) => setConfirmPassword(e.target.value)}
                                    className="block w-full pl-10 pr-10 py-3 border border-gray-700 rounded-xl shadow-sm placeholder:text-gray-400 focus:outline-none focus:ring-2 focus:ring-cyan-700 focus:border-cyan-500 text-white bg-gray-800"
                                    placeholder="••••••••"
                                />
                                <div className="absolute inset-y-0 right-0 pr-3 flex items-center">
                                    <button
                                    type="button"
                                    onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                                    className="text-gray-400 hover:text-gray-300"
                                    >
                                    {showConfirmPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                                    </button>
                                </div>
                            </div>
                        </div>
                    </div>

                    {/* Register button */}
                    <button
                        type="submit"
                        disabled={isLoading}
                        className="w-full py-3 px-4 border border-transparent rounded-xl shadow-sm text-sm font-medium text-[#FFECAB] bg-cyan-800 hover:bg-cyan-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-cyan-500 mt-4 cursor-pointer">
                        {isLoading ? "Creating Account..." : "Sign up"}
                    </button>
                </div>
              </form>

              {/* Link to login */}
              <p className="mt-6 text-center text-gray-300">
                Already have an account?{" "}
                <Link to="/login" className="text-cyan-400 hover:text-cyan-500">
                  Log in
                </Link>
              </p>

            </div>
          </div>
          
        </div>
      </div>
    );
};

export default SignupPage;