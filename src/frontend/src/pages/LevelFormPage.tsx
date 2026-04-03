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
import {
  buildToastNavigationState,
  useToast,
} from "../components/common/ToastProvider";
import MarkdownEditor from "../components/markdown/MarkdownEditor";
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

export default function LevelFormPage({
  sessionState,
  mode,
}: LevelFormPageProps) {
  const navigate = useNavigate();
  const { showToast } = useToast();
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
      navigate(await resolvePostRedirectPath(response, PATHS.levels), {
        state: buildToastNavigationState({
          variant: "success",
          message: isEdit
            ? "Level updated successfully."
            : "Level created successfully.",
        }),
      });
    } catch (error: unknown) {
      setSaveState({
        saving: false,
        error: error instanceof Error ? error.message : "Unable to save level.",
      });
      showToast({
        variant: "error",
        message:
          error instanceof Error ? error.message : "Unable to save level.",
      });
      return;
    } finally {
      submissionGuard.exit();
    }
    setSaveState({ saving: false, error: "" });
  };

  const deleteLevel = async () => {
    if (
      !id ||
      !window.confirm("Delete this level?") ||
      !submissionGuard.tryEnter()
    ) {
      return;
    }
    try {
      setSaveState({ saving: true, error: "" });
      const response = await postForm(`${PATHS.levels}/${id}/delete`, []);
      navigate(await resolvePostRedirectPath(response, PATHS.levels), {
        state: buildToastNavigationState({
          variant: "danger",
          message: "Level deleted.",
        }),
      });
    } catch (error: unknown) {
      setSaveState({
        saving: false,
        error:
          error instanceof Error ? error.message : "Unable to delete level.",
      });
      showToast({
        variant: "error",
        message:
          error instanceof Error ? error.message : "Unable to delete level.",
      });
      return;
    } finally {
      submissionGuard.exit();
    }
    setSaveState({ saving: false, error: "" });
  };

  return (
    <section className="panel">
      <DataState
        state={levelState}
        emptyMessage="Level not found."
        signInHref={sessionState.data?.homePath || "/login"}
      >
        {formState && level && (
          <div className="form-card ticket-detail-card">
            <form className="owner-form" onSubmit={submit}>
              <div className="owner-form-grid ticket-detail-grid">
                <label>
                  Name
                  <input
                    value={formState.name}
                    onChange={(event) =>
                      updateFormState("name", event.target.value)
                    }
                    required
                  />
                </label>
                <label>
                  Level
                  <input
                    type="number"
                    min="0"
                    value={formState.level}
                    onChange={(event) =>
                      updateFormState("level", event.target.value)
                    }
                    required
                  />
                </label>
                <label>
                  Color
                  <select
                    value={formState.color}
                    onChange={(event) =>
                      updateFormState("color", event.target.value)
                    }
                  >
                    {(level.colorOptions || []).map((option: LevelOption) => (
                      <option key={option.value} value={option.value}>
                        {`${levelColorMarker(String(option.value ?? ""))} ${option.display || `(${option.value ?? ""})`}`}
                      </option>
                    ))}
                  </select>
                </label>
                <label>
                  Country
                  <select
                    value={formState.countryId}
                    onChange={(event) => {
                      const nextCountryId = event.target.value;
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
                  >
                    <option value="">Select a country</option>
                    {(level.countries || []).map((country: CountryOption) => (
                      <option key={country.id} value={country.id}>
                        {country.name}
                      </option>
                    ))}
                  </select>
                </label>
                <label>
                  From day
                  <select
                    value={formState.fromDay}
                    onChange={(event) =>
                      updateFormState("fromDay", event.target.value)
                    }
                  >
                    {(level.dayOptions || []).map((option: LevelOption) => (
                      <option key={option.value} value={option.value}>
                        {option.label}
                      </option>
                    ))}
                  </select>
                </label>
                <label>
                  To day
                  <select
                    value={formState.toDay}
                    onChange={(event) =>
                      updateFormState("toDay", event.target.value)
                    }
                  >
                    {(level.dayOptions || []).map((option: LevelOption) => (
                      <option key={option.value} value={option.value}>
                        {option.label}
                      </option>
                    ))}
                  </select>
                </label>
                <label>
                  Time zone
                  <select
                    value={formState.timezoneId}
                    onChange={(event) =>
                      updateFormState("timezoneId", event.target.value)
                    }
                  >
                    <option value="">Select a time zone</option>
                    {availableTimezones.map((timezone: TimezoneOption) => (
                      <option key={timezone.id} value={timezone.id}>
                        {timezone.name}
                      </option>
                    ))}
                  </select>
                </label>
                <div className="detail-card-spacer" aria-hidden="true" />
                <label>
                  From time
                  <select
                    value={formState.fromTime}
                    onChange={(event) =>
                      updateFormState("fromTime", event.target.value)
                    }
                  >
                    {(level.hourOptions || []).map((option: LevelOption) => (
                      <option key={option.value} value={option.value}>
                        {option.label}
                      </option>
                    ))}
                  </select>
                </label>
                <label>
                  To time
                  <select
                    value={formState.toTime}
                    onChange={(event) =>
                      updateFormState("toTime", event.target.value)
                    }
                  >
                    {(level.hourOptions || []).map((option: LevelOption) => (
                      <option key={option.value} value={option.value}>
                        {option.label}
                      </option>
                    ))}
                  </select>
                </label>
                <label className="form-span-2">
                  Description
                  <MarkdownEditor
                    value={formState.description}
                    onChange={(value) => updateFormState("description", value)}
                    rows={10}
                    required
                  />
                </label>
              </div>

              <div
                className={`button-row${isEdit ? " button-row-split" : " button-row-end"}`}
              >
                {isEdit && (
                  <button
                    type="button"
                    className="secondary-button danger-button"
                    onClick={deleteLevel}
                    disabled={saveState.saving}
                  >
                    Delete
                  </button>
                )}
                <button
                  type="submit"
                  className="primary-button"
                  disabled={saveState.saving}
                >
                  {saveState.saving ? "Saving..." : isEdit ? "Save" : "Create"}
                </button>
              </div>
            </form>
          </div>
        )}
      </DataState>
    </section>
  );
}
