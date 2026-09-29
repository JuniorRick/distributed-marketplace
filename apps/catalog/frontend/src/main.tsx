import React from 'react';
import ReactDOM from 'react-dom/client';
import '@marketplace/ui/styles.css';
import './styles.css';
import App from './app/App';
import { initializeAuth } from '@marketplace/auth';

await initializeAuth(false);

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode><App /></React.StrictMode>,
);
