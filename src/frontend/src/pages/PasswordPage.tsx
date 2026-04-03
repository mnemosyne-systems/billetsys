/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import type { FormEvent } from "react";
import { useEffect, useState } from "react";
import { useLocation } from "react-router-dom";
import { useToast } from "../components/common/ToastProvider";
import DataState from "../components/common/DataState";
import type { SessionPageProps } from "../types/app";

export default function PasswordPage({ sessionState }: SessionPageProps) {
  const location = useLocation();
  const { showToast } = useToast();
  const [formState, setFormState] = useState({
    oldPassword: "",
    newPassword: "",
    confirmPassword: "",
  });
  const [saveState, setSaveState] = useState({
    saving: false,
    error: "",
    saved: false,
  });
  const routeError = new URLSearchParams(location.search).get("error") || "";

  useEffect(() => {
    if (routeError) {
      showToast({ variant: "error", message: routeError });
    }
  }, [routeError, showToast]);

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setSaveState({ saving: true, error: "", saved: false });
    try {
      const response = await fetch("/api/profile/password", {
        method: "POST",
        credentials: "same-origin",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(formState),
      });
      if (!response.ok) {
        throw new Error(
          (await response.text()) || "Unable to update password.",
        );
      }
      setFormState({ oldPassword: "", newPassword: "", confirmPassword: "" });
      setSaveState({ saving: false, error: "", saved: true });
      showToast({
        variant: "success",
        message: "Password updated successfully.",
      });
    } catch (error: unknown) {
      setSaveState({
        saving: false,
        error:
          error instanceof Error ? error.message : "Unable to update password.",
        saved: false,
      });
    }
  };

  return (
    <section className="panel auth-panel">
      <DataState
        state={{
          loading: false,
          unauthorized: !sessionState.data?.authenticated,
          forbidden: false,
          error: "",
          empty: false,
          data: null,
        }}
        emptyMessage=""
        signInHref="/login"
      >
        <form className="auth-form" onSubmit={submit}>
          <label>
            Old password
            <input
              type="password"
              value={formState.oldPassword}
              onChange={(event) =>
                setFormState((current) => ({
                  ...current,
                  oldPassword: event.target.value,
                }))
              }
              required
            />
          </label>
          <label>
            New password
            <input
              type="password"
              value={formState.newPassword}
              onChange={(event) =>
                setFormState((current) => ({
                  ...current,
                  newPassword: event.target.value,
                }))
              }
              required
            />
          </label>
          <label>
            Confirm password
            <input
              type="password"
              value={formState.confirmPassword}
              onChange={(event) =>
                setFormState((current) => ({
                  ...current,
                  confirmPassword: event.target.value,
                }))
              }
              required
            />
          </label>
          <div className="button-row">
            <button
              type="submit"
              className="primary-button"
              disabled={saveState.saving}
            >
              {saveState.saving ? "Saving..." : "Update"}
            </button>
          </div>
        </form>
      </DataState>
    </section>
  );
}
