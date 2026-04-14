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
import LexicalEditor from "../components/editor/LexicalEditor";
import PageHeader from "../components/layout/PageHeader";
import useJson from "../hooks/useJson";
import useSubmissionGuard from "../hooks/useSubmissionGuard";
import { toast } from "sonner";
import DataState from "../components/common/DataState";
import { postForm } from "../utils/api";
import { resolvePostRedirectPath } from "../utils/routing";
import type { FormMode, SessionPageProps } from "../types/app";
import type { EntitlementRecord, LevelRecord } from "../types/domain";
import type { FormEntries } from "../utils/api";
import {
  Card,
  CardHeader,
  CardTitle,
  CardContent,
} from "../components/ui/card";
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

interface EntitlementVersionForm {
  id: string;
  name: string;
  date: string;
}

interface EntitlementFormState {
  name: string;
  description: string;
  selectedLevelIds: Array<string | number>;
  versions: EntitlementVersionForm[];
}

interface EntitlementFormPageProps extends SessionPageProps {
  mode: FormMode;
}

interface EntitlementFormBootstrap extends EntitlementRecord {
  selectedLevelIds?: Array<string | number>;
  todayDate?: string;
}

export default function EntitlementEditorPage({
  mode,
}: EntitlementFormPageProps) {
  const navigate = useNavigate();

  const { id } = useParams();
  const entitlementState = useJson<EntitlementFormBootstrap>(
    mode === "edit" && id
      ? `/api/entitlements/${id}`
      : "/api/entitlements/bootstrap",
  );
  const entitlement = entitlementState.data;
  const supportLevels = (entitlement?.supportLevels || []) as LevelRecord[];
  const [formState, setFormState] = useState<EntitlementFormState | null>(null);
  const [saveState, setSaveState] = useState({ saving: false, error: "" });
  const submissionGuard = useSubmissionGuard();
  const isEdit = mode === "edit";

  const updateFormState = <K extends keyof EntitlementFormState>(
    field: K,
    value: EntitlementFormState[K],
  ) => {
    setFormState((current) =>
      current ? { ...current, [field]: value } : current,
    );
  };

  useEffect(() => {
    if (!entitlement) {
      return;
    }
    if (isEdit) {
      setFormState({
        name: entitlement.name || "",
        description: entitlement.description || "",
        selectedLevelIds: entitlement.selectedLevelIds || [],
        versions:
          entitlement.versions?.map((version) => ({
            id: version.id ? String(version.id) : "",
            name: version.name || "",
            date: version.date || "",
          })) || [],
      });
      return;
    }
    setFormState({
      name: "",
      description: "",
      selectedLevelIds: [],
      versions: [{ id: "", name: "", date: entitlement.todayDate || "" }],
    });
  }, [entitlement, isEdit]);

  const toggleLevel = (levelId: string | number) => {
    setFormState((current) =>
      current
        ? {
            ...current,
            selectedLevelIds: current.selectedLevelIds.includes(levelId)
              ? current.selectedLevelIds.filter(
                  (existing) => existing !== levelId,
                )
              : [...current.selectedLevelIds, levelId],
          }
        : current,
    );
  };

  const updateVersion = (
    index: number,
    field: keyof EntitlementVersionForm,
    value: string,
  ) => {
    setFormState((current) =>
      current
        ? {
            ...current,
            versions: current.versions.map((version, versionIndex) =>
              versionIndex === index ? { ...version, [field]: value } : version,
            ),
          }
        : current,
    );
  };

  const addVersion = () => {
    setFormState((current) =>
      current
        ? {
            ...current,
            versions: [
              ...current.versions,
              { id: "", name: "", date: entitlement?.todayDate || "" },
            ],
          }
        : current,
    );
  };

  const removeVersion = (index: number) => {
    setFormState((current) =>
      current
        ? {
            ...current,
            versions: current.versions.filter(
              (_, versionIndex) => versionIndex !== index,
            ),
          }
        : current,
    );
  };

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!formState || !submissionGuard.tryEnter()) {
      return;
    }
    try {
      setSaveState({ saving: true, error: "" });
      const entries: FormEntries = [
        ["name", formState.name],
        ["description", formState.description],
        ...formState.selectedLevelIds.map((levelId): [string, string] => [
          "levelIds",
          String(levelId),
        ]),
        ...formState.versions.flatMap((version): [string, string][] => [
          ["versionIds", version.id || ""],
          ["versionNames", version.name],
          ["versionDates", version.date],
        ]),
      ];
      const response = await postForm(
        isEdit ? `/entitlements/${id}` : "/entitlements",
        entries,
      );
      toast.success(
        isEdit
          ? "Entitlement updated successfully."
          : "Entitlement created successfully.",
      );
      navigate(await resolvePostRedirectPath(response, "/entitlements"));
    } catch (error: unknown) {
      setSaveState({
        saving: false,
        error:
          error instanceof Error
            ? error.message
            : "Unable to save entitlement.",
      });
      toast.error(
        error instanceof Error ? error.message : "Unable to save entitlement.",
      );
      return;
    } finally {
      submissionGuard.exit();
    }
    setSaveState({ saving: false, error: "" });
  };

  const deleteEntitlement = async () => {
    if (!id || !submissionGuard.tryEnter()) {
      return;
    }
    try {
      setSaveState({ saving: true, error: "" });
      const response = await postForm(`/entitlements/${id}/delete`, []);
      toast.success("Entitlement deleted.");
      navigate(await resolvePostRedirectPath(response, "/entitlements"));
    } catch (error: unknown) {
      setSaveState({
        saving: false,
        error:
          error instanceof Error
            ? error.message
            : "Unable to delete entitlement.",
      });
      toast.error(
        error instanceof Error
          ? error.message
          : "Unable to delete entitlement.",
      );
      return;
    } finally {
      submissionGuard.exit();
    }
    setSaveState({ saving: false, error: "" });
  };

  return (
    <section className="w-full mt-4">
      <DataState state={entitlementState} emptyMessage="Entitlement not found.">
        {formState && entitlement && (
          <form className="space-y-6 pb-20" onSubmit={submit}>
            <PageHeader
              title={
                isEdit && entitlement ? entitlement.name || "Edit" : "Create"
              }
            />
            {/* ENTITLEMENT DETAILS SECTION */}
            <Card>
              <CardHeader>
                <CardTitle>Entitlement Details</CardTitle>
                <p className="text-sm text-muted-foreground mt-1">
                  Required fields are marked{" "}
                  <span className="text-destructive">*</span>.
                </p>
              </CardHeader>
              <CardContent className="grid gap-6">
                <Field>
                  <FieldLabel>
                    Name <span className="text-destructive">*</span>
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
                  <FieldLabel>
                    Description <span className="text-destructive">*</span>
                  </FieldLabel>
                  <LexicalEditor
                    value={formState.description}
                    onChange={(value) => updateFormState("description", value)}
                    rows={10}
                    required
                  />
                </Field>
              </CardContent>
            </Card>

            {/* SUPPORT LEVELS SECTION */}
            <Card>
              <CardHeader>
                <CardTitle>Support levels</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="grid gap-3 md:grid-cols-2">
                  {supportLevels.map((level) => (
                    <label
                      key={level.id}
                      className="flex items-center space-x-3 rounded-md border p-4 hover:bg-muted/50 cursor-pointer transition-colors"
                    >
                      <input
                        type="checkbox"
                        className="h-4 w-4 rounded border-primary text-primary focus:ring-primary"
                        checked={formState.selectedLevelIds.includes(level.id)}
                        onChange={() => toggleLevel(level.id)}
                      />
                      <div className="flex-1 space-y-1">
                        <p className="text-sm font-medium leading-none">
                          {level.name}
                        </p>
                        <p className="text-sm text-muted-foreground">
                          {level.fromLabel} - {level.toLabel}
                        </p>
                      </div>
                    </label>
                  ))}
                </div>
                {supportLevels.length === 0 && (
                  <p className="text-sm text-muted-foreground italic text-center py-4">
                    No support levels available.
                  </p>
                )}
              </CardContent>
            </Card>

            {/* VERSIONS SECTION */}
            <Card>
              <CardHeader className="flex flex-row items-center justify-between">
                <div>
                  <CardTitle>Versions</CardTitle>
                </div>
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={addVersion}
                >
                  Add Version
                </Button>
              </CardHeader>
              <CardContent className="space-y-4">
                {formState.versions.map((version, index) => (
                  <div
                    key={`${version.id || "new"}-${index}`}
                    className="p-4 border rounded-lg bg-card text-card-foreground shadow-sm flex flex-col md:flex-row md:items-end gap-4"
                  >
                    <div className="grid gap-4 md:grid-cols-2 flex-grow">
                      <Field>
                        <FieldLabel>
                          Version <span className="text-destructive">*</span>
                        </FieldLabel>
                        <Input
                          value={version.name}
                          onChange={(event) =>
                            updateVersion(index, "name", event.target.value)
                          }
                          required
                        />
                      </Field>
                      <Field>
                        <FieldLabel>
                          Date <span className="text-destructive">*</span>
                        </FieldLabel>
                        <Input
                          type="date"
                          value={version.date}
                          onChange={(event) =>
                            updateVersion(index, "date", event.target.value)
                          }
                          required
                        />
                      </Field>
                    </div>
                    <div className="flex-shrink-0 mt-4 md:mt-0">
                      <Button
                        type="button"
                        variant="destructive"
                        onClick={() => removeVersion(index)}
                        disabled={formState.versions.length === 1}
                      >
                        Remove
                      </Button>
                    </div>
                  </div>
                ))}
              </CardContent>
            </Card>

            {/* ACTION BUTTONS */}
            <div className="flex items-center justify-end space-x-3 pt-4 border-t">
              {isEdit && (
                <AlertDialog>
                  <AlertDialogTrigger asChild>
                    <Button
                      type="button"
                      variant="destructive"
                      disabled={saveState.saving}
                      className="mr-auto"
                    >
                      Delete entitlement
                    </Button>
                  </AlertDialogTrigger>
                  <AlertDialogContent>
                    <AlertDialogHeader>
                      <AlertDialogTitle>Are you sure?</AlertDialogTitle>
                      <AlertDialogDescription>
                        This action cannot be undone. This will permanently
                        delete this entitlement.
                      </AlertDialogDescription>
                    </AlertDialogHeader>
                    <AlertDialogFooter>
                      <AlertDialogCancel>Cancel</AlertDialogCancel>
                      <AlertDialogAction
                        onClick={deleteEntitlement}
                        className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
                      >
                        Delete
                      </AlertDialogAction>
                    </AlertDialogFooter>
                  </AlertDialogContent>
                </AlertDialog>
              )}
              <Button type="submit" disabled={saveState.saving}>
                {saveState.saving ? "Saving..." : isEdit ? "Save" : "Create"}
              </Button>
            </div>
          </form>
        )}
      </DataState>
    </section>
  );
}
