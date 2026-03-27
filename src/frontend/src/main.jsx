import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import 'highlight.js/styles/github.css';
import App from './App';
import './app.css';

const legacyTarget = legacyRedirectTarget(window.location);
if (legacyTarget && legacyTarget !== `${window.location.pathname}${window.location.search}`) {
  window.history.replaceState(null, '', legacyTarget);
}

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <BrowserRouter>
      <App />
    </BrowserRouter>
  </React.StrictMode>
);

function legacyRedirectTarget(location) {
  const { pathname, hash } = location;
  if (!pathname.startsWith('/app')) {
    return null;
  }
  const legacyPath = hash.startsWith('#') ? hash.slice(1) : pathname.slice('/app'.length) || '/';
  return normalizeLegacyPath(legacyPath);
}

function normalizeLegacyPath(path) {
  if (!path || path === '/app' || path === '/app/') {
    return '/';
  }
  if (path.startsWith('/app/#')) {
    return normalizeLegacyPath(path.slice('/app/#'.length));
  }
  if (path.startsWith('/app/')) {
    const cleanPath = path.slice('/app'.length);
    return cleanPath || '/';
  }
  return path.startsWith('/') ? path : `/${path}`;
}
