/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import { useEffect } from "react";
import { useLocation } from "react-router-dom";
import { Toaster } from "./components/ui/sonner";
import AppFooter from "./components/layout/AppFooter";
import AuthenticatedHeader from "./components/layout/AuthenticatedHeader";
import LoginHeader from "./components/layout/LoginHeader";
import useJson from "./hooks/useJson";
import AppRoutes from "./AppRoutes";
import type { Session } from "./types/app";

function App() {
  const sessionState = useJson<Session>("/api/app/session");
  const session = sessionState.data;
  const location = useLocation();
  const isLoginRoute =
    location.pathname === "/login" && !session?.authenticated;
  const brandName = session?.installationCompanyName || "billetsys";

  useEffect(() => {
    document.title = `${brandName}: billetsys`;
  }, [brandName]);

  return (
    <>
      <div
        className={
          isLoginRoute
            ? "min-h-screen flex flex-col bg-header-bg text-header-text"
            : "min-h-screen flex flex-col bg-background"
        }
      >
        {isLoginRoute ? (
          <LoginHeader brandName={brandName} />
        ) : (
          <AuthenticatedHeader session={session} />
        )}
        <main
          className={
            isLoginRoute
              ? "flex-1 flex items-center justify-center p-6 bg-header-bg text-header-text"
              : "flex-1 p-5"
          }
        >
          <AppRoutes sessionState={sessionState} />
        </main>
        <AppFooter />
      </div>
      <Toaster position="bottom-right" richColors />
    </>
  );
}

export default App;
