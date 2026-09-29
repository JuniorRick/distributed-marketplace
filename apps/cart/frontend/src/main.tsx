import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import '@marketplace/ui/styles.css';
import './styles.css';
import App from './app/App';
import { initializeAuth } from '@marketplace/auth';

await initializeAuth(true);
createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
);
