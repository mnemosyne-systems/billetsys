/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import type { FormEvent } from "react";
import { useEffect, useRef, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { toast } from "sonner";
import useJson from "../hooks/useJson";
import useSubmissionGuard from "../hooks/useSubmissionGuard";
import DataState from "../components/common/DataState";
import PageHeader from "../components/layout/PageHeader";
import LexicalEditor from "../components/editor/LexicalEditor";
import { postForm, postMultipart } from "../utils/api";
import { resolvePostRedirectPath } from "../utils/routing";
import type { FormMode, SessionPageProps } from "../types/app";
import type { CategoryRecord } from "../types/domain";
import { Card, CardContent, CardFooter } from "../components/ui/card";
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
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "../components/ui/select";

interface CategoryFormState {
  name: string;
  description: string;
  isDefault: boolean;
}

interface CategoryFormPageProps extends SessionPageProps {
  mode: FormMode;
}

export default function CategoryFormPage({ mode }: CategoryFormPageProps) {
  const navigate = useNavigate();

  const { id } = useParams();
  const categoryState = useJson<CategoryRecord>(
    mode === "edit" && id
      ? `/api/categories/${id}`
      : "/api/categories/bootstrap",
  );
  const category = categoryState.data;
  const [formState, setFormState] = useState<CategoryFormState | null>(null);
  const [saveState, setSaveState] = useState({ saving: false, error: "" });
  const descriptionInputRef = useRef<HTMLTextAreaElement | null>(null);
  const submissionGuard = useSubmissionGuard();
  const isEdit = mode === "edit";

  const updateFormState = <K extends keyof CategoryFormState>(
    field: K,
    value: CategoryFormState[K],
  ) => {
    setFormState((current) =>
      current ? { ...current, [field]: value } : current,
    );
  };

  useEffect(() => {
    if (!category) {
      return;
    }
    if (isEdit) {
      setFormState({
        name: category.name || "",
        description: category.description || "",
        isDefault: Boolean(category.isDefault),
      });
      return;
    }
    setFormState({ name: "", description: "", isDefault: false });
  }, [category, isEdit]);

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!formState || !submissionGuard.tryEnter()) {
      return;
    }
    try {
      setSaveState({ saving: true, error: "" });
      const response = await postMultipart(
        isEdit ? `/categories/${id}` : "/categories",
        [
          ["name", formState.name],
          ["description", formState.description],
          ["isDefault", String(formState.isDefault)],
        ],
      );
      toast.success(
        isEdit
          ? "Category updated successfully."
          : "Category created successfully.",
      );
      navigate(
        await resolvePostRedirectPath(
          response,
          isEdit && id ? `/categories/${id}` : "/categories",
        ),
      );
    } catch (error: unknown) {
      setSaveState({
        saving: false,
        error:
          error instanceof Error ? error.message : "Unable to save category.",
      });
      toast.error(
        error instanceof Error ? error.message : "Unable to save category.",
      );
      return;
    } finally {
      submissionGuard.exit();
    }
    setSaveState({ saving: false, error: "" });
  };

  const deleteCategory = async () => {
    if (!id || !submissionGuard.tryEnter()) {
      return;
    }
    try {
      setSaveState({ saving: true, error: "" });
      const response = await postForm(`/categories/${id}/delete`, []);
      toast.success("Category deleted.");
      navigate(await resolvePostRedirectPath(response, "/categories"));
    } catch (error: unknown) {
      setSaveState({
        saving: false,
        error:
          error instanceof Error ? error.message : "Unable to delete category.",
      });
      toast.error(
        error instanceof Error ? error.message : "Unable to delete category.",
      );
      return;
    } finally {
      submissionGuard.exit();
    }
    setSaveState({ saving: false, error: "" });
  };

  return (
    <section className="w-full mt-4">
      <DataState state={categoryState} emptyMessage="Category unavailable.">
        {formState && (
          <>
            <PageHeader
              title={isEdit && category ? category.name || "Edit" : "Create"}
            />
            <Card>
              <form onSubmit={submit}>
                <CardContent className="grid gap-6 md:grid-cols-2 pt-6 pb-6">
                  <Field>
                    <FieldLabel>Name</FieldLabel>
                    <Input
                      value={formState.name}
                      onChange={(event) =>
                        updateFormState("name", event.target.value)
                      }
                      required
                    />
                  </Field>
                  <Field>
                    <FieldLabel>Default</FieldLabel>
                    <Select
                      value={String(formState.isDefault)}
                      onValueChange={(value) =>
                        updateFormState("isDefault", value === "true")
                      }
                    >
                      <SelectTrigger className="w-full">
                        <SelectValue placeholder="Select option" />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="false">No</SelectItem>
                        <SelectItem value="true">Yes</SelectItem>
                      </SelectContent>
                    </Select>
                  </Field>
                  <Field className="md:col-span-2">
                    <FieldLabel>Description</FieldLabel>
                    <LexicalEditor
                      value={formState.description}
                      onChange={(value) =>
                        updateFormState("description", value)
                      }
                      inputRef={descriptionInputRef}
                      rows={10}
                    />
                  </Field>
                </CardContent>
                <CardFooter
                  className={`flex items-center pt-6 border-t mt-4 bg-muted/20 ${isEdit ? "justify-between" : "justify-end"}`}
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
                              permanently delete this category.
                            </AlertDialogDescription>
                          </AlertDialogHeader>
                          <AlertDialogFooter>
                            <AlertDialogCancel>Cancel</AlertDialogCancel>
                            <AlertDialogAction
                              onClick={deleteCategory}
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
                </CardFooter>
              </form>
            </Card>
          </>
        )}
      </DataState>
    </section>
  );
}
