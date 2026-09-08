/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import { useEffect, useState, type FormEvent } from "react";
import { toast } from "sonner";

import {
  AvailabilityBadge,
  SCOPE_LABELS,
} from "@/availability/AvailabilityBadge";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Field, FieldError, FieldLabel } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Spinner } from "@/components/ui/spinner";
import useSubmissionGuard from "@/hooks/useSubmissionGuard";
import type { Availability, AvailabilityScope } from "@/types/domain";

interface AvailabilityDialogProps {
  open: boolean;
  scope: AvailabilityScope;
  entry?: Availability | null;
  initialDate?: string;
  canManageCompany?: boolean;
  onClose: () => void;
  onSaved: () => void;
}

interface AvailabilityFormState {
  startDate: string;
  endDate: string;
  reason: string;
}

function AvailabilityDialog({
  open,
  scope,
  entry,
  initialDate,
  canManageCompany = false,
  onClose,
  onSaved,
}: AvailabilityDialogProps) {
  const guard = useSubmissionGuard();
  const isEdit = Boolean(entry);

  const [formState, setFormState] = useState<AvailabilityFormState>({
    startDate: "",
    endDate: "",
    reason: "",
  });
  const [createScope, setCreateScope] = useState<AvailabilityScope>(
    canManageCompany ? scope : "PERSONAL",
  );
  const [saveState, setSaveState] = useState({ saving: false, error: "" });

  useEffect(() => {
    if (!open) {
      return;
    }
    setFormState({
      startDate: entry?.startDate ?? initialDate ?? "",
      endDate: entry?.endDate ?? initialDate ?? "",
      reason: entry?.reason ?? "",
    });
    setCreateScope(canManageCompany ? scope : "PERSONAL");
    setSaveState({ saving: false, error: "" });
  }, [open, entry, initialDate, scope, canManageCompany]);

  const updateFormState = (
    field: keyof AvailabilityFormState,
    value: string,
  ) => {
    setFormState((current) => ({ ...current, [field]: value }));
  };

  const submit = async (event: FormEvent) => {
    event.preventDefault();

    if (!formState.startDate || !formState.endDate) {
      setSaveState({
        saving: false,
        error: "Start date and end date are required.",
      });
      return;
    }

    if (formState.endDate < formState.startDate) {
      setSaveState({
        saving: false,
        error: "End date cannot be before start date.",
      });
      return;
    }

    if (!guard.tryEnter()) {
      return;
    }

    setSaveState({ saving: true, error: "" });

    const url = isEdit
      ? `/api/availability/${entry?.id}`
      : canManageCompany && createScope === "COMPANY"
        ? "/api/availability/company"
        : "/api/availability";

    try {
      const response = await fetch(url, {
        method: isEdit ? "PUT" : "POST",
        credentials: "same-origin",
        cache: "no-store",
        headers: {
          "Content-Type": "application/json",
          "X-Billetsys-Client": "react",
        },
        body: JSON.stringify({
          startDate: formState.startDate,
          endDate: formState.endDate,
          reason: formState.reason.trim() || null,
        }),
      });

      if (response.status === 401) {
        throw new Error("You need to sign in again.");
      }
      if (response.status === 403) {
        throw new Error("You do not have access to this action.");
      }
      if (!response.ok) {
        throw new Error(
          toErrorMessage(await response.text(), "Unable to save availability."),
        );
      }

      toast.success(isEdit ? "Availability updated." : "Availability created.");
      onSaved();
      onClose();
    } catch (error) {
      setSaveState({
        saving: false,
        error:
          error instanceof Error
            ? error.message
            : "Unable to save availability.",
      });
    } finally {
      guard.exit();
    }
  };

  return (
    <Dialog
      open={open}
      onOpenChange={(next) => {
        if (!next && !saveState.saving) {
          onClose();
        }
      }}
    >
      <DialogContent>
        <DialogHeader>
          <DialogTitle>
            {isEdit ? "Edit availability" : "Add availability"}
          </DialogTitle>
          <DialogDescription>
            Reserve a period of time when you will be unavailable.
          </DialogDescription>
        </DialogHeader>
        <form onSubmit={submit} className="space-y-4">
          {isEdit ? (
            <Field>
              <FieldLabel>Scope</FieldLabel>
              {entry ? <AvailabilityBadge scope={entry.scope} /> : null}
            </Field>
          ) : (
            <Field>
              <FieldLabel>Scope</FieldLabel>
              <Select
                value={createScope}
                onValueChange={(value) =>
                  setCreateScope(value as AvailabilityScope)
                }
              >
                <SelectTrigger className="w-full">
                  <SelectValue placeholder="Select a scope" />
                </SelectTrigger>
                <SelectContent>
                  {(Object.keys(SCOPE_LABELS) as AvailabilityScope[])
                    .filter((value) => canManageCompany || value === "PERSONAL")
                    .map((value) => (
                      <SelectItem key={value} value={value}>
                        {SCOPE_LABELS[value]}
                      </SelectItem>
                    ))}
                </SelectContent>
              </Select>
            </Field>
          )}
          <div className="grid gap-4 sm:grid-cols-2">
            <Field>
              <FieldLabel>
                Start date <span className="text-destructive">*</span>
              </FieldLabel>
              <Input
                type="date"
                value={formState.startDate}
                onChange={(event) =>
                  updateFormState("startDate", event.target.value)
                }
                required
              />
            </Field>
            <Field>
              <FieldLabel>
                End date <span className="text-destructive">*</span>
              </FieldLabel>
              <Input
                type="date"
                value={formState.endDate}
                onChange={(event) =>
                  updateFormState("endDate", event.target.value)
                }
                required
              />
            </Field>
          </div>
          <Field>
            <FieldLabel>Reason</FieldLabel>
            <Input
              type="text"
              value={formState.reason}
              onChange={(event) =>
                updateFormState("reason", event.target.value)
              }
              placeholder="Optional description"
            />
          </Field>
          {saveState.error ? <FieldError>{saveState.error}</FieldError> : null}
          <DialogFooter>
            <Button
              type="button"
              variant="outline"
              onClick={onClose}
              disabled={saveState.saving}
            >
              Cancel
            </Button>
            <Button type="submit" disabled={saveState.saving}>
              {saveState.saving ? <Spinner className="size-4" /> : null}
              {saveState.saving ? "Saving..." : isEdit ? "Save" : "Create"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

function toErrorMessage(text: string, fallback: string): string {
  const trimmed = text.trim();
  if (!trimmed) {
    return fallback;
  }
  if (trimmed.startsWith("<!doctype html") || trimmed.startsWith("<html")) {
    return fallback;
  }
  return trimmed;
}

export { AvailabilityDialog };
