/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import type { FormEvent } from "react";
import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import DataState from "../components/common/DataState";
import { toast } from "sonner";
import PageHeader from "../components/layout/PageHeader";
import LexicalEditor from "../components/editor/LexicalEditor";
import useJson from "../hooks/useJson";
import useSubmissionGuard from "../hooks/useSubmissionGuard";
import { PATHS } from "../routes/paths";
import { postForm } from "../utils/api";
import { levelColorMarker } from "../utils/formatting";
import { resolvePostRedirectPath } from "../utils/routing";
import type { FormMode, SessionPageProps } from "../types/app";
import type {
  CountryOption,
  LevelRecord,
  TimezoneOption,
} from "../types/domain";
import { Button } from "../components/ui/button";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from "../components/ui/alert-dialog";
import { Field, FieldLabel } from "../components/ui/field";
import { Input } from "../components/ui/input";
import { CountryDropdown } from "../components/ui/aevr/country-dropdown";
import { countries } from "country-data-list";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "../components/ui/select";

interface LevelFormState {
  name: string;
  description: string;
  level: string;
  color: string;
  fromDay: string;
  fromTime: string;
  toDay: string;
  toTime: string;
  countryId: string;
  timezoneId: string;
}

interface LevelOption {
  value?: string | number;
  label?: string;
  display?: string;
}

interface LevelFormBootstrap extends LevelRecord {
  defaultLevel?: number;
  defaultColor?: string;
  defaultFromDay?: number;
  defaultFromTime?: number;
  defaultToDay?: number;
  defaultToTime?: number;
  defaultCountryId?: string | number;
  defaultTimezoneId?: string | number;
  countryId?: string | number;
  timezoneId?: string | number;
  colorOptions?: LevelOption[];
  dayOptions?: LevelOption[];
  hourOptions?: LevelOption[];
  countries?: CountryOption[];
  timezones?: TimezoneOption[];
}

interface LevelFormPageProps extends SessionPageProps {
  mode: FormMode;
}

