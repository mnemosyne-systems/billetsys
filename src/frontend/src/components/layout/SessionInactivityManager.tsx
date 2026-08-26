/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import { useCallback, useEffect, useRef, useState } from "react";
import type { Session } from "../../types/app";
import { useAuth } from "../../auth/useAuth";
import { Button } from "../ui/button";
import {
  AlertDialog,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "../ui/alert-dialog";

interface SessionInactivityManagerProps {
  session?: Session | null;
}

const DEFAULT_TIMEOUT_SECONDS = 60 * 60;
const DEFAULT_WARNING_SECONDS = 5 * 60;
const KEEP_ALIVE_INTERVAL_MS = 60 * 1000;

function formatRemainingTime(totalSeconds: number): string {
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;
  return `${minutes}:${String(seconds).padStart(2, "0")}`;
}

export default function SessionInactivityManager({
  session,
}: SessionInactivityManagerProps) {
  const { keycloak } = useAuth();
  const timeoutSeconds =
    session?.inactivityTimeoutSeconds || DEFAULT_TIMEOUT_SECONDS;
  const warningSeconds =
    session?.inactivityWarningSeconds || DEFAULT_WARNING_SECONDS;
  const timeoutMs = timeoutSeconds * 1000;
  const warningMs = warningSeconds * 1000;
  const [warningOpen, setWarningOpen] = useState(false);
  const [secondsRemaining, setSecondsRemaining] = useState(timeoutSeconds);
  const lastActivityAtRef = useRef(Date.now());
  const lastKeepAliveAtRef = useRef(0);
  const keepAliveInFlightRef = useRef(false);
  const timeoutTriggeredRef = useRef(false);

  const expireSession = useCallback(async () => {
    if (timeoutTriggeredRef.current) {
      return;
    }
    timeoutTriggeredRef.current = true;
    setWarningOpen(false);
    keycloak.logout({ redirectUri: window.location.origin });
  }, [keycloak]);

  const keepAlive = useCallback(
    async (force = false) => {
      if (
        !session?.authenticated ||
        timeoutTriggeredRef.current ||
        keepAliveInFlightRef.current
      ) {
        return;
      }
      const now = Date.now();
      if (!force && now - lastKeepAliveAtRef.current < KEEP_ALIVE_INTERVAL_MS) {
        return;
      }
      keepAliveInFlightRef.current = true;
      try {
        const response = await fetch("/api/app/session", {
          credentials: "same-origin",
          cache: "no-store",
        });
        const payload = (await response.json()) as { authenticated?: boolean };
        if (response.status === 401 || !payload.authenticated) {
          await expireSession();
          return;
        }
        lastKeepAliveAtRef.current = now;
      } finally {
        keepAliveInFlightRef.current = false;
      }
    },
    [expireSession, session?.authenticated],
  );

  const recordActivity = useCallback(
    (forceKeepAlive = false) => {
      if (!session?.authenticated || timeoutTriggeredRef.current) {
        return;
      }
      lastActivityAtRef.current = Date.now();
      setWarningOpen(false);
      setSecondsRemaining(timeoutSeconds);
      if (
        forceKeepAlive ||
        Date.now() - lastKeepAliveAtRef.current >= KEEP_ALIVE_INTERVAL_MS
      ) {
        void keepAlive(forceKeepAlive);
      }
    },
    [keepAlive, session?.authenticated, timeoutSeconds],
  );

  useEffect(() => {
    if (!session?.authenticated) {
      timeoutTriggeredRef.current = false;
      setWarningOpen(false);
      return;
    }
    timeoutTriggeredRef.current = false;
    lastActivityAtRef.current = Date.now();
    lastKeepAliveAtRef.current = 0;
    setSecondsRemaining(timeoutSeconds);
    setWarningOpen(false);
    void keepAlive(true);
  }, [keepAlive, session?.authenticated, timeoutSeconds]);

  useEffect(() => {
    if (!session?.authenticated) {
      return undefined;
    }

    const handleActivity = () => {
      recordActivity(false);
    };
    const handleVisibilityChange = () => {
      if (document.visibilityState === "visible") {
        recordActivity(false);
      }
    };
    const intervalId = window.setInterval(() => {
      const remainingMs = timeoutMs - (Date.now() - lastActivityAtRef.current);
      if (remainingMs <= 0) {
        void expireSession();
        return;
      }
      setSecondsRemaining(Math.ceil(remainingMs / 1000));
      setWarningOpen(remainingMs <= warningMs);
    }, 1000);

    window.addEventListener("pointerdown", handleActivity, { passive: true });
    window.addEventListener("keydown", handleActivity);
    window.addEventListener("scroll", handleActivity, { passive: true });
    window.addEventListener("focus", handleActivity);
    document.addEventListener("visibilitychange", handleVisibilityChange);

    return () => {
      window.clearInterval(intervalId);
      window.removeEventListener("pointerdown", handleActivity);
      window.removeEventListener("keydown", handleActivity);
      window.removeEventListener("scroll", handleActivity);
      window.removeEventListener("focus", handleActivity);
      document.removeEventListener("visibilitychange", handleVisibilityChange);
    };
  }, [
    expireSession,
    recordActivity,
    session?.authenticated,
    timeoutMs,
    warningMs,
  ]);

  if (!session?.authenticated) {
    return null;
  }

  return (
    <AlertDialog open={warningOpen}>
      <AlertDialogContent size="sm">
        <AlertDialogHeader>
          <AlertDialogTitle>Session expiring soon</AlertDialogTitle>
          <AlertDialogDescription>
            Your session will expire after 1 hour of inactivity. You will be
            signed out in {formatRemainingTime(secondsRemaining)} unless you
            stay active.
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <Button
            type="button"
            variant="outline"
            onClick={() => void expireSession()}
          >
            Sign out now
          </Button>
          <Button type="button" onClick={() => recordActivity(true)}>
            Stay signed in
          </Button>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}
