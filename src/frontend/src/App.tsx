/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import { useEffect, useMemo } from "react";
import type { CSSProperties } from "react";
import { useLocation } from "react-router-dom";
import { Toaster } from "./components/ui/sonner";
import AppFooter from "./components/layout/AppFooter";
import AuthenticatedHeader from "./components/layout/AuthenticatedHeader";
import LoginHeader from "./components/layout/LoginHeader";
import SessionInactivityManager from "./components/layout/SessionInactivityManager";
import useJson from "./hooks/useJson";
import AppRoutes from "./AppRoutes";
import type { Session } from "./types/app";
import {
  pickInstallationBranding,
  readCachedInstallationBranding,
  writeCachedInstallationBranding,
} from "./utils/installationBranding";

const DEFAULT_INSTALLATION_COLOR = "#b00020";

function normalizeInstallationColor(color?: string) {
  return /^#[0-9a-fA-F]{6}$/.test(color || "")
    ? String(color).toLowerCase()
    : DEFAULT_INSTALLATION_COLOR;
}

function contrastTextColor(color: string) {
  const red = Number.parseInt(color.slice(1, 3), 16);
  const green = Number.parseInt(color.slice(3, 5), 16);
  const blue = Number.parseInt(color.slice(5, 7), 16);
  const brightness = (red * 299 + green * 587 + blue * 114) / 1000;
  return brightness >= 186 ? "#111827" : "#ffffff";
}

function darkenColor(color: string, amount = 0.16) {
  const channel = (offset: number) =>
    Math.max(
      0,
      Math.min(
        255,
        Math.round(
          Number.parseInt(color.slice(offset, offset + 2), 16) * (1 - amount),
        ),
      ),
    )
      .toString(16)
      .padStart(2, "0");
  return `#${channel(1)}${channel(3)}${channel(5)}`;
}

function brandingStyle(
  headerFooterColor?: string,
  headersColor?: string,
  buttonsColor?: string,
): CSSProperties {
  const headerBg = normalizeInstallationColor(headerFooterColor);
  const headerText = contrastTextColor(headerBg);
  const sectionHeaderColor = normalizeInstallationColor(headersColor);
  const sectionHeaderHover = darkenColor(sectionHeaderColor);
  const buttonBg = normalizeInstallationColor(buttonsColor);
  const buttonText = contrastTextColor(buttonBg);
  const buttonHover = darkenColor(buttonBg);
  return {
    "--header-bg": headerBg,
    "--header-text": headerText,
    "--primary-dark": sectionHeaderHover,
    "--section-header-color": sectionHeaderColor,
    "--section-header-hover": sectionHeaderHover,
    "--button-bg": buttonBg,
    "--button-text": buttonText,
    "--button-hover": buttonHover,
    "--color-header-bg": sectionHeaderColor,
    "--color-header-text": headerText,
    "--color-primary-dark": sectionHeaderHover,
    "--color-section-header": sectionHeaderColor,
    "--color-section-header-hover": sectionHeaderHover,
    "--color-buttons-bg": buttonBg,
    "--color-buttons-text": buttonText,
    "--color-buttons-hover": buttonHover,
  } as CSSProperties;
}

function loginBackgroundStyle(
  backgroundBase64?: string,
): CSSProperties | undefined {
  if (!backgroundBase64) {
    return undefined;
  }
  return {
    backgroundImage: `linear-gradient(rgba(0, 0, 0, 0.2), rgba(0, 0, 0, 0.2)), url(${backgroundBase64})`,
    backgroundPosition: "center",
    backgroundRepeat: "no-repeat",
    backgroundSize: "cover",
  };
}

function App() {
  const sessionState = useJson<Session>("/api/app/session");
  const session = sessionState.data;
  const cachedBranding = useMemo(() => readCachedInstallationBranding(), []);
  const location = useLocation();
  const branding = {
    ...cachedBranding,
    ...pickInstallationBranding(session),
  };
  const shellSession: Session | null = session
    ? { ...session, ...branding }
    : branding;
  const isLoginRoute =
    ["/login", "/forgot-password", "/reset-password"].includes(
      location.pathname,
    ) && !session?.authenticated;
  const brandName = branding.installationCompanyName || "billetsys";

  useEffect(() => {
    if (!session) {
      return;
    }
    writeCachedInstallationBranding(pickInstallationBranding(session));
  }, [session]);

  useEffect(() => {
    document.title = `${brandName}: billetsys`;
  }, [brandName]);

  return (
    <>
      <div
        style={brandingStyle(
          branding.installationHeaderFooterColor,
          branding.installationHeadersColor,
          branding.installationButtonsColor,
        )}
        className={
          isLoginRoute
            ? "min-h-screen flex flex-col"
            : "min-h-screen flex flex-col bg-background dark:bg-black"
        }
      >
        {!isLoginRoute && <AuthenticatedHeader session={shellSession} />}

        {isLoginRoute ? (
          <main
            className="flex-1 flex flex-col relative"
            style={{
              ...loginBackgroundStyle(branding.installationBackgroundBase64),
              backgroundColor: branding.installationBackgroundBase64
                ? undefined
                : "var(--header-bg)",
            }}
          >
            <div className="flex items-center justify-end p-4 absolute top-0 right-0 w-full z-10">
              <LoginHeader />
            </div>
            <div className="flex-1 flex items-center justify-center p-6 w-full relative z-0">
              <AppRoutes sessionState={sessionState} />
            </div>
          </main>
        ) : (
          <main className="flex-1 p-5 bg-background dark:bg-black">
            <AppRoutes sessionState={sessionState} />
          </main>
        )}
        <AppFooter />
      </div>
      <SessionInactivityManager session={session} />
      <Toaster position="bottom-right" richColors />
    </>
  );
}

export default App;
