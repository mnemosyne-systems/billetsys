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
import { toast } from "sonner";
import DataState from "../components/common/DataState";
import PageHeader from "../components/layout/PageHeader";
import type { SessionPageProps } from "../types/app";
import { Button } from "../components/ui/button";
import { Field, FieldLabel } from "../components/ui/field";
import { Input } from "../components/ui/input";

export default function PasswordPage({ sessionState }: SessionPageProps) {
  const location = useLocation();

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
      toast.error(routeError);
    }
  }, [routeError]);

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
      toast.success("Password updated successfully.");
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
    <section className="w-full mt-4">
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
      >
        <PageHeader title="Change password" />
        <form className="space-y-6" onSubmit={submit}>
          <Field>
            <FieldLabel>Old password</FieldLabel>
            <Input
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
          </Field>
          <Field>
            <FieldLabel>New password</FieldLabel>
            <Input
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
          </Field>
          <Field>
            <FieldLabel>Confirm password</FieldLabel>
            <Input
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
          </Field>

          <div className="pt-2 flex justify-end">
            <Button type="submit" disabled={saveState.saving}>
              {saveState.saving ? "Saving..." : "Update"}
            </Button>
          </div>
        </form>
      </DataState>
    </section>
  );
}
