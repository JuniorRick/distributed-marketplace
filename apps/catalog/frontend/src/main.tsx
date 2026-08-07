import React from 'react';
import ReactDOM from 'react-dom/client';
import '@marketplace/ui/styles.css';
import './styles.css';
import App from './app/App';

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
);
