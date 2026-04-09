/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import { useParams } from "react-router-dom";
import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { toast } from "sonner";
import useJson from "../hooks/useJson";
import useSubmissionGuard from "../hooks/useSubmissionGuard";
import DataState from "../components/common/DataState";
import MarkdownContent from "../components/markdown/MarkdownContent";
import { resolvePostRedirectPath, SmartLink } from "../utils/routing";
import { postForm } from "../utils/api";
import type { SessionPageProps } from "../types/app";
import type { ArticleRecord } from "../types/domain";
import {
  Card,
  CardHeader,
  CardTitle,
  CardContent,
  CardFooter,
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

interface DeleteArticleButtonProps {
  articleId: string | number;
  label?: string;
}

function DeleteArticleButton({
  articleId,
  label = "Delete article",
}: DeleteArticleButtonProps) {
  const navigate = useNavigate();

  const [deleting, setDeleting] = useState(false);
  const submissionGuard = useSubmissionGuard();

  const remove = async () => {
    if (!submissionGuard.tryEnter()) {
      return;
    }
    try {
      setDeleting(true);
      const response = await postForm(`/articles/${articleId}/delete`, []);
      toast.success("Article deleted.");
      navigate(await resolvePostRedirectPath(response, "/articles"));
    } catch (submitError: unknown) {
      setDeleting(false);
      toast.error(
        submitError instanceof Error
          ? submitError.message
          : "Unable to delete article.",
      );
      return;
    } finally {
      submissionGuard.exit();
    }
    setDeleting(false);
  };

  return (
    <AlertDialog>
      <AlertDialogTrigger asChild>
        <Button type="button" variant="destructive" disabled={deleting}>
          {deleting ? "Deleting..." : label}
        </Button>
      </AlertDialogTrigger>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>Are you sure?</AlertDialogTitle>
          <AlertDialogDescription>
            This action cannot be undone. This will permanently delete this
            article.
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel>Cancel</AlertDialogCancel>
          <AlertDialogAction
            onClick={remove}
            className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
          >
            Delete
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}

export { DeleteArticleButton };

export default function ArticleDetailPage({ sessionState }: SessionPageProps) {
  const { id } = useParams();
  const articleState = useJson<ArticleRecord>(
    id ? `/api/articles/${id}` : null,
  );
  const article = articleState.data;

  return (
    <section className="w-full max-w-5xl mx-auto mt-4">
      <DataState
        state={articleState}
        emptyMessage="Article not found."
        signInHref={sessionState.data?.homePath || "/login"}
      >
        {article && (
          <Card>
            <CardHeader className="pb-6">
              <CardTitle className="text-3xl font-bold tracking-tight">
                {article.title || "Article details"}
              </CardTitle>
            </CardHeader>
            <CardContent>
              <div className="grid gap-6 md:grid-cols-2">
                <Field>
                  <FieldLabel>Title</FieldLabel>
                  <Input value={article.title || "—"} readOnly />
                </Field>
                <Field>
                  <FieldLabel>Tags</FieldLabel>
                  <Input value={article.tags || "—"} readOnly />
                </Field>
                <div className="md:col-span-2 bg-muted/30 p-5 rounded-lg border border-border">
                  <h3 className="font-semibold text-sm mb-3">Body</h3>
                  <div>
                    {article.body ? (
                      <div className="prose prose-sm dark:prose-invert max-w-none">
                        <MarkdownContent>{article.body}</MarkdownContent>
                      </div>
                    ) : (
                      <p className="text-sm text-muted-foreground">No body.</p>
                    )}
                  </div>
                </div>
                <div className="md:col-span-2 bg-muted/30 p-5 rounded-lg border border-border">
                  <h3 className="font-semibold text-sm mb-3">Attachments</h3>
                  <div>
                    {article.attachments.length === 0 ? (
                      <p className="text-sm text-muted-foreground">
                        No attachments.
                      </p>
                    ) : (
                      <div className="rounded-md border bg-background overflow-hidden">
                        <div className="grid grid-cols-[1fr_auto_auto] gap-4 p-2.5 bg-muted font-semibold text-sm text-muted-foreground">
                          <span>Name</span>
                          <span>Type</span>
                          <span>Size</span>
                        </div>
                        {article.attachments.map((attachment) => (
                          <div
                            key={attachment.id}
                            className="grid grid-cols-[1fr_auto_auto] gap-4 p-2.5 border-t text-sm items-center"
                          >
                            <a
                              href={attachment.downloadPath}
                              target="_blank"
                              rel="noreferrer"
                              className="text-primary hover:underline truncate"
                            >
                              {attachment.name}
                            </a>
                            <span className="text-muted-foreground">
                              {attachment.mimeType}
                            </span>
                            <span className="text-muted-foreground whitespace-nowrap">
                              {attachment.sizeLabel}
                            </span>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                </div>
              </div>
            </CardContent>

            {(article.canDelete || (article.canEdit && article.editPath)) && (
              <CardFooter className="flex justify-end gap-3 pt-6 border-t mt-4 bg-muted/20">
                {article.canDelete && (
                  <DeleteArticleButton articleId={article.id} label="Delete" />
                )}
                {article.canEdit && article.editPath && (
                  <Button asChild>
                    <SmartLink href={article.editPath}>Edit</SmartLink>
                  </Button>
                )}
              </CardFooter>
            )}
          </Card>
        )}
      </DataState>
    </section>
  );
}
