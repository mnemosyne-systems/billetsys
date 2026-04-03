/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import type { ReactNode } from "react";
import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useState,
} from "react";
import { useLocation, useNavigate } from "react-router-dom";

const DEFAULT_TOAST_DURATION_MS = 4500;

export type ToastVariant = "success" | "error" | "danger";

export interface ToastInput {
  variant: ToastVariant;
  message: string;
  durationMs?: number;
}

interface ToastRecord extends ToastInput {
  id: string;
}

interface ToastContextValue {
  showToast: (toast: ToastInput) => void;
  dismissToast: (id: string) => void;
}

interface ToastProviderProps {
  children: ReactNode;
}

const ToastContext = createContext<ToastContextValue | null>(null);

export function buildToastNavigationState(
  toast: ToastInput,
  state?: unknown,
): Record<string, unknown> {
  if (state && typeof state === "object" && !Array.isArray(state)) {
    return { ...(state as Record<string, unknown>), appToast: toast };
  }
  return { appToast: toast };
}

export function ToastProvider({ children }: ToastProviderProps) {
  const [toasts, setToasts] = useState<ToastRecord[]>([]);
  const location = useLocation();
  const navigate = useNavigate();

  const dismissToast = useCallback((id: string) => {
    setToasts((current) => current.filter((toast) => toast.id !== id));
  }, []);

  const showToast = useCallback((toast: ToastInput) => {
    setToasts((current) => [
      ...current,
      {
        id: `${Date.now()}-${Math.random().toString(16).slice(2)}`,
        durationMs: toast.durationMs ?? DEFAULT_TOAST_DURATION_MS,
        ...toast,
      },
    ]);
  }, []);

  useEffect(() => {
    const locationState = location.state;
    if (
      !locationState ||
      typeof locationState !== "object" ||
      Array.isArray(locationState)
    ) {
      return;
    }
    const state = locationState as Record<string, unknown>;
    const pendingToast = state.appToast;
    if (!isToastInput(pendingToast)) {
      return;
    }

    const timeoutId = window.setTimeout(() => {
      showToast(pendingToast);
    }, 0);
    const nextState = { ...state };
    delete nextState.appToast;
    navigate(`${location.pathname}${location.search}${location.hash}`, {
      replace: true,
      state: Object.keys(nextState).length > 0 ? nextState : null,
    });
    return () => window.clearTimeout(timeoutId);
  }, [
    location.hash,
    location.key,
    location.pathname,
    location.search,
    location.state,
    navigate,
    showToast,
  ]);

  return (
    <ToastContext.Provider value={{ showToast, dismissToast }}>
      {children}
      <div className="toast-viewport" aria-live="polite" aria-atomic="true">
        {toasts.map((toast) => (
          <ToastItem key={toast.id} toast={toast} onDismiss={dismissToast} />
        ))}
      </div>
    </ToastContext.Provider>
  );
}

export function useToast() {
  const context = useContext(ToastContext);
  if (!context) {
    throw new Error("useToast must be used within ToastProvider.");
  }
  return context;
}

interface ToastItemProps {
  toast: ToastRecord;
  onDismiss: (id: string) => void;
}

function ToastItem({ toast, onDismiss }: ToastItemProps) {
  useEffect(() => {
    const timeoutId = window.setTimeout(
      () => onDismiss(toast.id),
      toast.durationMs ?? DEFAULT_TOAST_DURATION_MS,
    );
    return () => window.clearTimeout(timeoutId);
  }, [onDismiss, toast.durationMs, toast.id]);

  return (
    <div className={`toast toast-${toast.variant}`} role="status">
      <div className="toast-copy">
        <strong>
          {toast.variant === "success"
            ? "Success"
            : toast.variant === "danger"
              ? "Deleted"
              : "Error"}
        </strong>
        <span>{toast.message}</span>
      </div>
      <button
        type="button"
        className="toast-dismiss"
        onClick={() => onDismiss(toast.id)}
        aria-label="Dismiss notification"
      >
        ×
      </button>
    </div>
  );
}

function isToastInput(value: unknown): value is ToastInput {
  if (!value || typeof value !== "object" || Array.isArray(value)) {
    return false;
  }
  const candidate = value as Partial<ToastInput>;
  return (
    (candidate.variant === "success" ||
      candidate.variant === "error" ||
      candidate.variant === "danger") &&
    typeof candidate.message === "string"
  );
}
