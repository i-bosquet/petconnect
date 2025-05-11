import { JSX } from 'react';
import AppRouter from './routes/AppRouter'; 
import { TooltipProvider } from "@/components/ui/tooltip";
import { Toaster } from "@/components/ui/sonner";



/**
 * App - The root component of the application.
 * Sets up global providers and renders the main router.
 *
 * @returns {JSX.Element} The main application structure.
 */
function App(): JSX.Element {
  return (
   <TooltipProvider> 
          <AppRouter />
          <Toaster richColors position="top-right" /> 
    </TooltipProvider>
  );
}

export default App;