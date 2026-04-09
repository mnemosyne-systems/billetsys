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
import LexicalEditor from "../components/editor/LexicalEditor";
import AttachmentPicker from "../components/common/AttachmentPicker";
import { postMultipart } from "../utils/api";
import { resolvePostRedirectPath } from "../utils/routing";
import { DeleteArticleButton } from "./ArticleDetailPage";
import type { FormMode, SessionPageProps } from "../types/app";
import type { ArticleRecord } from "../types/domain";
import {
  Card,
  CardHeader,
  CardTitle,
  CardContent,
  CardFooter,
} from "../components/ui/card";
import { Button } from "../components/ui/button";
import { Field, FieldLabel } from "../components/ui/field";
import { Input } from "../components/ui/input";

interface ArticleFormState {
  title: string;
  tags: string;
  body: string;
}

interface ArticleFormPageProps extends SessionPageProps {
  mode: FormMode;
}

export default function ArticleFormPage({
  sessionState,
  mode,
}: ArticleFormPageProps) {
  const navigate = useNavigate();

  const { id } = useParams();
  const articleState = useJson<ArticleRecord>(
    mode === "edit" && id ? `/api/articles/${id}` : "/api/articles/bootstrap",
  );
  const article = articleState.data;
  const [formState, setFormState] = useState<ArticleFormState | null>(null);
  const [saveState, setSaveState] = useState({ saving: false, error: "" });
  const [files, setFiles] = useState<File[]>([]);
  const bodyInputRef = useRef<HTMLTextAreaElement | null>(null);
  const submissionGuard = useSubmissionGuard();
  const isEdit = mode === "edit";

  const updateFormState = (field: keyof ArticleFormState, value: string) => {
    setFormState((current) =>
      current ? { ...current, [field]: value } : current,
    );
  };

  useEffect(() => {
    if (!article) {
      return;
    }
    if (isEdit) {
      setFormState({
        title: article.title || "",
        tags: article.tags || "",
        body: article.body || "",
      });
      return;
    }
    setFormState({ title: "", tags: "", body: "" });
  }, [article, isEdit]);

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!formState || !submissionGuard.tryEnter()) {
      return;
    }
    try {
      setSaveState({ saving: true, error: "" });
      const response = await postMultipart(
        isEdit ? `/articles/${id}` : "/articles",
        [
          ["title", formState.title],
          ["tags", formState.tags],
          ["body", formState.body],
          ...files.map((file): ["attachments", File] => ["attachments", file]),
        ],
      );
      toast.success(
        isEdit
          ? "Article updated successfully."
          : "Article created successfully.",
      );
      navigate(
        await resolvePostRedirectPath(
          response,
          isEdit && id ? `/articles/${id}` : "/articles",
        ),
      );
    } catch (error: unknown) {
      setSaveState({
        saving: false,
        error:
          error instanceof Error ? error.message : "Unable to save article.",
      });
      toast.error(
        error instanceof Error ? error.message : "Unable to save article.",
      );
      return;
    } finally {
      submissionGuard.exit();
    }
    setSaveState({ saving: false, error: "" });
  };

  return (
    <section className="w-full max-w-5xl mx-auto mt-4">
      <DataState
        state={articleState}
        emptyMessage="Article unavailable."
        signInHref={sessionState.data?.homePath || "/login"}
      >
        {formState && article && article.canEdit && (
          <Card>
            {isEdit && (
              <CardHeader className="pb-6">
                <CardTitle className="text-3xl font-bold tracking-tight">
                  {formState.title || "Edit article"}
                </CardTitle>
              </CardHeader>
            )}
            <form onSubmit={submit}>
              <CardContent
                className={`grid gap-6 md:grid-cols-2 pb-6 ${!isEdit ? "pt-6" : ""}`}
              >
                <Field>
                  <FieldLabel>Title</FieldLabel>
                  <Input
                    value={formState.title}
                    onChange={(event) =>
                      updateFormState("title", event.target.value)
                    }
                    required
                  />
                </Field>
                <Field>
                  <FieldLabel>Tags</FieldLabel>
                  <Input
                    value={formState.tags}
                    onChange={(event) =>
                      updateFormState("tags", event.target.value)
                    }
                  />
                </Field>
                <Field className="md:col-span-2">
                  <FieldLabel>Body</FieldLabel>
                  <LexicalEditor
                    value={formState.body}
                    onChange={(value) => updateFormState("body", value)}
                    inputRef={bodyInputRef}
                    rows={12}
                    required
                  />
                </Field>

                <div className="md:col-span-2 mt-4">
                  <AttachmentPicker
                    files={files}
                    onFilesChange={setFiles}
                    existingAttachments={article.attachments || []}
                  />
                </div>
              </CardContent>

              <CardFooter className="flex items-center justify-between pt-6 border-t mt-4 bg-muted/20">
                <div>
                  {isEdit && article.id && article.canDelete && (
                    <DeleteArticleButton
                      articleId={article.id}
                      label="Delete"
                    />
                  )}
                </div>
                <Button type="submit" disabled={saveState.saving}>
                  {saveState.saving ? "Saving..." : isEdit ? "Save" : "Create"}
                </Button>
              </CardFooter>
            </form>
          </Card>
        )}
      </DataState>
    </section>
  );
}
