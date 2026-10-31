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
import { toast } from "sonner";
import useJson from "../hooks/useJson";
import useSubmissionGuard from "../hooks/useSubmissionGuard";
import useNumberShortcuts from "../hooks/useNumberShortcuts";
import DataState from "../components/common/DataState";
import PageHeader from "../components/layout/PageHeader";
import { postForm, postMultipart } from "../utils/api";
import { resolvePostRedirectPath } from "../utils/routing";
import type { AsyncState, FormMode, SessionPageProps } from "../types/app";
import type { CustomFieldBootstrap, CustomFieldDetail } from "../types/domain";
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
import { Checkbox } from "../components/ui/checkbox";
import { Field, FieldLabel } from "../components/ui/field";
import { Input } from "../components/ui/input";
import { Switch } from "../components/ui/switch";
import { Textarea } from "../components/ui/textarea";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "../components/ui/select";

const DEFAULT_FIELD_TYPES = ["TEXT", "NUMBER", "DATE", "SELECT"];

interface CustomFieldFormState {
  name: string;
  fieldType: string;
  required: boolean;
  description: string;
  optionsText: string;
  active: boolean;
  sortOrder: number;
}

interface CustomFieldFormPageProps extends SessionPageProps {
  mode: FormMode;
}

export default function CustomFieldFormPage({
  mode,
}: CustomFieldFormPageProps) {
  const navigate = useNavigate();
  const { id } = useParams();
  const isEdit = mode === "edit";
  const customFieldState = useJson<CustomFieldDetail>(
    isEdit && id ? `/api/custom-fields/${id}` : null,
  );
  const bootstrapState = useJson<CustomFieldBootstrap>(
    isEdit ? null : "/api/custom-fields/bootstrap",
  );
  const customField = customFieldState.data;
  const [formState, setFormState] = useState<CustomFieldFormState | null>(null);
  const [saveState, setSaveState] = useState({ saving: false, error: "" });
  const submissionGuard = useSubmissionGuard();

  useNumberShortcuts({ enableFieldJumps: true });

  const fieldTypes: string[] =
    (bootstrapState.data?.fieldTypes?.length || 0) > 0
      ? bootstrapState.data?.fieldTypes || DEFAULT_FIELD_TYPES
      : DEFAULT_FIELD_TYPES;

  const updateFormState = <K extends keyof CustomFieldFormState>(
    field: K,
    value: CustomFieldFormState[K],
  ) => {
    setFormState((current) =>
      current ? { ...current, [field]: value } : current,
    );
  };

  useEffect(() => {
    if (isEdit) {
      if (!customField) {
        return;
      }
      setFormState({
        name: customField.name || "",
        fieldType: customField.fieldType || "TEXT",
        required: Boolean(customField.required),
        description: customField.description || "",
        optionsText: (customField.options || []).join("\n"),
        active: customField.active !== false,
        sortOrder: customField.sortOrder ?? 0,
      });
      return;
    }
    if (!bootstrapState.data) {
      return;
    }
    setFormState({
      name: "",
      fieldType: fieldTypes[0] || "TEXT",
      required: false,
      description: "",
      optionsText: "",
      active: true,
      sortOrder: 0,
    });
  }, [bootstrapState.data, customField, fieldTypes, isEdit]);

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!formState || !submissionGuard.tryEnter()) {
      return;
    }
    const entries: Array<[string, string]> = [
      ["name", formState.name],
      ["fieldType", formState.fieldType],
      ["required", String(formState.required)],
      ["description", formState.description],
      ["sortOrder", String(formState.sortOrder)],
      ["active", String(formState.active)],
    ];
    if (formState.fieldType === "SELECT") {
      entries.push(["options", formState.optionsText]);
    }
    try {
      setSaveState({ saving: true, error: "" });
      const response = await postMultipart(
        isEdit && id ? `/custom-fields/${id}` : "/custom-fields",
        entries,
        { headers: { "X-Billetsys-Client": "react" } },
      );
      toast.success(
        isEdit
          ? "Custom field updated successfully."
          : "Custom field created successfully.",
      );
      navigate(await resolvePostRedirectPath(response, "/custom-fields"));
    } catch (error: unknown) {
      setSaveState({
        saving: false,
        error:
          error instanceof Error
            ? error.message
            : "Unable to save custom field.",
      });
      toast.error(
        error instanceof Error ? error.message : "Unable to save custom field.",
      );
      return;
    } finally {
      submissionGuard.exit();
    }
    setSaveState({ saving: false, error: "" });
  };

  const deleteCustomField = async () => {
    if (!id || !submissionGuard.tryEnter()) {
      return;
    }
    try {
      setSaveState({ saving: true, error: "" });
      const response = await postForm(`/custom-fields/${id}/delete`, [], {
        headers: { "X-Billetsys-Client": "react" },
      });
      toast.success("Custom field deleted.");
      navigate(await resolvePostRedirectPath(response, "/custom-fields"));
    } catch (error: unknown) {
      setSaveState({
        saving: false,
        error:
          error instanceof Error
            ? error.message
            : "Unable to delete custom field.",
      });
      toast.error(
        error instanceof Error
          ? error.message
          : "Unable to delete custom field.",
      );
      return;
    } finally {
      submissionGuard.exit();
    }
    setSaveState({ saving: false, error: "" });
  };

  const state: AsyncState<CustomFieldDetail | CustomFieldBootstrap> = isEdit
    ? customFieldState
    : bootstrapState;

  return (
    <section className="w-full mt-4">
      <DataState state={state} emptyMessage="Custom field unavailable.">
        {formState && (
          <>
            <PageHeader
              title={
                isEdit && customField
                  ? customField.name || "Edit custom field"
                  : "New custom field"
              }
            />
            <form onSubmit={submit}>
              <div className="grid gap-6 md:grid-cols-2">
                <Field>
                  <FieldLabel htmlFor="custom-field-name">Name</FieldLabel>
                  <Input
                    id="custom-field-name"
                    value={formState.name}
                    onChange={(event) =>
                      updateFormState("name", event.target.value)
                    }
                    required
                    data-shortcut-index="1"
                  />
                </Field>

                <Field>
                  <FieldLabel htmlFor="custom-field-type">Type</FieldLabel>
                  {isEdit ? (
                    <Input
                      id="custom-field-type"
                      value={formState.fieldType}
                      readOnly
                      data-shortcut-index="2"
                    />
                  ) : (
                    <Select
                      value={formState.fieldType}
                      onValueChange={(value) =>
                        updateFormState("fieldType", value)
                      }
                    >
                      <SelectTrigger
                        id="custom-field-type"
                        className="w-full"
                        data-shortcut-index="2"
                      >
                        <SelectValue placeholder="Select type" />
                      </SelectTrigger>
                      <SelectContent>
                        {fieldTypes.map((fieldType) => (
                          <SelectItem key={fieldType} value={fieldType}>
                            {fieldType}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  )}
                </Field>

                <Field>
                  <FieldLabel htmlFor="custom-field-required">
                    Required
                  </FieldLabel>
                  <div className="flex h-10 items-center gap-2 rounded-md border px-3">
                    <Checkbox
                      id="custom-field-required"
                      checked={formState.required}
                      onCheckedChange={(checked) =>
                        updateFormState("required", checked === true)
                      }
                      data-shortcut-index="3"
                    />
                    <label
                      htmlFor="custom-field-required"
                      className="text-sm font-normal"
                    >
                      Ticket value must be provided
                    </label>
                  </div>
                </Field>

                {isEdit && (
                  <Field>
                    <FieldLabel htmlFor="custom-field-active">
                      Active
                    </FieldLabel>
                    <div className="flex h-10 items-center gap-3 rounded-md border px-3">
                      <Switch
                        id="custom-field-active"
                        checked={formState.active}
                        onCheckedChange={(checked) =>
                          updateFormState("active", checked)
                        }
                      />
                      <span className="text-sm font-normal text-muted-foreground">
                        {formState.active
                          ? "Available in ticket forms"
                          : "Hidden from ticket forms"}
                      </span>
                    </div>
                  </Field>
                )}

                <Field className="md:col-span-2">
                  <FieldLabel htmlFor="custom-field-description">
                    Description
                  </FieldLabel>
                  <Textarea
                    id="custom-field-description"
                    value={formState.description}
                    onChange={(event) =>
                      updateFormState("description", event.target.value)
                    }
                    rows={4}
                    data-shortcut-index="4"
                  />
                </Field>

                {formState.fieldType === "SELECT" && (
                  <Field className="md:col-span-2">
                    <FieldLabel htmlFor="custom-field-options">
                      Options
                    </FieldLabel>
                    <Textarea
                      id="custom-field-options"
                      value={formState.optionsText}
                      onChange={(event) =>
                        updateFormState("optionsText", event.target.value)
                      }
                      rows={6}
                      placeholder={
                        "One option per line\nExample\nAnother option"
                      }
                      data-shortcut-index="5"
                    />
                    <p className="text-sm text-muted-foreground">
                      One option per line. Options that are used by existing
                      tickets cannot be removed or renamed.
                    </p>
                  </Field>
                )}
              </div>

              {saveState.error && (
                <p className="mt-4 text-sm font-medium text-destructive">
                  {saveState.error}
                </p>
              )}

              <div
                className={
                  isEdit
                    ? "mt-4 flex items-center justify-between gap-3"
                    : "mt-4 flex items-center justify-end"
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
                            This action cannot be undone. This will permanently
                            delete this custom field and any values saved on
                            tickets. To keep ticket values, deactivate the field
                            instead.
                          </AlertDialogDescription>
                        </AlertDialogHeader>
                        <AlertDialogFooter>
                          <AlertDialogCancel>Cancel</AlertDialogCancel>
                          <AlertDialogAction
                            onClick={deleteCustomField}
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
                  {saveState.saving ? "Saving..." : isEdit ? "Save" : "Create"}
                </Button>
              </div>
            </form>
          </>
        )}
      </DataState>
    </section>
  );
}
