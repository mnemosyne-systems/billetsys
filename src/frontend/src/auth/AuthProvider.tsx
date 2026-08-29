/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import React, { useEffect, useState } from "react";
import { AuthContext } from "./AuthContext";
import keycloak from "./keycloak";

let initPromise: Promise<boolean> | null = null;

const getInitPromise = () => {
  if (!initPromise) {
    initPromise = keycloak.init({
      onLoad: "check-sso",
      checkLoginIframe: false,
    });
  }
  return initPromise;
};

export const AuthProvider = ({ children }: { children: React.ReactNode }) => {
  const [authenticated, setAuthenticated] = useState(false);
  const [initialized, setInitialized] = useState(false);

  useEffect(() => {
    let active = true;
    let intervalId: ReturnType<typeof setInterval> | undefined;

    const initKeycloak = async () => {
      // Check parameters before Keycloak init clears them
      const hash = window.location.hash || "";
      const search = window.location.search || "";
      if (
        (hash.includes("kc_action=UPDATE_PASSWORD") &&
          hash.includes("kc_action_status=success")) ||
        (search.includes("kc_action=UPDATE_PASSWORD") &&
          search.includes("kc_action_status=success"))
      ) {
        sessionStorage.setItem("password_update_success", "true");
      }

      try {
        const isAuth = await getInitPromise();

        if (!active) return;

        setAuthenticated(isAuth);
        setInitialized(true);

        if (isAuth) {
          // Token refresh loop every 15s
          intervalId = setInterval(() => {
            keycloak
              .updateToken(30)
              .then((refreshed) => {
                if (refreshed) {
                  console.log("Token refreshed");
                }
              })
              .catch(() => {
                console.error("Failed to refresh token");
                keycloak.logout();
              });
          }, 15000);
        }
      } catch (err) {
        console.error("Keycloak initialization failed:", err);
        if (active) {
          setInitialized(true);
        }
      }
    };

    initKeycloak();

    return () => {
      active = false;
      if (intervalId) {
        clearInterval(intervalId);
      }
    };
  }, []);

  // Globally patch fetch once Keycloak is configured
  useEffect(() => {
    const originalFetch = window.fetch;
    window.fetch = async (input, init) => {
      let requestInit = init || {};
      const url =
        typeof input === "string"
          ? input
          : input instanceof Request
            ? input.url
            : "";

      // Only attach Authorization header to local API endpoints (/api/*)
      const isLocalApi =
        url.startsWith("/api/") ||
        url.includes(window.location.origin + "/api/");

      if (isLocalApi && keycloak.token) {
        try {
          await keycloak.updateToken(30);
        } catch (err) {
          console.error("Token refresh failed during fetch intercept", err);
        }

        const headers = new Headers(requestInit.headers);
        headers.set("Authorization", `Bearer ${keycloak.token}`);
        requestInit = {
          ...requestInit,
          headers,
        };
      }

      return originalFetch(input, requestInit);
    };

    return () => {
      window.fetch = originalFetch;
    };
  }, []);

  if (!initialized) {
    return (
      <div className="flex items-center justify-center min-h-screen bg-background">
        <div className="text-center space-y-4">
          <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-primary mx-auto"></div>
          <p className="text-muted-foreground">
            Initializing secure session...
          </p>
        </div>
      </div>
    );
  }

  return (
    <AuthContext.Provider value={{ keycloak, authenticated, initialized }}>
      {children}
    </AuthContext.Provider>
  );
};
export default AuthProvider;
