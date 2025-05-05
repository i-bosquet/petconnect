import { JSX } from 'react';

/**
 * Footer Component - Displays the standard application footer with copyright information.
 * Reusable across different pages and layouts.
 *
 * @param {object} props - Component properties.
 * @param {string} [props.className] - Optional additional CSS classes to apply to the footer element.
 * @returns {JSX.Element} The footer component.
 */
const Footer = ({ className = '' }: { className?: string }): JSX.Element => {
  return (
    <footer className={`py-6 text-center text-gray-500 ${className}`}>
      <p>© {new Date().getFullYear()} PetConnect. All rights reserved.</p> 
    </footer>
  );
};

export default Footer;