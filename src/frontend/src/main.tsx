/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import React from "react";
import ReactDOM from "react-dom/client";
import { BrowserRouter } from "react-router-dom";
import "highlight.js/styles/github.css";
import App from "./App";
import "./app.css";

const rootElement = document.getElementById("root");

if (!rootElement) {
  throw new Error("Root element not found.");
}

const legacyTarget = legacyRedirectTarget(window.location);
if (
  legacyTarget &&
  legacyTarget !== `${window.location.pathname}${window.location.search}`
) {
  window.history.replaceState(null, "", legacyTarget);
}

ReactDOM.createRoot(rootElement).render(
  <React.StrictMode>
    <BrowserRouter>
      <App />
    </BrowserRouter>
  </React.StrictMode>,
);

function legacyRedirectTarget(location: Location): string | null {
  const { pathname, hash } = location;
  if (!pathname.startsWith("/app")) {
    return null;
  }
  const legacyPath = hash.startsWith("#")
    ? hash.slice(1)
    : pathname.slice("/app".length) || "/";
  return normalizeLegacyPath(legacyPath);
}

function normalizeLegacyPath(path: string): string {
  if (!path || path === "/app" || path === "/app/") {
    return "/";
  }
  if (path.startsWith("/app/#")) {
    return normalizeLegacyPath(path.slice("/app/#".length));
  }
  if (path.startsWith("/app/")) {
    const cleanPath = path.slice("/app".length);
    return cleanPath || "/";
  }
  return path.startsWith("/") ? path : `/${path}`;
}
