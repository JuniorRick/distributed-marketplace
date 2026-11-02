import React from 'react';
import ReactDOM from 'react-dom/client';
import '@marketplace/ui/styles.css';
import './styles.css';
import App from './app/App';
import { initializeAuth } from '@marketplace/auth';

const authenticationEnabled = import.meta.env.VITE_AUTH_ENABLED !== 'false';

if (authenticationEnabled) {
  try {
    await initializeAuth(false);
  } catch (error) {
    console.warn('Authentication is unavailable; continuing with the public catalog.', error);
  }
}

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode><App /></React.StrictMode>,
);
