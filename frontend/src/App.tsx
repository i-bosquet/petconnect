import { JSX } from 'react';
import AppRouter from './routes/AppRouter'; 
import { TooltipProvider } from "@/components/ui/tooltip";


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
    </TooltipProvider>
  );
}

export default App;