export default function LevelFormPage({ mode }: LevelFormPageProps) {
  const navigate = useNavigate();

  const { id } = useParams();
  const levelState = useJson<LevelFormBootstrap>(
    mode === "edit" && id ? `/api/levels/${id}` : "/api/levels/bootstrap",
  );
  const level = levelState.data;
  const [formState, setFormState] = useState<LevelFormState | null>(null);
  const [saveState, setSaveState] = useState({ saving: false, error: "" });
  const submissionGuard = useSubmissionGuard();
  const isEdit = mode === "edit";

  useEffect(() => {
    if (!level) {
      return;
    }
    setFormState({
      name: level.name || "",
      description: level.description || "",
      level: String(level.level ?? level.defaultLevel ?? 0),
      color: level.color || level.defaultColor || "White",
      fromDay: String(level.fromDay ?? level.defaultFromDay ?? 1),
      fromTime: String(level.fromTime ?? level.defaultFromTime ?? 0),
      toDay: String(level.toDay ?? level.defaultToDay ?? 7),
      toTime: String(level.toTime ?? level.defaultToTime ?? 23),
      countryId: level.countryId
        ? String(level.countryId)
        : level.defaultCountryId
          ? String(level.defaultCountryId)
          : "",
      timezoneId: level.timezoneId
        ? String(level.timezoneId)
        : level.defaultTimezoneId
          ? String(level.defaultTimezoneId)
          : "",
    });
  }, [level]);

  const availableTimezones =
    level?.timezones?.filter(
      (timezone) =>
        !formState?.countryId ||
        String(timezone.countryId) === formState.countryId,
    ) || [];

  const updateFormState = <K extends keyof LevelFormState>(
    field: K,
    value: LevelFormState[K],
  ) => {
    setFormState((current) =>
      current ? { ...current, [field]: value } : current,
    );
  };

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!formState || !submissionGuard.tryEnter()) {
      return;
    }
    try {
      setSaveState({ saving: true, error: "" });
      const response = await postForm(
        isEdit ? `${PATHS.levels}/${id}` : PATHS.levels,
        [
          ["name", formState.name],
          ["description", formState.description],
          ["level", formState.level],
          ["color", formState.color],
          ["fromDay", formState.fromDay],
          ["fromTime", formState.fromTime],
          ["toDay", formState.toDay],
          ["toTime", formState.toTime],
          ["countryId", formState.countryId],
          ["timezoneId", formState.timezoneId],
        ],
      );
      toast.success(
        isEdit ? "Level updated successfully." : "Level created successfully.",
      );
      navigate(await resolvePostRedirectPath(response, PATHS.levels));
    } catch (error: unknown) {
      setSaveState({
        saving: false,
        error: error instanceof Error ? error.message : "Unable to save level.",
      });
      toast.error(
        error instanceof Error ? error.message : "Unable to save level.",
      );
      return;
    } finally {
      submissionGuard.exit();
    }
    setSaveState({ saving: false, error: "" });
  };

  const deleteLevel = async () => {
    if (!id || !submissionGuard.tryEnter()) {
      return;
    }
    try {
      setSaveState({ saving: true, error: "" });
      const response = await postForm(`${PATHS.levels}/${id}/delete`, []);
      toast.success("Level deleted.");
      navigate(await resolvePostRedirectPath(response, PATHS.levels));
    } catch (error: unknown) {
      setSaveState({
        saving: false,
        error:
          error instanceof Error ? error.message : "Unable to delete level.",
      });
      toast.error(
        error instanceof Error ? error.message : "Unable to delete level.",
      );
      return;
    } finally {
      submissionGuard.exit();
    }
    setSaveState({ saving: false, error: "" });
  };

  return (
    <section className="w-full mt-4">
      <DataState state={levelState} emptyMessage="Level not found.">
        {formState && level && (
          <>
            <PageHeader
              title={isEdit && level ? level.name || "Edit" : "Create"}
            />
            <div
              className={
                isEdit ? "space-y-4" : "rounded-xl border bg-card shadow-sm"
              }
            >
              <form onSubmit={submit}>
                <div
                  className={
                    isEdit
                      ? "grid gap-6 md:grid-cols-2"
                      : "grid gap-6 md:grid-cols-2 px-6 pt-6 pb-6"
                  }
                >
                  <Field>
                    <FieldLabel className="text-[var(--color-header-bg)]">
                      Name
                    </FieldLabel>
                    <Input
                      value={formState.name}
                      onChange={(event) =>
                        updateFormState("name", event.target.value)
                      }
                      required
                    />
                  </Field>
                  <Field>
                    <FieldLabel className="text-[var(--color-header-bg)]">
                      Level
                    </FieldLabel>
                    <Input
                      type="number"
                      min="0"
                      value={formState.level}
                      onChange={(event) =>
                        updateFormState("level", event.target.value)
                      }
                      required
                    />
                  </Field>
                  <Field>
                    <FieldLabel className="text-[var(--color-header-bg)]">
                      Color
                    </FieldLabel>
                    <Select
                      value={formState.color || undefined}
                      onValueChange={(value) => updateFormState("color", value)}
                    >
                      <SelectTrigger className="w-full">
                        <SelectValue placeholder="Select color" />
                      </SelectTrigger>
                      <SelectContent>
                        {(level.colorOptions || []).map(
                          (option: LevelOption) => (
                            <SelectItem
                              key={option.value}
                              value={String(option.value ?? "")}
                            >
                              {`${levelColorMarker(String(option.value ?? ""))} ${option.display || `(${option.value ?? ""})`}`}
                            </SelectItem>
                          ),
                        )}
                      </SelectContent>
                    </Select>
                  </Field>
                  <Field>
                    <FieldLabel className="text-[var(--color-header-bg)]">
                      Country
                    </FieldLabel>
                    <CountryDropdown
                      defaultValue={
                        countries.all.find(
                          (c) =>
                            c.name ===
                            (level.countries || []).find(
                              (lc: CountryOption) =>
                                String(lc.id) === formState.countryId,
                            )?.name,
                        )?.alpha2 || ""
                      }
                      onChange={(country) => {
                        const matched = (level.countries || []).find(
                          (c: CountryOption) => c.name === country.name,
                        );
                        const nextCountryId = matched ? String(matched.id) : "";
                        const timezoneStillValid = availableTimezones.some(
                          (timezone) =>
                            String(timezone.id) === formState.timezoneId,
                        );
                        setFormState((current) =>
                          current
                            ? {
                                ...current,
                                countryId: nextCountryId,
                                timezoneId: timezoneStillValid
                                  ? current.timezoneId
                                  : "",
                              }
                            : current,
                        );
                      }}
                    />
                  </Field>
                  <Field>
                    <FieldLabel className="text-[var(--color-header-bg)]">
                      From day
                    </FieldLabel>
                    <Select
                      value={formState.fromDay || undefined}
                      onValueChange={(value) =>
                        updateFormState("fromDay", value)
                      }
                    >
                      <SelectTrigger className="w-full">
                        <SelectValue placeholder="Select day" />
                      </SelectTrigger>
                      <SelectContent>
                        {(level.dayOptions || []).map((option: LevelOption) => (
                          <SelectItem
                            key={option.value}
                            value={String(option.value)}
                          >
                            {option.label}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </Field>
                  <Field>
                    <FieldLabel className="text-[var(--color-header-bg)]">
                      To day
                    </FieldLabel>
                    <Select
                      value={formState.toDay || undefined}
                      onValueChange={(value) => updateFormState("toDay", value)}
                    >
                      <SelectTrigger className="w-full">
                        <SelectValue placeholder="Select day" />
                      </SelectTrigger>
                      <SelectContent>
                        {(level.dayOptions || []).map((option: LevelOption) => (
                          <SelectItem
                            key={option.value}
                            value={String(option.value)}
                          >
                            {option.label}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </Field>
                  <Field>
                    <FieldLabel className="text-[var(--color-header-bg)]">
                      Time zone
                    </FieldLabel>
                    <Select
                      value={formState.timezoneId || undefined}
                      onValueChange={(value) =>
                        updateFormState("timezoneId", value)
                      }
                    >
                      <SelectTrigger className="w-full">
                        <SelectValue placeholder="Select a time zone" />
                      </SelectTrigger>
                      <SelectContent>
                        {availableTimezones.map((timezone: TimezoneOption) => (
                          <SelectItem
                            key={timezone.id}
                            value={String(timezone.id)}
                          >
                            {timezone.name}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </Field>
                  <div className="hidden md:block" aria-hidden="true" />
                  <Field>
                    <FieldLabel className="text-[var(--color-header-bg)]">
                      From time
                    </FieldLabel>
                    <Select
                      value={formState.fromTime || undefined}
                      onValueChange={(value) =>
                        updateFormState("fromTime", value)
                      }
                    >
                      <SelectTrigger className="w-full">
                        <SelectValue placeholder="Select time" />
                      </SelectTrigger>
                      <SelectContent>
                        {(level.hourOptions || []).map(
                          (option: LevelOption) => (
                            <SelectItem
                              key={option.value}
                              value={String(option.value)}
                            >
                              {option.label}
                            </SelectItem>
                          ),
                        )}
                      </SelectContent>
                    </Select>
                  </Field>
                  <Field>
                    <FieldLabel className="text-[var(--color-header-bg)]">
                      To time
                    </FieldLabel>
                    <Select
                      value={formState.toTime || undefined}
                      onValueChange={(value) =>
                        updateFormState("toTime", value)
                      }
                    >
                      <SelectTrigger className="w-full">
                        <SelectValue placeholder="Select time" />
                      </SelectTrigger>
                      <SelectContent>
                        {(level.hourOptions || []).map(
                          (option: LevelOption) => (
                            <SelectItem
                              key={option.value}
                              value={String(option.value)}
                            >
                              {option.label}
                            </SelectItem>
                          ),
                        )}
                      </SelectContent>
                    </Select>
                  </Field>
                  <Field className="md:col-span-2">
                    <FieldLabel className="text-[var(--color-header-bg)]">
                      Description
                    </FieldLabel>
                    <LexicalEditor
                      value={formState.description}
                      onChange={(value) =>
                        updateFormState("description", value)
                      }
                      rows={10}
                      required
                    />
                  </Field>
                </div>

                <div
                  className={
                    isEdit
                      ? "flex items-center justify-between gap-3 pt-4"
                      : "mt-4 flex items-center justify-end border-t bg-muted/20 px-6 pt-6"
                  }
                >
                  <div>
                    {isEdit && (
                      <AlertDialog>
                        <AlertDialogTrigger asChild>
                          <Button
                            type="button"
                            variant="destructive"
                            disabled={saveState.saving}
                          >
                            Delete
                          </Button>
                        </AlertDialogTrigger>
                        <AlertDialogContent>
                          <AlertDialogHeader>
                            <AlertDialogTitle>Are you sure?</AlertDialogTitle>
                            <AlertDialogDescription>
                              This action cannot be undone. This will
                              permanently delete this level.
                            </AlertDialogDescription>
                          </AlertDialogHeader>
                          <AlertDialogFooter>
                            <AlertDialogCancel>Cancel</AlertDialogCancel>
                            <AlertDialogAction
                              onClick={deleteLevel}
                              className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
                            >
                              Delete
                            </AlertDialogAction>
                          </AlertDialogFooter>
                        </AlertDialogContent>
                      </AlertDialog>
                    )}
                  </div>
                  <Button type="submit" disabled={saveState.saving}>
                    {saveState.saving
                      ? "Saving..."
                      : isEdit
                        ? "Save"
                        : "Create"}
                  </Button>
                </div>
              </form>
            </div>
          </>
        )}
      </DataState>
    </section>
  );
}
